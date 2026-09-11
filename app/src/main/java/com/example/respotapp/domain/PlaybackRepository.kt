package com.example.respotapp.domain

import com.example.respotapp.data.CacheManager
import com.example.respotapp.data.LocalTrackEntity
import com.example.respotapp.data.OfflineSyncScheduler
import com.example.respotapp.data.RespotDatabase
import com.example.respotapp.data.SpotifyApi
import com.example.respotapp.playback.PlaybackQueueItem
import com.example.respotapp.playback.PlaybackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackRepository(
    private val context: android.content.Context,
    private val spotifyApi: SpotifyApi,
    private val database: RespotDatabase,
    private val cacheManager: CacheManager
) {

    suspend fun buildQueueFromAlbum(album: AlbumDetail): List<PlaybackQueueItem> =
        withContext(Dispatchers.IO) {
            album.tracks.map { track ->
                buildSpotifyQueueItem(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    albumTitle = album.title,
                    artworkUrl = album.artworkUrl,
                    albumId = album.id,
                    durationMs = track.durationMs
                )
            }
        }

    suspend fun buildQueueFromSearchTrack(
        trackId: String,
        title: String,
        artist: String,
        albumTitle: String,
        artworkUrl: String?,
        albumId: String?,
        durationMs: Long
    ): PlaybackQueueItem = withContext(Dispatchers.IO) {
        buildSpotifyQueueItem(trackId, title, artist, albumTitle, artworkUrl, albumId, durationMs)
    }

    private suspend fun buildSpotifyQueueItem(
        trackId: String,
        title: String,
        artist: String,
        albumTitle: String,
        artworkUrl: String?,
        albumId: String?,
        durationMs: Long
    ): PlaybackQueueItem {
        val source = if (cacheManager.isCached(trackId)) {
            PlaybackSource.CACHED
        } else {
            PlaybackSource.SPOTIFY_STREAM
        }

        val previewUrl = runCatching { spotifyApi.getTrack(trackId).previewUrl }.getOrNull()

        return PlaybackQueueItem(
            id = trackId,
            title = title,
            artist = artist,
            albumTitle = albumTitle,
            artworkUrl = artworkUrl,
            source = source,
            playbackUri = previewUrl,
            spotifyUri = "spotify:track:$trackId",
            albumId = albumId,
            durationMs = durationMs
        )
    }

    suspend fun scheduleOfflineDownload(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!cacheManager.hasSufficientStorage()) {
            cacheManager.clearIncompleteDownloads()
            return@withContext Result.failure(IllegalStateException("Low storage space"))
        }
        val streamUrl = runCatching { spotifyApi.getTrack(trackId).previewUrl }.getOrNull()
        if (streamUrl.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No downloadable preview available for this track")
            )
        }
        OfflineSyncScheduler.enqueueDownload(context, trackId, streamUrl)
        Result.success(Unit)
    }

    suspend fun buildLocalQueue(albumTitle: String, artist: String): List<PlaybackQueueItem> =
        withContext(Dispatchers.IO) {
            database.localTrackDao().getAll()
                .filter { it.album == albumTitle && it.artist == artist }
                .map { entity -> localEntityToQueueItem(entity) }
        }

    private fun localEntityToQueueItem(entity: LocalTrackEntity): PlaybackQueueItem {
        return PlaybackQueueItem(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            albumTitle = entity.album,
            artworkUrl = null,
            source = PlaybackSource.LOCAL,
            playbackUri = entity.contentUri,
            spotifyUri = null,
            albumId = null,
            durationMs = entity.durationMs
        )
    }
}
