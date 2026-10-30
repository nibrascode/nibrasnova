package com.nibras.nova.core.prayer

import android.content.Context

/** Prayer settings: kept locally on the device only. */
class PrayerPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("nova_prayer", Context.MODE_PRIVATE)
    var enabled: Boolean get() = sp.getBoolean("enabled", true); set(v) = sp.edit().putBoolean("enabled", v).apply()
    var method: String get() = sp.getString("method2", "UMM_AL_QURA")!!; set(v) = sp.edit().putString("method2", v).apply()
    var useLocation: Boolean get() = sp.getBoolean("use_loc", false); set(v) = sp.edit().putBoolean("use_loc", v).apply()
    var country: String? get() = sp.getString("cc", null); set(v) = sp.edit().putString("cc", v).apply()
    var tz: String? get() = sp.getString("tz", null); set(v) = sp.edit().putString("tz", v).apply()
    var cityName: String? get() = sp.getString("city", null); set(v) = sp.edit().putString("city", v).apply()
    /** Coarse coordinates rounded to ~1 km, only stored on device. */
    var lat: Double? get() = sp.getString("lat", null)?.toDoubleOrNull(); set(v) = sp.edit().putString("lat", v?.let { "%.2f".format(java.util.Locale.US, it) }).apply()
    var lng: Double? get() = sp.getString("lng", null)?.toDoubleOrNull(); set(v) = sp.edit().putString("lng", v?.let { "%.2f".format(java.util.Locale.US, it) }).apply()
}
