package com.vigipro.core.data.detection

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.vigipro.core.model.SceneLabel
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Engine de classificacao de cena on-device via ML Kit Image Labeling.
 * Detecta o que esta na cena: indoor, outdoor, noite, chuva, pessoas, etc.
 * Util pra classificar automaticamente thumbnails e alertas inteligentes.
 */
@Singleton
class ImageLabelingEngine @Inject constructor() {

    private val labeler: ImageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)
            .build()
        ImageLabeling.getClient(options)
    }

    /**
     * Classifica a cena de um bitmap.
     * Retorna labels ordenados por confianca.
     */
    suspend fun classify(bitmap: Bitmap): List<SceneLabel> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val results = suspendCancellableCoroutine { cont ->
            labeler.process(inputImage)
                .addOnSuccessListener { labels -> cont.resume(labels) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

        return results.map { label ->
            SceneLabel(
                label = label.text,
                confidence = label.confidence,
            )
        }.sortedByDescending { it.confidence }
    }

    fun release() {
        labeler.close()
    }
}
