package com.nibras.nova.ui.components

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FindInPageBar(
    query: String,
    activeMatchOrdinal: Int,
    numberOfMatches: Int,
    onQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(L.s(R.string.t_find_on_page)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Text(
                            text = if (numberOfMatches > 0) "${activeMatchOrdinal + 1}/$numberOfMatches" else "0/0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
            IconButton(onClick = onFindPrevious, enabled = numberOfMatches > 0) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = L.s(R.string.t_previous_match))
            }
            IconButton(onClick = onFindNext, enabled = numberOfMatches > 0) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = L.s(R.string.t_next_match))
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = L.s(R.string.t_close_find_in_page))
            }
        }
    }
}
