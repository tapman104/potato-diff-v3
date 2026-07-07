package com.tapman104.mpvplayer.player.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.math.roundToInt
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.engine.MpvEventListener
import com.tapman104.mpvplayer.core.engine.InitResult
import com.tapman104.mpvplayer.core.engine.TrackListParser
import com.tapman104.mpvplayer.util.UriResolver
import com.tapman104.mpvplayer.player.model.*
import com.tapman104.mpvplayer.player.state.*
import com.tapman104.mpvplayer.player.gesture.MpvPlayerController
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.Utils

class PlayerViewModel(
    private val application: Application,
    val mpvController: MpvController,
    private val resumePositionManager: ResumePositionManager,
    val preferencesRepository: UserPreferencesRepository,
) : AndroidViewModel(application), MpvEventListener, MpvPlayerController {

    private val TAG = "PlayerViewModel"

    private var lastTimePosUpdate = 0L
    private var lastSeekTime = 0L

    val controller: MpvController get() = mpvController
    val userPreferencesRepository: UserPreferencesRepository get() = preferencesRepository

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val playlistManager = PlaylistManager(
        context = application,
        onLoadFile = { path -> mpvController.executor.loadFile(path) },
        hasSurface = { mpvController.surface.hasSurface() }
    )

    private val subtitleController = SubtitleController(
        executor = mpvController.executor,
        preferencesRepository = preferencesRepository,
        scope = viewModelScope
    )

    val playlistState: StateFlow<PlaylistState> = playlistManager.playlistState
    val subtitleAppearance: StateFlow<SubtitleAppearanceState> = subtitleController.subtitleAppearance
    val preferredSubtitleLang: StateFlow<String> = subtitleController.preferredSubtitleLang

    val subtitleSize = preferencesRepository.subtitleSize
    val subtitlePosition = preferencesRepository.subtitlePosition
    val resumePlayback = preferencesRepository.resumePlayback
    val decodeModePreference = preferencesRepository.decodeMode

    init {
        resumePositionManager.attach(viewModelScope) { _playerState.value.durationMs }
        mpvController.dispatcher.addListener(this)
        mpvController.init()
        mpvController.surface.setSurfaceReadyCallback {
            onSurfaceReady()
        }
        viewModelScope.launch {
            when (val result = mpvController.initResult.first()) {
                is InitResult.Success -> {
                    preferencesRepository.decodeMode.collect { modeStr ->
                        val mode = when (modeStr) {
                            DecodeMode.HWPlus.mpvValue -> DecodeMode.HWPlus
                            DecodeMode.SW.mpvValue -> DecodeMode.SW
                            else -> DecodeMode.HW
                        }
                        if (_playerState.value.decodeMode != mode) {
                            mpvController.executor.setHwdec(mode.mpvValue)
                            _playerState.update { it.copy(decodeMode = mode) }
                        }
                    }
                }
                is InitResult.Failure -> _playerState.update { it.copy(error = result.message, hasError = true, isLoading = false) }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Playback controls
    // ---------------------------------------------------------------------------

    fun play() = mpvController.executor.play()
    fun togglePlay() = mpvController.executor.togglePlay()
    
    override fun seekTo(positionMs: Long, precise: Boolean) {
        lastSeekTime = System.currentTimeMillis()
        if (precise) {
            mpvController.executor.seekCommit(positionMs / 1000.0)
        } else {
            mpvController.executor.seekGesture(positionMs / 1000.0)
        }
    }
    
    fun seekTo(positionMs: Long) = seekTo(positionMs, true)
    
    fun onSeekCommitMs(positionMs: Long) {
        lastSeekTime = 0L
        val seconds = positionMs / 1000.0
        mpvController.executor.seekCommit(seconds)
    }
    
    fun seekRelative(offsetMs: Long) {
        lastSeekTime = System.currentTimeMillis()
        mpvController.executor.seekRelativeCoalesced(offsetMs / 1000.0)
    }
    
    fun setSpeed(speed: Float) = mpvController.executor.setSpeed(speed.toDouble())
    fun setVolume(volume: Int) = mpvController.executor.setVolume(volume)
    fun setAudioTrack(id: Int) {
        mpvController.executor.setAudioTrack(id)
        _playerState.update { it.copy(currentAudioTrackId = id) }
    }
    fun addAudioTrack(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            mpvController.executor.addAudioTrack(path)
        }
    }
    fun setSubtitleTrack(id: Int) {
        mpvController.executor.setSubtitleTrack(id)
        _playerState.update { it.copy(currentSubtitleTrackId = id) }
    }
    fun addSubtitle(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            mpvController.executor.addSubtitle(path)
        }
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

    fun setAspectRatio(mode: AspectRatioMode) {
        _playerState.update { it.copy(aspectRatioMode = mode) }
        when (mode) {
            AspectRatioMode.DEFAULT -> mpvController.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.FIT -> mpvController.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.CROP -> mpvController.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 1.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.STRETCH -> mpvController.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "none")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Load / URI
    // ---------------------------------------------------------------------------

    fun loadAndPlay(uri: Uri) {
        _playerState.update { it.copy(isLoading = true, error = null, hasError = false) }
        playlistManager.loadAndPlay(uri)
    }

    fun onSurfaceReady() {
        playlistManager.onSurfaceReady()
    }

    /** Pauses playback immediately — used by screen-off receiver. */
    fun pausePlayback() {
        mpvController.executor.pause()
        _playerState.update { it.copy(isPaused = true) }
    }

    // ---------------------------------------------------------------------------
    // Playlist
    // ---------------------------------------------------------------------------

    fun setPlaylist(uris: List<Uri>) = playlistManager.setPlaylist(uris)
    fun addToPlaylist(uri: Uri) = playlistManager.addToPlaylist(uri)
    fun playNext() = playlistManager.playNext()
    fun playPrevious() = playlistManager.playPrevious()
    fun playAt(index: Int) = playlistManager.playAt(index)

    // ---------------------------------------------------------------------------
    // Decode mode
    // ---------------------------------------------------------------------------

    fun setDecodeMode(mode: DecodeMode) {
        cycleDecodeMode(mode)
    }

    fun cycleDecodeMode(next: DecodeMode, resumeAfter: Boolean = false) {
        val mpvMode = when (next) {
            DecodeMode.HW     -> "mediacodec"
            DecodeMode.HWPlus -> "mediacodec-copy"
            DecodeMode.SW     -> "no"
        }
        viewModelScope.launch {
            delay(150)
            mpvController.executor.setHwdec(mpvMode)
            if (resumeAfter) mpvController.executor.play()
        }
    }

    // ---------------------------------------------------------------------------
    // Zoom / Pan
    // ---------------------------------------------------------------------------

    fun setVideoZoom(zoom: Float) {
        mpvController.executor.setVideoZoom(zoom)
        _playerState.update { it.copy(videoZoom = zoom) }
    }

    fun setVideoPan(panX: Float, panY: Float) {
        mpvController.executor.setVideoPan(panX, panY)
        _playerState.update { it.copy(videoPanX = panX, videoPanY = panY) }
    }

    // ---------------------------------------------------------------------------
    // Subtitle appearance
    // ---------------------------------------------------------------------------

    fun setSubtitleFontColor(color: String) = subtitleController.setSubtitleFontColor(color)
    fun setSubtitleBold(bold: Boolean) = subtitleController.setSubtitleBold(bold)
    fun setSubtitleBorderStyle(style: String) = subtitleController.setSubtitleBorderStyle(style)
    fun setSubtitleBorderSize(size: Float) = subtitleController.setSubtitleBorderSize(size)
    fun setSubtitleShadow(shadow: Float) = subtitleController.setSubtitleShadow(shadow)
    fun setSubtitleBackgroundAlpha(alpha: Float) = subtitleController.setSubtitleBackgroundAlpha(alpha)
    fun setSubtitleAppearance(size: Float, position: Float) = subtitleController.setSubtitleAppearance(size, position)
    fun resetSubtitleAppearance() = subtitleController.resetSubtitleAppearance()

    // ---------------------------------------------------------------------------
    // Resume position
    // ---------------------------------------------------------------------------

    fun saveCurrentPosition(filePath: String, positionMs: Long) = resumePositionManager.saveCurrentPosition(filePath, positionMs)
    fun loadResumePosition(filePath: String, onResult: (Long?) -> Unit) = resumePositionManager.loadResumePosition(filePath, onResult)
    fun clearResumePosition(filePath: String) = resumePositionManager.clearResumePosition(filePath)

    // ---------------------------------------------------------------------------
    // Auto-subtitle selection
    // ---------------------------------------------------------------------------

    fun autoSelectSubtitle(tracks: List<SubtitleTrack>) = subtitleController.autoSelectSubtitle(tracks)
    fun setPreferredSubtitleLanguage(lang: String) = subtitleController.setPreferredSubtitleLanguage(lang)
    fun setSubtitleSize(size: Float) = subtitleController.setSubtitleSize(size)
    fun setSubtitlePosition(position: Float) = subtitleController.setSubtitlePosition(position)

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setResumePlayback(enabled)
        }
    }

    fun setDecodeModeStringPreference(mpvValue: String) {
        viewModelScope.launch {
            preferencesRepository.setDecodeMode(mpvValue)
        }
    }

    // ---------------------------------------------------------------------------
    // MpvPlayerController implementation
    // ---------------------------------------------------------------------------

    override val durationMs: Long get() = _playerState.value.durationMs
    override val currentPositionMs: Long get() = _playerState.value.currentPositionMs
    override val isPaused: Boolean get() = _playerState.value.isPaused
    override val currentZoomLog2: Float get() = _playerState.value.videoZoom
    override val currentPanX: Float get() = _playerState.value.videoPanX
    override val currentPanY: Float get() = _playerState.value.videoPanY
    override val volume: Float get() = _playerState.value.volume.toFloat()
    override val maxStandardVolume: Float get() = 100f
    override val maxBoostVolume: Float get() = 130f
    override val brightness: Float get() = getScreenBrightness()
    override val screenWidthPx: Float get() = application.resources.displayMetrics.widthPixels.toFloat()
    override val screenHeightPx: Float get() = application.resources.displayMetrics.heightPixels.toFloat()
    override val isVolumeSideRight: Boolean get() = true
    override val doubleTapSeekAreaWidthPercent: Int get() = 30
    override val isDynamicSpeedOverlayEnabled: Boolean get() = true
    override val playbackSpeed: Float get() = _playerState.value.speed

    private fun getScreenBrightness(): Float {
        return try {
            val sysBrightness = Settings.System.getInt(application.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            (sysBrightness / 255f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0.5f
        }
    }

    override fun pause() {
        mpvController.executor.pause()
    }

    override fun unpause() {
        mpvController.executor.play()
    }

    override fun seekForward(offsetMs: Long) {
        seekRelative(offsetMs)
    }

    override fun seekBackward(offsetMs: Long) {
        seekRelative(-offsetMs)
    }

    override fun seekGesture(positionMs: Long) {
        lastSeekTime = System.currentTimeMillis()
        mpvController.executor.seekGesture(positionMs / 1000.0)
    }

    override fun seekCommit(positionMs: Long) {
        lastSeekTime = 0L
        mpvController.executor.seekCommit(positionMs / 1000.0)
    }

    override fun setPlaybackSpeedRamped(targetSpeed: Float, stepCount: Int, stepDurationMs: Long) {
        mpvController.executor.setSpeed(targetSpeed.toDouble())
    }

    override fun restorePlaybackSpeed() {
        // Handled by PlayerActivity override restoration
    }

    override fun setVolume(volume: Float) {
        val volInt = volume.roundToInt().coerceIn(0, 130)
        mpvController.executor.setVolume(volInt)
        _playerState.update { it.copy(volume = volInt) }
    }

    override fun setBrightness(brightness: Float) {
        // MPV does not control system brightness directly
    }

    override fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float) {
        mpvController.executor.setVideoZoom(zoomLog2)
        mpvController.executor.setVideoPan(panX, panY)
        _playerState.update { it.copy(videoZoom = zoomLog2, videoPanX = panX, videoPanY = panY) }
    }

    override fun showDoubleTapSeekOverlay(seekAmountSec: Int, isForward: Boolean, label: String) {}
    override fun hideDoubleTapSeekOverlay() {}
    override fun showHorizontalSeekOverlay(currentTimeLabel: String, deltaLabel: String, targetPositionMs: Long) {}
    override fun hideHorizontalSeekOverlay(delayMs: Long) {}
    override fun showSpeedOverlay(speed: Float, interactiveSliderIndex: Int?) {}
    override fun hideSpeedOverlay() {}
    override fun showVolumeOverlay(percentage: Int) {}
    override fun hideVolumeOverlay() {}
    override fun showBrightnessOverlay(percentage: Int) {}
    override fun hideBrightnessOverlay() {}
    override fun showPinchZoomOverlay(zoomPercentage: Int) {}
    override fun hidePinchZoomOverlay() {}
    override fun showTapFeedback(x: Float, y: Float) {}

    override fun scheduleTimer(delayMs: Long, action: () -> Unit): Any = Any()
    override fun cancelTimer(timerId: Any?) {}
    override fun triggerSingleTapAction() {}

    // ---------------------------------------------------------------------------
    // MpvEventListener
    // ---------------------------------------------------------------------------

    override fun onFileLoaded() {
        _playerState.update { it.copy(fileLoaded = true, isLoading = false, error = null, hasError = false) }
    }

    override fun onPlaybackStarted() {
        _playerState.update { it.copy(isPaused = false, isLoading = false) }
    }

    override fun onPlaybackStopped(endReason: Int) {
        _playerState.update { it.copy(isPaused = true) }
        if (endReason == 0 && mpvController.surface.hasSurface()) {
            playlistManager.onPlaybackEnded()
        }
    }

    override fun onPropertyChange(name: String, value: Any?) {
        when (name) {
            "pause" -> {
                val paused = value as? Boolean ?: return
                _playerState.update { it.copy(isPaused = paused) }
            }
            "time-pos" -> {
                val seconds = value as? Double ?: return
                val newPosMs = (seconds * 1000).toLong()
                val now = System.currentTimeMillis()
                
                if (now - lastSeekTime < 2000 || now - lastTimePosUpdate >= 200) {
                    _playerState.update { it.copy(positionSec = seconds) }
                    lastTimePosUpdate = now
                }
            }
            "duration" -> {
                val seconds = value as? Double ?: return
                _playerState.update { it.copy(durationSec = seconds) }
            }
            "demuxer-cache-time" -> {
                val seconds = value as? Double ?: return
                _playerState.update { it.copy(cachedSec = seconds) }
            }
            "track-list" -> {
                val node = value as? MPVNode ?: return
                val audioTracks = TrackListParser.parseAudioTracks(node)
                val subtitleTracks = TrackListParser.parseSubtitleTracks(node)
                _playerState.update { it.copy(audioTracks = audioTracks, subtitleTracks = subtitleTracks) }
            }
            "aid" -> {
                val id = (value as? Long)?.toInt() ?: -1
                _playerState.update { it.copy(currentAudioTrackId = id) }
            }
            "sid" -> {
                val id = (value as? Long)?.toInt() ?: -1
                _playerState.update { it.copy(currentSubtitleTrackId = id) }
            }
            "speed" -> {
                val speed = value as? Double ?: return
                _playerState.update { it.copy(playbackSpeed = speed) }
            }
            "hwdec" -> {
                val hwdec = value as? String ?: return
                val mode = when (hwdec) {
                    DecodeMode.HWPlus.mpvValue -> DecodeMode.HWPlus
                    DecodeMode.SW.mpvValue, "no", "" -> DecodeMode.SW
                    else -> DecodeMode.HW
                }
                if (_playerState.value.decodeMode != mode) {
                    _playerState.update { it.copy(decodeMode = mode) }
                }
            }
            "volume" -> {
                val volume = value as? Double ?: return
                _playerState.update { it.copy(volume = volume.toInt()) }
            }
        }
    }

    override fun onError(message: String) {
        _playerState.update { it.copy(error = message, hasError = true, isLoading = false) }
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCleared() {
        mpvController.dispatcher.removeListener(this)
        mpvController.destroy()
        super.onCleared()
    }
}
