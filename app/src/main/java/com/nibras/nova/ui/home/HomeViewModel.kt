package com.nibras.nova.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.HistoryItem
import com.nibras.nova.domain.model.PrivacyMetrics
import com.nibras.nova.domain.model.QuickShortcut
import com.nibras.nova.domain.model.SearchEngineConfig
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.repository.HistoryRepository
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.repository.ShortcutRepository
import com.nibras.nova.domain.repository.TabRepository
import com.nibras.nova.domain.usecase.ResolveInputToUrlUseCase
import com.nibras.nova.domain.usecase.ResolvedNavigationTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val settings: BrowserSettings = BrowserSettings(),
    val shortcuts: List<QuickShortcut> = emptyList(),
    val recentHistory: List<HistoryItem> = emptyList(),
    val normalTabCount: Int = 1,
    val privateTabCount: Int = 0,
    val activeTab: TabItem? = null,
    val privacyMetrics: PrivacyMetrics = PrivacyMetrics()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val shortcutRepository: ShortcutRepository,
    private val historyRepository: HistoryRepository,
    private val tabRepository: TabRepository,
    private val privacyRepository: PrivacyRepository,
    private val resolveInputToUrlUseCase: ResolveInputToUrlUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.observeSettings(),
        shortcutRepository.observeShortcuts(),
        historyRepository.observeRecentHistory(limit = 5),
        tabRepository.observeNormalTabs(),
        tabRepository.observePrivateTabs(),
        tabRepository.observeActiveTab(),
        privacyRepository.observePrivacyMetrics()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val settings = values[0] as BrowserSettings
        @Suppress("UNCHECKED_CAST")
        val shortcuts = values[1] as List<QuickShortcut>
        @Suppress("UNCHECKED_CAST")
        val history = values[2] as List<HistoryItem>
        @Suppress("UNCHECKED_CAST")
        val normalTabs = values[3] as List<TabItem>
        @Suppress("UNCHECKED_CAST")
        val privateTabs = values[4] as List<TabItem>
        val activeTab = values[5] as TabItem?
        val metrics = values[6] as PrivacyMetrics

        HomeUiState(
            settings = settings,
            shortcuts = shortcuts,
            recentHistory = history,
            normalTabCount = normalTabs.size.coerceAtLeast(1),
            privateTabCount = privateTabs.size,
            activeTab = activeTab,
            privacyMetrics = metrics
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            shortcutRepository.ensureDefaultShortcutsSeeded()
        }
    }

    fun submitSearchOrUrl(
        rawQuery: String,
        openInPrivate: Boolean,
        onNavigateToBrowser: () -> Unit
    ) {
        val currentSettings = uiState.value.settings
        val searchConfig = SearchEngineConfig(
            selectedEngine = currentSettings.searchEngine,
            customSearchTemplate = currentSettings.customSearchUrlTemplate,
            enableLocalSuggestions = currentSettings.enableLocalSearchSuggestions
        )
        val resolved = resolveInputToUrlUseCase(
            rawInput = rawQuery,
            searchConfig = searchConfig,
            httpsOnlyMode = currentSettings.httpsOnlyMode
        )
        if (resolved is ResolvedNavigationTarget.LoadWebUrl) {
            viewModelScope.launch {
                val active = uiState.value.activeTab
                if (active != null && active.isPrivate == openInPrivate && active.isHomePage) {
                    tabRepository.updateTabMetadata(
                        tabId = active.id,
                        url = resolved.url,
                        title = resolved.url
                    )
                } else {
                    tabRepository.createTab(
                        url = resolved.url,
                        isPrivate = openInPrivate,
                        isDesktopMode = currentSettings.defaultDesktopMode,
                        selectImmediately = true
                    )
                }
                onNavigateToBrowser()
            }
        }
    }

    fun selectSearchEngine(engine: SearchEngineType) {
        viewModelScope.launch {
            settingsRepository.updateSearchEngine(engine, null)
        }
    }

    fun addShortcut(title: String, url: String) {
        viewModelScope.launch {
            shortcutRepository.addShortcut(title = title, url = url)
        }
    }

    fun updateShortcut(shortcut: QuickShortcut, newTitle: String, newUrl: String) {
        viewModelScope.launch {
            shortcutRepository.updateShortcut(
                shortcut.copy(title = newTitle, url = newUrl)
            )
        }
    }

    fun deleteShortcut(shortcutId: Long) {
        viewModelScope.launch {
            shortcutRepository.deleteShortcut(shortcutId)
        }
    }

    fun deleteRecentHistoryItem(historyId: Long) {
        viewModelScope.launch {
            historyRepository.deleteHistoryItem(historyId)
        }
    }

    fun startNewIncognitoSession(onNavigateToBrowser: () -> Unit) {
        viewModelScope.launch {
            tabRepository.createTab(
                url = "https://duckduckgo.com",
                isPrivate = true,
                isDesktopMode = uiState.value.settings.defaultDesktopMode,
                selectImmediately = true
            )
            onNavigateToBrowser()
        }
    }
}
