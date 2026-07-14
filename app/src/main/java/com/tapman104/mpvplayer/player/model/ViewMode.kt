package com.tapman104.mpvplayer.player.model

/**
 * Video scaling/aspect-ratio modes cycled by the rotate/fit button in the overlay.
 *
 * [mpvPanScan] — value for mpv's `panscan` property (null = don't set, use aspect override).
 * [mpvAspect]  — value for mpv's `video-aspect-override` property:
 *                  null  → "no" (container aspect),
 *                  -1f   → "-1" (disable override),
 *                  other → the numeric string.
 */
enum class ViewMode(val mpvPanScan: Float?, val mpvAspect: Float?) {
    /** Default: fit video inside the screen, preserving aspect ratio. */
    FIT(mpvPanScan = 0f, mpvAspect = null),

    /** Fill the screen, cropping if necessary (panscan = 1). */
    FILL(mpvPanScan = 1f, mpvAspect = null),

    /** Stretch to fill the entire screen, ignoring aspect ratio. */
    STRETCH(mpvPanScan = 0f, mpvAspect = -1f),
}
