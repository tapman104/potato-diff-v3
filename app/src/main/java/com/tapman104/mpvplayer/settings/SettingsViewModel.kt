package com.tapman104.mpvplayer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.tapman104.mpvplayer.player.model.QuickActionsPosition

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private fun <T> Flow<T>.asState(default: T) =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)

    val subtitleLanguage = userPreferencesRepository.subtitleLanguage
        .asState(UserPreferencesRepository.DEFAULT_SUBTITLE_LANGUAGE)

    val subtitleSize = userPreferencesRepository.subtitleSize
        .asState(UserPreferencesRepository.DEFAULT_SUBTITLE_SIZE)

    val subtitlePosition = userPreferencesRepository.subtitlePosition
        .asState(UserPreferencesRepository.DEFAULT_SUBTITLE_POSITION)

    val resumePlayback = userPreferencesRepository.resumePlayback
        .asState(UserPreferencesRepository.DEFAULT_RESUME_PLAYBACK)

    val decodeMode = userPreferencesRepository.decodeMode
        .asState(UserPreferencesRepository.DEFAULT_DECODE_MODE)

    val debandFilter = userPreferencesRepository.debandFilter
        .asState(UserPreferencesRepository.DEFAULT_DEBAND_FILTER)

    val videoScale = userPreferencesRepository.videoScale
        .asState(UserPreferencesRepository.DEFAULT_VIDEO_SCALE)

    val volumeBoost = userPreferencesRepository.volumeBoost
        .asState(UserPreferencesRepository.DEFAULT_VOLUME_BOOST)

    val pitchCorrection = userPreferencesRepository.pitchCorrection
        .asState(UserPreferencesRepository.DEFAULT_PITCH_CORRECTION)

    val audioOutputDriver = userPreferencesRepository.audioOutputDriver
        .asState(UserPreferencesRepository.DEFAULT_AUDIO_OUTPUT_DRIVER)

    val doubleTapSeekSeconds = userPreferencesRepository.doubleTapSeekSeconds
        .asState(UserPreferencesRepository.DEFAULT_DOUBLE_TAP_SEEK_SECONDS)

    val swipeToSeek = userPreferencesRepository.swipeToSeek
        .asState(UserPreferencesRepository.DEFAULT_SWIPE_TO_SEEK)

    val brightnessSwipe = userPreferencesRepository.brightnessSwipe
        .asState(UserPreferencesRepository.DEFAULT_BRIGHTNESS_SWIPE)

    val volumeSwipe = userPreferencesRepository.volumeSwipe
        .asState(UserPreferencesRepository.DEFAULT_VOLUME_SWIPE)

    val longPress2x = userPreferencesRepository.longPress2x
        .asState(UserPreferencesRepository.DEFAULT_LONG_PRESS_2X)
    val backgroundPlay = userPreferencesRepository.backgroundPlay
        .asState(UserPreferencesRepository.DEFAULT_BACKGROUND_PLAY)

    val quickActionsPosition = userPreferencesRepository.quickActionsPosition
        .asState(UserPreferencesRepository.DEFAULT_QUICK_ACTIONS_POSITION)

    fun setSubtitleLanguage(lang: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSubtitleLanguage(lang)
        }
    }

    fun setSubtitleSize(size: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setSubtitleSize(size)
        }
    }

    fun setSubtitlePosition(position: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setSubtitlePosition(position)
        }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumePlayback(enabled)
        }
    }

    fun setDecodeMode(mpvValue: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDecodeMode(mpvValue)
        }
    }

    fun setDebandFilter(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDebandFilter(enabled)
        }
    }

    fun setVideoScale(scale: String) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoScale(scale)
        }
    }

    fun setVolumeBoost(boost: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setVolumeBoost(boost)
        }
    }

    fun setPitchCorrection(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPitchCorrection(enabled)
        }
    }

    fun setAudioOutputDriver(driver: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAudioOutputDriver(driver)
        }
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setDoubleTapSeekSeconds(seconds)
        }
    }

    fun setSwipeToSeek(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSwipeToSeek(enabled)
        }
    }

    fun setBrightnessSwipe(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBrightnessSwipe(enabled)
        }
    }

    fun setVolumeSwipe(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setVolumeSwipe(enabled)
        }
    }

    fun setLongPress2x(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setLongPress2x(enabled)
        }
    }
    fun setBackgroundPlay(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBackgroundPlay(mode)
        }
    }

    fun setQuickActionsPosition(position: QuickActionsPosition) {
        viewModelScope.launch {
            userPreferencesRepository.setQuickActionsPosition(position)
        }
    }
}

class SettingsViewModelFactory(private val userPreferencesRepository: UserPreferencesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
