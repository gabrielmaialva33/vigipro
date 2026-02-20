package com.vigipro.core.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        val alertChannel = NotificationChannel(
            CHANNEL_CAMERA_ALERTS,
            "Alertas de Camera",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificacoes quando cameras ficam offline ou retornam"
        }
        notificationManager.createNotificationChannel(alertChannel)
    }

    fun notifyCameraOffline(cameraId: String, cameraName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CAMERA_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Camera Offline")
            .setContentText("$cameraName perdeu conexao")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(cameraId.hashCode(), notification)
    }

    fun notifyCameraOnline(cameraId: String, cameraName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CAMERA_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Camera Restaurada")
            .setContentText("$cameraName voltou a funcionar")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(cameraId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_CAMERA_ALERTS = "camera_alerts"
    }
}
