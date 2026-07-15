package com.tapman104.mpvplayer.player.domain.usecase

import com.tapman104.mpvplayer.player.viewmodel.PlaybackCoordinator

class SetSpeedUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    suspend operator fun invoke(speed: Float) {
        playbackCoordinator.setSpeed(speed)
    }
}
