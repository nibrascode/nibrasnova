package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.SavedPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPageDao {
    @Query("SELECT * FROM saved_offline_pages ORDER BY savedAt DESC")
    fun observeSavedPages(): Flow<List<SavedPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPage(page: SavedPageEntity): Long

    @Query("DELETE FROM saved_offline_pages WHERE id = :pageId")
    suspend fun deleteSavedPage(pageId: Long)
}
