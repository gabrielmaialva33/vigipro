package com.vigipro.core.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests AuthRepository contract via a fake implementation.
 * We avoid mocking SupabaseClient directly because its plugin system
 * (extension properties like supabase.auth) is not mockk-friendly.
 */
class AuthRepositoryContractTest {

    private val fakeAuth = FakeAuthRepository()

    @Test
    fun `initial session state is Loading`() = runTest {
        fakeAuth.sessionState.test {
            assertEquals(AuthSessionState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `session state emits NotAuthenticated after Loading`() = runTest {
        fakeAuth.sessionState.test {
            awaitItem() // Loading
            fakeAuth.emitSessionState(AuthSessionState.NotAuthenticated)
            assertEquals(AuthSessionState.NotAuthenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `session state emits Authenticated with user data`() = runTest {
        fakeAuth.sessionState.test {
            awaitItem() // Loading
            fakeAuth.emitSessionState(
                AuthSessionState.Authenticated(userId = "user-123", email = "test@vigipro.app"),
            )
            val result = awaitItem()
            assertTrue(result is AuthSessionState.Authenticated)
            assertEquals("user-123", (result as AuthSessionState.Authenticated).userId)
            assertEquals("test@vigipro.app", result.email)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentUserId is null when not authenticated`() {
        assertNull(fakeAuth.currentUserId)
    }

    @Test
    fun `currentUserId returns id when authenticated`() {
        fakeAuth.setCurrentUser("user-abc", "test@email.com")
        assertEquals("user-abc", fakeAuth.currentUserId)
        assertEquals("test@email.com", fakeAuth.currentUserEmail)
    }

    @Test
    fun `signIn succeeds with valid credentials`() = runTest {
        val result = fakeAuth.signIn("test@email.com", "password123")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signIn fails with invalid credentials`() = runTest {
        fakeAuth.shouldFailAuth = true
        val result = fakeAuth.signIn("wrong@email.com", "wrong")
        assertTrue(result.isFailure)
    }

    @Test
    fun `signUp succeeds`() = runTest {
        val result = fakeAuth.signUp("new@email.com", "password123")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signUp fails when configured to fail`() = runTest {
        fakeAuth.shouldFailAuth = true
        val result = fakeAuth.signUp("new@email.com", "password123")
        assertTrue(result.isFailure)
    }

    @Test
    fun `signOut clears current user`() = runTest {
        fakeAuth.setCurrentUser("user-1", "a@b.com")
        fakeAuth.signOut()
        assertNull(fakeAuth.currentUserId)
    }
}

/**
 * Fake AuthRepository for testing consumers without Supabase SDK dependency.
 */
class FakeAuthRepository : AuthRepository {
    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
    private var _currentUserId: String? = null
    private var _currentUserEmail: String? = null

    var shouldFailAuth = false

    override val sessionState = _sessionState
    override val currentUserId get() = _currentUserId
    override val currentUserEmail get() = _currentUserEmail

    fun emitSessionState(state: AuthSessionState) {
        _sessionState.value = state
    }

    fun setCurrentUser(id: String?, email: String?) {
        _currentUserId = id
        _currentUserEmail = email
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        if (shouldFailAuth) Result.failure(RuntimeException("Invalid login credentials"))
        else Result.success(Unit)

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        if (shouldFailAuth) Result.failure(RuntimeException("User already registered"))
        else Result.success(Unit)

    override suspend fun signOut() {
        _currentUserId = null
        _currentUserEmail = null
        _sessionState.value = AuthSessionState.NotAuthenticated
    }
}
