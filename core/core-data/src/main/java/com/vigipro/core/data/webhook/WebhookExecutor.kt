package com.vigipro.core.data.webhook

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookExecutor @Inject constructor() {

    data class WebhookResult(
        val success: Boolean,
        val statusCode: Int = 0,
        val message: String = "",
    )

    suspend fun execute(
        url: String,
        method: String = "POST",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): WebhookResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null && method != "GET") {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = connection.responseCode
            connection.disconnect()
            WebhookResult(success = code in 200..299, statusCode = code)
        } catch (e: Exception) {
            WebhookResult(success = false, message = e.message ?: "Erro desconhecido")
        }
    }
}
