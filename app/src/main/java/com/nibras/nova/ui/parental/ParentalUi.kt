package com.nibras.nova.ui.parental

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nibras.nova.R
import com.nibras.nova.core.parental.ParentalControls
import com.nibras.nova.core.parental.PinManager
import com.nibras.nova.core.parental.PinResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PinPromptDialog(title: String, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pc = remember { ParentalControls.get(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val lockedUntil = pc.pin.lockedUntil()
    val scope = rememberCoroutineScope()
    val wrongFmt = stringResource(R.string.pc_wrong_pin)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, null) },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin, onValueChange = { v -> if (v.length <= 6 && v.all(Char::isDigit)) pin = v },
                    label = { Text(stringResource(R.string.pc_pin)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    enabled = lockedUntil <= now && !busy
                )
                if (lockedUntil > now) Text(stringResource(R.string.pc_locked, ((lockedUntil - now) / 1000 + 1).toInt()), color = MaterialTheme.colorScheme.error)
                else error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length >= 4 && lockedUntil <= now && !busy, onClick = {
                busy = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) { pc.pin.verify(pin) }
                    busy = false; pin = ""
                    when (r) {
                        PinResult.Ok, PinResult.NoPin -> onSuccess()
                        is PinResult.Wrong -> error = String.format(wrongFmt, r.attemptsLeft)
                        is PinResult.LockedOut -> error = null
                    }
                }
            }) { Text(stringResource(R.string.pc_unlock)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pr_close)) } }
    )
}

/** Returns a guard: runs the action directly when no PIN is set or already unlocked, else asks for the PIN first. */
@Composable
fun rememberPinGuard(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val pc = remember { ParentalControls.get(context) }
    var unlocked by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    pending?.let { action ->
        PinPromptDialog(stringResource(R.string.pc_enter_pin), onSuccess = { unlocked = true; pending = null; action() }, onDismiss = { pending = null })
    }
    return remember { { action: () -> Unit -> if (!pc.pin.hasPin || unlocked) action() else pending = action } }
}

/** Wipes all app data (the documented PIN recovery path). The app closes afterwards. */
fun resetAllAppData(context: Context) {
    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.clearApplicationUserData()
}

@Composable
fun ParentalControlsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { ParentalControlsContent(onDismiss) }
    }
}

@Composable
fun ParentalControlsContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val pc = remember { ParentalControls.get(context) }
    var hasPin by remember { mutableStateOf(pc.pin.hasPin) }
    var p1 by remember { mutableStateOf("") }; var p2 by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var lockSettings by remember { mutableStateOf(pc.lockSettingsScreen) }
    var blocklist by remember { mutableStateOf(pc.customBlocklist.joinToString("\n")) }
    var limit by remember { mutableIntStateOf(pc.dailyLimitMinutes) }
    var confirmReset by remember { mutableStateOf(false) }
    val guard = rememberPinGuard()
    val savedText = stringResource(R.string.pc_saved); val mismatch = stringResource(R.string.pc_pin_mismatch)
    val scope = rememberCoroutineScope()

    androidx.compose.material3.Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pr_close)) }
            Text(stringResource(R.string.pc_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(stringResource(R.string.pc_intro), style = MaterialTheme.typography.bodySmall)

        Text(stringResource(if (hasPin) R.string.pc_change_pin else R.string.pc_set_pin), fontWeight = FontWeight.SemiBold)
        val pinField: @Composable (String, (String) -> Unit, Int) -> Unit = { v, set, label ->
            OutlinedTextField(v, { t -> if (t.length <= 6 && t.all(Char::isDigit)) set(t) }, label = { Text(stringResource(label)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
        }
        pinField(p1, { p1 = it }, R.string.pc_new_pin); pinField(p2, { p2 = it }, R.string.pc_repeat_pin)
        Button(enabled = PinManager.isValidPin(p1), onClick = {
            if (p1 != p2) { msg = mismatch; return@Button }
            guard { val np = p1; scope.launch { withContext(Dispatchers.Default) { pc.pin.setPin(np) }; hasPin = true; p1 = ""; p2 = ""; msg = savedText } }
        }) { Text(stringResource(R.string.pc_save_pin)) }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (hasPin) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.pc_lock_settings)); Text(stringResource(R.string.pc_lock_settings_info), style = MaterialTheme.typography.bodySmall)
                }
                Switch(lockSettings, { v -> guard { lockSettings = v; pc.lockSettingsScreen = v } })
            }
            HorizontalDivider()
            Text(stringResource(R.string.pc_blocklist), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.pc_blocklist_info), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(blocklist, { blocklist = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), placeholder = { Text("example.com") })
            OutlinedButton(onClick = { guard { pc.customBlocklist = blocklist.split('\n'); blocklist = pc.customBlocklist.joinToString("\n"); msg = savedText } }) { Text(stringResource(R.string.pc_save)) }
            HorizontalDivider()
            Text(stringResource(R.string.pc_limit), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.pc_limit_info), style = MaterialTheme.typography.bodySmall)
            listOf(0, 30, 60, 90, 120, 180).forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = limit == m, onClick = { guard { limit = m; pc.dailyLimitMinutes = m } })
                    Text(if (m == 0) stringResource(R.string.pc_limit_off) else stringResource(R.string.pc_minutes, m))
                }
            }
            HorizontalDivider()
            OutlinedButton(onClick = { guard { pc.pin.clearPin(); pc.lockSettingsScreen = false; pc.dailyLimitMinutes = 0; hasPin = false; lockSettings = false; limit = 0 } }) {
                Text(stringResource(R.string.pc_remove_pin))
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.pc_forgot), fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.pc_forgot_info), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { confirmReset = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text(stringResource(R.string.pc_reset_app))
        }
        Text(stringResource(R.string.pc_always_on), style = MaterialTheme.typography.bodySmall)
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.pc_reset_app)) },
            text = { Text(stringResource(R.string.pc_forgot_info)) },
            confirmButton = { TextButton(onClick = { resetAllAppData(context) }) { Text(stringResource(R.string.pc_reset_confirm), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.pr_close)) } }
        )
    } }
}

/** Counts foreground time; when the daily limit is reached, blocks the app until a parent extends it with the PIN. */
@Composable
fun UsageLimitGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val pc = remember { ParentalControls.get(context) }
    var over by remember { mutableStateOf(pc.isOverLimit()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = System.currentTimeMillis()
            while (true) {
                delay(15_000)
                val n = System.currentTimeMillis()
                if (pc.dailyLimitMinutes > 0) pc.addUsage(n - last)
                last = n
                over = pc.isOverLimit()
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (!over) content()
        if (over) {
            var ask by remember { mutableStateOf(false) }
            Column(
                Modifier.fillMaxSize().background(Color(0xF0070D26)).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("⏳", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.pc_time_up), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.pc_time_up_info), color = Color(0xFFB4BED8), textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { ask = true }) { Text(stringResource(R.string.pc_extend)) }
            }
            if (ask) PinPromptDialog(stringResource(R.string.pc_extend), onSuccess = { pc.extendToday(30); over = pc.isOverLimit(); ask = false }, onDismiss = { ask = false })
        }
    }
}
