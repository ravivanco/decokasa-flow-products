# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**Decokasa Tracking** — tracking of maritime import orders China → Ecuador for Decokasa S.A.S (Servientrega-style status line: 11 stages per order with traffic lights, checklists, documents, audit trail). Scope is tracking/communication only: no payments, costs or inventory. All UI text, identifiers and domain types are in **Spanish** (`Pedido`, `Etapa`, `Usuario`, `rol`, `semaforo`…); keep that convention in frontend and backend.

Current state: `frontend/` holds a **static demo exported from Google AI Studio** (all data in memory, resets on reload). `backend/` is still empty; the production backend is being built in it.

## Stack

| Layer | Technology |
|---|---|
| Frontend | React 19 + TypeScript + Vite + Tailwind v4, **Node.js 22** |
| Backend | **Java 21 LTS**, Spring Boot 3, Maven (Java 21 enforced via **Maven Toolchains**), JPA + Flyway, JWT auth, permissions enforced server-side |
| Database | **PostgreSQL 17** |
| Runtime | Docker + Docker Compose |
| CI | GitHub Actions `.github/workflows/ci.yml` — job `frontend` runs `npm ci`, `lint`, `build` on PRs/pushes to `main` and `develop` |

Java 21 selection: the developer machine keeps Java 26 as global `JAVA_HOME`; Maven runs on it, but the backend must compile/test/run with JDK 21 through `~/.m2/toolchains.xml` (`version 21`, vendor `temurin`) plus `maven-toolchains-plugin` in `backend/pom.xml` (planned — not created yet). Never "fix" a Java version problem by changing global `JAVA_HOME` or the toolchains file — add/keep the toolchains plugin in the POM.

## Repository layout

```
frontend/     React demo (all frontend paths below are relative to it)
backend/      Spring Boot API (to be scaffolded)
docs/
  prd/          product requirements
  actas/        meeting minutes with the client
  backlog/      DKT stories / sprint notes (Jira is managed manually)
  dominio/      domain model, glossary, business rules
  adr/          architecture decision records (ADR-NNN-titulo.md)
  diagramas/    draw.io / mermaid diagrams
  datos/        data model, DB notes
  api/          openapi.yaml — the API contract (planned, not created yet)
  seguridad/    roles, permission matrix, threat notes
  manuales/     user and deployment manuals
  clienteDocs/  original client material: process docs, real order Excel
                (IMP-2026-006 WPC panels), barcode labels, AI Studio prompts,
                master plan. Reference only, not code.
.github/      PR template + CI workflow
.gitmessage   commit template
```

## Commands

### Frontend (`frontend/`)

```bash
cd frontend
npm install
npm run dev      # Vite on http://localhost:3000
npm run build    # production build to dist/
npm run lint     # type-check only (tsc --noEmit); there is no ESLint
```

No test framework is configured in the frontend yet.

### Backend (`backend/`) — conventions to use once it is scaffolded

```bash
cd backend
./mvnw spring-boot:run                         # API (uses JDK 21 via toolchains)
./mvnw verify                                  # build + all tests
./mvnw test -Dtest=PedidoServiceTest           # one test class
./mvnw test -Dtest=PedidoServiceTest#avanzaEtapa  # one test method
```

Swagger UI (springdoc) is the reference for the API **actually running**: `http://localhost:8080/swagger-ui.html`. It must match `docs/api/openapi.yaml`.

### Docker Compose — conventions to use once `docker-compose.yml` exists at the repo root

```bash
docker compose up -d postgres     # only the database, for local backend dev
docker compose up -d --build      # full stack
docker compose logs -f backend
docker compose down               # keep data; add -v only to wipe the DB volume
```

Configuration comes from a root `.env` (never committed); keep `.env.example` with placeholder values in sync.

## Backend architecture (clean architecture by module)

One package per business module (e.g. `auth`, `usuarios`, `pedidos`, `etapas`, `documentos`, `auditoria`, `productosnuevos`, `bodega`, `reportes`), each split into layers:

```
com.decokasa.tracking.<modulo>.domain          entities, value objects, domain rules, repository ports (interfaces)
com.decokasa.tracking.<modulo>.application     use cases / services, DTOs, transactions
com.decokasa.tracking.<modulo>.infrastructure  JPA entities & repositories, external adapters (Drive, mail, PDF/Excel)
com.decokasa.tracking.<modulo>.web             REST controllers + request/response mapping, matching openapi.yaml
```

Dependency rules:
- `web` → `application` → `domain`.
- `infrastructure` → `application` / `domain` as needed (implements the ports they define).
- `domain` does not depend on Spring or JPA.
- Modules talk to each other through `application` services, never through another module's repositories. Shared cross-cutting code (security, error handling, auditing base, time) goes in a `shared` package.

Business rules that change often (stage deadlines, responsibles, checklists, required documents per stage, role permissions) must be **configurable data in the DB**, not hard-coded.

## API contract-first

- `docs/api/openapi.yaml` (**planned — not created yet**) will be the single source of truth for the API. Create it before the first backend endpoint.
- **Do not implement any endpoint that is not defined in `docs/api/openapi.yaml`.** To add or change an endpoint, update the contract first (in the same branch), then implement.
- Swagger UI shows the real running API; any divergence from the contract is a bug.

