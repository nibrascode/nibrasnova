package com.nibras.nova.ui.downloads

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.security.FileChecksums
import com.nibras.nova.core.security.FileHashVerifier
import com.nibras.nova.domain.model.DownloadItem
import com.nibras.nova.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val fileHashVerifier: FileHashVerifier
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = downloadRepository.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _verifiedChecksums = MutableStateFlow<Pair<String, FileChecksums>?>(null)
    val verifiedChecksums = _verifiedChecksums.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    fun verifyFileIntegrity(item: DownloadItem) {
        val path = item.localFilePath ?: return
        viewModelScope.launch {
            fileHashVerifier.computeFileChecksums(path)
                .onSuccess { sums ->
                    _verifiedChecksums.value = item.fileName to sums
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not compute file hash.")
                }
        }
    }

    fun dismissChecksumDialog() {
        _verifiedChecksums.value = null
    }

    fun pauseDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.pauseDownload(id).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Pause not supported for this server")
            }
        }
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.resumeDownload(id).onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Resume not supported")
            }
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(id)
        }
    }

    fun deleteDownload(id: Long, deleteFileFromStorage: Boolean) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadRecord(id, deleteFileFromStorage)
        }
    }

    fun openDownloadedFile(context: Context, item: DownloadItem) {
        val path = item.localFilePath ?: return
        val file = File(path)
        if (!file.exists()) {
            _events.tryEmit("File no longer exists on storage.")
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open ${item.fileName}"))
        }.onFailure {
            _events.tryEmit("No installed app can open ${item.fileName}")
        }
    }

    fun shareDownloadedFile(context: Context, item: DownloadItem) {
        val path = item.localFilePath ?: return
        val file = File(path)
        if (!file.exists()) {
            _events.tryEmit("File not found on disk.")
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${item.fileName}"))
        }
    }
}
