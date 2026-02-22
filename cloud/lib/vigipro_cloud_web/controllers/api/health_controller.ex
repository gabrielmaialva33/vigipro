defmodule VigiproCloudWeb.Api.HealthController do
  @moduledoc """
  Health check and server status endpoint.
  Public — no auth required. Used by Android app to verify connectivity.
  """
  use VigiproCloudWeb, :controller

  @doc "GET /api/health — Server health check"
  def index(conn, _params) do
    json(conn, %{
      status: "ok",
      version: "0.1.0",
      node: node() |> to_string(),
      uptime_seconds: uptime_seconds()
    })
  end

  defp uptime_seconds do
    {uptime_ms, _} = :erlang.statistics(:wall_clock)
    div(uptime_ms, 1000)
  end
end
