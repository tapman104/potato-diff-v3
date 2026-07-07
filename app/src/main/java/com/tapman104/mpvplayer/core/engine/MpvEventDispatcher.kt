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

    override fun eventProperty(name: String) {
        listeners.forEach { it.onPropertyChange(name, null) }
    }

    override fun eventProperty(name: String, value: Long) {
        listeners.forEach { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: Boolean) {
        listeners.forEach { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: String) {
        listeners.forEach { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: Double) {
        listeners.forEach { it.onPropertyChange(name, value) }
    }

    override fun eventProperty(name: String, value: MPVNode) {
        listeners.forEach { it.onPropertyChange(name, value) }
    }

    override fun event(eventId: Int, eventNode: MPVNode) {
        Log.d(TAG, "Received MPV event: $eventId")
        when (eventId) {
            MpvEvent.FILE_LOADED      -> listeners.forEach { it.onFileLoaded() }
            MpvEvent.PLAYBACK_RESTART -> listeners.forEach { it.onPlaybackStarted() }
            MpvEvent.END_FILE -> {
                val reason = try {
                    eventNode.get("reason")?.asInt()?.toInt() ?: 0
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse end-file reason", e)
                    0
                }
                listeners.forEach { it.onPlaybackStopped(reason) }
            }
            else -> { /* other events are handled via eventProperty callbacks */ }
        }
    }

    companion object {
        private const val TAG = "MpvEventDispatcher"
    }
}
