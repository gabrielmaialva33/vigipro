package com.vigipro.core.data.repository

import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraEvent
import com.vigipro.core.model.CameraStatus
import com.vigipro.core.network.cloud.CloudCameraDto
import com.vigipro.core.network.cloud.CloudEventDto
import com.vigipro.core.network.cloud.CloudHealthResponse
import com.vigipro.core.network.cloud.VigiProCloudApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VigiProCloudRepository @Inject constructor(
    private val api: VigiProCloudApi,
) : CloudRepository {

    override suspend fun healthCheck(): Result<CloudHealthResponse> = runCatching {
        api.healthCheck()
    }

    override suspend fun syncCameras(cameras: List<Camera>): Result<Int> = runCatching {
        val dtos = cameras.map { it.toCloudDto() }
        val response = api.syncCameras(dtos)
        response.synced
    }

    override suspend fun updateCameraStatus(cameraId: String, status: String): Result<Unit> = runCatching {
        api.updateCameraStatus(cameraId, status)
    }

    override suspend fun logEvent(event: CameraEvent): Result<Unit> = runCatching {
        api.logEvent(event.toCloudDto())
    }

    override suspend fun logEventsBatch(events: List<CameraEvent>): Result<Int> = runCatching {
        val dtos = events.map { it.toCloudDto() }
        val response = api.logEventsBatch(dtos)
        response.logged
    }

    override suspend fun fetchCloudCameras(): Result<List<Camera>> = runCatching {
        val response = api.listCameras()
        response.cameras.map { it.toDomain() }
    }
}

// --- Mappers ---

private fun Camera.toCloudDto() = CloudCameraDto(
    id = id,
    name = name,
    siteId = siteId,
    ptzCapable = ptzCapable,
    audioCapable = audioCapable,
    status = status.name,
    thumbnailUrl = thumbnailUrl,
    sortOrder = sortOrder,
    onvifAddress = onvifAddress,
    streamProfile = streamProfile,
    // rtspUrl and username are NEVER sent to cloud
)

private fun CloudCameraDto.toDomain() = Camera(
    id = id,
    siteId = siteId,
    name = name,
    ptzCapable = ptzCapable,
    audioCapable = audioCapable,
    status = try {
        CameraStatus.valueOf(status)
    } catch (_: Exception) {
        CameraStatus.OFFLINE
    },
    thumbnailUrl = thumbnailUrl,
    sortOrder = sortOrder,
    onvifAddress = onvifAddress,
    streamProfile = streamProfile,
)

private fun CameraEvent.toCloudDto() = CloudEventDto(
    cameraId = cameraId,
    cameraName = cameraName,
    eventType = eventType.name,
    message = message,
    timestamp = timestamp,
)
