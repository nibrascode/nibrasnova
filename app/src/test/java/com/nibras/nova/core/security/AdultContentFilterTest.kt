package com.nibras.nova.core.security

import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class AdultContentFilterTest {
    companion object {
        @BeforeClass @JvmStatic fun load() {
            val f = listOf("src/main/assets/adult_domains.txt.gz", "app/src/main/assets/adult_domains.txt.gz").map(::File).first { it.exists() }
            f.inputStream().use { AdultContentFilter.loadFrom(it) }
        }
    }

    @Test fun bundledListIsLarge() { assertTrue(AdultContentFilter.isHostListed("pornhub.com")) }
    @Test fun subdomainsMatch() {
        assertTrue(AdultContentFilter.shouldBlock("https://www.pornhub.com/"))
        assertTrue(AdultContentFilter.shouldBlock("https://de.xhamster.com/videos"))
    }
    @Test fun keywordHeuristicCatchesUnlisted() {
        assertTrue(AdultContentFilter.shouldBlock("https://brand-new-porn-site-12345.example/"))
        assertTrue(AdultContentFilter.shouldBlock("https://cdn.example.org/media/hentai/1.jpg"))
    }
    @Test fun normalSitesAllowed() {
        listOf(
            "https://nibrascode.com/programming/python", "https://www.google.com/", "https://github.com/x/y",
            "https://en.wikipedia.org/wiki/Essex", "https://www.youtube.com/watch?v=1", "https://www.bbc.co.uk/news",
            "https://sussex.ac.uk/", "https://example.com/expressions"
        ).forEach { assertFalse(it, AdultContentFilter.shouldBlock(it)) }
    }
    @Test fun nonHttpIgnored() { assertFalse(AdultContentFilter.shouldBlock("file:///android_asset/x.html")) }
    @Test fun safeSearchGoogle() {
        assertEquals("https://www.google.com/search?q=cats&safe=active", AdultContentFilter.enforceSafeSearch("https://www.google.com/search?q=cats"))
        assertEquals("https://www.google.az/search?q=a&safe=active", AdultContentFilter.enforceSafeSearch("https://www.google.az/search?q=a&safe=off"))
        assertNull(AdultContentFilter.enforceSafeSearch("https://www.google.com/search?q=cats&safe=active"))
    }
    @Test fun safeSearchBingDdg() {
        assertEquals("https://www.bing.com/search?q=x&adlt=strict", AdultContentFilter.enforceSafeSearch("https://www.bing.com/search?q=x"))
        assertEquals("https://duckduckgo.com/?q=x&kp=1#r", AdultContentFilter.enforceSafeSearch("https://duckduckgo.com/?q=x#r"))
        assertNull(AdultContentFilter.enforceSafeSearch("https://nibrascode.com/?q=x"))
    }
}
