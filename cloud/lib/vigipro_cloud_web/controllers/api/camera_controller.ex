defmodule VigiproCloudWeb.Api.CameraController do
  @moduledoc """
  REST API for cameras — consumed by Android app.
  Authenticated endpoints use Supabase JWT (user_id from token).
  """
  use VigiproCloudWeb, :controller

  alias VigiproCloud.Cameras.Store

  # --- Authenticated endpoints ---

  @doc "GET /api/v1/cameras — List user's cameras"
  def index(conn, params) do
    user_id = conn.assigns.current_user_id
    site_id = params["site_id"]

    cameras =
      Store.list_cameras(user_id)
      |> maybe_filter_site(site_id)
      |> Enum.map(&camera_to_json/1)

    json(conn, %{cameras: cameras})
  end

  @doc "GET /api/v1/cameras/:id — Get a single camera"
  def show(conn, %{"id" => id}) do
    user_id = conn.assigns.current_user_id

    case Store.get_camera(id) do
      {:ok, %{user_id: ^user_id} = camera} ->
        json(conn, %{camera: camera_to_json(camera)})

      {:ok, _other_user} ->
        conn |> put_status(:forbidden) |> json(%{error: "Access denied"})

      {:error, :not_found} ->
        conn |> put_status(:not_found) |> json(%{error: "Camera not found"})
    end
  end

  @doc "POST /api/v1/cameras — Register a new camera"
  def create(conn, %{"camera" => camera_params}) do
    user_id = conn.assigns.current_user_id
    Store.put_camera(user_id, camera_params)
    camera_id = camera_params["id"]

    case Store.get_camera(camera_id) do
      {:ok, camera} ->
        conn
        |> put_status(:created)
        |> json(%{camera: camera_to_json(camera)})

      _ ->
        conn |> put_status(:unprocessable_entity) |> json(%{error: "Failed to create camera"})
    end
  end

  @doc "PUT /api/v1/cameras/:id — Update camera metadata"
  def update(conn, %{"id" => id, "camera" => camera_params}) do
    user_id = conn.assigns.current_user_id

    case Store.get_camera(id) do
      {:ok, %{user_id: ^user_id}} ->
        camera_params = Map.put(camera_params, "id", id)
        Store.put_camera(user_id, camera_params)

        {:ok, camera} = Store.get_camera(id)
        json(conn, %{camera: camera_to_json(camera)})

      {:ok, _other_user} ->
        conn |> put_status(:forbidden) |> json(%{error: "Access denied"})

      {:error, :not_found} ->
        conn |> put_status(:not_found) |> json(%{error: "Camera not found"})
    end
  end

  @doc "DELETE /api/v1/cameras/:id — Remove camera"
  def delete(conn, %{"id" => id}) do
    user_id = conn.assigns.current_user_id

    case Store.get_camera(id) do
      {:ok, %{user_id: ^user_id}} ->
        Store.delete_camera(id)
        json(conn, %{status: "deleted"})

      {:ok, _other_user} ->
        conn |> put_status(:forbidden) |> json(%{error: "Access denied"})

      {:error, :not_found} ->
        conn |> put_status(:not_found) |> json(%{error: "Camera not found"})
    end
  end

  @doc "PATCH /api/v1/cameras/:id/status — Update camera status"
  def update_status(conn, %{"id" => id, "status" => status}) do
    user_id = conn.assigns.current_user_id

    case Store.get_camera(id) do
      {:ok, %{user_id: ^user_id}} ->
        status_atom =
          case status do
            "ONLINE" -> :online
            "OFFLINE" -> :offline
            "ERROR" -> :error
            _ -> :offline
          end

        Store.update_status(id, status_atom)
        json(conn, %{status: "ok"})

      {:ok, _other_user} ->
        conn |> put_status(:forbidden) |> json(%{error: "Access denied"})

      {:error, :not_found} ->
        conn |> put_status(:not_found) |> json(%{error: "Camera not found"})
    end
  end

  @doc "POST /api/v1/cameras/sync — Batch sync all cameras from Android"
  def sync(conn, %{"cameras" => cameras}) do
    user_id = conn.assigns.current_user_id

    case Store.sync_cameras(user_id, cameras) do
      {:ok, count} ->
        json(conn, %{synced: count, status: "ok"})

      {:error, reason} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: "Sync failed: #{inspect(reason)}"})
    end
  end

  # --- Demo cameras (no auth) ---

  @doc "GET /api/demo/cameras — Public demo cameras"
  def demo_index(conn, _params) do
    cameras = VigiproCloud.Demo.DemoServer.list_demo_cameras()
    json(conn, %{cameras: cameras})
  end

  # --- Private ---

  defp camera_to_json(camera) do
    %{
      id: camera.id,
      name: camera.name,
      site_id: camera.site_id,
      status: camera.status |> to_string() |> String.upcase(),
      ptz_capable: camera.ptz_capable,
      audio_capable: camera.audio_capable,
      thumbnail_url: camera.thumbnail_url,
      sort_order: camera.sort_order,
      onvif_address: camera.onvif_address,
      stream_profile: camera.stream_profile,
      updated_at: camera.updated_at
    }
  end

  defp maybe_filter_site(cameras, nil), do: cameras
  defp maybe_filter_site(cameras, site_id) do
    Enum.filter(cameras, &(&1.site_id == site_id))
  end
end
