package com.vigipro.core.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Manages audio capture from microphone and streaming to camera speaker
 * via RTSP backchannel (ONVIF Profile T).
 *
 * Audio pipeline: AudioRecord (PCM 16-bit, 8kHz mono)
 *   → G.711 encode (A-law or mu-law per camera SDP)
 *   → RTP packet (12-byte header + 160-byte payload)
 *   → TCP interleaved send via RtspBackchannelClient
 *
 * Uses VOICE_COMMUNICATION audio source for built-in AEC (Acoustic Echo Cancellation),
 * preventing feedback loop when camera speaker and phone mic are close.
 */
@Singleton
class AudioCaptureManager @Inject constructor(
    private val backchannelClient: RtspBackchannelClient,
) {

    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var isCapturing = false
    @Volatile private var payloadType: Int = 8 // PCMA default

    private var sequenceNumber = 0
    private var timestamp = 0L
    private val ssrc = Random.nextLong(0, 0xFFFFFFFFL)

    val isConnected: Boolean get() = backchannelClient.isConnected

    /**
     * Connect to camera RTSP backchannel.
     * @return true on success, false on failure
     */
    suspend fun connectToCamera(
        rtspUrl: String,
        username: String,
        password: String,
    ): Boolean {
        val pt = backchannelClient.connect(rtspUrl, username, password)
        if (pt < 0) return false
        payloadType = pt
        return true
    }

    /**
     * Start capturing audio from microphone and streaming to camera.
     * Must be called from a coroutine scope — runs blocking IO loop until cancelled or stopped.
     *
     * Audio is captured in 20ms frames (160 samples @ 8kHz = 160 bytes G.711),
     * matching standard RTP telephony framing.
     */
    @SuppressLint("MissingPermission")
    suspend fun startCapture() = withContext(Dispatchers.IO) {
        if (isCapturing) return@withContext

        val sampleRate = SAMPLE_RATE
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(FRAME_SIZE_BYTES * 4) // at least 4 frames buffer

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, // built-in AEC
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            recorder.release()
            return@withContext
        }

        audioRecord = recorder
        isCapturing = true
        sequenceNumber = 0
        timestamp = 0L

        try {
            recorder.startRecording()
            Log.i(TAG, "Audio capture started: codec=${if (payloadType == 8) "PCMA" else "PCMU"}")

            val pcmBuffer = ShortArray(FRAME_SIZE_SAMPLES)

            while (isActive && isCapturing) {
                val samplesRead = recorder.read(pcmBuffer, 0, FRAME_SIZE_SAMPLES)
                if (samplesRead <= 0) continue

                // Encode PCM → G.711
                val encoded = if (payloadType == PAYLOAD_PCMA) {
                    G711Codec.encodePcmToAlaw(pcmBuffer)
                } else {
                    G711Codec.encodePcmToMulaw(pcmBuffer)
                }

                // Build RTP packet
                val rtpPacket = RtpPacketBuilder.build(
                    payloadType = payloadType,
                    seq = sequenceNumber,
                    timestamp = timestamp,
                    ssrc = ssrc,
                    payload = encoded,
                )

                // Send via backchannel
                backchannelClient.sendAudio(rtpPacket)

                sequenceNumber = (sequenceNumber + 1) and 0xFFFF
                timestamp += FRAME_SIZE_SAMPLES
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio capture error", e)
        } finally {
            isCapturing = false
            try {
                recorder.stop()
            } catch (_: Exception) { }
            try {
                recorder.release()
            } catch (_: Exception) { }
            audioRecord = null
            Log.i(TAG, "Audio capture stopped")
        }
    }

    /**
     * Stop capturing audio. Does not disconnect from camera.
     */
    fun stopCapture() {
        isCapturing = false
    }

    /**
     * Stop capture and disconnect from camera backchannel.
     */
    suspend fun disconnect() {
        stopCapture()
        backchannelClient.disconnect()
    }

    companion object {
        private const val TAG = "AudioCapture"
        private const val SAMPLE_RATE = 8000
        private const val FRAME_SIZE_SAMPLES = 160 // 20ms @ 8kHz
        private const val FRAME_SIZE_BYTES = FRAME_SIZE_SAMPLES * 2 // PCM 16-bit = 2 bytes/sample
        private const val PAYLOAD_PCMA = 8
    }
}
