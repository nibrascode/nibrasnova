package com.nibras.nova.domain.repository

import com.nibras.nova.core.security.DnsOverHttpsProvider
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.ExternalLinkPolicy
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.domain.model.ThemeMode
import com.nibras.nova.domain.model.UserAgentProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<BrowserSettings>
    suspend fun completeOnboarding()
    suspend fun updateThemeMode(mode: ThemeMode)
    suspend fun updateHighContrastAccent(enabled: Boolean)
    suspend fun updateWallpaper(presetId: String, customUri: String?)
    suspend fun updateSearchEngine(engine: SearchEngineType, customTemplate: String?)
    suspend fun updateLocalSearchSuggestions(enabled: Boolean)
    suspend fun updateCustomHomepageUrl(url: String)
    suspend fun updateHomepageVisibility(showShortcuts: Boolean, showRecentHistory: Boolean)
    suspend fun updateRestoreTabsOnStartup(enabled: Boolean)
    suspend fun updateDefaultDesktopMode(enabled: Boolean)
    suspend fun updateJavaScriptEnabled(enabled: Boolean)
    suspend fun updateDomStorageEnabled(enabled: Boolean)
    suspend fun updateHttpsOnlyMode(enabled: Boolean)
    suspend fun updateSafeBrowsingEnabled(enabled: Boolean)
    suspend fun updateBlockThirdPartyCookies(enabled: Boolean)
    suspend fun updateSendDoNotTrack(enabled: Boolean)
    suspend fun updateBlockScreenshotsInIncognito(enabled: Boolean)
    suspend fun updateExternalLinkPolicy(policy: ExternalLinkPolicy)
    suspend fun updateAdAndTrackerBlocker(enabled: Boolean)
    suspend fun updateStripTrackingParameters(enabled: Boolean)
    suspend fun updateBiometricLockIncognito(enabled: Boolean)
    suspend fun updateBottomAddressBarReachability(enabled: Boolean)
    suspend fun updateEnableDesktopTabStrip(enabled: Boolean)
    suspend fun updateAllowBackgroundAudioPlayback(enabled: Boolean)
    // Stage 11 & 12
    suspend fun updateEnableAntiFingerprinting(enabled: Boolean)
    suspend fun updateDnsOverHttpsProvider(provider: DnsOverHttpsProvider)
    suspend fun updateCustomProxyHostPort(hostPort: String)
    suspend fun updateUserAgentProfile(profile: UserAgentProfile)
    suspend fun updateEnableDataSaverMode(enabled: Boolean)
    suspend fun updateEnableRedirectChainGuard(enabled: Boolean)
    suspend fun updateEnableAutoTabHibernation(enabled: Boolean)
}
