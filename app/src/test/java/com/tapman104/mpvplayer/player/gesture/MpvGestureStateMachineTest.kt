package com.tapman104.mpvplayer.player.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMpvPlayerController : MpvPlayerController {
    var pauseCallCount = 0
    var unpauseCallCount = 0
    var lastSeekToMs: Long? = null
    var lastSeekCommitMs: Long? = null

    data class DoubleTapArgs(val seekAmountSec: Int, val isForward: Boolean, val label: String)
    var showDoubleTapArgs: DoubleTapArgs? = null

    var showVolumeArgs: Int? = null
    var showBrightnessArgs: Int? = null

    data class SpeedArgs(val speed: Float, val interactiveSliderIndex: Int?)
    var showSpeedArgs: SpeedArgs? = null

    var triggerSingleTapCount = 0

    var hideSpeedOverlayCallCount = 0
    var hideDoubleTapSeekOverlayCallCount = 0
    var hideVolumeOverlayCallCount = 0
    var hideBrightnessOverlayCallCount = 0
    var hidePinchZoomOverlayCallCount = 0
    var hideHorizontalSeekOverlayCallCount = 0

    data class PlaybackSpeedRampedArgs(val targetSpeed: Float, val stepCount: Int, val stepDurationMs: Long)
    var setPlaybackSpeedRampedArgs: PlaybackSpeedRampedArgs? = null
    var setPlaybackSpeedRampedCallCount = 0

    var cancelTimerCallCount = 0

    override var durationMs: Long = 100_000L
    override var currentPositionMs: Long = 50_000L
    override var isPaused: Boolean = false
    override var currentZoomLog2: Float = 0f
    override var currentPanX: Float = 0f
    override var currentPanY: Float = 0f
    private var _volume: Float = 50f
    override val volume: Float get() = _volume
    override var maxStandardVolume: Float = 100f
    override var maxBoostVolume: Float = 130f
    private var _brightness: Float = 0.5f
    override val brightness: Float get() = _brightness
    override var screenWidthPx: Float = 1080f
    override var screenHeightPx: Float = 1920f
    override var isVolumeSideRight: Boolean = true
    override var doubleTapSeekAreaWidthPercent: Int = 30
    override var isDynamicSpeedOverlayEnabled: Boolean = true
    override var playbackSpeed: Float = 1.0f
    var restorePlaybackSpeedCallCount = 0

    override fun pause() {
        pauseCallCount++
    }

    override fun unpause() {
        unpauseCallCount++
    }

    override fun seekTo(positionMs: Long, precise: Boolean) {
        lastSeekToMs = positionMs
    }

    override fun seekForward(offsetMs: Long) {
        lastSeekToMs = currentPositionMs + offsetMs
    }

    override fun seekBackward(offsetMs: Long) {
        lastSeekToMs = currentPositionMs - offsetMs
    }

    override fun seekGesture(positionMs: Long) {
        lastSeekToMs = positionMs
    }

    override fun seekCommit(positionMs: Long) {
        lastSeekCommitMs = positionMs
    }

    override fun setPlaybackSpeedRamped(targetSpeed: Float, stepCount: Int, stepDurationMs: Long) {
        setPlaybackSpeedRampedArgs = PlaybackSpeedRampedArgs(targetSpeed, stepCount, stepDurationMs)
        setPlaybackSpeedRampedCallCount++
    }

    override fun restorePlaybackSpeed() {
        restorePlaybackSpeedCallCount++
    }

    override fun setVolume(volume: Float) {
        this._volume = volume
    }

    override fun setBrightness(brightness: Float) {
        this._brightness = brightness
    }

    override fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float) {
        this.currentZoomLog2 = zoomLog2
        this.currentPanX = panX
        this.currentPanY = panY
    }

    override fun showDoubleTapSeekOverlay(seekAmountSec: Int, isForward: Boolean, label: String) {
        showDoubleTapArgs = DoubleTapArgs(seekAmountSec, isForward, label)
    }

    override fun hideDoubleTapSeekOverlay() {
        showDoubleTapArgs = null
        hideDoubleTapSeekOverlayCallCount++
    }

    override fun showHorizontalSeekOverlay(currentTimeLabel: String, deltaLabel: String, targetPositionMs: Long) {}

    override fun hideHorizontalSeekOverlay(delayMs: Long) {
        hideHorizontalSeekOverlayCallCount++
    }

    override fun showSpeedOverlay(speed: Float, interactiveSliderIndex: Int?) {
        showSpeedArgs = SpeedArgs(speed, interactiveSliderIndex)
    }

    override fun hideSpeedOverlay() {
        showSpeedArgs = null
        hideSpeedOverlayCallCount++
    }

    override fun showVolumeOverlay(percentage: Int) {
        showVolumeArgs = percentage
    }

    override fun hideVolumeOverlay() {
        showVolumeArgs = null
        hideVolumeOverlayCallCount++
    }

    override fun showBrightnessOverlay(percentage: Int) {
        showBrightnessArgs = percentage
    }

    override fun hideBrightnessOverlay() {
        showBrightnessArgs = null
        hideBrightnessOverlayCallCount++
    }

    override fun showPinchZoomOverlay(zoomPercentage: Int) {}

    override fun hidePinchZoomOverlay() {
        hidePinchZoomOverlayCallCount++
    }

    override fun showTapFeedback(x: Float, y: Float) {}

    override fun scheduleTimer(delayMs: Long, action: () -> Unit): Any {
        return Any()
    }

    override fun cancelTimer(timerId: Any?) {
        if (timerId != null) {
            cancelTimerCallCount++
        }
    }

    override fun triggerSingleTapAction() {
        triggerSingleTapCount++
    }
}

