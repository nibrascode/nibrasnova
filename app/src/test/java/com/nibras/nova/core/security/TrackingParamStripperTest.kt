package com.nibras.nova.core.security

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrackingParamStripperTest {

    private lateinit var stripper: TrackingParamStripper

    @Before
    fun setUp() {
        stripper = TrackingParamStripper()
    }

    @Test
    fun `stripTrackingParameters removes utm_source, fbclid, and gclid while keeping functional query params`() {
        val rawUrl = "https://example.com/article?id=42&utm_source=newsletter&utm_medium=email&fbclid=IwAR12345&page=2"
        val result = stripper.stripTrackingParameters(rawUrl)
        assertEquals("https://example.com/article?id=42&page=2", result.sanitizedUrl)
        assertEquals(3, result.removedParamsCount)
    }

    @Test
    fun `stripTrackingParameters removes entire query string when only trackers exist`() {
        val rawUrl = "https://nibras.code/blog?gclid=test999&mc_cid=abc"
        val result = stripper.stripTrackingParameters(rawUrl)
        assertEquals("https://nibras.code/blog", result.sanitizedUrl)
        assertEquals(2, result.removedParamsCount)
    }
}
