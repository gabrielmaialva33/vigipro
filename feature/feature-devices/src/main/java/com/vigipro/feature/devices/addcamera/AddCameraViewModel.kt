package com.vigipro.feature.devices.addcamera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.data.rtsp.RtspConnectionTester
import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraStatus
import com.vigipro.feature.devices.onvif.OnvifDeviceInfo
import com.vigipro.feature.devices.onvif.OnvifDiscoveryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import java.net.URI
import java.util.UUID
import javax.inject.Inject

enum class ConnectionMethod { IP_ADDRESS, ONVIF }

data class DiscoveredDevice(
    val address: String,
    val name: String?,
    val manufacturer: String?,
    val model: String?,
    val rtspUrl: String?,
    val ptzCapable: Boolean = false,
    val audioCapable: Boolean = false,
)

data class AddCameraState(
    // Connection method
    val connectionMethod: ConnectionMethod = ConnectionMethod.IP_ADDRESS,

    // Common fields
    val name: String = "",
    val username: String = "",
    val password: String = "",

    // IP Address method fields
    val ipAddress: String = "",
    val port: String = "554",
    val rtspPath: String = "/stream1",

    // ONVIF discovery
    val isScanning: Boolean = false,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val selectedDevice: DiscoveredDevice? = null,

    // Testing & saving
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val testResult: String? = null,

    // Edit mode
    val isEditMode: Boolean = false,
    val editCameraId: String? = null,

    // Validation errors
    val nameError: String? = null,
    val ipAddressError: String? = null,
) {
    val builtRtspUrl: String
        get() {
            val ip = ipAddress.trim()
            val p = port.trim().ifBlank { "554" }
            val path = rtspPath.trim().let { if (it.startsWith("/")) it else "/$it" }
            return "rtsp://$ip:$p$path"
        }

    val effectiveRtspUrl: String
        get() = when (connectionMethod) {
            ConnectionMethod.IP_ADDRESS -> builtRtspUrl
            ConnectionMethod.ONVIF -> selectedDevice?.rtspUrl ?: ""
        }

    val canTest: Boolean
        get() = when (connectionMethod) {
            ConnectionMethod.IP_ADDRESS -> ipAddress.isNotBlank()
            ConnectionMethod.ONVIF -> selectedDevice?.rtspUrl != null
        }
}

sealed interface AddCameraSideEffect {
    data object CameraAdded : AddCameraSideEffect
    data object CameraUpdated : AddCameraSideEffect
    data object CameraDeleted : AddCameraSideEffect
    data class ShowError(val message: String) : AddCameraSideEffect
    data class ShowTestResult(val message: String) : AddCameraSideEffect
}

