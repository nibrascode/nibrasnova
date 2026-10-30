package com.nibras.nova.core.prayer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.batoulapps.adhan2.CalculationMethod
import com.nibras.nova.MainActivity
import com.nibras.nova.R
import com.nibras.nova.core.common.L
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

/** One upcoming notification: [kind] BEFORE fires 10 min before [prayerAtMs], AT fires at it. */
data class PrayerEvent(val prayer: PrayerName, val kind: Kind, val triggerAtMs: Long, val prayerAtMs: Long) {
    enum class Kind { BEFORE, AT }
}

object PrayerAlarms {
    const val LEAD_MS = 10 * 60_000L
    const val CHANNEL = "prayer_times"
    private const val REQ = 4701
    val NOTIFY_PRAYERS = listOf(PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA)

    /** Pure: the earliest enabled event strictly after [nowMs] (searches today and the next 2 days in [zone]). */
    fun nextEvent(
        lat: Double, lng: Double, zone: ZoneId, method: CalculationMethod, nowMs: Long,
        before: (PrayerName) -> Boolean, at: (PrayerName) -> Boolean
    ): PrayerEvent? {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        var best: PrayerEvent? = null
        for (d in 0..2) {
            val day = PrayerEngine.dayTimes(lat, lng, today.plusDays(d.toLong()), method)
            for (p in NOTIFY_PRAYERS) {
                val t = day.times.getValue(p)
                if (before(p) && t - LEAD_MS > nowMs) best = minOf(best, PrayerEvent(p, PrayerEvent.Kind.BEFORE, t - LEAD_MS, t))
                if (at(p) && t > nowMs) best = minOf(best, PrayerEvent(p, PrayerEvent.Kind.AT, t, t))
            }
            if (best != null) return best
        }
        return best
    }

    private fun minOf(a: PrayerEvent?, b: PrayerEvent) = if (a == null || b.triggerAtMs < a.triggerAtMs) b else a

    fun zoneOf(prefs: PrayerPrefs): ZoneId = prefs.tz?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

    fun rescheduleAsync(context: Context) {
        val app = context.applicationContext
        Thread { runCatching { reschedule(app) } }.start()
    }

    /** Cancels and re-arms the single pending alarm for the next enabled event. */
    fun reschedule(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val prefs = PrayerPrefs(app)
        val pi = { e: PrayerEvent? ->
            val i = Intent(app, PrayerAlarmReceiver::class.java).setAction(ACTION_FIRE)
            if (e != null) i.putExtra("p", e.prayer.name).putExtra("k", e.kind.name).putExtra("t", e.prayerAtMs)
            PendingIntent.getBroadcast(app, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        am.cancel(pi(null))
        val lat = prefs.lat; val lng = prefs.lng
        if (!prefs.enabled || !prefs.notifyEnabled || lat == null || lng == null) return
        val e = nextEvent(lat, lng, zoneOf(prefs), PrayerEngine.method(prefs.method), System.currentTimeMillis(),
            prefs::notifyBefore, prefs::notifyAt) ?: return
        val op = pi(e)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, e.triggerAtMs, op)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, e.triggerAtMs, op)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, e.triggerAtMs, op)
        }
    }

    fun canExact(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true)

    fun hasNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, L.s(R.string.pn_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                description = L.s(R.string.pn_channel_desc)
            })
        }
    }

    fun post(context: Context, prayer: PrayerName, kind: PrayerEvent.Kind, prayerAtMs: Long) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)
        val prefs = PrayerPrefs(context)
        val name = L.s(prayerLabel(prayer))
        val time = DateFormat.getTimeInstance(DateFormat.SHORT, context.resources.configuration.locales[0])
            .apply { timeZone = java.util.TimeZone.getTimeZone(zoneOf(prefs)) }.format(Date(prayerAtMs))
        val title = if (kind == PrayerEvent.Kind.BEFORE) L.s(R.string.pn_soon, name) else L.s(R.string.pn_now, name)
        val body = listOfNotNull(prefs.cityName, time).joinToString(" · ")
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_nova_n).setContentTitle(title).setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentIntent(open).setWhen(prayerAtMs).setShowWhen(true).build()
        runCatching { NotificationManagerCompat.from(context).notify(4800 + prayer.ordinal * 2 + kind.ordinal, n) }
    }

    fun prayerLabel(p: PrayerName): Int = when (p) {
        PrayerName.FAJR -> R.string.pr_fajr; PrayerName.SUNRISE -> R.string.pr_sunrise; PrayerName.DHUHR -> R.string.pr_dhuhr
        PrayerName.ASR -> R.string.pr_asr; PrayerName.MAGHRIB -> R.string.pr_maghrib; PrayerName.ISHA -> R.string.pr_isha
    }

    const val ACTION_FIRE = "com.nibras.nova.PRAYER_ALARM"
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                if (intent.action == PrayerAlarms.ACTION_FIRE) {
                    val p = intent.getStringExtra("p")?.let { runCatching { PrayerName.valueOf(it) }.getOrNull() }
                    val k = intent.getStringExtra("k")?.let { runCatching { PrayerEvent.Kind.valueOf(it) }.getOrNull() }
                    val t = intent.getLongExtra("t", 0)
                    // Skip if badly late (e.g. device was off): more than 15 min past the trigger.
                    val trigger = if (k == PrayerEvent.Kind.BEFORE) t - PrayerAlarms.LEAD_MS else t
                    if (p != null && k != null && System.currentTimeMillis() - trigger < 15 * 60_000L) PrayerAlarms.post(context, p, k, t)
                }
                PrayerAlarms.reschedule(context)
            } finally { pending.finish() }
        }.start()
    }
}

/** Re-arms after reboot, app update, time/time-zone change or exact-alarm permission change. */
class PrayerRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread { try { runCatching { PrayerAlarms.reschedule(context) } } finally { pending.finish() } }.start()
    }
}
