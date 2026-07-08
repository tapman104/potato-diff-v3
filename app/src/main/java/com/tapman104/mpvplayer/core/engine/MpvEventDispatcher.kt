package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

interface MpvEventListener {
    fun onFileLoaded()
    fun onPlaybackStarted()
    fun onPlaybackStopped(endReason: Int)
    fun onPropertyChange(name: String, value: Any?)
    fun onError(message: String)
    /**
     * Called when MPV's VO crashes fatally (e.g. surface destroyed during lock).
     * TODO: dispatch this from [MpvEventDispatcher.event] when a suitable MPV
     *  event or error signal is available (e.g. a VO-reinit failure).
     */
    fun onVoLost() {}
}

class MpvEventDispatcher : MPVLib.EventObserver {
    // CopyOnWriteArrayList is purpose-built for read-heavy, write-rare listener lists:
    // iteration never blocks and is always safe without an explicit snapshot copy.
    private val listeners = CopyOnWriteArrayList<MpvEventListener>()

    fun addListener(listener: MpvEventListener) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: MpvEventListener) {
        listeners.remove(listener)
    }

    private inline fun notifyListeners(block: (MpvEventListener) -> Unit) {
        listeners.forEach { listener ->
            try {
                block(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching MPV event to listener", e)
            }
        }
    }

    override fun eventProperty(name: String) {
        notifyListeners { it.onPropertyChange(name, null) }
    }

    override fun eventProperty(name: String, value: Long) {
        notifyListeners { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: Boolean) {
        notifyListeners { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: String) {
        notifyListeners { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: Double) {
        notifyListeners { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: MPVNode) {
        notifyListeners { it.onPropertyChange(name, value) }
    }

    override fun event(eventId: Int, eventNode: MPVNode) {
        Log.d(TAG, "Received MPV event: $eventId")
        when (eventId) {
            MpvEvent.FILE_LOADED      -> notifyListeners { it.onFileLoaded() }
            MpvEvent.PLAYBACK_RESTART -> notifyListeners { it.onPlaybackStarted() }
            MpvEvent.END_FILE -> {
                val reason = try {
                    eventNode.get("reason")?.asInt()?.toInt() ?: 0
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse end-file reason", e)
                    0
                }
                notifyListeners { it.onPlaybackStopped(reason) }
            }
            else -> { /* other events are handled via eventProperty callbacks */ }
        }
    }

    companion object {
        private const val TAG = "MpvEventDispatcher"
    }
}
