package com.tapman104.mpvplayer.player.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.tapman104.mpvplayer.core.database.AppDatabase
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository

class PlayerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(appContext: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "mpvplayer.db"
                ).build().also { INSTANCE = it }
            }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            val appContext = context.applicationContext
            val database = getDatabase(appContext)
            val userPreferencesRepository = UserPreferencesRepository(appContext)
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(
                context = appContext,
                resumePositionDao = database.resumePositionDao(),
                userPreferencesRepository = userPreferencesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
