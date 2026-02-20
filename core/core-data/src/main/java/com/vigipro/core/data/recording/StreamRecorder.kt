package com.vigipro.core.data.recording

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.vigipro.core.data.repository.RecordingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class StreamRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
) {

    companion object {
        private const val TAG = "StreamRecorder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val BITRATE = 4_000_000
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 2
        private const val DRAIN_TIMEOUT_US = 10_000L
    }

    @Volatile
    var isRecording: Boolean = false
        private set

    @Volatile
    var recordingStartTime: Long = 0L
        private set

    private var recordingPlayer: ExoPlayer? = null
    private var encoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var drainJob: Job? = null
    private var muxerStarted = false
    private var trackIndex = -1
    private var outputFile: File? = null
    private var recordingDbId: Long = -1L
    private var currentCameraId: String = ""
    private var currentCameraName: String = ""

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @OptIn(UnstableApi::class)
    suspend fun startRecording(
        rtspUrl: String,
        cameraId: String,
        cameraName: String,
        width: Int,
        height: Int,
    ): Boolean {
        if (isRecording) {
            Log.w(TAG, "Ja esta gravando")
            return false
        }

        return try {
            currentCameraId = cameraId
            currentCameraName = cameraName

            // Create output directory and file
            val dir = File(context.getExternalFilesDir("recordings"), "cam_$cameraId")
            dir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "$timestamp.mp4")
            outputFile = file

            // Setup MediaCodec encoder
            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            val codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface()
            codec.start()

            encoder = codec
            inputSurface = surface

            // Setup MediaMuxer
            val mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mediaMuxer
            muxerStarted = false
            trackIndex = -1

            // Setup second ExoPlayer for recording (renders to encoder surface)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 500,
                    /* maxBufferMs = */ 2_000,
                    /* bufferForPlaybackMs = */ 250,
                    /* bufferForPlaybackAfterRebufferMs = */ 500,
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val player = withContext(Dispatchers.Main) {
                ExoPlayer.Builder(context)
                    .setLoadControl(loadControl)
                    .build()
                    .also { exo ->
                        exo.setVideoSurface(surface)

                        val rtspMediaSource = RtspMediaSource.Factory()
                            .setForceUseRtpTcp(true)
                            .setTimeoutMs(10_000L)
                            .createMediaSource(MediaItem.fromUri(rtspUrl))

                        exo.setMediaSource(rtspMediaSource)
                        exo.prepare()
                        exo.playWhenReady = true
                    }
            }
            recordingPlayer = player

            // Wait for player to be ready
            val ready = withContext(Dispatchers.Main) {
                awaitPlayerReady(player)
            }

            if (!ready) {
                Log.e(TAG, "Player nao ficou pronto")
                releaseResources()
                return false
            }

            // Save to database
            recordingDbId = recordingRepository.startRecording(
                cameraId = cameraId,
                cameraName = cameraName,
                filePath = file.absolutePath,
            )

            // Start drain loop — assign drainJob BEFORE setting isRecording
            // to avoid race condition where stopRecording() sees isRecording=true but drainJob=null
            drainJob = scope.launch(Dispatchers.IO) {
                drainEncoderLoop()
            }

            recordingStartTime = System.currentTimeMillis()
            isRecording = true

            Log.d(TAG, "Gravacao iniciada: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar gravacao", e)
            releaseResources()
            false
        }
    }

    suspend fun stopRecording(): File? {
        if (!isRecording) return null

        isRecording = false
        val file = outputFile

        try {
            // Signal end of stream to encoder
            withContext(Dispatchers.Main) {
                recordingPlayer?.stop()
            }
            encoder?.signalEndOfInputStream()

            // Wait for drain to finish
            drainJob?.join()

            // Finalize muxer
            if (muxerStarted) {
                try {
                    muxer?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao parar muxer", e)
                }
            }

            // Update database
            if (file != null && file.exists() && recordingDbId > 0) {
                val durationMs = System.currentTimeMillis() - recordingStartTime
                recordingRepository.finishRecording(
                    id = recordingDbId,
                    fileSize = file.length(),
                    durationMs = durationMs,
                )
            }

            Log.d(TAG, "Gravacao salva: ${file?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parar gravacao", e)
        } finally {
            releaseResources()
        }

        return if (file != null && file.exists() && file.length() > 0) file else null
    }

    private suspend fun drainEncoderLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        val codec = encoder ?: return
        val mediaMuxer = muxer ?: return

        try {
            while (true) {
                coroutineContext.ensureActive()
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DRAIN_TIMEOUT_US)

                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) {
                            Log.w(TAG, "Formato mudou apos muxer iniciado")
                            continue
                        }
                        val newFormat = codec.outputFormat
                        trackIndex = mediaMuxer.addTrack(newFormat)
                        mediaMuxer.start()
                        muxerStarted = true
                        Log.d(TAG, "Muxer iniciado, formato: $newFormat")
                    }

                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!isRecording) {
                            // No more data and recording stopped
                            break
                        }
                        delay(5)
                    }

                    outputIndex >= 0 -> {
                        val encodedData = codec.getOutputBuffer(outputIndex)
                        if (encodedData != null && muxerStarted) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            Log.d(TAG, "End of stream recebido")
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no drain loop", e)
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun awaitPlayerReady(player: ExoPlayer): Boolean {
        return suspendCancellableCoroutine { cont ->
            if (player.playbackState == Player.STATE_READY) {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }

            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            player.removeListener(this)
                            if (cont.isActive) cont.resume(true)
                        }
                        Player.STATE_IDLE -> {
                            player.removeListener(this)
                            if (cont.isActive) cont.resume(false)
                        }
                        else -> {}
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    player.removeListener(this)
                    if (cont.isActive) cont.resume(false)
                }
            }
            player.addListener(listener)

            // Timeout after 15 seconds
            val timeoutJob = scope.launch {
                delay(15_000L)
                player.removeListener(listener)
                if (cont.isActive) cont.resume(false)
            }

            cont.invokeOnCancellation {
                timeoutJob.cancel()
                player.removeListener(listener)
            }
        }
    }

    private fun releaseResources() {
        drainJob?.cancel()
        drainJob = null

        try {
            encoder?.stop()
        } catch (_: Exception) {}
        try {
            encoder?.release()
        } catch (_: Exception) {}
        encoder = null

        inputSurface?.release()
        inputSurface = null

        try {
            muxer?.release()
        } catch (_: Exception) {}
        muxer = null
        muxerStarted = false
        trackIndex = -1

        try {
            recordingPlayer?.release()
        } catch (_: Exception) {}
        recordingPlayer = null

        recordingDbId = -1L
    }
}
