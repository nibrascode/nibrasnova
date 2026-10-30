package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cookie_whitelist",
    indices = [Index(value = ["domain"], unique = true)]
)
data class CookieWhitelistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val domain: String,
    val addedAt: Long = System.currentTimeMillis()
)
