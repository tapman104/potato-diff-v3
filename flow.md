# Potato Player MPV — Comprehensive Codebase Flow & System Architecture Report

This report provides an in-depth, architectural breakdown of the **Potato Player MPV** codebase. It illustrates how every component interacts, who commands whom, how execution flows through the system, and identifies the exact leader files, their roles, and their key functions across all subsystems.

---

## 1. Executive Summary: The Leaders of the Codebase

In a modern reactive Android application built with **Jetpack Compose**, **MVVM**, and a native **JNI engine (`libmpv`)**, clear separation of concerns is vital. The system is governed by four distinct **Leader Components**, each ruling its own domain:

```
+----------------------------------------------------------------------------------------------------+
|                                      THE CODEBASE LEADERS                                          |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  1. STATE & BUSINESS ORCHESTRATION LEADER                                                          |
|     File: [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt)                                           |
|     Role: The supreme brain of the player. Commands playback, manages UI state (`PlayerState`),    |
|           coordinates playlists, subtitles, audio tracks, and saves/restores watch progress.       |
|                                                                                                    |
|  2. NATIVE MEDIA ENGINE LEADERS                                                                    |
|     Files: [MpvController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt) & [MpvOptionsConfigurator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvOptionsConfigurator.kt)               |
|     Role: The bridge between Kotlin and native C/C++ `libmpv`. Commands surface binding with       |
|           generation-aware safety, configures engine options/VO, and dispatches native events back.|
|                                                                                                    |
|  3. TOUCH & GESTURE LEADERS                                                                        |
|     Files: [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt) & [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt)             |
|     Role: The input governor. Captures multi-touch gestures, classifies pointer events through a   |
|           single-ownership state machine, modifies OS volume/brightness, and commands visual overlays.|
|                                                                                                    |
|  4. UI PRESENTATION LEADERS                                                                        |
|     Files: [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt) & [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)                         |
|     Role: The Compose visual root. Renders the video output surface, overlays interactive controls,|
|           dialogs, and routes user clicks and drags to the ViewModel and Gesture Handler.          |
+----------------------------------------------------------------------------------------------------+
```

---

## 2. System Architecture & Layered Flow Map

The codebase is organized into five distinct layers. Data flows **downward** as command invocations and **upward** as reactive state updates (`StateFlow`) and asynchronous native event notifications.

```
======================================================================================================
                                     LAYER 1: APPLICATION ENTRY
======================================================================================================
  [MainActivity.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/MainActivity.kt) <---> [HomeScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/home/ui/HomeScreen.kt) ----(Launches Intent with Media URI)----> [PlayerActivity.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/PlayerActivity.kt)
                                                                                                 |
                                                                                (Creates ViewModel & SurfaceView)
                                                                                                 |
                                                                                                 v
======================================================================================================
                                  LAYER 2: UI & PRESENTATION LAYER (Compose)
======================================================================================================
                                                 [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt)
                                                        |
                 +--------------------------------------+--------------------------------------+
                 |                                      |                                      |
                 v                                      v                                      v
         [PlayerVideo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerVideo.kt)                         [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)                        [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt)
    (Wraps Android SurfaceView)                         |                                      |
                 |                     +----------------+----------------+                     v
                 |                     |                |                |         [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt)
                 |                     v                v                v                     |
                 |            [PlayerTopBar]   [BottomControls]  [QuickActions]                v
                 |                     |                |                |               [GestureModels.kt]
                 |                     +----------------+----------------+         (MpvPlayerController / States)
                 |                                      |                                      |
                 |                              (User Commands)                                v
                 |                                      |                            [GestureIndicators.kt]
                 v                                      v                         (Volume, Brightness, Seek, Speed)
======================================================================================================
                              LAYER 3: STATE & BUSINESS LOGIC LAYER (ViewModel)
======================================================================================================
                                              [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt) <-----------------------------+
                                                        |                                      |
                 +--------------------------------------+--------------------------------------+
                 |                                      |                                      |
                 v                                      v                                      v
       [PlaylistManager.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlaylistManager.kt)                     [SubtitleController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/SubtitleController.kt)             [ResumePositionManager.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/ResumePositionManager.kt)
  (Manages Queue & Next/Prev)            (Manages Subtitle Tracks & Styles)    (Saves/Restores Progress)
                 |                                      |                                      |
                 +--------------------------------------+--------------------------------------+
                                                        |
                                            (High-Level Engine Calls)
                                                        |
                                                        v
======================================================================================================
                                   LAYER 4: NATIVE ENGINE FACADE (MPV)
======================================================================================================
                                               [MpvController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt)
                                                        |
                 +-------------------+------------------+-------------------+
                 |                   |                  |                   |
                 v                   v                  v                   v
      [MpvCommandExecutor.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvCommandExecutor.kt)      [MpvSurface.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvSurface.kt)   [MpvOptionsConfigurator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvOptionsConfigurator.kt)  [MpvEventDispatcher.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvEventDispatcher.kt)
    (Sends C-strings & Debounced (Generation-Aware   (Configures Pre-init    (Receives Native Events &
     Seeks to libmpv thread)      Surface Binding)    VO, GPU & Font Assets)  Property Change Callbacks)
                 |                   |                  |                   ^
                 |                   |                  |                   |
                 v                   v                  v           (Property Changes)
         +---------------------------------------------------------------+  |
         |                         libmpv (Native JNI)                   |  |
         +---------------------------------------------------------------+  |
                                                                            |
                                                    (Direct observeProperty / Property Observers)
======================================================================================================
                                  LAYER 5: STORAGE & PERSISTENCE LAYER
======================================================================================================
                 [AppDatabase.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/AppDatabase.kt)                                     [UserPreferencesRepository.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/preferences/UserPreferencesRepository.kt)
                 [ResumePositionDao.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/ResumePositionDao.kt)
              (Room SQLite Database for Playback Progress)                        (DataStore for Styling & Hardware Decode Modes)
======================================================================================================
```

