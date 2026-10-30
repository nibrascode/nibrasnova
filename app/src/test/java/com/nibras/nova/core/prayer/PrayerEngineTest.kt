package com.nibras.nova.core.prayer

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerEngineTest {
    private val baku = ZoneId.of("Asia/Baku")
    private val mwl = PrayerEngine.method("MUSLIM_WORLD_LEAGUE")

    @Test fun defaultIsUmmAlQura() { assertEquals("UMM_AL_QURA", PrayerEngine.method(null).name) }

    /** Makkah, 1 Jan 2026 (not Ramadan). Reference (Umm al-Qura timetable): Fajr ~05:40, Dhuhr ~12:24, Asr ~15:27, Maghrib ~17:50, Isha = Maghrib + 90 min. */
    @Test fun makkahUmmAlQura() {
        val riyadh = ZoneId.of("Asia/Riyadh")
        val t = PrayerEngine.dayTimes(21.4225, 39.8262, LocalDate.of(2026, 1, 1), PrayerEngine.method("UMM_AL_QURA")).times
        fun hm(p: PrayerName) = java.time.Instant.ofEpochMilli(t.getValue(p)).atZone(riyadh).toLocalTime()
        fun near(p: PrayerName, h: Int, m: Int) {
            val diff = kotlin.math.abs(hm(p).toSecondOfDay() - (h * 3600 + m * 60))
            assertTrue("$p was ${hm(p)}", diff <= 4 * 60)
        }
        near(PrayerName.FAJR, 5, 40); near(PrayerName.DHUHR, 12, 24); near(PrayerName.ASR, 15, 27); near(PrayerName.MAGHRIB, 17, 50)
        assertEquals(90L, (t.getValue(PrayerName.ISHA) - t.getValue(PrayerName.MAGHRIB)) / 60_000)
    }

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
