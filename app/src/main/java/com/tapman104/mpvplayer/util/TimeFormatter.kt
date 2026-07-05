package com.tapman104.mpvplayer.util

object TimeFormatter {
    fun formatMs(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
