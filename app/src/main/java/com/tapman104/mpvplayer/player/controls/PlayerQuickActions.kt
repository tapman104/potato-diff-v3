package com.tapman104.mpvplayer.player.controls

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                        imageVector = Icons.Outlined.Audiotrack,
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
                        imageVector = Icons.Outlined.ClosedCaption,
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
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Always-visible collapse/expand toggle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    color = if (expanded) Color.Transparent else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(if (expanded) 0.dp else 2.dp)
        ) {
            OutlinedIconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(48.dp).glassButtonShadow(),
                colors = buttonColors,
                border = buttonBorder
            ) {
                Icon(
                    imageVector = if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
