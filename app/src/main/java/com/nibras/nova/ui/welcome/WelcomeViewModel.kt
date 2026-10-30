package com.nibras.nova.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.SearchEngineType
import com.nibras.nova.domain.model.ThemeMode
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.repository.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val shortcutRepository: ShortcutRepository
) : ViewModel() {

    val settings: StateFlow<BrowserSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserSettings())

    fun selectSearchEngine(engine: SearchEngineType) {
        viewModelScope.launch {
            settingsRepository.updateSearchEngine(engine, null)
        }
    }

    fun selectThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun toggleHttpsOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHttpsOnlyMode(enabled)
        }
    }

    fun toggleBlockThirdPartyCookies(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBlockThirdPartyCookies(enabled)
        }
    }

    fun completeSetup(onFinished: () -> Unit) {
        viewModelScope.launch {
            shortcutRepository.ensureDefaultShortcutsSeeded()
            settingsRepository.completeOnboarding()
            onFinished()
        }
    }
}
