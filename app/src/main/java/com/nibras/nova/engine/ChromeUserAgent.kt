package com.nibras.nova.engine

/**
 * Turns the WebView default UA into the UA Chrome for Android sends.
 * WebView adds "; wv" and "Version/4.0"; Google treats those (plus custom app tokens) as an
 * embedded/automated client and answers searches with the "unusual traffic" captcha.
 */
object ChromeUserAgent {
    fun fromWebViewDefault(defaultUa: String): String =
        defaultUa
            .replace("; wv)", ")")
            .replace(Regex("""\s*Version/\d+(\.\d+)*"""), "")
            .replace(Regex("""\s*NibrasNova/[\d.]+"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

    /** Hosts whose requests must go to the network exactly as the WebView sends them (cookies, headers). */
    fun isGoogleHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return Regex("""(^|\.)google\.[a-z.]+$""").containsMatchIn(h) ||
            listOf("gstatic.com", "googleusercontent.com", "ggpht.com", "googleapis.com", "recaptcha.net", "youtube.com", "ytimg.com")
                .any { h == it || h.endsWith(".$it") }
    }

    /** (useWideViewPort, loadWithOverviewMode): mobile honours the page viewport at normal zoom; desktop zooms out. */
    fun viewportFor(desktop: Boolean): Pair<Boolean, Boolean> = if (desktop) true to true else true to false

    fun isMobile(ua: String): Boolean = ua.contains("Mobile") && ua.contains("Android")
}
