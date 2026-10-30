package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nibras.nova.data.local.entity.BookmarkFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkFolderDao {
    @Query("SELECT * FROM bookmark_folders ORDER BY name ASC")
    fun observeAllFolders(): Flow<List<BookmarkFolderEntity>>

    @Query("SELECT * FROM bookmark_folders WHERE parentFolderId IS :parentFolderId ORDER BY name ASC")
    fun observeSubfolders(parentFolderId: Long?): Flow<List<BookmarkFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BookmarkFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: BookmarkFolderEntity)

    @Query("DELETE FROM bookmark_folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)
}
