package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pure UI — horizontal swipe-to-seek indicator.
 * ("Current time" + "delta" pill, e.g. "12:34   +00:10")
 *
 * WHAT DRIVES IT: caller passes two pre-formatted strings.
 *   currentTimeLabel -> the resulting/preview playback position, e.g. "12:34"
 *   deltaLabel       -> the signed offset from where the swipe started, e.g. "+00:10"
 * Formatting (sign, mm:ss) is the caller's job — this just lays out two texts.
 * No swipe/drag detection here.
 *
 * Example call site (you own the state + formatting):
 *   val sign = if (deltaMs >= 0) "+" else "-"
 *   HorizontalSeekIndicator(
 *       currentTimeLabel = formatMs(previewPositionMs),
 *       deltaLabel = "$sign${formatMs(abs(deltaMs))}"
 *   )
 */
@Composable
fun HorizontalSeekIndicator(currentTimeLabel: String, deltaLabel: String) {
    Box(
        modifier = Modifier
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
