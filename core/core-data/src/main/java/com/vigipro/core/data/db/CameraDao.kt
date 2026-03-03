package com.vigipro.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras ORDER BY sort_order ASC")
    fun getAllCameras(): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE site_id = :siteId ORDER BY sort_order ASC")
    fun getCamerasBySite(siteId: String): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: String): CameraEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(camera: CameraEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cameras: List<CameraEntity>)

    @Update
    suspend fun update(camera: CameraEntity)

    @Query("DELETE FROM cameras WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cameras WHERE site_id = :siteId")
    suspend fun deleteBySite(siteId: String)

    @Query("UPDATE cameras SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE cameras SET thumbnail_url = :thumbnailUrl WHERE id = :id")
    suspend fun updateThumbnailUrl(id: String, thumbnailUrl: String?)

    @Query("SELECT COUNT(*) FROM cameras WHERE site_id = :siteId")
    suspend fun countBySite(siteId: String): Int

    @Query("SELECT * FROM cameras WHERE site_id = :siteId ORDER BY sort_order ASC")
    suspend fun getCamerasBySiteSnapshot(siteId: String): List<CameraEntity>
}
