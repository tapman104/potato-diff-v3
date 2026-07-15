package com.tapman104.mpvplayer.player.domain.repository

import android.content.Context
import android.net.Uri
import com.tapman104.mpvplayer.player.viewmodel.PlaylistManager
import com.tapman104.mpvplayer.util.UriResolver

class LocalMediaRepository(
    private val context: Context,
    private val playlistManager: PlaylistManager,
) : MediaRepository {
    override suspend fun resolveUri(uri: Uri): String? {
        return UriResolver.resolveUri(context, uri)
    }

    override suspend fun getPlaylistNext(currentIndex: Int): String? {
        return playlistManager.getPlaylistNext(currentIndex)
    }

    override suspend fun getPlaylistPrevious(currentIndex: Int): String? {
        return playlistManager.getPlaylistPrevious(currentIndex)
    }
}
