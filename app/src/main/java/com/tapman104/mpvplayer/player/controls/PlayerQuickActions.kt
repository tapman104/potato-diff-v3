package com.tapman104.mpvplayer.player.controls

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.player.controls.PlayerControlsStyles.glassButtonShadow
import com.tapman104.mpvplayer.player.model.DecodeMode


@Composable
fun PlayerQuickActions(
    decodeMode: DecodeMode,
    onSelectAudioTrack: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onDecodeModeClick: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    val buttonColors = PlayerControlsStyles.quickActionButtonColors()
    val buttonBorder = PlayerControlsStyles.quickActionButtonBorder()

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = tween(PlayerControlsStyles.ANIM_DURATION_MS),
        label = "ChevronRotation"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(PlayerControlsStyles.ANIM_DURATION_MS)) + expandHorizontally(
                animationSpec = tween(PlayerControlsStyles.ANIM_DURATION_MS),
                expandFrom = Alignment.End
            ),
            exit = fadeOut(tween(PlayerControlsStyles.ANIM_DURATION_MS)) + shrinkHorizontally(
                animationSpec = tween(PlayerControlsStyles.ANIM_DURATION_MS),
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
                    modifier = Modifier.size(48.dp).glassButtonShadow(),
                    colors = buttonColors,
                    border = buttonBorder
                ) {
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = "Audio track",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedIconButton(
                    onClick = onSelectSubtitleTrack,
                    modifier = Modifier.size(48.dp).glassButtonShadow(),
                    colors = buttonColors,
                    border = buttonBorder
                ) {
                    Icon(
                        imageVector = Icons.Filled.ClosedCaption,
                        contentDescription = "Subtitle track",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedIconButton(
                    onClick = onDecodeModeClick,
                    modifier = Modifier.size(48.dp).glassButtonShadow(),
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
                            color = Color.White.copy(alpha = 0.95f),
                            style = PlayerControlsStyles.textShadowStyle
                        )
                    }
                }

                OutlinedIconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier.size(48.dp).glassButtonShadow(),
                    colors = buttonColors,
                    border = buttonBorder
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
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
            modifier = Modifier.size(48.dp).glassButtonShadow(),
            colors = buttonColors,
            border = buttonBorder
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}
