package com.nibras.nova.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.util.RssFeedArticleItem
import com.nibras.nova.core.util.RssFeedParser
import com.nibras.nova.data.local.entity.RssFeedEntity
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RssReaderUiState(
    val feeds: List<RssFeedEntity> = emptyList(),
    val articles: List<RssFeedArticleItem> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class RssReaderViewModel @Inject constructor(
    private val privacyRepository: PrivacyRepository,
    private val tabRepository: TabRepository,
    private val rssFeedParser: RssFeedParser
) : ViewModel() {

    private val _articlesState = MutableStateFlow<List<RssFeedArticleItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _status = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RssReaderUiState> = combine(
        privacyRepository.observeRssFeeds(),
        _articlesState,
        _isLoading,
        _status
    ) { feeds, articles, loading, status ->
        RssReaderUiState(
            feeds = feeds,
            articles = articles,
            isLoading = loading,
            statusMessage = status
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RssReaderUiState())

    init {
        viewModelScope.launch {
            privacyRepository.ensureDefaultRssFeedsSeeded()
            refreshAllFeeds()
        }
    }

    fun refreshAllFeeds() {
        viewModelScope.launch {
            _isLoading.value = true
            val feeds = privacyRepository.observeRssFeeds().firstOrNull().orEmpty()
            val combined = mutableListOf<RssFeedArticleItem>()
            for (feed in feeds) {
                rssFeedParser.fetchAndParseFeed(feed.feedUrl, feed.title)
                    .onSuccess { combined.addAll(it) }
            }
            _articlesState.value = combined
            _isLoading.value = false
        }
    }

    fun addSubscription(title: String, feedUrl: String) {
        viewModelScope.launch {
            privacyRepository.addRssFeed(title, feedUrl)
            refreshAllFeeds()
        }
    }

    fun removeSubscription(id: Long) {
        viewModelScope.launch {
            privacyRepository.deleteRssFeed(id)
            refreshAllFeeds()
        }
    }

    fun openArticleInBrowser(url: String, onReady: () -> Unit) {
        viewModelScope.launch {
            tabRepository.createTab(url = url, isPrivate = false, selectImmediately = true)
            onReady()
        }
    }
}
