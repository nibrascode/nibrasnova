package com.nibras.nova.data.repository

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.core.common.NovaConstants
import com.nibras.nova.data.local.dao.HistoryDao
import com.nibras.nova.data.local.entity.HistoryEntity
import com.nibras.nova.domain.model.HistoryClearRange
import com.nibras.nova.domain.model.HistoryItem
import com.nibras.nova.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    private val dispatchers: DispatcherProvider
) : HistoryRepository {

    override fun observeRecentHistory(limit: Int): Flow<List<HistoryItem>> =
        historyDao.observeRecentHistory(limit).map { list -> list.map { it.toDomain() } }

    override fun searchHistory(query: String): Flow<List<HistoryItem>> =
        historyDao.searchHistory(query.trim()).map { list -> list.map { it.toDomain() } }

    override suspend fun importHistory(items: List<com.nibras.nova.domain.model.HistoryItem>): Int = withContext(dispatchers.io) {
        var n = 0
        items.forEach { h ->
            val u = h.url.trim()
            if (u.startsWith("http://") || u.startsWith("https://")) {
                historyDao.insertHistory(HistoryEntity(title = h.title.ifBlank { u }, url = u, faviconPath = null, visitedAt = h.visitedAt)); n++
            }
        }
        n
    }

    override suspend fun recordVisit(
        title: String,
        url: String,
        faviconPath: String?,
        isPrivateSession: Boolean
    ) = withContext(dispatchers.io) {
        // Strict privacy rule: NEVER store private browsing history
        if (isPrivateSession) return@withContext
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() ||
            cleanUrl.equals(NovaConstants.DEFAULT_HOMEPAGE_SENTINEL, ignoreCase = true) ||
            cleanUrl.equals("about:blank", ignoreCase = true)
        ) {
            return@withContext
        }
        historyDao.insertHistory(
            HistoryEntity(
                title = title.trim().ifBlank { cleanUrl },
                url = cleanUrl,
                faviconPath = faviconPath,
                visitedAt = System.currentTimeMillis()
            )
        )
        Unit
    }

    override suspend fun deleteHistoryItem(historyId: Long) = withContext(dispatchers.io) {
        historyDao.deleteHistoryItem(historyId)
    }

    override suspend fun clearHistoryByRange(range: HistoryClearRange) = withContext(dispatchers.io) {
        val duration = range.durationMillis
        if (duration == null) {
            historyDao.clearAllHistory()
        } else {
            val cutoff = System.currentTimeMillis() - duration
            historyDao.deleteHistorySince(cutoff)
        }
    }
}
