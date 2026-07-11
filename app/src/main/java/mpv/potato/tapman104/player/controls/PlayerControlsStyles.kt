package mpv.potato.tapman104.player.controls

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

internal object PlayerControlsStyles {
    const val ANIM_DURATION_MS = 180

    /**
     * Drop shadow on text so it stays legible over any video frame.
     * Used on timestamps, file name, seek tooltip.
     */
    val textShadowStyle = TextStyle(
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.75f),
            offset = Offset(0f, 1f),
            blurRadius = 6f
        )
    )
}
