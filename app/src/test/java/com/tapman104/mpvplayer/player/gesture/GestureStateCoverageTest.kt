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
}
