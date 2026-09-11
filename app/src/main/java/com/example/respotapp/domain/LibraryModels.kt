package com.example.respotapp.domain

data class LibraryAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val source: AlbumSource,
    val trackCount: Int,
    val addedAt: String? = null,
    val lastPlayedAt: Long? = null,
    val libraryOrder: Long = 0L,
    val releaseDate: String? = null
)

enum class AlbumSource {
    SPOTIFY,
    LOCAL,
    VIRTUAL
}

data class LibraryPlaylist(
    val id: String,
    val title: String,
    val owner: String,
    val artworkUrl: String?,
    val trackCount: Int,
    val syncedAt: Long? = null,
    val libraryOrder: Long = 0L
)

sealed class LibraryItem {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String
    abstract val artworkUrl: String?
    abstract val trackCount: Int

    data class AlbumItem(
        val album: LibraryAlbum
    ) : LibraryItem() {
        override val id = album.id
        override val title = album.title
        override val subtitle = album.artist
        override val artworkUrl = album.artworkUrl
        override val trackCount = album.trackCount
    }

    data class PlaylistItem(
        val playlist: LibraryPlaylist
    ) : LibraryItem() {
        override val id = playlist.id
        override val title = playlist.title
        override val subtitle = playlist.owner
        override val artworkUrl = playlist.artworkUrl
        override val trackCount = playlist.trackCount
    }
}

enum class LibraryFilter {
    ALL,
    ALBUMS,
    PLAYLISTS,
    LOCAL,
    ARTISTS
}

enum class LibrarySort {
    RECENTLY_PLAYED,
    RECENTLY_ADDED,
    ALPHABETICAL,
    CREATOR_NAME
}

val LibrarySort.label: String
    get() = when (this) {
        LibrarySort.RECENTLY_PLAYED -> "Recently played"
        LibrarySort.RECENTLY_ADDED -> "Recently added"
        LibrarySort.ALPHABETICAL -> "Alphabetical"
        LibrarySort.CREATOR_NAME -> "Creator name"
    }

/** Normalizes Spotify release dates (YYYY, YYYY-MM, YYYY-MM-DD) for ascending sort. */
fun releaseDateSortKey(date: String?): Long {
    if (date.isNullOrBlank()) return Long.MAX_VALUE
    val parts = date.trim().split("-")
    return try {
        when (parts.size) {
            1 -> parts[0].toLong() * 10000
            2 -> parts[0].toLong() * 10000 + parts[1].toLong() * 100
            else -> parts[0].toLong() * 10000 + parts[1].toLong() * 100 + parts[2].toLong()
        }
    } catch (_: Exception) {
        Long.MAX_VALUE
    }
}

data class ArtistDetail(
    val id: String,
    val name: String,
    val artworkUrl: String?,
    val genres: List<String>,
    val followerCount: Int,
    val albums: List<LibraryAlbum>
)

data class AlbumDetail(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String?,
    val artworkUrl: String?,
    val releaseDate: String?,
    val tracks: List<AlbumTrackItem>,
    val virtualAlbumId: String? = null,
    val isCustomized: Boolean = false,
    val source: AlbumSource = AlbumSource.SPOTIFY
)

data class AlbumTrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val albumName: String,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int = 1,
    val isLiked: Boolean = false,
    val isLocal: Boolean = false,
    val localUri: String? = null,
    val artworkUrl: String? = null
)
