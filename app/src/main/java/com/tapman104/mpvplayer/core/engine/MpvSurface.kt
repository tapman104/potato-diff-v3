package com.tapman104.mpvplayer.core.engine

import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.util.Log
import `is`.xyz.mpv.MPVLib
import java.util.concurrent.atomic.AtomicReference

class MpvSurface(private val executor: MpvCommandExecutor) : SurfaceHolder.Callback {
    /** The surface that is currently presented to MPV. Written on the main thread only. */
    private var attachedSurface: Surface? = null

    /**
     * Holds the surface being queued for attachment before the executor task runs;
     * cleared inside the executor task once MPVLib.attachSurface() has returned.
     * Allows surfaceChanged to skip a redundant re-attach when the holder delivers
     * the same surface object we already have in flight.
     * AtomicReference ensures the executor thread's write is immediately visible to
     * the main thread without requiring @Volatile + a separate write barrier.
     */
    private val pendingAttachSurface = AtomicReference<Surface?>(null)

    private var voInUse: String = DEFAULT_VO

    fun setVo(vo: String) {
        voInUse = vo
    }

    var onSurfaceReady: (() -> Unit)? = null

    fun hasSurface(): Boolean = attachedSurface != null || pendingAttachSurface.get() != null

    override fun surfaceCreated(holder: SurfaceHolder) {
        val surface = holder.surface ?: return
        if (!surface.isValid) return

        attachedSurface = surface
        pendingAttachSurface.set(surface)
        val gen = executor.nextSurfaceGeneration()
        val callback = onSurfaceReady
        val vo = voInUse
        executor.execute {
            Log.d(TAG, "attachSurface gen=$gen")
            MPVLib.attachSurface(surface)
            MPVLib.setOptionString("force-window", "yes")
            // If no file is pending (recovery after lock/recents),
            // re-enable the VO so mpv resumes rendering.
            // onSurfaceReady will only load a file if pendingFileUri != null.
            MPVLib.setPropertyString("vo", vo)
            pendingAttachSurface.set(null)
            mainHandler.post { callback?.invoke() }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged: width=$width, height=$height")
        val size = "${width}x${height}"
        executor.execute {
            MPVLib.setPropertyString("android-surface-size", size)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        attachedSurface = null
        // Disable VO first so mpv stops rendering before we detach.
        // This matches BaseMPVView from mpv-android and avoids a race
        // where mpv tries to write to the surface after it is gone.
        executor.execute {
            runCatching { MPVLib.setPropertyString("vo", "null") }
            runCatching { MPVLib.setPropertyString("force-window", "no") }
        }
        executor.detachSurface()
    }

    companion object {
        private const val TAG = "MpvSurface"
        private const val DEFAULT_VO = "gpu"
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
