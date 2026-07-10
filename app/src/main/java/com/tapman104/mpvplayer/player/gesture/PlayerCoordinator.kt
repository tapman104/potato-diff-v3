package com.tapman104.mpvplayer.player.gesture

import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModel

class PlayerCoordinator(
    private val viewModel: PlayerViewModel,
    private val overlay: OverlayController,
) : MpvPlayerController {

    // --- state queries ---
    override val durationMs get() = viewModel.playerState.value.durationMs
    override val currentPositionMs get() = viewModel.playerState.value.currentPositionMs
    override val isPaused get() = viewModel.playerState.value.isPaused
    override val currentZoomLog2 get() = viewModel.playerState.value.videoZoom
    override val currentPanX get() = viewModel.playerState.value.videoPanX
    override val currentPanY get() = viewModel.playerState.value.videoPanY
    override val volume get() = viewModel.playerState.value.volume.toFloat()
    override val maxStandardVolume get() = 100f
    override val maxBoostVolume get() = 130f
    override val brightness get() = viewModel.currentBrightness()
    override val screenWidthPx get() = viewModel.screenWidthPx
    override val screenHeightPx get() = viewModel.screenHeightPx
    override val isVolumeSideRight get() = true
    override val doubleTapSeekAreaWidthPercent get() = 30
    override val isDynamicSpeedOverlayEnabled get() = true
    override val playbackSpeed get() = viewModel.playerState.value.speed

    // --- playback commands ---
    override fun pause() = viewModel.pause()
    override fun unpause() = viewModel.play()
    override fun seekTo(positionMs: Long, precise: Boolean) = viewModel.seekTo(positionMs, precise)
    override fun seekForward(offsetMs: Long) = viewModel.seekRelative(offsetMs)
    override fun seekBackward(offsetMs: Long) = viewModel.seekRelative(-offsetMs)
    override fun seekGesture(positionMs: Long) = viewModel.seekGesture(positionMs)
    override fun seekCommit(positionMs: Long) = viewModel.seekCommit(positionMs)
    override fun setPlaybackSpeedRamped(targetSpeed: Float, stepCount: Int, stepDurationMs: Long) =
        viewModel.setSpeed(targetSpeed)
    override fun restorePlaybackSpeed() {}
    override fun setVolume(volume: Float) = viewModel.setVolume(volume)
    override fun setBrightness(brightness: Float) {}
    override fun setZoomAndPan(zoomLog2: Float, panX: Float, panY: Float) =
        viewModel.setZoomAndPan(zoomLog2, panX, panY)

    // --- overlay routing ---
    override fun showDoubleTapSeekOverlay(seekAmountSec: Int, isForward: Boolean, label: String) =
        overlay.showDoubleTapSeekOverlay(seekAmountSec, isForward, label)
    override fun hideDoubleTapSeekOverlay() = overlay.hideDoubleTapSeekOverlay()
    override fun showHorizontalSeekOverlay(currentTimeLabel: String, deltaLabel: String, targetPositionMs: Long) =
        overlay.showHorizontalSeekOverlay(currentTimeLabel, deltaLabel, targetPositionMs)
    override fun hideHorizontalSeekOverlay(delayMs: Long) = overlay.hideHorizontalSeekOverlay(delayMs)
    override fun showSpeedOverlay(speed: Float, interactiveSliderIndex: Int?) =
        overlay.showSpeedOverlay(speed, interactiveSliderIndex)
    override fun hideSpeedOverlay() = overlay.hideSpeedOverlay()
    override fun showVolumeOverlay(percentage: Int) = overlay.showVolumeOverlay(percentage)
    override fun hideVolumeOverlay() = overlay.hideVolumeOverlay()
    override fun showBrightnessOverlay(percentage: Int) = overlay.showBrightnessOverlay(percentage)
    override fun hideBrightnessOverlay() = overlay.hideBrightnessOverlay()
    override fun showPinchZoomOverlay(zoomPercentage: Int) = overlay.showPinchZoomOverlay(zoomPercentage)
    override fun hidePinchZoomOverlay() = overlay.hidePinchZoomOverlay()
    override fun showTapFeedback(x: Float, y: Float) = overlay.showTapFeedback(x, y)
    override fun scheduleTimer(delayMs: Long, action: () -> Unit) = overlay.scheduleTimer(delayMs, action)
    override fun cancelTimer(timerId: Any?) = overlay.cancelTimer(timerId)
    override fun triggerSingleTapAction() = overlay.triggerSingleTapAction()
}
