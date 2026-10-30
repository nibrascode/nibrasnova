package com.nibras.nova.core.security

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SchemePolicyTest {

    private lateinit var schemePolicy: SchemePolicy

    @Before
    fun setUp() {
        schemePolicy = SchemePolicy(UrlValidator())
    }

    @Test
    fun `evaluateNavigation blocks javascript and file URLs`() {
        val jsResult = schemePolicy.evaluateNavigation("javascript:evil()", httpsOnlyMode = true)
        assertTrue(jsResult is NavigationDisposition.BlockUnsafeScheme)

        val fileResult = schemePolicy.evaluateNavigation("file:///sdcard/Download/secret.txt", httpsOnlyMode = false)
        assertTrue(fileResult is NavigationDisposition.BlockUnsafeScheme)
    }

    @Test
    fun `evaluateNavigation upgrades HTTP to HTTPS when httpsOnlyMode is active`() {
        val result = schemePolicy.evaluateNavigation("http://github.com/nibras", httpsOnlyMode = true)
        assertTrue(result is NavigationDisposition.UpgradedToHttps)
    }
}
