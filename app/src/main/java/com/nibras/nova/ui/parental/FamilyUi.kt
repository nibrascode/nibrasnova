package com.nibras.nova.ui.parental

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nibras.nova.R
import com.nibras.nova.core.parental.FamilyProfile
import com.nibras.nova.core.parental.ParentalControls

/** Switch between Adult and Child. Child -> Adult needs the parent PIN; a PIN must exist before Child is used. */
@Composable
fun ProfileSwitchDialog(onDismiss: () -> Unit, onSetPin: () -> Unit) {
    val context = LocalContext.current
    val pc = remember { ParentalControls.get(context) }
    val child = FamilyProfile.isChild
    var askPin by remember { mutableStateOf(false) }
    val activity = context as? Activity
    if (askPin) {
        PinPromptDialog(stringResource(R.string.fp_to_adult), onSuccess = { activity?.let { FamilyProfile.switchTo(it, FamilyProfile.ADULT) } }, onDismiss = { askPin = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fp_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.fp_current, stringResource(if (child) R.string.fp_child else R.string.fp_adult)))
                Text(stringResource(if (child) R.string.fp_child_info else R.string.fp_info), style = MaterialTheme.typography.bodySmall)
                if (!child && !pc.pin.hasPin) Text(stringResource(R.string.fp_need_pin), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    child -> askPin = true
                    !pc.pin.hasPin -> { onDismiss(); onSetPin() }
                    else -> activity?.let { FamilyProfile.switchTo(it, FamilyProfile.CHILD) }
                }
            }) { Text(stringResource(if (child) R.string.fp_to_adult else if (pc.pin.hasPin) R.string.fp_to_child else R.string.pc_set_pin)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pr_close)) } }
    )
}

/** Section inside Parental controls: profile switch + Child allowlist editor. */
@Composable
fun FamilySection(guard: (() -> Unit) -> Unit, savedText: String, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val pc = remember { ParentalControls.get(context) }
    var allow by remember { mutableStateOf(pc.childAllowlist.joinToString("\n")) }
    var showSwitch by remember { mutableStateOf(false) }
    Text(stringResource(R.string.fp_title), style = MaterialTheme.typography.titleSmall)
    Text(stringResource(R.string.fp_current, stringResource(if (FamilyProfile.isChild) R.string.fp_child else R.string.fp_adult)), style = MaterialTheme.typography.bodyMedium)
    OutlinedButton(onClick = { showSwitch = true }) {
        Text(stringResource(if (FamilyProfile.isChild) R.string.fp_to_adult else R.string.fp_to_child))
    }
    Text(stringResource(R.string.fp_allow), style = MaterialTheme.typography.titleSmall)
    Text(stringResource(R.string.fp_allow_info), style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(allow, { allow = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), placeholder = { Text("quran.com") })
    OutlinedButton(onClick = { guard { pc.childAllowlist = allow.split('\n'); allow = pc.childAllowlist.joinToString("\n"); onSaved(savedText) } }) { Text(stringResource(R.string.pc_save)) }
    if (showSwitch) ProfileSwitchDialog(onDismiss = { showSwitch = false }, onSetPin = {})
}
