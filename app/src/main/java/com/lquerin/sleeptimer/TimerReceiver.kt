package com.lquerin.sleeptimer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_EXPIRE -> SleepTimer.onExpired(context)
            ACTION_EXTEND -> SleepTimer.extend(context)
            ACTION_CANCEL -> SleepTimer.cancel(context)
        }
    }

    companion object {
        const val ACTION_EXPIRE = "com.lquerin.sleeptimer.EXPIRE"
        const val ACTION_EXTEND = "com.lquerin.sleeptimer.EXTEND"
        const val ACTION_CANCEL = "com.lquerin.sleeptimer.CANCEL"

        fun pendingIntent(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, TimerReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            SleepTimer.restore(context)
        }
    }
}
