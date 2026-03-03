package com.vigipro.core.data.di

import android.content.Context
import androidx.room.Room
import com.vigipro.core.data.db.CameraDao
import com.vigipro.core.data.db.CameraEventDao
import com.vigipro.core.data.db.PrivacyZoneDao
import com.vigipro.core.data.db.RecordingDao
import com.vigipro.core.data.db.SiteDao
import com.vigipro.core.data.db.VigiProDatabase
import com.vigipro.core.data.db.WebhookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VigiProDatabase {
        return Room.databaseBuilder(
            context,
            VigiProDatabase::class.java,
            "vigipro.db",
        )
            .addMigrations(
                VigiProDatabase.MIGRATION_1_2,
                VigiProDatabase.MIGRATION_2_3,
                VigiProDatabase.MIGRATION_3_4,
                VigiProDatabase.MIGRATION_4_5,
                VigiProDatabase.MIGRATION_5_6,
                VigiProDatabase.MIGRATION_6_7,
            )
            .build()
    }

    @Provides
    fun provideCameraDao(database: VigiProDatabase): CameraDao {
        return database.cameraDao()
    }

    @Provides
    fun provideSiteDao(database: VigiProDatabase): SiteDao {
        return database.siteDao()
    }

    @Provides
    fun provideCameraEventDao(database: VigiProDatabase): CameraEventDao {
        return database.cameraEventDao()
    }

    @Provides
    fun provideRecordingDao(database: VigiProDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    fun provideWebhookDao(database: VigiProDatabase): WebhookDao {
        return database.webhookDao()
    }

    @Provides
    fun providePrivacyZoneDao(database: VigiProDatabase): PrivacyZoneDao {
        return database.privacyZoneDao()
    }
}
