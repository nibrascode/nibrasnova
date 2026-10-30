package com.nibras.nova.core.parental

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * "Adult" and "Child" profiles on one device. Each profile gets its own Room database
 * (history, bookmarks, tabs, downloads list) and, on Android 9+, its own WebView data
 * directory (cookies, local storage, cache) via WebView.setDataDirectorySuffix.
 * Switching restarts the app process so the separation is complete.
 */
object FamilyProfile {
    const val ADULT = "adult"
    const val CHILD = "child"
    private const val PREFS = "nova_profile"

    @Volatile var isChild: Boolean = false
        private set

    fun init(context: Context) {
        isChild = context.getSharedPreferences(PREFS, 0).getString("active", ADULT) == CHILD
    }

    fun dbName(base: String): String = if (isChild) "child_$base" else base

    /** WebView data-dir suffix for this process, or null for the default directory. */
    fun webViewSuffix(processSuffix: String?): String? = when {
        processSuffix != null && isChild -> processSuffix + "_child"
        processSuffix != null -> processSuffix
        isChild -> "child"
        else -> null
    }

    val separatesWebData: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /** Persists the new profile and restarts the app. */
    fun switchTo(activity: Activity, profile: String) {
        activity.getSharedPreferences(PREFS, 0).edit().putString("active", profile).commit()
        if (!separatesWebData) {
            // Android 8.x: one shared WebView data dir, so clear web data when switching.
            runCatching { CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush() }
            runCatching { WebStorage.getInstance().deleteAllData() }
            runCatching { WebView(activity).apply { clearCache(true); destroy() } }
        }
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    val DEFAULT_CHILD_ALLOW = listOf(
        "nibrascode.com", "quran.com", "tanzil.net", "quranenc.com", "alquran.cloud",
        "everyayah.com", "quranicaudio.com", "corpus.quran.com"
    )

    fun isAllowed(host: String?, allow: Collection<String>): Boolean {
        val h = host?.lowercase()?.trimEnd('.')?.removePrefix("www.") ?: return false
        return allow.any { d -> h == d || h.endsWith(".$d") }
    }
}
