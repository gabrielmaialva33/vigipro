package com.vigipro.core.data.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.vigipro.core.data.notification.CameraNotificationHelper
import com.vigipro.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesRepository: UserPreferencesRepository
    @Inject lateinit var notificationHelper: CameraNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "Geofence error: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val siteIds = event.triggeringGeofences?.map { it.requestId } ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (transition) {
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        // User LEFT the site -> ARM detection
                        preferencesRepository.updateDetectionEnabled(true)
                        Log.d(TAG, "User left sites $siteIds — detection ARMED")
                        notificationHelper.showStatusNotification(
                            title = "Detecção ativada",
                            message = "Você saiu da área monitorada. Detecção automática ativada.",
                        )
                    }
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        // User ENTERED the site -> DISARM detection
                        preferencesRepository.updateDetectionEnabled(false)
                        Log.d(TAG, "User entered sites $siteIds — detection DISARMED")
                        notificationHelper.showStatusNotification(
                            title = "Detecção desativada",
                            message = "Você chegou na área monitorada. Detecção automática desativada.",
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
