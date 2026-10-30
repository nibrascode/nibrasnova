package com.nibras.nova.engine

import android.content.Context
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalIntentHandler @Inject constructor() {

    fun canResolveExternalIntent(context: Context, intent: Intent): Boolean {
        return try {
            val pm = context.packageManager
            intent.resolveActivity(pm) != null
        } catch (_: Exception) {
            false
        }
    }

    fun launchSanitizedExternalIntent(context: Context, intent: Intent): Boolean {
        return try {
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