---

## 3. Detailed Chained Execution Pipelines

To understand how files work with each other, we trace the exact **chain of execution** across four primary runtime scenarios.

### Pipeline A: Media Initialization & Playback Start Flow
When a user selects a video file from [HomeScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/home/ui/HomeScreen.kt) or launches the player from an external intent, the following chain reaction occurs:

```
[PlayerActivity.onCreate]
       │
       ├─► 1. Resolves URI via [UriResolver.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/util/UriResolver.kt) (converts Content URI to absolute file path or fd).
       ├─► 2. Creates Android `SurfaceView` and instantiates [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt) via [PlayerViewModelFactory.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModelFactory.kt).
       │
       ▼
[PlayerViewModel.init]
       │
       ├─► 3. Registers itself as an [MpvEventListener](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvEventDispatcher.kt) on `controller.dispatcher`.
       ├─► 4. Calls `controller.init()`.
       │
       ▼
[MpvController.init]
       │
       ├─► 5. Delegates option configuration to [MpvOptionsConfigurator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvOptionsConfigurator.kt), which copies `Roboto-Regular.ttf` font asset to internal app storage for ASS/OSD subtitle rendering.
       ├─► 6. Invokes native JNI `MPVLib.create()`, sets GPU/hardware decoding configuration (`vo=gpu`, `hwdec=mediacodec`) via `configurator.initOptions()`.
       ├─► 7. Calls `MPVLib.init()`, registers native property observers directly via `MPVLib.observeProperty()`, and connects [MpvEventDispatcher.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvEventDispatcher.kt).
       │
       ▼
[PlayerActivity.onStart]
       │
       ├─► 8. Passes `SurfaceView` into [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt) -> [PlayerVideo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerVideo.kt).
       ├─► 9. Binds surface via `viewModel.controller.surface.attachSurface(surfaceView.holder.surface)`.
       │
       ▼
[MpvSurface.surfaceCreated] (Generation-Aware Surface Binding)
       │
       ├─► 10. Increments generation counter: `val gen = executor.nextSurfaceGeneration()`.
       ├─► 11. Submits attach task to [MpvCommandExecutor.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvCommandExecutor.kt) -> calls `MPVLib.attachSurface(surface)` and `vo=gpu`.
       ├─► 12. Invokes main-thread callback `onSurfaceReady()`.
       │
       ▼
[PlayerViewModel.onSurfaceReady]
       │
       └─► 13. Calls `playlistManager.loadAndPlay(uri)` -> `controller.executor.loadFile(path)`.
               Video playback begins natively on the bound Android GPU surface!
```

---

### Pipeline B: User Playback Control & State Synchronization Loop
When a user interacts with on-screen controls (e.g., clicking **Play/Pause** or scrubbing the seekbar in [PlayerBottomControls.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerBottomControls.kt)):

```
[User Taps Play/Pause Button in PlayerBottomControls]
       │
       ├─► 1. Lambda `onTogglePlay()` invoked, routed up through [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt) -> [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt).
       │
       ▼
[PlayerActivity] (Observing UI events)
       │
       ├─► 2. Calls `viewModel.togglePlay()`.
       │
       ▼
[PlayerViewModel.togglePlay]
       │
       ├─► 3. Delegates command to `controller.executor.togglePlay()`.
       │
       ▼
[MpvCommandExecutor.togglePlay] (Dedicated MPV Thread)
       │
       ├─► 4. Executes on `"mpv-engine-thread"`: checks current pause property and sets `MPVLib.setPropertyBoolean("pause", !paused)`.
       │
       ▼
[Native libmpv Engine]
       │
       ├─► 5. Engine toggles internal pause state and fires property change event for `"pause"`.
       │
       ▼
[MpvEventDispatcher]
       │
       ├─► 6. Receives JNI callback, identifies property change for [MpvConstants.PROP_PAUSE](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvConstants.kt), notifies registered listeners.
       │
       ▼
[PlayerViewModel.onPropertyChange(property = "pause", value = true/false)]
       │
       ├─► 7. Updates immutable state: `_playerState.update { it.copy(isPlaying = !value) }`.
       │
       ▼
[Jetpack Compose UI (PlayerScreen / PlayerBottomControls)]
       │
       └─► 8. Collects new [PlayerState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/PlayerState.kt) via `collectAsStateWithLifecycle()`, recomposes icon from Pause to Play seamlessly!
```

