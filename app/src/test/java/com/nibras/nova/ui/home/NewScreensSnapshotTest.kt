package com.nibras.nova.ui.home

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class NewScreensSnapshotTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun shot(name: String, rtl: Boolean, c: @Composable () -> Unit) {
        val dir = System.getenv("SNAPSHOT_DIR"); assumeTrue(dir != null)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme(colorScheme = darkColorScheme()) { c() }
            }
        }
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test @Config(qualifiers = "az-w411dp-h891dp-xxhdpi") fun qiblaAz() = shot("qibla_az", false) { QiblaContent(40.4093, 49.8671, "Bakı", 180f, 3) {} }
    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi") fun qiblaAr() = shot("qibla_ar", true) { QiblaContent(40.4093, 49.8671, "باكو", null, 0) {} }
    @Test @Config(qualifiers = "az-w411dp-h891dp-xxhdpi") fun parentalAz() = shot("parental_az", false) { com.nibras.nova.ui.parental.ParentalControlsContent {} }
    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi") fun parentalAr() = shot("parental_ar", true) { com.nibras.nova.ui.parental.ParentalControlsContent {} }
    @Test @Config(qualifiers = "az-w411dp-h891dp-xxhdpi") fun playgroundAz() = shot("playground_az", false) {
        com.nibras.nova.ui.playground.HtmlPlaygroundContent({}, initialTab = 2, previewEnabled = false)
    }
    @Test @Config(qualifiers = "az-w411dp-h891dp-xxhdpi") fun prayerNotifAz() = shot("pnotif_az", false) { NotifHost() }
    @Test @Config(qualifiers = "ar-w411dp-h891dp-xxhdpi") fun prayerNotifAr() = shot("pnotif_ar", true) { NotifHost() }
    @Test @Config(qualifiers = "az-w411dp-h891dp-xxhdpi") fun familyAz() = shot("family_az", false) {
        androidx.compose.material3.Surface(androidx.compose.ui.Modifier) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(20.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                com.nibras.nova.ui.parental.FamilySection({ it() }, "ok") {}
            }
        }
    }
    @androidx.compose.runtime.Composable private fun NotifHost() {
        androidx.compose.material3.Surface(androidx.compose.ui.Modifier) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(20.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                com.nibras.nova.ui.home.PrayerNotificationsBody(true, granted = false, exact = false, before = { true }, at = { it != com.nibras.nova.core.prayer.PrayerName.FAJR },
                    onEnabled = {}, onBefore = { _, _ -> }, onAt = { _, _ -> }, onOpenNotifSettings = {}, onAllowExact = {})
            }
        }
    }
}
