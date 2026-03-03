package com.vigipro.feature.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vigipro.core.ui.theme.Dimens
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import timber.log.Timber

private const val WEB_CLIENT_ID =
    "444500286209-dmtl2vh2161shh1phf2q1e57pk8gql3m.apps.googleusercontent.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { contentVisible = true }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            AuthSideEffect.NavigateToDashboard -> onLoginSuccess()
            is AuthSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(sideEffect.message)
        }
    }

    // Show loading while checking existing session
    if (state.isCheckingSession) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Forgot password dialog
    if (state.showForgotPassword) {
        ForgotPasswordDialog(
            email = state.forgotPasswordEmail,
            isLoading = state.isLoading,
            onEmailChange = viewModel::onForgotPasswordEmailChange,
            onSend = viewModel::onSendPasswordReset,
            onDismiss = viewModel::onDismissForgotPassword,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Logo + title with staggered entrance
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconXxl),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                Text(
                    text = "VigiPro",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "Monitoramento Profissional",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingXxxl))

        // Form fields with delayed entrance
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200)) { 30 },
        ) {
            Column(
                modifier = Modifier.animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Google Sign-In button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(WEB_CLIENT_ID)
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    context as Activity,
                                    request,
                                )
                                val credential = result.credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val idToken = GoogleIdTokenCredential
                                        .createFrom(credential.data).idToken
                                    viewModel.onGoogleSignIn(idToken)
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Google Sign-In cancelled or failed")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeight),
                    enabled = !state.isLoading && !state.isGoogleLoading,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    if (state.isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = PhosphorIcons.Regular.GoogleLogo,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text("Continuar com Google")
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                // Divider "ou"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "ou",
                        modifier = Modifier.padding(horizontal = Dimens.SpacingMd),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    enabled = !state.isLoading,
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye,
                                if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onSubmit()
                        },
                    ),
                    enabled = !state.isLoading,
                )

                // Forgot password link
                AnimatedVisibility(visible = state.isLogin) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = viewModel::onShowForgotPassword,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Text(
                                text = "Esqueci minha senha",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                // Error message with animated visibility
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -10 },
                    exit = fadeOut(tween(150)),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                        Text(
                            text = state.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingXl))

                Button(
                    onClick = viewModel::onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeight),
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (state.isLogin) "Entrar" else "Criar conta")
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                TextButton(
                    onClick = viewModel::onToggleMode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state.isLogin) "Não tem conta? Criar conta" else "Já tem conta? Entrar",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordDialog(
    email: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recuperar senha") },
        text = {
            Column {
                Text(
                    text = "Informe seu email e enviaremos um link para redefinir sua senha.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSend() }),
                    enabled = !isLoading,
                )
            }
        },
        confirmButton = {
            Button(onClick = onSend, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
