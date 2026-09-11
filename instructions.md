# Respot

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

* **Frontend (UI Layer):**
* **Jetpack Compose:** Declarative UI implementation using single-state flows.
* **Compose Foundation Layout & Material 3:** Modern design elements conforming to dynamic styling guidelines.
* **Accompanist / Compose Reorderable List:** For drag-and-drop track re-ordering interfaces.
* **Coil (Compose Extension):** Asynchronous, hardware-accelerated image loading supporting local paths, content URIs, and network endpoints.


* **Audio Engine Layer:**
* **Jetpack Media3 ExoPlayer:** Complete audio pipeline support, progressive streaming, customizable audio-renderers, audio-focus delegation, and background service mapping.
* **Jetpack Media3 Session:** Provides system-wide media controls, lock screen metadata integration, and Android Auto extensibility.


* **Data Persistence Layer (Local DB):**
* **Room SQLite Object Mapping Library:** Manages relational entities for Local Tracks, Synced Spotify Mirror Tracks, Virtual Albums, Custom Playlists, and Homepage Module Preferences.
* **EncryptedSharedPreferences:** Secures client credentials, access tokens, refresh tokens, and cryptographic seeds.


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
* The login sequence triggers an OAuth2 authorization code flow with Proof Key for Code Exchange (PKCE) using scopes: `user-library-read`, `user-library-modify`, `playlist-read-private`, `playlist-modify-private`, `playlist-modify-public`, `streaming`.
* Token exchange returns access and refresh tokens. An `AuthInterceptor` appends the Bearer token to all outgoing Retrofit calls. A proactive token refresh loop evaluates token lifespan; if within 5 minutes of expiration, it executes a refresh request via `TokenManager`.
* MediaStore scanning is initiated upon granting `READ_MEDIA_AUDIO` permission. Scanning runs as a background process to avoid blocking the main UI thread.


* **Error Handling:**
* *Invalid Account Tier Exception:* If the `product` field in the Spotify profile object (`/v1/me`) is not `premium`, the user session is invalidated, and an alert dialog informs the user that Spotify Premium is required for audio streaming.
* *Storage Access Denied:* If media permissions are denied, local file capabilities are disabled, UI placeholders indicate missing local tracks, and a contextual "Grant Storage Access" action button is displayed.



### 2. The Virtual Metadata Engine (VME)

* **Purpose:** Enable modification of fixed cloud album layouts by establishing a locally managed database abstraction layer.
* **Design & Implementation:**
* Implemented using Room with three primary entities: `VirtualAlbumEntity`, `TrackMappingEntity`, and `TrackMetadataOverrideEntity`.
* When cloning an album, a new `VirtualAlbumEntity` record is created, referencing the target Spotify or local source.
* `TrackMappingEntity` records maintain the explicit sequence of items using a zero-indexed `sequence_position` field.
* Overrides (e.g., custom artwork URIs, user-edited titles, artist overrides) are stored in `TrackMetadataOverrideEntity`.
* Database updates are executed within atomic Room transactions (`@Transaction`).


* **Error Handling:**
* *Missing Local Source:* If a local content URI becomes unresolvable (`FileNotFoundException`), the VME updates the track's status flag in the local database to `BROKEN`. The UI displays a disabled state for the track, and playback orchestration skips over broken entries automatically.



### 3. High-Fidelity Audio Engine & Gapless Playback Pipeline

* **Purpose:** Provide uniform, continuous background playback transitions across heterogeneous audio components without audible clicks, gaps, or long buffering pauses.
* **Design & Implementation:**
* `RespotPlaybackService` manages an `ExoPlayer` instance initialized with an aggressive `DefaultLoadControl` (30s minimum buffer, 60s maximum buffer).
* Media sources are mapped to `Media3 MediaItem` objects. Gapless playback is maintained by enqueueing upcoming tracks into ExoPlayer's playlist queue before the active track finishes.
* Network streaming uses an `OkHttpDataSource.Factory` for streaming audio buffers over persistent HTTP/2 connections.


* **Error Handling:**
* *Network Timeout/Dropouts:* On network dropouts (`STATE_BUFFERING` timeout), `ExoPlayer` captures `PlaybackException`. If the next track in the queue is a cached or local asset, the controller immediately advances playback to the local item.



### 4. Offline Sync & Caching Architecture

* **Purpose:** Enable local downloading of streaming assets and reliable storage of tracks for completely disconnected playback.
* **Design & Implementation:**
* Uses `CacheDataSourceFactory` backed by a `SimpleCache` instance bound to `context.cacheDir/audio_cache` (managed by `CacheManager`).
* Background pre-fetching is orchestrated by `OfflineSyncWorker` (using `WorkManager`). Downloads process items iteratively, saving stream chunks into `SimpleCache` keyed by Spotify track ID.
* During playback, `CacheDataSource` resolves requests locally if the track key exists in cache, avoiding unnecessary network traffic.


