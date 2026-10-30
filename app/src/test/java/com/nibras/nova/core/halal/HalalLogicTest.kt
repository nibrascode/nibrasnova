package com.nibras.nova.core.halal

import org.junit.Assert.*
import org.junit.Test

class HalalLogicTest {
    @Test fun imageSearchPages() {
        assertTrue(HalalLogic.isImageSearch("https://www.google.com/search?q=cat&tbm=isch"))
        assertTrue(HalalLogic.isImageSearch("https://www.google.az/search?q=cat&udm=2"))
        assertTrue(HalalLogic.isImageSearch("https://www.bing.com/images/search?q=cat"))
        assertTrue(HalalLogic.isImageSearch("https://duckduckgo.com/?q=cat&ia=images&iax=images"))
        assertTrue(HalalLogic.isImageSearch("https://yandex.com/images/search?text=cat"))
        assertFalse(HalalLogic.isImageSearch("https://www.google.com/search?q=cat"))
        assertFalse(HalalLogic.isImageSearch("https://nibrascode.com/images/x"))
        assertFalse(HalalLogic.isImageSearch(null))
    }
    @Test fun imageDetection() {
        assertTrue(HalalLogic.looksLikeImage("https://a.com/x", "image/avif,image/webp,*/*"))
        assertTrue(HalalLogic.looksLikeImage("https://a.com/p/photo.JPG?w=200", null))
        assertFalse(HalalLogic.looksLikeImage("https://a.com/app.js", "*/*"))
    }
    @Test fun revealParam() {
        assertEquals("https://a.com/x.jpg" to true, HalalLogic.stripReveal("https://a.com/x.jpg?nova_reveal=1"))
        assertEquals("https://a.com/x.jpg?w=2" to true, HalalLogic.stripReveal("https://a.com/x.jpg?w=2&nova_reveal=1"))
        assertEquals("https://a.com/x.jpg?w=2" to true, HalalLogic.stripReveal("https://a.com/x.jpg?nova_reveal=1&w=2"))
        assertEquals("https://a.com/x.jpg?w=2" to false, HalalLogic.stripReveal("https://a.com/x.jpg?w=2"))
    }
    @Test fun skinAndDecision() {
        val skin = IntArray(100) { 0xFFE0AC8C.toInt() }; val sky = IntArray(100) { 0xFF3D7BFF.toInt() }
        assertEquals(1f, HalalLogic.skinRatio(skin), 0f); assertEquals(0f, HalalLogic.skinRatio(sky), 0f)
        assertTrue(HalalLogic.isUnsafe(0.5f, 0f))
        assertTrue(HalalLogic.isUnsafe(0.06f, 0.5f))
        assertFalse(HalalLogic.isUnsafe(0.01f, 0.5f))
        assertFalse(HalalLogic.isUnsafe(0.05f, 0.1f))
    }
}
