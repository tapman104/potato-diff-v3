package com.tapman104.mpvplayer.player.domain.usecase

import com.tapman104.mpvplayer.player.viewmodel.PlaybackCoordinator

class LoadMediaUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    suspend operator fun invoke(uri: String) {
        playbackCoordinator.loadFile(uri)
    }
}
