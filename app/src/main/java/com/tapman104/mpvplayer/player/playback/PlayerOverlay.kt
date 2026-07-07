package com.tapman104.mpvplayer.player.playback

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.model.DecodeMode
import kotlinx.coroutines.delay
import com.tapman104.mpvplayer.player.dialogs.AudioTrackDialog
import com.tapman104.mpvplayer.player.dialogs.SubtitleTrackDialog
import com.tapman104.mpvplayer.player.dialog.SubtitleAppearanceDialog
import com.tapman104.mpvplayer.player.dialog.DecodeModePicker
import com.tapman104.mpvplayer.player.controls.PlayerTopBar
import com.tapman104.mpvplayer.player.controls.PlayerBottomControls
import com.tapman104.mpvplayer.player.controls.PlayerQuickActions
import com.tapman104.mpvplayer.player.gesture.GestureHandler

@Composable
fun PlayerOverlay(
    fileName: String,
    playerState: PlayerState,
    onOpenFile: () -> Unit,
    initialBrightness: Float = -1f,
    onBrightnessChange: (Float) -> Unit = {},
    onTogglePlay: () -> Unit,
    onSeek: (Long, Boolean) -> Unit,
    onSeekGesture: (Long) -> Unit = {},
    onSeekCommit: (Long) -> Unit = {},
    onSeekForward: (Long) -> Unit,
    onSeekBackward: (Long) -> Unit,
    onSpeedOverride: (Float) -> Unit,
    onSpeedRestore: () -> Unit,
    onAudioTrackSelected: (Int) -> Unit,
    onAddAudioClick: () -> Unit,
    onSubtitleTrackSelected: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onAddSubtitleClick: () -> Unit,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onCycleDecodeMode: (DecodeMode) -> Unit,
    onMoreOptions: () -> Unit,
    onSubtitleSizeChange: (Float) -> Unit,
    onSubtitlePositionChange: (Float) -> Unit,
    onSubtitleAppearanceReset: () -> Unit,
    currentZoom: Float = 0f,
    onZoomChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleAppearanceDialog by remember { mutableStateOf(false) }
    var showDecodeModeDialog by remember { mutableStateOf(false) }
    // Tracks the gesture scrub target in real-time; -1L when not scrubbing.
    // Used to update the bottom seek bar without waiting for the 200ms-throttled playerState.
    var gestureSeekPreviewMs by remember { mutableStateOf(-1L) }
    val context = LocalContext.current
    var volumePercentage by remember {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableIntStateOf(if (max > 0) (current.toFloat() / max * 100).toInt() else 0)
    }

    // Auto-hide controls after 3 s of inactivity. Dialogs suppress auto-hide via the guard
    // inside the effect body — they don't need to be keys, as the body rechecks on each resume.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && !showAudioDialog && !showSubtitleDialog && !showSubtitleAppearanceDialog && !showDecodeModeDialog) {
            delay(3000L)
            controlsVisible = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        GestureHandler(
            currentPositionMs = { playerState.currentPositionMs },
            durationMs = { playerState.durationMs },
            isPlaying = playerState.isPlaying,
            currentSpeed = { playerState.speed },
            onSeekPreviewMs = { gestureSeekPreviewMs = it },
            onSeek = onSeek,
            onSeekGesture = onSeekGesture,
            onSeekCommit = { posMs ->
                gestureSeekPreviewMs = -1L
                onSeekCommit(posMs)
            },
            onSeekForward    = onSeekForward,
            onSeekBackward   = onSeekBackward,
            onToggleControls = { controlsVisible = !controlsVisible },
            onSpeedOverride  = onSpeedOverride,
            onSpeedRestore   = onSpeedRestore,
            modifier         = Modifier.fillMaxSize(),
            initialBrightness = initialBrightness,
            onBrightnessChange = onBrightnessChange,
            volumePercentage = volumePercentage,
            onVolumeChange = { volumePercentage = it },
            currentZoom = currentZoom,
            onZoomChange = onZoomChange
        )

        // ── TOP BAR ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                fileName = fileName,
                onBack = onOpenFile
            )
        }

        // ── QUICK ACTIONS ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 64.dp, start = 8.dp)
        ) {
            PlayerQuickActions(
                decodeMode = playerState.decodeMode,
                onSelectAudioTrack = { showAudioDialog = true },
                onSelectSubtitleTrack = { showSubtitleDialog = true },
                onDecodeModeClick = { showDecodeModeDialog = true },
                onMoreOptions = onMoreOptions
            )
        }

        // ── BOTTOM CONTROLS ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerBottomControls(
                isPlaying = playerState.isPlaying,
                currentPositionMs = playerState.currentPositionMs,
                durationMs = playerState.durationMs,
                gestureSeekPreviewMs = gestureSeekPreviewMs,
                onTogglePlay = onTogglePlay,
                onSeek = { pos -> onSeekCommit(pos) },
                onSeekGesture = onSeekGesture,
                onSeekPreviewMs = { gestureSeekPreviewMs = it }
            )
        }

        // ── LOADING INDICATOR ─────────────────────────────────────────────────
        if (playerState.isLoading) {
            CircularProgressIndicator(
                color = Color(0xFF8B5CF6),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── DIALOGS ───────────────────────────────────────────────────────────
        if (showAudioDialog) {
            AudioTrackDialog(
                tracks = playerState.audioTracks,
                selectedTrackId = playerState.currentAudioTrackId,
                onSelectTrack = {
                    onAudioTrackSelected(it)
                    showAudioDialog = false
                },
                onAddAudioClick = {
                    onAddAudioClick()
                    showAudioDialog = false
                },
                onDismiss = { showAudioDialog = false }
            )
        }

        if (showSubtitleDialog) {
            SubtitleTrackDialog(
                tracks = playerState.subtitleTracks,
                selectedTrackId = playerState.currentSubtitleTrackId,
                onSelectTrack = {
                    onSubtitleTrackSelected(it)
                    showSubtitleDialog = false
                },
                onDisableSubtitles = {
                    onDisableSubtitles()
                    showSubtitleDialog = false
                },
                onAddSubtitleClick = {
                    onAddSubtitleClick()
                    showSubtitleDialog = false
                },
                onAppearanceClick = {
                    showSubtitleDialog = false
                    showSubtitleAppearanceDialog = true
                },
                onDismiss = { showSubtitleDialog = false }
            )
        }

        if (showSubtitleAppearanceDialog) {
            SubtitleAppearanceDialog(
                initialSize = playerState.subtitleSize,
                initialPosition = playerState.subtitlePosition,
                onApply = { size, position ->
                    onSubtitleSizeChange(size)
                    onSubtitlePositionChange(position)
                    showSubtitleAppearanceDialog = false
                },
                onDismiss = { showSubtitleAppearanceDialog = false },
                onReset = { onSubtitleAppearanceReset() }
            )
        }

        if (showDecodeModeDialog) {
            LaunchedEffect(Unit) {
                onPause()
            }
            DecodeModePicker(
                current = playerState.decodeMode,
                onSelect = { mode ->
                    onCycleDecodeMode(mode)   // fires setHwdec + resume atomically after 150 ms
                    showDecodeModeDialog = false
                },
                onDismiss = {
                    showDecodeModeDialog = false
                    onPlay()  // Player was explicitly paused; resume it.
                }
            )
        }
    }
}
