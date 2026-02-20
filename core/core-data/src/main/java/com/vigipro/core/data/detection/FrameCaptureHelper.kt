package com.vigipro.core.data.detection

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object FrameCaptureHelper {

    private const val DETECTION_SIZE = 320

    suspend fun captureFrame(surfaceView: SurfaceView): Bitmap? {
        val width = surfaceView.width
        val height = surfaceView.height
        if (width <= 0 || height <= 0) return null

        val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val copyResult = suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { fullBitmap.recycle() }
            try {
                PixelCopy.request(
                    surfaceView,
                    fullBitmap,
                    { result -> cont.resume(result) },
                    Handler(Looper.getMainLooper()),
                )
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(PixelCopy.ERROR_UNKNOWN)
                }
            }
        }

        if (copyResult != PixelCopy.SUCCESS) {
            fullBitmap.recycle()
            return null
        }

        if (width == DETECTION_SIZE && height == DETECTION_SIZE) return fullBitmap

        val scaled = Bitmap.createScaledBitmap(fullBitmap, DETECTION_SIZE, DETECTION_SIZE, true)
        fullBitmap.recycle()
        return scaled
    }
}
