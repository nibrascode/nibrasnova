package com.nibras.nova.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdBlockerEngineTest {

    private lateinit var adBlockerEngine: AdBlockerEngine

    @Before
    fun setUp() {
        adBlockerEngine = AdBlockerEngine()
    }

    @Test
    fun `isHostBlocked identifies known advertising and tracker domains`() {
        assertTrue(adBlockerEngine.isHostBlocked("doubleclick.net"))
        assertTrue(adBlockerEngine.isHostBlocked("ad.doubleclick.net"))
        assertTrue(adBlockerEngine.isHostBlocked("www.google-analytics.com"))
        assertTrue(adBlockerEngine.isHostBlocked("connect.facebook.net"))
    }

    @Test
    fun `isHostBlocked allows legitimate content domains`() {
        assertFalse(adBlockerEngine.isHostBlocked("developer.android.com"))
        assertFalse(adBlockerEngine.isHostBlocked("kotlinlang.org"))
        assertFalse(adBlockerEngine.isHostBlocked("github.com"))
    }
}
