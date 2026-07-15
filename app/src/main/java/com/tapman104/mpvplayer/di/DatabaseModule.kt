package com.tapman104.mpvplayer.di

import android.content.Context
import com.tapman104.mpvplayer.core.database.AppDatabase
import com.tapman104.mpvplayer.core.database.ResumePositionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase = AppDatabase.getInstance(ctx)

    @Provides
    fun provideResumePositionDao(db: AppDatabase): ResumePositionDao = db.resumePositionDao()
}
