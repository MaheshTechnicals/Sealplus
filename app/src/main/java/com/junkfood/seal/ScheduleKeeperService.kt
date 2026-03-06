package com.junkfood.seal

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

/**
 * A long-running foreground service that keeps the app process alive from the moment the
 * user schedules a download until the scheduled moment arrives and the download fires.
 *
 * ## Why this exists
 * On aggressive OEM builds (MIUI, ColorOS, One UI, etc.) a swipe-to-dismiss in the Recents
 * list is treated as a **Force Stop**, wiping all [android.app.AlarmManager] alarms.
 * `ScheduleKeeperService` is an alternative approach:
 * instead of relying on the OS to wake the app at the right time, **we keep the process
 * alive ourselves** via a persistent foreground notification. When the coroutine's
 * `delay()` wakes up at the scheduled instant, the download fires immediately — no
 * AlarmManager dependency.
 *
 * ## Battery trade-off
 * A foreground service with [android.app.NotificationManager.IMPORTANCE_LOW] is basically
 * free in terms of CPU, but Android will not put the process in Doze buckets as aggressively.
 * This is more reliable at the cost of slightly higher battery usage. The user is informed
 * of this trade-off in the UI before they opt into "Reliability Mode".
 *
 * ## Lifecycle
 * 1. [onStartCommand] — show persistent "Waiting…" notification, start coroutine delay.
 * 2. Delay elapses → update notification to "Downloading…" → enqueue task into [DownloaderV2].
 * 3. [DownloaderV2] takes ownership; this service calls [stopForeground] + [stopSelf].
 * 4. [onTaskRemoved] — if the user swipes the app AFTER the service started (edge case),
 *    we reschedule a fresh start so the download still fires.
 *
 * ## Android 16 compliance
 * Declared with [android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC] in
 * the manifest and requires [android.permission.FOREGROUND_SERVICE_DATA_SYNC].
 */
class ScheduleKeeperService : Service() {

    private val downloader: DownloaderV2 by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Stored in instance fields so onTaskRemoved can reconstruct the restart intent
    // even when rootIntent (the removed activity's intent) doesn't carry our extras.
    private var lastUrl: String? = null
    private var lastPreferencesJson: String? = null
    private var lastScheduledTimeMillis: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // On START_STICKY system-restart the intent may be null; fall back to saved state.
        val url = intent?.getStringExtra(EXTRA_URL) ?: lastUrl
        val preferencesJson = intent?.getStringExtra(EXTRA_PREFERENCES_JSON) ?: lastPreferencesJson
        val scheduledTimeMillis = when {
            intent != null -> intent.getLongExtra(EXTRA_SCHEDULED_MILLIS, -1L)
            else -> lastScheduledTimeMillis
        }

        if (url == null || preferencesJson == null || scheduledTimeMillis < 0L) {
            Log.e(TAG, "onStartCommand: missing required extras — stopping immediately")
            stopSelf()
            return START_NOT_STICKY
        }

        // Persist so onTaskRemoved can rebuild the restart intent
        lastUrl = url
        lastPreferencesJson = preferencesJson
        lastScheduledTimeMillis = scheduledTimeMillis

        // ── Step 1: Satisfy Android 8+ FGS rules — call startForeground immediately ─

        val timeLabel = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(scheduledTimeMillis))

        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val waitingNotification = NotificationUtil.makeKeeperWaitingNotification(tapIntent, timeLabel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationUtil.KEEPER_NOTIFICATION_ID,
                waitingNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NotificationUtil.KEEPER_NOTIFICATION_ID, waitingNotification)
        }

        Log.d(TAG, "Keeper started — waiting until $timeLabel for '$url'")

        // ── Step 2: Sleep until the scheduled time, then hand off to DownloaderV2 ──

        // Capture as local vals so the coroutine lambda doesn't require null-checks
        // (Kotlin can't smart-cast mutable instance vars inside lambdas).
        val localUrl = url
        val localJson = preferencesJson

        serviceScope.launch {
            try {
                val delayMs = scheduledTimeMillis - System.currentTimeMillis()
                if (delayMs > 0L) {
                    Log.d(TAG, "Keeper sleeping for ${delayMs}ms")
                    delay(delayMs)
                }

                // Update notification so the user knows the download is now starting
                NotificationUtil.updateKeeperNotificationToDownloading(tapIntent)

                val preferences = Json { ignoreUnknownKeys = true }
                    .decodeFromString<DownloadUtil.DownloadPreferences>(localJson)

                val task = Task(url = localUrl, preferences = preferences)

                // Ensure DownloadService foreground slot is live before we release ours
                App.startService()
                delay(1_000)

                downloader.enqueue(task)
                Log.d(TAG, "Keeper enqueued task for '$localUrl'")

            } catch (e: Exception) {
                Log.e(TAG, "Keeper: failed to enqueue task", e)
            } finally {
                // Bridge gap: keep our FGS alive for 3 s while DownloadService binds
                // (same pattern as ScheduledDownloadService for race-condition safety)
                App.startService()
                delay(3_000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }

        return START_STICKY
    }

    /**
     * Called when the user swipes the app away from Recents **after** this service started.
     * Even though a foreground service normally survives an app swipe, some very aggressive
     * OEM memory managers kill the process anyway.  We restart it here so the download
     * cannot be lost.
     *
     * Note: [rootIntent] is the *activity's* root intent, not our service intent —
     * that is why we rebulid the service intent from the saved instance fields.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        val url = lastUrl ?: return
        val json = lastPreferencesJson ?: return
        val millis = lastScheduledTimeMillis.takeIf { it > 0L } ?: return

        Log.w(TAG, "onTaskRemoved: process eviction detected — restarting ScheduleKeeperService")

        val restartIntent = Intent(applicationContext, ScheduleKeeperService::class.java).apply {
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_PREFERENCES_JSON, json)
            putExtra(EXTRA_SCHEDULED_MILLIS, millis)
        }
        ContextCompat.startForegroundService(applicationContext, restartIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        /** Intent extra – the URL to download. */
        const val EXTRA_URL = "extra_keeper_url"
        /** Intent extra – JSON-serialised [DownloadUtil.DownloadPreferences]. */
        const val EXTRA_PREFERENCES_JSON = "extra_keeper_preferences_json"
        /** Intent extra – epoch-millisecond time when the download should fire. */
        const val EXTRA_SCHEDULED_MILLIS = "extra_keeper_scheduled_millis"
        private const val TAG = "ScheduleKeeperService"
    }
}
