package com.tapman104.mpvplayer.player.gesture

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Shared dark glass pill chrome used by gesture indicators.
 */
@Composable
fun IndicatorPill(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
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

@Composable
private fun VerticalPercentIndicator(
    icon: ImageVector,
    percentage: Int,
    contentDescription: String
) {
    IndicatorPill {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$percentage%",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Vertical volume drag indicator (0..100 percentage).
 */
@Composable
fun VolumeIndicator(percentage: Int) {
    val icon = when {
        percentage == 0 -> Icons.AutoMirrored.Filled.VolumeOff
        percentage < 50 -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }
    VerticalPercentIndicator(icon = icon, percentage = percentage, contentDescription = "Volume")
}

/**
 * Vertical brightness drag indicator (0f..1f fraction).
 */
@Composable
fun BrightnessIndicator(brightness: Float) {
    val percentage = (brightness * 100).roundToInt()
    val icon = when {
        percentage < 33 -> Icons.Filled.BrightnessLow
        percentage <= 66 -> Icons.Filled.BrightnessMedium
        else -> Icons.Filled.BrightnessHigh
    }
    VerticalPercentIndicator(icon = icon, percentage = percentage, contentDescription = "Brightness")
}

/**
 * Pinch-to-zoom indicator (zoom on log2 scale).
 */
@Composable
fun PinchZoomIndicator(zoom: Float) {
    val percentage = ((2.0.pow(zoom.toDouble())) * 100).roundToInt()
    VerticalPercentIndicator(icon = Icons.Filled.ZoomIn, percentage = percentage, contentDescription = "Zoom")
}

/**
 * Long-press speed override badge with pulsing glow animation.
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

    IndicatorPill(
        modifier = Modifier.alpha(glowAlpha),
        horizontalPadding = 16.dp,
        verticalPadding = 8.dp
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

/**
 * Circular burst icon shown for double-tap seek.
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

/**
 * Horizontal swipe-to-seek indicator pill ("12:34   +00:10").
 */
@Composable
fun HorizontalSeekIndicator(currentTimeLabel: String, deltaLabel: String) {
    IndicatorPill(horizontalPadding = 16.dp, verticalPadding = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentTimeLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = deltaLabel,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
