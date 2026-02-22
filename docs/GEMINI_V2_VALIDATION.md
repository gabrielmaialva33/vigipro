# Validacao Gemini 3 Pro — Arquitetura v2 (2026-02-22)

> Modelo: gemini-3-pro-preview | 10 perguntas tecnicas | Respostas diretas e criticas

---

## Veredicto Geral

"O desenho geral esta excelente e o uso de Elixir/Membrane para esse caso de uso brilha muito. No entanto, o requisito novo da Landing Page Publica introduz vetores de falha massivos se aplicarmos as mesmas regras do dashboard privado."

---

## 1. Landing Page Publica (Arquitetura para Anonimos)

**Pipeline por viewer eh suicidio de recursos.** Mesmo com EPYC e 31GB, instanciar pipeline Membrane completo (parser H264 -> WebRTC endpoint) por visitante vai derreter o servidor.

**Solucao**: Topologia **SFU (Selective Forwarding Unit)** obrigatoria. 1 pipeline rodando continuamente (ou sob demanda no primeiro acesso) que ingere o RTSP/SRT, faz o parse (1 vez) e faz o fan-out (replica pacotes RTP) para multiplos peers conectados.

## 2. MediaMTX vs Membrane Direto para Demo

**Use MediaMTX como proxy local no servidor.**

O `membrane_rtsp_plugin` eh elegante, mas o mundo real dos RTSPs publicos eh um inferno (pacotes corrompidos, drops constantes, formatos bizarros). MediaMTX eh escrito em Go, focado nisso e extremamente resiliente (um verdadeiro "tank").

**Fluxo**: MediaMTX engole o lixo da internet → repassa stream limpo (via RTMP ou RTSP local) → pipeline Membrane. Reduz a friccao absurdamente na fase de validacao.

## 3. R2 Integration (Upload Gateway → R2)

**Estrategia de pre-signed URLs esta perfeita.** Tira processamento de disco, RAM e banda do servidor Elixir.

**Gotchas do R2:**
1. Pre-signed URLs exigem **AWS Signature V4**. Usar `ex_aws_s3` no backend apontando para `<account_id>.r2.cloudflarestorage.com`
2. R2 suporta Multipart Upload, mas **feche chunks de MP4/TS a cada X segundos (ex: 5MB)** e faca PUT HTTP simples e atomico. Evite complexidade de multipart no gateway local.

## 4. Escala da Demo (1000 visitantes: WebRTC vs LL-HLS)

**LL-HLS 100% do tempo.**

WebRTC para 1000 viewers eh loucura economica e tecnica: 1000 negociacoes ICE (processador) e 1000 streams independentes batendo na placa de rede (2Mbps * 1000 = 2Gbps continuos, estourando banda).

**Solucao**: Membrane gera playlist e chunks uma unica vez, joga atras da CDN Cloudflare (CF Tunnel + R2 Publico), CDN faz fan-out de graca. Latencia sobe para ~3 segundos, mas visitantes de landing page sao passivos, nao precisam de latencia sub-500ms para PTZ.

## 5. Coturn vs rel (TURN em Elixir puro)

**Va de Coturn.** NAT traversal em redes corporativas/moveis 4G e firewalls bizarros nao eh o lugar para ser purista da linguagem. Coturn eh escrito em C, gasta quase nada de RAM, testado em batalha globalmente, resolve edge cases que o `rel` ainda nem descobriu que existem.

**Deixe o Elixir cuidando das regras de negocio e streaming.**

## 6. Phoenix LiveView vs SPA

**LiveView puro.** Ja tem client pesado (app Android). Adicionar SPA (React/Next) obriga a:
- Criar, documentar e manter APIs REST/GraphQL estritas para consumo proprio
- Gerenciar estado duplicado (server vs client)
- Lidar com JWTs no browser

Com LiveView: processos do dashboard conectam direto com Registry das cameras, PubSub envia pro DOM com zero overhead, autenticacao do Supabase fica segura via sessions HTTP-Only no Plug.

## 7. Modulo de Demo separado (VigiPro.Demo)

**Sim, absolutamente.** O `VigiPro.Streaming` lida com autenticacao granular (RBAC), pipelines baseados em Gateways autenticados e auditoria de eventos. A demonstracao publica nao segue nenhuma dessas regras: puxa de fontes inseguras, expoe para anonimos, e deve cuspir LL-HLS em vez de WebRTC para escalar.

**Criar contexto isolado** `VigiPro.Demo` ou `VigiPro.PublicStreams` para evitar que logica insegura de demonstracao contamine infraestrutura core.

## 8. ONVIF Discovery na Cloud (Para Demo)

**Nao existe ONVIF Discovery na demo.** Cameras publicas estao em redes alheias. Fazer **bypass do Gateway**, configurando "Mock Cameras" no banco de dados da Demo onde `stream_url` eh estatico (RTSP publico ou MediaMTX). Servidor instancia Membrane usando Source RTSP direto, ignorando camada de ingestao SRT.

## 9. Seguranca na Landing Page Publica

**ALERTA VERMELHO.** Se expor endpoint WHEP (WebRTC) aberto, basta script curl fazendo requisicoes de ofertas SDP repetidas para dar CPU exhaustion (DDoS).

**Protecoes necessarias:**
1. **Rate Limiting**: `Plug.Attack` ou `Hammer` para limitar requisicoes por IP
2. **Tokens Efemeros**: LiveView da landing page assina Token JWT de tempo curto (5min) que client JS usa para negociar stream. Impede "hotlinking" (outros sites roubarem stream)
3. **HLS + Cloudflare WAF**: Se usar HLS, Cloudflare bloqueia ataques volumetricos nativamente

## 10. O que esta faltando na v2? (3 Gaps Criticos)

### Gap 1: Lifecycle de Dados (Retention Policy)
Gateway sobe videos infinitamente pro R2. **Faltou desenhar job no Oban** (`VigiPro.Workers.PruneRecordings`) que roda de madrugada, limpa metadados velhos do PostgreSQL e manda DELETE para R2 com base na politica de retencao (7 dias, 30 dias). Sem isso, conta vai quebrar.

### Gap 2: SFU / Multiplexing Explicito
Documentacao diz "Pipeline criado on-demand" mas nao diz o que acontece se Usuario A e B abrirem camera X ao mesmo tempo. **Gateway NAO pode mandar 2 fluxos SRT.** Servidor precisa atuar como SFU: recebe 1 SRT do Gateway, faz Broadcast local no Node Elixir para N canais WebRTC egress.

### Gap 3: Telemetria do Edge (Gateway)
Gateway rodando em RPi de cliente pode fritar CPU ou perder conexao. **Falta canal de heartbeats** (Phoenix.Tracker ou PubSub) onde Gateway envia CPU load, RAM, uptime do RTSP pro servidor. Dashboard deve mostrar "Gateway Offline" ou "Camera Engasgando por Falta de CPU na Rede Local".

---

*Validacao realizada em 2026-02-22 por Gemini 3 Pro (gemini-3-pro-preview)*
