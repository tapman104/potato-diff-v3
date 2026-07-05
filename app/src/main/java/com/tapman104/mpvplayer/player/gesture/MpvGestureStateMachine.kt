package com.tapman104.mpvplayer.player.gesture

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Screen region classification for tap/seek gestures.
 */
enum class TapRegion {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Overlay panel open states.
 */
enum class PanelShown {
    NONE,
    AUDIO_SETTINGS,
    SUBTITLES,
    OTHER
}

/**
 * Controller interface abstracting MPV player commands, OS audio/brightness, and UI overlays.
 * Decouples the state machine from specific Android/Compose implementations.
 */
interface MpvPlayerController {
    val durationMs: Long
    val currentPositionMs: Long
    val isPaused: Boolean
    val currentZoomLog2: Float
    val currentPanX: Float
    val currentPanY: Float
    val volume: Float // 0f..100f standard, >100f boost regime
    val maxStandardVolume: Float
    val maxBoostVolume: Float
    val brightness: Float // 0f..1f
    val screenWidthPx: Float
    val screenHeightPx: Float
    val isVolumeSideRight: Boolean
    val doubleTapSeekAreaWidthPercent: Int // default 30
    val isDynamicSpeedOverlayEnabled: Boolean

    fun pause()
    fun unpause()
    fun seekTo(positionMs: Long, precise: Boolean = false)
    fun seekGesture(positionMs: Long)
    fun seekCommit(positionMs: Long)
    fun setPlaybackSpeedRamped(targetSpeed: Float, stepCount: Int = 5, stepDurationMs: Long = 16L)
    fun setVolume(volume: Float)
    fun setBrightness(brightness: Float)
    fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float)
    
    // UI Overlay control
    fun showDoubleTapSeekOverlay(seekAmountSec: Int, isForward: Boolean, label: String)
    fun hideDoubleTapSeekOverlay()
    fun showHorizontalSeekOverlay(currentTimeLabel: String, deltaLabel: String, targetPositionMs: Long)
    fun hideHorizontalSeekOverlay(delayMs: Long = 0L)
    fun showSpeedOverlay(speed: Float, interactiveSliderIndex: Int? = null)
    fun hideSpeedOverlay()
    fun showVolumeOverlay(percentage: Int)
    fun hideVolumeOverlay()
    fun showBrightnessOverlay(percentage: Int)
    fun hideBrightnessOverlay()
    fun showPinchZoomOverlay(zoomPercentage: Int)
    fun hidePinchZoomOverlay()
    fun showTapFeedback(x: Float, y: Float)

    // Timers and scheduling (must be guarded by state checks when firing)
    fun scheduleTimer(delayMs: Long, action: () -> Unit): Any // returns cancellation token / job
    fun cancelTimer(timerId: Any?)
    fun triggerSingleTapAction()
}

/**
 * Single-Ownership State Machine representing all possible mutually exclusive gesture states.
 * Exactly ONE state is active at any given moment for the lifetime of a pointer sequence.
 * Each state owns ONLY the fields it needs; when exited, data is discarded.
 */
sealed class GestureState {
    /**
     * State 1: No touch gestures active.
     */
    object Idle : GestureState()

    /**
     * State 2: Initial pointer down; collecting raw deltas without committing to a gesture type.
     * Evaluates single-tap vs double-tap vs long-press vs drag classifiers.
     */
    data class TapCandidate(
        val downX: Float,
        val downY: Float,
        val downTimeMs: Long,
        val pointerId: Long,
        val deferredTapTimerId: Any? = null,
        val longPressTimerId: Any? = null,
        val exceededTapThreshold: Boolean = false
    ) : GestureState()

    /**
     * State 3: Multi-tap seek sequence (tap counting, region matching, direction reversal).
     * This state legitimately outlives a single touch sequence until its 800ms inactivity timeout fires.
     */
    data class MultiTapSeeking(
        val tapCount: Int,
        val accumulatedSeekSec: Int,
        val lastTapRegion: TapRegion,
        val lastTapTimeMs: Long,
        val lastTapX: Float,
        val lastTapY: Float,
        val isReverseDirection: Boolean,
        val inactivityTimerId: Any? = null,
        val hideUiTimerId: Any? = null
    ) : GestureState()

    /**
     * State 4: Long press activated after holding 500ms with <=10px movement.
     */
    data class LongPress(
        val startX: Float,
        val downTimeMs: Long,
        val initialSpeed: Float,
        val currentSpeed: Float,
        val isDynamicOverlayEnabled: Boolean,
        val suppressedDeferredTap: Boolean = true
    ) : GestureState()

    /**
     * State 5: Dragging horizontally >10dp while in LongPress unlocks interactive preset speed scrubbing.
     */
    data class DynamicSpeedScrub(
        val startX: Float,
        val startSpeed: Float,
        val lastAppliedSpeed: Float,
        val startPresetIndex: Int
    ) : GestureState()

    /**
     * State 6: Vertical swipe locked for either volume or brightness control.
     */
    data class VerticalSwipe(
        val isVolumeSide: Boolean,
        val startX: Float,
        val startY: Float,
        val currentY: Float,
        val anchorY: Float, // Anchored baseline; updated upon regime transition
        val isBoostRegime: Boolean,
        val initialValue: Float,
        val currentValue: Float
    ) : GestureState()

