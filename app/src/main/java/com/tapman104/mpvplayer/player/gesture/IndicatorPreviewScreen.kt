package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * BOILERPLATE ONLY — not part of the player UI.
 *
 * A throwaway screen to sanity-check each indicator in isolation
 * with sliders/buttons instead of real gestures. Copy whatever's
 * useful into your own preview/test setup and delete the rest.
 *
 * Nothing here talks to MPV, AudioManager, or any gesture detector.
 */
@Composable
fun IndicatorPreviewScreen() {
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var volume by remember { mutableIntStateOf(70) }
    var zoomLog2 by remember { mutableFloatStateOf(0f) } // 0f..1.585f
    var showSpeed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // Brightness — driven by a 0f..1f fraction
        Text("Brightness", color = Color.White)
        BrightnessIndicator(brightness = brightness)
        Slider(value = brightness, onValueChange = { brightness = it })

        // Volume — driven by 0..100 Int
        Text("Volume", color = Color.White)
        VolumeIndicator(percentage = volume)
        Slider(
            value = volume.toFloat(),
            onValueChange = { volume = it.toInt() },
            valueRange = 0f..100f
        )

        // Zoom — driven by log2 scale, 0f = 100%, 1.585f = 300%
        Text("Zoom", color = Color.White)
        PinchZoomIndicator(zoom = zoomLog2)
        Slider(value = zoomLog2, onValueChange = { zoomLog2 = it }, valueRange = 0f..1.585f)

        // Horizontal seek — driven by two pre-formatted strings
        Text("Horizontal seek", color = Color.White)
        HorizontalSeekIndicator(currentTimeLabel = "12:34", deltaLabel = "+00:10")

        // Double-tap seek circle — driven by label + direction
        Text("Seek circle", color = Color.White)
        SeekCircleIndicator(label = "+20s", isForward = true)

        // Speed badge — driven by a Boolean you own (e.g. isLongPressing)
        Text("Speed badge", color = Color.White)
        Button(onClick = { showSpeed = !showSpeed }) {
            Text(if (showSpeed) "Hide" else "Show")
        }
        if (showSpeed) SpeedIndicator()
    }
}
