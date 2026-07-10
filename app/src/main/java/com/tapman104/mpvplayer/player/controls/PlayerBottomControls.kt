package com.tapman104.mpvplayer.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.util.TimeFormatter
import kotlin.math.abs

@Composable
fun PlayerBottomControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferPositionMs: Long = 0L,
    gestureSeekPreviewMs: Long = -1L,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekGesture: (Long) -> Unit = {},
    onSeekPreviewMs: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableLongStateOf(0L) }
    var dragStartMs by remember { mutableLongStateOf(0L) }
    var lastSeekSendTime by remember { mutableLongStateOf(0L) }

    // Optimistic hold after seek commit so slider doesn't jump back before mpv position catches up
    var optimisticSeekMs by remember { mutableStateOf<Long?>(null) }
    var optimisticSeekTime by remember { mutableLongStateOf(0L) }

    val safeDurationMs = durationMs.coerceAtLeast(0L)
    val now = System.currentTimeMillis()

    val activeOptimisticMs = optimisticSeekMs?.takeIf { opt ->
        val timeSinceCommit = now - optimisticSeekTime
        val distance = abs(currentPositionMs - opt)
        timeSinceCommit < 700L && distance > 1200L
    }

    // Priority: local drag > gesture scrub preview > optimistic hold > normal playback position.
    val rawDisplayMs = when {
        isDragging -> dragPositionMs
        gestureSeekPreviewMs >= 0L -> gestureSeekPreviewMs
        activeOptimisticMs != null -> activeOptimisticMs
        else -> currentPositionMs
    }
    val displayMs = rawDisplayMs.coerceAtLeast(0L)
    val fraction = if (safeDurationMs > 0L) {
        (displayMs.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val bufferFraction = if (safeDurationMs > 0L) {
        (bufferPositionMs.coerceAtLeast(0L).toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val onDragStart: (Long) -> Unit = { initialMs ->
        if (safeDurationMs > 0L) {
            onSeekPreviewMs(-1L)
            isDragging = true
            dragStartMs = displayMs
            dragPositionMs = initialMs
            lastSeekSendTime = System.currentTimeMillis()
            onSeekGesture(initialMs)
            onSeekPreviewMs(initialMs)
        }
    }

    val onDrag: (Long) -> Unit = { ms ->
        if (safeDurationMs > 0L && isDragging) {
            dragPositionMs = ms
            onSeekPreviewMs(ms)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSeekSendTime >= 80L) {
                lastSeekSendTime = currentTime
                onSeekGesture(ms)
            }
        }
    }

    val onDragEnd: () -> Unit = {
        if (isDragging) {
            val finalPos = dragPositionMs
            isDragging = false
            optimisticSeekMs = finalPos
            optimisticSeekTime = System.currentTimeMillis()
            onSeekPreviewMs(-1L)
            onSeek(finalPos)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Seek bar row with floating preview tooltip ────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f, animationSpec = tween(150)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-32).dp)
            ) {
                SeekPreviewTooltip(
                    targetMs = displayMs,
                    deltaMs = displayMs - dragStartMs,
                    durationMs = safeDurationMs
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = TimeFormatter.formatMs(displayMs),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(46.dp),
                    style = PlayerControlsStyles.textShadowStyle
                )
                RefinedSeekBar(
                    progressFraction = fraction,
                    bufferFraction = bufferFraction,
                    isDragging = isDragging,
                    enabled = safeDurationMs > 0L,
                    durationMs = safeDurationMs,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                )
                Text(
                    text = TimeFormatter.formatMs(safeDurationMs),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(46.dp),
                    textAlign = TextAlign.End,
                    style = PlayerControlsStyles.textShadowStyle
                )
            }
        }

        // ── Play / Pause button ────────────────────────────────────────────────
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 4.dp)
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

