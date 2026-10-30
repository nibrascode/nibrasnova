package com.nibras.nova.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Stage 11.3: Full-Page Scrolling Screenshot Capture.
 * Captures the entire vertical document height of a WebView (up to a safe memory ceiling of 9,000px)
 * so users can export long articles, receipts, or documentation pages as a single stitched PNG.
 */
@Singleton
class FullPageScreenshotHelper @Inject constructor() {

    @Suppress("DEPRECATION")
    fun captureFullScrollingPageOnMainThread(webView: WebView): Bitmap? {
        val width = webView.width
        if (width <= 0) return null
        val scale = webView.scale
        val totalHeight = (webView.contentHeight * scale).roundToInt()
            .coerceIn(webView.height.coerceAtLeast(400), 8_500)

        return try {
            val targetWidth = (width * 0.75f).roundToInt().coerceAtLeast(360)
            val ratio = targetWidth.toFloat() / width.toFloat()
            val targetHeight = (totalHeight * ratio).roundToInt().coerceAtLeast(400)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(ratio, ratio)
            webView.draw(canvas)
            bitmap
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
