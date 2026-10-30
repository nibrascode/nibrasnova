package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "web_annotations",
    indices = [Index(value = ["pageUrl"])]
)
data class WebAnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pageUrl: String,
    val pageTitle: String,
    val highlightedQuote: String,
    val userNote: String,
    val highlightColorHex: String = "#00E5FF",
    val createdAt: Long = System.currentTimeMillis()
)
