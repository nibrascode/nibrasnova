package com.nibras.nova.ui.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.security.BiometricLockHelper
import com.nibras.nova.data.local.entity.SessionSnapshotEntity
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.model.TabWorkspace
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.usecase.ManageTabsUseCase
import com.nibras.nova.engine.WebViewPoolManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class TabOverviewUiState(
    val normalTabs: List<TabItem> = emptyList(),
    val privateTabs: List<TabItem> = emptyList(),
    val activeTab: TabItem? = null,
    val selectedWorkspace: TabWorkspace = TabWorkspace.GENERAL,
    val recentlyClosedCount: Int = 0,
    val settings: BrowserSettings = BrowserSettings(),
    val isPrivateVaultUnlocked: Boolean = false,
    val sessionSnapshots: List<SessionSnapshotEntity> = emptyList()
)

@HiltViewModel
class TabOverviewViewModel @Inject constructor(
    val biometricLockHelper: BiometricLockHelper,
    private val manageTabsUseCase: ManageTabsUseCase,
    private val settingsRepository: SettingsRepository,
    private val privacyRepository: PrivacyRepository,
    private val webViewPoolManager: WebViewPoolManager
) : ViewModel() {

    private val _selectedWorkspace = MutableStateFlow(TabWorkspace.GENERAL)
    private val _isPrivateVaultUnlocked = MutableStateFlow(false)

    val uiState: StateFlow<TabOverviewUiState> = combine(
        manageTabsUseCase.observeNormalTabs(),
        manageTabsUseCase.observePrivateTabs(),
        manageTabsUseCase.observeActiveTab(),
        manageTabsUseCase.observeRecentlyClosedCount(),
        settingsRepository.observeSettings(),
        _selectedWorkspace,
        _isPrivateVaultUnlocked,
        privacyRepository.observeSessionSnapshots()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val normal = values[0] as List<TabItem>
        @Suppress("UNCHECKED_CAST")
        val priv = values[1] as List<TabItem>
        val active = values[2] as TabItem?
        val closedCount = values[3] as Int
        val settings = values[4] as BrowserSettings
        val workspace = values[5] as TabWorkspace
        val unlocked = values[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val snapshots = values[7] as List<SessionSnapshotEntity>

        TabOverviewUiState(
            normalTabs = normal,
            privateTabs = priv,
            activeTab = active,
            selectedWorkspace = workspace,
            recentlyClosedCount = closedCount,
            settings = settings,
            isPrivateVaultUnlocked = unlocked || !settings.biometricLockIncognitoTabs,
            sessionSnapshots = snapshots
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TabOverviewUiState())

    fun selectWorkspace(workspace: TabWorkspace) {
        _selectedWorkspace.value = workspace
    }

    fun markPrivateVaultUnlocked() {
        _isPrivateVaultUnlocked.value = true
    }

    fun saveCurrentNormalTabsAsSnapshot(snapshotName: String) {
        val urls = uiState.value.normalTabs.map { it.url }.filter { it.isNotBlank() && it != "nova://home" }
        viewModelScope.launch {
            privacyRepository.saveSessionSnapshot(snapshotName, urls)
        }
    }

    fun restoreSessionSnapshot(snapshot: SessionSnapshotEntity, onRestored: () -> Unit) {
        viewModelScope.launch {
            val urls = runCatching {
                Json.decodeFromString<List<String>>(snapshot.serializedUrlsJson)
            }.getOrDefault(emptyList())
            for (url in urls) {
                manageTabsUseCase.openNewTab(
                    url = url,
                    isPrivate = false,
                    workspace = _selectedWorkspace.value
                )
            }
            onRestored()
        }
    }

    fun deleteSessionSnapshot(id: Long) {
        viewModelScope.launch {
            privacyRepository.deleteSessionSnapshot(id)
        }
    }

    fun selectTab(tab: TabItem, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            manageTabsUseCase.switchTab(tab.id)
            onDone(tab.isHomePage)
        }
    }

    fun createNewTab(isPrivate: Boolean, onCreated: () -> Unit) {
        viewModelScope.launch {
            manageTabsUseCase.openNewTab(
                url = "nova://home",
                isPrivate = isPrivate,
                workspace = _selectedWorkspace.value
            )
            onCreated()
        }
    }

    fun duplicateTab(tabId: String) {
        viewModelScope.launch {
            manageTabsUseCase.duplicateTab(tabId)
        }
    }

    fun closeTab(tab: TabItem) {
        viewModelScope.launch {
            webViewPoolManager.destroyTabWebView(tab.id)
            manageTabsUseCase.closeTab(tab.id)
        }
    }

    fun reopenRecentlyClosedTab(onReopened: (Boolean) -> Unit) {
        viewModelScope.launch {
            val reopened = manageTabsUseCase.reopenLastClosedTab()
            if (reopened != null) {
                onReopened(reopened.isHomePage)
            }
        }
    }

    fun closeAllTabs(isPrivateMode: Boolean) {
        viewModelScope.launch {
            if (isPrivateMode) {
                webViewPoolManager.destroyAllIncognitoWebViews()
            } else {
                webViewPoolManager.destroyAllNormalWebViews()
            }
            manageTabsUseCase.closeAllTabs(isPrivateMode)
        }
    }
}
