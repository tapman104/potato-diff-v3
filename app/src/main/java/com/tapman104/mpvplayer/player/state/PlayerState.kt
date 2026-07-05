package com.tapman104.mpvplayer.player.state

import com.tapman104.mpvplayer.player.model.AudioTrack
import com.tapman104.mpvplayer.player.model.SubtitleTrack
import com.tapman104.mpvplayer.player.model.DecodeMode
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    // How far ahead is cached (from demuxer-cache-time), NOT a buffer byte position.
    val demuxerCacheTimeMs: Long = 0L,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedAudioTrackId: Int = -1,
    val selectedSubtitleTrackId: Int = -1,
    val speed: Float = 1.0f,
    val volume: Int = 100,
    val decodeMode: DecodeMode = DecodeMode.HW,
    val isLoading: Boolean = true,
    val error: String? = null,
    val subtitleSize: Float = 1.1f,
    val subtitlePosition: Float = 0.07f,
    val videoZoom: Float = 0f,      // MPV video-zoom range: -1f to 3f
    val videoPanX: Float = 0f,      // MPV video-pan-x
    val videoPanY: Float = 0f,      // MPV video-pan-y
    val aspectRatio: com.tapman104.mpvplayer.player.model.AspectRatioMode = com.tapman104.mpvplayer.player.model.AspectRatioMode.DEFAULT,
)
