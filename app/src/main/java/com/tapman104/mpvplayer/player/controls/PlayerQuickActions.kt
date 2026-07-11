package com.tapman104.mpvplayer.player.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tapman104.mpvplayer.player.model.DecodeMode

@Composable
fun PlayerQuickActions(
    decodeMode: DecodeMode,
    onSelectAudioTrack: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onDecodeModeClick: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    mpv.potato.tapman104.player.controls.PlayerQuickActions(
        decodeMode = decodeMode,
        onSelectAudioTrack = onSelectAudioTrack,
        onSelectSubtitleTrack = onSelectSubtitleTrack,
        onDecodeModeClick = onDecodeModeClick,
        onMoreOptions = onMoreOptions,
        modifier = modifier
    )
}