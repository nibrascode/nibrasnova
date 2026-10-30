package com.nibras.nova.core.prayer

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerEngineTest {
    private val baku = ZoneId.of("Asia/Baku")
    private val mwl = PrayerEngine.method(null)

    @Test fun defaultIsMwl() { assertEquals("MUSLIM_WORLD_LEAGUE", mwl.name) }

    @Test fun timesAreOrderedAndPlausible() {
        val d = PrayerEngine.dayTimes(40.4093, 49.8671, LocalDate.of(2026, 6, 21), mwl).times
        val order = PrayerName.entries.map { d.getValue(it) }
        assertEquals(order.sorted(), order)
        val dhuhr = java.time.Instant.ofEpochMilli(d.getValue(PrayerName.DHUHR)).atZone(baku)
        assertTrue("dhuhr ${dhuhr.toLocalTime()}", dhuhr.hour in 12..13)
    }

    @Test fun rollsOverToFajrAfterIsha() {
        val late = ZonedDateTime.of(2026, 6, 21, 23, 50, 0, 0, baku).toInstant().toEpochMilli()
        val n = PrayerEngine.next(40.4093, 49.8671, late, baku, mwl)
        assertEquals(PrayerName.FAJR, n.prayer); assertTrue(n.atEpochMs > late)
    }

    @Test fun countdownFormat() {
        assertEquals("1:02:03", PrayerEngine.formatCountdown(3723_000)); assertEquals("05:09", PrayerEngine.formatCountdown(309_000))
    }
}
