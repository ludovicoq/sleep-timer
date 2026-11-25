package com.lquerin.sleeptimer

import android.content.Context
import android.text.format.DateFormat
import java.util.Date
import java.util.Locale

/** 45 → "45 min", 60 → "1 h", 90 → "1 h 30 min" */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

/** Remaining time as "MM:SS" or "H:MM:SS". */
fun formatCountdown(millis: Long): String {
    val total = (millis + 999L) / 1000L
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%02d:%02d", m, s)
}

/** Clock time in the phone's 12/24h format, e.g. "01:23". */
fun formatClock(context: Context, timeMillis: Long): String =
    DateFormat.getTimeFormat(context).format(Date(timeMillis))