    /**
     * State 7: Multi-pointer pinch-to-zoom and simultaneous 2-finger pan.
     */
    data class PinchZoomPan(
        val initialSpan: Float,
        val prevSpan: Float,
        val initialZoomLog2: Float,
        val currentZoomLog2: Float, // Accumulated, clamped to log2 scale [-1.0, 3.0]
        val prevMidpointX: Float,
        val prevMidpointY: Float,
        val panX: Float,
        val panY: Float
    ) : GestureState()

    /**
     * State 8: Single-finger pan when zoom > 1.0x and movement > 20px.
     */
    data class SinglePan(
        val startX: Float,
        val startY: Float,
        val prevX: Float,
        val prevY: Float,
        val panX: Float,
        val panY: Float,
        val currentScale: Float
    ) : GestureState()

    /**
     * State 9: Horizontal seek-scrub locked when zoom <= 1.0x, |ΔX| > 30px, elapsed > 60ms, no panel open.
     */
    data class HorizontalSeek(
        val initialVideoPositionMs: Long,
        val durationMs: Long,
        val wasPlayingBeforeScrub: Boolean,
        val confirmationX: Float, // Baseline set at confirmation to avoid delta jumps
        val currentX: Float,
        val targetPositionMs: Long,
        val sensitivityMsPerPx: Float,
        val lastSeekIssuedAtMs: Long = 0L
    ) : GestureState()
}

/**
 * The Core Gesture Engine enforcing single-ownership state transitions and fixed decision points.
 */
class MpvGestureStateMachine(private val controller: MpvPlayerController) {

    @Volatile
    var currentState: GestureState = GestureState.Idle
        private set

    companion object {
        // Spec: Root gesture surface inset by 48dp dead-zone padding on all edges.
        const val EDGE_DEAD_ZONE_DP = 48f
        
        // Spec: Tap vs. drag: movement <= 10px while pressed = tap; >10px = drag
        const val TAP_MAX_MOVEMENT_PX = 10f
        
        // Spec: Drag classification thresholds
        const val VERTICAL_SWIPE_MIN_DY_PX = 20f
        const val VERTICAL_SWIPE_SLOP_RATIO = 1.5f // |ΔY| > |ΔX| * 1.5
        
        const val HORIZONTAL_SEEK_MIN_DX_PX = 30f
        const val HORIZONTAL_SEEK_SLOP_RATIO = 2.0f // |ΔX| > |ΔY| * 2.0
        const val HORIZONTAL_SEEK_MIN_ELAPSED_MS = 60L
        
        const val SINGLE_PAN_MIN_DELTA_PX = 20f
        
        // Spec: Tap & Multi-tap seek windows and timeouts
        const val DOUBLE_TAP_WINDOW_MS = 250L
        const val MULTI_TAP_CONTINUATION_WINDOW_MS = 650L
        const val TAP_SPATIAL_SLOP_PX = 100f
        const val MULTI_TAP_INACTIVITY_TIMEOUT_MS = 800L
        const val MULTI_TAP_UI_HIDE_ADDITIONAL_MS = 100L
        
        // Spec: Long-press hold duration
        const val LONG_PRESS_HOLD_MS = 500L
        const val DYNAMIC_SPEED_UNLOCK_DX_DP = 10f
        
        // Spec: Vertical swipe sensitivity
        const val BRIGHTNESS_SENSITIVITY_PER_PX = 0.001f
        // 0.002 * 100 = 0.2 vol/px → full 0–100 range in ~500px (≈ half screen height).
        // Previous value 0.017 gave 1.7 vol/px → full range in only ~59px (way too sensitive).
        const val VOLUME_SENSITIVITY_PER_PX = 0.002f
        
        // Spec: Pinch zoom range in log2 scale
        const val ZOOM_LOG2_MIN = -1.0f // 0.5x
        const val ZOOM_LOG2_MAX = 3.0f  // 8.0x
        const val ZOOM_SENSITIVITY_MULTIPLIER = 1.2f
        
        // Spec: Speed presets for dynamic scrubbing
        val SPEED_PRESETS = floatArrayOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f)
        
