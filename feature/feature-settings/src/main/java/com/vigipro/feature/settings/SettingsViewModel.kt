package com.vigipro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.preferences.ThemeMode
import com.vigipro.core.data.preferences.UserPreferences
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.data.preferences.VideoQuality
import com.vigipro.core.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class SettingsState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val showClearCacheConfirmation: Boolean = false,
    val userEmail: String? = null,
)

sealed interface SettingsSideEffect {
    data class ShowSnackbar(val message: String) : SettingsSideEffect
    data object NavigateBack : SettingsSideEffect
    data object NavigateToLogin : SettingsSideEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel(), ContainerHost<SettingsState, SettingsSideEffect> {

    override val container = viewModelScope.container<SettingsState, SettingsSideEffect>(SettingsState()) {
        observePreferences()
        loadUserInfo()
    }

    private fun observePreferences() = intent {
        preferencesRepository.userPreferences.collect { prefs ->
            reduce { state.copy(preferences = prefs, isLoading = false) }
        }
    }

    private fun loadUserInfo() = intent {
        reduce { state.copy(userEmail = authRepository.currentUserEmail) }
    }

    fun onVideoQualityChange(quality: VideoQuality) = intent {
        preferencesRepository.updateVideoQuality(quality)
    }

    fun onDefaultGridColumnsChange(columns: Int) = intent {
        preferencesRepository.updateDefaultGridColumns(columns)
    }

    fun onAudioEnabledChange(enabled: Boolean) = intent {
        preferencesRepository.updateAudioEnabled(enabled)
    }

    fun onMonitorIntervalChange(intervalMs: Long) = intent {
        preferencesRepository.updateMonitorInterval(intervalMs)
    }

    fun onThemeModeChange(mode: ThemeMode) = intent {
        preferencesRepository.updateThemeMode(mode)
    }

    fun onNotifyOfflineChange(enabled: Boolean) = intent {
        preferencesRepository.updateNotifyOffline(enabled)
    }

    fun onNotifyOnlineChange(enabled: Boolean) = intent {
        preferencesRepository.updateNotifyOnline(enabled)
    }

    fun onWatermarkEnabledChange(enabled: Boolean) = intent {
        preferencesRepository.updateWatermarkEnabled(enabled)
    }

    fun onDetectionEnabledChange(enabled: Boolean) = intent {
        preferencesRepository.updateDetectionEnabled(enabled)
    }

    fun onDetectionConfidenceChange(threshold: Float) = intent {
        preferencesRepository.updateDetectionConfidenceThreshold(threshold)
    }

    fun onDetectPersonsChange(enabled: Boolean) = intent {
        preferencesRepository.updateDetectPersons(enabled)
    }

    fun onDetectVehiclesChange(enabled: Boolean) = intent {
        preferencesRepository.updateDetectVehicles(enabled)
    }

    fun onDetectAnimalsChange(enabled: Boolean) = intent {
        preferencesRepository.updateDetectAnimals(enabled)
    }

    fun onNotifyPersonDetectedChange(enabled: Boolean) = intent {
        preferencesRepository.updateNotifyPersonDetected(enabled)
    }

    fun onDetectionIntervalChange(intervalMs: Long) = intent {
        preferencesRepository.updateDetectionIntervalMs(intervalMs)
    }

    fun onTalkbackEnabledChange(enabled: Boolean) = intent {
        preferencesRepository.updateTalkbackEnabled(enabled)
    }

    fun onClearCacheRequest() = intent {
        reduce { state.copy(showClearCacheConfirmation = true) }
    }

    fun onClearCacheConfirm() = intent {
        reduce { state.copy(showClearCacheConfirmation = false) }
        preferencesRepository.clearCache()
        postSideEffect(SettingsSideEffect.ShowSnackbar("Cache limpo com sucesso"))
    }

    fun onClearCacheDismiss() = intent {
        reduce { state.copy(showClearCacheConfirmation = false) }
    }

    fun onLogout() = intent {
        authRepository.signOut()
        postSideEffect(SettingsSideEffect.NavigateToLogin)
    }

    fun onBack() = intent {
        postSideEffect(SettingsSideEffect.NavigateBack)
    }
}
