package com.nibras.nova.ui.reader

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.domain.model.ReaderTheme
import com.nibras.nova.ui.components.EmptyStateView

@Composable
fun ReaderModeScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showTypographyControls by remember { mutableStateOf(false) }
    var showSavedPagesDrawer by remember { mutableStateOf(false) }
    var showSummaryCard by remember { mutableStateOf(true) }

    val readingProgress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val bgColor = Color(state.readerTheme.backgroundHex)
    val fgColor = Color(state.readerTheme.textHex)
    val accentColor = Color(state.readerTheme.accentHex)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            Column(modifier = Modifier.background(bgColor)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L.s(R.string.t_back), tint = fgColor)
                    }

                    Text(
                        text = if (showSavedPagesDrawer) L.s(R.string.t_saved_offline, state.savedOfflinePages.size)
                        else L.s(R.string.t_reader_mode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = fgColor
                    )

                    Row {
                        IconButton(onClick = {
                            viewModel.generateOnDeviceSummary()
                            showSummaryCard = !showSummaryCard
                        }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = L.s(R.string.t_toggle_on_device_article_summary),
                                tint = accentColor
                            )
                        }
                        IconButton(onClick = viewModel::toggleTtsSpeech) {
                            Icon(
                                imageVector = if (state.ttsState.isSpeaking) Icons.Default.StopCircle else Icons.Default.RecordVoiceOver,
                                contentDescription = L.s(R.string.t_listen_to_article_tts),
                                tint = if (state.ttsState.isSpeaking) accentColor else fgColor
                            )
                        }
                        IconButton(onClick = { showTypographyControls = !showTypographyControls }) {
                            Icon(Icons.Default.FormatSize, contentDescription = L.s(R.string.t_typography_themes), tint = fgColor)
                        }
                        IconButton(onClick = { viewModel.translateArticleOnDevice("en") }) {
                            Icon(Icons.Default.Translate, contentDescription = L.s(R.string.t_translate_article), tint = fgColor)
                        }
                        IconButton(onClick = viewModel::saveArticleForOfflineReading) {
                            Icon(Icons.Default.BookmarkAdded, contentDescription = L.s(R.string.t_save_offline), tint = accentColor)
                        }
                        IconButton(onClick = { viewModel.shareArticleText(context) }) {
                            Icon(Icons.Default.Share, contentDescription = L.s(R.string.t_share_article), tint = fgColor)
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { readingProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = accentColor,
                    trackColor = fgColor.copy(alpha = 0.12f)
                )

                if (showTypographyControls) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L.s(R.string.t_font_size_sp, state.fontSizeSp.toInt()), color = fgColor)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.adjustFontSize(-2f) }) { Text("A-") }
                                OutlinedButton(onClick = { viewModel.adjustFontSize(2f) }) { Text("A+") }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L.s(R.string.t_tts_voice_speed_x, state.ttsState.speechRate), color = fgColor)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                                    FilterChip(
                                        selected = state.ttsState.speechRate == speed,
                                        onClick = { viewModel.changeTtsSpeechRate(speed) },
                                        label = { Text("${speed}x") }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderTheme.entries.forEach { theme ->
                                FilterChip(
                                    selected = state.readerTheme == theme,
                                    onClick = { viewModel.selectReaderTheme(theme) },
                                    label = { Text(theme.label) }
                                )
                            }
                        }

                        OutlinedButton(onClick = { showSavedPagesDrawer = !showSavedPagesDrawer }) {
                            Text(if (showSavedPagesDrawer) L.s(R.string.t_back_to_active_article) else L.s(R.string.t_view_saved_offline_articles_archives, state.savedOfflinePages.size))
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    ) { innerPadding ->
        if (showSavedPagesDrawer) {
            if (state.savedOfflinePages.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.MenuBook,
                    title = L.s(R.string.t_no_offline_articles_or_web_archives_save),
                    subtitle = L.s(R.string.t_use_the_save_icon_in_reader_mode_or_save),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.savedOfflinePages, key = { it.id }) { saved ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadSavedOfflineArticle(saved)
                                    showSavedPagesDrawer = false
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
                                    Text(saved.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        L.s(R.string.t_min_read, saved.siteName, saved.estimatedReadMinutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteSavedOfflineArticle(saved.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_delete_saved_article))
                                }
                            }
                        }
                    }
                }
            }
            return@Scaffold
        }

        val article = state.article
        if (article == null || (!article.isReadable && article.plainText.isBlank())) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.MenuBook,
                    title = L.s(R.string.t_reader_mode_unavailable_on_this_page),
                    subtitle = L.s(R.string.t_this_page_does_not_contain_a_structured)
                )
                Button(onClick = { showSavedPagesDrawer = true }) {
                    Text(L.s(R.string.t_open_saved_offline_articles, state.savedOfflinePages.size))
                }
            }
        } else {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = article.title,
                        fontSize = (state.fontSizeSp + 6f).sp,
                        lineHeight = ((state.fontSizeSp + 6f) * 1.25f).sp,
                        fontWeight = FontWeight.Bold,
                        color = fgColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = L.s(R.string.t_min_read_words, article.siteName, article.estimatedReadMinutes, article.wordCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )

                    // Stage 12.2: On-Device Extractive Article Summary & Key Terms Card
                    val summary = state.extractiveSummary
                    if (showSummaryCard && summary != null && summary.bulletPoints.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = fgColor.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor)
                                    Text(
                                        text = "  " + L.s(R.string.t_on_device_key_takeaways_100_local),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = accentColor
                                    )
                                }
                                if (summary.topKeywords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = L.s(R.string.t_keywords, summary.topKeywords.joinToString(", ")),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fgColor.copy(alpha = 0.75f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                summary.bulletPoints.forEach { bullet ->
                                    Text(
                                        text = "• $bullet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = fgColor,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    state.statusMessage?.let { status ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = fgColor.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    val bodyToDisplay = state.translatedText ?: article.plainText
                    Text(
                        text = bodyToDisplay,
                        fontSize = state.fontSizeSp.sp,
                        lineHeight = (state.fontSizeSp * state.lineHeightMultiplier).sp,
                        color = fgColor
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}
