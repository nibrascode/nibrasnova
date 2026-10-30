package com.nibras.nova.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nibras.nova.data.local.converter.Converters
import com.nibras.nova.data.local.dao.BookmarkDao
import com.nibras.nova.data.local.dao.BookmarkFolderDao
import com.nibras.nova.data.local.dao.CookieWhitelistDao
import com.nibras.nova.data.local.dao.DownloadDao
import com.nibras.nova.data.local.dao.HistoryDao
import com.nibras.nova.data.local.dao.PageWatchDao
import com.nibras.nova.data.local.dao.PrivateVaultDao
import com.nibras.nova.data.local.dao.QuickShortcutDao
import com.nibras.nova.data.local.dao.RssFeedDao
import com.nibras.nova.data.local.dao.SavedPageDao
import com.nibras.nova.data.local.dao.SessionSnapshotDao
import com.nibras.nova.data.local.dao.SitePermissionDao
import com.nibras.nova.data.local.dao.SiteUsageDao
import com.nibras.nova.data.local.dao.SiteZoomDao
import com.nibras.nova.data.local.dao.TabDao
import com.nibras.nova.data.local.dao.UserScriptDao
import com.nibras.nova.data.local.dao.WebAnnotationDao
import com.nibras.nova.data.local.entity.BookmarkEntity
import com.nibras.nova.data.local.entity.BookmarkFolderEntity
import com.nibras.nova.data.local.entity.CookieWhitelistEntity
import com.nibras.nova.data.local.entity.DownloadEntity
import com.nibras.nova.data.local.entity.HistoryEntity
import com.nibras.nova.data.local.entity.PageWatchEntity
import com.nibras.nova.data.local.entity.PrivateVaultEntity
import com.nibras.nova.data.local.entity.QuickShortcutEntity
import com.nibras.nova.data.local.entity.RssFeedEntity
import com.nibras.nova.data.local.entity.SavedPageEntity
import com.nibras.nova.data.local.entity.SessionSnapshotEntity
import com.nibras.nova.data.local.entity.SitePermissionEntity
import com.nibras.nova.data.local.entity.SiteUsageEntity
import com.nibras.nova.data.local.entity.SiteZoomEntity
import com.nibras.nova.data.local.entity.TabEntity
import com.nibras.nova.data.local.entity.UserScriptEntity
import com.nibras.nova.data.local.entity.WebAnnotationEntity

@Database(
    entities = [
        BookmarkFolderEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        TabEntity::class,
        DownloadEntity::class,
        QuickShortcutEntity::class,
        SitePermissionEntity::class,
        SavedPageEntity::class,
        UserScriptEntity::class,
        CookieWhitelistEntity::class,
        WebAnnotationEntity::class,
        RssFeedEntity::class,
        SessionSnapshotEntity::class,
        SiteZoomEntity::class,
        PrivateVaultEntity::class,
        PageWatchEntity::class,
        SiteUsageEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun bookmarkFolderDao(): BookmarkFolderDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun tabDao(): TabDao
    abstract fun downloadDao(): DownloadDao
    abstract fun quickShortcutDao(): QuickShortcutDao
    abstract fun sitePermissionDao(): SitePermissionDao
    abstract fun savedPageDao(): SavedPageDao
    abstract fun userScriptDao(): UserScriptDao
    abstract fun cookieWhitelistDao(): CookieWhitelistDao
    abstract fun webAnnotationDao(): WebAnnotationDao
    abstract fun rssFeedDao(): RssFeedDao
    abstract fun sessionSnapshotDao(): SessionSnapshotDao
    abstract fun siteZoomDao(): SiteZoomDao
    abstract fun privateVaultDao(): PrivateVaultDao
    abstract fun pageWatchDao(): PageWatchDao
    abstract fun siteUsageDao(): SiteUsageDao
}
