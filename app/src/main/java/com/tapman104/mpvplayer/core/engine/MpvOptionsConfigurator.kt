package com.tapman104.mpvplayer.core.engine

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import `is`.xyz.mpv.MPVLib

enum class DebandingMode {
    NONE,
    CPU,
    GPU
}

data class GeneralOptions(
    val profile: String = "fast",
    val gpuNext: Boolean = false, // if true, uses "gpu-next" instead of "gpu"
    val useVulkan: Boolean = false,
    val tryHwDecoding: Boolean = true,
    val useYuv420p: Boolean = false,
    val cacheMegs: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32,
    val verboseLogging: Boolean = false,
    val keepOpen: Boolean = true,
    val inputDefaultBindings: Boolean = true,
    val tlsVerify: Boolean = true,
    val tlsCaFile: String? = null,
    val screenshotDir: String? = null,
    val defaultSpeed: Double = 1.0,
    val usePreciseSeeking: Boolean = false,
    val enableDirectRendering: Boolean = true, // vd-lavc-dr
    val filmGrainCpu: String = "cpu", // vd-lavc-film-grain
    val glslShaders: String? = null,
    val enableOpenglPbo: Boolean = true,
    val disableOpenglEarlyFlush: Boolean = true,
    val vdLavcThreads: String = "0",
    val videoSync: String = "audio",
    val interpolation: Boolean = false,
    val cachePauseWait: Boolean = true,
    val scale: String = "bilinear",
    val cscale: String = "bilinear",
    val dscale: String = "bilinear",
    val ditherDepth: Boolean = false,
    // Video Equalizer (-100 to 100)
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val gamma: Int = 0,
    val hue: Int = 0
)

data class SubtitleOptions(
    val autoSelectSubtitles: Boolean = false,
    val preferredSlang: String = "",
    val subFontsDir: String? = null,
    val subFontProvider: String = "none",
    val subDelayMs: Long = 0L,
    val subSpeed: Double = 1.0,
    val fontName: String = "Roboto",
    val overrideAssSubs: Boolean = true,
    val fontSize: Int = 55,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val justify: String = "auto",
    val textColorHexString: String = "#FFFFFF",
    val backgroundColorHexString: String = "#00000000",
    val borderColorHexString: String = "#000000",
    val borderSize: Int = 3,
    val borderStyle: String = "outline-and-shadow",
    val shadowOffset: Float = 1.0f,
    val subPos: Int = 100, // Bottom of screen (percentage)
    val secondarySubPos: Int = 10, // Top of screen (percentage) to avoid overlap with primary
    val subScale: Float = 1.0f,
    val scaleByWindow: Boolean = true
)

data class AudioOptions(
    val autoSelectAudio: Boolean = false,
    val preferredAlang: String = "",
    val audioDelayMs: Long = 0L,
    val audioPitchCorrection: Boolean = true,
    val volumeBoostCap: Int = 30, // volume-max = 100 + cap
    val volumeNormalization: Boolean = false
)

data class PostInitOptions(
    val debandingMode: DebandingMode = DebandingMode.NONE,
    val enabledStatsPage: Int = 0
)

/**
 * Standalone MPV option and property configurator extracted from battle-tested mpvEx architecture.
 * Contains no Koin or preference injections, relying on sensible defaults and parameter objects for things that vary.
 */
