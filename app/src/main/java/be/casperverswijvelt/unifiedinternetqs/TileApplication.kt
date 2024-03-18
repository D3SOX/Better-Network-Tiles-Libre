package be.casperverswijvelt.unifiedinternetqs

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import android.widget.Toast
import be.casperverswijvelt.unifiedinternetqs.data.BITPreferences
import be.casperverswijvelt.unifiedinternetqs.data.ShellMethod
import be.casperverswijvelt.unifiedinternetqs.util.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TileApplication : Application() {

    companion object {
        const val CHANNEL_ID = "autoStartServiceChannel"
        const val CHANNEL_NAME = "Shizuku Detection"
        const val TAG = "TileApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Created Tile Application")

        ExecutorServiceSingleton.getInstance()

        createNotificationChannel()
        startTileSyncService()

        val preferences = BITPreferences(this)
        runBlocking {
            if (preferences.getShellMethod.first() == ShellMethod.AUTO) {
                // Mode AUTO is when user has not explicitly set a
                Shell.getShell {
                    if (Shell.isAppGrantedRoot() == true) {
                        runBlocking {
                            preferences.setShellMethod(ShellMethod.ROOT)
                        }
                    } else if (ShizukuUtil.hasShizukuPermission()) {
                        runBlocking {
                            preferences.setShellMethod(ShellMethod.SHIZUKU)
                        }
                    }

                }
            }
        }
    }
    private fun startTileSyncService() {
        try {
            Log.d(TAG, "Starting tile sync service!")
            startForegroundService(
                Intent(
                    this,
                    TileSyncService::class.java
                )
            )
        } catch (e: Throwable) {
            Log.d(
                TAG,
                "Failed to start tile sync foreground service due to an ${e.message}"
            )

            // Not sure what the cause of the 'ForegroundServiceStartNotAllowedException'
            //  is or how to solve it.
            Toast.makeText(
                applicationContext,
                R.string.toast_foreground_service_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(serviceChannel)
    }
}