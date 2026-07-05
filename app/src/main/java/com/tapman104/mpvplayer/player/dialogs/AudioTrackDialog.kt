package com.tapman104.mpvplayer.player.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tapman104.mpvplayer.player.model.AudioTrack

@Composable
fun AudioTrackDialog(
    tracks: List<AudioTrack>,
    selectedTrackId: Int,
    onSelectTrack: (Int) -> Unit,
    onAddAudioClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Scrim — tapping outside dismisses
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // Sheet surface
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .background(
                        color = Color(0xCC1A1A1A),
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .clickable(enabled = false) {}
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(CenterHorizontally)
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                // Title
                Text(
                    text = "Audio Track",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                if (tracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No audio tracks available",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        itemsIndexed(tracks) { index, track ->
                            TrackRow(
                                title = track.title,
                                lang = track.lang,
                                isSelected = track.id == selectedTrackId,
                                onClick = {
                                    onSelectTrack(track.id)
                                    onDismiss()
                                }
                            )
                            if (index < tracks.lastIndex) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.06f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                AddTrackRow(
                    label = "Add audio track...",
                    onClick = {
                        onAddAudioClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    title: String,
    lang: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) Color(0xFF8B5CF6) else Color.White,
                fontSize = 15.sp
            )
            Text(
                text = "[$lang]",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier.padding(start = 8.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF8B5CF6),
                unselectedColor = Color.White.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun AddTrackRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
