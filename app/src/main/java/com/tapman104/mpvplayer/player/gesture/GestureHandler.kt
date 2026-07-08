package com.tapman104.mpvplayer.player.gesture

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Integration point that wires Compose touch events to [MpvGestureStateMachine]
 * and renders visual feedback indicators.
 */
@Composable
fun GestureHandler(
    currentPositionMs: () -> Long,
    durationMs: () -> Long,
    isPlaying: Boolean,
    currentSpeed: () -> Float = { 1.0f },
    onSeek: (Long, Boolean) -> Unit = { _, _ -> },
    onSeekGesture: (Long) -> Unit = {},
    onSeekCommit: (Long) -> Unit = {},
    onSeekForward: (Long) -> Unit = {},
    onSeekBackward: (Long) -> Unit = {},
    onToggleControls: () -> Unit,
    onSpeedOverride: (Float) -> Unit,
    onSpeedRestore: () -> Unit,
    modifier: Modifier = Modifier,
    currentZoom: Float = 0f,
    onZoomChange: (Float) -> Unit = {},
    initialBrightness: Float = -1f,
    onBrightnessChange: (Float) -> Unit = {},
    volumePercentage: Int = 0,
    onVolumeChange: (Int) -> Unit = {},
    onSeekPreviewMs: (Long) -> Unit = {},
    doubleTapSeekSeconds: Int = 10,
    swipeToSeek: Boolean = true,
    brightnessSwipe: Boolean = true,
    volumeSwipe: Boolean = true,
    longPress2x: Boolean = true,
    gestureSensitivity: String = "normal",
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxMusicVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    var screenHeightPx by remember { mutableFloatStateOf(1920f) }

    var localZoomLog2 by remember { mutableFloatStateOf(currentZoom) }
    var localPanX by remember { mutableFloatStateOf(0f) }
    var localPanY by remember { mutableFloatStateOf(0f) }
    var localVolumePercent by remember {
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf(if (maxMusicVol > 0) (currentVol / maxMusicVol) * 100f else 0f)
    }
    var localBrightness by remember { mutableFloatStateOf(if (initialBrightness >= 0f) initialBrightness else 0.5f) }

    var doubleTapOverlayData by remember { mutableStateOf<Triple<Int, Boolean, String>?>(null) }
    var hSeekOverlayData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var speedOverlayVal by remember { mutableStateOf<Float?>(null) }
    var volumeOverlayPct by remember { mutableStateOf<Int?>(null) }
    var brightnessOverlayPct by remember { mutableStateOf<Int?>(null) }
    var zoomOverlayShown by remember { mutableStateOf(false) }

    LaunchedEffect(currentZoom) {
        localZoomLog2 = currentZoom
    }
    LaunchedEffect(initialBrightness) {
        if (initialBrightness >= 0f) {
            localBrightness = initialBrightness
            if (brightnessOverlayPct != null) {
                brightnessOverlayPct = (initialBrightness * 100).roundToInt()
            }
        }
    }

    val currentPositionMsRef = rememberUpdatedState(currentPositionMs)
    val durationMsRef = rememberUpdatedState(durationMs)
    val isPlayingRef = rememberUpdatedState(isPlaying)
    val currentSpeedRef = rememberUpdatedState(currentSpeed)
    val screenWidthPxRef = rememberUpdatedState(screenWidthPx)
    val screenHeightPxRef = rememberUpdatedState(screenHeightPx)
    val onSeekRef = rememberUpdatedState(onSeek)
    val onSeekGestureRef = rememberUpdatedState(onSeekGesture)
    val onSeekCommitRef = rememberUpdatedState(onSeekCommit)
    val onSeekForwardRef = rememberUpdatedState(onSeekForward)
    val onSeekBackwardRef = rememberUpdatedState(onSeekBackward)
    val onSpeedOverrideRef = rememberUpdatedState(onSpeedOverride)
    val onSpeedRestoreRef = rememberUpdatedState(onSpeedRestore)
    val onZoomChangeRef = rememberUpdatedState(onZoomChange)
    val onBrightnessChangeRef = rememberUpdatedState(onBrightnessChange)
    val onVolumeChangeRef = rememberUpdatedState(onVolumeChange)
    val onSeekPreviewMsRef = rememberUpdatedState(onSeekPreviewMs)
    val onToggleControlsRef = rememberUpdatedState(onToggleControls)

    val controller = remember(audioManager, maxMusicVol) {
        object : MpvPlayerController {
            override val durationMs: Long get() = durationMsRef.value()
            override val currentPositionMs: Long get() = currentPositionMsRef.value()
            override val isPaused: Boolean get() = !isPlayingRef.value
            override val currentZoomLog2: Float get() = localZoomLog2
            override val currentPanX: Float get() = localPanX
            override val currentPanY: Float get() = localPanY
            override val volume: Float get() = localVolumePercent
            override val maxStandardVolume: Float get() = 100f
            override val maxBoostVolume: Float get() = 130f
            override val brightness: Float get() = localBrightness
            override val screenWidthPx: Float get() = screenWidthPxRef.value
            override val screenHeightPx: Float get() = screenHeightPxRef.value
            override val isVolumeSideRight: Boolean get() = true
            override val doubleTapSeekAreaWidthPercent: Int get() = 30
            override val isDynamicSpeedOverlayEnabled: Boolean get() = true
            override val playbackSpeed: Float get() = currentSpeedRef.value()

            override fun pause() {}
            override fun unpause() {}

            override fun seekTo(positionMs: Long, precise: Boolean) = onSeekRef.value(positionMs, precise)
            override fun seekForward(offsetMs: Long) = onSeekForwardRef.value(offsetMs)
            override fun seekBackward(offsetMs: Long) = onSeekBackwardRef.value(offsetMs)
            override fun seekGesture(positionMs: Long) = onSeekGestureRef.value(positionMs)
            override fun seekCommit(positionMs: Long) = onSeekCommitRef.value(positionMs)

            override fun setPlaybackSpeedRamped(targetSpeed: Float, stepCount: Int, stepDurationMs: Long) {
                onSpeedOverrideRef.value(targetSpeed)
            }

            override fun restorePlaybackSpeed() {
                onSpeedRestoreRef.value()
            }

            override fun setVolume(volume: Float) {
                localVolumePercent = volume.coerceIn(0f, 130f)
                val targetVol = ((localVolumePercent.coerceIn(0f, 100f) / 100f) * maxMusicVol).roundToInt().coerceIn(0, maxMusicVol.toInt())
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                } catch (e: Exception) {
                    // ignore if permission denied
                }
                val pct = localVolumePercent.roundToInt()
                volumeOverlayPct = pct
                onVolumeChangeRef.value(pct)
            }

            override fun setBrightness(brightness: Float) {
                localBrightness = brightness.coerceIn(0f, 1f)
                val pct = (localBrightness * 100).roundToInt()
                brightnessOverlayPct = pct
                onBrightnessChangeRef.value(localBrightness)
            }

            override fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float) {
                localZoomLog2 = zoomLog2
                localPanX = panX
                localPanY = panY
                onZoomChangeRef.value(zoomLog2)
            }

            override fun showDoubleTapSeekOverlay(seekAmountSec: Int, isForward: Boolean, label: String) {
                doubleTapOverlayData = Triple(seekAmountSec, isForward, label)
            }

            override fun hideDoubleTapSeekOverlay() {
                doubleTapOverlayData = null
            }

            override fun showHorizontalSeekOverlay(currentTimeLabel: String, deltaLabel: String, targetPositionMs: Long) {
                hSeekOverlayData = currentTimeLabel to deltaLabel
                onSeekPreviewMsRef.value(targetPositionMs)
            }

            override fun hideHorizontalSeekOverlay(delayMs: Long) {
                if (delayMs > 0) {
                    coroutineScope.launch {
                        delay(delayMs)
                        hSeekOverlayData = null
                        onSeekPreviewMsRef.value(-1L)
                    }
                } else {
                    hSeekOverlayData = null
                    onSeekPreviewMsRef.value(-1L)
                }
            }

            override fun showSpeedOverlay(speed: Float, interactiveSliderIndex: Int?) {
                speedOverlayVal = speed
            }

            override fun hideSpeedOverlay() {
                speedOverlayVal = null
            }

            override fun showVolumeOverlay(percentage: Int) {
                volumeOverlayPct = percentage
            }

            override fun hideVolumeOverlay() {
                volumeOverlayPct = null
            }

            override fun showBrightnessOverlay(percentage: Int) {
                brightnessOverlayPct = percentage
            }

            override fun hideBrightnessOverlay() {
                brightnessOverlayPct = null
            }

            override fun showPinchZoomOverlay(zoomPercentage: Int) {
                zoomOverlayShown = true
            }

            override fun hidePinchZoomOverlay() {
                zoomOverlayShown = false
            }

            override fun showTapFeedback(x: Float, y: Float) {}

            override fun scheduleTimer(delayMs: Long, action: () -> Unit): Any {
                return coroutineScope.launch {
                    delay(delayMs)
                    action()
                }
            }

            override fun cancelTimer(timerId: Any?) {
                (timerId as? Job)?.cancel()
            }

            override fun triggerSingleTapAction() = onToggleControlsRef.value()
        }
    }

    val stateMachine = remember { MpvGestureStateMachine(controller) }

    LaunchedEffect(
        doubleTapSeekSeconds,
        swipeToSeek,
        brightnessSwipe,
        volumeSwipe,
        longPress2x,
        gestureSensitivity
    ) {
        stateMachine.seekDurationSec = doubleTapSeekSeconds
        stateMachine.swipeToSeekEnabled = swipeToSeek
        stateMachine.brightnessSwipeEnabled = brightnessSwipe
        stateMachine.volumeSwipeEnabled = volumeSwipe
        stateMachine.longPress2xEnabled = longPress2x
        stateMachine.deadzoneMultiplier = when (gestureSensitivity) {
            "low" -> 1.5f
            "high" -> 0.6f
            else -> 1.0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                if (it.width > 0 && it.width.toFloat() != screenWidthPx) screenWidthPx = it.width.toFloat()
                if (it.height > 0 && it.height.toFloat() != screenHeightPx) screenHeightPx = it.height.toFloat()
            }
            .pointerInput(stateMachine) {
                awaitEachGesture {
                    val downEvent = awaitFirstDown(requireUnconsumed = false)
                    val density = this.density

                    stateMachine.onPointerDown(
                        pointerId = downEvent.id.value,
                        x = downEvent.position.x,
                        y = downEvent.position.y,
                        timeMs = downEvent.uptimeMillis,
                        activePointerCount = 1,
                        panelShown = PanelShown.NONE,
                        density = density
                    )
                    var previousActiveCount = 1

                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        val activeCount = changes.count { it.pressed }
                        val firstPressed = changes.firstOrNull { it.pressed }

                        if (firstPressed == null || activeCount == 0) {
                            val lastEvent = changes.firstOrNull() ?: downEvent
                            stateMachine.onPointerUp(
                                pointerId = lastEvent.id.value,
                                x = lastEvent.position.x,
                                y = lastEvent.position.y,
                                timeMs = lastEvent.uptimeMillis,
                                activePointerCount = 0
                            )
                            break
                        }

                        if (previousActiveCount >= 2 && activeCount == 1) {
                            val released = changes.firstOrNull { !it.pressed } ?: firstPressed
                            stateMachine.onPointerUp(
                                pointerId = released.id.value,
                                x = released.position.x,
                                y = released.position.y,
                                timeMs = released.uptimeMillis,
                                activePointerCount = 1
                            )
                        }

                        var span = 0f
                        var midX = firstPressed.position.x
                        var midY = firstPressed.position.y
                        if (activeCount >= 2) {
                            val pressedChanges = changes.filter { it.pressed }
                            if (pressedChanges.size >= 2) {
                                val p0 = pressedChanges[0].position
                                val p1 = pressedChanges[1].position
                                val dx = p0.x - p1.x
                                val dy = p0.y - p1.y
                                span = sqrt(dx * dx + dy * dy)
                                midX = (p0.x + p1.x) / 2f
                                midY = (p0.y + p1.y) / 2f
                            }
                        }

                        stateMachine.onPointerMove(
                            pointerId = firstPressed.id.value,
                            x = firstPressed.position.x,
                            y = firstPressed.position.y,
                            timeMs = firstPressed.uptimeMillis,
                            activePointerCount = activeCount,
                            panelShown = PanelShown.NONE,
                            density = density,
                            span = span,
                            midpointX = midX,
                            midpointY = midY
                        )
                        changes.forEach { it.consume() }
                        previousActiveCount = activeCount
                    }
                }
            }
    ) {
        doubleTapOverlayData?.let { (_, isForward, label) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 64.dp),
                contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                SeekCircleIndicator(label = label, isForward = isForward)
            }
        }
        hSeekOverlayData?.let { (currentLabel, deltaLabel) ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HorizontalSeekIndicator(currentTimeLabel = currentLabel, deltaLabel = deltaLabel)
            }
        }
        speedOverlayVal?.let { speed ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                SpeedIndicator(label = "${speed}× Speed")
            }
        }
        volumeOverlayPct?.let { pct ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VolumeIndicator(percentage = pct)
            }
        }
        brightnessOverlayPct?.let { pct ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BrightnessIndicator(brightness = pct / 100f)
            }
        }
        if (zoomOverlayShown) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PinchZoomIndicator(zoom = localZoomLog2)
            }
        }
    }
}
