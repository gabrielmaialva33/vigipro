package com.vigipro.feature.devices.addcamera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.ui.components.VigiProTopBar
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.core.ui.theme.StatusError
import com.vigipro.core.ui.theme.StatusOnline
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCameraScreen(
    onBack: () -> Unit,
    onCameraSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddCameraViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            AddCameraSideEffect.CameraAdded -> onCameraSaved()
            AddCameraSideEffect.CameraUpdated -> onCameraSaved()
            AddCameraSideEffect.CameraDeleted -> onCameraSaved()
            is AddCameraSideEffect.ShowError -> snackbarHostState.showSnackbar(sideEffect.message)
            is AddCameraSideEffect.ShowTestResult -> snackbarHostState.showSnackbar(sideEffect.message)
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            cameraName = state.name,
            onConfirm = {
                showDeleteDialog = false
                viewModel.onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VigiProTopBar(
                title = if (state.isEditMode) "Editar Camera" else "Adicionar Camera",
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.SpacingLg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Connection method selector
            ConnectionMethodSelector(
                selected = state.connectionMethod,
                onSelect = viewModel::onConnectionMethodChange,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Nome
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome") },
                placeholder = { Text("Ex: Entrada Principal") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { error -> { Text(error) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Conditional content based on method
            when (state.connectionMethod) {
                ConnectionMethod.IP_ADDRESS -> {
                    IpAddressForm(
                        state = state,
                        onIpChange = viewModel::onIpAddressChange,
                        onPortChange = viewModel::onPortChange,
                        onPathChange = viewModel::onRtspPathChange,
                    )
                }
                ConnectionMethod.ONVIF -> {
                    OnvifDiscoverySection(
                        state = state,
                        onStartScan = viewModel::onStartDiscovery,
                        onSelectDevice = viewModel::onSelectDevice,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Credentials section
            CredentialsSection(
                username = state.username,
                password = state.password,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Test connection
            OutlinedButton(
                onClick = viewModel::onTestConnection,
                enabled = !state.isTesting && !state.isSaving && state.canTest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.IconMd),
                        strokeWidth = Dimens.SpacingXxs,
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text("Testando...")
                } else {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconMd),
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text("Testar Conexao")
                }
            }

            // Test result
            AnimatedVisibility(visible = state.testResult != null) {
                state.testResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.startsWith("Conexao bem")) StatusOnline else StatusError,
                        modifier = Modifier.padding(top = Dimens.SpacingXs),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            // Save button
            Button(
                onClick = viewModel::onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = Dimens.SpacingXxs,
                        modifier = Modifier.size(Dimens.IconMd),
                    )
                } else {
                    Text("Salvar")
                }
            }

            // Delete button (edit mode only)
            if (state.isEditMode) {
                Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconMd),
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text("Excluir Camera")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionMethodSelector(
    selected: ConnectionMethod,
    onSelect: (ConnectionMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selected == ConnectionMethod.IP_ADDRESS,
            onClick = { onSelect(ConnectionMethod.IP_ADDRESS) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
                SegmentedButtonDefaults.Icon(active = selected == ConnectionMethod.IP_ADDRESS) {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = null,
                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                    )
                }
            },
        ) {
            Text("Endereco IP")
        }
        SegmentedButton(
            selected = selected == ConnectionMethod.ONVIF,
            onClick = { onSelect(ConnectionMethod.ONVIF) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
                SegmentedButtonDefaults.Icon(active = selected == ConnectionMethod.ONVIF) {
                    Icon(
                        imageVector = Icons.Default.DeviceHub,
                        contentDescription = null,
                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                    )
                }
            },
        ) {
            Text("ONVIF")
        }
    }
}

@Composable
private fun IpAddressForm(
    state: AddCameraState,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onPathChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        ) {
            OutlinedTextField(
                value = state.ipAddress,
                onValueChange = onIpChange,
                label = { Text("Endereco IP") },
                placeholder = { Text("192.168.1.100") },
                isError = state.ipAddressError != null,
                supportingText = state.ipAddressError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.weight(0.65f),
            )
            OutlinedTextField(
                value = state.port,
                onValueChange = onPortChange,
                label = { Text("Porta") },
                placeholder = { Text("554") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.35f),
            )
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        OutlinedTextField(
            value = state.rtspPath,
            onValueChange = onPathChange,
            label = { Text("Caminho RTSP") },
            placeholder = { Text("/stream1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingSm))

        // URL Preview
        if (state.ipAddress.isNotBlank()) {
            UrlPreview(url = state.builtRtspUrl)
        }
    }
}

@Composable
private fun UrlPreview(
    url: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(Dimens.SpacingSm),
        )
    }
}

@Composable
private fun OnvifDiscoverySection(
    state: AddCameraState,
    onStartScan: () -> Unit,
    onSelectDevice: (DiscoveredDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedButton(
            onClick = onStartScan,
            enabled = !state.isScanning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.IconMd),
                    strokeWidth = Dimens.SpacingXxs,
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text("Buscando dispositivos...")
            } else {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconMd),
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                Text("Buscar Dispositivos")
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        if (state.discoveredDevices.isEmpty() && !state.isScanning) {
            Text(
                text = "Nenhum dispositivo encontrado. Verifique se esta na mesma rede.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.discoveredDevices.forEach { device ->
            DiscoveredDeviceCard(
                device = device,
                isSelected = state.selectedDevice == device,
                onClick = { onSelectDevice(device) },
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
        }
    }
}

@Composable
private fun DiscoveredDeviceCard(
    device: DiscoveredDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingMd)) {
            Text(
                text = device.name ?: device.address,
                style = MaterialTheme.typography.titleMedium,
            )
            if (device.manufacturer != null || device.model != null) {
                Text(
                    text = listOfNotNull(device.manufacturer, device.model).joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (device.ptzCapable || device.audioCapable) {
                Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    if (device.ptzCapable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = "PTZ",
                                modifier = Modifier.size(Dimens.IconSm),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("PTZ", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (device.audioCapable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Audio",
                                modifier = Modifier.size(Dimens.IconSm),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Audio", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialsSection(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Credenciais (opcional)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Usuario") },
            placeholder = { Text("admin") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        var passwordVisible by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    cameraName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir Camera") },
        text = { Text("Tem certeza que deseja excluir \"$cameraName\"?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