---

### Pipeline C: Multi-Touch Gesture & State Machine Pipeline
When a user gestures on the playback screen, input is intercepted by [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt) and governed by the single-ownership state machine [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt) operating on domain models defined in [GestureModels.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureModels.kt):

```
[User Swipes, Taps, or Pinches on Playback Viewport]
       │
       ├─► 1. Raw pointer events (`pointerInput`) captured by [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt).
       ├─► 2. Events (`onPointerDown`, `onPointerMove`, `onPointerUp`) fed into [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt).
       │
       ▼
[MpvGestureStateMachine] (Single-Ownership Mutually Exclusive Classification)
       │
       ├─► 3. Evaluates active pointers against current `GestureState`:
       │      ├── [GestureState.TapCandidate]: Initial touch; evaluates tap vs double-tap vs drag.
       │      ├── [GestureState.MultiTapSeeking]: Double-tap on screen edges; triggers jump seek forward/backward.
       │      ├── [GestureState.LongPress] / [DynamicSpeedScrub]: Long-press or top-edge scrub; engages speed boost/scrub.
       │      ├── [GestureState.VerticalSwipe]: Left/right edge drag; classifies as brightness or volume scrubbing.
       │      ├── [GestureState.HorizontalSeek]: Horizontal drag across viewport; calculates target seek time.
       │      └── [GestureState.PinchZoomPan] / [SinglePan]: Two-finger pinch or drag; calculates video scale and xy-translation.
       │
       ▼
[MpvPlayerController Interface Execution] (Implemented by GestureHandler)
       │
       ├─► 4. For Volume / Brightness: Modifies OS `AudioManager` stream volume or window screen brightness attributes directly.
       ├─► 5. For Seeking: Calls `seekGesture(targetMs)` during scrub -> [MpvCommandExecutor.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvCommandExecutor.kt) debounces requests via `AtomicReference<Double?> pendingSeek` (sending only keyframe seeks during drag). When released, calls `seekCommit(targetMs)` for precise positioning!
       ├─► 6. For Zoom / Pan: Commands `executor.setVideoZoom(zoom)` and `executor.setVideoPan(x, y)`.
       │
       ▼
[Modular Visual Feedback Indicators in GestureIndicators.kt]
       │
       └─► 7. Appropriate overlay composable from [GestureIndicators.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureIndicators.kt) animates in real-time on screen:
              ├── `VolumeIndicator` & `BrightnessIndicator` (Vertical Edge Scrubbing using `IndicatorPill`)
              ├── `HorizontalSeekIndicator` & `SeekCircleIndicator` (Scrubbing & Double-Tap Jump Ripple)
              ├── `SpeedIndicator` (Long-Press Speed Boost & Dynamic Scrubbing)
              └── `PinchZoomIndicator` (Multi-Touch Scale Feedback)
```

---

### Pipeline D: Subtitle, Audio & Dialog Management Flow
When a user switches audio tracks, changes subtitle languages, or customizes ASS/OSD subtitle appearance:

```
[User Taps Audio/Subtitle/Decode Icon in PlayerTopBar or PlayerQuickActions]
       │
       ├─► 1. Sets dialog flag (`showSubtitleDialog`, `showAudioDialog`, or `showDecodeDialog`) in [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt).
       │
       ▼
[AudioTrackDialog / SubtitleTrackDialog / SubtitleAppearanceDialog / DecodeModePicker]
       │
       ├─► 2. For Tracks: Renders list parsed by [TrackListParser.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/TrackListParser.kt) into [AudioTrack.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/AudioTrack.kt) and [SubtitleTrack.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/SubtitleTrack.kt).
       ├─► 3. For Decode Mode: Renders [DecodeModePicker.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialog/DecodeModePicker.kt) to select `HW`, `HW+`, or `SW` decoding.
       ├─► 4. User selects track ID or adjusts font scaling/position slider -> triggers callback to ViewModel.
       │
       ▼
[PlayerViewModel & SubtitleController]
       │
       ├─► 5. For Tracks: Calls `subtitleController.selectTrack(id)` or `executor.setAudioTrack(id)` -> sends property `"sid"` / `"aid"` to libmpv.
       ├─► 6. For Sideloading: Opens external file picker, calling `subtitleController.addSubtitleFile(uri)` -> sends command `"sub-add"`.
       ├─► 7. For Appearance: Calls `executor.setSubtitleAppearance(size, pos)` -> sets `"sub-scale"` and `"sub-pos"`.
       ├─► 8. For Decode Mode: Calls `cycleDecodeMode()` / `setDecodeMode()` -> immediately sets property `"hwdec"` on the engine executor and asynchronously persists the mode.
       ├─► 9. Persists styling, decode mode, and language preferences asynchronously to [UserPreferencesRepository.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/preferences/UserPreferencesRepository.kt) (Android DataStore).
```

