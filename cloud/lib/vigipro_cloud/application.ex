defmodule VigiproCloud.Application do
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      # Telemetry
      VigiproCloudWeb.Telemetry,
      {DNSCluster, query: Application.get_env(:vigipro_cloud, :dns_cluster_query) || :ignore},
      {Phoenix.PubSub, name: VigiproCloud.PubSub},

      # Camera metadata store (ETS)
      VigiproCloud.Cameras.Store,

      # Event store (ETS)
      VigiproCloud.Events.EventStore,

      # Camera Registry + DynamicSupervisor (for active streaming)
      {Registry, keys: :unique, name: VigiproCloud.Cameras.Registry},
      VigiproCloud.Cameras.Supervisor,

      # Streaming pipeline (FFmpeg → HLS)
      VigiproCloud.Streaming.StreamSupervisor,
      VigiproCloud.Streaming.HlsCleaner,

      # Public camera catalog (ETS)
      VigiproCloud.PublicCameras.PublicCameraStore,

      # Demo server (starts HLS streams for lavfi test sources)
      VigiproCloud.Demo.DemoServer,

      # TODO: Background jobs (Oban) — requires Ecto repo, phase 2
      # {Oban, Application.fetch_env!(:vigipro_cloud, Oban)},

      # Web endpoint (always last)
      VigiproCloudWeb.Endpoint
    ]

    opts = [strategy: :one_for_one, name: VigiproCloud.Supervisor]
    Supervisor.start_link(children, opts)
  end

  # Tell Phoenix to update the endpoint configuration
  # whenever the application is updated.
  @impl true
  def config_change(changed, _new, removed) do
    VigiproCloudWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
