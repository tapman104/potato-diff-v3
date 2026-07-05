package com.tapman104.mpvplayer.player.gesture

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

enum class TapRegion {
    LEFT,
    CENTER,
    RIGHT
}

enum class PanelShown {
    NONE,
    AUDIO_SETTINGS,
    SUBTITLES,
    OTHER
}

interface MpvPlayerController {
    val durationMs: Long
    val currentPositionMs: Long
    val isPaused: Boolean
    val currentZoomLog2: Float
    val currentPanX: Float
    val currentPanY: Float
    val volume: Float
    val maxStandardVolume: Float
    val maxBoostVolume: Float
    val brightness: Float
    val screenWidthPx: Float
    val screenHeightPx: Float
    val isVolumeSideRight: Boolean
    val doubleTapSeekAreaWidthPercent: Int
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

    fun scheduleTimer(delayMs: Long, action: () -> Unit): Any
    fun cancelTimer(timerId: Any?)
    fun triggerSingleTapAction()
}

sealed class GestureState {
    object Idle : GestureState()

    data class TapCandidate(
        val downX: Float,
        val downY: Float,
        val downTimeMs: Long,
        val deferredTapTimerId: Any? = null,
        val longPressTimerId: Any? = null,
        val exceededTapThreshold: Boolean = false
    ) : GestureState()

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

    data class LongPress(
        val startX: Float,
        val downTimeMs: Long,
        val initialSpeed: Float,
        val isDynamicOverlayEnabled: Boolean
    ) : GestureState()

    data class DynamicSpeedScrub(
        val startX: Float,
        val startSpeed: Float,
        val lastAppliedSpeed: Float,
        val startPresetIndex: Int
    ) : GestureState()

    data class VerticalSwipe(
        val isVolumeSide: Boolean,
        val currentY: Float,
        val anchorY: Float,
        val isBoostRegime: Boolean,
        val initialValue: Float,
        val currentValue: Float
    ) : GestureState()

    data class PinchZoomPan(
        val prevSpan: Float,
        val currentZoomLog2: Float,
        val prevMidpointX: Float,
        val prevMidpointY: Float,
        val panX: Float,
        val panY: Float,
        val lastReportedZoomPct: Int = -1
    ) : GestureState()

    data class SinglePan(
        val prevX: Float,
        val prevY: Float,
        val panX: Float,
        val panY: Float,
        val currentScale: Float
    ) : GestureState()

    data class HorizontalSeek(
        val initialVideoPositionMs: Long,
        val durationMs: Long,
        val wasPlayingBeforeScrub: Boolean,
        val confirmationX: Float,
        val currentX: Float,
        val targetPositionMs: Long,
        val sensitivityMsPerPx: Float,
        val lastSeekIssuedAtMs: Long = 0L
    ) : GestureState()
}

class MpvGestureStateMachine(private val controller: MpvPlayerController) {

    @Volatile
    var currentState: GestureState = GestureState.Idle
        private set

