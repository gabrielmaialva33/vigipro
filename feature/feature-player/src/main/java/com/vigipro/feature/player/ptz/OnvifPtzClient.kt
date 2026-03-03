package com.vigipro.feature.player.ptz

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnvifPtzClient @Inject constructor() {

    @Volatile private var ptzServiceUrl: String? = null
    @Volatile private var profileToken: String? = null
    @Volatile private var username: String = ""
    @Volatile private var password: String = ""

    private val httpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 5_000
        }
    }

    val isConnected: Boolean get() = ptzServiceUrl != null && profileToken != null

    suspend fun connect(
        onvifAddress: String,
        username: String,
        password: String,
        streamProfileToken: String?,
    ): Boolean {
        this.username = username
        this.password = password

        return try {
            val deviceUrl = buildDeviceUrl(onvifAddress)
            val servicesXml = sendSoapRequest(
                deviceUrl,
                PtzSoapCommands.buildGetServicesRequest(username, password),
            ) ?: return false

            val ptzUrl = parsePtzServiceUrl(servicesXml)
            if (ptzUrl == null) {
                // Fallback: try common PTZ paths
                val baseUri = URI(deviceUrl)
                val fallbacks = listOf(
                    "${baseUri.scheme}://${baseUri.host}:${baseUri.port}/onvif/ptz_service",
                    "${baseUri.scheme}://${baseUri.host}:${baseUri.port}/onvif/PTZ",
                )
                for (fallback in fallbacks) {
                    val testToken = streamProfileToken ?: "Profile_1"
                    val testXml = PtzSoapCommands.buildGetPresetsRequest(testToken, username, password)
                    val response = sendSoapRequest(fallback, testXml)
                    if (response != null) {
                        ptzServiceUrl = fallback
                        profileToken = testToken
                        return true
                    }
                }
                ptzServiceUrl = null
                profileToken = null
                return false
            }

            ptzServiceUrl = ptzUrl
            profileToken = streamProfileToken ?: "Profile_1"
            true
        } catch (e: Exception) {
            Log.w(TAG, "PTZ connect failed", e)
            false
        }
    }

    suspend fun continuousMove(x: Float, y: Float, z: Float): Result<Unit> {
        val url = ptzServiceUrl ?: return Result.failure(Exception("PTZ nao conectado"))
        val token = profileToken ?: return Result.failure(Exception("Profile nao definido"))
        return try {
            val xml = PtzSoapCommands.buildContinuousMoveRequest(token, x, y, z, username, password)
            sendSoapRequest(url, xml)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stop(): Result<Unit> {
        val url = ptzServiceUrl ?: return Result.failure(Exception("PTZ nao conectado"))
        val token = profileToken ?: return Result.failure(Exception("Profile nao definido"))
        return try {
            val xml = PtzSoapCommands.buildStopRequest(token, username, password)
            sendSoapRequest(url, xml)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPresets(): Result<List<PtzPreset>> {
        val url = ptzServiceUrl ?: return Result.failure(Exception("PTZ nao conectado"))
        val token = profileToken ?: return Result.failure(Exception("Profile nao definido"))
        return try {
            val xml = PtzSoapCommands.buildGetPresetsRequest(token, username, password)
            val response = sendSoapRequest(url, xml)
                ?: return Result.success(emptyList())
            Result.success(parsePresets(response))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun gotoPreset(presetToken: String): Result<Unit> {
        val url = ptzServiceUrl ?: return Result.failure(Exception("PTZ nao conectado"))
        val token = profileToken ?: return Result.failure(Exception("Profile nao definido"))
        return try {
            val xml = PtzSoapCommands.buildGotoPresetRequest(token, presetToken, username, password)
            sendSoapRequest(url, xml)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Synchronized
    fun disconnect() {
        ptzServiceUrl = null
        profileToken = null
        username = ""
        password = ""
    }

    private fun buildDeviceUrl(onvifAddress: String): String {
        val address = onvifAddress.trim()
        return when {
            address.startsWith("http") && address.contains("/onvif/") -> address
            address.startsWith("http") -> "${address.trimEnd('/')}/onvif/device_service"
            address.contains(":") -> "http://$address/onvif/device_service"
            else -> "http://$address:80/onvif/device_service"
        }
    }

    private suspend fun sendSoapRequest(url: String, soapXml: String): String? {
        return try {
            val response = httpClient.post(url) {
                contentType(ContentType("application", "soap+xml"))
                setBody(soapXml)
            }
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                val errorBody = try { response.bodyAsText() } catch (_: Exception) { null }
                Log.w(TAG, "SOAP error ${response.status}: $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "SOAP request failed: ${e.message}")
            null
        }
    }

    private fun parsePtzServiceUrl(xml: String): String? {
        val ptzNamespace = "http://www.onvif.org/ver20/ptz/wsdl"
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inService = false
            var foundPtzNamespace = false
            var xAddr: String? = null

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "Service" -> {
                                inService = true
                                foundPtzNamespace = false
                                xAddr = null
                            }
                            "Namespace" -> if (inService) {
                                val text = parser.nextText().trim()
                                if (text == ptzNamespace) foundPtzNamespace = true
                            }
                            "XAddr" -> if (inService) {
                                xAddr = parser.nextText().trim()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "Service") {
                            if (foundPtzNamespace && xAddr != null) return xAddr
                            inService = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse PTZ service URL", e)
        }
        return null
    }

    private fun parsePresets(xml: String): List<PtzPreset> {
        val presets = mutableListOf<PtzPreset>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "Preset") {
                    val token = parser.getAttributeValue(null, "token") ?: ""
                    var name = ""
                    while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "Preset")) {
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Name") {
                            name = parser.nextText()
                        }
                        parser.next()
                    }
                    if (token.isNotEmpty()) {
                        presets.add(PtzPreset(token = token, name = name))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse presets", e)
        }
        return presets
    }

    companion object {
        private const val TAG = "OnvifPtzClient"
    }
}
