package com.nibras.nova.core.halal

import java.net.URI

/** Pure decision helpers for Halal search mode (no Android deps, unit-tested). */
object HalalLogic {
    const val REVEAL_PARAM = "nova_reveal"
    const val MIN_SIDE_PX = 48
    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "bmp", "heic", "heif")

    /** Image-search result pages, where images stay blurred even without a classifier. */
    fun isImageSearch(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = runCatching { URI(url) }.getOrNull() ?: return false
        val host = (u.host ?: return false).lowercase().removePrefix("www.").removePrefix("m.")
        val path = u.rawPath.orEmpty().lowercase()
        val q = "&" + u.rawQuery.orEmpty().lowercase() + "&"
        return when {
            host.startsWith("google.") || host.contains(".google.") ->
                path.startsWith("/search") && (q.contains("&tbm=isch&") || q.contains("&udm=2&")) || host.startsWith("images.google.")
            host == "bing.com" || host.endsWith(".bing.com") -> path.startsWith("/images")
            host == "duckduckgo.com" -> q.contains("&ia=images&") || q.contains("&iax=images&")
            host.startsWith("yandex.") -> path.startsWith("/images")
            host == "images.search.yahoo.com" -> true
            host == "ecosia.org" -> path.startsWith("/images")
            host == "search.brave.com" -> path.startsWith("/images")
            else -> false
        }
    }

    fun looksLikeImage(url: String, accept: String?): Boolean {
        if (accept != null && accept.trimStart().startsWith("image/")) return true
        val ext = url.substringBefore('#').substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXT
    }

    /** True if the URL carries our tap-to-reveal marker; returns the original URL. */
    fun stripReveal(url: String): Pair<String, Boolean> {
        val i = url.indexOf("$REVEAL_PARAM=")
        if (i <= 0 || (url[i - 1] != '?' && url[i - 1] != '&')) return url to false
        val end = url.indexOf('&', i).let { if (it < 0) url.length else it + 1 }
        var out = url.removeRange(i, end)
        out = out.trimEnd('&').trimEnd('?')
        return out to true
    }

    /** Kovac RGB skin rule over packed ARGB pixels; returns the skin fraction 0..1. */
    fun skinRatio(px: IntArray): Float {
        if (px.isEmpty()) return 0f
        var n = 0
        for (c in px) {
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
            if (r > 95 && g > 40 && b > 20 && mx - mn > 15 && kotlin.math.abs(r - g) > 15 && r > g && r > b) n++
        }
        return n.toFloat() / px.size
    }

    /**
     * Strict "modesty" decision: the open_nsfw score targets explicit images, so a lower
     * threshold plus a skin-exposure check is used to also catch revealing photos.
     */
    fun isUnsafe(nsfwScore: Float, skin: Float): Boolean =
        nsfwScore >= 0.15f || (skin >= 0.40f && nsfwScore >= 0.04f)
}
