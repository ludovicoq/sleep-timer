package com.lquerin.sleeptimer

import android.service.notification.NotificationListenerService

/**
 * Intentionally empty. Android only lets an app control other apps' media sessions
 * if it is an enabled notification listener. Notifications are never read.
 */
class MediaListenerService : NotificationListenerService()
