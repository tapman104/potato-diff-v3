package com.tapman104.mpvplayer.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private sealed interface SettingsNavSection {
    data object VideoDecoding : SettingsNavSection
    data object Audio : SettingsNavSection
    data object GesturesControls : SettingsNavSection
    data object BackgroundPlay : SettingsNavSection
    data object Subtitles : SettingsNavSection
    data object SubtitleAppearance : SettingsNavSection
    data object About : SettingsNavSection
}

@Composable
fun SettingsScreen(
    preferredSubtitleLang: String,
    onSubtitleLangChange: (String) -> Unit,
    subtitleSize: Float,
    subtitlePosition: Float,
    onSubtitleSizeChange: (Float) -> Unit,
    onSubtitlePositionChange: (Float) -> Unit,
    resumePlayback: Boolean,
    onResumePlaybackChange: (Boolean) -> Unit,
    decodeMode: String,
    onDecodeModeChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    debandFilter: Boolean = false,
    onDebandFilterChange: (Boolean) -> Unit = {},
    videoScale: String = "lanczos",
    onVideoScaleChange: (String) -> Unit = {},
    volumeBoost: Int = 100,
    onVolumeBoostChange: (Int) -> Unit = {},
    pitchCorrection: Boolean = true,
    onPitchCorrectionChange: (Boolean) -> Unit = {},
    audioOutputDriver: String = "audiotrack",
    onAudioOutputDriverChange: (String) -> Unit = {},
    doubleTapSeekSeconds: Int = 10,
    onDoubleTapSeekSecondsChange: (Int) -> Unit = {},
    swipeToSeek: Boolean = true,
    onSwipeToSeekChange: (Boolean) -> Unit = {},
    brightnessSwipe: Boolean = true,
    onBrightnessSwipeChange: (Boolean) -> Unit = {},
    volumeSwipe: Boolean = true,
    onVolumeSwipeChange: (Boolean) -> Unit = {},
    longPress2x: Boolean = true,
    onLongPress2xChange: (Boolean) -> Unit = {},
    gestureSensitivity: String = "normal",
    onGestureSensitivityChange: (String) -> Unit = {},
    backgroundPlay: String = "off",
    onBackgroundPlayChange: (String) -> Unit = {}
) {
    var currentSection by remember { mutableStateOf<SettingsNavSection?>(null) }

    when (currentSection) {
        null -> {
            RootSettingsCategoryScreen(
                resumePlayback = resumePlayback,
                onResumePlaybackChange = onResumePlaybackChange,
                onNavigate = { currentSection = it },
                onBack = onBack,
                modifier = modifier
            )
        }
        SettingsNavSection.VideoDecoding -> {
            VideoDecodingCategoryScreen(
                decodeMode = decodeMode,
                onDecodeModeChange = onDecodeModeChange,
                debandFilter = debandFilter,
                onDebandFilterChange = onDebandFilterChange,
                videoScale = videoScale,
                onVideoScaleChange = onVideoScaleChange,
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.Audio -> {
            AudioCategoryScreen(
                volumeBoost = volumeBoost,
                onVolumeBoostChange = onVolumeBoostChange,
                pitchCorrection = pitchCorrection,
                onPitchCorrectionChange = onPitchCorrectionChange,
                audioOutputDriver = audioOutputDriver,
                onAudioOutputDriverChange = onAudioOutputDriverChange,
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.GesturesControls -> {
            GesturesControlsCategoryScreen(
                doubleTapSeekSeconds = doubleTapSeekSeconds,
                onDoubleTapSeekSecondsChange = onDoubleTapSeekSecondsChange,
                swipeToSeek = swipeToSeek,
                onSwipeToSeekChange = onSwipeToSeekChange,
                brightnessSwipe = brightnessSwipe,
                onBrightnessSwipeChange = onBrightnessSwipeChange,
                volumeSwipe = volumeSwipe,
                onVolumeSwipeChange = onVolumeSwipeChange,
                longPress2x = longPress2x,
                onLongPress2xChange = onLongPress2xChange,
                gestureSensitivity = gestureSensitivity,
                onGestureSensitivityChange = onGestureSensitivityChange,
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.BackgroundPlay -> {
            BackgroundPlayCategoryScreen(
                backgroundPlay = backgroundPlay,
                onBackgroundPlayChange = onBackgroundPlayChange,
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.Subtitles -> {
            SubtitlesCategoryScreen(
                preferredSubtitleLang = preferredSubtitleLang,
                onSubtitleLangChange = onSubtitleLangChange,
                onOpenAppearance = { currentSection = SettingsNavSection.SubtitleAppearance },
                onBack = { currentSection = null },
                modifier = modifier
            )
        }
        SettingsNavSection.SubtitleAppearance -> {
            SubtitleAppearanceSection(
                subtitleSize = subtitleSize,
                subtitlePosition = subtitlePosition,
                onSizeChange = onSubtitleSizeChange,
                onPositionChange = onSubtitlePositionChange,
                onBack = { currentSection = SettingsNavSection.Subtitles }
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

@Composable
private fun RootSettingsCategoryScreen(
    resumePlayback: Boolean,
    onResumePlaybackChange: (Boolean) -> Unit,
    onNavigate: (SettingsNavSection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(label = "PLAYBACK")
            SettingsCard {
                CategoryNavigationRow(
                    icon = Icons.Rounded.Memory,
                    title = "Video & Decoding",
                    subtitle = "Hardware decoder, deband, upscaling",
                    onClick = { onNavigate(SettingsNavSection.VideoDecoding) }
                )
                HorizontalDivider(color = Color(0xFF262626))
                CategoryNavigationRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Audio",
                    subtitle = "Volume boost, pitch correction, output driver",
                    onClick = { onNavigate(SettingsNavSection.Audio) }
                )
                HorizontalDivider(color = Color(0xFF262626))
                CategoryNavigationRow(
                    icon = Icons.Rounded.Settings,
                    title = "Gestures & Controls",
                    subtitle = "Seek duration, swipe zones, sensitivity",
                    onClick = { onNavigate(SettingsNavSection.GesturesControls) }
                )
                HorizontalDivider(color = Color(0xFF262626))
                CategoryNavigationRow(
                    icon = Icons.Filled.Audiotrack,
                    title = "Background Play",
                    subtitle = "Continue audio when minimized",
                    onClick = { onNavigate(SettingsNavSection.BackgroundPlay) }
                )
                HorizontalDivider(color = Color(0xFF262626))
                SettingsToggleRow(
                    title = "Resume Playback",
                    subtitle = "Remember position when exiting",
                    checked = resumePlayback,
                    onCheckedChange = onResumePlaybackChange
                )
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
            SectionHeader(label = "INFO")
            SettingsCard {
                CategoryNavigationRow(
                    icon = Icons.Rounded.Info,
                    title = "About",
                    subtitle = "Potato Player v1.0 · libmpv",
                    onClick = { onNavigate(SettingsNavSection.About) }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "Video & Decoding", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default Decode Mode",
                        color = Color.White,
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
                        color = Color.White.copy(alpha = 0.6f),
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "--scale · upscaling filter quality",
                        color = Color.White.copy(alpha = 0.5f),
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
        }
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "Audio", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Allow volume above 100% (up to 200%)",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "${volumeBoost}%",
                            color = Color(0xFF8B5CF6),
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
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF8B5CF6),
                            inactiveTrackColor = Color(0xFF262626)
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "--ao · audio API selection",
                        color = Color.White.copy(alpha = 0.5f),
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
                }
            }
        }
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
    gestureSensitivity: String,
    onGestureSensitivityChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "Gestures & Controls", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Double-Tap Seek Duration",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Time jumped per tap",
                        color = Color.White.copy(alpha = 0.5f),
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
                HorizontalDivider(color = Color(0xFF262626))
                SettingsToggleRow(
                    title = "Brightness Swipe",
                    subtitle = "Left-side vertical swipe",
                    checked = brightnessSwipe,
                    onCheckedChange = onBrightnessSwipeChange
                )
                HorizontalDivider(color = Color(0xFF262626))
                SettingsToggleRow(
                    title = "Volume Swipe",
                    subtitle = "Right-side vertical swipe",
                    checked = volumeSwipe,
                    onCheckedChange = onVolumeSwipeChange
                )
                HorizontalDivider(color = Color(0xFF262626))
                SettingsToggleRow(
                    title = "Long Press 2× Speed",
                    subtitle = "Hold screen for fast-forward",
                    checked = longPress2x,
                    onCheckedChange = onLongPress2xChange
                )
            }

            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gesture Sensitivity",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Deadzone & swipe threshold adjustment",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    SegmentedChipGroup(
                        options = listOf(
                            ChipOption("Low", "low"),
                            ChipOption("Normal", "normal"),
                            ChipOption("High", "high")
                        ),
                        selectedValue = gestureSensitivity,
                        onSelect = onGestureSensitivityChange
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundPlayCategoryScreen(
    backgroundPlay: String,
    onBackgroundPlayChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "Background Play", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Background Playback",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Behavior when app is minimized or switched away",
                        color = Color.White.copy(alpha = 0.5f),
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
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }

            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Screen-off Behavior",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Locking screen or turning display off will always pause playback immediately, regardless of background play mode.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "Subtitles", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Subtitle Language",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Auto-select matching track on file load",
                        color = Color.White.copy(alpha = 0.5f),
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
        }
    }
}

@Composable
private fun AboutCategoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "About", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Potato Player",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0",
                        color = Color(0xFF8B5CF6),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Powered by libmpv for maximum video & hardware decoding compatibility on Android.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Components & Design Tokens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionHeader(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF8B5CF6))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
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
            .background(Color(0xFF1A1A1A)),
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
                .clip(CircleShape)
                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
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
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF262626)
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFF8B5CF6) else Color(0xFF262626))
                    .clickable { onSelect(option.value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
