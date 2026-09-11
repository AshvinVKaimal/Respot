package com.example.respotapp.data

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File

@UnstableApi
class CacheManager private constructor(private val appContext: Context) {

    companion object {
        private const val CACHE_DIR = "audio_cache"
        private const val MIN_FREE_BYTES = 200L * 1024 * 1024

        @Volatile
        private var INSTANCE: CacheManager? = null

        fun getInstance(context: Context): CacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val databaseProvider = StandaloneDatabaseProvider(appContext)
    private val cacheDir = File(appContext.cacheDir, CACHE_DIR).apply { mkdirs() }

    val simpleCache: SimpleCache = SimpleCache(
        cacheDir,
        NoOpCacheEvictor(),
        databaseProvider
    )

    private val okHttpClient = OkHttpClient.Builder().build()
    private val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)

    fun hasSufficientStorage(): Boolean = cacheDir.usableSpace >= MIN_FREE_BYTES

    fun isCached(cacheKey: String): Boolean {
        return simpleCache.isCached(cacheKey, 0, Long.MAX_VALUE)
    }

    fun createCacheDataSourceFactory(): DataSource.Factory {
        val upstreamFactory = okHttpFactory
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return DefaultDataSource.Factory(appContext, cacheDataSourceFactory)
    }

    fun createUpstreamCacheDataSource(): CacheDataSource {
        return CacheDataSource(
            simpleCache,
            okHttpFactory.createDataSource(),
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
        )
    }

    fun clearIncompleteDownloads() {
        simpleCache.keys.forEach { key ->
            if (!simpleCache.isCached(key, 0, Long.MAX_VALUE)) {
                runCatching { simpleCache.removeResource(key) }
            }
        }
    }
}
