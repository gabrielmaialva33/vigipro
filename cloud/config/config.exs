# This file is responsible for configuring your application
# and its dependencies with the aid of the Config module.
#
# This configuration file is loaded before any dependency and
# is restricted to this project.

# General application configuration
import Config

config :vigipro_cloud,
  generators: [timestamp_type: :utc_datetime]

# Configure the endpoint
config :vigipro_cloud, VigiproCloudWeb.Endpoint,
  url: [host: "localhost"],
  adapter: Bandit.PhoenixAdapter,
  render_errors: [
    formats: [html: VigiproCloudWeb.ErrorHTML, json: VigiproCloudWeb.ErrorJSON],
    layout: false
  ],
  pubsub_server: VigiproCloud.PubSub,
  live_view: [signing_salt: "W4DZ6j+Q"]

# Configure esbuild (the version is required)
config :esbuild,
  version: "0.25.4",
  vigipro_cloud: [
    args:
      ~w(js/app.js --bundle --target=es2022 --outdir=../priv/static/assets/js --external:/fonts/* --external:/images/* --alias:@=.),
    cd: Path.expand("../assets", __DIR__),
    env: %{"NODE_PATH" => [Path.expand("../deps", __DIR__), Mix.Project.build_path()]}
  ]

# Configure tailwind (the version is required)
config :tailwind,
  version: "4.1.12",
  vigipro_cloud: [
    args: ~w(
      --input=assets/css/app.css
      --output=priv/static/assets/css/app.css
    ),
    cd: Path.expand("..", __DIR__)
  ]

# Configure Elixir's Logger
config :logger, :default_formatter,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

# Oban (background jobs)
config :vigipro_cloud, Oban,
  engine: Oban.Engines.Lite,
  queues: [default: 10, recordings: 5],
  repo: nil

# Cloudflare R2 Storage
config :vigipro_cloud,
  r2_bucket: "vigipro",
  r2_endpoint: "5e169ace5c37c07688d84589e2ee87b0.r2.cloudflarestorage.com",
  r2_public_url: "https://pub-9bd1fe2f8d4844e99cdd166adaee7000.r2.dev"

# Supabase Auth
config :vigipro_cloud,
  supabase_jwt_secret: System.get_env("SUPABASE_JWT_SECRET", "dev-jwt-secret-change-me"),
  ephemeral_token_secret: System.get_env("EPHEMERAL_TOKEN_SECRET", "dev-ephemeral-secret")

# ExAws (R2 via S3 API)
config :ex_aws,
  json_codec: Jason,
  access_key_id: {:system, "R2_ACCESS_KEY_ID"},
  secret_access_key: {:system, "R2_SECRET_ACCESS_KEY"}

config :ex_aws, :s3,
  scheme: "https://",
  host: "5e169ace5c37c07688d84589e2ee87b0.r2.cloudflarestorage.com",
  region: "auto"

# Use Jason for JSON parsing in Phoenix
config :phoenix, :json_library, Jason

# Import environment specific config. This must remain at the bottom
# of this file so it overrides the configuration defined above.
import_config "#{config_env()}.exs"
