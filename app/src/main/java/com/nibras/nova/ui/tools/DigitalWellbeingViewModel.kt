package com.nibras.nova.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.data.local.entity.SiteUsageEntity
import com.nibras.nova.domain.repository.PrivacyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DigitalWellbeingViewModel @Inject constructor(
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    val siteUsageList: StateFlow<List<SiteUsageEntity>> = privacyRepository.observeAllSiteUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDailyLimitForDomain(domain: String, limitMinutes: Int) {
        viewModelScope.launch {
            privacyRepository.setSiteDailyLimitMinutes(domain, limitMinutes)
        }
    }

    fun resetAllUsage() {
        viewModelScope.launch {
            privacyRepository.resetAllSiteUsageStats()
        }
    }
}
