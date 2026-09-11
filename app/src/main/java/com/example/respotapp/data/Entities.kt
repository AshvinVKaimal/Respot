package com.example.respotapp.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "local_tracks")
data class LocalTrackEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long
)

@Entity(tableName = "spotify_albums")
data class SpotifyAlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "added_at") val addedAt: String?,
    @ColumnInfo(name = "release_date") val releaseDate: String? = null,
    @ColumnInfo(name = "library_order") val libraryOrder: Long = 0L
)

@Entity(tableName = "spotify_playlists")
data class SpotifyPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val owner: String,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = 0L,
    @ColumnInfo(name = "library_order") val libraryOrder: Long = 0L
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
    @ColumnInfo(name = "override_album") val overrideAlbum: String?,
    @ColumnInfo(name = "override_artwork_uri") val overrideArtworkUri: String?,
    @ColumnInfo(name = "override_track_number") val overrideTrackNumber: Int?,
    @ColumnInfo(name = "override_disc_number") val overrideDiscNumber: Int?
)

@Entity(tableName = "item_metadata_overrides")
data class ItemMetadataOverrideEntity(
    @PrimaryKey val itemId: String,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "override_title") val overrideTitle: String?,
    @ColumnInfo(name = "override_artist") val overrideArtist: String?,
    @ColumnInfo(name = "override_artwork_uri") val overrideArtworkUri: String?,
    @ColumnInfo(name = "override_release_date") val overrideReleaseDate: String? = null
)

@Entity(tableName = "listening_history")
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "album_id") val albumId: String? = null,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "played_at") val playedAt: Long = System.currentTimeMillis()
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
