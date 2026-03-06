package com.junkfood.seal.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.getSystemService
import com.junkfood.seal.DownloadAlarmReceiver
import com.junkfood.seal.database.objects.ScheduledTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ScheduleUtil"

object ScheduleUtil {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Permission helpers ───────────────────────────────────────────────────────

    /**
     * Returns true if the app is currently allowed to schedule exact alarms.
     *
     * On API < 31 (Android 12), exact alarms are always permitted.
     * On API 31+ the user must have granted [android.permission.SCHEDULE_EXACT_ALARM]
     * (or [android.permission.USE_EXACT_ALARM] for privileged apps).
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
    }

    /**
     * Returns `true` if the OS is currently applying battery optimizations to this app.
     *
     * When battery-optimized, aggressive OEM battery managers (MIUI, One UI, etc.) can
     * prevent [AlarmManager] exact alarms from waking the app after it has been swiped
     * away from Recents. Requesting exemption via
     * [android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] removes
     * this restriction and is required for reliable scheduled downloads.
     *
     * Always returns `false` on API < 23 where battery optimization didn't exist.
     */
    fun isBatteryOptimized(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // ── Public API ───────────────────────────────────────────────────────────────

    /**
     * Persist a new [ScheduledTask] in the database and register an exact
     * [AlarmManager.setAlarmClock] alarm that bypasses Doze mode completely.
     *
     * @param context         Application context.
     * @param url             URL to download.
     * @param title           Video title (display only, may be empty).
     * @param thumbnailUrl    Thumbnail URL (display only, may be empty).
     * @param preferences     Full download preferences to restore when the alarm fires.
     * @param scheduleParams  Epoch-millisecond trigger time + network constraint.
     * @param isPlaylist      True when the URL is a playlist.
     * @param scope           Coroutine scope for async DB work (defaults to a fresh IO scope).
     * @return                Human-readable scheduled-time string, e.g. "10:30 AM, Mar 5".
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
            // 1. Insert into DB to get a stable primary-key ID
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
            val taskId = DatabaseUtil.insertScheduledTask(entity).toInt()

            // 2. Register an exact alarm using the task's DB ID as the request code
            //    so we can later reconstruct the same PendingIntent for cancellation.
            setAlarm(context, taskId, scheduledTimeMillis)

            Log.d(TAG, "scheduleDownload: taskId=$taskId, triggerAt=${formatScheduledTime(scheduledTimeMillis)}")
        }

        return formatScheduledTime(scheduledTimeMillis)
    }

    /**
     * (Re-)register an exact [AlarmManager.setAlarmClock] alarm for an already-persisted task.
     *
     * [AlarmManager.setAlarmClock] fires at **exactly** [triggerAtMillis] regardless of
     * Doze mode, low-power mode, or app-standby buckets. It also displays a clock icon
     * in the status bar so the user knows an alarm is pending.
     *
     * @param context         Application context.
     * @param taskId          Primary key of the [ScheduledTask] row (used as PendingIntent
     *                        request code so each task gets a unique PendingIntent).
     * @param triggerAtMillis Epoch milliseconds when the alarm should fire.
     */
    fun setAlarm(context: Context, taskId: Int, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: run {
            Log.e(TAG, "setAlarm: AlarmManager unavailable")
            return
        }

        val operation = buildAlarmPendingIntent(context, taskId)

        // showIntent: what to open when the user taps the alarm clock icon in the
        // system status bar.  Opening the app's main launcher activity is ideal.
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE,
        )

        // setAlarmClock is the strongest guarantee: fires on time even in full Doze.
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            operation,
        )

        Log.d(TAG, "setAlarm: alarm set for taskId=$taskId at ${formatScheduledTime(triggerAtMillis)}")
    }

    /**
     * Cancel an exact alarm and delete the corresponding [ScheduledTask] row.
     *
     * [AlarmManager.cancel] is synchronous and main-thread safe; the DB delete
     * is dispatched on [Dispatchers.IO] via [scope].
     *
     * @param context Application context.
     * @param task    The [ScheduledTask] to cancel.
     * @param scope   Coroutine scope for the async DB delete.
     */
    fun cancelScheduledTask(
        context: Context,
        task: ScheduledTask,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    ) {
        // Cancel the alarm right away — safe to call on any thread
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.cancel(buildAlarmPendingIntent(context, task.id))
        Log.d(TAG, "cancelScheduledTask: alarm cancelled for taskId=${task.id}")

        // Delete the DB record; runs on IO dispatcher
        scope.launch {
            DatabaseUtil.deleteScheduledTask(task)
        }
    }

    /**
     * Cancel the existing alarm for [task], persist the updated schedule into the database,
     * and register a new [AlarmManager.setAlarmClock] alarm.
     *
     * This is a suspend function because it writes to Room; call it from a coroutine scope.
     *
     * @param context         Application context.
     * @param task            The [ScheduledTask] whose schedule is being changed.
     * @param newTimeMillis   New epoch-millisecond trigger time.
     * @param newNetworkPref  New network constraint to persist.
     */
    suspend fun rescheduleTask(
        context: Context,
        task: ScheduledTask,
        newTimeMillis: Long,
        newNetworkPref: ScheduleNetworkPreference,
    ) {
        // 1. Cancel the current alarm — synchronous, safe on any thread
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.cancel(buildAlarmPendingIntent(context, task.id))
        Log.d(TAG, "rescheduleTask: cancelled old alarm for taskId=${task.id}")

        // 2. Persist the updated schedule in the DB
        val updatedTask = task.copy(
            scheduledTimeMillis = newTimeMillis,
            networkPreference = newNetworkPref.id,
        )
        DatabaseUtil.updateScheduledTask(updatedTask)

        // 3. Register the new exact alarm
        setAlarm(context, task.id, newTimeMillis)
        Log.d(TAG, "rescheduleTask: taskId=${task.id} re-scheduled to ${formatScheduledTime(newTimeMillis)}")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Build the [PendingIntent] that AlarmManager fires when the alarm triggers.
     * Using [task.id] as the request code ensures every task has a distinct
     * PendingIntent, which is required for both scheduling and cancellation.
     */
    private fun buildAlarmPendingIntent(context: Context, taskId: Int): PendingIntent {
        val intent =
            Intent(context, DownloadAlarmReceiver::class.java)
                .putExtra(DownloadAlarmReceiver.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId, // unique per task — allows individual cancellation
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Returns a locale-aware, human-readable date+time string. */
    fun formatScheduledTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a, MMM d", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }
}
