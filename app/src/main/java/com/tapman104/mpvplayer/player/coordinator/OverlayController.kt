package com.tapman104.mpvplayer.player.coordinator

interface OverlayController {
    fun showVolumeOverlay(percent: Int)
    fun hideVolumeOverlay()
    fun showBrightnessOverlay(percent: Int)
    fun hideBrightnessOverlay()
    fun showSpeedOverlay(speed: Float, sliderIndex: Int?)
    fun hideSpeedOverlay()
    fun showHorizontalSeekOverlay(currentLabel: String, deltaLabel: String, targetMs: Long)
    fun hideHorizontalSeekOverlay(delayMs: Long)
    fun showDoubleTapSeekOverlay(amountSec: Int, isForward: Boolean, label: String)
    fun hideDoubleTapSeekOverlay()
    fun showPinchZoomOverlay(zoomPercent: Int)
    fun hidePinchZoomOverlay()
    fun showTapFeedback(x: Float, y: Float)
    fun triggerSingleTapAction()
    fun scheduleTimer(delayMs: Long, action: () -> Unit): Any
    fun cancelTimer(timerId: Any?)

    companion object {
        val NO_OP = object : OverlayController {
            override fun showVolumeOverlay(percent: Int) {}
            override fun hideVolumeOverlay() {}
            override fun showBrightnessOverlay(percent: Int) {}
            override fun hideBrightnessOverlay() {}
            override fun showSpeedOverlay(speed: Float, sliderIndex: Int?) {}
            override fun hideSpeedOverlay() {}
            override fun showHorizontalSeekOverlay(currentLabel: String, deltaLabel: String, targetMs: Long) {}
            override fun hideHorizontalSeekOverlay(delayMs: Long) {}
            override fun showDoubleTapSeekOverlay(amountSec: Int, isForward: Boolean, label: String) {}
            override fun hideDoubleTapSeekOverlay() {}
            override fun showPinchZoomOverlay(zoomPercent: Int) {}
            override fun hidePinchZoomOverlay() {}
            override fun showTapFeedback(x: Float, y: Float) {}
            override fun triggerSingleTapAction() {}
            override fun scheduleTimer(delayMs: Long, action: () -> Unit): Any = Any()
            override fun cancelTimer(timerId: Any?) {}
        }
    }
}
