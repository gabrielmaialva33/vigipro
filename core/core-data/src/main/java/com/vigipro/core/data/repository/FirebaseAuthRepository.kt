package com.vigipro.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val supabase: SupabaseClient,
) : AuthRepository {

    override val sessionState: Flow<AuthSessionState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val state = if (user != null) {
                AuthSessionState.Authenticated(
                    userId = user.uid,
                    email = user.email,
                )
            } else {
                AuthSessionState.NotAuthenticated
            }
            trySend(state)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            syncSupabaseAuth(email, password)
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            syncSupabaseAuth(email, password)
        }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            syncSupabaseGoogleAuth(idToken)
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        runCatching {
            firebaseAuth.sendPasswordResetEmail(email).await()
        }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Timber.w(e, "Firebase sign out failed")
        }
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Timber.w(e, "Supabase sign out failed")
        }
    }

    /**
     * Syncs email/password auth to Supabase so RLS policies continue working.
     * Best-effort — Firebase is the source of truth.
     */
    private suspend fun syncSupabaseAuth(email: String, password: String) {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (_: Exception) {
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (e: Exception) {
                Timber.d(e, "Supabase auth sync failed (best-effort)")
            }
        }
    }

    /**
     * Syncs Google Sign-In to Supabase using ID Token provider.
     * Requires Google provider enabled in Supabase dashboard.
     */
    private suspend fun syncSupabaseGoogleAuth(idToken: String) {
        try {
            supabase.auth.signInWith(IDToken) {
                provider = Google
                this.idToken = idToken
            }
        } catch (e: Exception) {
            Timber.d(e, "Supabase Google auth sync failed (best-effort)")
        }
    }
}
