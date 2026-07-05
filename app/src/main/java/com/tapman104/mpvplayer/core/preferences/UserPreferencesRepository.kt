package com.tapman104.mpvplayer.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        const val DEFAULT_SUBTITLE_LANGUAGE = "en"

        val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        val SUBTITLE_POSITION = floatPreferencesKey("subtitle_position")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")

        const val DEFAULT_SUBTITLE_SIZE = 1.1f
        const val DEFAULT_SUBTITLE_POSITION = 0.07f  // 0f = bottom, 1f = top
        const val DEFAULT_RESUME_PLAYBACK = true

        val DECODE_MODE = stringPreferencesKey("decode_mode")
        const val DEFAULT_DECODE_MODE = "mediacodec"
    }

    /** Emits the saved subtitle language preference, defaulting to "en". */
    val subtitleLanguage: Flow<String> = context.userPrefsDataStore.data.map { prefs ->
        prefs[SUBTITLE_LANGUAGE] ?: DEFAULT_SUBTITLE_LANGUAGE
    }

    val subtitleSize: Flow<Float> = context.userPrefsDataStore.data.map { prefs ->
        prefs[SUBTITLE_SIZE] ?: DEFAULT_SUBTITLE_SIZE
    }

    val subtitlePosition: Flow<Float> = context.userPrefsDataStore.data.map { prefs ->
        prefs[SUBTITLE_POSITION] ?: DEFAULT_SUBTITLE_POSITION
    }

    val resumePlayback: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[RESUME_PLAYBACK] ?: DEFAULT_RESUME_PLAYBACK
    }

    val decodeMode: Flow<String> = context.userPrefsDataStore.data.map { prefs ->
        prefs[DECODE_MODE] ?: DEFAULT_DECODE_MODE
    }

    suspend fun setSubtitleLanguage(lang: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[SUBTITLE_LANGUAGE] = lang
        }
    }

    suspend fun setSubtitleSize(size: Float) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[SUBTITLE_SIZE] = size
        }
    }

    suspend fun setSubtitlePosition(position: Float) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[SUBTITLE_POSITION] = position
        }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[RESUME_PLAYBACK] = enabled
        }
    }

    suspend fun setDecodeMode(mode: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[DECODE_MODE] = mode
        }
    }
}
