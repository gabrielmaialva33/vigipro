package com.vigipro.core.data.di

import com.vigipro.core.data.preferences.LocalUserPreferencesRepository
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.EventRepository
import com.vigipro.core.data.repository.InvitationRepository
import com.vigipro.core.data.repository.LocalCameraRepository
import com.vigipro.core.data.repository.LocalEventRepository
import com.vigipro.core.data.repository.LocalRecordingRepository
import com.vigipro.core.data.repository.LocalWebhookRepository
import com.vigipro.core.data.repository.RecordingRepository
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.data.repository.SupabaseAuthRepository
import com.vigipro.core.data.repository.SupabaseInvitationRepository
import com.vigipro.core.data.repository.SupabaseSiteRepository
import com.vigipro.core.data.repository.WebhookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCameraRepository(impl: LocalCameraRepository): CameraRepository

    @Binds
    abstract fun bindUserPreferencesRepository(impl: LocalUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindSiteRepository(impl: SupabaseSiteRepository): SiteRepository

    @Binds
    abstract fun bindInvitationRepository(impl: SupabaseInvitationRepository): InvitationRepository

    @Binds
    abstract fun bindEventRepository(impl: LocalEventRepository): EventRepository

    @Binds
    abstract fun bindRecordingRepository(impl: LocalRecordingRepository): RecordingRepository

    @Binds
    abstract fun bindWebhookRepository(impl: LocalWebhookRepository): WebhookRepository
}
