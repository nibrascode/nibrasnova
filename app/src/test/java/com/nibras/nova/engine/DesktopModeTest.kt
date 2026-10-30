package com.nibras.nova.engine

import org.junit.Assert.*
import org.junit.Test

class DesktopModeTest {
    private val mobile = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/AP1A; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.102 Mobile Safari/537.36 NibrasNova/1.0"

    @Test fun desktopUaHasNoMobileTokens() {
        val ua = DesktopMode.desktopUserAgent(mobile)
        assertFalse(ua.contains("Mobile")); assertFalse(ua.contains("Android")); assertFalse(ua.contains("wv"))
        assertFalse(ua.contains("Version/4.0"))
        assertTrue(ua.contains("X11; Linux x86_64")); assertTrue(ua.contains("Chrome/130.0.6723.102"))
        assertFalse(DesktopMode.isMobileUserAgent(ua))
    }
    @Test fun mobileUaDetected() { assertTrue(DesktopMode.isMobileUserAgent(mobile)) }
    @Test fun fallbackVersion() { assertTrue(DesktopMode.desktopUserAgent(null).contains("Chrome/131.0.0.0")) }
    @Test fun scriptOverridesDetection() {
        val js = DesktopMode.documentStartScript(DesktopMode.desktopUserAgent(mobile))
        listOf("'userAgent'", "'userAgentData'", "'platform'", "'maxTouchPoints'", "mobile:false", "width='+W").forEach { assertTrue(it, js.contains(it)) }
        assertTrue(js.contains("W=${DesktopMode.DESKTOP_VIEWPORT_WIDTH}"))
    }
}
