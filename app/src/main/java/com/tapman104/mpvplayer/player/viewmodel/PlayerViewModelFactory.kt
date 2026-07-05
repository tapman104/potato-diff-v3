package com.tapman104.mpvplayer.player.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tapman104.mpvplayer.core.database.AppDatabase
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository

class PlayerViewModelFactory(
    private val application: Application,
    private val mpvController: MpvController,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(application)
        val dao = db.resumePositionDao()
        val prefsRepo = UserPreferencesRepository(application)
        val resumeManager = ResumePositionManager(dao)
        return PlayerViewModel(
            application = application,
            mpvController = mpvController,
            resumePositionManager = resumeManager,
            preferencesRepository = prefsRepo,
        ) as T
    }

    companion object {
        fun create(application: Application, mpvController: MpvController) =
            PlayerViewModelFactory(application, mpvController)
    }
}
