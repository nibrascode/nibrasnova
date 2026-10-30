package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site_zoom_settings",
    indices = [Index(value = ["domain"], unique = true)]
)
data class SiteZoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val domain: String,
    val textZoomPercent: Int = 100
)
