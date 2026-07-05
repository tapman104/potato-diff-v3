package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pure UI — the circular "burst" icon shown for double-tap seek
 * (rewind/fast-forward), formerly the private SeekIndicator inside
 * GestureOverlay.
 *
 * WHAT DRIVES IT: caller decides direction + label text.
 *   isForward -> swaps FastForward/FastRewind icon
 *   label     -> whatever text you want under the icon, e.g. "+10s"
 * No tap-counting or double-tap-window logic here.
 *
 * Example call site (you own tap counting / seek-amount logic):
 *   SeekCircleIndicator(label = "+20s", isForward = true)
 */
@Composable
fun SeekCircleIndicator(
    label: String,
    isForward: Boolean,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.05f),
                    )
                )
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = if (isForward) "Seek forward" else "Seek backward",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
