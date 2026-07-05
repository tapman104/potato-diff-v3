package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import android.util.Log

interface MpvEventListener {
    fun onFileLoaded()
    fun onPlaybackStarted()
    fun onPlaybackStopped(endReason: Int)
    fun onPropertyChange(name: String, value: Any?)
    fun onError(message: String)
    /** Called when MPV's VO crashes fatally (e.g. surface destroyed during lock). */
    fun onVoLost() {}
}

class MpvEventDispatcher : MPVLib.EventObserver {
    private val TAG = "MpvEventDispatcher"
    private val listeners = ArrayList<MpvEventListener>()

    fun addListener(listener: MpvEventListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    fun removeListener(listener: MpvEventListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    private fun snapshot(): List<MpvEventListener> {
        synchronized(listeners) { return ArrayList(listeners) }
    }

    override fun eventProperty(name: String) {
        for (listener in snapshot()) {
            listener.onPropertyChange(name, null)
        }
    }

    override fun eventProperty(name: String, value: Long) {
        for (listener in snapshot()) {
            listener.onPropertyChange(name, value)
        }
    }

    override fun eventProperty(name: String, value: Boolean) {
        for (listener in snapshot()) {
            listener.onPropertyChange(name, value)
        }
    }

    override fun eventProperty(name: String, value: String) {
        for (listener in snapshot()) {
            listener.onPropertyChange(name, value)
        }
    }

    override fun eventProperty(name: String, value: Double) {
        for (listener in snapshot()) {
            listener.onPropertyChange(name, value)
        }
    }

    override fun eventProperty(name: String, value: MPVNode) {
        for (listener in snapshot()) {
            listener.onPropertyChange(name, value)
        }
    }

    override fun event(eventId: Int, eventNode: MPVNode) {
        Log.d(TAG, "Received MPV event: $eventId")
        val copy = snapshot()
        when (eventId) {
            MpvEvent.FILE_LOADED -> copy.forEach { it.onFileLoaded() }
            MpvEvent.PLAYBACK_RESTART -> copy.forEach { it.onPlaybackStarted() }
            MpvEvent.END_FILE -> {
                val reason = try {
                    eventNode.get("reason")?.asInt()?.toInt() ?: 0
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse end-file reason", e)
                    0
                }
                copy.forEach { it.onPlaybackStopped(reason) }
            }
            else -> {
                // Other events
            }
        }
    }
}
