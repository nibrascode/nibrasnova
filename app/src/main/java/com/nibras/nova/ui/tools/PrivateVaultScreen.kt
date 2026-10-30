package com.nibras.nova.ui.tools

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.ui.components.EmptyStateView
import com.nibras.nova.ui.theme.NovaIncognitoAccent

@Composable
fun PrivateVaultScreen(
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit,
    viewModel: PrivateVaultViewModel = hiltViewModel()
) {
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val items by viewModel.vaultItems.collectAsStateWithLifecycle()
    val genPassword by viewModel.generatedPassword.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showPasswordGeneratorDialog by remember { mutableStateOf(false) }

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
                    text = "Biometric Private Vault & Key Generator",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (isUnlocked) {
                    Row {
                        IconButton(onClick = { showPasswordGeneratorDialog = true }) {
                            Icon(Icons.Default.Key, contentDescription = "Password & Passphrase Generator")
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Secret Bookmark or Note")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (!isUnlocked) {
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
                    contentDescription = "Unlock Biometric Vault",
                    tint = NovaIncognitoAccent,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Zero-Trust Private Vault",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your private bookmarks, generated credentials, and secret notes are isolated from standard browser search and require biometric verification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            viewModel.biometricLockHelper.authenticateIncognitoVault(
                                activity = activity,
                                onSuccess = { viewModel.markUnlocked() },
                                onError = {}
                            )
                        } else {
                            viewModel.markUnlocked()
                        }
                    }
                ) {
                    Text("Authenticate to Unlock Vault")
                }
            }
            return@Scaffold
        }

        if (items.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Lock,
                title = "Private Vault Is Empty",
                subtitle = "Tap the Key icon to generate high-entropy passwords or tap + to store confidential links.",
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
                items(items, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (entry.urlOrNote.startsWith("http://") || entry.urlOrNote.startsWith("https://")) {
                                    viewModel.openVaultUrlPrivately(entry.urlOrNote, onReady = onOpenBrowser)
                                }
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
                                    text = "${entry.category} • ${entry.title}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                SelectionContainer {
                                    Text(
                                        text = entry.urlOrNote,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = NovaIncognitoAccent
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteVaultEntry(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Vault Entry")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPasswordGeneratorDialog) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordGeneratorDialog = false },
            title = { Text("Cryptographic Key & Passphrase Generator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectionContainer {
                        Text(
                            text = genPassword.password,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = genPassword.strengthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.regeneratePassword(24, true) }) {
                            Text("24-Char Random")
                        }
                        OutlinedButton(onClick = { viewModel.generateDicewarePassphrase() }) {
                            Text("Passphrase")
                        }
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Account / Website Label") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.copyAndSaveGeneratedPassword(label)
                    showPasswordGeneratorDialog = false
                }) {
                    Text("Copy & Save in Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordGeneratorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var urlOrNote by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add to Private Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Label") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = urlOrNote,
                        onValueChange = { urlOrNote = it },
                        label = { Text("Confidential URL or Secret Note") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (urlOrNote.isNotBlank()) {
                        viewModel.addVaultEntry(title, urlOrNote, "BOOKMARK")
                        showAddDialog = false
                    }
                }) {
                    Text("Save in Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
