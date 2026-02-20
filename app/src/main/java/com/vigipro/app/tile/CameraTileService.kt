package com.vigipro.app.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.vigipro.app.MainActivity

class CameraTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("navigate_to", "dashboard")
        }
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.label = "VigiPro"
            tile.subtitle = "Cameras"
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }
}
