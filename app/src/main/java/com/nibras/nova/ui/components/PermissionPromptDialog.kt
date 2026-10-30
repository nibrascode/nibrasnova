package com.nibras.nova.ui.components

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
                contentDescription = "Website Permission Request",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = "Website Permission Request")
        },
        text = {
            Text(
                text = "${prompt.origin} is requesting access to: $permissionsList.

Granting access allows this site to use the requested hardware or media capability during your session.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = prompt.onGrant) {
                Text("Allow")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = prompt.onDeny) {
                Text("Block")
            }
        }
    )
}
