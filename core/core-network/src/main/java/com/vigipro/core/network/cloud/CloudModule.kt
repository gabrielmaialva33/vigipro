package com.vigipro.core.network.cloud

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudModule {

    @Provides
    @Singleton
    fun provideVigiProCloudApi(supabase: SupabaseClient): VigiProCloudApi {
        return VigiProCloudApi(
            tokenProvider = { supabase.auth.currentSessionOrNull()?.accessToken },
        )
    }

    @Provides
    @Singleton
    fun provideAlertApi(): AlertApi {
        return AlertApi()
    }
}
