package com.vigipro.core.data.di

import android.content.Context
import androidx.room.Room
import com.vigipro.core.data.db.CameraDao
import com.vigipro.core.data.db.SiteDao
import com.vigipro.core.data.db.VigiProDatabase
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
            .addMigrations(VigiProDatabase.MIGRATION_1_2)
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
}
