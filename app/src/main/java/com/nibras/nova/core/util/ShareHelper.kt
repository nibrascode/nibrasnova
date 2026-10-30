package com.nibras.nova.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.nibras.nova.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    fun shareWebPage(activityContext: Context, title: String, url: String) {
        val shareText = if (title.isNotBlank() && title != url) "$title\n$url" else url
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share via Nibras Nova").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activityContext.startActivity(chooser)
    }

    fun shareSelectedText(activityContext: Context, selectedText: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, selectedText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Selected Text").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activityContext.startActivity(chooser)
    }

    suspend fun saveAndShareScreenshot(activityContext: Context, bitmap: Bitmap, pageTitle: String): Result<String> =
        withContext(dispatchers.io) {
            try {
                val screenshotDir = File(context.cacheDir, "screenshots").apply { mkdirs() }
                val safeName = pageTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(32).ifBlank { "nova_capture" }
                val file = File(screenshotDir, "${safeName}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                withContext(dispatchers.main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activityContext.startActivity(
                        Intent.createChooser(shareIntent, "Share Webpage Screenshot").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                Result.success(file.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
