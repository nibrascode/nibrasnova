package com.nibras.nova.domain.model

import kotlinx.serialization.Serializable

enum class ReaderTheme(
    val label: String,
    val backgroundHex: Long,
    val textHex: Long,
    val accentHex: Long
) {
    DARK("Nova Dark", 0xFF0A0F1D, 0xFFE2E8F0, 0xFF00E5FF),
    SEPIA("Warm Sepia", 0xFFF4ECD8, 0xFF3C2F2F, 0xFF8B5A2B),
    LIGHT("Paper Light", 0xFFFFFFFF, 0xFF0F172A, 0xFF0088A3)
}

@Serializable
data class ReaderBlock(
    val tag: String,
    val text: String
)

@Serializable
data class ReaderArticle(
    val isReadable: Boolean = false,
    val title: String = "",
    val byline: String = "",
    val siteName: String = "",
    val url: String = "",
    val wordCount: Int = 0,
    val estimatedReadMinutes: Int = 1,
    val blocks: List<ReaderBlock> = emptyList(),
    val plainText: String = ""
)

data class SavedOfflinePage(
    val id: Long = 0L,
    val title: String,
    val url: String,
    val siteName: String,
    val plainTextContent: String,
    val htmlFilePath: String?,
    val estimatedReadMinutes: Int,
    val savedAt: Long = System.currentTimeMillis()
)
