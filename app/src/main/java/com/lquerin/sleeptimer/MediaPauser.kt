package com.lquerin.sleeptimer

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat

/**
 * Sends a real "Pause" command to the media that is playing.
 * It never takes audio focus.
 *
 * 1. With notification access: Pause is sent directly to every playing media session
 *    (Apple Music, Spotify, YouTube, podcasts...).
 * 2. Without it: a media "Pause" key is dispatched, like the pause button on headphones.
 *    Android routes it to the app that played media most recently.
 */
object MediaPauser {

    private val PLAYING_STATES = setOf(
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
    )

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun pause(context: Context) {
        if (hasNotificationAccess(context) && pauseSessions(context)) return
        sendPauseKey(context)
    }

    /** @return true if the active sessions could be read (whether or not something was playing). */
    private fun pauseSessions(context: Context): Boolean = try {
        val msm = context.getSystemService(MediaSessionManager::class.java)
        val controllers = msm.getActiveSessions(ComponentName(context, MediaListenerService::class.java))
        for (controller in controllers) {
            if (controller.playbackState?.state in PLAYING_STATES) {
                controller.transportControls.pause()
            }
        }
        true
    } catch (e: SecurityException) {
        false
    }

    private fun sendPauseKey(context: Context) {
        val am = context.getSystemService(AudioManager::class.java)
        val now = SystemClock.uptimeMillis()
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
    }
}
