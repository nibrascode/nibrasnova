package com.nibras.nova.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.data.local.entity.PageWatchEntity
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PageMonitorViewModel @Inject constructor(
    private val privacyRepository: PrivacyRepository,
    private val tabRepository: TabRepository
) : ViewModel() {

    val watchedPages: StateFlow<List<PageWatchEntity>> = privacyRepository.observeWatchedPages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _lastCheckSummary = MutableStateFlow<String?>(null)
    val lastCheckSummary: StateFlow<String?> = _lastCheckSummary.asStateFlow()

    fun addWatchedPage(title: String, url: String) {
        viewModelScope.launch {
            _isChecking.value = true
            privacyRepository.addPageWatch(title, url)
            _isChecking.value = false
        }
    }

    fun checkAllNow() {
        viewModelScope.launch {
            _isChecking.value = true
            val changedCount = privacyRepository.checkAllWatchedPagesForChanges()
            _lastCheckSummary.value = if (changedCount > 0) {
                "$changedCount monitored page(s) have updated content!"
            } else {
                "All monitored pages checked — no content changes detected."
            }
            _isChecking.value = false
        }
    }

    fun deleteWatchedPage(id: Long) {
        viewModelScope.launch {
            privacyRepository.deletePageWatch(id)
        }
    }

    fun openWatchedPage(url: String, onReady: () -> Unit) {
        viewModelScope.launch {
            tabRepository.createTab(url = url, isPrivate = false, selectImmediately = true)
            onReady()
        }
    }
}
