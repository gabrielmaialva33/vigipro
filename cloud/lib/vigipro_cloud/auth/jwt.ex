defmodule VigiproCloud.Auth.JWT do
  @moduledoc """
  Supabase JWT validation.
  Verifies signature and claims (exp, iss, sub).
  """

  @doc """
  Verifies and decodes a Supabase JWT token.
  Returns {:ok, claims} or {:error, reason}.
  """
  def verify_token(token) do
    secret = supabase_jwt_secret()

    case JOSE.JWT.verify_strict(jwk(secret), ["HS256"], token) do
      {true, %JOSE.JWT{fields: claims}, _jws} ->
        validate_claims(claims)

      {false, _, _} ->
        {:error, :invalid_signature}
    end
  rescue
    _ -> {:error, :malformed_token}
  end

  @doc """
  Generates an ephemeral JWT token for the public landing page.
  Valid for 5 minutes, no user — anti-hotlinking only.
  """
  def generate_ephemeral_token do
    secret = Application.get_env(:vigipro_cloud, :ephemeral_token_secret, "dev-ephemeral-secret")
    now = System.os_time(:second)

    claims = %{
      "iss" => "vigipro_cloud",
      "purpose" => "demo_stream",
      "iat" => now,
      "exp" => now + 300
    }

    JOSE.JWT.sign(jwk(secret), %{"alg" => "HS256"}, claims)
    |> JOSE.JWS.compact()
    |> elem(1)
  end

  @doc "Verifies an ephemeral landing page token."
  def verify_ephemeral_token(token) do
    secret = Application.get_env(:vigipro_cloud, :ephemeral_token_secret, "dev-ephemeral-secret")

    case JOSE.JWT.verify_strict(jwk(secret), ["HS256"], token) do
      {true, %JOSE.JWT{fields: %{"purpose" => "demo_stream", "exp" => exp}}, _} ->
        if exp > System.os_time(:second), do: :ok, else: {:error, :expired}

      _ ->
        {:error, :invalid_token}
    end
  rescue
    _ -> {:error, :malformed_token}
  end

  # --- Private ---

  defp jwk(secret), do: JOSE.JWK.from_oct(secret)

  defp supabase_jwt_secret do
    Application.get_env(:vigipro_cloud, :supabase_jwt_secret) ||
      raise "Missing :supabase_jwt_secret config"
  end

  defp validate_claims(%{"exp" => exp, "sub" => sub}) when is_binary(sub) do
    if exp > System.os_time(:second) do
      {:ok, %{sub: sub, exp: exp}}
    else
      {:error, :expired}
    end
  end

  defp validate_claims(_), do: {:error, :missing_claims}
end
