package com.nibras.nova.ui.bookmarks

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.ui.components.EmptyStateView
import java.io.File

@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<BookmarkItem?>(null) }
    var showNewBookmarkDialog by remember { mutableStateOf(false) }

    val importHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBookmarksFromHtmlUri(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (state.currentFolderId != null) {
                                viewModel.selectFolder(null)
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L.s(R.string.t_back))
                    }

                    Text(
                        text = if (state.currentFolderId == null) L.s(R.string.t_bookmarks)
                        else state.allFolders.firstOrNull { it.id == state.currentFolderId }?.name ?: L.s(R.string.t_folder),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Row {
                        IconButton(onClick = { importHtmlLauncher.launch(arrayOf("text/html", "*/*")) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = L.s(R.string.t_import_bookmarks_html))
                        }
                        IconButton(onClick = { viewModel.exportAllBookmarksToHtml(context) }) {
                            Icon(Icons.Default.FileUpload, contentDescription = L.s(R.string.t_export_bookmarks_html))
                        }
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = L.s(R.string.t_new_folder))
                        }
                        IconButton(onClick = { showNewBookmarkDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = L.s(R.string.t_add_bookmark))
                        }
                    }
                }

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(L.s(R.string.t_search_bookmarks_by_title_or_url)) }
                )
            }
        }
    ) { innerPadding ->
        if (state.folders.isEmpty() && state.bookmarks.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Bookmarks,
                title = L.s(R.string.t_no_bookmarks_found),
                subtitle = L.s(R.string.t_save_websites_organize_folders_or_use_th),
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
                items(state.folders, key = { "folder_${it.id}" }) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectFolder(folder.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.deleteFolder(folder.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_delete_folder))
                            }
                        }
                    }
                }

                items(state.bookmarks, key = { "bm_${it.id}" }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openBookmark(item.url, isPrivate = false, onReady = onOpenBrowser)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconFile = item.faviconPath?.let { File(it) }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (iconFile != null && iconFile.exists()) {
                                    AsyncImage(
                                        model = iconFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        text = item.title.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { editingBookmark = item }) {
                                Icon(Icons.Default.Edit, contentDescription = L.s(R.string.t_edit_bookmark))
                            }
                            IconButton(onClick = { viewModel.deleteBookmark(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_delete_bookmark))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(L.s(R.string.t_create_bookmark_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(L.s(R.string.t_folder_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (folderName.isNotBlank()) {
                        viewModel.createFolder(folderName)
                        showCreateFolderDialog = false
                    }
                }) {
                    Text(L.s(R.string.t_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(L.s(R.string.t_cancel))
                }
            }
        )
    }

    if (showNewBookmarkDialog || editingBookmark != null) {
        val target = editingBookmark
        var title by remember(target) { mutableStateOf(target?.title ?: "") }
        var url by remember(target) { mutableStateOf(target?.url ?: "https://") }

        AlertDialog(
            onDismissRequest = {
                showNewBookmarkDialog = false
                editingBookmark = null
            },
            title = { Text(if (target == null) L.s(R.string.t_add_bookmark) else L.s(R.string.t_edit_bookmark)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(L.s(R.string.t_title)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(com.nibras.nova.core.common.L.s(com.nibras.nova.R.string.lbl_url)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (url.isNotBlank()) {
                        viewModel.saveBookmark(
                            id = target?.id ?: 0L,
                            title = title,
                            url = url,
                            folderId = target?.folderId ?: state.currentFolderId
                        )
                        showNewBookmarkDialog = false
                        editingBookmark = null
                    }
                }) {
                    Text(L.s(R.string.t_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewBookmarkDialog = false
                    editingBookmark = null
                }) {
                    Text(L.s(R.string.t_cancel))
                }
            }
        )
    }
}
