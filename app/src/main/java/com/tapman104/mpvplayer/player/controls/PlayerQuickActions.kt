package com.tapman104.mpvplayer.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val buttonModifier = Modifier
        .size(40.dp)
        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onSelectAudioTrack,
            modifier = buttonModifier
        ) {
            Icon(Icons.Default.AudioFile, contentDescription = "Audio track", tint = Color.White)
        }
        IconButton(
            onClick = onSelectSubtitleTrack,
            modifier = buttonModifier
        ) {
            Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
        }
        IconButton(
            onClick = onDecodeModeClick,
            modifier = buttonModifier
        ) {
            Text(
                text = when (decodeMode) {
                    DecodeMode.HW -> "HW"
                    DecodeMode.HWPlus -> "HW+"
                    DecodeMode.SW -> "SW"
                },
                color = Color.White,
                fontSize = 12.sp
            )
        }
        IconButton(
            onClick = onMoreOptions,
            modifier = buttonModifier
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
        }
    }
}