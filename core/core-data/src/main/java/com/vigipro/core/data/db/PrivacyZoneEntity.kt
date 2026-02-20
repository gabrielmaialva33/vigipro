package com.vigipro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "privacy_zones")
data class PrivacyZoneEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "camera_id") val cameraId: String,
    val label: String = "",
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
