package com.vigipro.feature.auth

import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.AuthSessionState
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.model.Site
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class AuthViewModelTest {

    private val sessionFlow = MutableStateFlow<AuthSessionState>(AuthSessionState.NotAuthenticated)
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { sessionState } returns sessionFlow
    }
    private val siteRepository: SiteRepository = mockk(relaxed = true) {
        every { getUserSites() } returns flowOf(listOf(Site("s1", "Meu Local", null, "o1")))
    }

    private fun createViewModel() = AuthViewModel(authRepository, siteRepository)

    @Test
    fun `initial state defaults are correct`() {
        val state = AuthState()
        assertFalse(state.isLoading)
        assertTrue(state.isCheckingSession)
        assertTrue(state.isLogin)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onEmailChange updates email and clears error`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onEmailChange("test@email.com")
            expectState { copy(email = "test@email.com", errorMessage = null) }
        }
    }

    @Test
    fun `onPasswordChange updates password and clears error`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onPasswordChange("secret123")
            expectState { copy(password = "secret123", errorMessage = null) }
        }
    }

    @Test
    fun `onToggleMode switches between login and register`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onToggleMode()
            expectState { copy(isLogin = false, errorMessage = null) }
        }
    }

    @Test
    fun `onSubmit with blank fields shows error`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onSubmit()
            expectState { copy(errorMessage = "Preencha todos os campos") }
        }
    }

    @Test
    fun `onSubmit with short password shows error`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onEmailChange("test@email.com")
            expectState { copy(email = "test@email.com", errorMessage = null) }
            containerHost.onPasswordChange("123")
            expectState { copy(password = "123", errorMessage = null) }
            containerHost.onSubmit()
            expectState { copy(errorMessage = "Senha deve ter no minimo 6 caracteres") }
        }
    }

    @Test
    fun `onSubmit with valid credentials calls signIn and sets loading`() = runTest {
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onEmailChange("test@email.com")
            expectState { copy(email = "test@email.com", errorMessage = null) }
            containerHost.onPasswordChange("password123")
            expectState { copy(password = "password123", errorMessage = null) }
            containerHost.onSubmit()
            expectState { copy(isLoading = true, errorMessage = null) }
        }
    }

    @Test
    fun `onSubmit in register mode sets loading`() = runTest {
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onToggleMode()
            expectState { copy(isLogin = false, errorMessage = null) }
            containerHost.onEmailChange("new@email.com")
            expectState { copy(email = "new@email.com", errorMessage = null) }
            containerHost.onPasswordChange("password123")
            expectState { copy(password = "password123", errorMessage = null) }
            containerHost.onSubmit()
            expectState { copy(isLoading = true, errorMessage = null) }
        }
    }
}
