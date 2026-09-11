package com.example.respotapp.domain

enum class MetadataItemType {
    TRACK,
    ALBUM,
    PLAYLIST
}

data class EditableMetadata(
    val itemId: String,
    val itemType: MetadataItemType,
    val title: String,
    val artist: String,
    val albumName: String? = null,
    val artworkUrl: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val releaseDate: String? = null,
    val isLocal: Boolean = false,
    val localUri: String? = null
)

data class PlaylistDetail(
    val id: String,
    val title: String,
    val owner: String,
    val artworkUrl: String?,
    val tracks: List<AlbumTrackItem>
)
