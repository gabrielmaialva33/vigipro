package com.vigipro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "site_id") val siteId: String,
    val name: String,
    @ColumnInfo(name = "onvif_address") val onvifAddress: String? = null,
    @ColumnInfo(name = "rtsp_url") val rtspUrl: String? = null,
    val username: String? = null,
    @ColumnInfo(name = "stream_profile") val streamProfile: String? = null,
    @ColumnInfo(name = "ptz_capable") val ptzCapable: Boolean = false,
    @ColumnInfo(name = "audio_capable") val audioCapable: Boolean = false,
    val status: String = "offline",
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "hls_url") val hlsUrl: String? = null,
    @ColumnInfo(name = "is_demo") val isDemo: Boolean = false,
)
