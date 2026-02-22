defmodule VigiproCloudWeb.HlsController do
  @moduledoc """
  Serves HLS playlist (.m3u8) and segment (.ts) files from the local
  FFmpeg output directory. Adds CORS headers for hls.js browser playback.
  """
  use VigiproCloudWeb, :controller

  @hls_root "/tmp/vigipro_hls"

  @allowed_extensions ~w(.m3u8 .ts)

  def serve(conn, %{"camera_id" => camera_id, "filename" => filename}) do
    ext = Path.extname(filename)

    unless ext in @allowed_extensions do
      conn |> send_resp(400, "Invalid file type") |> halt()
    end

    # Sanitize: only alphanumeric, dash, underscore, dot
    unless Regex.match?(~r/^[a-zA-Z0-9_\-\.]+$/, camera_id) and
             Regex.match?(~r/^[a-zA-Z0-9_\-\.]+$/, filename) do
      conn |> send_resp(400, "Invalid path") |> halt()
    end

    path = Path.join([@hls_root, camera_id, filename])

    if File.exists?(path) do
      content_type = content_type_for(ext)

      conn
      |> put_resp_header("content-type", content_type)
      |> put_resp_header("access-control-allow-origin", "*")
      |> put_resp_header("access-control-allow-methods", "GET")
      |> put_resp_header("cache-control", cache_control_for(ext))
      |> send_file(200, path)
    else
      send_resp(conn, 404, "Not found")
    end
  end

  defp content_type_for(".m3u8"), do: "application/vnd.apple.mpegurl"
  defp content_type_for(".ts"), do: "video/mp2t"
  defp content_type_for(_), do: "application/octet-stream"

  defp cache_control_for(".m3u8"), do: "no-cache, no-store"
  defp cache_control_for(".ts"), do: "public, max-age=30"
  defp cache_control_for(_), do: "no-cache"
end
