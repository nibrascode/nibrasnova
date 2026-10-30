package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nibras.nova.data.local.entity.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM normal_tabs ORDER BY createdAt ASC")
    fun observeNormalTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM normal_tabs ORDER BY createdAt ASC")
    suspend fun getAllNormalTabsOnce(): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTab(tab: TabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTabs(tabs: List<TabEntity>)

    @Query("UPDATE normal_tabs SET isActive = CASE WHEN id = :activeTabId THEN 1 ELSE 0 END")
    suspend fun setActiveTab(activeTabId: String)

    @Query("DELETE FROM normal_tabs WHERE id = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM normal_tabs")
    suspend fun deleteAllNormalTabs()

    @Transaction
    suspend fun replaceAllNormalTabs(tabs: List<TabEntity>) {
        deleteAllNormalTabs()
        upsertTabs(tabs)
    }
}
