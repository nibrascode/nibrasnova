package com.nibras.nova.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.domain.model.BookmarkItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkImportExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    /**
     * Generates a standards-compliant Netscape Bookmark HTML file compatible with desktop Chrome, Firefox, Brave, and Safari.
     */
    fun buildNetscapeBookmarkHtml(bookmarks: List<BookmarkItem>): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        sb.appendLine("<TITLE>Nibras Nova Bookmarks</TITLE>")
        sb.appendLine("<H1>Nibras Nova Bookmarks</H1>")
        sb.appendLine("<DL><p>")
        for (bm in bookmarks) {
            val escapedTitle = bm.title
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            val escapedUrl = bm.url.replace("\"", "%22")
            val timestampSec = bm.createdAt / 1000L
            sb.appendLine("    <DT><A HREF=\"$escapedUrl\" ADD_DATE=\"$timestampSec\">$escapedTitle</A>")
        }
        sb.appendLine("</DL><p>")
        return sb.toString()
    }

    /**
     * Parses any Netscape Bookmark HTML string into BookmarkItem domain models.
     */
    fun parseNetscapeBookmarkHtml(htmlContent: String): List<BookmarkItem> {
        val regex = Regex(
            pattern = "<A\\s+[^>]*HREF=[\"']([^\"']+)[\"'][^>]*>(.*?)</A>",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.findAll(htmlContent).mapNotNull { match ->
            val url = match.groupValues[1].trim()
            val title = match.groupValues[2].trim()
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
            if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                BookmarkItem(
                    title = title.ifBlank { url },
                    url = url
                )
            } else null
        }.toList()
    }

    suspend fun exportAndShareHtmlFile(
        activityContext: Context,
        bookmarks: List<BookmarkItem>
    ): Result<String> = withContext(dispatchers.io) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, "nibras_nova_bookmarks_${System.currentTimeMillis()}.html")
            outFile.writeText(buildNetscapeBookmarkHtml(bookmarks), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            withContext(dispatchers.main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/html"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activityContext.startActivity(
                    Intent.createChooser(shareIntent, "Export Nibras Nova Bookmarks (.html)").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            Result.success(outFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
