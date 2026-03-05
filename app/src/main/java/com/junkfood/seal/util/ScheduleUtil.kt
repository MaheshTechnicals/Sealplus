package com.junkfood.seal.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.junkfood.seal.database.objects.ScheduledTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ScheduleUtil"

/** Work-tag applied to all scheduled-download work requests (allows bulk cancel). */
const val SCHEDULE_WORK_TAG = "seal_scheduled_download"

object ScheduleUtil {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Persist + enqueue a new scheduled download.
     *
     * @param context       Application context (used to get WorkManager).
     * @param url           URL to download.
     * @param title         Video title, for display purposes.
     * @param thumbnailUrl  Thumbnail URL, for display purposes.
     * @param preferences   Full download preferences to use when the task fires.
     * @param scheduleParams Time and network constraint from the user.
     * @param isPlaylist    Whether the URL represents a playlist.
     * @param scope         Coroutine scope to run the suspend DB calls on.
     * @return              Human-readable scheduled-time string, e.g. "10:30 AM, Mar 5"
     */
    fun scheduleDownload(
        context: Context,
        url: String,
        title: String = "",
        thumbnailUrl: String = "",
        preferences: DownloadUtil.DownloadPreferences,
        scheduleParams: ScheduleParams,
        isPlaylist: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    ): String {
        val scheduledTimeMillis = scheduleParams.scheduledTimeMillis
        val preferencesJson = json.encodeToString(preferences)

        scope.launch {
            // 1. Insert into DB to get a stable ID
            val entity =
                ScheduledTask(
                    url = url,
                    title = title,
                    thumbnailUrl = thumbnailUrl,
                    scheduledTimeMillis = scheduledTimeMillis,
                    networkPreference = scheduleParams.networkPreference.id,
                    isPlaylist = isPlaylist,
                    preferencesJson = preferencesJson,
                )
            val rowId = DatabaseUtil.insertScheduledTask(entity)
            val taskId = rowId.toInt()

            // 2. Build WorkManager constraints
            val networkType =
                when (scheduleParams.networkPreference) {
                    ScheduleNetworkPreference.WIFI_ONLY -> NetworkType.UNMETERED
                    ScheduleNetworkPreference.MOBILE_DATA -> NetworkType.METERED
                    ScheduleNetworkPreference.BOTH -> NetworkType.CONNECTED
                }
            val constraints = Constraints.Builder().setRequiredNetworkType(networkType).build()

            // 3. Calculate initial delay
            val delayMs =
                (scheduledTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)

            // 4. Build input data
            val inputData =
                Data.Builder()
                    .putInt(ScheduledDownloadWorker.KEY_TASK_ID, taskId)
                    .build()

            // 5. Create one-time work request
            val workRequest =
                OneTimeWorkRequest.Builder(ScheduledDownloadWorker::class.java)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .addTag(SCHEDULE_WORK_TAG)
                    .build()

            // 6. Enqueue
            WorkManager.getInstance(context).enqueue(workRequest)

            // 7. Persist the work-request UUID so we can cancel it later
            DatabaseUtil.updateScheduledTaskWorkerId(taskId, workRequest.id.toString())

            Log.d(TAG, "scheduleDownload: taskId=$taskId, delay=${delayMs}ms, workId=${workRequest.id}")
        }

        return formatScheduledTime(scheduledTimeMillis)
    }

    /**
     * Cancel and delete a single scheduled task.
     *
     * @param context Application context.
     * @param task    The [ScheduledTask] to cancel.
     * @param scope   Coroutine scope.
     */
    fun cancelScheduledTask(
        context: Context,
        task: ScheduledTask,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    ) {
        scope.launch {
            // Cancel in WorkManager if we have a valid ID
            if (task.workRequestId.isNotBlank()) {
                try {
                    WorkManager.getInstance(context)
                        .cancelWorkById(java.util.UUID.fromString(task.workRequestId))
                } catch (e: Exception) {
                    Log.w(TAG, "cancelScheduledTask: could not cancel work ${task.workRequestId}", e)
                }
            }
            DatabaseUtil.deleteScheduledTask(task)
        }
    }

    /** Returns a locale-aware, human-readable date+time string. */
    fun formatScheduledTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a, MMM d", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }
}
