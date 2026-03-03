package com.vigipro.core.network.cloud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * HTTP client for VigiPro Cloud (Phoenix backend).
 * Uses Supabase JWT for authentication — same token the app already has.
 */
class VigiProCloudApi(
    private val tokenProvider: () -> String?,
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

    // --- Health ---

    suspend fun healthCheck(): CloudHealthResponse {
        return client.get("$url/api/health").body()
    }

    // --- Demo cameras (public, no auth) ---

    suspend fun getDemoCameras(): DemoCamerasResponse {
        return client.get("$url/api/demo/cameras").body()
    }

    // --- Public cameras (curated catalog, no auth) ---

    suspend fun getPublicCameras(
        category: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PublicCamerasResponse {
        return client.get("$url/api/public/cameras") {
            category?.let { parameter("category", it) }
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()
    }

    suspend fun getPublicCategories(): CategoriesResponse {
        return client.get("$url/api/public/categories").body()
    }

    // --- Cameras ---

    suspend fun listCameras(siteId: String? = null): CloudCamerasResponse {
        return client.get("$url/api/v1/cameras") {
            bearerAuth(requireToken())
            siteId?.let { parameter("site_id", it) }
        }.body()
    }

    suspend fun getCamera(id: String): CloudCameraWrapper {
        return client.get("$url/api/v1/cameras/$id") {
            bearerAuth(requireToken())
        }.body()
    }

    suspend fun createCamera(camera: CloudCameraDto): CloudCameraWrapper {
        return client.post("$url/api/v1/cameras") {
            bearerAuth(requireToken())
            setBody(mapOf("camera" to camera))
        }.body()
    }

    suspend fun updateCamera(id: String, camera: CloudCameraDto): CloudCameraWrapper {
        return client.put("$url/api/v1/cameras/$id") {
            bearerAuth(requireToken())
            setBody(mapOf("camera" to camera))
        }.body()
    }

    suspend fun deleteCamera(id: String): CloudStatusResponse {
        return client.delete("$url/api/v1/cameras/$id") {
            bearerAuth(requireToken())
        }.body()
    }

    suspend fun updateCameraStatus(id: String, status: String): CloudStatusResponse {
        return client.patch("$url/api/v1/cameras/$id/status") {
            bearerAuth(requireToken())
            setBody(mapOf("status" to status))
        }.body()
    }

    suspend fun syncCameras(cameras: List<CloudCameraDto>): CloudSyncResponse {
        return client.post("$url/api/v1/cameras/sync") {
            bearerAuth(requireToken())
            setBody(mapOf("cameras" to cameras))
        }.body()
    }

    // --- Events ---

    suspend fun listEvents(limit: Int = 50, cameraId: String? = null): CloudEventsResponse {
        return client.get("$url/api/v1/events") {
            bearerAuth(requireToken())
            parameter("limit", limit)
            cameraId?.let { parameter("camera_id", it) }
        }.body()
    }

    suspend fun logEvent(event: CloudEventDto): CloudEventWrapper {
        return client.post("$url/api/v1/events") {
            bearerAuth(requireToken())
            setBody(mapOf("event" to event))
        }.body()
    }

    suspend fun logEventsBatch(events: List<CloudEventDto>): CloudBatchResponse {
        return client.post("$url/api/v1/events/batch") {
            bearerAuth(requireToken())
            setBody(mapOf("events" to events))
        }.body()
    }

    // --- Private ---

    private fun requireToken(): String {
        return tokenProvider()
            ?: throw IllegalStateException("No auth token available. User may not be signed in.")
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://vigipro.mahina.cloud"
    }
}
