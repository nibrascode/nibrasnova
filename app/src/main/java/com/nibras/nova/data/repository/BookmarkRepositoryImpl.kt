package com.nibras.nova.data.repository

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.data.local.dao.BookmarkDao
import com.nibras.nova.data.local.dao.BookmarkFolderDao
import com.nibras.nova.data.local.entity.BookmarkEntity
import com.nibras.nova.data.local.entity.BookmarkFolderEntity
import com.nibras.nova.domain.model.BookmarkFolder
import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val folderDao: BookmarkFolderDao,
    private val dispatchers: DispatcherProvider
) : BookmarkRepository {

    override fun observeAllFolders(): Flow<List<BookmarkFolder>> =
        folderDao.observeAllFolders().map { list -> list.map { it.toDomain() } }

    override fun observeSubfolders(parentFolderId: Long?): Flow<List<BookmarkFolder>> =
        folderDao.observeSubfolders(parentFolderId).map { list -> list.map { it.toDomain() } }

    override fun observeBookmarksInFolder(folderId: Long?): Flow<List<BookmarkItem>> =
        bookmarkDao.observeBookmarksInFolder(folderId).map { list -> list.map { it.toDomain() } }

    override fun observeAllBookmarks(): Flow<List<BookmarkItem>> =
        bookmarkDao.observeAllBookmarks().map { list -> list.map { it.toDomain() } }

    override fun searchBookmarks(query: String): Flow<List<BookmarkItem>> =
        bookmarkDao.searchBookmarks(query.trim()).map { list -> list.map { it.toDomain() } }

    override fun observeIsBookmarked(url: String): Flow<Boolean> =
        bookmarkDao.observeIsBookmarked(url)

    override suspend fun createFolder(name: String, parentFolderId: Long?): Long = withContext(dispatchers.io) {
        folderDao.insertFolder(
            BookmarkFolderEntity(
                name = name.trim().ifBlank { L.s(R.string.t_new_folder) },
                parentFolderId = parentFolderId
            )
        )
    }

    override suspend fun renameFolder(folder: BookmarkFolder, newName: String) = withContext(dispatchers.io) {
        folderDao.updateFolder(
            BookmarkFolderEntity(
                id = folder.id,
                name = newName.trim().ifBlank { folder.name },
                parentFolderId = folder.parentFolderId,
                createdAt = folder.createdAt
            )
        )
    }

    override suspend fun deleteFolder(folderId: Long) = withContext(dispatchers.io) {
        folderDao.deleteFolder(folderId)
    }

    override suspend fun toggleBookmark(
        title: String,
        url: String,
        faviconPath: String?,
        folderId: Long?
    ): Boolean = withContext(dispatchers.io) {
        val existing = bookmarkDao.getBookmarkByUrl(url)
        if (existing != null) {
            bookmarkDao.deleteBookmarkById(existing.id)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    title = title.trim().ifBlank { url },
                    url = url.trim(),
                    folderId = folderId,
                    faviconPath = faviconPath
                )
            )
            true
        }
    }

    override suspend fun addOrUpdateBookmark(item: BookmarkItem): Long = withContext(dispatchers.io) {
        val entity = BookmarkEntity(
            id = item.id,
            title = item.title.trim().ifBlank { item.url },
            url = item.url.trim(),
            folderId = item.folderId,
            faviconPath = item.faviconPath,
            createdAt = item.createdAt
        )
        if (item.id == 0L) {
            bookmarkDao.insertBookmark(entity)
        } else {
            bookmarkDao.updateBookmark(entity)
            item.id
        }
    }

    override suspend fun deleteBookmark(bookmarkId: Long) = withContext(dispatchers.io) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }
}
