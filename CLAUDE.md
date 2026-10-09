# Decokasa — sistema de seguimiento de pedidos de importación

Web interna de DECOKASA S.A.S. para mover cada pedido de importación por etapas (Ecuador ↔ China), con responsable, plazo, check, observaciones, archivos en Google Drive y alertas por Gmail.

## Fuentes de verdad

- Requerimientos: `documentos/requerimientos.md`. Si un requisito cambia, se actualiza ahí antes que el código.
- Manual de trabajo por fases: `documentos/manual-paso-a-paso.md`.
- PRD y planes por fase: `.claude/PRPs/prds/` y `.claude/PRPs/plans/`.
- Decisiones: `documentos/adr/`. Diagramas: `documentos/diagramas/`.

## Estructura

- `frontend/` — React 19 + Vite + TypeScript + Tailwind 4 (demo de Google AI Studio, en migración).
- `backend/` — Spring Boot 3 + Java 21 + Gradle (se crea en la fase 1).
- `docker-compose.yml` — PostgreSQL 17 (backend y frontend se agregan en la fase 1).

## Reglas

- Stack fijo: frontend con shadcn/ui, TanStack Query, React Router, React Hook Form y Zod; backend con Spring Security (JWT + BCrypt), Spring Data JPA, Flyway, PostgreSQL. No cambiarlo sin un ADR.
- TDD: las pruebas se escriben antes del código.
- Ramas: cada fase en `fase-N-nombre` creada desde `develop`; PR hacia `develop`. Nunca commit directo en `main` ni en `develop`. `main` solo recibe `develop` al entregar una versión.
- Plazos en días hábiles con hora de Ecuador; si una fecha cae en fin de semana pasa al lunes.
- Los permisos se validan en el servidor, nunca solo en la pantalla.
- El navegador nunca llama a Google: Drive y Gmail solo desde el backend.
- Textos, comentarios y documentación en español; nombres técnicos en inglés cuando sea la convención.
