package com.example.respotapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.respotapp.RespotApp
import com.example.respotapp.domain.AlbumDetail
import com.example.respotapp.domain.AlbumTrackItem
import com.example.respotapp.domain.AlbumSource
import com.example.respotapp.domain.ArtistDetail
import com.example.respotapp.domain.AuthResult
import com.example.respotapp.domain.DarkModePreference
import com.example.respotapp.domain.LibraryFilter
import com.example.respotapp.domain.LibraryItem
import com.example.respotapp.domain.LibrarySort
import com.example.respotapp.domain.SearchResultItem
import com.example.respotapp.domain.SearchResultType
import com.example.respotapp.domain.EditableMetadata
import com.example.respotapp.domain.MetadataItemType
import com.example.respotapp.domain.NowPlayingLayout
import com.example.respotapp.domain.PlaylistDetail
import com.example.respotapp.domain.UiSettings
import com.example.respotapp.playback.PlayerUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isAuthenticated: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val hasClientId: Boolean = false,
    val clientIdInput: String = "",
    val displayName: String? = null,
    val libraryItems: List<LibraryItem> = emptyList(),
    val filteredLibraryItems: List<LibraryItem> = emptyList(),
    val librarySearchQuery: String = "",
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val librarySort: LibrarySort = LibrarySort.RECENTLY_ADDED,
    val isLibraryLoading: Boolean = false,
    val hasLocalAccess: Boolean = false,
    val localTrackCount: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val snackbarMessage: String? = null,
    val uiSettings: UiSettings = UiSettings(),
    val artistDetail: ArtistDetail? = null,
    val albumDetail: AlbumDetail? = null,
    val playlistDetail: PlaylistDetail? = null,
    val isDetailLoading: Boolean = false,
    val editingMetadata: EditableMetadata? = null,
    val showPlaylistPicker: Boolean = false,
    val playlistPickerTrackId: String? = null,
    val playlistPickerItems: List<com.example.respotapp.domain.LibraryPlaylist> = emptyList(),
    val playerState: PlayerUiState = PlayerUiState()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RespotApp
    private val authRepository = app.authRepository
    private val libraryRepository = app.libraryRepository
    private val homeRepository = app.homeRepository
    private val metadataRepository = app.metadataRepository
    private val settingsRepository = app.settingsRepository

    private val playbackRepository = app.playbackRepository
    private val playbackController = app.playbackController

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        _uiState.update {
            it.copy(
                isAuthenticated = authRepository.isAuthenticated(),
                hasClientId = authRepository.hasClientId()
            )
        }

        viewModelScope.launch {
            settingsRepository.uiSettings.collect { settings ->
                _uiState.update { it.copy(uiSettings = settings) }
            }
        }

        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            searchQueryFlow
                .debounce(400)
                .collect { query ->
                    val trimmed = query.trim()
                    if (trimmed.isNotBlank()) search(trimmed)
                }
        }

        viewModelScope.launch {
            playbackController.playerState.collect { player ->
                _uiState.update { it.copy(playerState = player) }
            }
        }

        if (authRepository.isAuthenticated()) {
            viewModelScope.launch {
                authRepository.getDisplayName()?.let { name ->
                    _uiState.update { it.copy(displayName = name, isAuthenticated = true) }
                }
                refreshLibrary()
            }
        }
    }

    fun onClientIdChange(value: String) {
        _uiState.update { it.copy(clientIdInput = value) }
    }

    fun saveClientId() {
        val id = _uiState.value.clientIdInput.trim()
        if (id.isBlank()) return
        authRepository.saveClientId(id)
        _uiState.update { it.copy(hasClientId = true, authError = null) }
    }

    fun handleAuthCallback(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            when (val result = authRepository.handleAuthCallback(code)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            isAuthenticated = true,
                            displayName = result.displayName,
                            authError = null
                        )
                    }
                    refreshLibrary()
                }
                is AuthResult.PremiumRequired -> {
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = "Spotify Premium is required for streaming."
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isAuthLoading = false, authError = result.message)
                    }
                }
            }
        }
    }

    fun onStoragePermissionGranted() {
        _uiState.update { it.copy(hasLocalAccess = true) }
        viewModelScope.launch {
            libraryRepository.scanLocalMedia().onSuccess { count ->
                _uiState.update { it.copy(localTrackCount = count) }
                loadLibraryItems()
            }
        }
    }

    fun onStoragePermissionDenied() {
        _uiState.update { it.copy(hasLocalAccess = false) }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLibraryLoading = true) }
            authRepository.refreshTokenIfNeeded()
            if (_uiState.value.hasLocalAccess) {
                libraryRepository.scanLocalMedia()
            }
            libraryRepository.syncSpotifyAlbums()
            libraryRepository.syncSpotifyPlaylists()
            loadLibraryItems()
            _uiState.update { it.copy(isLibraryLoading = false) }
        }
    }

    private fun presentLibraryItems(
        items: List<LibraryItem>,
        filter: LibraryFilter,
        searchQuery: String,
        sort: LibrarySort
    ): List<LibraryItem> = libraryRepository.filterAndSortLibraryItems(items, filter, searchQuery, sort)

    private suspend fun loadLibraryItems() {
        val items = libraryRepository.getLibraryItems()
        _uiState.update { state ->
            val filtered = presentLibraryItems(
                items,
                state.libraryFilter,
                state.librarySearchQuery,
                state.librarySort
            )
            state.copy(
                libraryItems = items,
                filteredLibraryItems = filtered,
                localTrackCount = if (state.hasLocalAccess) {
                    items.sumOf { item ->
                        if (item is LibraryItem.AlbumItem && item.album.source == AlbumSource.LOCAL) {
                            item.album.trackCount
                        } else 0
                    }
                } else 0
            )
        }
    }

    fun onLibrarySearchChange(query: String) {
        _uiState.update { state ->
            val filtered = presentLibraryItems(
                state.libraryItems,
                state.libraryFilter,
                query,
                state.librarySort
            )
            state.copy(librarySearchQuery = query, filteredLibraryItems = filtered)
        }
    }

    fun onLibraryFilterChange(filter: LibraryFilter) {
        _uiState.update { state ->
            val filtered = presentLibraryItems(
                state.libraryItems,
                filter,
                state.librarySearchQuery,
                state.librarySort
            )
            state.copy(libraryFilter = filter, filteredLibraryItems = filtered)
        }
    }

    fun onLibrarySortChange(sort: LibrarySort) {
        _uiState.update { state ->
            val filtered = presentLibraryItems(
                state.libraryItems,
                state.libraryFilter,
                state.librarySearchQuery,
                sort
            )
            state.copy(librarySort = sort, filteredLibraryItems = filtered)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            authRepository.refreshTokenIfNeeded()
            try {
                val results = libraryRepository.searchSpotify(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, snackbarMessage = "Search failed: ${e.message}")
                }
            }
        }
    }

    fun onSearchResultClick(item: SearchResultItem) {
        when (item.type) {
            SearchResultType.ARTIST -> loadArtistDetail(item.id)
            SearchResultType.ALBUM -> loadAlbumDetail(item.id)
            SearchResultType.TRACK -> item.albumId?.let { loadAlbumDetail(it) }
        }
    }

    fun loadArtistDetail(artistId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, artistDetail = null) }
            authRepository.refreshTokenIfNeeded()
            try {
                val detail = libraryRepository.getArtistDetail(artistId)
                _uiState.update { it.copy(artistDetail = detail, isDetailLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isDetailLoading = false, snackbarMessage = "Failed to load artist: ${e.message}")
                }
            }
        }
    }

    fun loadAlbumDetail(albumId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, albumDetail = null) }
            authRepository.refreshTokenIfNeeded()
            try {
                val detail = metadataRepository.getAlbumDetail(albumId)
                _uiState.update { it.copy(albumDetail = detail, isDetailLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isDetailLoading = false, snackbarMessage = "Failed to load album: ${e.message}")
                }
            }
        }
    }

    fun loadPlaylistDetail(playlistId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, playlistDetail = null) }
            authRepository.refreshTokenIfNeeded()
            try {
                val detail = metadataRepository.getPlaylistDetail(playlistId)
                _uiState.update { it.copy(playlistDetail = detail, isDetailLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isDetailLoading = false, snackbarMessage = "Failed to load playlist: ${e.message}")
                }
            }
        }
    }

    fun clearDetailState() {
        _uiState.update {
            it.copy(
                artistDetail = null,
                albumDetail = null,
                playlistDetail = null,
                isDetailLoading = false
            )
        }
    }

    fun openEditAlbumMetadata(album: AlbumDetail) {
        _uiState.update {
            it.copy(
                editingMetadata = EditableMetadata(
                    itemId = album.id,
                    itemType = MetadataItemType.ALBUM,
                    title = album.title,
                    artist = album.artist,
                    artworkUrl = album.artworkUrl,
                    releaseDate = album.releaseDate
                )
            )
        }
    }

    fun openEditPlaylistMetadata(playlist: PlaylistDetail) {
        _uiState.update {
            it.copy(
                editingMetadata = EditableMetadata(
                    itemId = playlist.id,
                    itemType = MetadataItemType.PLAYLIST,
                    title = playlist.title,
                    artist = playlist.owner,
                    artworkUrl = playlist.artworkUrl
                )
            )
        }
    }

    fun openEditTrackMetadata(track: AlbumTrackItem) {
        _uiState.update {
            it.copy(
                editingMetadata = EditableMetadata(
                    itemId = track.id,
                    itemType = MetadataItemType.TRACK,
                    title = track.title,
                    artist = track.artist,
                    albumName = track.albumName,
                    artworkUrl = track.artworkUrl,
                    trackNumber = track.trackNumber,
                    discNumber = track.discNumber,
                    isLocal = track.isLocal,
                    localUri = track.localUri
                )
            )
        }
    }

    fun dismissEditMetadata() {
        _uiState.update { it.copy(editingMetadata = null) }
    }

    fun saveMetadata(metadata: EditableMetadata) {
        viewModelScope.launch {
            metadataRepository.updateMetadata(metadata).onSuccess {
                when (metadata.itemType) {
                    MetadataItemType.ALBUM -> {
                        val detail = metadataRepository.getAlbumDetail(metadata.itemId)
                        _uiState.update {
                            it.copy(albumDetail = detail, editingMetadata = null, snackbarMessage = "Metadata updated")
                        }
                        loadLibraryItems()
                    }
                    MetadataItemType.PLAYLIST -> {
                        val detail = metadataRepository.getPlaylistDetail(metadata.itemId)
                        _uiState.update {
                            it.copy(playlistDetail = detail, editingMetadata = null, snackbarMessage = "Metadata updated")
                        }
                    }
                    MetadataItemType.TRACK -> {
                        val albumId = _uiState.value.albumDetail?.id
                        val playlistId = _uiState.value.playlistDetail?.id
                        val albumDetail = albumId?.let { metadataRepository.getAlbumDetail(it) }
                        val playlistDetail = playlistId?.let { metadataRepository.getPlaylistDetail(it) }
                        _uiState.update {
                            it.copy(
                                albumDetail = albumDetail ?: it.albumDetail,
                                playlistDetail = playlistDetail ?: it.playlistDetail,
                                editingMetadata = null,
                                snackbarMessage = "Metadata updated"
                            )
                        }
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(snackbarMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun removeTrackFromAlbum(album: AlbumDetail, track: AlbumTrackItem) {
        viewModelScope.launch {
            metadataRepository.removeTrackFromAlbum(album.id, track.id).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Track removed") }
                loadAlbumDetail(album.id)
            }.onFailure { e ->
                _uiState.update { it.copy(snackbarMessage = "Failed to remove: ${e.message}") }
            }
        }
    }

    fun toggleTrackLike(track: AlbumTrackItem) {
        if (track.isLocal) return
        viewModelScope.launch {
            authRepository.refreshTokenIfNeeded()
            val likedAfter = !track.isLiked

            // Optimistic UI update so the heart flips immediately
            applyLikedState(track.id, likedAfter)

            val result = if (track.isLiked) {
                metadataRepository.unlikeTrack(track.id)
            } else {
                metadataRepository.likeTrack(track.id)
            }
            result.onSuccess {
                // Reconcile with Spotify so heart state matches library contains
                val albumId = _uiState.value.albumDetail?.id
                val playlistId = _uiState.value.playlistDetail?.id
                if (albumId != null) {
                    runCatching { metadataRepository.getAlbumDetail(albumId) }
                        .onSuccess { detail -> _uiState.update { it.copy(albumDetail = detail) } }
                }
                if (playlistId != null) {
                    runCatching { metadataRepository.getPlaylistDetail(playlistId) }
                        .onSuccess { detail -> _uiState.update { it.copy(playlistDetail = detail) } }
                }
                libraryRepository.syncSpotifyPlaylists()
                loadLibraryItems()
                _uiState.update {
                    it.copy(
                        snackbarMessage = if (likedAfter) "Added to Liked Songs" else "Removed from Liked Songs"
                    )
                }
            }.onFailure { e ->
                applyLikedState(track.id, track.isLiked)
                val message = if (e.message?.contains("403") == true || e.message?.contains("401") == true) {
                    "Like failed: permission denied. Log out and sign in again to refresh Spotify permissions."
                } else {
                    "Like failed: ${e.message}"
                }
                _uiState.update { it.copy(snackbarMessage = message) }
            }
        }
    }

    private fun applyLikedState(trackId: String, isLiked: Boolean) {
        _uiState.update { state ->
            state.copy(
                albumDetail = state.albumDetail?.copy(
                    tracks = state.albumDetail.tracks.map { track ->
                        if (track.id == trackId) track.copy(isLiked = isLiked) else track
                    }
                ),
                playlistDetail = state.playlistDetail?.copy(
                    tracks = state.playlistDetail.tracks.map { track ->
                        if (track.id == trackId) track.copy(isLiked = isLiked) else track
                    }
                )
            )
        }
    }

    fun openPlaylistPicker(trackId: String) {
        viewModelScope.launch {
            val playlists = metadataRepository.getPlaylistPickerItems()
            _uiState.update {
                it.copy(
                    showPlaylistPicker = true,
                    playlistPickerTrackId = trackId,
                    playlistPickerItems = playlists
                )
            }
        }
    }

    fun dismissPlaylistPicker() {
        _uiState.update {
            it.copy(showPlaylistPicker = false, playlistPickerTrackId = null)
        }
    }

    fun addTrackToPlaylist(playlistId: String) {
        val trackId = _uiState.value.playlistPickerTrackId ?: return
        viewModelScope.launch {
            authRepository.refreshTokenIfNeeded()
            metadataRepository.addTrackToPlaylist(playlistId, trackId).onSuccess {
                _uiState.update {
                    it.copy(
                        showPlaylistPicker = false,
                        playlistPickerTrackId = null,
                        snackbarMessage = "Added to playlist"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(snackbarMessage = "Failed to add: ${e.message}") }
            }
        }
    }

    fun saveAlbum(albumId: String) {
        viewModelScope.launch {
            metadataRepository.resetAlbumCustomizations(albumId)
            libraryRepository.saveAlbumToLibrary(albumId).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Album saved to library") }
                loadLibraryItems()
                loadAlbumDetail(albumId)
            }.onFailure { e ->
                _uiState.update { it.copy(snackbarMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun playSearchTrackFromDetail(track: AlbumTrackItem) {
        viewModelScope.launch {
            authRepository.refreshTokenIfNeeded()
            if (track.isLocal && track.localUri != null) {
                _uiState.update { it.copy(snackbarMessage = "Local playback coming soon") }
                return@launch
            }
            playbackController.playSpotifyTrack(
                spotifyUri = "spotify:track:${track.id}",
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                albumTitle = track.albumName,
                artworkUrl = track.artworkUrl,
                durationMs = track.durationMs,
                onError = { message ->
                    _uiState.update { it.copy(snackbarMessage = message) }
                }
            )
            recordPlayback(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                artworkUrl = track.artworkUrl,
                durationMs = track.durationMs,
                albumId = null
            )
        }
    }

    fun playAlbumTrack(album: AlbumDetail, trackId: String) {
        viewModelScope.launch {
            authRepository.refreshTokenIfNeeded()
            val index = album.tracks.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
            val track = album.tracks.getOrNull(index)
            playbackController.playSpotifyAlbum(
                albumId = album.id,
                trackIndex = index,
                fallbackTitle = track?.title ?: album.title,
                fallbackArtist = track?.artist ?: album.artist,
                fallbackArtwork = album.artworkUrl,
                onError = { message ->
                    _uiState.update { it.copy(snackbarMessage = message) }
                }
            )
            track?.let {
                recordPlayback(
                    trackId = it.id,
                    title = it.title,
                    artist = it.artist,
                    artworkUrl = album.artworkUrl,
                    durationMs = it.durationMs,
                    albumId = album.id
                )
            }
        }
    }

    private suspend fun recordPlayback(
        trackId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        durationMs: Long,
        albumId: String? = null
    ) {
        homeRepository.recordPlay(
            trackId = trackId,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            albumId = albumId
        )
        loadLibraryItems()
    }

    fun playSearchTrack(item: SearchResultItem) {
        viewModelScope.launch {
            authRepository.refreshTokenIfNeeded()
            playbackController.playSpotifyTrack(
                spotifyUri = item.uri ?: "spotify:track:${item.id}",
                trackId = item.id,
                title = item.title,
                artist = item.subtitle,
                albumTitle = item.subtitle,
                artworkUrl = item.artworkUrl,
                durationMs = 0L,
                onError = { message ->
                    _uiState.update { it.copy(snackbarMessage = message) }
                }
            )
            recordPlayback(
                trackId = item.id,
                title = item.title,
                artist = item.subtitle,
                artworkUrl = item.artworkUrl,
                durationMs = 0L,
                albumId = item.albumId
            )
        }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun skipNext() = playbackController.skipNext()

    fun skipPrevious() = playbackController.skipPrevious()

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun showNowPlaying() = playbackController.showExpandedPlayer()

    fun hideNowPlaying() = playbackController.hideExpandedPlayer()

    fun downloadCurrentTrack() {
        val trackId = _uiState.value.playerState.trackId
        if (trackId == null) return
        viewModelScope.launch {
            playbackRepository.scheduleOfflineDownload(trackId).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Downloading for offline playback") }
            }.onFailure { e ->
                _uiState.update { it.copy(snackbarMessage = e.message ?: "Download failed") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun logout() {
        playbackController.disconnect()
        authRepository.logout()
        _uiState.update {
            MainUiState(hasClientId = authRepository.hasClientId())
        }
    }

    fun updateDarkMode(mode: DarkModePreference) {
        viewModelScope.launch { settingsRepository.updateDarkMode(mode) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDynamicColor(enabled) }
    }

    fun updateAccentColor(hex: String) {
        viewModelScope.launch {
            // Accent only applies when Material You dynamic color is off
            settingsRepository.updateDynamicColor(false)
            settingsRepository.updateAccentColor(hex)
        }
    }

    fun updateCompactLayout(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateCompactLayout(enabled) }
    }

    fun updateShowAlbumArt(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowAlbumArt(enabled) }
    }

    fun updateNowPlayingLayout(layout: NowPlayingLayout) {
        viewModelScope.launch { settingsRepository.updateNowPlayingLayout(layout) }
    }

    fun updateNowPlayingShowArtBackground(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateNowPlayingShowArtBackground(enabled) }
    }

    fun updateNowPlayingShowProgress(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateNowPlayingShowProgress(enabled) }
    }

    fun updateNowPlayingLargeArtwork(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateNowPlayingLargeArtwork(enabled) }
    }
}
