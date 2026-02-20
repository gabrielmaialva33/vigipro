package com.vigipro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "webhooks",
    indices = [Index("camera_id")],
)
data class WebhookEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "camera_id") val cameraId: String,
    val name: String,
    val url: String,
    val method: String = "POST",
    val headers: String = "{}",
    val body: String? = null,
    val icon: String = "power",
)
