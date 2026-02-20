package com.vigipro.core.data.repository

import com.vigipro.core.data.db.CameraEventDao
import com.vigipro.core.data.db.CameraEventEntity
import com.vigipro.core.model.CameraEvent
import com.vigipro.core.model.CameraEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface EventRepository {
    fun getAllEvents(): Flow<List<CameraEvent>>
    fun getRecentEvents(limit: Int = 50): Flow<List<CameraEvent>>
    fun getEventsForCamera(cameraId: String): Flow<List<CameraEvent>>
    suspend fun logEvent(
        cameraId: String,
        cameraName: String,
        type: CameraEventType,
        message: String? = null,
    )
    suspend fun cleanOldEvents(keepDays: Int = 30)
}

@Singleton
class LocalEventRepository @Inject constructor(
    private val eventDao: CameraEventDao,
) : EventRepository {

    override fun getAllEvents() = eventDao.getAllEvents().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getRecentEvents(limit: Int) = eventDao.getRecentEvents(limit).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getEventsForCamera(cameraId: String) =
        eventDao.getEventsForCamera(cameraId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun logEvent(
        cameraId: String,
        cameraName: String,
        type: CameraEventType,
        message: String?,
    ) {
        eventDao.insert(
            CameraEventEntity(
                cameraId = cameraId,
                cameraName = cameraName,
                eventType = type.name,
                timestamp = System.currentTimeMillis(),
                message = message,
            ),
        )
    }

    override suspend fun cleanOldEvents(keepDays: Int) {
        val cutoff = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        eventDao.deleteOlderThan(cutoff)
    }

    private fun CameraEventEntity.toDomain() = CameraEvent(
        id = id,
        cameraId = cameraId,
        cameraName = cameraName,
        eventType = runCatching { CameraEventType.valueOf(eventType) }
            .getOrDefault(CameraEventType.WENT_OFFLINE),
        timestamp = timestamp,
        message = message,
    )
}
