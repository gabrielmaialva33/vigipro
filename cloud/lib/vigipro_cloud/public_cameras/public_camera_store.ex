defmodule VigiproCloud.PublicCameras.PublicCameraStore do
  @moduledoc """
  ETS-backed store for the public camera catalog.
  Loads curated cameras from `Catalog` at startup and serves
  filtered/paginated queries.
  """
  use GenServer

  require Logger

  alias VigiproCloud.PublicCameras.Catalog
  alias VigiproCloud.Streaming.StreamSupervisor

  @table :vigipro_public_cameras

  # --- Public API ---

  def start_link(opts) do
    GenServer.start_link(__MODULE__, opts, name: __MODULE__)
  end

  @doc "List cameras with optional filtering and pagination."
  def list_cameras(opts \\ []) do
    GenServer.call(__MODULE__, {:list, opts})
  end

  @doc "Get a single camera by ID."
  def get_camera(id) do
    GenServer.call(__MODULE__, {:get, id})
  end

  @doc "List available categories with counts."
  def list_categories do
    GenServer.call(__MODULE__, :categories)
  end

  # --- Callbacks ---

  @impl true
  def init(_opts) do
    table = :ets.new(@table, [:set, :named_table, :protected, read_concurrency: true])

    cameras = Catalog.all()

    for cam <- cameras do
      :ets.insert(table, {cam.id, cam})
    end

    # Start FFmpeg streams for backend-managed cameras (lavfi/mjpeg)
    backend_managed =
      Enum.filter(cameras, fn cam ->
        String.starts_with?(cam.stream_url, "lavfi:") or
          String.starts_with?(cam.stream_url, "mjpeg:")
      end)

    for cam <- backend_managed do
      StreamSupervisor.start_stream(cam.id, cam.stream_url)
    end

    Logger.info(
      "PublicCameraStore loaded #{length(cameras)} cameras, " <>
        "started #{length(backend_managed)} backend streams"
    )

    {:ok, %{table: table, cameras: cameras}}
  end

  @impl true
  def handle_call({:list, opts}, _from, state) do
    category = Keyword.get(opts, :category)
    page = Keyword.get(opts, :page, 1)
    page_size = Keyword.get(opts, :page_size, 20)

    filtered =
      state.cameras
      |> maybe_filter_category(category)
      |> Enum.sort_by(&{&1.sort_order, &1.name})

    total = length(filtered)
    total_pages = max(ceil(total / page_size), 1)
    offset = (page - 1) * page_size

    cameras = Enum.slice(filtered, offset, page_size)

    meta = %{
      page: page,
      page_size: page_size,
      total: total,
      total_pages: total_pages
    }

    {:reply, {cameras, meta}, state}
  end

  def handle_call({:get, id}, _from, state) do
    camera = Enum.find(state.cameras, &(&1.id == id))
    {:reply, camera, state}
  end

  def handle_call(:categories, _from, state) do
    {:reply, Catalog.categories(), state}
  end

  # --- Private ---

  defp maybe_filter_category(cameras, nil), do: cameras
  defp maybe_filter_category(cameras, ""), do: cameras

  defp maybe_filter_category(cameras, category) do
    Enum.filter(cameras, &(&1.category == category))
  end
end
