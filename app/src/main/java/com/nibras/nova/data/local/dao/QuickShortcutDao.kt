package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nibras.nova.data.local.entity.QuickShortcutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickShortcutDao {
    @Query("SELECT * FROM quick_shortcuts ORDER BY sortOrder ASC, id ASC")
    fun observeShortcuts(): Flow<List<QuickShortcutEntity>>

    @Query("SELECT COUNT(*) FROM quick_shortcuts")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: QuickShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcuts(shortcuts: List<QuickShortcutEntity>)

    @Update
    suspend fun updateShortcut(shortcut: QuickShortcutEntity)

    @Query("SELECT * FROM quick_shortcuts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<QuickShortcutEntity>

    @Query("DELETE FROM quick_shortcuts")
    suspend fun deleteAll()

    @Query("DELETE FROM quick_shortcuts WHERE id = :shortcutId")
    suspend fun deleteShortcut(shortcutId: Long)
}
