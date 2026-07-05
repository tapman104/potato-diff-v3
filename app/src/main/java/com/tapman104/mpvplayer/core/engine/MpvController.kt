package com.tapman104.mpvplayer.core.engine

import android.content.Context
import `is`.xyz.mpv.MPVLib
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow

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

sealed class InitResult {
    object Success : InitResult()
    data class Failure(val message: String) : InitResult()
}

class MpvController(private val context: Context) {
    private val TAG = "MpvController"
    val executor = MpvCommandExecutor()
    val dispatcher = MpvEventDispatcher()
    val surface = MpvSurface(executor)
    val configurator = MpvOptionsConfigurator(context, onVoConfigured = { surface.setVo(it) })

    @Volatile private var initialized = false

    val initResult = MutableSharedFlow<InitResult>(replay = 1)

    private fun copyFontAsset() {
        val fontsDir = java.io.File(context.filesDir, "fonts")
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }
        val fontFile = java.io.File(fontsDir, "Roboto-Regular.ttf")
        if (!fontFile.exists()) {
            try {
                context.assets.open("Roboto-Regular.ttf").use { inputStream ->
                    java.io.FileOutputStream(fontFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Copied Roboto-Regular.ttf to fonts directory")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy font asset", e)
            }
        }
    }

    fun init() {
        if (initialized) return
        Log.d(TAG, "Initializing MPV engine")
        
        executor.execute {
            try {
                copyFontAsset()
                // Initialize JNI context and native engine
                MPVLib.create(context.applicationContext)
                
                // Configure engine options before init using standalone configurator
                configurator.initOptions()
                
                // Initialize MPV
                MPVLib.init()
                
                // Configure post-init options
                configurator.postInitOptions()
                
                // Add event observer and property observers
                MPVLib.addObserver(dispatcher)
                registerPropertyObservers()
                
                initialized = true
                initResult.tryEmit(InitResult.Success)
                Log.d(TAG, "MPV engine initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MPV engine", e)
                initResult.tryEmit(InitResult.Failure(e.message ?: "Unknown error"))
            }
        }
    }

    private fun registerPropertyObservers() {
        // Observe status properties
        MPVLib.observeProperty(MpvProp.PAUSE, MpvFmt.FLAG)
        MPVLib.observeProperty(MpvProp.TIME_POS, MpvFmt.DOUBLE)
        MPVLib.observeProperty(MpvProp.DURATION, MpvFmt.DOUBLE)
        MPVLib.observeProperty(MpvProp.DEMUXER_CACHE_TIME, MpvFmt.DOUBLE)
        
        // Observe track structures
        MPVLib.observeProperty(MpvProp.TRACK_LIST, MpvFmt.NODE)
        MPVLib.observeProperty(MpvProp.AUDIO_ID, MpvFmt.INT64)
        MPVLib.observeProperty(MpvProp.SUBTITLE_ID, MpvFmt.INT64)
        
        // Observe playback parameters
        MPVLib.observeProperty(MpvProp.SPEED, MpvFmt.DOUBLE)
        MPVLib.observeProperty(MpvProp.HWDEC, MpvFmt.STRING)
        MPVLib.observeProperty(MpvProp.VOLUME, MpvFmt.DOUBLE)

        // Observe buffering and end of file flags
        MPVLib.observeProperty(MpvProp.PAUSED_FOR_CACHE, MpvFmt.FLAG)
        MPVLib.observeProperty(MpvProp.EOF_REACHED, MpvFmt.FLAG)
    }

    fun destroy() {
        if (!initialized) return
        Log.d(TAG, "Destroying MPV engine")
        initialized = false

        executor.detachSurface()

        executor.execute {
            try {
                MPVLib.removeObserver(dispatcher)
                MPVLib.destroy()
                Log.d(TAG, "MPV engine destroyed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to destroy MPV engine", e)
            }
        }

        // Shutdown AFTER submitting the cleanup task, not inside it
        executor.shutdown()
    }
}
