package com.example

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.PlayerViewModel
import com.example.ui.chat.handleChatKey
import com.example.ui.player.GrindhouseMainScreen
import com.example.ui.theme.GrindhouseTheme
import com.example.ui.theme.applyPalette
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    private var isInPipMode by mutableStateOf(false)
    private val isTv: Boolean by lazy {
        packageManager.hasSystemFeature("android.software.leanback")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 1. Keep Screen On (Wake Lock for TV & Mobile playback)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Register VideoPlayerManager as LifecycleObserver for onStart / onStop resource management
        lifecycle.addObserver(viewModel.playerManager)

        // Gespeichertes Farbthema setzen, bevor das erste Bild gezeichnet wird — sonst
        // blitzt beim Start kurz die Vorgabepalette auf.
        applyPalette(viewModel.settings.value.appTheme)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            // Dynamic locale wrapper if user changed language in settings
            val (localizedContext, localizedConfig) = rememberLocalizedConfig(this, settings.languageCode)

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig
            ) {
                GrindhouseTheme {
                    GrindhouseMainScreen(
                        viewModel = viewModel,
                        isInPipMode = isInPipMode,
                        onExitApp = { finish() }
                    )
                }
            }
        }

        // 3. Borderless Immersive Fullscreen Mode
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        try {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } catch (e: Exception) {
            // Gracefully handle if window decor is not yet attached
        }
    }

    /**
     * Amazon Fire TV Remote Control Hardware Mapping:
     * - When Settings or Exit Dialog is open: All D-Pad keys (UP, DOWN, LEFT, RIGHT, CENTER, ENTER)
     *   pass through to Compose for menu navigation and item selection.
     * - In Fullscreen Player mode:
     *   - D-Pad UP: Show "Now Playing" & "Up Next" metadata HUD
     *   - D-Pad DOWN: Toggle native subtitle chat overlay
     *   - D-Pad CENTER (Select) / PLAY / PAUSE: Toggle video playback
     *   - MENU / SETTINGS: Open/close Settings dialog
     *   - BACK: Close open dialogs or prompt exit confirmation
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isMenuOrDialogOpen = viewModel.isSettingsOpen.value || viewModel.showExitDialog.value

            // Bei offenem Detail-Panel gehoeren hoch/runter der Liste, sonst laesst sich in
            // langen Trivia-Texten nicht blaettern. Links und Zurueck schliessen weiterhin hier.
            if (viewModel.isTriviaVisible.value && !isMenuOrDialogOpen) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        return super.dispatchKeyEvent(event)
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BACK -> {
                        viewModel.hideTrivia()
                        return true
                    }
                }
            }

            if (isMenuOrDialogOpen) {
                // When in Settings or Exit Dialog, allow Compose focus navigation
                when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        return viewModel.handleBackPress()
                    }
                    KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                        viewModel.closeSettings()
                        return true
                    }
                }
                // Let Compose handle DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT, DPAD_CENTER, ENTER
                return super.dispatchKeyEvent(event)
            }

            // Normal Fullscreen Player state
            viewModel.onRemoteActivity()

            // Tasten der Full-Ausgabe zuerst anbieten; in der Light-Ausgabe meldet das
            // immer false und aendert nichts.
            if (handleChatKey(viewModel, event.keyCode)) return true

            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.showMetadataOverlay()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    viewModel.toggleChat()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    viewModel.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                    viewModel.toggleSettings()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (isTv) {
                        val handled = viewModel.handleBackPress()
                        if (handled) return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.toggleUpNext()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    viewModel.toggleTrivia()
                    return true
                }
                // Trivia und Filmdetails liegen auf D-Pad links (siehe oben) und auf 'T' fuer
                // Tastaturen. KEYCODE_INFO waere naheliegend gewesen, ist auf Fire TV aber vom
                // System belegt und oeffnet den Alexa-Kopplungsdialog.
                KeyEvent.KEYCODE_T -> {
                    viewModel.toggleTrivia()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Mobile Target (Smartphone):
     * When minimized / Home button pressed, transition into Picture-in-Picture (PiP).
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val isTv = packageManager.hasSystemFeature("android.software.leanback")
                if (!isTv && viewModel.isPlaying.value) {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                }
            } catch (e: Exception) {
                // Ignore if device does not support PiP
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            hideSystemBars()
        }
    }
}

/**
 * Liefert Context UND Configuration fuer die gewaehlte Sprache.
 *
 * Zwei Korrekturen gegenueber der Vorversion: der Aufruf war trotz des Namens nicht gemerkt, lief
 * also samt Locale.setDefault() und createConfigurationContext() bei jeder Recomposition durch;
 * und der Aufrufer stellte nur LocalContext um, waehrend LocalConfiguration die Systemsprache
 * behielt — alles, was seine Sprache daraus zieht, blieb damit auf der falschen Locale.
 */
@Composable
private fun rememberLocalizedConfig(context: Context, languageCode: String): Pair<Context, Configuration> =
    remember(context, languageCode) {
        if (languageCode == "system") {
            context to context.resources.configuration
        } else {
            val locale = Locale.forLanguageTag(languageCode)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config) to config
        }
    }
