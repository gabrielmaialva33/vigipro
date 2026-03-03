package com.vigipro.feature.devices.addcamera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
            TopAppBar(
                title = { 
                    Text(
                        text = if (state.name.isNotBlank()) state.name else if (state.isEditMode) "Editar Câmera" else "Adicionar Câmera",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ) 
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text("Voltar", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::onSave,
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Text("Salvar", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = Dimens.SpacingXl)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            Text(
                text = "Método de Conexão",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            
            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Connection method selector (Chips)
            ConnectionMethodChips(
                selected = state.connectionMethod,
                onSelect = viewModel::onConnectionMethodChange,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            // Nome
            CustomTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Nome",
                placeholder = "Ex: Entrada Principal",
                isError = state.nameError != null,
                errorMessage = state.nameError,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

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

            Spacer(modifier = Modifier.height(Dimens.SpacingXxl))

            // Test connection
            Button(
                onClick = viewModel::onTestConnection,
                enabled = !state.isTesting && !state.isSaving && state.canTest,
                modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testando...", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                } else {
                    Text("Testar Conexão", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            // Test result
            AnimatedVisibility(visible = state.testResult != null) {
                state.testResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.startsWith("Conexao bem")) StatusOnline else StatusError,
                        modifier = Modifier.padding(top = Dimens.SpacingMd).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Delete button (edit mode only)
            if (state.isEditMode) {
                Spacer(modifier = Modifier.height(Dimens.SpacingXl))
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Trash,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconMd),
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text("Excluir Câmera", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXxxl))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectionMethodChips(
    selected: ConnectionMethod,
    onSelect: (ConnectionMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mock Chips for visual parity with the image
        ChipItem(
            text = "Cloud",
            icon = PhosphorIcons.Regular.Cloud,
            isSelected = false,
            onClick = { }
        )
        ChipItem(
            text = "Domínio",
            icon = PhosphorIcons.Regular.Globe,
            isSelected = false,
            onClick = { }
        )
        
        // Actual functional chips
        ChipItem(
            text = "Endereço de IP",
            icon = PhosphorIcons.Regular.WifiHigh, // Fallback if not selected
            isSelected = selected == ConnectionMethod.IP_ADDRESS,
            onClick = { onSelect(ConnectionMethod.IP_ADDRESS) }
        )
        
        ChipItem(
            text = "ONVIF",
            icon = PhosphorIcons.Regular.Broadcast,
            isSelected = selected == ConnectionMethod.ONVIF,
            onClick = { onSelect(ConnectionMethod.ONVIF) }
        )
    }
}

@Composable
private fun ChipItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = PhosphorIcons.Regular.CheckCircle,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            isError = isError,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
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
        CustomTextField(
            value = state.ipAddress,
            onValueChange = onIpChange,
            label = "Endereço de IP",
            placeholder = "179.228.93.195",
            isError = state.ipAddressError != null,
            errorMessage = state.ipAddressError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        CustomTextField(
            value = state.port,
            onValueChange = onPortChange,
            label = "Porta de serviço (TCP)",
            placeholder = "1100",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        CustomTextField(
            value = state.rtspPath,
            onValueChange = onPathChange,
            label = "Caminho RTSP",
            placeholder = "/stream1",
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(Dimens.SpacingMd),
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
        Button(
            onClick = onStartScan,
            enabled = !state.isScanning,
            modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscando dispositivos...")
            } else {
                Text("Buscar Dispositivos na Rede", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        if (state.discoveredDevices.isEmpty() && !state.isScanning) {
            Text(
                text = "Nenhum dispositivo encontrado. Verifique se esta na mesma rede.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingLg)
            )
        }

        state.discoveredDevices.forEach { device ->
            Card(
                onClick = { onSelectDevice(device) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.selectedDevice == device) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (state.selectedDevice == device) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(Dimens.SpacingLg)) {
                    Text(
                        text = device.name ?: device.address,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
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
        CustomTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = "Usuário",
            placeholder = "admin",
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        var passwordVisible by remember { mutableStateOf(false) }

        CustomTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Senha",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye,
                        contentDescription = "Alternar Visibilidade",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
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
        title = { Text("Excluir Câmera") },
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
