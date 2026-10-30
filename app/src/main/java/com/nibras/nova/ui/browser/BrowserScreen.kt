package com.nibras.nova.ui.browser

import android.app.Activity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.HardwareKeyboardShortcut
import com.nibras.nova.MainActivity
import com.nibras.nova.ui.components.BottomBrowserToolbar
import com.nibras.nova.ui.components.DesktopTabStrip
import com.nibras.nova.ui.components.FindInPageBar
import com.nibras.nova.ui.components.OmniAddressBar
import com.nibras.nova.ui.components.PermissionPromptDialog
import com.nibras.nova.ui.components.SslWarningDialog
import com.nibras.nova.ui.theme.NovaDangerRed
import com.nibras.nova.ui.tools.ColorFontInspectorSheet
import com.nibras.nova.ui.tools.DevToolsSheet
import com.nibras.nova.ui.tools.JsonXmlFormatterSheet
import com.nibras.nova.ui.tools.MediaGrabberSheet
import com.nibras.nova.ui.tools.NovaQuickCommand
import com.nibras.nova.ui.tools.OmniCommandDialog
import com.nibras.nova.ui.tools.QrGeneratorDialog
import com.nibras.nova.ui.tools.WebNotesSheet
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    keyboardShortcutFlow: SharedFlow<HardwareKeyboardShortcut>,
    onNavigateToHome: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onNavigateToPrivateVault: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToRssReader: () -> Unit,
    onNavigateToPageMonitor: () -> Unit,
    onNavigateToDigitalWellbeing: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToReaderMode: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToUserScripts: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fullScreenVideo by viewModel.fullScreenCustomView.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val snackbarHostState = remember { SnackbarHostState() }
    val addressFocusRequester = remember { FocusRequester() }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showConnectionSecuritySheet by remember { mutableStateOf(false) }
    var splitInputDraft by remember(state.splitSecondaryUrl) { mutableStateOf(state.splitSecondaryUrl) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.onUserSelectedUploadFiles(uris)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.launchFilePickerEvent.collect { acceptTypes ->
            filePickerLauncher.launch(acceptTypes)
        }
    }

    LaunchedEffect(Unit) {
        keyboardShortcutFlow.collect { shortcut ->
            when (shortcut) {
                HardwareKeyboardShortcut.NEW_TAB -> viewModel.createNewTab(isPrivate = false)
                HardwareKeyboardShortcut.NEW_INCOGNITO_TAB -> viewModel.createNewTab(isPrivate = true)
                HardwareKeyboardShortcut.CLOSE_CURRENT_TAB -> viewModel.closeActiveTab()
                HardwareKeyboardShortcut.RELOAD_PAGE -> viewModel.reloadOrStop()
                HardwareKeyboardShortcut.FIND_IN_PAGE -> viewModel.openFindInPage()
                HardwareKeyboardShortcut.FOCUS_ADDRESS_BAR -> addressFocusRequester.requestFocus()
                HardwareKeyboardShortcut.OPEN_OMNI_COMMAND_PALETTE -> viewModel.setOmniCommandPaletteOpen(true)
            }
        }
    }

    BackHandler {
        when {
            fullScreenVideo != null -> viewModel.onExitFullScreenVideo()
            state.isFindInPageVisible -> viewModel.closeFindInPage()
            state.isSplitScreenActive -> viewModel.toggleSplitScreenMode()
            viewModel.navigateBack() -> Unit
            else -> onNavigateToHome()
        }
    }

    DisposableEffect(state.activeTab?.id) {
        onDispose {
            viewModel.captureActiveTabThumbnail()
        }
    }

    if (fullScreenVideo != null) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    addView(fullScreenVideo!!.first)
                }
            }
        )
        return
    }

    val activeTab = state.activeTab
    val showDesktopStrip = state.settings.enableDesktopTabStrip || isWideScreen
    val useBottomOmniBar = state.settings.bottomAddressBarReachability

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                if (showDesktopStrip) {
                    val stripTabs = if (activeTab?.isPrivate == true) state.privateTabs else state.normalTabs
                    DesktopTabStrip(
                        tabs = stripTabs,
                        activeTabId = activeTab?.id,
                        isPrivateMode = activeTab?.isPrivate == true,
                        onSelectTab = viewModel::switchTabDirectly,
                        onCloseTab = viewModel::closeTabDirectly,
                        onNewTabClick = { viewModel.createNewTab(isPrivate = activeTab?.isPrivate == true) }
                    )
                }

                if (!useBottomOmniBar) {
                    OmniAddressBar(
                        currentUrl = activeTab?.url ?: "nova://home",
                        pageTitle = activeTab?.title ?: "New Tab",
                        isLoading = state.isLoading,
                        loadingProgress = state.loadingProgress,
                        isPrivate = activeTab?.isPrivate == true,
                        isReaderAvailable = state.isReaderAvailable,
                        focusRequester = addressFocusRequester,
                        onUrlSubmitted = viewModel::submitOmnibarInput,
                        onQueryChanged = viewModel::onAddressQueryChanged,
                        onReloadOrStopClick = viewModel::reloadOrStop,
                        onReaderModeClick = onNavigateToReaderMode,
                        onQrScannerClick = onNavigateToQrScanner,
                        onSecurityBadgeClick = { showConnectionSecuritySheet = true }
                    )
                }

                // Stage 13.2: Homograph / Punycode Phishing High-Visibility Alert Bar
                state.homographWarning?.let { warning ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NovaDangerRed)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color.White)
                        Text(
                            text = "  ${warning.warningReason}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::dismissHomographWarning) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }

                if (state.isFindInPageVisible) {
                    FindInPageBar(
                        query = state.findQuery,
                        activeMatchOrdinal = state.findActiveOrdinal,
                        numberOfMatches = state.findTotalMatches,
                        onQueryChange = viewModel::updateFindQuery,
                        onFindNext = { viewModel.findNext(true) },
                        onFindPrevious = { viewModel.findNext(false) },
                        onClose = viewModel::closeFindInPage
                    )
                }
            }
        },
        bottomBar = {
            Column {
                if (useBottomOmniBar) {
                    OmniAddressBar(
                        currentUrl = activeTab?.url ?: "nova://home",
                        pageTitle = activeTab?.title ?: "New Tab",
                        isLoading = state.isLoading,
                        loadingProgress = state.loadingProgress,
                        isPrivate = activeTab?.isPrivate == true,
                        isReaderAvailable = state.isReaderAvailable,
                        focusRequester = addressFocusRequester,
                        onUrlSubmitted = viewModel::submitOmnibarInput,
                        onQueryChanged = viewModel::onAddressQueryChanged,
                        onReloadOrStopClick = viewModel::reloadOrStop,
                        onReaderModeClick = onNavigateToReaderMode,
                        onQrScannerClick = onNavigateToQrScanner,
                        onSecurityBadgeClick = { showConnectionSecuritySheet = true }
                    )
                }

                BottomBrowserToolbar(
                    canGoBack = state.canGoBack,
                    canGoForward = state.canGoForward,
                    isBookmarked = state.isBookmarked,
                    tabCount = if (activeTab?.isPrivate == true) state.privateTabCount else state.normalTabCount,
                    isPrivate = activeTab?.isPrivate == true,
                    onBackClick = { viewModel.navigateBack() },
                    onForwardClick = viewModel::navigateForward,
                    onHomeClick = onNavigateToHome,
                    onBookmarkToggleClick = viewModel::toggleBookmarkForActiveTab,
                    onTabsClick = {
                        viewModel.captureActiveTabThumbnail()
                        onNavigateToTabs()
                    },
                    onMenuClick = { showMenuSheet = true }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (activeTab != null && !activeTab.isHomePage) {
                    key(activeTab.id) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                FrameLayout(ctx).apply {
                                    val webView = viewModel.webViewPoolManager.obtainOrCreateWebView(
                                        tab = activeTab,
                                        settings = state.settings,
                                        webViewCallback = viewModel,
                                        chromeCallback = viewModel,
                                        downloadListener = viewModel.downloadListener
                                    )
                                    (webView.parent as? ViewGroup)?.removeView(webView)
                                    addView(webView)
                                }
                            },
                            update = { container ->
                                val currentChild = container.getChildAt(0)
                                val expectedWebView = viewModel.webViewPoolManager.getLiveWebView(activeTab.id)
                                if (expectedWebView != null && currentChild !== expectedWebView) {
                                    container.removeAllViews()
                                    (expectedWebView.parent as? ViewGroup)?.removeView(expectedWebView)
                                    container.addView(expectedWebView)
                                }
                            }
                        )
                    }
                } else {
                    LaunchedEffect(activeTab?.isHomePage) {
                        if (activeTab?.isHomePage == true) {
                            onNavigateToHome()
                        }
                    }
                }

                state.webErrorState?.let { err ->
                    ErrorPageOverlay(
                        failingUrl = err.failingUrl,
                        errorCode = err.errorCode,
                        errorDescription = err.description,
                        onRetryClick = viewModel::reloadOrStop,
                        onReturnHomeClick = onNavigateToHome
                    )
                }

                if (state.addressBarSuggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column {
                            state.addressBarSuggestions.forEach { suggestionUrl ->
                                Text(
                                    text = suggestionUrl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.submitOmnibarInput(suggestionUrl)
                                            viewModel.onAddressQueryChanged("")
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (state.isSplitScreenActive) {
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = splitInputDraft,
                            onValueChange = { splitInputDraft = it },
                            singleLine = true,
                            placeholder = { Text("Second pane URL or search…") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = { viewModel.updateSplitSecondaryUrl(splitInputDraft) }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::toggleSplitScreenMode) {
                            Icon(Icons.Default.Close, contentDescription = "Close Split Screen")
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = state.settings.enableJavaScript
                                settings.domStorageEnabled = state.settings.enableDomStorage
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                webViewClient = WebViewClient()
                                loadUrl(state.splitSecondaryUrl)
                            }
                        },
                        update = { wv ->
                            if (wv.url != state.splitSecondaryUrl) {
                                wv.loadUrl(state.splitSecondaryUrl)
                            }
                        }
                    )
                }
            }
        }
    }

    state.activeSslChallenge?.let { challenge ->
        SslWarningDialog(challenge = challenge)
    }

    state.activePermissionPrompt?.let { prompt ->
        PermissionPromptDialog(prompt = prompt)
    }

    state.pendingExternalAppPrompt?.let { extPrompt ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExternalAppPrompt,
            title = { Text("Open in External Application?") },
            text = {
                Text("This webpage wants to open a link outside Nibras Nova:\n\n${extPrompt.targetUri}")
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmExternalAppLaunch(context, extPrompt) }) {
                    Text("Open App")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissExternalAppPrompt) {
                    Text("Stay Here")
                }
            }
        )
    }

    // Stage 13.9: Daily Domain Focus Timer Exceeded Alert
    if (state.isDomainFocusLimitReached) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDomainFocusLimitAlert,
            title = { Text("Daily Focus Time Limit Reached") },
            text = {
                Text("You have reached your configured daily time budget for this website in Nibras Nova Digital Wellbeing.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissDomainFocusLimitAlert()
                    onNavigateToHome()
                }) {
                    Text("Return to Homepage")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissDomainFocusLimitAlert) {
                    Text("Continue 5 More Mins")
                }
            }
        )
    }

    if (showConnectionSecuritySheet && activeTab != null) {
        val isHttps = activeTab.url.startsWith("https://", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { showConnectionSecuritySheet = false },
            title = {
                Text(if (isHttps) "Connection Is Encrypted (HTTPS)" else "Unencrypted Connection (HTTP)")
            },
            text = {
                Text(
                    if (isHttps) {
                        "Your connection to ${activeTab.url} is encrypted with TLS and verified by Android's system certificate store."
                    } else {
                        "This page is not using HTTPS. Avoid entering passwords or sensitive financial details on unencrypted sites."
                    }
                )
            },
            confirmButton = {
                Button(onClick = { showConnectionSecuritySheet = false }) {
                    Text("Close")
                }
            }
        )
    }

    state.showQrGeneratorForUrl?.let { qrUrl ->
        QrGeneratorDialog(
            initialContent = qrUrl,
            onDismiss = viewModel::dismissQrGenerator
        )
    }

    if (state.isDevToolsSheetOpen) {
        DevToolsSheet(
            consoleLogs = state.consoleLogs,
            networkLogs = state.networkLogs,
            domSourcePreview = state.domSourcePreview,
            storageSummary = state.storageSummary,
            isDesignModeEnabled = state.isLiveDomDesignModeEnabled,
            onExecuteJsRepl = viewModel::executeJsInDevConsole,
            onRefreshDomAndStorage = viewModel::refreshDomAndStorageInspector,
            onToggleLiveDomDesignMode = viewModel::toggleLiveDomDesignMode,
            onClearLogs = viewModel::clearDevToolsLogs,
            onDismiss = viewModel::closeDevToolsConsole
        )
    }

    if (state.isMediaGrabberSheetOpen) {
        MediaGrabberSheet(
            extractedMediaUrls = state.extractedMediaUrls,
            onDownloadSingleAsset = viewModel::downloadSingleMediaAsset,
            onBatchDownloadAll = viewModel::batchDownloadAllMediaAssets,
            onDismiss = viewModel::closeMediaGrabber
        )
    }

    if (state.isWebNotesSheetOpen && activeTab != null) {
        WebNotesSheet(
            currentPageUrl = activeTab.url,
            initialSelectedQuote = state.currentSelectedTextOnPage,
            annotationsForPage = state.annotationsForActivePage,
            onSaveAnnotation = viewModel::saveWebAnnotationForActivePage,
            onDeleteAnnotation = viewModel::deleteWebAnnotation,
            onDismiss = viewModel::closeWebNotesAnnotator
        )
    }

    state.formattedJsonXmlPayload?.let { payload ->
        JsonXmlFormatterSheet(
            rawPayload = payload,
            onDismiss = viewModel::closeJsonXmlFormatter
        )
    }

    // Stage 13.8: Eyedropper Color Picker & CSS Typography Inspector Sheet
    state.colorFontReport?.let { report ->
        ColorFontInspectorSheet(
            report = report,
            onDismiss = viewModel::closeColorFontInspector
        )
    }

    if (state.isOmniCommandPaletteOpen) {
        val commands = remember(state.isSplitScreenActive, state.isTtsSpeaking, state.isAutoScrollActive) {
            listOf(
                NovaQuickCommand("cmd_vault", "Open Biometric Private Media & Bookmark Vault", "Security", onNavigateToPrivateVault),
                NovaQuickCommand("cmd_split", "Toggle Dual Split-Screen WebView", "Multitasking", viewModel::toggleSplitScreenMode),
                NovaQuickCommand("cmd_autoscroll", "Toggle Hands-Free Auto-Scroll (Teleprompter)", "Reading", viewModel::toggleHandsFreeAutoScroll),
                NovaQuickCommand("cmd_color", "Eyedropper Color Picker & Font Inspector", "Developer", viewModel::inspectPageColorsAndFonts),
                NovaQuickCommand("cmd_devtools", "Open F12 Web Developer Console & DOM Inspector", "Developer", viewModel::openDevToolsConsole),
                NovaQuickCommand("cmd_userscripts", "Open UserScripts (JS) & Custom CSS Manager", "Developer", onNavigateToUserScripts),
                NovaQuickCommand("cmd_json", "Format Page as JSON / XML Tree", "Developer", viewModel::openJsonXmlFormatterForActivePage),
                NovaQuickCommand("cmd_monitor", "Open Web Page Change Monitor (Price/News Watcher)", "Tools", onNavigateToPageMonitor),
                NovaQuickCommand("cmd_wellbeing", "Open Digital Wellbeing & Focus Site Timer", "Wellbeing", onNavigateToDigitalWellbeing),
                NovaQuickCommand("cmd_reader", "Open Reader Mode & Extractive Summarizer", "Reading", onNavigateToReaderMode),
                NovaQuickCommand("cmd_tts", "Listen to Webpage Aloud (TTS Voice Reader)", "Reading", viewModel::toggleTtsVoiceReader),
                NovaQuickCommand("cmd_notes", "Annotate & Highlight Page Text", "Research", viewModel::openWebNotesAnnotator),
                NovaQuickCommand("cmd_media", "Extract All Images & Videos (Media Grabber)", "Tools", viewModel::openMediaGrabberForActivePage),
                NovaQuickCommand("cmd_mhtml", "Save Full Offline Web Archive (.mht)", "Offline", viewModel::saveFullWebArchiveMhtml),
                NovaQuickCommand("cmd_rss", "Open Built-in RSS & Atom News Reader", "Feeds", onNavigateToRssReader),
                NovaQuickCommand("cmd_fullshot", "Capture Full-Page Scrolling Screenshot", "Tools") {
                    viewModel.captureAndShareFullScrollingScreenshot(context)
                },
                NovaQuickCommand("cmd_panic", "Emergency Panic Shred (Wipe All & Exit)", "Security") {
                    viewModel.triggerEmergencyPanicShred(context as? Activity)
                }
            )
        }
        OmniCommandDialog(
            commands = commands,
            onDismiss = { viewModel.setOmniCommandPaletteOpen(false) }
        )
    }

    if (showMenuSheet && activeTab != null) {
        BrowserMenuSheet(
            isDesktopMode = activeTab.isDesktopMode,
            isSplitScreenActive = state.isSplitScreenActive,
            isAutoScrollActive = state.isAutoScrollActive,
            isTtsSpeaking = state.isTtsSpeaking,
            siteTextZoomPercent = state.siteTextZoomPercent,
            onDismiss = { showMenuSheet = false },
            onOpenOmniCommandPalette = { viewModel.setOmniCommandPaletteOpen(true) },
            onNewNormalTab = { viewModel.createNewTab(isPrivate = false) },
            onNewIncognitoTab = { viewModel.createNewTab(isPrivate = true) },
            onToggleSplitScreenMode = viewModel::toggleSplitScreenMode,
            onToggleAutoScrollTeleprompter = viewModel::toggleHandsFreeAutoScroll,
            onEnterPipMode = { (context as? MainActivity)?.enterFloatingVideoPipMode() },
            onAdjustSiteZoom = viewModel::adjustSiteTextZoom,
            onCopyCleanUrlWithClipboardGuard = viewModel::copyCleanActiveUrlToClipboard,
            onOpenPrivateVault = onNavigateToPrivateVault,
            onOpenBookmarks = onNavigateToBookmarks,
            onOpenHistory = onNavigateToHistory,
            onOpenDownloads = onNavigateToDownloads,
            onOpenRssReader = onNavigateToRssReader,
            onOpenPageMonitor = onNavigateToPageMonitor,
            onOpenDigitalWellbeing = onNavigateToDigitalWellbeing,
            onOpenReaderMode = onNavigateToReaderMode,
            onToggleTtsReadAloud = viewModel::toggleTtsVoiceReader,
            onOpenWebNotesAnnotator = viewModel::openWebNotesAnnotator,
            onOpenMediaGrabber = viewModel::openMediaGrabberForActivePage,
            onOpenColorFontInspector = viewModel::inspectPageColorsAndFonts,
            onOpenJsonXmlFormatter = viewModel::openJsonXmlFormatterForActivePage,
            onSaveFullMhtmlWebArchive = viewModel::saveFullWebArchiveMhtml,
            onOpenDevToolsConsole = viewModel::openDevToolsConsole,
            onOpenUserScriptsManager = onNavigateToUserScripts,
            onToggleDesktopMode = viewModel::toggleDesktopSiteMode,
            onFindInPage = viewModel::openFindInPage,
            onTranslatePage = viewModel::translateCurrentWebPage,
            onCaptureFullScrollingScreenshot = { viewModel.captureAndShareFullScrollingScreenshot(context) },
            onPrintPage = { viewModel.printCurrentPage(context) },
            onGeneratePageQrCode = viewModel::showQrGeneratorForActiveTab,
            onAddHomeScreenShortcut = viewModel::addActivePageToHomeScreen,
            onSharePage = { viewModel.shareActivePage(context) },
            onTriggerEmergencyPanicShred = { viewModel.triggerEmergencyPanicShred(context as? Activity) },
            onOpenPrivacyDashboard = onNavigateToPrivacy,
            onOpenSettings = onNavigateToSettings
        )
    }
}
