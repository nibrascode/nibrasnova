package com.nibras.nova.ui.tools

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonXmlFormatterSheet(
    rawPayload: String,
    onDismiss: () -> Unit
) {
    val prettyFormatted = remember(rawPayload) {
        prettyFormatJsonOrXml(rawPayload)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Text(
                text = L.s(R.string.t_formatted_json_xml_api_payload_viewer),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = prettyFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        }
    }
}

fun prettyFormatJsonOrXml(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return L.s(R.string.t_empty_document_body)
    return try {
        val json = Json { prettyPrint = true; isLenient = true }
        val element = json.decodeFromString<JsonElement>(trimmed)
        json.encodeToString(element)
    } catch (_: Exception) {
        // Fallback XML indentation
        if (trimmed.startsWith("<")) {
            trimmed.replace(">\\s*<".toRegex(), ">\n<")
        } else {
            trimmed
        }
    }
}