---

## 4. Complete Component Catalog & Chain of Responsibility Matrix

The following matrix documents every file in the project, defining who controls it, whom it commands, and its core functions.

### A. Core Engine & Native Interface (`core/engine/`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[MpvController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt)** | **Engine Facade Leader.** Initializes libmpv, sets GPU context, configures engine options via `configurator`, and connects property observers. | `init()`, `destroy()`, `copyFontAsset()` | `PlayerViewModel` | `MpvCommandExecutor`, `MpvEventDispatcher`, `MpvOptionsConfigurator`, `MpvSurface`, `MPVLib` (JNI) |
| **[MpvCommandExecutor.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvCommandExecutor.kt)** | **Thread-Safe Engine Translator.** Queues commands on a single-thread executor; implements debounced seek gesturing and generation-aware surface detachment. | `execute()`, `nextSurfaceGeneration()`, `detachSurface()`, `seekGesture()`, `seekCommit()`, `loadFile()`, `setSpeed()` | `MpvController`, `PlayerViewModel`, `MpvSurface`, `GestureHandler` | `MPVLib.command()`, `MPVLib.setProperty*()` |
| **[MpvEventDispatcher.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvEventDispatcher.kt)** | **Native Event Router & Listener Contract.** Implements observer pattern over raw JNI signals from libmpv and broadcasts them to Kotlin listeners. Defines `MpvEventListener` interface. | `eventProperty()`, `event()`, `addListener()`, `removeListener()`, `onPropertyChange()` | `MpvController`, `MPVLib` (JNI Callback) | `MpvEventListener` implementations (`PlayerViewModel`) |
| **[MpvOptionsConfigurator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvOptionsConfigurator.kt)** | **Engine Configuration Leader.** Extracted option and property configurator managing pre-init engine settings (`GeneralOptions`, `SubtitleOptions`, `AudioOptions`), VO/GPU context configuration (`vo=gpu`, `hwdec=mediacodec`), and copying bundled font assets (`Roboto-Regular.ttf`). | `initOptions()`, `copyFontAssets()`, `setVoConfigured()` | `MpvController` | `MPVLib.setOptionString()`, Android `Context.assets`, `Environment` |
| **[MpvSurface.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvSurface.kt)** | **Video Render Target.** Manages binding/unbinding of Android `Surface` to mpv VO using generation counters to prevent race conditions. | `surfaceCreated()`, `surfaceDestroyed()`, `hasSurface()`, `setVo()` | `MpvController`, `PlayerActivity`, `PlayerVideo` | `MpvCommandExecutor.nextSurfaceGeneration()`, `detachSurface()` |
| **[MpvConstants.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvConstants.kt)** | **Property Token Definitions.** Central object containing literal string constants for libmpv property names and commands. | Constant properties (`PROP_PAUSE`, `PROP_TIME_POS`, `PROP_SPEED`, etc.) | Used globally across engine & viewmodel | None (Static definitions) |
| **[TrackListParser.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/TrackListParser.kt)** | **Metadata Translator.** Parses raw mpv `track-list` JSON/property strings into structured Kotlin `AudioTrack` and `SubtitleTrack` models. | `parseTrackList()` | `PlayerViewModel`, `SubtitleController` | `AudioTrack`, `SubtitleTrack` models |

