package com.nibras.nova.core.prayer

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale

class CityDatabaseTest {
    private val db by lazy {
        val f = listOf("src/main/assets/cities15000.tsv.gz", "app/src/main/assets/cities15000.tsv.gz").map(::File).first { it.exists() }
        f.inputStream().use { CityDatabase.parse(it) }
    }
    @Test fun coversManyCountries() { assertTrue(db.countries().size > 200) }
    @Test fun pinnedFirst() { assertEquals(listOf("AZ", "SA", "TR"), db.sortedCountries(Locale.ENGLISH).take(3).map { it.first }) }
    @Test fun bakuHasTimezone() {
        val baku = db.citiesIn("AZ", "baki").firstOrNull() ?: db.citiesIn("AZ", "baku").first()
        assertEquals("Asia/Baku", baku.tz); assertEquals(40.4, baku.lat, 0.2)
    }
    @Test fun makkahInSaudi() { assertTrue(db.citiesIn("SA", "mecca").isNotEmpty() || db.citiesIn("SA", "makkah").isNotEmpty()) }
    @Test fun countrySearch() { assertTrue(db.sortedCountries(Locale.ENGLISH, "azer").any { it.first == "AZ" }) }
}
