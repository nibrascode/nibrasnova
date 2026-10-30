package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.DownloadItem
import com.nibras.nova.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val systemDownloadId: Long? = null,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String,
    val localFilePath: String?,
    val supportsPauseResume: Boolean = false,
    val eTag: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val errorReason: String? = null
) {
    fun toDomain() = DownloadItem(
        id = id,
        systemDownloadId = systemDownloadId,
        fileName = fileName,
        url = url,
        mimeType = mimeType,
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED),
        localFilePath = localFilePath,
        supportsPauseResume = supportsPauseResume,
        eTag = eTag,
        createdAt = createdAt,
        errorReason = errorReason
    )
}
