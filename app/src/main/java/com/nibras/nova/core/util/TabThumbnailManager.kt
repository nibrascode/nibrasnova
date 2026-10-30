package com.nibras.nova.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.util.LruCache
import android.webkit.WebView
import com.nibras.nova.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabThumbnailManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    private val incognitoThumbCache = object : LruCache<String, Bitmap>(16) {}
    private val normalMemCache = object : LruCache<String, Bitmap>(16) {}

    private val thumbDir: File by lazy {
        File(context.cacheDir, "tab_thumbs").apply { mkdirs() }
    }

    fun captureWebViewSnapshotOnMainThread(webView: WebView): Bitmap? {
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return null
        return try {
            val targetWidth = (width * 0.45f).toInt().coerceAtLeast(180)
            val targetHeight = (height * 0.45f).toInt().coerceAtLeast(280)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scaleX = targetWidth.toFloat() / width.toFloat()
            val scaleY = targetHeight.toFloat() / height.toFloat()
            canvas.scale(scaleX, scaleY)
            canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())
            webView.draw(canvas)
            bitmap
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun persistSnapshot(tabId: String, bitmap: Bitmap, isPrivate: Boolean): String? = withContext(dispatchers.io) {
        if (isPrivate) {
            incognitoThumbCache.put(tabId, bitmap)
            return@withContext "memory://incognito/$tabId"
        }
        normalMemCache.put(tabId, bitmap)
        return@withContext try {
            val file = File(thumbDir, "$tabId.webp")
            FileOutputStream(file).use { out ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(format, 75, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getThumbnailBitmap(tabId: String, isPrivate: Boolean): Bitmap? = withContext(dispatchers.io) {
        if (isPrivate) {
            return@withContext incognitoThumbCache.get(tabId)
        }
        normalMemCache.get(tabId)?.let { return@withContext it }
        val file = File(thumbDir, "$tabId.webp")
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.also {
                normalMemCache.put(tabId, it)
            }
        } else {
            null
        }
    }

    suspend fun deleteThumbnail(tabId: String, isPrivate: Boolean) = withContext(dispatchers.io) {
        if (isPrivate) {
            incognitoThumbCache.remove(tabId)
        } else {
            normalMemCache.remove(tabId)
            File(thumbDir, "$tabId.webp").delete()
        }
    }

    suspend fun clearAllIncognitoThumbnails() = withContext(dispatchers.io) {
        incognitoThumbCache.evictAll()
    }
}
