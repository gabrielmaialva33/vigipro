# VigiPro

Sistema profissional de monitoramento de cameras de seguranca para Android.

Alternativa brasileira moderna ao iSIC Lite, com arquitetura multi-tenant, controle de acesso granular e suporte completo a ONVIF/RTSP.

## Funcionalidades

### Monitoramento
- Grid de cameras com layouts 1x1, 2x2 e 3x3
- Player RTSP profissional com Media3/ExoPlayer
- Controle PTZ (Pan-Tilt-Zoom) via SOAP/ONVIF
- Fullscreen com orientacao automatica
- Captura de snapshot com compartilhamento
- Audio bidirecional
- Monitoramento de status (online/offline/erro)

### Dispositivos
- Descoberta automatica ONVIF (WS-Discovery)
- Configuracao manual de cameras RTSP
- Scan de QR Code para adicionar cameras
- Edicao e remocao de dispositivos

### Multi-Tenant
- Autenticacao via Supabase Auth (email/senha)
- Gerenciamento de sites (locais de monitoramento)
- Convites com QR Code e link compartilhavel
- Papeis: Owner, Admin, Viewer, Time Restricted, Guest
- Deep link: `vigipro.app/invite/{code}`

### Controle de Acesso
- Listagem de membros por site com badge de papel
- Criacao de convites com validade e limite de usos
- Resgate de convites com verificacao automatica
- Remocao de membros (owner/admin only)
- Row Level Security no backend

### Configuracoes
- Informacoes da conta (email + logout)
- Qualidade padrao de stream
- Timeout de conexao
- Tema (claro/escuro/sistema)
- Tamanho do buffer

## Arquitetura

```
┌─────────────────────────────────────────────────┐
│                    app (NavHost)                 │
├──────────┬──────────┬──────────┬────────────────┤
│  auth    │dashboard │ player   │ access-control │
│  devices │ settings │          │                │
├──────────┴──────────┴──────────┴────────────────┤
│              core-ui (Compose + Theme)          │
├─────────────────────────────────────────────────┤
│              core-data (Room + Repos)           │
├──────────────────┬──────────────────────────────┤
│   core-model     │      core-network            │
│   (Domain)       │    (Supabase SDK)            │
└──────────────────┴──────────────────────────────┘
         │                       │
    Local (Room)          Supabase Cloud
    - cameras             - auth.users
    - sites (cache)       - sites (RLS)
                          - site_members (RLS)
                          - invitations (RLS)
```

## Stack Tecnologica

| Camada | Tecnologia |
|--------|-----------|
| UI | Jetpack Compose + Material 3 |
| State | Orbit MVI 9.x |
| DI | Dagger Hilt |
| Navigation | Navigation Compose |
| Video | Media3/ExoPlayer (RTSP) |
| Camera Protocol | ONVIF (WS-Discovery + SOAP) |
| Backend | Supabase (Auth + Postgrest + Realtime) |
| Local DB | Room (v2, migrations) |
| Networking | Ktor + OkHttp |
| Serialization | kotlinx.serialization |
| QR Code | ZXing (gen) + ML Kit (scan) |
| Images | Coil 3 |
| Testing | JUnit + MockK + Turbine + orbit-test |

## Setup

### Pre-requisitos

- Android Studio Ladybug+ (AGP 8.8.2)
- JDK 21
- Android SDK 36

### Supabase

1. Crie um projeto em [supabase.com](https://supabase.com)
2. Configure as credenciais em `core/core-network/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"https://SEU_PROJETO.supabase.co\"")
   buildConfigField("String", "SUPABASE_KEY", "\"SUA_ANON_KEY\"")
   ```
3. Instale e configure o CLI:
   ```bash
   npx supabase login --token SEU_TOKEN
   npx supabase link --project-ref SEU_PROJECT_REF
   npx supabase db push --linked
   ```

### Build

```bash
./gradlew assembleDebug
```

### Testes

```bash
./gradlew test
```

## Estrutura de Modulos

```
vigipro/
├── app/                          # Entry point, navigation
├── core/
│   ├── core-model/               # Site, Camera, SiteMember, Invitation, UserRole
│   ├── core-network/             # SupabaseClient (Auth, Postgrest, Realtime, Storage)
│   ├── core-data/                # Room DB, DAOs, Repositories, Extensions
│   │   ├── db/                   # CameraEntity, SiteEntity, DAOs, Database
│   │   ├── repository/           # Auth, Site, Invitation, Camera repos
│   │   ├── extensions/           # Result, Flow, String extensions
│   │   ├── monitor/              # CameraStatusMonitor
│   │   └── rtsp/                 # RtspConnectionTester
│   └── core-ui/                  # Theme, Dimens, Components, Extensions
│       ├── components/           # CameraCard, RoleBadge, SiteDropdown, etc.
│       ├── theme/                # Material 3 theme + color scheme
│       └── extensions/           # Modifier (shimmer), Context (share, copy)
├── feature/
│   ├── feature-auth/             # LoginScreen, AuthViewModel
│   ├── feature-dashboard/        # DashboardScreen, DashboardViewModel
│   ├── feature-player/           # PlayerScreen, PTZ, Snapshot, Fullscreen
│   ├── feature-devices/          # AddCamera, ONVIF Discovery
│   ├── feature-access-control/   # Members, Invites, QR Code, Redeem
│   └── feature-settings/        # Settings, Account, Logout
├── build-logic/convention/       # Gradle convention plugins
├── supabase/migrations/          # SQL schema + RLS policies
└── gradle/libs.versions.toml     # Dependency catalog
```

## Seguranca

- **RLS (Row Level Security)**: Todas as tabelas do Supabase possuem policies que restringem acesso baseado no `auth.uid()`. Nenhum dado e acessivel sem autenticacao.
- **SECURITY DEFINER**: A funcao `redeem_invitation` roda com permissoes elevadas para permitir criacao de membros independente de RLS do usuario.
- **Credenciais RTSP**: Armazenadas apenas localmente no dispositivo (Room). Nunca sincronizadas com o backend.
- **Anon Key**: A chave anonima do Supabase e publica por design. Seguranca e garantida por RLS no servidor.
- **Token Refresh**: O SDK do Supabase gerencia refresh de tokens automaticamente.

## Licenca

Projeto privado. Todos os direitos reservados.
