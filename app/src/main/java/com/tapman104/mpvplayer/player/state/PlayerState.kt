package com.tapman104.mpvplayer.player.state

import com.tapman104.mpvplayer.player.model.AudioTrack
import com.tapman104.mpvplayer.player.model.SubtitleTrack
import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.model.AspectRatioMode

data class PlayerState(
    val fileLoaded: Boolean = false,
    val isPaused: Boolean = true,
    val isBuffering: Boolean = false,
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
    val subtitleSize: Float = 1.1f,
    val subtitlePosition: Float = 0.07f,
    val videoZoom: Float = 0f,      // MPV video-zoom range: -1f to 3f
    val videoPanX: Float = 0f,      // MPV video-pan-x
    val videoPanY: Float = 0f,      // MPV video-pan-y
) {
    val isPlaying: Boolean get() = !isPaused
    val speed: Float get() = playbackSpeed.toFloat()
}
