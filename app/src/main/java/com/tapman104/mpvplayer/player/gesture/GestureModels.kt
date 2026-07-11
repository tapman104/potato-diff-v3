package com.tapman104.mpvplayer.player.gesture

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
    val playbackSpeed: Float

    fun pause()
    fun unpause()
    fun seekForward(offsetMs: Long)
    fun seekBackward(offsetMs: Long)
    fun seekGestureDrag(positionMs: Long)
    fun seekCommit(positionMs: Long)
    fun setPlaybackSpeedRamped(targetSpeed: Float, stepCount: Int = 5, stepDurationMs: Long = 16L)
    fun restorePlaybackSpeed()
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
    data object Idle : GestureState()

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
        val inactivityTimerId: Any? = null
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
        val currentScale: Float,
        val zoomLog2: Float
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
