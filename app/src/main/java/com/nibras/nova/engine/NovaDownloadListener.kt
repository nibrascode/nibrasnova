package com.nibras.nova.engine

import android.webkit.CookieManager
import android.webkit.DownloadListener

data class PendingDownloadRequest(
    val url: String,
    val userAgent: String,
    val contentDisposition: String?,
    val mimeType: String?,
    val contentLength: Long,
    val cookies: String?
)

class NovaDownloadListener(
    private val onDownloadRequested: (PendingDownloadRequest) -> Unit
) : DownloadListener {

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        val cleanUrl = url?.trim().orEmpty()
        if (cleanUrl.isBlank()) return
        val cookies = runCatching { CookieManager.getInstance().getCookie(cleanUrl) }.getOrNull()
        onDownloadRequested(
            PendingDownloadRequest(
                url = cleanUrl,
                userAgent = userAgent ?: "NibrasNova/1.0",
                contentDisposition = contentDisposition,
                mimeType = mimetype,
                contentLength = contentLength,
                cookies = cookies
            )
        )
    }
}
