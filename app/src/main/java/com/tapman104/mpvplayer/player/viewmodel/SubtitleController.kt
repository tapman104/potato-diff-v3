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
    private val preferencesRepository: UserPreferencesRepository,
    private val scope: CoroutineScope,
) {

    private val _subtitleAppearance = MutableStateFlow(SubtitleAppearanceState())
    val subtitleAppearance: StateFlow<SubtitleAppearanceState> = _subtitleAppearance.asStateFlow()

    private val _preferredSubtitleLang = MutableStateFlow(UserPreferencesRepository.DEFAULT_SUBTITLE_LANGUAGE)
    val preferredSubtitleLang: StateFlow<String> = _preferredSubtitleLang.asStateFlow()

    init {
        scope.launch {
            preferencesRepository.subtitleLanguage.collect {
                _preferredSubtitleLang.value = it
            }
        }

        scope.launch {
            combine(
                preferencesRepository.subtitleSize,
                preferencesRepository.subtitlePosition
            ) { size, position -> Pair(size, position) }
                .collect { (size, position) ->
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
        executor.setSubtitleAppearance(size, position)
        scope.launch {
            preferencesRepository.setSubtitleSize(size)
            preferencesRepository.setSubtitlePosition(position)
        }
    }

    fun resetSubtitleAppearance() {
        val size = UserPreferencesRepository.DEFAULT_SUBTITLE_SIZE
        val position = UserPreferencesRepository.DEFAULT_SUBTITLE_POSITION
        executor.setSubtitleAppearance(size, position)
        scope.launch {
            preferencesRepository.setSubtitleSize(size)
            preferencesRepository.setSubtitlePosition(position)
        }
    }

    fun setSubtitleTrack(id: Int) = executor.setSubtitleTrack(id)
    fun addSubtitle(uri: String) = executor.addSubtitle(uri)

    fun autoSelectSubtitle(tracks: List<SubtitleTrack>) {
        val lang = _preferredSubtitleLang.value
        val match = tracks.firstOrNull { it.lang.lowercase().startsWith(lang.lowercase()) }
        if (match != null) {
            executor.setSubtitleTrack(match.id)
        }
    }

    fun setPreferredSubtitleLanguage(lang: String) {
        scope.launch {
            preferencesRepository.setSubtitleLanguage(lang)
        }
    }

    fun setSubtitleSize(size: Float) {
        scope.launch {
            preferencesRepository.setSubtitleSize(size)
        }
    }

    fun setSubtitlePosition(position: Float) {
        scope.launch {
            preferencesRepository.setSubtitlePosition(position)
        }
    }
}
