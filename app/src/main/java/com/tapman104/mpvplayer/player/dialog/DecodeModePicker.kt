package com.tapman104.mpvplayer.player.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tapman104.mpvplayer.player.model.DecodeMode

@Composable
fun DecodeModePicker(
    current: DecodeMode,
    onSelect: (DecodeMode) -> Unit,   // called with chosen mode; dialog auto-dismisses
    onDismiss: () -> Unit
) {
    data class ModeEntry(val mode: DecodeMode, val label: String, val subtitle: String)

    val modes = listOf(
        ModeEntry(DecodeMode.HW,     "HW",  "MediaCodec"),
        ModeEntry(DecodeMode.HWPlus, "HW+", "MediaCodec copy"),
        ModeEntry(DecodeMode.SW,     "SW",  "Software"),
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Hardware Decoding",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Mode rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { entry ->
                    val selected = current == entry.mode
                    OutlinedCard(
                        onClick = {
                            onSelect(entry.mode)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder().let {
                            androidx.compose.foundation.BorderStroke(
                                width = it.width,
                                color = Color(0xFF8B5CF6).copy(alpha = if (selected) 1f else 0.25f)
                            )
                        },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = Color(0xFF8B5CF6).copy(alpha = if (selected) 0.15f else 0f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.label,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = entry.subtitle,
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Cancel button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFAAAAAA))
                }
            }
        }
    }
}
