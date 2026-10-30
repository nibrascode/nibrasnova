package com.nibras.nova.core.util

import com.nibras.nova.core.common.DispatcherProvider
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class RssFeedArticleItem(
    val title: String,
    val link: String,
    val summary: String,
    val pubDate: String,
    val feedTitle: String
)

@Singleton
class RssFeedParser @Inject constructor(
    private val dispatchers: DispatcherProvider
) {
    suspend fun fetchAndParseFeed(feedUrl: String, fallbackFeedTitle: String): Result<List<RssFeedArticleItem>> =
        withContext(dispatchers.io) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(feedUrl.trim()).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "NibrasNova-RSS/1.3")
                }
                conn.connect()
                val xmlContent = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(parseXmlString(xmlContent, fallbackFeedTitle))
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                conn?.disconnect()
            }
        }

    fun parseXmlString(xmlContent: String, feedTitle: String): List<RssFeedArticleItem> {
        val items = mutableListOf<RssFeedArticleItem>()
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var insideItem = false
            var currentTitle = ""
            var currentLink = ""
            var currentSummary = ""
            var currentPubDate = ""
            var currentTag = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.lowercase()
                        if (currentTag == "item" || currentTag == "entry") {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentSummary = ""
                            currentPubDate = ""
                        } else if (insideItem && currentTag == "link") {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) {
                                currentLink = href
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideItem) {
                            val text = parser.text?.trim().orEmpty()
                            if (text.isNotEmpty()) {
                                when (currentTag) {
                                    "title" -> currentTitle += text
                                    "link" -> if (currentLink.isBlank()) currentLink += text
                                    "description", "summary", "content" -> {
                                        if (currentSummary.length < 240) {
                                            currentSummary += text.replace("<[^>]*>".toRegex(), " ").trim()
                                        }
                                    }
                                    "pubdate", "published", "updated" -> currentPubDate = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name.lowercase()
                        if ((tag == "item" || tag == "entry") && insideItem) {
                            if (currentTitle.isNotBlank() && currentLink.isNotBlank()) {
                                items.add(
                                    RssFeedArticleItem(
                                        title = currentTitle.trim(),
                                        link = currentLink.trim(),
                                        summary = currentSummary.take(200).trim(),
                                        pubDate = currentPubDate.take(25),
                                        feedTitle = feedTitle
                                    )
                                )
                            }
                            insideItem = false
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
            items.take(30)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
