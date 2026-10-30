package com.nibras.nova.core.parental

import android.content.Context
import com.nibras.nova.core.security.AdultContentFilter
import java.time.LocalDate

/** Parental controls state, kept in app-private storage on the device only. */
class ParentalControls private constructor(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("nova_parental", Context.MODE_PRIVATE)
    private val kv = object : KvStore {
        override fun getString(k: String) = sp.getString(k, null)
        override fun putString(k: String, v: String?) { sp.edit().apply { if (v == null) remove(k) else putString(k, v) }.apply() }
        override fun getLong(k: String, d: Long) = sp.getLong(k, d)
        override fun putLong(k: String, v: Long) { sp.edit().putLong(k, v).apply() }
    }
    val pin = PinManager(kv)

    var lockSettingsScreen: Boolean get() = sp.getBoolean("lock_settings", false); set(v) = sp.edit().putBoolean("lock_settings", v).apply()
    var customBlocklist: List<String>
        get() = sp.getString("blocklist", "").orEmpty().split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        set(v) { sp.edit().putString("blocklist", v.joinToString("\n")).apply(); applyBlocklist() }
    /** 0 = off. */
    var dailyLimitMinutes: Int get() = sp.getInt("limit_min", 0); set(v) = sp.edit().putInt("limit_min", v).apply()

    fun applyBlocklist() = AdultContentFilter.setCustomDomains(customBlocklist.mapNotNull { normalizeDomain(it) }.toSet())

    // ---- daily usage ----
    private fun today() = LocalDate.now().toString()
    fun usedTodayMs(): Long = if (sp.getString("use_day", null) == today()) sp.getLong("use_ms", 0) else 0
    fun addUsage(ms: Long) { sp.edit().putString("use_day", today()).putLong("use_ms", usedTodayMs() + ms).apply() }
    fun extraTodayMs(): Long = if (sp.getString("extra_day", null) == today()) sp.getLong("extra_ms", 0) else 0
    fun extendToday(minutes: Int) { sp.edit().putString("extra_day", today()).putLong("extra_ms", extraTodayMs() + minutes * 60_000L).apply() }
    fun isOverLimit(): Boolean = dailyLimitMinutes > 0 && usedTodayMs() >= dailyLimitMinutes * 60_000L + extraTodayMs()

    companion object {
        @Volatile private var inst: ParentalControls? = null
        fun get(context: Context): ParentalControls = inst ?: synchronized(this) { inst ?: ParentalControls(context).also { inst = it; it.applyBlocklist() } }

        fun normalizeDomain(raw: String): String? {
            var s = raw.trim().lowercase()
            if (s.isEmpty() || s.startsWith("#")) return null
            s = s.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore(':').removePrefix("*.").removePrefix("www.").trimEnd('.')
            return s.takeIf { it.contains('.') && it.all { c -> c.isLetterOrDigit() || c == '.' || c == '-' } }
        }
    }
}
