package com.nibras.nova.ui.privacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.core.security.DnsOverHttpsProvider
import com.nibras.nova.core.security.EncryptedBackupManager
import com.nibras.nova.core.security.NovaBackupBookmark
import com.nibras.nova.core.security.NovaBackupPayload
import com.nibras.nova.core.security.NovaBackupShortcut
import com.nibras.nova.core.security.NovaBackupUserScript
import com.nibras.nova.core.security.ProxyDoHManager
import com.nibras.nova.data.local.entity.CookieWhitelistEntity
import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.domain.model.BrowserSettings
import com.nibras.nova.domain.model.PrivacyMetrics
import com.nibras.nova.domain.model.ScriptInjectionType
import com.nibras.nova.domain.model.SitePermission
import com.nibras.nova.domain.repository.BookmarkRepository
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.repository.ShortcutRepository
import com.nibras.nova.domain.repository.UserScriptRepository
import com.nibras.nova.domain.usecase.ClearBrowsingDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PrivacyDashboardUiState(
    val settings: BrowserSettings = BrowserSettings(),
    val metrics: PrivacyMetrics = PrivacyMetrics(),
    val sitePermissions: List<SitePermission> = emptyList(),
    val cookieWhitelist: List<CookieWhitelistEntity> = emptyList()
)

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val privacyRepository: PrivacyRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val shortcutRepository: ShortcutRepository,
    private val userScriptRepository: UserScriptRepository,
    private val clearBrowsingDataUseCase: ClearBrowsingDataUseCase,
    private val proxyDoHManager: ProxyDoHManager,
    private val encryptedBackupManager: EncryptedBackupManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<PrivacyDashboardUiState> = combine(
        settingsRepository.observeSettings(),
        privacyRepository.observePrivacyMetrics(),
        privacyRepository.observeSitePermissions(),
        privacyRepository.observeWhitelistedCookieDomains()
    ) { settings, metrics, permissions, whitelist ->
        PrivacyDashboardUiState(
            settings = settings,
            metrics = metrics,
            sitePermissions = permissions,
            cookieWhitelist = whitelist
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyDashboardUiState())

    fun setAdAndTrackerBlocker(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAdAndTrackerBlocker(enabled) }
    }

    fun setStripTrackingParams(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateStripTrackingParameters(enabled) }
    }

    fun setBiometricLockIncognito(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateBiometricLockIncognito(enabled) }
    }

    fun setAntiFingerprinting(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableAntiFingerprinting(enabled) }
    }

    fun setRedirectChainGuard(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableRedirectChainGuard(enabled) }
    }

    fun setDnsOverHttpsProvider(provider: DnsOverHttpsProvider) {
        viewModelScope.launch { settingsRepository.updateDnsOverHttpsProvider(provider) }
    }

    fun applyCustomProxy(hostPort: String) {
        viewModelScope.launch {
            settingsRepository.updateCustomProxyHostPort(hostPort)
            proxyDoHManager.applyCustomProxyOrClear(hostPort)
                .onSuccess { active ->
                    _events.tryEmit(if (active) "Custom Proxy active: $hostPort" else "Cleared Custom Proxy")
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Proxy configuration failed")
                }
        }
    }

    fun setHttpsOnly(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateHttpsOnlyMode(enabled) }
    }

    fun setSafeBrowsing(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSafeBrowsingEnabled(enabled) }
    }

    fun setBlockThirdPartyCookies(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateBlockThirdPartyCookies(enabled) }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateJavaScriptEnabled(enabled) }
    }

    fun setDomStorageEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDomStorageEnabled(enabled) }
    }

    fun setDoNotTrack(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSendDoNotTrack(enabled) }
    }

    fun setBlockIncognitoScreenshots(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateBlockScreenshotsInIncognito(enabled) }
    }

    fun addWhitelistedCookieDomain(domain: String) {
        viewModelScope.launch { privacyRepository.addCookieWhitelistDomain(domain) }
    }

    fun removeWhitelistedCookieDomain(id: Long) {
        viewModelScope.launch { privacyRepository.removeCookieWhitelistDomain(id) }
    }

    fun removeSitePermission(id: Long) {
        viewModelScope.launch { privacyRepository.removeSitePermission(id) }
    }

    // Stage 12.4: Export AES-256-GCM Encrypted .novabackup
    fun exportEncryptedNovaBackup(activityContext: Context, passphrase: String) {
        viewModelScope.launch {
            val bookmarks = bookmarkRepository.observeAllBookmarks().firstOrNull().orEmpty()
                .map { NovaBackupBookmark(it.title, it.url) }
            val shortcuts = shortcutRepository.observeShortcuts().firstOrNull().orEmpty()
                .map { NovaBackupShortcut(it.title, it.url) }
            val scripts = userScriptRepository.observeAllScripts().firstOrNull().orEmpty()
                .map { NovaBackupUserScript(it.name, it.matchDomainPattern, it.scriptType.name, it.codeContent, it.isEnabled) }
            val whitelist = privacyRepository.observeWhitelistedCookieDomains().firstOrNull().orEmpty()
                .map { it.domain }
            val feeds = privacyRepository.observeRssFeeds().firstOrNull().orEmpty()
                .map { it.feedUrl }

            val payload = NovaBackupPayload(
                bookmarks = bookmarks,
                shortcuts = shortcuts,
                userScripts = scripts,
                cookieWhitelistDomains = whitelist,
                rssFeedUrls = feeds
            )

            encryptedBackupManager.encryptBackupPayload(payload, passphrase)
                .onSuccess { encryptedJson ->
                    withContext(dispatchers.io) {
                        val exportDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
                        val outFile = File(exportDir, "nibras_nova_encrypted_${System.currentTimeMillis()}.novabackup")
                        outFile.writeText(encryptedJson, Charsets.UTF_8)
                        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", outFile)
                        withContext(dispatchers.main) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            activityContext.startActivity(
                                Intent.createChooser(shareIntent, "Export Encrypted .novabackup (AES-256-GCM)")
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Encryption failed")
                }
        }
    }

    // Stage 12.4: Restore AES-256-GCM Encrypted .novabackup
    fun restoreEncryptedNovaBackup(context: Context, uri: Uri, passphrase: String) {
        viewModelScope.launch {
            val rawEncrypted = withContext(dispatchers.io) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            encryptedBackupManager.decryptBackupPayload(rawEncrypted, passphrase)
                .onSuccess { payload ->
                    for (bm in payload.bookmarks) {
                        bookmarkRepository.addOrUpdateBookmark(BookmarkItem(title = bm.title, url = bm.url))
                    }
                    for (sc in payload.shortcuts) {
                        shortcutRepository.addShortcut(sc.title, sc.url)
                    }
                    for (us in payload.userScripts) {
                        userScriptRepository.addOrUpdateScript(
                            name = us.name,
                            matchDomainPattern = us.matchDomainPattern,
                            scriptType = runCatching { ScriptInjectionType.valueOf(us.scriptType) }.getOrDefault(ScriptInjectionType.JAVASCRIPT),
                            codeContent = us.codeContent,
                            isEnabled = us.isEnabled
                        )
                    }
                    for (domain in payload.cookieWhitelistDomains) {
                        privacyRepository.addCookieWhitelistDomain(domain)
                    }
                    for (feedUrl in payload.rssFeedUrls) {
                        privacyRepository.addRssFeed(feedUrl, feedUrl)
                    }
                    _events.tryEmit("Restored ${payload.bookmarks.size} bookmarks & ${payload.userScripts.size} scripts from encrypted backup!")
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Failed to decrypt .novabackup archive")
                }
        }
    }

    fun executeDataWipe(
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearPermissions: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            clearBrowsingDataUseCase(
                clearHistory = clearHistory,
                clearCookiesAndSiteData = clearCookies,
                clearCache = clearCache,
                clearSitePermissions = clearPermissions
            )
            onComplete()
        }
    }
}
