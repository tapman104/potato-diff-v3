package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVNode
import android.util.Log
import com.tapman104.mpvplayer.player.model.AudioTrack
import com.tapman104.mpvplayer.player.model.SubtitleTrack

object TrackListParser {
    fun parseAudioTracks(trackListNode: MPVNode): List<AudioTrack> {
        val list = ArrayList<AudioTrack>()
        try {
            val arr = trackListNode.asArray() ?: return emptyList()
            for (node in arr) {
                val type = node.get("type")?.asString() ?: continue
                if (type == "audio") {
                    val id = node.get("id")?.asInt()?.toInt() ?: continue
                    val title = node.get("title")?.asString() ?: node.get("codec")?.asString() ?: "Audio Track $id"
                    val lang = node.get("lang")?.asString() ?: "unknown"
                    val selected = node.get("selected")?.asBoolean() ?: false
                    list.add(AudioTrack(id = id, title = title, lang = lang, isSelected = selected))
                }
            }
        } catch (e: Exception) {
            Log.e("TrackListParser", "Failed to parse audio tracks", e)
        }
        return list
    }

    fun parseSubtitleTracks(trackListNode: MPVNode): List<SubtitleTrack> {
        val list = ArrayList<SubtitleTrack>()
        try {
            val arr = trackListNode.asArray() ?: return emptyList()
            for (node in arr) {
                val type = node.get("type")?.asString() ?: continue
                if (type == "sub") {
                    val id = node.get("id")?.asInt()?.toInt() ?: continue
                    val title = node.get("title")?.asString() ?: node.get("codec")?.asString() ?: "Subtitle Track $id"
                    val lang = node.get("lang")?.asString() ?: "unknown"
                    val selected = node.get("selected")?.asBoolean() ?: false
                    list.add(SubtitleTrack(id = id, title = title, lang = lang, isSelected = selected))
                }
            }
        } catch (e: Exception) {
            Log.e("TrackListParser", "Failed to parse subtitle tracks", e)
        }
        return list
    }
}
