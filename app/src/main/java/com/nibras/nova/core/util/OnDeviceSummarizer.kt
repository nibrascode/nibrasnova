package com.nibras.nova.core.util

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractiveSummaryResult(
    val bulletPoints: List<String>,
    val topKeywords: List<String>
)

/**
 * Stage 12.2: 100% Local On-Device Extractive Article Summarizer & Keyword Extractor.
 * Uses sentence term-frequency scoring and positional weighting with zero external API calls.
 */
@Singleton
class OnDeviceSummarizer @Inject constructor() {

    private val stopWords = setOf(
        "the", "and", "for", "that", "this", "with", "from", "your", "have", "are", "was", "were",
        "will", "would", "could", "should", "about", "into", "their", "there", "what", "which",
        "when", "where", "who", "whom", "whose", "why", "how", "all", "any", "both", "each",
        "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own",
        "same", "so", "than", "too", "very", "can", "just", "don", "now", "also", "been", "has", "had"
    )

    fun summarizeArticle(plainText: String, maxBullets: Int = 4): ExtractiveSummaryResult {
        val cleaned = plainText.replace("\\s+".toRegex(), " ").trim()
        if (cleaned.length < 120) {
            return ExtractiveSummaryResult(
                bulletPoints = if (cleaned.isNotBlank()) listOf(cleaned) else emptyList(),
                topKeywords = emptyList()
            )
        }

        // Tokenize words & compute term frequencies
        val words = cleaned.lowercase(Locale.ROOT)
            .split("[^a-zA-Z0-9əöüöğçşıƏÖÜÖĞÇŞİ]+".toRegex())
            .filter { it.length >= 4 && it !in stopWords }

        val wordFreq = mutableMapOf<String, Int>()
        for (w in words) {
            wordFreq[w] = (wordFreq[w] ?: 0) + 1
        }

        val topKeywords = wordFreq.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key }

        // Split into candidate sentences
        val sentences = cleaned.split("(?<=[.!?])\\s+".toRegex())
            .map { it.trim() }
            .filter { it.length in 45..320 }

        if (sentences.size <= maxBullets) {
            return ExtractiveSummaryResult(
                bulletPoints = sentences,
                topKeywords = topKeywords
            )
        }

        // Score each sentence by sum of top word frequencies normalized by sentence length + lead position bonus
        val scored = sentences.mapIndexed { index, sentence ->
            val tokens = sentence.lowercase(Locale.ROOT)
                .split("[^a-zA-Z0-9əöüöğçşıƏÖÜÖĞÇŞİ]+".toRegex())
                .filter { it.length >= 4 && it !in stopWords }
            val tfScore = tokens.sumOf { wordFreq[it] ?: 0 }.toDouble() / (tokens.size.coerceAtLeast(5))
            val positionBonus = if (index < 3) 1.25 else 1.0
            Triple(index, sentence, tfScore * positionBonus)
        }

        val selectedBullets = scored
            .sortedByDescending { it.third }
            .take(maxBullets)
            .sortedBy { it.first }
            .map { it.second }

        return ExtractiveSummaryResult(
            bulletPoints = selectedBullets,
            topKeywords = topKeywords
        )
    }
}
