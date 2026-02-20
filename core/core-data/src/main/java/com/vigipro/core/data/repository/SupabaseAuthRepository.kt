package com.vigipro.core.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override val sessionState: Flow<AuthSessionState> =
        supabase.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthSessionState.Authenticated(
                    userId = status.session.user?.id ?: "",
                    email = status.session.user?.email,
                )
                is SessionStatus.NotAuthenticated -> AuthSessionState.NotAuthenticated
                SessionStatus.Initializing -> AuthSessionState.Loading
                is SessionStatus.RefreshFailure -> AuthSessionState.NotAuthenticated
            }
        }

    override val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override val currentUserEmail: String?
        get() = supabase.auth.currentUserOrNull()?.email

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (_: Exception) {
            // Best-effort sign out
        }
    }
}
