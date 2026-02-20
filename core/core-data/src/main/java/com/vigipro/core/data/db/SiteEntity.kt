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
)
