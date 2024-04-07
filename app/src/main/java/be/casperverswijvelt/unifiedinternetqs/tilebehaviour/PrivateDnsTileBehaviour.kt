package be.casperverswijvelt.unifiedinternetqs.tilebehaviour

import android.content.Context
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import be.casperverswijvelt.unifiedinternetqs.R
import be.casperverswijvelt.unifiedinternetqs.TileSyncService
import be.casperverswijvelt.unifiedinternetqs.tiles.PrivateDnsTileService
import be.casperverswijvelt.unifiedinternetqs.util.AlertDialogData
import be.casperverswijvelt.unifiedinternetqs.util.executeShellCommandAsync
import be.casperverswijvelt.unifiedinternetqs.util.getPrivateDnsMode
import kotlinx.coroutines.Runnable

class PrivateDnsTileBehaviour(
    context: Context,
    showDialog: (AlertDialogData) -> Unit,
    unlockAndRun: (Runnable) -> Unit = { it.run() }
): TileBehaviour(context, showDialog, unlockAndRun) {

    companion object {
        private const val TAG = "PrivateDnsTileBehaviour"
        const val DNS_MODE_OFF = "off"
        const val DNS_MODE_AUTO = "opportunistic"
        const val DNS_MODE_ON = "hostname"
    }

    override val type: TileType
        get() = TileType.PrivateDns
    override val tileName: String
        get() = resources.getString(R.string.private_dns)
    override val defaultIcon: Icon
        get() = Icon.createWithResource(
            context,
            R.drawable.ic_baseline_public_off_24
        )
    @Suppress("UNCHECKED_CAST")
    override val tileServiceClass: Class<TileService>
        get() = PrivateDnsTileService::class.java as Class<TileService>

    override val tileState: TileState
        get() {
            val tile = TileState()
            val dnsmode = getPrivateDnsMode(context)
            val privateDnsEnabled = dnsmode.equals(DNS_MODE_ON, true) || dnsmode.equals(DNS_MODE_AUTO, true)

            tile.icon = if (privateDnsEnabled) R.drawable.ic_baseline_public_24 else R.drawable.ic_baseline_public_off_24
            tile.label = resources.getString(R.string.private_dns)

            if ((privateDnsEnabled && !TileSyncService.isTurningOffPrivateDns) || TileSyncService.isTurningOnPrivateDns) {

                if (privateDnsEnabled) TileSyncService.isTurningOnPrivateDns = false

                tile.state = Tile.STATE_ACTIVE
                tile.subtitle =
                    if (TileSyncService.isTurningOnPrivateDns) resources.getString(R.string.turning_on) else resources.getString(
                        R.string.on
                    )

            } else {

                if (!privateDnsEnabled) TileSyncService.isTurningOffPrivateDns = false

                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = resources.getString(R.string.off)
            }

            return tile
        }
    override val onLongClickIntentAction: String
        get() {
            return Settings.ACTION_WIRELESS_SETTINGS
        }

    override fun onClick() {
        log("onClick")

        if (!checkShellAccess()) return

        if (requiresUnlock) {
            unlockAndRun { togglePrivateDns() }
        } else {
            togglePrivateDns()
        }
    }

    private fun togglePrivateDns() {
        // Toggle private DNs for now:
        // If Private DNS is on or auto -> disable
        // If Private DNS is off -> enable

        val dnsmode = getPrivateDnsMode(context)
        val privateDnsEnabled = dnsmode.equals(DNS_MODE_ON, true) || dnsmode.equals(DNS_MODE_AUTO, true)

        when {
            privateDnsEnabled || TileSyncService.isTurningOnPrivateDns -> {
                TileSyncService.isTurningOnPrivateDns = false
                TileSyncService.isTurningOffPrivateDns = true
                executeShellCommandAsync("settings put global private_dns_mode $DNS_MODE_OFF", context) {
                    if (it?.isSuccess != true) {
                        TileSyncService.isTurningOffPrivateDns = false
                    }
                    updateTile()
                }
            }
            else -> {
                TileSyncService.isTurningOnPrivateDns = true
                TileSyncService.isTurningOffPrivateDns = false
                executeShellCommandAsync("settings put global private_dns_mode $DNS_MODE_ON", context) {
                    if (it?.isSuccess != true) {
                        TileSyncService.isTurningOnPrivateDns = false
                    }
                    updateTile()
                }
            }
        }
        updateTile()
    }

    private fun log(text: String) {
        Log.d(TAG, text)
    }
}