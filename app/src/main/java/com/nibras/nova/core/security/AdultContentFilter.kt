package com.nibras.nova.core.security

import android.content.Context
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.GZIPInputStream

/**
 * On-device adult-content filter. Always on, no setting to disable, applies to every tab
 * (private tabs included). Nothing is sent off the device.
 *
 * Domain list: StevenBlack/hosts "porn" extension (MIT; sources incl. hostsVN Adult VN, Snuff hosts,
 * Clefspeare13/pornhosts, all MIT), bundled as assets/adult_domains.txt.gz.
 */
object AdultContentFilter {

    @Volatile private var domains: Set<String> = emptySet()
    @Volatile private var loaded = false
    @Volatile private var custom: Set<String> = emptySet()

    /** Extra domains a parent added (PIN-protected). Matched with subdomains. */
    fun setCustomDomains(d: Set<String>) { custom = d }
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        Thread({ ensureLoaded() }, "adult-filter-load").apply { isDaemon = true }.start()
    }

    /** For tests or custom lists. */
    fun loadFrom(stream: InputStream, gzipped: Boolean = true) {
        val input = if (gzipped) GZIPInputStream(stream) else stream
        val set = HashSet<String>(65_536)
        input.bufferedReader().useLines { lines ->
            lines.forEach { l -> val d = l.trim().lowercase(); if (d.isNotEmpty() && !d.startsWith("#")) set.add(d) }
        }
        domains = set
        loaded = true
    }

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        val ctx = appContext ?: return
        runCatching { com.nibras.nova.core.common.Assets.openText(ctx.assets, "adult_domains.txt").use { loadFrom(it, gzipped = false) } }
    }

    private val HOST_KEYWORDS = listOf(
        "porn", "xxx", "xvideo", "xnxx", "xhamster", "redtube", "youporn", "brazzers", "hentai",
        "onlyfans", "chaturbate", "camsoda", "stripchat", "livejasmin", "bongacams", "fansly",
        "nsfw", "sexcam", "sextube", "sexchat", "freesex", "sexvideo", "milf", "nudes", "rule34", "erotic"
    )
    private val PATH_KEYWORDS = listOf("porn", "xxx", "hentai", "nsfw", "rule34", "xvideos", "xnxx")
    private val TOKEN_SPLIT = Regex("[^a-z0-9]+")

    fun hostFromUrl(url: String): String? {
        val s = url.trim()
        val schemeEnd = s.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = s.substring(0, schemeEnd).lowercase()
        if (scheme != "http" && scheme != "https" && scheme != "ws" && scheme != "wss") return null
        var rest = s.substring(schemeEnd + 3)
        val end = rest.indexOfAny(charArrayOf('/', '?', '#'))
        if (end >= 0) rest = rest.substring(0, end)
        rest = rest.substringAfterLast('@')
        if (rest.startsWith("[")) return null
        return rest.substringBefore(':').trimEnd('.').lowercase().ifEmpty { null }
    }

    fun isHostListed(host: String): Boolean {
        ensureLoaded()
        var h = host.lowercase().trimEnd('.')
        val set = domains
        val extra = custom
        while (true) {
            if (h in set || h in extra) return true
            val dot = h.indexOf('.')
            if (dot < 0 || h.indexOf('.', dot + 1) < 0) return false // stop at registrable "x.tld"
            h = h.substring(dot + 1)
        }
    }

    fun hostLooksAdult(host: String): Boolean {
        val h = host.lowercase()
        return HOST_KEYWORDS.any { h.contains(it) }
    }

    fun pathLooksAdult(url: String): Boolean {
        val afterHost = url.substringAfter("://").substringAfter('/', "")
        if (afterHost.isEmpty()) return false
        val decoded = runCatching { URLDecoder.decode(afterHost, "UTF-8") }.getOrDefault(afterHost).lowercase()
        val tokens = decoded.split(TOKEN_SPLIT)
        return tokens.any { t -> PATH_KEYWORDS.any { k -> t == k || (k.length >= 5 && t.startsWith(k)) } }
    }

    /** True when this URL must not load (page or subresource). */
    fun shouldBlock(url: String): Boolean {
        val host = hostFromUrl(url) ?: return false
        if (isSearchEngine(host)) return false // search pages are made safe via SafeSearch instead
        return hostLooksAdult(host) || isHostListed(host) || pathLooksAdult(url)
    }

    private fun isSearchEngine(host: String): Boolean =
        host.contains("google.") || host.endsWith("bing.com") || host.endsWith("duckduckgo.com") ||
            host.endsWith("youtube.com") || host == "youtu.be"

    /** Returns a SafeSearch-enforced version of a search URL, or null if no change is needed. */
    fun enforceSafeSearch(url: String): String? {
        val host = hostFromUrl(url) ?: return null
        val path = url.substringAfter("://").substringAfter('/', "").substringBefore('?').substringBefore('#')
        val (param, value) = when {
            Regex("""(^|\.)google\.[a-z.]+$""").containsMatchIn(host) &&
                (path.startsWith("search") || path.startsWith("images") || path.startsWith("webhp") || path.isEmpty()) &&
                url.contains("q=") -> "safe" to "active"
            (host == "bing.com" || host.endsWith(".bing.com")) &&
                (path.startsWith("search") || path.startsWith("images") || path.startsWith("videos")) -> "adlt" to "strict"
            (host == "duckduckgo.com" || host.endsWith(".duckduckgo.com")) && url.contains("q=") -> "kp" to "1"
            else -> return null
        }
        return setQueryParam(url, param, value)
    }

    fun isYouTube(host: String): Boolean = host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"

    internal fun setQueryParam(url: String, key: String, value: String): String? {
        val hashIdx = url.indexOf('#')
        val base = if (hashIdx >= 0) url.substring(0, hashIdx) else url
        val frag = if (hashIdx >= 0) url.substring(hashIdx) else ""
        val qIdx = base.indexOf('?')
        val path = if (qIdx >= 0) base.substring(0, qIdx) else base
        val params = if (qIdx >= 0) base.substring(qIdx + 1).split('&').filter { it.isNotEmpty() } else emptyList()
        val existing = params.firstOrNull { it.substringBefore('=') == key }
        if (existing != null && existing.substringAfter('=', "") == value) return null
        val kept = params.filterNot { it.substringBefore('=') == key } + "$key=$value"
        return path + "?" + kept.joinToString("&") + frag
    }
}
