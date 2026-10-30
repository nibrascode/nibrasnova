package com.nibras.nova.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UrlValidatorTest {

    private lateinit var validator: UrlValidator

    @Before
    fun setUp() {
        validator = UrlValidator()
    }

    @Test
    fun `classifyInput identifies valid HTTPS and HTTP URLs`() {
        assertEquals(InputClassification.VALID_HTTPS_URL, validator.classifyInput("https://nibras.code/docs"))
        assertEquals(InputClassification.VALID_HTTP_URL, validator.classifyInput("http://example.org/test?a=1"))
    }

    @Test
    fun `classifyInput identifies naked domains, IPv4 addresses, and localhost`() {
        assertEquals(InputClassification.NAKED_DOMAIN_OR_HOST, validator.classifyInput("developer.android.com"))
        assertEquals(InputClassification.NAKED_DOMAIN_OR_HOST, validator.classifyInput("192.168.1.1:8080/admin"))
        assertEquals(InputClassification.NAKED_DOMAIN_OR_HOST, validator.classifyInput("localhost:3000"))
    }

    @Test
    fun `classifyInput blocks dangerous URI schemes`() {
        assertEquals(InputClassification.DANGEROUS_BLOCKED_SCHEME, validator.classifyInput("javascript:alert(document.cookie)"))
        assertEquals(InputClassification.DANGEROUS_BLOCKED_SCHEME, validator.classifyInput("file:///data/data/com.nibras.nova/databases/nova_browser.db"))
        assertEquals(InputClassification.DANGEROUS_BLOCKED_SCHEME, validator.classifyInput("data:text/html,<script>alert(1)</script>"))
        assertEquals(InputClassification.DANGEROUS_BLOCKED_SCHEME, validator.classifyInput("content://com.nibras.nova.provider/secret"))
    }

    @Test
    fun `classifyInput treats natural language and multi-word strings as search queries`() {
        assertEquals(InputClassification.SEARCH_QUERY, validator.classifyInput("kotlin coroutines flow tutorial"))
        assertEquals(InputClassification.SEARCH_QUERY, validator.classifyInput("what is 2 + 2?"))
    }

    @Test
    fun `upgradeToHttpsIfEnabled upgrades remote HTTP links while preserving localhost`() {
        assertEquals("https://example.com/login", validator.upgradeToHttpsIfEnabled("http://example.com/login", httpsOnlyMode = true))
        assertEquals("http://localhost:8080/api", validator.upgradeToHttpsIfEnabled("http://localhost:8080/api", httpsOnlyMode = true))
        assertEquals("http://example.com/login", validator.upgradeToHttpsIfEnabled("http://example.com/login", httpsOnlyMode = false))
    }

    @Test
    fun `isSafeWebNavigationUrl rejects javascript and file schemes`() {
        assertTrue(validator.isSafeWebNavigationUrl("https://kotlinlang.org"))
        assertFalse(validator.isSafeWebNavigationUrl("javascript:void(0)"))
        assertFalse(validator.isSafeWebNavigationUrl("file:///etc/hosts"))
    }
}
