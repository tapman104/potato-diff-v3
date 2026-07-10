package com.tapman104.mpvplayer.player.gesture

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
}
