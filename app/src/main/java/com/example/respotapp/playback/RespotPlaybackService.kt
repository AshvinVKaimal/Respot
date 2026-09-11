package com.example.respotapp.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.respotapp.data.CacheManager

@UnstableApi
class RespotPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var cacheManager: CacheManager

    override fun onCreate() {
        super.onCreate()
        cacheManager = CacheManager.getInstance(this)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 60_000, 1_500, 3_000)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    cacheManager.createCacheDataSourceFactory()
                )
            )
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (player.hasNextMediaItem()) {
                    player.prepare()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.play()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        fun buildMediaItem(item: PlaybackQueueItem): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setAlbumTitle(item.albumTitle)
                .apply {
                    if (item.artworkUrl != null) setArtworkUri(Uri.parse(item.artworkUrl))
                }
                .build()

            return MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(item.playbackUri!!)
                .setCustomCacheKey(item.id)
                .setMediaMetadata(metadata)
                .build()
        }

        fun sessionComponent(context: Context): ComponentName {
            return ComponentName(context, RespotPlaybackService::class.java)
        }
    }
}
