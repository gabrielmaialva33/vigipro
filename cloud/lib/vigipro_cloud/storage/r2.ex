defmodule VigiproCloud.Storage.R2 do
  @moduledoc """
  Cloudflare R2 integration via AWS S3 API.
  Generates pre-signed URLs for direct Gateway uploads.
  """

  @doc """
  Generates a pre-signed URL for uploading (PUT) a video chunk.
  The Gateway uses this to upload directly to R2 without going through the server.
  """
  def presigned_upload_url(key, opts \\ []) do
    expires_in = Keyword.get(opts, :expires_in, 3600)
    content_type = Keyword.get(opts, :content_type, "video/mp4")

    config = r2_config()

    {:ok, url} =
      ExAws.S3.presigned_url(
        config,
        :put,
        bucket(),
        key,
        expires_in: expires_in,
        headers: [{"content-type", content_type}]
      )

    url
  end

  @doc """
  Generates a pre-signed URL for downloading (GET) a chunk/recording.
  """
  def presigned_download_url(key, opts \\ []) do
    expires_in = Keyword.get(opts, :expires_in, 3600)
    config = r2_config()

    {:ok, url} =
      ExAws.S3.presigned_url(
        config,
        :get,
        bucket(),
        key,
        expires_in: expires_in
      )

    url
  end

  @doc "Deletes an object from R2 (used by PruneRecordings worker)."
  def delete_object(key) do
    ExAws.S3.delete_object(bucket(), key)
    |> ExAws.request(r2_config_opts())
  end

  @doc "Lists objects with a prefix (e.g. 'recordings/site-123/')."
  def list_objects(prefix, opts \\ []) do
    max_keys = Keyword.get(opts, :max_keys, 1000)

    ExAws.S3.list_objects(bucket(), prefix: prefix, max_keys: max_keys)
    |> ExAws.request(r2_config_opts())
  end

  # --- Private ---

  defp bucket do
    Application.get_env(:vigipro_cloud, :r2_bucket, "vigipro")
  end

  defp r2_config do
    ExAws.Config.new(:s3, r2_config_opts())
  end

  defp r2_config_opts do
    [
      access_key_id: Application.get_env(:vigipro_cloud, :r2_access_key_id),
      secret_access_key: Application.get_env(:vigipro_cloud, :r2_secret_access_key),
      host: Application.get_env(:vigipro_cloud, :r2_endpoint),
      region: "auto",
      scheme: "https://"
    ]
  end
end
