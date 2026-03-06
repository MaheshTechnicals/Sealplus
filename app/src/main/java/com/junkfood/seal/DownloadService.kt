package com.junkfood.seal

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.junkfood.seal.util.NotificationUtil
import com.junkfood.seal.util.NotificationUtil.SERVICE_NOTIFICATION_ID

private const val TAG = "DownloadService"

/** This `Service` does nothing */
class DownloadService : Service() {

    /**
     * Called when [App.startService] uses [ContextCompat.startForegroundService].
     *
     * On Android 8+ we MUST call [startForeground] within 5 seconds of this call;
     * binding alone (via [onBind]) is NOT sufficient to satisfy that requirement
     * when the service is started with [android.content.Context.startForegroundService].
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        startForeground(SERVICE_NOTIFICATION_ID, NotificationUtil.makeServiceNotification(pendingIntent))
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }
        val notification = NotificationUtil.makeServiceNotification(pendingIntent)
        startForeground(SERVICE_NOTIFICATION_ID, notification)
        return DownloadServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
        return super.onUnbind(intent)
    }

    inner class DownloadServiceBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }
}