class MpvGestureStateMachineTest {

    @Test
    fun idle_to_tap_candidate_on_pointer_down() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(
            pointerId = 1L,
            x = 540f,
            y = 960f,
            timeMs = 100L,
            activePointerCount = 1,
            panelShown = PanelShown.NONE,
            density = 3f
        )

        assertTrue(stateMachine.currentState is GestureState.TapCandidate)
    }

    @Test
    fun single_center_tap_triggers_action_after_deferred_timeout() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        // Test non-center tap where deferred timer is scheduled and executed via onDeferredSingleTapTimeout
        stateMachine.onPointerDown(1L, 200f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 200f, 960f, 150L, 1)
        assertEquals(0, controller.triggerSingleTapCount)
        assertTrue(stateMachine.currentState is GestureState.TapCandidate)

        stateMachine.onDeferredSingleTapTimeout(100L)
        assertEquals(1, controller.triggerSingleTapCount)
        assertTrue(stateMachine.currentState is GestureState.Idle)

        // Test center tap where action is triggered
        stateMachine.onPointerDown(1L, 540f, 960f, 300L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 540f, 960f, 350L, 1)
        assertEquals(2, controller.triggerSingleTapCount)
        assertTrue(stateMachine.currentState is GestureState.Idle)
    }

    @Test
    fun left_double_tap_seeks_backward() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 200f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 200f, 960f, 150L, 1)

        stateMachine.onPointerDown(1L, 200f, 960f, 250L, 1, PanelShown.NONE, 3f)

        assertEquals(40_000L, controller.lastSeekToMs)
        assertNotNull(controller.showDoubleTapArgs)
        assertEquals(10, controller.showDoubleTapArgs?.seekAmountSec)
        assertEquals(false, controller.showDoubleTapArgs?.isForward)
        assertEquals("-10s", controller.showDoubleTapArgs?.label)
        assertTrue(stateMachine.currentState is GestureState.MultiTapSeeking)
    }

    @Test
    fun right_double_tap_seeks_forward() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 850f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 150L, 1)

        stateMachine.onPointerDown(1L, 850f, 960f, 250L, 1, PanelShown.NONE, 3f)

        assertEquals(60_000L, controller.lastSeekToMs)
        assertNotNull(controller.showDoubleTapArgs)
        assertEquals(10, controller.showDoubleTapArgs?.seekAmountSec)
        assertEquals(true, controller.showDoubleTapArgs?.isForward)
        assertEquals("+10s", controller.showDoubleTapArgs?.label)
        assertTrue(stateMachine.currentState is GestureState.MultiTapSeeking)
    }

    @Test
    fun triple_tap_right_accumulates_seek() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 850f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 150L, 1)

        stateMachine.onPointerDown(1L, 850f, 960f, 250L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 300L, 1)

        stateMachine.onPointerDown(1L, 850f, 960f, 400L, 1, PanelShown.NONE, 3f)

        assertNotNull(controller.showDoubleTapArgs)
        assertEquals(30, controller.showDoubleTapArgs?.seekAmountSec)
        assertEquals(true, controller.showDoubleTapArgs?.isForward)
        assertEquals("+30s", controller.showDoubleTapArgs?.label)

        val state = stateMachine.currentState as GestureState.MultiTapSeeking
        assertEquals(2, state.tapCount)
        assertEquals(30, state.accumulatedSeekSec)
    }

    @Test
    fun horizontal_swipe_enters_horizontal_seek_without_pausing() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 540f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerMove(1L, 590f, 960f, 170L, 1, PanelShown.NONE, 3f)

        assertTrue(stateMachine.currentState is GestureState.HorizontalSeek)
        assertEquals(0, controller.pauseCallCount)
    }

    @Test
    fun horizontal_swipe_release_commits_seek_and_unpauses_if_playing() {
        val controller = FakeMpvPlayerController()
        controller.isPaused = false
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 540f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerMove(1L, 590f, 960f, 170L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 590f, 960f, 200L, 1)

        assertEquals(1, controller.unpauseCallCount)
        assertNotNull(controller.lastSeekCommitMs)
        assertTrue(stateMachine.currentState is GestureState.Idle)
    }

    @Test
    fun vertical_swipe_right_half_shows_volume_overlay() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 800f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerMove(1L, 800f, 910f, 120L, 1, PanelShown.NONE, 3f)

        assertTrue(stateMachine.currentState is GestureState.VerticalSwipe)
        val state = stateMachine.currentState as GestureState.VerticalSwipe
        assertTrue(state.isVolumeSide)
        assertNotNull(controller.showVolumeArgs)
    }

    @Test
    fun vertical_swipe_left_half_shows_brightness_overlay() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 200f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerMove(1L, 200f, 910f, 120L, 1, PanelShown.NONE, 3f)

        assertTrue(stateMachine.currentState is GestureState.VerticalSwipe)
        val state = stateMachine.currentState as GestureState.VerticalSwipe
        assertFalse(state.isVolumeSide)
        assertNotNull(controller.showBrightnessArgs)
    }

    @Test
    fun long_press_shows_speed_overlay_after_timeout() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 540f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onLongPressTimeout(100L)

        assertTrue(stateMachine.currentState is GestureState.LongPress)
        assertNotNull(controller.showSpeedArgs)
        assertEquals(2.0f, controller.showSpeedArgs?.speed)
    }

    @Test
    fun pointer_down_inside_dead_zone_remains_idle() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        // Dead zone is 48dp * density 3f = 144px. x = 100px is inside dead zone.
        stateMachine.onPointerDown(1L, 100f, 960f, 100L, 1, PanelShown.NONE, 3f)

        assertTrue(stateMachine.currentState is GestureState.Idle)
    }

    @Test
    fun movement_exceeding_threshold_cancels_long_press_and_marks_exceeded() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 540f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerMove(1L, 555f, 975f, 120L, 1, PanelShown.NONE, 3f)

        assertTrue(stateMachine.currentState is GestureState.TapCandidate)
        val state = stateMachine.currentState as GestureState.TapCandidate
        assertTrue(state.exceededTapThreshold)
        assertNull(state.longPressTimerId)
        assertTrue(controller.cancelTimerCallCount > 0)
    }
}
