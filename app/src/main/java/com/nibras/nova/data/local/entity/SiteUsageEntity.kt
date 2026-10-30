package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site_usage_stats",
    indices = [Index(value = ["domain"], unique = true)]
)
data class SiteUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val domain: String,
    val totalSecondsSpent: Long = 0L,
    val dailyLimitMinutes: Int = 0, // 0 means no limit set
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isDailyLimitExceeded: Boolean
        get() = dailyLimitMinutes > 0 && (totalSecondsSpent / 60L) >= dailyLimitMinutes
}
