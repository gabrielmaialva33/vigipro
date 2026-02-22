package com.vigipro.core.data.detection

import android.graphics.Bitmap
import com.vigipro.core.data.repository.AlertRepository
import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.EventRepository
import com.vigipro.core.model.CameraEventType
import com.vigipro.core.model.DetectedObject
import com.vigipro.core.model.DetectionCategory
import com.vigipro.core.model.RecognizedText
import com.vigipro.core.model.SceneLabel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestra deteccao inteligente on-device combinando:
 * - Object Detection (pessoa, veiculo, animal)
 * - Text Recognition (placa, sinalizacao)
 * - Image Labeling (classificacao de cena)
 *
 * Processa frames de camera e dispara alertas automaticos.
 */
@Singleton
class SmartDetectionManager @Inject constructor(
    private val objectDetection: ObjectDetectionEngine,
    private val textRecognition: TextRecognitionEngine,
    private val imageLabeling: ImageLabelingEngine,
    private val eventRepository: EventRepository,
    private val alertRepository: AlertRepository,
    private val authRepository: AuthRepository,
) {

    data class DetectionResult(
        val objects: List<DetectedObject> = emptyList(),
        val texts: List<RecognizedText> = emptyList(),
        val sceneLabels: List<SceneLabel> = emptyList(),
    ) {
        val hasPersons: Boolean get() = objects.any { it.category == DetectionCategory.PERSON }
        val hasVehicles: Boolean get() = objects.any { it.category == DetectionCategory.VEHICLE }
        val hasText: Boolean get() = texts.isNotEmpty()
        val isEmpty: Boolean get() = objects.isEmpty() && texts.isEmpty() && sceneLabels.isEmpty()
    }

    data class DetectionConfig(
        val detectPersons: Boolean = true,
        val detectVehicles: Boolean = true,
        val detectAnimals: Boolean = false,
        val recognizeText: Boolean = false,
        val classifyScene: Boolean = false,
        val alertOnPerson: Boolean = true,
    )

    /**
     * Processa um frame de camera com todas as engines configuradas.
     */
    suspend fun processFrame(
        bitmap: Bitmap,
        cameraId: String,
        cameraName: String,
        siteId: String,
        config: DetectionConfig = DetectionConfig(),
    ): DetectionResult {
        val objects = objectDetection.detect(
            bitmap = bitmap,
            detectPersons = config.detectPersons,
            detectVehicles = config.detectVehicles,
            detectAnimals = config.detectAnimals,
        )

        val texts = if (config.recognizeText) {
            textRecognition.recognize(bitmap)
        } else {
            emptyList()
        }

        val sceneLabels = if (config.classifyScene) {
            imageLabeling.classify(bitmap)
        } else {
            emptyList()
        }

        val result = DetectionResult(objects, texts, sceneLabels)

        // Log e alerta automatico pra deteccao de pessoa
        if (result.hasPersons && config.alertOnPerson) {
            handlePersonDetected(cameraId, cameraName, siteId, result)
        }

        return result
    }

    private suspend fun handlePersonDetected(
        cameraId: String,
        cameraName: String,
        siteId: String,
        result: DetectionResult,
    ) {
        val personCount = result.objects.count { it.category == DetectionCategory.PERSON }
        val message = if (personCount == 1) {
            "Pessoa detectada na camera $cameraName"
        } else {
            "$personCount pessoas detectadas na camera $cameraName"
        }

        // Log evento local
        eventRepository.logEvent(
            cameraId = cameraId,
            cameraName = cameraName,
            type = CameraEventType.OBJECT_DETECTED,
            message = message,
        )

        // Broadcast alerta via Cloud Run
        try {
            alertRepository.sendBroadcast(
                siteId = siteId,
                alertType = "PERSON_DETECTED",
                cameraName = cameraName,
                message = message,
            )
        } catch (e: Exception) {
            Timber.w(e, "Falha ao enviar alerta de pessoa detectada")
        }
    }

    fun initialize() {
        objectDetection.initialize()
    }

    fun release() {
        objectDetection.release()
        textRecognition.release()
        imageLabeling.release()
    }
}
