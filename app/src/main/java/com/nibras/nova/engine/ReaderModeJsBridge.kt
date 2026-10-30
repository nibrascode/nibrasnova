package com.nibras.nova.engine

import android.webkit.WebView
import com.nibras.nova.domain.model.ReaderArticle
import com.nibras.nova.domain.usecase.ExtractReaderContentUseCase
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ReaderModeJsBridge @Inject constructor(
    private val extractReaderContentUseCase: ExtractReaderContentUseCase
) {
    /**
     * Evaluates readability extraction strictly via one-way WebView.evaluateJavascript()
     * without registering any @JavascriptInterface on the WebView.
     */
    suspend fun evaluateCurrentPageReadability(webView: WebView): ReaderArticle? =
        suspendCancellableCoroutine { continuation ->
            try {
                webView.evaluateJavascript(extractReaderContentUseCase.extractorScript) { rawJson ->
                    val parsed = extractReaderContentUseCase.parseEvaluateJavascriptResult(rawJson)
                    if (continuation.isActive) {
                        continuation.resume(parsed)
                    }
                }
            } catch (_: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
}
