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
import com.tapman104.mpvplayer.player.model.PlayerError
import com.tapman104.mpvplayer.player.model.SubtitleTrack
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PlaylistState
import com.tapman104.mpvplayer.player.state.PositionState
import com.tapman104.mpvplayer.player.state.SubtitleAppearanceState
import com.tapman104.mpvplayer.player.viewmodel.PlaybackCoordinator
import com.tapman104.mpvplayer.player.viewmodel.PlaylistManager
import com.tapman104.mpvplayer.player.viewmodel.ResumePositionManager
import com.tapman104.mpvplayer.player.viewmodel.SubtitleController
import com.tapman104.mpvplayer.player.viewmodel.TrackCoordinator
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PlayerEngine — top-level orchestrator that wires coordinators, managers, and the
 * preference-watching coroutines together, then exposes a single [dispatch] entry point.
 *
 * Playback commands are now delegated to [PlaybackCoordinator] (play/pause/seek/volume/
 * speed/aspect/zoom) and [TrackCoordinator] (audio tracks, subtitle tracks, decode mode).
 *
 * Pipeline:
 *   UI → dispatch(PlayerAction) → PlayerEngine → Coordinator → MpvController → MPV
 *                                                                    ↓
 *                                                            EventProcessor → StateFlow → Compose
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
    val quickActionsPosition = preferencesRepository.quickActionsPosition

    // ── Coordinators ──────────────────────────────────────────────────────────

    val playbackCoordinator = PlaybackCoordinator(
        application = application,
        controller = controller,
        eventProcessor = eventProcessor,
        sharedPlayerState = _playerState,
    )

    val trackCoordinator = TrackCoordinator(
        application = application,
        controller = controller,
        sharedPlayerState = _playerState,
        preferencesRepository = preferencesRepository,
        scope = scope,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // init
    // ─────────────────────────────────────────────────────────────────────────

    init {
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
                    _playerState.update { it.copy(error = PlayerError.EngineError(result.message), hasError = true, isLoading = false) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // dispatch — public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Routes a [PlayerAction] to the appropriate coordinator or manager.
     * This is the single entry point for all UI-driven playback commands.
     */
    fun dispatch(action: PlayerAction) {
        when (action) {
            // Playback — delegated to PlaybackCoordinator
            PlayerAction.Play                          -> playbackCoordinator.play()
            PlayerAction.Pause                         -> playbackCoordinator.pause()
            PlayerAction.TogglePlay                    -> playbackCoordinator.togglePlay()
            PlayerAction.PausePlayback                 -> playbackCoordinator.pausePlayback()

            // Seeking — delegated to PlaybackCoordinator
            is PlayerAction.SeekRelative               -> playbackCoordinator.seekRelative(action.offsetMs)
            is PlayerAction.SeekGestureDrag            -> playbackCoordinator.seekGestureDrag(action.positionMs)
            is PlayerAction.SeekCommit                 -> playbackCoordinator.seekCommit(action.positionMs)

            // Volume / Speed — delegated to PlaybackCoordinator
            is PlayerAction.SetVolume                  -> playbackCoordinator.setVolume(action.volume)
            is PlayerAction.SetSpeed                   -> playbackCoordinator.setSpeed(action.speed)
            is PlayerAction.SetPlaybackSpeedRamped     -> playbackCoordinator.setPlaybackSpeedRamped(action.targetSpeed)
            PlayerAction.RestorePlaybackSpeed          -> playbackCoordinator.restorePlaybackSpeed()

            // Video geometry — delegated to PlaybackCoordinator
            is PlayerAction.SetAspectRatio             -> playbackCoordinator.setAspectRatio(action.mode)
            is PlayerAction.SetZoomAndPan              -> playbackCoordinator.setZoomAndPan(action.zoomLog2, action.panX, action.panY)

            // Audio tracks — delegated to TrackCoordinator
            is PlayerAction.SetAudioTrack              -> trackCoordinator.setAudioTrack(action.id)
            is PlayerAction.AddAudioTrack              -> trackCoordinator.addAudioTrack(action.uri)

            // Subtitle tracks — delegated to TrackCoordinator
            is PlayerAction.SetSubtitleTrack           -> trackCoordinator.setSubtitleTrack(action.id)
            is PlayerAction.AddSubtitle                -> trackCoordinator.addSubtitle(action.uri)

            // Decode mode — delegated to TrackCoordinator
            is PlayerAction.SetDecodeMode              -> trackCoordinator.cycleDecodeMode(action.mode, resumeAfter = true)

            // Playlist — delegated to PlaylistManager
            is PlayerAction.LoadAndPlay                -> loadAndPlay(action.uri)
            is PlayerAction.SetPlaylist                -> playlistManager.setPlaylist(action.uris)
            is PlayerAction.AddToPlaylist              -> playlistManager.addToPlaylist(action.uri)
            PlayerAction.PlayNext                      -> playlistManager.playNext()
            PlayerAction.PlayPrevious                  -> playlistManager.playPrevious()
            is PlayerAction.PlayAt                     -> playlistManager.playAt(action.index)
            PlayerAction.ClearError                    -> _playerState.update { it.copy(error = null, hasError = false) }
            PlayerAction.ToggleLock                    -> _playerState.update { it.copy(isLocked = !it.isLocked) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal handlers (playlist / surface — not covered by coordinators)
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadAndPlay(uri: Uri) {
        _playerState.update { it.copy(isLoading = true, error = null, hasError = false) }
        playlistManager.loadAndPlay(uri)
    }

    private fun onSurfaceReady() {
        playlistManager.onSurfaceReady()
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
