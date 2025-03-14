package com.kardani.partymonster.tiles

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.kardani.partymonster.service.PartyMonsterService

class PartyMonsterTileService : TileService() {
    private var isPartyTime = false

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (isPartyTime) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (isPartyTime) "PARTY ON! 🎉" else "Party Time?"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isPartyTime) {
            stopParty()
        } else {
            startParty()
        }
    }

    private fun startParty() {
        isPartyTime = true
        updateTileState()

        // Create and start the service
        val serviceIntent = Intent(this, PartyMonsterService::class.java).apply {
            action = PartyMonsterService.ACTION_START_PARTY
        }

        try {
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("PartyMonster", "Failed to start service", e)
            isPartyTime = false
            updateTileState()
        }
    }

    private fun stopParty() {
        isPartyTime = false
        updateTileState()

        val serviceIntent = Intent(this, PartyMonsterService::class.java).apply {
            action = PartyMonsterService.ACTION_STOP_PARTY
        }
        startService(serviceIntent)
    }

    private fun updateTileState() {
        qsTile?.apply {
            state = if (isPartyTime) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (isPartyTime) "PARTY ON! 🎉" else "Party Time?"
            updateTile()
        }
    }
}