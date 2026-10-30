package com.nibras.nova.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.widget.Toast
import com.nibras.nova.R
import com.nibras.nova.core.common.L

/** Long-press menu for links and images: open, copy, share. */
object LinkLongPressMenu {
    fun install(webView: WebView) {
        webView.setOnLongClickListener { v ->
            val wv = v as WebView
            val hit = wv.hitTestResult
            val url = hit.extra
            val isLink = hit.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            val isImage = hit.type == WebView.HitTestResult.IMAGE_TYPE || hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            if (url.isNullOrBlank() || !(isLink || isImage) || !(url.startsWith("http://") || url.startsWith("https://"))) return@setOnLongClickListener false
            if (isImage && !isLink) {
                show(wv, url, L.s(R.string.lp_open_image))
            } else {
                show(wv, url, L.s(R.string.lp_open))
            }
            true
        }
    }

    private fun show(wv: WebView, url: String, openLabel: String) {
        val ctx = wv.context
        val items = arrayOf(openLabel, L.s(R.string.lp_copy), L.s(R.string.lp_share))
        android.app.AlertDialog.Builder(ctx)
            .setTitle(url.take(120))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> wv.loadUrl(url) // filter still applies in shouldInterceptRequest
                    1 -> copy(ctx, url)
                    2 -> ctx.startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url), null)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            .show()
    }

    private fun copy(ctx: Context, url: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText("URL", url))
        Toast.makeText(ctx, L.s(R.string.lp_copied), Toast.LENGTH_SHORT).show()
    }
}
