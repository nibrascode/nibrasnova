package com.nibras.nova.core.security

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdBlockerEngine @Inject constructor() {

    private val blockedTrackerAndAdDomains = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "adnxs.com",
        "adsrvr.org",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "quantserve.com",
        "zedo.com",
        "moatads.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "adform.net",
        "amazon-adsystem.com",
        "analytics.yahoo.com",
        "ads.yahoo.com",
        "Hotjar.com".lowercase(Locale.ROOT),
        "mixpanel.com",
        "segment.io",
        "amplitude.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "an.facebook.com",
        "ads-twitter.com",
        "analytics.tiktok.com",
        "ads.tiktok.com",
        "mc.yandex.ru"
    )

    private val blockedPathPatterns = listOf(
        "/pagead/",
        "/ads/ga-audiences",
        "/collect?v=1",
        "/gtm.js",
        "/fbevents.js",
        "/analytics.js"
    )

    fun shouldBlockRequest(requestUrl: String): Boolean {
        val trimmed = requestUrl.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = Uri.parse(trimmed)
            val host = uri.host?.lowercase(Locale.ROOT) ?: return false
            if (isHostBlocked(host)) {
                return true
            }
            val pathAndQuery = (uri.path.orEmpty() + "?" + uri.query.orEmpty()).lowercase(Locale.ROOT)
            blockedPathPatterns.any { pathAndQuery.contains(it) }
        } catch (_: Exception) {
            false
        }
    }

    fun isHostBlocked(host: String): Boolean {
        val cleanHost = host.lowercase(Locale.ROOT).removePrefix("www.")
        if (cleanHost in blockedTrackerAndAdDomains) return true
        return blockedTrackerAndAdDomains.any { domain ->
            cleanHost == domain || cleanHost.endsWith(".$domain")
        }
    }

    fun createEmptyBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "No Content",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
