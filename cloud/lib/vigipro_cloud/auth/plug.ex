defmodule VigiproCloud.Auth.Plug do
  @moduledoc """
  Authentication plug via Supabase JWT.
  Extracts token from Authorization: Bearer <token> header.
  """
  import Plug.Conn

  @behaviour Plug

  @impl true
  def init(opts), do: opts

  @impl true
  def call(conn, _opts) do
    with ["Bearer " <> token] <- get_req_header(conn, "authorization"),
         {:ok, claims} <- VigiproCloud.Auth.JWT.verify_token(token) do
      conn
      |> assign(:current_user_id, claims.sub)
      |> assign(:jwt_claims, claims)
    else
      _ ->
        conn
        |> put_status(:unauthorized)
        |> Phoenix.Controller.json(%{error: "Invalid or missing token"})
        |> halt()
    end
  end
end
