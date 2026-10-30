package com.nibras.nova.core.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.LocalDate
import java.time.ZoneId

data class City(val name: String, val lat: Double, val lng: Double)

/** Manual fallback when location is not granted. */
val PRAYER_CITIES = listOf(
    City("Bakı", 40.4093, 49.8671), City("Gəncə", 40.6828, 46.3606), City("Sumqayıt", 40.5897, 49.6686),
    City("Lənkəran", 38.7529, 48.8475), City("Şəki", 41.1919, 47.1706), City("Naxçıvan", 39.2089, 45.4122),
    City("Mingəçevir", 40.7703, 47.0496), City("Quba", 41.3611, 48.5134), City("Şamaxı", 40.6303, 48.6414),
    City("İstanbul", 41.0082, 28.9784), City("Ankara", 39.9334, 32.8597), City("İzmir", 38.4237, 27.1428),
    City("Moskva", 55.7558, 37.6173), City("Kazan", 55.7963, 49.1088), City("Mahachkala", 42.9849, 47.5047),
    City("Makkah", 21.3891, 39.8579), City("Madinah", 24.5247, 39.5692), City("Riyadh", 24.7136, 46.6753),
    City("Dubai", 25.2048, 55.2708), City("Doha", 25.2854, 51.5310), City("Kuwait", 29.3759, 47.9774),
    City("Cairo", 30.0444, 31.2357), City("Amman", 31.9539, 35.9106), City("Baghdad", 33.3152, 44.3661),
    City("Tehran", 35.6892, 51.3890), City("Tbilisi", 41.7151, 44.8271), City("London", 51.5072, -0.1276),
    City("Berlin", 52.5200, 13.4050), City("Paris", 48.8566, 2.3522), City("New York", 40.7128, -74.0060),
    City("Tashkent", 41.2995, 69.2401), City("Almaty", 43.2220, 76.8512), City("Kuala Lumpur", 3.1390, 101.6869),
    City("Jakarta", -6.2088, 106.8456), City("Karachi", 24.8607, 67.0011),
)

enum class PrayerName { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

data class NextPrayer(val prayer: PrayerName, val atEpochMs: Long)

data class DayTimes(val times: Map<PrayerName, Long>)

object PrayerEngine {
    val METHODS: List<CalculationMethod> = listOf(
        CalculationMethod.MUSLIM_WORLD_LEAGUE, CalculationMethod.TURKEY, CalculationMethod.UMM_AL_QURA,
        CalculationMethod.EGYPTIAN, CalculationMethod.KARACHI, CalculationMethod.DUBAI, CalculationMethod.KUWAIT,
        CalculationMethod.QATAR, CalculationMethod.NORTH_AMERICA, CalculationMethod.MOON_SIGHTING_COMMITTEE,
        CalculationMethod.SINGAPORE,
    )

    fun method(id: String?): CalculationMethod =
        METHODS.firstOrNull { it.name == id } ?: CalculationMethod.MUSLIM_WORLD_LEAGUE

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
