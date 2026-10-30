package com.nibras.nova.data.repository

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.data.local.dao.BookmarkDao
import com.nibras.nova.data.local.dao.BookmarkFolderDao
import com.nibras.nova.data.local.entity.BookmarkEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    @Test
    fun `toggleBookmark inserts when absent and deletes when already bookmarked`() = runTest {
        val bookmarkDao: BookmarkDao = mockk(relaxed = true)
        val folderDao: BookmarkFolderDao = mockk(relaxed = true)
        val repo = BookmarkRepositoryImpl(bookmarkDao, folderDao, dispatchers)

        coEvery { bookmarkDao.getBookmarkByUrl("https://kotlinlang.org") } returns null
        val added = repo.toggleBookmark("Kotlin", "https://kotlinlang.org", null, null)
        assertTrue(added)
        coVerify(exactly = 1) { bookmarkDao.insertBookmark(any()) }

        coEvery { bookmarkDao.getBookmarkByUrl("https://kotlinlang.org") } returns
            BookmarkEntity(id = 42L, title = "Kotlin", url = "https://kotlinlang.org")
        val removed = repo.toggleBookmark("Kotlin", "https://kotlinlang.org", null, null)
        assertFalse(removed)
        coVerify(exactly = 1) { bookmarkDao.deleteBookmarkById(42L) }
    }
}
