package com.tapman104.mpvplayer.player.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tapman104.mpvplayer.player.engine.PlayerAction
import com.tapman104.mpvplayer.player.engine.PlayerEngine
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.player.domain.repository.MediaRepository
import com.tapman104.mpvplayer.player.domain.usecase.CycleAspectRatioUseCase
import com.tapman104.mpvplayer.player.domain.usecase.GetResumePositionUseCase
import com.tapman104.mpvplayer.player.domain.usecase.LoadMediaUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SaveResumePositionUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SeekUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SetAudioTrackUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SetSpeedUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SetSubtitleTrackUseCase
import com.tapman104.mpvplayer.player.domain.usecase.TogglePlaybackUseCase
import com.tapman104.mpvplayer.player.gesture.GestureIntent
import com.tapman104.mpvplayer.player.model.QuickActionsPosition
import com.tapman104.mpvplayer.player.model.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin lifecycle bridge between the Android ViewModel lifecycle and [PlayerEngine].
 *
 * Responsibilities:
 *  - Expose [PlayerEngine] state flows to the UI layer.
 *  - Forward [dispatch] calls to [PlayerEngine].
 *  - Receive lifecycle signals from [PlayerActivity] and translate to engine actions.
 *  - Call [PlayerEngine.destroy] when the ViewModel is cleared.
 *
 * No direct engine calls. No business logic. No MpvEventListener.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val engine: PlayerEngine,
    private val seekUseCase: SeekUseCase,
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
    private val loadMediaUseCase: LoadMediaUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    private val setAudioTrackUseCase: SetAudioTrackUseCase,
    private val setSubtitleTrackUseCase: SetSubtitleTrackUseCase,
    private val cycleAspectRatioUseCase: CycleAspectRatioUseCase,
    private val saveResumePositionUseCase: SaveResumePositionUseCase,
    private val getResumePositionUseCase: GetResumePositionUseCase,
    private val mediaRepository: MediaRepository,
) : AndroidViewModel(application), DefaultLifecycleObserver {

    // ── State flows ───────────────────────────────────────────────────────────

    val playerState = engine.state
    val positionState = engine.positionState
    val playlistState = engine.playlistState
    val subtitleAppearance = engine.subtitleAppearance
    val preferredSubtitleLang = engine.preferredSubtitleLang

    // ── Preference flows (read by PlayerActivity) ─────────────────────────────

    val resumePlayback: StateFlow<Boolean> = engine.resumePlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferencesRepository.DEFAULT_RESUME_PLAYBACK)
    val backgroundPlay: StateFlow<String> = engine.backgroundPlay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferencesRepository.DEFAULT_BACKGROUND_PLAY)
    val doubleTapSeekSeconds = engine.doubleTapSeekSeconds
    val swipeToSeek = engine.swipeToSeek
    val brightnessSwipe = engine.brightnessSwipe
    val volumeSwipe = engine.volumeSwipe
    val longPress2x = engine.longPress2x

    val quickActionsPosition: StateFlow<QuickActionsPosition> =
        engine.quickActionsPosition
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickActionsPosition.TOP_RIGHT)

    // ── Engine reference (needed by PlayerActivity for surface wiring) ────────

    val controller get() = engine.controller

    // ── Gesture intents ───────────────────────────────────────────────────────

    private val _gestureIntents = MutableSharedFlow<GestureIntent>(extraBufferCapacity = 64)
    val gestureIntents = _gestureIntents.asSharedFlow()

    fun submitGestureIntent(intent: GestureIntent) {
        _gestureIntents.tryEmit(intent)
    }

    // ── Resume & playback tracking ────────────────────────────────────────────

    private var pendingResumeMs: Long = 0L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                onScreenOff()
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(screenOffReceiver, filter)
        }

        // Save position whenever playback pauses (e.g. from UI pause button or screen lock)
        viewModelScope.launch {
            playerState.map { it.isPlaying }.distinctUntilChanged().collectLatest { isPlaying ->
                if (!isPlaying) {
                    val path = playlistState.value.currentUri
                    if (path != null) {
                        saveCurrentPosition(path, positionState.value.currentPositionMs)
                    }
                }
            }
        }

        // Detect new file loads and load saved resume position once per file
        viewModelScope.launch {
            playlistState.map { it.currentUri }.distinctUntilChanged().collectLatest { uriStr ->
                if (uriStr != null) {
                    val savedMs = getResumePositionUseCase(uriStr)
                    if (savedMs != null && savedMs > 5000L && resumePlayback.value) {
                        pendingResumeMs = savedMs
                    } else {
                        pendingResumeMs = 0L
                    }
                } else {
                    pendingResumeMs = 0L
                }
            }
        }

        // Commit pending resume seek once loading completes
        viewModelScope.launch {
            playerState.map { it.isLoading }.distinctUntilChanged().collectLatest { isLoading ->
                if (!isLoading && pendingResumeMs > 0L) {
                    dispatch(PlayerAction.SeekCommit(pendingResumeMs))
                    pendingResumeMs = 0L
                }
            }
        }

        // Collect and route gesture intents
        viewModelScope.launch {
            _gestureIntents.collect { intent ->
                when (intent) {
                    is GestureIntent.Seek -> seekUseCase(intent.deltaMs)
                    is GestureIntent.SeekCommit -> dispatch(PlayerAction.SeekCommit(intent.positionMs))
                    is GestureIntent.SeekGestureDrag -> dispatch(PlayerAction.SeekGestureDrag(intent.positionMs))
                    is GestureIntent.SetSpeed -> setSpeedUseCase(intent.speed)
                    is GestureIntent.RestoreSpeed -> dispatch(PlayerAction.RestorePlaybackSpeed)
                    is GestureIntent.VolumeChange -> dispatch(PlayerAction.SetVolume(intent.delta.toInt()))
                    is GestureIntent.ZoomChange -> dispatch(PlayerAction.SetZoomAndPan(intent.scale, intent.panX, intent.panY))
                    is GestureIntent.TogglePlay -> togglePlaybackUseCase()
                    is GestureIntent.BrightnessChange -> { /* Handled directly by window brightness update */ }
                }
            }
        }
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    fun dispatch(action: PlayerAction) {
        when (action) {
            is PlayerAction.SeekRelative -> viewModelScope.launch { seekUseCase(action.offsetMs) }
            PlayerAction.TogglePlay -> viewModelScope.launch { togglePlaybackUseCase() }
            is PlayerAction.SetSpeed -> viewModelScope.launch { setSpeedUseCase(action.speed) }
            is PlayerAction.SetAudioTrack -> viewModelScope.launch { setAudioTrackUseCase(action.id) }
            is PlayerAction.SetSubtitleTrack -> viewModelScope.launch { setSubtitleTrackUseCase(action.id) }
            else -> engine.dispatch(action)
        }
    }

    // ── Subtitle appearance delegates (not PlayerAction variants) ─────────────

    fun setSubtitleSize(size: Float) = engine.setSubtitleSize(size)
    fun setSubtitlePosition(position: Float) = engine.setSubtitlePosition(position)
    fun resetSubtitleAppearance() = engine.resetSubtitleAppearance()
    fun setSubtitleFontColor(color: String) = engine.setSubtitleFontColor(color)
    fun setSubtitleBold(bold: Boolean) = engine.setSubtitleBold(bold)
    fun setSubtitleBorderStyle(style: String) = engine.setSubtitleBorderStyle(style)
    fun setSubtitleBorderSize(size: Float) = engine.setSubtitleBorderSize(size)
    fun setSubtitleShadow(shadow: Float) = engine.setSubtitleShadow(shadow)
    fun setSubtitleBackgroundAlpha(alpha: Float) = engine.setSubtitleBackgroundAlpha(alpha)
    fun setSubtitleAppearance(size: Float, position: Float) = engine.setSubtitleAppearance(size, position)
    fun setPreferredSubtitleLanguage(lang: String) = engine.setPreferredSubtitleLanguage(lang)

    // ── Resume position & playback use case delegates ─────────────────────────

    fun saveCurrentPosition(filePath: String, positionMs: Long) {
        viewModelScope.launch {
            saveResumePositionUseCase(filePath, positionMs)
        }
    }

    fun loadResumePosition(filePath: String, onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            val result = getResumePositionUseCase(filePath)
            onResult(result)
        }
    }

    fun clearResumePosition(filePath: String) =
        engine.clearResumePosition(filePath)

    fun cycleAspectRatio() {
        viewModelScope.launch {
            cycleAspectRatioUseCase()
        }
    }

    fun loadMedia(path: String) {
        viewModelScope.launch {
            loadMediaUseCase(path)
        }
    }


    // ── View mode & rotation state ────────────────────────────────────────────

    private val _viewMode = MutableStateFlow(ViewMode.FIT)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private var currentRotation = 0

    fun cycleViewMode() {
        val nextIndex = (_viewMode.value.ordinal + 1) % ViewMode.entries.size
        val nextMode = ViewMode.entries[nextIndex]
        _viewMode.value = nextMode

        controller.executor.execute {
            val panScan = nextMode.mpvPanScan ?: 0f
            MPVLib.setPropertyDouble("video-pan-scan", panScan.toDouble())
            MPVLib.setPropertyDouble("panscan", panScan.toDouble())
            when (val aspect = nextMode.mpvAspect) {
                null -> MPVLib.setPropertyString("video-aspect-override", "no")
                -1f -> MPVLib.setPropertyString("video-aspect-override", "-1")
                else -> MPVLib.setPropertyString("video-aspect-override", aspect.toString())
            }
        }
    }

    fun toggleVideoRotate() {
        currentRotation = if (currentRotation == 0) 90 else 0
        controller.executor.execute {
            MPVLib.setPropertyString("video-rotate", currentRotation.toString())
        }
    }

    // ── Activity-lifecycle delegation ─────────────────────────────────────────

    /**
     * Called by the Activity's BroadcastReceiver when the screen turns off.
     * Pauses playback so audio does not continue on a locked screen.
     */
    fun onScreenOff() {
        dispatch(PlayerAction.PausePlayback)
    }

    /**
     * Called from [PlayerActivity.onPause] to apply the background-play preference.
     *
     * @param backgroundPlayPref Current value of the "background_play" preference string.
     * @param isHeadphonesConnected True when wired or Bluetooth audio output is active.
     */
    fun onActivityPause(backgroundPlayPref: String, isHeadphonesConnected: Boolean) {
        when (backgroundPlayPref) {
            "off" -> dispatch(PlayerAction.PausePlayback)
            "always" -> { /* continue in background — no-op */ }
            "headphones_only" -> {
                if (!isHeadphonesConnected) dispatch(PlayerAction.PausePlayback)
            }
            else -> dispatch(PlayerAction.PausePlayback)
        }
    }

    fun onLifecycleEvent(event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                val app = getApplication<Application>()
                val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                @Suppress("DEPRECATION")
                val headphonesConnected = audioManager?.let { am ->
                    am.isWiredHeadsetOn || am.isBluetoothA2dpOn
                } ?: false
                onActivityPause(backgroundPlay.value, headphonesConnected)

                val path = playlistState.value.currentUri
                if (path != null) {
                    saveCurrentPosition(path, positionState.value.currentPositionMs)
                }
            }
            Lifecycle.Event.ON_RESUME -> { /* No-op */ }
            else -> {}
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        onLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onResume(owner: LifecycleOwner) {
        onLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * Called when a video file-picker result arrives in the Activity.
     * Routes the URI to the engine without the Activity making any playback decisions.
     */
    fun handleFileResult(uri: Uri) {
        dispatch(PlayerAction.LoadAndPlay(uri))
    }

    /**
     * Called when a subtitle-picker result arrives in the Activity.
     */
    fun handleSubtitleResult(uri: Uri) {
        dispatch(PlayerAction.AddSubtitle(uri))
    }

    /**
     * Called when an audio-track picker result arrives in the Activity.
     */
    fun handleAudioTrackResult(uri: Uri) {
        dispatch(PlayerAction.AddAudioTrack(uri))
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        try {
            getApplication<Application>().unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        engine.destroy()
        super.onCleared()
    }
}
