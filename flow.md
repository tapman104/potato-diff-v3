# Potato Player MPV — Comprehensive Codebase Flow & System Architecture Report

This report provides an in-depth, architectural breakdown of the **Potato Player MPV** codebase. It illustrates how every component interacts, who commands whom, how execution flows through the system, and identifies the exact leader files, their roles, and their key functions across all subsystems.

---

## 1. Executive Summary: The Leaders of the Codebase

In a modern reactive Android application built with **Jetpack Compose**, **MVVM**, and a native **JNI engine (`libmpv`)**, clear separation of concerns is vital. The system is governed by five distinct **Leader Components**, each ruling its own domain:

```
+----------------------------------------------------------------------------------------------------+
|                                      THE CODEBASE LEADERS                                          |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  1. STATE & BUSINESS ORCHESTRATION LEADER                                                          |
|     File: [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt)                                           |
|     Role: The supreme brain of the player. Commands playback, manages UI state (`PlayerState`),    |
|           coordinates playlists, subtitles, audio tracks, and saves/restores watch progress.       |
|           Guards state updates against redundant recompositions via equality checks.               |
|                                                                                                    |
|  2. NATIVE MEDIA ENGINE LEADERS                                                                    |
|     Files: [MpvController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt) & [MpvOptionsConfigurator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvOptionsConfigurator.kt)               |
|     Role: The bridge between Kotlin and native C/C++ `libmpv`. Commands surface binding with       |
|           generation-aware safety, configures engine options/VO, and dispatches native events back.|
|                                                                                                    |
|  3. TOUCH, GESTURE & OVERLAY LEADERS                                                               |
|     Files: [PlayerCoordinator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/PlayerCoordinator.kt), [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt) & [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt) |
|     Role: The input governor and overlay router. Captures multi-touch gestures, classifies pointer   |
|           events through a single-ownership state machine, modifies OS volume/brightness, and      |
|           routes visual overlays through [OverlayController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/OverlayController.kt).                       |
|                                                                                                    |
|  4. UI PRESENTATION LEADERS                                                                        |
|     Files: [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt) & [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)                         |
|     Role: The Compose visual root. Renders the video output surface, overlays interactive controls,|
|           dialogs, and routes user clicks, gestures, and settings navigation flows.                |
|                                                                                                    |
|  5. SETTINGS & DIALOG NAVIGATION LEADERS                                                           |
|     Files: [MoreOptionsSheet.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/MoreOptionsSheet.kt) & [SettingsScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/SettingsScreen.kt)                                     |
|     Role: Controls playback speed chips, file metadata inspection ([FileInfo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/FileInfo.kt)), and |
|           global application preferences including hardware decoding and ASS subtitle styling.     |
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
                 |                     |                |                |          [PlayerCoordinator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/PlayerCoordinator.kt)
                 |                     +----------------+----------------+      (Implements MpvPlayerController)
                 |                                      |                              |       |
                 |                              (User Commands)                        |       v
                 |                                      |                              | [OverlayController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/OverlayController.kt)
                 |                                      v                              |       |
                 |                             [MoreOptionsSheet.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/MoreOptionsSheet.kt)                 |       v
                 |                        (Playback Speed, FileInfo, Settings)         v [GestureIndicators.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureIndicators.kt)
                 v                                      |                              |
========================================================|==============================|==============
                              LAYER 3: STATE & BUSINESS LOGIC LAYER (ViewModel)        |
========================================================|==============================|==============
                                              [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt) <------------------+
                                     (Equality-Guarded State Flow Updates)
                                                        |
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

To understand how files work with each other, we trace the exact **chain of execution** across five primary runtime scenarios.

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

### Pipeline B: User Playback Control & Equality-Guarded State Loop
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
       ├─► 7. Equality Guard Check: Verifies `_playerState.value.isPaused != value` before mutating state!
       │      If value changed: `_playerState.update { it.copy(isPaused = value) }`.
       │
       ▼
[Jetpack Compose UI (PlayerScreen / PlayerBottomControls)]
       │
       └─► 8. Collects new [PlayerState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/PlayerState.kt) via `collectAsStateWithLifecycle()`, recomposes icon seamlessly!
```

---

