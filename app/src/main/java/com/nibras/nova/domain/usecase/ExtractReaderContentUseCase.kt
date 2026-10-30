package com.nibras.nova.domain.usecase

import android.content.Context
import com.nibras.nova.domain.model.ReaderArticle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractReaderContentUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val extractorScript: String by lazy {
        context.assets.open("reader_extractor.js").bufferedReader().use { it.readText() }
    }

    fun parseEvaluateJavascriptResult(rawEvaluationString: String?): ReaderArticle? {
        if (rawEvaluationString.isNullOrBlank() || rawEvaluationString == "null") return null
        return try {
            // evaluateJavascript returns a JSON-escaped string literal
            val unescaped = if (rawEvaluationString.startsWith("\"") && rawEvaluationString.endsWith("\"")) {
                json.decodeFromString<String>(rawEvaluationString)
            } else {
                rawEvaluationString
            }
            json.decodeFromString<ReaderArticle>(unescaped)
        } catch (_: Exception) {
            null
        }
    }
}
