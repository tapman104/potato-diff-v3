package com.tapman104.mpvplayer.core.engine

import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventProcessorTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun testOnFileLoaded_clearsErrorAndSetsLoaded() {
        val playerState = MutableStateFlow(PlayerState(isLoading = true, hasError = true))
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onFileLoaded()

        assertTrue(playerState.value.fileLoaded)
        assertFalse(playerState.value.isLoading)
        assertFalse(playerState.value.hasError)
    }

    @Test
    fun testOnPlaybackStarted_clearsLoadingAndPaused() {
        val playerState = MutableStateFlow(PlayerState(isLoading = true, isPaused = true))
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onPlaybackStarted()

        assertFalse(playerState.value.isPaused)
        assertFalse(playerState.value.isLoading)
    }

    @Test
    fun testOnPlaybackStopped_setsPausedAndTriggersCallbackWhenEndReasonZero() {
        val playerState = MutableStateFlow(PlayerState(isPaused = false))
        val positionState = MutableStateFlow(PositionState())
        var callbackTriggered = false
        val processor = EventProcessor(
            scope = scope,
            playerState = playerState,
            positionState = positionState,
            onPlaybackEnded = { callbackTriggered = true }
        )

        processor.onPlaybackStopped(0)

        assertTrue(playerState.value.isPaused)
        assertTrue(callbackTriggered)
    }

    @Test
    fun testPropertyChangePause() {
        val playerState = MutableStateFlow(PlayerState(isPaused = false))
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onPropertyChange("pause", true)
        assertTrue(playerState.value.isPaused)
    }

    @Test
    fun testPropertyChangeTimePos_suppressedWhenSliderSeeking() {
        val playerState = MutableStateFlow(PlayerState())
        val positionState = MutableStateFlow(PositionState(positionSec = 10.0))
        val processor = EventProcessor(scope, playerState, positionState)

        processor.isSliderSeeking = true
        processor.onPropertyChange("time-pos", 50.0)

        assertEquals(10.0, positionState.value.positionSec, 0.001)
    }

    @Test
    fun testPropertyChangeDuration() {
        val playerState = MutableStateFlow(PlayerState())
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onPropertyChange("duration", 120.5)

        assertEquals(120.5, positionState.value.durationSec, 0.001)
        assertEquals(120500L, positionState.value.durationMs)
    }

    @Test
    fun testPropertyChangeHwdec() {
        val playerState = MutableStateFlow(PlayerState())
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onPropertyChange("hwdec", "mediacodec-copy")
        assertEquals(DecodeMode.HWPlus, playerState.value.decodeMode)

        processor.onPropertyChange("hwdec", "mediacodec")
        assertEquals(DecodeMode.HW, playerState.value.decodeMode)

        processor.onPropertyChange("hwdec", "no")
        assertEquals(DecodeMode.SW, playerState.value.decodeMode)
    }

    @Test
    fun testPropertyChangeSpeed() {
        val playerState = MutableStateFlow(PlayerState())
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onPropertyChange("speed", 1.75)
        assertEquals(1.75, playerState.value.playbackSpeed, 0.001)
    }

    @Test
    fun testPropertyChangeVolume() {
        val playerState = MutableStateFlow(PlayerState())
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onPropertyChange("volume", 85.0)
        assertEquals(85, playerState.value.volume)
    }

    @Test
    fun testOnError() {
        val playerState = MutableStateFlow(PlayerState(isLoading = true))
        val positionState = MutableStateFlow(PositionState())
        val processor = EventProcessor(scope, playerState, positionState)

        processor.onError("Decoder failure")

        assertTrue(playerState.value.hasError)
        assertFalse(playerState.value.isLoading)
    }
}
