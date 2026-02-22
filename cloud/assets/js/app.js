// Include phoenix_html to handle method=PUT/DELETE in forms and buttons.
import "phoenix_html"
// Establish Phoenix Socket and LiveView configuration.
import {Socket} from "phoenix"
import {LiveSocket} from "phoenix_live_view"
import {hooks as colocatedHooks} from "phoenix-colocated/vigipro_cloud"
import topbar from "../vendor/topbar"
import Hls from "../vendor/hls.mjs"

// --- HLS Player Hook ---
// Manages hls.js video playback for live camera streams.
// Used by LandingLive with phx-hook="HlsPlayer".
const HlsPlayer = {
  mounted() {
    this.setupPlayer()

    // Listen for camera switches via LiveView push_event
    this.handleEvent("switch-camera", ({camera_id}) => {
      if (camera_id && camera_id !== this.currentCameraId) {
        this.switchStream(camera_id)
      }
    })
  },

  destroyed() {
    this.cleanup()
  },

  setupPlayer() {
    const cameraId = this.el.dataset.cameraId
    if (!cameraId) return

    this.currentCameraId = cameraId
    this.retryTimer = null
    this.retryCount = 0

    // Create <video> element
    this.video = document.createElement("video")
    this.video.autoplay = true
    this.video.muted = true
    this.video.playsInline = true
    this.video.loop = false
    this.video.className = "w-full h-full object-cover"
    this.video.style.background = "black"

    // Clear placeholder and append video
    const placeholder = this.el.querySelector("[data-placeholder]")
    if (placeholder) placeholder.style.display = "none"
    this.el.appendChild(this.video)

    if (Hls.isSupported()) {
      this.hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        liveSyncDurationCount: 3,
        liveMaxLatencyDurationCount: 6,
        maxBufferLength: 10,
        manifestLoadingTimeOut: 15000,
        manifestLoadingMaxRetry: 6,
        levelLoadingTimeOut: 15000,
      })
      this.loadStream(cameraId)
    } else if (this.video.canPlayType("application/vnd.apple.mpegurl")) {
      // Safari native HLS
      this.video.src = `/hls/${cameraId}/index.m3u8`
      this.video.play().catch(() => {})
    } else {
      console.warn("HLS not supported in this browser")
    }
  },

  loadStream(cameraId) {
    if (!this.hls) return

    const url = `/hls/${cameraId}/index.m3u8`
    this.hls.loadSource(url)
    this.hls.attachMedia(this.video)

    this.hls.off(Hls.Events.MANIFEST_PARSED)
    this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
      this.video.play().catch(() => {})
      this.retryCount = 0
      // Notify LiveView that stream is playing
      this.el.dispatchEvent(new CustomEvent("stream-playing", {bubbles: true}))
    })

    this.hls.off(Hls.Events.ERROR)
    this.hls.on(Hls.Events.ERROR, (_event, data) => {
      if (data.fatal) {
        switch (data.type) {
          case Hls.ErrorTypes.NETWORK_ERROR:
            // Playlist not ready yet or network hiccup — retry
            this.scheduleRetry(cameraId)
            break
          case Hls.ErrorTypes.MEDIA_ERROR:
            this.hls.recoverMediaError()
            break
          default:
            this.scheduleRetry(cameraId)
            break
        }
      }
    })
  },

  switchStream(cameraId) {
    this.currentCameraId = cameraId
    this.retryCount = 0
    if (this.retryTimer) {
      clearTimeout(this.retryTimer)
      this.retryTimer = null
    }

    if (this.hls) {
      this.loadStream(cameraId)
    } else if (this.video) {
      this.video.src = `/hls/${cameraId}/index.m3u8`
      this.video.play().catch(() => {})
    }
  },

  scheduleRetry(cameraId) {
    if (this.retryCount >= 30) return

    const delay = Math.min(2000 * Math.pow(1.5, this.retryCount), 15000)
    this.retryCount++

    this.retryTimer = setTimeout(() => {
      if (this.currentCameraId === cameraId && this.hls) {
        this.loadStream(cameraId)
      }
    }, delay)
  },

  cleanup() {
    if (this.retryTimer) clearTimeout(this.retryTimer)
    if (this.hls) {
      this.hls.destroy()
      this.hls = null
    }
    if (this.video) {
      this.video.pause()
      this.video.src = ""
      this.video.remove()
      this.video = null
    }
  },
}

// --- LiveSocket Setup ---

const csrfToken = document.querySelector("meta[name='csrf-token']").getAttribute("content")
const liveSocket = new LiveSocket("/live", Socket, {
  longPollFallbackMs: 2500,
  params: {_csrf_token: csrfToken},
  hooks: {...colocatedHooks, HlsPlayer},
})

// Show progress bar on live navigation and form submits
topbar.config({barColors: {0: "#10b981"}, shadowColor: "rgba(0, 0, 0, .3)"})
window.addEventListener("phx:page-loading-start", _info => topbar.show(300))
window.addEventListener("phx:page-loading-stop", _info => topbar.hide())

// connect if there are any LiveViews on the page
liveSocket.connect()

// expose liveSocket on window for web console debug logs and latency simulation:
// >> liveSocket.enableDebug()
// >> liveSocket.enableLatencySim(1000)
// >> liveSocket.disableLatencySim()
window.liveSocket = liveSocket

// Dev helpers
if (process.env.NODE_ENV === "development") {
  window.addEventListener("phx:live_reload:attached", ({detail: reloader}) => {
    reloader.enableServerLogs()

    let keyDown
    window.addEventListener("keydown", e => keyDown = e.key)
    window.addEventListener("keyup", _e => keyDown = null)
    window.addEventListener("click", e => {
      if(keyDown === "c"){
        e.preventDefault()
        e.stopImmediatePropagation()
        reloader.openEditorAtCaller(e.target)
      } else if(keyDown === "d"){
        e.preventDefault()
        e.stopImmediatePropagation()
        reloader.openEditorAtDef(e.target)
      }
    }, true)

    window.liveReloader = reloader
  })
}
