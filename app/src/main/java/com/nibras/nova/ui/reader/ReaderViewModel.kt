package com.nibras.nova.ui.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.util.ExtractiveSummaryResult
import com.nibras.nova.core.util.OnDeviceSummarizer
import com.nibras.nova.core.util.ShareHelper
import com.nibras.nova.core.util.TtsPlaybackState
import com.nibras.nova.core.util.TtsReaderManager
import com.nibras.nova.domain.model.ReaderArticle
import com.nibras.nova.domain.model.ReaderTheme
import com.nibras.nova.domain.model.SavedOfflinePage
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.TabRepository
import com.nibras.nova.engine.ReaderModeJsBridge
import com.nibras.nova.engine.WebViewPoolManager
import com.nibras.nova.ui.tools.TranslationHelper
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

data class ReaderUiState(
    val article: ReaderArticle? = null,
    val isLoading: Boolean = true,
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.55f,
    val readerTheme: ReaderTheme = ReaderTheme.DARK,
    val translatedText: String? = null,
    val isTranslating: Boolean = false,
    val savedOfflinePages: List<SavedOfflinePage> = emptyList(),
    val statusMessage: String? = null,
    val ttsState: TtsPlaybackState = TtsPlaybackState(),
    val extractiveSummary: ExtractiveSummaryResult? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    private val webViewPoolManager: WebViewPoolManager,
    private val readerModeJsBridge: ReaderModeJsBridge,
    private val privacyRepository: PrivacyRepository,
    private val translationHelper: TranslationHelper,
    private val shareHelper: ShareHelper,
    private val ttsReaderManager: TtsReaderManager,
    private val onDeviceSummarizer: OnDeviceSummarizer
) : ViewModel() {

    private val _localState = MutableStateFlow(ReaderUiState())

    val uiState: StateFlow<ReaderUiState> = combine(
        _localState,
        privacyRepository.observeSavedOfflinePages(),
        ttsReaderManager.state
    ) { local, savedPages, tts ->
        local.copy(
            savedOfflinePages = savedPages,
            ttsState = tts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        extractFromActiveTab()
    }

    fun extractFromActiveTab() {
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true, translatedText = null, extractiveSummary = null) }
            val activeTab = tabRepository.observeActiveTab().firstOrNull()
            val wv = activeTab?.let { webViewPoolManager.getLiveWebView(it.id) }
            if (wv == null) {
                _localState.update { it.copy(isLoading = false, article = null) }
                return@launch
            }
            val extracted = readerModeJsBridge.evaluateCurrentPageReadability(wv)
            val summary = extracted?.plainText?.takeIf { it.isNotBlank() }?.let {
                onDeviceSummarizer.summarizeArticle(it, maxBullets = 4)
            }
            _localState.update {
                it.copy(
                    isLoading = false,
                    article = extracted,
                    extractiveSummary = summary
                )
            }
        }
    }

    fun generateOnDeviceSummary() {
        val text = _localState.value.translatedText ?: _localState.value.article?.plainText.orEmpty()
        if (text.isBlank()) return
        val summary = onDeviceSummarizer.summarizeArticle(text, maxBullets = 4)
        _localState.update { it.copy(extractiveSummary = summary) }
    }

    fun toggleTtsSpeech() {
        if (ttsReaderManager.state.value.isSpeaking) {
            ttsReaderManager.stopSpeaking()
        } else {
            val text = _localState.value.translatedText ?: _localState.value.article?.plainText.orEmpty()
            ttsReaderManager.speakArticleText(text)
        }
    }

    fun changeTtsSpeechRate(rate: Float) {
        ttsReaderManager.setSpeechRate(rate)
    }

    fun adjustFontSize(delta: Float) {
        _localState.update {
            it.copy(fontSizeSp = (it.fontSizeSp + delta).coerceIn(14f, 30f))
        }
    }

    fun adjustLineSpacing(delta: Float) {
        _localState.update {
            it.copy(lineHeightMultiplier = (it.lineHeightMultiplier + delta).coerceIn(1.2f, 2.2f))
        }
    }

    fun selectReaderTheme(theme: ReaderTheme) {
        _localState.update { it.copy(readerTheme = theme) }
    }

    fun translateArticleOnDevice(targetLangCode: String) {
        val currentArticle = _localState.value.article ?: return
        if (currentArticle.plainText.isBlank()) return
        viewModelScope.launch {
            _localState.update { it.copy(isTranslating = true, statusMessage = "Translating article on-device…") }
            val result = translationHelper.translateText(
                sourceText = currentArticle.plainText.take(4000),
                targetLanguageCode = targetLangCode
            )
            result.onSuccess { translated ->
                _localState.update {
                    it.copy(
                        isTranslating = false,
                        translatedText = translated,
                        statusMessage = "Translated to ${targetLangCode.uppercase()}"
                    )
                }
            }.onFailure { err ->
                _localState.update {
                    it.copy(
                        isTranslating = false,
                        statusMessage = err.localizedMessage ?: "Translation model download required"
                    )
                }
            }
        }
    }

    fun saveArticleForOfflineReading() {
        val article = _localState.value.article ?: return
        if (article.plainText.isBlank()) return
        viewModelScope.launch {
            privacyRepository.saveOfflineArticle(
                title = article.title,
                url = article.url,
                siteName = article.siteName,
                plainText = article.plainText,
                estimatedReadMinutes = article.estimatedReadMinutes
            )
            _localState.update { it.copy(statusMessage = "Saved article text for offline reading") }
        }
    }

    fun loadSavedOfflineArticle(saved: SavedOfflinePage) {
        val summary = onDeviceSummarizer.summarizeArticle(saved.plainTextContent, maxBullets = 4)
        _localState.update {
            it.copy(
                article = ReaderArticle(
                    isReadable = true,
                    title = saved.title,
                    byline = saved.siteName,
                    siteName = saved.siteName,
                    url = saved.url,
                    wordCount = saved.plainTextContent.split("\\s+".toRegex()).size,
                    estimatedReadMinutes = saved.estimatedReadMinutes,
                    blocks = saved.plainTextContent.split("\n\n").map { p ->
                        com.nibras.nova.domain.model.ReaderBlock(tag = "p", text = p)
                    },
                    plainText = saved.plainTextContent
                ),
                translatedText = null,
                extractiveSummary = summary
            )
        }
    }

    fun deleteSavedOfflineArticle(id: Long) {
        viewModelScope.launch {
            privacyRepository.deleteSavedOfflinePage(id)
        }
    }

    fun shareArticleText(activityContext: Context) {
        val article = _localState.value.article ?: return
        val body = _localState.value.translatedText ?: article.plainText
        shareHelper.shareSelectedText(activityContext, "${article.title}\n${article.url}\n\n$body")
    }
}
