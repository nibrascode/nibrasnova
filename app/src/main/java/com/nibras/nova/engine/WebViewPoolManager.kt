package com.nibras.nova.engine

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import com.nibras.nova.core.common.NovaConstants
import com.nibras.nova.core.security.AdBlockerEngine
import com.nibras.nova.core.security.SchemePolicy
import com.nibras.nova.core.security.SslErrorHandlerPolicy
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.TabItem

class WebViewPoolManager(
    private val appContext: Context,
    private val configurator: WebViewConfigurator,
    private val incognitoSessionManager: IncognitoSessionManager,
    private val schemePolicy: SchemePolicy,
    private val sslPolicy: SslErrorHandlerPolicy,
    private val adBlockerEngine: AdBlockerEngine
) {
    private val livePool = object : LinkedHashMap<String, WebView>(
        NovaConstants.MAX_LIVE_WEBVIEWS + 1,
        0.75f,
        true
    ) {}

    private val savedStateBundles = HashMap<String, Bundle>()
    private val incognitoTabIds = HashSet<String>()

    fun obtainOrCreateWebView(
        tab: TabItem,
        settings: BrowserSettings,
        webViewCallback: WebViewSessionCallback,
        chromeCallback: ChromeSessionCallback,
        downloadListener: NovaDownloadListener
    ): WebView {
        // Pause non-active WebViews unless user enabled Background Audio Playback
        if (!settings.allowBackgroundAudioPlayback) {
            livePool.forEach { (id, wv) ->
                if (id != tab.id) {
                    wv.onPause()
                }
            }
        }

        val existing = livePool[tab.id]
        if (existing != null) {
            detachFromParent(existing)
            configurator.applySecurityAndUserSettings(
                webView = existing,
                settings = settings,
                isPrivateTab = tab.isPrivate,
                isDesktopMode = tab.isDesktopMode
            )
            existing.onResume()
            return existing
        }

        if (livePool.size >= NovaConstants.MAX_LIVE_WEBVIEWS) {
            val eldestId = livePool.keys.firstOrNull()
            if (eldestId != null) {
                evictAndSaveTabState(eldestId)
            }
        }

        val newWebView = WebView(appContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            if (tab.isPrivate) {
                incognitoTabIds.add(tab.id)
                incognitoSessionManager.bindIncognitoProfileIfSupported(this)
            }
            configurator.applySecurityAndUserSettings(
                webView = this,
                settings = settings,
                isPrivateTab = tab.isPrivate,
                isDesktopMode = tab.isDesktopMode
            )
            webViewClient = NovaWebViewClient(
                tabId = tab.id,
                schemePolicy = schemePolicy,
                sslPolicy = sslPolicy,
                adBlockerEngine = adBlockerEngine,
                callback = webViewCallback
            )
            webChromeClient = NovaWebChromeClient(
                tabId = tab.id,
                callback = chromeCallback
            )
            setDownloadListener(downloadListener)
        }

        val savedBundle = savedStateBundles[tab.id]
        if (savedBundle != null) {
            newWebView.restoreState(savedBundle)
        } else if (!tab.isHomePage) {
            val headers = if (settings.sendDoNotTrackHeader) mapOf("DNT" to "1") else emptyMap()
            newWebView.loadUrl(tab.url, headers)
        }

        livePool[tab.id] = newWebView
        newWebView.onResume()
        return newWebView
    }

    fun getLiveWebView(tabId: String): WebView? = livePool[tabId]

    /** Applies desktop/mobile mode to one live tab only (other tabs keep their own mode). */
    fun applyDesktopMode(tab: TabItem, settings: BrowserSettings): WebView? {
        val wv = livePool[tab.id] ?: return null
        configurator.applySecurityAndUserSettings(
            webView = wv, settings = settings,
            isPrivateTab = tab.isPrivate || incognitoTabIds.contains(tab.id),
            isDesktopMode = tab.isDesktopMode
        )
        return wv
    }

    fun updateSettingsAcrossLivePool(
        settings: BrowserSettings,
        activeTabsById: Map<String, TabItem>
    ) {
        livePool.forEach { (tabId, webView) ->
            val tab = activeTabsById[tabId]
            configurator.applySecurityAndUserSettings(
                webView = webView,
                settings = settings,
                isPrivateTab = tab?.isPrivate ?: incognitoTabIds.contains(tabId),
                isDesktopMode = tab?.isDesktopMode ?: settings.defaultDesktopMode
            )
        }
    }

    fun destroyTabWebView(tabId: String) {
        savedStateBundles.remove(tabId)
        val wasIncognito = incognitoTabIds.remove(tabId)
        val webView = livePool.remove(tabId)
        if (webView != null) {
            cleanlyDestroyWebView(webView, clearDiskCache = wasIncognito)
        }
        if (wasIncognito && incognitoTabIds.isEmpty()) {
            incognitoSessionManager.purgeIncognitoSessionData(emptyList())
        }
    }

    fun destroyAllIncognitoWebViews() {
        val idsToRemove = incognitoTabIds.toList()
        val incognitoViews = idsToRemove.mapNotNull { livePool.remove(it) }
        incognitoSessionManager.purgeIncognitoSessionData(incognitoViews)
        incognitoViews.forEach { cleanlyDestroyWebView(it, clearDiskCache = true) }
        idsToRemove.forEach { savedStateBundles.remove(it) }
        incognitoTabIds.clear()
    }

    fun destroyAllNormalWebViews() {
        val normalIds = livePool.keys.filterNot { incognitoTabIds.contains(it) }
        normalIds.forEach { id ->
            savedStateBundles.remove(id)
            livePool.remove(id)?.let { cleanlyDestroyWebView(it, clearDiskCache = false) }
        }
    }

    private fun evictAndSaveTabState(tabId: String) {
        val webView = livePool.remove(tabId) ?: return
        val bundle = Bundle()
        runCatching { webView.saveState(bundle) }
        savedStateBundles[tabId] = bundle
        cleanlyDestroyWebView(webView, clearDiskCache = false)
    }

    private fun detachFromParent(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
    }

    private fun cleanlyDestroyWebView(webView: WebView, clearDiskCache: Boolean) {
        runCatching {
            detachFromParent(webView)
            webView.stopLoading()
            webView.onPause()
            if (clearDiskCache) {
                webView.clearCache(true)
            }
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}
