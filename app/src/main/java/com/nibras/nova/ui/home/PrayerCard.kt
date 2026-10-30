package com.nibras.nova.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nibras.nova.R
import com.nibras.nova.core.prayer.CityDatabase
import com.nibras.nova.core.prayer.GeoCity
import com.nibras.nova.core.prayer.PrayerEngine
import com.nibras.nova.core.prayer.PrayerName
import com.nibras.nova.core.prayer.PrayerPrefs
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun prayerLabel(p: PrayerName): Int = when (p) {
    PrayerName.FAJR -> R.string.pr_fajr; PrayerName.SUNRISE -> R.string.pr_sunrise; PrayerName.DHUHR -> R.string.pr_dhuhr
    PrayerName.ASR -> R.string.pr_asr; PrayerName.MAGHRIB -> R.string.pr_maghrib; PrayerName.ISHA -> R.string.pr_isha
}

fun methodLabel(context: Context, name: String): String {
    val id = context.resources.getIdentifier("m_$name", "string", context.packageName)
    return if (id != 0) context.getString(id) else name
}

/** Hijri date via ICU (on device). */
fun hijriDate(locale: Locale): String = runCatching {
    val cal = android.icu.util.IslamicCalendar()
    cal.calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
    val fmt = android.icu.text.DateFormat.getDateInstance(cal, android.icu.text.DateFormat.LONG, android.icu.util.ULocale.forLocale(locale))
    fmt.format(cal.time)
}.getOrDefault("")

@SuppressLint("MissingPermission")
fun coarseLocation(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        .maxByOrNull { it.time } ?: return null
    return loc.latitude to loc.longitude
}

@Composable
fun PrayerCard(modifier: Modifier = Modifier, onQibla: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { PrayerPrefs(context) }
    var version by remember { mutableIntStateOf(0) }
    var showSetup by remember { mutableStateOf(false) }
    var showNotif by remember { mutableStateOf(false) }
    val askNotif = rememberNotificationPermissionAsker()
    LaunchedEffect(version) { com.nibras.nova.core.prayer.PrayerAlarms.rescheduleAsync(context) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    if (!prefs.enabled) return

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val c = coarseLocation(context)
            if (c != null) { prefs.lat = c.first; prefs.lng = c.second; prefs.useLocation = true; prefs.cityName = null; prefs.tz = null; prefs.country = null }
            else android.widget.Toast.makeText(context, context.getString(R.string.pr_no_location), android.widget.Toast.LENGTH_SHORT).show()
        }
        version++
    }

    val coords = remember(version) {
        if (prefs.useLocation) coarseLocation(context)?.also { prefs.lat = it.first; prefs.lng = it.second }
        prefs.lat?.let { la -> prefs.lng?.let { lo -> la to lo } }
    }
    val locale = context.resources.configuration.locales[0]
    val hijri = remember(locale, now / 3_600_000) { hijriDate(locale) }

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0x332A4BD8), Color(0x339B5CFF))))
            .border(1.dp, Color(0x33A8C4FF), RoundedCornerShape(18.dp))
            .clickable { showSetup = true }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.NightsStay, null, tint = Color(0xFFFFD27A), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (coords == null) {
                Text(stringResource(R.string.pr_setup), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (hijri.isNotEmpty()) Text(hijri, color = Color(0xFFB4BED8), fontSize = 12.sp)
            } else {
                val zone = remember(version) { prefs.tz?.let { runCatching { java.time.ZoneId.of(it) }.getOrNull() } ?: java.time.ZoneId.systemDefault() }
                val next = remember(coords, now / 30_000, version) {
                    PrayerEngine.next(coords.first, coords.second, now, zone, PrayerEngine.method(prefs.method))
                }
                val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).apply { timeZone = java.util.TimeZone.getTimeZone(zone) }.format(Date(next.atEpochMs))
                Text(stringResource(R.string.pr_next, stringResource(prayerLabel(next.prayer)), time), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    listOfNotNull(hijri.ifEmpty { null }, prefs.cityName ?: stringResource(R.string.pr_location)).joinToString(" · "),
                    color = Color(0xFFB4BED8), fontSize = 12.sp, maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.pr_in, PrayerEngine.formatCountdown(next.atEpochMs - now)), color = Color(0xFFFFD27A), fontSize = 13.sp)
            }
        }
        IconButton(onClick = { showNotif = true }) { Icon(Icons.Default.NotificationsActive, stringResource(R.string.pn_title), tint = Color(0xFFFFD27A)) }
        IconButton(onClick = onQibla) { Icon(Icons.Default.Explore, stringResource(R.string.qb_title), tint = Color(0xFFFFD27A)) }
    }

    if (showNotif) PrayerNotificationsDialog(onDismiss = { showNotif = false })
    if (showSetup) {
        CityPickerDialog(
            onDismiss = { showSetup = false },
            onUseLocation = { permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION); showSetup = false },
            onPick = { c ->
                prefs.useLocation = false; prefs.cityName = c.displayName(context.resources.configuration.locales[0].language); prefs.lat = c.lat; prefs.lng = c.lng
                prefs.tz = c.tz; prefs.country = c.country
                version++; showSetup = false
                if (!prefs.askedNotifPermission && prefs.notifyEnabled) askNotif()
            },
            selectedCity = prefs.cityName,
            initialCountry = prefs.country
        )
    }
}

@Composable
private fun CityPickerDialog(
    onDismiss: () -> Unit,
    onUseLocation: () -> Unit,
    onPick: (GeoCity) -> Unit,
    selectedCity: String?,
    initialCountry: String?
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    var db by remember { mutableStateOf<CityDatabase?>(null) }
    LaunchedEffect(Unit) { db = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CityDatabase.get(context) } }
    var country by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pr_close)) } },
        dismissButton = if (country != null) ({ TextButton(onClick = { country = null; query = "" }) { Text(stringResource(R.string.pr_back_countries)) } }) else null,
        title = { Text(if (country == null) stringResource(R.string.pr_choose_country) else CityDatabase.countryName(country!!, locale)) },
        text = {
            Column {
                if (country == null) {
                    Text(stringResource(R.string.pr_setup_hint), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onUseLocation, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.pr_use_location))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text(stringResource(if (country == null) R.string.pr_search_country else R.string.pr_search_city)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                val d = db
                if (d == null) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (country == null) {
                    val list = remember(d, query, locale) { d.sortedCountries(locale, query) }
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(list, key = { it.first }) { (cc, name) ->
                            Text(
                                name,
                                modifier = Modifier.fillMaxWidth().clickable { country = cc; query = "" }.padding(vertical = 10.dp),
                                fontWeight = if (cc == initialCountry || cc in CityDatabase.PINNED) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                } else {
                    val list = remember(d, country, query) { d.citiesIn(country!!, query) }
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(list, key = { it.name + it.lat + it.lng }) { c ->
                            Text(
                                c.displayName(locale.language),
                                modifier = Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 10.dp),
                                fontWeight = if (c.displayName(locale.language) == selectedCity) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    )
}
