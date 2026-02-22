defmodule VigiproCloudWeb.GatewaySocket do
  @moduledoc """
  Socket for gateway connections.
  Authentication via Supabase JWT passed in connection params.
  """
  use Phoenix.Socket

  channel "gateway:*", VigiproCloudWeb.GatewayChannel

  @impl true
  def connect(%{"token" => token}, socket, _connect_info) do
    case VigiproCloud.Auth.JWT.verify_token(token) do
      {:ok, claims} ->
        socket = assign(socket, :user_id, claims.sub)
        {:ok, socket}

      {:error, _reason} ->
        :error
    end
  end

  def connect(_params, _socket, _connect_info), do: :error

  @impl true
  def id(socket), do: "gateway:#{socket.assigns.user_id}"
end
