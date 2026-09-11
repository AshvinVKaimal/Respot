package com.example.respotapp.playback

import android.content.Context
import com.example.respotapp.data.TokenManager
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SpotifyRemoteController(
    private val context: Context,
    private val clientIdProvider: () -> String
) {

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var isConnecting = false
    private var pendingAction: (() -> Unit)? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect(onReady: () -> Unit, onError: (Throwable) -> Unit) {
        if (spotifyAppRemote != null) {
            onReady()
            return
        }
        if (isConnecting) {
            pendingAction = onReady
            return
        }

        val clientId = clientIdProvider()
        if (clientId.isBlank()) {
            onError(IllegalStateException("Spotify Client ID is not configured"))
            return
        }

        isConnecting = true
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(TokenManager.REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.disconnect(spotifyAppRemote)

        SpotifyAppRemote.connect(
            context,
            connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    spotifyAppRemote = appRemote
                    isConnecting = false
                    _isConnected.value = true
                    subscribeToPlayerState(appRemote)
                    onReady()
                    pendingAction?.invoke()
                    pendingAction = null
                }

                override fun onFailure(error: Throwable) {
                    isConnecting = false
                    _isConnected.value = false
                    onError(error)
                    pendingAction = null
                }
            }
        )
    }

    private fun subscribeToPlayerState(appRemote: SpotifyAppRemote) {
        playerStateSubscription?.cancel()
        playerStateSubscription = appRemote.playerApi
            .subscribeToPlayerState()
            .setEventCallback { state ->
                onPlayerStateChanged?.invoke(state)
            }
    }

    var onPlayerStateChanged: ((PlayerState) -> Unit)? = null

    fun playTrack(spotifyUri: String, onError: (Throwable) -> Unit) {
        connect(
            onReady = {
                spotifyAppRemote?.playerApi?.play(spotifyUri)
                    ?.setErrorCallback { onError(it) }
            },
            onError = onError
        )
    }

    fun playAlbumAtIndex(albumId: String, index: Int, onError: (Throwable) -> Unit) {
        val albumUri = "spotify:album:$albumId"
        connect(
            onReady = {
                spotifyAppRemote?.playerApi?.skipToIndex(albumUri, index)
                    ?.setErrorCallback { onError(it) }
            },
            onError = onError
        )
    }

    fun togglePlayPause() {
        val remote = spotifyAppRemote ?: return
        remote.playerApi.playerState
            .setResultCallback { state ->
                if (state.isPaused) {
                    remote.playerApi.resume()
                } else {
                    remote.playerApi.pause()
                }
            }
    }

    fun skipNext() {
        spotifyAppRemote?.playerApi?.skipNext()
    }

    fun skipPrevious() {
        spotifyAppRemote?.playerApi?.skipPrevious()
    }

    fun disconnect() {
        playerStateSubscription?.cancel()
        playerStateSubscription = null
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null
        _isConnected.value = false
        isConnecting = false
        pendingAction = null
    }
}
