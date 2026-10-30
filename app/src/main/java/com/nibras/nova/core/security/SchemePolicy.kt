package com.nibras.nova.core.security

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import android.content.Intent
import android.net.Uri
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NavigationDisposition {
    data class AllowInWebView(val finalUrl: String) : NavigationDisposition
    data class UpgradedToHttps(val upgradedUrl: String) : NavigationDisposition
    data class PromptExternalApp(val sanitizedIntent: Intent, val targetUri: String, val fallbackUrl: String?) : NavigationDisposition
    data class BlockUnsafeScheme(val blockedUrl: String, val reason: String) : NavigationDisposition
}

@Singleton
class SchemePolicy @Inject constructor(
    private val urlValidator: UrlValidator
) {
    fun evaluateNavigation(
        url: String,
        httpsOnlyMode: Boolean
    ): NavigationDisposition {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return NavigationDisposition.BlockUnsafeScheme(url, L.s(R.string.t_empty_url))
        }

        val lower = trimmed.lowercase(Locale.ROOT)
        when {
            lower == "about:blank" || lower == "nova://home" -> {
                return NavigationDisposition.AllowInWebView(trimmed)
            }
            lower.startsWith("javascript:") ||
            lower.startsWith("vbscript:") ||
            lower.startsWith("file:") ||
            lower.startsWith("content:") ||
            lower.startsWith("data:") -> {
                return NavigationDisposition.BlockUnsafeScheme(
                    blockedUrl = trimmed,
                    reason = L.s(R.string.t_blocked_dangerous_uri_scheme_for_securit)
                )
            }
            lower.startsWith("http://") -> {
                return if (httpsOnlyMode && !lower.startsWith("http://localhost") && !lower.startsWith("http://127.0.0.1")) {
                    NavigationDisposition.UpgradedToHttps(
                        urlValidator.upgradeToHttpsIfEnabled(trimmed, true)
                    )
                } else {
                    NavigationDisposition.AllowInWebView(trimmed)
                }
            }
            lower.startsWith("https://") -> {
                return NavigationDisposition.AllowInWebView(trimmed)
            }
            lower.startsWith("intent:") -> {
                return parseAndSanitizeExternalIntent(trimmed)
            }
            else -> {
                // Handle mailto:, tel:, geo:, market:, etc.
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        component = null
                        selector = null
                    }
                    NavigationDisposition.PromptExternalApp(
                        sanitizedIntent = intent,
                        targetUri = trimmed,
                        fallbackUrl = null
                    )
                } catch (e: Exception) {
                    NavigationDisposition.BlockUnsafeScheme(trimmed, L.s(R.string.t_malformed_external_scheme))
                }
            }
        }
    }

    fun parseAndSanitizeExternalIntent(intentUriString: String): NavigationDisposition {
        return try {
            val parsedIntent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME)
            val fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url")
                ?.takeIf { urlValidator.isSafeWebNavigationUrl(it) }

            // Security hardening: prevent Intent Redirection attacks into internal app components
            parsedIntent.addCategory(Intent.CATEGORY_BROWSABLE)
            parsedIntent.component = null
            parsedIntent.selector = null
            parsedIntent.flags = parsedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION.inv()
            parsedIntent.flags = parsedIntent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION.inv()

            NavigationDisposition.PromptExternalApp(
                sanitizedIntent = parsedIntent,
                targetUri = intentUriString,
                fallbackUrl = fallbackUrl
            )
        } catch (e: Exception) {
            NavigationDisposition.BlockUnsafeScheme(intentUriString, L.s(R.string.t_invalid_intent_syntax))
        }
    }
}
