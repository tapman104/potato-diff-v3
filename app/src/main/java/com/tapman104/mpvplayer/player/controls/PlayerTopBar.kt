package com.tapman104.mpvplayer.player.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlayerTopBar(
    fileName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    mpv.potato.tapman104.player.controls.PlayerTopBar(
        fileName = fileName,
        onBack = onBack,
        modifier = modifier
    )
}
