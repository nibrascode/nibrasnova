package com.nibras.nova.ui.tools

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationHelper @Inject constructor() {

    suspend fun translateText(
        sourceText: String,
        sourceLanguageCode: String = TranslateLanguage.ENGLISH,
        targetLanguageCode: String = TranslateLanguage.ENGLISH
    ): Result<String> {
        if (sourceText.isBlank()) return Result.success("")
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguageCode) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguageCode) ?: TranslateLanguage.ENGLISH)
            .build()
        val translator = Translation.getClient(options)
        return try {
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            val translated = translator.translate(sourceText).await()
            Result.success(translated)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            translator.close()
        }
    }
}
