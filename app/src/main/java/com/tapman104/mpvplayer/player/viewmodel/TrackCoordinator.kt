package com.tapman104.mpvplayer.player.viewmodel

import android.app.Application
import android.net.Uri
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.player.model.DecodeMode
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.util.UriResolver
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Owns audio-track selection, subtitle-track selection, subtitle sideloading,
 * and hardware-decode mode switching.
 *
 * Extracted from [com.tapman104.mpvplayer.player.engine.PlayerEngine] so that
 * [PlayerEngine.dispatch] stays thin.  This coordinator holds **no** StateFlow of
 * its own; it mutates the [sharedPlayerState] that is also observed by
 * [EventProcessor] and exposed via [PlayerEngine.state].
 *
 * Constructor injection only — no singletons.
 */
class TrackCoordinator(
    private val application: Application,
    private val controller: MpvController,
    private val sharedPlayerState: MutableStateFlow<PlayerState>,
    private val preferencesRepository: UserPreferencesRepository,
    private val scope: CoroutineScope,
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Audio tracks
    // ─────────────────────────────────────────────────────────────────────────

    fun setAudioTrack(id: Int) {
        if (sharedPlayerState.value.currentAudioTrackId == id) return
        controller.executor.setAudioTrack(id)
        sharedPlayerState.update { it.copy(currentAudioTrackId = id) }
    }

    fun addAudioTrack(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            controller.executor.addAudioTrack(path)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subtitle tracks
    // ─────────────────────────────────────────────────────────────────────────

    fun setSubtitleTrack(id: Int) {
        if (sharedPlayerState.value.currentSubtitleTrackId == id) return
        controller.executor.setSubtitleTrack(id)
        sharedPlayerState.update { it.copy(currentSubtitleTrackId = id) }
    }

    fun addSubtitle(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            val path = resolveTrackPath(uri) ?: return@launch
            controller.executor.addSubtitle(path)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Decode mode
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Switch hardware-decode mode and optionally resume playback after the switch.
     *
     * @param next The target [DecodeMode].
     * @param resumeAfter When true, [MpvController.executor.play] is called after
     *                    applying the new hwdec value (used when called from the
     *                    decode-mode picker dialog, which pauses first).
     */
    fun cycleDecodeMode(next: DecodeMode, resumeAfter: Boolean = false) {
        val wasPaused = sharedPlayerState.value.isPaused
        val mpvMode = when (next) {
            DecodeMode.HW     -> "mediacodec"
            DecodeMode.HWPlus -> "mediacodec-copy"
            DecodeMode.SW     -> "no"
        }
        controller.executor.setHwdec(mpvMode)
        if (wasPaused) {
            controller.executor.pause()
        } else if (resumeAfter) {
            controller.executor.play()
        }
        scope.launch {
            preferencesRepository.setDecodeMode(mpvMode)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // URI resolution (shared helper)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun resolveTrackPath(uri: Uri): String? {
        if (uri.scheme != "content") {
            return uri.path ?: uri.toString()
        }
        val fd = application.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val realPath = Utils.findRealPath(fd.fd)
        fd.close()

        return realPath ?: run {
            // Fallback: copy to cache so mpv can open the file via a real path
            val ext = UriResolver.getDisplayName(application, uri).substringAfterLast('.', "tmp")
            val cache = File(application.cacheDir, "ext_track_${System.currentTimeMillis()}.$ext")
            val copied = application.contentResolver.openInputStream(uri)?.use { input ->
                cache.outputStream().use { output -> input.copyTo(output) }
            } != null
            if (!copied) return null
            cache.absolutePath
        }
    }
}