### Pipeline C: Touch Gesture & Overlay Routing via PlayerCoordinator
When a user gestures on the playback screen, input is intercepted by [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt) and governed by [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt), routed through [PlayerCoordinator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/PlayerCoordinator.kt):

```
[User Swipes, Taps, or Pinches on Playback Viewport]
       │
       ├─► 1. Raw pointer events (`pointerInput`) captured by [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt).
       ├─► 2. Events fed into [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt).
       │
       ▼
[MpvGestureStateMachine] (Single-Ownership Mutually Exclusive Classification)
       │
       ├─► 3. Evaluates active pointers against current `GestureState` (`TapCandidate`, `MultiTapSeeking`, `LongPress`, `DynamicSpeedScrub`, `VerticalSwipe`, `HorizontalSeek`, `PinchZoomPan`).
       │
       ▼
[PlayerCoordinator implementation of MpvPlayerController]
       │
       ├─► 4. Playback Commands: Calls `viewModel.seekTo()`, `viewModel.setSpeed()`, `viewModel.setVolume()`, or modifies OS brightness.
       ├─► 5. Overlay Routing: Calls `overlay.showBrightnessOverlay()`, `overlay.showVolumeOverlay()`, `overlay.showSpeedOverlay()`, or `overlay.showHorizontalSeekOverlay()` via [OverlayController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/OverlayController.kt).
       │
       ▼
[Modular Visual Feedback Indicators in GestureIndicators.kt & PlayerOverlay]
       │
       └─► 6. Appropriate overlay composable from [GestureIndicators.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureIndicators.kt) animates in real-time on screen (`VolumeIndicator`, `BrightnessIndicator`, `HorizontalSeekIndicator`, `SpeedIndicator`, `PinchZoomIndicator`).
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
       │
       ▼
[PlayerViewModel & SubtitleController]
       │
       ├─► 4. For Tracks: Calls `subtitleController.selectTrack(id)` or `executor.setAudioTrack(id)`.
       ├─► 5. For Sideloading: Opens external file picker, calling `subtitleController.addSubtitleFile(uri)` -> sends `"sub-add"`.
       ├─► 6. For Appearance: Calls `executor.setSubtitleAppearance(size, pos)` -> sets `"sub-scale"` and `"sub-pos"`.
       ├─► 7. For Decode Mode: Calls `cycleDecodeMode()` / `setDecodeMode()` -> sets property `"hwdec"` on engine and persists to [UserPreferencesRepository.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/preferences/UserPreferencesRepository.kt).
```

---

### Pipeline E: More Options Sheet & App Settings Navigation Flow
When a user clicks the 3-dot overflow button in [PlayerQuickActions.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerQuickActions.kt):

