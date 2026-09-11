package com.example.respotapp.domain

sealed class AudioTrack {
    abstract val id: String
    abstract val title: String
    abstract val artist: String
    abstract val durationMs: Long

    data class Local(
        override val id: String,
        val contentUri: String,
        override val title: String,
        override val artist: String,
        val album: String,
        override val durationMs: Long
    ) : AudioTrack()

    data class Spotify(
        override val id: String,
        val uri: String,
        override val title: String,
        override val artist: String,
        val albumName: String,
        val artworkUrl: String?,
        override val durationMs: Long
    ) : AudioTrack()
}

data class VirtualAlbum(
    val id: String,
    val originalSpotifyId: String?,
    val customTitle: String,
    val customArtist: String,
    val customArtworkUri: String?,
    val tracks: List<VirtualTrackMapping>
)

data class VirtualTrackMapping(
    val sequencePosition: Int,
    val spotifyTrackId: String?,
    val localTrackUri: String?,
    val isBroken: Boolean,
    val overrideTitle: String?,
    val overrideArtist: String?,
    val overrideArtworkUri: String?
)

enum class HomeModuleType {
    RECENTLY_PLAYED,
    VIRTUAL_ALBUMS,
    CUSTOM_PLAYLISTS,
    LOCAL_TRACKS,
    LISTENING_STATS
}

data class HomeModule(
    val type: HomeModuleType,
    val orderIndex: Int,
    val isVisible: Boolean
)

enum class UserTier {
    FREE,
    PREMIUM,
    UNKNOWN
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val type: SearchResultType,
    val uri: String?,
    val albumId: String? = null,
    val relevanceScore: Int = 0
)

enum class SearchResultType {
    ARTIST,
    ALBUM,
    TRACK
}

data class UiSettings(
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val useDynamicColor: Boolean = false,
    val accentColorHex: String = "#1EEBD8",
    val compactLayout: Boolean = false,
    val showAlbumArtInList: Boolean = true,
    val nowPlayingLayout: NowPlayingLayout = NowPlayingLayout.CLASSIC,
    val nowPlayingShowArtBackground: Boolean = true,
    val nowPlayingShowProgress: Boolean = true,
    val nowPlayingLargeArtwork: Boolean = true
)

enum class NowPlayingLayout {
    CLASSIC,
    MINIMAL,
    IMMERSIVE
}

enum class DarkModePreference {
    SYSTEM,
    LIGHT,
    DARK
}
