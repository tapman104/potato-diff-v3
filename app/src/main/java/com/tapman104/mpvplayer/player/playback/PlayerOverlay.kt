package com.tapman104.mpvplayer.player.playback

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tapman104.mpvplayer.player.controls.PlayerBottomControls
import com.tapman104.mpvplayer.player.controls.PlayerQuickActions
import com.tapman104.mpvplayer.player.controls.PlayerTopBar
import com.tapman104.mpvplayer.player.coordinator.OverlayController
import com.tapman104.mpvplayer.player.dialog.DecodeModePicker
import com.tapman104.mpvplayer.player.dialog.SubtitleAppearanceDialog
import com.tapman104.mpvplayer.player.dialogs.AudioTrackDialog
import com.tapman104.mpvplayer.player.dialogs.SubtitleTrackDialog
import com.tapman104.mpvplayer.player.gesture.GestureHandler
import com.tapman104.mpvplayer.player.gesture.MpvPlayerController
import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerOverlay(
    coordinator: MpvPlayerController? = null,
    onCoordinatorReady: ((OverlayController) -> Unit)? = null,
    fileName: String,
    playerState: PlayerState,
    positionState: PositionState,
    onOpenFile: () -> Unit,
    initialBrightness: Float = -1f,
    onBrightnessChange: (Float) -> Unit = {},
    onTogglePlay: () -> Unit,
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
    doubleTapSeekSeconds: Int = 10,
    swipeToSeek: Boolean = true,
    brightnessSwipe: Boolean = true,
    volumeSwipe: Boolean = true,
    longPress2x: Boolean = true,
    gestureSensitivity: String = "normal",
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleAppearanceDialog by remember { mutableStateOf(false) }
    var showDecodeModeDialog by remember { mutableStateOf(false) }
    // Drives the animated visibility of the decode-mode dialog independently so
    // we can play the exit animation before the dialog is removed from composition.
    var decodeDialogVisible by remember { mutableStateOf(false) }
    var pendingDecodeMode by remember { mutableStateOf<DecodeMode?>(null) }
    val coroutineScope = rememberCoroutineScope()
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

    val overlayImpl = remember {
        object : OverlayController {
            override fun showVolumeOverlay(percent: Int) { volumePercentage = percent }
            override fun hideVolumeOverlay() { /* optionally hide after delay */ }
            override fun showBrightnessOverlay(percent: Int) { /* drive brightness state if it exists */ }
            override fun hideBrightnessOverlay() {}
            override fun showHorizontalSeekOverlay(currentLabel: String, deltaLabel: String, targetMs: Long) {
                gestureSeekPreviewMs = targetMs
            }
            override fun hideHorizontalSeekOverlay(delayMs: Long) {
                coroutineScope.launch { delay(delayMs); gestureSeekPreviewMs = -1L }
            }
            override fun triggerSingleTapAction() { controlsVisible = !controlsVisible }
            override fun showDoubleTapSeekOverlay(amountSec: Int, isForward: Boolean, label: String) {}
            override fun hideDoubleTapSeekOverlay() {}
            override fun showSpeedOverlay(speed: Float, sliderIndex: Int?) {}
            override fun hideSpeedOverlay() {}
            override fun showPinchZoomOverlay(zoomPercent: Int) {}
            override fun hidePinchZoomOverlay() {}
            override fun showTapFeedback(x: Float, y: Float) {}
            override fun scheduleTimer(delayMs: Long, action: () -> Unit): Any {
                return coroutineScope.launch { delay(delayMs); action() }
            }
            override fun cancelTimer(timerId: Any?) {
                (timerId as? kotlinx.coroutines.Job)?.cancel()
            }
        }
    }

    LaunchedEffect(overlayImpl) {
        onCoordinatorReady?.invoke(overlayImpl)
    }

    // When the decode dialog is requested, immediately hide the player controls
    // so the dialog appears on a clean, uncluttered background.
    LaunchedEffect(showDecodeModeDialog) {
        if (showDecodeModeDialog) {
            controlsVisible = false
            decodeDialogVisible = true
        }
    }

    // Auto-hide controls after 3 s of inactivity. Dialogs suppress auto-hide via the guard
    // inside the effect body — they don't need to be keys, as the body rechecks on each resume.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && !showAudioDialog && !showSubtitleDialog && !showSubtitleAppearanceDialog && !showDecodeModeDialog) {
            delay(3000L)
            controlsVisible = false
        }
    }

    val currentPositionMs = remember(coordinator) { { coordinator?.currentPositionMs ?: 0L } }
    val durationMs = remember(coordinator) { { coordinator?.durationMs ?: 0L } }
    val currentSpeed = remember(coordinator) { { coordinator?.playbackSpeed ?: 1.0f } }
    val onSeekPreviewMs = remember { { ms: Long -> gestureSeekPreviewMs = ms } }
    val onSeek = remember(coordinator) { { pos: Long, precise: Boolean -> coordinator?.seekTo(pos, precise) ?: Unit } }
    val onSeekGesture = remember(coordinator) { { pos: Long -> coordinator?.seekGesture(pos) ?: Unit } }
    val onSeekCommit = remember(coordinator) {
        { posMs: Long ->
            gestureSeekPreviewMs = -1L
            coordinator?.seekCommit(posMs) ?: Unit
        }
    }
    val onSeekForward = remember(coordinator) { { offsetMs: Long -> coordinator?.seekForward(offsetMs) ?: Unit } }
    val onSeekBackward = remember(coordinator) { { offsetMs: Long -> coordinator?.seekBackward(offsetMs) ?: Unit } }
    val onToggleControls = remember { { controlsVisible = !controlsVisible } }
    val onSpeedOverride = remember(coordinator) { { speed: Float -> coordinator?.setPlaybackSpeedRamped(speed) ?: Unit } }
    val onSpeedRestore = remember(coordinator) { { coordinator?.restorePlaybackSpeed() ?: Unit } }
    val onZoomChange = remember(coordinator) {
        { zoom: Float ->
            coordinator?.setZoomAndPan(zoom, coordinator.currentPanX, coordinator.currentPanY) ?: Unit
        }
    }
    val onVolumeChange = remember(coordinator) {
        { vol: Int ->
            volumePercentage = vol
            coordinator?.setVolume(vol.toFloat()) ?: Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        GestureHandler(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = coordinator?.isPaused?.not() ?: playerState.isPlaying,
            currentSpeed = currentSpeed,
            onSeekPreviewMs = onSeekPreviewMs,
            onSeek = onSeek,
            onSeekGesture = onSeekGesture,
            onSeekCommit = onSeekCommit,
            onSeekForward    = onSeekForward,
            onSeekBackward   = onSeekBackward,
            onToggleControls = onToggleControls,
            onSpeedOverride  = onSpeedOverride,
            onSpeedRestore   = onSpeedRestore,
            modifier         = Modifier.fillMaxSize(),
            initialBrightness = initialBrightness,
            onBrightnessChange = onBrightnessChange,
            volumePercentage = volumePercentage,
            onVolumeChange = onVolumeChange,
            currentZoom = coordinator?.currentZoomLog2 ?: 0f,
            onZoomChange = onZoomChange,
            doubleTapSeekSeconds = doubleTapSeekSeconds,
            swipeToSeek = swipeToSeek,
            brightnessSwipe = brightnessSwipe,
            volumeSwipe = volumeSwipe,
            longPress2x = longPress2x,
            gestureSensitivity = gestureSensitivity
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
                currentPositionMs = positionState.currentPositionMs,
                durationMs = positionState.durationMs,
                gestureSeekPreviewMs = gestureSeekPreviewMs,
                onTogglePlay = onTogglePlay,
                onSeek = onSeekCommit,
                onSeekGesture = onSeekGesture,
                onSeekPreviewMs = onSeekPreviewMs
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

        // ── DECODE MODE DIALOG (animated) ────────────────────────────────────
        if (showDecodeModeDialog) {
            LaunchedEffect(Unit) { onPause() }

            AnimatedVisibility(
                visible = decodeDialogVisible,
                enter = fadeIn(tween(180)) + scaleIn(
                    animationSpec = tween(180),
                    initialScale = 0.92f,
                    transformOrigin = TransformOrigin.Center
                ),
                exit = fadeOut(tween(200)) + scaleOut(
                    animationSpec = tween(200),
                    targetScale = 0.88f,
                    transformOrigin = TransformOrigin.Center
                )
            ) {
                DecodeModePicker(
                    current = playerState.decodeMode,
                    onSelect = { mode ->
                        pendingDecodeMode = mode
                        // Trigger exit animation; fire the actual switch after it finishes.
                        coroutineScope.launch {
                            decodeDialogVisible = false
                            delay(220L)   // slightly longer than exit tween to guarantee completion
                            onCycleDecodeMode(mode)   // ViewModel delays 150 ms then switches hwdec + resumes
                            showDecodeModeDialog = false
                            pendingDecodeMode = null
                        }
                    },
                    onDismiss = {
                        coroutineScope.launch {
                            decodeDialogVisible = false
                            delay(220L)
                            showDecodeModeDialog = false
                            onPlay()   // Player was explicitly paused; resume it.
                        }
                    }
                )
            }
        }
    }
}
