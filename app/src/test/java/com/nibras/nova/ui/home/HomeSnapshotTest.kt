package com.nibras.nova.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the composed home layout to PNG. Runs only when SNAPSHOT_DIR is set. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi")
class HomeSnapshotTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun shot(name: String, rtl: Boolean) {
        val dir = System.getenv("SNAPSHOT_DIR"); assumeTrue(dir != null)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Column(Modifier.fillMaxSize().background(HomeBg)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            HomeHero()
                            Box(Modifier.padding(horizontal = 20.dp)) {
                                HomeSearchBar("", {}, {}, "Google", false, {}, {}, {}, androidx.compose.ui.res.stringResource(com.nibras.nova.R.string.home_search_hint))
                            }
                            Row(Modifier.padding(horizontal = 14.dp)) {
                                listOf("Google" to "https://google.com", "YouTube" to "https://youtube.com", "GitHub" to "https://github.com", "Nibras" to "https://nibrascode.com").forEach { (t, u) ->
                                    QuickTile(t, {}, modifier = Modifier.weight(1f)) { TileIcon(u, t) }
                                }
                                QuickTile(androidx.compose.ui.res.stringResource(com.nibras.nova.R.string.home_add), {}, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                }
                            }
                            Column {
                                Box(Modifier.padding(horizontal = 20.dp)) { DiscoverHeader {} }
                                DiscoverRow(DISCOVER_ITEMS) {}
                            }
                        }
                        HomeBottomBar(1, {}, {}, {}, {}, false, {}, {})
                    }
                }
            }
        }
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test @Config(qualifiers = "az-w411dp-h891dp-xxhdpi") fun az() = shot("home_az", false)
    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi") fun ar() = shot("home_ar", true)
    @Test @Config(qualifiers = "en-w411dp-h891dp-xxhdpi") fun en() = shot("home_en", false)
}
