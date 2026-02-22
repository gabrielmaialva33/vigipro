defmodule VigiproCloud.Events.EventStore do
  @moduledoc """
  ETS-based event store for camera events.
  Stores recent events in memory, auto-prunes old entries.
  Phase 2 will add Ecto persistence + Oban cleanup.
  """
  use GenServer

  require Logger

  @table :vigipro_events
  @max_events_per_camera 100
  @prune_interval_ms :timer.minutes(5)

  # --- Public API ---

  def start_link(_opts) do
    GenServer.start_link(__MODULE__, [], name: __MODULE__)
  end

  @doc "Log a camera event."
  def log_event(user_id, %{} = event_data) do
    event = %{
      id: System.unique_integer([:positive, :monotonic]),
      user_id: user_id,
      camera_id: event_data["camera_id"],
      camera_name: event_data["camera_name"],
      event_type: event_data["event_type"],
      message: event_data["message"],
      timestamp: event_data["timestamp"] || now_ms()
    }

    :ets.insert(@table, {event.id, event})

    Phoenix.PubSub.broadcast(
      VigiproCloud.PubSub,
      "events:#{user_id}",
      {:new_event, event}
    )

    {:ok, event}
  end

  @doc "Get recent events for a user."
  def list_events(user_id, opts \\ []) do
    limit = Keyword.get(opts, :limit, 50)
    camera_id = Keyword.get(opts, :camera_id)

    match_spec =
      if camera_id do
        [{{:_, %{user_id: user_id, camera_id: camera_id}}, [], [:"$_"]}]
      else
        [{{:_, %{user_id: user_id}}, [], [:"$_"]}]
      end

    :ets.select(@table, match_spec)
    |> Enum.map(fn {_id, event} -> event end)
    |> Enum.sort_by(& &1.timestamp, :desc)
    |> Enum.take(limit)
  end

  # --- GenServer Callbacks ---

  @impl true
  def init(_opts) do
    :ets.new(@table, [:named_table, :set, :public, read_concurrency: true])
    schedule_prune()
    Logger.info("Event store started")
    {:ok, %{}}
  end

  @impl true
  def handle_info(:prune, state) do
    prune_old_events()
    schedule_prune()
    {:noreply, state}
  end

  # --- Private ---

  defp schedule_prune do
    Process.send_after(self(), :prune, @prune_interval_ms)
  end

  defp prune_old_events do
    # Group by camera_id, keep only @max_events_per_camera per camera
    all_events =
      :ets.tab2list(@table)
      |> Enum.map(fn {_id, event} -> event end)

    events_by_camera = Enum.group_by(all_events, & &1.camera_id)

    Enum.each(events_by_camera, fn {_camera_id, events} ->
      if length(events) > @max_events_per_camera do
        events
        |> Enum.sort_by(& &1.timestamp, :desc)
        |> Enum.drop(@max_events_per_camera)
        |> Enum.each(fn event ->
          :ets.delete(@table, event.id)
        end)
      end
    end)
  end

  defp now_ms, do: System.system_time(:millisecond)
end
