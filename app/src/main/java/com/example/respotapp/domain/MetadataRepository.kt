package com.example.respotapp.domain

import android.content.Context
import com.example.respotapp.data.ItemMetadataOverrideEntity
import com.example.respotapp.data.LocalMetadataWriter
import com.example.respotapp.data.LocalTrackEntity
import com.example.respotapp.data.RespotDatabase
import com.example.respotapp.data.SpotifyAddTracksRequest
import com.example.respotapp.data.SpotifyApi
import com.example.respotapp.data.TrackMappingEntity
import com.example.respotapp.data.TrackMetadataOverrideEntity
import com.example.respotapp.data.VirtualAlbumEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MetadataRepository(
    private val context: Context,
    private val database: RespotDatabase,
    private val spotifyApi: SpotifyApi
) {
    private val localMetadataWriter = LocalMetadataWriter(context)

    suspend fun getAlbumDetail(albumId: String): AlbumDetail = withContext(Dispatchers.IO) {
        when {
            albumId.startsWith("local_") -> buildLocalAlbumDetail(albumId)
            else -> {
                val virtualByOriginal = database.virtualAlbumDao().getByOriginalSpotifyId(albumId)
                val virtualRelation = virtualByOriginal?.let {
                    database.virtualAlbumDao().getVirtualAlbumWithTracks(it.id)
                } ?: database.virtualAlbumDao().getVirtualAlbumWithTracks(albumId)

                if (virtualRelation != null) {
                    buildVirtualAlbumDetail(virtualRelation, albumId)
                } else {
                    buildSpotifyAlbumDetail(albumId)
                }
            }
        }
    }

    suspend fun getPlaylistDetail(playlistId: String): PlaylistDetail = withContext(Dispatchers.IO) {
        if (playlistId == SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID) {
            return@withContext buildLikedSongsPlaylistDetail()
        }

        val playlistEntity = database.spotifyPlaylistDao().getAll().find { it.id == playlistId }
        val override = database.itemMetadataOverrideDao().getOverrideOrNull(playlistId, "PLAYLIST")

        val playlistMeta = try {
            spotifyApi.getPlaylist(playlistId)
        } catch (_: Exception) {
            null
        }

        val allItems = try {
            fetchAllPlaylistTrackItems(playlistId)
        } catch (_: Exception) {
            emptyList()
        }
        val trackIds = allItems.mapNotNull { it.track?.id?.takeIf { id -> id.isNotBlank() } }
        val likedMap = fetchLikedMap(trackIds)
        val overrideDao = database.metadataOverrideDao()

        val tracks = allItems.mapIndexedNotNull { index, item ->
            val track = item.track ?: return@mapIndexedNotNull null
            val trackId = track.id?.takeIf { it.isNotBlank() }
                ?: track.uri?.takeIf { it.isNotBlank() }
                ?: return@mapIndexedNotNull null
            val trackOverride = overrideDao.getOverrideOrNull(trackId)
            AlbumTrackItem(
                id = trackId,
                title = trackOverride?.overrideTitle ?: track.name ?: "Unknown Track",
                artist = trackOverride?.overrideArtist
                    ?: track.artists?.firstOrNull()?.name
                    ?: "Unknown",
                albumName = trackOverride?.overrideAlbum ?: track.album?.name ?: "",
                durationMs = track.durationMs,
                trackNumber = trackOverride?.overrideTrackNumber ?: track.trackNumber ?: (index + 1),
                discNumber = trackOverride?.overrideDiscNumber ?: track.discNumber ?: 1,
                isLiked = likedMap[trackId] == true,
                isLocal = track.isLocal || track.id.isNullOrBlank(),
                artworkUrl = trackOverride?.overrideArtworkUri ?: track.album?.images?.firstOrNull()?.url
            )
        }

        PlaylistDetail(
            id = playlistId,
            title = override?.overrideTitle
                ?: playlistMeta?.name
                ?: playlistEntity?.name
                ?: "Playlist",
            owner = override?.overrideArtist
                ?: playlistMeta?.owner?.displayName
                ?: playlistEntity?.owner
                ?: "",
            artworkUrl = override?.overrideArtworkUri
                ?: playlistMeta?.images?.firstOrNull()?.url
                ?: playlistEntity?.artworkUrl,
            tracks = tracks
        )
    }

    private suspend fun fetchAllPlaylistTrackItems(playlistId: String): List<com.example.respotapp.data.SpotifyPlaylistTrackItem> {
        val allItems = mutableListOf<com.example.respotapp.data.SpotifyPlaylistTrackItem>()
        var offset = 0
        var hasMore = true
        while (hasMore) {
            val response = spotifyApi.getPlaylistItems(
                playlistId = playlistId,
                limit = 50,
                offset = offset
            )
            allItems.addAll(response.items)
            offset += response.limit
            hasMore = response.next != null
        }
        return allItems
    }

    suspend fun updateMetadata(metadata: EditableMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (metadata.itemType) {
                MetadataItemType.TRACK -> updateTrackMetadata(metadata)
                MetadataItemType.ALBUM -> updateAlbumMetadata(metadata)
                MetadataItemType.PLAYLIST -> updatePlaylistMetadata(metadata)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeTrackFromAlbum(albumId: String, trackId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val virtualId = when {
                    albumId.startsWith("local_") -> ensureVirtualAlbumFromLocal(albumId)
                    else -> ensureVirtualAlbumFromSpotify(albumId)
                }
                val relation = database.virtualAlbumDao().getVirtualAlbumWithTracks(virtualId)
                    ?: return@withContext Result.failure(IllegalStateException("Album not found"))

                val localEntity = database.localTrackDao().getById(trackId)
                val localUri = localEntity?.contentUri
                val remaining = relation.mappings
                    .filter { mapping ->
                        mapping.spotifyTrackId != trackId &&
                            (localUri == null || mapping.localTrackUri != localUri)
                    }
                    .mapIndexed { index, mapping -> mapping.copy(sequencePosition = index) }

                database.virtualAlbumDao().clearMappings(virtualId)
                database.virtualAlbumDao().insertMappings(remaining)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun resetAlbumCustomizations(spotifyAlbumId: String) = withContext(Dispatchers.IO) {
        val virtual = database.virtualAlbumDao().getByOriginalSpotifyId(spotifyAlbumId)
        if (virtual != null) {
            database.virtualAlbumDao().deleteVirtualAlbum(virtual.id)
        }
        database.itemMetadataOverrideDao().deleteOverride(spotifyAlbumId, "ALBUM")
    }

    suspend fun likeTrack(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uri = "spotify:track:$trackId"
        try {
            spotifyApi.saveToLibrary(uri)
            Result.success(Unit)
        } catch (libraryError: Exception) {
            try {
                spotifyApi.saveTracks(trackId)
                Result.success(Unit)
            } catch (tracksError: Exception) {
                Result.failure(preferAuthError(libraryError, tracksError))
            }
        }
    }

    suspend fun unlikeTrack(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uri = "spotify:track:$trackId"
        try {
            spotifyApi.removeFromLibrary(uri)
            Result.success(Unit)
        } catch (libraryError: Exception) {
            try {
                spotifyApi.removeTracks(trackId)
                Result.success(Unit)
            } catch (tracksError: Exception) {
                Result.failure(preferAuthError(libraryError, tracksError))
            }
        }
    }

    private fun preferAuthError(primary: Exception, fallback: Exception): Exception {
        val primaryMsg = primary.message.orEmpty()
        if (primaryMsg.contains("403") || primaryMsg.contains("401")) return primary
        return fallback
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (playlistId == SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID) {
                return@withContext likeTrack(trackId)
            }
            try {
                spotifyApi.addTracksToPlaylist(
                    playlistId,
                    SpotifyAddTracksRequest(uris = listOf("spotify:track:$trackId"))
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getPlaylistPickerItems(): List<LibraryPlaylist> = withContext(Dispatchers.IO) {
        database.spotifyPlaylistDao().getAll()
            .filter { it.id != SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID }
            .map { playlist ->
            val override = database.itemMetadataOverrideDao().getOverrideOrNull(playlist.id, "PLAYLIST")
            LibraryPlaylist(
                id = playlist.id,
                title = override?.overrideTitle ?: playlist.name,
                owner = override?.overrideArtist ?: playlist.owner,
                artworkUrl = override?.overrideArtworkUri ?: playlist.artworkUrl,
                trackCount = playlist.trackCount,
                syncedAt = playlist.syncedAt
            )
        }
    }

    private suspend fun buildSpotifyAlbumDetail(albumId: String): AlbumDetail {
        val album = spotifyApi.getAlbum(albumId)
        val artist = album.artists?.firstOrNull()
        val albumOverride = database.itemMetadataOverrideDao().getOverrideOrNull(albumId, "ALBUM")
        val overrideDao = database.metadataOverrideDao()
        val trackIds = album.tracks?.items.orEmpty().mapNotNull { it.id }
        val likedMap = fetchLikedMap(trackIds)

        val tracks = album.tracks?.items.orEmpty().mapIndexedNotNull { index, track ->
            val trackId = track.id?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val override = overrideDao.getOverrideOrNull(trackId)
            AlbumTrackItem(
                id = trackId,
                title = override?.overrideTitle ?: track.name ?: "Unknown Track",
                artist = override?.overrideArtist
                    ?: track.artists?.firstOrNull()?.name
                    ?: artist?.name
                    ?: "Unknown",
                albumName = override?.overrideAlbum ?: album.name ?: "",
                durationMs = track.durationMs,
                trackNumber = override?.overrideTrackNumber ?: track.trackNumber ?: (index + 1),
                discNumber = override?.overrideDiscNumber ?: track.discNumber ?: 1,
                isLiked = likedMap[trackId] == true,
                artworkUrl = override?.overrideArtworkUri ?: album.images?.firstOrNull()?.url
            )
        }

        return AlbumDetail(
            id = albumId,
            title = albumOverride?.overrideTitle ?: album.name ?: "Album",
            artist = albumOverride?.overrideArtist ?: artist?.name ?: "Unknown Artist",
            artistId = artist?.id,
            artworkUrl = albumOverride?.overrideArtworkUri ?: album.images?.firstOrNull()?.url,
            releaseDate = albumOverride?.overrideReleaseDate ?: album.releaseDate,
            tracks = tracks,
            isCustomized = albumOverride != null,
            source = AlbumSource.SPOTIFY
        )
    }

    private suspend fun buildVirtualAlbumDetail(
        relation: com.example.respotapp.data.FullVirtualAlbumRelation,
        navigationAlbumId: String
    ): AlbumDetail {
        val virtual = relation.album
        val albumOverride = database.itemMetadataOverrideDao().getOverrideOrNull(navigationAlbumId, "ALBUM")
        val overrideDao = database.metadataOverrideDao()
        val spotifyTrackIds = relation.mappings.mapNotNull { it.spotifyTrackId }
        val likedMap = fetchLikedMap(spotifyTrackIds)
        val spotifyAlbum = virtual.originalSpotifyId?.let {
            try { spotifyApi.getAlbum(it) } catch (_: Exception) { null }
        }

        val tracks = relation.mappings.sortedBy { it.sequencePosition }.mapIndexed { index, mapping ->
            buildTrackFromMapping(
                mapping = mapping,
                index = index,
                overrideDao = overrideDao,
                likedMap = likedMap,
                defaultAlbumName = virtual.customTitle,
                defaultArtist = virtual.customArtist,
                defaultArtwork = virtual.customArtworkUri,
                spotifyAlbum = spotifyAlbum
            )
        }

        return AlbumDetail(
            id = navigationAlbumId,
            title = albumOverride?.overrideTitle ?: virtual.customTitle,
            artist = albumOverride?.overrideArtist ?: virtual.customArtist,
            artistId = spotifyAlbum?.artists?.firstOrNull()?.id,
            artworkUrl = albumOverride?.overrideArtworkUri ?: virtual.customArtworkUri,
            releaseDate = albumOverride?.overrideReleaseDate ?: spotifyAlbum?.releaseDate,
            tracks = tracks,
            virtualAlbumId = virtual.id,
            isCustomized = true,
            source = if (virtual.originalSpotifyId != null) AlbumSource.SPOTIFY else AlbumSource.VIRTUAL
        )
    }

    private suspend fun buildLocalAlbumDetail(albumId: String): AlbumDetail {
        val summaries = database.localTrackDao().getDistinctAlbums()
        val match = summaries.find { "local_${it.album}_${it.artist}" == albumId }
            ?: throw IllegalStateException("Local album not found")
        val albumName = match.album
        val artistName = match.artist
        val override = database.itemMetadataOverrideDao().getOverrideOrNull(albumId, "ALBUM")
        val localTracks = database.localTrackDao().getTracksInAlbum(albumName, artistName)
        val overrideDao = database.metadataOverrideDao()

        val tracks = localTracks.mapIndexed { index, entity ->
            val trackOverride = overrideDao.getOverrideOrNull(entity.id)
            AlbumTrackItem(
                id = entity.id,
                title = trackOverride?.overrideTitle ?: entity.title,
                artist = trackOverride?.overrideArtist ?: entity.artist,
                albumName = trackOverride?.overrideAlbum ?: entity.album,
                durationMs = entity.durationMs,
                trackNumber = trackOverride?.overrideTrackNumber ?: (index + 1),
                discNumber = trackOverride?.overrideDiscNumber ?: 1,
                isLocal = true,
                localUri = entity.contentUri
            )
        }

        return AlbumDetail(
            id = albumId,
            title = override?.overrideTitle ?: albumName,
            artist = override?.overrideArtist ?: artistName,
            artistId = null,
            artworkUrl = override?.overrideArtworkUri,
            releaseDate = override?.overrideReleaseDate,
            tracks = tracks,
            isCustomized = override != null,
            source = AlbumSource.LOCAL
        )
    }

    private suspend fun buildTrackFromMapping(
        mapping: TrackMappingEntity,
        index: Int,
        overrideDao: com.example.respotapp.data.MetadataOverrideDao,
        likedMap: Map<String, Boolean>,
        defaultAlbumName: String,
        defaultArtist: String,
        defaultArtwork: String?,
        spotifyAlbum: com.example.respotapp.data.SpotifyAlbum?
    ): AlbumTrackItem {
        if (mapping.localTrackUri != null) {
            val localId = mapping.localTrackUri
            val entity = database.localTrackDao().getAll().find {
                it.contentUri == localId || it.id == localId
            }
            val override = overrideDao.getOverrideOrNull(entity?.id ?: localId)
            return AlbumTrackItem(
                id = entity?.id ?: localId,
                title = override?.overrideTitle ?: entity?.title ?: "Local Track",
                artist = override?.overrideArtist ?: entity?.artist ?: defaultArtist,
                albumName = override?.overrideAlbum ?: entity?.album ?: defaultAlbumName,
                durationMs = entity?.durationMs ?: 0L,
                trackNumber = override?.overrideTrackNumber ?: (index + 1),
                discNumber = override?.overrideDiscNumber ?: 1,
                isLocal = true,
                localUri = entity?.contentUri ?: localId
            )
        }

        val spotifyTrackId = mapping.spotifyTrackId ?: "${mapping.virtualAlbumId}_$index"
        val override = overrideDao.getOverrideOrNull(spotifyTrackId)
        val spotifyTrack = spotifyAlbum?.tracks?.items?.find { it.id == spotifyTrackId }
            ?: try { spotifyApi.getTrack(spotifyTrackId) } catch (_: Exception) { null }

        return AlbumTrackItem(
            id = spotifyTrackId,
            title = override?.overrideTitle ?: spotifyTrack?.name ?: "Unknown Track",
            artist = override?.overrideArtist ?: spotifyTrack?.artists?.firstOrNull()?.name ?: defaultArtist,
            albumName = override?.overrideAlbum ?: defaultAlbumName,
            durationMs = spotifyTrack?.durationMs ?: 0L,
            trackNumber = override?.overrideTrackNumber ?: (index + 1),
            discNumber = override?.overrideDiscNumber ?: spotifyTrack?.discNumber ?: 1,
            isLiked = likedMap[spotifyTrackId] == true,
            artworkUrl = override?.overrideArtworkUri ?: defaultArtwork
        )
    }

    private suspend fun fetchLikedMap(trackIds: List<String>): Map<String, Boolean> {
        if (trackIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, Boolean>()
        trackIds.distinct().chunked(40).forEach { chunk ->
            val saved = checkTracksSavedBatch(chunk)
            chunk.zip(saved).forEach { (id, liked) -> result[id] = liked }
        }
        return result
    }

    private suspend fun checkTracksSavedBatch(trackIds: List<String>): List<Boolean> {
        val uris = trackIds.joinToString(",") { "spotify:track:$it" }
        return try {
            spotifyApi.checkLibraryContains(uris)
        } catch (_: Exception) {
            try {
                // Legacy fallback for Extended Quota apps that still expose /me/tracks/contains
                spotifyApi.checkTracksSaved(trackIds.joinToString(","))
            } catch (_: Exception) {
                trackIds.map { false }
            }
        }
    }

    private suspend fun buildLikedSongsPlaylistDetail(): PlaylistDetail {
        val override = database.itemMetadataOverrideDao().getOverrideOrNull(
            SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID,
            "PLAYLIST"
        )
        val playlistEntity = database.spotifyPlaylistDao().getAll()
            .find { it.id == SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID }
        val overrideDao = database.metadataOverrideDao()

        val allSaved = mutableListOf<com.example.respotapp.data.SpotifySavedTrack>()
        var offset = 0
        var hasMore = true
        while (hasMore) {
            val response = spotifyApi.getSavedTracks(limit = 50, offset = offset)
            allSaved.addAll(response.items)
            offset += response.limit
            hasMore = response.next != null
        }

        val trackIds = allSaved.mapNotNull { it.track?.id }
        val likedMap = trackIds.associateWith { true }

        val tracks = allSaved.mapIndexedNotNull { index, saved ->
            val track = saved.track ?: return@mapIndexedNotNull null
            val trackId = track.id?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val override = overrideDao.getOverrideOrNull(trackId)
            AlbumTrackItem(
                id = trackId,
                title = override?.overrideTitle ?: track.name ?: "Unknown Track",
                artist = override?.overrideArtist ?: track.artists?.firstOrNull()?.name ?: "Unknown",
                albumName = override?.overrideAlbum ?: track.album?.name ?: "",
                durationMs = track.durationMs,
                trackNumber = override?.overrideTrackNumber ?: track.trackNumber ?: (index + 1),
                discNumber = override?.overrideDiscNumber ?: track.discNumber ?: 1,
                isLiked = likedMap[trackId] == true,
                artworkUrl = override?.overrideArtworkUri ?: track.album?.images?.firstOrNull()?.url
            )
        }

        return PlaylistDetail(
            id = SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID,
            title = override?.overrideTitle ?: playlistEntity?.name ?: "Liked Songs",
            owner = override?.overrideArtist ?: playlistEntity?.owner ?: "Spotify",
            artworkUrl = override?.overrideArtworkUri ?: playlistEntity?.artworkUrl,
            tracks = tracks
        )
    }

    private suspend fun ensureVirtualAlbumFromSpotify(spotifyAlbumId: String): String {
        val existing = database.virtualAlbumDao().getByOriginalSpotifyId(spotifyAlbumId)
        if (existing != null) return existing.id

        val album = spotifyApi.getAlbum(spotifyAlbumId)
        val virtualId = UUID.randomUUID().toString()
        val entity = VirtualAlbumEntity(
            id = virtualId,
            originalSpotifyId = spotifyAlbumId,
            customTitle = album.name ?: "Album",
            customArtist = album.artists?.firstOrNull()?.name ?: "Unknown Artist",
            customArtworkUri = album.images?.firstOrNull()?.url
        )
        val mappings = album.tracks?.items.orEmpty().mapIndexedNotNull { index, track ->
            val trackId = track.id?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            TrackMappingEntity(
                virtualAlbumId = virtualId,
                sequencePosition = index,
                spotifyTrackId = trackId,
                localTrackUri = null
            )
        }
        database.virtualAlbumDao().cloneAndModifyAlbum(entity, mappings)
        return virtualId
    }

    private suspend fun ensureVirtualAlbumFromLocal(localAlbumId: String): String {
        val existing = database.virtualAlbumDao().getAllVirtualAlbums()
            .find { it.id == localAlbumId || it.originalSpotifyId == localAlbumId }
        if (existing != null) return existing.id

        val summaries = database.localTrackDao().getDistinctAlbums()
        val match = summaries.find { "local_${it.album}_${it.artist}" == localAlbumId }
            ?: throw IllegalStateException("Local album not found")
        val localTracks = database.localTrackDao().getTracksInAlbum(match.album, match.artist)
        val virtualId = localAlbumId
        val entity = VirtualAlbumEntity(
            id = virtualId,
            originalSpotifyId = null,
            customTitle = match.album,
            customArtist = match.artist,
            customArtworkUri = null
        )
        val mappings = localTracks.mapIndexed { index, track ->
            TrackMappingEntity(
                virtualAlbumId = virtualId,
                sequencePosition = index,
                spotifyTrackId = null,
                localTrackUri = track.contentUri
            )
        }
        database.virtualAlbumDao().cloneAndModifyAlbum(entity, mappings)
        return virtualId
    }

    private suspend fun updateTrackMetadata(metadata: EditableMetadata) {
        database.metadataOverrideDao().insertOverride(
            TrackMetadataOverrideEntity(
                trackId = metadata.itemId,
                overrideTitle = metadata.title,
                overrideArtist = metadata.artist,
                overrideAlbum = metadata.albumName,
                overrideArtworkUri = metadata.artworkUrl,
                overrideTrackNumber = metadata.trackNumber,
                overrideDiscNumber = metadata.discNumber
            )
        )

        if (metadata.isLocal && metadata.localUri != null) {
            localMetadataWriter.updateTrackMetadata(
                contentUri = metadata.localUri,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.albumName,
                trackNumber = metadata.trackNumber,
                discNumber = metadata.discNumber
            )
            val entity = database.localTrackDao().getById(metadata.itemId)
            if (entity != null) {
                database.localTrackDao().insert(
                    entity.copy(
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.albumName ?: entity.album
                    )
                )
            }
        }
    }

    private suspend fun updateAlbumMetadata(metadata: EditableMetadata) {
        if (metadata.itemId.startsWith("local_")) {
            database.itemMetadataOverrideDao().insertOverride(
                ItemMetadataOverrideEntity(
                    itemId = metadata.itemId,
                    itemType = "ALBUM",
                    overrideTitle = metadata.title,
                    overrideArtist = metadata.artist,
                    overrideArtworkUri = metadata.artworkUrl,
                    overrideReleaseDate = metadata.releaseDate?.trim()?.ifBlank { null }
                )
            )
            return
        }

        val virtualId = ensureVirtualAlbumFromSpotify(metadata.itemId)
        val relation = database.virtualAlbumDao().getVirtualAlbumWithTracks(virtualId)
        if (relation != null) {
            database.virtualAlbumDao().insertVirtualAlbum(
                relation.album.copy(
                    customTitle = metadata.title,
                    customArtist = metadata.artist,
                    customArtworkUri = metadata.artworkUrl
                )
            )
        }
        database.itemMetadataOverrideDao().insertOverride(
            ItemMetadataOverrideEntity(
                itemId = metadata.itemId,
                itemType = "ALBUM",
                overrideTitle = metadata.title,
                overrideArtist = metadata.artist,
                overrideArtworkUri = metadata.artworkUrl,
                overrideReleaseDate = metadata.releaseDate?.trim()?.ifBlank { null }
            )
        )
    }

    private suspend fun updatePlaylistMetadata(metadata: EditableMetadata) {
        database.itemMetadataOverrideDao().insertOverride(
            ItemMetadataOverrideEntity(
                itemId = metadata.itemId,
                itemType = "PLAYLIST",
                overrideTitle = metadata.title,
                overrideArtist = metadata.artist,
                overrideArtworkUri = metadata.artworkUrl
            )
        )
    }
}
