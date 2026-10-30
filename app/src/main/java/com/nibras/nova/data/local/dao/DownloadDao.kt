package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nibras.nova.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :downloadId LIMIT 1")
    suspend fun getDownloadById(downloadId: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long, status: String)

    @Query("DELETE FROM downloads WHERE id = :downloadId")
    suspend fun deleteDownload(downloadId: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}
