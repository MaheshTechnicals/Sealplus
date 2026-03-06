package com.junkfood.seal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives [android.app.AlarmManager] alarms for scheduled downloads and immediately
 * starts [ScheduledDownloadService] as a foreground service.
 *
 * AlarmManager alarms set via [android.app.AlarmManager.setAlarmClock] (or
 * [android.app.AlarmManager.setExactAndAllowWhileIdle]) temporarily lift the
 * background-service restrictions on Android 12+ (API 31+), which means we are
 * allowed to call [Context.startForegroundService] from this receiver even while
 * the app is in the background.
 */
class DownloadAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, INVALID_TASK_ID)
        if (taskId == INVALID_TASK_ID) {
            Log.e(TAG, "onReceive: intent is missing EXTRA_TASK_ID — ignoring alarm")
            return
        }

        Log.d(TAG, "onReceive: alarm fired for taskId=$taskId, starting ScheduledDownloadService")

        val serviceIntent =
            Intent(context, ScheduledDownloadService::class.java)
                .putExtra(ScheduledDownloadService.EXTRA_TASK_ID, taskId)

        // ContextCompat.startForegroundService handles the API 26+ check internally and
        // ensures the service is promoted to foreground even when the app process was
        // killed before the alarm fired. The service MUST call startForeground() within
        // ~5 seconds of onStartCommand() to avoid ForegroundServiceDidNotStartInTimeException
        // on Android 14+.
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        /** Intent extra key carrying the [com.junkfood.seal.database.objects.ScheduledTask] ID. */
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val INVALID_TASK_ID = -1
        private const val TAG = "DownloadAlarmReceiver"
    }
}
