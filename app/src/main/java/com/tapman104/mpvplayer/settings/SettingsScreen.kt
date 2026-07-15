package com.tapman104.mpvplayer.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import com.tapman104.mpvplayer.player.model.QuickActionsPosition

private object SettingsColors {
    val Background     = Color(0xFF0D0D0D)
    val Surface        = Color(0xFF1C1C1E)
    val Divider        = Color(0xFF2C2C2E)
    val Accent         = Color(0xFF8B5CF6)
    val AccentSubtle   = Color(0xFF8B5CF6).copy(alpha = 0.14f)
    val OnSurface      = Color.White
    val OnSurfaceMed   = Color.White.copy(alpha = 0.55f)
    val OnSurfaceLow   = Color.White.copy(alpha = 0.35f)
    val ChipUnselected = Color(0xFF2C2C2E)
}

private sealed class SettingsNavSection {
    object VideoDecoding    : SettingsNavSection()
    object Audio            : SettingsNavSection()
    object GesturesControls : SettingsNavSection()
    object BackgroundPlay   : SettingsNavSection()
    object Subtitles        : SettingsNavSection()
    object SubtitleAppearance : SettingsNavSection()
    object HowToUse         : SettingsNavSection()
    object About            : SettingsNavSection()
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferredSubtitleLang by viewModel.subtitleLanguage.collectAsStateWithLifecycle()
    val subtitleSize by viewModel.subtitleSize.collectAsStateWithLifecycle()
    val subtitlePosition by viewModel.subtitlePosition.collectAsStateWithLifecycle()
    val resumePlayback by viewModel.resumePlayback.collectAsStateWithLifecycle()
    val quickActionsPosition by viewModel.quickActionsPosition.collectAsStateWithLifecycle()
    val decodeMode by viewModel.decodeMode.collectAsStateWithLifecycle()
    val debandFilter by viewModel.debandFilter.collectAsStateWithLifecycle()
    val videoScale by viewModel.videoScale.collectAsStateWithLifecycle()
    val volumeBoost by viewModel.volumeBoost.collectAsStateWithLifecycle()
    val pitchCorrection by viewModel.pitchCorrection.collectAsStateWithLifecycle()
    val audioOutputDriver by viewModel.audioOutputDriver.collectAsStateWithLifecycle()
    val doubleTapSeekSeconds by viewModel.doubleTapSeekSeconds.collectAsStateWithLifecycle()
    val swipeToSeek by viewModel.swipeToSeek.collectAsStateWithLifecycle()
    val brightnessSwipe by viewModel.brightnessSwipe.collectAsStateWithLifecycle()
    val volumeSwipe by viewModel.volumeSwipe.collectAsStateWithLifecycle()
    val longPress2x by viewModel.longPress2x.collectAsStateWithLifecycle()
    val backgroundPlay by viewModel.backgroundPlay.collectAsStateWithLifecycle()

    var currentSection by remember { mutableStateOf<SettingsNavSection?>(null) }

    BackHandler {
        if (currentSection != null) {
            currentSection = null
        } else {
            onBack()
        }
    }

