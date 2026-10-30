package com.nibras.nova.ui.home

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.domain.model.QuickShortcut
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.ui.components.NovaLogo
import com.nibras.nova.ui.theme.NovaDeepNavy
import com.nibras.nova.ui.theme.NovaElevatedNavy
import com.nibras.nova.ui.theme.NovaIncognitoAccent
import com.nibras.nova.ui.theme.NovaTurquoise
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenBrowser: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchInput by remember { mutableStateOf("") }
    var isPrivateInputMode by remember { mutableStateOf(false) }
    var showEnginePickerSheet by remember { mutableStateOf(false) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<QuickShortcut?>(null) }

    val backgroundBrush = when (state.settings.wallpaperPresetId) {
        "nova_turquoise_aurora" -> Brush.verticalGradient(
            listOf(Color(0xFF061A23), NovaDeepNavy, Color(0xFF0A1424))
        )
        "nova_graphite_minimal" -> Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
        )
        else -> Brush.verticalGradient(
            listOf(NovaDeepNavy, Color(0xFF0E172A), MaterialTheme.colorScheme.background)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .systemBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Top Header Bar: Privacy Shield Pill, Incognito Switch, Tabs Counter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onOpenPrivacyDashboard,
                        label = {
                            Text(
                                text = L.s(R.string.t_protected, state.privacyMetrics.httpsUpgradesCount + state.privacyMetrics.unsafeSchemesBlockedCount),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = L.s(R.string.t_privacy_dashboard),
                                tint = NovaTurquoise,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { isPrivateInputMode = !isPrivateInputMode },
                            label = {
                                Text(if (isPrivateInputMode) L.s(R.string.t_private_on) else L.s(R.string.t_private_2))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = L.s(R.string.t_toggle_private_mode),
                                    tint = if (isPrivateInputMode) NovaIncognitoAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        IconButton(onClick = onOpenTabs) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .border(
                                        2.dp,
                                        if (isPrivateInputMode) NovaIncognitoAccent else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val count = if (isPrivateInputMode) state.privateTabCount else state.normalTabCount
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isPrivateInputMode) NovaIncognitoAccent else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Nibras Nova Hero Branding
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NovaLogo(
                        iconSize = 54.dp,
                        showSubtitle = true,
                        isIncognito = isPrivateInputMode
                    )
                }
            }

            // 3. Intelligent Search / Address Bar with Engine Badge & QR Scanner
            item {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = if (isPrivateInputMode) {
                                L.s(R.string.t_search_privately_with, state.settings.searchEngine.displayName)
                            } else {
                                L.s(R.string.t_search_or_type_url, state.settings.searchEngine.displayName)
                            }
                        )
                    },
                    leadingIcon = {
                        Surface(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showEnginePickerSheet = true },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = L.s(R.string.t_select_search_engine),
                                    tint = if (isPrivateInputMode) NovaIncognitoAccent else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = state.settings.searchEngine.displayName.take(6),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = L.s(R.string.t_clear_search))
                                }
                            }
                            IconButton(onClick = onOpenQrScanner) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = L.s(R.string.t_scan_qr_code),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (searchInput.isNotBlank()) {
                                viewModel.submitSearchOrUrl(
                                    rawQuery = searchInput,
                                    openInPrivate = isPrivateInputMode,
                                    onNavigateToBrowser = onOpenBrowser
                                )
                                searchInput = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isPrivateInputMode) NovaIncognitoAccent else NovaTurquoise,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // 4. Quick-Action Utility Dock (Bookmarks, History, Downloads, Privacy, Settings)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HomeDockActionButton(
                        icon = Icons.Default.Bookmarks,
                        label = L.s(R.string.t_bookmarks),
                        onClick = onOpenBookmarks
                    )
                    HomeDockActionButton(
                        icon = Icons.Default.History,
                        label = L.s(R.string.t_history),
                        onClick = onOpenHistory
                    )
                    HomeDockActionButton(
                        icon = Icons.Default.Download,
                        label = L.s(R.string.t_downloads),
                        onClick = onOpenDownloads
                    )
                    HomeDockActionButton(
                        icon = Icons.Default.Policy,
                        label = L.s(R.string.t_privacy),
                        onClick = onOpenPrivacyDashboard
                    )
                    HomeDockActionButton(
                        icon = Icons.Default.Settings,
                        label = L.s(R.string.t_settings),
                        onClick = onOpenSettings
                    )
                }
            }

            // 5. Quick-Access Website Shortcuts (nibrascode.com is always pinned first)
            run {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.quick_access_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = { showAddShortcutDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" " + stringResource(R.string.home_add_site))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.home_shortcuts_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val homeContext = LocalContext.current
                        val pinnedToastText = stringResource(R.string.home_pinned_toast)
                        val pinnedShortcut = QuickShortcut(id = PINNED_NIBRASCODE_ID, title = "nibrascode.com", url = "https://nibrascode.com")
                        val userShortcuts = if (state.settings.showQuickShortcutsOnHome) {
                            state.shortcuts.filterNot { it.url.contains("nibrascode.com", ignoreCase = true) }
                        } else emptyList()
                        val rows = (listOf(pinnedShortcut) + userShortcuts).chunked(4)
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { shortcut ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    viewModel.submitSearchOrUrl(
                                                        rawQuery = shortcut.url,
                                                        openInPrivate = isPrivateInputMode,
                                                        onNavigateToBrowser = onOpenBrowser
                                                    )
                                                },
                                                onLongClick = {
                                                    if (shortcut.id == PINNED_NIBRASCODE_ID) {
                                                        Toast.makeText(homeContext, pinnedToastText, Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        editingShortcut = shortcut
                                                    }
                                                }
                                            )
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(NovaElevatedNavy)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortcut.title.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = shortcut.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 6. Recently Visited Websites Strip (Only shown in Normal Mode when non-empty)
            if (state.settings.showRecentHistoryOnHome && !isPrivateInputMode && state.recentHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recently_visited_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        TextButton(onClick = onOpenHistory) {
                            Text(stringResource(R.string.home_view_all))
                        }
                    }
                }

                items(state.recentHistory, key = { it.id }) { historyItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.submitSearchOrUrl(
                                    rawQuery = historyItem.url,
                                    openInPrivate = false,
                                    onNavigateToBrowser = onOpenBrowser
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = historyItem.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = historyItem.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = historyItem.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.deleteRecentHistoryItem(historyItem.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = L.s(R.string.t_remove_from_recent),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Engine Quick Selector Bottom Sheet
        if (showEnginePickerSheet) {
            ModalBottomSheet(onDismissRequest = { showEnginePickerSheet = false }) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = L.s(R.string.t_select_active_search_engine),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SearchEngineType.entries.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSearchEngine(engine)
                                    showEnginePickerSheet = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.settings.searchEngine == engine,
                                onClick = {
                                    viewModel.selectSearchEngine(engine)
                                    showEnginePickerSheet = false
                                }
                            )
                            Column {
                                Text(engine.displayName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    engine.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Add Shortcut Dialog
        if (showAddShortcutDialog) {
            ShortcutEditorDialog(
                dialogTitle = L.s(R.string.t_add_website_shortcut),
                initialTitle = "",
                initialUrl = "https://",
                onDismiss = { showAddShortcutDialog = false },
                onSave = { title, url ->
                    viewModel.addShortcut(title, url)
                    showAddShortcutDialog = false
                },
                onDelete = null
            )
        }

        // Edit / Delete Shortcut Dialog
        editingShortcut?.let { target ->
            ShortcutEditorDialog(
                dialogTitle = L.s(R.string.t_edit_shortcut),
                initialTitle = target.title,
                initialUrl = target.url,
                onDismiss = { editingShortcut = null },
                onSave = { newTitle, newUrl ->
                    viewModel.updateShortcut(target, newTitle, newUrl)
                    editingShortcut = null
                },
                onDelete = {
                    viewModel.deleteShortcut(target.id)
                    editingShortcut = null
                }
            )
        }
    }
}

@Composable
private fun HomeDockActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShortcutEditorDialog(
    dialogTitle: String,
    initialTitle: String,
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(L.s(R.string.t_website_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(L.s(R.string.t_website_url)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (url.isNotBlank()) onSave(title, url) }
            ) {
                Text(L.s(R.string.t_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete) {
                        Text(L.s(R.string.t_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(L.s(R.string.t_cancel))
                }
            }
        }
    )
}

/** Sentinel id for the built-in, non-removable nibrascode.com shortcut. */
private const val PINNED_NIBRASCODE_ID = -100L
