package com.vigipro.core.data.repository

import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraStatus
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    fun getAllCameras(): Flow<List<Camera>>
    fun getCamerasBySite(siteId: String): Flow<List<Camera>>
    suspend fun getCameraById(id: String): Camera?
    suspend fun addCamera(camera: Camera)
    suspend fun updateCamera(camera: Camera)
    suspend fun deleteCamera(id: String)
    suspend fun updateCameraStatus(id: String, status: CameraStatus)
    suspend fun updateCameraThumbnailUrl(id: String, thumbnailUrl: String?)
}
