package com.tapman104.mpvplayer.player.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mpv.potato.tapman104.player.model.ViewMode

@Composable
fun PlayerViewControls(
    currentViewMode: ViewMode,
    onCycleViewMode: () -> Unit,
    onRotate: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    mpv.potato.tapman104.player.controls.PlayerViewControls(
        currentViewMode = currentViewMode,
        onCycleViewMode = onCycleViewMode,
        onRotate = onRotate,
        onEnterPip = onEnterPip,
        modifier = modifier
    )
}
