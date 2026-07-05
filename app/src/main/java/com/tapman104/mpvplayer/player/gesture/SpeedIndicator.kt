package com.tapman104.mpvplayer.player.gesture

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pure UI — long-press speed-override badge with a gentle pulsing
 * glow (infinite alpha animation between 0.85 and 1.0).
 *
 * WHAT DRIVES IT: caller controls visibility (e.g. wrap this in
 * AnimatedVisibility keyed off your own "isLongPressing" state).
 * The label is hardcoded to "2× Speed" here since that's the only
 * value used previously — pass `label` if you want other speeds.
 * No long-press detection here.
 *
 * Example call site (you own the long-press detection):
 *   if (isLongPressing) SpeedIndicator()
 */
@Composable
fun SpeedIndicator(label: String = "2× Speed") {
    val infiniteTransition = rememberInfiniteTransition(label = "speed_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "speed_glow",
    )

    Box(
        modifier = Modifier
            .alpha(glowAlpha) // pulses the whole badge, matching the original
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1E1E).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = "Speed override",
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