## Frontend demo architecture (reference for porting business logic)

- **`src/App.tsx` is the whole app shell**: holds all state (`currentUser`, `pedidos`, `productosNuevos`, `alertas`, toasts, `lang`), does "routing" by switching on `currentModule: AppModule` (no router library), and passes `handle*` callbacks down to views. Modules a role can't access fall back via `canUserAccessModule`.
- **`src/utils/orderState.ts` holds all business logic as pure functions** `(pedido, usuario, …) → { updatedPedido, message }`: advance / return / revert stage, extend fabrication (max +10 business days cumulative), customs substate, pause/cancel/reopen/approve, checklist, observations, file versions. Each mutation appends a `HistorialAuditoria` entry and, when dates are affected, calls `recalculateOrderStages`. This is the reference behavior for the backend `pedidos`/`etapas` use cases.
- **Stage scheduling**: 11 stages in `ETAPAS_BASE_CONFIG` (`src/data/initialData.ts`) with deadlines in business days (Mon–Fri, `src/utils/dateUtils.ts`). Each stage starts when the previous one actually finished (or is estimated to). Stage 2 gets +10 days if `esProductoNuevo`; stage 4 gets `ampliacionFabricacionDias`. `etapas[]` is 0-indexed, `etapaActualNumero` is 1-based.
- **"Today" is fixed** in the demo: `SIMULATED_TODAY` in `dateUtils.ts` drives traffic lights (`getStageTrafficLight`: verde/naranja/rojo/gris) and delays.
- **Permissions**: `MATRIZ_PERMISOS` in `src/utils/permissions.ts` maps 7 roles (`admin`, `junta`, `editor_china`, `editor_marketing`, `editor_logistica`, `editor_compras_ec`, `consulta`) to modules, operable stages and capability flags. A second, divergent helper `canUserActOnCurrentStage` in `orderState.ts` uses `usuario.etapasAsignadas` — consolidate when moving permissions to the backend.
- **Stage UI**: `OrderDetail.tsx` → tabs (`OrderScheduleTab`, `OrderProductsTab`, `OrderDocumentsTab`, `OrderHistoryTab`) and `StagePanel.tsx` (checklist, required docs, observations, Junta approval, customs). `HorizontalStatusLine.tsx` renders the 11-stage line.
- **Demo data**: `src/data/initialData.ts` (demo users share `DEMO_PASSWORD`). `src/utils/translations.ts` (es/en) is only partially wired.
- Known demo issues: starts logged in as `USUARIOS_DEMO[0]` instead of `LoginScreen`; Junta "Rechazar" cancels instead of returning; ES/EN toggle mostly untranslated; user management is UI-only.
- **Technical debt (pending)**: `@google/genai`, `express`, `dotenv` in `package.json` and `GEMINI_API_KEY` in `.env.example` are unused AI Studio leftovers. Do not use them; removal is pending and will be done in its own DKT task.
- Brand palette: yellow `#FFD100` dominant, dark grey `#515151`, mid grey `#6C6B6D`; text on yellow is always dark. Traffic lights: green `#16A34A`, orange `#EA580C`, red `#DC2626`, grey `#6C6B6D`.

## Git workflow

- `main` — stable releases delivered to the client (tagged `vX.Y.Z`).
- `develop` — integration branch, must always build; feature work merges here via PR.
- `feature/DKT-xx-nombre-corto` (also `fix/DKT-xx-…`) — branch from `develop`, merge back to `develop`. `hotfix/DKT-xx-…` branches from `main` and merges into `main` and `develop`.
- Commit format: `DKT-xx: descripción en imperativo` (≤ 72 chars; template in `.gitmessage`). PRs use `.github/pull_request_template.md` (Historia DKT, Qué cambia, Cómo probar, Capturas, DoD checklist).
- Branch protection on `main`/`develop` is not configured yet (pending); don't push directly to them anyway.
- Jira (project key `DKT`) is managed **manually** by the developer — do not set up or call any Jira integration.

## Rules that must not be broken

1. Never commit secrets, tokens or credentials. `.env` stays out of Git (already in `.gitignore`); only `.env.example` with placeholders is versioned. Same for `.claude/settings.local.json`.
2. The UI must not load any Google-hosted resource (Google Fonts, Google CDNs, Maps, Analytics, `@google/genai`…): users in China must access without VPN. Self-host fonts and assets. Google Drive integration, if any, goes **through the backend**, never from the browser.
3. No endpoint without a definition in `docs/api/openapi.yaml`.
4. Every significant architecture decision gets an ADR in `docs/adr/` (`ADR-NNN-titulo.md`: context, decision, consequences) in the same PR.
5. Backend targets Java 21 via Maven Toolchains; frontend targets Node 22; database is PostgreSQL 17. Don't bump these without an ADR.
6. Permissions are enforced server-side; the frontend only hides what the backend already forbids.
7. The GitHub repo is **public**: do not add new client documents or real client data without the developer's explicit approval.
