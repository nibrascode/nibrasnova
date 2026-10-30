package com.nibras.nova.data.repository

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.webkit.WebViewFeature
import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.core.util.FaviconCacheManager
import com.nibras.nova.data.datastore.UserPreferencesDataSource
import com.nibras.nova.data.local.dao.CookieWhitelistDao
import com.nibras.nova.data.local.dao.HistoryDao
import com.nibras.nova.data.local.dao.PageWatchDao
import com.nibras.nova.data.local.dao.PrivateVaultDao
import com.nibras.nova.data.local.dao.RssFeedDao
import com.nibras.nova.data.local.dao.SavedPageDao
import com.nibras.nova.data.local.dao.SessionSnapshotDao
import com.nibras.nova.data.local.dao.SitePermissionDao
import com.nibras.nova.data.local.dao.SiteUsageDao
import com.nibras.nova.data.local.dao.SiteZoomDao
import com.nibras.nova.data.local.dao.WebAnnotationDao
import com.nibras.nova.data.local.entity.CookieWhitelistEntity
import com.nibras.nova.data.local.entity.PageWatchEntity
import com.nibras.nova.data.local.entity.PrivateVaultEntity
import com.nibras.nova.data.local.entity.RssFeedEntity
import com.nibras.nova.data.local.entity.SavedPageEntity
import com.nibras.nova.data.local.entity.SessionSnapshotEntity
import com.nibras.nova.data.local.entity.SitePermissionEntity
import com.nibras.nova.data.local.entity.SiteUsageEntity
import com.nibras.nova.data.local.entity.SiteZoomEntity
import com.nibras.nova.data.local.entity.WebAnnotationEntity
import com.nibras.nova.domain.model.PermissionDecision
import com.nibras.nova.domain.model.PrivacyMetrics
import com.nibras.nova.domain.model.SavedOfflinePage
import com.nibras.nova.domain.model.SitePermission
import com.nibras.nova.domain.model.SitePermissionType
import com.nibras.nova.domain.repository.PrivacyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataSource: UserPreferencesDataSource,
    private val historyDao: HistoryDao,
    private val sitePermissionDao: SitePermissionDao,
    private val savedPageDao: SavedPageDao,
    private val cookieWhitelistDao: CookieWhitelistDao,
    private val webAnnotationDao: WebAnnotationDao,
    private val rssFeedDao: RssFeedDao,
    private val sessionSnapshotDao: SessionSnapshotDao,
    private val siteZoomDao: SiteZoomDao,
    private val privateVaultDao: PrivateVaultDao,
    private val pageWatchDao: PageWatchDao,
    private val siteUsageDao: SiteUsageDao,
    private val faviconCacheManager: FaviconCacheManager,
    private val dispatchers: DispatcherProvider
) : PrivacyRepository {

    override fun observePrivacyMetrics(): Flow<PrivacyMetrics> =
        preferencesDataSource.privacyMetricsFlow.map { metrics ->
            metrics.copy(
                isMultiProfileIncognitoSupported = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE),
                isSafeBrowsingSupported = WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING) ||
                    WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)
            )
        }

    override fun observeSitePermissions(): Flow<List<SitePermission>> =
        sitePermissionDao.observeAllSitePermissions().map { list -> list.map { it.toDomain() } }

    override fun observeSavedOfflinePages(): Flow<List<SavedOfflinePage>> =
        savedPageDao.observeSavedPages().map { list -> list.map { it.toDomain() } }

    override fun observeWhitelistedCookieDomains(): Flow<List<CookieWhitelistEntity>> =
        cookieWhitelistDao.observeWhitelistedDomains()

    override fun observeAllAnnotations(): Flow<List<WebAnnotationEntity>> =
        webAnnotationDao.observeAllAnnotations()

    override fun observeAnnotationsForUrl(url: String): Flow<List<WebAnnotationEntity>> =
        webAnnotationDao.observeAnnotationsForUrl(url)

    override fun observeRssFeeds(): Flow<List<RssFeedEntity>> =
        rssFeedDao.observeAllFeeds()

    override fun observeSessionSnapshots(): Flow<List<SessionSnapshotEntity>> =
        sessionSnapshotDao.observeAllSnapshots()

    override fun observeAllSiteZooms(): Flow<List<SiteZoomEntity>> =
        siteZoomDao.observeAllSiteZooms()

    override fun observePrivateVaultItems(): Flow<List<PrivateVaultEntity>> =
        privateVaultDao.observeAllVaultItems()

    override fun observeWatchedPages(): Flow<List<PageWatchEntity>> =
        pageWatchDao.observeAllWatchedPages()

    override fun observeAllSiteUsage(): Flow<List<SiteUsageEntity>> =
        siteUsageDao.observeAllSiteUsage()

    override suspend fun addPrivateVaultItem(title: String, urlOrNote: String, category: String) =
        withContext(dispatchers.io) {
            privateVaultDao.insertVaultItem(
                PrivateVaultEntity(
                    title = title.trim().ifBlank { "Vault Entry" },
                    urlOrNote = urlOrNote.trim(),
                    category = category
                )
            )
            Unit
        }

    override suspend fun deletePrivateVaultItem(id: Long) = withContext(dispatchers.io) {
        privateVaultDao.deleteVaultItem(id)
    }

    override suspend fun addPageWatch(title: String, url: String) = withContext(dispatchers.io) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return@withContext
        val (hash, snippet) = fetchNormalizedTextAndHash(cleanUrl)
        pageWatchDao.upsertPageWatch(
            PageWatchEntity(
                title = title.trim().ifBlank { cleanUrl },
                url = cleanUrl,
                lastContentHash = hash,
                lastSnippetPreview = snippet,
                hasChangedSinceLastCheck = false,
                lastCheckedAt = System.currentTimeMillis()
            )
        )
        Unit
    }

    override suspend fun checkAllWatchedPagesForChanges(): Int = withContext(dispatchers.io) {
        var changedCount = 0
        val all = pageWatchDao.getAllWatchedPagesOnce()
        for (watch in all) {
            val (newHash, newSnippet) = fetchNormalizedTextAndHash(watch.url)
            if (newHash.isNotBlank()) {
                val changed = watch.lastContentHash.isNotBlank() && watch.lastContentHash != newHash
                if (changed) changedCount++
                pageWatchDao.updatePageWatch(
                    watch.copy(
                        lastContentHash = newHash,
                        lastSnippetPreview = newSnippet.ifBlank { watch.lastSnippetPreview },
                        hasChangedSinceLastCheck = changed,
                        lastCheckedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        changedCount
    }

    override suspend fun deletePageWatch(id: Long) = withContext(dispatchers.io) {
        pageWatchDao.deletePageWatch(id)
    }

    override suspend fun recordSiteUsageDuration(domain: String, elapsedSeconds: Long): Boolean =
        withContext(dispatchers.io) {
            val clean = domain.trim().lowercase().removePrefix("www.")
            if (clean.isBlank() || elapsedSeconds <= 0L) return@withContext false
            val existing = siteUsageDao.getUsageForDomain(clean)
            val updated = SiteUsageEntity(
                id = existing?.id ?: 0L,
                domain = clean,
                totalSecondsSpent = (existing?.totalSecondsSpent ?: 0L) + elapsedSeconds,
                dailyLimitMinutes = existing?.dailyLimitMinutes ?: 0,
                updatedAt = System.currentTimeMillis()
            )
            siteUsageDao.upsertSiteUsage(updated)
            updated.isDailyLimitExceeded
        }

    override suspend fun setSiteDailyLimitMinutes(domain: String, limitMinutes: Int) =
        withContext(dispatchers.io) {
            val clean = domain.trim().lowercase().removePrefix("www.")
            if (clean.isBlank()) return@withContext
            val existing = siteUsageDao.getUsageForDomain(clean)
            siteUsageDao.upsertSiteUsage(
                SiteUsageEntity(
                    id = existing?.id ?: 0L,
                    domain = clean,
                    totalSecondsSpent = existing?.totalSecondsSpent ?: 0L,
                    dailyLimitMinutes = limitMinutes.coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
            )
            Unit
        }

    override suspend fun resetAllSiteUsageStats() = withContext(dispatchers.io) {
        siteUsageDao.resetAllSiteUsage()
    }

    override suspend fun getSiteZoomForDomain(domain: String): Int = withContext(dispatchers.io) {
        val clean = domain.trim().lowercase().removePrefix("www.")
        siteZoomDao.getZoomForDomain(clean)?.textZoomPercent ?: 100
    }

    override suspend fun setSiteZoomForDomain(domain: String, zoomPercent: Int) = withContext(dispatchers.io) {
        val clean = domain.trim().lowercase().removePrefix("www.")
        if (clean.isNotBlank()) {
            val existing = siteZoomDao.getZoomForDomain(clean)
            siteZoomDao.upsertSiteZoom(
                SiteZoomEntity(
                    id = existing?.id ?: 0L,
                    domain = clean,
                    textZoomPercent = zoomPercent.coerceIn(50, 250)
                )
            )
        }
    }

    override suspend fun addWebAnnotation(
        pageUrl: String,
        pageTitle: String,
        quote: String,
        note: String
    ) = withContext(dispatchers.io) {
        webAnnotationDao.insertAnnotation(
            WebAnnotationEntity(
                pageUrl = pageUrl,
                pageTitle = pageTitle,
                highlightedQuote = quote.trim(),
                userNote = note.trim()
            )
        )
        Unit
    }

    override suspend fun deleteWebAnnotation(id: Long) = withContext(dispatchers.io) {
        webAnnotationDao.deleteAnnotation(id)
    }

    override suspend fun ensureDefaultRssFeedsSeeded() = withContext(dispatchers.io) {
        if (rssFeedDao.getCount() == 0) {
            rssFeedDao.insertFeed(RssFeedEntity(title = "Android Developers Blog", feedUrl = "https://android-developers.googleblog.com/feeds/posts/default?alt=rss"))
            rssFeedDao.insertFeed(RssFeedEntity(title = "Kotlin Blog", feedUrl = "https://blog.jetbrains.com/kotlin/feed/"))
            rssFeedDao.insertFeed(RssFeedEntity(title = "Hacker News Frontpage", feedUrl = "https://hnrss.org/frontpage"))
        }
    }

    override suspend fun addRssFeed(title: String, feedUrl: String) = withContext(dispatchers.io) {
        rssFeedDao.insertFeed(
            RssFeedEntity(
                title = title.trim().ifBlank { feedUrl.trim() },
                feedUrl = feedUrl.trim()
            )
        )
        Unit
    }

    override suspend fun deleteRssFeed(id: Long) = withContext(dispatchers.io) {
        rssFeedDao.deleteFeed(id)
    }

    override suspend fun saveSessionSnapshot(name: String, urls: List<String>) = withContext(dispatchers.io) {
        val cleanUrls = urls.filter { it.isNotBlank() && it != "nova://home" }
        if (cleanUrls.isNotEmpty()) {
            sessionSnapshotDao.insertSnapshot(
                SessionSnapshotEntity(
                    name = name.trim().ifBlank { "Session (${cleanUrls.size} Tabs)" },
                    serializedUrlsJson = Json.encodeToString(cleanUrls),
                    tabCount = cleanUrls.size
                )
            )
        }
    }

    override suspend fun deleteSessionSnapshot(id: Long) = withContext(dispatchers.io) {
        sessionSnapshotDao.deleteSnapshot(id)
    }

    override suspend fun addCookieWhitelistDomain(domain: String) = withContext(dispatchers.io) {
        val clean = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
        if (clean.isNotBlank()) {
            cookieWhitelistDao.addDomain(CookieWhitelistEntity(domain = clean))
        }
    }

    override suspend fun removeCookieWhitelistDomain(id: Long) = withContext(dispatchers.io) {
        cookieWhitelistDao.removeDomainById(id)
    }

    override suspend fun evaluateSitePermission(
        origin: String,
        type: SitePermissionType
    ): PermissionDecision? = withContext(dispatchers.io) {
        sitePermissionDao.getPermissionForOrigin(origin, type.name)?.toDomain()?.decision
    }

    override suspend fun saveSitePermission(
        origin: String,
        type: SitePermissionType,
        decision: PermissionDecision
    ) = withContext(dispatchers.io) {
        sitePermissionDao.upsertSitePermission(
            SitePermissionEntity(
                origin = origin,
                permissionType = type.name,
                decision = decision.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        Unit
    }

    override suspend fun removeSitePermission(id: Long) = withContext(dispatchers.io) {
        sitePermissionDao.deleteSitePermission(id)
    }

    override suspend fun clearAllSitePermissions() = withContext(dispatchers.io) {
        sitePermissionDao.clearAllSitePermissions()
    }

    override suspend fun recordHttpsUpgrade() = preferencesDataSource.incrementHttpsUpgradeCount()
    override suspend fun recordUnsafeSchemeBlocked() = preferencesDataSource.incrementUnsafeSchemeBlockedCount()
    override suspend fun recordSslWarningIntercepted() = preferencesDataSource.incrementSslWarningCount()
    override suspend fun recordAdOrTrackerBlocked() = preferencesDataSource.incrementAdsBlockedCount()
    override suspend fun recordTrackingParamsStripped(count: Int) = preferencesDataSource.incrementTrackingParamsStrippedCount(count)
    override suspend fun recordRedirectChainBlocked() = preferencesDataSource.incrementRedirectChainsBlockedCount()

    override suspend fun clearBrowsingData(
        clearHistory: Boolean,
        clearCookiesAndSiteData: Boolean,
        clearCache: Boolean,
        clearSitePermissions: Boolean
    ) {
        val whitelistedCookiesBackup = mutableMapOf<String, String>()
        if (clearCookiesAndSiteData) {
            val whitelisted = withContext(dispatchers.io) {
                cookieWhitelistDao.getAllWhitelistedDomainsOnce()
            }
            withContext(dispatchers.main) {
                val cm = CookieManager.getInstance()
                for (entry in whitelisted) {
                    val httpsUrl = "https://${entry.domain}"
                    cm.getCookie(httpsUrl)?.let { whitelistedCookiesBackup[httpsUrl] = it }
                }
            }
        }

        withContext(dispatchers.io) {
            if (clearHistory) {
                historyDao.clearAllHistory()
                faviconCacheManager.clearAllFavicons()
            }
            if (clearSitePermissions) {
                sitePermissionDao.clearAllSitePermissions()
            }
            if (clearCache) {
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.name != "tab_thumbs") {
                        file.deleteRecursively()
                    }
                }
            }
            preferencesDataSource.incrementDataClearCount()
        }

        if (clearCookiesAndSiteData) {
            withContext(dispatchers.main) {
                runCatching {
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies(null)
                    for ((url, cookieHeader) in whitelistedCookiesBackup) {
                        cookieHeader.split(";").forEach { singleCookie ->
                            cookieManager.setCookie(url, singleCookie.trim())
                        }
                    }
                    cookieManager.flush()
                    WebStorage.getInstance().deleteAllData()
                }
            }
        }
    }

    override suspend fun saveOfflineArticle(
        title: String,
        url: String,
        siteName: String,
        plainText: String,
        estimatedReadMinutes: Int,
        archivePath: String?
    ): Long = withContext(dispatchers.io) {
        val finalPath = if (!archivePath.isNullOrBlank()) {
            archivePath
        } else {
            val savedDir = File(context.filesDir, "saved_pages").apply { mkdirs() }
            val file = File(savedDir, "article_${System.currentTimeMillis()}.txt")
            file.writeText(plainText)
            file.absolutePath
        }
        savedPageDao.insertSavedPage(
            SavedPageEntity(
                title = title.ifBlank { url },
                url = url,
                siteName = siteName,
                plainTextContent = plainText,
                htmlFilePath = finalPath,
                estimatedReadMinutes = estimatedReadMinutes
            )
        )
    }

    override suspend fun deleteSavedOfflinePage(id: Long) = withContext(dispatchers.io) {
        savedPageDao.deleteSavedPage(id)
    }

    private fun fetchNormalizedTextAndHash(url: String): Pair<String, String> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NibrasNova-PageMonitor/1.4")
            }
            conn.connect()
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val textOnly = html.replace("<script[^>]*>[\s\S]*?</script>".toRegex(RegexOption.IGNORE_CASE), " ")
                .replace("<style[^>]*>[\s\S]*?</style>".toRegex(RegexOption.IGNORE_CASE), " ")
                .replace("<[^>]+>".toRegex(), " ")
                .replace("\s+".toRegex(), " ")
                .trim()
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(textOnly.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            digest to textOnly.take(160)
        } catch (_: Exception) {
            "" to ""
        } finally {
            conn?.disconnect()
        }
    }
}
