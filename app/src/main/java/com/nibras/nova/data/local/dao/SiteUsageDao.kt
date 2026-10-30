package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.SiteUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteUsageDao {
    @Query("SELECT * FROM site_usage_stats ORDER BY totalSecondsSpent DESC")
    fun observeAllSiteUsage(): Flow<List<SiteUsageEntity>>

    @Query("SELECT * FROM site_usage_stats WHERE domain = :domain LIMIT 1")
    suspend fun getUsageForDomain(domain: String): SiteUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSiteUsage(entity: SiteUsageEntity): Long

    @Query("DELETE FROM site_usage_stats")
    suspend fun resetAllSiteUsage()
}
