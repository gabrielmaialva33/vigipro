defmodule VigiproCloud.Streaming.HlsCleaner do
  @moduledoc """
  Periodically cleans up stale HLS segments from /tmp/vigipro_hls/.
  Removes directories not modified in the last 5 minutes.
  """
  use GenServer

  require Logger

  @hls_root "/tmp/vigipro_hls"
  @interval_ms 60_000
  @stale_seconds 300

  def start_link(opts) do
    GenServer.start_link(__MODULE__, opts, name: __MODULE__)
  end

  @impl true
  def init(_opts) do
    schedule_cleanup()
    {:ok, %{}}
  end

  @impl true
  def handle_info(:cleanup, state) do
    cleanup_stale_dirs()
    schedule_cleanup()
    {:noreply, state}
  end

  defp schedule_cleanup do
    Process.send_after(self(), :cleanup, @interval_ms)
  end

  defp cleanup_stale_dirs do
    case File.ls(@hls_root) do
      {:ok, dirs} ->
        now = System.os_time(:second)

        Enum.each(dirs, fn dir ->
          path = Path.join(@hls_root, dir)

          case File.stat(path) do
            {:ok, %{mtime: mtime}} ->
              mtime_unix = :calendar.datetime_to_gregorian_seconds(mtime) - 62_167_219_200
              age = now - mtime_unix

              if age > @stale_seconds do
                Logger.info("[HlsCleaner] Removing stale directory: #{dir}")
                File.rm_rf!(path)
              end

            _ ->
              :ok
          end
        end)

      {:error, :enoent} ->
        :ok
    end
  end
end
