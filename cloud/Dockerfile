# ---- Build Stage ----
FROM hexpm/elixir:1.17.3-erlang-27.2-debian-bookworm-20241202-slim AS builder

RUN apt-get update -y && apt-get install -y \
    build-essential git curl \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Instala Node.js para assets
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN mix local.hex --force && mix local.rebar --force

ENV MIX_ENV=prod

COPY mix.exs mix.lock ./
RUN mix deps.get --only $MIX_ENV

COPY config/config.exs config/prod.exs config/runtime.exs config/
RUN mix deps.compile

COPY assets assets
COPY priv priv
COPY lib lib

RUN mix assets.deploy
RUN mix compile
RUN mix release

# ---- App Stage ----
FROM debian:bookworm-slim AS app

RUN apt-get update -y && apt-get install -y \
    libstdc++6 openssl libncurses5 locales curl \
    ffmpeg \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && locale-gen
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8

RUN mkdir -p /tmp/vigipro_hls && chmod 777 /tmp/vigipro_hls

WORKDIR /app
RUN chown nobody /app

COPY --from=builder --chown=nobody:root /app/_build/prod/rel/vigipro_cloud ./

USER nobody

ENV HOME=/app PHX_SERVER=true

EXPOSE 4000

CMD ["bin/vigipro_cloud", "start"]
