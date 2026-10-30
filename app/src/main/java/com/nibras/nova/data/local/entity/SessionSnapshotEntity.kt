package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_snapshots")
data class SessionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val serializedUrlsJson: String,
    val tabCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
