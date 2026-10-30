package com.nibras.nova.ui.tools

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nibras.nova.core.util.LanSyncEndpoint

@Composable
fun LanSyncDialog(
    endpoint: LanSyncEndpoint,
    onStopAndDismiss: () -> Unit
) {
    val qrBitmap = remember(endpoint.url) {
        generateQrCodeBitmap(endpoint.url, 512)
    }

    AlertDialog(
        onDismissRequest = onStopAndDismiss,
        title = {
            Text(L.s(R.string.t_peer_to_peer_wi_fi_lan_sync_active))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = L.s(R.string.t_scan_this_qr_code_with_another_phone_or),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                qrBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = L.s(R.string.t_lan_sync_qr_code),
                        modifier = Modifier.size(210.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        text = endpoint.url,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onStopAndDismiss) {
                Text(L.s(R.string.t_stop_server_close))
            }
        }
    )
}
