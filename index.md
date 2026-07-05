# Potato Player MPV - Architecture & File Overview

## Architecture Overview

**Potato Player MPV** is a modern Android video player application built using **Kotlin**, **Jetpack Compose** (for the UI), and the powerful **mpv** media player engine. 

The architecture follows a clean, reactive **MVVM (Model-View-ViewModel)** pattern integrated with an event-driven media engine wrapper:

1.  **UI/Presentation Layer (Jetpack Compose)**: Completely built using Compose. It defines declarative UI components (`PlayerScreen`, `PlayerBottomBar`, `GestureOverlay`). It observes state from the ViewModel and sends user intents (clicks, gestures) back to it.
2.  **ViewModel Layer (`PlayerViewModel`)**: The central brain connecting the UI to the underlying media engine. It holds the `PlayerState` using Kotlin StateFlows, receives user inputs, and delegates media commands to the `MpvController`. It also listens to engine events and updates the state accordingly.
3.  **Media Engine Layer (`core.engine`)**: Acts as an abstraction layer over the native `libmpv` library. It translates high-level app commands into low-level mpv properties/commands, and maps native mpv events back into Kotlin-friendly callbacks.
4.  **Data & Persistence Layer (`core.database`, `core.preferences`)**: 
    *   **Room Database**: Manages local data persistence, primarily for saving playback progress (`ResumePosition`) so users can resume videos from where they left off.
    *   **Preferences DataStore**: Manages user-configurable settings, such as subtitle appearance preferences.

---

## Detailed File List & Roles

Here is a detailed breakdown of all Kotlin source files, what they do, and why they exist in the project structure.

### App Entry Points
*   **`MainActivity.kt`**: The default entry point of the app. It likely serves as a launcher or handles the transition into the home screen or media picker.
*   **`PlayerActivity.kt`**: The dedicated Android `Activity` that hosts the video playback interface. *Why?* Video playback often requires specific Window flag configurations (like keeping the screen on, immersive full-screen mode, handling picture-in-picture) that are best scoped to a dedicated Activity.

### Core Domain & Data (`com.tapman104.mpvplayer.core`)

**Database (`core.database`)**
*   **`AppDatabase.kt`**: The Room Database class. *Why?* Serves as the main access point for the underlying SQLite database, defining database configurations and providing DAOs.
*   **`ResumePositionDao.kt`**: Data Access Object for playback resume points. *Why?* Provides SQL queries (insert, retrieve, delete) to manage saved video positions.
*   **`ResumePositionEntity.kt`**: The data model representing a row in the database. *Why?* Maps a video file path/URI to its last played position and timestamp.

**Mpv Engine Wrappers (`core.engine`)**
*   **`MpvController.kt`**: The primary coordinator for the mpv player. *Why?* Provides a unified, high-level API for the ViewModel to interact with the player (e.g., `play()`, `pause()`, `seek()`) without knowing native mpv specifics.
*   **`MpvCommandExecutor.kt`**: Responsible for sending string-based commands to the mpv engine. *Why?* mpv operates heavily on a command/property string interface; this file safely formats and executes them.
*   **`MpvEventDispatcher.kt`**: Listens to native events from mpv (like "playback started", "file loaded"). *Why?* Routes low-level engine events to registered listeners in the app's Kotlin layer.
*   **`MpvEventListener.kt`**: Interface defining callbacks for player events. *Why?* Allows decoupled components (like the ViewModel) to listen for changes.
*   **`MpvPropertyObserver.kt`**: Observes specific mpv properties (like `time-pos` or `duration`). *Why?* mpv requires explicit observation of properties. This file handles registering and parsing these property changes to keep the UI in sync.
*   **`MpvSurface.kt`**: A wrapper around an Android `SurfaceView` or `TextureView`. *Why?* The native video decoder requires an Android Surface to render the video frames onto the screen.
*   **`TrackListParser.kt`**: Parses metadata for audio and subtitle tracks. *Why?* mpv returns track lists in a raw format; this parses them into structured Kotlin objects.
*   **`MpvConstants.kt`**: Holds constant strings and values used for mpv properties and commands.

