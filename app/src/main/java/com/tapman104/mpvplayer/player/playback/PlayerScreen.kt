package com.tapman104.mpvplayer.player.playback

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.model.DecodeMode

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    surfaceView: SurfaceView,
    onTogglePlay: () -> Unit,
    onSeek: (Long, Boolean) -> Unit,
    onSeekGesture: (Long) -> Unit = {},
    onSeekCommit: (Long) -> Unit = {},
    initialBrightness: Float = -1f,
    onBrightnessChange: (Float) -> Unit = {},
    onOpenFile: () -> Unit,
    fileName: String = "Unknown",
    onSeekForward: (Long) -> Unit = {},
    onSeekBackward: (Long) -> Unit = {},
    onSpeedOverride: (Float) -> Unit = {},
    onSpeedRestore: () -> Unit = {},
    onAudioTrackSelected: (Int) -> Unit = {},
    onAddAudioClick: () -> Unit = {},
    onSubtitleTrackSelected: (Int) -> Unit = {},
    onDisableSubtitles: () -> Unit = {},
    onAddSubtitleClick: () -> Unit = {},
    onCycleDecodeMode: (DecodeMode) -> Unit = {},
    onPause: () -> Unit = {},
    onPlay: () -> Unit = {},
    onMoreOptions: () -> Unit = {},
    onSubtitleSizeChange: (Float) -> Unit = {},
    onSubtitlePositionChange: (Float) -> Unit = {},
    onSubtitleAppearanceReset: () -> Unit = {},
    currentZoom: Float = 0f,
    onZoomChange: (Float) -> Unit = {},
    doubleTapSeekSeconds: Int = 10,
    swipeToSeek: Boolean = true,
    brightnessSwipe: Boolean = true,
    volumeSwipe: Boolean = true,
    longPress2x: Boolean = true,
    gestureSensitivity: String = "normal",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(modifier)
    ) {
        AndroidView(
            factory = {
                surfaceView.apply {
                    setOnTouchListener { _, _ -> false }
                }
            },
            update = { /* intentionally empty */ },
            modifier = Modifier.fillMaxSize()
        )

        PlayerOverlay(
            fileName = fileName,
            playerState = playerState,
            onOpenFile = onOpenFile,
            initialBrightness = initialBrightness,
            onBrightnessChange = onBrightnessChange,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onSeekGesture = onSeekGesture,
            onSeekCommit = onSeekCommit,
            onSeekForward = onSeekForward,
            onSeekBackward = onSeekBackward,
            onSpeedOverride = onSpeedOverride,
            onSpeedRestore = onSpeedRestore,
            onAudioTrackSelected = onAudioTrackSelected,
            onAddAudioClick = onAddAudioClick,
            onSubtitleTrackSelected = onSubtitleTrackSelected,
            onDisableSubtitles = onDisableSubtitles,
            onAddSubtitleClick = onAddSubtitleClick,
            onCycleDecodeMode = onCycleDecodeMode,
            onPause = onPause,
            onPlay = onPlay,
            onMoreOptions = onMoreOptions,
            onSubtitleSizeChange = onSubtitleSizeChange,
            onSubtitlePositionChange = onSubtitlePositionChange,
            onSubtitleAppearanceReset = onSubtitleAppearanceReset,
            currentZoom = currentZoom,
            onZoomChange = onZoomChange,
            doubleTapSeekSeconds = doubleTapSeekSeconds,
            swipeToSeek = swipeToSeek,
            brightnessSwipe = brightnessSwipe,
            volumeSwipe = volumeSwipe,
            longPress2x = longPress2x,
            gestureSensitivity = gestureSensitivity,
            modifier = Modifier.fillMaxSize()
        )
    }
}
