package com.nibras.nova.data.repository

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.data.local.dao.QuickShortcutDao
import com.nibras.nova.data.local.entity.QuickShortcutEntity
import com.nibras.nova.domain.model.QuickShortcut
import com.nibras.nova.domain.repository.ShortcutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutRepositoryImpl @Inject constructor(
    private val shortcutDao: QuickShortcutDao,
    private val dispatchers: DispatcherProvider
) : ShortcutRepository {

    override fun observeShortcuts(): Flow<List<QuickShortcut>> =
        shortcutDao.observeShortcuts().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureDefaultShortcutsSeeded() = withContext(dispatchers.io) {
        if (shortcutDao.getCount() == 0) {
            val defaults = listOf(
                QuickShortcutEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", iconColorHex = "#00E5FF", sortOrder = 0),
                QuickShortcutEntity(title = "Wikipedia", url = "https://www.wikipedia.org", iconColorHex = "#38BDF8", sortOrder = 1),
                QuickShortcutEntity(title = "GitHub", url = "https://github.com", iconColorHex = "#94A3B8", sortOrder = 2),
                QuickShortcutEntity(title = "Android Dev", url = "https://developer.android.com", iconColorHex = "#10B981", sortOrder = 3),
                QuickShortcutEntity(title = "Kotlin", url = "https://kotlinlang.org", iconColorHex = "#A855F7", sortOrder = 4),
                QuickShortcutEntity(title = "MDN Web Docs", url = "https://developer.mozilla.org", iconColorHex = "#00B4D8", sortOrder = 5),
                QuickShortcutEntity(title = "Hacker News", url = "https://news.ycombinator.com", iconColorHex = "#F59E0B", sortOrder = 6),
                QuickShortcutEntity(title = "Ars Technica", url = "https://arstechnica.com", iconColorHex = "#EC4899", sortOrder = 7)
            )
            shortcutDao.insertShortcuts(defaults)
        }
    }

    override suspend fun addShortcut(title: String, url: String, iconColorHex: String) = withContext(dispatchers.io) {
        val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url.trim() else "https://${url.trim()}"
        val nextSortOrder = shortcutDao.getCount()
        shortcutDao.insertShortcut(
            QuickShortcutEntity(
                title = title.trim().ifBlank { normalizedUrl },
                url = normalizedUrl,
                iconColorHex = iconColorHex,
                sortOrder = nextSortOrder
            )
        )
        Unit
    }

    override suspend fun updateShortcut(shortcut: QuickShortcut) = withContext(dispatchers.io) {
        val normalizedUrl = if (shortcut.url.startsWith("http://") || shortcut.url.startsWith("https://")) {
            shortcut.url.trim()
        } else {
            "https://${shortcut.url.trim()}"
        }
        shortcutDao.updateShortcut(
            QuickShortcutEntity(
                id = shortcut.id,
                title = shortcut.title.trim().ifBlank { normalizedUrl },
                url = normalizedUrl,
                iconColorHex = shortcut.iconColorHex,
                sortOrder = shortcut.sortOrder,
                isPinned = shortcut.isPinned
            )
        )
    }

    override suspend fun deleteShortcut(shortcutId: Long) = withContext(dispatchers.io) {
        shortcutDao.deleteShortcut(shortcutId)
    }
}
