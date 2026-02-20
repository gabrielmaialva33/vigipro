package com.vigipro.core.data.repository

import com.vigipro.core.data.db.PrivacyZoneDao
import com.vigipro.core.data.db.PrivacyZoneEntity
import com.vigipro.core.model.PrivacyZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PrivacyZoneRepository {
    fun getZonesForCamera(cameraId: String): Flow<List<PrivacyZone>>
    suspend fun addZone(cameraId: String, left: Float, top: Float, right: Float, bottom: Float, label: String = "")
    suspend fun deleteZone(zone: PrivacyZone)
    suspend fun deleteAllForCamera(cameraId: String)
}

@Singleton
class LocalPrivacyZoneRepository @Inject constructor(
    private val privacyZoneDao: PrivacyZoneDao,
) : PrivacyZoneRepository {

    override fun getZonesForCamera(cameraId: String): Flow<List<PrivacyZone>> {
        return privacyZoneDao.getZonesForCamera(cameraId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addZone(
        cameraId: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        label: String,
    ) {
        val entity = PrivacyZoneEntity(
            id = UUID.randomUUID().toString(),
            cameraId = cameraId,
            label = label,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
        privacyZoneDao.insertZone(entity)
    }

    override suspend fun deleteZone(zone: PrivacyZone) {
        privacyZoneDao.deleteZone(zone.toEntity())
    }

    override suspend fun deleteAllForCamera(cameraId: String) {
        privacyZoneDao.deleteAllForCamera(cameraId)
    }

    private fun PrivacyZoneEntity.toDomain() = PrivacyZone(
        id = id,
        cameraId = cameraId,
        label = label,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )

    private fun PrivacyZone.toEntity() = PrivacyZoneEntity(
        id = id,
        cameraId = cameraId,
        label = label,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )
}
