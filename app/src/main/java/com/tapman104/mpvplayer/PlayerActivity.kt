package com.tapman104.mpvplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tapman104.mpvplayer.player.playback.PlayerScreen
import com.tapman104.mpvplayer.settings.SettingsScreen
import com.tapman104.mpvplayer.ui.theme.MpvPlayerTheme
import com.tapman104.mpvplayer.util.UriResolver
import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModel
import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModelFactory
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.player.coordinator.PlayerCoordinator
import com.tapman104.mpvplayer.settings.SettingsViewModel
import com.tapman104.mpvplayer.settings.SettingsViewModelFactory


class PlayerActivity : ComponentActivity() {

    private val mpvController by lazy { MpvController(applicationContext) }
    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory.create(application, mpvController)
    }

    private lateinit var coordinator: PlayerCoordinator

    private lateinit var surfaceView: SurfaceView

    /**
     * Pauses playback when the screen turns off (power button).
     * Using a BroadcastReceiver rather than onPause so that dialogs,
     * notifications, and picture-in-picture transitions do NOT pause playback.
     */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.pausePlayback()
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.loadAndPlay(it)
        }
    }

    private val subtitlePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addSubtitle(it)
        }
    }

    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addAudioTrack(it)
        }
    }

    /** The URI string of the currently loaded file — used as the resume key. */
    private var currentFilePath: String? = null
    private var currentBackgroundPlayPref: String = UserPreferencesRepository.DEFAULT_BACKGROUND_PLAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Immersive sticky — hide system bars, show transiently on swipe
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        // Register screen-off receiver to pause audio on lock
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // Create the SurfaceView that mpv will render into
        surfaceView = SurfaceView(this)

        // Register MpvSurface as the holder callback BEFORE setContent
        surfaceView.holder.addCallback(viewModel.controller.surface)

        val updateWindowBrightness: (Float) -> Unit = { newBrightness ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = newBrightness.coerceIn(0f, 1f)
            window.attributes = layoutParams
        }
        coordinator = PlayerCoordinator(viewModel, onBrightnessChange = updateWindowBrightness)

        setContent {
            MpvPlayerTheme {
                val playerState by viewModel.playerState.collectAsStateWithLifecycle()
                val positionState by viewModel.positionState.collectAsStateWithLifecycle()
                val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()

                var resumePlaybackEnabled by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    viewModel.resumePlayback.collect { resumePlaybackEnabled = it }
                }
                val backgroundPlayPref by viewModel.backgroundPlay.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_BACKGROUND_PLAY
                )
                currentBackgroundPlayPref = backgroundPlayPref

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
                val gestureSensitivity by viewModel.gestureSensitivity.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_GESTURE_SENSITIVITY
                )

                var pendingResumeMs by remember { mutableStateOf(0L) }
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

                // Dynamically manage screen wake lock and save position on pause
                val isPlaying = playerState.isPlaying
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        val path = currentFilePath ?: return@LaunchedEffect
                        val posMs = positionState.currentPositionMs
                        viewModel.saveCurrentPosition(path, posMs)
                    }
                }

                // Detect new file loads via playlist change, load resume position once per file.
                LaunchedEffect(playlistState.currentUri) {
                    val uriStr = playlistState.currentUri ?: return@LaunchedEffect
                    currentFilePath = uriStr
                    viewModel.loadResumePosition(uriStr) { savedMs ->
                        if (savedMs != null && savedMs > 5000L && resumePlaybackEnabled) {
                            pendingResumeMs = savedMs
                        } else {
                            pendingResumeMs = 0L
                        }
                    }
                }

                LaunchedEffect(playerState.isLoading, pendingResumeMs) {
                    if (!playerState.isLoading && pendingResumeMs > 0L) {
                        viewModel.seekTo(pendingResumeMs, true)
                        pendingResumeMs = 0L
                    }
                }

                PlayerScreen(
                    coordinator = coordinator,
                    onCoordinatorReady = { overlayImpl ->
                        coordinator.attachOverlay(overlayImpl)
                    },
                    fileName = playlistState.currentUri
                        ?.let { UriResolver.getDisplayName(applicationContext, Uri.parse(it)) }
                        ?: "Unknown",
                    playerState = playerState,
                    positionState = positionState,
                    surfaceView = surfaceView,
                    onTogglePlay = { viewModel.togglePlay() },
                    initialBrightness = initialBrightness,
                    onBrightnessChange = updateWindowBrightness,
                    onOpenFile = { filePickerLauncher.launch(arrayOf("video/*")) },
                    onBack = { finish() },
                    onOpenSettings = { showSettings = true },
                    onAudioTrackSelected = { viewModel.setAudioTrack(it) },
                    onAddAudioClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    onSubtitleTrackSelected = { viewModel.setSubtitleTrack(it) },
                    onDisableSubtitles = { viewModel.setSubtitleTrack(-1) },
                    onAddSubtitleClick = {
                        subtitlePickerLauncher.launch(
                            arrayOf("application/x-subrip", "text/x-ass", "text/vtt", "application/octet-stream", "*/*")
                        )
                    },
                    onCycleDecodeMode = { newMode -> viewModel.cycleDecodeMode(newMode, resumeAfter = true) },
                    onPause = viewModel::pausePlayback,
                    onPlay = viewModel::play,
                    onSubtitleSizeChange = { viewModel.setSubtitleSize(it) },
                    onSubtitlePositionChange = { viewModel.setSubtitlePosition(it) },
                    onSubtitleAppearanceReset = { viewModel.resetSubtitleAppearance() },
                    doubleTapSeekSeconds = doubleTapSeekSeconds,
                    swipeToSeek = swipeToSeek,
                    brightnessSwipe = brightnessSwipe,
                    volumeSwipe = volumeSwipe,
                    longPress2x = longPress2x,
                    gestureSensitivity = gestureSensitivity,
                )

                if (showSettings) {
                    val settingsViewModel: SettingsViewModel by viewModels {
                        SettingsViewModelFactory(UserPreferencesRepository(application))
                    }
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { showSettings = false }
                    )
                }
            }
        }

        // Handle direct launch from a file manager or intent
        intent.data?.let { viewModel.loadAndPlay(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { viewModel.loadAndPlay(it) }
    }

    override fun onPause() {
        super.onPause()
        when (currentBackgroundPlayPref) {
            "off" -> {
                viewModel.pausePlayback()
            }
            "always" -> {
                // Do nothing — playback continues when minimized/backgrounded
            }
            "headphones_only" -> {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                val headphonesConnected = audioManager?.let { am ->
                    @Suppress("DEPRECATION")
                    am.isWiredHeadsetOn || am.isBluetoothA2dpOn
                } ?: false
                if (!headphonesConnected) {
                    viewModel.pausePlayback()
                }
            }
            else -> {
                viewModel.pausePlayback()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOffReceiver)
    }
}
