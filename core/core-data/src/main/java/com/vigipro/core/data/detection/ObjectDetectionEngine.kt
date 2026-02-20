package com.vigipro.core.data.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.vigipro.core.model.DetectedObject
import com.vigipro.core.model.DetectionCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ObjectDetectionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var detector: ObjectDetector? = null
    private var labels: List<String> = emptyList()

    private val personIndices = setOf(0)
    private val vehicleIndices = setOf(1, 2, 3, 5, 7)
    private val animalIndices = setOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 23)

    fun initialize(confidenceThreshold: Float = 0.5f) {
        if (detector != null) return

        labels = context.assets.open("coco_labels.txt").bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }

        val localModel = LocalModel.Builder()
            .setAssetFilePath("efficientdet_lite0.tflite")
            .build()

        val options = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .setClassificationConfidenceThreshold(confidenceThreshold)
            .setMaxPerObjectLabelCount(1)
            .build()

        detector = ObjectDetection.getClient(options)
    }

    suspend fun detect(
        bitmap: Bitmap,
        detectPersons: Boolean = true,
        detectVehicles: Boolean = true,
        detectAnimals: Boolean = false,
    ): List<DetectedObject> {
        val det = detector ?: run {
            initialize()
            detector ?: return emptyList()
        }

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        val results = suspendCancellableCoroutine { cont ->
            det.process(inputImage)
                .addOnSuccessListener { objects -> cont.resume(objects) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

        return results.mapNotNull { obj ->
            val bestLabel = obj.labels.maxByOrNull { it.confidence } ?: return@mapNotNull null
            val labelIndex = bestLabel.index
            val category = categorize(labelIndex)

            val shouldInclude = when (category) {
                DetectionCategory.PERSON -> detectPersons
                DetectionCategory.VEHICLE -> detectVehicles
                DetectionCategory.ANIMAL -> detectAnimals
                DetectionCategory.OTHER -> false
            }
            if (!shouldInclude) return@mapNotNull null

            val labelText = labels.getOrElse(labelIndex) { bestLabel.text }

            DetectedObject(
                label = labelText,
                category = category,
                confidence = bestLabel.confidence,
                boundingBox = RectF(
                    obj.boundingBox.left / width,
                    obj.boundingBox.top / height,
                    obj.boundingBox.right / width,
                    obj.boundingBox.bottom / height,
                ),
                trackingId = obj.trackingId,
            )
        }
    }

    private fun categorize(labelIndex: Int): DetectionCategory = when {
        labelIndex in personIndices -> DetectionCategory.PERSON
        labelIndex in vehicleIndices -> DetectionCategory.VEHICLE
        labelIndex in animalIndices -> DetectionCategory.ANIMAL
        else -> DetectionCategory.OTHER
    }

    fun release() {
        detector?.close()
        detector = null
    }
}
