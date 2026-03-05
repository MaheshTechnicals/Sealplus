package com.junkfood.seal

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

/**
 * Short-lived foreground service that is started by [DownloadAlarmReceiver] when an
 * [android.app.AlarmManager] alarm fires for a scheduled download.
 *
 * Why a foreground service?
 * ─────────────────────────
 * On Android 12+ (API 31+) apps cannot start a background service while they are
 * in the background. However, alarms fired by [android.app.AlarmManager.setAlarmClock]
 * (or [android.app.AlarmManager.setExactAndAllowWhileIdle]) grant a temporary
 * **foreground-service start exemption** that lets us call [startForegroundService].
 * We must call [startForeground] within ~5 s of [onStartCommand] to avoid an ANR.
 *
 * What does it do?
 * ─────────────────
 * 1. Calls [startForeground] immediately so the OS considers the app active.
 * 2. Fetches the [com.junkfood.seal.database.objects.ScheduledTask] from Room.
 * 3. Enqueues a [Task] into [DownloaderV2] (the real download runs in that engine).
 * 4. Deletes the task from the DB (it has been handed off to the downloader).
 * 5. Calls [stopForeground] + [stopSelf] — the service exits immediately after queuing.
 *
 * Android 16 (API 36) compliance
 * ────────────────────────────────
 * The service is declared with [android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC]
 * in AndroidManifest.xml, and [android.permission.FOREGROUND_SERVICE_DATA_SYNC] is declared
 * in the manifest alongside [android.permission.FOREGROUND_SERVICE].
 */
class ScheduledDownloadService : Service() {

    private val downloader: DownloaderV2 by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getIntExtra(EXTRA_TASK_ID, INVALID_ID) ?: INVALID_ID

        // ── Step 1: Satisfy the OS — call startForeground immediately ────────────
        val showAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationUtil.makeScheduledServiceNotification(showAppIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ — supply the foreground service type declared in the manifest.
            // On API 34+ (Android 14) the type MUST match what is in the manifest;
            // FOREGROUND_SERVICE_TYPE_DATA_SYNC is the correct type for downloads.
            startForeground(
                NotificationUtil.SCHEDULED_SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NotificationUtil.SCHEDULED_SERVICE_NOTIFICATION_ID, notification)
        }

        if (taskId == INVALID_ID) {
            Log.e(TAG, "onStartCommand: no task ID in intent — stopping immediately")
            shutDown()
            return START_NOT_STICKY
        }

        // ── Step 2-5: Look up the task, enqueue it, then exit ───────────────────
        serviceScope.launch {
            try {
                val scheduledTask = DatabaseUtil.getScheduledTaskById(taskId)
                if (scheduledTask == null) {
                    Log.w(TAG, "Task $taskId not found in DB (already cancelled?) — stopping")
                    return@launch
                }

                val preferences =
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString<DownloadUtil.DownloadPreferences>(
                            scheduledTask.preferencesJson
                        )

                val task = Task(url = scheduledTask.url, preferences = preferences)
                downloader.enqueue(task)

                // Remove from DB — the download engine has taken ownership
                DatabaseUtil.deleteScheduledTaskById(taskId)

                Log.d(TAG, "Task $taskId enqueued successfully for '${scheduledTask.url}'")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue task $taskId", e)
            } finally {
                shutDown()
            }
        }

        return START_NOT_STICKY
    }

    /** Stops the foreground state and terminates the service. */
    private fun shutDown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        /** Intent extra key carrying the [com.junkfood.seal.database.objects.ScheduledTask] ID. */
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val INVALID_ID = -1
        private const val TAG = "ScheduledDownloadService"
    }
}
