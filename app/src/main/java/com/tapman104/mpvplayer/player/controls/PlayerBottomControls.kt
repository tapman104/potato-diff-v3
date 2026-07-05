package com.tapman104.mpvplayer.player.controls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.util.TimeFormatter

private val PillShape = RoundedCornerShape(50)

@Composable
fun PlayerBottomControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    gestureSeekPreviewMs: Long = -1L,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekGesture: (Long) -> Unit = {},
    onSeekPreviewMs: (Long) -> Unit = {},
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0L) }

    // Priority: local drag > gesture scrub preview (finger-accurate) > normal playback position.
    val displayMs = when {
        isDragging -> dragPositionMs
        gestureSeekPreviewMs >= 0L -> gestureSeekPreviewMs
        else -> currentPositionMs
    }
    val fraction = if (durationMs > 0L)
        (displayMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Seek bar ───────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = TimeFormatter.formatMs(displayMs),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.width(42.dp),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                        blurRadius = 3f
                    )
                )
            )
            Slider(
                value = fraction,
                onValueChange = { v ->
                    isDragging = true
                    dragPositionMs = (v * durationMs).toLong()
                    onSeekGesture(dragPositionMs)
                    onSeekPreviewMs(dragPositionMs)
                },
                onValueChangeFinished = {
                    if (isDragging) {
                        onSeek(dragPositionMs)
                    }
                    isDragging = false
                    onSeekPreviewMs(-1L)
                },
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 3.dp,
                        shape = PillShape,
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    ),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = TimeFormatter.formatMs(durationMs),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.End,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }

        // ── Play / Pause button ────────────────────────────────────────────────
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .padding(top = 14.dp, bottom = 4.dp)
                .size(52.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                ),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.92f),
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

