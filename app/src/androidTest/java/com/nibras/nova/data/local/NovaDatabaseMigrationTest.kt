package com.nibras.nova.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nibras.nova.data.local.entity.BookmarkEntity
import com.nibras.nova.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NovaDatabaseMigrationTest {

    private lateinit var database: NovaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryBookmarksAndHistory() = runBlocking {
        val bookmarkDao = database.bookmarkDao()
        val historyDao = database.historyDao()

        bookmarkDao.insertBookmark(BookmarkEntity(title = "Nibras Code", url = "https://nibras.code"))
        historyDao.insertHistory(HistoryEntity(title = "Android Dev", url = "https://developer.android.com"))

        val bookmarks = bookmarkDao.observeAllBookmarks().first()
        val history = historyDao.observeRecentHistory(10).first()

        assertEquals(1, bookmarks.size)
        assertEquals("https://nibras.code", bookmarks.first().url)
        assertEquals(1, history.size)
        assertEquals("https://developer.android.com", history.first().url)
    }
}
