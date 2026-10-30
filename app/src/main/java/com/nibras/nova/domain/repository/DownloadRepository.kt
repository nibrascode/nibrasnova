package com.nibras.nova.domain.repository

import com.nibras.nova.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadItem>>
    suspend fun enqueueDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        cookies: String?
    ): Result<Long>
    suspend fun pauseDownload(downloadId: Long): Result<Unit>
    suspend fun resumeDownload(downloadId: Long): Result<Unit>
    suspend fun cancelDownload(downloadId: Long)
    suspend fun deleteDownloadRecord(downloadId: Long, deleteFileOnDisk: Boolean)
}
