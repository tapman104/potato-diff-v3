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
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.Utils

class PlayerViewModel(
    private val application: Application,
    val controller: MpvController,
    private val resumePositionManager: ResumePositionManager,
    val preferencesRepository: UserPreferencesRepository,
) : AndroidViewModel(application), MpvEventListener {

    private val TAG = "PlayerViewModel"

    private var lastTimePosUpdate = 0L
    private var lastCacheUpdate = 0L
    private var lastSeekTime = 0L
    /** True while the user is actively dragging the seek slider — suppresses mpv position echo-backs. */
    @Volatile private var isSliderSeeking = false

    private val _positionState = MutableStateFlow(PositionState())
    val positionState: StateFlow<PositionState> = _positionState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val playlistManager = PlaylistManager(
        context = application,
        onLoadFile = { path -> controller.executor.loadFile(path) },
        hasSurface = { controller.surface.hasSurface() }
    )

    private val subtitleController = SubtitleController(
        executor = controller.executor,
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

    val debandFilter = preferencesRepository.debandFilter
    val videoScale = preferencesRepository.videoScale
    val volumeBoost = preferencesRepository.volumeBoost
    val pitchCorrection = preferencesRepository.pitchCorrection
    val audioOutputDriver = preferencesRepository.audioOutputDriver
    val doubleTapSeekSeconds = preferencesRepository.doubleTapSeekSeconds
    val swipeToSeek = preferencesRepository.swipeToSeek
    val brightnessSwipe = preferencesRepository.brightnessSwipe
    val volumeSwipe = preferencesRepository.volumeSwipe
    val longPress2x = preferencesRepository.longPress2x
    val gestureSensitivity = preferencesRepository.gestureSensitivity
    val backgroundPlay = preferencesRepository.backgroundPlay

    init {
        resumePositionManager.attach(viewModelScope) { _positionState.value.durationMs }
        controller.dispatcher.addListener(this)
        controller.init()
        controller.surface.setSurfaceReadyCallback {
            onSurfaceReady()
        }
        viewModelScope.launch {
            when (val result = controller.initResult.first()) {
                is InitResult.Success -> {
                    viewModelScope.launch {
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
                    viewModelScope.launch {
                        preferencesRepository.debandFilter.collect { deband ->
                            controller.executor.execute { MPVLib.setPropertyBoolean("deband", deband) }
                        }
                    }
                    viewModelScope.launch {
                        preferencesRepository.videoScale.collect { scale ->
                            controller.executor.execute { MPVLib.setPropertyString("scale", scale) }
                        }
                    }
                    viewModelScope.launch {
                        preferencesRepository.volumeBoost.collect { boost ->
                            controller.executor.execute {
                                MPVLib.setPropertyInt("volume-max", boost)
                                if (boost > 100) {
                                    MPVLib.setPropertyInt("volume", boost)
                                }
                            }
                        }
                    }
                    viewModelScope.launch {
                        preferencesRepository.pitchCorrection.collect { enabled ->
                            controller.executor.execute { MPVLib.setPropertyBoolean("audio-pitch-correction", enabled) }
                        }
                    }
                    viewModelScope.launch {
                        preferencesRepository.audioOutputDriver.collect { ao ->
                            controller.executor.execute { MPVLib.setPropertyString("ao", ao) }
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

    fun play() = controller.executor.play()
    fun togglePlay() = controller.executor.togglePlay()
    
    fun seekTo(positionMs: Long, precise: Boolean = false) {
        lastSeekTime = System.currentTimeMillis()
        if (precise) {
            controller.executor.seekCommit(positionMs / 1000.0)
        } else {
            controller.executor.seekGesture(positionMs / 1000.0)
        }
    }
    
    fun onSeekCommitMs(positionMs: Long) {
        isSliderSeeking = false
        lastSeekTime = 0L
        lastTimePosUpdate = 0L   // accept next time-pos immediately to snap to committed position
        val seconds = positionMs / 1000.0
        controller.executor.seekCommit(seconds)
    }
    
    fun seekRelative(offsetMs: Long) {
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seekRelativeCoalesced(offsetMs / 1000.0)
    }
    
    fun setSpeed(speed: Float) = controller.executor.setSpeed(speed.toDouble())
    fun setVolume(volume: Int) = controller.executor.setVolume(volume)
    fun setAudioTrack(id: Int) {
        controller.executor.setAudioTrack(id)
        _playerState.update { it.copy(currentAudioTrackId = id) }
    }
    fun addAudioTrack(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            controller.executor.addAudioTrack(path)
        }
    }
    fun setSubtitleTrack(id: Int) {
        controller.executor.setSubtitleTrack(id)
        _playerState.update { it.copy(currentSubtitleTrackId = id) }
    }
    fun addSubtitle(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            controller.executor.addSubtitle(path)
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
            // DEFAULT and FIT both use container aspect with no panscan. DEFAULT honours the
            // container's embedded aspect; FIT explicitly forces the same — both are equivalent
            // for standard content but are kept separate for future divergence (e.g. letterbox
            // vs. cropped-fit semantics).
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
        controller.executor.pause()
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

    fun setDecodeMode(mode: DecodeMode) = cycleDecodeMode(mode)

    fun cycleDecodeMode(next: DecodeMode, resumeAfter: Boolean = false) {
        val mpvMode = when (next) {
            DecodeMode.HW     -> "mediacodec"
            DecodeMode.HWPlus -> "mediacodec-copy"
            DecodeMode.SW     -> "no"
        }
        controller.executor.setHwdec(mpvMode)
        if (resumeAfter) controller.executor.play()
        viewModelScope.launch {
            preferencesRepository.setDecodeMode(mpvMode)
        }
    }

    // ---------------------------------------------------------------------------
    // Zoom / Pan
    // ---------------------------------------------------------------------------

    fun setVideoZoom(zoom: Float) {
        controller.executor.setVideoZoom(zoom)
        _playerState.update { it.copy(videoZoom = zoom) }
    }

    fun setVideoPan(panX: Float, panY: Float) {
        controller.executor.setVideoPan(panX, panY)
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

    fun setDebandFilter(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDebandFilter(enabled)
        }
    }

    fun setVideoScale(scale: String) {
        viewModelScope.launch {
            preferencesRepository.setVideoScale(scale)
        }
    }

    fun setVolumeBoost(boost: Int) {
        viewModelScope.launch {
            preferencesRepository.setVolumeBoost(boost)
        }
    }

    fun setPitchCorrection(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPitchCorrection(enabled)
        }
    }

    fun setAudioOutputDriver(driver: String) {
        viewModelScope.launch {
            preferencesRepository.setAudioOutputDriver(driver)
        }
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setDoubleTapSeekSeconds(seconds)
        }
    }

    fun setSwipeToSeek(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSwipeToSeek(enabled)
        }
    }

    fun setBrightnessSwipe(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBrightnessSwipe(enabled)
        }
    }

    fun setVolumeSwipe(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVolumeSwipe(enabled)
        }
    }

    fun setLongPress2x(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLongPress2x(enabled)
        }
    }

    fun setGestureSensitivity(sensitivity: String) {
        viewModelScope.launch {
            preferencesRepository.setGestureSensitivity(sensitivity)
        }
    }

    fun setBackgroundPlay(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundPlay(mode)
        }
    }

    fun currentBrightness(): Float = getScreenBrightness()
    val screenWidthPx: Float get() = application.resources.displayMetrics.widthPixels.toFloat()
    val screenHeightPx: Float get() = application.resources.displayMetrics.heightPixels.toFloat()

    private fun getScreenBrightness(): Float {
        return try {
            val sysBrightness = Settings.System.getInt(application.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            (sysBrightness / 255f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0.5f
        }
    }

    fun pause() {
        controller.executor.pause()
    }

    fun seekGesture(positionMs: Long) {
        isSliderSeeking = true
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seekGesture(positionMs / 1000.0)
    }

    fun seekCommit(positionMs: Long) {
        isSliderSeeking = false
        lastSeekTime = 0L
        controller.executor.seekCommit(positionMs / 1000.0)
    }

    fun setVolume(volume: Float) {
        val volInt = volume.roundToInt().coerceIn(0, 130)
        controller.executor.setVolume(volInt)
        _playerState.update { it.copy(volume = volInt) }
    }

    fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float) {
        controller.executor.setVideoZoom(zoomLog2)
        controller.executor.setVideoPan(panX, panY)
        _playerState.update { it.copy(videoZoom = zoomLog2, videoPanX = panX, videoPanY = panY) }
    }


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
        if (endReason == 0 && controller.surface.hasSurface()) {
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
                val now = System.currentTimeMillis()

                // Suppress echo-backs while the slider is being actively dragged —
                // the UI already drives position from dragPositionMs in that window.
                if (isSliderSeeking) return

                // Outside drag: throttle to ~5 Hz so Compose isn't recomposed on every
                // mpv frame. After a seek commit, the next update is always accepted
                // (lastTimePosUpdate was reset to 0 on commit) to snap to the new position.
                if (now - lastTimePosUpdate >= 200) {
                    _positionState.update { it.copy(positionSec = seconds) }
                    lastTimePosUpdate = now
                }
            }
            "duration" -> {
                val seconds = value as? Double ?: return
                _positionState.update { it.copy(durationSec = seconds) }
            }
            "demuxer-cache-time" -> {
                val seconds = value as? Double ?: return
                val now = System.currentTimeMillis()
                if (now - lastCacheUpdate >= 500) {  // 2Hz is enough for cache indicator
                    _positionState.update { it.copy(cachedSec = seconds) }
                    lastCacheUpdate = now
                }
            }
            "track-list" -> {
                val node = value as? MPVNode ?: return
                val audioTracks = TrackListParser.parseAudioTracks(node)
                val subtitleTracks = TrackListParser.parseSubtitleTracks(node)
                _playerState.update { it.copy(audioTracks = audioTracks, subtitleTracks = subtitleTracks) }
                autoSelectSubtitle(subtitleTracks)
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
        controller.dispatcher.removeListener(this)
        controller.destroy()
        super.onCleared()
    }
}
