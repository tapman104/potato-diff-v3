package com.tapman104.mpvplayer.player.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Common styling tokens, shapes, colors, and reusable modifiers for UI controls in the player overlay.
 * Centralizing these properties prevents repeated allocations during recompositions and ensures a
 * consistent, high-contrast visual design over variable video backgrounds.
 */
internal object PlayerControlsStyles {

    /** Standard animation duration in milliseconds for UI transitions and expansions. */
    const val ANIM_DURATION_MS = 180

    /** Pill shape used for seek bar slider thumb and track. */
    val PillShape = RoundedCornerShape(50)

    /**
     * Shared text style with a distinct black drop-shadow.
     * Ensures timestamps, filenames, and badges remain clearly legible even over bright or white video scenes.
     */
    val textShadowStyle = TextStyle(
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(0f, 1f),
            blurRadius = 4f
        )
    )

    /** Reusable modifier that applies circular drop-shadow styling to icon buttons. */
    fun Modifier.glassButtonShadow(): Modifier = this.shadow(
        elevation = 3.dp,
        shape = CircleShape,
        ambientColor = Color.Black,
        spotColor = Color.Black
    )

    /** Outlined icon button border for quick actions. */
    @Composable
    fun quickActionButtonBorder(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )

    /** Outlined icon button colors for quick actions. */
    @Composable
    fun quickActionButtonColors() = IconButtonDefaults.outlinedIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
        contentColor = Color.White.copy(alpha = 0.95f)
    )

    /** Slider colors for seek bar. */
    @Composable
    fun seekBarColors() = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
        disabledThumbColor = Color.White.copy(alpha = 0.5f),
        disabledActiveTrackColor = Color.White.copy(alpha = 0.5f),
        disabledInactiveTrackColor = Color.White.copy(alpha = 0.15f)
    )
}
