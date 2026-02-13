package com.vigipro.core.data.di

import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.LocalCameraRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCameraRepository(impl: LocalCameraRepository): CameraRepository
}
