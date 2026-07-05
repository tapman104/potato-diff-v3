package com.tapman104.mpvplayer.player.state

import com.tapman104.mpvplayer.player.model.AudioTrack
import com.tapman104.mpvplayer.player.model.SubtitleTrack
import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.model.AspectRatioMode

data class PlayerState(
    val fileLoaded: Boolean = false,
    val isPaused: Boolean = true,
    val isBuffering: Boolean = false,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val cachedSec: Double = 0.0,
    val playbackSpeed: Double = 1.0,
    val volume: Int = 100,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val currentAudioTrackId: Int = -1,
    val currentSubtitleTrackId: Int = -1,
    val decodeMode: DecodeMode = DecodeMode.HW,
    val hasError: Boolean = false,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.DEFAULT,
    val isLoading: Boolean = true,
    val error: String? = null,
    val subtitleSize: Float = 1.1f,
    val subtitlePosition: Float = 0.07f,
    val videoZoom: Float = 0f,      // MPV video-zoom range: -1f to 3f
    val videoPanX: Float = 0f,      // MPV video-pan-x
    val videoPanY: Float = 0f,      // MPV video-pan-y
) {
    val isPlaying: Boolean get() = !isPaused
    val currentPositionMs: Long get() = (positionSec * 1000).toLong()
    val durationMs: Long get() = (durationSec * 1000).toLong()
    val demuxerCacheTimeMs: Long get() = (cachedSec * 1000).toLong()
    val speed: Float get() = playbackSpeed.toFloat()
    val selectedAudioTrackId: Int get() = currentAudioTrackId
    val selectedSubtitleTrackId: Int get() = currentSubtitleTrackId
    val aspectRatio: AspectRatioMode get() = aspectRatioMode
}
