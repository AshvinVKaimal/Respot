## Project Overview

**Respot** is a specialized, modern Android audio application engineered to overcome the structural rigidity of server-side album metadata. The music listening app decouples the physical location of audio assets from their conceptual presentation and acts as a **unified local metadata virtualization engine** sitting on top of both the official Spotify Web API (leveraging an authenticated Spotify Premium account) and the Android device's Scoped Storage subsystem. 

Through Respot, users can stream any track from the vast Spotify catalog alongside their own local files (`.mp3`, `.flac`, `.wav`, `.m4a`) within a single, completely editable user interface. Users can inject local files straight into official streaming albums, fully overwrite tracklists, modify metadata fields (such as overriding album title and artwork, track titles, artist names, etc.), and curate hybrid offline/online listening environments. 

The application prioritizes a high-performance, purist audio experience featuring a modular, user-customizable homepage layout and seamless, gapless playback.

---

### Key Flow

```
+----------------------------------------------------------------------------------+
|                                  USER LAUNCH                                     |
+----------------------------------------------------------------------------------+
                                         |
                                         v
+----------------------------------------------------------------------------------+
|                          INITIAL AUTHENTICATION FLOW                             |
| - App verifies local SharedPreferences for encrypted OAuth2 Tokens.              |
| - If absent/expired, launches Spotify Custom Tab Auth (App Remote SDK / Web API) |
| - Validates Premium status tier; simultaneously requests READ_MEDIA_AUDIO.       |
+----------------------------------------------------------------------------------+
                                         |
                                         v
+---------------------------------------------------------------------------------+
|                           ASSET INTERPRETATION FLOW                             |
| - Local Scoped Storage scan populates Room DB with absolute file URIs.          |
| - App fetches User's Saved Albums/Playlists via Spotify Web API.                |
| - Room DB populates relational records linking Spotify Track IDs & Local URIs.  |
+---------------------------------------------------------------------------------+
                                         |
                                         v
+----------------------------------------------------------------------------------+
|                          VIRTUAL METADATA ENGINE (VME)                           |
|  - User selects an official Album and selects "Clone to Virtual Album".          |
|  - User edits title, artwork, inserts a local track between Spotify tracks.      |
|  - Room updates relational mapping without altering original Spotify source.     |
+----------------------------------------------------------------------------------+
                                         |
+----------------------------------------+------------------------------------------+
|                                                                                   |
v                                                                                   v
+------------------------------------------------+  +-------------------------------+
|             PLAYBACK CONTROLLER                |  |       OFFLINE SYNC            |
| - User hits Play on Virtual Album.             |  | - User tags a Spotify track   |
| - Pipeline maps tracks to Media3 MediaItems.   |  |   or Virtual Album for        |
| - Audio Engine evaluates track type:           |  |   offline caching.            |
|                                                |  | - WorkManager launches secure |
|   [IF LOCAL]:                                  |  |   encapsulated background     |
|   ExoPlayer reads ContentProvider File URI.    |  |   progressive cache stream.   |
|                                                |  +-------------------------------+
|   [IF SPOTIFY STREAM]:                         |
|   App requests streaming URL via Spotify SDK / |
|   Web API proxy. Emits to ExoPlayer audio-sink.|
|                                                |
|   [IF CACHED STREAM]:                          |
|   ExoPlayer reads from local internal cache.   |
|                                                |
| - Jetpack Media3 orchestrates gapless buffer   |
|   preloading on upcoming items.                |
+------------------------------------------------+
```

#### Detailed Package Implementation Steps:
1. **Authentication & Token Management:** The user launches the app and triggers the `Spotify-App-Remote` auth flow or standard OAuth2 Web API endpoint via a secure Chrome Custom Tab (`androidx.browser:browser`). Tokens are received via an Intent filter redirect URI, evaluated, and stored securely using `androidx.security:security-crypto`. Concurrently, the user is prompted for physical media access via the Jetpack Compose `RememberLauncherForActivityResult` binding to `ActivityResultContracts.RequestPermission()`.
2. **Library Synthesis (Scanning & Syncing):** Upon acquisition of `READ_MEDIA_AUDIO` (or `READ_EXTERNAL_STORAGE` on legacy APIs), a background worker subclassed from `androidx.work:work-runtime-ktx` initiates a query across the system `ContentResolver` to index audio metadata (Title, Artist, Duration, Absolute URI). These records are inserted into a Local Tracks schema within a **Room Database** (`androidx.room:room-runtime`). Simultaneously, an asynchronous `Retrofit` service fetches user-saved cloud albums and playlists from the Spotify Web API, staging them within the same schema.
3. **Virtual Customization (VME Loop):** When a user triggers an edit command on an item via the UI, a Jetpack Compose screen updates state flowing from a `ViewModel`. If a track is replaced by a local file, a junction table in the Room Database registers a mapping pairing the `virtual_album_id` to the local file's `content://` URI, along with any user-overridden strings (e.g., customized track name, customized artwork URI).
4. **Playback Orchestration:** When a Virtual Album is queued, the repository evaluates the unified collection. Each element is mapped to an `androidx.media3:media3-common:MediaItem`. The collection is dispatched to an ongoing background `MediaSessionService` wrapping an `ExoPlayer` instance. If an entry is local, its asset URI is directly parsed. If it is a Spotify entry, its web stream URI is fed to the player. ExoPlayer handles pre-buffering of the next track index 20 seconds before the current audio pointer finishes, ensuring an immediate audio-sink transition (Gapless Playback).

