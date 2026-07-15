package com.tapman104.mpvplayer.di

import com.tapman104.mpvplayer.player.domain.usecase.CycleAspectRatioUseCase
import com.tapman104.mpvplayer.player.domain.usecase.GetResumePositionUseCase
import com.tapman104.mpvplayer.player.domain.usecase.LoadMediaUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SaveResumePositionUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SeekUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SetAudioTrackUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SetSpeedUseCase
import com.tapman104.mpvplayer.player.domain.usecase.SetSubtitleTrackUseCase
import com.tapman104.mpvplayer.player.domain.usecase.TogglePlaybackUseCase
import com.tapman104.mpvplayer.player.viewmodel.PlaybackCoordinator
import com.tapman104.mpvplayer.player.viewmodel.ResumePositionManager
import com.tapman104.mpvplayer.player.viewmodel.TrackCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideSeekUseCase(c: PlaybackCoordinator): SeekUseCase = SeekUseCase(c)

    @Provides
    @ViewModelScoped
    fun provideTogglePlaybackUseCase(c: PlaybackCoordinator): TogglePlaybackUseCase = TogglePlaybackUseCase(c)

    @Provides
    @ViewModelScoped
    fun provideLoadMediaUseCase(c: PlaybackCoordinator): LoadMediaUseCase = LoadMediaUseCase(c)

    @Provides
    @ViewModelScoped
    fun provideSetSpeedUseCase(c: PlaybackCoordinator): SetSpeedUseCase = SetSpeedUseCase(c)

    @Provides
    @ViewModelScoped
    fun provideSetAudioTrackUseCase(c: TrackCoordinator): SetAudioTrackUseCase = SetAudioTrackUseCase(c)

    @Provides
    @ViewModelScoped
    fun provideSetSubtitleTrackUseCase(c: TrackCoordinator): SetSubtitleTrackUseCase = SetSubtitleTrackUseCase(c)

    @Provides
    @ViewModelScoped
    fun provideCycleAspectRatioUseCase(c: PlaybackCoordinator): CycleAspectRatioUseCase = CycleAspectRatioUseCase(c)

    @Provides
    @ViewModelScoped
    fun provideSaveResumePositionUseCase(m: ResumePositionManager): SaveResumePositionUseCase = SaveResumePositionUseCase(m)

    @Provides
    @ViewModelScoped
    fun provideGetResumePositionUseCase(m: ResumePositionManager): GetResumePositionUseCase = GetResumePositionUseCase(m)
}
