package com.lquerin.sleeptimer

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: tap = start with the last duration, tap again = cancel. */
class SleepTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (TimerPrefs.isRunning(this)) {
            SleepTimer.cancel(this)
        } else {
            SleepTimer.start(this, TimerPrefs.lastMinutes(this))
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val end = TimerPrefs.endTime(this)
        tile.label = getString(R.string.app_name)
        if (end > 0L) {
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_until, formatClock(this, end))
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = formatDuration(TimerPrefs.lastMinutes(this))
            }
        }
        tile.updateTile()
    }

    companion object {
        /** Ask the system to refresh the tile (e.g. after the timer changed elsewhere). */
        fun refresh(context: Context) {
            try {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, SleepTileService::class.java),
                )
            } catch (e: Exception) {
                // Tile not added or system refused: nothing to refresh.
            }
        }
    }
}
