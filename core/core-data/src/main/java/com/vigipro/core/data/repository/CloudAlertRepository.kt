package com.vigipro.core.data.repository

import com.vigipro.core.network.cloud.AlertApi
import com.vigipro.core.network.cloud.AlertRequest
import com.vigipro.core.network.cloud.BroadcastRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudAlertRepository @Inject constructor(
    private val alertApi: AlertApi,
) : AlertRepository {

    override suspend fun sendAlert(
        userId: String,
        cameraName: String,
        alertType: String,
        siteId: String?,
        message: String?,
        fcmToken: String?,
    ): Result<Unit> = runCatching {
        alertApi.sendAlert(
            AlertRequest(
                userId = userId,
                siteId = siteId,
                cameraName = cameraName,
                alertType = alertType,
                message = message,
                fcmToken = fcmToken,
            ),
        )
        Timber.d("Alerta enviado: $alertType para camera $cameraName")
    }

    override suspend fun sendBroadcast(
        siteId: String,
        alertType: String,
        cameraName: String?,
        message: String?,
    ): Result<Unit> = runCatching {
        alertApi.sendBroadcast(
            BroadcastRequest(
                siteId = siteId,
                cameraName = cameraName,
                alertType = alertType,
                message = message,
            ),
        )
        Timber.d("Broadcast enviado: $alertType para site $siteId")
    }

    override suspend fun isServiceHealthy(): Boolean {
        return try {
            val response = alertApi.healthCheck()
            response.status == "healthy"
        } catch (e: Exception) {
            Timber.w(e, "Alert service health check falhou")
            false
        }
    }
}
