package com.nibras.nova.domain.repository

import com.nibras.nova.domain.model.HistoryClearRange
import com.nibras.nova.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeRecentHistory(limit: Int = 500): Flow<List<HistoryItem>>
    fun searchHistory(query: String): Flow<List<HistoryItem>>
    suspend fun recordVisit(title: String, url: String, faviconPath: String?, isPrivateSession: Boolean)
    suspend fun deleteHistoryItem(historyId: Long)
    suspend fun clearHistoryByRange(range: HistoryClearRange)
}
