package com.tapman104.mpvplayer.di

import android.content.Context
import com.tapman104.mpvplayer.core.database.ResumePositionDao
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
    fun provideResumePositionManager(dao: ResumePositionDao): ResumePositionManager = ResumePositionManager(dao)
}
