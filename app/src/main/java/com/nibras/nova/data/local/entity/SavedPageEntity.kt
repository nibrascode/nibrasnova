package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.SavedOfflinePage

@Entity(tableName = "saved_offline_pages")
data class SavedPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val siteName: String,
    val plainTextContent: String,
    val htmlFilePath: String?,
    val estimatedReadMinutes: Int,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = SavedOfflinePage(
        id = id,
        title = title,
        url = url,
        siteName = siteName,
        plainTextContent = plainTextContent,
        htmlFilePath = htmlFilePath,
        estimatedReadMinutes = estimatedReadMinutes,
        savedAt = savedAt
    )
}
