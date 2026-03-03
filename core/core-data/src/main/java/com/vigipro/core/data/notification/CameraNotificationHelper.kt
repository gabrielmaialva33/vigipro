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
            "Alertas de Câmera",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificações quando câmeras ficam offline ou retornam"
        }
        notificationManager.createNotificationChannel(alertChannel)

        val detectionChannel = NotificationChannel(
            CHANNEL_DETECTION_ALERTS,
            "Alertas de Detecção",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificações quando objetos são detectados pela IA"
        }
        notificationManager.createNotificationChannel(detectionChannel)

        val statusChannel = NotificationChannel(
            CHANNEL_STATUS,
            "Status do Sistema",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notificações de status como geofencing e automações"
        }
        notificationManager.createNotificationChannel(statusChannel)

        val digestChannel = NotificationChannel(
            CHANNEL_ALERT_DIGEST,
            "Resumo de Alertas",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Resumos periódicos de atividade das câmeras"
        }
        notificationManager.createNotificationChannel(digestChannel)
    }

    fun notifyCameraOffline(cameraId: String, cameraName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CAMERA_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Camera Offline")
            .setContentText("$cameraName perdeu conexão")
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

    fun notifyPersonDetected(cameraId: String, cameraName: String, count: Int = 1) {
        val text = if (count > 1) {
            "$count pessoas detectadas em $cameraName"
        } else {
            "Pessoa detectada em $cameraName"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_DETECTION_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Pessoa Detectada")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify("detection_${cameraId}".hashCode(), notification)
    }

    fun showDigestNotification(title: String, summary: String, details: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT_DIGEST)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$summary\n\n$details"),
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_DIGEST, notification)
    }

    fun showStatusNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
    }

    companion object {
        const val CHANNEL_CAMERA_ALERTS = "camera_alerts"
        const val CHANNEL_DETECTION_ALERTS = "detection_alerts"
        const val CHANNEL_STATUS = "status"
        const val CHANNEL_ALERT_DIGEST = "alert_digest"
        private const val NOTIFICATION_ID_STATUS = 9001
        private const val NOTIFICATION_ID_DIGEST = 9002
    }
}
