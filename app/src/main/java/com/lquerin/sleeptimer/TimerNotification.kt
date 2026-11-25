package com.lquerin.sleeptimer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build

/** Silent ongoing notification with a live countdown and "+5 min" / "Cancel" buttons. */
object TimerNotification {
    private const val CHANNEL_ID = "sleep_timer"
    private const val NOTIFICATION_ID = 1

    fun show(context: Context, end: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannel(context, nm)

        val openApp = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val extend = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_add),
            context.getString(R.string.extend),
            TimerReceiver.pendingIntent(context, TimerReceiver.ACTION_EXTEND, requestCode = 1),
        ).build()

        val cancel = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_close),
            context.getString(R.string.cancel),
            TimerReceiver.pendingIntent(context, TimerReceiver.ACTION_CANCEL, requestCode = 2),
        ).build()

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_moon)
            .setColor(context.getColor(R.color.lavender))
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.pause_at, formatClock(context, end)))
            .setWhen(end)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(openApp)
            .addAction(extend)
            .addAction(cancel)
            // Safety net: disappears by itself shortly after the end time.
            .setTimeoutAfter((end - System.currentTimeMillis()).coerceAtLeast(0L) + 60_000L)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context, nm: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW, // no sound, no vibration
        ).apply {
            description = context.getString(R.string.channel_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }
}
