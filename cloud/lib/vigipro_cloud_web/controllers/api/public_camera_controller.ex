defmodule VigiproCloudWeb.Api.PublicCameraController do
  use VigiproCloudWeb, :controller

  alias VigiproCloud.PublicCameras.PublicCameraStore
  alias VigiproCloud.Streaming.StreamServer

  @doc "GET /api/public/cameras?category=traffic&page=1&page_size=20"
  def index(conn, params) do
    opts = [
      category: params["category"],
      page: parse_int(params["page"], 1),
      page_size: parse_int(params["page_size"], 20) |> min(100)
    ]

    {cameras, meta} = PublicCameraStore.list_cameras(opts)

    json(conn, %{
      cameras: Enum.map(cameras, &camera_to_json/1),
      meta: meta
    })
  end

  @doc "GET /api/public/categories"
  def categories(conn, _params) do
    categories = PublicCameraStore.list_categories()
    json(conn, %{categories: categories})
  end

  # --- Private ---

  defp camera_to_json(cam) do
    # For backend-managed sources (lavfi demo, mjpeg proxy), generate HLS URL via our backend.
    # For external HLS streams, use the stream_url directly.
    backend_managed? =
      String.starts_with?(cam.stream_url, "lavfi:") or
        String.starts_with?(cam.stream_url, "mjpeg:")

    hls_url =
      if backend_managed? do
        "#{VigiproCloudWeb.Endpoint.url()}/hls/#{cam.id}/index.m3u8"
      else
        cam.stream_url
      end

    status =
      if backend_managed? do
        to_string(StreamServer.get_status(cam.id))
      else
        to_string(cam.status)
      end

    %{
      id: cam.id,
      name: cam.name,
      description: cam.description,
      category: cam.category,
      city: cam.city,
      state: cam.state,
      hls_url: hls_url,
      thumbnail_url: cam.thumbnail_url,
      status: status,
      featured: cam.featured
    }
  end

  defp parse_int(nil, default), do: default
  defp parse_int(val, default) when is_binary(val) do
    case Integer.parse(val) do
      {n, _} -> max(n, 1)
      :error -> default
    end
  end
  defp parse_int(val, _default) when is_integer(val), do: val
end
