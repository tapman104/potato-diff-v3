package com.tapman104.mpvplayer.player.input

import android.media.AudioManager
import android.view.KeyEvent
import kotlin.math.roundToInt

/**
 * Routes hardware volume-key events to a volume-sync callback.
 *
 * Extracted from [PlayerActivity] so the Activity contains no playback decisions.
 * Construct once per Activity and delegate [handleKeyDown] / [handleKeyUp] from
 * [android.app.Activity.onKeyDown] and [android.app.Activity.onKeyUp].
 *
 * @param onVolumeSync Called with the current system volume percentage (0–100) whenever
 *                     a volume key event is processed, so the engine can stay in sync.
 */
class KeyEventHandler(
    private val onVolumeSync: (Int) -> Unit,
) {
    /**
     * Call from [android.app.Activity.onKeyDown].
     * Returns the result of [android.app.Activity.onKeyDown] for volume keys so that
     * the system handles the actual volume change; then we read back the new level.
     *
     * @param keyCode Key code from the Activity callback.
     * @param superResult The result of calling `super.onKeyDown(keyCode, event)`.
     * @param audioManager The system AudioManager.
     * @return `true` if this handler consumed the event, `false` to pass through.
     */
    fun handleKeyDown(keyCode: Int, superResult: Boolean, audioManager: AudioManager): Boolean {
        if (isVolumeKey(keyCode)) {
            syncSystemVolume(audioManager)
            return superResult
        }
        return false
    }

    /**
     * Call from [android.app.Activity.onKeyUp].
     */
    fun handleKeyUp(keyCode: Int, superResult: Boolean, audioManager: AudioManager): Boolean {
        if (isVolumeKey(keyCode)) {
            syncSystemVolume(audioManager)
            return superResult
        }
        return false
    }

    private fun isVolumeKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE

    private fun syncSystemVolume(audioManager: AudioManager) {
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVol > 0) {
            val pct = ((currentVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt()
            onVolumeSync(pct)
        }
    }
}
