package mpv.potato.tapman104.player.model

enum class ViewMode(val label: String, val mpvPanScan: Float?, val mpvAspect: Float?) {
    FIT("Fit", mpvPanScan = 0f, mpvAspect = null),          // default fit
    FILL("Fill", mpvPanScan = 1f, mpvAspect = null),         // stretch to fill
    CROP("Crop", mpvPanScan = 0f, mpvAspect = -1f),          // crop to fill
    STRETCH("Stretch", mpvPanScan = null, mpvAspect = 0f);   // ignore AR, stretch

    val panScan: Float? get() = mpvPanScan
    val aspect: Float? get() = mpvAspect
}