### B. Player State & Business Logic (`player/viewmodel/`, `player/state/`, `player/model/`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt)** | **Overall State Leader.** Central ViewModel governing player UI state (`PlayerState`), media commands (with precise seeking by default via `seekTo(..., precise=true)`), responsive decoding mode cycling (`cycleDecodeMode()`) with immediate native execution and asynchronous DataStore persistence without eager state mutations, seek timestamp resets (`lastSeekTime = 0L` on commit), and coordinating sub-managers. | `play()`, `pause()`, `seekTo()`, `seekCommit()`, `onSeekCommitMs()`, `cycleDecodeMode()`, `onSurfaceReady()`, `onPropertyChange()` | `PlayerActivity`, UI Controls | `MpvController`, `PlaylistManager`, `SubtitleController`, `ResumePositionManager`, `UserPreferencesRepository` |
| **[PlayerViewModelFactory.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModelFactory.kt)** | **Dependency Injection Factory.** Instantiates `PlayerViewModel` with `MpvController`, preferences, and Room database DAOs. | `create()` | `PlayerActivity` | `AppDatabase`, `UserPreferencesRepository`, `MpvController` |
| **[PlaylistManager.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlaylistManager.kt)** | **Queue Orchestrator.** Manages playlist items, current index, file loading, and next/previous track switching upon EOF. | `loadAndPlay()`, `playNext()`, `playPrevious()`, `addToPlaylist()` | `PlayerViewModel` | `MpvCommandExecutor.loadFile()`, `PlaylistState` |
| **[SubtitleController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/SubtitleController.kt)** | **Subtitle Governor.** Handles subtitle track selection, external ASS/SRT file sideloading, and font scale/position styling properties. | `selectTrack()`, `addSubtitleFile()`, `setSubtitleSize()`, `setSubtitlePosition()` | `PlayerViewModel` | `MpvCommandExecutor`, `UserPreferencesRepository` |
| **[ResumePositionManager.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/ResumePositionManager.kt)** | **Progress Guardian.** Automatically debounces and saves watch progress on pause/stop and restores timestamps on file reopen. | `savePosition()`, `getResumePosition()`, `clearPosition()` | `PlayerViewModel` | `ResumePositionDao` (Room Database) |
| **[PlayerState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/PlayerState.kt)** | **Immutable UI State.** Data class representing current playback state (time, duration, pause, speed, volume, tracks, loading). | `copy()` | Emitted by `PlayerViewModel` | Collected by `PlayerScreen`, `PlayerOverlay` |
| **[PlaylistState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/PlaylistState.kt)** | **Playlist State Model.** Holds list of media URIs and current active index. | `copy()` | Emitted by `PlaylistManager` | Collected by UI overlays |
| **[SubtitleAppearanceState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/SubtitleAppearanceState.kt)** | **Styling State Model.** Holds current font scale, position, color, and background styling values for subtitles. | `copy()` | Emitted by `SubtitleController` | Collected by `SubtitleAppearanceDialog` |
| **[AudioTrack.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/AudioTrack.kt) & [SubtitleTrack.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/SubtitleTrack.kt)** | **Track Domain Models.** Structured data models representing track ID, title, language, external flag, and selection state. | Data properties (`id`, `name`, `lang`, `isSelected`) | Created by `TrackListParser` | Used by dialogs and `PlayerViewModel` |
| **[DecodeMode.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/DecodeMode.kt) & [AspectRatioMode.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/AspectRatioMode.kt)** | **Playback Setting Enums.** Defines hardware decoding modes (`HW`, `HW+`, `SW`) and video aspect ratios (`FIT`, `STRETCH`, `CROP`, `16:9`, `4:3`). | Enum values | Used by `PlayerQuickActions` & `DecodeModePicker` | Routed to `MpvCommandExecutor` |

### C. Touch Gestures & State Machine (`player/gesture/`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt)** | **Touch Input Leader.** Intercepts Compose pointer events, implements `MpvPlayerController`, and renders visual feedback indicators. | `GestureHandler(...)`, `pointerInput(stateMachine)` | `PlayerOverlay` | `MpvGestureStateMachine`, `MpvCommandExecutor`, OS `AudioManager` / Window attributes |
| **[MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt)** | **Core State Machine.** Classifies multi-touch sequences into mutually exclusive `GestureState` objects with zero ambiguity. | `onPointerDown()`, `onPointerMove()`, `onPointerUp()`, `transitionTo()` | `GestureHandler` | `MpvPlayerController` interface |
| **[GestureModels.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureModels.kt)** | **Gesture Domain Contract.** Defines `MpvPlayerController` interface, `TapRegion`, `PanelShown`, and sealed classes for all mutually exclusive gesture states (`TapCandidate`, `MultiTapSeeking`, `LongPress`, `DynamicSpeedScrub`, `VerticalSwipe`, `PinchZoomPan`, `SinglePan`, `HorizontalSeek`). | Interface methods, sealed class hierarchies | Used by `MpvGestureStateMachine` & `GestureHandler` | None (Domain model definitions) |
| **[GestureIndicators.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureIndicators.kt)** | **Consolidated Indicator Library.** Renders all visual feedback overlays during gesture scrubbing: `VolumeIndicator`, `BrightnessIndicator`, `HorizontalSeekIndicator`, `SeekCircleIndicator`, `SpeedIndicator`, `PinchZoomIndicator`, and glassmorphic `IndicatorPill`. | `VolumeIndicator(...)`, `BrightnessIndicator(...)`, `HorizontalSeekIndicator(...)`, `SeekCircleIndicator(...)`, `SpeedIndicator(...)`, `PinchZoomIndicator(...)`, `IndicatorPill(...)` | `GestureHandler` | Compose Foundation, Material 3 icons & animations |

