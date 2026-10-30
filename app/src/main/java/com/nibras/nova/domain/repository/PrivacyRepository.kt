package com.nibras.nova.domain.repository

import com.nibras.nova.data.local.entity.CookieWhitelistEntity
import com.nibras.nova.data.local.entity.PageWatchEntity
import com.nibras.nova.data.local.entity.PrivateVaultEntity
import com.nibras.nova.data.local.entity.RssFeedEntity
import com.nibras.nova.data.local.entity.SessionSnapshotEntity
import com.nibras.nova.data.local.entity.SiteUsageEntity
import com.nibras.nova.data.local.entity.SiteZoomEntity
import com.nibras.nova.data.local.entity.WebAnnotationEntity
import com.nibras.nova.domain.model.PermissionDecision
import com.nibras.nova.domain.model.PrivacyMetrics
import com.nibras.nova.domain.model.SavedOfflinePage
import com.nibras.nova.domain.model.SitePermission
import com.nibras.nova.domain.model.SitePermissionType
import kotlinx.coroutines.flow.Flow

interface PrivacyRepository {
    fun observePrivacyMetrics(): Flow<PrivacyMetrics>
    fun observeSitePermissions(): Flow<List<SitePermission>>
    fun observeSavedOfflinePages(): Flow<List<SavedOfflinePage>>
    fun observeWhitelistedCookieDomains(): Flow<List<CookieWhitelistEntity>>
    fun observeAllAnnotations(): Flow<List<WebAnnotationEntity>>
    fun observeAnnotationsForUrl(url: String): Flow<List<WebAnnotationEntity>>
    fun observeRssFeeds(): Flow<List<RssFeedEntity>>
    fun observeSessionSnapshots(): Flow<List<SessionSnapshotEntity>>
    fun observeAllSiteZooms(): Flow<List<SiteZoomEntity>>
    // Stage 13
    fun observePrivateVaultItems(): Flow<List<PrivateVaultEntity>>
    fun observeWatchedPages(): Flow<List<PageWatchEntity>>
    fun observeAllSiteUsage(): Flow<List<SiteUsageEntity>>
    suspend fun addPrivateVaultItem(title: String, urlOrNote: String, category: String)
    suspend fun deletePrivateVaultItem(id: Long)
    suspend fun addPageWatch(title: String, url: String)
    suspend fun checkAllWatchedPagesForChanges(): Int
    suspend fun deletePageWatch(id: Long)
    suspend fun recordSiteUsageDuration(domain: String, elapsedSeconds: Long): Boolean
    suspend fun setSiteDailyLimitMinutes(domain: String, limitMinutes: Int)
    suspend fun resetAllSiteUsageStats()

    suspend fun getSiteZoomForDomain(domain: String): Int
    suspend fun setSiteZoomForDomain(domain: String, zoomPercent: Int)
    suspend fun addWebAnnotation(pageUrl: String, pageTitle: String, quote: String, note: String)
    suspend fun deleteWebAnnotation(id: Long)
    suspend fun ensureDefaultRssFeedsSeeded()
    suspend fun addRssFeed(title: String, feedUrl: String)
    suspend fun deleteRssFeed(id: Long)
    suspend fun saveSessionSnapshot(name: String, urls: List<String>)
    suspend fun deleteSessionSnapshot(id: Long)
    suspend fun addCookieWhitelistDomain(domain: String)
    suspend fun removeCookieWhitelistDomain(id: Long)
    suspend fun evaluateSitePermission(origin: String, type: SitePermissionType): PermissionDecision?
    suspend fun saveSitePermission(origin: String, type: SitePermissionType, decision: PermissionDecision)
    suspend fun removeSitePermission(id: Long)
    suspend fun clearAllSitePermissions()
    suspend fun recordHttpsUpgrade()
    suspend fun recordUnsafeSchemeBlocked()
    suspend fun recordSslWarningIntercepted()
    suspend fun recordAdOrTrackerBlocked()
    suspend fun recordTrackingParamsStripped(count: Int)
    suspend fun recordRedirectChainBlocked()
    suspend fun clearBrowsingData(
        clearHistory: Boolean,
        clearCookiesAndSiteData: Boolean,
        clearCache: Boolean,
        clearSitePermissions: Boolean
    )
    suspend fun saveOfflineArticle(
        title: String,
        url: String,
        siteName: String,
        plainText: String,
        estimatedReadMinutes: Int,
        archivePath: String? = null
    ): Long
    suspend fun deleteSavedOfflinePage(id: Long)
}
