package com.tapman104.mpvplayer.player.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tapman104.mpvplayer.player.model.DecodeMode

@Composable
fun PlayerBottomControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferPositionMs: Long = 0L,
    gestureSeekPreviewMs: Long = -1L,
    decodeMode: DecodeMode,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekGesture: (Long) -> Unit = {},
    onSeekPreviewMs: (Long) -> Unit = {},
    onSelectAudioTrack: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onDecodeModeClick: () -> Unit,
    onMoreOptions: () -> Unit,
    showQuickActions: Boolean = true,
    modifier: Modifier = Modifier,
) {
    mpv.potato.tapman104.player.controls.PlayerBottomControls(
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        bufferPositionMs = bufferPositionMs,
        gestureSeekPreviewMs = gestureSeekPreviewMs,
        decodeMode = decodeMode,
        onTogglePlay = onTogglePlay,
        onSeek = onSeek,
        onSeekGesture = onSeekGesture,
        onSeekPreviewMs = onSeekPreviewMs,
        onSelectAudioTrack = onSelectAudioTrack,
        onSelectSubtitleTrack = onSelectSubtitleTrack,
        onDecodeModeClick = onDecodeModeClick,
        onMoreOptions = onMoreOptions,
        showQuickActions = showQuickActions,
        modifier = modifier
    )
}