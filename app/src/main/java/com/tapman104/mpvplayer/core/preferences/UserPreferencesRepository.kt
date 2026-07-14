package com.tapman104.mpvplayer.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.tapman104.mpvplayer.player.model.QuickActionsPosition

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

        val DEBAND_FILTER = booleanPreferencesKey("deband_filter")
        const val DEFAULT_DEBAND_FILTER = false

        val VIDEO_SCALE = stringPreferencesKey("video_scale")
        const val DEFAULT_VIDEO_SCALE = "lanczos"

        val VOLUME_BOOST = intPreferencesKey("volume_boost")
        const val DEFAULT_VOLUME_BOOST = 100

        val PITCH_CORRECTION = booleanPreferencesKey("pitch_correction")
        const val DEFAULT_PITCH_CORRECTION = true

        val AUDIO_OUTPUT_DRIVER = stringPreferencesKey("audio_output_driver")
        const val DEFAULT_AUDIO_OUTPUT_DRIVER = "audiotrack"

        val DOUBLE_TAP_SEEK_SECONDS = intPreferencesKey("double_tap_seek_seconds")
        const val DEFAULT_DOUBLE_TAP_SEEK_SECONDS = 10

        val SWIPE_TO_SEEK = booleanPreferencesKey("swipe_to_seek")
        const val DEFAULT_SWIPE_TO_SEEK = true

        val BRIGHTNESS_SWIPE = booleanPreferencesKey("brightness_swipe")
        const val DEFAULT_BRIGHTNESS_SWIPE = true

        val VOLUME_SWIPE = booleanPreferencesKey("volume_swipe")
        const val DEFAULT_VOLUME_SWIPE = true

        val LONG_PRESS_2X = booleanPreferencesKey("long_press_2x")
        const val DEFAULT_LONG_PRESS_2X = true

        val BACKGROUND_PLAY = stringPreferencesKey("background_play")
        const val DEFAULT_BACKGROUND_PLAY = "off"

        val QUICK_ACTIONS_POSITION = stringPreferencesKey("quick_actions_position")
        val DEFAULT_QUICK_ACTIONS_POSITION = QuickActionsPosition.TOP_RIGHT
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

    val debandFilter: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[DEBAND_FILTER] ?: DEFAULT_DEBAND_FILTER
    }

    val videoScale: Flow<String> = context.userPrefsDataStore.data.map { prefs ->
        prefs[VIDEO_SCALE] ?: DEFAULT_VIDEO_SCALE
    }

    val volumeBoost: Flow<Int> = context.userPrefsDataStore.data.map { prefs ->
        prefs[VOLUME_BOOST] ?: DEFAULT_VOLUME_BOOST
    }

    val pitchCorrection: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[PITCH_CORRECTION] ?: DEFAULT_PITCH_CORRECTION
    }

    val audioOutputDriver: Flow<String> = context.userPrefsDataStore.data.map { prefs ->
        prefs[AUDIO_OUTPUT_DRIVER] ?: DEFAULT_AUDIO_OUTPUT_DRIVER
    }

    val doubleTapSeekSeconds: Flow<Int> = context.userPrefsDataStore.data.map { prefs ->
        prefs[DOUBLE_TAP_SEEK_SECONDS] ?: DEFAULT_DOUBLE_TAP_SEEK_SECONDS
    }

    val swipeToSeek: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[SWIPE_TO_SEEK] ?: DEFAULT_SWIPE_TO_SEEK
    }

    val brightnessSwipe: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[BRIGHTNESS_SWIPE] ?: DEFAULT_BRIGHTNESS_SWIPE
    }

    val volumeSwipe: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[VOLUME_SWIPE] ?: DEFAULT_VOLUME_SWIPE
    }

    val longPress2x: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[LONG_PRESS_2X] ?: DEFAULT_LONG_PRESS_2X
    }

    val backgroundPlay: Flow<String> = context.userPrefsDataStore.data.map { prefs ->
        prefs[BACKGROUND_PLAY] ?: DEFAULT_BACKGROUND_PLAY
    }

    val quickActionsPosition: Flow<QuickActionsPosition> = context.userPrefsDataStore.data.map { prefs ->
        val raw = prefs[QUICK_ACTIONS_POSITION] ?: QuickActionsPosition.TOP_RIGHT.name
        runCatching { QuickActionsPosition.valueOf(raw) }.getOrDefault(QuickActionsPosition.TOP_RIGHT)
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

    suspend fun setDebandFilter(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[DEBAND_FILTER] = enabled
        }
    }

    suspend fun setVideoScale(scale: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[VIDEO_SCALE] = scale
        }
    }

    suspend fun setVolumeBoost(boost: Int) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[VOLUME_BOOST] = boost
        }
    }

    suspend fun setPitchCorrection(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[PITCH_CORRECTION] = enabled
        }
    }

    suspend fun setAudioOutputDriver(driver: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[AUDIO_OUTPUT_DRIVER] = driver
        }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[DOUBLE_TAP_SEEK_SECONDS] = seconds
        }
    }

    suspend fun setSwipeToSeek(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[SWIPE_TO_SEEK] = enabled
        }
    }

    suspend fun setBrightnessSwipe(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[BRIGHTNESS_SWIPE] = enabled
        }
    }

    suspend fun setVolumeSwipe(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[VOLUME_SWIPE] = enabled
        }
    }

    suspend fun setLongPress2x(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[LONG_PRESS_2X] = enabled
        }
    }
    suspend fun setBackgroundPlay(mode: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[BACKGROUND_PLAY] = mode
        }
    }

    suspend fun setQuickActionsPosition(position: QuickActionsPosition) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[QUICK_ACTIONS_POSITION] = position.name
        }
    }
}

