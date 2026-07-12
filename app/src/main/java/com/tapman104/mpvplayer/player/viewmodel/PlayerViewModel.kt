package com.tapman104.mpvplayer.player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tapman104.mpvplayer.player.engine.PlayerAction
import com.tapman104.mpvplayer.player.engine.PlayerEngine
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import mpv.potato.tapman104.player.model.QuickActionsPosition
import mpv.potato.tapman104.player.model.ViewMode

/**
 * Thin lifecycle bridge between the Android ViewModel lifecycle and [PlayerEngine].
 *
 * Responsibilities:
 *  - Expose [PlayerEngine] state flows to the UI layer.
 *  - Forward [dispatch] calls to [PlayerEngine].
 *  - Call [PlayerEngine.destroy] when the ViewModel is cleared.
 *
 * No playback logic. No business logic. No MpvEventListener.
 */
class PlayerViewModel(
    application: Application,
    private val engine: PlayerEngine,
) : AndroidViewModel(application) {

    // ── State flows ───────────────────────────────────────────────────────────

    val playerState = engine.state
    val positionState = engine.positionState
    val playlistState = engine.playlistState
    val subtitleAppearance = engine.subtitleAppearance
    val preferredSubtitleLang = engine.preferredSubtitleLang

    // ── Preference flows (read by PlayerActivity) ─────────────────────────────

    val resumePlayback = engine.resumePlayback
    val backgroundPlay = engine.backgroundPlay
    val doubleTapSeekSeconds = engine.doubleTapSeekSeconds
    val swipeToSeek = engine.swipeToSeek
    val brightnessSwipe = engine.brightnessSwipe
    val volumeSwipe = engine.volumeSwipe
    val longPress2x = engine.longPress2x

    val quickActionsPosition: StateFlow<QuickActionsPosition> =
        engine.quickActionsPosition
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickActionsPosition.TOP_RIGHT)

    // ── Engine reference (needed by PlayerActivity for surface wiring) ────────

    val controller get() = engine.controller

    // ── Dispatch ──────────────────────────────────────────────────────────────

    fun dispatch(action: PlayerAction) = engine.dispatch(action)

    // ── Subtitle appearance delegates (not PlayerAction variants) ─────────────

    fun setSubtitleSize(size: Float) = engine.setSubtitleSize(size)
    fun setSubtitlePosition(position: Float) = engine.setSubtitlePosition(position)
    fun resetSubtitleAppearance() = engine.resetSubtitleAppearance()
    fun setSubtitleFontColor(color: String) = engine.setSubtitleFontColor(color)
    fun setSubtitleBold(bold: Boolean) = engine.setSubtitleBold(bold)
    fun setSubtitleBorderStyle(style: String) = engine.setSubtitleBorderStyle(style)
    fun setSubtitleBorderSize(size: Float) = engine.setSubtitleBorderSize(size)
    fun setSubtitleShadow(shadow: Float) = engine.setSubtitleShadow(shadow)
    fun setSubtitleBackgroundAlpha(alpha: Float) = engine.setSubtitleBackgroundAlpha(alpha)
    fun setSubtitleAppearance(size: Float, position: Float) = engine.setSubtitleAppearance(size, position)
    fun setPreferredSubtitleLanguage(lang: String) = engine.setPreferredSubtitleLanguage(lang)

    // ── Resume position delegates ─────────────────────────────────────────────

    fun saveCurrentPosition(filePath: String, positionMs: Long) =
        engine.saveCurrentPosition(filePath, positionMs)

    fun loadResumePosition(filePath: String, onResult: (Long?) -> Unit) =
        engine.loadResumePosition(filePath, onResult)

    fun clearResumePosition(filePath: String) =
        engine.clearResumePosition(filePath)

    // ── View mode & rotation state ────────────────────────────────────────────

    private val _viewMode = MutableStateFlow(ViewMode.FIT)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private var currentRotation = 0

    fun cycleViewMode() {
        val nextIndex = (_viewMode.value.ordinal + 1) % ViewMode.entries.size
        val nextMode = ViewMode.entries[nextIndex]
        _viewMode.value = nextMode

        controller.executor.execute {
            val panScan = nextMode.mpvPanScan ?: 0f
            MPVLib.setPropertyDouble("video-pan-scan", panScan.toDouble())
            MPVLib.setPropertyDouble("panscan", panScan.toDouble())
            when (val aspect = nextMode.mpvAspect) {
                null -> MPVLib.setPropertyString("video-aspect-override", "no")
                -1f -> MPVLib.setPropertyString("video-aspect-override", "-1")
                else -> MPVLib.setPropertyString("video-aspect-override", aspect.toString())
            }
        }
    }

    fun toggleVideoRotate() {
        currentRotation = if (currentRotation == 0) 90 else 0
        controller.executor.execute {
            MPVLib.setPropertyString("video-rotate", currentRotation.toString())
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        engine.destroy()
        super.onCleared()
    }
}
