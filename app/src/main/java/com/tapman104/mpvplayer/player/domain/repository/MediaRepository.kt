package com.tapman104.mpvplayer.player.domain.repository

import android.net.Uri

interface MediaRepository {
    suspend fun resolveUri(uri: Uri): String?
    suspend fun getPlaylistNext(currentIndex: Int): String?
    suspend fun getPlaylistPrevious(currentIndex: Int): String?
}
