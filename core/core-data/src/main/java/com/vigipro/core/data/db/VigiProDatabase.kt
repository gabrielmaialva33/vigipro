package com.vigipro.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CameraEntity::class,
        SiteEntity::class,
        CameraEventEntity::class,
        RecordingEntity::class,
        WebhookEntity::class,
        PrivacyZoneEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class VigiProDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun siteDao(): SiteDao
    abstract fun cameraEventDao(): CameraEventDao
    abstract fun recordingDao(): RecordingDao
    abstract fun webhookDao(): WebhookDao
    abstract fun privacyZoneDao(): PrivacyZoneDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recordings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        camera_id TEXT NOT NULL,
                        camera_name TEXT NOT NULL,
                        file_path TEXT NOT NULL,
                        start_time INTEGER NOT NULL,
                        end_time INTEGER NOT NULL DEFAULT 0,
                        file_size INTEGER NOT NULL DEFAULT 0,
                        duration_ms INTEGER NOT NULL DEFAULT 0,
                        thumbnail_path TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recordings_camera_id ON recordings(camera_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recordings_start_time ON recordings(start_time)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS webhooks (
                        id TEXT NOT NULL PRIMARY KEY,
                        camera_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        method TEXT NOT NULL DEFAULT 'POST',
                        headers TEXT NOT NULL DEFAULT '{}',
                        body TEXT,
                        icon TEXT NOT NULL DEFAULT 'power'
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_webhooks_camera_id ON webhooks(camera_id)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sites ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE sites ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE sites ADD COLUMN geofence_radius REAL NOT NULL DEFAULT 200.0")
                db.execSQL("ALTER TABLE sites ADD COLUMN geofence_enabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS privacy_zones (
                        id TEXT NOT NULL PRIMARY KEY,
                        camera_id TEXT NOT NULL,
                        label TEXT NOT NULL,
                        left REAL NOT NULL,
                        top REAL NOT NULL,
                        right REAL NOT NULL,
                        bottom REAL NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_privacy_zones_camera_id ON privacy_zones(camera_id)")
            }
        }
    }
}
