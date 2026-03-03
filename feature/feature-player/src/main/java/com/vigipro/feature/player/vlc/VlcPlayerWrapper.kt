package com.vigipro.feature.player.vlc

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import android.util.Log

class VlcPlayerWrapper(context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val libVlc: LibVLC = LibVLC(
        context.applicationContext,
        arrayListOf(
            "--file-caching=2000",
            "--network-caching=1500",
            "--live-caching=1500",
            "--no-audio-time-stretch",
            "--avcodec-fast",
            "--avcodec-threads=0",
            "--avcodec-skiploopfilter", "0",
            "--avcodec-skip-frame", "0",
            "--avcodec-skip-idct", "0",
            "--audio-resampler", "soxr",
            "--no-drop-late-frames",
            "--no-skip-frames",
        ),
    )

    private val mediaPlayer: MediaPlayer = MediaPlayer(libVlc)

    var isPlaying: Boolean = false
        private set

    var onPlaybackStarted: (() -> Unit)? = null
    var onBuffering: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onVideoInfo: ((codec: String, width: Int, height: Int) -> Unit)? = null

    init {
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    isPlaying = true
                    mainHandler.post { onPlaybackStarted?.invoke() }
                }
                MediaPlayer.Event.Buffering -> {
                    if (event.buffering < 100f) {
                        mainHandler.post { onBuffering?.invoke() }
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    isPlaying = false
                    Log.e("VlcPlayer", "VLC playback error")
                    mainHandler.post { onError?.invoke("Erro ao reproduzir vídeo") }
                }
                MediaPlayer.Event.Vout -> {
                    val track = mediaPlayer.currentVideoTrack
                    if (track != null) {
                        val codec = track.codec?.uppercase()
                            ?.replace("H264", "H.264")
                            ?.replace("H265", "H.265")
                            ?.replace("HEVC", "H.265")
                            ?: "RTSP"
                        mainHandler.post {
                            onVideoInfo?.invoke(codec, track.width, track.height)
                        }
                    }
                }
                MediaPlayer.Event.Stopped,
                MediaPlayer.Event.EndReached,
                -> {
                    isPlaying = false
                }
            }
        }
    }

    fun attachSurface(surfaceView: SurfaceView) {
        val vlcVout = mediaPlayer.vlcVout
        vlcVout.setVideoSurface(surfaceView.holder.surface, surfaceView.holder)
        vlcVout.setWindowSize(surfaceView.width, surfaceView.height)
        vlcVout.attachViews()
        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
    }

    fun detachSurface() {
        try {
            mediaPlayer.vlcVout.detachViews()
        } catch (_: Exception) {
        }
    }

    fun play(rtspUrl: String) {
        val media = Media(libVlc, Uri.parse(rtspUrl))
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=1500")
        media.addOption(":rtsp-tcp")
        media.addOption(":no-audio-time-stretch")
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        }
    }

    fun resume() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.play()
        }
    }

    fun setVolume(volume: Int) {
        mediaPlayer.volume = volume
    }

    fun stop() {
        mediaPlayer.stop()
        isPlaying = false
    }

    fun release() {
        try {
            mediaPlayer.vlcVout.detachViews()
        } catch (_: Exception) {
        }
        mediaPlayer.release()
        libVlc.release()
    }
}
