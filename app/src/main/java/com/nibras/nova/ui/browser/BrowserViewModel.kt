package com.nibras.nova.ui.browser

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.security.AntiFingerprintInjector
import com.nibras.nova.core.security.ClipboardPrivacyGuard
import com.nibras.nova.core.security.FileUploadValidator
import com.nibras.nova.core.security.HomographAssessment
import com.nibras.nova.core.security.HomographAttackDetector
import com.nibras.nova.core.security.NavigationDisposition
import com.nibras.nova.core.security.SslCertificateChallenge
import com.nibras.nova.core.util.FaviconCacheManager
import com.nibras.nova.core.util.FullPageScreenshotHelper
import com.nibras.nova.core.util.PrintHelper
import com.nibras.nova.core.util.ShareHelper
import com.nibras.nova.core.util.ShortcutHelper
import com.nibras.nova.core.util.TabThumbnailManager
import com.nibras.nova.core.util.TtsReaderManager
import com.nibras.nova.data.local.entity.WebAnnotationEntity
import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.ConsoleLogLevel
import com.nibras.nova.domain.model.DevConsoleEntry
import com.nibras.nova.domain.model.ExternalLinkPolicy
import com.nibras.nova.domain.model.HistoryItem
import com.nibras.nova.domain.model.NetworkResourceLog
import com.nibras.nova.domain.model.PermissionDecision
import com.nibras.nova.domain.model.ReaderArticle
import com.nibras.nova.domain.model.ScriptInjectionType
import com.nibras.nova.domain.model.SearchEngineConfig
import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.repository.BookmarkRepository
import com.nibras.nova.domain.repository.DownloadRepository
import com.nibras.nova.domain.repository.HistoryRepository
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.repository.TabRepository
import com.nibras.nova.domain.repository.UserScriptRepository
import com.nibras.nova.domain.usecase.RecordHistoryUseCase
import com.nibras.nova.domain.usecase.ResolveInputToUrlUseCase
import com.nibras.nova.domain.usecase.ResolvedNavigationTarget
import com.nibras.nova.engine.ChromeSessionCallback
import com.nibras.nova.engine.ExternalIntentHandler
import com.nibras.nova.engine.NovaDownloadListener
import com.nibras.nova.engine.PendingDownloadRequest
import com.nibras.nova.engine.ReaderModeJsBridge
import com.nibras.nova.engine.WebViewPoolManager
import com.nibras.nova.engine.WebViewSessionCallback
import com.nibras.nova.engine.WebsitePermissionPrompt
import com.nibras.nova.ui.tools.PageStyleInspectionReport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject

data class WebErrorState(
    val failingUrl: String,
    val errorCode: Int,
    val description: String
)

