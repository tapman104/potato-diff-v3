package com.tapman104.mpvplayer.player.engine

import android.net.Uri
import com.tapman104.mpvplayer.player.model.AspectRatioMode
import com.tapman104.mpvplayer.player.model.DecodeMode

/**
 * Sealed interface representing every user intent that can be dispatched to [PlayerEngine].
 * All UI interactions with playback must be expressed as one of these actions.
 *
 * Pipeline: UI → dispatch(PlayerAction) → PlayerEngine → MpvController → MPV
 */
sealed interface PlayerAction {

    // ── Playback ─────────────────────────────────────────────────────────────

    /** Resume playback. */
    data object Play : PlayerAction

    /** Pause playback immediately (no guard on current state). */
    data object Pause : PlayerAction

    /** Toggle between play and pause. */
    data object TogglePlay : PlayerAction

    /**
     * Pause playback unconditionally — used by screen-off receiver and background-play logic.
     * Unlike [Pause], this is a no-op if already paused (idempotent guard inside engine).
     */
    data object PausePlayback : PlayerAction

    // ── Seeking ───────────────────────────────────────────────────────────────

    /** Seek forward or backward by [offsetMs] milliseconds (negative = backward). */
    data class SeekRelative(val offsetMs: Long) : PlayerAction

    /**
     * Live seek while a gesture drag or slider drag is in progress.
     * Suppresses time-pos echo-backs in [EventProcessor] until [SeekCommit] is fired.
     */
    data class SeekGestureDrag(val positionMs: Long) : PlayerAction

    /**
     * Commit the final seek position after a drag ends.
     * Clears the seek-suppression flag in [EventProcessor].
     */
    data class SeekCommit(val positionMs: Long) : PlayerAction

    // ── Volume / Speed ────────────────────────────────────────────────────────

    /** Set mpv volume to [volume] (0–130, clamped). */
    data class SetVolume(val volume: Int) : PlayerAction

    /** Set playback speed to [speed]. */
    data class SetSpeed(val speed: Float) : PlayerAction

    /**
     * Temporarily ramp playback speed to [targetSpeed], saving the pre-override speed
     * so it can be restored later with [RestorePlaybackSpeed].
     */
    data class SetPlaybackSpeedRamped(val targetSpeed: Float) : PlayerAction

    /** Restore the playback speed saved before the last [SetPlaybackSpeedRamped]. */
    data object RestorePlaybackSpeed : PlayerAction

    // ── Audio tracks ──────────────────────────────────────────────────────────

    /** Switch the active audio track to the stream with [id]. */
    data class SetAudioTrack(val id: Int) : PlayerAction

    /** Sideload an external audio file via [uri]. */
    data class AddAudioTrack(val uri: Uri) : PlayerAction

    // ── Subtitle tracks ───────────────────────────────────────────────────────

    /** Switch the active subtitle track to the stream with [id] (-1 = disable). */
    data class SetSubtitleTrack(val id: Int) : PlayerAction

    /** Sideload an external subtitle file via [uri]. */
    data class AddSubtitle(val uri: Uri) : PlayerAction

    // ── Video settings ────────────────────────────────────────────────────────

    /** Switch hardware decode mode. Always resumes playback after the switch. */
    data class SetDecodeMode(val mode: DecodeMode) : PlayerAction

    /** Apply a video aspect-ratio override. */
    data class SetAspectRatio(val mode: AspectRatioMode) : PlayerAction

    /**
     * Apply zoom and pan values to the video output.
     * [zoomLog2] is log₂ scale (0 = 1×), [panX]/[panY] are normalised offsets.
     */
    data class SetZoomAndPan(val zoomLog2: Float, val panX: Float, val panY: Float) : PlayerAction

    // ── Playlist ──────────────────────────────────────────────────────────────

    /** Load [uri] and begin playback, adding it to the playlist if not already present. */
    data class LoadAndPlay(val uri: Uri) : PlayerAction

    /** Replace the current playlist with [uris]. */
    data class SetPlaylist(val uris: List<Uri>) : PlayerAction

    /** Append [uri] to the current playlist without starting playback. */
    data class AddToPlaylist(val uri: Uri) : PlayerAction

    /** Advance to the next item in the playlist. */
    data object PlayNext : PlayerAction

    /** Return to the previous item in the playlist. */
    data object PlayPrevious : PlayerAction

    /** Jump to the playlist item at [index]. */
    data class PlayAt(val index: Int) : PlayerAction

    // ── Error handling ────────────────────────────────────────────────────────
    /** Clear the current player error state. */
    data object ClearError : PlayerAction

    // ── Lock ──────────────────────────────────────────────────────────────────
    /** Toggle the screen lock overlay on/off. */
    data object ToggleLock : PlayerAction
}
