package com.nibras.nova.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.model.TabWorkspace

@Entity(tableName = "normal_tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val isDesktopMode: Boolean,
    val isActive: Boolean,
    val workspaceId: String = "general",
    val parentTabId: String?,
    val thumbnailPath: String?,
    val faviconPath: String?,
    val createdAt: Long,
    val lastAccessedAt: Long
) {
    fun toDomain() = TabItem(
        id = id,
        url = url,
        title = title,
        isPrivate = false,
        isDesktopMode = isDesktopMode,
        isActive = isActive,
        workspace = TabWorkspace.fromId(workspaceId),
        parentTabId = parentTabId,
        thumbnailPath = thumbnailPath,
        faviconPath = faviconPath,
        createdAt = createdAt,
        lastAccessedAt = lastAccessedAt
    )

    companion object {
        fun fromDomain(tab: TabItem) = TabEntity(
            id = tab.id,
            url = tab.url,
            title = tab.title,
            isDesktopMode = tab.isDesktopMode,
            isActive = tab.isActive,
            workspaceId = tab.workspace.id,
            parentTabId = tab.parentTabId,
            thumbnailPath = tab.thumbnailPath,
            faviconPath = tab.faviconPath,
            createdAt = tab.createdAt,
            lastAccessedAt = tab.lastAccessedAt
        )
    }
}
