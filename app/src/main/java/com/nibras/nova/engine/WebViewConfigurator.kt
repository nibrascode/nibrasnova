package com.nibras.nova.engine

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.nibras.nova.core.common.NovaConstants
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.UserAgentProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewConfigurator @Inject constructor() {

    private var defaultMobileUserAgent: String? = null
    private val desktopScripts = java.util.WeakHashMap<WebView, androidx.webkit.ScriptHandler>()

    @SuppressLint("SetJavaScriptEnabled")
    fun applySecurityAndUserSettings(
        webView: WebView,
        settings: BrowserSettings,
        isPrivateTab: Boolean,
        isDesktopMode: Boolean,
        siteTextZoomPercent: Int = 100
    ) {
        val ws = webView.settings

        if (defaultMobileUserAgent == null) {
            // Use the platform default, never the (possibly already desktop) UA of a reused WebView.
            val base = runCatching { WebSettings.getDefaultUserAgent(webView.context) }.getOrNull() ?: ws.userAgentString
            defaultMobileUserAgent = base + NovaConstants.MOBILE_USER_AGENT_SUFFIX
        }

        // Strict Security Hardening
        ws.allowFileAccess = false
        ws.allowContentAccess = false
        @Suppress("DEPRECATION")
        ws.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        ws.allowUniversalAccessFromFileURLs = false

        // Controlled JavaScript & DOM Storage
        ws.javaScriptEnabled = settings.enableJavaScript
        ws.domStorageEnabled = settings.enableDomStorage
        ws.databaseEnabled = !isPrivateTab && settings.enableDomStorage

        // Block unsolicited popups
        ws.javaScriptCanOpenWindowsAutomatically = false
        ws.setSupportMultipleWindows(true)

        // Viewport, Per-Site Text Zoom & User-Agent Profile Simulator (Stage 11.8 & Stage 12.8)
        ws.setSupportZoom(true)
        ws.builtInZoomControls = true
        ws.displayZoomControls = false
        ws.textZoom = siteTextZoomPercent.coerceIn(50, 250)

        val customProfile = settings.userAgentProfile
        val desktopRequested = isDesktopMode && customProfile == UserAgentProfile.ANDROID_DEFAULT
        val useDesktopViewport = isDesktopMode || customProfile.forceWideDesktopViewport
        ws.useWideViewPort = useDesktopViewport
        ws.loadWithOverviewMode = useDesktopViewport

        val targetUa = when {
            desktopRequested -> DesktopMode.desktopUserAgent(defaultMobileUserAgent)
            customProfile.userAgentString != null -> customProfile.userAgentString
            else -> defaultMobileUserAgent ?: ws.userAgentString
        }
        if (ws.userAgentString != targetUa) ws.userAgentString = targetUa
        applyDesktopOverrides(webView, ws, if (useDesktopViewport && !DesktopMode.isMobileUserAgent(targetUa)) targetUa else null)

        // Stage 12.10: Smart Data Saver Mode (restrict heavy network image auto-loading & media autoplay)
        ws.loadsImagesAutomatically = true
        ws.blockNetworkImage = settings.enableDataSaverMode
        ws.mediaPlaybackRequiresUserGesture = settings.enableDataSaverMode || !settings.allowBackgroundAudioPlayback

        // Mixed Content Policy
        ws.mixedContentMode = if (settings.httpsOnlyMode) {
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        } else {
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // Cache and Form Data Isolation for Private Browsing
        if (isPrivateTab) {
            ws.cacheMode = WebSettings.LOAD_NO_CACHE
            @Suppress("DEPRECATION")
            ws.saveFormData = false
        } else {
            ws.cacheMode = WebSettings.LOAD_DEFAULT
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(ws, settings.enableSafeBrowsing)
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(ws, true)
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, !settings.blockThirdPartyCookies && !isPrivateTab)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.importantForAutofill = if (isPrivateTab) {
                WebView.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            } else {
                WebView.IMPORTANT_FOR_AUTOFILL_YES
            }
        }
    }

    /** Client hints + document-start JS so both server and JS mobile detection see desktop. Null = restore mobile. */
    private fun applyDesktopOverrides(webView: WebView, ws: WebSettings, desktopUa: String?) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            runCatching {
                if (desktopUa != null) {
                    val major = DesktopMode.chromeVersion(desktopUa).substringBefore('.')
                    val brands = listOf(
                        androidx.webkit.UserAgentMetadata.BrandVersion.Builder().setBrand("Chromium").setMajorVersion(major).setFullVersion(DesktopMode.chromeVersion(desktopUa)).build(),
                        androidx.webkit.UserAgentMetadata.BrandVersion.Builder().setBrand("Google Chrome").setMajorVersion(major).setFullVersion(DesktopMode.chromeVersion(desktopUa)).build()
                    )
                    WebSettingsCompat.setUserAgentMetadata(
                        ws,
                        androidx.webkit.UserAgentMetadata.Builder()
                            .setBrandVersionList(brands)
                            .setFullVersion(DesktopMode.chromeVersion(desktopUa))
                            .setPlatform("Linux").setPlatformVersion("")
                            .setArchitecture("x86").setBitness(64).setModel("")
                            .setMobile(false).build()
                    )
                } else {
                    WebSettingsCompat.setUserAgentMetadata(ws, androidx.webkit.UserAgentMetadata.Builder().build())
                }
            }
        }
        val existing = desktopScripts[webView]
        if (desktopUa == null) {
            existing?.remove(); desktopScripts.remove(webView)
        } else if (existing == null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching {
                desktopScripts[webView] = androidx.webkit.WebViewCompat.addDocumentStartJavaScript(
                    webView, DesktopMode.documentStartScript(desktopUa), setOf("*")
                )
            }
        }
    }
}
