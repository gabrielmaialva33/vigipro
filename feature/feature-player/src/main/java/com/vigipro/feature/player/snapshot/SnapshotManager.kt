package com.vigipro.feature.player.snapshot

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class SnapshotManager(private val context: Context) {

    suspend fun captureFrame(playerView: PlayerView): Uri? {
        val surfaceView = playerView.videoSurfaceView as? SurfaceView
            ?: return null

        val width = surfaceView.width
        val height = surfaceView.height
        if (width <= 0 || height <= 0) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val copyResult = suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                bitmap.recycle()
            }
            try {
                PixelCopy.request(
                    surfaceView,
                    bitmap,
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
            Log.w(TAG, "PixelCopy failed: ${pixelCopyErrorName(copyResult)}")
            bitmap.recycle()
            return null
        }

        return withContext(Dispatchers.IO) {
            saveBitmapToMediaStore(bitmap)
        }
    }

    private fun pixelCopyErrorName(code: Int): String = when (code) {
        PixelCopy.SUCCESS -> "SUCCESS"
        PixelCopy.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
        PixelCopy.ERROR_TIMEOUT -> "ERROR_TIMEOUT"
        PixelCopy.ERROR_SOURCE_NO_DATA -> "ERROR_SOURCE_NO_DATA"
        PixelCopy.ERROR_SOURCE_INVALID -> "ERROR_SOURCE_INVALID"
        PixelCopy.ERROR_DESTINATION_INVALID -> "ERROR_DESTINATION_INVALID"
        else -> "UNKNOWN($code)"
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "VigiPro_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VigiPro")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        ) ?: run {
            bitmap.recycle()
            return null
        }

        return try {
            val compressed = context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            } ?: false
            bitmap.recycle()
            if (compressed) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)
                uri
            } else {
                context.contentResolver.delete(uri, null, null)
                null
            }
        } catch (e: Exception) {
            bitmap.recycle()
            context.contentResolver.delete(uri, null, null)
            null
        }
    }

    companion object {
        private const val TAG = "SnapshotManager"
    }
}
