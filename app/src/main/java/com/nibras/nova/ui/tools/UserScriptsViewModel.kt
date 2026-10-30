package com.nibras.nova.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.domain.model.ScriptInjectionType
import com.nibras.nova.domain.model.UserScriptItem
import com.nibras.nova.domain.repository.UserScriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserScriptsViewModel @Inject constructor(
    private val userScriptRepository: UserScriptRepository
) : ViewModel() {

    val scripts: StateFlow<List<UserScriptItem>> = userScriptRepository.observeAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            userScriptRepository.ensureDefaultBuiltInScriptsSeeded()
        }
    }

    fun saveScript(
        id: Long = 0L,
        name: String,
        domainPattern: String,
        type: ScriptInjectionType,
        code: String
    ) {
        viewModelScope.launch {
            userScriptRepository.addOrUpdateScript(
                id = id,
                name = name,
                matchDomainPattern = domainPattern,
                scriptType = type,
                codeContent = code,
                isEnabled = true
            )
        }
    }

    fun toggleScript(item: UserScriptItem, enabled: Boolean) {
        viewModelScope.launch {
            userScriptRepository.toggleScriptEnabled(item, enabled)
        }
    }

    fun deleteScript(id: Long) {
        viewModelScope.launch {
            userScriptRepository.deleteScript(id)
        }
    }
}
