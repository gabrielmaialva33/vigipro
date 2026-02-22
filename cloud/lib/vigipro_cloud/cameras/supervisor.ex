defmodule VigiproCloud.Cameras.Supervisor do
  @moduledoc """
  DynamicSupervisor for cameras.
  Each camera = 1 CameraServer GenServer.
  """
  use DynamicSupervisor

  def start_link(init_arg) do
    DynamicSupervisor.start_link(__MODULE__, init_arg, name: __MODULE__)
  end

  @impl true
  def init(_init_arg) do
    DynamicSupervisor.init(strategy: :one_for_one)
  end

  @doc "Starts a CameraServer for the given camera."
  def start_camera(opts) do
    spec = {VigiproCloud.Cameras.CameraServer, opts}
    DynamicSupervisor.start_child(__MODULE__, spec)
  end

  @doc "Stops the CameraServer for a camera."
  def stop_camera(camera_id) do
    case Registry.lookup(VigiproCloud.Cameras.Registry, camera_id) do
      [{pid, _}] -> DynamicSupervisor.terminate_child(__MODULE__, pid)
      [] -> {:error, :not_found}
    end
  end

  @doc "Lists all active cameras."
  def list_cameras do
    DynamicSupervisor.which_children(__MODULE__)
    |> Enum.map(fn {_, pid, _, _} ->
      GenServer.call(pid, :get_state)
    end)
  end
end
