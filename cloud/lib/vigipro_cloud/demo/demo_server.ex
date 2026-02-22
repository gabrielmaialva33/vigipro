defmodule VigiproCloud.Demo.DemoServer do
  @moduledoc """
  Manages public demo streams.
  ISOLATED context — no auth, no RBAC, no auditing.
  Pulls from public cameras or MediaMTX and serves LL-HLS via CDN.
  """
  use GenServer

  require Logger

  defstruct cameras: [], started_at: nil

  @demo_cameras [
    %{
      id: "demo-camera1",
      name: "Entrada Principal",
      stream_url: "lavfi:testsrc2=size=854x480:rate=15",
      type: :demo_lavfi
    },
    %{
      id: "demo-cam1",
      name: "Estacionamento",
      stream_url: "lavfi:smptebars=size=854x480:rate=15",
      type: :demo_lavfi
    }
  ]

  # --- Public API ---

  def start_link(opts) do
    GenServer.start_link(__MODULE__, opts, name: __MODULE__)
  end

  def list_demo_cameras do
    GenServer.call(__MODULE__, :list_cameras)
  end

  def get_demo_camera(id) do
    GenServer.call(__MODULE__, {:get_camera, id})
  end

  # --- Callbacks ---

  @impl true
  def init(_opts) do
    Logger.info("DemoServer started with #{length(@demo_cameras)} cameras")

    state = %__MODULE__{
      cameras: @demo_cameras,
      started_at: DateTime.utc_now()
    }

    # Start HLS streams for demo cameras
    for cam <- @demo_cameras do
      VigiproCloud.Streaming.StreamSupervisor.start_stream(cam.id, cam.stream_url)
    end

    {:ok, state}
  end

  @impl true
  def handle_call(:list_cameras, _from, state) do
    {:reply, state.cameras, state}
  end

  def handle_call({:get_camera, id}, _from, state) do
    camera = Enum.find(state.cameras, &(&1.id == id))
    {:reply, camera, state}
  end
end
