package com.nibras.nova.domain.model

import java.net.URLEncoder

enum class SearchEngineType(
    val id: String,
    val displayName: String,
    val hostLabel: String,
    val queryTemplateUrl: String,
    val description: String
) {
    GOOGLE(
        id = "google",
        displayName = "Google",
        hostLabel = "google.com",
        queryTemplateUrl = "https://www.google.com/search?q=%s",
        description = "Comprehensive global web search."
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        hostLabel = "duckduckgo.com",
        queryTemplateUrl = "https://duckduckgo.com/?q=%s",
        description = "Privacy-first search that does not track you."
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave Search",
        hostLabel = "search.brave.com",
        queryTemplateUrl = "https://search.brave.com/search?q=%s",
        description = "Independent index with strong privacy defaults."
    ),
    STARTPAGE(
        id = "startpage",
        displayName = "Startpage",
        hostLabel = "startpage.com",
        queryTemplateUrl = "https://www.startpage.com/do/dsearch?query=%s",
        description = "Google search results delivered with anonymized privacy."
    ),
    BING(
        id = "bing",
        displayName = "Microsoft Bing",
        hostLabel = "bing.com",
        queryTemplateUrl = "https://www.bing.com/search?q=%s",
        description = "Visual and AI-assisted search index by Microsoft."
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom Search Engine",
        hostLabel = "custom",
        queryTemplateUrl = "https://duckduckgo.com/?q=%s",
        description = "User-configured URL template containing %s placeholder."
    );

    companion object {
        fun fromId(id: String): SearchEngineType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DUCKDUCKGO
        }
    }
}

data class SearchEngineConfig(
    val selectedEngine: SearchEngineType = SearchEngineType.DUCKDUCKGO,
    val customSearchTemplate: String = "https://duckduckgo.com/?q=%s",
    val enableLocalSuggestions: Boolean = true
) {
    fun buildSearchUrl(rawQuery: String): String {
        val encodedQuery = try {
            URLEncoder.encode(rawQuery.trim(), Charsets.UTF_8.name())
        } catch (_: Exception) {
            rawQuery.trim().replace(" ", "+")
        }
        val template = if (selectedEngine == SearchEngineType.CUSTOM && customSearchTemplate.contains("%s")) {
            customSearchTemplate.trim()
        } else {
            selectedEngine.queryTemplateUrl
        }
        return template.replace("%s", encodedQuery)
    }
}
