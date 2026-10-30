package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.SiteZoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteZoomDao {
    @Query("SELECT * FROM site_zoom_settings ORDER BY domain ASC")
    fun observeAllSiteZooms(): Flow<List<SiteZoomEntity>>

    @Query("SELECT * FROM site_zoom_settings WHERE domain = :domain LIMIT 1")
    suspend fun getZoomForDomain(domain: String): SiteZoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSiteZoom(entity: SiteZoomEntity): Long

    @Query("DELETE FROM site_zoom_settings WHERE id = :id")
    suspend fun deleteSiteZoom(id: Long)
}
