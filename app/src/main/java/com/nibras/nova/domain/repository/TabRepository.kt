package com.nibras.nova.domain.repository

import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.model.TabWorkspace
import kotlinx.coroutines.flow.Flow

interface TabRepository {
    fun observeNormalTabs(): Flow<List<TabItem>>
    fun observePrivateTabs(): Flow<List<TabItem>>
    fun observeActiveTab(): Flow<TabItem?>
    fun observeRecentlyClosedCount(): Flow<Int>
    suspend fun initializeOrRestoreTabs(restoreOnStartup: Boolean, defaultDesktopMode: Boolean)
    suspend fun createTab(
        url: String,
        isPrivate: Boolean = false,
        isDesktopMode: Boolean = false,
        selectImmediately: Boolean = true,
        workspace: TabWorkspace = TabWorkspace.GENERAL,
        parentTabId: String? = null
    ): TabItem
    suspend fun switchToTab(tabId: String)
    suspend fun updateTabMetadata(
        tabId: String,
        url: String? = null,
        title: String? = null,
        isDesktopMode: Boolean? = null,
        workspace: TabWorkspace? = null,
        thumbnailPath: String? = null,
        faviconPath: String? = null
    )
    suspend fun duplicateTab(tabId: String): TabItem?
    suspend fun closeTab(tabId: String)
    suspend fun reopenLastClosedTab(): TabItem?
    suspend fun closeAllTabs(isPrivateMode: Boolean)
}
