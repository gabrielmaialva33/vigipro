package com.vigipro.core.data.audio

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RTSP backchannel client for ONVIF Profile T 2-way audio.
 * Sends G.711 audio to camera speaker via RTP over TCP interleaved transport.
 *
 * Uses raw Socket (not OkHttp) to maintain persistent TCP connection for bidirectional streaming.
 */
@Singleton
class RtspBackchannelClient @Inject constructor() {

    @Volatile private var socket: Socket? = null
    @Volatile private var outputStream: OutputStream? = null
    @Volatile private var reader: BufferedReader? = null
    @Volatile private var sessionId: String? = null
    @Volatile private var interleavedChannel: Int = 2
    @Volatile private var payloadType: Int = 8 // PCMA by default

    val isConnected: Boolean get() = socket?.isConnected == true && sessionId != null

    /**
     * Connect to camera RTSP backchannel.
     * Sends DESCRIBE with backchannel requirement, SETUP, and PLAY.
     *
     * @return payload type (0=PCMU, 8=PCMA) or -1 on failure
     */
    suspend fun connect(
        rtspUrl: String,
        username: String,
        password: String,
    ): Int = withContext(Dispatchers.IO) {
        try {
            val uri = URI(rtspUrl)
            val host = uri.host
            val port = if (uri.port > 0) uri.port else 554

            val sock = Socket(host, port)
            sock.soTimeout = 10_000
            socket = sock
            outputStream = sock.getOutputStream()
            reader = BufferedReader(InputStreamReader(sock.getInputStream()))

            val authHeader = buildBasicAuth(username, password)
            var cSeq = 1

            // 1. DESCRIBE with backchannel requirement
            val describeReq = buildRtspRequest(
                method = "DESCRIBE",
                url = rtspUrl,
                cSeq = cSeq++,
                extraHeaders = buildString {
                    appendLine("Accept: application/sdp")
                    appendLine("Require: www.onvif.org/ver20/backchannel")
                    if (authHeader != null) appendLine("Authorization: $authHeader")
                },
            )
            sendRtspRequest(describeReq)
            val describeResponse = readRtspResponse()

            if (!describeResponse.statusLine.contains("200")) {
                Log.w(TAG, "DESCRIBE failed: ${describeResponse.statusLine}")
                disconnect()
                return@withContext -1
            }

            // 2. Parse SDP to find backchannel track
            val sdp = describeResponse.body
            val backchannelTrack = parseSdpBackchannel(sdp, rtspUrl)
            if (backchannelTrack == null) {
                Log.w(TAG, "No backchannel track found in SDP")
                disconnect()
                return@withContext -1
            }

            payloadType = backchannelTrack.payloadType
            val trackUrl = backchannelTrack.controlUrl

            // 3. SETUP backchannel track with TCP interleaved
            val setupReq = buildRtspRequest(
                method = "SETUP",
                url = trackUrl,
                cSeq = cSeq++,
                extraHeaders = buildString {
                    appendLine("Transport: RTP/AVP/TCP;unicast;interleaved=$interleavedChannel-${interleavedChannel + 1}")
                    appendLine("Require: www.onvif.org/ver20/backchannel")
                    if (authHeader != null) appendLine("Authorization: $authHeader")
                },
            )
            sendRtspRequest(setupReq)
            val setupResponse = readRtspResponse()

            if (!setupResponse.statusLine.contains("200")) {
                Log.w(TAG, "SETUP failed: ${setupResponse.statusLine}")
                disconnect()
                return@withContext -1
            }

            // Extract session ID
            sessionId = setupResponse.headers["Session"]?.split(";")?.firstOrNull()?.trim()

            // 4. PLAY
            val playReq = buildRtspRequest(
                method = "PLAY",
                url = rtspUrl,
                cSeq = cSeq++,
                extraHeaders = buildString {
                    appendLine("Session: ${sessionId}")
                    appendLine("Require: www.onvif.org/ver20/backchannel")
                    if (authHeader != null) appendLine("Authorization: $authHeader")
                },
            )
            sendRtspRequest(playReq)
            val playResponse = readRtspResponse()

            if (!playResponse.statusLine.contains("200")) {
                Log.w(TAG, "PLAY failed: ${playResponse.statusLine}")
                disconnect()
                return@withContext -1
            }

            Log.i(TAG, "Backchannel connected: codec=${if (payloadType == 8) "PCMA" else "PCMU"}")
            payloadType
        } catch (e: Exception) {
            Log.e(TAG, "Backchannel connect failed", e)
            disconnect()
            -1
        }
    }

