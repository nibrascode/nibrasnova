package com.nibras.nova.core.parental

import org.junit.Assert.*
import org.junit.Test

class PinManagerTest {
    private class Mem : KvStore {
        val s = HashMap<String, String?>(); val l = HashMap<String, Long>()
        override fun getString(k: String) = s[k]
        override fun putString(k: String, v: String?) { s[k] = v }
        override fun getLong(k: String, d: Long) = l[k] ?: d
        override fun putLong(k: String, v: Long) { l[k] = v }
    }
    private var now = 1_000_000L
    private val kv = Mem()
    private val pm = PinManager(kv) { now }

    @Test fun hashAndVerify() {
        assertEquals(PinResult.NoPin, pm.verify("1234"))
        pm.setPin("4821")
        assertTrue(pm.hasPin)
        assertFalse(kv.s.values.any { it?.contains("4821") == true })
        assertEquals(PinResult.Ok, pm.verify("4821"))
        assertTrue(pm.verify("0000") is PinResult.Wrong)
    }
    @Test fun saltDiffers() {
        val a = PinManager.hash("1234", ByteArray(16) { 1 }); val b = PinManager.hash("1234", ByteArray(16) { 2 })
        assertFalse(a.contentEquals(b))
    }
    @Test fun invalidPins() {
        assertFalse(PinManager.isValidPin("123")); assertFalse(PinManager.isValidPin("1234567"))
        assertFalse(PinManager.isValidPin("12a4")); assertTrue(PinManager.isValidPin("123456"))
    }
    @Test fun lockoutEscalates() {
        pm.setPin("1111")
        for (i in 4 downTo 1) assertEquals(PinResult.Wrong(i), pm.verify("2222"))
        val r = pm.verify("2222"); assertTrue(r is PinResult.LockedOut)
        assertEquals(now + 30_000, (r as PinResult.LockedOut).untilMs)
        assertTrue(pm.verify("1111") is PinResult.LockedOut) // even correct PIN refused
        now += 30_001
        repeat(4) { pm.verify("2222") }
        val r2 = pm.verify("2222") as PinResult.LockedOut
        assertEquals(now + 60_000, r2.untilMs)
        now += 60_001
        assertEquals(PinResult.Ok, pm.verify("1111"))
    }
    @Test fun clearPin() {
        pm.setPin("1234"); pm.clearPin(); assertFalse(pm.hasPin); assertEquals(PinResult.NoPin, pm.verify("1234"))
    }
    @Test fun normalizeDomain() {
        assertEquals("example.com", ParentalControls.normalizeDomain(" https://www.Example.com/path "))
    }
}
