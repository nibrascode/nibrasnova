package com.nibras.nova.ui.components

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nibras.nova.engine.WebsitePermissionPrompt

@Composable
fun PermissionPromptDialog(
    prompt: WebsitePermissionPrompt
) {
    val permissionsList = prompt.permissionTypes.joinToString(", ") { it.displayName }

    AlertDialog(
        onDismissRequest = prompt.onDeny,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = L.s(R.string.t_website_permission_request),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = L.s(R.string.t_website_permission_request))
        },
        text = {
            Text(
                text = L.s(R.string.t_is_requesting_access_to_n_ngranting_acce, prompt.origin, permissionsList),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = prompt.onGrant) {
                Text(L.s(R.string.t_allow))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = prompt.onDeny) {
                Text(L.s(R.string.t_block))
            }
        }
    )
}
