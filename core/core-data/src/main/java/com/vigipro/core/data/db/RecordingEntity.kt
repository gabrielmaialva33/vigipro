package com.vigipro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings",
    indices = [
        Index("camera_id"),
        Index("start_time"),
    ],
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "camera_id") val cameraId: String,
    @ColumnInfo(name = "camera_name") val cameraName: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long = 0,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String? = null,
)
