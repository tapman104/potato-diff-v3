package com.tapman104.mpvplayer.player.controls

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.util.TimeFormatter

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
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0L) }

    val safeDurationMs = durationMs.coerceAtLeast(0L)
    // Priority: local drag > gesture scrub preview (finger-accurate) > normal playback position.
    val rawDisplayMs = when {
        isDragging -> dragPositionMs
        gestureSeekPreviewMs >= 0L -> gestureSeekPreviewMs
        else -> currentPositionMs
    }
    val displayMs = rawDisplayMs.coerceAtLeast(0L)
    val fraction = if (safeDurationMs > 0L) {
        (displayMs.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
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
                style = PlayerControlsStyles.textShadowStyle
            )
            Slider(
                value = fraction,
                enabled = safeDurationMs > 0L,
                onValueChange = { v ->
                    if (safeDurationMs > 0L) {
                        if (!isDragging) onSeekPreviewMs(-1L)
                        isDragging = true
                        dragPositionMs = (v * safeDurationMs).toLong()
                        onSeekGesture(dragPositionMs)
                        onSeekPreviewMs(dragPositionMs)
                    }
                },
                onValueChangeFinished = {
                    if (isDragging) {
                        val finalPos = dragPositionMs
                        isDragging = false
                        onSeekPreviewMs(-1L)
                        onSeek(finalPos)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = PlayerControlsStyles.seekBarColors()
            )
            Text(
                text = TimeFormatter.formatMs(safeDurationMs),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.End,
                style = PlayerControlsStyles.textShadowStyle
            )
        }

        // ── Play / Pause button ────────────────────────────────────────────────
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .padding(top = 14.dp, bottom = 4.dp)
                .size(52.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.92f),
                contentColor = Color.Black
            )
        ) {
            Crossfade(
                targetState = isPlaying,
                animationSpec = tween(durationMillis = 150),
                label = "PlayPauseCrossfade"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

