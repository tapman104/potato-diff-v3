package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.model.SubtitleTrack
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PositionState

class EventProcessor(
    private val scope: CoroutineScope,
    private val playerState: MutableStateFlow<PlayerState>,
    private val positionState: MutableStateFlow<PositionState>,
    private val onPlaybackEnded: () -> Unit = {},
    private val onTracksLoaded: (List<SubtitleTrack>) -> Unit = {}
) : MpvEventListener {

    @Volatile var isSliderSeeking: Boolean = false
    @Volatile var lastTimePosUpdate: Long = 0L
    @Volatile var lastCacheUpdate: Long = 0L

    override fun onFileLoaded() {
        playerState.update { it.copy(fileLoaded = true, isLoading = false, error = null, hasError = false) }
    }

    override fun onPlaybackStarted() {
        playerState.update { it.copy(isPaused = false, isLoading = false) }
    }

    override fun onPlaybackStopped(endReason: Int) {
        playerState.update { it.copy(isPaused = true) }
        if (endReason == 0) {
            onPlaybackEnded()
        }
    }

    override fun onPropertyChange(name: String, value: Any?) {
        when (name) {
            "pause" -> {
                val paused = value as? Boolean ?: return
                if (playerState.value.isPaused == paused) return
                playerState.update { it.copy(isPaused = paused) }
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
                    if (positionState.value.positionSec != seconds) {
                        positionState.update { it.copy(positionSec = seconds) }
                    }
                    lastTimePosUpdate = now
                }
            }
            "duration" -> {
                val seconds = value as? Double ?: return
                if (positionState.value.durationSec != seconds) {
                    positionState.update { it.copy(durationSec = seconds) }
                }
            }
            "demuxer-cache-time" -> {
                val seconds = value as? Double ?: return
                val now = System.currentTimeMillis()
                if (now - lastCacheUpdate >= 500) {  // 2Hz is enough for cache indicator
                    if (positionState.value.cachedSec != seconds) {
                        positionState.update { it.copy(cachedSec = seconds) }
                    }
                    lastCacheUpdate = now
                }
            }
            "track-list" -> {
                val node = value as? MPVNode ?: return
                val audioTracks = TrackListParser.parseAudioTracks(node)
                val subtitleTracks = TrackListParser.parseSubtitleTracks(node)
                playerState.update { it.copy(audioTracks = audioTracks, subtitleTracks = subtitleTracks) }
                onTracksLoaded(subtitleTracks)
            }
            "aid" -> {
                val id = (value as? Long)?.toInt() ?: -1
                if (playerState.value.currentAudioTrackId == id) return
                playerState.update { it.copy(currentAudioTrackId = id) }
            }
            "sid" -> {
                val id = (value as? Long)?.toInt() ?: -1
                if (playerState.value.currentSubtitleTrackId == id) return
                playerState.update { it.copy(currentSubtitleTrackId = id) }
            }
            "speed" -> {
                val speed = value as? Double ?: return
                if (playerState.value.playbackSpeed == speed) return
                playerState.update { it.copy(playbackSpeed = speed) }
            }
            "hwdec" -> {
                val hwdec = value as? String ?: return
                val mode = when (hwdec) {
                    DecodeMode.HWPlus.mpvValue -> DecodeMode.HWPlus
                    DecodeMode.SW.mpvValue, "no", "" -> DecodeMode.SW
                    else -> DecodeMode.HW
                }
                if (playerState.value.decodeMode != mode) {
                    playerState.update { it.copy(decodeMode = mode) }
                }
            }
            "volume" -> {
                val volume = value as? Double ?: return
                if (playerState.value.volume == volume.toInt()) return
                playerState.update { it.copy(volume = volume.toInt()) }
            }
        }
    }

    override fun onError(message: String) {
        playerState.update { it.copy(error = message, hasError = true, isLoading = false) }
    }
}
