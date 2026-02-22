package com.vigipro.core.data.detection

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vigipro.core.model.RecognizedText
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Engine de reconhecimento de texto on-device via ML Kit.
 * Util pra placas de veiculos, sinalizacao, crachás etc.
 */
@Singleton
class TextRecognitionEngine @Inject constructor() {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Reconhece texto em um bitmap.
     * Retorna lista de blocos de texto detectados com bounding boxes normalizadas.
     */
    suspend fun recognize(bitmap: Bitmap): List<RecognizedText> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        val result = suspendCancellableCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText -> cont.resume(visionText) }
                .addOnFailureListener { cont.resume(null) }
        } ?: return emptyList()

        return result.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val rect = line.boundingBox
                RecognizedText(
                    text = line.text,
                    confidence = line.confidence,
                    boundingBox = rect?.let {
                        RectF(
                            it.left / width,
                            it.top / height,
                            it.right / width,
                            it.bottom / height,
                        )
                    },
                    language = line.recognizedLanguage.takeIf { it.isNotEmpty() },
                )
            }
        }
    }

    fun release() {
        recognizer.close()
    }
}