@Composable
private fun RefinedSeekBar(
    progressFraction: Float,
    bufferFraction: Float,
    isDragging: Boolean,
    enabled: Boolean,
    durationMs: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 3.5.dp,
        animationSpec = tween(160),
        label = "trackHeight"
    )
    val animatedThumbRadius by animateDpAsState(
        targetValue = if (isDragging) 8.5.dp else 6.dp,
        animationSpec = tween(160),
        label = "thumbRadius"
    )
    val animatedGlowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(160),
        label = "glowAlpha"
    )

    val currentDurationMs by rememberUpdatedState(durationMs)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentEnabled by rememberUpdatedState(enabled)

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                val dur = currentDurationMs
                if (!currentEnabled || dur <= 0L) return@awaitEachGesture
                down.consume()

                val thumbRadiusPx = 8.dp.toPx()
                val trackWidthPx = (size.width - 2 * thumbRadiusPx).coerceAtLeast(1f)
                val initialFraction = ((down.position.x - thumbRadiusPx) / trackWidthPx).coerceIn(0f, 1f)
                val initialMs = (initialFraction * dur).toLong()

                currentOnDragStart(initialMs)

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    change.consume()

                    val fraction = ((change.position.x - thumbRadiusPx) / trackWidthPx).coerceIn(0f, 1f)
                    val ms = (fraction * dur).toLong()
                    currentOnDrag(ms)
                }
                currentOnDragEnd()
            }
        }
    ) {
        val thumbRadiusPx = animatedThumbRadius.toPx()
        val sidePaddingPx = 8.dp.toPx()
        val trackHeightPx = animatedTrackHeight.toPx()
        val trackWidthPx = (size.width - 2 * sidePaddingPx).coerceAtLeast(1f)
        val centerY = size.height / 2f
        val startX = sidePaddingPx
        val endX = sidePaddingPx + trackWidthPx

        // 1. Inactive Track
        drawLine(
            color = Color.White.copy(alpha = if (enabled) 0.22f else 0.12f),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackHeightPx,
            cap = StrokeCap.Round
        )

        if (enabled) {
            // 2. Buffered / Cache Track
            val safeBuffer = bufferFraction.coerceIn(0f, 1f)
            if (safeBuffer > 0f) {
                val bufferEndX = startX + trackWidthPx * safeBuffer
                drawLine(
                    color = Color.White.copy(alpha = 0.44f),
                    start = Offset(startX, centerY),
                    end = Offset(bufferEndX, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Active Progress Track
            val safeProgress = progressFraction.coerceIn(0f, 1f)
            val progressEndX = startX + trackWidthPx * safeProgress
            if (safeProgress > 0f) {
                drawLine(
                    color = Color.White,
                    start = Offset(startX, centerY),
                    end = Offset(progressEndX, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )
            }

            // 4. Glow Ring around Thumb when active
            val glowAlpha = animatedGlowAlpha
            if (glowAlpha > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f * glowAlpha),
                    radius = thumbRadiusPx + 8.dp.toPx() * glowAlpha,
                    center = Offset(progressEndX, centerY)
                )
            }

            // 5. Thumb Circle
            drawCircle(
                color = Color.White,
                radius = thumbRadiusPx,
                center = Offset(progressEndX, centerY)
            )
        }
    }
}

@Composable
private fun SeekPreviewTooltip(
    targetMs: Long,
    deltaMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val deltaColor = when {
        deltaMs > 500L -> Color(0xFF69F0AE)  // soft green for forward
        deltaMs < -500L -> Color(0xFFFFAB40) // soft amber for backward
        else -> Color.White.copy(alpha = 0.7f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141417).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 8.dp,
        modifier = modifier.padding(bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = TimeFormatter.formatMs(targetMs),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = PlayerControlsStyles.textShadowStyle
            )
            Text(
                text = "/",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 12.sp
            )
            Text(
                text = TimeFormatter.formatMs(durationMs),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                style = PlayerControlsStyles.textShadowStyle
            )
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Text(
                text = formatDeltaMs(deltaMs),
                color = deltaColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                style = PlayerControlsStyles.textShadowStyle
            )
        }
    }
}

private fun formatDeltaMs(deltaMs: Long): String {
    val sign = if (deltaMs >= 0) "+" else "-"
    val absMs = abs(deltaMs)
    return sign + TimeFormatter.formatMs(absMs)
}


