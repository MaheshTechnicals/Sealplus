package com.junkfood.seal.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.NotificationActionReceiver
import com.junkfood.seal.NotificationActionReceiver.Companion.ACTION_CANCEL_TASK
import com.junkfood.seal.NotificationActionReceiver.Companion.ACTION_ERROR_REPORT
import com.junkfood.seal.NotificationActionReceiver.Companion.ACTION_KEY
import com.junkfood.seal.NotificationActionReceiver.Companion.ERROR_REPORT_KEY
import com.junkfood.seal.NotificationActionReceiver.Companion.NOTIFICATION_ID_KEY
import com.junkfood.seal.NotificationActionReceiver.Companion.TASK_ID_KEY
import com.junkfood.seal.R
import com.junkfood.seal.util.PreferenceUtil.getBoolean

private const val TAG = "NotificationUtil"

@SuppressLint("StaticFieldLeak")
object NotificationUtil {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private const val PROGRESS_MAX = 100
    private const val PROGRESS_INITIAL = 0
    private const val CHANNEL_ID = "download_notification"
    private const val SERVICE_CHANNEL_ID = "download_service"
    private const val NOTIFICATION_GROUP_ID = "seal.download.notification"
    private const val DEFAULT_NOTIFICATION_ID = 100
    const val SERVICE_NOTIFICATION_ID = 123
    private lateinit var serviceNotification: Notification

