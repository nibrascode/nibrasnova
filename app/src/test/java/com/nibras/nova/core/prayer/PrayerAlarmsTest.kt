package com.nibras.nova.core.prayer

import com.batoulapps.adhan2.CalculationMethod
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerAlarmsTest {
    private val baku = ZoneId.of("Asia/Baku")
    private val m = CalculationMethod.UMM_AL_QURA
    private val day = LocalDate.of(2026, 10, 31)
    private val times = PrayerEngine.dayTimes(40.41, 49.87, day, m).times

    @Test fun beforeComesFirst() {
        val now = ZonedDateTime.of(day.atTime(10, 0), baku).toInstant().toEpochMilli()
        val e = PrayerAlarms.nextEvent(40.41, 49.87, baku, m, now, { true }, { true })!!
        assertEquals(PrayerName.DHUHR, e.prayer); assertEquals(PrayerEvent.Kind.BEFORE, e.kind)
        assertEquals(times.getValue(PrayerName.DHUHR) - 600_000, e.triggerAtMs)
    }
    @Test fun atAfterBefore() {
        val now = times.getValue(PrayerName.DHUHR) - 300_000
        val e = PrayerAlarms.nextEvent(40.41, 49.87, baku, m, now, { true }, { true })!!
        assertEquals(PrayerEvent.Kind.AT, e.kind); assertEquals(times.getValue(PrayerName.DHUHR), e.triggerAtMs)
    }
    @Test fun perPrayerToggles() {
        val now = ZonedDateTime.of(day.atTime(10, 0), baku).toInstant().toEpochMilli()
        val e = PrayerAlarms.nextEvent(40.41, 49.87, baku, m, now, { false }, { it == PrayerName.ASR })!!
        assertEquals(PrayerName.ASR, e.prayer); assertEquals(PrayerEvent.Kind.AT, e.kind)
        assertNull(PrayerAlarms.nextEvent(40.41, 49.87, baku, m, now, { false }, { false }))
    }
    @Test fun rollsToTomorrowFajrAndSkipsSunrise() {
        val now = times.getValue(PrayerName.ISHA) + 60_000
        val e = PrayerAlarms.nextEvent(40.41, 49.87, baku, m, now, { true }, { true })!!
        assertEquals(PrayerName.FAJR, e.prayer); assertEquals(PrayerEvent.Kind.BEFORE, e.kind)
        val tomorrowFajr = PrayerEngine.dayTimes(40.41, 49.87, day.plusDays(1), m).times.getValue(PrayerName.FAJR)
        assertEquals(tomorrowFajr - 600_000, e.triggerAtMs)
        val afterFajr = PrayerAlarms.nextEvent(40.41, 49.87, baku, m, tomorrowFajr + 1000, { true }, { true })!!
        assertEquals(PrayerName.DHUHR, afterFajr.prayer)
    }
}