* **Error Handling:**
* *Out of Storage Allocation:* If available disk space falls below 200MB, `OfflineSyncWorker` cancels active jobs, clears incomplete download caches, and posts a system notification informing the user of low storage space.



### 5. Modular & Personalizable Home Interface

* **Purpose:** Provide an uncluttered, high-speed launch screen completely configurable by the end user.
* **Design & Implementation:**
* Home screen layout structure is driven by configuration records stored in `HomeLayoutEntity`.
* Supported module types include `RECENTLY_PLAYED`, `VIRTUAL_ALBUMS`, `CUSTOM_PLAYLISTS`, `LOCAL_TRACKS`, and `LISTENING_STATS`.
* Users can toggle visibility and reorder modules via drag-and-drop. `HomeScreen` dynamically renders modules based on the sorted configuration.


* **Error Handling:**
* *Empty State Anomalies:* If a module has no content to display (e.g., `LISTENING_STATS` with zero history), the composable renders a clean, non-intrusive empty state card with an actionable prompt rather than hiding the module entirely or causing UI layout shifts.



---

## Documentation

### 1. Authentication & Spotify Web API Integration

#### PKCE OAuth Authorization Request

* **Endpoint:** `[https://accounts.spotify.com/authorize](https://accounts.spotify.com/authorize)`
* **HTTP Method:** `GET`
* **Query Parameters:**
* `client_id`: Application Client ID string.
* `response_type`: `code`
* `redirect_uri`: `respot://auth/callback`
* `code_challenge_method`: `S256`
* `code_challenge`: Base64URL-encoded SHA-256 string derived from code verifier.
* `scope`: `user-library-read user-library-modify playlist-read-private playlist-modify-private playlist-modify-public streaming`



#### Token Exchange Request

* **Endpoint:** `[https://accounts.spotify.com/api/token](https://accounts.spotify.com/api/token)`
* **HTTP Method:** `POST`
* **Headers:** `Content-Type: application/x-www-form-urlencoded`
* **Payload:**
```
grant_type=authorization_code&client_id=<CLIENT_ID>&code=<AUTH_CODE>&redirect_uri=respot%3A%2F%2Fauth%2Fcallback&code_verifier=<CODE_VERIFIER>

```


* **Success Response (200 OK):**
```json
{
  "access_token": "BQB7...x9A",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "LMAO...z10",
  "scope": "user-library-read user-library-modify playlist-read-private playlist-modify-private playlist-modify-public streaming"
}

```



#### Fetch Current User Profile

* **Endpoint:** `GET [https://api.spotify.com/v1/me](https://api.spotify.com/v1/me)`
* **Headers:** `Authorization: Bearer <ACCESS_TOKEN>`
* **Success Response (200 OK):**
```json
{
  "display_name": "Audio Purist",
  "id": "user_id_12345",
  "email": "user@example.com",
  "product": "premium",
  "images": [
    {
      "url": "https://i.scdn.co/image/ab67706c0000bebb..."
    }
  ]
}

```



#### Fetch Album Metadata

* **Endpoint:** `GET [https://api.spotify.com/v1/albums/](https://api.spotify.com/v1/albums/){id}`
* **Headers:** `Authorization: Bearer <ACCESS_TOKEN>`
* **Success Response (200 OK):**
```json
{
  "id": "4aawyAB9vmqN3uQFRMTOfY",
  "name": "Random Access Memories",
  "artists": [
    { "id": "4tZ1A9Erz2492A1R6iL2m1", "name": "Daft Punk" }
  ],
  "images": [
    { "url": "https://i.scdn.co/image/ab67616d0000b273b33d46dfa2635a64e17d13eb", "height": 640, "width": 640 }
  ],
  "tracks": {
    "items": [
      {
        "id": "69L24A22kS4aCqO4f2bL2m",
        "name": "Give Life Back to Music",
        "duration_ms": 274826,
        "track_number": 1,
        "uri": "spotify:track:69L24A22kS4aCqO4f2bL2m"
      }
    ]
  }
}

```



---

### 2. Local MediaStore Query Implementation

The following Kotlin code snippet demonstrates querying Android Scoped Storage via `ContentResolver` to index local `.mp3`, `.flac`, `.wav`, and `.m4a` files into domain objects:

```kotlin
package com.respot.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.respot.domain.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(private val context: Context) {

    suspend fun scanLocalAudioFiles(): List<AudioTrack.Local> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioTrack.Local>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Track"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                audioList.add(
                    AudioTrack.Local(
                        id = "local_$id",
                        contentUri = contentUri.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration
                    )
                )
            }
        }
        return@withContext audioList
    }
}

```

---

### 3. Room Database Schema Definition

Below are the entity definitions and atomic DAO queries required for maintaining virtualized metadata relationships.

