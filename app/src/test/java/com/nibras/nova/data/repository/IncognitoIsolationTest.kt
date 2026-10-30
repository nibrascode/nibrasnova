package com.nibras.nova.data.repository

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.core.util.TabThumbnailManager
import com.nibras.nova.data.local.dao.HistoryDao
import com.nibras.nova.data.local.dao.TabDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncognitoIsolationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    @Test
    fun `HistoryRepository never persists visits when isPrivateSession is true`() = runTest {
        val historyDao: HistoryDao = mockk(relaxed = true)
        val repo = HistoryRepositoryImpl(historyDao, dispatchers)

        repo.recordVisit(
            title = "Secret Private Site",
            url = "https://private.example.com",
            faviconPath = null,
            isPrivateSession = true
        )

        coVerify(exactly = 0) { historyDao.insertHistory(any()) }
    }

    @Test
    fun `TabRepository stores private tabs strictly in memory and never writes to TabDao`() = runTest {
        val tabDao: TabDao = mockk(relaxed = true)
        val thumbnailManager: TabThumbnailManager = mockk(relaxed = true)
        val repo = TabRepositoryImpl(tabDao, thumbnailManager, dispatchers)

        repo.createTab(
            url = "https://duckduckgo.com",
            isPrivate = true,
            isDesktopMode = false,
            selectImmediately = true
        )

        coVerify(exactly = 0) { tabDao.upsertTab(any()) }
        coVerify(exactly = 0) { tabDao.replaceAllNormalTabs(any()) }
    }
}
