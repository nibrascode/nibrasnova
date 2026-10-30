package com.nibras.nova.ui.tools

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.ui.components.EmptyStateView
import com.nibras.nova.ui.theme.NovaHttpsVerifiedGreen
import com.nibras.nova.ui.theme.NovaWarningAmber

@Composable
fun PageMonitorScreen(
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit,
    viewModel: PageMonitorViewModel = hiltViewModel()
) {
    val watchedPages by viewModel.watchedPages.collectAsStateWithLifecycle()
    val isChecking by viewModel.isChecking.collectAsStateWithLifecycle()
    val statusSummary by viewModel.lastCheckSummary.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L.s(R.string.t_back))
                    }
                    Text(
                        text = L.s(R.string.t_web_page_change_monitor),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row {
                        IconButton(onClick = viewModel::checkAllNow) {
                            Icon(Icons.Default.Refresh, contentDescription = L.s(R.string.t_check_all_pages_for_changes))
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = L.s(R.string.t_add_page_to_watch))
                        }
                    }
                }
                if (isChecking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                statusSummary?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (watchedPages.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.TrackChanges,
                title = L.s(R.string.t_no_pages_monitored_yet),
                subtitle = L.s(R.string.t_track_product_prices_university_announce),
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
                items(watchedPages, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openWatchedPage(item.url, onReady = onOpenBrowser)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (item.hasChangedSinceLastCheck) L.s(R.string.t_updated, item.title) else L.s(R.string.t_unchanged, item.title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.hasChangedSinceLastCheck) NovaWarningAmber else NovaHttpsVerifiedGreen
                                )
                                Text(
                                    text = item.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.lastSnippetPreview.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.lastSnippetPreview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteWatchedPage(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_remove_watcher))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(L.s(R.string.t_monitor_webpage_for_changes)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(L.s(R.string.t_label_e_g_exam_results_product_page)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(L.s(R.string.t_target_webpage_url)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (url.isNotBlank()) {
                        viewModel.addWatchedPage(title, url)
                        showAddDialog = false
                    }
                }) {
                    Text(L.s(R.string.t_start_monitoring))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(L.s(R.string.t_cancel))
                }
            }
        )
    }
}
