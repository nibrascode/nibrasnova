package com.nibras.nova.domain.usecase

import com.nibras.nova.core.security.InputClassification
import com.nibras.nova.core.security.TrackingParamStripper
import com.nibras.nova.core.security.UrlValidator
import com.nibras.nova.domain.model.SearchEngineConfig
import javax.inject.Inject

sealed interface ResolvedNavigationTarget {
    data class LoadWebUrl(val url: String, val strippedTrackingParamsCount: Int = 0) : ResolvedNavigationTarget
    data object OpenNovaHome : ResolvedNavigationTarget
    data class BlockedUnsafeInput(val rawInput: String, val reason: String) : ResolvedNavigationTarget
}

class ResolveInputToUrlUseCase @Inject constructor(
    private val urlValidator: UrlValidator,
    private val trackingParamStripper: TrackingParamStripper = TrackingParamStripper()
) {
    operator fun invoke(
        rawInput: String,
        searchConfig: SearchEngineConfig,
        httpsOnlyMode: Boolean,
        stripTrackingParams: Boolean = true
    ): ResolvedNavigationTarget {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ResolvedNavigationTarget.OpenNovaHome
        }

        return when (urlValidator.classifyInput(trimmed)) {
            InputClassification.INTERNAL_NOVA_SCHEME -> {
                if (trimmed.equals("nova://home", ignoreCase = true)) {
                    ResolvedNavigationTarget.OpenNovaHome
                } else {
                    ResolvedNavigationTarget.LoadWebUrl("about:blank")
                }
            }
            InputClassification.DANGEROUS_BLOCKED_SCHEME -> {
                ResolvedNavigationTarget.BlockedUnsafeInput(
                    rawInput = trimmed,
                    reason = "Direct execution of javascript:, file:, content:, or data: URI schemes in the address bar is prohibited."
                )
            }
            InputClassification.VALID_HTTPS_URL -> {
                val sanitized = if (stripTrackingParams) trackingParamStripper.stripTrackingParameters(trimmed)
                else com.nibras.nova.core.security.SanitizedUrlResult(trimmed, 0)
                ResolvedNavigationTarget.LoadWebUrl(sanitized.sanitizedUrl, sanitized.removedParamsCount)
            }
            InputClassification.VALID_HTTP_URL -> {
                val target = urlValidator.upgradeToHttpsIfEnabled(trimmed, httpsOnlyMode)
                val sanitized = if (stripTrackingParams) trackingParamStripper.stripTrackingParameters(target)
                else com.nibras.nova.core.security.SanitizedUrlResult(target, 0)
                ResolvedNavigationTarget.LoadWebUrl(sanitized.sanitizedUrl, sanitized.removedParamsCount)
            }
            InputClassification.NAKED_DOMAIN_OR_HOST -> {
                val isLocalLoopback = trimmed.startsWith("localhost", ignoreCase = true) ||
                    trimmed.startsWith("127.0.0.1")
                val scheme = if (isLocalLoopback && !httpsOnlyMode) "http://" else "https://"
                val full = "$scheme$trimmed"
                val sanitized = if (stripTrackingParams) trackingParamStripper.stripTrackingParameters(full)
                else com.nibras.nova.core.security.SanitizedUrlResult(full, 0)
                ResolvedNavigationTarget.LoadWebUrl(sanitized.sanitizedUrl, sanitized.removedParamsCount)
            }
            InputClassification.EXTERNAL_APP_SCHEME -> {
                ResolvedNavigationTarget.LoadWebUrl(trimmed)
            }
            InputClassification.SEARCH_QUERY -> {
                val searchUrl = searchConfig.buildSearchUrl(trimmed)
                ResolvedNavigationTarget.LoadWebUrl(searchUrl)
            }
        }
    }
}
