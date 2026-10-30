package com.nibras.nova.ui.bookmarks

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Text(
                        text = if (state.currentFolderId == null) "Bookmarks"
                        else state.allFolders.firstOrNull { it.id == state.currentFolderId }?.name ?: "Folder",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Row {
                        IconButton(onClick = { importHtmlLauncher.launch(arrayOf("text/html", "*/*")) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Import Bookmarks (.html)")
                        }
                        IconButton(onClick = { viewModel.exportAllBookmarksToHtml(context) }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Export Bookmarks (.html)")
                        }
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                        }
                        IconButton(onClick = { showNewBookmarkDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Bookmark")
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
                    placeholder = { Text("Search bookmarks by title or URL…") }
                )
            }
        }
    ) { innerPadding ->
        if (state.folders.isEmpty() && state.bookmarks.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Bookmarks,
                title = "No Bookmarks Found",
                subtitle = "Save websites, organize folders, or use the top buttons to Import / Export Netscape HTML bookmarks.",
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
                                Icon(Icons.Default.Delete, contentDescription = "Delete Folder")
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
                                Icon(Icons.Default.Edit, contentDescription = "Edit Bookmark")
                            }
                            IconButton(onClick = { viewModel.deleteBookmark(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Bookmark")
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
            title = { Text("Create Bookmark Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
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
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
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
            title = { Text(if (target == null) "Add Bookmark" else "Edit Bookmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewBookmarkDialog = false
                    editingBookmark = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
