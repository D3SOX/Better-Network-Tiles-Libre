package be.casperverswijvelt.unifiedinternetqs.tiles

import be.casperverswijvelt.unifiedinternetqs.tilebehaviour.PrivateDnsTileBehaviour
import be.casperverswijvelt.unifiedinternetqs.util.toDialog

class PrivateDnsTileService : ReportingTileService() {

    override fun getTag(): String {
        return "PrivateDnsTileService"
    }

    override fun onCreate() {
        log("Private DNS tile service created")

        tileBehaviour = PrivateDnsTileBehaviour(
            context = this,
            showDialog = { showDialog(it.toDialog(applicationContext)) },
            unlockAndRun = { unlockAndRun(it) }
        )
        super.onCreate()
    }
}