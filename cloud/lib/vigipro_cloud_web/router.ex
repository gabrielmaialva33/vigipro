defmodule VigiproCloudWeb.Router do
  use VigiproCloudWeb, :router

  pipeline :browser do
    plug :accepts, ["html"]
    plug :fetch_session
    plug :fetch_live_flash
    plug :put_root_layout, html: {VigiproCloudWeb.Layouts, :root}
    plug :protect_from_forgery
    plug :put_secure_browser_headers
  end

  pipeline :api do
    plug :accepts, ["json"]
  end

  pipeline :authenticated_api do
    plug :accepts, ["json"]
    plug VigiproCloud.Auth.Plug
  end

  # HLS streaming — serves .m3u8 and .ts segments
  scope "/hls", VigiproCloudWeb do
    pipe_through :api

    get "/:camera_id/:filename", HlsController, :serve
  end

  # Public API — health check + demo/public cameras
  scope "/api", VigiproCloudWeb.Api do
    pipe_through :api

    get "/health", HealthController, :index
    get "/demo/cameras", CameraController, :demo_index

    # Public camera catalog (curated)
    get "/public/cameras", PublicCameraController, :index
    get "/public/categories", PublicCameraController, :categories
  end

  # Private API — requires Supabase JWT
  scope "/api/v1", VigiproCloudWeb.Api do
    pipe_through :authenticated_api

    # Cameras CRUD
    get "/cameras", CameraController, :index
    get "/cameras/:id", CameraController, :show
    post "/cameras", CameraController, :create
    put "/cameras/:id", CameraController, :update
    delete "/cameras/:id", CameraController, :delete
    patch "/cameras/:id/status", CameraController, :update_status
    post "/cameras/sync", CameraController, :sync

    # Events
    get "/events", EventController, :index
    post "/events", EventController, :create
    post "/events/batch", EventController, :batch_create
  end

  # Dev routes (LiveDashboard)
  if Application.compile_env(:vigipro_cloud, :dev_routes) do
    import Phoenix.LiveDashboard.Router

    scope "/dev" do
      pipe_through :browser
      live_dashboard "/dashboard", metrics: VigiproCloudWeb.Telemetry
    end
  end
end
