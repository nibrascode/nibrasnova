package com.nibras.nova.core.security

import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class InputClassification {
    VALID_HTTP_URL,
    VALID_HTTPS_URL,
    NAKED_DOMAIN_OR_HOST,
    INTERNAL_NOVA_SCHEME,
    DANGEROUS_BLOCKED_SCHEME,
    EXTERNAL_APP_SCHEME,
    SEARCH_QUERY
}

@Singleton
class UrlValidator @Inject constructor() {

    private val blockedSchemes = setOf(
        "javascript",
        "vbscript",
        "file",
        "content",
        "data",
        "jar",
        "filesystem"
    )

    private val externalAppSchemes = setOf(
        "intent",
        "mailto",
        "tel",
        "sms",
        "geo",
        "market",
        "whatsapp",
        "tg"
    )

    private val domainRegex = Regex(
        pattern = "^([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,63}(:\d{1,5})?(/.*)?$"
    )

    private val ipv4Regex = Regex(
        pattern = "^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(:\d{1,5})?(/.*)?$"
    )

    private val localhostRegex = Regex(
        pattern = "^localhost(:\d{1,5})?(/.*)?$",
        option = RegexOption.IGNORE_CASE
    )

    fun classifyInput(rawInput: String): InputClassification {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return InputClassification.SEARCH_QUERY

        if (trimmed.equals("nova://home", ignoreCase = true) ||
            trimmed.equals("about:blank", ignoreCase = true)
        ) {
            return InputClassification.INTERNAL_NOVA_SCHEME
        }

        // Any whitespace inside input means it is a search query unless it starts with an explicit scheme and has no spaces in host
        val schemeSeparatorIdx = trimmed.indexOf(":")
        if (schemeSeparatorIdx > 0) {
            val scheme = trimmed.substring(0, schemeSeparatorIdx).lowercase(Locale.ROOT)
            if (scheme in blockedSchemes) {
                return InputClassification.DANGEROUS_BLOCKED_SCHEME
            }
            if (scheme in externalAppSchemes) {
                return InputClassification.EXTERNAL_APP_SCHEME
            }
            if (scheme == "https" && !trimmed.contains(' ')) {
                return if (isValidUriHost(trimmed)) InputClassification.VALID_HTTPS_URL
                else InputClassification.SEARCH_QUERY
            }
            if (scheme == "http" && !trimmed.contains(' ')) {
                return if (isValidUriHost(trimmed)) InputClassification.VALID_HTTP_URL
                else InputClassification.SEARCH_QUERY
            }
        }

        if (trimmed.contains(' ')) {
            return InputClassification.SEARCH_QUERY
        }

        if (domainRegex.matches(trimmed) || ipv4Regex.matches(trimmed) || localhostRegex.matches(trimmed)) {
            return InputClassification.NAKED_DOMAIN_OR_HOST
        }

        return InputClassification.SEARCH_QUERY
    }

    fun isSafeWebNavigationUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.equals("about:blank", ignoreCase = true) ||
            trimmed.equals("nova://home", ignoreCase = true)
        ) {
            return true
        }
        val lower = trimmed.lowercase(Locale.ROOT)
        if (blockedSchemes.any { lower.startsWith("$it:") }) {
            return false
        }
        return lower.startsWith("https://") || lower.startsWith("http://")
    }

    fun upgradeToHttpsIfEnabled(url: String, httpsOnlyMode: Boolean): String {
        val trimmed = url.trim()
        if (!httpsOnlyMode) return trimmed
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            val hostPart = trimmed.removePrefix("http://").removePrefix("HTTP://")
            // Do not force HTTPS on localhost or RFC1918 local loopback during local development unless requested
            if (hostPart.startsWith("localhost") || hostPart.startsWith("127.0.0.1")) {
                return trimmed
            }
            return "https://$hostPart"
        }
        return trimmed
    }

    private fun isValidUriHost(candidate: String): Boolean {
        return try {
            val uri = URI(candidate)
            !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
