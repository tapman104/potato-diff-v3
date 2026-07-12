package mpv.potato.tapman104.player.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val buttonBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
    val buttonColors = IconButtonDefaults.outlinedIconButtonColors(
        containerColor = Color.Black.copy(alpha = 0.45f),
        contentColor = Color.White,
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedIconButton(
            onClick = onSelectAudioTrack,
            modifier = Modifier.size(44.dp),
            border = buttonBorder,
            colors = buttonColors,
        ) {
            Icon(
                imageVector = Icons.Outlined.Audiotrack,
                contentDescription = "Audio track",
                modifier = Modifier.size(20.dp)
            )
        }

        OutlinedIconButton(
            onClick = onSelectSubtitleTrack,
            modifier = Modifier.size(44.dp),
            border = buttonBorder,
            colors = buttonColors,
        ) {
            Icon(
                imageVector = Icons.Outlined.ClosedCaption,
                contentDescription = "Subtitle track",
                modifier = Modifier.size(20.dp)
            )
        }

        AssistChip(
            onClick = onDecodeModeClick,
            label = {
                Text(
                    text = when (decodeMode) {
                        DecodeMode.HWPlus -> "HW+"
                        DecodeMode.HW     -> "HW"
                        DecodeMode.SW     -> "SW"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            },
            modifier = Modifier.height(36.dp),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color.Black.copy(alpha = 0.45f),
                labelColor = Color.White,
            ),
            border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = Color.White.copy(alpha = 0.35f),
                borderWidth = 1.dp,
            ),
            shape = CircleShape,
        )

        OutlinedIconButton(
            onClick = onMoreOptions,
            modifier = Modifier.size(44.dp),
            border = buttonBorder,
            colors = buttonColors,
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More options",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
