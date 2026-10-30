package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM browsing_history ORDER BY visitedAt DESC LIMIT :limit")
    fun observeRecentHistory(limit: Int = 500): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM browsing_history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 200")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntity): Long

    @Query("DELETE FROM browsing_history WHERE id = :historyId")
    suspend fun deleteHistoryItem(historyId: Long)

    @Query("DELETE FROM browsing_history WHERE visitedAt >= :sinceEpochMillis")
    suspend fun deleteHistorySince(sinceEpochMillis: Long)

    @Query("DELETE FROM browsing_history")
    suspend fun clearAllHistory()
}
