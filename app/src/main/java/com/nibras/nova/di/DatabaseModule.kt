package com.nibras.nova.di

import android.content.Context
import androidx.room.Room
import com.nibras.nova.core.common.NovaConstants
import com.nibras.nova.data.local.NovaDatabase
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
import com.nibras.nova.data.repository.BookmarkRepositoryImpl
import com.nibras.nova.data.repository.DownloadRepositoryImpl
import com.nibras.nova.data.repository.HistoryRepositoryImpl
import com.nibras.nova.data.repository.PrivacyRepositoryImpl
import com.nibras.nova.data.repository.SettingsRepositoryImpl
import com.nibras.nova.data.repository.ShortcutRepositoryImpl
import com.nibras.nova.data.repository.TabRepositoryImpl
import com.nibras.nova.data.repository.UserScriptRepositoryImpl
import com.nibras.nova.domain.repository.BookmarkRepository
import com.nibras.nova.domain.repository.DownloadRepository
import com.nibras.nova.domain.repository.HistoryRepository
import com.nibras.nova.domain.repository.PrivacyRepository
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.repository.ShortcutRepository
import com.nibras.nova.domain.repository.TabRepository
import com.nibras.nova.domain.repository.UserScriptRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseProvideModule {

    @Provides
    @Singleton
    fun provideNovaDatabase(
        @ApplicationContext context: Context
    ): NovaDatabase {
        return Room.databaseBuilder(
            context,
            NovaDatabase::class.java,
            NovaConstants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun provideBookmarkFolderDao(db: NovaDatabase): BookmarkFolderDao = db.bookmarkFolderDao()
    @Provides fun provideBookmarkDao(db: NovaDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun provideHistoryDao(db: NovaDatabase): HistoryDao = db.historyDao()
    @Provides fun provideTabDao(db: NovaDatabase): TabDao = db.tabDao()
    @Provides fun provideDownloadDao(db: NovaDatabase): DownloadDao = db.downloadDao()
    @Provides fun provideQuickShortcutDao(db: NovaDatabase): QuickShortcutDao = db.quickShortcutDao()
    @Provides fun provideSitePermissionDao(db: NovaDatabase): SitePermissionDao = db.sitePermissionDao()
    @Provides fun provideSavedPageDao(db: NovaDatabase): SavedPageDao = db.savedPageDao()
    @Provides fun provideUserScriptDao(db: NovaDatabase): UserScriptDao = db.userScriptDao()
    @Provides fun provideCookieWhitelistDao(db: NovaDatabase): CookieWhitelistDao = db.cookieWhitelistDao()
    @Provides fun provideWebAnnotationDao(db: NovaDatabase): WebAnnotationDao = db.webAnnotationDao()
    @Provides fun provideRssFeedDao(db: NovaDatabase): RssFeedDao = db.rssFeedDao()
    @Provides fun provideSessionSnapshotDao(db: NovaDatabase): SessionSnapshotDao = db.sessionSnapshotDao()
    @Provides fun provideSiteZoomDao(db: NovaDatabase): SiteZoomDao = db.siteZoomDao()
    @Provides fun providePrivateVaultDao(db: NovaDatabase): PrivateVaultDao = db.privateVaultDao()
    @Provides fun providePageWatchDao(db: NovaDatabase): PageWatchDao = db.pageWatchDao()
    @Provides fun provideSiteUsageDao(db: NovaDatabase): SiteUsageDao = db.siteUsageDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindModule {

    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindShortcutRepository(impl: ShortcutRepositoryImpl): ShortcutRepository
    @Binds @Singleton abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository
    @Binds @Singleton abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
    @Binds @Singleton abstract fun bindTabRepository(impl: TabRepositoryImpl): TabRepository
    @Binds @Singleton abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
    @Binds @Singleton abstract fun bindPrivacyRepository(impl: PrivacyRepositoryImpl): PrivacyRepository
    @Binds @Singleton abstract fun bindUserScriptRepository(impl: UserScriptRepositoryImpl): UserScriptRepository
}
