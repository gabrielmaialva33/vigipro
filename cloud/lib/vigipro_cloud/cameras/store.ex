defmodule VigiproCloud.Cameras.Store do
  @moduledoc """
  ETS-based camera metadata store.
  Persists camera info registered by Android apps.
  Scoped by user_id — each user sees only their cameras.

  Note: Data lives in memory only. Android app re-syncs on connect.
  Phase 2 will add Ecto persistence.
  """
  use GenServer

  require Logger

  @table :vigipro_cameras

  # --- Public API ---

  def start_link(_opts) do
    GenServer.start_link(__MODULE__, [], name: __MODULE__)
  end

  @doc "Upsert a camera for a user. Returns :ok."
  def put_camera(user_id, %{} = camera_data) do
    camera = normalize_camera(user_id, camera_data)
    :ets.insert(@table, {camera.id, camera})
    broadcast_change(camera.id, :updated)
    :ok
  end

  @doc "Batch upsert cameras for a user. Removes cameras not in the list."
  def sync_cameras(user_id, cameras) when is_list(cameras) do
    new_ids = MapSet.new(cameras, & &1["id"])

    # Remove cameras that are no longer on the device
    existing_ids = list_camera_ids(user_id)

    Enum.each(existing_ids, fn id ->
      unless MapSet.member?(new_ids, id) do
        delete_camera(id)
      end
    end)

    # Upsert all cameras
    Enum.each(cameras, fn cam ->
      put_camera(user_id, cam)
    end)

    {:ok, MapSet.size(new_ids)}
  end

  @doc "Get a single camera by ID."
  def get_camera(camera_id) do
    case :ets.lookup(@table, camera_id) do
      [{^camera_id, camera}] -> {:ok, camera}
      [] -> {:error, :not_found}
    end
  end

  @doc "List all cameras for a user."
  def list_cameras(user_id) do
    match_spec = [{{:_, %{user_id: user_id}}, [], [:"$_"]}]

    :ets.select(@table, match_spec)
    |> Enum.map(fn {_id, camera} -> camera end)
    |> Enum.sort_by(& &1.sort_order)
  end

  @doc "List camera IDs for a user."
  def list_camera_ids(user_id) do
    list_cameras(user_id) |> Enum.map(& &1.id)
  end

  @doc "Update camera status."
  def update_status(camera_id, status) when status in [:online, :offline, :error] do
    case get_camera(camera_id) do
      {:ok, camera} ->
        updated = %{camera | status: status, updated_at: now()}
        :ets.insert(@table, {camera_id, updated})
        broadcast_change(camera_id, {:status, status})
        :ok

      error ->
        error
    end
  end

  @doc "Delete a camera."
  def delete_camera(camera_id) do
    :ets.delete(@table, camera_id)
    broadcast_change(camera_id, :deleted)
    :ok
  end

  @doc "Count cameras for a user."
  def count_cameras(user_id) do
    list_cameras(user_id) |> length()
  end

  # --- GenServer Callbacks ---

  @impl true
  def init(_opts) do
    table = :ets.new(@table, [:named_table, :set, :public, read_concurrency: true])
    Logger.info("Camera store started (ETS: #{inspect(table)})")
    {:ok, %{}}
  end

  # --- Private ---

  defp normalize_camera(user_id, data) when is_map(data) do
    %{
      id: data["id"] || generate_uuid(),
      user_id: user_id,
      site_id: data["site_id"],
      name: data["name"] || "Unnamed",
      ptz_capable: data["ptz_capable"] || false,
      audio_capable: data["audio_capable"] || false,
      status: parse_status(data["status"]),
      thumbnail_url: data["thumbnail_url"],
      sort_order: data["sort_order"] || 0,
      onvif_address: data["onvif_address"],
      stream_profile: data["stream_profile"],
      gateway_id: data["gateway_id"],
      updated_at: now(),
      created_at: data["created_at"] || now()
    }
  end

  defp parse_status("ONLINE"), do: :online
  defp parse_status("OFFLINE"), do: :offline
  defp parse_status("ERROR"), do: :error
  defp parse_status(:online), do: :online
  defp parse_status(:offline), do: :offline
  defp parse_status(:error), do: :error
  defp parse_status(_), do: :offline

  defp now, do: DateTime.utc_now() |> DateTime.to_iso8601()

  defp generate_uuid do
    <<a::48, _::4, b::12, _::2, c::62>> = :crypto.strong_rand_bytes(16)

    <<a::48, 4::4, b::12, 2::2, c::62>>
    |> Base.encode16(case: :lower)
    |> then(fn hex ->
      <<a::binary-8, ?-, b::binary-4, ?-, c::binary-4, ?-, d::binary-4, ?-, e::binary-12>> = hex
      "#{a}-#{b}-#{c}-#{d}-#{e}"
    end)
  end

  defp broadcast_change(camera_id, event) do
    Phoenix.PubSub.broadcast(
      VigiproCloud.PubSub,
      "cameras",
      {:camera_change, camera_id, event}
    )
  end
end
