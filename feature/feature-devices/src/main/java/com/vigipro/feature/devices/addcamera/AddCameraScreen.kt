package com.vigipro.feature.devices.addcamera

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
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    onCameraAdded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddCameraViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            AddCameraSideEffect.CameraAdded -> onCameraAdded()
            is AddCameraSideEffect.ShowError -> snackbarHostState.showSnackbar(sideEffect.message)
            is AddCameraSideEffect.ShowTestResult -> snackbarHostState.showSnackbar(sideEffect.message)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VigiProTopBar(
                title = "Adicionar Camera",
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

            OutlinedTextField(
                value = state.rtspUrl,
                onValueChange = viewModel::onRtspUrlChange,
                label = { Text("URL RTSP") },
                placeholder = { Text("rtsp://192.168.1.100:554/stream1") },
                isError = state.rtspUrlError != null,
                supportingText = state.rtspUrlError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            OutlinedButton(
                onClick = viewModel::onTestConnection,
                enabled = !state.isTesting && !state.isSaving,
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

            if (state.testResult != null) {
                Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                Text(
                    text = state.testResult!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.testResult!!.startsWith("Conexao bem")) {
                        StatusOnline
                    } else {
                        StatusError
                    },
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            Text(
                text = "Credenciais (opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Usuario") },
                placeholder = { Text("admin") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            var passwordVisible by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
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

            Spacer(modifier = Modifier.height(Dimens.SpacingXxl))

            Button(
                onClick = viewModel::onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = Dimens.SpacingXxs,
                    )
                } else {
                    Text("Salvar")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))
        }
    }
}
