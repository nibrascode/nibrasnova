package com.nibras.nova.domain.model

import com.nibras.nova.R
import com.nibras.nova.core.common.L

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
        description = L.s(R.string.t_comprehensive_global_web_search)
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        hostLabel = "duckduckgo.com",
        queryTemplateUrl = "https://duckduckgo.com/?q=%s",
        description = L.s(R.string.t_privacy_first_search_that_does_not_track)
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave Search",
        hostLabel = "search.brave.com",
        queryTemplateUrl = "https://search.brave.com/search?q=%s",
        description = L.s(R.string.t_independent_index_with_strong_privacy_de)
    ),
    STARTPAGE(
        id = "startpage",
        displayName = "Startpage",
        hostLabel = "startpage.com",
        queryTemplateUrl = "https://www.startpage.com/do/dsearch?query=%s",
        description = L.s(R.string.t_google_search_results_delivered_with_ano)
    ),
    BING(
        id = "bing",
        displayName = "Microsoft Bing",
        hostLabel = "bing.com",
        queryTemplateUrl = "https://www.bing.com/search?q=%s",
        description = L.s(R.string.t_visual_and_ai_assisted_search_index_by_m)
    ),
    CUSTOM(
        id = "custom",
        displayName = L.s(R.string.t_custom_search_engine),
        hostLabel = "custom",
        queryTemplateUrl = "https://duckduckgo.com/?q=%s",
        description = L.s(R.string.t_user_configured_url_template_containing)
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
