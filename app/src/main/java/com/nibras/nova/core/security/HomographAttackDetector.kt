package com.nibras.nova.core.security

import java.net.IDN
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class HomographAssessment(
    val isSuspicious: Boolean,
    val domain: String,
    val unicodeDisplayDomain: String,
    val warningReason: String?
)

/**
 * Stage 13.2: Homograph / IDN Punycode Phishing Detector.
 * Detects domains that use Punycode ("xn--") or mix Latin and Cyrillic/Greek confusable characters
 * within the same domain label to impersonate trusted brands.
 */
@Singleton
class HomographAttackDetector @Inject constructor() {

    fun inspectUrlForHomographAttack(url: String): HomographAssessment {
        val host = extractHost(url) ?: return HomographAssessment(false, "", "", null)
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host).lowercase(Locale.ROOT)
        val unicodeHost = runCatching { IDN.toUnicode(host) }.getOrDefault(host).lowercase(Locale.ROOT)

        val hasPunycode = asciiHost.contains("xn--")
        val hasMixedScripts = containsMixedLatinAndCyrillicOrGreek(unicodeHost)

        return when {
            hasMixedScripts -> HomographAssessment(
                isSuspicious = true,
                domain = asciiHost,
                unicodeDisplayDomain = unicodeHost,
                warningReason = "Mixed-Script Homograph Alert: This domain combines Latin and Cyrillic/Greek lookalike characters ($unicodeHost / $asciiHost) often used in phishing."
            )
            hasPunycode && asciiHost != unicodeHost -> HomographAssessment(
                isSuspicious = true,
                domain = asciiHost,
                unicodeDisplayDomain = unicodeHost,
                warningReason = "Internationalized Punycode Domain ($asciiHost): Verify the exact spelling of '$unicodeHost' to avoid lookalike phishing sites."
            )
            else -> HomographAssessment(
                isSuspicious = false,
                domain = asciiHost,
                unicodeDisplayDomain = unicodeHost,
                warningReason = null
            )
        }
    }

    fun containsMixedLatinAndCyrillicOrGreek(label: String): Boolean {
        var hasLatin = false
        var hasCyrillicOrGreek = false
        for (ch in label) {
            if (ch == '.' || ch == '-' || ch.isDigit()) continue
            val block = Character.UnicodeBlock.of(ch)
            if (block == Character.UnicodeBlock.BASIC_LATIN ||
                block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT ||
                block == Character.UnicodeBlock.LATIN_EXTENDED_A
            ) {
                hasLatin = true
            } else if (block == Character.UnicodeBlock.CYRILLIC ||
                block == Character.UnicodeBlock.GREEK
            ) {
                hasCyrillicOrGreek = true
            }
        }
        return hasLatin && hasCyrillicOrGreek
    }

    private fun extractHost(url: String): String? {
        val trimmed = url.trim()
        // java.net.URI returns a null host for non-ASCII (IDN) hosts, so fall back to manual parsing.
        val uriHost = runCatching { URI(trimmed).host }.getOrNull()
        if (!uriHost.isNullOrBlank()) return uriHost
        val afterScheme = trimmed.substringAfter("://", trimmed)
        val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = authority.substringAfterLast('@').substringBefore(':')
        return host.ifBlank { null }
    }
}
