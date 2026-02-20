package com.vigipro.feature.player.ptz

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object PtzSoapCommands {

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    fun buildGetServicesRequest(username: String = "", password: String = ""): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:tds="http://www.onvif.org/ver10/device/wsdl">
          ${buildSecurityHeader(username, password)}
          <s:Body>
            <tds:GetServices>
              <tds:IncludeCapability>false</tds:IncludeCapability>
            </tds:GetServices>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    fun buildContinuousMoveRequest(
        profileToken: String,
        x: Float,
        y: Float,
        z: Float,
        username: String,
        password: String,
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl"
                    xmlns:tt="http://www.onvif.org/ver10/schema">
          ${buildSecurityHeader(username, password)}
          <s:Body>
            <tptz:ContinuousMove>
              <tptz:ProfileToken>${xmlEscape(profileToken)}</tptz:ProfileToken>
              <tptz:Velocity>
                <tt:PanTilt x="$x" y="$y"/>
                <tt:Zoom x="$z"/>
              </tptz:Velocity>
            </tptz:ContinuousMove>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    fun buildStopRequest(
        profileToken: String,
        username: String,
        password: String,
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
          ${buildSecurityHeader(username, password)}
          <s:Body>
            <tptz:Stop>
              <tptz:ProfileToken>${xmlEscape(profileToken)}</tptz:ProfileToken>
              <tptz:PanTilt>true</tptz:PanTilt>
              <tptz:Zoom>true</tptz:Zoom>
            </tptz:Stop>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    fun buildGetPresetsRequest(
        profileToken: String,
        username: String,
        password: String,
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
          ${buildSecurityHeader(username, password)}
          <s:Body>
            <tptz:GetPresets>
              <tptz:ProfileToken>${xmlEscape(profileToken)}</tptz:ProfileToken>
            </tptz:GetPresets>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    fun buildGotoPresetRequest(
        profileToken: String,
        presetToken: String,
        username: String,
        password: String,
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl">
          ${buildSecurityHeader(username, password)}
          <s:Body>
            <tptz:GotoPreset>
              <tptz:ProfileToken>${xmlEscape(profileToken)}</tptz:ProfileToken>
              <tptz:PresetToken>${xmlEscape(presetToken)}</tptz:PresetToken>
            </tptz:GotoPreset>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    private fun buildSecurityHeader(username: String, password: String): String {
        if (username.isBlank()) return "<s:Header/>"

        val nonceBytes = ByteArray(20).also { SecureRandom().nextBytes(it) }
        val nonce = Base64.encode(nonceBytes)
        val created = java.time.Instant.now()
            .truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
            .toString()

        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(nonceBytes)
        digest.update(created.toByteArray(Charsets.UTF_8))
        digest.update(password.toByteArray(Charsets.UTF_8))
        val passwordDigest = Base64.encode(digest.digest())

        return """
          <s:Header>
            <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                           xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
              <wsse:UsernameToken>
                <wsse:Username>${xmlEscape(username)}</wsse:Username>
                <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">$passwordDigest</wsse:Password>
                <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">$nonce</wsse:Nonce>
                <wsu:Created>$created</wsu:Created>
              </wsse:UsernameToken>
            </wsse:Security>
          </s:Header>
        """.trimIndent()
    }
}