### D. UI Presentation, Controls & Dialogs (`player/playback/`, `player/controls/`, `player/dialogs/`, `player/dialog/`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt)** | **UI Root Leader.** Root Compose layout stacking video rendering surface, gesture detector, and interactive overlay controls. | `PlayerScreen(...)` | `PlayerActivity` | `PlayerVideo`, `PlayerOverlay` |
| **[PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)** | **Overlay Coordinator.** Stacks top bar, bottom controls, quick actions, gestures, dialogs, manages 3-second auto-hide timers, and synchronizes slider preview state (`gestureSeekPreviewMs`) with gesture scrubbing. | `PlayerOverlay(...)` | `PlayerScreen` | `GestureHandler`, `PlayerTopBar`, `PlayerBottomControls`, `PlayerQuickActions`, Dialogs |
| **[PlayerVideo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerVideo.kt)** | **Surface Container.** AndroidView wrapper embedding Android `SurfaceView` into Compose and binding its lifecycle to `MpvSurface`. | `PlayerVideo(...)` | `PlayerScreen` | `MpvSurface.surfaceCreated()`, `surfaceDestroyed()` |
| **[PlayerBottomControls.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerBottomControls.kt)** | **Primary Control Bar.** Renders Play/Pause, interactive seekbar slider with drag scrubbing preview (`onSeekPreviewMs`, `onSeekGesture`), local drag priority, and `0L` initialization guards against accidental seeks during fast taps, time text (`HH:MM:SS`), and dialog triggers. | `PlayerBottomControls(...)` | `PlayerOverlay` | Lambda callbacks (`onSeekPreviewMs`, `onSeekGesture`) -> `PlayerViewModel`, `TimeFormatter`, `PlayerControlsStyles` |
| **[PlayerQuickActions.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerQuickActions.kt)** | **Auxiliary Action Bar.** Renders aspect ratio toggle, hardware decode switcher (`HW`/`HW+`/`SW`), speed selector, and control lock button. | `PlayerQuickActions(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerViewModel`, `PlayerControlsStyles` |
| **[PlayerTopBar.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerTopBar.kt)** | **Header Bar.** Renders back navigation arrow, video file title, audio/subtitle selectors, and settings overflow menu. | `PlayerTopBar(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerActivity` / Dialog flags, `PlayerControlsStyles` |
| **[PlayerControlsStyles.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerControlsStyles.kt)** | **Control Styling Tokens.** Defines common UI dimensions, alpha values, glassmorphic background modifiers (`controlBarBackground`), and reusable styled icon buttons (`PlayerIconButton`) for player overlays. | `PlayerIconButton(...)`, `Modifier.controlBarBackground(...)`, styling constants | Used by `PlayerTopBar`, `PlayerBottomControls`, `PlayerQuickActions` | Compose Foundation, Material 3 |
| **[AudioTrackDialog.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/AudioTrackDialog.kt)** | **Audio Track Modal.** Dialog listing available audio streams for selection or disabling audio. | `AudioTrackDialog(...)` | `PlayerOverlay` | `PlayerViewModel.setAudioTrack()` |
| **[SubtitleTrackDialog.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/SubtitleTrackDialog.kt)** | **Subtitle Track Modal.** Dialog listing embedded subtitle tracks and providing an action button to load external ASS/SRT files. | `SubtitleTrackDialog(...)` | `PlayerOverlay` | `PlayerViewModel.setSubtitleTrack()`, `addSubtitleFile()` |
| **[SubtitleAppearanceDialog.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialog/SubtitleAppearanceDialog.kt)** | **Subtitle Styling Modal.** Dialog with sliders for adjusting subtitle font scaling (`sub-scale`) and vertical screen position (`sub-pos`). | `SubtitleAppearanceDialog(...)` | `PlayerOverlay` | `SubtitleController.setSubtitleSize()`, `setSubtitlePosition()` |
| **[DecodeModePicker.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialog/DecodeModePicker.kt)** | **Hardware Decode Modal.** Dialog presenting selectable cards for hardware decoding modes (`HW`, `HW+`, `SW`) with clear subtitles describing MediaCodec behavior. | `DecodeModePicker(...)` | `PlayerOverlay` | `PlayerViewModel.setDecodeMode()`, `DecodeMode` |

