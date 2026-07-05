package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Pure UI — vertical brightness drag indicator.
 *
 * WHAT DRIVES IT: caller passes brightness as a 0f..1f fraction.
 * The icon swaps low/medium/high at the 33% / 66% thresholds.
 * No gesture code here — just render whatever value you have.
 *
 * Example call site (you own the state):
 *   var brightness by remember { mutableFloatStateOf(0.5f) }
 *   BrightnessIndicator(brightness = brightness)
 */
@Composable
fun BrightnessIndicator(brightness: Float) {
    val percentage = (brightness * 100).roundToInt()
    val icon = when {
        percentage < 33 -> Icons.Filled.BrightnessLow
        percentage <= 66 -> Icons.Filled.BrightnessMedium
        else -> Icons.Filled.BrightnessHigh
    }

    IndicatorPill {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Brightness",
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
