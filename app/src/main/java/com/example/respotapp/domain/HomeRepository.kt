package com.example.respotapp.domain

import com.example.respotapp.data.HomeLayoutEntity
import com.example.respotapp.data.ListeningHistoryEntity
import com.example.respotapp.data.RespotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecentPlayItem(
    val trackId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val playedAt: Long
)

data class ListeningStats(
    val totalPlays: Int,
    val uniqueTracks: Int,
    val hoursListened: Float
)

data class HomeDashboardData(
    val modules: List<HomeModule>,
    val recentlyPlayed: List<RecentPlayItem>,
    val virtualAlbums: List<LibraryAlbum>,
    val playlists: List<LibraryPlaylist>,
    val localAlbums: List<LibraryAlbum>,
    val savedAlbums: List<LibraryAlbum>,
    val stats: ListeningStats
)

class HomeRepository(
    private val database: RespotDatabase,
    private val libraryRepository: LibraryRepository
) {

    suspend fun ensureDefaultLayout() = withContext(Dispatchers.IO) {
        val existing = database.homeLayoutDao().getAll()
        if (existing.isNotEmpty()) return@withContext

        val defaults = HomeModuleType.entries.mapIndexed { index, type ->
            HomeLayoutEntity(
                moduleType = type.name,
                orderIndex = index,
                isVisible = true
            )
        }
        database.homeLayoutDao().insertAll(defaults)
    }

    suspend fun getLayoutModules(): List<HomeModule> = withContext(Dispatchers.IO) {
        database.homeLayoutDao().getAll().map { entity ->
            HomeModule(
                type = HomeModuleType.valueOf(entity.moduleType),
                orderIndex = entity.orderIndex,
                isVisible = entity.isVisible
            )
        }.sortedBy { it.orderIndex }
    }

    suspend fun updateLayout(modules: List<HomeModule>) = withContext(Dispatchers.IO) {
        val entities = modules.map {
            HomeLayoutEntity(
                moduleType = it.type.name,
                orderIndex = it.orderIndex,
                isVisible = it.isVisible
            )
        }
        database.homeLayoutDao().insertAll(entities)
    }

    suspend fun recordPlay(
        trackId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        durationMs: Long,
        albumId: String? = null
    ) = withContext(Dispatchers.IO) {
        database.listeningHistoryDao().insert(
            ListeningHistoryEntity(
                trackId = trackId,
                albumId = albumId,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                durationMs = durationMs
            )
        )
    }

    suspend fun loadDashboard(): HomeDashboardData = withContext(Dispatchers.IO) {
        ensureDefaultLayout()
        val modules = getLayoutModules()
        val libraryItems = libraryRepository.getLibraryItems()

        val albums = libraryItems.filterIsInstance<LibraryItem.AlbumItem>().map { it.album }
        val playlists = libraryItems.filterIsInstance<LibraryItem.PlaylistItem>().map { it.playlist }

        val recent = database.listeningHistoryDao().getRecent(12).map { entity ->
            RecentPlayItem(
                trackId = entity.trackId,
                title = entity.title,
                artist = entity.artist,
                artworkUrl = entity.artworkUrl,
                playedAt = entity.playedAt
            )
        }

        val totalMs = database.listeningHistoryDao().getTotalDurationMs() ?: 0L

        HomeDashboardData(
            modules = modules,
            recentlyPlayed = recent,
            virtualAlbums = albums.filter { it.source == AlbumSource.VIRTUAL },
            playlists = playlists,
            localAlbums = albums.filter { it.source == AlbumSource.LOCAL },
            savedAlbums = albums.filter { it.source == AlbumSource.SPOTIFY },
            stats = ListeningStats(
                totalPlays = database.listeningHistoryDao().getTotalPlays(),
                uniqueTracks = database.listeningHistoryDao().getUniqueTracks(),
                hoursListened = totalMs / 3_600_000f
            )
        )
    }
}
