package com.nibras.nova.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.data.local.dao.DownloadDao
import com.nibras.nova.data.local.entity.DownloadEntity
import com.nibras.nova.domain.model.DownloadItem
import com.nibras.nova.domain.model.DownloadStatus
import com.nibras.nova.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val dispatchers: DispatcherProvider
) : DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    override fun observeDownloads(): Flow<List<DownloadItem>> =
        downloadDao.observeAllDownloads().map { entities -> entities.map { it.toDomain() } }

    override suspend fun enqueueDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        cookies: String?
    ): Result<Long> = withContext(dispatchers.io) {
        try {
            val cleanUrl = url.trim()
            if (!cleanUrl.startsWith("https://", ignoreCase = true) &&
                !cleanUrl.startsWith("http://", ignoreCase = true)
            ) {
                return@withContext Result.failure(IllegalArgumentException("Only HTTP and HTTPS downloads are supported."))
            }

            val guessedFileName = URLUtil.guessFileName(cleanUrl, contentDisposition, mimeType)
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                .ifBlank { "nova_download_${System.currentTimeMillis()}" }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads").apply { mkdirs() }
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val targetFile = resolveUniqueFile(downloadsDir, guessedFileName)
            val resolvedMime = mimeType ?: "application/octet-stream"

            // Probe server headers first to determine truthfully if HTTP byte-range Pause/Resume is supported
            val probe = probeServerRangeSupport(cleanUrl, userAgent, cookies)
            val totalSize = if (probe.contentLength > 0) probe.contentLength else contentLength

            val downloadId = downloadDao.insertDownload(
                DownloadEntity(
                    systemDownloadId = null,
                    fileName = targetFile.name,
                    url = cleanUrl,
                    mimeType = resolvedMime,
                    totalBytes = totalSize,
                    downloadedBytes = 0L,
                    status = DownloadStatus.QUEUED.name,
                    localFilePath = targetFile.absolutePath,
                    supportsPauseResume = probe.acceptsByteRanges,
                    eTag = probe.eTag
                )
            )

            startOrResumeHttpTransfer(
                downloadId = downloadId,
                url = cleanUrl,
                targetFile = targetFile,
                userAgent = userAgent,
                cookies = cookies
            )

            Result.success(downloadId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pauseDownload(downloadId: Long): Result<Unit> = withContext(dispatchers.io) {
        val entity = downloadDao.getDownloadById(downloadId)
            ?: return@withContext Result.failure(IllegalStateException("Download not found"))

        if (!entity.supportsPauseResume) {
            return@withContext Result.failure(
                UnsupportedOperationException("The remote server does not advertise Accept-Ranges: bytes; pause/resume is not supported for this file.")
            )
        }

        activeJobs.remove(downloadId)?.cancel()
        downloadDao.updateDownload(entity.copy(status = DownloadStatus.PAUSED.name))
        Result.success(Unit)
    }

    override suspend fun resumeDownload(downloadId: Long): Result<Unit> = withContext(dispatchers.io) {
        val entity = downloadDao.getDownloadById(downloadId)
            ?: return@withContext Result.failure(IllegalStateException("Download not found"))

        if (!entity.supportsPauseResume) {
            return@withContext Result.failure(
                UnsupportedOperationException("Pause/Resume is not supported by this server.")
            )
        }
        val path = entity.localFilePath ?: return@withContext Result.failure(IllegalStateException("Missing file path"))
        startOrResumeHttpTransfer(
            downloadId = downloadId,
            url = entity.url,
            targetFile = File(path),
            userAgent = "NibrasNova/1.0",
            cookies = null
        )
        Result.success(Unit)
    }

    override suspend fun cancelDownload(downloadId: Long) = withContext(dispatchers.io) {
        activeJobs.remove(downloadId)?.cancel()
        val entity = downloadDao.getDownloadById(downloadId) ?: return@withContext
        entity.localFilePath?.let { File(it).delete() }
        downloadDao.updateDownload(entity.copy(status = DownloadStatus.CANCELLED.name))
    }

    override suspend fun deleteDownloadRecord(downloadId: Long, deleteFileOnDisk: Boolean) = withContext(dispatchers.io) {
        activeJobs.remove(downloadId)?.cancel()
        if (deleteFileOnDisk) {
            downloadDao.getDownloadById(downloadId)?.localFilePath?.let { File(it).delete() }
        }
        downloadDao.deleteDownload(downloadId)
    }

    private fun startOrResumeHttpTransfer(
        downloadId: Long,
        url: String,
        targetFile: File,
        userAgent: String,
        cookies: String?
    ) {
        activeJobs.remove(downloadId)?.cancel()
        val job = scope.launch {
            var connection: HttpURLConnection? = null
            try {
                val existingBytes = if (targetFile.exists()) targetFile.length() else 0L
                val currentEntity = downloadDao.getDownloadById(downloadId) ?: return@launch

                downloadDao.updateProgress(
                    id = downloadId,
                    downloadedBytes = existingBytes,
                    totalBytes = currentEntity.totalBytes,
                    status = DownloadStatus.DOWNLOADING.name
                )

                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", userAgent)
                    if (!cookies.isNullOrBlank()) {
                        setRequestProperty("Cookie", cookies)
                    }
                    if (existingBytes > 0L && currentEntity.supportsPauseResume) {
                        setRequestProperty("Range", "bytes=$existingBytes-")
                        currentEntity.eTag?.let { setRequestProperty("If-Range", it) }
                    }
                }

                connection.connect()
                val responseCode = connection.responseCode
                val isPartial = responseCode == HttpURLConnection.HTTP_PARTIAL
                val startOffset = if (isPartial) existingBytes else 0L
                val contentLen = connection.contentLengthLong
                val resolvedTotal = when {
                    isPartial && contentLen > 0 -> existingBytes + contentLen
                    contentLen > 0 -> contentLen
                    else -> currentEntity.totalBytes
                }

                if (responseCode !in 200..299) {
                    downloadDao.updateDownload(
                        currentEntity.copy(
                            status = DownloadStatus.FAILED.name,
                            errorReason = "HTTP $responseCode"
                        )
                    )
                    return@launch
                }

                RandomAccessFile(targetFile, "rw").use { raf ->
                    raf.seek(startOffset)
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(16 * 1024)
                        var totalRead = startOffset
                        var lastEmitTime = System.currentTimeMillis()

                        while (isActive) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break
                            raf.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 250L) {
                                lastEmitTime = now
                                downloadDao.updateProgress(
                                    id = downloadId,
                                    downloadedBytes = totalRead,
                                    totalBytes = resolvedTotal,
                                    status = DownloadStatus.DOWNLOADING.name
                                )
                            }
                        }

                        if (isActive) {
                            downloadDao.updateProgress(
                                id = downloadId,
                                downloadedBytes = totalRead,
                                totalBytes = if (resolvedTotal > 0) resolvedTotal else totalRead,
                                status = DownloadStatus.COMPLETED.name
                            )
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                downloadDao.getDownloadById(downloadId)?.let { entity ->
                    downloadDao.updateDownload(
                        entity.copy(
                            status = DownloadStatus.FAILED.name,
                            errorReason = e.localizedMessage ?: "Network transfer interrupted"
                        )
                    )
                }
            } finally {
                connection?.disconnect()
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    private data class RangeProbeResult(
        val acceptsByteRanges: Boolean,
        val contentLength: Long,
        val eTag: String?
    )

    private fun probeServerRangeSupport(url: String, userAgent: String, cookies: String?): RangeProbeResult {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                if (!cookies.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookies)
                }
            }
            conn.connect()
            val acceptRanges = conn.getHeaderField("Accept-Ranges")
            val supportsRanges = acceptRanges?.equals("bytes", ignoreCase = true) == true
            val length = conn.contentLengthLong
            val eTag = conn.getHeaderField("ETag")
            RangeProbeResult(
                acceptsByteRanges = supportsRanges,
                contentLength = length,
                eTag = eTag
            )
        } catch (_: Exception) {
            RangeProbeResult(acceptsByteRanges = false, contentLength = -1L, eTag = null)
        } finally {
            conn?.disconnect()
        }
    }

    private fun resolveUniqueFile(directory: File, desiredName: String): File {
        var candidate = File(directory, desiredName)
        if (!candidate.exists()) return candidate
        val dotIdx = desiredName.lastIndexOf('.')
        val base = if (dotIdx > 0) desiredName.substring(0, dotIdx) else desiredName
        val ext = if (dotIdx > 0) desiredName.substring(dotIdx) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(directory, "${base}_($counter)$ext")
            counter++
        }
        return candidate
    }
}
