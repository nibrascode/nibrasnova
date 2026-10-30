package com.nibras.nova.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomographAttackDetectorTest {

    private lateinit var detector: HomographAttackDetector

    @Before
    fun setUp() {
        detector = HomographAttackDetector()
    }

    @Test
    fun `inspectUrlForHomographAttack flags mixed Latin and Cyrillic lookalike domains`() {
        // Contains Cyrillic small letter 'а' (U+0430) mixed with Latin 'pple.com'
        val spoofedUrl = "https://аpple.com/login"
        val assessment = detector.inspectUrlForHomographAttack(spoofedUrl)
        assertTrue(assessment.isSuspicious)
    }

    @Test
    fun `inspectUrlForHomographAttack allows standard ASCII domains`() {
        val safeUrl = "https://developer.android.com/jetpack/compose"
        val assessment = detector.inspectUrlForHomographAttack(safeUrl)
        assertFalse(assessment.isSuspicious)
    }
}
