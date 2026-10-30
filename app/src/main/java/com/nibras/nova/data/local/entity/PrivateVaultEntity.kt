package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "private_vault_items")
data class PrivateVaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val urlOrNote: String,
    val category: String = "BOOKMARK", // BOOKMARK, SECRET_NOTE, OFFLINE_ARCHIVE
    val createdAt: Long = System.currentTimeMillis()
)
