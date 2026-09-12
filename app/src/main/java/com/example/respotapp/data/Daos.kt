package com.example.respotapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LocalTrackDao {
    @Query("SELECT * FROM local_tracks ORDER BY title ASC")
    suspend fun getAll(): List<LocalTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<LocalTrackEntity>)

    @Query("DELETE FROM local_tracks")
    suspend fun clearAll()

    @Query("SELECT DISTINCT album, artist FROM local_tracks ORDER BY album ASC")
    suspend fun getDistinctAlbums(): List<LocalAlbumSummary>

    @Query("SELECT COUNT(*) FROM local_tracks WHERE album = :album AND artist = :artist")
    suspend fun countTracksInAlbum(album: String, artist: String): Int

    @Query("SELECT * FROM local_tracks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LocalTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: LocalTrackEntity)

    @Query("SELECT * FROM local_tracks WHERE album = :album AND artist = :artist ORDER BY title ASC")
    suspend fun getTracksInAlbum(album: String, artist: String): List<LocalTrackEntity>
}

data class LocalAlbumSummary(
  val album: String,
  val artist: String
)

@Dao
interface SpotifyAlbumDao {
    @Query("SELECT * FROM spotify_albums ORDER BY name ASC")
    suspend fun getAll(): List<SpotifyAlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<SpotifyAlbumEntity>)

    @Query("DELETE FROM spotify_albums")
    suspend fun clearAll()
}

@Dao
interface SpotifyPlaylistDao {
    @Query("SELECT * FROM spotify_playlists ORDER BY name ASC")
    suspend fun getAll(): List<SpotifyPlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlists: List<SpotifyPlaylistEntity>)

    @Query("DELETE FROM spotify_playlists WHERE id != :keepId")
    suspend fun deleteAllExcept(keepId: String)

    @Query("DELETE FROM spotify_playlists")
    suspend fun clearAll()
}

@Dao
interface VirtualAlbumDao {
    @Transaction
    @Query("SELECT * FROM virtual_albums WHERE id = :albumId")
    suspend fun getVirtualAlbumWithTracks(albumId: String): FullVirtualAlbumRelation?

    @Query("SELECT * FROM virtual_albums ORDER BY custom_title ASC")
    suspend fun getAllVirtualAlbums(): List<VirtualAlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVirtualAlbum(album: VirtualAlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<TrackMappingEntity>)

    @Query("SELECT * FROM virtual_albums WHERE original_spotify_id = :spotifyId LIMIT 1")
    suspend fun getByOriginalSpotifyId(spotifyId: String): VirtualAlbumEntity?

    @Query("DELETE FROM track_mappings WHERE virtual_album_id = :albumId")
    suspend fun clearMappings(albumId: String)

    @Query("DELETE FROM virtual_albums WHERE id = :albumId")
    suspend fun deleteVirtualAlbum(albumId: String)

    @Transaction
    suspend fun cloneAndModifyAlbum(
        album: VirtualAlbumEntity,
        mappings: List<TrackMappingEntity>
    ) {
        insertVirtualAlbum(album)
        insertMappings(mappings)
    }
}

@Dao
interface MetadataOverrideDao {
    @Query("SELECT * FROM metadata_overrides WHERE trackId = :trackId")
    suspend fun getOverride(trackId: String): TrackMetadataOverrideEntity?

    @Query("SELECT * FROM metadata_overrides")
    suspend fun getAllOverrides(): List<TrackMetadataOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: TrackMetadataOverrideEntity)

    @Query("DELETE FROM metadata_overrides WHERE trackId = :trackId")
    suspend fun deleteOverride(trackId: String)

    suspend fun getOverrideOrNull(trackId: String?): TrackMetadataOverrideEntity? {
        if (trackId.isNullOrBlank()) return null
        return getOverride(trackId)
    }
}

@Dao
interface ItemMetadataOverrideDao {
    @Query("SELECT * FROM item_metadata_overrides WHERE itemId = :itemId AND item_type = :itemType")
    suspend fun getOverride(itemId: String, itemType: String): ItemMetadataOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: ItemMetadataOverrideEntity)

    @Query("SELECT * FROM item_metadata_overrides")
    suspend fun getAll(): List<ItemMetadataOverrideEntity>

    @Query("DELETE FROM item_metadata_overrides WHERE itemId = :itemId AND item_type = :itemType")
    suspend fun deleteOverride(itemId: String, itemType: String)

    suspend fun getOverrideOrNull(itemId: String?, itemType: String): ItemMetadataOverrideEntity? {
        if (itemId.isNullOrBlank()) return null
        return getOverride(itemId, itemType)
    }
}

@Dao
interface ListeningHistoryDao {
    @Query("SELECT * FROM listening_history ORDER BY played_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ListeningHistoryEntity>

    @Query("SELECT COUNT(*) FROM listening_history")
    suspend fun getTotalPlays(): Int

    @Query("SELECT COUNT(DISTINCT track_id) FROM listening_history")
    suspend fun getUniqueTracks(): Int

    @Query("SELECT SUM(duration_ms) FROM listening_history")
    suspend fun getTotalDurationMs(): Long?

    @Insert
    suspend fun insert(entry: ListeningHistoryEntity)

    @Query(
        "SELECT album_id AS albumId, MAX(played_at) AS playedAt " +
            "FROM listening_history WHERE album_id IS NOT NULL GROUP BY album_id"
    )
    suspend fun getLastPlayedByAlbum(): List<AlbumPlaySummary>
}

data class AlbumPlaySummary(
    val albumId: String,
    val playedAt: Long
)

@Dao
interface HomeLayoutDao {
    @Query("SELECT * FROM home_layout ORDER BY order_index ASC")
    suspend fun getAll(): List<HomeLayoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modules: List<HomeLayoutEntity>)
}
