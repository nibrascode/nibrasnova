package com.nibras.nova.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultShortcutsTest {
    @Test fun exactlyThreePlusPinnedNibras() {
        assertEquals(listOf("YouTube", "GitHub", "Vercel"), DefaultShortcuts.CURRENT.map { it.first })
    }
    @Test fun migratesOnlyUntouchedLegacyList() {
        assertTrue(DefaultShortcuts.isUntouchedLegacy(DefaultShortcuts.LEGACY))
        assertFalse(DefaultShortcuts.isUntouchedLegacy(DefaultShortcuts.LEGACY.drop(1)))
        assertFalse(DefaultShortcuts.isUntouchedLegacy(DefaultShortcuts.LEGACY + ("Mine" to "https://example.com")))
        assertFalse(DefaultShortcuts.isUntouchedLegacy(DefaultShortcuts.LEGACY.reversed()))
    }
}
