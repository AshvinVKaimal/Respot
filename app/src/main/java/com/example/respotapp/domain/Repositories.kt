package com.example.respotapp.domain

import android.content.Context
import com.example.respotapp.BuildConfig
import com.example.respotapp.data.LocalTrackEntity
import com.example.respotapp.data.MediaStoreScanner
import com.example.respotapp.data.RespotDatabase
import com.example.respotapp.data.SpotifyAlbumEntity
import com.example.respotapp.data.SpotifyPlaylistEntity
import com.example.respotapp.data.SpotifyApi
import com.example.respotapp.data.SpotifyAuthApi
import com.example.respotapp.data.TokenManager
import com.example.respotapp.data.TrackMappingEntity
import com.example.respotapp.data.TrackMetadataOverrideEntity
import com.example.respotapp.data.VirtualAlbumEntity
import com.example.respotapp.data.createSpotifyAuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(
    private val tokenManager: TokenManager,
    private val spotifyApi: SpotifyApi
) {
    private val authApi: SpotifyAuthApi = createSpotifyAuthApi()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: Flow<AuthState> = _authState.asStateFlow()

    fun isAuthenticated(): Boolean = tokenManager.isLoggedIn()

    fun hasClientId(): Boolean = tokenManager.hasClientId(BuildConfig.SPOTIFY_CLIENT_ID)

    fun getClientId(): String = tokenManager.getEffectiveClientId(BuildConfig.SPOTIFY_CLIENT_ID)

    fun saveClientId(clientId: String) {
        tokenManager.saveClientId(clientId)
    }

    suspend fun handleAuthCallback(code: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val verifier = tokenManager.getCodeVerifier()
            if (verifier == null) return@withContext AuthResult.Error("Missing code verifier")

            val clientId = getClientId()
            if (clientId.isBlank()) return@withContext AuthResult.Error("Spotify Client ID is not configured")

            val tokenResponse = authApi.exchangeToken(
                grantType = "authorization_code",
                clientId = clientId,
                code = code,
                redirectUri = TokenManager.REDIRECT_URI,
                codeVerifier = verifier
            )
            tokenManager.saveTokens(
                tokenResponse.accessToken,
                tokenResponse.refreshToken,
                tokenResponse.expiresIn,
                tokenResponse.scope
            )
            tokenManager.clearCodeVerifier()

            val profile = spotifyApi.getCurrentUser()
            if (!isPremiumUser(profile.product, tokenResponse.scope ?: tokenManager.getGrantedScope())) {
                tokenManager.clearTokens()
                return@withContext AuthResult.PremiumRequired
            }

            _authState.value = AuthState.Authenticated(profile.displayName ?: profile.id)
            AuthResult.Success(profile.displayName ?: profile.id)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Authentication failed")
        }
    }

    suspend fun refreshTokenIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (!tokenManager.isTokenExpired()) return@withContext true
        val refreshToken = tokenManager.getRefreshToken() ?: return@withContext false
        try {
            val clientId = getClientId()
            if (clientId.isBlank()) return@withContext false

            val response = authApi.exchangeToken(
                grantType = "refresh_token",
                clientId = clientId,
                refreshToken = refreshToken
            )
            tokenManager.saveTokens(
                response.accessToken,
                response.refreshToken ?: refreshToken,
                response.expiresIn,
                response.scope
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun logout() {
        tokenManager.clearTokens()
        _authState.value = AuthState.Idle
    }

    suspend fun getDisplayName(): String? = withContext(Dispatchers.IO) {
        try {
            refreshTokenIfNeeded()
            val profile = spotifyApi.getCurrentUser()
            profile.displayName ?: profile.id
        } catch (_: Exception) {
            null
        }
    }

    private fun isPremiumUser(product: String?, grantedScope: String?): Boolean {
        if (product.equals("premium", ignoreCase = true)) return true

        val scopes = grantedScope?.split(" ").orEmpty()
        if (scopes.contains("streaming")) return true

        if (product.equals("free", ignoreCase = true) || product.equals("open", ignoreCase = true)) {
            return false
        }

        // product missing when user-read-private wasn't granted; streaming scope is the fallback signal
        return false
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data class Authenticated(val displayName: String) : AuthState()
}

sealed class AuthResult {
    data class Success(val displayName: String) : AuthResult()
    data object PremiumRequired : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class LibraryRepository(
    private val context: Context,
    private val database: RespotDatabase,
    private val spotifyApi: SpotifyApi,
    private val tokenManager: TokenManager
) {
    private val scanner = MediaStoreScanner(context)

    suspend fun scanLocalMedia(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tracks = scanner.scanLocalAudioFiles()
            val entities = tracks.map {
                LocalTrackEntity(
                    id = it.id,
                    contentUri = it.contentUri,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    durationMs = it.durationMs
                )
            }
            database.localTrackDao().clearAll()
            database.localTrackDao().insertAll(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncSpotifyAlbums(): Result<Int> = withContext(Dispatchers.IO) {
        if (!tokenManager.isLoggedIn()) return@withContext Result.failure(IllegalStateException("Not logged in"))
        try {
            val existingAlbums = database.spotifyAlbumDao().getAll().associateBy { it.id }
            var nextLibraryOrder = existingAlbums.values.maxOfOrNull { it.libraryOrder } ?: System.currentTimeMillis()
            val allAlbums = mutableListOf<SpotifyAlbumEntity>()
            var offset = 0
            var hasMore = true
            var fetchOrder = 0L
            while (hasMore) {
                val response = spotifyApi.getSavedAlbums(limit = 50, offset = offset)
                allAlbums.addAll(response.items.map { saved ->
                    val existing = existingAlbums[saved.album.id]
                    SpotifyAlbumEntity(
                        id = saved.album.id,
                        name = saved.album.name,
                        artist = saved.album.artists.firstOrNull()?.name ?: "Unknown Artist",
                        artworkUrl = saved.album.images?.firstOrNull()?.url,
                        trackCount = saved.album.tracks?.total ?: 0,
                        addedAt = saved.addedAt,
                        releaseDate = saved.album.releaseDate,
                        libraryOrder = existing?.libraryOrder ?: (nextLibraryOrder - fetchOrder++)
                    )
                })
                offset += response.limit
                hasMore = response.next != null
            }
            database.spotifyAlbumDao().clearAll()
            database.spotifyAlbumDao().insertAll(allAlbums)
            Result.success(allAlbums.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncSpotifyPlaylists(): Result<Int> = withContext(Dispatchers.IO) {
        if (!tokenManager.isLoggedIn()) return@withContext Result.failure(IllegalStateException("Not logged in"))
        try {
            val existingPlaylists = database.spotifyPlaylistDao().getAll()
                .associateBy { it.id }
            var nextLibraryOrder = existingPlaylists.values.maxOfOrNull { it.libraryOrder }
                ?: System.currentTimeMillis()
            val allPlaylists = mutableListOf<SpotifyPlaylistEntity>()
            var offset = 0
            var hasMore = true
            var fetchOrder = 0L
            while (hasMore) {
                val response = spotifyApi.getUserPlaylists(limit = 50, offset = offset)
                allPlaylists.addAll(response.items.map { playlist ->
                    val existing = existingPlaylists[playlist.id]
                    val order = existing?.libraryOrder ?: (nextLibraryOrder - fetchOrder++)
                    val trackCount = resolvePlaylistTrackCount(playlist)
                    SpotifyPlaylistEntity(
                        id = playlist.id,
                        name = playlist.name,
                        owner = playlist.owner?.displayName ?: "Spotify",
                        artworkUrl = playlist.images?.firstOrNull()?.url,
                        trackCount = trackCount,
                        syncedAt = existing?.syncedAt ?: order,
                        libraryOrder = order
                    )
                })
                offset += response.limit
                hasMore = response.next != null
            }
            database.spotifyPlaylistDao().deleteAllExcept(SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID)
            database.spotifyPlaylistDao().insertAll(allPlaylists)
            syncLikedSongsPlaylist()
            Result.success(allPlaylists.size + 1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncLikedSongsPlaylist() {
        try {
            val response = spotifyApi.getSavedTracks(limit = 1, offset = 0)
            val artworkUrl = response.items.firstOrNull()?.track?.album?.images?.firstOrNull()?.url
            val existing = database.spotifyPlaylistDao().getAll()
                .find { it.id == SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID }
            val order = existing?.libraryOrder ?: Long.MAX_VALUE
            database.spotifyPlaylistDao().insertAll(
                listOf(
                    SpotifyPlaylistEntity(
                        id = SpotifyLibraryIds.LIKED_SONGS_PLAYLIST_ID,
                        name = "Liked Songs",
                        owner = "Spotify",
                        artworkUrl = artworkUrl,
                        trackCount = response.total,
                        syncedAt = order,
                        libraryOrder = order
                    )
                )
            )
        } catch (_: Exception) {
            // Liked songs sync is optional if scopes are missing
        }
    }

    private suspend fun resolvePlaylistTrackCount(playlist: com.example.respotapp.data.SpotifyPlaylist): Int {
        val fromMeta = playlist.tracks?.total ?: 0
        if (fromMeta > 0) return fromMeta
        return try {
            spotifyApi.getPlaylistItems(playlist.id, limit = 1, offset = 0).total
        } catch (_: Exception) {
            try {
                spotifyApi.getPlaylist(playlist.id).tracks?.total ?: 0
            } catch (_: Exception) {
                0
            }
        }
    }

    suspend fun getLibraryItems(): List<LibraryItem> = withContext(Dispatchers.IO) {

        val lastPlayedByAlbum = database.listeningHistoryDao()
            .getLastPlayedByAlbum()
            .associate { it.albumId to it.playedAt }
        val itemOverrides = database.itemMetadataOverrideDao().getAll()
            .associateBy { it.itemId }
        val virtualBySpotifyId = database.virtualAlbumDao().getAllVirtualAlbums()
            .filter { it.originalSpotifyId != null }
            .associateBy { it.originalSpotifyId!! }

        val albums = getLibraryAlbums(lastPlayedByAlbum).map { album ->
            val override = itemOverrides[album.id]
            val virtual = virtualBySpotifyId[album.id]
            val merged = album.copy(
                title = override?.overrideTitle ?: virtual?.customTitle ?: album.title,
                artist = override?.overrideArtist ?: virtual?.customArtist ?: album.artist,
                artworkUrl = override?.overrideArtworkUri ?: virtual?.customArtworkUri ?: album.artworkUrl,
                releaseDate = override?.overrideReleaseDate ?: album.releaseDate
            )
            LibraryItem.AlbumItem(merged)
        }
        val playlists = database.spotifyPlaylistDao().getAll().map { playlist ->
            val override = itemOverrides[playlist.id]
            LibraryItem.PlaylistItem(
                LibraryPlaylist(
                    id = playlist.id,
                    title = override?.overrideTitle ?: playlist.name,
                    owner = override?.overrideArtist ?: playlist.owner,
                    artworkUrl = override?.overrideArtworkUri ?: playlist.artworkUrl,
                    trackCount = playlist.trackCount,
                    syncedAt = playlist.syncedAt,
                    libraryOrder = playlist.libraryOrder
                )
            )
        }
        albums + playlists
    }

    fun filterAndSortLibraryItems(
        items: List<LibraryItem>,
        filter: LibraryFilter,
        searchQuery: String,
        sort: LibrarySort
    ): List<LibraryItem> {
        val filtered = filterLibraryItems(items, filter, searchQuery)
        return sortLibraryItems(filtered, sort)
    }

    fun sortLibraryItems(items: List<LibraryItem>, sort: LibrarySort): List<LibraryItem> {
        val comparator = when (sort) {
            LibrarySort.RECENTLY_PLAYED -> compareByDescending<LibraryItem> { recentlyPlayedSortKey(it) }
                .thenBy { it.title.lowercase() }
                .thenBy { it.id }
            LibrarySort.RECENTLY_ADDED -> compareByDescending<LibraryItem> { recentlyAddedSortKey(it) }
                .thenBy { it.title.lowercase() }
                .thenBy { it.id }
            LibrarySort.ALPHABETICAL -> compareBy<LibraryItem> { it.title.lowercase() }
                .thenBy { it.id }
            LibrarySort.CREATOR_NAME -> Comparator { a, b ->
                val creatorCmp = a.subtitle.lowercase().compareTo(b.subtitle.lowercase())
                if (creatorCmp != 0) return@Comparator creatorCmp

                if (a is LibraryItem.AlbumItem && b is LibraryItem.AlbumItem) {
                    val dateCmp = releaseDateSortKey(a.album.releaseDate)
                        .compareTo(releaseDateSortKey(b.album.releaseDate))
                    if (dateCmp != 0) return@Comparator dateCmp
                }

                val titleCmp = a.title.lowercase().compareTo(b.title.lowercase())
                if (titleCmp != 0) return@Comparator titleCmp
                a.id.compareTo(b.id)
            }
        }
        return items.sortedWith(comparator)
    }

    private fun recentlyPlayedSortKey(item: LibraryItem): Long = when (item) {
        is LibraryItem.AlbumItem -> item.album.lastPlayedAt ?: 0L
        is LibraryItem.PlaylistItem -> 0L
    }

    private fun recentlyAddedSortKey(item: LibraryItem): Long = when (item) {
        is LibraryItem.AlbumItem -> {
            val addedTs = parseSpotifyTimestamp(item.album.addedAt)
            if (addedTs > 0L) addedTs else item.album.libraryOrder
        }
        is LibraryItem.PlaylistItem -> item.playlist.libraryOrder.takeIf { it > 0L }
            ?: item.playlist.syncedAt ?: 0L
    }

    fun filterLibraryItems(
        items: List<LibraryItem>,
        filter: LibraryFilter,
        searchQuery: String
    ): List<LibraryItem> {
        val q = searchQuery.trim().lowercase()
        return items.filter { item ->
            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.ALBUMS -> item is LibraryItem.AlbumItem
                LibraryFilter.PLAYLISTS -> item is LibraryItem.PlaylistItem
                LibraryFilter.LOCAL -> item is LibraryItem.AlbumItem &&
                    item.album.source == AlbumSource.LOCAL
                LibraryFilter.ARTISTS -> item is LibraryItem.AlbumItem &&
                    item.album.source == AlbumSource.SPOTIFY
            }
            val matchesSearch = q.isBlank() ||
                item.title.lowercase().contains(q) ||
                item.subtitle.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    suspend fun getLibraryAlbums(
        lastPlayedByAlbum: Map<String, Long> = emptyMap()
    ): List<LibraryAlbum> = withContext(Dispatchers.IO) {
        val spotifyAlbums = database.spotifyAlbumDao().getAll().map {
            LibraryAlbum(
                id = it.id,
                title = it.name,
                artist = it.artist,
                artworkUrl = it.artworkUrl,
                source = AlbumSource.SPOTIFY,
                trackCount = it.trackCount,
                addedAt = it.addedAt,
                lastPlayedAt = lastPlayedByAlbum[it.id],
                libraryOrder = it.libraryOrder,
                releaseDate = it.releaseDate
            )
        }

        val localSummaries = database.localTrackDao().getDistinctAlbums()
        val localAlbums = localSummaries.map { summary ->
            val count = database.localTrackDao().countTracksInAlbum(summary.album, summary.artist)
            val localId = "local_${summary.album}_${summary.artist}"
            LibraryAlbum(
                id = localId,
                title = summary.album,
                artist = summary.artist,
                artworkUrl = null,
                source = AlbumSource.LOCAL,
                trackCount = count,
                lastPlayedAt = lastPlayedByAlbum[localId]
            )
        }

        val virtualAlbums = database.virtualAlbumDao().getAllVirtualAlbums().map {
            LibraryAlbum(
                id = it.id,
                title = it.customTitle,
                artist = it.customArtist,
                artworkUrl = it.customArtworkUri,
                source = AlbumSource.VIRTUAL,
                trackCount = 0,
                lastPlayedAt = lastPlayedByAlbum[it.id]
            )
        }

        spotifyAlbums + localAlbums + virtualAlbums
    }

    private fun parseSpotifyTimestamp(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            try {
                java.time.ZonedDateTime.parse(iso).toInstant().toEpochMilli()
            } catch (_: Exception) {
                try {
                    java.time.LocalDateTime.parse(iso.substringBefore("+").substringBefore("Z"))
                        .atZone(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    0L
                }
            }
        }
    }

    suspend fun searchSpotify(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return@withContext emptyList()
        val response = spotifyApi.search(trimmedQuery)
        val results = mutableListOf<SearchResultItem>()

        response.tracks?.items?.forEach { track ->
            results.add(
                SearchResultItem(
                    id = track.id,
                    title = track.name,
                    subtitle = track.artists.firstOrNull()?.name ?: "Track",
                    artworkUrl = track.album?.images?.firstOrNull()?.url,
                    type = SearchResultType.TRACK,
                    uri = track.uri,
                    albumId = track.album?.id
                )
            )
        }
        response.albums?.items?.forEach { album ->
            results.add(
                SearchResultItem(
                    id = album.id,
                    title = album.name,
                    subtitle = album.artists.firstOrNull()?.name ?: "Album",
                    artworkUrl = album.images?.firstOrNull()?.url,
                    type = SearchResultType.ALBUM,
                    uri = "spotify:album:${album.id}"
                )
            )
        }
        response.artists?.items?.forEach { artist ->
            results.add(
                SearchResultItem(
                    id = artist.id,
                    title = artist.name,
                    subtitle = "Artist",
                    artworkUrl = artist.images?.firstOrNull()?.url,
                    type = SearchResultType.ARTIST,
                    uri = "spotify:artist:${artist.id}"
                )
            )
        }
        SearchRanking.sortResults(trimmedQuery, results)
    }

    suspend fun getArtistDetail(artistId: String): ArtistDetail = withContext(Dispatchers.IO) {
        val artist = spotifyApi.getArtist(artistId)
        val albumsResponse = spotifyApi.getArtistAlbums(artistId)
        ArtistDetail(
            id = artist.id,
            name = artist.name,
            artworkUrl = artist.images?.firstOrNull()?.url,
            genres = artist.genres.orEmpty(),
            followerCount = artist.followers?.total ?: 0,
            albums = albumsResponse.items.map { album ->
                LibraryAlbum(
                    id = album.id,
                    title = album.name,
                    artist = album.artists.firstOrNull()?.name ?: artist.name,
                    artworkUrl = album.images?.firstOrNull()?.url,
                    source = AlbumSource.SPOTIFY,
                    trackCount = album.totalTracks ?: album.tracks?.total ?: 0
                )
            }
        )
    }

    suspend fun getAlbumDetail(albumId: String): AlbumDetail = withContext(Dispatchers.IO) {
        val album = spotifyApi.getAlbum(albumId)
        val artist = album.artists.firstOrNull()
        AlbumDetail(
            id = album.id,
            title = album.name,
            artist = artist?.name ?: "Unknown Artist",
            artistId = artist?.id,
            artworkUrl = album.images?.firstOrNull()?.url,
            releaseDate = album.releaseDate,
            tracks = album.tracks?.items.orEmpty().map { track ->
                AlbumTrackItem(
                    id = track.id,
                    title = track.name,
                    artist = track.artists.firstOrNull()?.name ?: artist?.name ?: "Unknown",
                    albumName = album.name,
                    durationMs = track.durationMs,
                    trackNumber = track.trackNumber ?: 0
                )
            }
        )
    }

    suspend fun saveAlbumToLibrary(albumId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = "spotify:album:$albumId"
            try {
                spotifyApi.saveToLibrary(uri)
            } catch (_: Exception) {
                spotifyApi.saveAlbumsLegacy(albumId)
            }
            syncSpotifyAlbums()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class VmeRepository(private val database: RespotDatabase) {

    suspend fun getVirtualAlbum(albumId: String): VirtualAlbum? = withContext(Dispatchers.IO) {
        val relation = database.virtualAlbumDao().getVirtualAlbumWithTracks(albumId) ?: return@withContext null
        val overrideDao = database.metadataOverrideDao()
        VirtualAlbum(
            id = relation.album.id,
            originalSpotifyId = relation.album.originalSpotifyId,
            customTitle = relation.album.customTitle,
            customArtist = relation.album.customArtist,
            customArtworkUri = relation.album.customArtworkUri,
            tracks = relation.mappings.map { mapping ->
                val override = overrideDao.getOverride(
                    mapping.spotifyTrackId ?: mapping.localTrackUri ?: "${mapping.virtualAlbumId}_${mapping.sequencePosition}"
                )
                VirtualTrackMapping(
                    sequencePosition = mapping.sequencePosition,
                    spotifyTrackId = mapping.spotifyTrackId,
                    localTrackUri = mapping.localTrackUri,
                    isBroken = mapping.isBroken,
                    overrideTitle = override?.overrideTitle,
                    overrideArtist = override?.overrideArtist,
                    overrideArtworkUri = override?.overrideArtworkUri
                )
            }
        )
    }

    suspend fun cloneSpotifyAlbum(
        spotifyAlbumId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        trackIds: List<String>
    ): String = withContext(Dispatchers.IO) {
        val virtualId = UUID.randomUUID().toString()
        val album = VirtualAlbumEntity(
            id = virtualId,
            originalSpotifyId = spotifyAlbumId,
            customTitle = title,
            customArtist = artist,
            customArtworkUri = artworkUrl
        )
        val mappings = trackIds.mapIndexed { index, trackId ->
            TrackMappingEntity(
                virtualAlbumId = virtualId,
                sequencePosition = index,
                spotifyTrackId = trackId,
                localTrackUri = null
            )
        }
        database.virtualAlbumDao().cloneAndModifyAlbum(album, mappings)
        virtualId
    }

    suspend fun updateTrackOverride(
        trackId: String,
        title: String?,
        artist: String?,
        artworkUri: String?
    ) = withContext(Dispatchers.IO) {
        database.metadataOverrideDao().insertOverride(
            TrackMetadataOverrideEntity(
                trackId = trackId,
                overrideTitle = title,
                overrideArtist = artist,
                overrideAlbum = null,
                overrideArtworkUri = artworkUri,
                overrideTrackNumber = null,
                overrideDiscNumber = null
            )
        )
    }

    suspend fun markTrackBroken(virtualAlbumId: String, sequencePosition: Int) =
        withContext(Dispatchers.IO) {
            val relation = database.virtualAlbumDao().getVirtualAlbumWithTracks(virtualAlbumId)
            if (relation == null) return@withContext
            val updated = relation.mappings.map {
                if (it.sequencePosition == sequencePosition) it.copy(isBroken = true) else it
            }
            database.virtualAlbumDao().insertMappings(updated)
        }
}
