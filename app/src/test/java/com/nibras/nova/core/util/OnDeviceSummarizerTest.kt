package com.nibras.nova.core.util

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnDeviceSummarizerTest {

    private lateinit var summarizer: OnDeviceSummarizer

    @Before
    fun setUp() {
        summarizer = OnDeviceSummarizer()
    }

    @Test
    fun `summarizeArticle extracts top ranked sentences and keywords locally`() {
        val sampleArticle = """
            Android WebView security requires strict configuration of file access and JavaScript bridges.
            Developers should always disable allowFileAccess and allowContentAccess to protect local sandbox files.
            Furthermore, intercepting SSL certificate errors prevents man-in-the-middle attacks on untrusted networks.
            Memory management is equally important when building a multi-tab browser on mobile devices.
            Using a bounded LRU pool of WebViews prevents native Chromium renderer out-of-memory crashes.
            Finally, stripping tracking parameters like utm_source and fbclid improves user privacy across websites.
        """.trimIndent()

        val result = summarizer.summarizeArticle(sampleArticle, maxBullets = 3)
        assertTrue(result.bulletPoints.size == 3)
        assertTrue(result.topKeywords.isNotEmpty())
    }
}
