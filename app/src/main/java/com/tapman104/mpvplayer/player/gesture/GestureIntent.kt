package com.tapman104.mpvplayer.player.gesture

/**
 * Sealed hierarchy representing every player-affecting intent that a gesture
 * interaction can produce.
 *
 * `GestureHandler` (the Composable touch layer) translates raw pointer events
 * via `MpvGestureStateMachine` into these typed intents.  `PlayerViewModel`
 * collects them from a `MutableSharedFlow` and routes each one to the
 * appropriate coordinator.
 *
 * Keeping intents as a value type (sealed class data variants) means:
 *  - The gesture layer has **no** reference to any ViewModel or engine.
 *  - New gestures only require adding a variant here and a routing branch in
 *    `PlayerViewModel.collectGestureIntents()`.
 */
sealed class GestureIntent {

    /** Relative seek: add [deltaMs] to the current position (negative = rewind). */
    data class Seek(val deltaMs: Long) : GestureIntent()

    /** Preview seek position during gesture drag. */
    data class SeekGestureDrag(val positionMs: Long) : GestureIntent()

    /** Commit a seek to an absolute [positionMs] after a drag ends. */
    data class SeekCommit(val positionMs: Long) : GestureIntent()

    /** Toggle between play and pause. */
    data object TogglePlay : GestureIntent()

    /** Temporarily override playback speed to [speed]. */
    data class SetSpeed(val speed: Float) : GestureIntent()

    /**
     * Restore the speed that was active before the last [SetSpeed].
     * [previousSpeed] is carried for diagnostic / logging purposes.
     */
    data class RestoreSpeed(val previousSpeed: Float) : GestureIntent()

    /** Adjust system + engine volume by [delta] percentage points. */
    data class VolumeChange(val delta: Float) : GestureIntent()

    /** Adjust screen brightness by [delta] (−1f … +1f normalised). */
    data class BrightnessChange(val delta: Float) : GestureIntent()

    /**
     * Set video zoom and pan in one atomic update.
     * [scale] is log₂ zoom (0 = 1×), [panX]/[panY] are normalised offsets.
     */
    data class ZoomChange(val scale: Float, val panX: Float, val panY: Float) : GestureIntent()
}
