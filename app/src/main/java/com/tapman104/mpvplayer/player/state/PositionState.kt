package com.tapman104.mpvplayer.player.state

data class PositionState(
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val cachedSec: Double = 0.0,
) {
    val currentPositionMs: Long get() = (positionSec * 1000).toLong()
    val durationMs: Long get() = (durationSec * 1000).toLong()
    val demuxerCacheTimeMs: Long get() = (cachedSec * 1000).toLong()
}