    //    private var builder =
    //        NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_seal)
    private val commandNotificationBuilder =
        NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_seal)

    private fun applySmartNotificationSettings(
        builder: NotificationCompat.Builder,
        isSuccess: Boolean = false,
        isError: Boolean = false,
        isProgress: Boolean = false
    ) {
        // Master notification sound switch
        if (!NOTIFICATION_SOUND.getBoolean()) {
            builder.setSound(null)
            builder.setVibrate(null)
            return
        }
        
        // Apply sound based on task status
        val shouldPlaySound = when {
            isSuccess -> NOTIFICATION_SUCCESS_SOUND.getBoolean()
            isError -> NOTIFICATION_ERROR_SOUND.getBoolean()
            isProgress -> false // Progress notifications should not play sound on every update
            else -> false // Default: no sound
        }
        
        if (shouldPlaySound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        } else {
            builder.setSound(null)
        }

        // Apply vibration (only for completion/error, not progress)
        if (NOTIFICATION_VIBRATE.getBoolean() && !isProgress) {
            // Works on all Android versions including 13, 14, 15, 16+
            builder.setVibrate(longArrayOf(0, 250, 250, 250))
        } else {
            builder.setVibrate(null)
        }

        // Apply LED indicator - Compatible with all Android versions
        if (NOTIFICATION_LED.getBoolean()) {
            val ledColor = when {
                isSuccess -> Color.GREEN
                isError -> Color.RED
                isProgress -> Color.BLUE
                else -> Color.BLUE
            }
            // LED only shows for non-progress notifications
            // Note: LED support varies by device manufacturer, especially on Android 13+
            // Some devices may not show LED even when configured
            if (!isProgress) {
                builder.setLights(ledColor, 1000, 1000)
            }
        }
        
        // Android 13+ (API 33+) specific enhancements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Ensure notification shows badge on app icon
            builder.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        }
        
        // Android 12+ (API 31+) - Set notification category for better system handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                isSuccess || isProgress -> builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
                isError -> builder.setCategory(NotificationCompat.CATEGORY_ERROR)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        val name = context.getString(R.string.channel_name)
        val descriptionText = context.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channelGroup =
            NotificationChannelGroup(NOTIFICATION_GROUP_ID, context.getString(R.string.download))
        val channel =
            NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                group = NOTIFICATION_GROUP_ID
                // Enable sound, vibration, and LED - Compatible with Android 8.0+ (API 26+)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                lightColor = Color.BLUE
                
                // Android 13+ (API 33+) - Additional notification settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Allow badge on app icon
                    setShowBadge(true)
                }
                
                // Android 14+ (API 34+) compatibility - No specific changes needed
                // The channel will work correctly with all features
                
                // Note: For Android 13+, POST_NOTIFICATIONS permission is already declared in manifest
                // and requested at runtime in the app's permission flow
            }
        val serviceChannel =
            NotificationChannel(SERVICE_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.service_title)
                group = NOTIFICATION_GROUP_ID
                // Service notifications should be silent
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
        notificationManager.createNotificationChannelGroup(channelGroup)
        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(serviceChannel)
    }

    fun notifyProgress(
        title: String,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        progress: Int = PROGRESS_INITIAL,
        taskId: String? = null,
        text: String? = null,
    ) {
        if (!NOTIFICATION.getBoolean()) return
        val pendingIntent =
            taskId?.let {
                Intent(context.applicationContext, NotificationActionReceiver::class.java)
                    .putExtra(TASK_ID_KEY, taskId)
                    .putExtra(NOTIFICATION_ID_KEY, notificationId)
                    .putExtra(ACTION_KEY, ACTION_CANCEL_TASK)
                    .run {
                        PendingIntent.getBroadcast(
                            context.applicationContext,
                            notificationId,
                            this,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    }
            }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_seal)
            .setContentTitle(title)
            .setProgress(PROGRESS_MAX, progress, progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        
        pendingIntent?.let {
            builder.addAction(R.drawable.outline_cancel_24, context.getString(R.string.cancel), it)
        }
        
        // Apply smart notification settings for progress notifications
        applySmartNotificationSettings(builder, isProgress = true)
        
        notificationManager.notify(notificationId, builder.build())
    }

    fun finishNotification(
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        title: String? = null,
        text: String? = null,
        intent: PendingIntent? = null,
    ) {
        Log.d(TAG, "finishNotification: ")
        notificationManager.cancel(notificationId)
        if (!NOTIFICATION.getBoolean()) return

        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_seal)
                .setContentText(text)
                .setOngoing(false)
                .setAutoCancel(true)
        title?.let { builder.setContentTitle(title) }
        intent?.let { builder.setContentIntent(intent) }
        applySmartNotificationSettings(builder, isSuccess = true)
        notificationManager.notify(notificationId, builder.build())
    }

    fun finishNotificationForCustomCommands(
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        title: String? = null,
        text: String? = null,
    ) {
        //        notificationManager.cancel(notificationId)
        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_seal)
                .setContentText(text)
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setOngoing(false)
                .setStyle(null)
        title?.let { builder.setContentTitle(title) }
        applySmartNotificationSettings(builder, isSuccess = true)

        notificationManager.notify(notificationId, builder.build())
    }

    fun makeServiceNotification(intent: PendingIntent, text: String? = null): Notification {
        serviceNotification =
            NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_seal)
                .setContentTitle(context.getString(R.string.service_title))
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(intent)
                .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        return serviceNotification
    }

    fun updateServiceNotificationForPlaylist(index: Int, itemCount: Int) {
        serviceNotification =
            NotificationCompat.Builder(context, serviceNotification)
                .setContentTitle(context.getString(R.string.service_title) + " ($index/$itemCount)")
                .build()
        notificationManager.notify(SERVICE_NOTIFICATION_ID, serviceNotification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun notifyError(
        title: String,
        textId: Int = R.string.download_error_msg,
        notificationId: Int,
        report: String,
    ) {
        if (!NOTIFICATION.getBoolean()) return

        val intent =
            Intent()
                .setClass(context, NotificationActionReceiver::class.java)
                .putExtra(NOTIFICATION_ID_KEY, notificationId)
                .putExtra(ERROR_REPORT_KEY, report)
                .putExtra(ACTION_KEY, ACTION_ERROR_REPORT)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_ONE_SHOT or
                    PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT,
            )
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_seal)
            .setContentTitle(title)
            .setContentText(context.getString(textId))
            .setOngoing(false)
            .addAction(
                R.drawable.outline_content_copy_24,
                context.getString(R.string.copy_error_report),
                pendingIntent,
            )
            .run {
                applySmartNotificationSettings(this, isError = true)
                notificationManager.cancel(notificationId)
                notificationManager.notify(notificationId, build())
            }
    }

    fun makeNotificationForCustomCommand(
        notificationId: Int,
        taskId: String,
        progress: Int,
        text: String? = null,
        templateName: String,
        taskUrl: String,
    ) {
        if (!NOTIFICATION.getBoolean()) return

        val intent =
            Intent(context.applicationContext, NotificationActionReceiver::class.java)
                .putExtra(TASK_ID_KEY, taskId)
                .putExtra(NOTIFICATION_ID_KEY, notificationId)
                .putExtra(ACTION_KEY, ACTION_CANCEL_TASK)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context.applicationContext,
                notificationId,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_seal)
            .setContentTitle(
                "[${templateName}_${taskUrl}] " +
                    context.getString(R.string.execute_command_notification)
            )
            .setContentText(text)
            .setOngoing(true)
            .setProgress(PROGRESS_MAX, progress, progress == -1)
            .addAction(
                R.drawable.outline_cancel_24,
                context.getString(R.string.cancel),
                pendingIntent,
            )
        
        // Apply smart notification settings
        applySmartNotificationSettings(builder, isProgress = true)
        
        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    fun areNotificationsEnabled(): Boolean {
        return when {
            // Android 13+ (API 33+) - Check POST_NOTIFICATIONS permission
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                notificationManager.areNotificationsEnabled()
            }
            // Android 7.1+ (API 25+) - Check if notifications are enabled
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                notificationManager.areNotificationsEnabled()
            }
            // Android 7.0 and below - Notifications always enabled
            else -> true
        }
    }
    
    /**
     * Check if a specific notification channel is enabled
     * Useful for Android 8.0+ (API 26+) where users can disable specific channels
     */
    fun isChannelEnabled(channelId: String = CHANNEL_ID): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val channel = notificationManager.getNotificationChannel(channelId)
                channel?.importance != NotificationManager.IMPORTANCE_NONE
            }
            else -> areNotificationsEnabled()
        }
    }
}
