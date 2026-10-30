package com.nibras.nova

import android.app.Application
import android.os.Build
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovaApplication : Application() {

    override fun onCreate() {
        com.nibras.nova.core.common.L.init(this)
        com.nibras.nova.core.parental.FamilyProfile.init(this)
        com.nibras.nova.core.halal.HalalImageFilter.init(this)
        com.nibras.nova.core.security.AdultContentFilter.init(this)
        com.nibras.nova.core.parental.ParentalControls.get(this)
        super.onCreate()
        configureWebViewProcessDataDirectory()
        com.nibras.nova.core.prayer.PrayerAlarms.rescheduleAsync(this)
    }

    private fun configureWebViewProcessDataDirectory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            val procSuffix = if (packageName != processName) processName.replace(":", "_") else null
            com.nibras.nova.core.parental.FamilyProfile.webViewSuffix(procSuffix)?.let { WebView.setDataDirectorySuffix(it) }
        }
    }
}
