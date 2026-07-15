package com.tapman104.mpvplayer.player.viewmodel

import android.content.Context
import android.net.Uri
import com.tapman104.mpvplayer.player.state.PlaylistState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlaylistManager(
    private val context: Context,
    private val onLoadFile: (resolvedPath: String) -> Unit,
    private val hasSurface: () -> Boolean
) {
    var pendingFileUri: Uri? = null

    private val _playlistState = MutableStateFlow(PlaylistState())
    val playlistState: StateFlow<PlaylistState> = _playlistState.asStateFlow()

    private fun resolveUri(uri: Uri): String {
        return com.tapman104.mpvplayer.util.UriResolver.resolveUri(context, uri)
    }

    fun resolveUriForSurface(uri: Uri): String = resolveUri(uri)

    fun onSurfaceReady() {
        val pending = pendingFileUri ?: return
        pendingFileUri = null
        onLoadFile(resolveUri(pending))
    }

    fun loadAndPlay(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Permission may already be held or not grantable; proceed anyway
        }
        // Update playlist state so currentUri reflects the new file immediately.
        // If the URI is already in the list, jump to it; otherwise append and jump.
        _playlistState.update { current ->
            val uriStr = uri.toString()
            val existingIndex = current.items.indexOf(uriStr)
            if (existingIndex >= 0) {
                current.copy(currentIndex = existingIndex)
            } else {
                current.copy(
                    items = current.items + uriStr,
                    currentIndex = current.items.size
                )
            }
        }

        if (hasSurface()) {
            onLoadFile(resolveUri(uri))
        } else {
            pendingFileUri = uri
        }
    }

    fun setPlaylist(uris: List<Uri>) {
        val stringUris = uris.map { it.toString() }
        _playlistState.update { PlaylistState(items = stringUris, currentIndex = 0) }
    }

    fun addToPlaylist(uri: Uri) {
        _playlistState.update { current ->
            current.copy(items = current.items + uri.toString())
        }
    }

    fun playNext() {
        val playlist = _playlistState.value
        if (playlist.hasNext) {
            val nextIndex = playlist.currentIndex + 1
            _playlistState.update { it.copy(currentIndex = nextIndex) }
            val nextUri = Uri.parse(playlist.items[nextIndex])
            loadAndPlay(nextUri)
        }
    }

    fun playPrevious() {
        val playlist = _playlistState.value
        if (playlist.hasPrevious) {
            val prevIndex = playlist.currentIndex - 1
            _playlistState.update { it.copy(currentIndex = prevIndex) }
            val prevUri = Uri.parse(playlist.items[prevIndex])
            loadAndPlay(prevUri)
        }
    }

    fun playAt(index: Int) {
        val playlist = _playlistState.value
        if (index in playlist.items.indices) {
            _playlistState.update { it.copy(currentIndex = index) }
            val uri = Uri.parse(playlist.items[index])
            loadAndPlay(uri)
        }
    }

    fun onPlaybackEnded() {
        playNext()
    }

    fun getPlaylistNext(currentIndex: Int): String? {
        val playlist = _playlistState.value
        val nextIndex = currentIndex + 1
        return if (nextIndex in playlist.items.indices) playlist.items[nextIndex] else null
    }

    fun getPlaylistPrevious(currentIndex: Int): String? {
        val playlist = _playlistState.value
        val prevIndex = currentIndex - 1
        return if (prevIndex in playlist.items.indices) playlist.items[prevIndex] else null
    }
}
