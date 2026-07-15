package com.tapman104.mpvplayer.player.playback

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tapman104.mpvplayer.player.controls.PlayerBottomControls
import com.tapman104.mpvplayer.player.controls.PlayerQuickActions
import com.tapman104.mpvplayer.player.controls.PlayerTopBar
import com.tapman104.mpvplayer.player.controls.PlayerViewControls
import com.tapman104.mpvplayer.player.model.QuickActionsPosition
import com.tapman104.mpvplayer.player.model.ViewMode
import com.tapman104.mpvplayer.player.dialog.DecodeModePicker
import com.tapman104.mpvplayer.player.dialog.SubtitleAppearanceDialog
import com.tapman104.mpvplayer.player.dialogs.AudioTrackDialog
import com.tapman104.mpvplayer.player.dialogs.MoreOptionsSheet
import com.tapman104.mpvplayer.player.dialogs.SubtitleTrackDialog
import com.tapman104.mpvplayer.player.gesture.BrightnessIndicator
import com.tapman104.mpvplayer.player.gesture.GestureHandler
import com.tapman104.mpvplayer.player.gesture.GestureIntent
import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.model.FileInfo
import com.tapman104.mpvplayer.player.model.PlayerError
import kotlin.math.roundToInt
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface OverlayDialog {
    data object AudioTracks : OverlayDialog
    data object SubtitleTracks : OverlayDialog
    data object SubtitleAppearance : OverlayDialog
    data object DecodeModePicker : OverlayDialog
    data object MoreOptions : OverlayDialog
}

@Stable
data class OverlayUiState(
    val areControlsVisible: Boolean = true,
    val activeDialog: OverlayDialog? = null,
    val isGestureActive: Boolean = false
) {
    val shouldAutoHide: Boolean
        get() = areControlsVisible && activeDialog == null && !isGestureActive
}

