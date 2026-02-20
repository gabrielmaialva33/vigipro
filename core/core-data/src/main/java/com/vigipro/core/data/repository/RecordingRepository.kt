package com.vigipro.core.data.repository

import com.vigipro.core.data.db.RecordingDao
import com.vigipro.core.data.db.RecordingEntity
import com.vigipro.core.model.Recording
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<Recording>>
    fun getRecordingsByCamera(cameraId: String): Flow<List<Recording>>
    suspend fun getById(id: Long): Recording?
    suspend fun startRecording(cameraId: String, cameraName: String, filePath: String): Long
    suspend fun finishRecording(id: Long, fileSize: Long, durationMs: Long)
    suspend fun deleteRecording(id: Long)
    suspend fun getTotalSize(): Long
}

@Singleton
class LocalRecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao,
) : RecordingRepository {

    override fun getAllRecordings(): Flow<List<Recording>> =
        recordingDao.getAllRecordings().map { entities -> entities.map { it.toDomain() } }

    override fun getRecordingsByCamera(cameraId: String): Flow<List<Recording>> =
        recordingDao.getRecordingsByCamera(cameraId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): Recording? =
        recordingDao.getById(id)?.toDomain()

    override suspend fun startRecording(cameraId: String, cameraName: String, filePath: String): Long {
        val entity = RecordingEntity(
            cameraId = cameraId,
            cameraName = cameraName,
            filePath = filePath,
            startTime = System.currentTimeMillis(),
        )
        return recordingDao.insert(entity)
    }

    override suspend fun finishRecording(id: Long, fileSize: Long, durationMs: Long) {
        val entity = recordingDao.getById(id) ?: return
        recordingDao.update(
            entity.copy(
                endTime = System.currentTimeMillis(),
                fileSize = fileSize,
                durationMs = durationMs,
            ),
        )
    }

    override suspend fun deleteRecording(id: Long) {
        val entity = recordingDao.getById(id)
        if (entity != null) {
            File(entity.filePath).delete()
            entity.thumbnailPath?.let { File(it).delete() }
            recordingDao.deleteById(id)
        }
    }

    override suspend fun getTotalSize(): Long =
        recordingDao.getTotalSize() ?: 0L
}

private fun RecordingEntity.toDomain() = Recording(
    id = id,
    cameraId = cameraId,
    cameraName = cameraName,
    filePath = filePath,
    startTime = startTime,
    endTime = endTime,
    fileSize = fileSize,
    durationMs = durationMs,
    thumbnailPath = thumbnailPath,
)
