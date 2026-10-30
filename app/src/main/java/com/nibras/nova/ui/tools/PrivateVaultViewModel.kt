package com.nibras.nova.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.security.BiometricLockHelper
import com.nibras.nova.core.security.ClipboardPrivacyGuard
import com.nibras.nova.core.security.GeneratedPasswordResult
import com.nibras.nova.core.security.PasswordGeneratorHelper
import com.nibras.nova.data.local.entity.PrivateVaultEntity
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
class PrivateVaultViewModel @Inject constructor(
    val biometricLockHelper: BiometricLockHelper,
    private val passwordGeneratorHelper: PasswordGeneratorHelper,
    private val clipboardPrivacyGuard: ClipboardPrivacyGuard,
    private val privacyRepository: PrivacyRepository,
    private val tabRepository: TabRepository
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _generatedPassword = MutableStateFlow(passwordGeneratorHelper.generateStrongPassword(20))
    val generatedPassword: StateFlow<GeneratedPasswordResult> = _generatedPassword.asStateFlow()

    val vaultItems: StateFlow<List<PrivateVaultEntity>> = privacyRepository.observePrivateVaultItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markUnlocked() {
        _isUnlocked.value = true
    }

    fun regeneratePassword(length: Int = 20, symbols: Boolean = true) {
        _generatedPassword.value = passwordGeneratorHelper.generateStrongPassword(
            length = length,
            includeUppercase = true,
            includeDigits = true,
            includeSymbols = symbols
        )
    }

    fun generateDicewarePassphrase() {
        val phrase = passwordGeneratorHelper.generateMemorablePassphrase(4)
        _generatedPassword.value = GeneratedPasswordResult(
            password = phrase,
            entropyBits = 64.0,
            strengthLabel = "Memorable Diceware Passphrase"
        )
    }

    fun copyAndSaveGeneratedPassword(label: String) {
        val current = _generatedPassword.value.password
        clipboardPrivacyGuard.copyCleanUrlToClipboard(current, autoClearAfterSeconds = 60L)
        viewModelScope.launch {
            privacyRepository.addPrivateVaultItem(
                title = label.ifBlank { "Generated Credential" },
                urlOrNote = current,
                category = "CREDENTIAL"
            )
        }
    }

    fun addVaultEntry(title: String, urlOrNote: String, category: String) {
        viewModelScope.launch {
            privacyRepository.addPrivateVaultItem(title, urlOrNote, category)
        }
    }

    fun deleteVaultEntry(id: Long) {
        viewModelScope.launch {
            privacyRepository.deletePrivateVaultItem(id)
        }
    }

    fun openVaultUrlPrivately(url: String, onReady: () -> Unit) {
        viewModelScope.launch {
            tabRepository.createTab(
                url = url,
                isPrivate = true,
                selectImmediately = true
            )
            onReady()
        }
    }
}
