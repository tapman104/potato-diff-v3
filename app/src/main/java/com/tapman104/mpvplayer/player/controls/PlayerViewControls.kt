package com.tapman104.mpvplayer.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tapman104.mpvplayer.player.model.ViewMode

@Composable
fun PlayerViewControls(
    currentViewMode: ViewMode,
    onCycleViewMode: () -> Unit,
    onRotate: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCycleViewMode) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = "Cycle view mode: ${currentViewMode.name}",
                tint = Color.White
            )
        }
        IconButton(onClick = onRotate) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "Rotate video",
                tint = Color.White
            )
        }
        IconButton(onClick = onEnterPip) {
            Icon(
                imageVector = Icons.Default.PictureInPicture,
                contentDescription = "Picture-in-picture",
                tint = Color.White
            )
        }
    }
}
