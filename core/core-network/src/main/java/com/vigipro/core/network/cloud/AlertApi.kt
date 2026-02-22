package com.vigipro.core.network.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * HTTP client for VigiPro Alert Service (Cloud Run).
 * Envia alertas de camera e push notifications via FCM.
 */
class AlertApi(
    baseUrl: String = DEFAULT_BASE_URL,
) {

    private val url: String = baseUrl.trimEnd('/')

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
            })
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    /** Envia alerta individual (publica no Pub/Sub + push via FCM token). */
    suspend fun sendAlert(request: AlertRequest): AlertResponse {
        return client.post("$url/api/alert") {
            setBody(request)
        }.body()
    }

    /** Envia broadcast pra todos os membros de um site via FCM topic. */
    suspend fun sendBroadcast(request: BroadcastRequest): BroadcastResponse {
        return client.post("$url/api/alert/broadcast") {
            setBody(request)
        }.body()
    }

    /** Health check do alert service. */
    suspend fun healthCheck(): AlertHealthResponse {
        return client.get("$url/api/health").body()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://vigipro-alert-service-444500286209.southamerica-east1.run.app"
    }
}
