package com.tapman104.mpvplayer.player.model

sealed class PlayerError {
    data class EngineError(val message: String) : PlayerError()
    data class FileNotFound(val path: String) : PlayerError()
    data class UnsupportedFormat(val path: String) : PlayerError()
    data object UnknownError : PlayerError()
}
