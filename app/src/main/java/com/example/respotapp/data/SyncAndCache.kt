package com.example.respotapp.data

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.example.respotapp.RespotApp

@UnstableApi
class OfflineSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_STREAM_URL = "KEY_STREAM_URL"
        const val KEY_TRACK_ID = "KEY_TRACK_ID"
        const val WORK_NAME_PREFIX = "offline_sync_"
    }

    override suspend fun doWork(): Result {
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return Result.failure()
        val trackId = inputData.getString(KEY_TRACK_ID) ?: return Result.failure()

        val app = applicationContext as RespotApp
        val cacheManager = app.cacheManager

        if (!cacheManager.hasSufficientStorage()) {
            cacheManager.clearIncompleteDownloads()
            return Result.failure()
        }

        return try {
            val cacheDataSource = cacheManager.createUpstreamCacheDataSource()
            val dataSpec = DataSpec.Builder()
                .setUri(Uri.parse(streamUrl))
                .setKey(trackId)
                .setLength(C.LENGTH_UNSET.toLong())
                .build()

            cacheDataSource.open(dataSpec)
            val buffer = ByteArray(131072)
            while (true) {
                if (isStopped) {
                    cacheDataSource.close()
                    return Result.failure()
                }
                val read = cacheDataSource.read(buffer, 0, buffer.size)
                if (read == C.RESULT_END_OF_INPUT) break
            }
            cacheDataSource.close()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object OfflineSyncScheduler {

    fun enqueueDownload(context: Context, trackId: String, streamUrl: String) {
        val app = context.applicationContext as RespotApp
        if (!app.cacheManager.hasSufficientStorage()) return

        val request = androidx.work.OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setInputData(
                androidx.work.workDataOf(
                    OfflineSyncWorker.KEY_TRACK_ID to trackId,
                    OfflineSyncWorker.KEY_STREAM_URL to streamUrl
                )
            )
            .addTag(OfflineSyncWorker.WORK_NAME_PREFIX + trackId)
            .build()

        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork(
                OfflineSyncWorker.WORK_NAME_PREFIX + trackId,
                androidx.work.ExistingWorkPolicy.KEEP,
                request
            )
    }
}
