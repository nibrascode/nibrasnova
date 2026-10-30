package com.nibras.nova.engine

import androidx.test.core.app.ApplicationProvider
import com.nibras.nova.core.common.L
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Writes the blocked page HTML for visual checks. Runs only when SNAPSHOT_DIR is set. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BlockedPageDumpTest {
    private fun dump(name: String) {
        val dir = System.getenv("SNAPSHOT_DIR"); assumeTrue(dir != null)
        L.init(ApplicationProvider.getApplicationContext())
        File(dir, "$name.html").writeText(BlockedPage.html())
        File(dir, "child_$name.html").writeText(BlockedPage.childHtml("youtube.com"))
    }
    @Test @Config(qualifiers = "az") fun az() = dump("blocked_az")
    @Test @Config(qualifiers = "ar") fun ar() = dump("blocked_ar")
}
