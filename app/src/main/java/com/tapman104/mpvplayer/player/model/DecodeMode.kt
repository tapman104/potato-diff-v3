package com.tapman104.mpvplayer.player.model

sealed class DecodeMode(val mpvValue: String) {
    object HW : DecodeMode("mediacodec")
    object HWPlus : DecodeMode("mediacodec-copy")
    object SW : DecodeMode("no")
}
