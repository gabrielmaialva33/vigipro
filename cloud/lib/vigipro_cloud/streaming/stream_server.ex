defmodule VigiproCloud.Streaming.StreamServer do
  @moduledoc """
  GenServer managing an FFmpeg process that converts RTSP → HLS segments.
  One StreamServer per camera. Registered in Cameras.Registry as {:stream, camera_id}.
  """
  use GenServer, restart: :transient

  require Logger

  @hls_root "/tmp/vigipro_hls"
  @ffmpeg_path "/usr/bin/ffmpeg"
  @restart_delay_ms 5_000

  defstruct [
    :camera_id,
    :rtsp_url,
    :port,
    :os_pid,
    status: :starting,
    restart_count: 0
  ]

  # --- Public API ---

  def start_link(opts) do
    camera_id = Keyword.fetch!(opts, :camera_id)
    GenServer.start_link(__MODULE__, opts, name: via(camera_id))
  end

  def get_status(camera_id) do
    GenServer.call(via(camera_id), :get_status)
  catch
    :exit, _ -> :not_running
  end

  def hls_dir(camera_id), do: Path.join(@hls_root, camera_id)

  # --- Registry ---

  defp via(camera_id) do
    {:via, Registry, {VigiproCloud.Cameras.Registry, {:stream, camera_id}}}
  end

  # --- Callbacks ---

  @impl true
  def init(opts) do
    camera_id = Keyword.fetch!(opts, :camera_id)
    rtsp_url = Keyword.fetch!(opts, :rtsp_url)

    state = %__MODULE__{
      camera_id: camera_id,
      rtsp_url: rtsp_url
    }

    {:ok, state, {:continue, :start_ffmpeg}}
  end

  @impl true
  def handle_continue(:start_ffmpeg, state) do
    case start_ffmpeg(state.camera_id, state.rtsp_url) do
      {:ok, port, os_pid} ->
        Logger.info("[StreamServer] FFmpeg started for #{state.camera_id} (pid #{os_pid})")

        Phoenix.PubSub.broadcast(
          VigiproCloud.PubSub,
          "streams",
          {:stream_status, state.camera_id, :streaming}
        )

        {:noreply, %{state | port: port, os_pid: os_pid, status: :streaming}}

      {:error, reason} ->
        Logger.error("[StreamServer] Failed to start FFmpeg for #{state.camera_id}: #{reason}")
        schedule_restart(state)
        {:noreply, %{state | status: :error}}
    end
  end

  @impl true
  def handle_info({port, {:data, data}}, %{port: port} = state) do
    line = to_string(data)

    cond do
      String.contains?(line, "Opening") ->
        Logger.debug("[StreamServer] #{state.camera_id}: RTSP connection opened")

      String.contains?(line, "error") or String.contains?(line, "Error") ->
        Logger.warning("[StreamServer] #{state.camera_id}: #{String.trim(line)}")

      true ->
        :ok
    end

    {:noreply, state}
  end

  def handle_info({port, {:exit_status, code}}, %{port: port} = state) do
    Logger.warning("[StreamServer] FFmpeg exited for #{state.camera_id} with code #{code}")

    Phoenix.PubSub.broadcast(
      VigiproCloud.PubSub,
      "streams",
      {:stream_status, state.camera_id, :offline}
    )

    schedule_restart(state)
    {:noreply, %{state | port: nil, os_pid: nil, status: :error}}
  end

  def handle_info(:restart, state) do
    {:noreply, %{state | restart_count: state.restart_count + 1}, {:continue, :start_ffmpeg}}
  end

  def handle_info(_msg, state), do: {:noreply, state}

  @impl true
  def handle_call(:get_status, _from, state) do
    {:reply, state.status, state}
  end

  @impl true
  def terminate(_reason, state) do
    kill_ffmpeg(state)
    :ok
  end

  # --- Private ---

  defp start_ffmpeg(camera_id, rtsp_url) do
    dir = hls_dir(camera_id)
    File.mkdir_p!(dir)

    segment_pattern = Path.join(dir, "seg_%05d.ts")
    playlist = Path.join(dir, "index.m3u8")

    # If stream_url starts with "lavfi:" use direct test source (no RTSP, low CPU)
    # Otherwise, transcode RTSP stream
    args =
      if String.starts_with?(rtsp_url, "lavfi:") do
        lavfi_src = String.replace_prefix(rtsp_url, "lavfi:", "")

        [
          "-hide_banner", "-loglevel", "warning",
          "-re",
          "-f", "lavfi", "-i", lavfi_src,
          "-c:v", "libx264", "-preset", "ultrafast", "-tune", "zerolatency",
          "-g", "30", "-keyint_min", "30",
          "-b:v", "400k", "-maxrate", "400k", "-bufsize", "800k",
          "-pix_fmt", "yuv420p",
          "-an",
          "-f", "hls",
          "-hls_time", "2", "-hls_list_size", "5",
          "-hls_flags", "delete_segments+append_list",
          "-hls_segment_filename", segment_pattern,
          playlist
        ]
      else
        [
          "-hide_banner", "-loglevel", "warning",
          "-rtsp_transport", "tcp",
          "-i", rtsp_url,
          "-c:v", "copy",
          "-an",
          "-f", "hls",
          "-hls_time", "2", "-hls_list_size", "5",
          "-hls_flags", "delete_segments+append_list",
          "-hls_segment_filename", segment_pattern,
          playlist
        ]
      end

    # Use a wrapper script to get the OS PID
    cmd = Enum.join([@ffmpeg_path | args], " ")

    port =
      Port.open({:spawn, cmd}, [
        :binary,
        :exit_status,
        :stderr_to_stdout,
        {:line, 1024}
      ])

    # Get OS PID from port info
    case Port.info(port, :os_pid) do
      {:os_pid, os_pid} -> {:ok, port, os_pid}
      nil -> {:ok, port, nil}
    end
  rescue
    e -> {:error, Exception.message(e)}
  end

  defp kill_ffmpeg(%{port: nil}), do: :ok

  defp kill_ffmpeg(%{port: port, os_pid: os_pid}) do
    try do
      Port.close(port)
    catch
      _, _ -> :ok
    end

    if os_pid do
      System.cmd("kill", ["-9", to_string(os_pid)])
    end
  rescue
    _ -> :ok
  end

  defp schedule_restart(%{restart_count: count}) when count >= 10 do
    Logger.error("[StreamServer] Max restart attempts reached, giving up")
  end

  defp schedule_restart(%{restart_count: count}) do
    delay = @restart_delay_ms * min(count + 1, 6)
    Logger.info("[StreamServer] Scheduling restart in #{delay}ms")
    Process.send_after(self(), :restart, delay)
  end
end
