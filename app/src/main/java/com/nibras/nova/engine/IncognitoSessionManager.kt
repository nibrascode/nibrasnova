package com.nibras.nova.engine

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncognitoSessionManager @Inject constructor() {

    companion object {
        const val INCOGNITO_PROFILE_NAME = "NovaIncognitoProfile"
    }

    val isMultiProfileSupported: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    fun bindIncognitoProfileIfSupported(webView: WebView) {
        if (isMultiProfileSupported) {
            runCatching {
                WebViewCompat.setProfile(webView, INCOGNITO_PROFILE_NAME)
            }
        }
    }

    fun purgeIncognitoSessionData(activeIncognitoWebViews: Collection<WebView>) {
        activeIncognitoWebViews.forEach { webView ->
            runCatching {
                webView.stopLoading()
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
            }
        }

        if (isMultiProfileSupported) {
            runCatching {
                val profileStore = ProfileStore.getInstance()
                val profile = profileStore.getProfile(INCOGNITO_PROFILE_NAME)
                profile?.cookieManager?.removeAllCookies(null)
                profile?.cookieManager?.flush()
                profile?.webStorage?.deleteAllData()
                profileStore.deleteProfile(INCOGNITO_PROFILE_NAME)
            }
        } else {
            // Fallback for older System WebView builds without MULTI_PROFILE:
            // Remove session cookies so ephemeral auth tokens do not linger.
            runCatching {
                CookieManager.getInstance().removeSessionCookies(null)
                CookieManager.getInstance().flush()
            }
        }
    }
}
