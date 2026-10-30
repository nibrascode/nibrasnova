package com.nibras.nova.core.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.nibras.nova.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage 13.3: Clipboard Privacy Guard & Auto-Scrubber.
 * Sanitizes tracking parameters from copied URLs and automatically clears sensitive clipboard contents
 * after a configurable timeout (default 60 seconds).
 */
@Singleton
class ClipboardPrivacyGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingParamStripper: TrackingParamStripper,
    private val dispatchers: DispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private var autoClearJob: Job? = null

    fun copyCleanUrlToClipboard(rawUrl: String, autoClearAfterSeconds: Long = 60L): String {
        val cleanUrl = trackingParamStripper.stripTrackingParameters(rawUrl).sanitizedUrl
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return cleanUrl
        cm.setPrimaryClip(ClipData.newPlainText("Nibras Nova Clean URL", cleanUrl))

        if (autoClearAfterSeconds > 0L) {
            scheduleClipboardAutoScrub(autoClearAfterSeconds)
        }
        return cleanUrl
    }

    fun scrubClipboardImmediately(): Boolean {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.clearPrimaryClip()
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun scheduleClipboardAutoScrub(seconds: Long) {
        autoClearJob?.cancel()
        autoClearJob = scope.launch {
            delay(seconds * 1_000L)
            scrubClipboardImmediately()
        }
    }
}