data class BrowserUiState(
    val activeTab: TabItem? = null,
    val normalTabs: List<TabItem> = emptyList(),
    val privateTabs: List<TabItem> = emptyList(),
    val settings: BrowserSettings = BrowserSettings(),
    val normalTabCount: Int = 1,
    val privateTabCount: Int = 0,
    val isLoading: Boolean = false,
    val loadingProgress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isBookmarked: Boolean = false,
    val isReaderAvailable: Boolean = false,
    val latestReaderArticle: ReaderArticle? = null,
    val activeSslChallenge: SslCertificateChallenge? = null,
    val activePermissionPrompt: WebsitePermissionPrompt? = null,
    val pendingExternalAppPrompt: NavigationDisposition.PromptExternalApp? = null,
    val webErrorState: WebErrorState? = null,
    val isFindInPageVisible: Boolean = false,
    val findQuery: String = "",
    val findActiveOrdinal: Int = 0,
    val findTotalMatches: Int = 0,
    val addressBarSuggestions: List<String> = emptyList(),
    val showQrGeneratorForUrl: String? = null,
    // Stage 10 DevTools & TTS
    val isDevToolsSheetOpen: Boolean = false,
    val consoleLogs: List<DevConsoleEntry> = emptyList(),
    val networkLogs: List<NetworkResourceLog> = emptyList(),
    val domSourcePreview: String = "",
    val storageSummary: String = "",
    val isLiveDomDesignModeEnabled: Boolean = false,
    val isTtsSpeaking: Boolean = false,
    // Stage 11 & 12 Ultimate State
    val isSplitScreenActive: Boolean = false,
    val splitSecondaryUrl: String = "https://duckduckgo.com",
    val siteTextZoomPercent: Int = 100,
    val isOmniCommandPaletteOpen: Boolean = false,
    val isMediaGrabberSheetOpen: Boolean = false,
    val extractedMediaUrls: List<String> = emptyList(),
    val isWebNotesSheetOpen: Boolean = false,
    val currentSelectedTextOnPage: String = "",
    val annotationsForActivePage: List<WebAnnotationEntity> = emptyList(),
    val formattedJsonXmlPayload: String? = null,
    // Stage 13 Hyper Features
    val homographWarning: HomographAssessment? = null,
    val isAutoScrollActive: Boolean = false,
    val colorFontReport: PageStyleInspectionReport? = null,
    val isDomainFocusLimitReached: Boolean = false
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val webViewPoolManager: WebViewPoolManager,
    private val tabRepository: TabRepository,
    private val settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val downloadRepository: DownloadRepository,
    private val privacyRepository: PrivacyRepository,
    private val userScriptRepository: UserScriptRepository,
    private val resolveInputToUrlUseCase: ResolveInputToUrlUseCase,
    private val recordHistoryUseCase: RecordHistoryUseCase,
    private val readerModeJsBridge: ReaderModeJsBridge,
    private val antiFingerprintInjector: AntiFingerprintInjector,
    private val homographAttackDetector: HomographAttackDetector,
    private val clipboardPrivacyGuard: ClipboardPrivacyGuard,
    private val faviconCacheManager: FaviconCacheManager,
    private val thumbnailManager: TabThumbnailManager,
    private val fullPageScreenshotHelper: FullPageScreenshotHelper,
    private val fileUploadValidator: FileUploadValidator,
    private val externalIntentHandler: ExternalIntentHandler,
    private val shareHelper: ShareHelper,
    private val printHelper: PrintHelper,
    private val shortcutHelper: ShortcutHelper,
    private val ttsReaderManager: TtsReaderManager
) : ViewModel(), WebViewSessionCallback, ChromeSessionCallback {

    private val _runtimeState = MutableStateFlow(BrowserUiState())
    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _fullScreenCustomView = MutableStateFlow<Pair<View, WebChromeClient.CustomViewCallback>?>(null)
    val fullScreenCustomView = _fullScreenCustomView.asStateFlow()

    private var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val _launchFilePickerEvent = MutableSharedFlow<Array<String>>(extraBufferCapacity = 1)
    val launchFilePickerEvent = _launchFilePickerEvent.asSharedFlow()

    private var autoScrollJob: Job? = null
    private var usageTrackingJob: Job? = null

    val downloadListener = NovaDownloadListener { request ->
        onDownloadRequested(request)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val isCurrentUrlBookmarkedFlow = tabRepository.observeActiveTab()
        .flatMapLatest { tab ->
            if (tab == null || tab.isHomePage) flowOf(false)
            else bookmarkRepository.observeIsBookmarked(tab.url)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activePageAnnotationsFlow = tabRepository.observeActiveTab()
        .flatMapLatest { tab ->
            if (tab == null || tab.isHomePage) flowOf(emptyList())
            else privacyRepository.observeAnnotationsForUrl(tab.url)
        }

    val uiState: StateFlow<BrowserUiState> = combine(
        tabRepository.observeActiveTab(),
        settingsRepository.observeSettings(),
        tabRepository.observeNormalTabs(),
        tabRepository.observePrivateTabs(),
        isCurrentUrlBookmarkedFlow,
        activePageAnnotationsFlow,
        _runtimeState,
        ttsReaderManager.state
    ) { values ->
        val activeTab = values[0] as TabItem?
        val settings = values[1] as BrowserSettings
        @Suppress("UNCHECKED_CAST")
        val normalTabs = values[2] as List<TabItem>
        @Suppress("UNCHECKED_CAST")
        val privateTabs = values[3] as List<TabItem>
        val isBookmarked = values[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val annotations = values[5] as List<WebAnnotationEntity>
        val runtime = values[6] as BrowserUiState
        val ttsState = values[7] as com.nibras.nova.core.util.TtsPlaybackState

        runtime.copy(
            activeTab = activeTab,
            normalTabs = normalTabs,
            privateTabs = privateTabs,
            settings = settings,
            normalTabCount = normalTabs.size.coerceAtLeast(1),
            privateTabCount = privateTabs.size,
            isBookmarked = isBookmarked,
            annotationsForActivePage = annotations,
            isTtsSpeaking = ttsState.isSpeaking
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserUiState())

    init {
        viewModelScope.launch {
            userScriptRepository.ensureDefaultBuiltInScriptsSeeded()
            settingsRepository.observeSettings().collect { settings ->
                tabRepository.initializeOrRestoreTabs(
                    restoreOnStartup = settings.restoreTabsOnStartup,
                    defaultDesktopMode = settings.defaultDesktopMode
                )
            }
        }
        startLocalDomainTimeTracker()
    }

    // Stage 13.9: Local Digital Wellbeing Active Domain Time Tracker (ticks every 10s when browsing non-private site)
    private fun startLocalDomainTimeTracker() {
        usageTrackingJob?.cancel()
        usageTrackingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000L)
                val active = uiState.value.activeTab
                if (active != null && !active.isPrivate && !active.isHomePage) {
                    val host = runCatching { Uri.parse(active.url).host.orEmpty() }.getOrDefault("")
                    if (host.isNotBlank()) {
                        val exceeded = privacyRepository.recordSiteUsageDuration(host, 10L)
                        _runtimeState.update { it.copy(isDomainFocusLimitReached = exceeded) }
                    }
                }
            }
        }
    }

    fun dismissDomainFocusLimitAlert() {
        _runtimeState.update { it.copy(isDomainFocusLimitReached = false) }
    }

    // Stage 13.10: Hands-Free Auto-Scroll / Teleprompter Mode
    fun toggleHandsFreeAutoScroll() {
        if (_runtimeState.value.isAutoScrollActive) {
            autoScrollJob?.cancel()
            _runtimeState.update { it.copy(isAutoScrollActive = false) }
            _snackbarMessages.tryEmit(L.s(R.string.t_stopped_hands_free_auto_scroll))
        } else {
            _runtimeState.update { it.copy(isAutoScrollActive = true) }
            _snackbarMessages.tryEmit(L.s(R.string.t_hands_free_auto_scroll_active))
            autoScrollJob?.cancel()
            autoScrollJob = viewModelScope.launch {
                while (isActive && _runtimeState.value.isAutoScrollActive) {
                    delay(45L)
                    val activeId = uiState.value.activeTab?.id ?: continue
                    webViewPoolManager.getLiveWebView(activeId)?.scrollBy(0, 3)
                }
            }
        }
    }

    // Stage 13.3: Copy Clean URL with 60s Clipboard Auto-Scrub
    fun copyCleanActiveUrlToClipboard() {
        val active = uiState.value.activeTab ?: return
        if (active.isHomePage) return
        val clean = clipboardPrivacyGuard.copyCleanUrlToClipboard(active.url, autoClearAfterSeconds = 60L)
        _snackbarMessages.tryEmit(L.s(R.string.t_copied_sanitized_url_clipboard_will_auto, clean))
    }

    // Stage 13.8: Eyedropper Color Picker & CSS Typography Inspector
    fun inspectPageColorsAndFonts() {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        val inspectorJs = """
            (function() {
                function rgbToHex(rgb) {
                    var m = rgb.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
                    if (!m) return null;
                    function hex(x) { return ("0" + parseInt(x, 10).toString(16)).slice(-2); }
                    return "#" + hex(m[1]) + hex(m[2]) + hex(m[3]);
                }
                var colors = [];
                var fonts = [];
                var els = document.querySelectorAll('body, header, nav, main, h1, h2, h3, p, a, button, div');
                for (var i = 0; i < Math.min(els.length, 80); i++) {
                    var st = window.getComputedStyle(els[i]);
                    var c1 = rgbToHex(st.backgroundColor || '');
                    var c2 = rgbToHex(st.color || '');
                    if (c1 && c1 !== '#000000' && colors.indexOf(c1) === -1) colors.push(c1);
                    if (c2 && colors.indexOf(c2) === -1) colors.push(c2);
                    var ff = (st.fontFamily || '').split(',')[0].replace(/['"]/g, '').trim();
                    if (ff && fonts.indexOf(ff) === -1) fonts.push(ff);
                }
                var bodyStyle = window.getComputedStyle(document.body);
                var summary = 'Body Font: ' + bodyStyle.fontFamily + '\nFont Size: ' + bodyStyle.fontSize + '\nLine Height: ' + bodyStyle.lineHeight;
                return JSON.stringify({
                    colors: colors.slice(0, 12),
                    fonts: fonts.slice(0, 8),
                    summary: summary
                });
            })();
        """.trimIndent()

        wv.evaluateJavascript(inspectorJs) { rawJson ->
            runCatching {
                val unescaped = if (rawJson != null && rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                    Json.decodeFromString<String>(rawJson)
                } else rawJson.orEmpty()
                val obj = Json.parseToJsonElement(unescaped) as kotlinx.serialization.json.JsonObject
                val colors = (obj["colors"] as? kotlinx.serialization.json.JsonArray)
                    ?.map { it.toString().removeSurrounding("\"") }.orEmpty()
                val fonts = (obj["fonts"] as? kotlinx.serialization.json.JsonArray)
                    ?.map { it.toString().removeSurrounding("\"") }.orEmpty()
                val summary = obj["summary"]?.toString()?.removeSurrounding("\"")?.replace("\\n", "\n").orEmpty()
                _runtimeState.update {
                    it.copy(
                        colorFontReport = PageStyleInspectionReport(
                            detectedColorsHex = colors,
                            detectedFonts = fonts,
                            typographySummary = summary
                        )
                    )
                }
            }
        }
    }

    fun closeColorFontInspector() {
        _runtimeState.update { it.copy(colorFontReport = null) }
    }

    fun dismissHomographWarning() {
        _runtimeState.update { it.copy(homographWarning = null) }
    }

    fun submitOmnibarInput(rawInput: String) {
        val currentSettings = uiState.value.settings
        val searchConfig = SearchEngineConfig(
            selectedEngine = currentSettings.searchEngine,
            customSearchTemplate = currentSettings.customSearchUrlTemplate,
            enableLocalSuggestions = currentSettings.enableLocalSearchSuggestions
        )

        when (val target = resolveInputToUrlUseCase(
            rawInput = rawInput,
            searchConfig = searchConfig,
            httpsOnlyMode = currentSettings.httpsOnlyMode,
            stripTrackingParams = currentSettings.stripTrackingUrlParameters
        )) {
            is ResolvedNavigationTarget.OpenNovaHome -> {
                val active = uiState.value.activeTab ?: return
                viewModelScope.launch {
                    tabRepository.updateTabMetadata(active.id, url = "nova://home", title = L.s(R.string.t_new_tab))
                }
            }
            is ResolvedNavigationTarget.BlockedUnsafeInput -> {
                viewModelScope.launch {
                    privacyRepository.recordUnsafeSchemeBlocked()
                    _snackbarMessages.tryEmit(target.reason)
                }
            }
            is ResolvedNavigationTarget.LoadWebUrl -> {
                if (target.strippedTrackingParamsCount > 0) {
                    viewModelScope.launch {
                        privacyRepository.recordTrackingParamsStripped(target.strippedTrackingParamsCount)
                    }
                }
                loadUrlInActiveTab(target.url)
            }
        }
    }

    fun loadUrlInActiveTab(url: String) {
        val active = uiState.value.activeTab
        if (active == null) {
            viewModelScope.launch {
                tabRepository.createTab(
                    url = url,
                    isPrivate = false,
                    isDesktopMode = uiState.value.settings.defaultDesktopMode,
                    selectImmediately = true
                )
            }
            return
        }
        _runtimeState.update { it.copy(webErrorState = null) }
        viewModelScope.launch {
            tabRepository.updateTabMetadata(active.id, url = url)
            val webView = webViewPoolManager.getLiveWebView(active.id)
            if (webView != null) {
                val headers = if (uiState.value.settings.sendDoNotTrackHeader) mapOf("DNT" to "1") else emptyMap()
                webView.loadUrl(url, headers)
            }
        }
    }

    fun toggleSplitScreenMode() {
        _runtimeState.update { it.copy(isSplitScreenActive = !it.isSplitScreenActive) }
    }

    fun updateSplitSecondaryUrl(rawInput: String) {
        val currentSettings = uiState.value.settings
        val searchConfig = SearchEngineConfig(
            selectedEngine = currentSettings.searchEngine,
            customSearchTemplate = currentSettings.customSearchUrlTemplate,
            enableLocalSuggestions = false
        )
        val resolved = resolveInputToUrlUseCase(
            rawInput = rawInput,
            searchConfig = searchConfig,
            httpsOnlyMode = currentSettings.httpsOnlyMode,
            stripTrackingParams = currentSettings.stripTrackingUrlParameters
        )
        if (resolved is ResolvedNavigationTarget.LoadWebUrl) {
            _runtimeState.update { it.copy(splitSecondaryUrl = resolved.url) }
        }
    }

    fun adjustSiteTextZoom(deltaPercent: Int) {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        val newZoom = (_runtimeState.value.siteTextZoomPercent + deltaPercent).coerceIn(50, 250)
        _runtimeState.update { it.copy(siteTextZoomPercent = newZoom) }
        wv.settings.textZoom = newZoom
        val host = runCatching { Uri.parse(active.url).host.orEmpty() }.getOrDefault("")
        if (host.isNotBlank()) {
            viewModelScope.launch {
                privacyRepository.setSiteZoomForDomain(host, newZoom)
            }
        }
    }

    fun setOmniCommandPaletteOpen(open: Boolean) {
        _runtimeState.update { it.copy(isOmniCommandPaletteOpen = open) }
    }

    fun openMediaGrabberForActivePage() {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        val mediaExtractorJs = """
            (function() {
                var urls = [];
                document.querySelectorAll('img[src], video[src], video source[src], audio[src], a[href$=".pdf"], a[href$=".mp4"], a[href$=".zip"]').forEach(function(el) {
                    var u = el.src || el.href;
                    if (u && (u.indexOf('http://') === 0 || u.indexOf('https://') === 0) && urls.indexOf(u) === -1) {
                        urls.push(u);
                    }
                });
                return JSON.stringify(urls.slice(0, 60));
            })();
        """.trimIndent()

        wv.evaluateJavascript(mediaExtractorJs) { rawJson ->
            val list = runCatching {
                val unescaped = if (rawJson != null && rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                    Json.decodeFromString<String>(rawJson)
                } else rawJson.orEmpty()
                Json.decodeFromString<List<String>>(unescaped)
            }.getOrDefault(emptyList())

            _runtimeState.update {
                it.copy(
                    extractedMediaUrls = list,
                    isMediaGrabberSheetOpen = true
                )
            }
        }
    }

    fun closeMediaGrabber() {
        _runtimeState.update { it.copy(isMediaGrabberSheetOpen = false) }
    }

    fun downloadSingleMediaAsset(url: String) {
        onDownloadRequested(
            PendingDownloadRequest(
                url = url,
                userAgent = "NibrasNova/1.4",
                contentDisposition = null,
                mimeType = null,
                contentLength = -1L,
                cookies = null
            )
        )
    }

    fun batchDownloadAllMediaAssets() {
        val urls = _runtimeState.value.extractedMediaUrls
        viewModelScope.launch {
            for (url in urls.take(15)) {
                downloadRepository.enqueueDownload(
                    url = url,
                    userAgent = "NibrasNova/1.4",
                    contentDisposition = null,
                    mimeType = null,
                    contentLength = -1L,
                    cookies = null
                )
            }
            _runtimeState.update { it.copy(isMediaGrabberSheetOpen = false) }
            _snackbarMessages.tryEmit(L.s(R.string.t_queued_media_assets_in_download_center, urls.take(15).size))
        }
    }

    fun openWebNotesAnnotator() {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id)
        if (wv != null) {
            wv.evaluateJavascript("(window.getSelection() ? window.getSelection().toString() : '')") { sel ->
                val cleanSel = sel?.removeSurrounding("\"")?.trim().orEmpty()
                _runtimeState.update {
                    it.copy(
                        currentSelectedTextOnPage = cleanSel,
                        isWebNotesSheetOpen = true
                    )
                }
            }
        } else {
            _runtimeState.update { it.copy(isWebNotesSheetOpen = true) }
        }
    }

    fun closeWebNotesAnnotator() {
        _runtimeState.update { it.copy(isWebNotesSheetOpen = false) }
    }

    fun saveWebAnnotationForActivePage(quote: String, note: String) {
        val active = uiState.value.activeTab ?: return
        viewModelScope.launch {
            privacyRepository.addWebAnnotation(
                pageUrl = active.url,
                pageTitle = active.title,
                quote = quote,
                note = note
            )
            _snackbarMessages.tryEmit(L.s(R.string.t_saved_persistent_highlight_note_for_this))
        }
    }

    fun deleteWebAnnotation(id: Long) {
        viewModelScope.launch {
            privacyRepository.deleteWebAnnotation(id)
        }
    }

    fun openJsonXmlFormatterForActivePage() {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        wv.evaluateJavascript("(document.body ? document.body.innerText.slice(0, 12000) : '')") { raw ->
            val text = raw?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                .orEmpty()
            _runtimeState.update { it.copy(formattedJsonXmlPayload = text) }
        }
    }

    fun closeJsonXmlFormatter() {
        _runtimeState.update { it.copy(formattedJsonXmlPayload = null) }
    }

    fun triggerEmergencyPanicShred(activity: Activity?) {
        viewModelScope.launch {
            webViewPoolManager.destroyAllIncognitoWebViews()
            webViewPoolManager.destroyAllNormalWebViews()
            tabRepository.closeAllTabs(isPrivateMode = true)
            tabRepository.closeAllTabs(isPrivateMode = false)
            privacyRepository.clearBrowsingData(
                clearHistory = true,
                clearCookiesAndSiteData = true,
                clearCache = true,
                clearSitePermissions = true
            )
            clipboardPrivacyGuard.scrubClipboardImmediately()
            activity?.finishAndRemoveTask()
        }
    }

    fun switchTabDirectly(tab: TabItem) {
        viewModelScope.launch {
            captureActiveTabThumbnail()
            tabRepository.switchToTab(tab.id)
        }
    }

    fun closeTabDirectly(tab: TabItem) {
        viewModelScope.launch {
            webViewPoolManager.destroyTabWebView(tab.id)
            tabRepository.closeTab(tab.id)
        }
    }

    fun onAddressQueryChanged(query: String) {
        if (query.isBlank() || !uiState.value.settings.enableLocalSearchSuggestions) {
            _runtimeState.update { it.copy(addressBarSuggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            combine(
                bookmarkRepository.searchBookmarks(query),
                historyRepository.searchHistory(query)
            ) { bookmarks: List<BookmarkItem>, history: List<HistoryItem> ->
                (bookmarks.map { it.url } + history.map { it.url }).distinct().take(5)
            }.collect { suggestions ->
                _runtimeState.update { it.copy(addressBarSuggestions = suggestions) }
            }
        }
    }

    fun navigateBack(): Boolean {
        val activeId = uiState.value.activeTab?.id ?: return false
        val wv = webViewPoolManager.getLiveWebView(activeId) ?: return false
        if (wv.canGoBack()) {
            wv.goBack()
            return true
        }
        return false
    }

    fun navigateForward() {
        val activeId = uiState.value.activeTab?.id ?: return
        val wv = webViewPoolManager.getLiveWebView(activeId) ?: return
        if (wv.canGoForward()) {
            wv.goForward()
        }
    }

    fun reloadOrStop() {
        val activeId = uiState.value.activeTab?.id ?: return
        val wv = webViewPoolManager.getLiveWebView(activeId) ?: return
        if (uiState.value.isLoading) {
            wv.stopLoading()
            _runtimeState.update { it.copy(isLoading = false) }
        } else {
            _runtimeState.update { it.copy(webErrorState = null) }
            wv.reload()
        }
    }

    fun toggleBookmarkForActiveTab() {
        val active = uiState.value.activeTab ?: return
        if (active.isHomePage) return
        viewModelScope.launch {
            val added = bookmarkRepository.toggleBookmark(
                title = active.title,
                url = active.url,
                faviconPath = active.faviconPath
            )
            _snackbarMessages.tryEmit(if (added) L.s(R.string.t_added_to_bookmarks) else L.s(R.string.t_removed_from_bookmarks))
        }
    }

    fun toggleDesktopSiteMode(enabled: Boolean) {
        val active = uiState.value.activeTab ?: return
        viewModelScope.launch {
            tabRepository.updateTabMetadata(active.id, isDesktopMode = enabled)
            val wv = webViewPoolManager.getLiveWebView(active.id) ?: return@launch
            webViewPoolManager.updateSettingsAcrossLivePool(
                settings = uiState.value.settings,
                activeTabsById = mapOf(active.id to active.copy(isDesktopMode = enabled))
            )
            wv.reload()
        }
    }

    fun createNewTab(isPrivate: Boolean) {
        viewModelScope.launch {
            captureActiveTabThumbnail()
            tabRepository.createTab(
                url = "nova://home",
                isPrivate = isPrivate,
                isDesktopMode = uiState.value.settings.defaultDesktopMode,
                selectImmediately = true
            )
        }
    }

    fun closeActiveTab() {
        val active = uiState.value.activeTab ?: return
        viewModelScope.launch {
            webViewPoolManager.destroyTabWebView(active.id)
            tabRepository.closeTab(active.id)
        }
    }

    fun captureActiveTabThumbnail() {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        val snapshot = thumbnailManager.captureWebViewSnapshotOnMainThread(wv) ?: return
        viewModelScope.launch {
            val savedPath = thumbnailManager.persistSnapshot(active.id, snapshot, active.isPrivate)
            tabRepository.updateTabMetadata(active.id, thumbnailPath = savedPath)
        }
    }

    fun saveFullWebArchiveMhtml() {
        val active = uiState.value.activeTab ?: return
        if (active.isHomePage) return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        val archiveDir = File(appContext.filesDir, "web_archives").apply { mkdirs() }
        val safeName = active.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(32).ifBlank { "nova_archive" }
        val targetFile = File(archiveDir, "${safeName}_${System.currentTimeMillis()}.mht")

        wv.saveWebArchive(targetFile.absolutePath, false) { savedPath ->
            if (!savedPath.isNullOrBlank()) {
                viewModelScope.launch {
                    val plainBody = _runtimeState.value.latestReaderArticle?.plainText
                        ?: L.s(R.string.t_full_offline_mhtml_web_archive_saved_at, savedPath)
                    privacyRepository.saveOfflineArticle(
                        title = L.s(R.string.t_mhtml_web_archive, active.title),
                        url = active.url,
                        siteName = active.url,
                        plainText = plainBody,
                        estimatedReadMinutes = _runtimeState.value.latestReaderArticle?.estimatedReadMinutes ?: 3,
                        archivePath = savedPath
                    )
                    _snackbarMessages.tryEmit(L.s(R.string.t_full_offline_web_archive_mht_saved))
                }
            } else {
                _snackbarMessages.tryEmit(L.s(R.string.t_failed_to_save_web_archive))
            }
        }
    }

    fun toggleTtsVoiceReader() {
        if (ttsReaderManager.state.value.isSpeaking) {
            ttsReaderManager.stopSpeaking()
            _snackbarMessages.tryEmit(L.s(R.string.t_stopped_voice_reader))
            return
        }
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        viewModelScope.launch {
            val article = _runtimeState.value.latestReaderArticle
                ?: readerModeJsBridge.evaluateCurrentPageReadability(wv)
            val textToRead = article?.plainText?.takeIf { it.isNotBlank() } ?: active.title
            ttsReaderManager.speakArticleText(textToRead)
            _snackbarMessages.tryEmit(L.s(R.string.t_reading_webpage_aloud))
        }
    }

    fun openDevToolsConsole() {
        refreshDomAndStorageInspector()
        _runtimeState.update { it.copy(isDevToolsSheetOpen = true) }
    }

    fun closeDevToolsConsole() {
        _runtimeState.update { it.copy(isDevToolsSheetOpen = false) }
    }

    fun executeJsInDevConsole(script: String) {
        val activeId = uiState.value.activeTab?.id ?: return
        val wv = webViewPoolManager.getLiveWebView(activeId) ?: return
        wv.evaluateJavascript(script) { result ->
            val entry = DevConsoleEntry(
                level = ConsoleLogLevel.INFO,
                message = "> $script\n< $result",
                sourceId = "REPL",
                lineNumber = 1
            )
            _runtimeState.update { state ->
                state.copy(consoleLogs = (state.consoleLogs + entry).takeLast(120))
            }
        }
    }

    fun refreshDomAndStorageInspector() {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        wv.evaluateJavascript("document.documentElement.outerHTML.slice(0, 8000)") { htmlJson ->
            val cleaned = htmlJson?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\t", "  ")
                .orEmpty()
            _runtimeState.update { it.copy(domSourcePreview = cleaned) }
        }

        val cookies = runCatching { CookieManager.getInstance().getCookie(active.url).orEmpty() }.getOrDefault("")
        wv.evaluateJavascript(
            "(function(){ try { return 'localStorage keys (' + Object.keys(localStorage).length + '): ' + Object.keys(localStorage).join(', ') + '\\nsessionStorage keys (' + Object.keys(sessionStorage).length + '): ' + Object.keys(sessionStorage).join(', '); } catch(e){ return e.toString(); } })()"
        ) { storageJson ->
            val storageText = storageJson?.removeSurrounding("\"")?.replace("\\n", "\n").orEmpty()
            val combined = "Cookies for ${active.url}:\n${cookies.ifBlank { "(none)" }}\n\n$storageText"
            _runtimeState.update { it.copy(storageSummary = combined) }
        }
    }

    fun toggleLiveDomDesignMode(enabled: Boolean) {
        val activeId = uiState.value.activeTab?.id ?: return
        val wv = webViewPoolManager.getLiveWebView(activeId) ?: return
        val modeValue = if (enabled) "on" else "off"
        wv.evaluateJavascript(
            "document.designMode = '$modeValue'; document.body.contentEditable = '${enabled}';",
            null
        )
        _runtimeState.update { it.copy(isLiveDomDesignModeEnabled = enabled) }
        _snackbarMessages.tryEmit(
            if (enabled) L.s(R.string.t_live_dom_designmode_on_tap_any_text_on_p)
            else L.s(R.string.t_live_dom_designmode_off)
        )
    }

    fun clearDevToolsLogs() {
        _runtimeState.update { it.copy(consoleLogs = emptyList(), networkLogs = emptyList()) }
    }

    fun openFindInPage() {
        _runtimeState.update { it.copy(isFindInPageVisible = true) }
    }

    fun closeFindInPage() {
        val activeId = uiState.value.activeTab?.id
        activeId?.let { webViewPoolManager.getLiveWebView(it)?.clearMatches() }
        _runtimeState.update {
            it.copy(
                isFindInPageVisible = false,
                findQuery = "",
                findActiveOrdinal = 0,
                findTotalMatches = 0
            )
        }
    }

    fun updateFindQuery(query: String) {
        _runtimeState.update { it.copy(findQuery = query) }
        val activeId = uiState.value.activeTab?.id ?: return
        val wv = webViewPoolManager.getLiveWebView(activeId) ?: return
        if (query.isBlank()) {
            wv.clearMatches()
            _runtimeState.update { it.copy(findActiveOrdinal = 0, findTotalMatches = 0) }
        } else {
            wv.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                _runtimeState.update {
                    it.copy(
                        findActiveOrdinal = activeMatchOrdinal,
                        findTotalMatches = numberOfMatches
                    )
                }
            }
            wv.findAllAsync(query)
        }
    }

    fun findNext(forward: Boolean) {
        val activeId = uiState.value.activeTab?.id ?: return
        webViewPoolManager.getLiveWebView(activeId)?.findNext(forward)
    }

    fun translateCurrentWebPage() {
        val active = uiState.value.activeTab ?: return
        if (active.isHomePage) return
        val encoded = URLEncoder.encode(active.url, Charsets.UTF_8.name())
        val translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=$encoded"
        loadUrlInActiveTab(translateUrl)
    }

    fun captureAndShareFullScrollingScreenshot(activityContext: Context) {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        val bmp = fullPageScreenshotHelper.captureFullScrollingPageOnMainThread(wv)
            ?: thumbnailManager.captureWebViewSnapshotOnMainThread(wv)
        if (bmp == null) {
            _snackbarMessages.tryEmit(L.s(R.string.t_unable_to_capture_webpage_snapshot))
            return
        }
        viewModelScope.launch {
            shareHelper.saveAndShareScreenshot(activityContext, bmp, active.title)
        }
    }

    fun printCurrentPage(activityContext: Context) {
        val active = uiState.value.activeTab ?: return
        val wv = webViewPoolManager.getLiveWebView(active.id) ?: return
        printHelper.printWebPage(activityContext, wv, active.title)
    }

    fun showQrGeneratorForActiveTab() {
        val active = uiState.value.activeTab ?: return
        if (!active.isHomePage) {
            _runtimeState.update { it.copy(showQrGeneratorForUrl = active.url) }
        }
    }

    fun dismissQrGenerator() {
        _runtimeState.update { it.copy(showQrGeneratorForUrl = null) }
    }

    fun addActivePageToHomeScreen() {
        val active = uiState.value.activeTab ?: return
        if (active.isHomePage) return
        val favicon = faviconCacheManager.getMemoryFavicon(active.url)
        val pinned = shortcutHelper.createHomeScreenWebsiteShortcut(active.title, active.url, favicon)
        _snackbarMessages.tryEmit(
            if (pinned) L.s(R.string.t_shortcut_requested_on_launcher_home_scre)
            else L.s(R.string.t_launcher_does_not_support_pinned_shortcu)
        )
    }

    fun shareActivePage(activityContext: Context) {
        val active = uiState.value.activeTab ?: return
        if (active.isHomePage) return
        shareHelper.shareWebPage(activityContext, active.title, active.url)
    }

    fun onUserSelectedUploadFiles(uris: List<Uri>) {
        val callback = pendingFileChooserCallback ?: return
        pendingFileChooserCallback = null
        if (uris.isEmpty()) {
            callback.onReceiveValue(null)
            return
        }
        val validatedUris = mutableListOf<Uri>()
        for (uri in uris) {
            fileUploadValidator.validateSelectedUri(uri)
                .onSuccess { validatedUris.add(it.uri) }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: L.s(R.string.t_blocked_unsafe_file_upload))
                }
        }
        if (validatedUris.isEmpty()) {
            callback.onReceiveValue(null)
        } else {
            callback.onReceiveValue(validatedUris.toTypedArray())
        }
    }

    fun confirmExternalAppLaunch(activityContext: Context, disposition: NavigationDisposition.PromptExternalApp) {
        _runtimeState.update { it.copy(pendingExternalAppPrompt = null) }
        val launched = externalIntentHandler.launchSanitizedExternalIntent(
            activityContext,
            disposition.sanitizedIntent
        )
        if (!launched) {
            disposition.fallbackUrl?.let { loadUrlInActiveTab(it) }
                ?: _snackbarMessages.tryEmit(L.s(R.string.t_no_installed_application_can_open_this_l))
        }
    }

    fun dismissExternalAppPrompt() {
        val fallback = _runtimeState.value.pendingExternalAppPrompt?.fallbackUrl
        _runtimeState.update { it.copy(pendingExternalAppPrompt = null) }
        if (!fallback.isNullOrBlank()) {
            loadUrlInActiveTab(fallback)
        }
    }

    // WebViewSessionCallback implementations
    override fun isHttpsOnlyEnabled(): Boolean = uiState.value.settings.httpsOnlyMode
    override fun isAdBlockerEnabled(): Boolean = uiState.value.settings.enableAdAndTrackerBlocker
    override fun isRedirectChainGuardEnabled(): Boolean = uiState.value.settings.enableRedirectChainGuard
    override fun shouldSendDoNotTrack(): Boolean = uiState.value.settings.sendDoNotTrackHeader

    override fun onPageLoadStarted(tabId: String, url: String) {
        // Stage 13.2: Inspect URL for Punycode / Homograph lookalike attacks immediately
        val homograph = homographAttackDetector.inspectUrlForHomographAttack(url)
            .takeIf { it.isSuspicious }

        _runtimeState.update {
            it.copy(
                isLoading = true,
                loadingProgress = 10,
                webErrorState = null,
                isReaderAvailable = false,
                isLiveDomDesignModeEnabled = false,
                homographWarning = homograph
            )
        }
        viewModelScope.launch {
            tabRepository.updateTabMetadata(tabId = tabId, url = url)
        }
    }

    override fun onPageLoadFinished(
        tabId: String,
        url: String,
        title: String?,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        _runtimeState.update {
            it.copy(
                isLoading = false,
                loadingProgress = 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward
            )
        }
        viewModelScope.launch {
            val active = uiState.value.activeTab
            val cleanTitle = title?.takeIf { it.isNotBlank() } ?: url
            tabRepository.updateTabMetadata(tabId = tabId, url = url, title = cleanTitle)
            if (active != null && active.id == tabId) {
                recordHistoryUseCase(
                    title = cleanTitle,
                    url = url,
                    faviconPath = active.faviconPath,
                    isPrivateSession = active.isPrivate
                )
                webViewPoolManager.getLiveWebView(tabId)?.let { wv ->
                    val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
                    if (host.isNotBlank()) {
                        val savedZoom = privacyRepository.getSiteZoomForDomain(host)
                        _runtimeState.update { it.copy(siteTextZoomPercent = savedZoom) }
                        wv.settings.textZoom = savedZoom
                    }

                    if (uiState.value.settings.enableAntiFingerprinting) {
                        wv.evaluateJavascript(antiFingerprintInjector.shieldScript, null)
                    }

                    val scripts = userScriptRepository.getMatchingEnabledScriptsForUrl(url)
                    for (script in scripts) {
                        when (script.scriptType) {
                            ScriptInjectionType.JAVASCRIPT -> {
                                wv.evaluateJavascript(script.codeContent, null)
                            }
                            ScriptInjectionType.CSS -> {
                                val encodedCss = Base64.encodeToString(
                                    script.codeContent.toByteArray(Charsets.UTF_8),
                                    Base64.NO_WRAP
                                )
                                val cssInjectorJs = "(function(){ var s = document.createElement('style'); s.innerHTML = atob('$encodedCss'); document.head.appendChild(s); })();"
                                wv.evaluateJavascript(cssInjectorJs, null)
                            }
                        }
                    }

                    val article = readerModeJsBridge.evaluateCurrentPageReadability(wv)
                    _runtimeState.update { state ->
                        state.copy(
                            isReaderAvailable = article?.isReadable == true,
                            latestReaderArticle = article
                        )
                    }
                }
            }
        }
    }

    override fun onNavigationBlocked(tabId: String, blockedUrl: String, reason: String) {
        viewModelScope.launch {
            privacyRepository.recordUnsafeSchemeBlocked()
            _snackbarMessages.tryEmit(reason)
        }
    }

    override fun onHttpsUpgraded(tabId: String, newHttpsUrl: String) {
        viewModelScope.launch {
            privacyRepository.recordHttpsUpgrade()
        }
    }

    override fun onAdOrTrackerBlocked(tabId: String, blockedUrl: String) {
        viewModelScope.launch {
            privacyRepository.recordAdOrTrackerBlocked()
        }
    }

    override fun onRedirectChainBlocked(tabId: String, blockedTargetUrl: String, hopCount: Int) {
        viewModelScope.launch {
            privacyRepository.recordRedirectChainBlocked()
            _snackbarMessages.tryEmit(L.s(R.string.t_redirect_guard_blocked_rapid_hop_cross_d, hopCount))
        }
    }

    override fun onNetworkResourceObserved(tabId: String, method: String, url: String, blocked: Boolean) {
        val log = NetworkResourceLog(method = method, url = url, wasBlockedByAdBlocker = blocked)
        _runtimeState.update { state ->
            state.copy(networkLogs = (state.networkLogs + log).takeLast(80))
        }
    }

    override fun onExternalAppNavigationRequested(
        tabId: String,
        disposition: NavigationDisposition.PromptExternalApp
    ) {
        when (uiState.value.settings.externalLinkPolicy) {
            ExternalLinkPolicy.STAY_IN_BROWSER -> {
                disposition.fallbackUrl?.let { loadUrlInActiveTab(it) }
                    ?: _snackbarMessages.tryEmit(L.s(R.string.t_external_app_link_blocked_by_your_stay_i))
            }
            ExternalLinkPolicy.ASK_EVERY_TIME, ExternalLinkPolicy.ALWAYS_OPEN_EXTERNAL -> {
                _runtimeState.update { it.copy(pendingExternalAppPrompt = disposition) }
            }
        }
    }

    override fun onSslCertificateError(tabId: String, challenge: SslCertificateChallenge) {
        viewModelScope.launch {
            privacyRepository.recordSslWarningIntercepted()
        }
        val wrappedChallenge = challenge.copy(
            onCancelNavigation = {
                challenge.onCancelNavigation()
                _runtimeState.update { it.copy(activeSslChallenge = null, isLoading = false) }
            },
            onProceedWithExplicitRisk = {
                challenge.onProceedWithExplicitRisk()
                _runtimeState.update { it.copy(activeSslChallenge = null) }
            }
        )
        _runtimeState.update { it.copy(activeSslChallenge = wrappedChallenge) }
    }

    override fun onMainFrameNetworkError(
        tabId: String,
        failingUrl: String,
        errorCode: Int,
        description: String
    ) {
        _runtimeState.update {
            it.copy(
                isLoading = false,
                webErrorState = WebErrorState(
                    failingUrl = failingUrl,
                    errorCode = errorCode,
                    description = description
                )
            )
        }
    }

    // ChromeSessionCallback implementations
    override fun onProgressChanged(tabId: String, progress: Int) {
        _runtimeState.update {
            it.copy(
                loadingProgress = progress,
                isLoading = progress in 1..99
            )
        }
    }

    override fun onTitleUpdated(tabId: String, title: String) {
        viewModelScope.launch {
            tabRepository.updateTabMetadata(tabId = tabId, title = title)
        }
    }

    override fun onFaviconUpdated(tabId: String, icon: Bitmap) {
        val active = uiState.value.activeTab ?: return
        viewModelScope.launch {
            val savedPath = faviconCacheManager.saveFavicon(active.url, icon, active.isPrivate)
            tabRepository.updateTabMetadata(tabId = tabId, faviconPath = savedPath)
        }
    }

    override fun onConsoleMessageLogged(tabId: String, entry: DevConsoleEntry) {
        _runtimeState.update { state ->
            state.copy(consoleLogs = (state.consoleLogs + entry).takeLast(120))
        }
    }

    override fun onEnterFullScreenVideo(
        customView: View,
        exitCallback: WebChromeClient.CustomViewCallback
    ) {
        _fullScreenCustomView.value = customView to exitCallback
    }

    override fun onExitFullScreenVideo() {
        _fullScreenCustomView.value?.second?.onCustomViewHidden()
        _fullScreenCustomView.value = null
    }

    override fun onFileUploadRequested(
        filePathCallback: ValueCallback<Array<Uri>>,
        acceptTypes: Array<String>,
        allowMultiple: Boolean
    ): Boolean {
        pendingFileChooserCallback?.onReceiveValue(null)
        pendingFileChooserCallback = filePathCallback
        _launchFilePickerEvent.tryEmit(acceptTypes)
        return true
    }

    override fun onWebsitePermissionRequested(prompt: WebsitePermissionPrompt) {
        viewModelScope.launch {
            val primaryType = prompt.permissionTypes.first()
            val storedDecision = privacyRepository.evaluateSitePermission(prompt.origin, primaryType)
            when (storedDecision) {
                PermissionDecision.ALLOW -> prompt.onGrant()
                PermissionDecision.DENY -> prompt.onDeny()
                null -> {
                    val managedPrompt = prompt.copy(
                        onGrant = {
                            viewModelScope.launch {
                                prompt.permissionTypes.forEach { type ->
                                    privacyRepository.saveSitePermission(prompt.origin, type, PermissionDecision.ALLOW)
                                }
                            }
                            prompt.onGrant()
                            _runtimeState.update { it.copy(activePermissionPrompt = null) }
                        },
                        onDeny = {
                            viewModelScope.launch {
                                prompt.permissionTypes.forEach { type ->
                                    privacyRepository.saveSitePermission(prompt.origin, type, PermissionDecision.DENY)
                                }
                            }
                            prompt.onDeny()
                            _runtimeState.update { it.copy(activePermissionPrompt = null) }
                        }
                    )
                    _runtimeState.update { it.copy(activePermissionPrompt = managedPrompt) }
                }
            }
        }
    }

    override fun onOpenNewTabFromUserGesture(targetUrl: String) {
        val isPrivate = uiState.value.activeTab?.isPrivate ?: false
        viewModelScope.launch {
            tabRepository.createTab(
                url = targetUrl,
                isPrivate = isPrivate,
                isDesktopMode = uiState.value.settings.defaultDesktopMode,
                selectImmediately = true
            )
        }
    }

    private fun onDownloadRequested(request: PendingDownloadRequest) {
        viewModelScope.launch {
            val result = downloadRepository.enqueueDownload(
                url = request.url,
                userAgent = request.userAgent,
                contentDisposition = request.contentDisposition,
                mimeType = request.mimeType,
                contentLength = request.contentLength,
                cookies = request.cookies
            )
            result.onSuccess {
                _snackbarMessages.tryEmit(L.s(R.string.t_download_started_in_download_center))
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: L.s(R.string.t_could_not_start_download))
            }
        }
    }
}