---

### Tech Stack

* **Frontend (UI Layer):** * **Jetpack Compose:** Declarative UI implementation using single-state flows.
    * **Compose Foundation Layout & Material 3:** Modern, accessible design elements conforming to dynamic styling guidelines.
    * **Accompanist / Compose Reorderable List:** For drag-and-drop track re-ordering interfaces.
    * **Coil (Compose Extension):** Asynchronous, hardware-accelerated image loading supporting local paths, content URIs, and network image endpoints.
* **Audio Engine Layer:**
    * **Jetpack Media3 ExoPlayer:** Complete audio pipeline support, progressive streaming, customizable audio-renderers, audio-focus delegation, and background service mapping.
    * **Jetpack Media3 Session:** Provides system-wide media controls, lock screen metadata integration, and Android Auto extensibility.
* **Data Persistence Layer (Local DB):**
    * **Room SQLite Object Mapping Library:** Manages relational entities for Local Tracks, Synced Spotify Mirror Tracks, Virtual Albums, Custom Playlists, and Homepage Module Preferences.
    * **EncryptedSharedPreferences:** Secures client credentials, access tokens, refresh tokens, and specific cryptographic seeds.
* **Networking & Integration Layer:**
    * **Retrofit 2 & OkHttp 3:** Network abstraction for Web API calls, featuring token refresh interceptors, rate-limit retry handlers, and disk-caching mechanisms.
    * **Kotlin Coroutines & Flow:** Reactive, asynchronous stream boundaries across Data, Domain, and UI layers.
* **APIs & Services:**
    * **Spotify Web API:** Metadata lookup, audio feature parsing, cloud playlist replication, and premium authorization flags.
    * **Spotify App Remote SDK / Web Stream Proxy:** Media playback control hooks for authenticating audio output from Spotify servers into the unified wrapper.
    * **Android MediaStore System API:** Queries local filesystem audio content securely within the storage scope.

---

## Core Functionalities

### 1. Multi-Source Authentication & Storage Verification

* **Purpose:** Securely link the user's Spotify Premium account while establishing safe read access to local audio assets.
* **Design & Implementation:**
    * The login sequence triggers an OAuth2 authorization code flow with proof key for code exchange (PKCE). It utilizes specific scopes: `user-library-read`, `user-library-modify`, `playlist-read-private`, `playlist-modify-private`, `playlist-modify-public`, `streaming`.
    * Upon receiving an auth callback, an `Interceptor` appends the `Authorization: Bearer <token>` header to all outgoing requests. A periodic evaluation checks if tokens are within 5 minutes of expiration; if true, it quietly spins a Coroutine to trigger the `/api/token` refresh endpoint using the refresh token payload stored inside `EncryptedSharedPreferences`.
    * Simultaneously, the UI displays a clean runtime prompt for media verification. If access is granted, a background sync service queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
* **Error Handling:**
    * *Invalid Account Tier Exception:* If the profile object returned by `https://api.spotify.com/v1/me` contains a `product` field equal to `free`, the app terminates the session, invalidates cache tokens, and presents a non-intrusive full-screen warning specifying that a Spotify Premium account is required for third-party streaming integration.
    * *Storage Access Denied:* If the user rejects the storage permission, all local file functionalities gracefully lock out, showing a placeholder icon on UI blocks that normally display local music, alongside an operational "Grant Permission" button.

### 2. The Virtual Metadata Engine (VME)

* **Purpose:** Enable modification of fixed cloud album layouts by establishing a locally managed database abstraction layer.
* **Design & Implementation:**
    * Define three concrete database schemas: `VirtualAlbumEntity`, `TrackMappingEntity`, and `TrackMetadataOverrideEntity`.
    * When an official Spotify album is selected for editing, the app creates a `VirtualAlbumEntity` containing a unique uuid while copying the original properties (Default Name, Original Artist, Original Cover URL).
    * Tracks within this virtual album are mapped via a relational table containing order indices (`sequence_position`). 
    * To replace a track, the `TrackMappingEntity` swaps its pointer reference from the old `spotify_track_id` to a newly registered local file path `content://media/external/audio/media/...`.
    * Metadata customization screens execute standard atomic database transactions (`@Transaction` in Room). If a user renames an album or selects a custom local image file for artwork, the path to that image file or text string is saved to the override entity. Coil reads these local string indices before defaulting back to network targets.
