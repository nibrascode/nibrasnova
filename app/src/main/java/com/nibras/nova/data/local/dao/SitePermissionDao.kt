package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.SitePermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SitePermissionDao {
    @Query("SELECT * FROM site_permissions ORDER BY origin ASC, permissionType ASC")
    fun observeAllSitePermissions(): Flow<List<SitePermissionEntity>>

    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permissionType = :permissionType LIMIT 1")
    suspend fun getPermissionForOrigin(origin: String, permissionType: String): SitePermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSitePermission(permission: SitePermissionEntity): Long

    @Query("DELETE FROM site_permissions WHERE id = :id")
    suspend fun deleteSitePermission(id: Long)

    @Query("DELETE FROM site_permissions")
    suspend fun clearAllSitePermissions()
}
