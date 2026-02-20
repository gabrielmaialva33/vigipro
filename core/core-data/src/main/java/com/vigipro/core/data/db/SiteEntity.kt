package com.vigipro.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String? = null,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "geofence_radius") val geofenceRadius: Float = 200f,
    @ColumnInfo(name = "geofence_enabled") val geofenceEnabled: Boolean = false,
)
