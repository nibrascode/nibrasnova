package com.nibras.nova.data.repository

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.data.local.dao.HistoryDao
import com.nibras.nova.domain.model.HistoryClearRange
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    @Test
    fun `clearHistoryByRange deletes all history for ALL_TIME and time-bounded for LAST_HOUR`() = runTest {
        val historyDao: HistoryDao = mockk(relaxed = true)
        val repo = HistoryRepositoryImpl(historyDao, dispatchers)

        repo.clearHistoryByRange(HistoryClearRange.ALL_TIME)
        coVerify(exactly = 1) { historyDao.clearAllHistory() }

        repo.clearHistoryByRange(HistoryClearRange.LAST_HOUR)
        coVerify(exactly = 1) { historyDao.deleteHistorySince(any()) }
    }
}
