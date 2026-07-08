package com.tapman104.mpvplayer.core.engine

import android.content.Context
import `is`.xyz.mpv.MPVLib
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.atomic.AtomicBoolean

sealed class InitResult {
    object Success : InitResult()
    data class Failure(val message: String) : InitResult()
}

class MpvController(private val context: Context) {
    private val _initResult = MutableSharedFlow<InitResult>(replay = 1)

    /** Publicly read-only; callers cannot emit spurious results. */
    val initResult: SharedFlow<InitResult> = _initResult

    val executor = MpvCommandExecutor()
    val dispatcher = MpvEventDispatcher()
    val surface = MpvSurface(executor)
    val configurator = MpvOptionsConfigurator(context, onVoConfigured = { surface.setVo(it) })

    /** Guards against concurrent or repeated init calls without a TOCTOU race. */
    private val initialized = AtomicBoolean(false)

    fun init() {
        if (!initialized.compareAndSet(false, true)) return
        Log.d(TAG, "Initializing MPV engine")

        executor.execute {
            try {
                // Copy bundled font assets so MPV can locate them for subtitle rendering
                configurator.copyFontAssets()

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

                _initResult.tryEmit(InitResult.Success)
                Log.d(TAG, "MPV engine initialized successfully")
            } catch (e: Exception) {
                initialized.set(false)           // allow a retry after failure
                Log.e(TAG, "Failed to initialize MPV engine", e)
                _initResult.tryEmit(InitResult.Failure(e.message ?: "Unknown error"))
            }
        }
    }

    private fun registerPropertyObservers() {
        // Status properties
        MPVLib.observeProperty(MpvProp.PAUSE, MpvFmt.FLAG)
        MPVLib.observeProperty(MpvProp.TIME_POS, MpvFmt.DOUBLE)
        MPVLib.observeProperty(MpvProp.DURATION, MpvFmt.DOUBLE)
        MPVLib.observeProperty(MpvProp.DEMUXER_CACHE_TIME, MpvFmt.DOUBLE)

        // Track structures
        MPVLib.observeProperty(MpvProp.TRACK_LIST, MpvFmt.NODE)
        MPVLib.observeProperty(MpvProp.AUDIO_ID, MpvFmt.INT64)
        MPVLib.observeProperty(MpvProp.SUBTITLE_ID, MpvFmt.INT64)

        // Playback parameters
        MPVLib.observeProperty(MpvProp.SPEED, MpvFmt.DOUBLE)
        MPVLib.observeProperty(MpvProp.HWDEC, MpvFmt.STRING)
        MPVLib.observeProperty(MpvProp.VOLUME, MpvFmt.DOUBLE)

        // Buffering and end-of-file flags
        MPVLib.observeProperty(MpvProp.PAUSED_FOR_CACHE, MpvFmt.FLAG)
        MPVLib.observeProperty(MpvProp.EOF_REACHED, MpvFmt.FLAG)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun destroy() {
        if (!initialized.compareAndSet(true, false)) return
        Log.d(TAG, "Destroying MPV engine")

        _initResult.resetReplayCache()
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

    companion object {
        private const val TAG = "MpvController"
    }
}