### E. Storage, Preferences, Settings & Home UI (`core/database/`, `core/preferences/`, `settings/`, `home/ui/`, `util/`, `ui/theme/`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[AppDatabase.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/AppDatabase.kt)** | **Database Leader.** Room SQLite database definition and DAO provider for local persistence. | `resumePositionDao()` | `PlayerViewModelFactory` | `ResumePositionDao`, `ResumePositionEntity` |
| **[ResumePositionDao.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/ResumePositionDao.kt) & [ResumePositionEntity.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/ResumePositionEntity.kt)** | **Playback Progress Storage.** Room DAO and SQLite table entity storing file path, playback timestamp in milliseconds, and duration. | `savePosition()`, `getPosition()`, `deletePosition()` | `ResumePositionManager` | SQLite database engine |
| **[UserPreferencesRepository.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/preferences/UserPreferencesRepository.kt)** | **Preferences Leader.** Android DataStore repository managing all persistent user choices, including hardware decode mode (`HW`, `HW+`, `SW`) and default subtitle styling. | `updateSubtitleSize()`, `updateDecodeMode()`, preference flows | `PlayerViewModel`, `SubtitleController`, `SettingsViewModel` | Android DataStore |
| **[HomeScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/home/ui/HomeScreen.kt)** | **Home Landing UI.** Initial dashboard allowing users to pick local video files via system file picker or stream network URLs. | `HomeScreen(...)` | `MainActivity` | Launches `PlayerActivity` with Media URI |
| **[SettingsScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/SettingsScreen.kt) & [SettingsViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/SettingsViewModel.kt)** | **Global Settings Hub.** Application settings screen and ViewModel managing default player configurations and subtitle appearance. | `SettingsScreen(...)`, `update*()` | `MainActivity` / Navigation | `UserPreferencesRepository`, `AboutSection`, `SubtitleAppearanceSection` |
| **[AboutSection.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/AboutSection.kt) & [SubtitleAppearanceSection.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/SubtitleAppearanceSection.kt)** | **Settings Sub-Sections.** Modular Compose settings layouts for displaying app version info and configuring default subtitle styling. | `AboutSection(...)`, `SubtitleAppearanceSection(...)` | `SettingsScreen` | `SettingsViewModel` |
| **[UriResolver.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/util/UriResolver.kt) & [TimeFormatter.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/util/TimeFormatter.kt)** | **System Utilities.** Helpers resolving Android Content URIs to file paths/descriptors and formatting milliseconds into `HH:MM:SS` strings. | `resolveUri()`, `formatTime()` | `PlayerActivity`, `PlayerBottomControls` | Android ContentResolver |
| **[Theme.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/ui/theme/Theme.kt), [Color.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/ui/theme/Color.kt) & [Type.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/ui/theme/Type.kt)** | **Design System Tokens.** Material Design 3 color palettes, typography scales, and theme wrapper for Potato Player MPV. | `MpvPlayerTheme(...)` | `MainActivity`, `PlayerActivity` | Material 3 Compose |

### F. Verification & Unit Test Suites (`test/...`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[MpvGestureStateMachineTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachineTest.kt) & [GestureStateCoverageTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/gesture/GestureStateCoverageTest.kt)** | **Touch Engine Verification.** Verifies multi-touch classification, volume/brightness edge scrubbing, double-tap seek jumps, pinch zoom, and ensures 100% mutually exclusive state transition coverage. | `@Test` methods (`tapToSeekTransition`, `volumeScrubbing`, etc.) | JUnit 4 / Gradle Test Runner | `MpvGestureStateMachine`, `MpvPlayerController` mock |
| **[DecodeModeTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/model/DecodeModeTest.kt)** | **Hardware Decoding Verification.** Verifies hardware decode enum definitions (`HW`, `HW+`, `SW`) and validates exact mpv engine string mappings (`mediacodec`, `mediacodec-copy`, `no`). | `@Test` methods (`verifyMpvValues`) | JUnit 4 / Gradle Test Runner | `DecodeMode` |
| **[PlayerViewModelPropertyChangeTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModelPropertyChangeTest.kt)** | **Reactive State & Event Verification.** Verifies that native libmpv property events (`time-pos`, `duration`, `pause`, `eof-reached`) correctly update immutable `PlayerState` and trigger playlist EOF transitions. | `@Test` methods (`onPropertyChange_pause`, `eofReached_triggersNext`) | JUnit 4 / Gradle Test Runner | `PlayerViewModel`, `MpvController`, `PlaylistManager` |

---

## 5. Leader & Subsystem Deep-Dive

### A. The Brain: `PlayerViewModel` & Its Triad of Managers
Why is [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt) not just a single monolithic file? Because robust video playback involves three distinct, complex sub-domains:
1. **Playlist Management (`PlaylistManager`)**: Handles queueing multiple videos, resolving file paths, and managing end-of-file (EOF) transitions. When mpv reports `eof-reached`, `PlayerViewModel` notifies `PlaylistManager`, which automatically loads the next URI in the queue.
2. **Subtitle & Styling (`SubtitleController`)**: mpv requires specific property formatting for ASS (Advanced SubStation Alpha) overrides. This controller insulates the ViewModel from string-parsing logic, managing subtitle selection, external file sideloading (`sub-add`), and font scaling.
3. **Resume Progress (`ResumePositionManager`)**: Intercepts `time-pos` updates from mpv. To prevent flooding SQLite with writes on every video frame, it debounces time updates and commits the current playback timestamp to `ResumePositionDao` when playback pauses, stops, or changes files.

*Note on Precise Seeking, Decoder Switching & State Synchronization*: `PlayerViewModel` enforces precise seeking by default (`precise = true` in `seekTo`) and resets `lastSeekTime = 0L` upon seek commit (`seekCommit` / `onSeekCommitMs`) to prevent unnecessary recompositions after scrubbing ends. Furthermore, when switching hardware decode modes via `cycleDecodeMode()`, it immediately executes `controller.executor.setHwdec(mpvMode)` on the engine executor for responsive switching without artificial delay wrappers, while saving the preference asynchronously via `preferencesRepository.setDecodeMode(mpvMode)` in a dedicated `viewModelScope.launch` block. State updates are delegated entirely to the native mpv observer (`PROP_HWDEC`) rather than eagerly mutating state, eliminating race conditions and UI flickering.

