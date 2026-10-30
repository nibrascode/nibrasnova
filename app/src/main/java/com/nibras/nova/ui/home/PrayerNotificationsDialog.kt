package com.nibras.nova.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nibras.nova.R
import com.nibras.nova.core.prayer.PrayerAlarms
import com.nibras.nova.core.prayer.PrayerPrefs

/** Ask once for POST_NOTIFICATIONS (Android 13+). */
@Composable
fun rememberNotificationPermissionAsker(onResult: (Boolean) -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult(it); PrayerAlarms.rescheduleAsync(context) }
    return remember {
        {
            if (Build.VERSION.SDK_INT >= 33 && !PrayerAlarms.hasNotificationPermission(context)) {
                PrayerPrefs(context).askedNotifPermission = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun PrayerNotificationsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrayerPrefs(context) }
    var tick by remember { mutableIntStateOf(0) }
    var granted by remember { mutableStateOf(PrayerAlarms.hasNotificationPermission(context)) }
    var exact by remember { mutableStateOf(PrayerAlarms.canExact(context)) }
    val ask = rememberNotificationPermissionAsker { granted = it }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            granted = PrayerAlarms.hasNotificationPermission(context); exact = PrayerAlarms.canExact(context)
            PrayerAlarms.rescheduleAsync(context)
        }
    }
    LaunchedEffect(Unit) { if (prefs.notifyEnabled) ask() }
    fun changed() { tick++; PrayerAlarms.rescheduleAsync(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pn_title)) },
        text = {
            key(tick) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrayerNotificationsBody(
                        enabled = prefs.notifyEnabled, granted = granted, exact = exact,
                        before = { prefs.notifyBefore(it) }, at = { prefs.notifyAt(it) },
                        onEnabled = { prefs.notifyEnabled = it; if (it) ask(); changed() },
                        onBefore = { p, v -> prefs.setNotifyBefore(p, v); changed() },
                        onAt = { p, v -> prefs.setNotifyAt(p, v); changed() },
                        onOpenNotifSettings = {
                            if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotifPermission) ask()
                            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        onAllowExact = {
                            if (Build.VERSION.SDK_INT >= 31) runCatching {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pr_close)) } }
    )
}

@Composable
fun PrayerNotificationsBody(
    enabled: Boolean, granted: Boolean, exact: Boolean,
    before: (com.nibras.nova.core.prayer.PrayerName) -> Boolean, at: (com.nibras.nova.core.prayer.PrayerName) -> Boolean,
    onEnabled: (Boolean) -> Unit,
    onBefore: (com.nibras.nova.core.prayer.PrayerName, Boolean) -> Unit,
    onAt: (com.nibras.nova.core.prayer.PrayerName, Boolean) -> Unit,
    onOpenNotifSettings: () -> Unit, onAllowExact: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.pn_master), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Switch(enabled, onEnabled)
    }
    Text(stringResource(R.string.pn_info), style = MaterialTheme.typography.bodySmall)
    if (enabled && !granted) {
        Text(stringResource(R.string.pn_perm_denied), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onOpenNotifSettings) { Text(stringResource(R.string.pn_open_settings)) }
    }
    if (enabled && !exact) {
        Text(stringResource(R.string.pn_exact_off), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onAllowExact) { Text(stringResource(R.string.pn_allow_exact)) }
    }
    if (enabled) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.pn_before), Modifier.width(72.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.pn_at), Modifier.width(72.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
        PrayerAlarms.NOTIFY_PRAYERS.forEach { p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(prayerLabel(p)), Modifier.weight(1f))
                Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) { Checkbox(before(p), { onBefore(p, it) }) }
                Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) { Checkbox(at(p), { onAt(p, it) }) }
            }
        }
    }
}
