package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure UI — pinch-to-zoom percentage indicator.
 *
 * WHAT DRIVES IT: caller passes zoom on a LOG2 SCALE (not linear %).
 *   0f       = 100% (1x)
 *   1f       = 200% (2x)
 *   1.585f   = 300% (3x)  [log2(3)]
 * This matches whatever "video-zoom"-style property your player
 * exposes on a log2 scale. No pinch-detection code here.
 *
 * Example call site (you own the state):
 *   var zoom by remember { mutableFloatStateOf(0f) } // log2 scale
 *   PinchZoomIndicator(zoom = zoom)
 */
@Composable
fun PinchZoomIndicator(zoom: Float) {
    val percentage = ((2.0.pow(zoom.toDouble())) * 100).roundToInt()

    IndicatorPill {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ZoomIn,
                contentDescription = "Zoom",
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
