package com.example.respotapp.playback

enum class PlaybackSource {
    LOCAL,
    SPOTIFY_STREAM,
    CACHED
}

enum class PlaybackEngine {
    EXOPLAYER,
    SPOTIFY_REMOTE
}

data class PlaybackQueueItem(
    val id: String,
    val title: String,
    val artist: String,
    val albumTitle: String,
    val artworkUrl: String?,
    val source: PlaybackSource,
    val playbackUri: String?,
    val spotifyUri: String? = null,
    val albumId: String? = null,
    val durationMs: Long
)

data class PlayerUiState(
    val isActive: Boolean = false,
    val trackId: String? = null,
    val title: String = "",
    val artist: String = "",
    val albumTitle: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val showExpandedPlayer: Boolean = false,
    val playbackEngine: PlaybackEngine? = null
)