class MpvOptionsConfigurator(
    private val context: Context,
    private val onVoConfigured: ((String) -> Unit)? = null
) {
    // -----------------------------------------------------------------------
    // Asset setup
    // -----------------------------------------------------------------------

    /**
     * Copies bundled font assets to the app's files directory so MPV can find them
     * for subtitle rendering. Safe to call multiple times — skips if the file
     * already exists. Must be called before [initOptions].
     */
    fun copyFontAssets() {
        val fontsDir = java.io.File(context.filesDir, "fonts")
        if (!fontsDir.exists()) fontsDir.mkdirs()

        val fontFile = java.io.File(fontsDir, "Roboto-Regular.ttf")
        if (!fontFile.exists()) {
            try {
                context.assets.open("Roboto-Regular.ttf").use { input ->
                    java.io.FileOutputStream(fontFile).use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "Copied Roboto-Regular.ttf to fonts directory")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy font asset", e)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Pre-init options
    // -----------------------------------------------------------------------

    /**
     * Configures all pre-init options (general engine, video, subtitles, and audio).
     * This MUST be called before MPVLib.init().
     */
    fun initOptions(
        general: GeneralOptions = GeneralOptions(),
        subtitles: SubtitleOptions = SubtitleOptions(),
        audio: AudioOptions = AudioOptions()
    ) {
        Log.d(TAG, "Configuring pre-init MPV options")

        // Base MPV engine options
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", context.filesDir.path)
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("idle", "once")

        // Profile and Video Output
        MPVLib.setOptionString("profile", general.profile)
        val vo = if (general.gpuNext) "gpu-next" else "gpu"
        MPVLib.setOptionString("vo", vo)
        onVoConfigured?.invoke(vo)

        MPVLib.setOptionString("gpu-context", if (general.useVulkan) "androidvk" else "android")

        // Hardware Decoding fallback order: HW+ (mediacodec) -> HW (mediacodec-copy) -> SW (no)
        MPVLib.setOptionString(
            "hwdec",
            if (general.tryHwDecoding) "mediacodec,mediacodec-copy,no" else "no"
        )
        MPVLib.setOptionString("hwdec-codecs", "all")

        if (general.useYuv420p) {
            MPVLib.setOptionString("vf", "format=yuv420p")
        }

        // Cap demuxer cache for mobile to prevent memory issues.
        // cacheMegs * 1024 * 1024 converts MiB to bytes. Use Long to prevent overflow.
        val cacheBytes = (general.cacheMegs.toLong() * 1024L * 1024L).toString()
        MPVLib.setOptionString("demuxer-max-bytes", cacheBytes)
        MPVLib.setOptionString("demuxer-max-back-bytes", cacheBytes)

        val logLevel = if (general.verboseLogging) "v" else "warn"
        MPVLib.setOptionString("msg-level", "all=$logLevel")

        MPVLib.setPropertyBoolean("keep-open", general.keepOpen)
        MPVLib.setPropertyBoolean("input-default-bindings", general.inputDefaultBindings)

        MPVLib.setOptionString("tls-verify", if (general.tlsVerify) "yes" else "no")
        val caFile = general.tlsCaFile ?: "${context.filesDir.path}/cacert.pem"
        MPVLib.setOptionString("tls-ca-file", caFile)

        val screenshotDir = general.screenshotDir ?: run {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            dir.mkdirs()
            dir.path
        }
        MPVLib.setOptionString("screenshot-directory", screenshotDir)

        // Video Equalizer properties
        MPVLib.setOptionString("brightness", general.brightness.toString())
        MPVLib.setOptionString("contrast", general.contrast.toString())
        MPVLib.setOptionString("saturation", general.saturation.toString())
        MPVLib.setOptionString("gamma", general.gamma.toString())
        MPVLib.setOptionString("hue", general.hue.toString())

        MPVLib.setOptionString("speed", general.defaultSpeed.toString())
        MPVLib.setOptionString("vd-lavc-film-grain", general.filmGrainCpu)

        val preciseSeek = general.usePreciseSeeking
        MPVLib.setOptionString("hr-seek", if (preciseSeek) "yes" else "no")
        MPVLib.setOptionString("hr-seek-framedrop", if (preciseSeek) "no" else "yes")

        // Performance & Threading options
        MPVLib.setOptionString("vd-lavc-threads", general.vdLavcThreads)
        MPVLib.setOptionString("video-sync", general.videoSync)
        MPVLib.setOptionString("interpolation", if (general.interpolation) "yes" else "no")
        MPVLib.setOptionString("cache-pause-wait", if (general.cachePauseWait) "1" else "0")
        MPVLib.setOptionString("scale", general.scale)
        MPVLib.setOptionString("cscale", general.cscale)
        MPVLib.setOptionString("dscale", general.dscale)
        MPVLib.setOptionString("dither-depth", if (general.ditherDepth) "auto" else "no")

        // Shaders and Rendering optimizations
        if (general.enableDirectRendering) {
            MPVLib.setOptionString("vd-lavc-dr", "yes")
        }
        if (!general.useVulkan) {
            MPVLib.setOptionString("opengl-pbo", if (general.enableOpenglPbo) "yes" else "no")
            MPVLib.setOptionString("opengl-early-flush", if (general.disableOpenglEarlyFlush) "no" else "yes")
        }
        if (!general.glslShaders.isNullOrBlank()) {
            MPVLib.setOptionString("glsl-shaders", general.glslShaders)
        }

        setupSubtitlesOptions(subtitles)
        setupAudioOptions(audio)
    }

    // -----------------------------------------------------------------------
    // Subtitle options
    // -----------------------------------------------------------------------

    /**
     * Configures subtitle track selection, font directory, delay, speed, and ASS styling.
     */
    fun setupSubtitlesOptions(options: SubtitleOptions = SubtitleOptions()) {
        Log.d(TAG, "Configuring subtitle options")

        // Disable MPV's automatic subtitle selection by default.
        // The app handles track selection manually via TrackSelector to respect user choices.
        val slang = if (options.autoSelectSubtitles) options.preferredSlang else ""
        val subAuto = if (options.autoSelectSubtitles) "exact" else "no"
        val subsFallback = if (options.autoSelectSubtitles) "yes" else "no"

        MPVLib.setOptionString("slang", slang)
        MPVLib.setOptionString("sub-auto", subAuto)
        MPVLib.setOptionString("sub-file-paths", "")
        MPVLib.setOptionString("subs-fallback", subsFallback)

        val fontsDir = options.subFontsDir ?: "${context.filesDir.path}/fonts"
        MPVLib.setOptionString("sub-fonts-dir", fontsDir)
        MPVLib.setOptionString("sub-font-provider", options.subFontProvider)

        // Delay and speed for both primary and secondary
        val subDelay = (options.subDelayMs / 1000.0).toString()
        val subSpeed = options.subSpeed.toString()
        MPVLib.setOptionString("sub-delay", subDelay)
        MPVLib.setOptionString("sub-speed", subSpeed)
        MPVLib.setOptionString("secondary-sub-delay", subDelay)
        MPVLib.setOptionString("secondary-sub-speed", subSpeed)

        if (options.fontName.isNotBlank()) {
            MPVLib.setOptionString("sub-font", options.fontName)
            MPVLib.setOptionString("secondary-sub-font", options.fontName)
        }

        if (options.overrideAssSubs) {
            MPVLib.setOptionString("sub-ass-override", "force")
            MPVLib.setOptionString("sub-ass-justify", "yes")
            MPVLib.setOptionString("secondary-sub-ass-override", "force")
        } else {
            MPVLib.setOptionString("sub-ass-override", "no")
            MPVLib.setOptionString("secondary-sub-ass-override", "no")
        }

        // Apply typography/styling to both primary ("sub") and secondary ("secondary-sub") tracks
        applySubStyle("sub", options, posOverride = options.subPos)
        applySubStyle("secondary-sub", options, posOverride = options.secondarySubPos)

        val scaleByWindow = if (options.scaleByWindow) "yes" else "no"
        MPVLib.setOptionString("sub-scale-by-window", scaleByWindow)
        MPVLib.setOptionString("sub-use-margins", scaleByWindow)
        MPVLib.setOptionString("secondary-sub-scale-by-window", scaleByWindow)
        MPVLib.setOptionString("secondary-sub-use-margins", scaleByWindow)
    }

    /**
     * Applies the shared typography and styling options to the sub track identified
     * by [prefix] (either "sub" or "secondary-sub"). [posOverride] lets the caller
     * supply a different vertical position for primary vs secondary tracks.
     */
    private fun applySubStyle(prefix: String, options: SubtitleOptions, posOverride: Int) {
        MPVLib.setOptionString("$prefix-font-size",     options.fontSize.toString())
        MPVLib.setOptionString("$prefix-bold",          if (options.bold) "yes" else "no")
        MPVLib.setOptionString("$prefix-italic",        if (options.italic) "yes" else "no")
        MPVLib.setOptionString("$prefix-justify",       options.justify)
        MPVLib.setOptionString("$prefix-color",         options.textColorHexString)
        MPVLib.setOptionString("$prefix-back-color",    options.backgroundColorHexString)
        MPVLib.setOptionString("$prefix-border-color",  options.borderColorHexString)
        MPVLib.setOptionString("$prefix-border-size",   options.borderSize.toString())
        MPVLib.setOptionString("$prefix-border-style",  options.borderStyle)
        MPVLib.setOptionString("$prefix-shadow-offset", options.shadowOffset.toString())
        MPVLib.setOptionString("$prefix-scale",         options.subScale.toString())
        MPVLib.setOptionString("$prefix-pos",           posOverride.toString())
    }

    // -----------------------------------------------------------------------
    // Audio options
    // -----------------------------------------------------------------------

    /**
     * Configures audio track selection, delay, pitch correction, volume boost, and normalization.
     */
    fun setupAudioOptions(options: AudioOptions = AudioOptions()) {
        Log.d(TAG, "Configuring audio options")

        // Disable MPV's automatic audio selection by default.
        // The app handles track selection manually via TrackSelector to respect user choices.
        val alang = if (options.autoSelectAudio) options.preferredAlang else ""
        MPVLib.setOptionString("alang", alang)
        MPVLib.setOptionString("audio-delay", (options.audioDelayMs / 1000.0).toString())
        MPVLib.setOptionString("audio-pitch-correction", if (options.audioPitchCorrection) "yes" else "no")
        MPVLib.setOptionString("volume-max", (options.volumeBoostCap + 100).toString())

        // Volume normalization using dynamic audio normalization filter
        MPVLib.setOptionString("af", if (options.volumeNormalization) "dynaudnorm" else "")
    }

    // -----------------------------------------------------------------------
    // Post-init options
    // -----------------------------------------------------------------------

    /**
     * Configures post-init options such as debanding filters and statistics page overlays.
     * This MUST be called after MPVLib.init().
     */
    fun postInitOptions(options: PostInitOptions = PostInitOptions()) {
        Log.d(TAG, "Configuring post-init MPV options")

        when (options.debandingMode) {
            DebandingMode.NONE -> {
                MPVLib.setOptionString("deband", "no")
                // runCatching because @deband may not exist yet if no file has loaded
                runCatching { MPVLib.command("vf", "remove", "@deband") }
            }
            DebandingMode.CPU -> {
                MPVLib.setOptionString("deband", "no")
                runCatching { MPVLib.command("vf", "add", "@deband:gradfun=radius=12") }
            }
            DebandingMode.GPU -> {
                // runCatching because @deband may not exist yet if no file has loaded
                runCatching { MPVLib.command("vf", "remove", "@deband") }
                MPVLib.setOptionString("deband", "yes")
            }
        }

        if (options.enabledStatsPage != 0) {
            MPVLib.command("script-binding", "stats/display-stats-toggle")
            MPVLib.command("script-binding", "stats/display-page-${options.enabledStatsPage}")
        }
    }

    companion object {
        private const val TAG = "MpvOptionsConfigurator"
    }
}
