package com.nibras.nova.engine

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.nibras.nova.core.security.AdBlockerEngine
import com.nibras.nova.core.security.NavigationDisposition
import com.nibras.nova.core.security.SchemePolicy
import com.nibras.nova.core.security.SslCertificateChallenge
import com.nibras.nova.core.security.SslErrorHandlerPolicy

interface WebViewSessionCallback {
    fun isHttpsOnlyEnabled(): Boolean
    fun isAdBlockerEnabled(): Boolean
    fun isRedirectChainGuardEnabled(): Boolean
    fun shouldSendDoNotTrack(): Boolean
    fun onPageLoadStarted(tabId: String, url: String)
    fun onPageLoadFinished(tabId: String, url: String, title: String?, canGoBack: Boolean, canGoForward: Boolean)
    fun onNavigationBlocked(tabId: String, blockedUrl: String, reason: String)
    fun onHttpsUpgraded(tabId: String, newHttpsUrl: String)
    fun onAdOrTrackerBlocked(tabId: String, blockedUrl: String)
    fun onRedirectChainBlocked(tabId: String, blockedTargetUrl: String, hopCount: Int)
    fun onNetworkResourceObserved(tabId: String, method: String, url: String, blocked: Boolean)
    fun onExternalAppNavigationRequested(tabId: String, disposition: NavigationDisposition.PromptExternalApp)
    fun onSslCertificateError(tabId: String, challenge: SslCertificateChallenge)
    fun onMainFrameNetworkError(tabId: String, failingUrl: String, errorCode: Int, description: String)
}

class NovaWebViewClient(
    private val tabId: String,
    private val schemePolicy: SchemePolicy,
    private val sslPolicy: SslErrorHandlerPolicy,
    private val adBlockerEngine: AdBlockerEngine,
    private val callback: WebViewSessionCallback
) : WebViewClient() {

    // Stage 12.6: Rapid Cross-Domain Redirect Chain Detector
    private var lastMainFrameNavTimestamp = 0L
    private var lastMainFrameHost = ""
    private var rapidCrossDomainRedirectHops = 0

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (request == null) return null
        val reqUrl = request.url?.toString().orEmpty()
        val method = request.method ?: "GET"

        if (!request.isForMainFrame && callback.isAdBlockerEnabled() && adBlockerEngine.shouldBlockRequest(reqUrl)) {
            callback.onAdOrTrackerBlocked(tabId, reqUrl)
            callback.onNetworkResourceObserved(tabId, method, reqUrl, blocked = true)
            return adBlockerEngine.createEmptyBlockedResponse()
        }

        callback.onNetworkResourceObserved(tabId, method, reqUrl, blocked = false)
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val targetUrl = request.url?.toString().orEmpty()
        if (request.isForMainFrame && callback.isRedirectChainGuardEnabled() && !request.hasGesture()) {
            val now = System.currentTimeMillis()
            val newHost = runCatching { Uri.parse(targetUrl).host?.lowercase().orEmpty() }.getOrDefault("")
            if (now - lastMainFrameNavTimestamp < 1_800L && newHost.isNotBlank() && newHost != lastMainFrameHost) {
                rapidCrossDomainRedirectHops++
                if (rapidCrossDomainRedirectHops >= 3) {
                    callback.onRedirectChainBlocked(tabId, targetUrl, rapidCrossDomainRedirectHops)
                    rapidCrossDomainRedirectHops = 0
                    return true
                }
            } else {
                rapidCrossDomainRedirectHops = 0
            }
            lastMainFrameNavTimestamp = now
            lastMainFrameHost = newHost
        } else if (request.hasGesture()) {
            rapidCrossDomainRedirectHops = 0
        }
        return handleNavigationRequest(view, targetUrl, isMainFrame = request.isForMainFrame)
    }

    private fun handleNavigationRequest(view: WebView, targetUrl: String, isMainFrame: Boolean): Boolean {
        when (val disposition = schemePolicy.evaluateNavigation(targetUrl, callback.isHttpsOnlyEnabled())) {
            is NavigationDisposition.AllowInWebView -> return false
            is NavigationDisposition.UpgradedToHttps -> {
                if (isMainFrame) {
                    callback.onHttpsUpgraded(tabId, disposition.upgradedUrl)
                    val headers = if (callback.shouldSendDoNotTrack()) mapOf("DNT" to "1") else emptyMap()
                    view.loadUrl(disposition.upgradedUrl, headers)
                    return true
                }
                return false
            }
            is NavigationDisposition.PromptExternalApp -> {
                callback.onExternalAppNavigationRequested(tabId, disposition)
                return true
            }
            is NavigationDisposition.BlockUnsafeScheme -> {
                callback.onNavigationBlocked(tabId, disposition.blockedUrl, disposition.reason)
                return true
            }
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!url.isNullOrBlank()) {
            callback.onPageLoadStarted(tabId, url)
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        val currentUrl = url ?: view.url.orEmpty()
        callback.onPageLoadFinished(
            tabId = tabId,
            url = currentUrl,
            title = view.title,
            canGoBack = view.canGoBack(),
            canGoForward = view.canGoForward()
        )
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (handler == null || error == null) {
            handler?.cancel()
            return
        }
        val (title, description) = sslPolicy.describeSslError(error.primaryError)
        val challenge = SslCertificateChallenge(
            failingUrl = error.url ?: view?.url.orEmpty(),
            primaryErrorCode = error.primaryError,
            errorTitle = title,
            technicalDescription = description,
            issuedTo = error.certificate?.issuedTo?.dName ?: L.s(R.string.t_unknown_subject),
            issuedBy = error.certificate?.issuedBy?.dName ?: L.s(R.string.t_unknown_issuer),
            onCancelNavigation = { handler.cancel() },
            onProceedWithExplicitRisk = { handler.proceed() }
        )
        callback.onSslCertificateError(tabId, challenge)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            callback.onMainFrameNetworkError(
                tabId = tabId,
                failingUrl = request.url?.toString().orEmpty(),
                errorCode = error.errorCode,
                description = error.description?.toString() ?: L.s(R.string.t_unable_to_connect_to_server)
            )
        }
    }
}
