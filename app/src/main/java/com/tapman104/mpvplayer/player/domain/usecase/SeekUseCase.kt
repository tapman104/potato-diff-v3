package com.tapman104.mpvplayer.player.domain.usecase

import com.tapman104.mpvplayer.player.viewmodel.PlaybackCoordinator

class SeekUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    suspend operator fun invoke(deltaMs: Long) {
        playbackCoordinator.seek(deltaMs)
    }
}
