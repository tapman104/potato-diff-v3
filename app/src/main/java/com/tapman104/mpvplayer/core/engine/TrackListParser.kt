package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVNode
import android.util.Log
import com.tapman104.mpvplayer.player.model.AudioTrack
import com.tapman104.mpvplayer.player.model.SubtitleTrack

object TrackListParser {

    fun parseAudioTracks(trackListNode: MPVNode): List<AudioTrack> =
        parseTracks(trackListNode, type = "audio") { id, title, lang, selected ->
            AudioTrack(id = id, title = title, lang = lang, isSelected = selected)
        }

    fun parseSubtitleTracks(trackListNode: MPVNode): List<SubtitleTrack> =
        parseTracks(trackListNode, type = "sub") { id, title, lang, selected ->
            SubtitleTrack(id = id, title = title, lang = lang, isSelected = selected)
        }

    /**
     * Generic track parser. Iterates [trackListNode] and calls [factory] for every
     * entry whose "type" field matches [type]. Returns an empty list on any error.
     */
    private inline fun <T> parseTracks(
        trackListNode: MPVNode,
        type: String,
        crossinline factory: (id: Int, title: String, lang: String, selected: Boolean) -> T
    ): List<T> {
        return try {
            val arr = trackListNode.asArray() ?: return emptyList()
            buildList {
                for (node in arr) {
                    try {
                        if (node.get("type")?.asString() != type) continue
                        // asInt() returns Long on most MPVNode implementations; .toInt() is safe for track IDs
                        val id = node.get("id")?.asInt()?.toInt() ?: continue
                        val title = node.get("title")?.asString()?.takeIf { it.isNotBlank() }
                            ?: node.get("codec")?.asString()?.takeIf { it.isNotBlank() }
                            ?: "${type.replaceFirstChar { it.uppercaseChar() }} Track $id"
                        val lang = node.get("lang")?.asString() ?: "unknown"
                        val selected = node.get("selected")?.asBoolean() ?: false
                        add(factory(id, title, lang, selected))
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed track node", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $type tracks", e)
            emptyList()
        }
    }

    private const val TAG = "TrackListParser"
}
