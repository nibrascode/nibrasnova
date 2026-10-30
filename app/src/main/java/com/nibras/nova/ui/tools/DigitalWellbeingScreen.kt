package com.nibras.nova.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.ui.components.EmptyStateView
import com.nibras.nova.ui.theme.NovaDangerRed
import com.nibras.nova.ui.theme.NovaHttpsVerifiedGreen

@Composable
fun DigitalWellbeingScreen(
    onBack: () -> Unit,
    viewModel: DigitalWellbeingViewModel = hiltViewModel()
) {
    val usageList by viewModel.siteUsageList.collectAsStateWithLifecycle()
    var showLimitDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Digital Wellbeing & Focus Timer",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row {
                    IconButton(onClick = { showLimitDialog = true }) {
                        Icon(Icons.Default.AddAlarm, contentDescription = "Set Daily Domain Timer")
                    }
                    IconButton(onClick = viewModel::resetAllUsage) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Usage Analytics")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (usageList.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Timer,
                title = "No Local Browsing Time Recorded Yet",
                subtitle = "Nibras Nova calculates your active reading time per domain 100% locally on-device and lets you set focus timers.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(usageList, key = { it.id }) { stat ->
                    val mins = stat.totalSecondsSpent / 60L
                    val secs = stat.totalSecondsSpent % 60L
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stat.domain,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Active Time: ${mins}m ${secs}s" +
                                        if (stat.dailyLimitMinutes > 0) " • Limit: ${stat.dailyLimitMinutes}m" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (stat.isDailyLimitExceeded) NovaDangerRed else NovaHttpsVerifiedGreen
                                )
                            }
                            if (stat.isDailyLimitExceeded) {
                                Text(
                                    text = "LIMIT REACHED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NovaDangerRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLimitDialog) {
        var domain by remember { mutableStateOf("") }
        var minutesText by remember { mutableStateOf("30") }
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Set Daily Focus Timer for Domain") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Domain (e.g. youtube.com or x.com)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it },
                        label = { Text("Daily Limit in Minutes (0 to remove)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val mins = minutesText.toIntOrNull() ?: 30
                    if (domain.isNotBlank()) {
                        viewModel.setDailyLimitForDomain(domain, mins)
                        showLimitDialog = false
                    }
                }) {
                    Text("Save Focus Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
