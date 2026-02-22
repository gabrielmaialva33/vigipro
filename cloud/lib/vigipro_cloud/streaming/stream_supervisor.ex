defmodule VigiproCloud.Streaming.StreamSupervisor do
  @moduledoc """
  DynamicSupervisor for StreamServer processes.
  Each camera stream = 1 FFmpeg process managed by a StreamServer.
  """
  use DynamicSupervisor

  def start_link(init_arg) do
    DynamicSupervisor.start_link(__MODULE__, init_arg, name: __MODULE__)
  end

  @impl true
  def init(_init_arg) do
    DynamicSupervisor.init(strategy: :one_for_one)
  end

  @doc "Starts an FFmpeg HLS stream for a camera."
  def start_stream(camera_id, rtsp_url) do
    spec = {
      VigiproCloud.Streaming.StreamServer,
      camera_id: camera_id, rtsp_url: rtsp_url
    }

    case DynamicSupervisor.start_child(__MODULE__, spec) do
      {:ok, pid} -> {:ok, pid}
      {:error, {:already_started, pid}} -> {:ok, pid}
      error -> error
    end
  end

  @doc "Stops the FFmpeg stream for a camera."
  def stop_stream(camera_id) do
    case Registry.lookup(VigiproCloud.Cameras.Registry, {:stream, camera_id}) do
      [{pid, _}] -> DynamicSupervisor.terminate_child(__MODULE__, pid)
      [] -> {:error, :not_found}
    end
  end

  @doc "Lists all active stream camera IDs."
  def list_streams do
    DynamicSupervisor.which_children(__MODULE__)
    |> Enum.map(fn {_, pid, _, _} ->
      GenServer.call(pid, :get_status)
    end)
  end
end
