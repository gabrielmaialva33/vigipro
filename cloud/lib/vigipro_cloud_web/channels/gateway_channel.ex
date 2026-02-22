defmodule VigiproCloudWeb.GatewayChannel do
  @moduledoc """
  Phoenix Channel for Local Gateway communication.
  Handles: camera registration, heartbeats, ONVIF events, PTZ commands.
  """
  use Phoenix.Channel

  require Logger

  alias VigiproCloud.Cameras.Supervisor, as: CameraSup

  @impl true
  def join("gateway:" <> gateway_id, _payload, socket) do
    # TODO: Validate gateway JWT from payload
    Logger.info("Gateway #{gateway_id} connected")

    socket =
      socket
      |> assign(:gateway_id, gateway_id)
      |> assign(:cameras, [])

    {:ok, %{status: "connected"}, socket}
  end

  # Gateway registers available cameras on the LAN
  @impl true
  def handle_in("register_cameras", %{"cameras" => cameras}, socket) do
    gateway_id = socket.assigns.gateway_id

    Enum.each(cameras, fn cam ->
      CameraSup.start_camera(
        id: cam["id"],
        name: cam["name"],
        stream_url: cam["stream_url"],
        gateway_id: gateway_id,
        ptz_capable: cam["ptz_capable"] || false,
        audio_capable: cam["audio_capable"] || false
      )
    end)

    camera_ids = Enum.map(cameras, & &1["id"])
    socket = assign(socket, :cameras, camera_ids)

    Logger.info("Gateway #{gateway_id} registered #{length(cameras)} cameras")

    {:reply, {:ok, %{registered: length(cameras)}}, socket}
  end

  # Gateway sends heartbeat with hardware telemetry
  def handle_in("heartbeat", telemetry, socket) do
    gateway_id = socket.assigns.gateway_id

    Phoenix.PubSub.broadcast(
      VigiproCloud.PubSub,
      "gateways",
      {:gateway_heartbeat, gateway_id, telemetry}
    )

    # Update heartbeat on each gateway camera
    Enum.each(socket.assigns.cameras, fn cam_id ->
      VigiproCloud.Cameras.CameraServer.heartbeat(cam_id, telemetry)
    end)

    {:reply, :ok, socket}
  end

  # Gateway reports ONVIF event (motion, tamper, etc)
  def handle_in("onvif_event", %{"camera_id" => cam_id, "event" => event}, socket) do
    Logger.info("ONVIF event on #{cam_id}: #{event["type"]}")

    Phoenix.PubSub.broadcast(
      VigiproCloud.PubSub,
      "events",
      {:onvif_event, cam_id, event}
    )

    {:reply, :ok, socket}
  end

  # Server sends PTZ command to gateway (via push)
  # LiveView/Android sends here via PubSub, channel forwards to gateway
  @impl true
  def handle_info({:ptz_command, camera_id, command}, socket) do
    push(socket, "ptz_command", %{camera_id: camera_id, command: command})
    {:noreply, socket}
  end

  @impl true
  def terminate(_reason, socket) do
    gateway_id = socket.assigns.gateway_id
    Logger.info("Gateway #{gateway_id} disconnected")

    # Mark gateway cameras as offline
    Enum.each(socket.assigns.cameras, fn cam_id ->
      VigiproCloud.Cameras.CameraServer.update_status(cam_id, :offline)
    end)

    :ok
  end
end
