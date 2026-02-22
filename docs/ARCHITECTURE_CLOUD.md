# VigiPro Cloud — Arquitetura v3

> v1 → v2: Corrigidas 3 falhas mortais (Gemini, 2026-02-22)
> v2 → v3: +Landing Page Publica, +SFU, +MediaMTX, +Seguranca, +Telemetria (Gemini, 2026-02-22)

---

## Visao Geral

```
┌─────────────────────────────────────┐
│         REDE LOCAL (LAN)            │
│                                     │
│  [Camera IP] ──RTSP──► [Gateway]   │
│  [Camera IP] ──RTSP──►  (Elixir)  │
│  [Camera IP] ──ONVIF──►    │       │
│                            │       │
│  [Gateway Telemetria]──────┤       │
│   CPU/RAM/uptime ──────────┘       │
└────────────────────────────┼───────┘
                             │
              SRT (media) + WebSocket (signaling + telemetria)
                             │
              ┌──────────────┼──────────────┐
              │    SERVIDOR CLOUD            │
              │    72.60.9.175               │
              │    8 vCPU / 31GB RAM         │
              │                              │
              │  ┌─────────────────────┐     │
              │  │ MediaMTX (Go)       │     │  ← RTSP publicos → RTSP local limpo
              │  │ Proxy/Normalizer    │     │
              │  └────────┬────────────┘     │
              │           │                  │
              │  ┌────────▼────────────┐     │
              │  │ Phoenix 1.7         │◄────┼── CF Tunnel (API/WS only)
              │  │ + Supabase JWT Auth │     │    vigipro.mahina.cloud
              │  └────────┬────────────┘     │
              │           │                  │
              │  ┌────────▼────────────┐     │
              │  │ Membrane Pipelines  │     │
              │  │ SRT/RTSP → SFU     │     │  ← 1 ingress, N egress (fan-out)
              │  │ WebRTC (privado)    │     │
              │  │ LL-HLS (publico)    │     │
              │  └────────┬────────────┘     │
              │           │                  │
              │  ┌────────▼────────────┐     │
              │  │ Coturn STUN/TURN    │◄────┼── UDP :3478, :50000-60000
              │  └─────────────────────┘     │
              │                              │
              │  ┌─────────────────────┐     │
              │  │ Oban Workers        │     │
              │  │ PruneRecordings     │     │  ← Lifecycle R2 (7d/30d retention)
              │  └─────────────────────┘     │
              └──────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │   ARMAZENAMENTO              │
              │                              │
              │  Cloudflare R2               │
              │  bucket: vigipro             │
              │  public: pub-9bd1...r2.dev   │
              │  LL-HLS chunks (CDN)         │
              │  Gravacoes (pre-signed PUT)  │
              └──────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │         CLIENTES             │
              │                              │
              │  [Landing Page]  → LL-HLS via CDN (anonimo)
              │  [LiveView]      → WebRTC (autenticado)
              │  [App Android]   → WebRTC via WHEP (autenticado)
              └──────────────────────────────┘
```

---

## Decisoes Arquiteturais

### 1. Ingress: Local Gateway (NAO Android Bridge)

**Problema**: Android Doze Mode mata processos background.

**Solucao**: CLI leve em Elixir rodando em qualquer Linux na LAN.

```elixir
# vigipro_gateway (escript ou release Burrito)
# - Descobre cameras via ONVIF
# - Conecta no Phoenix Channel (via CF Tunnel)
# - Puxa RTSP on-demand e envia via SRT pro servidor
# - Escuta ONVIF Events (motion detection nativo)
# - Encaminha comandos PTZ
# - Envia heartbeats de hardware (CPU, RAM, disk, uptime RTSP)
```

### 2. Rota de Media: SRT Ingress → SFU → WebRTC/LL-HLS Egress

**Problema**: CF Tunnel nao roteia UDP. Pipeline por viewer mata o servidor.

**Solucao**: SFU (Selective Forwarding Unit) — 1 ingress, N egress.

| Trecho | Protocolo | Via |
|--------|-----------|-----|
| Gateway → Servidor | SRT (UDP) | IP direto, porta dedicada |
| Servidor → Cliente Privado | WebRTC (UDP) | Coturn STUN/TURN |
| Servidor → Landing Page | LL-HLS | CDN Cloudflare (R2 publico) |
| Signaling (SDP/ICE) | WebSocket | CF Tunnel (HTTPS) |
| API REST | HTTP | CF Tunnel (HTTPS) |

**SFU explicito**: Servidor recebe 1 SRT do Gateway, faz parse H264 1 vez, replica pacotes RTP para N canais WebRTC egress. Gateway NUNCA manda 2 fluxos da mesma camera.