```kotlin
package com.respot.data

import androidx.room.*

@Entity(tableName = "local_tracks")
data class LocalTrackEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long
)

@Entity(tableName = "virtual_albums")
data class VirtualAlbumEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "original_spotify_id") val originalSpotifyId: String?,
    @ColumnInfo(name = "custom_title") val customTitle: String,
    @ColumnInfo(name = "custom_artist") val customArtist: String,
    @ColumnInfo(name = "custom_artwork_uri") val customArtworkUri: String?
)

@Entity(
    tableName = "track_mappings",
    primaryKeys = ["virtual_album_id", "sequence_position"],
    foreignKeys = [
        ForeignKey(
            entity = VirtualAlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["virtual_album_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TrackMappingEntity(
    @ColumnInfo(name = "virtual_album_id") val virtualAlbumId: String,
    @ColumnInfo(name = "sequence_position") val sequencePosition: Int,
    @ColumnInfo(name = "spotify_track_id") val spotifyTrackId: String?,
    @ColumnInfo(name = "local_track_uri") val localTrackUri: String?,
    @ColumnInfo(name = "is_broken") val isBroken: Boolean = false
)

@Entity(tableName = "metadata_overrides")
data class TrackMetadataOverrideEntity(
    @PrimaryKey val trackId: String,
    @ColumnInfo(name = "override_title") val overrideTitle: String?,
    @ColumnInfo(name = "override_artist") val overrideArtist: String?,
    @ColumnInfo(name = "override_artwork_uri") val overrideArtworkUri: String?
)

@Entity(tableName = "home_layout")
data class HomeLayoutEntity(
    @PrimaryKey val moduleType: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "is_visible") val isVisible: Boolean
)

data class FullVirtualAlbumRelation(
    @Embedded val album: VirtualAlbumEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "virtual_album_id"
    )
    val mappings: List<TrackMappingEntity>
)

@Dao
interface VirtualAlbumDao {
    @Transaction
    @Query("SELECT * FROM virtual_albums WHERE id = :albumId")
    suspend fun getVirtualAlbumWithTracks(albumId: String): FullVirtualAlbumRelation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVirtualAlbum(album: VirtualAlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<TrackMappingEntity>)

    @Transaction
    suspend fun cloneAndModifyAlbum(
        album: VirtualAlbumEntity,
        mappings: List<TrackMappingEntity>
    ) {
        insertVirtualAlbum(album)
        insertMappings(mappings)
    }
}

```

---

### 4. Jetpack Media3 Playback Service & ExoPlayer Setup

This setup initializes a foreground `MediaSessionService` with pre-buffering support to enable gapless audio transitions.

```kotlin
package com.respot.playback

import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RespotPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000, // Min buffer: 30s
                60_000, // Max buffer: 60s
                1_500,  // Playback start buffer: 1.5s
                3_000   // Rebuffer start buffer: 3.0s
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Ensure next track is pre-buffered for gapless playback
                if (player.hasNextMediaItem()) {
                    player.prepare()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

```

---

### 5. WorkManager Offline Download & Cache Worker

This worker implementation manages progressive stream caching to internal disk storage using ExoPlayer's `SimpleCache`.

```kotlin
package com.respot.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.HttpDataSource
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineSyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val simpleCache: SimpleCache,
    private val upstreamFactory: HttpDataSource.Factory
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val streamUrl = inputData.getString("KEY_STREAM_URL") ?: return@withContext Result.failure()
        val trackId = inputData.getString("KEY_TRACK_ID") ?: return@withContext Result.failure()

        // Verify device storage threshold (> 200 MB free)
        val freeBytes = applicationContext.cacheDir.usableSpace
        if (freeBytes < 200 * 1024 * 1024) {
            return@withContext Result.failure()
        }

        try {
            val cacheDataSource = CacheDataSource(
                simpleCache,
                upstreamFactory.createDataSource(),
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
            )

            val dataSpec = DataSpec.Builder()
                .setUri(Uri.parse(streamUrl))
                .setKey(trackId)
                .setLength(C.LENGTH_UNSET.toLong())
                .build()

            val buffer = ByteArray(131072) // 128 KB buffer
            cacheDataSource.open(dataSpec)
            while (cacheDataSource.read(buffer, 0, buffer.size) != C.RESULT_END_OF_INPUT) {
                if (isStopped) {
                    cacheDataSource.close()
                    return@withContext Result.stopped()
                }
            }
            cacheDataSource.close()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

```

---

## Current File Structure

To maintain clean architecture boundaries while keeping file count minimal, the project is structured into **11 Kotlin source files** under `com.respot`:

```
app/src/main/java/com/respot/
├── RespotApp.kt
├── MainActivity.kt
├── data/
│   ├── AuthAndStorage.kt
│   ├── SpotifyApi.kt
│   ├── RespotDatabase.kt
│   └── SyncAndCache.kt
├── domain/
│   ├── Models.kt
│   └── Repositories.kt
├── playback/
│   ├── RespotPlaybackService.kt
│   └── PlaybackController.kt
└── ui/
    ├── ThemeAndComponents.kt
    ├── HomeScreen.kt
    ├── VmeScreen.kt
    └── PlayerScreen.kt

```

### File Map & Responsibilities

1. **`RespotApp.kt`**: Subclasses `Application`. Handles Hilt/DI initialization, creates notification channels for background audio playback, and instantiates global singletons (`SimpleCache`).
2. **`MainActivity.kt`**: Single Activity layout setup. Handles edge-to-edge UI configuration, runtime permissions (`READ_MEDIA_AUDIO`), Spotify OAuth authorization redirect callbacks, and the top-level Jetpack Compose `NavHost`.
3. **`data/AuthAndStorage.kt`**: Encapsulates `TokenManager` (`EncryptedSharedPreferences`), OkHttp `AuthInterceptor` for automatic Bearer token injection and refresh token rotation, and `MediaStoreScanner` for local audio indexing.
4. **`data/SpotifyApi.kt`**: Contains the `Retrofit` interface definition, OkHttp client setup, and inline API response Data Transfer Objects (DTOs) for Spotify endpoints (`/v1/me`, `/v1/albums`, `/v1/playlists`, `/v1/tracks`).
5. **`data/RespotDatabase.kt`**: Co-locates the Room database definition (`RespotDatabase`), all schema entities (`LocalTrackEntity`, `VirtualAlbumEntity`, `TrackMappingEntity`, `TrackMetadataOverrideEntity`, `HomeLayoutEntity`), and corresponding DAOs (`VirtualAlbumDao`, `TrackDao`, `HomeLayoutDao`).
6. **`data/SyncAndCache.kt`**: Implements `OfflineSyncWorker` (`CoroutineWorker`) for background stream downloading and `CacheManager` for managing ExoPlayer's disk cache setup.
7. **`domain/Models.kt`**: Defines clean domain models, sealed classes, and enums, including `AudioTrack` (handling Local, Spotify, and Hybrid tracks), `VirtualAlbum`, `HomeModule`, `PlaybackState`, and `UserTier`.
8. **`domain/Repositories.kt`**: Repository interfaces and implementations: `LibraryRepository` (merges local media and Spotify cloud libraries) and `VmeRepository` (handles atomic Room transactions for virtual metadata edits).
9. **`playback/RespotPlaybackService.kt`**: Extends `MediaSessionService`. Instantiates `ExoPlayer`, `DefaultLoadControl` (30s-60s pre-buffer configuration), `MediaSession`, and handles system media controls/lock screen integration.
10. **`playback/PlaybackController.kt`**: Bridges UI logic with `MediaController`. Converts domain models (`AudioTrack`) into `Media3 MediaItem` objects, exposing playback controls and state `StateFlow`s.
11. **`ui/ThemeAndComponents.kt`**: Defines Material 3 color schemes, typography, and shared reusable Composables (`AsyncArtworkImage`, `TrackRowItem`, error indicators).
12. **`ui/HomeScreen.kt`**: Contains `HomeViewModel` and `HomeScreen` composables for rendering and reordering home modules (`RecentlyPlayedModule`, `VirtualAlbumsModule`, `ListeningStatsModule`).
13. **`ui/VmeScreen.kt`**: Contains `VmeViewModel` and `VmeScreen` composables for metadata editing, artwork file picking, and drag-and-drop track reordering.
14. **`ui/PlayerScreen.kt`**: Contains `PlayerViewModel` and `PlayerScreen` composables for the expanded player interface, playback timeline slider, dynamic background styling, and mini-player UI.

---

## Non-Functional & Performance Benchmarks

### 1. Latency & Playback Performance

* **Gapless Transition Overhead:** Continuous audio transitions when shifting between network streams and local content URIs must complete within **40 milliseconds**, avoiding audible clicks or delays. Pre-buffering thresholds must load at least 5 seconds of downstream data before track transition.
* **Database Transaction Snappiness:** Room database reads, writes, and metadata overrides executed via the Virtual Metadata Engine must resolve within **15 milliseconds** on background thread pools to ensure fluid UI performance in Jetpack Compose views.

### 2. Security Parameters

* **Cryptographic Safety:** All OAuth tokens, client secrets, and authentication payloads must be stored in hardware-backed secure storage (`EncryptedSharedPreferences`). Token credentials must never be written to plaintext log outputs.

### 3. Resource Conservation

* **Power Optimization:** During background audio streaming with the display off, `RespotPlaybackService` must optimize CPU wake locks, targeting less than **6% battery consumption per hour** on a standard 4000mAh battery.