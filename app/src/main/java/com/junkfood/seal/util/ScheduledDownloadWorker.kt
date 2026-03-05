package com.junkfood.seal.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WorkManager worker that executes a previously-scheduled download.
 *
 * Input data key: [KEY_TASK_ID] — the primary key of the [com.junkfood.seal.database.objects.ScheduledTask]
 * row to execute.
 */
class ScheduledDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val downloader: DownloaderV2 by inject()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val taskId = inputData.getInt(KEY_TASK_ID, -1)
            if (taskId == -1) {
                Log.e(TAG, "doWork: no task_id in input data")
                return@withContext Result.failure()
            }

            val scheduledTask = DatabaseUtil.getScheduledTaskById(taskId)
            if (scheduledTask == null) {
                Log.w(TAG, "doWork: ScheduledTask $taskId not found (already deleted?)")
                return@withContext Result.success() // already gone — treat as success
            }

            return@withContext try {
                val preferences =
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString<DownloadUtil.DownloadPreferences>(
                            scheduledTask.preferencesJson
                        )

                val task = Task(url = scheduledTask.url, preferences = preferences)
                downloader.enqueue(task)

                // Remove from scheduled-tasks table now that the download is queued
                DatabaseUtil.deleteScheduledTaskById(taskId)

                Log.d(TAG, "doWork: enqueued download for '${scheduledTask.url}'")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "doWork: failed to enqueue download for task $taskId", e)
                Result.retry()
            }
        }

    companion object {
        const val KEY_TASK_ID = "task_id"
        private const val TAG = "ScheduledDownloadWorker"
    }
}
