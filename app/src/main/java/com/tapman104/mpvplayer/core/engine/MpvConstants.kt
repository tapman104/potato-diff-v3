package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVLib
import kotlin.reflect.KProperty

// ---------------------------------------------------------------------------
// MPV property name constants
// ---------------------------------------------------------------------------

internal object MpvProp {
    const val PAUSE = "pause"
    const val TIME_POS = "time-pos"
    const val DURATION = "duration"
    const val DEMUXER_CACHE_TIME = "demuxer-cache-time"
    const val TRACK_LIST = "track-list"
    const val AUDIO_ID = "aid"
    const val SUBTITLE_ID = "sid"
    const val SPEED = "speed"
    const val HWDEC = "hwdec"
    const val VOLUME = "volume"
    const val MUTE = "mute"
    const val PAUSED_FOR_CACHE = "paused-for-cache"
    const val EOF_REACHED = "eof-reached"
}

// ---------------------------------------------------------------------------
// MPV property format type IDs (mpv_format enum values)
// ---------------------------------------------------------------------------

internal object MpvFmt {
    const val NONE = 0
    const val STRING = 1
    const val OSD_STRING = 2
    const val FLAG = 3
    const val INT64 = 4
    const val DOUBLE = 5
    const val NODE = 6
    const val NODE_ARRAY = 7
    const val NODE_MAP = 8
}

// ---------------------------------------------------------------------------
// MPV event IDs (mpv_event_id enum values)
// ---------------------------------------------------------------------------

internal object MpvEvent {
    const val NONE = 0
    const val SHUTDOWN = 1
    const val LOG_MESSAGE = 2
    const val GET_PROPERTY_REPLY = 3
    const val SET_PROPERTY_REPLY = 4
    const val COMMAND_REPLY = 5
    const val START_FILE = 6
    const val END_FILE = 7
    const val FILE_LOADED = 8
    const val IDLE = 9
    const val TICK = 10
    const val CLIENT_MESSAGE = 11
    const val VIDEO_RECONFIG = 12
    const val AUDIO_RECONFIG = 13
    const val SEEK = 14
    const val PLAYBACK_RESTART = 15
    const val PROPERTY_CHANGE = 16
    const val QUEUE_OVERFLOW = 17
    const val HOOK = 18
}

// ---------------------------------------------------------------------------
// Kotlin property delegate for MPV track-ID properties.
//
// Reading returns the current integer ID, or -1 if MPV returns "no" or null.
// Writing -1 sets the property to "no" (disables the track); any other value
// sets the integer.
//
// NOTE: MPVLib getters/setters are NOT thread-safe. Only use these delegates
// from within an executor.execute{} block.
// ---------------------------------------------------------------------------

class TrackDelegate(private val propName: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
        MPVLib.getPropertyString(propName)?.toIntOrNull() ?: -1

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        if (value == -1) MPVLib.setPropertyString(propName, "no")
        else MPVLib.setPropertyInt(propName, value)
    }
}
