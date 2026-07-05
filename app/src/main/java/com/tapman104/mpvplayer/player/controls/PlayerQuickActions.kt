package com.tapman104.mpvplayer.player.controls

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.player.model.DecodeMode


@Composable
fun PlayerQuickActions(
    decodeMode: DecodeMode,
    onSelectAudioTrack: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onCycleDecodeMode: () -> Unit,
    onMoreOptions: () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    val buttonColors = IconButtonDefaults.outlinedIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
        contentColor = Color.White.copy(alpha = 0.95f)
    )
    val buttonBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

    /** Shadow modifier reused by every button. */
    fun Modifier.glassButton(): Modifier = this
        .shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandHorizontally(
                animationSpec = tween(180),
                expandFrom = Alignment.End
            ),
            exit = fadeOut(tween(180)) + shrinkHorizontally(
                animationSpec = tween(180),
                shrinkTowards = Alignment.End
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                OutlinedIconButton(
                    onClick = onSelectAudioTrack,
                    modifier = Modifier.size(48.dp).glassButton(),
                    colors = buttonColors,
                    border = buttonBorder
                ) {
                    Icon(
                        Icons.Filled.Audiotrack,
                        contentDescription = "Audio track",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedIconButton(
                    onClick = onSelectSubtitleTrack,
                    modifier = Modifier.size(48.dp).glassButton(),
                    colors = buttonColors,
                    border = buttonBorder
                ) {
                    Icon(
                        Icons.Filled.ClosedCaption,
                        contentDescription = "Subtitle track",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedIconButton(
                    onClick = onCycleDecodeMode,
                    modifier = Modifier.size(48.dp).glassButton(),
                    colors = buttonColors,
                    border = buttonBorder
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = when (decodeMode) {
                                DecodeMode.HW     -> "HW"
                                DecodeMode.HWPlus -> "HW+"
                                DecodeMode.SW     -> "SW"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }

                OutlinedIconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier.size(48.dp).glassButton(),
                    colors = buttonColors,
                    border = buttonBorder
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "More options",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Always-visible collapse/expand toggle
        OutlinedIconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.size(48.dp).glassButton(),
            colors = buttonColors,
            border = buttonBorder
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
