package com.vigipro.core.data.repository

import com.vigipro.core.data.db.CameraDao
import com.vigipro.core.data.db.toDomain
import com.vigipro.core.data.db.toEntity
import com.vigipro.core.model.Camera
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCameraRepository @Inject constructor(
    private val cameraDao: CameraDao,
) : CameraRepository {

    override fun getAllCameras(): Flow<List<Camera>> =
        cameraDao.getAllCameras().map { entities -> entities.map { it.toDomain() } }

    override fun getCamerasBySite(siteId: String): Flow<List<Camera>> =
        cameraDao.getCamerasBySite(siteId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getCameraById(id: String): Camera? =
        cameraDao.getCameraById(id)?.toDomain()

    override suspend fun addCamera(camera: Camera) =
        cameraDao.insert(camera.toEntity())

    override suspend fun updateCamera(camera: Camera) =
        cameraDao.update(camera.toEntity())

    override suspend fun deleteCamera(id: String) =
        cameraDao.deleteById(id)
}
