# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

Web interna de DECOKASA S.A.S. para mover cada pedido de importación por etapas (Ecuador ↔ China), con responsable, plazo, check ✓/✗, observaciones, archivos en Google Drive y alertas por Gmail. Hay dos flujos: **A** (pedido de Ecuador, 11 etapas) y **B** (pedido propuesto por China, 12 etapas). Primero se implementa Ecuador; Colombia viene después.

## Fuentes de verdad

- `documentos/requerimientos.md` — requisitos, transiciones por etapa (secciones 4.4 y 4.5) y respuestas del cliente (sección 5). Si un requisito cambia, se actualiza aquí antes que el código.
- `documentos/manual-paso-a-paso.md` — orden de trabajo por fases (0 a 8), la skill de cada paso y la puerta de salida de cada fase. Seguirlo en orden.
- `.claude/PRPs/prds/` y `.claude/PRPs/plans/` — PRD y planes por fase (los usan `/ecc:prp-plan` y `/ecc:prp-implement`).
- `documentos/adr/` — decisiones de arquitectura. `documentos/diagramas/` — diagramas Mermaid.

## Estado actual

- `frontend/` es el demo importado de Google AI Studio, todavía sin backend.
- `backend/` está vacío; el proyecto Spring Boot se crea en la fase 1.
- `docker-compose.yml` solo tiene PostgreSQL 17; el backend y el frontend se agregan en la fase 1.
- No hay pruebas automatizadas todavía (ni Vitest ni Playwright ni JUnit).

## Comandos

Frontend (desde `frontend/`):

```bash
npm install
npm run dev       # Vite en http://localhost:3000 (escucha en 0.0.0.0)
npm run build     # build de producción en frontend/dist
npm run preview   # sirve el build
npm run lint      # chequeo de tipos: tsc --noEmit (no hay ESLint)
```

`npm run clean` usa `rm -rf` y no funciona en PowerShell; en Windows usar Git Bash o borrar `dist/` a mano.

Base de datos (desde la raíz; necesita `POSTGRES_PASSWORD` en un `.env` en la raíz, si falta no arranca):

```bash
docker compose up -d postgres
docker compose down
```

## Arquitectura del demo (frontend/src)

Toda la lógica vive en el navegador y en memoria; no hay llamadas HTTP, ni `localStorage`, ni uso real de `@google/genai`.

- `App.tsx` — contiene todo el estado (usuario actual, país, pedidos, bodegas, notificaciones) con `useState` y navega por `AppModule` (estado `currentModule`), sin React Router. Los componentes reciben datos y callbacks por props.
- `utils/orderState.ts` — el "motor" del demo: funciones puras que reciben un `Pedido` y devuelven uno nuevo (`advanceOrderStage`, `rejectStageWithObservations`, `returnToCodingWithUrgency`, `reportCustomsDelay`, `uploadStageFile`…). Es la referencia de reglas de negocio para el motor del backend de la fase 2.
- `data/initialData.ts` — definiciones de etapas `ETAPAS_FLUJO_A` / `ETAPAS_FLUJO_B`, usuarios demo (contraseña `DEMO_PASSWORD`), bodegas, pedidos y notificaciones de ejemplo; `buildStagesForOrder` arma las etapas de un pedido.
- `utils/permissions.ts` — permisos por rol y etapa, solo en el cliente. En el sistema real se validan en el servidor.
- `utils/dateUtils.ts` — días hábiles y semáforo de plazos. `SIMULATED_TODAY` fija "hoy" en `2026-10-09`; reemplazarlo por la fecha real al conectar el backend.
- `utils/translations.ts` — textos es/en. `types.ts` — modelo de dominio (`Pedido`, `EtapaInstancia`, `HistorialAuditoria`, `UrgenciaDevolucion`…).
- Alias `@/` apunta a `frontend/` (vite.config.ts y tsconfig.json).

## Reglas del proyecto

- Stack fijo. Frontend: React 19 + Vite + TypeScript + Tailwind 4 + shadcn/ui + TanStack Query + React Router + React Hook Form + Zod. Backend: Spring Boot 3 + Java 21 + Gradle + Spring Security (JWT + BCrypt) + Spring Data JPA + Flyway + PostgreSQL + Docker. No cambiarlo sin un ADR.
- Arquitectura hexagonal en el backend: el dominio (pedidos, flujos, transiciones, plazos) no depende de Spring, JPA ni Google. Drive y Gmail son adaptadores.
- TDD: escribir las pruebas antes del código.
- Ramas: cada fase en `fase-N-nombre`, creada desde `develop`, con PR hacia `develop`. Nunca hacer commit directo en `main` ni en `develop`; `main` solo recibe `develop` al entregar una versión. Al pasar `develop` a `main`, confirmar con `git diff --stat develop main` que no quedan diferencias.
- Plazos en días hábiles con hora de Ecuador (UTC−5). Una fecha que cae en fin de semana pasa al lunes, y un retraso arrastra todas las etapas siguientes.
- Los permisos se validan en el servidor, nunca solo en la pantalla. El navegador nunca llama a Google: Drive y Gmail solo desde el backend.
- Código, comentarios y documentación en español, salvo los nombres técnicos donde la convención es inglés.
