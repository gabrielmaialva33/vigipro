defmodule VigiproCloud.Auth.JWT do
  @moduledoc """
  Supabase JWT validation.

  Suporta dois modos de assinatura:
  - ES256 (EC P-256): modo moderno do Supabase, configurado via SUPABASE_JWK (JWK JSON)
  - HS256 (HMAC): modo legado, configurado via SUPABASE_JWT_SECRET

  Se SUPABASE_JWK estiver definido, ES256 tem prioridade.
  """

  @doc """
  Verifica e decodifica um token JWT do Supabase.
  Retorna {:ok, claims} ou {:error, reason}.
  """
  def verify_token(token) do
    case supabase_key_mode() do
      {:ec, jwk} ->
        case JOSE.JWT.verify_strict(jwk, ["ES256"], token) do
          {true, %JOSE.JWT{fields: claims}, _jws} -> validate_claims(claims)
          {false, _, _} -> {:error, :invalid_signature}
        end

      {:hmac, secret} ->
        case JOSE.JWT.verify_strict(JOSE.JWK.from_oct(secret), ["HS256"], token) do
          {true, %JOSE.JWT{fields: claims}, _jws} -> validate_claims(claims)
          {false, _, _} -> {:error, :invalid_signature}
        end
    end
  rescue
    _ -> {:error, :malformed_token}
  end

  @doc """
  Gera um token JWT efêmero para o demo público.
  Válido por 5 minutos — apenas anti-hotlinking.
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

    JOSE.JWT.sign(JOSE.JWK.from_oct(secret), %{"alg" => "HS256"}, claims)
    |> JOSE.JWS.compact()
    |> elem(1)
  end

  @doc "Verifica um token efêmero do demo público."
  def verify_ephemeral_token(token) do
    secret = Application.get_env(:vigipro_cloud, :ephemeral_token_secret, "dev-ephemeral-secret")

    case JOSE.JWT.verify_strict(JOSE.JWK.from_oct(secret), ["HS256"], token) do
      {true, %JOSE.JWT{fields: %{"purpose" => "demo_stream", "exp" => exp}}, _} ->
        if exp > System.os_time(:second), do: :ok, else: {:error, :expired}

      _ ->
        {:error, :invalid_token}
    end
  rescue
    _ -> {:error, :malformed_token}
  end

  # --- Private ---

  # Retorna {:ec, jwk} se SUPABASE_JWK estiver configurado (ES256)
  # ou {:hmac, secret} para fallback HS256
  defp supabase_key_mode do
    case Application.get_env(:vigipro_cloud, :supabase_jwk) do
      jwk_json when is_binary(jwk_json) and jwk_json != "" ->
        jwk = jwk_json |> Jason.decode!() |> JOSE.JWK.from_map()
        {:ec, jwk}

      _ ->
        secret =
          Application.get_env(:vigipro_cloud, :supabase_jwt_secret) ||
            raise "Missing :supabase_jwk or :supabase_jwt_secret config"

        {:hmac, secret}
    end
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