    /**
     * Send an RTP audio packet via TCP interleaved transport.
     */
    fun sendAudio(rtpPacket: ByteArray) {
        try {
            val wrapped = RtpPacketBuilder.wrapTcpInterleaved(interleavedChannel, rtpPacket)
            outputStream?.write(wrapped)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send audio packet", e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            val sid = sessionId
            if (sid != null && socket?.isConnected == true) {
                val teardownReq = buildRtspRequest(
                    method = "TEARDOWN",
                    url = "",
                    cSeq = 99,
                    extraHeaders = "Session: $sid\r\n",
                )
                try {
                    sendRtspRequest(teardownReq)
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { } finally {
            sessionId = null
            try { reader?.close() } catch (_: Exception) { }
            try { outputStream?.close() } catch (_: Exception) { }
            try { socket?.close() } catch (_: Exception) { }
            reader = null
            outputStream = null
            socket = null
        }
    }

    // --- RTSP helpers ---

    private fun buildRtspRequest(
        method: String,
        url: String,
        cSeq: Int,
        extraHeaders: String,
    ): String = buildString {
        appendLine("$method $url RTSP/1.0")
        appendLine("CSeq: $cSeq")
        append(extraHeaders)
        appendLine() // blank line = end of headers
    }

    private fun sendRtspRequest(request: String) {
        outputStream?.write(request.toByteArray(Charsets.US_ASCII))
        outputStream?.flush()
    }

    private fun readRtspResponse(): RtspResponse {
        val rd = reader ?: throw IllegalStateException("Reader not initialized")
        val statusLine = rd.readLine() ?: ""
        val headers = mutableMapOf<String, String>()
        var contentLength = 0

        // Read headers
        while (true) {
            val line = rd.readLine() ?: break
            if (line.isEmpty()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
                if (key.equals("Content-Length", ignoreCase = true)) {
                    contentLength = value.toIntOrNull() ?: 0
                }
            }
        }

        // Read body if present
        val body = if (contentLength > 0) {
            val chars = CharArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val n = rd.read(chars, read, contentLength - read)
                if (n == -1) break
                read += n
            }
            String(chars, 0, read)
        } else ""

        return RtspResponse(statusLine, headers, body)
    }

    private fun buildBasicAuth(username: String, password: String): String? {
        if (username.isBlank()) return null
        val credentials = "$username:$password"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        return "Basic $encoded"
    }

    // --- SDP parsing ---

    private data class BackchannelTrack(
        val controlUrl: String,
        val payloadType: Int,
    )

    private fun parseSdpBackchannel(sdp: String, baseUrl: String): BackchannelTrack? {
        val lines = sdp.lines()
        var inMediaSection = false
        var isSendonly = false
        var mediaPayloadType = 8
        var controlAttr: String? = null

        for (line in lines) {
            if (line.startsWith("m=audio")) {
                // New audio section — reset
                if (inMediaSection && isSendonly && controlAttr != null) {
                    return buildTrack(controlAttr, mediaPayloadType, baseUrl)
                }
                inMediaSection = true
                isSendonly = false
                controlAttr = null
                // Parse payload type from m= line: "m=audio 0 RTP/AVP 8"
                val parts = line.split(" ")
                if (parts.size >= 4) {
                    mediaPayloadType = parts[3].trim().toIntOrNull() ?: 8
                }
            } else if (inMediaSection) {
                when {
                    line.trim() == "a=sendonly" -> isSendonly = true
                    line.startsWith("a=control:") -> controlAttr = line.substringAfter("a=control:").trim()
                    line.startsWith("a=rtpmap:") -> {
                        // e.g. "a=rtpmap:8 PCMA/8000"
                        val rtpmapParts = line.substringAfter("a=rtpmap:").split(" ")
                        if (rtpmapParts.isNotEmpty()) {
                            mediaPayloadType = rtpmapParts[0].trim().toIntOrNull() ?: mediaPayloadType
                        }
                    }
                    line.startsWith("m=") -> {
                        // Finished audio section, check if it was backchannel
                        if (isSendonly && controlAttr != null) {
                            return buildTrack(controlAttr, mediaPayloadType, baseUrl)
                        }
                        inMediaSection = false
                    }
                }
            }
        }

        // Check last section
        if (inMediaSection && isSendonly && controlAttr != null) {
            return buildTrack(controlAttr, mediaPayloadType, baseUrl)
        }

        return null
    }

    private fun buildTrack(control: String, payloadType: Int, baseUrl: String): BackchannelTrack {
        val controlUrl = if (control.startsWith("rtsp://")) {
            control
        } else {
            "${baseUrl.trimEnd('/')}/$control"
        }
        return BackchannelTrack(controlUrl, payloadType)
    }

    private data class RtspResponse(
        val statusLine: String,
        val headers: Map<String, String>,
        val body: String,
    )

    companion object {
        private const val TAG = "RtspBackchannel"
    }
}
