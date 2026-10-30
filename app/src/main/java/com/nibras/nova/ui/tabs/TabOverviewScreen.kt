package com.nibras.nova.ui.tabs

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.model.TabWorkspace
import com.nibras.nova.ui.components.EmptyStateView
import com.nibras.nova.ui.theme.NovaIncognitoAccent
import com.nibras.nova.ui.theme.NovaTurquoise
import java.io.File

@Composable
fun TabOverviewScreen(
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenBrowser: () -> Unit,
    viewModel: TabOverviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var viewingPrivateTabs by remember(state.activeTab?.isPrivate) {
        mutableStateOf(state.activeTab?.isPrivate == true)
    }
    var showSnapshotsDialog by remember { mutableStateOf(false) }

    val filteredNormalTabs = state.normalTabs.filter { it.workspace == state.selectedWorkspace }
    val displayedTabs = if (viewingPrivateTabs) state.privateTabs else filteredNormalTabs
    val accentColor = if (viewingPrivateTabs) NovaIncognitoAccent else NovaTurquoise

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L.s(R.string.t_back))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !viewingPrivateTabs,
                            onClick = { viewingPrivateTabs = false },
                            label = { Text(L.s(R.string.t_normal, state.normalTabs.size)) },
                            leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = viewingPrivateTabs,
                            onClick = { viewingPrivateTabs = true },
                            label = { Text(L.s(R.string.t_private, state.privateTabs.size)) },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Row {
                        IconButton(onClick = { showSnapshotsDialog = true }) {
                            Icon(Icons.Default.Bookmarks, contentDescription = L.s(R.string.t_tab_session_snapshots))
                        }
                        IconButton(
                            onClick = { viewModel.closeAllTabs(viewingPrivateTabs) },
                            enabled = displayedTabs.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = L.s(R.string.t_close_all_tabs))
                        }
                    }
                }

                if (!viewingPrivateTabs) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TabWorkspace.entries) { ws ->
                            val countInWs = state.normalTabs.count { it.workspace == ws }
                            FilterChip(
                                selected = state.selectedWorkspace == ws,
                                onClick = { viewModel.selectWorkspace(ws) },
                                label = { Text("${ws.displayName} ($countInWs)") }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!viewingPrivateTabs && state.recentlyClosedCount > 0) {
                    OutlinedButton(
                        onClick = {
                            viewModel.reopenRecentlyClosedTab { isHome ->
                                if (isHome) onOpenHome() else onOpenBrowser()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Text(" " + L.s(R.string.t_reopen, state.recentlyClosedCount))
                    }
                }

                Button(
                    onClick = {
                        viewModel.createNewTab(isPrivate = viewingPrivateTabs) {
                            onOpenHome()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        if (viewingPrivateTabs) " " + L.s(R.string.t_new_private_tab)
                        else " " + L.s(R.string.t_new_tab_2, state.selectedWorkspace.displayName)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (viewingPrivateTabs && !state.isPrivateVaultUnlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = L.s(R.string.t_biometric_vault_locked),
                    tint = NovaIncognitoAccent,
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = L.s(R.string.t_private_tabs_vault_locked),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L.s(R.string.t_biometric_protection_is_active_for_your),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            viewModel.biometricLockHelper.authenticateIncognitoVault(
                                activity = activity,
                                onSuccess = { viewModel.markPrivateVaultUnlocked() },
                                onError = { }
                            )
                        } else {
                            viewModel.markPrivateVaultUnlocked()
                        }
                    }
                ) {
                    Text(L.s(R.string.t_unlock_with_biometrics_pin))
                }
            }
            return@Scaffold
        }

        if (displayedTabs.isEmpty()) {
            EmptyStateView(
                icon = if (viewingPrivateTabs) Icons.Default.VisibilityOff else Icons.Default.Tab,
                title = if (viewingPrivateTabs) L.s(R.string.t_no_private_tabs_open)
                else L.s(R.string.t_no_tabs_in_workspace, state.selectedWorkspace.displayName),
                subtitle = if (viewingPrivateTabs) {
                    L.s(R.string.t_private_tabs_never_store_history_search)
                } else {
                    L.s(R.string.t_create_a_new_tab_in_this_workspace_to_ke)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(displayedTabs, key = { it.id }) { tab ->
                    TabPreviewCard(
                        tab = tab,
                        accentColor = accentColor,
                        onSelect = {
                            viewModel.selectTab(tab) { isHome ->
                                if (isHome) onOpenHome() else onOpenBrowser()
                            }
                        },
                        onDuplicate = { viewModel.duplicateTab(tab.id) },
                        onClose = { viewModel.closeTab(tab) }
                    )
                }
            }
        }
    }

    // Stage 12.3: Session Snapshots / Collections Dialog
    if (showSnapshotsDialog) {
        var newCollectionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSnapshotsDialog = false },
            title = { Text(L.s(R.string.t_tab_session_collections)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            placeholder = { Text(L.s(R.string.t_save_open_tabs_as)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            viewModel.saveCurrentNormalTabsAsSnapshot(newCollectionName)
                            newCollectionName = ""
                        }) {
                            Text(L.s(R.string.t_save))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.sessionSnapshots, key = { it.id }) { snap ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.restoreSessionSnapshot(snap) {
                                            showSnapshotsDialog = false
                                            onOpenBrowser()
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(snap.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text(L.s(R.string.t_saved_tabs_tap_to_restore, snap.tabCount), style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { viewModel.deleteSessionSnapshot(snap.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_delete_snapshot))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnapshotsDialog = false }) {
                    Text(L.s(R.string.t_close))
                }
            }
        )
    }
}

@Composable
private fun TabPreviewCard(
    tab: TabItem,
    accentColor: androidx.compose.ui.graphics.Color,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onClose: () -> Unit
) {
    val borderWidth = if (tab.isActive) 2.dp else 1.dp
    val borderColor = if (tab.isActive) accentColor else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.title.ifBlank { L.s(R.string.t_new_tab) },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = L.s(R.string.t_duplicate_tab),
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = L.s(R.string.t_close_tab),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = tab.thumbnailPath?.takeIf { !it.startsWith("memory://") }?.let { File(it) }
                if (thumbFile != null && thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = L.s(R.string.t_preview_of, tab.title),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (tab.isHomePage) L.s(R.string.t_nova_home) else tab.url.removePrefix("https://").removePrefix("http://").take(22),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
