package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.SessionSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionSnapshotDao {
    @Query("SELECT * FROM session_snapshots ORDER BY createdAt DESC")
    fun observeAllSnapshots(): Flow<List<SessionSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SessionSnapshotEntity): Long

    @Query("DELETE FROM session_snapshots WHERE id = :id")
    suspend fun deleteSnapshot(id: Long)
}
