package com.nibras.nova.domain.usecase

import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.model.TabWorkspace
import com.nibras.nova.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageTabsUseCase @Inject constructor(
    private val tabRepository: TabRepository
) {
    fun observeNormalTabs(): Flow<List<TabItem>> = tabRepository.observeNormalTabs()
    fun observePrivateTabs(): Flow<List<TabItem>> = tabRepository.observePrivateTabs()
    fun observeActiveTab(): Flow<TabItem?> = tabRepository.observeActiveTab()
    fun observeRecentlyClosedCount(): Flow<Int> = tabRepository.observeRecentlyClosedCount()

    suspend fun openNewTab(
        url: String,
        isPrivate: Boolean,
        isDesktopMode: Boolean = false,
        workspace: TabWorkspace = TabWorkspace.GENERAL
    ): TabItem {
        return tabRepository.createTab(
            url = url,
            isPrivate = isPrivate,
            isDesktopMode = isDesktopMode,
            selectImmediately = true,
            workspace = workspace
        )
    }

    suspend fun assignTabWorkspace(tabId: String, workspace: TabWorkspace) {
        tabRepository.updateTabMetadata(tabId = tabId, workspace = workspace)
    }

    suspend fun switchTab(tabId: String) = tabRepository.switchToTab(tabId)
    suspend fun closeTab(tabId: String) = tabRepository.closeTab(tabId)
    suspend fun duplicateTab(tabId: String): TabItem? = tabRepository.duplicateTab(tabId)
    suspend fun reopenLastClosedTab(): TabItem? = tabRepository.reopenLastClosedTab()
    suspend fun closeAllTabs(isPrivateMode: Boolean) = tabRepository.closeAllTabs(isPrivateMode)
}
