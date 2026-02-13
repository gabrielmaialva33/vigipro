package com.vigipro.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CameraEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class VigiProDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
}
