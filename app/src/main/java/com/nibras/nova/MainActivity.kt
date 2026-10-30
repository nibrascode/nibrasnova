package com.nibras.nova

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.domain.model.ThemeMode
import com.nibras.nova.domain.repository.SettingsRepository
import com.nibras.nova.domain.repository.TabRepository
import com.nibras.nova.ui.navigation.NovaNavHost
import com.nibras.nova.ui.theme.NovaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

enum class HardwareKeyboardShortcut {
    NEW_TAB,
    NEW_INCOGNITO_TAB,
    CLOSE_CURRENT_TAB,
    RELOAD_PAGE,
    FIND_IN_PAGE,
    FOCUS_ADDRESS_BAR,
    OPEN_OMNI_COMMAND_PALETTE
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var tabRepository: TabRepository

    private val _incomingUrlFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val incomingUrlFlow = _incomingUrlFlow.asSharedFlow()

    private val _keyboardShortcutFlow = MutableSharedFlow<HardwareKeyboardShortcut>(extraBufferCapacity = 1)
    val keyboardShortcutFlow = _keyboardShortcutFlow.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        extractIntentUrlOrProcessText(intent)?.let { url ->
            _incomingUrlFlow.tryEmit(url)
        }

        setContent {
            val settings by settingsRepository.observeSettings().collectAsStateWithLifecycle(initialValue = null)
            val activeTab by tabRepository.observeActiveTab().collectAsStateWithLifecycle(initialValue = null)

            val shouldBlockScreenshots = (activeTab?.isPrivate == true) && (settings?.blockScreenshotsInIncognito != false)
            LaunchedEffect(shouldBlockScreenshots) {
                if (shouldBlockScreenshots) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val darkTheme = when (settings?.themeMode ?: ThemeMode.DARK) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            NovaTheme(
                darkTheme = darkTheme,
                highContrastTurquoise = settings?.highContrastAccent ?: true
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (settings != null) {
                        NovaNavHost(
                            isFirstLaunch = settings!!.isFirstLaunch,
                            incomingUrlFlow = incomingUrlFlow,
                            keyboardShortcutFlow = keyboardShortcutFlow
                        )
                    }
                }
            }
        }
    }

    fun enterFloatingVideoPipMode(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return runCatching {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }.getOrDefault(false)
        }
        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentUrlOrProcessText(intent)?.let { url ->
            _incomingUrlFlow.tryEmit(url)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.isCtrlPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_T -> {
                    _keyboardShortcutFlow.tryEmit(
                        if (event.isShiftPressed) HardwareKeyboardShortcut.NEW_INCOGNITO_TAB
                        else HardwareKeyboardShortcut.NEW_TAB
                    )
                    return true
                }
                KeyEvent.KEYCODE_N -> {
                    if (event.isShiftPressed) {
                        _keyboardShortcutFlow.tryEmit(HardwareKeyboardShortcut.NEW_INCOGNITO_TAB)
                        return true
                    }
                }
                KeyEvent.KEYCODE_W -> {
                    _keyboardShortcutFlow.tryEmit(HardwareKeyboardShortcut.CLOSE_CURRENT_TAB)
                    return true
                }
                KeyEvent.KEYCODE_R -> {
                    _keyboardShortcutFlow.tryEmit(HardwareKeyboardShortcut.RELOAD_PAGE)
                    return true
                }
                KeyEvent.KEYCODE_F -> {
                    _keyboardShortcutFlow.tryEmit(HardwareKeyboardShortcut.FIND_IN_PAGE)
                    return true
                }
                KeyEvent.KEYCODE_L -> {
                    _keyboardShortcutFlow.tryEmit(HardwareKeyboardShortcut.FOCUS_ADDRESS_BAR)
                    return true
                }
                KeyEvent.KEYCODE_K -> {
                    _keyboardShortcutFlow.tryEmit(HardwareKeyboardShortcut.OPEN_OMNI_COMMAND_PALETTE)
                    return true
                }
            }
        }
        if (keyCode == KeyEvent.KEYCODE_F5) {
            _keyboardShortcutFlow.tryEmit(HardwareKeyboardShortcut.RELOAD_PAGE)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun extractIntentUrlOrProcessText(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_WEB_SEARCH -> intent.getStringExtra("query")
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
    }
}
