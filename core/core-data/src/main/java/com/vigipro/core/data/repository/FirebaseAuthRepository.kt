package com.vigipro.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
            // NonCancellable: sync MUST complete even if caller scope is cancelled
            // (e.g. ViewModel destroyed during navigation after Firebase auth succeeds)
            withContext(NonCancellable) { syncSupabaseAuth(email, password) }
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            withContext(NonCancellable) { syncSupabaseAuth(email, password) }
        }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            withContext(NonCancellable) { syncSupabaseGoogleAuth(idToken) }
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
                Timber.w(e, "Supabase auth sync failed — DB operations may fail until re-login")
            }
        }
    }

    /**
     * Syncs Google Sign-In to Supabase.
     * Tries IDToken provider first; if Google is not enabled in Supabase dashboard,
     * falls back to a shadow email/password account for RLS session.
     */
    private suspend fun syncSupabaseGoogleAuth(idToken: String) {
        try {
            supabase.auth.signInWith(IDToken) {
                provider = Google
                this.idToken = idToken
            }
            return
        } catch (e: Exception) {
            Timber.d(e, "IDToken sync failed, falling back to shadow account")
        }

        // Fallback: create/sign-in shadow Supabase account using Firebase user's email.
        // This handles the case where Google provider is not enabled in Supabase.
        val email = firebaseAuth.currentUser?.email ?: return
        val shadowPassword = shadowPassword(firebaseAuth.currentUser?.uid ?: return)
        syncSupabaseAuth(email, shadowPassword)
    }

    /**
     * Deterministic password for shadow Supabase accounts.
     * Used when OAuth providers aren't configured in Supabase — we only need
     * a valid session for RLS, Firebase remains the source of truth.
     */
    private fun shadowPassword(firebaseUid: String): String =
        "vp_shadow_${firebaseUid}"
}
