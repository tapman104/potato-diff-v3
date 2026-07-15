package com.tapman104.mpvplayer.player.domain.usecase

import com.tapman104.mpvplayer.player.viewmodel.TrackCoordinator

class SetSubtitleTrackUseCase(
    private val trackCoordinator: TrackCoordinator,
) {
    suspend operator fun invoke(id: Int) {
        trackCoordinator.setSubtitleTrack(id)
    }
}
