package com.tapman104.mpvplayer.player.engine

import android.app.Application
import android.net.Uri
import android.util.Log
import com.tapman104.mpvplayer.core.engine.EventProcessor
import com.tapman104.mpvplayer.core.engine.InitResult
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.player.model.AspectRatioMode
import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.model.SubtitleTrack
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PlaylistState
import com.tapman104.mpvplayer.player.state.PositionState
import com.tapman104.mpvplayer.player.state.SubtitleAppearanceState
import com.tapman104.mpvplayer.player.viewmodel.PlaylistManager
import com.tapman104.mpvplayer.player.viewmodel.ResumePositionManager
import com.tapman104.mpvplayer.player.viewmodel.SubtitleController
import com.tapman104.mpvplayer.util.UriResolver
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * PlayerEngine — the single orchestrator for all playback business logic.
 *
 * Owns [PlayerState] and [PositionState], wires the preference-watching coroutines,
 * registers the [EventProcessor] with the dispatcher, and exposes a single
 * [dispatch] entry point for every [PlayerAction].
 *
 * Pipeline:
 *   UI → dispatch(PlayerAction) → PlayerEngine → MpvController → MPV
 *                                                      ↓
 *                                              EventProcessor → StateFlow → Compose
 */
class PlayerEngine(
    private val application: Application,
    val controller: MpvController,
    val eventProcessor: EventProcessor,
    val playlistManager: PlaylistManager,
    val subtitleController: SubtitleController,
    private val resumePositionManager: ResumePositionManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val scope: CoroutineScope,
    // Shared mutable state stores — created by the factory so EventProcessor
    // and PlayerEngine write to the exact same MutableStateFlow instances.
    sharedPlayerState: MutableStateFlow<PlayerState> = MutableStateFlow(PlayerState()),
    sharedPositionState: MutableStateFlow<PositionState> = MutableStateFlow(PositionState()),
) {
    private val TAG = "PlayerEngine"

    // ── Private mutable state (assigned from constructor params) ─────────────

    private val _playerState = sharedPlayerState
    private val _positionState = sharedPositionState

    // ── Public read-only state ────────────────────────────────────────────────

    val state: StateFlow<PlayerState> = _playerState.asStateFlow()
    val positionState: StateFlow<PositionState> = _positionState.asStateFlow()

    // ── Derived/sub-controller state flows ────────────────────────────────────

    val playlistState: StateFlow<PlaylistState> = playlistManager.playlistState
    val subtitleAppearance: StateFlow<SubtitleAppearanceState> = subtitleController.subtitleAppearance
    val preferredSubtitleLang: StateFlow<String> = subtitleController.preferredSubtitleLang

    // ── Preference flows forwarded to ViewModel / Activity ───────────────────

    val resumePlayback = preferencesRepository.resumePlayback
    val backgroundPlay = preferencesRepository.backgroundPlay
    val doubleTapSeekSeconds = preferencesRepository.doubleTapSeekSeconds
    val swipeToSeek = preferencesRepository.swipeToSeek
    val brightnessSwipe = preferencesRepository.brightnessSwipe
    val volumeSwipe = preferencesRepository.volumeSwipe
    val longPress2x = preferencesRepository.longPress2x
    val gestureSensitivity = preferencesRepository.gestureSensitivity

    // ── Speed override tracking ───────────────────────────────────────────────

    private var preOverrideSpeed: Float = 1f
    private var isSpeedOverridden: Boolean = false

    // ── Last seek timestamp (for coalesced relative seeks) ───────────────────

    private var lastSeekTime = 0L

    // ─────────────────────────────────────────────────────────────────────────
    // init
    // ─────────────────────────────────────────────────────────────────────────

    init {
        // Wire the EventProcessor to the MutableStateFlows owned by this engine.
        // EventProcessor is constructed externally with these same references.
        resumePositionManager.attach(scope) { _positionState.value.durationMs }

        controller.dispatcher.addListener(eventProcessor)
        controller.init()

        controller.surface.setSurfaceReadyCallback {
            onSurfaceReady()
        }

        scope.launch {
            when (val result = controller.initResult.first()) {
                is InitResult.Success -> {
                    scope.launch {
                        preferencesRepository.decodeMode.collect { modeStr ->
                            val mode = when (modeStr) {
                                DecodeMode.HWPlus.mpvValue -> DecodeMode.HWPlus
                                DecodeMode.SW.mpvValue -> DecodeMode.SW
                                else -> DecodeMode.HW
                            }
                            if (_playerState.value.decodeMode != mode) {
                                controller.executor.setHwdec(mode.mpvValue)
                                _playerState.update { it.copy(decodeMode = mode) }
                            }
                        }
                    }
                    scope.launch {
                        preferencesRepository.debandFilter.collect { deband ->
                            controller.executor.execute { MPVLib.setPropertyBoolean("deband", deband) }
                        }
                    }
                    scope.launch {
                        preferencesRepository.videoScale.collect { scale ->
                            controller.executor.execute { MPVLib.setPropertyString("scale", scale) }
                        }
                    }
                    scope.launch {
                        preferencesRepository.volumeBoost.collect { boost ->
                            controller.executor.execute {
                                MPVLib.setPropertyInt("volume-max", boost)
                                if (boost > 100) {
                                    MPVLib.setPropertyInt("volume", boost)
                                }
                            }
                        }
                    }
                    scope.launch {
                        preferencesRepository.pitchCorrection.collect { enabled ->
                            controller.executor.execute { MPVLib.setPropertyBoolean("audio-pitch-correction", enabled) }
                        }
                    }
                    scope.launch {
                        preferencesRepository.audioOutputDriver.collect { ao ->
                            controller.executor.execute { MPVLib.setPropertyString("ao", ao) }
                        }
                    }
                }
                is InitResult.Failure ->
                    _playerState.update { it.copy(error = result.message, hasError = true, isLoading = false) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // dispatch — public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Routes a [PlayerAction] to the correct internal handler.
     * This is the single entry point for all UI-driven playback commands.
     */
    fun dispatch(action: PlayerAction) {
        when (action) {
            // Playback
            PlayerAction.Play -> controller.executor.play()
            PlayerAction.Pause -> controller.executor.pause()
            PlayerAction.TogglePlay -> controller.executor.togglePlay()
            PlayerAction.PausePlayback -> pausePlayback()

            // Seeking
            is PlayerAction.SeekRelative -> seekRelative(action.offsetMs)
            is PlayerAction.SeekGestureDrag -> seekGestureDrag(action.positionMs)
            is PlayerAction.SeekCommit -> seekCommit(action.positionMs)

            // Volume / Speed
            is PlayerAction.SetVolume -> setVolume(action.volume)
            is PlayerAction.SetSpeed -> setSpeed(action.speed)
            is PlayerAction.SetPlaybackSpeedRamped -> setPlaybackSpeedRamped(action.targetSpeed)
            PlayerAction.RestorePlaybackSpeed -> restorePlaybackSpeed()

            // Audio tracks
            is PlayerAction.SetAudioTrack -> setAudioTrack(action.id)
            is PlayerAction.AddAudioTrack -> addAudioTrack(action.uri)

            // Subtitle tracks
            is PlayerAction.SetSubtitleTrack -> setSubtitleTrack(action.id)
            is PlayerAction.AddSubtitle -> addSubtitle(action.uri)

            // Video settings
            is PlayerAction.SetDecodeMode -> cycleDecodeMode(action.mode, resumeAfter = true)
            is PlayerAction.SetAspectRatio -> setAspectRatio(action.mode)
            is PlayerAction.SetZoomAndPan -> setZoomAndPan(action.zoomLog2, action.panX, action.panY)

            // Playlist
            is PlayerAction.LoadAndPlay -> loadAndPlay(action.uri)
            is PlayerAction.SetPlaylist -> playlistManager.setPlaylist(action.uris)
            is PlayerAction.AddToPlaylist -> playlistManager.addToPlaylist(action.uri)
            PlayerAction.PlayNext -> playlistManager.playNext()
            PlayerAction.PlayPrevious -> playlistManager.playPrevious()
            is PlayerAction.PlayAt -> playlistManager.playAt(action.index)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal handlers
    // ─────────────────────────────────────────────────────────────────────────

    private fun seekRelative(offsetMs: Long) {
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seekRelativeCoalesced(offsetMs / 1000.0)
    }

    private fun seekGestureDrag(positionMs: Long) {
        eventProcessor.isSliderSeeking = true
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seekGesture(positionMs / 1000.0)
    }

    private fun seekCommit(positionMs: Long) {
        eventProcessor.isSliderSeeking = false
        lastSeekTime = 0L
        eventProcessor.lastTimePosUpdate = 0L
        controller.executor.seekCommit(positionMs / 1000.0)
    }

    private fun setVolume(volume: Int) {
        val volInt = volume.coerceIn(0, 130)
        controller.executor.setVolume(volInt)
        _playerState.update { it.copy(volume = volInt) }
    }

    private fun setSpeed(speed: Float) = controller.executor.setSpeed(speed.toDouble())

    private fun setPlaybackSpeedRamped(targetSpeed: Float) {
        if (!isSpeedOverridden) {
            preOverrideSpeed = _playerState.value.speed
            isSpeedOverridden = true
        }
        setSpeed(targetSpeed)
    }

    private fun restorePlaybackSpeed() {
        if (isSpeedOverridden) {
            setSpeed(preOverrideSpeed)
            isSpeedOverridden = false
        }
    }

    private fun setAudioTrack(id: Int) {
        if (_playerState.value.currentAudioTrackId == id) return
        controller.executor.setAudioTrack(id)
        _playerState.update { it.copy(currentAudioTrackId = id) }
    }

    private fun addAudioTrack(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            controller.executor.addAudioTrack(path)
        }
    }

    private fun setSubtitleTrack(id: Int) {
        if (_playerState.value.currentSubtitleTrackId == id) return
        controller.executor.setSubtitleTrack(id)
        _playerState.update { it.copy(currentSubtitleTrackId = id) }
    }

    private fun addSubtitle(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            controller.executor.addSubtitle(path)
        }
    }

    private fun cycleDecodeMode(next: DecodeMode, resumeAfter: Boolean = false) {
        val mpvMode = when (next) {
            DecodeMode.HW     -> "mediacodec"
            DecodeMode.HWPlus -> "mediacodec-copy"
            DecodeMode.SW     -> "no"
        }
        controller.executor.setHwdec(mpvMode)
        if (resumeAfter) controller.executor.play()
        scope.launch {
            preferencesRepository.setDecodeMode(mpvMode)
        }
    }

    private fun setAspectRatio(mode: AspectRatioMode) {
        if (_playerState.value.aspectRatioMode == mode) return
        _playerState.update { it.copy(aspectRatioMode = mode) }
        when (mode) {
            AspectRatioMode.DEFAULT,
            AspectRatioMode.FIT -> controller.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.CROP -> controller.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 1.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.STRETCH -> controller.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "none")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
        }
    }

    private fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float) {
        controller.executor.setVideoZoom(zoomLog2)
        controller.executor.setVideoPan(panX, panY)
        _playerState.update { it.copy(videoZoom = zoomLog2, videoPanX = panX, videoPanY = panY) }
    }

    private fun loadAndPlay(uri: Uri) {
        _playerState.update { it.copy(isLoading = true, error = null, hasError = false) }
        playlistManager.loadAndPlay(uri)
    }

    private fun onSurfaceReady() {
        playlistManager.onSurfaceReady()
    }

    /** Pauses playback unconditionally — idempotent if already paused. */
    private fun pausePlayback() {
        if (_playerState.value.isPaused) return
        controller.executor.pause()
        _playerState.update { it.copy(isPaused = true) }
    }

    private suspend fun resolveTrackPath(uri: Uri): String? {
        if (uri.scheme != "content") {
            return uri.path ?: uri.toString()
        }
        val fd = application.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val realPath = Utils.findRealPath(fd.fd)
        fd.close()

        return realPath ?: run {
            // Fallback: copy to cache
            val ext = UriResolver.getDisplayName(application, uri).substringAfterLast('.', "tmp")
            val cache = File(application.cacheDir, "ext_track_${System.currentTimeMillis()}.$ext")
            val copied = application.contentResolver.openInputStream(uri)?.use { input ->
                cache.outputStream().use { output ->
                    input.copyTo(output)
                }
            } != null
            if (!copied) return null
            cache.absolutePath
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-controller delegates (subtitle appearance — not PlayerAction variants)
    // ─────────────────────────────────────────────────────────────────────────

    fun autoSelectSubtitle(tracks: List<SubtitleTrack>) = subtitleController.autoSelectSubtitle(tracks)
    fun setPreferredSubtitleLanguage(lang: String) = subtitleController.setPreferredSubtitleLanguage(lang)
    fun setSubtitleSize(size: Float) = subtitleController.setSubtitleSize(size)
    fun setSubtitlePosition(position: Float) = subtitleController.setSubtitlePosition(position)
    fun setSubtitleFontColor(color: String) = subtitleController.setSubtitleFontColor(color)
    fun setSubtitleBold(bold: Boolean) = subtitleController.setSubtitleBold(bold)
    fun setSubtitleBorderStyle(style: String) = subtitleController.setSubtitleBorderStyle(style)
    fun setSubtitleBorderSize(size: Float) = subtitleController.setSubtitleBorderSize(size)
    fun setSubtitleShadow(shadow: Float) = subtitleController.setSubtitleShadow(shadow)
    fun setSubtitleBackgroundAlpha(alpha: Float) = subtitleController.setSubtitleBackgroundAlpha(alpha)
    fun setSubtitleAppearance(size: Float, position: Float) = subtitleController.setSubtitleAppearance(size, position)
    fun resetSubtitleAppearance() = subtitleController.resetSubtitleAppearance()

    // ── Resume position delegates ─────────────────────────────────────────────

    fun saveCurrentPosition(filePath: String, positionMs: Long) =
        resumePositionManager.saveCurrentPosition(filePath, positionMs)

    fun loadResumePosition(filePath: String, onResult: (Long?) -> Unit) =
        resumePositionManager.loadResumePosition(filePath, onResult)

    fun clearResumePosition(filePath: String) =
        resumePositionManager.clearResumePosition(filePath)

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    fun destroy() {
        Log.d(TAG, "PlayerEngine.destroy()")
        controller.dispatcher.removeListener(eventProcessor)
        controller.destroy()
    }
}
