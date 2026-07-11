package com.tapman104.mpvplayer.player.viewmodel

import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PositionState
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertyChangeMapper {
    var state = PlayerState()
        private set
    var positionState = PositionState()
        private set

    fun onPropertyChange(name: String, value: Any?) {
        when (name) {
            "pause" -> {
                val paused = value as? Boolean ?: return
                state = state.copy(isPaused = paused)
            }
            "time-pos" -> {
                val seconds = value as? Double ?: return
                positionState = positionState.copy(positionSec = seconds)
            }
            "duration" -> {
                val seconds = value as? Double ?: return
                positionState = positionState.copy(durationSec = seconds)
            }
            "speed" -> {
                val speed = value as? Double ?: return
                state = state.copy(playbackSpeed = speed)
            }
            "hwdec" -> {
                val hwdec = value as? String ?: return
                val mode = when (hwdec) {
                    DecodeMode.HWPlus.mpvValue -> DecodeMode.HWPlus
                    DecodeMode.SW.mpvValue, "no", "" -> DecodeMode.SW
                    else -> DecodeMode.HW
                }
                if (state.decodeMode != mode) {
                    state = state.copy(decodeMode = mode)
                }
            }
            "volume" -> {
                val volume = value as? Double ?: return
                state = state.copy(volume = volume.toInt())
            }
        }
    }
}

class PlayerViewModelPropertyChangeTest {

    @Test
    fun prop_pause_true_maps_to_is_playing_false() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("pause", true)
        assertEquals(false, mapper.state.isPlaying)
    }

    @Test
    fun prop_pause_false_maps_to_is_playing_true() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("pause", false)
        assertEquals(true, mapper.state.isPlaying)
    }

    @Test
    fun prop_duration_maps_to_duration_ms() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("duration", 90.0)
        assertEquals(90000L, mapper.positionState.durationMs)
    }

    @Test
    fun prop_time_pos_maps_to_current_position_ms() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("time-pos", 30.5)
        assertEquals(30500L, mapper.positionState.currentPositionMs)
    }

    @Test
    fun prop_hwdec_mediacodec_maps_to_hw_decode_mode() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("hwdec", "mediacodec")
        assertEquals(DecodeMode.HW, mapper.state.decodeMode)
    }

    @Test
    fun prop_hwdec_mediacodec_copy_maps_to_hw_plus_decode_mode() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("hwdec", "mediacodec-copy")
        assertEquals(DecodeMode.HWPlus, mapper.state.decodeMode)
    }

    @Test
    fun prop_hwdec_no_maps_to_sw_decode_mode() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("hwdec", "no")
        assertEquals(DecodeMode.SW, mapper.state.decodeMode)
    }

    @Test
    fun prop_speed_maps_to_speed_float() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("speed", 2.0)
        assertEquals(2.0f, mapper.state.speed)
    }

    @Test
    fun prop_volume_maps_to_volume_int() {
        val mapper = PropertyChangeMapper()
        mapper.onPropertyChange("volume", 75.0)
        assertEquals(75, mapper.state.volume)
    }
}
