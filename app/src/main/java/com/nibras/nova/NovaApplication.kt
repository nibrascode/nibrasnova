package com.nibras.nova

import android.app.Application
import android.os.Build
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovaApplication : Application() {

    override fun onCreate() {
        com.nibras.nova.core.common.L.init(this)
        com.nibras.nova.core.security.AdultContentFilter.init(this)
        super.onCreate()
        configureWebViewProcessDataDirectory()
    }

    private fun configureWebViewProcessDataDirectory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName.replace(":", "_"))
            }
        }
    }
}