        // Spec: Multi-tap seek curve in seconds by tap count
        val MULTI_TAP_SEEK_CURVE_SEC = intArrayOf(10, 20, 30, 40, 50, 60)
    }

    /**
     * Entry point for pointer-down events.
     */
    fun onPointerDown(
        pointerId: Long,
        x: Float,
        y: Float,
        timeMs: Long,
        activePointerCount: Int,
        panelShown: PanelShown,
        density: Float
    ) {
        // Spec: Root gesture surface inset by 48dp dead-zone padding on all edges.
        val deadZonePx = EDGE_DEAD_ZONE_DP * density
        if (x < deadZonePx || x > controller.screenWidthPx - deadZonePx ||
            y < deadZonePx || y > controller.screenHeightPx - deadZonePx
        ) {
            return
        }

        // Rule #4: Explicit handoff rules for multi-touch arrival during active gestures.
        if (activePointerCount >= 2) {
            handleMultiPointerArrival(pointerId, x, y, activePointerCount)
            return
        }

        val region = classifyTapRegion(x)

        when (val state = currentState) {
            is GestureState.Idle -> {
                startTapCandidate(pointerId, x, y, timeMs)
            }
            is GestureState.MultiTapSeeking -> {
                // Check if within continuation window, matching region, and spatial slop
                val elapsed = timeMs - state.lastTapTimeMs
                val dist = sqrt((x - state.lastTapX) * (x - state.lastTapX) + (y - state.lastTapY) * (y - state.lastTapY))
                if (elapsed <= MULTI_TAP_CONTINUATION_WINDOW_MS && dist <= TAP_SPATIAL_SLOP_PX && region == state.lastTapRegion) {
                    controller.cancelTimer(state.inactivityTimerId)
                    controller.cancelTimer(state.hideUiTimerId)
                    
                    val newTapCount = state.tapCount + 1
                    // Spec: Seek amount by tap count: 10s/20s/30s/40s/50s/60s (clamped at last index)
                    val curveIndex = min(newTapCount - 1, MULTI_TAP_SEEK_CURVE_SEC.lastIndex)
                    val stepSeekSec = MULTI_TAP_SEEK_CURVE_SEC[curveIndex]
                    
                    // Determine direction: left half rewinds (-), right half fast-forwards (+)
                    val isForward = region == TapRegion.RIGHT
                    val directionSign = if (isForward) 1 else -1
                    
                    // Spec: Direction reversal mid-sequence: zero accumulated seek amount before starting new direction (do not subtract).
                    val wasForward = !state.isReverseDirection
                    val newAccumulatedSec = if (isForward != wasForward && state.tapCount > 0) {
                        stepSeekSec
                    } else {
                        state.accumulatedSeekSec + stepSeekSec
                    }

                    // Execute seek relative to current target
                    controller.seekTo(controller.currentPositionMs + (stepSeekSec * 1000L * directionSign), precise = false)
                    
                    val label = "${if (isForward) "+" else "-"}${newAccumulatedSec}s"
                    controller.showDoubleTapSeekOverlay(newAccumulatedSec, isForward, label)

                    // Spec: Inactivity auto-reset: 800ms after last increment -> clear seek state
                    val inactivityJob = controller.scheduleTimer(MULTI_TAP_INACTIVITY_TIMEOUT_MS) {
                        onMultiTapInactivityTimeout(timeMs)
                    }
                    val hideJob = controller.scheduleTimer(MULTI_TAP_INACTIVITY_TIMEOUT_MS + MULTI_TAP_UI_HIDE_ADDITIONAL_MS) {
                        onMultiTapUiHideTimeout(timeMs)
                    }

                    transitionTo(
                        state.copy(
                            tapCount = newTapCount,
                            accumulatedSeekSec = newAccumulatedSec,
                            lastTapTimeMs = timeMs,
                            lastTapX = x,
                            lastTapY = y,
                            isReverseDirection = !isForward,
                            inactivityTimerId = inactivityJob,
                            hideUiTimerId = hideJob
                        )
                    )
                } else {
                    // Tap was outside window or region; treat as new tap candidate from Idle
                    startTapCandidate(pointerId, x, y, timeMs)
                }
            }
            is GestureState.TapCandidate -> {
                if (state.deferredTapTimerId != null) {
                    // deferredTapTimerId is non-null only AFTER the first finger has lifted while
                    // waiting for a potential double-tap. This new pointer-down is therefore a
                    // sequential second tap, not a simultaneous second finger (pinch).
                    controller.cancelTimer(state.deferredTapTimerId)
                    controller.cancelTimer(state.longPressTimerId)
                    if (region != TapRegion.CENTER) {
                        val isForward = region == TapRegion.RIGHT
                        val directionSign = if (isForward) 1 else -1
                        val stepSeekSec = MULTI_TAP_SEEK_CURVE_SEC[0] // 10 s for the first double-tap
                        controller.seekTo(
                            controller.currentPositionMs + (stepSeekSec * 1000L * directionSign),
                            precise = false
                        )
                        val label = "${if (isForward) "+" else "-"}${stepSeekSec}s"
                        controller.showDoubleTapSeekOverlay(stepSeekSec, isForward, label)
                        val inactivityJob = controller.scheduleTimer(MULTI_TAP_INACTIVITY_TIMEOUT_MS) {
                            onMultiTapInactivityTimeout(timeMs)
                        }
                        val hideJob = controller.scheduleTimer(
                            MULTI_TAP_INACTIVITY_TIMEOUT_MS + MULTI_TAP_UI_HIDE_ADDITIONAL_MS
                        ) {
                            onMultiTapUiHideTimeout(timeMs)
                        }
                        transitionTo(
                            GestureState.MultiTapSeeking(
                                tapCount = 1,
                                accumulatedSeekSec = stepSeekSec,
                                lastTapRegion = region,
                                lastTapTimeMs = timeMs,
                                lastTapX = x,
                                lastTapY = y,
                                isReverseDirection = !isForward,
                                inactivityTimerId = inactivityJob,
                                hideUiTimerId = hideJob
                            )
                        )
                    } else {
                        // Center double-tap — start fresh
                        startTapCandidate(pointerId, x, y, timeMs)
                    }
                } else {
                    // deferredTapTimerId is null → first finger is still down; a second simultaneous
                    // finger has arrived. Hand off to pinch-to-zoom.
                    handleMultiPointerArrival(pointerId, x, y, activePointerCount)
                }
            }
            else -> {
                // Active gesture in progress; ignored or handled via multi-pointer arrival
            }
        }
    }

    /**
     * Entry point for pointer-move events.
     * Evaluates fixed decision point if in TapCandidate; otherwise dispatches to locked handler.
     */
    fun onPointerMove(
        pointerId: Long,
        x: Float,
        y: Float,
        timeMs: Long,
        activePointerCount: Int,
        panelShown: PanelShown,
        density: Float,
        span: Float = 0f,
        midpointX: Float = x,
        midpointY: Float = y
    ) {
        if (activePointerCount >= 2 && currentState !is GestureState.PinchZoomPan) {
            handleMultiPointerArrival(pointerId, x, y, activePointerCount, span, midpointX, midpointY)
            return
        }

        when (val state = currentState) {
            is GestureState.TapCandidate -> {
                val deltaX = x - state.downX
                val deltaY = y - state.downY
                val dist = sqrt(deltaX * deltaX + deltaY * deltaY)

                // Spec: Tap vs. drag: movement <= 10px while pressed = tap; >10px = drag
                var currentCandidate = state
                if (dist > TAP_MAX_MOVEMENT_PX && !state.exceededTapThreshold) {
                    controller.cancelTimer(state.longPressTimerId)
                    currentCandidate = state.copy(
                        exceededTapThreshold = true,
                        longPressTimerId = null
                    )
                    transitionTo(currentCandidate)
                }

                // Spec: Resolve ownership once, at a fixed decision point — not continuously.
                // Evaluated together at first threshold crossing (whichever fires first wins, no re-checking after).
                val nextState = classifyDragThresholds(
                    candidate = currentCandidate,
                    currentX = x,
                    currentY = y,
                    currentTimeMs = timeMs,
                    panelShown = panelShown
                )
                if (nextState != null) {
                    transitionTo(nextState)
                }
            }
            is GestureState.VerticalSwipe -> handleVerticalSwipeMove(state, y)
            is GestureState.HorizontalSeek -> handleHorizontalSeekMove(state, x, timeMs)
            is GestureState.SinglePan -> handleSinglePanMove(state, x, y)
            is GestureState.PinchZoomPan -> handlePinchZoomPanMove(state, span, midpointX, midpointY)
            is GestureState.LongPress -> handleLongPressMove(state, x, density)
            is GestureState.DynamicSpeedScrub -> handleDynamicSpeedScrubMove(state, x)
            is GestureState.Idle, is GestureState.MultiTapSeeking -> {
                // No drag handling in Idle or MultiTapSeeking
            }
        }
    }

    /**
     * Entry point for pointer-up events.
     */
    fun onPointerUp(pointerId: Long, x: Float, y: Float, timeMs: Long, activePointerCount: Int) {
        when (val state = currentState) {
            is GestureState.TapCandidate -> {
                controller.cancelTimer(state.longPressTimerId)
                if (!state.exceededTapThreshold) {
                    // Valid tap occurred. Check if this is the first tap or a second tap within double-tap window.
                    val region = classifyTapRegion(x)
                    if (region == TapRegion.CENTER) {
                        // Center tap toggles UI / triggers single tap action immediately
                        controller.triggerSingleTapAction()
                        transitionTo(GestureState.Idle)
                    } else {
                        // Schedule deferred single-tap execution to allow double-tap window
                        // Spec: Double-tap window: 250ms, spatial slop 100px.
                        controller.showTapFeedback(x, y)
                        val downTime = state.downTimeMs
                        val timerJob = controller.scheduleTimer(DOUBLE_TAP_WINDOW_MS) {
                            onDeferredSingleTapTimeout(downTime)
                        }
                        transitionTo(
                            state.copy(
                                deferredTapTimerId = timerJob,
                                longPressTimerId = null
                            )
                        )
                    }
                } else {
                    transitionTo(GestureState.Idle)
                }
            }
            is GestureState.VerticalSwipe -> {
                controller.hideVolumeOverlay()
                controller.hideBrightnessOverlay()
                transitionTo(GestureState.Idle)
            }
            is GestureState.HorizontalSeek -> {
                // Spec: On release: if it was playing before, unpause; if it was already paused, stay paused.
                // Unpause before issuing the final exact seek so playback resumption isn't blocked by decoder seek time.
                if (state.wasPlayingBeforeScrub) {
                    controller.unpause()
                }
                // Issue one final precise seek to guarantee frame accuracy on release
                controller.seekCommit(state.targetPositionMs)
                // Spec: Hide overlay 300ms after release.
                controller.hideHorizontalSeekOverlay(delayMs = 300L)
                transitionTo(GestureState.Idle)
            }
            is GestureState.SinglePan -> {
                transitionTo(GestureState.Idle)
            }
            is GestureState.PinchZoomPan -> {
                if (activePointerCount <= 1) {
                    controller.hidePinchZoomOverlay()
                    transitionTo(GestureState.Idle)
                }
            }
            is GestureState.LongPress -> {
                // Spec: On release: ramp back down to original speed over the same 5-step/16ms curve.
                controller.setPlaybackSpeedRamped(state.initialSpeed, stepCount = 5, stepDurationMs = 16L)
                controller.hideSpeedOverlay()
                transitionTo(GestureState.Idle)
            }
            is GestureState.DynamicSpeedScrub -> {
                // Spec: On release: ramp back down to original speed over the same 5-step/16ms curve.
                controller.setPlaybackSpeedRamped(state.startSpeed, stepCount = 5, stepDurationMs = 16L)
                controller.hideSpeedOverlay()
                transitionTo(GestureState.Idle)
            }
            is GestureState.MultiTapSeeking -> {
                // Remains in MultiTapSeeking state across pointer up/down until 800ms timer fires.
            }
            is GestureState.Idle -> {}
        }
    }

    /**
     * Single Decision-Point Classifier.
     * Evaluates raw deltas and returns the confirmed locked state upon first threshold crossing.
     */
    private fun classifyDragThresholds(
        candidate: GestureState.TapCandidate,
        currentX: Float,
        currentY: Float,
        currentTimeMs: Long,
        panelShown: PanelShown
    ): GestureState? {
        val deltaX = currentX - candidate.downX
        val deltaY = currentY - candidate.downY
        val absDx = abs(deltaX)
        val absDy = abs(deltaY)
        val elapsedMs = currentTimeMs - candidate.downTimeMs
        val currentZoomLog2 = controller.currentZoomLog2
        val zoomScale = max(0.5f, Math.pow(2.0, currentZoomLog2.toDouble()).toFloat())

        // Spec: Single-finger pan: same as horizontal seek but zoom > 1.0x AND |ΔX or ΔY| > 20px — takes priority over horizontal seek when zoomed.
        if (zoomScale > 1.0f && (absDx > SINGLE_PAN_MIN_DELTA_PX || absDy > SINGLE_PAN_MIN_DELTA_PX)) {
            return GestureState.SinglePan(
                startX = candidate.downX,
                startY = candidate.downY,
                prevX = currentX,
                prevY = currentY,
                panX = controller.currentPanX,
                panY = controller.currentPanY,
                currentScale = zoomScale
            )
        }

        // Spec: Vertical swipe: |ΔY| > 20px AND |ΔY| > |ΔX| * 1.5
        if (absDy > VERTICAL_SWIPE_MIN_DY_PX && absDy > absDx * VERTICAL_SWIPE_SLOP_RATIO) {
            val isRightHalf = candidate.downX > controller.screenWidthPx / 2f
            val isVolumeSide = if (controller.isVolumeSideRight) isRightHalf else !isRightHalf
            val initialVal = if (isVolumeSide) controller.volume else controller.brightness
            val isBoost = isVolumeSide && initialVal > controller.maxStandardVolume

            if (isVolumeSide) {
                controller.showVolumeOverlay(initialVal.roundToInt())
            } else {
                controller.showBrightnessOverlay((initialVal * 100f).roundToInt())
            }

            return GestureState.VerticalSwipe(
                isVolumeSide = isVolumeSide,
                startX = candidate.downX,
                startY = candidate.downY,
                currentY = currentY,
                anchorY = candidate.downY, // Initial anchor at touch down
                isBoostRegime = isBoost,
                initialValue = initialVal,
                currentValue = initialVal
            )
        }

        // Spec: Horizontal seek: |ΔX| > 30px AND |ΔX| > |ΔY| * 2.0 AND elapsed time since down > 60ms AND zoom <= 1.0x AND no panel open.
        if (absDx > HORIZONTAL_SEEK_MIN_DX_PX && absDx > absDy * HORIZONTAL_SEEK_SLOP_RATIO &&
            elapsedMs > HORIZONTAL_SEEK_MIN_ELAPSED_MS && zoomScale <= 1.0f && panelShown == PanelShown.NONE
        ) {
            val wasPaused = controller.isPaused
            // Do NOT pause playback on scrub start — let it keep playing while the user drags.
            // wasPlayingBeforeScrub is still tracked so onPointerUp knows whether to unpause or not.

            val duration = max(1L, controller.durationMs)
            val initialPos = controller.currentPositionMs
            // Calculate sensitivity: full screen width represents 90s or proportional to duration
            val sensitivity = max(0.1f, (duration / controller.screenWidthPx) * 0.15f)

            // Spec: Critical bug to avoid: do not use raw/unreset ΔX accumulated from original touch-down origin once gesture is confirmed —
            // reset delta baseline at moment of gesture confirmation so small subsequent move doesn't get multiplied against stale large ΔX.
            val confirmedBaselineX = currentX

            val initialState = GestureState.HorizontalSeek(
                initialVideoPositionMs = initialPos,
                durationMs = duration,
                wasPlayingBeforeScrub = !wasPaused,
                confirmationX = confirmedBaselineX,
                currentX = currentX,
                targetPositionMs = initialPos,
                sensitivityMsPerPx = sensitivity
            )
            updateHorizontalSeekUi(initialState)
            return initialState
        }

        return null
    }

    /**
     * Rule #4 Explicit Handoff Rules for multi-pointer arrivals.
     */
    private fun handleMultiPointerArrival(
        pointerId: Long,
        x: Float,
        y: Float,
        activePointerCount: Int,
        span: Float = 100f,
        midpointX: Float = x,
        midpointY: Float = y
    ) {
        when (val state = currentState) {
            is GestureState.SinglePan, is GestureState.HorizontalSeek -> {
                // Spec: A second pointer arriving while SinglePan or HorizontalSeek is locked immediately transitions to PinchZoomPan
                // (cancel single-finger state cleanly first: restore pause state, hide overlays).
                if (state is GestureState.HorizontalSeek) {
                    if (state.wasPlayingBeforeScrub) {
                        controller.unpause()
                    }
                    controller.hideHorizontalSeekOverlay()
                }
                transitionTo(
                    GestureState.PinchZoomPan(
                        initialSpan = max(1f, span),
                        prevSpan = max(1f, span),
                        initialZoomLog2 = controller.currentZoomLog2,
                        currentZoomLog2 = controller.currentZoomLog2,
                        prevMidpointX = midpointX,
                        prevMidpointY = midpointY,
                        panX = controller.currentPanX,
                        panY = controller.currentPanY
                    )
                )
            }
            is GestureState.VerticalSwipe -> {
                // Spec: A second pointer arriving while VerticalSwipe is locked cancels it outright (does not hand off).
                controller.hideVolumeOverlay()
                controller.hideBrightnessOverlay()
                transitionTo(GestureState.Idle)
            }
            is GestureState.TapCandidate -> {
                controller.cancelTimer(state.longPressTimerId)
                controller.cancelTimer(state.deferredTapTimerId)
                transitionTo(
                    GestureState.PinchZoomPan(
                        initialSpan = max(1f, span),
                        prevSpan = max(1f, span),
                        initialZoomLog2 = controller.currentZoomLog2,
                        currentZoomLog2 = controller.currentZoomLog2,
                        prevMidpointX = midpointX,
                        prevMidpointY = midpointY,
                        panX = controller.currentPanX,
                        panY = controller.currentPanY
                    )
                )
            }
            is GestureState.PinchZoomPan -> {
                // Already in PinchZoomPan; update span reference
            }
            else -> {
                transitionTo(GestureState.Idle)
            }
        }
    }

    // --- State-specific Move Handlers ---

    private fun handleVerticalSwipeMove(state: GestureState.VerticalSwipe, currentY: Float) {
        val deltaY = currentY - state.anchorY
        
        if (state.isVolumeSide) {
            // Spec: Volume: 0.017 per pixel in both the standard (0–100) and boosted (100–100+cap) regimes.
            // In Android UI, dragging UP decreases Y coordinate, so negative deltaY increases volume.
            val rawOffset = -deltaY * VOLUME_SENSITIVITY_PER_PX * 100f
            var newVolume = state.initialValue + rawOffset
            
            // Spec: Regime transition (100% crossing into boost): anchor a NEW Y baseline at the exact frame of crossing so there's no jump.
            val standardMax = controller.maxStandardVolume
            val boostMax = controller.maxBoostVolume
            
            var newBoostRegime = state.isBoostRegime
            var newAnchorY = state.anchorY
            var newInitialValue = state.initialValue
            
            if (!state.isBoostRegime && newVolume >= standardMax && boostMax > standardMax) {
                // Crossed from standard into boost regime
                newBoostRegime = true
                newAnchorY = currentY
                newInitialValue = standardMax
                newVolume = standardMax
            } else if (state.isBoostRegime && newVolume <= standardMax) {
                // Crossed from boost back into standard regime
                newBoostRegime = false
                newAnchorY = currentY
                newInitialValue = standardMax
                newVolume = standardMax
            }
            
            val clampedVolume = max(0f, min(newVolume, boostMax))
            controller.setVolume(clampedVolume)
            controller.showVolumeOverlay(clampedVolume.roundToInt())
            
            if (newBoostRegime != state.isBoostRegime || newAnchorY != state.anchorY) {
                transitionTo(
                    state.copy(
                        currentY = currentY,
                        anchorY = newAnchorY,
                        isBoostRegime = newBoostRegime,
                        initialValue = newInitialValue,
                        currentValue = clampedVolume
                    )
                )
            } else {
                transitionTo(state.copy(currentY = currentY, currentValue = clampedVolume))
            }
        } else {
            // Spec: Brightness: 0.001 units per pixel, clamped [0, 1].
            val rawOffset = -deltaY * BRIGHTNESS_SENSITIVITY_PER_PX
            val newBrightness = max(0f, min(state.initialValue + rawOffset, 1.0f))
            controller.setBrightness(newBrightness)
            controller.showBrightnessOverlay((newBrightness * 100f).roundToInt())
            transitionTo(state.copy(currentY = currentY, currentValue = newBrightness))
        }
    }

    private fun handleHorizontalSeekMove(state: GestureState.HorizontalSeek, currentX: Float, timeMs: Long) {
        // Spec: Live preview: targetMs = clamp(initialPositionMs + ΔX * sensitivity, 0, durationMs), seek continuously as finger moves.
        // Reads from confirmed baseline (confirmationX), not original pointer-down origin.
        val deltaX = currentX - state.confirmationX
        val deltaMs = (deltaX * state.sensitivityMsPerPx).roundToLong()
        val targetMs = max(0L, min(state.initialVideoPositionMs + deltaMs, state.durationMs))
        
        var newLastSeekIssuedAtMs = state.lastSeekIssuedAtMs
        val deltaXSinceLastSeek = abs(currentX - state.currentX)
        // 16ms ≈ one 60Hz frame for fast drags; 24ms for slow drags. Keeps seek commands in sync
        // with display refresh so there is no visible stutter between finger and decoded frame.
        val throttleMs = if (deltaXSinceLastSeek > 20f) 16L else 24L
        if (timeMs - state.lastSeekIssuedAtMs >= throttleMs) {
            controller.seekGesture(targetMs)
            newLastSeekIssuedAtMs = timeMs
        }
        
        val updatedState = state.copy(
            currentX = currentX,
            targetPositionMs = targetMs,
            lastSeekIssuedAtMs = newLastSeekIssuedAtMs
        )
        updateHorizontalSeekUi(updatedState)
        transitionTo(updatedState)
    }

    private fun updateHorizontalSeekUi(state: GestureState.HorizontalSeek) {
        val deltaMs = state.targetPositionMs - state.initialVideoPositionMs
        val sign = if (deltaMs >= 0) "+" else "-"
        val absDeltaSec = abs(deltaMs) / 1000L
        
        // Spec: Time formatting: H:MM:SS if >=3600s else MM:SS. Delta always signed (+01:15 / -00:45).
        val currentStr = formatTime(state.targetPositionMs / 1000L)
        val deltaStr = "$sign${formatTime(absDeltaSec)}"
        controller.showHorizontalSeekOverlay(currentStr, deltaStr, state.targetPositionMs)
    }

    private fun handleSinglePanMove(state: GestureState.SinglePan, currentX: Float, currentY: Float) {
        val deltaX = currentX - state.prevX
        val deltaY = currentY - state.prevY
        
        // Spec: normalize by (display size * current scale), smooth with EMA α=0.5, clamp pan offset to max(0, (S-1)/(2S)) per axis.
        val normDx = deltaX / (controller.screenWidthPx * state.currentScale)
        val normDy = deltaY / (controller.screenHeightPx * state.currentScale)
        
        val alpha = 0.5f
        val smoothedPanX = state.panX + alpha * normDx
        val smoothedPanY = state.panY + alpha * normDy
        
        val maxPan = max(0f, (state.currentScale - 1f) / (2f * state.currentScale))
        val clampedPanX = max(-maxPan, min(smoothedPanX, maxPan))
        val clampedPanY = max(-maxPan, min(smoothedPanY, maxPan))
        
        controller.setZoomAndPan(controller.currentZoomLog2, clampedPanX, clampedPanY)
        transitionTo(
            state.copy(
                prevX = currentX,
                prevY = currentY,
                panX = clampedPanX,
                panY = clampedPanY
            )
        )
    }

    private fun handlePinchZoomPanMove(state: GestureState.PinchZoomPan, currentSpan: Float, midpointX: Float, midpointY: Float) {
        // Spec: Zoom: Δzoom = ln(currentDist/prevDist) * 1.2, accumulated, clamped to log2-scale range [-1.0, 3.0]
        val validPrevSpan = max(1f, state.prevSpan)
        val validCurrentSpan = max(1f, currentSpan)
        val deltaZoom = ln(validCurrentSpan / validPrevSpan) * ZOOM_SENSITIVITY_MULTIPLIER
        
        val newZoomLog2 = max(ZOOM_LOG2_MIN, min(state.currentZoomLog2 + deltaZoom, ZOOM_LOG2_MAX))
        val scale = max(0.5f, Math.pow(2.0, newZoomLog2.toDouble()).toFloat())
        
        // Spec: Simultaneous 2-finger pan: track pointer midpoint delta, normalize by (display size * current scale), smooth with EMA α=0.5
        val deltaX = midpointX - state.prevMidpointX
        val deltaY = midpointY - state.prevMidpointY
        val normDx = deltaX / (controller.screenWidthPx * scale)
        val normDy = deltaY / (controller.screenHeightPx * scale)
        
        val alpha = 0.5f
        val smoothedPanX = state.panX + alpha * normDx
        val smoothedPanY = state.panY + alpha * normDy
        
        val maxPan = max(0f, (scale - 1f) / (2f * scale))
        val clampedPanX = max(-maxPan, min(smoothedPanX, maxPan))
        val clampedPanY = max(-maxPan, min(smoothedPanY, maxPan))
        
        controller.setZoomAndPan(newZoomLog2, clampedPanX, clampedPanY)
        val percentage = (scale * 100f).roundToInt()
        controller.showPinchZoomOverlay(percentage)
        
        transitionTo(
            state.copy(
                prevSpan = validCurrentSpan,
                currentZoomLog2 = newZoomLog2,
                prevMidpointX = midpointX,
                prevMidpointY = midpointY,
                panX = clampedPanX,
                panY = clampedPanY
            )
        )
    }

    private fun handleLongPressMove(state: GestureState.LongPress, currentX: Float, density: Float) {
        if (!state.isDynamicOverlayEnabled) return
        
        val deltaX = currentX - state.startX
        val thresholdPx = DYNAMIC_SPEED_UNLOCK_DX_DP * density
        
        // Spec: If dynamic overlay enabled: horizontal drag >10dp unlocks interactive preset scrubbing across [0.25, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0]
        if (abs(deltaX) > thresholdPx) {
            val startPresetIdx = findClosestPresetIndex(state.initialSpeed)
            val scrubState = GestureState.DynamicSpeedScrub(
                startX = state.startX,
                startSpeed = state.initialSpeed,
                lastAppliedSpeed = state.initialSpeed,
                startPresetIndex = startPresetIdx
            )
            transitionTo(scrubState)
            handleDynamicSpeedScrubMove(scrubState, currentX)
        }
    }

    private fun handleDynamicSpeedScrubMove(state: GestureState.DynamicSpeedScrub, currentX: Float) {
        val deltaX = currentX - state.startX
        // Spec: mapped via ΔX/screenWidth * 7 * 3.5 offset from starting preset index, clamped to [0,7].
        val offset = (deltaX / controller.screenWidthPx) * 7f * 3.5f
        val rawIndex = (state.startPresetIndex + offset).roundToInt()
        val clampedIndex = max(0, min(rawIndex, SPEED_PRESETS.lastIndex))
        val targetSpeed = SPEED_PRESETS[clampedIndex]
        
        if (targetSpeed != state.lastAppliedSpeed) {
            // Spec: Step-ramp speed changes over 5 steps at 16ms/step (~80ms total) — never snap instantly.
            controller.setPlaybackSpeedRamped(targetSpeed, stepCount = 5, stepDurationMs = 16L)
            controller.showSpeedOverlay(targetSpeed, interactiveSliderIndex = clampedIndex)
            transitionTo(state.copy(lastAppliedSpeed = targetSpeed))
        }
    }

    // --- Timers & Rule #4 Guarded Callbacks ---

    private fun startTapCandidate(pointerId: Long, x: Float, y: Float, timeMs: Long) {
        val candidate = GestureState.TapCandidate(
            downX = x,
            downY = y,
            downTimeMs = timeMs,
            pointerId = pointerId
        )
        transitionTo(candidate)
        
        // Spec: Long-press: no movement >10px sustained for 500ms (time-based, races against drag classifiers).
        val longPressJob = controller.scheduleTimer(LONG_PRESS_HOLD_MS) {
            onLongPressTimeout(timeMs)
        }
        transitionTo(candidate.copy(longPressTimerId = longPressJob))
    }

    /**
     * Rule #4: Timer-driven resets must check current state before acting, never assume it.
     */
    fun onLongPressTimeout(expectedDownTimeMs: Long) {
        val state = currentState as? GestureState.TapCandidate ?: return
        if (state.downTimeMs != expectedDownTimeMs || state.exceededTapThreshold) return
        
        // Activate long press
        // Spec: Long-press firing must suppress a pending deferred single-tap on release (track as one boolean owned by state machine).
        val initialSpeed = 1.0f // Or fetch from controller if variable speed supported
        val longPressState = GestureState.LongPress(
            startX = state.downX,
            downTimeMs = state.downTimeMs,
            initialSpeed = initialSpeed,
            currentSpeed = initialSpeed * 2.0f, // Example 2x fast forward on long press
            isDynamicOverlayEnabled = controller.isDynamicSpeedOverlayEnabled,
            suppressedDeferredTap = true
        )
        controller.setPlaybackSpeedRamped(longPressState.currentSpeed, stepCount = 5, stepDurationMs = 16L)
        controller.showSpeedOverlay(longPressState.currentSpeed)
        transitionTo(longPressState)
    }

    /**
     * Rule #4: Guarded callback for deferred single tap execution.
     */
    fun onDeferredSingleTapTimeout(expectedDownTimeMs: Long) {
        val state = currentState as? GestureState.TapCandidate ?: return
        if (state.downTimeMs != expectedDownTimeMs) return
        
        controller.triggerSingleTapAction()
        transitionTo(GestureState.Idle)
    }

    /**
     * Rule #4: Guarded callback for multi-tap inactivity timeout (800ms).
     */
    fun onMultiTapInactivityTimeout(expectedLastTapTimeMs: Long) {
        val state = currentState as? GestureState.MultiTapSeeking ?: return
        if (state.lastTapTimeMs != expectedLastTapTimeMs) return
        
        // Spec: 800ms after last increment -> clear seek state
        transitionTo(GestureState.Idle)
    }

    /**
     * Rule #4: Guarded callback for hiding multi-tap UI (+100ms more).
     */
    fun onMultiTapUiHideTimeout(expectedLastTapTimeMs: Long) {
        // If state is still MultiTapSeeking with same tap timestamp, or if we just transitioned to Idle from that sequence
        controller.hideDoubleTapSeekOverlay()
    }

    // --- Helper Methods ---

    private fun transitionTo(newState: GestureState) {
        currentState = newState
    }

    private fun classifyTapRegion(x: Float): TapRegion {
        // Spec: Horizontal tap regions: left 0–30%, center 30–70%, right 70–100% of width (config: doubleTapSeekAreaWidth, default 30).
        val leftBoundary = controller.screenWidthPx * (controller.doubleTapSeekAreaWidthPercent / 100f)
        val rightBoundary = controller.screenWidthPx * ((100 - controller.doubleTapSeekAreaWidthPercent) / 100f)
        return when {
            x <= leftBoundary -> TapRegion.LEFT
            x >= rightBoundary -> TapRegion.RIGHT
            else -> TapRegion.CENTER
        }
    }

    private fun findClosestPresetIndex(speed: Float): Int {
        var minDiff = Float.MAX_VALUE
        var bestIdx = 0
        for (i in SPEED_PRESETS.indices) {
            val diff = abs(SPEED_PRESETS[i] - speed)
            if (diff < minDiff) {
                minDiff = diff
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun formatTime(totalSec: Long): String {
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
