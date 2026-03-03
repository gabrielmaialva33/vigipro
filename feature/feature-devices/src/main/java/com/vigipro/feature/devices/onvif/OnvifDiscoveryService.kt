package com.vigipro.feature.devices.onvif

import android.content.Context
import android.net.wifi.WifiManager
import com.seanproctor.onvifcamera.OnvifDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class OnvifDeviceInfo(
    val address: String,
    val name: String?,
    val manufacturer: String?,
    val model: String?,
    val rtspUrl: String?,
    val snapshotUrl: String?,
    val ptzCapable: Boolean,
    val audioCapable: Boolean,
)

@Singleton
class OnvifDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun discoverDevices(timeoutMs: Long = DISCOVERY_TIMEOUT_MS): List<String> =
        withContext(Dispatchers.IO) {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val multicastLock = wifiManager.createMulticastLock("vigipro_onvif_discovery")

            try {
                multicastLock.setReferenceCounted(true)
                multicastLock.acquire()

                val probeMessage = buildProbeMessage()
                val addresses = mutableSetOf<String>()

                DatagramSocket().use { socket ->
                    socket.soTimeout = timeoutMs.toInt()
                    socket.broadcast = true

                    val probeBytes = probeMessage.toByteArray()
                    val destAddress = InetAddress.getByName(WS_DISCOVERY_ADDRESS)
                    val packet = DatagramPacket(
                        probeBytes,
                        probeBytes.size,
                        destAddress,
                        WS_DISCOVERY_PORT,
                    )
                    socket.send(packet)

                    val buffer = ByteArray(BUFFER_SIZE)
                    val startTime = System.currentTimeMillis()

                    while (System.currentTimeMillis() - startTime < timeoutMs) {
                        try {
                            val response = DatagramPacket(buffer, buffer.size)
                            socket.receive(response)
                            val responseText = String(response.data, 0, response.length)
                            parseXAddrs(responseText).forEach { addr ->
                                addresses.add(addr)
                            }
                        } catch (_: java.net.SocketTimeoutException) {
                            break
                        }
                    }
                }

                addresses.toList()
            } finally {
                if (multicastLock.isHeld) {
                    multicastLock.release()
                }
            }
        }

    suspend fun getDeviceDetails(
        address: String,
        username: String = "",
        password: String = "",
    ): OnvifDeviceInfo? = withContext(Dispatchers.IO) {
        try {
            val result = withTimeoutOrNull(DEVICE_TIMEOUT_MS) {
                val device = OnvifDevice.requestDevice(address, username, password)
                val deviceInfo = runCatching { device.getDeviceInformation() }.getOrNull()
                val profiles = runCatching { device.getProfiles() }.getOrNull()

                val streamProfile = profiles?.firstOrNull { it.canStream() }
                val snapshotProfile = profiles?.firstOrNull { it.canSnapshot() }

                val streamUri = streamProfile?.let {
                    runCatching { device.getStreamURI(it) }.getOrNull()
                }
                val snapshotUri = snapshotProfile?.let {
                    runCatching { device.getSnapshotURI(it) }.getOrNull()
                }

                // v1.8.3 MediaProfile doesn't expose PTZ/audio config
                // These will be detected via profile name heuristics
                val hasPtz = profiles?.any {
                    it.name.contains("ptz", ignoreCase = true)
                } ?: false
                val hasAudio = profiles?.any {
                    it.name.contains("audio", ignoreCase = true) ||
                        it.encoding.contains("aac", ignoreCase = true) ||
                        it.encoding.contains("g711", ignoreCase = true)
                } ?: false

                OnvifDeviceInfo(
                    address = address,
                    name = deviceInfo?.model ?: address,
                    manufacturer = deviceInfo?.manufacturer,
                    model = deviceInfo?.model,
                    rtspUrl = streamUri,
                    snapshotUrl = snapshotUri,
                    ptzCapable = hasPtz,
                    audioCapable = hasAudio,
                )
            }
            result
        } catch (_: Exception) {
            OnvifDeviceInfo(
                address = address,
                name = address,
                manufacturer = null,
                model = null,
                rtspUrl = null,
                snapshotUrl = null,
                ptzCapable = false,
                audioCapable = false,
            )
        }
    }

    private fun buildProbeMessage(): String {
        val uuid = UUID.randomUUID()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope"
                        xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                        xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery"
                        xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
              <e:Header>
                <w:MessageID>uuid:$uuid</w:MessageID>
                <w:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>
                <w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>
              </e:Header>
              <e:Body>
                <d:Probe>
                  <d:Types>dn:NetworkVideoTransmitter</d:Types>
                </d:Probe>
              </e:Body>
            </e:Envelope>
        """.trimIndent()
    }

    private fun parseXAddrs(xml: String): List<String> {
        val addresses = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "XAddrs") {
                    val urls = parser.nextText().trim().split("\\s+".toRegex())
                    for (url in urls) {
                        try {
                            val uri = java.net.URI(url)
                            val host = uri.host ?: continue
                            val port = if (uri.port > 0) uri.port else 80
                            addresses.add("$host:$port")
                        } catch (_: Exception) {
                            // Skip malformed URLs
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Fallback: return whatever was parsed so far
        }
        return addresses.distinct()
    }

    companion object {
        private const val WS_DISCOVERY_ADDRESS = "239.255.255.250"
        private const val WS_DISCOVERY_PORT = 3702
        private const val DISCOVERY_TIMEOUT_MS = 5_000L
        private const val DEVICE_TIMEOUT_MS = 10_000L
        private const val BUFFER_SIZE = 4096
    }
}
