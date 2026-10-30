package com.nibras.nova.data.repository

import com.nibras.nova.core.security.DnsOverHttpsProvider
import com.nibras.nova.data.datastore.UserPreferencesDataSource
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.ExternalLinkPolicy
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.domain.model.ThemeMode
import com.nibras.nova.domain.model.UserAgentProfile
import com.nibras.nova.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataSource: UserPreferencesDataSource
) : SettingsRepository {

    override fun observeSettings(): Flow<BrowserSettings> = preferencesDataSource.settingsFlow
    override suspend fun completeOnboarding() = preferencesDataSource.completeOnboarding()
    override suspend fun updateThemeMode(mode: ThemeMode) = preferencesDataSource.setThemeMode(mode)
    override suspend fun updateHighContrastAccent(enabled: Boolean) = preferencesDataSource.setHighContrastAccent(enabled)
    override suspend fun updateWallpaper(presetId: String, customUri: String?) = preferencesDataSource.setWallpaperConfig(presetId, customUri)
    override suspend fun updateSearchEngine(engine: SearchEngineType, customTemplate: String?) = preferencesDataSource.setSearchEngine(engine, customTemplate)
    override suspend fun updateLocalSearchSuggestions(enabled: Boolean) = preferencesDataSource.setLocalSearchSuggestions(enabled)
    override suspend fun updateCustomHomepageUrl(url: String) = preferencesDataSource.setCustomHomepageUrl(url)
    override suspend fun updateHomepageVisibility(showShortcuts: Boolean, showRecentHistory: Boolean) =
        preferencesDataSource.setHomepageModulesVisibility(showShortcuts, showRecentHistory)
    override suspend fun updateRestoreTabsOnStartup(enabled: Boolean) = preferencesDataSource.setRestoreTabsOnStartup(enabled)
    override suspend fun updateDefaultDesktopMode(enabled: Boolean) = preferencesDataSource.setDefaultDesktopMode(enabled)
    override suspend fun updateJavaScriptEnabled(enabled: Boolean) = preferencesDataSource.setJavaScriptEnabled(enabled)
    override suspend fun updateDomStorageEnabled(enabled: Boolean) = preferencesDataSource.setDomStorageEnabled(enabled)
    override suspend fun updateHttpsOnlyMode(enabled: Boolean) = preferencesDataSource.setHttpsOnlyMode(enabled)
    override suspend fun updateSafeBrowsingEnabled(enabled: Boolean) = preferencesDataSource.setSafeBrowsingEnabled(enabled)
    override suspend fun updateBlockThirdPartyCookies(enabled: Boolean) = preferencesDataSource.setBlockThirdPartyCookies(enabled)
    override suspend fun updateSendDoNotTrack(enabled: Boolean) = preferencesDataSource.setSendDoNotTrack(enabled)
    override suspend fun updateBlockScreenshotsInIncognito(enabled: Boolean) = preferencesDataSource.setBlockScreenshotsInIncognito(enabled)
    override suspend fun updateExternalLinkPolicy(policy: ExternalLinkPolicy) = preferencesDataSource.setExternalLinkPolicy(policy)
    override suspend fun updateAdAndTrackerBlocker(enabled: Boolean) = preferencesDataSource.setAdAndTrackerBlocker(enabled)
    override suspend fun updateStripTrackingParameters(enabled: Boolean) = preferencesDataSource.setStripTrackingParameters(enabled)
    override suspend fun updateBiometricLockIncognito(enabled: Boolean) = preferencesDataSource.setBiometricLockIncognito(enabled)
    override suspend fun updateBottomAddressBarReachability(enabled: Boolean) = preferencesDataSource.setBottomAddressBarReachability(enabled)
    override suspend fun updateEnableDesktopTabStrip(enabled: Boolean) = preferencesDataSource.setEnableDesktopTabStrip(enabled)
    override suspend fun updateAllowBackgroundAudioPlayback(enabled: Boolean) = preferencesDataSource.setAllowBackgroundAudioPlayback(enabled)
    override suspend fun updateEnableAntiFingerprinting(enabled: Boolean) = preferencesDataSource.setEnableAntiFingerprinting(enabled)
    override suspend fun updateDnsOverHttpsProvider(provider: DnsOverHttpsProvider) = preferencesDataSource.setDnsOverHttpsProvider(provider)
    override suspend fun updateCustomProxyHostPort(hostPort: String) = preferencesDataSource.setCustomProxyHostPort(hostPort)
    override suspend fun updateUserAgentProfile(profile: UserAgentProfile) = preferencesDataSource.setUserAgentProfile(profile)
    override suspend fun updateEnableDataSaverMode(enabled: Boolean) = preferencesDataSource.setEnableDataSaverMode(enabled)
    override suspend fun updateEnableRedirectChainGuard(enabled: Boolean) = preferencesDataSource.setEnableRedirectChainGuard(enabled)
    override suspend fun updateEnableAutoTabHibernation(enabled: Boolean) = preferencesDataSource.setEnableAutoTabHibernation(enabled)
}