### 3. Demo Publica: MediaMTX + LL-HLS + CDN

**Problema**: RTSPs publicos sao um inferno (drops, formatos bizarros). WebRTC nao escala para 1000+ viewers anonimos.

**Solucao em 3 camadas**:

```
[RTSP Publico/MediaMTX Loop] → MediaMTX (normaliza) → Membrane (LL-HLS) → R2/CDN → Browser
```

1. **MediaMTX** (Go): Proxy resiliente que engole RTSP sujo e repassa stream limpo
2. **Membrane**: Gera playlist LL-HLS + chunks uma unica vez
3. **Cloudflare CDN**: Fan-out gratis para qualquer numero de viewers (~3s latencia)

**Por que nao WebRTC na demo**: 1000 viewers = 1000 ICE negotiations + 2Gbps banda. LL-HLS com CDN = custo zero de escala.

### 4. IA: ONVIF Events no MVP (NAO YOLO)

**Problema**: YOLO consome 1-2 vCPUs por camera.

**Solucao MVP**: Cameras profissionais ja tem motion detection nativo via ONVIF Events.

```
Camera (ONVIF Event: motion) → Gateway → Phoenix Channel → Push notification
```

### 5. Gravacao: Upload Direto pro R2

**Estrategia validada pelo Gemini.**

```
1. Servidor gera URL pre-assinada S3 (AWS Sig V4 via ex_aws_s3)
   → Endpoint: 5e169ace5c37c07688d84589e2ee87b0.r2.cloudflarestorage.com
2. Envia pro Gateway via Phoenix Channel
3. Gateway fecha chunks MP4/TS de ~5MB → PUT HTTP atomico pro R2
   (NAO usar multipart upload — simplicidade no gateway)
4. Servidor registra metadata no PostgreSQL
```

### 6. Lifecycle de Dados (Retention Policy) — NOVO v3

**Gap critico identificado pelo Gemini.**

```elixir
# VigiPro.Workers.PruneRecordings (Oban)
# - Roda diariamente de madrugada (cron: "0 3 * * *")
# - Consulta PostgreSQL por gravacoes expiradas (> retention_days do site)
# - DELETE no R2 via ex_aws_s3
# - Remove metadata do PostgreSQL
# - Politicas: 7d (free), 30d (pro), 90d (enterprise)
```

### 7. Telemetria do Edge (Gateway) — NOVO v3

**Gap critico: Gateway em RPi pode fritar sem aviso.**

```elixir
# Gateway envia heartbeats a cada 30s via Phoenix Channel:
# %{
#   cpu_percent: 45.2,
#   ram_mb_used: 512,
#   ram_mb_total: 1024,
#   disk_percent: 78.5,
#   rtsp_streams_active: 3,
#   uptime_seconds: 86400,
#   bandwidth_mbps: 12.3
# }
#
# Dashboard mostra:
# - "Gateway Online" (verde) / "Gateway Offline" (vermelho)
# - "Camera Engasgando por Falta de CPU na Rede Local" (amarelo)
```

---

## Modulos Elixir (v3 Refinados)

### `VigiPro.Cameras`
```elixir
# DynamicSupervisor + Registry
# Cada camera = 1 GenServer
# Estado: online/offline, ultimo evento, viewers ativos, metricas SFU
# Recebe heartbeats do Gateway via Phoenix Channel
```

### `VigiPro.Gateway`
```elixir
# Phoenix Channel handler para gateways
# Autentica via Supabase JWT
# Registra cameras disponiveis na LAN
# Encaminha comandos PTZ
# Recebe ONVIF Events e emite alertas
# Processa heartbeats de telemetria (CPU/RAM/disk/bandwidth)
# Detecta "Gateway Offline" via Phoenix.Presence timeout
```

### `VigiPro.Streaming`
```elixir
# Membrane Pipeline por camera ativa (SFU topology)
# 1 SRT Source → H264 Parser → Fan-out para N WebRTC Sinks
# Pipeline criado on-demand, destruido quando zero viewers
# Rate limit: max 50 viewers simultaneos por camera (configurable)
# Auth: Supabase JWT obrigatorio para WebRTC
```

### `VigiPro.Demo` — NOVO v3
```elixir
# Contexto ISOLADO da infraestrutura core
# Puxa RTSP de cameras publicas via MediaMTX (proxy local)
# "Mock Cameras" no banco com stream_url estatico
# Membrane RTSP Source direto (bypass Gateway/SRT)
# Gera LL-HLS → upload pro R2 publico → CDN serve viewers anonimos
# Zero auth, zero RBAC, zero auditoria
# Rate limiting via Plug.Attack (por IP)
# Tokens JWT efemeros (5min) para anti-hotlinking
```

