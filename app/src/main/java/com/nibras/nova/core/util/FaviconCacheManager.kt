package com.nibras.nova.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.nibras.nova.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaviconCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    private val memoryCache = object : LruCache<String, Bitmap>(64) {}
    private val faviconDir: File by lazy {
        File(context.cacheDir, "favicons").apply { mkdirs() }
    }

    fun getMemoryFavicon(url: String): Bitmap? {
        val host = extractHostKey(url) ?: return null
        return memoryCache.get(host)
    }

    suspend fun saveFavicon(url: String, bitmap: Bitmap, isIncognito: Boolean): String? = withContext(dispatchers.io) {
        val host = extractHostKey(url) ?: return@withContext null
        memoryCache.put(host, bitmap)
        if (isIncognito) {
            return@withContext null
        }
        return@withContext try {
            val targetFile = File(faviconDir, "$host.png")
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            targetFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadFaviconBitmap(url: String): Bitmap? = withContext(dispatchers.io) {
        val host = extractHostKey(url) ?: return@withContext null
        memoryCache.get(host)?.let { return@withContext it }
        val file = File(faviconDir, "$host.png")
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.also {
                memoryCache.put(host, it)
            }
        } else {
            null
        }
    }

    suspend fun clearAllFavicons() = withContext(dispatchers.io) {
        memoryCache.evictAll()
        faviconDir.listFiles()?.forEach { it.delete() }
    }

    private fun extractHostKey(url: String): String? {
        return try {
            Uri.parse(url).host?.lowercase()?.replace("[^a-z0-9._-]".toRegex(), "_")
        } catch (_: Exception) {
            null
        }
    }
}
