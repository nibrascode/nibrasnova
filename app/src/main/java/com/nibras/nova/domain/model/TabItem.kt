package com.nibras.nova.domain.model

import com.nibras.nova.core.common.NovaConstants

enum class TabWorkspace(val id: String, val displayName: String) {
    GENERAL("general", "General"),
    WORK("work", "Work"),
    RESEARCH("research", "Research"),
    PERSONAL("personal", "Personal");

    companion object {
        fun fromId(id: String): TabWorkspace =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GENERAL
    }
}

data class TabItem(
    val id: String,
    val url: String = NovaConstants.DEFAULT_HOMEPAGE_SENTINEL,
    val title: String = "New Tab",
    val isPrivate: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isActive: Boolean = false,
    val workspace: TabWorkspace = TabWorkspace.GENERAL,
    val parentTabId: String? = null,
    val thumbnailPath: String? = null,
    val faviconPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
) {
    val isHomePage: Boolean
        get() = url.isBlank() || url.equals(NovaConstants.DEFAULT_HOMEPAGE_SENTINEL, ignoreCase = true)
}
