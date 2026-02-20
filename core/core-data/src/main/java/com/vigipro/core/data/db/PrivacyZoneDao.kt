package com.vigipro.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacyZoneDao {
    @Query("SELECT * FROM privacy_zones WHERE camera_id = :cameraId")
    fun getZonesForCamera(cameraId: String): Flow<List<PrivacyZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: PrivacyZoneEntity)

    @Delete
    suspend fun deleteZone(zone: PrivacyZoneEntity)

    @Query("DELETE FROM privacy_zones WHERE camera_id = :cameraId")
    suspend fun deleteAllForCamera(cameraId: String)
}
