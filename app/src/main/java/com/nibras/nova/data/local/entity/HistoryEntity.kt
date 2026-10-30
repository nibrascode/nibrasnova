package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.HistoryItem

@Entity(
    tableName = "browsing_history",
    indices = [
        Index(value = ["visitedAt"]),
        Index(value = ["url"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val faviconPath: String? = null,
    val visitedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = HistoryItem(
        id = id,
        title = title,
        url = url,
        faviconPath = faviconPath,
        visitedAt = visitedAt
    )
}
