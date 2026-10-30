package com.nibras.nova.core.util

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrintHelper @Inject constructor() {

    fun printWebPage(activityContext: Context, webView: WebView, pageTitle: String) {
        val printManager = activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val cleanTitle = pageTitle.ifBlank { "Nibras_Nova_Document" }
        val jobName = L.s(R.string.t_nibras_nova, cleanTitle)
        val printAdapter = webView.createPrintDocumentAdapter(cleanTitle)
        printManager.print(
            jobName,
            printAdapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }
}
