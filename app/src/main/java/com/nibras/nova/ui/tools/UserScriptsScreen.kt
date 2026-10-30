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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.domain.model.ScriptInjectionType
import com.nibras.nova.domain.model.UserScriptItem
import com.nibras.nova.ui.components.EmptyStateView

@Composable
fun UserScriptsScreen(
    onBack: () -> Unit,
    viewModel: UserScriptsViewModel = hiltViewModel()
) {
    val scripts by viewModel.scripts.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingScript by remember { mutableStateOf<UserScriptItem?>(null) }

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
                    text = "UserScripts & Custom CSS",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add UserScript or CSS")
                }
            }
        }
    ) { innerPadding ->
        if (scripts.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Code,
                title = "No Custom UserScripts or CSS Rules",
                subtitle = "Create Tampermonkey-style JavaScript snippets or custom CSS overrides for any domain.",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scripts, key = { it.id }) { script ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = script.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${script.scriptType.label} • Domain: ${script.matchDomainPattern}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = script.isEnabled,
                                    onCheckedChange = { viewModel.toggleScript(script, it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = script.codeContent,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { editingScript = script }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Script")
                                }
                                IconButton(onClick = { viewModel.deleteScript(script.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Script")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingScript != null) {
        val target = editingScript
        var name by remember(target) { mutableStateOf(target?.name ?: "") }
        var domain by remember(target) { mutableStateOf(target?.matchDomainPattern ?: "*") }
        var scriptType by remember(target) { mutableStateOf(target?.scriptType ?: ScriptInjectionType.JAVASCRIPT) }
        var code by remember(target) { mutableStateOf(target?.codeContent ?: "") }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingScript = null
            },
            title = { Text(if (target == null) "New UserScript / CSS" else "Edit UserScript / CSS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Rule Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Domain Match (* for all, or example.com)") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScriptInjectionType.entries.forEach { type ->
                            FilterChip(
                                selected = scriptType == type,
                                onClick = { scriptType = type },
                                label = { Text(if (type == ScriptInjectionType.JAVASCRIPT) "JS" else "CSS") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(if (scriptType == ScriptInjectionType.JAVASCRIPT) "JavaScript Code" else "CSS Stylesheet") },
                        minLines = 4,
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (code.isNotBlank()) {
                        viewModel.saveScript(
                            id = target?.id ?: 0L,
                            name = name,
                            domainPattern = domain,
                            type = scriptType,
                            code = code
                        )
                        showCreateDialog = false
                        editingScript = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    editingScript = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
