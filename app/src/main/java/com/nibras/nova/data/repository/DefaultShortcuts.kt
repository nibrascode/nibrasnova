package com.nibras.nova.data.repository

/**
 * Home quick links. Nibrascode.com is pinned separately in the UI (always shown, not removable),
 * so the home screen shows exactly 4: YouTube, GitHub, Vercel + Nibrascode.com.
 */
object DefaultShortcuts {
    val CURRENT: List<Triple<String, String, String>> = listOf(
        Triple("YouTube", "https://www.youtube.com", "#FF0033"),
        Triple("GitHub", "https://github.com", "#94A3B8"),
        Triple("Vercel", "https://vercel.com", "#FFFFFF")
    )

    /** Defaults seeded by versions up to 1.0.0. */
    val LEGACY: List<Pair<String, String>> = listOf(
        "DuckDuckGo" to "https://duckduckgo.com",
        "Wikipedia" to "https://www.wikipedia.org",
        "GitHub" to "https://github.com",
        "Android Dev" to "https://developer.android.com",
        "Kotlin" to "https://kotlinlang.org",
        "MDN Web Docs" to "https://developer.mozilla.org",
        "Hacker News" to "https://news.ycombinator.com",
        "Ars Technica" to "https://arstechnica.com"
    )

    /** True only if the user never added, removed, renamed or reordered a link. */
    fun isUntouchedLegacy(current: List<Pair<String, String>>): Boolean = current == LEGACY
}
