package com.vigipro.feature.player.vlc

import android.annotation.SuppressLint
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.GestureDetectorCompat

@SuppressLint("ClickableViewAccessibility")
@Composable
fun VlcVideoSurface(
    vlcPlayer: VlcPlayerWrapper,
    rtspUrl: String,
    onSurfaceViewReady: (SurfaceView) -> Unit,
    onTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // rememberUpdatedState ensures the factory lambda always calls the latest callbacks
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).also { surface ->
                val detector = GestureDetectorCompat(
                    ctx,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            Log.d("VlcVideoSurface", "onSingleTapConfirmed at (${e.x}, ${e.y})")
                            currentOnTap()
                            return true
                        }

                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            Log.d("VlcVideoSurface", "onDoubleTap at (${e.x}, ${e.y})")
                            currentOnDoubleTap()
                            return true
                        }
                    },
                )
                surface.setOnTouchListener { _, event ->
                    detector.onTouchEvent(event)
                    true
                }
                surface.post {
                    onSurfaceViewReady(surface)
                    vlcPlayer.attachSurface(surface)
                    vlcPlayer.play(rtspUrl)
                }
            }
        },
        onRelease = {
            vlcPlayer.detachSurface()
        },
        modifier = modifier,
    )
}
