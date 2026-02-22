package com.vigipro.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.AuthSessionState
import com.vigipro.core.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val isLogin: Boolean = true,
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    val isGoogleLoading: Boolean = false,
    val showForgotPassword: Boolean = false,
    val forgotPasswordEmail: String = "",
)

sealed interface AuthSideEffect {
    data object NavigateToDashboard : AuthSideEffect
    data class ShowSnackbar(val message: String) : AuthSideEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val siteRepository: SiteRepository,
) : ViewModel(), ContainerHost<AuthState, AuthSideEffect> {

    override val container = viewModelScope.container<AuthState, AuthSideEffect>(AuthState()) {
        observeSession()
    }

    private fun observeSession() = intent {
        authRepository.sessionState.collect { sessionState ->
            when (sessionState) {
                is AuthSessionState.Authenticated -> {
                    reduce { state.copy(isCheckingSession = false, isLoading = false) }
                    // Sync sites on auth
                    try {
                        siteRepository.syncSites()
                        // Auto-create default site if user has none
                        siteRepository.getUserSites().collect { sites ->
                            if (sites.isEmpty()) {
                                siteRepository.createSite("Meu Local", null)
                            }
                            postSideEffect(AuthSideEffect.NavigateToDashboard)
                            return@collect
                        }
                    } catch (_: Exception) {
                        postSideEffect(AuthSideEffect.NavigateToDashboard)
                    }
                }
                is AuthSessionState.NotAuthenticated -> {
                    reduce { state.copy(isCheckingSession = false, isLoading = false) }
                }
                AuthSessionState.Loading -> {
                    reduce { state.copy(isCheckingSession = true) }
                }
            }
        }
    }

    fun onEmailChange(email: String) = intent {
        reduce { state.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) = intent {
        reduce { state.copy(password = password, errorMessage = null) }
    }

    fun onToggleMode() = intent {
        reduce { state.copy(isLogin = !state.isLogin, errorMessage = null) }
    }

    fun onSubmit() = intent {
        val email = state.email.trim()
        val password = state.password

        if (email.isBlank() || password.isBlank()) {
            reduce { state.copy(errorMessage = "Preencha todos os campos") }
            return@intent
        }

        if (password.length < 6) {
            reduce { state.copy(errorMessage = "Senha deve ter no minimo 6 caracteres") }
            return@intent
        }

        reduce { state.copy(isLoading = true, errorMessage = null) }

        val result = if (state.isLogin) {
            authRepository.signIn(email, password)
        } else {
            authRepository.signUp(email, password)
        }

        result.onFailure { error ->
            val msg = error.message.orEmpty()
            val message = when {
                // Firebase error codes
                msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                    msg.contains("INVALID_EMAIL", ignoreCase = true) ||
                    msg.contains("Invalid login", ignoreCase = true) ->
                    "Email ou senha incorretos"
                msg.contains("EMAIL_EXISTS", ignoreCase = true) ||
                    msg.contains("already in use", ignoreCase = true) ||
                    msg.contains("already registered", ignoreCase = true) ->
                    "Este email ja esta cadastrado"
                msg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                    msg.contains("no user record", ignoreCase = true) ->
                    "Email nao cadastrado"
                msg.contains("WEAK_PASSWORD", ignoreCase = true) ||
                    msg.contains("weak_password", ignoreCase = true) ->
                    "Senha muito fraca. Use no minimo 6 caracteres"
                msg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ||
                    msg.contains("rate_limit", ignoreCase = true) ->
                    "Muitas tentativas. Aguarde alguns segundos"
                msg.contains("badly formatted", ignoreCase = true) ||
                    msg.contains("invalid_email", ignoreCase = true) ->
                    "Email invalido"
                msg.contains("network", ignoreCase = true) ||
                    msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("Unable to resolve", ignoreCase = true) ->
                    "Sem conexao com o servidor. Verifique sua internet"
                else -> "Erro de autenticacao. Tente novamente"
            }
            reduce { state.copy(isLoading = false, errorMessage = message) }
        }

        // Success handled by sessionState observer
    }

    fun onGoogleSignIn(idToken: String) = intent {
        reduce { state.copy(isGoogleLoading = true, errorMessage = null) }

        authRepository.signInWithGoogle(idToken).onFailure { error ->
            val message = when {
                error.message.orEmpty().contains("network", ignoreCase = true) ->
                    "Sem conexao. Verifique sua internet"
                else -> "Erro ao entrar com Google. Tente novamente"
            }
            reduce { state.copy(isGoogleLoading = false, errorMessage = message) }
        }
        // Success handled by sessionState observer
    }

    fun onShowForgotPassword() = intent {
        reduce { state.copy(showForgotPassword = true, forgotPasswordEmail = state.email) }
    }

    fun onDismissForgotPassword() = intent {
        reduce { state.copy(showForgotPassword = false) }
    }

    fun onForgotPasswordEmailChange(email: String) = intent {
        reduce { state.copy(forgotPasswordEmail = email) }
    }

    fun onSendPasswordReset() = intent {
        val email = state.forgotPasswordEmail.trim()
        if (email.isBlank()) {
            reduce { state.copy(errorMessage = "Informe seu email") }
            return@intent
        }

        reduce { state.copy(isLoading = true) }

        authRepository.sendPasswordResetEmail(email)
            .onSuccess {
                reduce { state.copy(isLoading = false, showForgotPassword = false) }
                postSideEffect(AuthSideEffect.ShowSnackbar("Email de recuperacao enviado para $email"))
            }
            .onFailure {
                reduce { state.copy(isLoading = false, errorMessage = "Erro ao enviar email de recuperacao") }
            }
    }
}
