# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**Decokasa Tracking** — tracking of maritime import orders China → Ecuador for Decokasa S.A.S (Servientrega-style status line). Scope is tracking/communication only: no payments, costs or inventory. All UI text, identifiers and domain types are in **Spanish** (`Pedido`, `Etapa`, `Usuario`, `rol`, `semaforo`…); keep that convention.

The current code is a **static demo exported from Google AI Studio**: React 19 + TypeScript + Vite + Tailwind v4, all data in memory (resets on reload), no backend. `express`, `@google/genai`, `dotenv` and `GEMINI_API_KEY` in `package.json` / `.env.example` are AI Studio template leftovers and are unused by `src/`.

Planned production stack (not built yet): reuse this React frontend + Spring Boot 3 (Java 21) + PostgreSQL (JPA/Flyway) + Docker, JWT auth, permissions enforced server-side. The China team cannot reach Google services without VPN, so **never load Google Fonts or other Google-hosted resources in the UI**.

## Monorepo layout

- `frontend/` — the React demo (all paths under **Architecture** are relative to it). Run npm commands from here.
- `backend/` — empty, reserved for Spring Boot.
- `docs/` — project docs by type: `prd`, `actas`, `backlog`, `dominio`, `adr`, `diagramas`, `datos`, `api`, `seguridad`, `manuales`.
- `documentacion/` — original client material (see below).

## Git conventions

- Branches: `main` (stable releases, tags) ← `develop` (integration) ← `feature/DKT-xx-nombre-corto` / `fix/DKT-xx-…`; `hotfix/DKT-xx-…` from `main` merges into both.
- Commit messages: `DKT-xx: descripción en imperativo` (template in `.gitmessage`).

## Commands

```bash
cd frontend
npm install
npm run dev      # Vite on http://localhost:3000
npm run build    # production build to dist/
npm run lint     # type-check only (tsc --noEmit); there is no ESLint
```

There is no test framework configured.

## Architecture

- **`src/App.tsx` is the whole app shell**: holds all state (`currentUser`, `pedidos`, `productosNuevos`, `alertas`, toasts, `lang`), does "routing" by switching on `currentModule: AppModule` (no router library), and passes `handle*` callbacks down to views. Modules a role can't access fall back via `canUserAccessModule`.
- **`src/utils/orderState.ts` holds all business logic as pure functions** `(pedido, usuario, …) → { updatedPedido, message }`: advance / return / revert stage, extend fabrication (max +10 business days cumulative), customs substate, pause/cancel/reopen/approve, checklist, observations, file versions. Each mutation appends a `HistorialAuditoria` entry (audit trail) and, when dates are affected, calls `recalculateOrderStages`. Put new order mutations here, not in components.
- **Stage scheduling**: 11 stages defined in `ETAPAS_BASE_CONFIG` (`src/data/initialData.ts`) with deadlines in business days (Mon–Fri, `src/utils/dateUtils.ts`). Stages chain: each starts when the previous one actually finished (or is estimated to finish). Stage 2 gets +10 days if `esProductoNuevo`; stage 4 gets `ampliacionFabricacionDias`. `etapas[]` is 0-indexed, `etapaActualNumero` is 1-based.
- **"Today" is fixed**: `SIMULATED_TODAY` in `dateUtils.ts` drives traffic lights (`getStageTrafficLight`: verde/naranja/rojo/gris), audit timestamps and delays. Audit times are hard-coded strings.
- **Permissions**: `MATRIZ_PERMISOS` in `src/utils/permissions.ts` maps the 7 roles (`admin`, `junta`, `editor_china`, `editor_marketing`, `editor_logistica`, `editor_compras_ec`, `consulta`) to allowed modules, operable stages and capability flags. Components use `canUserOperateStage` (role matrix). Note there is a second, divergent helper `canUserActOnCurrentStage` in `orderState.ts` based on `usuario.etapasAsignadas` — keep the two consistent or consolidate.
- **Stage UI**: `OrderDetail.tsx` → tabs (`OrderScheduleTab`, `OrderProductsTab`, `OrderDocumentsTab`, `OrderHistoryTab`) and `StagePanel.tsx` (largest component: checklist, required docs, observations, stage-specific actions such as Junta approval and customs). `HorizontalStatusLine.tsx` renders the 11-stage tracking line.
- **Demo data**: `src/data/initialData.ts` has demo users (shared password `DEMO_PASSWORD`), orders built with `buildChainedStages`, new-product requests (separate 6-step flow) and alerts. The reference real order is `IMP-2026-006` (WPC exterior panels).
- **i18n**: `src/utils/translations.ts` (es/en) is only partially wired; most strings are hard-coded Spanish.

## Known demo issues

- `App.tsx` starts already logged in as `USUARIOS_DEMO[0]` (admin) instead of showing `LoginScreen`.
- Junta "Rechazar" cancels the order instead of returning it; ES/EN toggle doesn't translate most of the UI; user management is UI-only.

## Repo layout notes

- `documentacion/`: client process document, real order Excel, barcode labels, AI Studio prompts, 15-day implementation guide and master plan (.docx). Reference material, not code.
- Brand palette: yellow `#FFD100` dominant, dark grey `#515151`, mid grey `#6C6B6D`; text on yellow is always dark. Traffic-light colors are separate: green `#16A34A`, orange `#EA580C`, red `#DC2626`, grey `#6C6B6D`.
