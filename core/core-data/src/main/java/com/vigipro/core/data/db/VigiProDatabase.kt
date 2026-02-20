package com.vigipro.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CameraEntity::class, SiteEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class VigiProDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun siteDao(): SiteDao

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
    }
}
