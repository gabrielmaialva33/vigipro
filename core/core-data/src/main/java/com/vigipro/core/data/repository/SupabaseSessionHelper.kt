package com.vigipro.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ensures the Supabase auth session is loaded and valid before DB operations.
 *
 * Layered recovery strategy:
 * 1. Load persisted session from local storage
 * 2. Refresh expired session using stored refresh token
 * 3. Self-heal: create/sign-in shadow account using Firebase user's email
 * 4. Fail with clear error if nothing works
 */
@Singleton
class SupabaseSessionHelper @Inject constructor(
    private val supabase: SupabaseClient,
    private val firebaseAuth: FirebaseAuth,
) {

    /**
     * Returns the Supabase user ID, ensuring a valid session exists.
     *
     * @throws IllegalStateException if no Supabase session can be established
     */
    suspend fun requireUserId(): String {
        // 1. Wait for the SDK to load persisted session from storage
        supabase.auth.awaitInitialization()

        // 2. Session loaded successfully
        supabase.auth.currentUserOrNull()?.id?.let { return it }

        // 3. Session expired or token invalid — try refreshing
        try {
            supabase.auth.refreshCurrentSession()
            supabase.auth.currentUserOrNull()?.id?.let { return it }
        } catch (e: Exception) {
            Timber.d(e, "Supabase session refresh failed")
        }

        // 4. Self-heal: create shadow Supabase account from Firebase credentials
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null && firebaseUser.email != null) {
            try {
                val email = firebaseUser.email!!
                val password = "vp_shadow_${firebaseUser.uid}"
                try {
                    supabase.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                } catch (_: Exception) {
                    supabase.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                supabase.auth.currentUserOrNull()?.id?.let { return it }
            } catch (e: Exception) {
                Timber.w(e, "Shadow account creation failed")
            }
        }

        // 5. Nothing worked
        error("Sessao expirada. Faca login novamente")
    }
}
