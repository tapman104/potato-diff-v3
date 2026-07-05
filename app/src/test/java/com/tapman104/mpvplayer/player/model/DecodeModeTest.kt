package com.tapman104.mpvplayer.player.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DecodeModeTest {

    @Test
    fun `SW maps to no`() {
        assertEquals("no", DecodeMode.SW.mpvValue)
    }

    @Test
    fun `HW maps to mediacodec`() {
        assertEquals("mediacodec", DecodeMode.HW.mpvValue)
    }

    @Test
    fun `HWPlus maps to mediacodec-copy`() {
        assertEquals("mediacodec-copy", DecodeMode.HWPlus.mpvValue)
    }
}
