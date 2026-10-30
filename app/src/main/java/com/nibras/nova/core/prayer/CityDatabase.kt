package com.nibras.nova.core.prayer

import android.content.Context
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

data class GeoCity(
    val country: String, val name: String, val lat: Double, val lng: Double, val tz: String, val population: Int,
    /** Localized names by language code (az, en, tr, ru, ar); missing = use [name]. */
    val localized: Map<String, String> = emptyMap(),
    val aliases: List<String> = emptyList()
) {
    fun displayName(lang: String): String = localized[lang]?.takeIf { it.isNotBlank() } ?: name
    val searchKeys: List<String> by lazy { (listOf(name) + localized.values + aliases).filter { it.isNotBlank() }.map { CityDatabase.norm(it) } }
}

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
        return list.filter { c -> c.searchKeys.any { it.contains(q) } }
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
        val LANGS = listOf("az", "en", "tr", "ru", "ar")

        fun countryName(cc: String, locale: Locale): String =
            Locale("", cc).getDisplayCountry(locale).ifBlank { cc }

        fun norm(s: String): String = java.text.Normalizer.normalize(s.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace('ı', 'i').replace('ə', 'e').replace('ǝ', 'e')
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ـ', ' ').trim()

        fun parse(stream: InputStream, gzipped: Boolean = true): CityDatabase {
            val input = if (gzipped) GZIPInputStream(stream) else stream
            val out = ArrayList<GeoCity>(35_000)
            var tz: List<String> = emptyList()
            input.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("#tz\t")) { tz = line.split('\t').drop(1); return@forEach }
                    val f = line.split('\t'); if (f.size < 6) return@forEach
                    val loc = HashMap<String, String>(5)
                    LANGS.forEachIndexed { i, l -> f.getOrNull(6 + i)?.takeIf { it.isNotEmpty() }?.let { loc[l] = it } }
                    val al = f.getOrNull(11)?.takeIf { it.isNotEmpty() }?.split('|') ?: emptyList()
                    out.add(GeoCity(f[0], f[1], f[2].toDouble(), f[3].toDouble(), tz.getOrElse(f[4].toInt()) { "UTC" }, f[5].toIntOrNull() ?: 0, loc, al))
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
