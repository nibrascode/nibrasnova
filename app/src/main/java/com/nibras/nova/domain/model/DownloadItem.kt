package com.nibras.nova.domain.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadItem(
    val id: Long = 0L,
    val systemDownloadId: Long? = null,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val localFilePath: String?,
    val supportsPauseResume: Boolean = false,
    val eTag: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val errorReason: String? = null
) {
    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
