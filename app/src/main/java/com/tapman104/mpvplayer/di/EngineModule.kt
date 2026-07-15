package com.tapman104.mpvplayer.di

import android.app.Application
import android.content.Context
import android.media.AudioManager
import com.tapman104.mpvplayer.core.engine.EventProcessor
import com.tapman104.mpvplayer.core.engine.MpvCommandExecutor
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.engine.MpvEventDispatcher
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.player.engine.PlayerEngine
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PositionState
import com.tapman104.mpvplayer.player.viewmodel.PlaybackCoordinator
import com.tapman104.mpvplayer.player.viewmodel.PlaylistManager
import com.tapman104.mpvplayer.player.viewmodel.ResumePositionManager
import com.tapman104.mpvplayer.player.viewmodel.SubtitleController
import com.tapman104.mpvplayer.player.viewmodel.TrackCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton
import kotlin.math.roundToInt

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideMpvController(@ApplicationContext context: Context): MpvController = MpvController(context)

    @Provides
    @Singleton
    fun provideMpvCommandExecutor(c: MpvController): MpvCommandExecutor = c.executor

    @Provides
    @Singleton
    fun provideMpvEventDispatcher(c: MpvController): MpvEventDispatcher = c.dispatcher

    @Provides
    @Singleton
    fun providePlayerEngine(
        @ApplicationContext context: Context,
        controller: MpvController,
        executor: MpvCommandExecutor,
        dispatcher: MpvEventDispatcher,
        playlistManager: PlaylistManager,
        resumePositionManager: ResumePositionManager,
        preferencesRepository: UserPreferencesRepository
    ): PlayerEngine {
        val application = context.applicationContext as Application
        val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val initialVolumePct = audioManager?.let { am ->
            val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) ((currentVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt() else 100
        } ?: 100

        val playerState = MutableStateFlow(PlayerState(volume = initialVolumePct))
        val positionState = MutableStateFlow(PositionState())

        val subtitleController = SubtitleController(
            executor = executor,
            preferencesRepository = preferencesRepository,
            scope = engineScope
        )

        val eventProcessor = EventProcessor(
            scope = engineScope,
            playerState = playerState,
            positionState = positionState,
            onPlaybackEnded = {
                if (controller.surface.hasSurface()) {
                    playlistManager.onPlaybackEnded()
                }
            },
            onTracksLoaded = { subtitleTracks ->
                subtitleController.autoSelectSubtitle(subtitleTracks)
            }
        )

        return PlayerEngine(
            application = application,
            controller = controller,
            eventProcessor = eventProcessor,
            playlistManager = playlistManager,
            subtitleController = subtitleController,
            resumePositionManager = resumePositionManager,
            preferencesRepository = preferencesRepository,
            scope = engineScope,
            sharedPlayerState = playerState,
            sharedPositionState = positionState
        )
    }

    @Provides
    @Singleton
    fun providePlaybackCoordinator(engine: PlayerEngine): PlaybackCoordinator = engine.playbackCoordinator

    @Provides
    @Singleton
    fun provideTrackCoordinator(engine: PlayerEngine): TrackCoordinator = engine.trackCoordinator
}
