package com.tapman104.mpvplayer.player.state

data class SubtitleAppearanceState(
    val fontColor: String = "#FFFFFF",
    val bold: Boolean = false,
    val borderStyle: String = "outline",
    val borderSize: Float = 2.0f,
    val shadow: Float = 0.0f,
    // Alpha 0.0 (transparent) – 1.0 (opaque). Converted to ARGB hex string in ViewModel
    // before being passed to mpv as "sub-back-color".
    val backgroundAlpha: Float = 0.0f
)
