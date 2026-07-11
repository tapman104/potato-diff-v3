package com.tapman104.mpvplayer.player.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapman104.mpvplayer.core.database.AppDatabase
import com.tapman104.mpvplayer.core.engine.EventProcessor
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.player.engine.PlayerEngine
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

class PlayerViewModelFactory(
    private val application: Application,
    private val mpvController: MpvController,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(application)
        val prefsRepo = UserPreferencesRepository(application)
        val resumeManager = ResumePositionManager(db.resumePositionDao())

        // Engine owns its own supervised scope — cancelled by engine.destroy()
        val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val initialVolumePct = audioManager?.let { am ->
            val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) ((currentVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt() else 100
        } ?: 100

        // Shared mutable state stores — owned by PlayerEngine, observed by EventProcessor
        val playerState = MutableStateFlow(PlayerState(volume = initialVolumePct))
        val positionState = MutableStateFlow(PositionState())

        val playlistManager = PlaylistManager(
            context = application,
            onLoadFile = { path -> mpvController.executor.loadFile(path) },
            hasSurface = { mpvController.surface.hasSurface() }
        )

        val subtitleController = SubtitleController(
            executor = mpvController.executor,
            preferencesRepository = prefsRepo,
            scope = engineScope
        )

        val eventProcessor = EventProcessor(
            scope = engineScope,
            playerState = playerState,
            positionState = positionState,
            onPlaybackEnded = {
                if (mpvController.surface.hasSurface()) {
                    playlistManager.onPlaybackEnded()
                }
            },
            onTracksLoaded = { subtitleTracks ->
                subtitleController.autoSelectSubtitle(subtitleTracks)
            }
        )

        val engine = PlayerEngine(
            application = application,
            controller = mpvController,
            eventProcessor = eventProcessor,
            playlistManager = playlistManager,
            subtitleController = subtitleController,
            resumePositionManager = resumeManager,
            preferencesRepository = prefsRepo,
            scope = engineScope,
            sharedPlayerState = playerState,
            sharedPositionState = positionState,
        )

        return PlayerViewModel(application = application, engine = engine) as T
    }

    companion object {
        fun create(application: Application, mpvController: MpvController) =
            PlayerViewModelFactory(application, mpvController)
    }
}