### `VigiPro.Events`
```elixir
# Broadway consumer para eventos ONVIF
# Persiste no PostgreSQL (Supabase)
# Emite push notifications via Phoenix PubSub
# Trigger de gravacao (instrui Gateway a fazer upload)
```

### `VigiPro.Workers`
```elixir
# Oban workers:
# - PruneRecordings: limpeza R2 por retention policy (diario)
# - GatewayHealthCheck: alerta admin se gateway offline > 5min
# - DemoStreamHealth: restart MediaMTX/pipeline se demo stream cair
```

### `VigiPro.Web`
```elixir
# Phoenix 1.7 + LiveView (SEM SPA)
# Landing page publica: LL-HLS player, zero auth
# REST API para app Android (cameras, sites, eventos)
# LiveView dashboard privado: WebRTC player, full RBAC
# WHEP endpoint para streaming WebRTC (autenticado)
# Supabase JWT validation via Plug
# Plug.Attack rate limiting
# Phoenix.Presence para contagem de viewers + gateway status
```

---

## Stack Tecnica (Versoes Confirmadas)

```elixir
# mix.exs
defp deps do
  [
    # Web
    {:phoenix, "~> 1.7"},
    {:phoenix_live_view, "~> 1.0"},
    {:bandit, "~> 1.5"},

    # WebRTC (Software Mansion)
    {:ex_webrtc, "~> 0.2.0"},

    # Membrane (streaming pipelines)
    {:membrane_core, "~> 1.2"},
    {:membrane_srt_plugin, "~> 0.1.1"},
    {:membrane_rtsp_plugin, "~> 0.7"},
    {:membrane_h26x_plugin, "~> 0.10"},
    {:membrane_webrtc_plugin, "~> 0.22"},
    {:membrane_http_adaptive_stream_plugin},    # LL-HLS

    # Background jobs
    {:oban, "~> 2.18"},

    # Storage (R2)
    {:ex_aws, "~> 2.5"},
    {:ex_aws_s3, "~> 2.5"},
    {:hackney, "~> 1.20"},                     # HTTP client for ex_aws

    # Auth
    {:jose, "~> 1.11"},                        # Supabase JWT validation

    # Security
    {:plug_attack, "~> 0.4"},                  # Rate limiting

    # Telemetry
    {:telemetry, "~> 1.0"},
    {:phoenix_live_dashboard, "~> 0.8"},

    # Serialization
    {:jason, "~> 1.4"},
  ]
end
```

**Infraestrutura adicional (nao Elixir)**:
- **MediaMTX** v1.x (Go): RTSP proxy/normalizer para demo
- **Coturn**: STUN/TURN server (C, apt-get install)

---

## Infraestrutura Real (Configurada)

### Servidor: 72.60.9.175
- 8 vCPU AMD EPYC 9354P, 31GB RAM, 387GB SSD
- Ubuntu 25.04
- Docker disponivel

### Cloudflare R2
- Bucket: `vigipro`
- Endpoint S3: `https://5e169ace5c37c07688d84589e2ee87b0.r2.cloudflarestorage.com`
- R2 Public: `https://pub-9bd1fe2f8d4844e99cdd166adaee7000.r2.dev`
- Credenciais S3 configuradas

### Cloudflare Tunnel
- Dominio: `vigipro.mahina.cloud`
- Tunnel ID: `f71e9ebb-90ca-4cb5-ad39-0f065b39f90d`
- Rota: API REST + WebSocket signaling APENAS

### Portas UDP (Diretas)

| Porta | Protocolo | Servico |
|-------|-----------|---------|
| 3478 | UDP+TCP | Coturn STUN/TURN |
| 5349 | TCP | Coturn TLS |
| 9000 | UDP | SRT Ingress |
| 50000-60000 | UDP | WebRTC Media Relay |

### Coturn Config

```ini
# /etc/turnserver.conf
realm=vigipro.mahina.cloud
server-name=vigipro-turn
fingerprint
lt-cred-mech
use-auth-secret
static-auth-secret=<GENERATED_SECRET>

listening-port=3478
tls-listening-port=5349
listening-ip=0.0.0.0
relay-ip=<PRIVATE_IP>
external-ip=72.60.9.175/<PRIVATE_IP>

min-port=50000
max-port=60000
total-quota=100

# Security
no-stun-backward-compatibility
no-rfc5780
denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=172.16.0.0-172.31.255.255
denied-peer-ip=192.168.0.0-192.168.255.255
```

---

## Landing Page Publica (Estrategia de Demo)

### Fontes de Video

