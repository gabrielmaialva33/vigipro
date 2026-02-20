package com.vigipro.app

import android.animation.ObjectAnimator
import android.app.PictureInPictureParams
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vigipro.app.biometric.BiometricLockScreen
import com.vigipro.app.navigation.VigiProNavHost
import com.vigipro.core.data.preferences.ThemeMode
import com.vigipro.core.data.preferences.UserPreferences
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.data.security.BiometricLockManager
import com.vigipro.core.ui.theme.VigiProTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var biometricLockManager: BiometricLockManager

    private var isLocked by mutableStateOf(false)
    private var biometricEnabled by mutableStateOf(false)
    private var backgroundTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Splash screen exit animation — scale + fade
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val scaleX = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_X, 1f, 1.5f, 0f)
            val scaleY = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_Y, 1f, 1.5f, 0f)
            val alpha = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)

            scaleX.interpolator = OvershootInterpolator()
            scaleY.interpolator = OvershootInterpolator()

            scaleX.duration = 500L
            scaleY.duration = 500L
            alpha.duration = 400L
            alpha.startDelay = 200L

            alpha.doOnEnd { splashScreenView.remove() }

            scaleX.start()
            scaleY.start()
            alpha.start()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupBiometricLifecycleObserver()
        setContent {
            val preferences by preferencesRepository.userPreferences
                .collectAsState(initial = UserPreferences())

            // Track biometric preference changes
            LaunchedEffect(preferences.biometricLockEnabled) {
                val wasEnabled = biometricEnabled
                biometricEnabled = preferences.biometricLockEnabled
                // Lock on first launch if biometric is enabled
                if (preferences.biometricLockEnabled && !wasEnabled && !isLocked) {
                    isLocked = true
                }
            }

            val darkTheme = when (preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            VigiProTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isLocked && biometricEnabled) {
                        BiometricLockScreen(
                            onUnlockRequest = ::requestBiometricUnlock,
                        )
                        // Automatically prompt biometric on lock screen show
                        LaunchedEffect(Unit) {
                            requestBiometricUnlock()
                        }
                    } else {
                        VigiProNavHost()
                    }
                }
            }
        }
    }

    private fun setupBiometricLifecycleObserver() {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundTimestamp = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (biometricEnabled && backgroundTimestamp > 0L) {
                        val elapsedMs = System.currentTimeMillis() - backgroundTimestamp
                        if (elapsedMs >= LOCK_TIMEOUT_MS) {
                            isLocked = true
                        }
                        backgroundTimestamp = 0L
                    }
                }
                else -> { /* no-op */ }
            }
        })
    }

    private fun requestBiometricUnlock() {
        val activity = this as? FragmentActivity ?: return
        if (biometricLockManager.checkBiometricStatus() != BiometricLockManager.BiometricStatus.AVAILABLE) {
            // If biometric is no longer available, unlock gracefully
            isLocked = false
            return
        }
        biometricLockManager.showBiometricPrompt(
            activity = activity,
            onSuccess = { isLocked = false },
            onError = { /* Keep locked — user will tap Desbloquear again */ },
            onFailed = { /* Keep locked — fingerprint not recognized */ },
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isInPictureInPictureMode) return
        enterPipMode()
    }

    private fun enterPipMode() {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(pipParams)
    }

    companion object {
        private const val LOCK_TIMEOUT_MS = 30_000L
    }
}
