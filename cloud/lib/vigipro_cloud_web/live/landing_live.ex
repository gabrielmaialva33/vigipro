defmodule VigiproCloudWeb.LandingLive do
  @moduledoc """
  Public landing page with live camera streams.
  Zero auth — any visitor can see demo streams via HLS.
  """
  use VigiproCloudWeb, :live_view

  @impl true
  def mount(_params, _session, socket) do
    cameras = VigiproCloud.Demo.DemoServer.list_demo_cameras()
    token = VigiproCloud.Auth.JWT.generate_ephemeral_token()

    events =
      if connected?(socket) do
        Phoenix.PubSub.subscribe(VigiproCloud.PubSub, "demo:events")
        Phoenix.PubSub.subscribe(VigiproCloud.PubSub, "streams")
        generate_demo_events()
      else
        []
      end

    socket =
      socket
      |> assign(:cameras, cameras)
      |> assign(:ephemeral_token, token)
      |> assign(:selected_camera, List.first(cameras))
      |> assign(:viewer_count, Enum.random(12..47))
      |> assign(:events, events)
      |> assign(:uptime_seconds, 0)
      |> assign(:stream_status, :connecting)

    if connected?(socket) do
      :timer.send_interval(1000, self(), :tick)
      :timer.send_interval(8000, self(), :generate_event)
    end

    {:ok, socket}
  end

  @impl true
  def render(assigns) do
    ~H"""
    <div class="min-h-screen bg-gradient-to-b from-gray-950 via-gray-900 to-gray-950 text-white selection:bg-emerald-500/30">
      <style>
        @keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes slideIn { from { opacity: 0; transform: translateX(-10px); } to { opacity: 1; transform: translateX(0); } }
        @keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
        @keyframes glow { 0%, 100% { box-shadow: 0 0 5px rgba(16, 185, 129, 0.4); } 50% { box-shadow: 0 0 20px rgba(16, 185, 129, 0.6), 0 0 40px rgba(16, 185, 129, 0.2); } }
        .animate-fade-in-up { animation: fadeInUp 0.6s ease-out both; }
        .animate-slide-in { animation: slideIn 0.4s ease-out both; }
        .animate-glow { animation: glow 2s ease-in-out infinite; }
        .delay-100 { animation-delay: 0.1s; }
        .delay-200 { animation-delay: 0.2s; }
        .delay-300 { animation-delay: 0.3s; }
        .delay-400 { animation-delay: 0.4s; }
        .delay-500 { animation-delay: 0.5s; }
        .glass { background: rgba(255,255,255,0.03); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.08); }
        .glass-hover:hover { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.12); }
      </style>

      <!-- Header -->
      <header class="px-6 py-4 flex items-center justify-between border-b border-white/5 animate-fade-in-up">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 bg-gradient-to-br from-emerald-400 to-emerald-600 rounded-lg flex items-center justify-center font-bold text-sm shadow-lg shadow-emerald-500/20">
            VP
          </div>
          <div>
            <h1 class="text-lg font-semibold tracking-tight">VigiPro</h1>
            <p class="text-[10px] text-emerald-400/80 font-medium uppercase tracking-widest -mt-0.5">Cloud</p>
          </div>
        </div>
        <div class="flex items-center gap-5">
          <div class="hidden sm:flex items-center gap-2 text-sm text-gray-400">
            <span class="relative flex h-2 w-2">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span>{@viewer_count} assistindo</span>
          </div>
          <a href="/register" class="px-5 py-2 bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-400 hover:to-emerald-500 rounded-lg text-sm font-medium transition-all shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/40">
            Comecar gratis
          </a>
        </div>
      </header>

      <!-- Main Content -->
      <main class="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-10">
        <!-- Hero -->
        <div class="text-center mb-8 sm:mb-10 animate-fade-in-up delay-100">
          <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full glass text-xs text-emerald-400 mb-4">
            <span class="relative flex h-1.5 w-1.5">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-500 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-red-500"></span>
            </span>
            AO VIVO — Demo com cameras reais
          </div>
          <h2 class="text-3xl sm:text-5xl font-bold mb-3 tracking-tight">
            Suas cameras,
            <span class="bg-gradient-to-r from-emerald-400 to-cyan-400 bg-clip-text text-transparent">em qualquer lugar</span>
          </h2>
          <p class="text-gray-400 text-base sm:text-lg max-w-2xl mx-auto leading-relaxed">
            Sem port forwarding. Sem NVR caro. Sem vendor lock-in.
            <span class="text-gray-300">Qualquer camera ONVIF/RTSP funciona.</span>
          </p>
        </div>

        <!-- Camera Grid -->
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-4 mb-10 animate-fade-in-up delay-200">
          <!-- Main Player -->
          <div class="lg:col-span-3 glass rounded-2xl overflow-hidden animate-glow">
            <div class="aspect-video relative bg-black/50">
              <div
                id="main-player"
                phx-hook="HlsPlayer"
                phx-update="ignore"
                data-camera-id={@selected_camera && @selected_camera.id}
                class="w-full h-full"
              >
                <div data-placeholder class="absolute inset-0 flex items-center justify-center">
                  <div class="text-center">
                    <div class="w-12 h-12 border-2 border-emerald-500/50 border-t-emerald-500 rounded-full animate-spin mx-auto mb-3"></div>
                    <p class="text-gray-500 text-sm">Conectando ao stream...</p>
                  </div>
                </div>
              </div>

              <!-- Top overlay -->
              <div class="absolute top-0 left-0 right-0 p-3 flex items-start justify-between bg-gradient-to-b from-black/60 to-transparent pointer-events-none">
                <div class="flex items-center gap-2">
                  <span class="flex items-center gap-1.5 px-2.5 py-1 bg-red-600/90 rounded text-[11px] font-bold uppercase tracking-wider">
                    <span class="relative flex h-1.5 w-1.5">
                      <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-white opacity-75"></span>
                      <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-white"></span>
                    </span>
                    Ao vivo
                  </span>
                  <span class="px-2.5 py-1 glass rounded text-xs font-medium">
                    {@selected_camera && @selected_camera.name}
                  </span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="px-2.5 py-1 glass rounded text-[11px] text-gray-300">
                    {format_uptime(@uptime_seconds)}
                  </span>
                </div>
              </div>

              <!-- Bottom gradient -->
              <div class="absolute bottom-0 left-0 right-0 h-16 bg-gradient-to-t from-black/40 to-transparent pointer-events-none" />
            </div>
          </div>

          <!-- Sidebar -->
          <div class="space-y-3">
            <!-- Camera List -->
            <div class="glass rounded-2xl p-4">
              <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Cameras</h3>
              <div class="space-y-2">
                <%= for camera <- @cameras do %>
                  <button
                    phx-click="select_camera"
                    phx-value-id={camera.id}
                    class={"w-full text-left p-3 rounded-xl transition-all duration-200 #{if @selected_camera && @selected_camera.id == camera.id, do: "bg-emerald-500/15 border border-emerald-500/30 shadow-lg shadow-emerald-500/5", else: "glass glass-hover"}"}
                  >
                    <div class="flex items-center gap-3">
                      <div class={"w-9 h-9 rounded-lg flex items-center justify-center #{if @selected_camera && @selected_camera.id == camera.id, do: "bg-emerald-500/20", else: "bg-white/5"}"}>
                        <svg class={"w-4 h-4 #{if @selected_camera && @selected_camera.id == camera.id, do: "text-emerald-400", else: "text-gray-500"}"} fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                        </svg>
                      </div>
                      <div class="min-w-0">
                        <p class="font-medium text-sm truncate">{camera.name}</p>
                        <div class="flex items-center gap-1.5 mt-0.5">
                          <span class="relative flex h-1.5 w-1.5">
                            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-500 opacity-50"></span>
                            <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-emerald-500"></span>
                          </span>
                          <span class="text-[11px] text-emerald-400/80">Online</span>
                        </div>
                      </div>
                    </div>
                  </button>
                <% end %>
              </div>
            </div>

            <!-- Live Events Feed -->
            <div class="glass rounded-2xl p-4">
              <h3 class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Eventos recentes</h3>
              <div class="space-y-2 max-h-48 overflow-y-auto" id="events-feed">
                <%= for {event, i} <- Enum.with_index(@events) do %>
                  <div class={"flex items-start gap-2.5 py-1.5 animate-slide-in"} style={"animation-delay: #{i * 80}ms"}>
                    <div class={"w-1.5 h-1.5 rounded-full mt-1.5 flex-shrink-0 #{event_color(event.type)}"} />
                    <div class="min-w-0">
                      <p class="text-xs text-gray-300 truncate">{event.message}</p>
                      <p class="text-[10px] text-gray-500 mt-0.5">{event.time}</p>
                    </div>
                  </div>
                <% end %>
              </div>
            </div>

            <!-- CTA -->
            <div class="relative overflow-hidden rounded-2xl">
              <div class="absolute inset-0 bg-gradient-to-br from-emerald-600/20 to-cyan-600/10" />
              <div class="relative glass p-4 border-emerald-500/20">
                <p class="text-sm font-semibold mb-1">Suas cameras aqui</p>
                <p class="text-xs text-gray-400 mb-3 leading-relaxed">
                  Conecte qualquer camera ONVIF/RTSP em menos de 5 minutos.
                </p>
                <a href="/register" class="block text-center px-4 py-2.5 bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-400 hover:to-emerald-500 rounded-xl text-sm font-medium transition-all shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/40">
                  Comecar gratis
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Features -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-16">
          <div class="glass rounded-2xl p-5 glass-hover transition-all duration-300 animate-fade-in-up delay-300">
            <div class="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center mb-3">
              <svg class="w-5 h-5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 class="font-semibold text-sm mb-1">Ultra baixa latencia</h3>
            <p class="text-xs text-gray-400 leading-relaxed">Stream otimizado com menos de 2 segundos de atraso. Mais rapido que qualquer DVR.</p>
          </div>

          <div class="glass rounded-2xl p-5 glass-hover transition-all duration-300 animate-fade-in-up delay-400">
            <div class="w-10 h-10 rounded-xl bg-cyan-500/10 flex items-center justify-center mb-3">
              <svg class="w-5 h-5 text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>
            <h3 class="font-semibold text-sm mb-1">Zero port forwarding</h3>
            <p class="text-xs text-gray-400 leading-relaxed">Gateway na sua rede local. Sem abrir portas, sem DDNS, sem risco.</p>
          </div>

          <div class="glass rounded-2xl p-5 glass-hover transition-all duration-300 animate-fade-in-up delay-400">
            <div class="w-10 h-10 rounded-xl bg-violet-500/10 flex items-center justify-center mb-3">
              <svg class="w-5 h-5 text-violet-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h3 class="font-semibold text-sm mb-1">Qualquer camera</h3>
            <p class="text-xs text-gray-400 leading-relaxed">ONVIF, RTSP, qualquer marca. Intelbras, Hikvision, Dahua, Axis — tudo funciona.</p>
          </div>

          <div class="glass rounded-2xl p-5 glass-hover transition-all duration-300 animate-fade-in-up delay-500">
            <div class="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center mb-3">
              <svg class="w-5 h-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 class="font-semibold text-sm mb-1">App nativo Android</h3>
            <p class="text-xs text-gray-400 leading-relaxed">Notificacoes push, multiview, PTZ, biometria. Tudo na palma da mao.</p>
          </div>
        </div>

        <!-- Stats -->
        <div class="glass rounded-2xl p-6 sm:p-8 mb-16 animate-fade-in-up delay-500">
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-6 text-center">
            <div>
              <p class="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-emerald-400 to-cyan-400 bg-clip-text text-transparent">99.9%</p>
              <p class="text-xs text-gray-400 mt-1">Uptime garantido</p>
            </div>
            <div>
              <p class="text-2xl sm:text-3xl font-bold text-white">&lt;2s</p>
              <p class="text-xs text-gray-400 mt-1">Latencia media</p>
            </div>
            <div>
              <p class="text-2xl sm:text-3xl font-bold text-white">256-bit</p>
              <p class="text-xs text-gray-400 mt-1">Criptografia</p>
            </div>
            <div>
              <p class="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-emerald-400 to-cyan-400 bg-clip-text text-transparent">24/7</p>
              <p class="text-xs text-gray-400 mt-1">Monitoramento</p>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <footer class="text-center py-8 border-t border-white/5">
          <div class="flex items-center justify-center gap-2 mb-2">
            <div class="w-6 h-6 bg-gradient-to-br from-emerald-400 to-emerald-600 rounded flex items-center justify-center font-bold text-[10px]">
              VP
            </div>
            <span class="text-sm font-medium text-gray-400">VigiPro Cloud</span>
          </div>
          <p class="text-xs text-gray-600">
            Monitoramento profissional sem complicacao
          </p>
        </footer>
      </main>
    </div>
    """
  end

  # --- Event Handlers ---

  @impl true
  def handle_event("select_camera", %{"id" => id}, socket) do
    camera = Enum.find(socket.assigns.cameras, &(&1.id == id))

    socket =
      socket
      |> assign(:selected_camera, camera)
      |> push_event("switch-camera", %{camera_id: id})

    {:noreply, socket}
  end

  # --- PubSub + Timers ---

  @impl true
  def handle_info(:tick, socket) do
    # Update uptime and randomly fluctuate viewer count
    viewer_delta = Enum.random(-2..3)
    new_viewers = max(5, socket.assigns.viewer_count + viewer_delta)

    socket =
      socket
      |> assign(:uptime_seconds, socket.assigns.uptime_seconds + 1)
      |> assign(:viewer_count, new_viewers)

    {:noreply, socket}
  end

  def handle_info(:generate_event, socket) do
    event = random_event()
    events = [event | Enum.take(socket.assigns.events, 7)]
    {:noreply, assign(socket, :events, events)}
  end

  def handle_info({:stream_status, _camera_id, status}, socket) do
    {:noreply, assign(socket, :stream_status, status)}
  end

  def handle_info(_msg, socket), do: {:noreply, socket}

  # --- Helpers ---

  defp format_uptime(seconds) do
    h = div(seconds, 3600)
    m = div(rem(seconds, 3600), 60)
    s = rem(seconds, 60)

    if h > 0 do
      "#{pad(h)}:#{pad(m)}:#{pad(s)}"
    else
      "#{pad(m)}:#{pad(s)}"
    end
  end

  defp pad(n), do: String.pad_leading(to_string(n), 2, "0")

  defp event_color(:motion), do: "bg-amber-500"
  defp event_color(:online), do: "bg-emerald-500"
  defp event_color(:snapshot), do: "bg-cyan-500"
  defp event_color(:alert), do: "bg-red-500"
  defp event_color(_), do: "bg-gray-500"

  defp generate_demo_events do
    [
      %{type: :online, message: "Entrada Principal conectada", time: "agora"},
      %{type: :online, message: "Estacionamento conectada", time: "agora"},
      %{type: :motion, message: "Movimento detectado — Estacionamento", time: "1 min atras"},
      %{type: :snapshot, message: "Snapshot salvo — Entrada Principal", time: "3 min atras"},
      %{type: :alert, message: "Alerta de zona — Estacionamento", time: "5 min atras"},
    ]
  end

  defp random_event do
    cam = Enum.random(["Entrada Principal", "Estacionamento"])

    events = [
      %{type: :motion, message: "Movimento detectado — #{cam}"},
      %{type: :snapshot, message: "Snapshot automatico — #{cam}"},
      %{type: :alert, message: "Alerta de perimetro — #{cam}"},
      %{type: :online, message: "Heartbeat OK — #{cam}"},
    ]

    event = Enum.random(events)
    Map.put(event, :time, "agora")
  end
end
