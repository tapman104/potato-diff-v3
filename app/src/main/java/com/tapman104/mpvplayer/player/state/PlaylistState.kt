package com.tapman104.mpvplayer.player.state

data class PlaylistState(
    val items: List<String> = emptyList(),
    val currentIndex: Int = 0
) {
    val hasPrevious: Boolean get() = currentIndex > 0
    val hasNext: Boolean get() = currentIndex < items.size - 1
    val currentUri: String? get() = items.getOrNull(currentIndex)
}
