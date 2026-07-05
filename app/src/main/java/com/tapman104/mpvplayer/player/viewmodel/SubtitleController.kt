package com.tapman104.mpvplayer.player.viewmodel

import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.core.engine.MpvCommandExecutor
import com.tapman104.mpvplayer.player.model.SubtitleTrack
import com.tapman104.mpvplayer.player.state.SubtitleAppearanceState

class SubtitleController(
    private val executor: MpvCommandExecutor,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val coroutineScope: CoroutineScope,
    private val onPlayerStateUpdate: (subtitleSize: Float, subtitlePosition: Float) -> Unit
) {

    private val _subtitleAppearance = MutableStateFlow(SubtitleAppearanceState())
    val subtitleAppearance: StateFlow<SubtitleAppearanceState> = _subtitleAppearance.asStateFlow()

    private val _preferredSubtitleLang = MutableStateFlow(UserPreferencesRepository.DEFAULT_SUBTITLE_LANGUAGE)
    val preferredSubtitleLang: StateFlow<String> = _preferredSubtitleLang.asStateFlow()

    init {
        coroutineScope.launch {
            userPreferencesRepository.subtitleLanguage.collect {
                _preferredSubtitleLang.value = it
            }
        }

        coroutineScope.launch {
            combine(
                userPreferencesRepository.subtitleSize,
                userPreferencesRepository.subtitlePosition
            ) { size, position -> Pair(size, position) }
                .collect { (size, position) ->
                    onPlayerStateUpdate(size, position)
                    executor.setSubtitleAppearance(size, position)
                }
        }
    }

    fun setSubtitleFontColor(color: String) {
        _subtitleAppearance.update { it.copy(fontColor = color) }
        executor.execute {
            MPVLib.setPropertyString("sub-color", color)
        }
    }

    fun setSubtitleBold(bold: Boolean) {
        _subtitleAppearance.update { it.copy(bold = bold) }
        executor.execute {
            MPVLib.setPropertyBoolean("sub-bold", bold)
        }
    }

    fun setSubtitleBorderStyle(style: String) {
        _subtitleAppearance.update { it.copy(borderStyle = style) }
        executor.execute {
            MPVLib.setPropertyString("sub-border-style", style)
        }
    }

    fun setSubtitleBorderSize(size: Float) {
        _subtitleAppearance.update { it.copy(borderSize = size) }
        executor.execute {
            MPVLib.setPropertyDouble("sub-border-size", size.toDouble())
        }
    }

    fun setSubtitleShadow(shadow: Float) {
        _subtitleAppearance.update { it.copy(shadow = shadow) }
        executor.execute {
            MPVLib.setPropertyDouble("sub-shadow-offset", shadow.toDouble())
        }
    }

    fun setSubtitleBackgroundAlpha(alpha: Float) {
        _subtitleAppearance.update { it.copy(backgroundAlpha = alpha) }
        // sub-back-color expects an ASS ARGB color string "#AARRggBB".
        // We keep RGB as black (000000) and only vary the alpha channel.
        val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
        val color = String.format("#%02X000000", alphaInt)
        executor.execute {
            MPVLib.setPropertyString("sub-back-color", color)
        }
    }

    fun setSubtitleAppearance(size: Float, position: Float) {
        onPlayerStateUpdate(size, position)
        executor.setSubtitleAppearance(size, position)
        coroutineScope.launch {
            userPreferencesRepository.setSubtitleSize(size)
            userPreferencesRepository.setSubtitlePosition(position)
        }
    }

    fun resetSubtitleAppearance() {
        val size = UserPreferencesRepository.DEFAULT_SUBTITLE_SIZE
        val position = UserPreferencesRepository.DEFAULT_SUBTITLE_POSITION
        onPlayerStateUpdate(size, position)
        executor.setSubtitleAppearance(size, position)
        coroutineScope.launch {
            userPreferencesRepository.setSubtitleSize(size)
            userPreferencesRepository.setSubtitlePosition(position)
        }
    }

    fun autoSelectSubtitle(tracks: List<SubtitleTrack>) {
        val lang = _preferredSubtitleLang.value
        val match = tracks.firstOrNull { it.lang.lowercase().startsWith(lang.lowercase()) }
        if (match != null) {
            executor.setSubtitleTrack(match.id)
        }
    }

    fun setPreferredSubtitleLanguage(lang: String) {
        coroutineScope.launch {
            userPreferencesRepository.setSubtitleLanguage(lang)
        }
    }

    fun setSubtitleSize(size: Float) {
        coroutineScope.launch {
            userPreferencesRepository.setSubtitleSize(size)
        }
    }

    fun setSubtitlePosition(position: Float) {
        coroutineScope.launch {
            userPreferencesRepository.setSubtitlePosition(position)
        }
    }
}
