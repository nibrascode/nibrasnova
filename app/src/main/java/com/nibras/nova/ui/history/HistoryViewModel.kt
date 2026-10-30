package com.nibras.nova.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.domain.model.HistoryClearRange
import com.nibras.nova.domain.model.HistoryItem
import com.nibras.nova.domain.repository.HistoryRepository
import com.nibras.nova.domain.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val searchQuery: String = "",
    val historyItems: List<HistoryItem> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val tabRepository: TabRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val itemsFlow = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            historyRepository.observeRecentHistory(limit = 500)
        } else {
            historyRepository.searchHistory(query)
        }
    }

    val uiState: StateFlow<HistoryUiState> = combine(_searchQuery, itemsFlow) { query, items ->
        HistoryUiState(searchQuery = query, historyItems = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteHistoryItem(id)
        }
    }

    fun clearByRange(range: HistoryClearRange) {
        viewModelScope.launch {
            historyRepository.clearHistoryByRange(range)
        }
    }

    fun openHistoryUrl(url: String, onReady: () -> Unit) {
        viewModelScope.launch {
            tabRepository.createTab(url = url, isPrivate = false, selectImmediately = true)
            onReady()
        }
    }
}
