package com.tapman104.mpvplayer.player.domain.usecase

import com.tapman104.mpvplayer.player.viewmodel.ResumePositionManager

class SaveResumePositionUseCase(
    private val resumePositionManager: ResumePositionManager,
) {
    suspend operator fun invoke(path: String, posMs: Long) {
        resumePositionManager.save(path, posMs)
    }
}
