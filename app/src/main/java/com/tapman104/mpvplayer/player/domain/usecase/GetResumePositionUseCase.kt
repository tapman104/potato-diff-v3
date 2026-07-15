package com.tapman104.mpvplayer.player.domain.usecase

import com.tapman104.mpvplayer.player.viewmodel.ResumePositionManager

class GetResumePositionUseCase(
    private val resumePositionManager: ResumePositionManager,
) {
    suspend operator fun invoke(path: String): Long? {
        return resumePositionManager.get(path)
    }
}
