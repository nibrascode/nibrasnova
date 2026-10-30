package com.nibras.nova.core.util

import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.domain.model.HistoryItem
import org.junit.Assert.*
import org.junit.Test

class BrowserDataJsonTest {
    @Test fun roundTrip() {
        val txt = BrowserDataJson.encode(
            listOf(BookmarkItem(title = "Nibras", url = "https://nibrascode.com", folderId = 3, createdAt = 5)),
            listOf(HistoryItem(title = "G", url = "https://google.com", visitedAt = 9)),
            { if (it == 3L) "Work" else null }, now = 1
        )
        val b = BrowserDataJson.decode(txt)
        assertEquals("Work", b.bookmarks[0].folder); assertEquals(9L, b.history[0].visitedAt)
    }
    @Test fun dropsUnsafeSchemes() {
        val b = BrowserDataJson.decode("""{"format":"nibras-nova-backup","bookmarks":[{"title":"x","url":"javascript:alert(1)"}],"history":[{"title":"f","url":"file:///x","visitedAt":1}]}""")
        assertTrue(b.bookmarks.isEmpty()); assertTrue(b.history.isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOtherFiles() { BrowserDataJson.decode("""{"format":"x"}""") }
}