@HiltViewModel
class AddCameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val siteRepository: SiteRepository,
    private val connectionTester: RtspConnectionTester,
    private val onvifDiscovery: OnvifDiscoveryService,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), ContainerHost<AddCameraState, AddCameraSideEffect> {

    private val cameraId: String? = savedStateHandle["cameraId"]

    override val container = viewModelScope.container<AddCameraState, AddCameraSideEffect>(
        AddCameraState(),
    ) {
        if (cameraId != null) loadCameraForEdit(cameraId)
    }

    // --- Field change handlers ---

    fun onConnectionMethodChange(method: ConnectionMethod) = intent {
        reduce { state.copy(connectionMethod = method, testResult = null) }
    }

    fun onNameChange(name: String) = intent {
        reduce { state.copy(name = name, nameError = null) }
    }

    fun onIpAddressChange(ip: String) = intent {
        reduce { state.copy(ipAddress = ip, ipAddressError = null, testResult = null) }
    }

    fun onPortChange(port: String) = intent {
        reduce { state.copy(port = port, testResult = null) }
    }

    fun onRtspPathChange(path: String) = intent {
        reduce { state.copy(rtspPath = path, testResult = null) }
    }

    fun onUsernameChange(username: String) = intent {
        reduce { state.copy(username = username) }
    }

    fun onPasswordChange(password: String) = intent {
        reduce { state.copy(password = password) }
    }

    // --- ONVIF discovery ---

    fun onStartDiscovery() = intent {
        reduce { state.copy(isScanning = true, discoveredDevices = emptyList(), selectedDevice = null) }

        try {
            val addresses = onvifDiscovery.discoverDevices()
            val devices = addresses.map { address ->
                val info = onvifDiscovery.getDeviceDetails(
                    address = address,
                    username = state.username,
                    password = state.password,
                )
                info.toDiscoveredDevice()
            }
            reduce { state.copy(isScanning = false, discoveredDevices = devices) }
        } catch (e: Exception) {
            reduce { state.copy(isScanning = false) }
            postSideEffect(AddCameraSideEffect.ShowError("Erro na busca de dispositivos. Verifique o endereco"))
        }
    }

    fun onSelectDevice(device: DiscoveredDevice) = intent {
        reduce {
            state.copy(
                selectedDevice = device,
                name = if (state.name.isBlank()) device.name ?: "" else state.name,
                testResult = null,
            )
        }
    }

    // --- Actions ---

    fun onTestConnection() = intent {
        if (!state.canTest) {
            if (state.connectionMethod == ConnectionMethod.IP_ADDRESS) {
                reduce { state.copy(ipAddressError = "Endereco IP obrigatorio") }
            }
            return@intent
        }

        reduce { state.copy(isTesting = true, testResult = null) }

        val url = buildRtspUrlWithCredentials(
            state.effectiveRtspUrl,
            state.username,
            state.password,
        )
        val result = connectionTester.testConnection(url)

        val message = if (result.success) {
            "Conexao bem-sucedida (${result.latencyMs}ms)"
        } else {
            result.errorMessage ?: "Falha na conexao"
        }

        reduce { state.copy(isTesting = false, testResult = message) }
        postSideEffect(AddCameraSideEffect.ShowTestResult(message))
    }

    fun onSave() = intent {
        val nameError = if (state.name.isBlank()) "Nome obrigatorio" else null
        val ipAddressError = if (state.connectionMethod == ConnectionMethod.IP_ADDRESS && state.ipAddress.isBlank()) {
            "Endereco IP obrigatorio"
        } else {
            null
        }

        if (nameError != null || ipAddressError != null) {
            reduce { state.copy(nameError = nameError, ipAddressError = ipAddressError) }
            return@intent
        }

        if (state.effectiveRtspUrl.isBlank()) {
            postSideEffect(AddCameraSideEffect.ShowError("URL RTSP nao disponivel"))
            return@intent
        }

        reduce { state.copy(isSaving = true) }

        try {
            val url = buildRtspUrlWithCredentials(
                state.effectiveRtspUrl,
                state.username,
                state.password,
            )

            val testResult = connectionTester.testConnection(url)
            val initialStatus = if (testResult.success) CameraStatus.ONLINE else CameraStatus.OFFLINE

            val activeSiteId = siteRepository.getUserSites().firstOrNull()?.firstOrNull()?.id ?: "local"

            val camera = Camera(
                id = state.editCameraId ?: UUID.randomUUID().toString(),
                siteId = activeSiteId,
                name = state.name.trim(),
                rtspUrl = url,
                onvifAddress = if (state.connectionMethod == ConnectionMethod.ONVIF) {
                    state.selectedDevice?.address
                } else {
                    null
                },
                username = state.username.ifBlank { null },
                ptzCapable = state.selectedDevice?.ptzCapable ?: false,
                audioCapable = state.selectedDevice?.audioCapable ?: false,
                status = initialStatus,
            )

            if (state.isEditMode) {
                cameraRepository.updateCamera(camera)
                postSideEffect(AddCameraSideEffect.CameraUpdated)
            } else {
                cameraRepository.addCamera(camera)
                postSideEffect(AddCameraSideEffect.CameraAdded)
            }
        } catch (e: Exception) {
            reduce { state.copy(isSaving = false) }
            postSideEffect(AddCameraSideEffect.ShowError("Erro ao salvar camera. Tente novamente"))
        }
    }

    fun onDelete() = intent {
        val id = state.editCameraId ?: return@intent
        try {
            cameraRepository.deleteCamera(id)
            postSideEffect(AddCameraSideEffect.CameraDeleted)
        } catch (e: Exception) {
            postSideEffect(AddCameraSideEffect.ShowError("Erro ao excluir camera. Tente novamente"))
        }
    }

    // --- Private helpers ---

    private fun loadCameraForEdit(id: String) = intent {
        val camera = cameraRepository.getCameraById(id) ?: return@intent
        val (ip, port, path) = parseRtspUrl(camera.rtspUrl ?: "")
        reduce {
            state.copy(
                isEditMode = true,
                editCameraId = id,
                name = camera.name,
                ipAddress = ip,
                port = port,
                rtspPath = path,
                username = camera.username ?: "",
                connectionMethod = if (camera.onvifAddress != null) {
                    ConnectionMethod.ONVIF
                } else {
                    ConnectionMethod.IP_ADDRESS
                },
            )
        }
    }

    private fun parseRtspUrl(url: String): Triple<String, String, String> {
        return try {
            val cleanUrl = url.replaceFirst(Regex("rtsp://[^@]+@"), "rtsp://")
            val uri = URI(cleanUrl)
            val ip = uri.host ?: ""
            val port = if (uri.port > 0) uri.port.toString() else "554"
            val path = uri.path?.ifBlank { "/stream1" } ?: "/stream1"
            Triple(ip, port, path)
        } catch (_: Exception) {
            Triple("", "554", "/stream1")
        }
    }

    private fun buildRtspUrlWithCredentials(
        url: String,
        username: String,
        password: String,
    ): String {
        val trimmedUrl = url.trim()
        if (username.isBlank() || trimmedUrl.contains("@")) return trimmedUrl
        val credentials = "$username:$password"
        return trimmedUrl.replaceFirst("rtsp://", "rtsp://$credentials@")
    }

    private fun OnvifDeviceInfo?.toDiscoveredDevice(): DiscoveredDevice {
        if (this == null) return DiscoveredDevice(address = "", name = null, manufacturer = null, model = null, rtspUrl = null)
        return DiscoveredDevice(
            address = address,
            name = name,
            manufacturer = manufacturer,
            model = model,
            rtspUrl = rtspUrl,
            ptzCapable = ptzCapable,
            audioCapable = audioCapable,
        )
    }
}
