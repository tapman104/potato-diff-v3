package mpv.potato.tapman104.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tapman104.mpvplayer.ui.theme.Typography

/**
 * Always-dark neutral theme applied to the player overlay.
 * Forces M3 dark neutral color tokens regardless of system setting,
 * so all tonal surfaces render correctly over a black video background
 * without unwanted purple tints.
 */
private val PlayerDarkColorScheme = darkColorScheme(
    primary            = Color.White,
    onPrimary          = Color.Black,
    primaryContainer   = Color(0xFF2A2A2A),   // dark neutral, no purple
    onPrimaryContainer = Color.White,
    surface            = Color(0xFF0E0E0E),
    onSurface          = Color.White,
    surfaceVariant     = Color(0xFF1E1E1E),
    onSurfaceVariant   = Color(0xFFCACACA),
    outline            = Color(0xFF3A3A3A),
)

@Composable
fun PlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PlayerDarkColorScheme,
        typography  = Typography,
        content     = content,
    )
}
