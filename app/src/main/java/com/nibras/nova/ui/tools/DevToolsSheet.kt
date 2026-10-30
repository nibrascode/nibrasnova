package com.nibras.nova.ui.tools

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nibras.nova.domain.model.ConsoleLogLevel
import com.nibras.nova.domain.model.DevConsoleEntry
import com.nibras.nova.domain.model.NetworkResourceLog
import com.nibras.nova.ui.theme.NovaDangerRed
import com.nibras.nova.ui.theme.NovaHttpsVerifiedGreen
import com.nibras.nova.ui.theme.NovaWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsSheet(
    consoleLogs: List<DevConsoleEntry>,
    networkLogs: List<NetworkResourceLog>,
    domSourcePreview: String,
    storageSummary: String,
    isDesignModeEnabled: Boolean,
    onExecuteJsRepl: (String) -> Unit,
    onRefreshDomAndStorage: () -> Unit,
    onToggleLiveDomDesignMode: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var jsCommand by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "  " + L.s(R.string.t_nibras_nova_devtools_f12),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onClearLogs) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = L.s(R.string.t_clear_console_network_logs))
                }
            }

            // Tab Strip: Console | Network | DOM & Live Edit | Storage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text(L.s(R.string.t_console, consoleLogs.size)) }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text(L.s(R.string.t_network, networkLogs.size)) }
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onRefreshDomAndStorage()
                    },
                    label = { Text(com.nibras.nova.core.common.L.s(com.nibras.nova.R.string.devtools_dom)) }
                )
                FilterChip(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        onRefreshDomAndStorage()
                    },
                    label = { Text(L.s(R.string.t_storage)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                // 0: Interactive JS Console & REPL
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(consoleLogs) { entry ->
                            val color = when (entry.level) {
                                ConsoleLogLevel.ERROR -> NovaDangerRed
                                ConsoleLogLevel.WARN -> NovaWarningAmber
                                ConsoleLogLevel.INFO -> MaterialTheme.colorScheme.primary
                                ConsoleLogLevel.LOG -> MaterialTheme.colorScheme.onSurface
                            }
                            SelectionContainer {
                                Text(
                                    text = "[${entry.level.name}] ${entry.message} (${entry.sourceId.takeLast(24)}:${entry.lineNumber})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = color
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = jsCommand,
                            onValueChange = { jsCommand = it },
                            placeholder = { Text(com.nibras.nova.core.common.L.s(com.nibras.nova.R.string.devtools_eval_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (jsCommand.isNotBlank()) {
                                    onExecuteJsRepl(jsCommand)
                                    jsCommand = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = L.s(R.string.t_run_js))
                        }
                    }
                }

                // 1: Network Request & AdBlock Monitor
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(networkLogs) { req ->
                            val badgeColor = if (req.wasBlockedByAdBlocker) NovaDangerRed else NovaHttpsVerifiedGreen
                            val statusText = if (req.wasBlockedByAdBlocker) "BLOCKED" else req.method
                            Text(
                                text = "[$statusText] ${req.url}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = badgeColor
                            )
                        }
                    }
                }

                // 2: DOM HTML Source & Live ContentEditable Mode
                2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live DOM Edit (document.designMode)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedButton(onClick = { onToggleLiveDomDesignMode(!isDesignModeEnabled) }) {
                            Text(if (isDesignModeEnabled) L.s(R.string.t_disable_live_edit) else L.s(R.string.t_enable_live_edit))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = domSourcePreview.ifBlank { "Tap 'DOM' to inspect current document.documentElement.outerHTML…" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                    }
                }

                // 3: Cookies, localStorage, and sessionStorage Inspector
                3 -> {
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = storageSummary.ifBlank { L.s(R.string.t_no_storage_entries_found_for_this_origin) },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
