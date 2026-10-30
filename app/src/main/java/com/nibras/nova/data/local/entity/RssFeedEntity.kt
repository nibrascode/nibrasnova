package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rss_feeds",
    indices = [Index(value = ["feedUrl"], unique = true)]
)
data class RssFeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val feedUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)
