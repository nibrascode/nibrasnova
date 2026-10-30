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
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenuItem
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

    val homeContext = LocalContext.current
    val pinnedToastText = stringResource(R.string.home_pinned_toast)
    var moreExpanded by remember { mutableStateOf(false) }
    val lang = homeContext.resources.configuration.locales[0].language
    fun open(url: String) = viewModel.submitSearchOrUrl(rawQuery = url, openInPrivate = isPrivateInputMode, onNavigateToBrowser = onOpenBrowser)
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) open(spoken)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg)
    ) {
        Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Hero with privacy + private-mode controls overlaid
            item {
                Box {
                    HomeHero()
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onOpenPrivacyDashboard) {
                            Icon(Icons.Default.Security, L.s(R.string.t_privacy_dashboard), tint = NovaTurquoise)
                        }
                        IconButton(onClick = { isPrivateInputMode = !isPrivateInputMode }) {
                            Icon(
                                Icons.Default.VisibilityOff, L.s(R.string.t_toggle_private_mode),
                                tint = if (isPrivateInputMode) NovaIncognitoAccent else Color(0x99FFFFFF)
                            )
                        }
                    }
                }
            }

            // 2. Glass search bar
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    HomeSearchBar(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        onGo = {
                            if (searchInput.isNotBlank()) { open(searchInput); searchInput = "" }
                        },
                        engineName = state.settings.searchEngine.displayName,
                        isPrivate = isPrivateInputMode,
                        onEngineClick = { showEnginePickerSheet = true },
                        onMic = {
                            runCatching {
                                voiceLauncher.launch(
                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                )
                            }
                        },
                        onQr = onOpenQrScanner,
                        placeholder = if (isPrivateInputMode) L.s(R.string.t_search_privately_with, state.settings.searchEngine.displayName)
                        else stringResource(R.string.home_search_hint)
                    )
                }
            }

            // 3. Quick-link tiles: user shortcuts (defaults when empty), pinned Nibras, add
            item {
                val defaults = listOf(
                    QuickShortcut(id = -1L, title = "Google", url = "https://www.google.com"),
                    QuickShortcut(id = -2L, title = "YouTube", url = "https://www.youtube.com"),
                    QuickShortcut(id = -3L, title = "GitHub", url = "https://github.com")
                )
                val user = if (state.settings.showQuickShortcutsOnHome) {
                    state.shortcuts.filterNot { it.url.contains("nibrascode.com", ignoreCase = true) }
                } else emptyList()
                val tiles = (if (user.isEmpty() && state.settings.showQuickShortcutsOnHome) defaults else user) +
                    QuickShortcut(id = PINNED_NIBRASCODE_ID, title = "Nibras", url = "https://nibrascode.com")
                val cells: List<QuickShortcut?> = tiles + listOf(null)
                Column(Modifier.padding(horizontal = 14.dp)) {
                    cells.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { sc ->
                                if (sc == null) {
                                    QuickTile(stringResource(R.string.home_add), onClick = { showAddShortcutDialog = true }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                } else {
                                    QuickTile(
                                        sc.title,
                                        onClick = { open(sc.url) },
                                        onLongClick = {
                                            when {
                                                sc.id == PINNED_NIBRASCODE_ID -> Toast.makeText(homeContext, pinnedToastText, Toast.LENGTH_SHORT).show()
                                                sc.id > 0 -> editingShortcut = sc
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { TileIcon(sc.url, sc.title) }
                                }
                            }
                            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // 4. Discover: real nibrascode.com pages, bundled for offline
            item {
                Column {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        DiscoverHeader(onSeeAll = { open(if (lang in setOf("en", "tr", "ru", "ar")) "https://nibrascode.com/$lang" else "https://nibrascode.com") })
                    }
                    DiscoverRow(DISCOVER_ITEMS) { open(it.url(lang)) }
                }
            }

            // 5. Recently visited (normal mode only)
            if (state.settings.showRecentHistoryOnHome && !isPrivateInputMode && state.recentHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.recently_visited_title), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.home_view_all)) }
                    }
                }
                items(state.recentHistory, key = { it.id }) { historyItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF111A3A))
                            .clickable { viewModel.submitSearchOrUrl(rawQuery = historyItem.url, openInPrivate = false, onNavigateToBrowser = onOpenBrowser) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(NovaElevatedNavy), contentAlignment = Alignment.Center) {
                            Text(historyItem.title.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(historyItem.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(historyItem.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.deleteRecentHistoryItem(historyItem.id) }) {
                            Icon(Icons.Default.Close, L.s(R.string.t_remove_from_recent), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        HomeBottomBar(
            tabCount = if (isPrivateInputMode) state.privateTabCount else state.normalTabCount,
            onBookmarks = onOpenBookmarks,
            onHistory = onOpenHistory,
            onTabs = onOpenTabs,
            moreExpanded = moreExpanded,
            onMore = { moreExpanded = true },
            onDismissMore = { moreExpanded = false },
            moreMenu = { dismiss ->
                DropdownMenuItem(text = { Text(L.s(R.string.t_downloads)) }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = { dismiss(); onOpenDownloads() })
                DropdownMenuItem(text = { Text(L.s(R.string.t_privacy)) }, leadingIcon = { Icon(Icons.Default.Policy, null) }, onClick = { dismiss(); onOpenPrivacyDashboard() })
                DropdownMenuItem(text = { Text(stringResource(R.string.t_scan_qr_code)) }, leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) }, onClick = { dismiss(); onOpenQrScanner() })
                DropdownMenuItem(
                    text = { Text(if (isPrivateInputMode) L.s(R.string.t_private_on) else L.s(R.string.t_private_2)) },
                    leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                    onClick = { dismiss(); isPrivateInputMode = !isPrivateInputMode }
                )
                DropdownMenuItem(text = { Text(L.s(R.string.t_settings)) }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { dismiss(); onOpenSettings() })
            }
        )
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
