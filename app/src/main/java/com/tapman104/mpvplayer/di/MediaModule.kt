package com.tapman104.mpvplayer.di

import android.content.Context
import com.tapman104.mpvplayer.core.database.ResumePositionDao
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.player.domain.repository.LocalMediaRepository
import com.tapman104.mpvplayer.player.domain.repository.MediaRepository
import com.tapman104.mpvplayer.player.viewmodel.PlaylistManager
import com.tapman104.mpvplayer.player.viewmodel.ResumePositionManager
import com.tapman104.mpvplayer.util.UriResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideUriResolver(@ApplicationContext ctx: Context): UriResolver = UriResolver

    @Provides
    @Singleton
    fun providePlaylistManager(
        @ApplicationContext context: Context,
        controller: MpvController
    ): PlaylistManager = PlaylistManager(
        context = context,
        onLoadFile = { path -> controller.executor.loadFile(path) },
        hasSurface = { controller.surface.hasSurface() }
    )

    @Provides
    @Singleton
    fun provideResumePositionManager(dao: ResumePositionDao): ResumePositionManager = ResumePositionManager(dao)

    @Provides
    @Singleton
    fun provideMediaRepository(
        @ApplicationContext context: Context,
        playlistManager: PlaylistManager
    ): MediaRepository = LocalMediaRepository(context, playlistManager)
}
