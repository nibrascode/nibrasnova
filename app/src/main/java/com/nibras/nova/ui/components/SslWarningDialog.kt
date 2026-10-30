package com.nibras.nova.ui.components

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nibras.nova.core.security.SslCertificateChallenge
import com.nibras.nova.ui.theme.NovaDangerRed

@Composable
fun SslWarningDialog(
    challenge: SslCertificateChallenge
) {
    AlertDialog(
        onDismissRequest = challenge.onCancelNavigation,
        icon = {
            Icon(
                imageVector = Icons.Default.GppBad,
                contentDescription = L.s(R.string.t_ssl_certificate_error),
                tint = NovaDangerRed
            )
        },
        title = {
            Text(
                text = L.s(R.string.t_connection_is_not_private, challenge.errorTitle),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = challenge.technicalDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = L.s(R.string.t_url, challenge.failingUrl),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = L.s(R.string.t_issued_to, challenge.issuedTo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = L.s(R.string.t_issued_by, challenge.issuedBy),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = challenge.onCancelNavigation) {
                Text(L.s(R.string.t_back_to_safety))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = challenge.onProceedWithExplicitRisk,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaDangerRed)
            ) {
                Text(L.s(R.string.t_proceed_unsafe))
            }
        }
    )
}
