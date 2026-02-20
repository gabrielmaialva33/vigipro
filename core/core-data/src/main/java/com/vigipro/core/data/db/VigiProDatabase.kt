package com.vigipro.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CameraEntity::class, SiteEntity::class, CameraEventEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class VigiProDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun siteDao(): SiteDao
    abstract fun cameraEventDao(): CameraEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sites (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        address TEXT,
                        owner_id TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS camera_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        camera_id TEXT NOT NULL,
                        camera_name TEXT NOT NULL,
                        event_type TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        message TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_camera_events_camera_id ON camera_events(camera_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_camera_events_timestamp ON camera_events(timestamp)",
                )
            }
        }
    }
}
