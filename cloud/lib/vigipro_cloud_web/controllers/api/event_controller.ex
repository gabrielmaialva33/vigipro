defmodule VigiproCloudWeb.Api.EventController do
  @moduledoc """
  REST API for camera events — consumed by Android app.
  Events: online/offline/detection/snapshot/camera_added/camera_removed.
  """
  use VigiproCloudWeb, :controller

  alias VigiproCloud.Events.EventStore

  @doc "GET /api/v1/events — List recent events for user"
  def index(conn, params) do
    user_id = conn.assigns.current_user_id
    limit = parse_int(params["limit"], 50)
    camera_id = params["camera_id"]

    opts = [limit: limit]
    opts = if camera_id, do: Keyword.put(opts, :camera_id, camera_id), else: opts

    events =
      EventStore.list_events(user_id, opts)
      |> Enum.map(&event_to_json/1)

    json(conn, %{events: events})
  end

  @doc "POST /api/v1/events — Log a new event"
  def create(conn, %{"event" => event_params}) do
    user_id = conn.assigns.current_user_id

    case EventStore.log_event(user_id, event_params) do
      {:ok, event} ->
        conn
        |> put_status(:created)
        |> json(%{event: event_to_json(event)})

      {:error, reason} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{error: "Failed to log event: #{inspect(reason)}"})
    end
  end

  @doc "POST /api/v1/events/batch — Log multiple events at once"
  def batch_create(conn, %{"events" => events}) do
    user_id = conn.assigns.current_user_id

    results =
      Enum.map(events, fn event_params ->
        case EventStore.log_event(user_id, event_params) do
          {:ok, event} -> event_to_json(event)
          _ -> nil
        end
      end)
      |> Enum.reject(&is_nil/1)

    json(conn, %{logged: length(results), status: "ok"})
  end

  # --- Private ---

  defp event_to_json(event) do
    %{
      id: event.id,
      camera_id: event.camera_id,
      camera_name: event.camera_name,
      event_type: event.event_type,
      message: event.message,
      timestamp: event.timestamp
    }
  end

  defp parse_int(nil, default), do: default
  defp parse_int(val, default) when is_binary(val) do
    case Integer.parse(val) do
      {n, _} -> min(n, 200)
      :error -> default
    end
  end
  defp parse_int(val, _default) when is_integer(val), do: min(val, 200)
end
