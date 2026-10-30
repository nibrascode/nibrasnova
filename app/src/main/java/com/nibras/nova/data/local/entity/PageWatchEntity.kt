package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page_watch_monitors",
    indices = [Index(value = ["url"], unique = true)]
)
data class PageWatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val lastContentHash: String,
    val lastSnippetPreview: String,
    val hasChangedSinceLastCheck: Boolean = false,
    val lastCheckedAt: Long = System.currentTimeMillis()
)
