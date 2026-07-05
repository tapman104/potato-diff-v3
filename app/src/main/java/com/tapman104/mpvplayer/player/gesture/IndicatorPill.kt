package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared "dark glass pill" chrome used by every gesture indicator
 * (Brightness / Volume / Zoom / Speed / Horizontal seek).
 *
 * This is pure UI chrome extracted from the repeated
 * clip + background + border + padding block that was duplicated
 * across all indicator composables. No gesture logic lives here —
 * it's just the shape + look.
 *
 * Usage:
 *   IndicatorPill {
 *       // your Icon + Text content
 *   }
 */
@Composable
fun IndicatorPill(
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1E1E).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        content()
    }
}
