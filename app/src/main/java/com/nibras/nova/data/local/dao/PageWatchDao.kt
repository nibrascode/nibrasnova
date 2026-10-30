package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nibras.nova.data.local.entity.PageWatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageWatchDao {
    @Query("SELECT * FROM page_watch_monitors ORDER BY hasChangedSinceLastCheck DESC, lastCheckedAt DESC")
    fun observeAllWatchedPages(): Flow<List<PageWatchEntity>>

    @Query("SELECT * FROM page_watch_monitors")
    suspend fun getAllWatchedPagesOnce(): List<PageWatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPageWatch(entity: PageWatchEntity): Long

    @Update
    suspend fun updatePageWatch(entity: PageWatchEntity)

    @Query("DELETE FROM page_watch_monitors WHERE id = :id")
    suspend fun deletePageWatch(id: Long)
}
