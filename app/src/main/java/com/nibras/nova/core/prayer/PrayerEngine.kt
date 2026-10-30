package com.nibras.nova.core.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.LocalDate
import java.time.ZoneId


enum class PrayerName { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

data class NextPrayer(val prayer: PrayerName, val atEpochMs: Long)

data class DayTimes(val times: Map<PrayerName, Long>)

object PrayerEngine {
    val METHODS: List<CalculationMethod> = listOf(
        CalculationMethod.UMM_AL_QURA, CalculationMethod.MUSLIM_WORLD_LEAGUE, CalculationMethod.TURKEY,
        CalculationMethod.EGYPTIAN, CalculationMethod.KARACHI, CalculationMethod.DUBAI, CalculationMethod.KUWAIT,
        CalculationMethod.QATAR, CalculationMethod.NORTH_AMERICA, CalculationMethod.MOON_SIGHTING_COMMITTEE,
        CalculationMethod.SINGAPORE,
    )

    fun method(id: String?): CalculationMethod =
        METHODS.firstOrNull { it.name == id } ?: CalculationMethod.UMM_AL_QURA

    fun dayTimes(lat: Double, lng: Double, date: LocalDate, method: CalculationMethod): DayTimes {
        val pt = PrayerTimes(Coordinates(lat, lng), DateComponents(date.year, date.monthValue, date.dayOfMonth), method.parameters)
        return DayTimes(
            mapOf(
                PrayerName.FAJR to pt.fajr.toEpochMilliseconds(), PrayerName.SUNRISE to pt.sunrise.toEpochMilliseconds(),
                PrayerName.DHUHR to pt.dhuhr.toEpochMilliseconds(), PrayerName.ASR to pt.asr.toEpochMilliseconds(),
                PrayerName.MAGHRIB to pt.maghrib.toEpochMilliseconds(), PrayerName.ISHA to pt.isha.toEpochMilliseconds(),
            )
        )
    }

    /** Next prayer after [nowMs] (rolls over to tomorrow's Fajr after Isha). */
    fun next(lat: Double, lng: Double, nowMs: Long, zone: ZoneId, method: CalculationMethod): NextPrayer {
        val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        for (d in 0..1) {
            val day = dayTimes(lat, lng, today.plusDays(d.toLong()), method)
            PrayerName.entries.forEach { p -> val t = day.times.getValue(p); if (t > nowMs) return NextPrayer(p, t) }
        }
        return NextPrayer(PrayerName.FAJR, nowMs)
    }

    fun formatCountdown(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

}
