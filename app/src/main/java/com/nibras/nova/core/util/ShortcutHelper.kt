package com.nibras.nova.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.nibras.nova.MainActivity
import com.nibras.nova.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createHomeScreenWebsiteShortcut(
        title: String,
        url: String,
        favicon: Bitmap?
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val icon = if (favicon != null) {
            IconCompat.createWithBitmap(favicon)
        } else {
            IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground)
        }
        val shortcutInfo = ShortcutInfoCompat.Builder(context, "nova_pin_${url.hashCode()}")
            .setShortLabel(title.ifBlank { url }.take(20))
            .setLongLabel(title.ifBlank { url }.take(45))
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }
}
