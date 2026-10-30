package com.nibras.nova.domain.model

import com.nibras.nova.R
import com.nibras.nova.core.common.L

enum class HistoryClearRange(val label: String, val durationMillis: Long?) {
    LAST_HOUR(L.s(R.string.t_last_hour), 60L * 60L * 1000L),
    LAST_24_HOURS(L.s(R.string.t_last_24_hours), 24L * 60L * 60L * 1000L),
    LAST_7_DAYS(L.s(R.string.t_last_7_days), 7L * 24L * 60L * 60L * 1000L),
    LAST_4_WEEKS(L.s(R.string.t_last_4_weeks), 28L * 24L * 60L * 60L * 1000L),
    ALL_TIME(L.s(R.string.t_all_time), null)
}

data class HistoryItem(
    val id: Long = 0L,
    val title: String,
    val url: String,
    val faviconPath: String? = null,
    val visitedAt: Long = System.currentTimeMillis()
)
