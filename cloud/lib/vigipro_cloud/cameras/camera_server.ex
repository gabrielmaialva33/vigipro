defmodule VigiproCloud.Cameras.CameraServer do
  @moduledoc """
  GenServer for each active camera in the system.
  Maintains state: online/offline, viewers, last event, metrics.
  """
  use GenServer, restart: :transient

  require Logger

  defstruct [
    :id,
    :name,
    :stream_url,
    :site_id,
    :gateway_id,
    status: :offline,
    viewers: 0,
    ptz_capable: false,
    audio_capable: false,
    last_event: nil,
    last_heartbeat: nil,
    metadata: %{}
  ]

  # --- Public API ---

  def start_link(opts) do
    camera_id = Keyword.fetch!(opts, :id)
    GenServer.start_link(__MODULE__, opts, name: via(camera_id))
  end

  def get_state(camera_id) do
    GenServer.call(via(camera_id), :get_state)
  end

  def update_status(camera_id, status) when status in [:online, :offline, :error] do
    GenServer.cast(via(camera_id), {:update_status, status})
  end

  def increment_viewers(camera_id) do
    GenServer.cast(via(camera_id), :increment_viewers)
  end

  def decrement_viewers(camera_id) do
    GenServer.cast(via(camera_id), :decrement_viewers)
  end

  def heartbeat(camera_id, telemetry \\ %{}) do
    GenServer.cast(via(camera_id), {:heartbeat, telemetry})
  end

  # --- Registry ---

  defp via(camera_id) do
    {:via, Registry, {VigiproCloud.Cameras.Registry, camera_id}}
  end

  # --- Callbacks ---

  @impl true
  def init(opts) do
    state = %__MODULE__{
      id: Keyword.fetch!(opts, :id),
      name: Keyword.get(opts, :name, "Unknown"),
      stream_url: Keyword.get(opts, :stream_url),
      site_id: Keyword.get(opts, :site_id),
      gateway_id: Keyword.get(opts, :gateway_id),
      ptz_capable: Keyword.get(opts, :ptz_capable, false),
      audio_capable: Keyword.get(opts, :audio_capable, false),
      status: :online,
      last_heartbeat: System.monotonic_time(:second)
    }

    Logger.info("Camera #{state.id} (#{state.name}) started")

    # Broadcast camera online
    Phoenix.PubSub.broadcast(
      VigiproCloud.PubSub,
      "cameras",
      {:camera_status, state.id, :online}
    )

    {:ok, state}
  end

  @impl true
  def handle_call(:get_state, _from, state) do
    {:reply, state, state}
  end

  @impl true
  def handle_cast({:update_status, status}, state) do
    state = %{state | status: status}

    Phoenix.PubSub.broadcast(
      VigiproCloud.PubSub,
      "cameras",
      {:camera_status, state.id, status}
    )

    {:noreply, state}
  end

  def handle_cast(:increment_viewers, state) do
    {:noreply, %{state | viewers: state.viewers + 1}}
  end

  def handle_cast(:decrement_viewers, state) do
    {:noreply, %{state | viewers: max(0, state.viewers - 1)}}
  end

  def handle_cast({:heartbeat, telemetry}, state) do
    state = %{state | last_heartbeat: System.monotonic_time(:second), metadata: telemetry}
    {:noreply, state}
  end
end
