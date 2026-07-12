package mpv.potato.tapman104.player.controls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mpv.potato.tapman104.player.model.ViewMode

@Composable
fun PlayerViewControls(
    currentViewMode: ViewMode,
    onCycleViewMode: () -> Unit,
    onRotate: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ViewControlButton(
            label = currentViewMode.label,
            onClick = onCycleViewMode
        )
        IconButton(
            onClick = onRotate,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.ScreenRotation,
                contentDescription = "Rotate",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onEnterPip,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.PictureInPicture,
                contentDescription = "Picture in Picture",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ViewControlButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 52.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Text(text = label, fontSize = 10.sp)
    }
}
