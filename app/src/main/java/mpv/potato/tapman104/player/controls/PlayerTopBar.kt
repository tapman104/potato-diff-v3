package mpv.potato.tapman104.player.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun parseDisplayName(fileName: String): String {
    // Strip file extension
    var name = fileName.substringBeforeLast(".")

    // Remove common bracket tags: [720p], [1080p], [Sub], [Dub], [x265], [HEVC], etc.
    name = name.replace(Regex("\\[[^\\]]{1,20}\\]"), "")

    // Remove common paren tags: (2024), (BD), (WEB-DL), etc.
    name = name.replace(Regex("\\([^)]{1,20}\\)"), "")

    // Normalize separators: dots and underscores to spaces
    name = name.replace(".", " ").replace("_", " ")

    // Collapse multiple spaces
    name = name.replace(Regex("\\s{2,}"), " ").trim()

    return name.ifBlank { fileName }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTopBar(
    fileName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
        ),
        title = {
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = parseDisplayName(fileName),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(1f, 1f),
                            blurRadius = 4f
                        )
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        },
        navigationIcon = {
            OutlinedIconButton(
                onClick = onBack,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.45f),
                    contentColor = Color.White,
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }
    )
}
