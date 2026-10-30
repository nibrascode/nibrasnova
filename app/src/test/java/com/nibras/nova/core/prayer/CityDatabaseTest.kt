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

    @Test fun azerbaijaniNames() {
        for (q in listOf("Bakı", "Baki", "baku", "Gəncə", "gence", "Ganja")) assertTrue(q, db.citiesIn("AZ", q).isNotEmpty())
        assertEquals("Bakı", db.citiesIn("AZ", "baku").first().displayName("az"))
    }
    @Test fun makkahLocalized() {
        assertTrue(db.citiesIn("SA", "Məkkə").any { it.name == "Makkah" })
        assertTrue(db.citiesIn("SA", "مكة").any { it.name == "Makkah" })
        assertTrue(db.citiesIn("SA", "Мекка").any { it.name == "Makkah" })
    }
    @Test fun foldingMatchesTurkishLetters() { assertTrue(db.citiesIn("TR", "istanbul").isNotEmpty()); assertTrue(db.citiesIn("TR", "İzmir").isNotEmpty()) }
}