### B. The Engine Gateway: `MpvController`, `MpvOptionsConfigurator`, & Generation-Aware Surface Safety
[MpvController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt) encapsulates the C-JNI boundary into modular, decoupled helpers:
* **`MpvOptionsConfigurator`**: Extracted standalone configurator responsible for copying bundled font assets (`Roboto-Regular.ttf`) and applying pre-init engine settings (`GeneralOptions`, `SubtitleOptions`, `AudioOptions`, VO/GPU context). This ensures `MpvController` remains focused on lifecycle and event coordination without cluttering initialization code with option strings.
* **`MpvCommandExecutor`**: Prevents concurrent modification exceptions and JNI threading crashes by dispatching all commands (`seek`, `pause`, `loadfile`) through an internal single-thread executor queue (`"mpv-engine-thread"`). Crucially, it implements **generation-aware surface handling**: an `AtomicInteger` tracks surface generation so that if a stale detach task executes during rapid Picture-in-Picture or window focus transitions, it recognises the generation mismatch and silently drops the detach, preventing crashes! It also implements **debounced seek gesturing** via `AtomicReference<Double?> pendingSeek`, ensuring rapid drag scrubbing never floods the JNI command queue.
* **`MpvEventDispatcher`**: Implements the Observer pattern over native JNI callbacks. When C++ `libmpv` fires an event (e.g., `MPV_EVENT_PROPERTY_CHANGE`), the dispatcher converts raw C-pointers into safe Kotlin strings and types (`Boolean`, `Long`, `Float`), notifying `PlayerViewModel`.
* **`MpvSurface`**: Manages the Android `SurfaceView` lifecycle. When destroyed, it immediately sets `vo="null"` and `force-window="no"` before detaching to stop mpv rendering cleanly and avoid race conditions.

### C. The Input Engine: `GestureHandler`, `GestureModels`, & Single-Ownership State Machine
Instead of cluttering Compose UI code with complex multi-touch math, [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt) delegates to a dedicated single-ownership state machine, [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt), operating on data structures defined in [GestureModels.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureModels.kt). This architecture ensures **zero touch ambiguity** by enforcing that exactly ONE state is active at any moment:
* **`Idle` & `TapCandidate`**: Captures initial touch down, evaluating single-tap (toggle controls) versus double-tap versus drag thresholds.
* **`MultiTapSeeking`**: Activated on double-tap on left/right screen edges. Calculates cumulative seek jumps (e.g., $\pm 10\text{s}$, $\pm 20\text{s}$) and commands `SeekCircleIndicator` ripple animations.
* **`LongPress` & `DynamicSpeedScrub`**: Activated on long-press or top-edge dragging, temporarily engaging $2.0\times$ speed boost or interactive playback rate scrubbing while displaying `SpeedIndicator`.
* **`VerticalSwipe`**: Activated on vertical drag delta $>10\text{px}$ along screen edges. Left edge modulates window brightness; right edge modulates OS audio stream volume, animating `BrightnessIndicator` and `VolumeIndicator`.
* **`HorizontalSeek`**: Activated on horizontal drag across the center viewport. Calculates time offset based on video duration and shows `HorizontalSeekIndicator`, using debounced seek previewing until finger release commits exact positioning.
* **`PinchZoomPan` & `SinglePan`**: Activated when two fingers pinch or pan, dynamically scaling video zoom and xy-translation properties in `libmpv` while displaying `PinchZoomIndicator`.

All visual indicator overlays are consolidated within [GestureIndicators.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureIndicators.kt), which uses a shared glassmorphic container (`IndicatorPill`) for clean, consistent UI presentation.

*Note on Interactive Controls & Slider Preview Sync*: In [PlayerBottomControls.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerBottomControls.kt), seek bar drag scrubbing (`dragPositionMs`) is initialized to `0L` (rather than current playback position) to prevent stale composition snapshots, and is guarded by `if (isDragging)` on value change finished so rapid taps never trigger accidental jump seeks to `0L`. During scrubbing, [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt) coordinates `gestureSeekPreviewMs` across both the bottom control bar and screen gesture indicators, ensuring seamless and unified visual feedback.

---

## 6. Summary of Architecture Harmony

The codebase achieves state-of-the-art performance, stability, and maintainability through a strict chain of responsibility:
* **UI & Gesture Components** never call mpv JNI functions directly; they only emit user intents to `PlayerViewModel` or commands via `MpvPlayerController`.
* **ViewModel & Business Logic** never manage Android Views, canvases, or raw touch math; they only update immutable `StateFlow<PlayerState>` and coordinate domain managers.
* **Engine Components** never know about UI layouts or databases; they only execute thread-safe, generation-aware commands and emit raw playback events.

This structured chaining ensures that **Potato Player MPV** remains responsive, modular, and robust against race conditions across all Android form factors.

