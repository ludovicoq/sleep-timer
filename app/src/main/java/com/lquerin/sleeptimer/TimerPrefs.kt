package com.lquerin.sleeptimer

import android.content.Context
import android.content.SharedPreferences

/** Persistent timer state: when it ends (0 = not running) and the last duration used. */
object TimerPrefs {
    private const val FILE = "sleep_timer"
    private const val KEY_END = "end_time"
    private const val KEY_LAST = "last_minutes"
    private const val KEY_TOTAL = "total_millis"
    const val DEFAULT_MINUTES = 30

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** End time as stored, even if already in the past. */
    fun rawEndTime(context: Context): Long = prefs(context).getLong(KEY_END, 0L)

    /** End time if the timer is running, otherwise 0. */
    fun endTime(context: Context): Long {
        val end = rawEndTime(context)
        return if (end > System.currentTimeMillis()) end else 0L
    }

    fun isRunning(context: Context): Boolean = endTime(context) > 0L

    fun setEndTime(context: Context, end: Long) {
        prefs(context).edit().putLong(KEY_END, end).apply()
    }

    /** Full length of the current timer (including extensions), used for the progress ring. */
    fun totalMillis(context: Context): Long = prefs(context).getLong(KEY_TOTAL, 0L)

    fun setTotalMillis(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_TOTAL, millis).apply()
    }

    fun lastMinutes(context: Context): Int = prefs(context).getInt(KEY_LAST, DEFAULT_MINUTES)

    fun setLastMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_LAST, minutes).apply()
    }
}
