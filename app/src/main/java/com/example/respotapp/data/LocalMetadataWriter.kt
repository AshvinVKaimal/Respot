package com.example.respotapp.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class LocalMetadataWriter(private val context: Context) {

    fun updateTrackMetadata(
        contentUri: String,
        title: String?,
        artist: String?,
        album: String?,
        trackNumber: Int?,
        discNumber: Int?
    ): Result<Unit> {
        return try {
            val uri = Uri.parse(contentUri)
            val values = ContentValues()
            title?.let { values.put(MediaStore.Audio.Media.TITLE, it) }
            artist?.let { values.put(MediaStore.Audio.Media.ARTIST, it) }
            album?.let { values.put(MediaStore.Audio.Media.ALBUM, it) }
            trackNumber?.let { values.put(MediaStore.Audio.Media.TRACK, it) }
            discNumber?.let { values.put(MediaStore.Audio.Media.DISC_NUMBER, it) }

            if (values.size() == 0) return Result.success(Unit)

            val updated = context.contentResolver.update(uri, values, null, null)
            if (updated > 0) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Could not update file metadata"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateLocalTrackEntity(
        trackId: String,
        title: String?,
        artist: String?,
        album: String?
    ): LocalTrackEntity? {
        val numericId = trackId.removePrefix("local_")
        return try {
            val uri = Uri.withAppendedPath(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                numericId
            )
            val values = ContentValues()
            title?.let { values.put(MediaStore.Audio.Media.TITLE, it) }
            artist?.let { values.put(MediaStore.Audio.Media.ARTIST, it) }
            album?.let { values.put(MediaStore.Audio.Media.ALBUM, it) }
            if (values.size() > 0) {
                context.contentResolver.update(uri, values, null, null)
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
