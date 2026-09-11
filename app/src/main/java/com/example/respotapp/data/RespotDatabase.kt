package com.example.respotapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalTrackEntity::class,
        SpotifyAlbumEntity::class,
        SpotifyPlaylistEntity::class,
        VirtualAlbumEntity::class,
        TrackMappingEntity::class,
        TrackMetadataOverrideEntity::class,
        ItemMetadataOverrideEntity::class,
        HomeLayoutEntity::class,
        ListeningHistoryEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class RespotDatabase : RoomDatabase() {
    abstract fun localTrackDao(): LocalTrackDao
    abstract fun spotifyAlbumDao(): SpotifyAlbumDao
    abstract fun spotifyPlaylistDao(): SpotifyPlaylistDao
    abstract fun virtualAlbumDao(): VirtualAlbumDao
    abstract fun metadataOverrideDao(): MetadataOverrideDao
    abstract fun itemMetadataOverrideDao(): ItemMetadataOverrideDao
    abstract fun homeLayoutDao(): HomeLayoutDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: RespotDatabase? = null

        fun getInstance(context: Context): RespotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RespotDatabase::class.java,
                    "respot_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
