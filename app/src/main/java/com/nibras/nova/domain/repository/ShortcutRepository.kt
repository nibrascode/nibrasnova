package com.nibras.nova.domain.repository

import com.nibras.nova.domain.model.QuickShortcut
import kotlinx.coroutines.flow.Flow

interface ShortcutRepository {
    fun observeShortcuts(): Flow<List<QuickShortcut>>
    suspend fun ensureDefaultShortcutsSeeded()
    suspend fun addShortcut(title: String, url: String, iconColorHex: String = "#00E5FF")
    suspend fun updateShortcut(shortcut: QuickShortcut)
    suspend fun deleteShortcut(shortcutId: Long)
}
