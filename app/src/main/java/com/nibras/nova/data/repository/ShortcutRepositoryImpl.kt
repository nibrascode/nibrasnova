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
        val current = shortcutDao.getAll()
        // Fresh install, or an untouched list from older versions (exact old 8 defaults) -> new defaults.
        if (current.isEmpty() || DefaultShortcuts.isUntouchedLegacy(current.map { it.title to it.url })) {
            if (current.isNotEmpty()) shortcutDao.deleteAll()
            shortcutDao.insertShortcuts(DefaultShortcuts.CURRENT.mapIndexed { i, (t, u, c) ->
                QuickShortcutEntity(title = t, url = u, iconColorHex = c, sortOrder = i)
            })
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
