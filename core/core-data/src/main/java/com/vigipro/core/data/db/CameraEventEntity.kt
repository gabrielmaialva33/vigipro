package com.vigipro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "camera_events",
    indices = [
        Index(value = ["camera_id"]),
        Index(value = ["timestamp"]),
    ],
)
data class CameraEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "camera_id") val cameraId: String,
    @ColumnInfo(name = "camera_name") val cameraName: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    val timestamp: Long,
    val message: String? = null,
)
