package com.lquerin.sleeptimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Timer logic. No foreground service: an exact alarm fires at the end time,
 * and the notification counts down on its own (chronometer).
 */
object SleepTimer {
    const val EXTEND_MINUTES = 5
    const val MAX_MINUTES = 240

    fun start(context: Context, minutes: Int) {
        val m = minutes.coerceIn(1, MAX_MINUTES)
        TimerPrefs.setLastMinutes(context, m)
        TimerPrefs.setTotalMillis(context, m * 60_000L)
        schedule(context, System.currentTimeMillis() + m * 60_000L)
    }

    fun extend(context: Context, minutes: Int = EXTEND_MINUTES) {
        val end = TimerPrefs.endTime(context)
        if (end == 0L) return
        val extra = minutes * 60_000L
        TimerPrefs.setTotalMillis(context, TimerPrefs.totalMillis(context) + extra)
        schedule(context, end + extra)
    }

    fun cancel(context: Context) {
        alarmManager(context).cancel(expireIntent(context))
        clear(context)
    }

    /** Called by the alarm: send Pause, then reset everything. */
    fun onExpired(context: Context) {
        val end = TimerPrefs.rawEndTime(context)
        // Ignore a stale alarm (timer cancelled, or extended in the meantime).
        if (end == 0L || end > System.currentTimeMillis() + 2_000L) return
        MediaPauser.pause(context)
        clear(context)
    }

    /** After reboot or app update alarms are lost: re-create it if still running. */
    fun restore(context: Context) {
        val end = TimerPrefs.endTime(context)
        if (end > 0L) schedule(context, end) else clear(context)
    }

    private fun schedule(context: Context, end: Long) {
        TimerPrefs.setEndTime(context, end)
        val am = alarmManager(context)
        val pi = expireIntent(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi)
        }
        TimerNotification.show(context, end)
        SleepTileService.refresh(context)
    }

    private fun clear(context: Context) {
        TimerPrefs.setEndTime(context, 0L)
        TimerNotification.cancel(context)
        SleepTileService.refresh(context)
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    private fun expireIntent(context: Context): PendingIntent =
        TimerReceiver.pendingIntent(context, TimerReceiver.ACTION_EXPIRE, requestCode = 0)
}
