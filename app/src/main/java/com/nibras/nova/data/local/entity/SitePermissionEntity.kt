package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.PermissionDecision
import com.nibras.nova.domain.model.SitePermission
import com.nibras.nova.domain.model.SitePermissionType

@Entity(
    tableName = "site_permissions",
    indices = [Index(value = ["origin", "permissionType"], unique = true)]
)
data class SitePermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val origin: String,
    val permissionType: String,
    val decision: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = SitePermission(
        id = id,
        origin = origin,
        permissionType = runCatching { SitePermissionType.valueOf(permissionType) }.getOrDefault(SitePermissionType.LOCATION),
        decision = runCatching { PermissionDecision.valueOf(decision) }.getOrDefault(PermissionDecision.DENY),
        updatedAt = updatedAt
    )
}
