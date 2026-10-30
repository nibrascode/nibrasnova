package com.nibras.nova.domain.model

enum class HistoryClearRange(val label: String, val durationMillis: Long?) {
    LAST_HOUR("Last Hour", 60L * 60L * 1000L),
    LAST_24_HOURS("Last 24 Hours", 24L * 60L * 60L * 1000L),
    LAST_7_DAYS("Last 7 Days", 7L * 24L * 60L * 60L * 1000L),
    LAST_4_WEEKS("Last 4 Weeks", 28L * 24L * 60L * 60L * 1000L),
    ALL_TIME("All Time", null)
}

data class HistoryItem(
    val id: Long = 0L,
    val title: String,
    val url: String,
    val faviconPath: String? = null,
    val visitedAt: Long = System.currentTimeMillis()
)
