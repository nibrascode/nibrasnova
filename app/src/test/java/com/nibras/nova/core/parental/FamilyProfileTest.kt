package com.nibras.nova.core.parental

import org.junit.Assert.*
import org.junit.Test

class FamilyProfileTest {
    @Test fun allowlistMatchesSubdomains() {
        val a = FamilyProfile.DEFAULT_CHILD_ALLOW
        assertTrue(FamilyProfile.isAllowed("nibrascode.com", a))
        assertTrue(FamilyProfile.isAllowed("www.quran.com", a))
        assertTrue(FamilyProfile.isAllowed("legacy.quran.com", a))
        assertFalse(FamilyProfile.isAllowed("notquran.com", a))
        assertFalse(FamilyProfile.isAllowed("youtube.com", a))
        assertFalse(FamilyProfile.isAllowed(null, a))
    }
    @Test fun adultDefaults() {
        assertEquals("nova.db", FamilyProfile.dbName("nova.db"))
        assertNull(FamilyProfile.webViewSuffix(null))
        assertEquals("p_x", FamilyProfile.webViewSuffix("p_x"))
    }
}
