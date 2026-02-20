package com.vigipro.feature.player.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object FullscreenHelper {

    fun setFullscreen(activity: Activity, fullscreen: Boolean) {
        val window = activity.window
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)

        if (fullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
