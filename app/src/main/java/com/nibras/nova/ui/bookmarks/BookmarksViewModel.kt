package com.nibras.nova.ui.bookmarks

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.util.BookmarkImportExportHelper
import com.nibras.nova.domain.model.BookmarkFolder
import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.domain.repository.BookmarkRepository
import com.nibras.nova.domain.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookmarksUiState(
    val currentFolderId: Long? = null,
    val searchQuery: String = "",
    val folders: List<BookmarkFolder> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val allFolders: List<BookmarkFolder> = emptyList()
)

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: com.nibras.nova.domain.repository.HistoryRepository,
    private val tabRepository: TabRepository,
    private val importExportHelper: BookmarkImportExportHelper
) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val foldersFlow = _currentFolderId.flatMapLatest { parentId ->
        bookmarkRepository.observeSubfolders(parentId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bookmarksFlow = combine(_currentFolderId, _searchQuery) { folderId, query ->
        folderId to query
    }.flatMapLatest { (folderId, query) ->
        if (query.isNotBlank()) {
            bookmarkRepository.searchBookmarks(query)
        } else {
            bookmarkRepository.observeBookmarksInFolder(folderId)
        }
    }

    val uiState: StateFlow<BookmarksUiState> = combine(
        _currentFolderId,
        _searchQuery,
        foldersFlow,
        bookmarksFlow,
        bookmarkRepository.observeAllFolders()
    ) { folderId, query, subfolders, items, allFolders ->
        BookmarksUiState(
            currentFolderId = folderId,
            searchQuery = query,
            folders = if (query.isBlank()) subfolders else emptyList(),
            bookmarks = items,
            allFolders = allFolders
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState())

    fun selectFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            bookmarkRepository.createFolder(name, _currentFolderId.value)
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            bookmarkRepository.deleteFolder(folderId)
        }
    }

    fun saveBookmark(id: Long = 0L, title: String, url: String, folderId: Long?) {
        viewModelScope.launch {
            bookmarkRepository.addOrUpdateBookmark(
                BookmarkItem(
                    id = id,
                    title = title,
                    url = url,
                    folderId = folderId
                )
            )
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(bookmarkId)
        }
    }

    fun exportAllBookmarksToHtml(activityContext: Context) {
        viewModelScope.launch {
            val all = bookmarkRepository.observeAllBookmarks().firstOrNull().orEmpty()
            if (all.isEmpty()) {
                _events.tryEmit(L.s(R.string.t_no_bookmarks_to_export))
                return@launch
            }
            importExportHelper.exportAndShareHtmlFile(activityContext, all)
        }
    }

    fun exportJsonTo(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val bms = bookmarkRepository.observeAllBookmarks().firstOrNull().orEmpty()
                val folders = bookmarkRepository.observeAllFolders().firstOrNull().orEmpty().associate { it.id to it.name }
                val hist = historyRepository.observeRecentHistory(100_000).firstOrNull().orEmpty()
                val text = com.nibras.nova.core.util.BrowserDataJson.encode(bms, hist, { id -> id?.let { folders[it] } })
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                }
                _events.tryEmit(L.s(R.string.bk_json_exported, bms.size, hist.size))
            }.onFailure { _events.tryEmit(L.s(R.string.bk_json_failed)) }
        }
    }

    fun importJsonFrom(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                val b = com.nibras.nova.core.util.BrowserDataJson.decode(text)
                val existing = bookmarkRepository.observeAllBookmarks().firstOrNull().orEmpty().map { it.url }.toHashSet()
                val folderIds = HashMap<String, Long>()
                bookmarkRepository.observeAllFolders().firstOrNull().orEmpty().forEach { folderIds[it.name] = it.id }
                var added = 0
                for (bm in b.bookmarks) {
                    if (bm.url in existing) continue
                    val fid = bm.folder?.let { name -> folderIds.getOrPut(name) { bookmarkRepository.createFolder(name) } }
                    bookmarkRepository.addOrUpdateBookmark(com.nibras.nova.domain.model.BookmarkItem(title = bm.title, url = bm.url, folderId = fid, createdAt = if (bm.createdAt > 0) bm.createdAt else System.currentTimeMillis()))
                    existing.add(bm.url); added++
                }
                val h = historyRepository.importHistory(b.history.map { com.nibras.nova.domain.model.HistoryItem(title = it.title, url = it.url, visitedAt = it.visitedAt) })
                _events.tryEmit(L.s(R.string.bk_json_imported, added, h))
            }.onFailure { _events.tryEmit(L.s(R.string.bk_json_failed)) }
        }
    }

    fun importBookmarksFromHtmlUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val htmlText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val parsed = importExportHelper.parseNetscapeBookmarkHtml(htmlText)
                for (bm in parsed) {
                    bookmarkRepository.addOrUpdateBookmark(bm.copy(folderId = _currentFolderId.value))
                }
                _events.tryEmit(L.s(R.string.t_imported_bookmarks_from_html_file, parsed.size))
            }.onFailure {
                _events.tryEmit(L.s(R.string.t_could_not_parse_bookmark_html_file))
            }
        }
    }

    fun openBookmark(url: String, isPrivate: Boolean, onReady: () -> Unit) {
        viewModelScope.launch {
            tabRepository.createTab(
                url = url,
                isPrivate = isPrivate,
                selectImmediately = true
            )
            onReady()
        }
    }
}