    companion object {
        const val EDGE_DEAD_ZONE_DP = 48f
        const val TAP_MAX_MOVEMENT_PX = 10f
        const val VERTICAL_SWIPE_MIN_DY_PX = 20f
        const val VERTICAL_SWIPE_SLOP_RATIO = 1.5f
        const val HORIZONTAL_SEEK_MIN_DX_PX = 30f
        const val HORIZONTAL_SEEK_SLOP_RATIO = 2.0f
        const val HORIZONTAL_SEEK_MIN_ELAPSED_MS = 60L
        const val SINGLE_PAN_MIN_DELTA_PX = 20f
        const val DOUBLE_TAP_WINDOW_MS = 250L
        const val MULTI_TAP_CONTINUATION_WINDOW_MS = 650L
        const val TAP_SPATIAL_SLOP_PX = 100f
        const val MULTI_TAP_INACTIVITY_TIMEOUT_MS = 800L
        const val MULTI_TAP_UI_HIDE_ADDITIONAL_MS = 100L
        const val LONG_PRESS_HOLD_MS = 500L
        const val DYNAMIC_SPEED_UNLOCK_DX_DP = 10f
        const val BRIGHTNESS_SENSITIVITY_PER_PX = 0.001f
        const val VOLUME_SENSITIVITY_PER_PX = 0.002f
        const val ZOOM_LOG2_MIN = -1.0f
        const val ZOOM_LOG2_MAX = 3.0f
        const val ZOOM_SENSITIVITY_MULTIPLIER = 1.2f
        val SPEED_PRESETS = floatArrayOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f)
        val MULTI_TAP_SEEK_CURVE_SEC = intArrayOf(10, 20, 30, 40, 50, 60)
    }

    fun onPointerDown(
        pointerId: Long,
        x: Float,
        y: Float,
        timeMs: Long,
        activePointerCount: Int,
        panelShown: PanelShown,
        density: Float
    ) {
        val deadZonePx = EDGE_DEAD_ZONE_DP * density
        if (x < deadZonePx || x > controller.screenWidthPx - deadZonePx ||
            y < deadZonePx || y > controller.screenHeightPx - deadZonePx
        ) {
            return
        }

        if (activePointerCount >= 2) {
            handleMultiPointerArrival(pointerId, x, y, activePointerCount)
            return
        }

        val region = classifyTapRegion(x)

        when (val state = currentState) {
            is GestureState.Idle -> {
                startTapCandidate(x, y, timeMs)
            }
            is GestureState.MultiTapSeeking -> {
                val elapsed = timeMs - state.lastTapTimeMs
                val dist = sqrt((x - state.lastTapX) * (x - state.lastTapX) + (y - state.lastTapY) * (y - state.lastTapY))
                if (elapsed <= MULTI_TAP_CONTINUATION_WINDOW_MS && dist <= TAP_SPATIAL_SLOP_PX && region == state.lastTapRegion) {
                    controller.cancelTimer(state.inactivityTimerId)
                    controller.cancelTimer(state.hideUiTimerId)

                    val newTapCount = state.tapCount + 1
                    val curveIndex = min(newTapCount - 1, MULTI_TAP_SEEK_CURVE_SEC.lastIndex)
                    val stepSeekSec = MULTI_TAP_SEEK_CURVE_SEC[curveIndex]

                    val isForward = region == TapRegion.RIGHT
                    val directionSign = if (isForward) 1 else -1

                    val wasForward = !state.isReverseDirection
                    val newAccumulatedSec = if (isForward != wasForward && state.tapCount > 0) {
                        stepSeekSec
                    } else {
                        state.accumulatedSeekSec + stepSeekSec
                    }

                    controller.seekTo(controller.currentPositionMs + (stepSeekSec * 1000L * directionSign), precise = false)

                    val label = "${if (isForward) "+" else "-"}${newAccumulatedSec}s"
                    controller.showDoubleTapSeekOverlay(newAccumulatedSec, isForward, label)

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
                    startTapCandidate(x, y, timeMs)
                }
            }
            is GestureState.TapCandidate -> {
                if (state.deferredTapTimerId != null) {
                    controller.cancelTimer(state.deferredTapTimerId)
                    controller.cancelTimer(state.longPressTimerId)
                    if (region != TapRegion.CENTER) {
                        val isForward = region == TapRegion.RIGHT
                        val directionSign = if (isForward) 1 else -1
                        val stepSeekSec = MULTI_TAP_SEEK_CURVE_SEC[0]
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
                        startTapCandidate(x, y, timeMs)
                    }
                } else {
                    handleMultiPointerArrival(pointerId, x, y, activePointerCount)
                }
            }
            else -> {}
        }
    }

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

                var currentCandidate = state
                if (dist > TAP_MAX_MOVEMENT_PX && !state.exceededTapThreshold) {
                    controller.cancelTimer(state.longPressTimerId)
                    currentCandidate = state.copy(
                        exceededTapThreshold = true,
                        longPressTimerId = null
                    )
                    transitionTo(currentCandidate)
                }

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
            is GestureState.Idle, is GestureState.MultiTapSeeking -> {}
        }
    }

    fun onPointerUp(pointerId: Long, x: Float, y: Float, timeMs: Long, activePointerCount: Int) {
        when (val state = currentState) {
            is GestureState.TapCandidate -> {
                controller.cancelTimer(state.longPressTimerId)
                if (!state.exceededTapThreshold) {
                    val region = classifyTapRegion(x)
                    if (region == TapRegion.CENTER) {
                        controller.triggerSingleTapAction()
                        transitionTo(GestureState.Idle)
                    } else {
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
                if (state.wasPlayingBeforeScrub) {
                    controller.unpause()
                }
                controller.seekCommit(state.targetPositionMs)
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
                controller.setPlaybackSpeedRamped(state.initialSpeed, stepCount = 5, stepDurationMs = 16L)
                controller.hideSpeedOverlay()
                transitionTo(GestureState.Idle)
            }
            is GestureState.DynamicSpeedScrub -> {
                controller.setPlaybackSpeedRamped(state.startSpeed, stepCount = 5, stepDurationMs = 16L)
                controller.hideSpeedOverlay()
                transitionTo(GestureState.Idle)
            }
            is GestureState.MultiTapSeeking, is GestureState.Idle -> {}
        }
    }

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

        if (zoomScale > 1.0f && (absDx > SINGLE_PAN_MIN_DELTA_PX || absDy > SINGLE_PAN_MIN_DELTA_PX)) {
            return GestureState.SinglePan(
                prevX = currentX,
                prevY = currentY,
                panX = controller.currentPanX,
                panY = controller.currentPanY,
                currentScale = zoomScale
            )
        }

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
                currentY = currentY,
                anchorY = candidate.downY,
                isBoostRegime = isBoost,
                initialValue = initialVal,
                currentValue = initialVal
            )
        }

        if (absDx > HORIZONTAL_SEEK_MIN_DX_PX && absDx > absDy * HORIZONTAL_SEEK_SLOP_RATIO &&
            elapsedMs > HORIZONTAL_SEEK_MIN_ELAPSED_MS && zoomScale <= 1.0f && panelShown == PanelShown.NONE
        ) {
            val wasPaused = controller.isPaused
            val duration = max(1L, controller.durationMs)
            val initialPos = controller.currentPositionMs
            val sensitivity = max(0.1f, (duration / controller.screenWidthPx) * 0.15f)

            val initialState = GestureState.HorizontalSeek(
                initialVideoPositionMs = initialPos,
                durationMs = duration,
                wasPlayingBeforeScrub = !wasPaused,
                confirmationX = currentX,
                currentX = currentX,
                targetPositionMs = initialPos,
                sensitivityMsPerPx = sensitivity
            )
            updateHorizontalSeekUi(initialState)
            return initialState
        }

        return null
    }

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
                if (state is GestureState.HorizontalSeek) {
                    if (state.wasPlayingBeforeScrub) {
                        controller.unpause()
                    }
                    controller.hideHorizontalSeekOverlay()
                }
                transitionTo(
                    GestureState.PinchZoomPan(
                        prevSpan = max(1f, span),
                        currentZoomLog2 = controller.currentZoomLog2,
                        prevMidpointX = midpointX,
                        prevMidpointY = midpointY,
                        panX = controller.currentPanX,
                        panY = controller.currentPanY
                    )
                )
            }
            is GestureState.VerticalSwipe -> {
                controller.hideVolumeOverlay()
                controller.hideBrightnessOverlay()
                transitionTo(GestureState.Idle)
            }
            is GestureState.TapCandidate -> {
                controller.cancelTimer(state.longPressTimerId)
                controller.cancelTimer(state.deferredTapTimerId)
                transitionTo(
                    GestureState.PinchZoomPan(
                        prevSpan = max(1f, span),
                        currentZoomLog2 = controller.currentZoomLog2,
                        prevMidpointX = midpointX,
                        prevMidpointY = midpointY,
                        panX = controller.currentPanX,
                        panY = controller.currentPanY
                    )
                )
            }
            is GestureState.PinchZoomPan -> {}
            else -> {
                transitionTo(GestureState.Idle)
            }
        }
    }

    private fun handleVerticalSwipeMove(state: GestureState.VerticalSwipe, currentY: Float) {
        val deltaY = currentY - state.anchorY

        if (state.isVolumeSide) {
            val rawOffset = -deltaY * VOLUME_SENSITIVITY_PER_PX * 100f
            var newVolume = state.initialValue + rawOffset

            val standardMax = controller.maxStandardVolume
            val boostMax = controller.maxBoostVolume

            var newBoostRegime = state.isBoostRegime
            var newAnchorY = state.anchorY
            var newInitialValue = state.initialValue

            if (!state.isBoostRegime && newVolume >= standardMax && boostMax > standardMax) {
                newBoostRegime = true
                newAnchorY = currentY
                newInitialValue = standardMax
                newVolume = standardMax
            } else if (state.isBoostRegime && newVolume <= standardMax) {
                newBoostRegime = false
                newAnchorY = currentY
                newInitialValue = standardMax
                newVolume = standardMax
            }

            val clampedVolume = max(0f, min(newVolume, boostMax))
            controller.setVolume(clampedVolume)

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
            val rawOffset = -deltaY * BRIGHTNESS_SENSITIVITY_PER_PX
            val newBrightness = max(0f, min(state.initialValue + rawOffset, 1.0f))
            controller.setBrightness(newBrightness)
            transitionTo(state.copy(currentY = currentY, currentValue = newBrightness))
        }
    }

    private fun handleHorizontalSeekMove(state: GestureState.HorizontalSeek, currentX: Float, timeMs: Long) {
        val deltaX = currentX - state.confirmationX
        val deltaMs = (deltaX * state.sensitivityMsPerPx).roundToLong()
        val targetMs = max(0L, min(state.initialVideoPositionMs + deltaMs, state.durationMs))

        var newLastSeekIssuedAtMs = state.lastSeekIssuedAtMs
        val deltaXSinceLastSeek = abs(currentX - state.currentX)
        val throttleMs = when {
            deltaXSinceLastSeek > 40f -> 16L
            deltaXSinceLastSeek > 10f -> 24L
            else -> 48L
        }
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

        val currentStr = formatTime(state.targetPositionMs / 1000L)
        val deltaStr = "$sign${formatTime(absDeltaSec)}"
        controller.showHorizontalSeekOverlay(currentStr, deltaStr, state.targetPositionMs)
    }

    private fun handleSinglePanMove(state: GestureState.SinglePan, currentX: Float, currentY: Float) {
        val deltaX = currentX - state.prevX
        val deltaY = currentY - state.prevY

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
        val validPrevSpan = max(1f, state.prevSpan)
        val validCurrentSpan = max(1f, currentSpan)
        val deltaZoom = ln(validCurrentSpan / validPrevSpan) * ZOOM_SENSITIVITY_MULTIPLIER

        val newZoomLog2 = max(ZOOM_LOG2_MIN, min(state.currentZoomLog2 + deltaZoom, ZOOM_LOG2_MAX))
        val scale = max(0.5f, Math.pow(2.0, newZoomLog2.toDouble()).toFloat())

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
        if (percentage != state.lastReportedZoomPct) {
            controller.showPinchZoomOverlay(percentage)
        }

        transitionTo(
            state.copy(
                prevSpan = validCurrentSpan,
                currentZoomLog2 = newZoomLog2,
                prevMidpointX = midpointX,
                prevMidpointY = midpointY,
                panX = clampedPanX,
                panY = clampedPanY,
                lastReportedZoomPct = percentage
            )
        )
    }

    private fun handleLongPressMove(state: GestureState.LongPress, currentX: Float, density: Float) {
        if (!state.isDynamicOverlayEnabled) return

        val deltaX = currentX - state.startX
        val thresholdPx = DYNAMIC_SPEED_UNLOCK_DX_DP * density

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
        val offset = (deltaX / controller.screenWidthPx) * 7f * 3.5f
        val rawIndex = (state.startPresetIndex + offset).roundToInt()
        val clampedIndex = max(0, min(rawIndex, SPEED_PRESETS.lastIndex))
        val targetSpeed = SPEED_PRESETS[clampedIndex]

        if (targetSpeed != state.lastAppliedSpeed) {
            controller.setPlaybackSpeedRamped(targetSpeed, stepCount = 5, stepDurationMs = 16L)
            controller.showSpeedOverlay(targetSpeed, interactiveSliderIndex = clampedIndex)
            transitionTo(state.copy(lastAppliedSpeed = targetSpeed))
        }
    }

    private fun startTapCandidate(x: Float, y: Float, timeMs: Long) {
        val candidate = GestureState.TapCandidate(
            downX = x,
            downY = y,
            downTimeMs = timeMs
        )
        transitionTo(candidate)

        val longPressJob = controller.scheduleTimer(LONG_PRESS_HOLD_MS) {
            onLongPressTimeout(timeMs)
        }
        transitionTo(candidate.copy(longPressTimerId = longPressJob))
    }

    fun onLongPressTimeout(expectedDownTimeMs: Long) {
        val state = currentState as? GestureState.TapCandidate ?: return
        if (state.downTimeMs != expectedDownTimeMs || state.exceededTapThreshold) return

        val initialSpeed = 1.0f
        val targetSpeed = initialSpeed * 2.0f
        val longPressState = GestureState.LongPress(
            startX = state.downX,
            downTimeMs = state.downTimeMs,
            initialSpeed = initialSpeed,
            isDynamicOverlayEnabled = controller.isDynamicSpeedOverlayEnabled
        )
        controller.setPlaybackSpeedRamped(targetSpeed, stepCount = 5, stepDurationMs = 16L)
        controller.showSpeedOverlay(targetSpeed)
        transitionTo(longPressState)
    }

    fun onDeferredSingleTapTimeout(expectedDownTimeMs: Long) {
        val state = currentState as? GestureState.TapCandidate ?: return
        if (state.downTimeMs != expectedDownTimeMs) return

        controller.triggerSingleTapAction()
        transitionTo(GestureState.Idle)
    }

    fun onMultiTapInactivityTimeout(expectedLastTapTimeMs: Long) {
        val state = currentState as? GestureState.MultiTapSeeking ?: return
        if (state.lastTapTimeMs != expectedLastTapTimeMs) return

        transitionTo(GestureState.Idle)
    }

    fun onMultiTapUiHideTimeout(expectedLastTapTimeMs: Long) {
        val state = currentState as? GestureState.MultiTapSeeking ?: return
        if (state.lastTapTimeMs != expectedLastTapTimeMs) return
        controller.hideDoubleTapSeekOverlay()
    }

    private fun transitionTo(newState: GestureState) {
        currentState = newState
    }

    private fun classifyTapRegion(x: Float): TapRegion {
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
