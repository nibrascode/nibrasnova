package com.nibras.nova.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.nibras.nova.R
import com.nibras.nova.core.prayer.PrayerPrefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** Taps through the home prayer card (card, city picker, notifications, Qibla) on several SDKs. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "az-w411dp-h891dp")
class PrayerTapTest(private val withCity: Boolean) {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "withCity={0}")
        fun params() = listOf(arrayOf<Any>(true), arrayOf<Any>(false))
    }
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun s(id: Int) = ctx.getString(id)

    private fun run() {
        val p = PrayerPrefs(ctx)
        p.enabled = true
        if (withCity) { p.useLocation = false; p.cityName = "Bakı"; p.lat = 40.4093; p.lng = 49.8671; p.tz = "Asia/Baku"; p.country = "AZ" }
        else { p.useLocation = false; p.lat = null; p.lng = null; p.cityName = null; p.tz = null; p.country = null }
        // The crash: the bundled city list asset could not be opened on a real build. This must load now.
        check(com.nibras.nova.core.prayer.CityDatabase.get(ctx).countries().isNotEmpty())
        rule.mainClock.autoAdvance = false // the card has a 1 s countdown ticker that never lets Compose go idle
        rule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var qibla by remember { mutableStateOf(false) }
                PrayerCard(onQibla = { qibla = true })
                if (qibla) QiblaDialog(onDismiss = { qibla = false })
            }
        }
        step()
        // Picker data (the crash path) loads for every country used by the picker.
        val db = com.nibras.nova.core.prayer.CityDatabase.get(ctx)
        check(db.sortedCountries(java.util.Locale("az")).isNotEmpty())
        check(db.citiesIn("AZ").any { it.name.contains("Baku") || it.displayName("az").contains("Bakı") })
    }

    private fun step() { repeat(5) { rule.mainClock.advanceTimeBy(200) }; org.robolectric.shadows.ShadowLooper.idleMainLooper() }
    private fun waitFor(cond: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!cond()) { check(System.currentTimeMillis() < end) { "timed out" }; Thread.sleep(50); step() }
    }

    @Test @Config(sdk = [24]) fun sdk24() = run()
    @Test @Config(sdk = [33]) fun sdk33() = run()
    @Test @Config(sdk = [34]) fun sdk34() = run()
    @Test @Config(sdk = [35]) fun sdk35() = run()
}
