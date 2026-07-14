package com.tapman104.mpvplayer

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.player.engine.PlayerAction
import com.tapman104.mpvplayer.player.gesture.GestureIntent
import com.tapman104.mpvplayer.player.input.KeyEventHandler
import com.tapman104.mpvplayer.player.playback.PlayerScreen
import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModel
import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModelFactory
import com.tapman104.mpvplayer.settings.SettingsScreen
import com.tapman104.mpvplayer.settings.SettingsViewModel
import com.tapman104.mpvplayer.settings.SettingsViewModelFactory
import com.tapman104.mpvplayer.ui.theme.MpvPlayerTheme
import com.tapman104.mpvplayer.util.UriResolver
import com.tapman104.mpvplayer.player.model.QuickActionsPosition
import android.provider.Settings

/**
 * Pure window host for the player UI.
 *
 * Responsibilities (only):
 *  - Set window flags and system-UI visibility.
 *  - Create the SurfaceView and wire it to the engine surface holder.
 *  - Host [setContent] with [PlayerScreen] and the settings overlay.
 *  - Read Intent extras and forward URIs to the ViewModel.
 *  - Delegate screen-off, lifecycle pause, and key events to ViewModel / [KeyEventHandler].
 *
 * No playback decisions. No engine calls. No AudioManager logic beyond reading headphone state.
 */
class PlayerActivity : ComponentActivity() {

    private val mpvController by lazy { MpvController(applicationContext) }
    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory.create(application, mpvController)
    }

    private lateinit var surfaceView: SurfaceView

    // ── File pickers ─────────────────────────────────────────────────────────

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.handleFileResult(it) } }

    private val subtitlePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.handleSubtitleResult(it) } }

    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.handleAudioTrackResult(it) } }

    // ── Key-event routing ─────────────────────────────────────────────────────

    private val keyEventHandler = KeyEventHandler { volumePct ->
        viewModel.dispatch(PlayerAction.SetVolume(volumePct))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Immersive sticky — hide system bars, show transiently on swipe
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        // Register ViewModel as lifecycle observer
        lifecycle.addObserver(viewModel)

        // Create the SurfaceView that mpv will render into
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(viewModel.controller.surface)

        val updateWindowBrightness: (Float) -> Unit = { newBrightness ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = newBrightness.coerceIn(0f, 1f)
            window.attributes = layoutParams
        }

        setContent {
            MpvPlayerTheme {
                val playerState by viewModel.playerState.collectAsStateWithLifecycle()
                val positionState by viewModel.positionState.collectAsStateWithLifecycle()
                val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()

                val doubleTapSeekSeconds by viewModel.doubleTapSeekSeconds.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_DOUBLE_TAP_SEEK_SECONDS
                )
                val swipeToSeek by viewModel.swipeToSeek.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_SWIPE_TO_SEEK
                )
                val brightnessSwipe by viewModel.brightnessSwipe.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_BRIGHTNESS_SWIPE
                )
                val volumeSwipe by viewModel.volumeSwipe.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_VOLUME_SWIPE
                )
                val longPress2x by viewModel.longPress2x.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_LONG_PRESS_2X
                )
                val quickActionsPosition by viewModel.quickActionsPosition.collectAsStateWithLifecycle(
                    initialValue = QuickActionsPosition.TOP_RIGHT
                )
                val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()

                var showSettings by remember { mutableStateOf(false) }

                val initialBrightness = remember {
                    val currentWindowBrightness = window.attributes.screenBrightness
                    if (currentWindowBrightness < 0f) {
                        val sysBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                        (sysBrightness / 255f).coerceIn(0f, 1f)
                    } else {
                        currentWindowBrightness
                    }
                }

                // Dynamically manage screen wake lock
                val isPlaying = playerState.isPlaying
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                PlayerScreen(
                    fileName = playlistState.currentUri
                        ?.let { UriResolver.getDisplayName(applicationContext, Uri.parse(it)) }
                        ?: "Unknown",
                    playerState = playerState,
                    positionState = positionState,
                    surfaceView = surfaceView,
                    onTogglePlay = { viewModel.dispatch(PlayerAction.TogglePlay) },
                    initialBrightness = initialBrightness,
                    onBrightnessChange = updateWindowBrightness,
                    onGestureIntent = { intent ->
                        if (intent is GestureIntent.BrightnessChange) {
                            updateWindowBrightness(intent.delta)
                        } else {
                            viewModel.submitGestureIntent(intent)
                        }
                    },
                    onOpenFile = { filePickerLauncher.launch(arrayOf("video/*")) },
                    onBack = { finish() },
                    onOpenSettings = {
                        showSettings = true
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    },
                    onAudioTrackSelected = { viewModel.dispatch(PlayerAction.SetAudioTrack(it)) },
                    onAddAudioClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    onSubtitleTrackSelected = { viewModel.dispatch(PlayerAction.SetSubtitleTrack(it)) },
                    onDisableSubtitles = { viewModel.dispatch(PlayerAction.SetSubtitleTrack(-1)) },
                    onAddSubtitleClick = {
                        subtitlePickerLauncher.launch(
                            arrayOf("application/x-subrip", "text/x-ass", "text/vtt", "application/octet-stream", "*/*")
                        )
                    },
                    onCycleDecodeMode = { newMode -> viewModel.dispatch(PlayerAction.SetDecodeMode(newMode)) },
                    onPause = { viewModel.dispatch(PlayerAction.PausePlayback) },
                    onPlay = { viewModel.dispatch(PlayerAction.Play) },
                    onSubtitleSizeChange = { viewModel.setSubtitleSize(it) },
                    onSubtitlePositionChange = { viewModel.setSubtitlePosition(it) },
                    onSubtitleAppearanceReset = { viewModel.resetSubtitleAppearance() },
                    doubleTapSeekSeconds = doubleTapSeekSeconds,
                    swipeToSeek = swipeToSeek,
                    brightnessSwipe = brightnessSwipe,
                    volumeSwipe = volumeSwipe,
                    longPress2x = longPress2x,
                    quickActionsPosition = quickActionsPosition,
                    currentViewMode = viewMode,
                    onCycleViewMode = { viewModel.cycleViewMode() },
                    onRotate = { viewModel.toggleVideoRotate() },
                    onEnterPip = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            runCatching {
                                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                            }
                        }
                    },
                )

                if (showSettings) {
                    val settingsViewModel: SettingsViewModel by viewModels {
                        SettingsViewModelFactory(UserPreferencesRepository(application))
                    }
                    BackHandler {
                        showSettings = false
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = {
                            showSettings = false
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    )
                }
            }
        }

        // Handle direct launch from a file manager or intent
        intent.data?.let { viewModel.handleFileResult(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { viewModel.handleFileResult(it) }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return super.onKeyDown(keyCode, event)
        val superResult = super.onKeyDown(keyCode, event)
        return keyEventHandler.handleKeyDown(keyCode, superResult, audioManager).let {
            if (it) it else superResult
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return super.onKeyUp(keyCode, event)
        val superResult = super.onKeyUp(keyCode, event)
        return keyEventHandler.handleKeyUp(keyCode, superResult, audioManager).let {
            if (it) it else superResult
        }
    }
}
