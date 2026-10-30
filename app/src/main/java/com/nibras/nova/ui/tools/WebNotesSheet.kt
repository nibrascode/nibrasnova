package com.nibras.nova.ui.tools

import com.nibras.nova.R
import com.nibras.nova.core.common.L

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nibras.nova.data.local.entity.WebAnnotationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebNotesSheet(
    currentPageUrl: String,
    initialSelectedQuote: String,
    annotationsForPage: List<WebAnnotationEntity>,
    onSaveAnnotation: (String, String) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var quoteText by remember(initialSelectedQuote) { mutableStateOf(initialSelectedQuote) }
    var noteText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "  " + L.s(R.string.t_web_annotator_sticky_highlights),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = currentPageUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = quoteText,
                onValueChange = { quoteText = it },
                label = { Text(L.s(R.string.t_highlighted_sentence_phrase_on_page)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(L.s(R.string.t_your_personal_note_research_annotation)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (quoteText.isNotBlank() || noteText.isNotBlank()) {
                        onSaveAnnotation(quoteText, noteText)
                        quoteText = ""
                        noteText = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(L.s(R.string.t_save_persistent_highlight_note))
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(annotationsForPage, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (item.highlightedQuote.isNotBlank()) {
                                    Text(
                                        text = "“${item.highlightedQuote}”",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (item.userNote.isNotBlank()) {
                                    Text(
                                        text = item.userNote,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteAnnotation(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_delete_note))
                            }
                        }
                    }
                }
            }
        }
    }
}
