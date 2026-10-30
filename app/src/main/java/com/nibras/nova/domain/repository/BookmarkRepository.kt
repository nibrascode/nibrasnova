package com.nibras.nova.domain.repository

import com.nibras.nova.domain.model.BookmarkFolder
import com.nibras.nova.domain.model.BookmarkItem
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeAllFolders(): Flow<List<BookmarkFolder>>
    fun observeSubfolders(parentFolderId: Long?): Flow<List<BookmarkFolder>>
    fun observeBookmarksInFolder(folderId: Long?): Flow<List<BookmarkItem>>
    fun observeAllBookmarks(): Flow<List<BookmarkItem>>
    fun searchBookmarks(query: String): Flow<List<BookmarkItem>>
    fun observeIsBookmarked(url: String): Flow<Boolean>
    suspend fun createFolder(name: String, parentFolderId: Long? = null): Long
    suspend fun renameFolder(folder: BookmarkFolder, newName: String)
    suspend fun deleteFolder(folderId: Long)
    suspend fun toggleBookmark(title: String, url: String, faviconPath: String? = null, folderId: Long? = null): Boolean
    suspend fun addOrUpdateBookmark(item: BookmarkItem): Long
    suspend fun deleteBookmark(bookmarkId: Long)
}
