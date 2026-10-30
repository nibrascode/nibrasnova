package com.nibras.nova.core.prayer

import android.content.Context
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

data class GeoCity(val country: String, val name: String, val lat: Double, val lng: Double, val tz: String, val population: Int)

/**
 * Offline city list: GeoNames cities15000 (population > 15,000), CC BY 4.0, https://www.geonames.org/.
 * Bundled as assets/cities15000.tsv.gz.
 */
class CityDatabase(val cities: List<GeoCity>) {
    val byCountry: Map<String, List<GeoCity>> = cities.groupBy { it.country }

    fun countries(): List<String> = byCountry.keys.toList()

    fun citiesIn(cc: String, query: String = ""): List<GeoCity> {
        val list = byCountry[cc].orEmpty()
        if (query.isBlank()) return list
        val q = norm(query)
        return list.filter { norm(it.name).contains(q) }
    }

    /** Countries sorted by localized name, with Azerbaijan, Saudi Arabia, Turkey first. */
    fun sortedCountries(locale: Locale, query: String = ""): List<Pair<String, String>> {
        val q = norm(query)
        val all = countries().map { it to countryName(it, locale) }
            .filter { q.isEmpty() || norm(it.second).contains(q) || norm(countryName(it.first, Locale.ENGLISH)).contains(q) || it.first.equals(query.trim(), true) }
        val pinned = PINNED.mapNotNull { p -> all.firstOrNull { it.first == p } }
        val rest = all.filter { it.first !in PINNED }.sortedWith(compareBy(java.text.Collator.getInstance(locale)) { it.second })
        return pinned + rest
    }

    companion object {
        val PINNED = listOf("AZ", "SA", "TR")

        fun countryName(cc: String, locale: Locale): String =
            Locale("", cc).getDisplayCountry(locale).ifBlank { cc }

        fun norm(s: String): String = java.text.Normalizer.normalize(s.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace('ı', 'i').replace('ə', 'e').trim()

        fun parse(stream: InputStream, gzipped: Boolean = true): CityDatabase {
            val input = if (gzipped) GZIPInputStream(stream) else stream
            val out = ArrayList<GeoCity>(35_000)
            var tz: List<String> = emptyList()
            input.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("#tz\t")) { tz = line.split('\t').drop(1); return@forEach }
                    val f = line.split('\t'); if (f.size < 6) return@forEach
                    out.add(GeoCity(f[0], f[1], f[2].toDouble(), f[3].toDouble(), tz.getOrElse(f[4].toInt()) { "UTC" }, f[5].toIntOrNull() ?: 0))
                }
            }
            return CityDatabase(out)
        }

        @Volatile private var cached: CityDatabase? = null
        fun get(context: Context): CityDatabase = cached ?: synchronized(this) {
            cached ?: context.assets.open("cities15000.tsv.gz").use { parse(it) }.also { cached = it }
        }
    }
}
