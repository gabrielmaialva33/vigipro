package com.vigipro.feature.devices.addcamera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import java.util.UUID
import javax.inject.Inject

data class AddCameraState(
    val name: String = "",
    val rtspUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val rtspUrlError: String? = null,
)

sealed interface AddCameraSideEffect {
    data object CameraAdded : AddCameraSideEffect
    data class ShowError(val message: String) : AddCameraSideEffect
}

@HiltViewModel
class AddCameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
) : ViewModel(), ContainerHost<AddCameraState, AddCameraSideEffect> {

    override val container = viewModelScope.container<AddCameraState, AddCameraSideEffect>(AddCameraState())

    fun onNameChange(name: String) = intent {
        reduce { state.copy(name = name, nameError = null) }
    }

    fun onRtspUrlChange(url: String) = intent {
        reduce { state.copy(rtspUrl = url, rtspUrlError = null) }
    }

    fun onUsernameChange(username: String) = intent {
        reduce { state.copy(username = username) }
    }

    fun onPasswordChange(password: String) = intent {
        reduce { state.copy(password = password) }
    }

    fun onSave() = intent {
        val nameError = if (state.name.isBlank()) "Nome obrigatorio" else null
        val rtspUrlError = if (state.rtspUrl.isBlank()) "URL RTSP obrigatoria" else null

        if (nameError != null || rtspUrlError != null) {
            reduce { state.copy(nameError = nameError, rtspUrlError = rtspUrlError) }
            return@intent
        }

        reduce { state.copy(isSaving = true) }

        try {
            val camera = Camera(
                id = UUID.randomUUID().toString(),
                siteId = "local",
                name = state.name.trim(),
                rtspUrl = buildRtspUrl(state),
                username = state.username.ifBlank { null },
                status = CameraStatus.OFFLINE,
            )
            cameraRepository.addCamera(camera)
            postSideEffect(AddCameraSideEffect.CameraAdded)
        } catch (e: Exception) {
            reduce { state.copy(isSaving = false) }
            postSideEffect(AddCameraSideEffect.ShowError("Erro ao salvar: ${e.message}"))
        }
    }

    private fun buildRtspUrl(state: AddCameraState): String {
        val url = state.rtspUrl.trim()
        if (state.username.isBlank() || url.contains("@")) return url
        val credentials = "${state.username}:${state.password}"
        return url.replaceFirst("rtsp://", "rtsp://$credentials@")
    }
}