| Fonte | URL | Uso |
|-------|-----|-----|
| RTSP Test Pattern | `rtsp://rtsp.stream/pattern` | Teste funcional |
| RTSP Movie | `rtsp://rtsp.stream/movie` | Demo visual |
| IPVM Demo (ONVIF) | `rtsp://demo:demo@ipvmdemo.dyndns.org:5541/...` | Demo ONVIF |
| MediaMTX Loop | Footage CC local em loop | Demo principal |

### Arquitetura da Demo

```
Browser → vigipro.mahina.cloud (LiveView)
       → LL-HLS player (hls.js)
       → Chunks servidos pelo CDN Cloudflare (R2 publico)
       → Gerados 1x pelo Membrane pipeline
       → Alimentado pelo MediaMTX (RTSP proxy)
       → Puxando cameras publicas ou loops locais
```

### Seguranca da Demo

1. **Plug.Attack**: Rate limiting por IP (max 10 req/s para endpoints de stream)
2. **JWT Efemero**: LiveView gera token de 5min pro player JS negociar stream
3. **Anti-hotlinking**: Token impede embed em sites terceiros
4. **Cloudflare WAF**: Protecao DDoS nativa nos chunks HLS

### Funil de Conversao

```
[Landing com cameras ao vivo] → zero friction, ~3s latencia
         ↓
[Tour interativo do dashboard] → screenshots/video
         ↓
[Criar conta gratis] → 1 site, 2 cameras, 7 dias gravacao
         ↓
[Plano Pro] → ilimitado
```

---

## Roadmap MVP (2 Semanas)

### Semana 1: Fundacao + Demo Publica

| Dia | Tarefa | Entregavel |
|-----|--------|------------|
| 1 | Setup Phoenix 1.7, Supabase JWT auth, Plug.Attack | Projeto rodando com auth |
| 2 | MediaMTX Docker + RTSP test streams | Streams publicos normalizados |
| 3 | Membrane pipeline RTSP → LL-HLS | HLS chunks gerados |
| 4 | R2 upload dos chunks + CDN serving | Landing page com 1 camera ao vivo |
| 5 | LiveView landing page + hls.js player | Demo publica acessivel em vigipro.mahina.cloud |

### Semana 2: Infra Privada + Android

| Dia | Tarefa | Entregavel |
|-----|--------|------------|
| 6 | Phoenix Channels + Gateway auth | Gateway conecta e registra cameras |
| 7 | SRT ingress + Membrane SFU → WebRTC | 1 camera privada via WebRTC |
| 8 | REST API + WHEP endpoint | App Android assiste stream |
| 9 | Coturn + portas UDP + PTZ | WebRTC via 4G + controle PTZ |
| 10 | Oban workers + telemetria Gateway | Deploy completo end-to-end |

### Cortado do MVP (Fase 2+)
- YOLO/IA (usar ONVIF Events nativo no MVP)
- Gravacao S3/R2 completa (upload direto do gateway)
- Multi-site management
- Dashboard LiveView completo
- Planos de assinatura

---

## Comparativo vs Concorrencia

| Feature | Intelbras iVMS | Hikvision Hik-Connect | VigiPro Cloud |
|---------|---------------|----------------------|---------------|
| Demo sem conta | Nao | Nao | **Sim, cameras ao vivo** |
| Acesso remoto | P2P proprietario | P2P proprietario | WebRTC zero-trust |
| Cameras suportadas | So Intelbras | So Hikvision | Qualquer ONVIF/RTSP |
| Dashboard web | Nao | Limitado | LiveView full |
| RBAC | Basico | Basico | 5 roles granulares |
| Alertas push | 3-5s | 3-5s | <1s (Phoenix Channels) |
| Auto-recovery | Nao | Nao | OTP Supervisors |
| Scalability | ~50 cameras | ~64 cameras | 1000+ (BEAM VM) |
| Telemetria Gateway | Nao | Nao | CPU/RAM/bandwidth em tempo real |
| Retention Policy | Manual | Manual | Automatica (Oban + R2) |
| Open source | Nao | Nao | Sim |

---

## Killer Features

1. **Try Before Buy**: Landing page com cameras ao vivo, zero signup
2. **Zero Port Forwarding**: Gateway LAN + CF Tunnel
3. **Camera Agnostica**: Qualquer RTSP/ONVIF
4. **Latencia <500ms**: SRT + SFU WebRTC + Coturn
5. **Auto-Recovery**: OTP Supervisors
6. **Dashboard Instant**: Phoenix LiveView, zero install
7. **RBAC Real**: 5 roles + RLS Supabase
8. **Push <1s**: Phoenix PubSub → Channels
9. **Gravacao Inteligente**: Motion-triggered, R2 direto, retention automatica
10. **Gateway Health**: Telemetria CPU/RAM/banda em tempo real

---

*Documento v3 — Claude Opus 4.6 + Gemini 3 Pro (2x validacoes) — 2026-02-22*
