package com.tapman104.mpvplayer.player.viewmodel

import android.app.Application
import android.content.Context
import com.tapman104.mpvplayer.core.engine.EventProcessor
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.player.model.AspectRatioMode
import com.tapman104.mpvplayer.player.state.PlayerState
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

/**
 * Owns all play/pause/seek/speed/volume/aspect/zoom-pan commands.
 *
 * Extracted from [com.tapman104.mpvplayer.player.engine.PlayerEngine] so that
 * [PlayerEngine.dispatch] stays thin.  This coordinator holds **no** StateFlow of
 * its own; it mutates the [sharedPlayerState] that is also observed by
 * [EventProcessor] and exposed via [PlayerEngine.state].
 *
 * Constructor injection only — no singletons.
 */
class PlaybackCoordinator(
    private val application: Application,
    private val controller: MpvController,
    private val eventProcessor: EventProcessor,
    private val sharedPlayerState: MutableStateFlow<PlayerState>,
) {

    // ── Speed override tracking ───────────────────────────────────────────────

    private var preOverrideSpeed: Float = 1f
    private var isSpeedOverridden: Boolean = false

    // ── Last seek timestamp (for coalesced relative seeks) ───────────────────

    private var lastSeekTime = 0L

    // ─────────────────────────────────────────────────────────────────────────
    // Playback controls
    // ─────────────────────────────────────────────────────────────────────────

    fun play() = controller.executor.play()

    fun pause() = controller.executor.pause()

    fun togglePlay() = controller.executor.togglePlay()

    /**
     * Pause unconditionally — idempotent if already paused.
     * Used by the screen-off receiver and background-play logic.
     */
    fun pausePlayback() {
        if (sharedPlayerState.value.isPaused) return
        controller.executor.pause()
        sharedPlayerState.update { it.copy(isPaused = true) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Seeking
    // ─────────────────────────────────────────────────────────────────────────

    fun seekRelative(offsetMs: Long) {
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seekRelativeCoalesced(offsetMs / 1000.0)
    }

    fun seekGestureDrag(positionMs: Long) {
        eventProcessor.isSliderSeeking = true
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seekGesture(positionMs / 1000.0)
    }

    fun seekCommit(positionMs: Long) {
        eventProcessor.isSliderSeeking = false
        lastSeekTime = 0L
        eventProcessor.lastTimePosUpdate = 0L
        controller.executor.seekCommit(positionMs / 1000.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Volume & speed
    // ─────────────────────────────────────────────────────────────────────────

    fun setVolume(volume: Int) {
        val volInt = volume.coerceIn(0, 130)
        val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        audioManager?.let { am ->
            val maxMusicVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val targetStreamVol = ((volInt.coerceIn(0, 100) / 100f) * maxMusicVol).roundToInt().coerceIn(0, maxMusicVol)
            try {
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetStreamVol, 0)
            } catch (_: Exception) {}
        }
        val mpvVol = if (volInt <= 100) 100 else volInt
        controller.executor.setVolume(mpvVol)
        sharedPlayerState.update { it.copy(volume = volInt) }
    }

    fun setSpeed(speed: Float) = controller.executor.setSpeed(speed.toDouble())

    fun setPlaybackSpeedRamped(targetSpeed: Float) {
        if (!isSpeedOverridden) {
            preOverrideSpeed = sharedPlayerState.value.speed
            isSpeedOverridden = true
        }
        setSpeed(targetSpeed)
    }

    fun restorePlaybackSpeed() {
        if (isSpeedOverridden) {
            setSpeed(preOverrideSpeed)
            isSpeedOverridden = false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Video geometry
    // ─────────────────────────────────────────────────────────────────────────

    fun setAspectRatio(mode: AspectRatioMode) {
        if (sharedPlayerState.value.aspectRatioMode == mode) return
        sharedPlayerState.update { it.copy(aspectRatioMode = mode) }
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

    fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float) {
        controller.executor.setVideoZoom(zoomLog2)
        controller.executor.setVideoPan(panX, panY)
        sharedPlayerState.update { it.copy(videoZoom = zoomLog2, videoPanX = panX, videoPanY = panY) }
    }
}