```
[User Taps 3-Dot More Options Icon in PlayerQuickActions]
       │
       ├─► 1. Sets `showMoreOptionsSheet = true` in [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt).
       │
       ▼
[MoreOptionsSheet.kt]
       │
       ├─► 2. Renders playback speed selector (`LazyRow` of speed chips: 0.5x, 1.0x, 1.25x, 1.5x, 2.0x).
       ├─► 3. Renders expandable File Info card showing [FileInfo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/FileInfo.kt) metadata (File Path, Duration, Video/Audio/Subtitle Track counts).
       ├─► 4. Renders "App Settings" button triggering `onOpenSettings()`.
       │
       ▼
[PlayerActivity.onOpenSettings Callback]
       │
       └─► 5. Routes navigation out of overlay to launch [SettingsScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/SettingsScreen.kt) governed by [SettingsViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/SettingsViewModel.kt).
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
| **[PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt)** | **Overall State Leader.** Central ViewModel governing player UI state (`PlayerState`), media commands (with precise seeking by default via `seekTo(..., precise=true)`), responsive decoding mode cycling (`cycleDecodeMode()`), equality-guarded property updates (`onPropertyChange`), seek timestamp resets (`lastSeekTime = 0L` on commit), and coordinating sub-managers. | `play()`, `pause()`, `seekTo()`, `seekCommit()`, `onSeekCommitMs()`, `cycleDecodeMode()`, `onSurfaceReady()`, `onPropertyChange()` | `PlayerActivity`, UI Controls, `PlayerCoordinator` | `MpvController`, `PlaylistManager`, `SubtitleController`, `ResumePositionManager`, `UserPreferencesRepository` |
| **[PlayerViewModelFactory.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModelFactory.kt)** | **Dependency Injection Factory.** Instantiates `PlayerViewModel` with `MpvController`, preferences, and Room database DAOs. | `create()` | `PlayerActivity` | `AppDatabase`, `UserPreferencesRepository`, `MpvController` |
| **[PlaylistManager.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlaylistManager.kt)** | **Queue Orchestrator.** Manages playlist items, current index, file loading, and next/previous track switching upon EOF. | `loadAndPlay()`, `playNext()`, `playPrevious()`, `addToPlaylist()` | `PlayerViewModel` | `MpvCommandExecutor.loadFile()`, `PlaylistState` |
| **[SubtitleController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/SubtitleController.kt)** | **Subtitle Governor.** Handles subtitle track selection, external ASS/SRT file sideloading, and font scale/position styling properties. | `selectTrack()`, `addSubtitleFile()`, `setSubtitleSize()`, `setSubtitlePosition()` | `PlayerViewModel` | `MpvCommandExecutor`, `UserPreferencesRepository` |
| **[ResumePositionManager.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/ResumePositionManager.kt)** | **Progress Guardian.** Automatically debounces and saves watch progress on pause/stop and restores timestamps on file reopen. | `savePosition()`, `getResumePosition()`, `clearPosition()` | `PlayerViewModel` | `ResumePositionDao` (Room Database) |
| **[PlayerState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/PlayerState.kt)** | **Immutable UI State.** Data class representing current playback state (time, duration, pause, speed, volume, tracks, loading). | `copy()` | Emitted by `PlayerViewModel` | Collected by `PlayerScreen`, `PlayerOverlay` |
| **[PlaylistState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/PlaylistState.kt)** | **Playlist State Model.** Holds list of media URIs and current active index. | `copy()` | Emitted by `PlaylistManager` | Collected by UI overlays |
| **[SubtitleAppearanceState.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/state/SubtitleAppearanceState.kt)** | **Styling State Model.** Holds current font scale, position, color, and background styling values for subtitles. | `copy()` | Emitted by `SubtitleController` | Collected by `SubtitleAppearanceDialog` |
| **[AudioTrack.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/AudioTrack.kt) & [SubtitleTrack.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/SubtitleTrack.kt)** | **Track Domain Models.** Structured data models representing track ID, title, language, external flag, and selection state. | Data properties (`id`, `name`, `lang`, `isSelected`) | Created by `TrackListParser` | Used by dialogs and `PlayerViewModel` |
| **[DecodeMode.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/DecodeMode.kt) & [AspectRatioMode.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/AspectRatioMode.kt)** | **Playback Setting Enums.** Defines hardware decoding modes (`HW`, `HW+`, `SW`) and video aspect ratios (`FIT`, `STRETCH`, `CROP`, `16:9`, `4:3`). | Enum values | Used by `PlayerQuickActions` & `DecodeModePicker` | Routed to `MpvCommandExecutor` |
| **[FileInfo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/FileInfo.kt)** | **Media Metadata Model.** Immutable data class encapsulating video filename, absolute file path, duration in milliseconds, and track counts. | Data properties (`fileName`, `filePath`, `durationMs`, `videoTracks`, `audioTracks`, `subtitleTracks`) | Created by `PlayerViewModel` | Used by `MoreOptionsSheet` |

### C. Touch Gestures & Coordination (`player/gesture/`, `player/coordinator/`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[PlayerCoordinator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/PlayerCoordinator.kt)** | **Playback & Overlay Bridge.** Implements `MpvPlayerController`, bridging `MpvGestureStateMachine` with `PlayerViewModel` and routing visual overlays through `OverlayController`. | `attachOverlay()`, `seekTo()`, `setPlaybackSpeedRamped()`, `setBrightness()`, overlay show/hide implementations | `PlayerActivity`, `GestureHandler` | `PlayerViewModel`, `OverlayController` |
| **[OverlayController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/OverlayController.kt)** | **Overlay Routing Contract.** Interface and companion `NO_OP` implementation defining commands for showing/hiding volume, brightness, speed, seek, and pinch overlays. | `showVolumeOverlay()`, `showBrightnessOverlay()`, `showSpeedOverlay()`, `scheduleTimer()` | `PlayerCoordinator` | Implemented by `PlayerOverlay` |
| **[GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt)** | **Touch Input Leader.** Intercepts Compose pointer events, feeds them into `MpvGestureStateMachine`, and renders visual feedback indicators. | `GestureHandler(...)`, `pointerInput(stateMachine)` | `PlayerOverlay` | `MpvGestureStateMachine`, `MpvPlayerController` (`PlayerCoordinator`) |
| **[MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt)** | **Core State Machine.** Classifies multi-touch sequences into mutually exclusive `GestureState` objects with zero ambiguity. | `onPointerDown()`, `onPointerMove()`, `onPointerUp()`, `transitionTo()` | `GestureHandler` | `MpvPlayerController` interface |
| **[GestureModels.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureModels.kt)** | **Gesture Domain Contract.** Defines `MpvPlayerController` interface, `TapRegion`, `PanelShown`, and sealed classes for all mutually exclusive gesture states. | Interface methods, sealed class hierarchies | Used by `MpvGestureStateMachine` & `GestureHandler` | None (Domain model definitions) |
| **[GestureIndicators.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureIndicators.kt)** | **Consolidated Indicator Library.** Renders all visual feedback overlays during gesture scrubbing (`VolumeIndicator`, `BrightnessIndicator`, `HorizontalSeekIndicator`, `SeekCircleIndicator`, `SpeedIndicator`, `PinchZoomIndicator`, `IndicatorPill`). | `VolumeIndicator(...)`, `BrightnessIndicator(...)`, `HorizontalSeekIndicator(...)`, `SeekCircleIndicator(...)`, `SpeedIndicator(...)`, `PinchZoomIndicator(...)`, `IndicatorPill(...)` | `GestureHandler` | Compose Foundation, Material 3 icons & animations |

### D. UI Presentation, Controls & Dialogs (`player/playback/`, `player/controls/`, `player/dialogs/`, `player/dialog/`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt)** | **UI Root Leader.** Root Compose layout stacking video rendering surface, gesture detector, and interactive overlay controls, wiring `onOpenSettings`. | `PlayerScreen(...)` | `PlayerActivity` | `PlayerVideo`, `PlayerOverlay` |
| **[PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)** | **Overlay Coordinator.** Stacks top bar, bottom controls, quick actions, gestures, dialogs (including `MoreOptionsSheet`), manages 3-second auto-hide timers, and synchronizes slider preview state. | `PlayerOverlay(...)` | `PlayerScreen` | `GestureHandler`, `PlayerTopBar`, `PlayerBottomControls`, `PlayerQuickActions`, `MoreOptionsSheet`, Dialogs |
| **[PlayerVideo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerVideo.kt)** | **Surface Container.** AndroidView wrapper embedding Android `SurfaceView` into Compose and binding its lifecycle to `MpvSurface`. | `PlayerVideo(...)` | `PlayerScreen` | `MpvSurface.surfaceCreated()`, `surfaceDestroyed()` |
| **[PlayerBottomControls.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerBottomControls.kt)** | **Primary Control Bar.** Renders Play/Pause, interactive seekbar slider with drag scrubbing preview (`onSeekPreviewMs`, `onSeekGesture`), local drag priority, and time text (`HH:MM:SS`). | `PlayerBottomControls(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerViewModel`, `TimeFormatter`, `PlayerControlsStyles` |
| **[PlayerQuickActions.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerQuickActions.kt)** | **Auxiliary Action Bar.** Renders aspect ratio toggle, hardware decode switcher (`HW`/`HW+`/`SW`), and 3-dot More Options overflow button. | `PlayerQuickActions(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerViewModel`, `PlayerControlsStyles` |
| **[PlayerTopBar.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerTopBar.kt)** | **Header Bar.** Renders back navigation arrow, video file title, and audio/subtitle selectors. | `PlayerTopBar(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerActivity` / Dialog flags, `PlayerControlsStyles` |
| **[PlayerControlsStyles.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerControlsStyles.kt)** | **Control Styling Tokens.** Defines common UI dimensions, alpha values, glassmorphic background modifiers (`controlBarBackground`), and reusable styled icon buttons (`PlayerIconButton`). | `PlayerIconButton(...)`, `Modifier.controlBarBackground(...)`, styling constants | Used by `PlayerTopBar`, `PlayerBottomControls`, `PlayerQuickActions` | Compose Foundation, Material 3 |
| **[AudioTrackDialog.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/AudioTrackDialog.kt)** | **Audio Track Modal.** Dialog listing available audio streams for selection or disabling audio. | `AudioTrackDialog(...)` | `PlayerOverlay` | `PlayerViewModel.setAudioTrack()` |
| **[SubtitleTrackDialog.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/SubtitleTrackDialog.kt)** | **Subtitle Track Modal.** Dialog listing embedded subtitle tracks and providing an action button to load external ASS/SRT files. | `SubtitleTrackDialog(...)` | `PlayerOverlay` | `PlayerViewModel.setSubtitleTrack()`, `addSubtitleFile()` |
| **[MoreOptionsSheet.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/MoreOptionsSheet.kt)** | **More Options Modal Sheet.** Side sheet providing playback speed selection chips, expandable media `FileInfo` inspection, and navigation to App Settings. | `MoreOptionsSheet(...)` | `PlayerOverlay` | `PlayerViewModel.setSpeed()`, `FileInfo`, `onOpenSettings()` |
| **[SubtitleAppearanceDialog.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialog/SubtitleAppearanceDialog.kt)** | **Subtitle Styling Modal.** Dialog with sliders for adjusting subtitle font scaling (`sub-scale`) and vertical screen position (`sub-pos`). | `SubtitleAppearanceDialog(...)` | `PlayerOverlay` | `SubtitleController.setSubtitleSize()`, `setSubtitlePosition()` |
| **[DecodeModePicker.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialog/DecodeModePicker.kt)** | **Hardware Decode Modal.** Dialog presenting selectable cards for hardware decoding modes (`HW`, `HW+`, `SW`). | `DecodeModePicker(...)` | `PlayerOverlay` | `PlayerViewModel.setDecodeMode()`, `DecodeMode` |

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
| **[Theme.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/ui/theme/Theme.kt), [Color.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/ui/theme/Color.kt) & [Type.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/ui/theme/Type.kt)** | **Design System Tokens.** Material Design 3 color palettes, typography scales, and theme wrapper for Potato Player MPV. | `MpvPlayerTheme(...)` | `MainActivity`, `PlayerActivity` | Material 3 Compose |

### F. Verification & Unit Test Suites (`test/...`)
| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By | Commands / Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **[MpvGestureStateMachineTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachineTest.kt) & [GestureStateCoverageTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/gesture/GestureStateCoverageTest.kt)** | **Touch Engine Verification.** Verifies multi-touch classification, volume/brightness edge scrubbing, double-tap seek jumps, pinch zoom, and ensures 100% mutually exclusive state transition coverage. | `@Test` methods (`tapToSeekTransition`, `volumeScrubbing`, etc.) | JUnit 4 / Gradle Test Runner | `MpvGestureStateMachine`, `MpvPlayerController` mock |
| **[DecodeModeTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/model/DecodeModeTest.kt)** | **Hardware Decoding Verification.** Verifies hardware decode enum definitions (`HW`, `HW+`, `SW`) and validates exact mpv engine string mappings (`mediacodec`, `mediacodec-copy`, `no`). | `@Test` methods (`verifyMpvValues`) | JUnit 4 / Gradle Test Runner | `DecodeMode` |
| **[PlayerViewModelPropertyChangeTest.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/test/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModelPropertyChangeTest.kt)** | **Reactive State & Event Verification.** Verifies that native libmpv property events (`time-pos`, `duration`, `pause`, `eof-reached`) correctly update immutable `PlayerState` with equality guards and trigger playlist EOF transitions. | `@Test` methods (`onPropertyChange_pause`, `eofReached_triggersNext`) | JUnit 4 / Gradle Test Runner | `PlayerViewModel`, `MpvController`, `PlaylistManager` |

---

## 5. Leader & Subsystem Deep-Dive

### A. The Brain: `PlayerViewModel` & Equality-Guarded State Updates
[PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt) orchestrates three specialized sub-domains:
1. **Playlist Management (`PlaylistManager`)**: Handles queueing multiple videos, resolving file paths, and managing end-of-file (EOF) transitions.
2. **Subtitle & Styling (`SubtitleController`)**: Manages subtitle selection, external file sideloading (`sub-add`), and font scaling.
3. **Resume Progress (`ResumePositionManager`)**: Automatically debounces time updates and commits the current playback timestamp to `ResumePositionDao`.

*Equality-Guarded Property Changes*: In `onPropertyChange(property, value)`, the ViewModel explicitly guards against redundant `_playerState.update` calls by comparing incoming native property values against the current `_playerState.value` across all high-frequency properties (`"pause"`, `"aid"`, `"sid"`, `"speed"`, and `"volume"`). This guarantees that Compose UI components only recompose when a value has genuinely changed.

### B. The Gesture & Overlay Bridge: `PlayerCoordinator` & `OverlayController`
To decouple gesture processing from direct ViewModel or Compose UI dependencies, [PlayerCoordinator.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/PlayerCoordinator.kt) implements the [MpvPlayerController](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureModels.kt) interface required by [GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt):
* **Playback Commands**: Directly executes media operations (`seekTo`, `setPlaybackSpeedRamped`, `setVolume`, `setBrightness`) against `PlayerViewModel`.
* **Overlay Routing**: Forwards visual indicator requests (`showVolumeOverlay`, `showBrightnessOverlay`, `showSpeedOverlay`, `showHorizontalSeekOverlay`) to [OverlayController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/coordinator/OverlayController.kt), which is attached and implemented by [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt).

### C. The Engine Gateway: `MpvController`, `MpvOptionsConfigurator`, & Generation-Aware Surface Safety
[MpvController.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt) encapsulates the C-JNI boundary into modular helpers:
* **`MpvOptionsConfigurator`**: Manages copying bundled font assets (`Roboto-Regular.ttf`) and applying pre-init engine settings (`GeneralOptions`, `SubtitleOptions`, `AudioOptions`, VO/GPU context).
* **`MpvCommandExecutor`**: Prevents concurrent modification exceptions and JNI threading crashes by dispatching all commands through a single-thread executor queue (`"mpv-engine-thread"`). It implements **generation-aware surface handling** via an `AtomicInteger` generation counter to prevent race conditions during lifecycle detachment.

### D. The Input Engine: `GestureHandler`, `GestureModels`, & Single-Ownership State Machine
[GestureHandler.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt) delegates multi-touch classification to [MpvGestureStateMachine.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/MpvGestureStateMachine.kt), ensuring **zero touch ambiguity** across mutually exclusive states:
* `TapCandidate`, `MultiTapSeeking`, `LongPress`, `DynamicSpeedScrub`, `VerticalSwipe`, `HorizontalSeek`, `PinchZoomPan`, and `SinglePan`.

### E. More Options Menu & Global Settings Integration
[MoreOptionsSheet.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/dialogs/MoreOptionsSheet.kt) provides a unified side sheet triggered from the 3-dot overflow menu in [PlayerQuickActions.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerQuickActions.kt):
* **Speed Selection**: Interactive speed selection chips.
* **FileInfo Inspection**: Displays [FileInfo.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/model/FileInfo.kt) metadata including video/audio/subtitle track counts.
* **App Settings Integration**: Triggers `onOpenSettings()`, wired through [PlayerActivity.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/PlayerActivity.kt) -> [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt) -> [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt) to open [SettingsScreen.kt](file:///c:/Users/tapman/Desktop/potatompv/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/settings/SettingsScreen.kt).

---

## 6. Summary of Architecture Harmony

The codebase achieves state-of-the-art performance, stability, and maintainability through a strict chain of responsibility:
* **UI & Gesture Components** never call mpv JNI functions directly; they emit user intents to `PlayerViewModel` or commands via `PlayerCoordinator`.
* **ViewModel & Business Logic** never manage Android Views or touch math; they update equality-guarded `StateFlow<PlayerState>` and coordinate domain managers.
* **Engine Components** never know about UI layouts or databases; they execute thread-safe, generation-aware C/JNI commands and emit structured events.
