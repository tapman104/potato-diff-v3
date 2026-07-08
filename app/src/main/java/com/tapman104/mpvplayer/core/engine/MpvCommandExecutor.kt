package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.KeyMapping
import `is`.xyz.mpv.MPVLib
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class MpvCommandExecutor {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mpv-engine-thread")
    }

    /**
     * Incremented on every surface attach. Detach tasks capture this value at queue
     * time and skip themselves if a newer attach has since been queued, preventing
     * a stale detach from tearing down a freshly attached surface.
     */
    private val surfaceGeneration = AtomicInteger(0)
    private val pendingSeek = AtomicReference<Double?>(null)
    private val pendingRelativeSeek = AtomicReference<Double?>(null)

    /** Main-thread handler used only by [getVideoAspect] to deliver results. */
    private val mainHandler = Handler(Looper.getMainLooper())

    fun execute(action: () -> Unit) {
        if (!executor.isShutdown) {
            executor.submit(action)
        }
    }

    /**
     * Increments and returns the new surface generation. Call this immediately
     * before queuing an attachSurface task so the matching detachSurface can
     * recognise it is stale if a newer generation has already been registered.
     */
    fun nextSurfaceGeneration(): Int = surfaceGeneration.incrementAndGet()

    /**
     * Queues MPVLib.detachSurface(). If [nextSurfaceGeneration] has been called
     * again by the time this task executes (i.e., a new attach is already in
     * flight), the detach is silently dropped.
     */
    fun detachSurface() {
        val capturedGen = surfaceGeneration.get()
        execute {
            val current = surfaceGeneration.get()
            if (current == capturedGen) {
                Log.d(TAG, "detachSurface gen=$capturedGen")
                MPVLib.detachSurface()
            } else {
                Log.d(TAG, "detachSurface skipped — stale gen=$capturedGen, current=$current")
            }
        }
    }

    fun play() {
        execute {
            MPVLib.setPropertyBoolean(MpvProp.PAUSE, false)
        }
    }

    fun pause() {
        execute {
            MPVLib.setPropertyBoolean(MpvProp.PAUSE, true)
        }
    }

    fun togglePlay() {
        execute {
            val paused = MPVLib.getPropertyBoolean(MpvProp.PAUSE) ?: false
            MPVLib.setPropertyBoolean(MpvProp.PAUSE, !paused)
        }
    }

    /**
     * Called continuously during scrub. Coalesces: only the last value
     * queued before the executor picks it up is actually sent.
     */
    fun seekGesture(seconds: Double) {
        if (!seconds.isFinite()) return
        pendingSeek.set(seconds)
        execute {
            val target = pendingSeek.getAndSet(null) ?: return@execute
            MPVLib.command("seek", target.toString(), "absolute+keyframes")
        }
    }

    /**
     * Called on finger lift. Cancels any pending coalesced seek and sends
     * a precise final position.
     */
    fun seekCommit(seconds: Double) {
        if (!seconds.isFinite()) return
        pendingSeek.set(null)
        execute {
            MPVLib.command("seek", seconds.toString(), "absolute+exact")
        }
    }

    fun seekRelative(seconds: Double) {
        if (!seconds.isFinite()) return
        execute {
            MPVLib.command("seek", seconds.toString(), "relative")
        }
    }

    fun seekRelativeCoalesced(seconds: Double) {
        if (!seconds.isFinite()) return
        // Accumulate: add to whatever is already pending
        pendingRelativeSeek.getAndUpdate { prev -> (prev ?: 0.0) + seconds }
        execute {
            val delta = pendingRelativeSeek.getAndSet(null) ?: return@execute
            MPVLib.command("seek", delta.toString(), "relative+keyframes")
        }
    }

    fun loadFile(path: String) {
        execute {
            MPVLib.command("loadfile", path)
        }
    }

    fun setAudioTrack(id: Int) {
        execute {
            val value = if (id < 0) "no" else id.toString()
            MPVLib.setPropertyString(MpvProp.AUDIO_ID, value)
        }
    }

    fun addAudioTrack(uri: String) {
        execute {
            MPVLib.command("audio-add", uri, "select")
        }
    }

    fun setSubtitleTrack(id: Int) {
        execute {
            val value = if (id < 0) "no" else id.toString()
            MPVLib.setPropertyString(MpvProp.SUBTITLE_ID, value)
        }
    }

    fun addSubtitle(uri: String) {
        execute {
            MPVLib.command("sub-add", uri, "select")
        }
    }

    fun setSpeed(speed: Double) {
        if (!speed.isFinite() || speed <= 0.0) return
        execute {
            MPVLib.setPropertyDouble(MpvProp.SPEED, speed)
        }
    }

    fun setHwdec(mode: String) {
        execute {
            MPVLib.setPropertyString(MpvProp.HWDEC, mode)
        }
    }

    fun setVolume(volume: Int) {
        execute {
            MPVLib.setPropertyDouble(MpvProp.VOLUME, volume.toDouble())
        }
    }

    fun setSubtitleAppearance(size: Float, position: Float) {
        if (!size.isFinite() || !position.isFinite()) return
        val pos = (100.0 - (position * 100.0)).coerceIn(0.0, 100.0)
        execute {
            MPVLib.setPropertyDouble("sub-scale", size.toDouble())
            MPVLib.setPropertyDouble("sub-pos", pos)
        }
    }

    fun setVideoZoom(zoom: Float) {
        if (!zoom.isFinite()) return
        execute { MPVLib.setPropertyDouble("video-zoom", zoom.toDouble()) }
    }

    fun setVideoPan(panX: Float, panY: Float) {
        if (!panX.isFinite() || !panY.isFinite()) return
        execute {
            MPVLib.setPropertyDouble("video-pan-x", panX.toDouble())
            MPVLib.setPropertyDouble("video-pan-y", panY.toDouble())
        }
    }

    /**
     * Reads the corrected display aspect ratio off the executor thread and delivers
     * the result via [onResult] posted back to the main thread.
     *
     * Usage from ViewModel (main thread):
     *   executor.getVideoAspect { aspect -> _aspectRatio.value = aspect }
     */
    fun getVideoAspect(onResult: (Double?) -> Unit) {
        execute {
            val rawAspect = runCatching { MPVLib.getPropertyDouble("video-params/aspect") }.getOrNull()
            val rotate    = runCatching { MPVLib.getPropertyInt("video-params/rotate") }.getOrNull() ?: 0

            val aspect = if (rawAspect == null || rawAspect < 0.001) {
                val w = runCatching {
                    MPVLib.getPropertyInt("video-params/w") ?: MPVLib.getPropertyInt("width") ?: 0
                }.getOrDefault(0)
                val h = runCatching {
                    MPVLib.getPropertyInt("video-params/h") ?: MPVLib.getPropertyInt("height") ?: 0
                }.getOrDefault(0)
                if (w > 0 && h > 0) w.toDouble() / h.toDouble() else null
            } else rawAspect

            val result = if (aspect == null || aspect <= 0.001) null
                         else if (kotlin.math.abs(rotate) % 180 == 90) 1.0 / aspect else aspect
            mainHandler.post { onResult(result) }
        }
    }

    /**
     * Routes a hardware KeyEvent to MPV's input system.
     * Call this from PlayerActivity.onKeyDown / onKeyUp.
     * Returns true if the event was consumed by MPV, false if the caller should handle it.
     *
     * Handles: modifier keys, dead keys, key-repeat suppression, Shift/Ctrl/Alt/Meta chords.
     * Requires: `is.xyz.mpv.KeyMapping` from the mpv-android AAR.
     */
    @Suppress("DEPRECATION")
    fun onKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
            return false
        }
        var mapped = KeyMapping[event.keyCode]
        if (mapped == null) {
            if (!event.isPrintingKey) return false
            val ch = event.unicodeChar
            if (ch == 0 || (ch and KeyCharacterMap.COMBINING_ACCENT != 0)) return false  // dead key or 0
            mapped = ch.toChar().toString()
        }
        // Suppress auto-repeat — MPV handles its own repeat internally
        if (event.repeatCount > 0) return true

        val mods = buildList {
            if (event.isShiftPressed) add("shift")
            if (event.isCtrlPressed)  add("ctrl")
            if (event.isAltPressed)   add("alt")
            if (event.isMetaPressed)  add("meta")
            add(mapped)
        }
        val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
        // MPV key commands are safe to fire from any thread, but queue through executor
        // to preserve ordering with other commands.
        execute { MPVLib.command(action, mods.joinToString("+")) }
        return true
    }

    fun shutdown() {
        executor.shutdown()
    }

    companion object {
        private const val TAG = "MpvCommandExecutor"
    }
}
