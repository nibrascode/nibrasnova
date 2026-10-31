package com.nibras.nova.engine

import com.nibras.nova.domain.model.SearchEngineConfig
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.core.security.AdultContentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeUserAgentTest {
    private val webViewUa = "Mozilla/5.0 (Linux; Android 14; Pixel 7 Build/UQ1A.240205.004; wv) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.102 Mobile Safari/537.36"

    @Test fun webViewUaBecomesChromeMobileUa() {
        val ua = ChromeUserAgent.fromWebViewDefault(webViewUa)
        assertEquals("Mozilla/5.0 (Linux; Android 14; Pixel 7 Build/UQ1A.240205.004) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.6723.102 Mobile Safari/537.36", ua)
        assertFalse(ua.contains("wv")); assertFalse(ua.contains("Version/")); assertFalse(ua.contains("NibrasNova"))
    }

    @Test fun appTokenIsStripped() {
        assertFalse(ChromeUserAgent.fromWebViewDefault("$webViewUa NibrasNova/1.0").contains("NibrasNova"))
    }

    @Test fun googleHostsAreRecognised() {
        listOf("www.google.com", "google.az", "encrypted-tbn0.gstatic.com", "lh3.googleusercontent.com", "www.google.com.tr")
            .forEach { assertTrue(it, ChromeUserAgent.isGoogleHost(it)) }
        listOf("example.com", "notgoogle.org", null).forEach { assertFalse(ChromeUserAgent.isGoogleHost(it)) }
    }

    @Test fun googleSearchUrlIsCleanWithSafeSearch() {
        val url = SearchEngineConfig(selectedEngine = SearchEngineType.GOOGLE).buildSearchUrl("namaz vaxti")
        assertEquals("https://www.google.com/search?q=namaz+vaxti&safe=active", url)
        assertNull(AdultContentFilter.enforceSafeSearch(url)) // no extra redirect hop
    }

    @Test fun mobileUaIsMobileDesktopIsNot() {
        val mobile = ChromeUserAgent.fromWebViewDefault(webViewUa)
        assertTrue(mobile.contains("Mobile")); assertTrue(mobile.contains("Android")); assertTrue(ChromeUserAgent.isMobile(mobile))
        val desktop = DesktopMode.desktopUserAgent(mobile)
        assertFalse(desktop.contains("Mobile")); assertFalse(desktop.contains("Android")); assertFalse(ChromeUserAgent.isMobile(desktop))
    }

    @Test fun mobileViewportHonoursPageAtNormalZoom() {
        assertEquals(true to false, ChromeUserAgent.viewportFor(desktop = false))
        assertEquals(true to true, ChromeUserAgent.viewportFor(desktop = true))
    }
}
