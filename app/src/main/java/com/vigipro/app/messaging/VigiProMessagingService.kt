package com.vigipro.app.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vigipro.app.MainActivity
import com.vigipro.app.R
import timber.log.Timber

class VigiProMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token atualizado: ${token.take(10)}...")
        // TODO: Enviar token pro backend quando integrar push server-side
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM mensagem recebida: ${message.data}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "VigiPro"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        showNotification(title, body, message.data)
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Criar canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alertas de Seguranca",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notificacoes de alertas de cameras e eventos de seguranca"
            }
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Geral",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notificacoes gerais do VigiPro"
            }
            notificationManager.createNotificationChannels(listOf(alertChannel, generalChannel))
        }

        val channelId = when (data["type"]) {
            "alert", "motion", "intrusion" -> CHANNEL_ALERTS
            else -> CHANNEL_GENERAL
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ALERTS = "vigipro_alerts"
        const val CHANNEL_GENERAL = "vigipro_general"
    }
}
