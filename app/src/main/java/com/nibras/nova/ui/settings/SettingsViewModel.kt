package com.nibras.nova.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewCompat
import com.nibras.nova.core.util.LanSyncEndpoint
import com.nibras.nova.core.util.LocalLanSyncServer
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.ExternalLinkPolicy
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.domain.model.ThemeMode
import com.nibras.nova.domain.model.UserAgentProfile
import com.nibras.nova.domain.repository.BookmarkRepository
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val tabRepository: TabRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val localLanSyncServer: LocalLanSyncServer
) : ViewModel() {

    val settings: StateFlow<BrowserSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserSettings())

    private val _activeLanSyncEndpoint = MutableStateFlow<LanSyncEndpoint?>(null)
    val activeLanSyncEndpoint: StateFlow<LanSyncEndpoint?> = _activeLanSyncEndpoint.asStateFlow()

    val systemWebViewVersion: String by lazy {
        val pkg = WebViewCompat.getCurrentWebViewPackage(context)
        if (pkg != null) "${pkg.packageName} (${pkg.versionName})" else "Android System WebView"
    }

    fun startLanSyncServer() {
        viewModelScope.launch {
            val tabs = tabRepository.observeNormalTabs().firstOrNull().orEmpty()
            val bookmarks = bookmarkRepository.observeAllBookmarks().firstOrNull().orEmpty()
            localLanSyncServer.startSharingSession(tabs, bookmarks)
                .onSuccess { endpoint ->
                    _activeLanSyncEndpoint.value = endpoint
                }
        }
    }

    fun stopLanSyncServer() {
        localLanSyncServer.stopSharingSession()
        _activeLanSyncEndpoint.value = null
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.updateThemeMode(mode) }
    }

    fun updateHighContrastAccent(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateHighContrastAccent(enabled) }
    }

    fun updateWallpaperPreset(presetId: String, customUri: String? = null) {
        viewModelScope.launch { settingsRepository.updateWallpaper(presetId, customUri) }
    }

    fun updateSearchEngine(engine: SearchEngineType, customTemplate: String?) {
        viewModelScope.launch { settingsRepository.updateSearchEngine(engine, customTemplate) }
    }

    fun updateLocalSearchSuggestions(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateLocalSearchSuggestions(enabled) }
    }

    fun updateCustomHomepageUrl(url: String) {
        viewModelScope.launch { settingsRepository.updateCustomHomepageUrl(url) }
    }

    fun updateHomepageVisibility(showShortcuts: Boolean, showRecentHistory: Boolean) {
        viewModelScope.launch { settingsRepository.updateHomepageVisibility(showShortcuts, showRecentHistory) }
    }

    fun updateRestoreTabsOnStartup(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateRestoreTabsOnStartup(enabled) }
    }

    fun updateDefaultDesktopMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDefaultDesktopMode(enabled) }
    }

    fun updateExternalLinkPolicy(policy: ExternalLinkPolicy) {
        viewModelScope.launch { settingsRepository.updateExternalLinkPolicy(policy) }
    }

    fun updateBottomAddressBarReachability(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateBottomAddressBarReachability(enabled) }
    }

    fun updateEnableDesktopTabStrip(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableDesktopTabStrip(enabled) }
    }

    fun updateAllowBackgroundAudioPlayback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAllowBackgroundAudioPlayback(enabled) }
    }

    fun updateUserAgentProfile(profile: UserAgentProfile) {
        viewModelScope.launch { settingsRepository.updateUserAgentProfile(profile) }
    }

    fun updateDataSaverMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableDataSaverMode(enabled) }
    }

    fun updateAutoTabHibernation(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableAutoTabHibernation(enabled) }
    }
}
