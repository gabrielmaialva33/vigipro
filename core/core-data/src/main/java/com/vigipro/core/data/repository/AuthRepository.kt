package com.vigipro.core.data.repository

import kotlinx.coroutines.flow.Flow

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object NotAuthenticated : AuthSessionState
    data class Authenticated(val userId: String, val email: String?) : AuthSessionState
}

interface AuthRepository {
    val sessionState: Flow<AuthSessionState>
    val currentUserId: String?
    val currentUserEmail: String?
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut()
}
