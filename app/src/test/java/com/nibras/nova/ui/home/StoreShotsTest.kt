package com.nibras.nova.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Raw screens for Play store frames. Runs only when SNAPSHOT_DIR is set. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class StoreShotsTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun shot(name: String, setup: () -> Unit = {}, c: @Composable () -> Unit) {
        val dir = System.getenv("SNAPSHOT_DIR"); assumeTrue(dir != null)
        com.nibras.nova.core.prayer.PrayerPrefs(rule.activity).apply {
            enabled = true; useLocation = false; lat = 40.4093; lng = 49.8671; tz = "Asia/Baku"; cityName = "Baku"; country = "AZ"
        }
        setup()
        rule.setContent { MaterialTheme(colorScheme = darkColorScheme()) { c() } }
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun lang() = rule.activity.resources.configuration.locales[0].language
    private fun withPin() { com.nibras.nova.core.parental.ParentalControls.get(rule.activity).pin.setPin("2580") }

    @Composable private fun Pad(c: @Composable ColumnScope.() -> Unit) =
        Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = c) }

    private fun qibla() = shot("store_qibla_" + lang()) { QiblaContent(40.4093, 49.8671, if (lang() == "az") "Bakı" else "Baku", 196f, 3) {} }
    private fun parental() = shot("store_parental_" + lang(), ::withPin) { com.nibras.nova.ui.parental.ParentalControlsContent {} }
    private fun playground() = shot("store_playground_" + lang()) { com.nibras.nova.ui.playground.HtmlPlaygroundContent({}, initialTab = 0, previewEnabled = false) }
    private fun notif() = shot("store_prayer_" + lang()) {
        Pad {
            PrayerCard()
            Spacer(Modifier.height(8.dp))
            PrayerNotificationsBody(true, granted = true, exact = true, before = { true }, at = { true },
                onEnabled = {}, onBefore = { _, _ -> }, onAt = { _, _ -> }, onOpenNotifSettings = {}, onAllowExact = {})
        }
    }
    private fun family() = shot("store_family_" + lang(), ::withPin) {
        Pad { com.nibras.nova.ui.parental.FamilySection({ it() }, "ok") {} }
    }

    @Test @Config(qualifiers = "en-w412dp-h915dp-xxhdpi") fun en_qibla() = qibla()
    @Test @Config(qualifiers = "en-w412dp-h915dp-xxhdpi") fun en_parental() = parental()
    @Test @Config(qualifiers = "en-w412dp-h915dp-xxhdpi") fun en_playground() = playground()
    @Test @Config(qualifiers = "en-w412dp-h915dp-xxhdpi") fun en_notif() = notif()
    @Test @Config(qualifiers = "en-w412dp-h915dp-xxhdpi") fun en_family() = family()
    @Test @Config(qualifiers = "az-w412dp-h915dp-xxhdpi") fun az_qibla() = qibla()
    @Test @Config(qualifiers = "az-w412dp-h915dp-xxhdpi") fun az_parental() = parental()
    @Test @Config(qualifiers = "az-w412dp-h915dp-xxhdpi") fun az_playground() = playground()
    @Test @Config(qualifiers = "az-w412dp-h915dp-xxhdpi") fun az_notif() = notif()
    @Test @Config(qualifiers = "az-w412dp-h915dp-xxhdpi") fun az_family() = family()
    @Test @Config(qualifiers = "tr-w412dp-h915dp-xxhdpi") fun tr_qibla() = qibla()
    @Test @Config(qualifiers = "tr-w412dp-h915dp-xxhdpi") fun tr_parental() = parental()
    @Test @Config(qualifiers = "tr-w412dp-h915dp-xxhdpi") fun tr_playground() = playground()
    @Test @Config(qualifiers = "tr-w412dp-h915dp-xxhdpi") fun tr_notif() = notif()
    @Test @Config(qualifiers = "tr-w412dp-h915dp-xxhdpi") fun tr_family() = family()
    @Test @Config(qualifiers = "ru-w412dp-h915dp-xxhdpi") fun ru_qibla() = qibla()
    @Test @Config(qualifiers = "ru-w412dp-h915dp-xxhdpi") fun ru_parental() = parental()
    @Test @Config(qualifiers = "ru-w412dp-h915dp-xxhdpi") fun ru_playground() = playground()
    @Test @Config(qualifiers = "ru-w412dp-h915dp-xxhdpi") fun ru_notif() = notif()
    @Test @Config(qualifiers = "ru-w412dp-h915dp-xxhdpi") fun ru_family() = family()
    @Test @Config(qualifiers = "ar-ldrtl-w412dp-h915dp-xxhdpi") fun ar_qibla() = qibla()
    @Test @Config(qualifiers = "ar-ldrtl-w412dp-h915dp-xxhdpi") fun ar_parental() = parental()
    @Test @Config(qualifiers = "ar-ldrtl-w412dp-h915dp-xxhdpi") fun ar_playground() = playground()
    @Test @Config(qualifiers = "ar-ldrtl-w412dp-h915dp-xxhdpi") fun ar_notif() = notif()
    @Test @Config(qualifiers = "ar-ldrtl-w412dp-h915dp-xxhdpi") fun ar_family() = family()
}
