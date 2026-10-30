package com.nibras.nova.domain.model

import com.nibras.nova.core.common.NovaConstants
import com.nibras.nova.core.security.DnsOverHttpsProvider

enum class ThemeMode(val label: String) {
    DARK("Nibras Signature Dark"),
    LIGHT("Nibras Crisp Light"),
    SYSTEM("Follow System Theme")
}

enum class ExternalLinkPolicy(val label: String) {
    ASK_EVERY_TIME("Ask Before Opening External App"),
    ALWAYS_OPEN_EXTERNAL("Always Open External App Automatically"),
    STAY_IN_BROWSER("Keep Links Inside Nibras Nova")
}

data class BrowserSettings(
    val isFirstLaunch: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val highContrastAccent: Boolean = true,
    val customWallpaperUri: String? = null,
    val wallpaperPresetId: String = "nova_deep_navy",
    val searchEngine: SearchEngineType = SearchEngineType.DUCKDUCKGO,
    val customSearchUrlTemplate: String = "https://duckduckgo.com/?q=%s",
    val enableLocalSearchSuggestions: Boolean = true,
    val customHomepageUrl: String = NovaConstants.DEFAULT_HOMEPAGE_SENTINEL,
    val showQuickShortcutsOnHome: Boolean = true,
    val showRecentHistoryOnHome: Boolean = true,
    val restoreTabsOnStartup: Boolean = true,
    val defaultDesktopMode: Boolean = false,
    val enableJavaScript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val httpsOnlyMode: Boolean = true,
    val enableSafeBrowsing: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val sendDoNotTrackHeader: Boolean = true,
    val blockScreenshotsInIncognito: Boolean = true,
    val externalLinkPolicy: ExternalLinkPolicy = ExternalLinkPolicy.ASK_EVERY_TIME,
    // Stage 10 Pro Features
    val enableAdAndTrackerBlocker: Boolean = true,
    val stripTrackingUrlParameters: Boolean = true,
    val biometricLockIncognitoTabs: Boolean = false,
    val bottomAddressBarReachability: Boolean = false,
    val enableDesktopTabStrip: Boolean = false,
    val allowBackgroundAudioPlayback: Boolean = false,
    // Stage 11 & Stage 12 Ultimate Features
    val enableAntiFingerprinting: Boolean = true,
    val dnsOverHttpsProvider: DnsOverHttpsProvider = DnsOverHttpsProvider.CLOUDFLARE_1111,
    val customProxyHostPort: String = "",
    val userAgentProfile: UserAgentProfile = UserAgentProfile.ANDROID_DEFAULT,
    val enableDataSaverMode: Boolean = false,
    val enableRedirectChainGuard: Boolean = true,
    val enableAutoTabHibernation: Boolean = true
)