* **Error Handling:**
    * *Missing Local Source:* If a local audio file is renamed or moved outside the app's scope, the file descriptor will throw a `FileNotFoundException`. The database catches this flag during playback preparation, updates the target track entity state with a broken status flag, grays out the list track visually, and safely skips to the next valid sequence element.

### 3. High-Fidelity Audio Engine & Gapless Playback Pipeline

* **Purpose:** Provide uniform, continuous background playback transitions across heterogeneous audio components without audible clicks, gaps, or long buffering pauses.
* **Design & Implementation:**
    * The core audio service extends `MediaSessionService`, managing a single highly optimized `ExoPlayer` instance configured via an `DefaultLoadControl` that allocates an aggressive buffer size (minimum 30 seconds, maximum 60 seconds).
    * Gapless functionality is handled by chaining `MediaItem` inputs inside ExoPlayer's internal playlist sequence mechanism. When index `N` is actively playing, ExoPlayer automatically initializes a background network socket connection or content file descriptor channel for index `N+1`.
    * For streaming resources, the app sets up an `OkHttpDataSource.Factory` which handles chunked stream downloads directly.
* **Error Handling:**
    * *Network Timeout/Dropouts:* If the streaming audio buffer starves due to zero network connection, ExoPlayer transitions to `STATE_BUFFERING`. If a timeout condition triggers, the engine catches the `PlaybackException`, records the elapsed timestamp, tests if the upcoming track index `N+1` is an offline local file, and immediately executes a jump skip to keep the music playing.

### 4. Offline Sync & Caching Architecture

* **Purpose:** Enable local downloading of streaming assets and reliable storage of tracks for completely disconnected playback.
* **Design & Implementation:**
    * Utilizes ExoPlayer's `CacheDataSourceFactory` configured with a dedicated `SimpleCache` instance bound to a localized directory (`context.cacheDir/audio_cache`).
    * When an offline flag is enabled for an album or playlist, a persistent background download manager implemented via `WorkManager` queues the target streaming tracks. It loops through network assets, downloads them progressively using low-priority bandwidth flags, and writes them to the `SimpleCache` using the track's canonical Spotify ID as the cache key.
    * When playing a streaming asset, the `CacheDataSource` interceptor evaluates whether the key exists locally. If present, it serves audio packets directly from internal flash memory with absolute zero network utilization.
* **Error Handling:**
    * *Out of Storage Allocation:* If the device storage limit drops below 200MB during download loops, the `WorkManager` intercepts the disk error, halts active download operations, drops remaining items in the queue, and sends a system tray notification stating: "Sync paused due to low storage availability."

### 5. Modular & Personalizable Home Interface

* **Purpose:** Provide an uncluttered, high-speed launch screen completely configurable by the end user.
* **Design & Implementation:**
    * The home view uses a collection layout that reads structural configuration matrices out of a lightweight `home_layout_preferences` Room schema.
    * The layout data model consists of a list of active modules:
        ```kotlin
        enum class HomeModuleType { RECENTLY_PLAYED, VIRTUAL_ALBUMS, CUSTOM_PLAYLISTS, LOCAL_TRACKS, LISTENING_STATS }
        data class HomeModuleConfig(val type: HomeModuleType, val orderIndex: Int, val isVisible: Boolean)
        ```
    * A preference panel allows users to toggle visibility flags or drag-and-drop rows to modify `orderIndex`. The main interface queries this dynamic profile, emitting a list of UI composables arranged exactly as the user specified.
    * "Listening Stats" computes internal telemetry parameters gathered quietly by tracking every successful audio completion record written to a local database ledger.
* **Error Handling:**
    * *Empty State Anomalies:* If a user activates a module (e.g., "Recently Played") but the database contains zero history rows, the composable function catches the empty collection state gracefully, rendering a minimal, clean illustrative frame recommending actions instead of throwing null-pointer redraw exceptions.

---

## Documentation

{to be filled later}

---

## Current File Structure

{to be filled later}

---

## Non-Functional & Performance Benchmarks

### 1. Latency & Playback Performance
* **Gapless Transition Overhead:** When shifting between a streaming network asset and a local file descriptor source, the continuous audio transition gap must not exceed **40 milliseconds**. Pre-buffering thresholds must load at least 5 seconds of downstream data into memory loops before current track completion.
* **Database Transaction Snappiness:** Any local state modification or metadata overwrite executed via the Virtual Metadata Engine must resolve inside Room within **15 milliseconds** on a standard thread pool to prevent rendering glitches on Jetpack Compose screens.

### 2. Security Parameters
* **Cryptographic Safety:** All OAuth tokens, client secrets, and identifying account payloads must remain completely isolated within internal hardware-backed storage sandboxes provided by `EncryptedSharedPreferences`. Plaintext token strings must never be printed to application logging loops.

### 3. Resource Conservation
* **Power Optimization:** While streaming in the background with the display module inactive, the `RespotPlaybackService` must effectively pool CPU awake states, maintaining an operational target footprint below **6% total battery drain per hour** on a standard 4000mAh mobile device battery.