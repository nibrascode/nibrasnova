package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.RssFeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RssFeedDao {
    @Query("SELECT * FROM rss_feeds ORDER BY addedAt ASC")
    fun observeAllFeeds(): Flow<List<RssFeedEntity>>

    @Query("SELECT * FROM rss_feeds ORDER BY addedAt ASC")
    suspend fun getAllFeedsOnce(): List<RssFeedEntity>

    @Query("SELECT COUNT(*) FROM rss_feeds")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeed(entity: RssFeedEntity): Long

    @Query("DELETE FROM rss_feeds WHERE id = :id")
    suspend fun deleteFeed(id: Long)
}
