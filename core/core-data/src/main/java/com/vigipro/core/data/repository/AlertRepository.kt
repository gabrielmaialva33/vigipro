package com.vigipro.core.data.repository

/**
 * Repository for sending alerts via VigiPro Cloud Run service.
 * Publica eventos no Pub/Sub e envia push notifications via FCM.
 */
interface AlertRepository {

    /** Envia alerta individual pra um usuario (Pub/Sub + push opcional). */
    suspend fun sendAlert(
        userId: String,
        cameraName: String,
        alertType: String,
        siteId: String? = null,
        message: String? = null,
        fcmToken: String? = null,
    ): Result<Unit>

    /** Envia broadcast pra todos os membros de um site via FCM topic. */
    suspend fun sendBroadcast(
        siteId: String,
        alertType: String,
        cameraName: String? = null,
        message: String? = null,
    ): Result<Unit>

    /** Verifica se o alert service esta acessivel. */
    suspend fun isServiceHealthy(): Boolean
}
