
package com.tapman104.mpvplayer.player.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureStateCoverageTest {

    @Test
    fun center_double_tap_does_not_seek_starts_fresh_candidate() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        // Start a tap candidate on left (x = 200f) so deferred timer is active
        stateMachine.onPointerDown(1L, 200f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 200f, 960f, 150L, 1)

        // Second tap within window arrives in center (x = 540f)
        stateMachine.onPointerDown(1L, 540f, 960f, 200L, 1, PanelShown.NONE, 3f)

        assertNull(controller.lastSeekToMs)
        assertTrue(stateMachine.currentState is GestureState.TapCandidate)
        val candidate = stateMachine.currentState as GestureState.TapCandidate
        assertEquals(540f, candidate.downX)
        assertEquals(200L, candidate.downTimeMs)
    }

    @Test
    fun direction_reversal_resets_accumulated_seek() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        // Double-tap right (+10s)
        stateMachine.onPointerDown(1L, 850f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 150L, 1)
        stateMachine.onPointerDown(1L, 850f, 960f, 250L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 300L, 1)

        val rightState = stateMachine.currentState as GestureState.MultiTapSeeking
        assertEquals(10, rightState.accumulatedSeekSec)
        assertEquals("+10s", controller.showDoubleTapArgs?.label)

        // Tap left (reversal)
        stateMachine.onPointerDown(1L, 200f, 960f, 400L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 200f, 960f, 450L, 1)
        stateMachine.onPointerDown(1L, 200f, 960f, 500L, 1, PanelShown.NONE, 3f)

        val leftState = stateMachine.currentState as GestureState.MultiTapSeeking
        assertEquals(10, leftState.accumulatedSeekSec)
        assertEquals("-10s", controller.showDoubleTapArgs?.label)
    }

    @Test
    fun tap_outside_continuation_window_restarts_sequence() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        // Double-tap right (+10s)
        stateMachine.onPointerDown(1L, 850f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 150L, 1)
        stateMachine.onPointerDown(1L, 850f, 960f, 250L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 300L, 1)

        // Tap outside continuation window (>650ms after last tap at 250ms -> e.g. 950ms)
        stateMachine.onPointerDown(1L, 850f, 960f, 950L, 1, PanelShown.NONE, 3f)

        assertTrue(stateMachine.currentState is GestureState.TapCandidate)
        val candidate = stateMachine.currentState as GestureState.TapCandidate
        assertEquals(950L, candidate.downTimeMs)
    }

    @Test
    fun pinch_arrival_during_tap_candidate_transitions_to_pinch_zoom_pan() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 540f, 960f, 100L, 1, PanelShown.NONE, 3f)
        // Simultaneous second finger arrives
        stateMachine.onPointerDown(2L, 600f, 960f, 120L, 2, PanelShown.NONE, 3f)

        assertTrue(stateMachine.currentState is GestureState.PinchZoomPan)
    }

    @Test
    fun horizontal_seek_when_paused_does_not_unpause_on_release() {
        val controller = FakeMpvPlayerController()
        controller.isPaused = true
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 540f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerMove(1L, 590f, 960f, 170L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 590f, 960f, 200L, 1)

        assertEquals(0, controller.unpauseCallCount)
        assertNotNull(controller.lastSeekCommitMs)
        assertTrue(stateMachine.currentState is GestureState.Idle)
    }

    @Test
    fun long_press_release_restores_speed_and_hides_overlay() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 540f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onLongPressTimeout(100L)

        assertTrue(stateMachine.currentState is GestureState.LongPress)

        stateMachine.onPointerUp(1L, 540f, 960f, 700L, 1)

        assertEquals(1, controller.restorePlaybackSpeedCallCount)
        assertEquals(1, controller.hideSpeedOverlayCallCount)
        assertTrue(stateMachine.currentState is GestureState.Idle)
    }

    @Test
    fun single_pan_entry_move_and_release() {
        val controller = FakeMpvPlayerController()
        controller.currentZoomLog2 = 1.0f // Scale = 2.0f
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 500f, 500f, 100L, 1, PanelShown.NONE, 3f)
        assertTrue(stateMachine.currentState is GestureState.TapCandidate)

        // Move > SINGLE_PAN_MIN_DELTA_PX (20f) while zoomed
        stateMachine.onPointerMove(1L, 550f, 500f, 120L, 1, PanelShown.NONE, 3f)
        assertTrue(stateMachine.currentState is GestureState.SinglePan)
        val singlePan = stateMachine.currentState as GestureState.SinglePan
        assertEquals(2.0f, singlePan.currentScale)

        // Move again to trigger handleSinglePanMove
        stateMachine.onPointerMove(1L, 600f, 500f, 140L, 1, PanelShown.NONE, 3f)
        val updatedPan = stateMachine.currentState as GestureState.SinglePan
        assertEquals(600f, updatedPan.prevX)
        assertTrue(controller.currentPanX != 0f)

        // Release pointer
        stateMachine.onPointerUp(1L, 600f, 500f, 160L, 1)
        assertTrue(stateMachine.currentState is GestureState.Idle)
    }

    @Test
    fun pinch_zoom_pan_move_and_release() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 400f, 500f, 100L, 1, PanelShown.NONE, 3f)
        // Second pointer arrives via onPointerMove with activeCount = 2
        stateMachine.onPointerMove(1L, 400f, 500f, 110L, 2, PanelShown.NONE, 3f, span = 200f, midpointX = 500f, midpointY = 500f)
        assertTrue(stateMachine.currentState is GestureState.PinchZoomPan)

        // Move pointers to zoom in (span increases)
        stateMachine.onPointerMove(1L, 350f, 500f, 130L, 2, PanelShown.NONE, 3f, span = 400f, midpointX = 520f, midpointY = 500f)
        val zoomedState = stateMachine.currentState as GestureState.PinchZoomPan
        assertTrue(zoomedState.currentZoomLog2 > 0f)

        // Release one pointer -> activePointerCount <= 1 transitions to Idle
        stateMachine.onPointerUp(2L, 650f, 500f, 150L, 1)
        assertTrue(stateMachine.currentState is GestureState.Idle)
        assertEquals(1, controller.hidePinchZoomOverlayCallCount)
    }

    @Test
    fun dynamic_speed_scrub_unlock_move_and_release() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 500f, 500f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onLongPressTimeout(100L)
        assertTrue(stateMachine.currentState is GestureState.LongPress)

        // Move horizontally > threshold (DYNAMIC_SPEED_UNLOCK_DX_DP * 3f = 30f)
        stateMachine.onPointerMove(1L, 550f, 500f, 600L, 1, PanelShown.NONE, 3f)
        assertTrue(stateMachine.currentState is GestureState.DynamicSpeedScrub)
        val scrubState = stateMachine.currentState as GestureState.DynamicSpeedScrub
        assertEquals(500f, scrubState.startX)

        // Scrub further right to change preset speed
        stateMachine.onPointerMove(1L, 800f, 500f, 650L, 1, PanelShown.NONE, 3f)
        assertNotNull(controller.showSpeedArgs)
        assertTrue(controller.showSpeedArgs!!.speed > 1.0f)

        // Release pointer
        stateMachine.onPointerUp(1L, 800f, 500f, 700L, 0)
        assertEquals(1, controller.restorePlaybackSpeedCallCount)
        assertEquals(1, controller.hideSpeedOverlayCallCount)
        assertTrue(stateMachine.currentState is GestureState.Idle)
    }

    @Test
    fun horizontal_seek_to_pinch_zoom_pan_transition() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 200f, 500f, 100L, 1, PanelShown.NONE, 3f)
        // Move horizontally to trigger HorizontalSeek (> 30px, > 60ms)
        stateMachine.onPointerMove(1L, 300f, 500f, 200L, 1, PanelShown.NONE, 3f)
        assertTrue(stateMachine.currentState is GestureState.HorizontalSeek)

        // Second pointer arrives during seek
        stateMachine.onPointerMove(1L, 300f, 500f, 220L, 2, PanelShown.NONE, 3f, span = 200f, midpointX = 400f, midpointY = 500f)
        assertTrue(stateMachine.currentState is GestureState.PinchZoomPan)
        assertEquals(1, controller.hideHorizontalSeekOverlayCallCount)
    }

    @Test
    fun vertical_swipe_boost_regime_and_release() {
        val controller = FakeMpvPlayerController()
        controller.isVolumeSideRight = true
        controller.maxStandardVolume = 100f
        controller.maxBoostVolume = 150f
        controller.setVolume(90f)
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 800f, 500f, 100L, 1, PanelShown.NONE, 3f)
        // Move vertically to trigger VerticalSwipe (> 20px)
        stateMachine.onPointerMove(1L, 800f, 450f, 120L, 1, PanelShown.NONE, 3f)
        assertTrue(stateMachine.currentState is GestureState.VerticalSwipe)
        var swipeState = stateMachine.currentState as GestureState.VerticalSwipe
        assertFalse(swipeState.isBoostRegime)

        // Move upwards to enter boost regime
        stateMachine.onPointerMove(1L, 800f, 100f, 140L, 1, PanelShown.NONE, 3f)
        swipeState = stateMachine.currentState as GestureState.VerticalSwipe
        assertTrue(swipeState.isBoostRegime)
        assertEquals(100f, controller.volume)

        // Move upwards further in boost regime
        stateMachine.onPointerMove(1L, 800f, 50f, 160L, 1, PanelShown.NONE, 3f)
        assertTrue(controller.volume > 100f)

        // Release pointer
        stateMachine.onPointerUp(1L, 800f, 50f, 180L, 1)
        assertTrue(stateMachine.currentState is GestureState.Idle)
        assertEquals(1, controller.hideVolumeOverlayCallCount)
    }

    @Test
    fun multi_tap_inactivity_timeout() {
        val controller = FakeMpvPlayerController()
        val stateMachine = MpvGestureStateMachine(controller)

        stateMachine.onPointerDown(1L, 850f, 960f, 100L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 150L, 1)
        stateMachine.onPointerDown(1L, 850f, 960f, 250L, 1, PanelShown.NONE, 3f)
        stateMachine.onPointerUp(1L, 850f, 960f, 300L, 1)

        assertTrue(stateMachine.currentState is GestureState.MultiTapSeeking)
        val hideBefore = controller.hideDoubleTapSeekOverlayCallCount
        stateMachine.onMultiTapInactivityTimeout(250L)
        assertTrue(stateMachine.currentState is GestureState.Idle)
        assertTrue(controller.hideDoubleTapSeekOverlayCallCount > hideBefore)
    }
}
