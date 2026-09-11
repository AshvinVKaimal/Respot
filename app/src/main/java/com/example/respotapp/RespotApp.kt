package com.example.respotapp

import android.app.Application
import com.example.respotapp.data.CacheManager
import com.example.respotapp.data.RespotDatabase
import com.example.respotapp.data.TokenManager
import com.example.respotapp.data.createSpotifyApi
import com.example.respotapp.domain.AuthRepository
import com.example.respotapp.domain.LibraryRepository
import com.example.respotapp.domain.PlaybackRepository
import com.example.respotapp.domain.SettingsRepository
import com.example.respotapp.domain.HomeRepository
import com.example.respotapp.domain.MetadataRepository
import com.example.respotapp.domain.VmeRepository
import com.example.respotapp.playback.PlaybackController

class RespotApp : Application() {

    lateinit var database: RespotDatabase
    lateinit var tokenManager: TokenManager
    lateinit var cacheManager: CacheManager
    lateinit var authRepository: AuthRepository
    lateinit var libraryRepository: LibraryRepository
    lateinit var homeRepository: HomeRepository
    lateinit var playbackRepository: PlaybackRepository
    lateinit var metadataRepository: MetadataRepository
    lateinit var vmeRepository: VmeRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var playbackController: PlaybackController

    override fun onCreate() {
        super.onCreate()
        database = RespotDatabase.getInstance(this)
        tokenManager = TokenManager(this)
        cacheManager = CacheManager.getInstance(this)
        val spotifyApi = createSpotifyApi(tokenManager)
        authRepository = AuthRepository(tokenManager, spotifyApi)
        libraryRepository = LibraryRepository(
            context = this,
            database = database,
            spotifyApi = spotifyApi,
            tokenManager = tokenManager
        )
        homeRepository = HomeRepository(database, libraryRepository)
        metadataRepository = MetadataRepository(
            context = this,
            database = database,
            spotifyApi = spotifyApi
        )
        playbackRepository = PlaybackRepository(
            context = this,
            spotifyApi = spotifyApi,
            database = database,
            cacheManager = cacheManager
        )
        vmeRepository = VmeRepository(database)
        settingsRepository = SettingsRepository(this)
        playbackController = PlaybackController(this) { authRepository.getClientId() }
    }
}
