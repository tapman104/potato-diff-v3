package com.tapman104.mpvplayer.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val subtitleLanguage = userPreferencesRepository.subtitleLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_SUBTITLE_LANGUAGE)

    val subtitleSize = userPreferencesRepository.subtitleSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_SUBTITLE_SIZE)

    val subtitlePosition = userPreferencesRepository.subtitlePosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_SUBTITLE_POSITION)

    val resumePlayback = userPreferencesRepository.resumePlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_RESUME_PLAYBACK)

    val decodeMode = userPreferencesRepository.decodeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_DECODE_MODE)

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
