package com.nibras.nova.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nibras.nova.core.common.NovaConstants
import com.nibras.nova.core.security.DnsOverHttpsProvider
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.ExternalLinkPolicy
import com.nibras.nova.domain.model.PrivacyMetrics
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.domain.model.ThemeMode
import com.nibras.nova.domain.model.UserAgentProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIGH_CONTRAST_ACCENT = booleanPreferencesKey("high_contrast_accent")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val WALLPAPER_PRESET_ID = stringPreferencesKey("wallpaper_preset_id")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val CUSTOM_SEARCH_TEMPLATE = stringPreferencesKey("custom_search_template")
        val LOCAL_SEARCH_SUGGESTIONS = booleanPreferencesKey("local_search_suggestions")
        val CUSTOM_HOMEPAGE_URL = stringPreferencesKey("custom_homepage_url")
        val SHOW_QUICK_SHORTCUTS = booleanPreferencesKey("show_quick_shortcuts")
        val SHOW_RECENT_HISTORY = booleanPreferencesKey("show_recent_history")
        val RESTORE_TABS_ON_STARTUP = booleanPreferencesKey("restore_tabs_on_startup")
        val DEFAULT_DESKTOP_MODE = booleanPreferencesKey("default_desktop_mode")
        val ENABLE_JAVASCRIPT = booleanPreferencesKey("enable_javascript")
        val ENABLE_DOM_STORAGE = booleanPreferencesKey("enable_dom_storage")
        val HTTPS_ONLY_MODE = booleanPreferencesKey("https_only_mode")
        val ENABLE_SAFE_BROWSING = booleanPreferencesKey("enable_safe_browsing")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val SEND_DO_NOT_TRACK = booleanPreferencesKey("send_do_not_track")
        val BLOCK_SCREENSHOTS_INCOGNITO = booleanPreferencesKey("block_screenshots_incognito")
        val EXTERNAL_LINK_POLICY = stringPreferencesKey("external_link_policy")

        // Stage 10 Pro Keys
        val ENABLE_AD_BLOCKER = booleanPreferencesKey("enable_ad_blocker")
        val STRIP_TRACKING_PARAMS = booleanPreferencesKey("strip_tracking_params")
        val BIOMETRIC_LOCK_INCOGNITO = booleanPreferencesKey("biometric_lock_incognito")
        val BOTTOM_ADDRESS_BAR = booleanPreferencesKey("bottom_address_bar")
        val ENABLE_DESKTOP_TAB_STRIP = booleanPreferencesKey("enable_desktop_tab_strip")
        val ALLOW_BACKGROUND_AUDIO = booleanPreferencesKey("allow_background_audio")

        // Stage 11 & 12 Keys
        val ENABLE_ANTI_FINGERPRINTING = booleanPreferencesKey("enable_anti_fingerprinting")
        val DOH_PROVIDER = stringPreferencesKey("doh_provider")
        val CUSTOM_PROXY_HOST_PORT = stringPreferencesKey("custom_proxy_host_port")
        val USER_AGENT_PROFILE = stringPreferencesKey("user_agent_profile")
        val ENABLE_DATA_SAVER = booleanPreferencesKey("enable_data_saver")
        val ENABLE_REDIRECT_GUARD = booleanPreferencesKey("enable_redirect_guard")
        val ENABLE_TAB_HIBERNATION = booleanPreferencesKey("enable_tab_hibernation")

        // Privacy Metrics
        val HTTPS_UPGRADES_COUNT = intPreferencesKey("https_upgrades_count")
        val UNSAFE_SCHEMES_BLOCKED_COUNT = intPreferencesKey("unsafe_schemes_blocked_count")
        val SSL_WARNINGS_COUNT = intPreferencesKey("ssl_warnings_count")
        val DATA_CLEARS_COUNT = intPreferencesKey("data_clears_count")
        val ADS_BLOCKED_COUNT = intPreferencesKey("ads_blocked_count")
        val TRACKING_PARAMS_STRIPPED_COUNT = intPreferencesKey("tracking_params_stripped_count")
        val REDIRECT_CHAINS_BLOCKED_COUNT = intPreferencesKey("redirect_chains_blocked_count")
    }

    val settingsFlow: Flow<BrowserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            BrowserSettings(
                isFirstLaunch = prefs[Keys.IS_FIRST_LAUNCH] ?: true,
                themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.DARK,
                highContrastAccent = prefs[Keys.HIGH_CONTRAST_ACCENT] ?: true,
                customWallpaperUri = prefs[Keys.CUSTOM_WALLPAPER_URI],
                wallpaperPresetId = prefs[Keys.WALLPAPER_PRESET_ID] ?: "nova_deep_navy",
                searchEngine = SearchEngineType.fromId(prefs[Keys.SEARCH_ENGINE] ?: SearchEngineType.DUCKDUCKGO.id),
                customSearchUrlTemplate = prefs[Keys.CUSTOM_SEARCH_TEMPLATE] ?: "https://duckduckgo.com/?q=%s",
                enableLocalSearchSuggestions = prefs[Keys.LOCAL_SEARCH_SUGGESTIONS] ?: true,
                customHomepageUrl = prefs[Keys.CUSTOM_HOMEPAGE_URL] ?: NovaConstants.DEFAULT_HOMEPAGE_SENTINEL,
                showQuickShortcutsOnHome = prefs[Keys.SHOW_QUICK_SHORTCUTS] ?: true,
                showRecentHistoryOnHome = prefs[Keys.SHOW_RECENT_HISTORY] ?: true,
                restoreTabsOnStartup = prefs[Keys.RESTORE_TABS_ON_STARTUP] ?: true,
                defaultDesktopMode = prefs[Keys.DEFAULT_DESKTOP_MODE] ?: false,
                enableJavaScript = prefs[Keys.ENABLE_JAVASCRIPT] ?: true,
                enableDomStorage = prefs[Keys.ENABLE_DOM_STORAGE] ?: true,
                httpsOnlyMode = prefs[Keys.HTTPS_ONLY_MODE] ?: true,
                enableSafeBrowsing = prefs[Keys.ENABLE_SAFE_BROWSING] ?: true,
                blockThirdPartyCookies = prefs[Keys.BLOCK_THIRD_PARTY_COOKIES] ?: true,
                sendDoNotTrackHeader = prefs[Keys.SEND_DO_NOT_TRACK] ?: true,
                blockScreenshotsInIncognito = prefs[Keys.BLOCK_SCREENSHOTS_INCOGNITO] ?: true,
                externalLinkPolicy = prefs[Keys.EXTERNAL_LINK_POLICY]?.let {
                    runCatching { ExternalLinkPolicy.valueOf(it) }.getOrNull()
                } ?: ExternalLinkPolicy.ASK_EVERY_TIME,
                enableAdAndTrackerBlocker = prefs[Keys.ENABLE_AD_BLOCKER] ?: true,
                stripTrackingUrlParameters = prefs[Keys.STRIP_TRACKING_PARAMS] ?: true,
                biometricLockIncognitoTabs = prefs[Keys.BIOMETRIC_LOCK_INCOGNITO] ?: false,
                bottomAddressBarReachability = prefs[Keys.BOTTOM_ADDRESS_BAR] ?: false,
                enableDesktopTabStrip = prefs[Keys.ENABLE_DESKTOP_TAB_STRIP] ?: false,
                allowBackgroundAudioPlayback = prefs[Keys.ALLOW_BACKGROUND_AUDIO] ?: false,
                enableAntiFingerprinting = prefs[Keys.ENABLE_ANTI_FINGERPRINTING] ?: true,
                dnsOverHttpsProvider = DnsOverHttpsProvider.fromId(
                    prefs[Keys.DOH_PROVIDER] ?: DnsOverHttpsProvider.CLOUDFLARE_1111.id
                ),
                customProxyHostPort = prefs[Keys.CUSTOM_PROXY_HOST_PORT] ?: "",
                userAgentProfile = UserAgentProfile.fromId(
                    prefs[Keys.USER_AGENT_PROFILE] ?: UserAgentProfile.ANDROID_DEFAULT.id
                ),
                enableDataSaverMode = prefs[Keys.ENABLE_DATA_SAVER] ?: false,
                enableRedirectChainGuard = prefs[Keys.ENABLE_REDIRECT_GUARD] ?: true,
                enableAutoTabHibernation = prefs[Keys.ENABLE_TAB_HIBERNATION] ?: true
            )
        }

    val privacyMetricsFlow: Flow<PrivacyMetrics> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            PrivacyMetrics(
                httpsUpgradesCount = prefs[Keys.HTTPS_UPGRADES_COUNT] ?: 0,
                unsafeSchemesBlockedCount = prefs[Keys.UNSAFE_SCHEMES_BLOCKED_COUNT] ?: 0,
                sslWarningsInterceptedCount = prefs[Keys.SSL_WARNINGS_COUNT] ?: 0,
                dataClearsExecutedCount = prefs[Keys.DATA_CLEARS_COUNT] ?: 0,
                adsAndTrackersBlockedCount = prefs[Keys.ADS_BLOCKED_COUNT] ?: 0,
                trackingParamsStrippedCount = prefs[Keys.TRACKING_PARAMS_STRIPPED_COUNT] ?: 0,
                redirectChainsBlockedCount = prefs[Keys.REDIRECT_CHAINS_BLOCKED_COUNT] ?: 0
            )
        }

    suspend fun completeOnboarding() { dataStore.edit { it[Keys.IS_FIRST_LAUNCH] = false } }
    suspend fun setThemeMode(mode: ThemeMode) { dataStore.edit { it[Keys.THEME_MODE] = mode.name } }
    suspend fun setHighContrastAccent(enabled: Boolean) { dataStore.edit { it[Keys.HIGH_CONTRAST_ACCENT] = enabled } }
    suspend fun setWallpaperConfig(presetId: String, customUri: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.WALLPAPER_PRESET_ID] = presetId
            if (customUri != null) prefs[Keys.CUSTOM_WALLPAPER_URI] = customUri else prefs.remove(Keys.CUSTOM_WALLPAPER_URI)
        }
    }
    suspend fun setSearchEngine(engine: SearchEngineType, customTemplate: String? = null) {
        dataStore.edit { prefs ->
            prefs[Keys.SEARCH_ENGINE] = engine.id
            if (!customTemplate.isNullOrBlank() && customTemplate.contains("%s")) {
                prefs[Keys.CUSTOM_SEARCH_TEMPLATE] = customTemplate.trim()
            }
        }
    }
    suspend fun setLocalSearchSuggestions(enabled: Boolean) { dataStore.edit { it[Keys.LOCAL_SEARCH_SUGGESTIONS] = enabled } }
    suspend fun setCustomHomepageUrl(url: String) {
        dataStore.edit { it[Keys.CUSTOM_HOMEPAGE_URL] = url.trim().ifBlank { NovaConstants.DEFAULT_HOMEPAGE_SENTINEL } }
    }
    suspend fun setHomepageModulesVisibility(showShortcuts: Boolean, showRecentHistory: Boolean) {
        dataStore.edit {
            it[Keys.SHOW_QUICK_SHORTCUTS] = showShortcuts
            it[Keys.SHOW_RECENT_HISTORY] = showRecentHistory
        }
    }
    suspend fun setRestoreTabsOnStartup(enabled: Boolean) { dataStore.edit { it[Keys.RESTORE_TABS_ON_STARTUP] = enabled } }
    suspend fun setDefaultDesktopMode(enabled: Boolean) { dataStore.edit { it[Keys.DEFAULT_DESKTOP_MODE] = enabled } }
    suspend fun setJavaScriptEnabled(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_JAVASCRIPT] = enabled } }
    suspend fun setDomStorageEnabled(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_DOM_STORAGE] = enabled } }
    suspend fun setHttpsOnlyMode(enabled: Boolean) { dataStore.edit { it[Keys.HTTPS_ONLY_MODE] = enabled } }
    suspend fun setSafeBrowsingEnabled(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_SAFE_BROWSING] = enabled } }
    suspend fun setBlockThirdPartyCookies(enabled: Boolean) { dataStore.edit { it[Keys.BLOCK_THIRD_PARTY_COOKIES] = enabled } }
    suspend fun setSendDoNotTrack(enabled: Boolean) { dataStore.edit { it[Keys.SEND_DO_NOT_TRACK] = enabled } }
    suspend fun setBlockScreenshotsInIncognito(enabled: Boolean) { dataStore.edit { it[Keys.BLOCK_SCREENSHOTS_INCOGNITO] = enabled } }
    suspend fun setExternalLinkPolicy(policy: ExternalLinkPolicy) { dataStore.edit { it[Keys.EXTERNAL_LINK_POLICY] = policy.name } }
    suspend fun setAdAndTrackerBlocker(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_AD_BLOCKER] = enabled } }
    suspend fun setStripTrackingParameters(enabled: Boolean) { dataStore.edit { it[Keys.STRIP_TRACKING_PARAMS] = enabled } }
    suspend fun setBiometricLockIncognito(enabled: Boolean) { dataStore.edit { it[Keys.BIOMETRIC_LOCK_INCOGNITO] = enabled } }
    suspend fun setBottomAddressBarReachability(enabled: Boolean) { dataStore.edit { it[Keys.BOTTOM_ADDRESS_BAR] = enabled } }
    suspend fun setEnableDesktopTabStrip(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_DESKTOP_TAB_STRIP] = enabled } }
    suspend fun setAllowBackgroundAudioPlayback(enabled: Boolean) { dataStore.edit { it[Keys.ALLOW_BACKGROUND_AUDIO] = enabled } }

    // Stage 11 & 12 Setters
    suspend fun setEnableAntiFingerprinting(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_ANTI_FINGERPRINTING] = enabled } }
    suspend fun setDnsOverHttpsProvider(provider: DnsOverHttpsProvider) { dataStore.edit { it[Keys.DOH_PROVIDER] = provider.id } }
    suspend fun setCustomProxyHostPort(hostPort: String) { dataStore.edit { it[Keys.CUSTOM_PROXY_HOST_PORT] = hostPort.trim() } }
    suspend fun setUserAgentProfile(profile: UserAgentProfile) { dataStore.edit { it[Keys.USER_AGENT_PROFILE] = profile.id } }
    suspend fun setEnableDataSaverMode(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_DATA_SAVER] = enabled } }
    suspend fun setEnableRedirectChainGuard(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_REDIRECT_GUARD] = enabled } }
    suspend fun setEnableAutoTabHibernation(enabled: Boolean) { dataStore.edit { it[Keys.ENABLE_TAB_HIBERNATION] = enabled } }

    suspend fun incrementHttpsUpgradeCount() {
        dataStore.edit { prefs -> prefs[Keys.HTTPS_UPGRADES_COUNT] = (prefs[Keys.HTTPS_UPGRADES_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementUnsafeSchemeBlockedCount() {
        dataStore.edit { prefs -> prefs[Keys.UNSAFE_SCHEMES_BLOCKED_COUNT] = (prefs[Keys.UNSAFE_SCHEMES_BLOCKED_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementSslWarningCount() {
        dataStore.edit { prefs -> prefs[Keys.SSL_WARNINGS_COUNT] = (prefs[Keys.SSL_WARNINGS_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementDataClearCount() {
        dataStore.edit { prefs -> prefs[Keys.DATA_CLEARS_COUNT] = (prefs[Keys.DATA_CLEARS_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementAdsBlockedCount() {
        dataStore.edit { prefs -> prefs[Keys.ADS_BLOCKED_COUNT] = (prefs[Keys.ADS_BLOCKED_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementTrackingParamsStrippedCount(delta: Int = 1) {
        dataStore.edit { prefs -> prefs[Keys.TRACKING_PARAMS_STRIPPED_COUNT] = (prefs[Keys.TRACKING_PARAMS_STRIPPED_COUNT] ?: 0) + delta }
    }
    suspend fun incrementRedirectChainsBlockedCount() {
        dataStore.edit { prefs -> prefs[Keys.REDIRECT_CHAINS_BLOCKED_COUNT] = (prefs[Keys.REDIRECT_CHAINS_BLOCKED_COUNT] ?: 0) + 1 }
    }
}
