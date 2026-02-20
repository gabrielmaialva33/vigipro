package com.vigipro.core.data.rtsp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long = 0,
    val errorMessage: String? = null,
)

@Singleton
class RtspConnectionTester @Inject constructor() {

    suspend fun testConnection(rtspUrl: String): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                val uri = URI(rtspUrl)
                val host = uri.host
                    ?: return@withContext ConnectionTestResult(
                        success = false,
                        errorMessage = "URL invalida: host nao encontrado",
                    )
                val port = if (uri.port > 0) uri.port else DEFAULT_RTSP_PORT

                val startTime = System.currentTimeMillis()

                val result = withTimeoutOrNull(TIMEOUT_MS) {
                    val socket = Socket()
                    try {
                        socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT_MS)
                        val elapsed = System.currentTimeMillis() - startTime
                        ConnectionTestResult(success = true, latencyMs = elapsed)
                    } finally {
                        runCatching { socket.close() }
                    }
                }

                result ?: ConnectionTestResult(
                    success = false,
                    errorMessage = "Tempo esgotado ao conectar",
                )
            } catch (e: Exception) {
                ConnectionTestResult(
                    success = false,
                    errorMessage = "Falha na conexao: ${e.localizedMessage}",
                )
            }
        }

    companion object {
        private const val TIMEOUT_MS = 10_000L
        private const val SOCKET_TIMEOUT_MS = 8_000
        private const val DEFAULT_RTSP_PORT = 554
    }
}