    when (currentSection) {
        null -> {
            RootSettingsCategoryScreen(
                decodeMode = decodeMode,
                audioOutputDriver = audioOutputDriver,
                backgroundPlay = backgroundPlay,
                resumePlayback = resumePlayback,
                onResumePlaybackChange = { viewModel.setResumePlayback(it) },
                quickActionsPosition = quickActionsPosition,
                onQuickActionsPositionChange = { viewModel.setQuickActionsPosition(it) },
                onNavigate = { currentSection = it },
                onBack = onBack,
                modifier = modifier
            )
        }
        SettingsNavSection.VideoDecoding -> {
            VideoDecodingCategoryScreen(
                decodeMode = decodeMode,
                onDecodeModeChange = { viewModel.setDecodeMode(it) },
                debandFilter = debandFilter,
                onDebandFilterChange = { viewModel.setDebandFilter(it) },
                videoScale = videoScale,
                onVideoScaleChange = { viewModel.setVideoScale(it) },
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.Audio -> {
            AudioCategoryScreen(
                volumeBoost = volumeBoost,
                onVolumeBoostChange = { viewModel.setVolumeBoost(it) },
                pitchCorrection = pitchCorrection,
                onPitchCorrectionChange = { viewModel.setPitchCorrection(it) },
                audioOutputDriver = audioOutputDriver,
                onAudioOutputDriverChange = { viewModel.setAudioOutputDriver(it) },
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.GesturesControls -> {
            GesturesControlsCategoryScreen(
                doubleTapSeekSeconds = doubleTapSeekSeconds,
                onDoubleTapSeekSecondsChange = { viewModel.setDoubleTapSeekSeconds(it) },
                swipeToSeek = swipeToSeek,
                onSwipeToSeekChange = { viewModel.setSwipeToSeek(it) },
                brightnessSwipe = brightnessSwipe,
                onBrightnessSwipeChange = { viewModel.setBrightnessSwipe(it) },
                volumeSwipe = volumeSwipe,
                onVolumeSwipeChange = { viewModel.setVolumeSwipe(it) },
                longPress2x = longPress2x,
                onLongPress2xChange = { viewModel.setLongPress2x(it) },
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.BackgroundPlay -> {
            BackgroundPlayCategoryScreen(
                backgroundPlay = backgroundPlay,
                onBackgroundPlayChange = { viewModel.setBackgroundPlay(it) },
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.Subtitles -> {
            SubtitlesCategoryScreen(
                preferredSubtitleLang = preferredSubtitleLang,
                onSubtitleLangChange = { viewModel.setSubtitleLanguage(it) },
                onOpenAppearance = { currentSection = SettingsNavSection.SubtitleAppearance },
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.SubtitleAppearance -> {
            SubtitleAppearanceSection(
                subtitleSize = subtitleSize,
                subtitlePosition = subtitlePosition,
                onSizeChange = { viewModel.setSubtitleSize(it) },
                onPositionChange = { viewModel.setSubtitlePosition(it) },
                onBack = { currentSection = SettingsNavSection.Subtitles },
                modifier = modifier
            )
        }
        SettingsNavSection.HowToUse -> {
            HowToUseCategoryScreen(
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.About -> {
            AboutCategoryScreen(
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root Settings Page
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RootSettingsCategoryScreen(
    decodeMode: String,
    audioOutputDriver: String,
    backgroundPlay: String,
    resumePlayback: Boolean,
    onResumePlaybackChange: (Boolean) -> Unit,
    quickActionsPosition: QuickActionsPosition,
    onQuickActionsPositionChange: (QuickActionsPosition) -> Unit,
    onNavigate: (SettingsNavSection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "Settings",
        onBack = onBack,
        modifier = modifier
    ) {
        SectionHeader(label = "PLAYBACK")
        SettingsCard {
            CategoryNavigationRow(
                icon = Icons.Rounded.Memory,
                title = "Video & Decoding",
                subtitle = when (decodeMode) {
                    "mediacodec-copy" -> "Hardware+ (mediacodec-copy)"
                    "mediacodec"      -> "Hardware (mediacodec)"
                    else              -> "Software decode"
                },
                onClick = { onNavigate(SettingsNavSection.VideoDecoding) }
            )
            HorizontalDivider(color = SettingsColors.Divider)
            CategoryNavigationRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Audio",
                subtitle = when (audioOutputDriver) {
                    "aaudio"   -> "AAudio · volume boost, pitch correction"
                    "opensles" -> "OpenSL ES · volume boost, pitch correction"
                    else       -> "AudioTrack · volume boost, pitch correction"
                },
                onClick = { onNavigate(SettingsNavSection.Audio) }
            )
            HorizontalDivider(color = SettingsColors.Divider)
            CategoryNavigationRow(
                icon = Icons.Rounded.Settings,
                title = "Gestures & Controls",
                subtitle = "Seek duration, swipe zones, sensitivity",
                onClick = { onNavigate(SettingsNavSection.GesturesControls) }
            )
            HorizontalDivider(color = SettingsColors.Divider)
            CategoryNavigationRow(
                icon = Icons.Filled.Audiotrack,
                title = "Background Play",
                subtitle = when (backgroundPlay) {
                    "always"          -> "Always on"
                    "headphones_only" -> "Headphones only"
                    else              -> "Off"
                },
                onClick = { onNavigate(SettingsNavSection.BackgroundPlay) }
            )
            HorizontalDivider(color = SettingsColors.Divider)
            SettingsToggleRow(
                title = "Resume Playback",
                subtitle = "Remember position when exiting",
                checked = resumePlayback,
                onCheckedChange = onResumePlaybackChange
            )
            HorizontalDivider(color = SettingsColors.Divider)
            Text(
                text = "Quick actions position",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )
            val positionOptions = listOf(
                QuickActionsPosition.TOP_LEFT    to "Top left",
                QuickActionsPosition.TOP_RIGHT   to "Top right",
                QuickActionsPosition.BOTTOM_LEFT to "Bottom left",
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                positionOptions.forEachIndexed { index, (position, label) ->
                    SegmentedButton(
                        selected = quickActionsPosition == position,
                        onClick = { onQuickActionsPositionChange(position) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = positionOptions.size),
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(label = "APPEARANCE")
        SettingsCard {
            CategoryNavigationRow(
                icon = Icons.Filled.ClosedCaption,
                title = "Subtitles",
                subtitle = "Language, size, position, styling",
                onClick = { onNavigate(SettingsNavSection.Subtitles) }
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(label = "HELP")
        SettingsCard {
            CategoryNavigationRow(
                icon = Icons.Rounded.TouchApp,
                title = "How to Use",
                subtitle = "Gesture guide and controls reference",
                onClick = { onNavigate(SettingsNavSection.HowToUse) }
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(label = "INFO")
        SettingsCard {
            CategoryNavigationRow(
                icon = Icons.Rounded.Info,
                title = "About",
                subtitle = "Potato Player v${com.tapman104.mpvplayer.BuildConfig.VERSION_NAME} · libmpv",
                onClick = { onNavigate(SettingsNavSection.About) }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-screens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoDecodingCategoryScreen(
    decodeMode: String,
    onDecodeModeChange: (String) -> Unit,
    debandFilter: Boolean,
    onDebandFilterChange: (Boolean) -> Unit,
    videoScale: String,
    onVideoScaleChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "Video & Decoding",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Default Decode Mode",
                    color = SettingsColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChipGroup(
                    options = listOf(
                        ChipOption("HW+", "mediacodec-copy"),
                        ChipOption("HW", "mediacodec"),
                        ChipOption("SW", "no")
                    ),
                    selectedValue = decodeMode,
                    onSelect = onDecodeModeChange
                )
                Spacer(Modifier.height(12.dp))
                val description = when (decodeMode) {
                    "mediacodec-copy" -> "HW+ (mediacodec-copy)\nBest for 4K/HDR, lowest power"
                    "mediacodec" -> "HW (mediacodec)\nHardware decode + GPU shader support"
                    else -> "SW (no)\nMax compatibility, 10-bit Hi10P"
                }
                Text(
                    text = description,
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        SettingsCard {
            SettingsToggleRow(
                title = "Deband Filter",
                subtitle = "--deband · reduces color banding in anime/dark scenes",
                checked = debandFilter,
                onCheckedChange = onDebandFilterChange
            )
        }

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Video Scale Algorithm",
                    color = SettingsColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "--scale · upscaling filter quality",
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChipGroup(
                    options = listOf(
                        ChipOption("Bilinear", "bilinear"),
                        ChipOption("Lanczos", "lanczos"),
                        ChipOption("Spline36", "spline36")
                    ),
                    selectedValue = videoScale,
                    onSelect = onVideoScaleChange
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AudioCategoryScreen(
    volumeBoost: Int,
    onVolumeBoostChange: (Int) -> Unit,
    pitchCorrection: Boolean,
    onPitchCorrectionChange: (Boolean) -> Unit,
    audioOutputDriver: String,
    onAudioOutputDriverChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "Audio",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Volume Boost",
                            color = SettingsColors.OnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Allow volume above 100% (up to 200%)",
                            color = SettingsColors.OnSurfaceMed,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${volumeBoost}%",
                        color = SettingsColors.Accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = volumeBoost.toFloat(),
                    onValueChange = { onVolumeBoostChange(it.roundToInt()) },
                    valueRange = 100f..200f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = SettingsColors.OnSurface,
                        activeTrackColor = SettingsColors.Accent,
                        inactiveTrackColor = SettingsColors.Divider
                    )
                )
            }
        }

        SettingsCard {
            SettingsToggleRow(
                title = "Pitch Correction",
                subtitle = "--audio-pitch-correction · natural voice at speed",
                checked = pitchCorrection,
                onCheckedChange = onPitchCorrectionChange
            )
        }

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Audio Output Driver",
                    color = SettingsColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "--ao · audio API selection",
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChipGroup(
                    options = listOf(
                        ChipOption("AAudio", "aaudio"),
                        ChipOption("OpenSL ES", "opensles"),
                        ChipOption("AudioTrack", "audiotrack")
                    ),
                    selectedValue = audioOutputDriver,
                    onSelect = onAudioOutputDriverChange
                )
                Spacer(Modifier.height(12.dp))
                val aoDesc = when (audioOutputDriver) {
                    "aaudio" -> "AAudio (Android 8+) — lowest latency, recommended for modern devices"
                    "opensles" -> "OpenSL ES — wider compatibility, use if AAudio has issues"
                    else -> "AudioTrack — Android default, most compatible"
                }
                Text(
                    text = aoDesc,
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GesturesControlsCategoryScreen(
    doubleTapSeekSeconds: Int,
    onDoubleTapSeekSecondsChange: (Int) -> Unit,
    swipeToSeek: Boolean,
    onSwipeToSeekChange: (Boolean) -> Unit,
    brightnessSwipe: Boolean,
    onBrightnessSwipeChange: (Boolean) -> Unit,
    volumeSwipe: Boolean,
    onVolumeSwipeChange: (Boolean) -> Unit,
    longPress2x: Boolean,
    onLongPress2xChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "Gestures & Controls",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Double-Tap Seek Duration",
                    color = SettingsColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Time jumped per tap",
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChipGroup(
                    options = listOf(
                        ChipOption("5s", 5),
                        ChipOption("10s", 10),
                        ChipOption("15s", 15),
                        ChipOption("30s", 30)
                    ),
                    selectedValue = doubleTapSeekSeconds,
                    onSelect = onDoubleTapSeekSecondsChange
                )
            }
        }

        SettingsCard {
            SettingsToggleRow(
                title = "Swipe to Seek",
                subtitle = "Horizontal swipe across screen to seek",
                checked = swipeToSeek,
                onCheckedChange = onSwipeToSeekChange
            )
            HorizontalDivider(color = SettingsColors.Divider)
            SettingsToggleRow(
                title = "Brightness Swipe",
                subtitle = "Left-side vertical swipe",
                checked = brightnessSwipe,
                onCheckedChange = onBrightnessSwipeChange
            )
            HorizontalDivider(color = SettingsColors.Divider)
            SettingsToggleRow(
                title = "Volume Swipe",
                subtitle = "Right-side vertical swipe",
                checked = volumeSwipe,
                onCheckedChange = onVolumeSwipeChange
            )
            HorizontalDivider(color = SettingsColors.Divider)
            SettingsToggleRow(
                title = "Long Press 2× Speed",
                subtitle = "Hold screen for fast-forward",
                checked = longPress2x,
                onCheckedChange = onLongPress2xChange
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BackgroundPlayCategoryScreen(
    backgroundPlay: String,
    onBackgroundPlayChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "Background Play",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Background Playback",
                    color = SettingsColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Behavior when app is minimized or switched away",
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChipGroup(
                    options = listOf(
                        ChipOption("Off", "off"),
                        ChipOption("Always", "always"),
                        ChipOption("Headphones Only", "headphones_only")
                    ),
                    selectedValue = backgroundPlay,
                    onSelect = onBackgroundPlayChange
                )
                Spacer(Modifier.height(12.dp))
                val desc = when (backgroundPlay) {
                    "always" -> "Audio continues playing when minimized"
                    "headphones_only" -> "Continues playing only if headphones or Bluetooth audio are connected"
                    else -> "Playback pauses automatically when minimized"
                }
                Text(
                    text = desc,
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 13.sp
                )
            }
        }

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Screen-off Behavior",
                    color = SettingsColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Locking screen or turning display off will always pause playback immediately, regardless of background play mode.",
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SubtitlesCategoryScreen(
    preferredSubtitleLang: String,
    onSubtitleLangChange: (String) -> Unit,
    onOpenAppearance: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "Subtitles",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Preferred Subtitle Language",
                    color = SettingsColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Auto-select matching track on file load",
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                SegmentedChipGroup(
                    options = listOf(
                        ChipOption("English", "en"),
                        ChipOption("Japanese", "jpn"),
                        ChipOption("Korean", "kor"),
                        ChipOption("None", "none")
                    ),
                    selectedValue = preferredSubtitleLang,
                    onSelect = onSubtitleLangChange
                )
            }
        }

        SettingsCard {
            CategoryNavigationRow(
                icon = Icons.Filled.ClosedCaption,
                title = "Subtitle Appearance",
                subtitle = "Size, position, styling",
                onClick = onOpenAppearance
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SubtitleAppearanceSection(
    subtitleSize: Float,
    subtitlePosition: Float,
    onSizeChange: (Float) -> Unit,
    onPositionChange: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "Subtitle Appearance",
        onBack = onBack,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subtitle Size", color = SettingsColors.OnSurface, fontSize = 15.sp)
                    Text(
                        "${"%.1f".format(subtitleSize)}×",
                        color = SettingsColors.Accent,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = subtitleSize,
                    onValueChange = onSizeChange,
                    valueRange = 0.5f..3.0f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = SettingsColors.OnSurface,
                        activeTrackColor = SettingsColors.Accent,
                        inactiveTrackColor = SettingsColors.OnSurfaceLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val positionLabel = when {
                    subtitlePosition >= 0.66f -> "Top"
                    subtitlePosition >= 0.33f -> "Middle"
                    else -> "Bottom"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subtitle Position", color = SettingsColors.OnSurface, fontSize = 15.sp)
                    Text(positionLabel, color = SettingsColors.Accent, fontSize = 13.sp)
                }
                Slider(
                    value = subtitlePosition,
                    onValueChange = onPositionChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = SettingsColors.OnSurface,
                        activeTrackColor = SettingsColors.Accent,
                        inactiveTrackColor = SettingsColors.OnSurfaceLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HowToUseCategoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "How to Use",
        onBack = onBack,
        modifier = modifier
    ) {
        SectionHeader(label = "PLAYBACK")
        SettingsCard {
            GestureRow(
                gesture = "Double Tap Left / Right",
                description = "Seek backward / forward (uses your configured duration)"
            )
            HorizontalDivider(color = SettingsColors.Divider)
            GestureRow(
                gesture = "Tap Center",
                description = "Show/hide controls"
            )
            HorizontalDivider(color = SettingsColors.Divider)
            GestureRow(
                gesture = "Horizontal Swipe",
                description = "Scrub through video (if Swipe to Seek is on)"
            )
            HorizontalDivider(color = SettingsColors.Divider)
            GestureRow(
                gesture = "Long Press",
                description = "Hold for 2× speed (if Long Press 2× is on)"
            )
        }

        SectionHeader(label = "VOLUME & BRIGHTNESS")
        SettingsCard {
            GestureRow(
                gesture = "Swipe Up/Down on Right side",
                description = "Volume"
            )
            HorizontalDivider(color = SettingsColors.Divider)
            GestureRow(
                gesture = "Swipe Up/Down on Left side",
                description = "Screen brightness"
            )
        }

        SectionHeader(label = "ZOOM & PAN")
        SettingsCard {
            GestureRow(
                gesture = "Pinch",
                description = "Zoom in/out"
            )
            HorizontalDivider(color = SettingsColors.Divider)
            GestureRow(
                gesture = "Two-finger drag",
                description = "Pan video"
            )
        }

        SectionHeader(label = "TRACK SELECTION")
        SettingsCard {
            GestureRow(
                gesture = "Tap audio / subtitle icons",
                description = "Switch tracks from the top-left quick actions"
            )
            HorizontalDivider(color = SettingsColors.Divider)
            GestureRow(
                gesture = "Tap decoder icon",
                description = "Change hardware decode mode mid-playback"
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AboutCategoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = "About",
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SettingsColors.AccentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🥔", fontSize = 32.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Potato Player",
                    color = SettingsColors.OnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Version ${com.tapman104.mpvplayer.BuildConfig.VERSION_NAME}",
                    color = SettingsColors.Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Powered by libmpv for maximum video & hardware decoding compatibility on Android.",
                    color = SettingsColors.OnSurfaceMed,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "github.com/tapman104",
                    color = SettingsColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { /* open URL — leave as no-op for now */ }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Components & Design Tokens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SettingsColors.Background)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = SettingsColors.OnSurface
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                color = SettingsColors.OnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
        }
        HorizontalDivider(
            color = SettingsColors.Divider,
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun SectionHeader(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SettingsColors.Accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = SettingsColors.OnSurfaceMed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SettingsColors.Surface)
            .border(
                width = 0.5.dp,
                color = SettingsColors.Divider,
                shape = RoundedCornerShape(16.dp)
            ),
        content = content
    )
}

@Composable
private fun CategoryNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SettingsColors.AccentSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SettingsColors.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SettingsColors.OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SettingsColors.OnSurfaceMed,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = SettingsColors.OnSurface.copy(alpha = 0.45f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SettingsColors.OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SettingsColors.OnSurfaceMed,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsColors.OnSurface,
                checkedTrackColor = SettingsColors.Accent,
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = SettingsColors.Divider
            )
        )
    }
}

private data class ChipOption<T>(val label: String, val value: T)

@Composable
private fun <T> SegmentedChipGroup(
    options: List<ChipOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (option in options) {
            val isSelected = option.value == selectedValue
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.00f else 0.97f,
                label = "chip_scale"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) SettingsColors.Accent else SettingsColors.ChipUnselected
                    )
                    .clickable { onSelect(option.value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    color = if (isSelected) SettingsColors.OnSurface else SettingsColors.OnSurfaceMed,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun GestureRow(
    gesture: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = gesture,
            color = SettingsColors.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        if (description.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = SettingsColors.OnSurfaceMed,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
