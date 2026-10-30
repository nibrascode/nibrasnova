package com.nibras.nova.core.security

import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SanitizedUrlResult(
    val sanitizedUrl: String,
    val removedParamsCount: Int
)

@Singleton
class TrackingParamStripper @Inject constructor() {

    private val exactTrackingParams = setOf(
        "fbclid",
        "gclid",
        "gclsrc",
        "dclid",
        "msclkid",
        "twclid",
        "ttclid",
        "yclid",
        "mc_cid",
        "mc_eid",
        "igshid",
        "si",
        "_ga",
        "_gl",
        "vero_id",
        "oly_enc_id",
        "oly_anon_id",
        "rb_clickid",
        "s_kwcid"
    )

    private val prefixTrackingParams = listOf(
        "utm_",
        "hsa_",
        "mkt_tok"
    )

    fun stripTrackingParameters(rawUrl: String): SanitizedUrlResult {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return SanitizedUrlResult(trimmed, 0)
        }

        return try {
            val uri = URI(trimmed)
            val rawQuery = uri.rawQuery
            if (rawQuery.isNullOrBlank()) {
                return SanitizedUrlResult(trimmed, 0)
            }

            val pairs = rawQuery.split("&")
            var removedCount = 0
            val keptPairs = mutableListOf<String>()

            for (pair in pairs) {
                val eqIdx = pair.indexOf('=')
                val key = (if (eqIdx >= 0) pair.substring(0, eqIdx) else pair).lowercase(Locale.ROOT)
                val isTracker = key in exactTrackingParams ||
                    prefixTrackingParams.any { key.startsWith(it) }
                if (isTracker) {
                    removedCount++
                } else {
                    keptPairs.add(pair)
                }
            }

            if (removedCount == 0) {
                return SanitizedUrlResult(trimmed, 0)
            }

            val rebuiltQuery = if (keptPairs.isEmpty()) null else keptPairs.joinToString("&")
            val rebuiltUri = URI(
                uri.scheme,
                uri.rawAuthority,
                uri.rawPath,
                rebuiltQuery,
                uri.rawFragment
            )
            SanitizedUrlResult(rebuiltUri.toString(), removedCount)
        } catch (_: Exception) {
            SanitizedUrlResult(trimmed, 0)
        }
    }
}
