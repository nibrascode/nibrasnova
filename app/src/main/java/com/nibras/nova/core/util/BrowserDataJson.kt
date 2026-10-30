package com.nibras.nova.core.util

import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.domain.model.HistoryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Portable JSON backup of bookmarks + history (no favicons, no tabs, no secrets). */
object BrowserDataJson {
    @Serializable data class JBookmark(val title: String, val url: String, val folder: String? = null, val createdAt: Long = 0L)
    @Serializable data class JHistory(val title: String, val url: String, val visitedAt: Long)
    @Serializable data class Backup(
        val format: String = "nibras-nova-backup", val version: Int = 1, val exportedAt: Long = 0L,
        val bookmarks: List<JBookmark> = emptyList(), val history: List<JHistory> = emptyList()
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(bookmarks: List<BookmarkItem>, history: List<HistoryItem>, folderName: (Long?) -> String? = { null }, now: Long = System.currentTimeMillis()): String =
        json.encodeToString(
            Backup(
                exportedAt = now,
                bookmarks = bookmarks.map { JBookmark(it.title, it.url, folderName(it.folderId), it.createdAt) },
                history = history.map { JHistory(it.title, it.url, it.visitedAt) }
            )
        )

    fun decode(text: String): Backup {
        val b = json.decodeFromString(Backup.serializer(), text)
        require(b.format == "nibras-nova-backup") { "Not a Nibras Nova backup" }
        return b.copy(
            bookmarks = b.bookmarks.filter { it.url.startsWith("http://") || it.url.startsWith("https://") },
            history = b.history.filter { it.url.startsWith("http://") || it.url.startsWith("https://") }
        )
    }
}