@Composable
fun PlayerOverlay(
    fileName: String,
    playerState: PlayerState,
    positionState: PositionState,
    onOpenFile: () -> Unit,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    initialBrightness: Float = -1f,
    onBrightnessChange: (Float) -> Unit = {},
    onGestureIntent: (GestureIntent) -> Unit = {},
    onTogglePlay: () -> Unit,
    onAudioTrackSelected: (Int) -> Unit,
    onAddAudioClick: () -> Unit,
    onSubtitleTrackSelected: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onAddSubtitleClick: () -> Unit,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onCycleDecodeMode: (DecodeMode) -> Unit,
    onSubtitleSizeChange: (Float) -> Unit,
    onSubtitlePositionChange: (Float) -> Unit,
    onSubtitleAppearanceReset: () -> Unit,
    doubleTapSeekSeconds: Int = 10,
    swipeToSeek: Boolean = true,
    brightnessSwipe: Boolean = true,
    volumeSwipe: Boolean = true,
    longPress2x: Boolean = true,
    quickActionsPosition: QuickActionsPosition = QuickActionsPosition.TOP_RIGHT,
    currentViewMode: ViewMode = ViewMode.FIT,
    onCycleViewMode: () -> Unit = {},
    onRotate: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var overlayState by remember { mutableStateOf(OverlayUiState()) }
    // Drives the animated visibility of the decode-mode dialog independently so
    // we can play the exit animation before the dialog is removed from composition.
    var decodeDialogVisible by remember { mutableStateOf(false) }
    var pendingDecodeMode by remember { mutableStateOf<DecodeMode?>(null) }
    val coroutineScope = rememberCoroutineScope()
    // Tracks the gesture scrub target in real-time; -1L when not scrubbing.
    // Used to update the bottom seek bar without waiting for the 200ms-throttled playerState.
    var gestureSeekPreviewMs by remember { mutableStateOf(-1L) }
    val volumePercentageState = remember(playerState.volume) { mutableIntStateOf(playerState.volume) }
    var volumePercentage by volumePercentageState

    // When the decode dialog is requested, immediately hide the player controls
    // so the dialog appears on a clean, uncluttered background.
    LaunchedEffect(overlayState.activeDialog) {
        if (overlayState.activeDialog == OverlayDialog.DecodeModePicker) {
            overlayState = overlayState.copy(areControlsVisible = false)
            decodeDialogVisible = true
        }
    }

    LaunchedEffect(overlayState.shouldAutoHide) {
        if (overlayState.shouldAutoHide) {
            delay(3000L)
            overlayState = overlayState.copy(areControlsVisible = false)
        }
    }

    val currentPositionMs = rememberUpdatedState(positionState.currentPositionMs)
    val durationMs = rememberUpdatedState(positionState.durationMs)
    val currentSpeed = rememberUpdatedState(playerState.speed)
    val onSeekPreviewMs = remember { { ms: Long -> gestureSeekPreviewMs = ms } }
    val onGestureIntentAction = remember(onGestureIntent) {
        { intent: GestureIntent ->
            when (intent) {
                is GestureIntent.SeekCommit -> {
                    gestureSeekPreviewMs = -1L
                    onGestureIntent(intent)
                }
                is GestureIntent.VolumeChange -> {
                    volumePercentage = intent.delta.roundToInt()
                    onGestureIntent(intent)
                }
                else -> onGestureIntent(intent)
            }
        }
    }
    val onSeekCommitAction = remember(onGestureIntent) {
        { posMs: Long ->
            gestureSeekPreviewMs = -1L
            onGestureIntent(GestureIntent.SeekCommit(posMs))
        }
    }
    val onSeekGestureDragAction = remember(onGestureIntent) {
        { posMs: Long ->
            onGestureIntent(GestureIntent.SeekGestureDrag(posMs))
        }
    }
    val onToggleControls = remember {
        { overlayState = overlayState.copy(areControlsVisible = !overlayState.areControlsVisible) }
    }
    val onOpenAudioDialog = remember {
        { overlayState = overlayState.copy(activeDialog = OverlayDialog.AudioTracks) }
    }
    val onOpenSubtitleDialog = remember {
        { overlayState = overlayState.copy(activeDialog = OverlayDialog.SubtitleTracks) }
    }
    val onOpenDecodeDialog = remember {
        { overlayState = overlayState.copy(activeDialog = OverlayDialog.DecodeModePicker) }
    }
    val onOpenMoreOptions = remember {
        { overlayState = overlayState.copy(activeDialog = OverlayDialog.MoreOptions) }
    }
    val onDismissDialog = remember {
        { overlayState = overlayState.copy(activeDialog = null) }
    }
    val onGestureActiveChange = remember {
        { active: Boolean ->
            overlayState = overlayState.copy(isGestureActive = active)
        }
    }

    val onOpenSettingsAction = remember(onOpenSettings, onPause) {
        {
            onPause()
            onOpenSettings()
        }
    }
    val fileInfo = remember(fileName, positionState.durationMs, playerState.audioTracks, playerState.subtitleTracks) {
        FileInfo(
            fileName = fileName,
            filePath = null,
            durationMs = positionState.durationMs,
            videoTracks = 1,
            audioTracks = playerState.audioTracks.size,
            subtitleTracks = playerState.subtitleTracks.size,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {

        GestureHandler(
            currentPositionMs = { currentPositionMs.value },
            durationMs = { durationMs.value },
            isPlaying = !playerState.isPaused,
            currentSpeed = { currentSpeed.value },
            onIntent = onGestureIntentAction,
            onToggleControls = onToggleControls,
            modifier = Modifier.fillMaxSize(),
            currentZoom = playerState.videoZoom,
            initialBrightness = initialBrightness,
            volumePercentage = volumePercentage,
            onSeekPreviewMs = onSeekPreviewMs,
            doubleTapSeekSeconds = doubleTapSeekSeconds,
            swipeToSeek = swipeToSeek,
            brightnessSwipe = brightnessSwipe,
            volumeSwipe = volumeSwipe,
            longPress2x = longPress2x,
            onGestureActiveChange = onGestureActiveChange
        )

        // ── GRADIENT SCRIMS ──────────────────────────────────────────────────
        val scrimAlpha by animateFloatAsState(
            targetValue = if (overlayState.areControlsVisible) 1f else 0f,
            animationSpec = tween(200),
            label = "ScrimAlpha"
        )

        // Top scrim
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = scrimAlpha }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
        )

        // Bottom scrim
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = scrimAlpha }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )

        // ── TOP BAR ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = overlayState.areControlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TopBar (back button + filename) takes all remaining space
                    Box(modifier = Modifier.weight(1f)) {
                        PlayerTopBar(
                            fileName = fileName,
                            onBack = onBack,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quick actions on the right — only when TOP_RIGHT
                    if (quickActionsPosition == QuickActionsPosition.TOP_RIGHT) {
                        PlayerQuickActions(
                            decodeMode = playerState.decodeMode,
                            onSelectAudioTrack = onOpenAudioDialog,
                            onSelectSubtitleTrack = onOpenSubtitleDialog,
                            onDecodeModeClick = onOpenDecodeDialog,
                            onMoreOptions = onOpenMoreOptions,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(56.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }
                }

                if (quickActionsPosition == QuickActionsPosition.TOP_LEFT) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerQuickActions(
                            decodeMode = playerState.decodeMode,
                            onSelectAudioTrack = onOpenAudioDialog,
                            onSelectSubtitleTrack = onOpenSubtitleDialog,
                            onDecodeModeClick = onOpenDecodeDialog,
                            onMoreOptions = onOpenMoreOptions
                        )
                    }
                }
            }
        }

        // ── BOTTOM CONTROLS ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = overlayState.areControlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerBottomControls(
                isPlaying = playerState.isPlaying,
                currentPositionMs = positionState.currentPositionMs,
                durationMs = positionState.durationMs,
                bufferPositionMs = positionState.demuxerCacheTimeMs,
                gestureSeekPreviewMs = gestureSeekPreviewMs,
                decodeMode = playerState.decodeMode,
                showQuickActions = quickActionsPosition == QuickActionsPosition.BOTTOM_LEFT,
                onTogglePlay = onTogglePlay,
                onSeek = onSeekCommitAction,
                onSeekGesture = onSeekGestureDragAction,
                onSeekPreviewMs = onSeekPreviewMs,
                onSelectAudioTrack = onOpenAudioDialog,
                onSelectSubtitleTrack = onOpenSubtitleDialog,
                onDecodeModeClick = onOpenDecodeDialog,
                onMoreOptions = onOpenMoreOptions
            )
        }

        // VIEW CONTROLS — separate, anchored BottomEnd, with bottom padding to
        // sit ABOVE the seek bar, not overlapping it
        AnimatedVisibility(
            visible = overlayState.areControlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
        ) {
            PlayerViewControls(
                currentViewMode = currentViewMode,
                onCycleViewMode = onCycleViewMode,
                onRotate = onRotate,
                onEnterPip = onEnterPip
            )
        }

        // ── LOADING INDICATOR ─────────────────────────────────────────────────
        if (playerState.isLoading) {
            CircularProgressIndicator(
                color = Color(0xFF8B5CF6),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── ERROR BANNER ──────────────────────────────────────────────────────
        val errorMessage = when (val err = playerState.error) {
            is PlayerError.EngineError -> err.message
            is PlayerError.FileNotFound -> "File not found: ${err.path}"
            is PlayerError.UnsupportedFormat -> "Unsupported format: ${err.path}"
            PlayerError.UnknownError -> "An unknown playback error occurred"
            null -> null
        }
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)),
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFB00020),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(onClick = onClearError) {
                            Text(
                                text = "DISMISS",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // ── DIALOGS ───────────────────────────────────────────────────────────
        if (overlayState.activeDialog == OverlayDialog.AudioTracks) {
            AudioTrackDialog(
                tracks = playerState.audioTracks,
                selectedTrackId = playerState.currentAudioTrackId,
                onSelectTrack = {
                    onAudioTrackSelected(it)
                    onDismissDialog()
                },
                onAddAudioClick = {
                    onAddAudioClick()
                    onDismissDialog()
                },
                onDismiss = onDismissDialog
            )
        }

        if (overlayState.activeDialog == OverlayDialog.SubtitleTracks) {
            SubtitleTrackDialog(
                tracks = playerState.subtitleTracks,
                selectedTrackId = playerState.currentSubtitleTrackId,
                onSelectTrack = {
                    onSubtitleTrackSelected(it)
                    onDismissDialog()
                },
                onDisableSubtitles = {
                    onDisableSubtitles()
                    onDismissDialog()
                },
                onAddSubtitleClick = {
                    onAddSubtitleClick()
                    onDismissDialog()
                },
                onAppearanceClick = {
                    overlayState = overlayState.copy(activeDialog = OverlayDialog.SubtitleAppearance)
                },
                onDismiss = onDismissDialog
            )
        }

        if (overlayState.activeDialog == OverlayDialog.SubtitleAppearance) {
            SubtitleAppearanceDialog(
                initialSize = playerState.subtitleSize,
                initialPosition = playerState.subtitlePosition,
                onApply = { size, position ->
                    onSubtitleSizeChange(size)
                    onSubtitlePositionChange(position)
                    onDismissDialog()
                },
                onDismiss = onDismissDialog,
                onReset = { onSubtitleAppearanceReset() }
            )
        }

        // ── DECODE MODE DIALOG (animated) ────────────────────────────────────
        if (overlayState.activeDialog == OverlayDialog.DecodeModePicker) {
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
                            onDismissDialog()
                            pendingDecodeMode = null
                        }
                    },
                    onDismiss = {
                        coroutineScope.launch {
                            decodeDialogVisible = false
                            delay(220L)
                            onDismissDialog()
                            onPlay()   // Player was explicitly paused; resume it.
                        }
                    }
                )
            }
        }

        if (overlayState.activeDialog == OverlayDialog.MoreOptions) {
            MoreOptionsSheet(
                playbackSpeed = playerState.playbackSpeed.toFloat(),
                fileInfo = fileInfo,
                onSpeedChange = { speed ->
                    onGestureIntent(GestureIntent.SetSpeed(speed))
                },
                onOpenSettings = onOpenSettingsAction,
                onDismiss = onDismissDialog
            )
        }
    }
}
