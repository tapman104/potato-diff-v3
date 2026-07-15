package com.tapman104.mpvplayer.player.service

import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tapman104.mpvplayer.player.engine.PlayerAction
import com.tapman104.mpvplayer.player.engine.PlayerEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground MediaSessionService for background playback.
 *
 * Provides a system-visible media session so the notification shade media controls
 * and Bluetooth devices can issue play/pause/seek commands.
 *
 * A minimal [StubPlayer] wraps [PlayerEngine.dispatch] to satisfy the media3
 * [Player] interface without modifying [PlayerEngine] itself.
 */
@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    @Inject
    lateinit var engine: PlayerEngine

    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        val stub = StubPlayer(engine, Looper.getMainLooper())
        mediaSession = MediaSession.Builder(this, stub).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }

    // ── Minimal Player stub ───────────────────────────────────────────────────

    /**
     * Minimal [androidx.media3.common.SimpleBasePlayer] that delegates
     * play/pause/seek to [PlayerEngine.dispatch].
     *
     * Only the operations needed for media session control are implemented;
     * everything else falls back to the no-op defaults in [SimpleBasePlayer].
     */
    private class StubPlayer(
        private val engine: PlayerEngine,
        looper: Looper,
    ) : androidx.media3.common.SimpleBasePlayer(looper) {

        override fun getState(): State {
            val ps = engine.state.value
            return State.Builder()
                .setAvailableCommands(
                    Player.Commands.Builder()
                        .addAll(
                            Player.COMMAND_PLAY_PAUSE,
                            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        )
                        .build()
                )
                .setPlayWhenReady(ps.isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlaylist(
                    listOf(
                        MediaItemData.Builder(/* uid = */ 0).build()
                    )
                )
                .build()
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): com.google.common.util.concurrent.ListenableFuture<*> {
            if (playWhenReady) engine.dispatch(PlayerAction.Play)
            else engine.dispatch(PlayerAction.Pause)
            return com.google.common.util.concurrent.Futures.immediateVoidFuture()
        }

        override fun handleSeek(
            mediaItemIndex: Int,
            positionMs: Long,
            seekCommand: Int,
        ): com.google.common.util.concurrent.ListenableFuture<*> {
            engine.dispatch(PlayerAction.SeekCommit(positionMs))
            return com.google.common.util.concurrent.Futures.immediateVoidFuture()
        }
    }
}
