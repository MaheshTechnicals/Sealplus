package com.junkfood.seal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.ScheduleUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for [Intent.ACTION_BOOT_COMPLETED] and re-registers all pending
 * [android.app.AlarmManager] alarms after a device reboot.
 *
 * **Why this is necessary**
 * [android.app.AlarmManager] alarms are volatile — they are cleared when the device
 * powers off or reboots. Without this receiver, a scheduled download that was set
 * before a reboot would never fire if the device was restarted before the trigger time.
 *
 * **What it does**
 * 1. Queries all [com.junkfood.seal.database.objects.ScheduledTask] rows from Room.
 * 2. For each task whose [com.junkfood.seal.database.objects.ScheduledTask.scheduledTimeMillis]
 *    is still in the future, calls [ScheduleUtil.setAlarm] to re-register the exact alarm.
 * 3. For any task that already expired during the reboot window, it immediately enqueues
 *    the download via [com.junkfood.seal.download.DownloaderV2] and removes the DB row.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        Log.d(TAG, "onReceive: device rebooted — re-registering scheduled alarms")

        // Use a long-lived scope tied to the Application process; goAsync() is not
        // needed here because Room + ScheduleUtil.setAlarm are fast, but we run on IO
        // to avoid blocking the main thread (Room cannot be queried on main in strict mode).
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val tasks = DatabaseUtil.getAllScheduledTasks()
                val now = System.currentTimeMillis()

                tasks.forEach { task ->
                    if (task.scheduledTimeMillis > now) {
                        // Still in the future — re-register the exact alarm
                        ScheduleUtil.setAlarm(context, task.id, task.scheduledTimeMillis)
                        Log.d(TAG, "Re-registered alarm for taskId=${task.id}")
                    } else {
                        // Already expired while the device was off — fire it immediately
                        Log.w(TAG, "Task ${task.id} expired during reboot; firing immediately")
                        val serviceIntent =
                            Intent(context, ScheduledDownloadService::class.java)
                                .putExtra(ScheduledDownloadService.EXTRA_TASK_ID, task.id)
                        // ContextCompat handles the API 26+ check and is correct for a
                        // cold-start scenario (boot) where the app process does not exist yet.
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                }

                Log.d(TAG, "onReceive: restored ${tasks.size} scheduled task(s)")
            } catch (e: Exception) {
                Log.e(TAG, "onReceive: failed to restore alarms", e)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
