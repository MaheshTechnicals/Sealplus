package com.junkfood.seal

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.junkfood.seal.util.NotificationUtil
import com.junkfood.seal.util.NotificationUtil.SERVICE_NOTIFICATION_ID

private const val TAG = "DownloadService"

/** This `Service` does nothing */
class DownloadService : Service() {

    /** True once [startForeground] has actually succeeded — false if it was blocked. */
    @Volatile private var isForegroundActive = false

    override fun onBind(intent: Intent): IBinder {
        attemptPromoteToForeground()
        return DownloadServiceBinder()
    }

    /**
     * CRITICAL: on Android 12+ (API 31+), startForeground() throws
     * ForegroundServiceStartNotAllowedException if the app is fully backgrounded (no visible
     * UI) AND has no exemption (battery-optimization-disabled is one of the few documented
     * exemptions — see developer.android.com/develop/background-work/services/fgs/
     * restrictions-bg-start). This happens whenever a download auto-resumes (e.g. after a
     * network drop/reconnect) while the app is in the background without that exemption. Left
     * uncaught, this RuntimeException propagates to the app's global uncaught exception handler
     * (App.kt), which kills the ENTIRE process — silently ending the download and every other
     * running task. That is the real cause behind "download sometimes fails but works again
     * after reopening the app": reopening puts the app in the foreground, satisfying the
     * exemption, so the next start succeeds.
     *
     * We must never let this crash the app: catching it lets the download keep running as a
     * background (non-foreground) task — degraded but alive — instead of being killed outright.
     * The wake lock (acquired separately in DownloaderV2Impl) still protects the CPU from Doze
     * suspension for as long as it's held. [retryPromoteToForegroundIfNeeded] is called again
     * once the app becomes visible, to upgrade the still-bound service to a proper foreground
     * service (with its notification) as soon as it's allowed.
     */
    private fun attemptPromoteToForeground() {
        if (isForegroundActive) return
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }
        val notification = NotificationUtil.makeServiceNotification(pendingIntent)
        try {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
            isForegroundActive = true
        } catch (e: Exception) {
            val isBgStartRestriction =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e is ForegroundServiceStartNotAllowedException
            Log.w(
                TAG,
                if (isBgStartRestriction) {
                    "startForeground() blocked: app is backgrounded without a battery " +
                        "optimization exemption. Download continues without the foreground " +
                        "promotion instead of crashing."
                } else {
                    "startForeground() failed unexpectedly: ${e.message}"
                },
                e,
            )
        }
    }

    /** Called by [App] once the app becomes visible again, to retry a previously-blocked
     *  foreground promotion (the visible-app exemption now applies even without battery
     *  optimization being disabled). No-op if already promoted. */
    fun retryPromoteToForegroundIfNeeded() {
        attemptPromoteToForeground()
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
