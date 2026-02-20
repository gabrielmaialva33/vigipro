package com.vigipro.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraEventDao {

    @Query("SELECT * FROM camera_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<CameraEventEntity>>

    @Query("SELECT * FROM camera_events WHERE camera_id = :cameraId ORDER BY timestamp DESC")
    fun getEventsForCamera(cameraId: String): Flow<List<CameraEventEntity>>

    @Query("SELECT * FROM camera_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<CameraEventEntity>>

    @Insert
    suspend fun insert(event: CameraEventEntity)

    @Query("DELETE FROM camera_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
