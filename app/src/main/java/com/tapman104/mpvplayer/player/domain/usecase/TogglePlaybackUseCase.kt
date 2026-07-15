package com.tapman104.mpvplayer.player.domain.usecase

import com.tapman104.mpvplayer.player.viewmodel.PlaybackCoordinator

class TogglePlaybackUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    suspend operator fun invoke() {
        playbackCoordinator.togglePlay()
    }
}
