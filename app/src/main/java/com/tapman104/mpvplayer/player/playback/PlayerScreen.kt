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
import com.tapman104.mpvplayer.player.state.PositionState
import com.tapman104.mpvplayer.player.model.DecodeMode
import mpv.potato.tapman104.player.model.QuickActionsPosition
import mpv.potato.tapman104.ui.theme.PlayerTheme

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    positionState: PositionState,
    surfaceView: SurfaceView,
    onTogglePlay: () -> Unit,
    initialBrightness: Float = -1f,
    onBrightnessChange: (Float) -> Unit = {},
    onSeekForward: (Long) -> Unit = {},
    onSeekBackward: (Long) -> Unit = {},
    onSeekGestureDrag: (Long) -> Unit = {},
    onSeekCommit: (Long) -> Unit = {},
    onSpeedOverride: (Float) -> Unit = {},
    onSpeedRestore: () -> Unit = {},
    onZoomChange: (Float) -> Unit = {},
    onVolumeChange: (Int) -> Unit = {},
    onOpenFile: () -> Unit,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    fileName: String = "Unknown",
    onAudioTrackSelected: (Int) -> Unit = {},
    onAddAudioClick: () -> Unit = {},
    onSubtitleTrackSelected: (Int) -> Unit = {},
    onDisableSubtitles: () -> Unit = {},
    onAddSubtitleClick: () -> Unit = {},
    onCycleDecodeMode: (DecodeMode) -> Unit = {},
    onPause: () -> Unit = {},
    onPlay: () -> Unit = {},
    onSubtitleSizeChange: (Float) -> Unit = {},
    onSubtitlePositionChange: (Float) -> Unit = {},
    onSubtitleAppearanceReset: () -> Unit = {},
    doubleTapSeekSeconds: Int = 10,
    swipeToSeek: Boolean = true,
    brightnessSwipe: Boolean = true,
    volumeSwipe: Boolean = true,
    longPress2x: Boolean = true,
    quickActionsPosition: QuickActionsPosition = QuickActionsPosition.BOTTOM_LEFT,
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

        PlayerTheme {
            PlayerOverlay(
                fileName = fileName,
                playerState = playerState,
                positionState = positionState,
                onSeekForward = onSeekForward,
                onSeekBackward = onSeekBackward,
                onSeekGestureDrag = onSeekGestureDrag,
                onSeekCommit = onSeekCommit,
                onSpeedOverride = onSpeedOverride,
                onSpeedRestore = onSpeedRestore,
                onZoomChange = onZoomChange,
                onVolumeChange = onVolumeChange,
                onOpenFile = onOpenFile,
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                initialBrightness = initialBrightness,
                onBrightnessChange = onBrightnessChange,
                onTogglePlay = onTogglePlay,
                onAudioTrackSelected = onAudioTrackSelected,
                onAddAudioClick = onAddAudioClick,
                onSubtitleTrackSelected = onSubtitleTrackSelected,
                onDisableSubtitles = onDisableSubtitles,
                onAddSubtitleClick = onAddSubtitleClick,
                onCycleDecodeMode = onCycleDecodeMode,
                onPause = onPause,
                onPlay = onPlay,
                onSubtitleSizeChange = onSubtitleSizeChange,
                onSubtitlePositionChange = onSubtitlePositionChange,
                onSubtitleAppearanceReset = onSubtitleAppearanceReset,
                doubleTapSeekSeconds = doubleTapSeekSeconds,
                swipeToSeek = swipeToSeek,
                brightnessSwipe = brightnessSwipe,
                volumeSwipe = volumeSwipe,
                longPress2x = longPress2x,
                quickActionsPosition = quickActionsPosition,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