**Preferences (`core.preferences`)**
*   **`SubtitlePreferences.kt`**: Data class representing a user's subtitle styling choices (color, size, background).
*   **`UserPreferencesRepository.kt`**: Repository pattern for interacting with Android DataStore/SharedPreferences. *Why?* Abstracts the persistence mechanism for saving/loading user settings.

### User Interface (`com.tapman104.mpvplayer.player.ui`)

**Models & State (`player.model`, `player.state`)**
*   **`AudioTrack.kt`, `SubtitleTrack.kt`**: Data models representing specific media tracks.
*   **`DecodeMode.kt`**: Enum or sealed class for hardware/software decoding modes.
*   **`PlayerState.kt`**: Comprehensive data class representing the entire UI state of the player (is playing, current time, duration, buffering status). *Why?* Fundamental to Compose—the UI is simply a reflection of this state.
*   **`PlaylistState.kt`**: Manages state for the current playlist or queue of videos.
*   **`SubtitleAppearanceState.kt`**: Represents the current visual state applied to subtitles.

**Player Controls (`player.ui.controls`)**
*   **`PlayerTopBar.kt`**: The top toolbar containing the back button, title, and options menu.
*   **`PlayerBottomBar.kt`**: The bottom control bar containing the timeline/seekbar, play/pause, and time indicators.
*   **`PlayPauseButton.kt`**: An isolated Compose component for the central play/pause toggle.
*   **`SeekBar.kt`**: Custom implementation of a slider for video scrubbing. *Why?* Standard sliders often don't support video-specific needs like showing a buffered range or precise drag handling.

**Player Dialogs (`player.ui.dialog`)**
*   *These files represent Compose dialogs that pop up over the player.*
*   **`AudioTrackDialog.kt`**: Lets users switch audio languages/streams.
*   **`SubtitleTrackDialog.kt`**: Lets users select active subtitles.
*   **`PlaybackSpeedDialog.kt`**: Allows changing the playback rate (e.g., 1.5x, 2x).
*   **`SubtitleAppearanceDialog.kt`**: A settings panel to tweak subtitle styling.
*   **`MoreOptionsDialog.kt`**: A generic menu for less frequently used settings.
*   **`ResumeDialog.kt`**: Prompts the user "Resume from HH:MM?" when opening a previously watched video.

**Player Overlays & Screens (`player.ui.overlay`, `player.ui.screen`, `player.ui.video`)**
*   **`PlayerVideo.kt`**: The Compose wrapper that integrates `MpvSurface` into the Compose hierarchy. *Why?* Android Views must be wrapped in `AndroidView` to live inside Jetpack Compose.
*   **`GestureOverlay.kt`**: A transparent layer sitting on top of the video that intercepts touch events. *Why?* Detects horizontal swipes for seeking and vertical swipes for volume/brightness without interfering with button clicks.
*   **`PlayerOverlay.kt`**: The container that manages the visibility (fade in/out) of the on-screen display (OSD) controls like the TopBar and BottomBar.
*   **`PlayerScreen.kt`**: The master Compose layout that stacks the `PlayerVideo` at the bottom, `GestureOverlay` in the middle, and `PlayerOverlay` on top.

### Home Screen (`home.ui`)
*   **`HomeScreen.kt`**: The primary dashboard for the app where a user might select videos from a library or file picker before launching the player.

### Player Logic (`com.tapman104.mpvplayer.player.viewmodel`)
*   **`PlayerViewModel.kt`**: The core logic hub. It instantiates the `MpvController`, observes the `AppDatabase` for resume points, maintains the `PlayerState`, and handles all user interactions from the Compose UI. *Why?* Separates UI rendering from business logic, making the code testable and resilient to configuration changes (like screen rotations).
*   **`PlayerViewModelFactory.kt`**: A factory for creating the ViewModel. *Why?* Required when a ViewModel needs dependencies injected into its constructor (like Repositories or Controllers).

### Theme & Styling (`com.tapman104.mpvplayer.ui.theme`)
*   **`Color.kt`**: Defines the color palette (Primary, Background, Surface colors).
*   **`Type.kt`**: Defines typography and text styles.
*   **`Theme.kt`**: The overarching Compose Material Theme that applies the colors and typography globally to the application.
