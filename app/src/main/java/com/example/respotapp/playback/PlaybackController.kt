package com.example.respotapp.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.spotify.protocol.types.PlayerState as SpotifyPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException

class PlaybackController(
    private val context: Context,
    clientIdProvider: () -> String
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaController: MediaController? = null
    private val spotifyRemote = SpotifyRemoteController(context, clientIdProvider)

    private var activeEngine: PlaybackEngine? = null
    private var pendingArtworkUrl: String? = null

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private val exoListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = syncFromExoPlayer()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncFromExoPlayer()
        override fun onPlaybackStateChanged(playbackState: Int) = syncFromExoPlayer()
    }

    private var positionJob: Job? = null

    init {
        spotifyRemote.onPlayerStateChanged = { state -> syncFromSpotifyPlayer(state) }
    }

    fun connect() {
        if (mediaController != null) return
        val sessionToken = SessionToken(context, RespotPlaybackService.sessionComponent(context))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                mediaController?.addListener(exoListener)
                syncFromExoPlayer()
                startExoPositionUpdates()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }, context.mainExecutor)
    }

    fun disconnect() {
        positionJob?.cancel()
        mediaController?.removeListener(exoListener)
        mediaController?.release()
        mediaController = null
        spotifyRemote.disconnect()
        activeEngine = null
        _playerState.value = PlayerUiState()
    }

    fun playSpotifyAlbum(
        albumId: String,
        trackIndex: Int,
        fallbackTitle: String,
        fallbackArtist: String,
        fallbackArtwork: String?,
        onError: (String) -> Unit
    ) {
        connect()
        pauseExoPlayer()
        pendingArtworkUrl = fallbackArtwork
        _playerState.update {
            it.copy(
                isActive = true,
                playbackEngine = PlaybackEngine.SPOTIFY_REMOTE,
                title = fallbackTitle,
                artist = fallbackArtist,
                artworkUrl = fallbackArtwork,
                isPlaying = true
            )
        }
        activeEngine = PlaybackEngine.SPOTIFY_REMOTE

        spotifyRemote.playAlbumAtIndex(
            albumId = albumId,
            index = trackIndex,
            onError = { error ->
                onError(error.message ?: "Failed to connect to Spotify")
            }
        )
    }

    fun playSpotifyTrack(
        spotifyUri: String,
        trackId: String,
        title: String,
        artist: String,
        albumTitle: String,
        artworkUrl: String?,
        durationMs: Long,
        onError: (String) -> Unit
    ) {
        connect()
        pauseExoPlayer()
        pendingArtworkUrl = artworkUrl
        activeEngine = PlaybackEngine.SPOTIFY_REMOTE
        _playerState.update {
            it.copy(
                isActive = true,
                trackId = trackId,
                title = title,
                artist = artist,
                albumTitle = albumTitle,
                artworkUrl = artworkUrl,
                durationMs = durationMs,
                isPlaying = true,
                playbackEngine = PlaybackEngine.SPOTIFY_REMOTE
            )
        }
        spotifyRemote.playTrack(
            spotifyUri = spotifyUri,
            onError = { error ->
                onError(error.message ?: "Failed to connect to Spotify. Is the Spotify app installed?")
            }
        )
    }

    fun playLocalQueue(items: List<PlaybackQueueItem>, startIndex: Int = 0) {
        spotifyRemote.disconnect()
        playExoQueue(items, startIndex)
    }

    private fun playExoQueue(items: List<PlaybackQueueItem>, startIndex: Int) {
        val controller = mediaController ?: return
        val playable = items.filter { it.playbackUri?.isNotBlank() == true }
        if (playable.isEmpty()) return

        activeEngine = PlaybackEngine.EXOPLAYER
        val mediaItems = playable.map { RespotPlaybackService.buildMediaItem(it) }
        val index = startIndex.coerceIn(0, mediaItems.lastIndex)
        controller.setMediaItems(mediaItems, index, 0L)
        controller.prepare()
        controller.play()
        syncFromExoPlayer()
    }

    private fun pauseExoPlayer() {
        mediaController?.pause()
        mediaController?.clearMediaItems()
    }

    fun togglePlayPause() {
        when (activeEngine) {
            PlaybackEngine.SPOTIFY_REMOTE -> spotifyRemote.togglePlayPause()
            PlaybackEngine.EXOPLAYER -> {
                val controller = mediaController ?: return
                if (controller.isPlaying) controller.pause() else controller.play()
            }
            null -> {}
        }
    }

    fun skipNext() {
        when (activeEngine) {
            PlaybackEngine.SPOTIFY_REMOTE -> spotifyRemote.skipNext()
            PlaybackEngine.EXOPLAYER -> mediaController?.seekToNextMediaItem()
            null -> {}
        }
    }

    fun skipPrevious() {
        when (activeEngine) {
            PlaybackEngine.SPOTIFY_REMOTE -> spotifyRemote.skipPrevious()
            PlaybackEngine.EXOPLAYER -> mediaController?.seekToPreviousMediaItem()
            null -> {}
        }
    }

    fun seekTo(positionMs: Long) {
        if (activeEngine == PlaybackEngine.EXOPLAYER) {
            mediaController?.seekTo(positionMs)
        }
    }

    fun showExpandedPlayer() {
        _playerState.update { it.copy(showExpandedPlayer = true) }
    }

    fun hideExpandedPlayer() {
        _playerState.update { it.copy(showExpandedPlayer = false) }
    }

    private fun syncFromExoPlayer() {
        if (activeEngine != PlaybackEngine.EXOPLAYER) return
        val controller = mediaController
        if (controller == null || controller.mediaItemCount == 0) {
            _playerState.update { PlayerUiState() }
            activeEngine = null
            return
        }

        val metadata = controller.mediaMetadata
        _playerState.update {
            it.copy(
                isActive = true,
                trackId = controller.currentMediaItem?.mediaId,
                title = metadata.title?.toString() ?: "",
                artist = metadata.artist?.toString() ?: "",
                albumTitle = metadata.albumTitle?.toString() ?: "",
                artworkUrl = metadata.artworkUri?.toString(),
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition,
                durationMs = controller.duration.coerceAtLeast(0L),
                playbackEngine = PlaybackEngine.EXOPLAYER
            )
        }
    }

    private fun syncFromSpotifyPlayer(state: SpotifyPlayerState) {
        if (activeEngine != PlaybackEngine.SPOTIFY_REMOTE) return
        val track = state.track
        val trackId = track.uri?.substringAfterLast(":")

        _playerState.update {
            it.copy(
                isActive = true,
                trackId = trackId,
                title = track.name ?: it.title,
                artist = track.artist.name ?: it.artist,
                albumTitle = track.album.name ?: it.albumTitle,
                artworkUrl = pendingArtworkUrl ?: it.artworkUrl,
                isPlaying = !state.isPaused,
                positionMs = state.playbackPosition,
                durationMs = track.duration,
                playbackEngine = PlaybackEngine.SPOTIFY_REMOTE
            )
        }
    }

    private fun startExoPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                if (activeEngine == PlaybackEngine.EXOPLAYER) {
                    val controller = mediaController
                    if (controller != null && controller.isPlaying) {
                        _playerState.update {
                            it.copy(
                                positionMs = controller.currentPosition,
                                durationMs = controller.duration.coerceAtLeast(0L),
                                isPlaying = controller.isPlaying,
                                trackId = controller.currentMediaItem?.mediaId
                            )
                        }
                    }
                }
                delay(500)
            }
        }
    }
}
