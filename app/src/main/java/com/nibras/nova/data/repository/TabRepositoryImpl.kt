package com.nibras.nova.data.repository

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.core.common.NovaConstants
import com.nibras.nova.core.util.TabThumbnailManager
import com.nibras.nova.data.local.dao.TabDao
import com.nibras.nova.data.local.entity.TabEntity
import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.model.TabWorkspace
import com.nibras.nova.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabRepositoryImpl @Inject constructor(
    private val tabDao: TabDao,
    private val thumbnailManager: TabThumbnailManager,
    private val dispatchers: DispatcherProvider
) : TabRepository {

    private val mutex = Mutex()

    private val _privateTabs = MutableStateFlow<List<TabItem>>(emptyList())
    private val _isPrivateModeActive = MutableStateFlow(false)

    private val closedNormalTabsStack = ArrayDeque<TabItem>()
    private val _recentlyClosedCount = MutableStateFlow(0)

    override fun observeNormalTabs(): Flow<List<TabItem>> =
        tabDao.observeNormalTabs().map { entities -> entities.map { it.toDomain() } }

    override fun observePrivateTabs(): Flow<List<TabItem>> = _privateTabs.asStateFlow()

    override fun observeRecentlyClosedCount(): Flow<Int> = _recentlyClosedCount.asStateFlow()

    override fun observeActiveTab(): Flow<TabItem?> = combine(
        observeNormalTabs(),
        _privateTabs,
        _isPrivateModeActive
    ) { normalTabs, privateTabs, isPrivateMode ->
        if (isPrivateMode && privateTabs.isNotEmpty()) {
            privateTabs.firstOrNull { it.isActive } ?: privateTabs.lastOrNull()
        } else {
            normalTabs.firstOrNull { it.isActive } ?: normalTabs.lastOrNull()
        }
    }

    override suspend fun initializeOrRestoreTabs(
        restoreOnStartup: Boolean,
        defaultDesktopMode: Boolean
    ) = withContext(dispatchers.io) {
        mutex.withLock {
            if (!restoreOnStartup) {
                tabDao.deleteAllNormalTabs()
            }
            val existing = tabDao.getAllNormalTabsOnce()
            if (existing.isEmpty()) {
                val initialTab = TabItem(
                    id = UUID.randomUUID().toString(),
                    url = NovaConstants.DEFAULT_HOMEPAGE_SENTINEL,
                    title = L.s(R.string.t_new_tab),
                    isPrivate = false,
                    isDesktopMode = defaultDesktopMode,
                    isActive = true,
                    workspace = TabWorkspace.GENERAL
                )
                tabDao.upsertTab(TabEntity.fromDomain(initialTab))
            } else if (existing.none { it.isActive }) {
                tabDao.setActiveTab(existing.last().id)
            }
        }
    }

    override suspend fun createTab(
        url: String,
        isPrivate: Boolean,
        isDesktopMode: Boolean,
        selectImmediately: Boolean,
        workspace: TabWorkspace,
        parentTabId: String?
    ): TabItem = withContext(dispatchers.io) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val newTab = TabItem(
                id = UUID.randomUUID().toString(),
                url = url.ifBlank { NovaConstants.DEFAULT_HOMEPAGE_SENTINEL },
                title = if (url.isBlank() || url == NovaConstants.DEFAULT_HOMEPAGE_SENTINEL) L.s(R.string.t_new_tab) else url,
                isPrivate = isPrivate,
                isDesktopMode = isDesktopMode,
                isActive = selectImmediately,
                workspace = workspace,
                parentTabId = parentTabId,
                createdAt = now,
                lastAccessedAt = now
            )

            if (isPrivate) {
                val updated = _privateTabs.value.map {
                    if (selectImmediately) it.copy(isActive = false) else it
                } + newTab
                _privateTabs.value = updated
                if (selectImmediately) {
                    _isPrivateModeActive.value = true
                }
            } else {
                val existing = tabDao.getAllNormalTabsOnce().map { it.toDomain() }
                val updated = existing.map {
                    if (selectImmediately) it.copy(isActive = false) else it
                } + newTab
                tabDao.replaceAllNormalTabs(updated.map { TabEntity.fromDomain(it) })
                if (selectImmediately) {
                    _isPrivateModeActive.value = false
                }
            }
            newTab
        }
    }

    override suspend fun switchToTab(tabId: String) = withContext(dispatchers.io) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val privateMatch = _privateTabs.value.any { it.id == tabId }
            if (privateMatch) {
                _privateTabs.value = _privateTabs.value.map {
                    it.copy(
                        isActive = it.id == tabId,
                        lastAccessedAt = if (it.id == tabId) now else it.lastAccessedAt
                    )
                }
                _isPrivateModeActive.value = true
            } else {
                val normalTabs = tabDao.getAllNormalTabsOnce().map { it.toDomain() }
                if (normalTabs.any { it.id == tabId }) {
                    val updated = normalTabs.map {
                        it.copy(
                            isActive = it.id == tabId,
                            lastAccessedAt = if (it.id == tabId) now else it.lastAccessedAt
                        )
                    }
                    tabDao.replaceAllNormalTabs(updated.map { TabEntity.fromDomain(it) })
                    _isPrivateModeActive.value = false
                }
            }
        }
    }

    override suspend fun updateTabMetadata(
        tabId: String,
        url: String?,
        title: String?,
        isDesktopMode: Boolean?,
        workspace: TabWorkspace?,
        thumbnailPath: String?,
        faviconPath: String?
    ) = withContext(dispatchers.io) {
        mutex.withLock {
            if (_privateTabs.value.any { it.id == tabId }) {
                _privateTabs.value = _privateTabs.value.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(
                            url = url ?: tab.url,
                            title = title ?: tab.title,
                            isDesktopMode = isDesktopMode ?: tab.isDesktopMode,
                            workspace = workspace ?: tab.workspace,
                            thumbnailPath = thumbnailPath ?: tab.thumbnailPath,
                            faviconPath = faviconPath ?: tab.faviconPath,
                            lastAccessedAt = System.currentTimeMillis()
                        )
                    } else tab
                }
            } else {
                val normalTabs = tabDao.getAllNormalTabsOnce().map { it.toDomain() }
                val target = normalTabs.firstOrNull { it.id == tabId } ?: return@withLock
                val updated = target.copy(
                    url = url ?: target.url,
                    title = title ?: target.title,
                    isDesktopMode = isDesktopMode ?: target.isDesktopMode,
                    workspace = workspace ?: target.workspace,
                    thumbnailPath = thumbnailPath ?: target.thumbnailPath,
                    faviconPath = faviconPath ?: target.faviconPath,
                    lastAccessedAt = System.currentTimeMillis()
                )
                tabDao.upsertTab(TabEntity.fromDomain(updated))
            }
        }
    }

    override suspend fun duplicateTab(tabId: String): TabItem? = withContext(dispatchers.io) {
        val sourceTab = _privateTabs.value.firstOrNull { it.id == tabId }
            ?: tabDao.getAllNormalTabsOnce().firstOrNull { it.id == tabId }?.toDomain()
            ?: return@withContext null

        createTab(
            url = sourceTab.url,
            isPrivate = sourceTab.isPrivate,
            isDesktopMode = sourceTab.isDesktopMode,
            selectImmediately = true,
            workspace = sourceTab.workspace,
            parentTabId = sourceTab.id
        )
    }

    override suspend fun closeTab(tabId: String) = withContext(dispatchers.io) {
        mutex.withLock {
            val privateMatch = _privateTabs.value.firstOrNull { it.id == tabId }
            if (privateMatch != null) {
                thumbnailManager.deleteThumbnail(tabId, isPrivate = true)
                val remaining = _privateTabs.value.filterNot { it.id == tabId }
                if (remaining.isEmpty()) {
                    _privateTabs.value = emptyList()
                    _isPrivateModeActive.value = false
                } else {
                    val hasActive = remaining.any { it.isActive }
                    _privateTabs.value = if (hasActive) remaining else {
                        val lastId = remaining.last().id
                        remaining.map { it.copy(isActive = it.id == lastId) }
                    }
                }
                return@withLock
            }

            val normalTabs = tabDao.getAllNormalTabsOnce().map { it.toDomain() }
            val closingTab = normalTabs.firstOrNull { it.id == tabId } ?: return@withLock
            if (closedNormalTabsStack.size >= NovaConstants.MAX_CLOSED_TAB_UNDO_STACK) {
                closedNormalTabsStack.removeFirst()
            }
            closedNormalTabsStack.addLast(closingTab)
            _recentlyClosedCount.value = closedNormalTabsStack.size

            val remaining = normalTabs.filterNot { it.id == tabId }
            if (remaining.isEmpty()) {
                val replacement = TabItem(
                    id = UUID.randomUUID().toString(),
                    url = NovaConstants.DEFAULT_HOMEPAGE_SENTINEL,
                    title = L.s(R.string.t_new_tab),
                    isPrivate = false,
                    isActive = true,
                    workspace = closingTab.workspace
                )
                tabDao.replaceAllNormalTabs(listOf(TabEntity.fromDomain(replacement)))
            } else {
                val hasActive = remaining.any { it.isActive }
                val adjusted = if (hasActive) remaining else {
                    val fallbackId = remaining.last().id
                    remaining.map { it.copy(isActive = it.id == fallbackId) }
                }
                tabDao.replaceAllNormalTabs(adjusted.map { TabEntity.fromDomain(it) })
            }
        }
    }

    override suspend fun reopenLastClosedTab(): TabItem? = withContext(dispatchers.io) {
        val restored = mutex.withLock {
            if (closedNormalTabsStack.isEmpty()) return@withLock null
            val last = closedNormalTabsStack.removeLast()
            _recentlyClosedCount.value = closedNormalTabsStack.size
            last
        } ?: return@withContext null

        createTab(
            url = restored.url,
            isPrivate = false,
            isDesktopMode = restored.isDesktopMode,
            selectImmediately = true,
            workspace = restored.workspace
        )
    }

    override suspend fun closeAllTabs(isPrivateMode: Boolean) = withContext(dispatchers.io) {
        mutex.withLock {
            if (isPrivateMode) {
                thumbnailManager.clearAllIncognitoThumbnails()
                _privateTabs.value = emptyList()
                _isPrivateModeActive.value = false
            } else {
                val freshTab = TabItem(
                    id = UUID.randomUUID().toString(),
                    url = NovaConstants.DEFAULT_HOMEPAGE_SENTINEL,
                    title = L.s(R.string.t_new_tab),
                    isPrivate = false,
                    isActive = true
                )
                tabDao.replaceAllNormalTabs(listOf(TabEntity.fromDomain(freshTab)))
                _isPrivateModeActive.value = false
            }
        }
    }
}
