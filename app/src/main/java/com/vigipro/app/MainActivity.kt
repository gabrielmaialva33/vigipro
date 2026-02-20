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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vigipro.app.navigation.VigiProNavHost
import com.vigipro.core.data.preferences.ThemeMode
import com.vigipro.core.data.preferences.UserPreferences
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.ui.theme.VigiProTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

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
        setContent {
            val preferences by preferencesRepository.userPreferences
                .collectAsState(initial = UserPreferences())

            val darkTheme = when (preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            VigiProTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VigiProNavHost()
                }
            }
        }
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
}
