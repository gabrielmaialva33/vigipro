package com.vigipro.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebhookAction(
    val id: String,
    @SerialName("camera_id") val cameraId: String,
    val name: String,
    val url: String,
    val method: HttpMethod = HttpMethod.POST,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val icon: String = "power",
)

@Serializable
enum class HttpMethod {
    @SerialName("GET") GET,
    @SerialName("POST") POST,
    @SerialName("PUT") PUT,
}
