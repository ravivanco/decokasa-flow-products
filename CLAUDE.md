# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

Web interna de DECOKASA S.A.S. para mover cada pedido de importación por etapas (Ecuador ↔ China), con responsable, plazo, check ✓/✗, observaciones, archivos en Google Drive y alertas por Gmail. Hay dos flujos: **A** (pedido de Ecuador, 11 etapas) y **B** (pedido propuesto por China, 12 etapas). Primero se implementa Ecuador; Colombia viene después.

## Fuentes de verdad

- `documentos/requerimientos.md` — requisitos, transiciones por etapa (secciones 4.4 y 4.5) y respuestas del cliente (sección 5). Si un requisito cambia, se actualiza aquí antes que el código.
- `documentos/manual-paso-a-paso.md` — orden de trabajo por fases (0 a 8), la skill de cada paso y la puerta de salida de cada fase. Seguirlo en orden.
- `.claude/PRPs/prds/` y `.claude/PRPs/plans/` — PRD y planes por fase (los usan `/ecc:prp-plan` y `/ecc:prp-implement`).
- `documentos/adr/` — decisiones de arquitectura (16 ADR: 8 accepted por el cliente, 5 accepted por el equipo y 3 proposed; el estado de cada uno está en su `README.md`). `documentos/diagramas/` — diagramas Mermaid.
- `documentos/arquitectura.md` (módulos y reglas R1–R9), `documentos/api/openapi.yaml` (contrato) y `documentos/modelo-datos.md` (tablas) — diseño técnico de la fase 0. El código los sigue; si hay que apartarse, se registra en un ADR.
- `documentos/diseno/DESIGN.md` — colores, tipografía, tokens y componentes base. `documentos/auditoria-demo.md` — qué se reutiliza del demo y hallazgos de calidad (Q) y seguridad (S).
- `documentos/epicas-historias-jira.docx` — 8 épicas y 32 historias, ya cargadas a mano en Jira.
- Las marcas [P] (propuesta) y ⏸ (pendiente del cliente) de los documentos no son requisitos confirmados.

## Estado actual

- Fase 0 (documentación): integrada en `develop` mediante el PR #1. La fase 1 se trabaja en la rama `fase-1-base-acceso`.
- `frontend/` es el demo importado de Google AI Studio, todavía sin backend.
- `backend/` tiene el esqueleto (paso 1.3): Spring Boot 4.1.1, Java 21, Gradle 9.7.1 (wrapper con sha256), paquete `com.decokasa.seguimiento`. Hay `compartido` (errores del contrato, `CodigoRol`, `CodigoPais`, puerto `Reloj`, manejador RFC 9457) y `configuracion` (`/api/v1/salud` y `SeguridadWebConfig`: JWT, CORS, `Origin` y cabeceras; el login y la gestión de usuarios están en `usuarios`, paso 1.7). Las reglas R1–R4 y R9 se verifican con ArchUnit (`arquitectura/ReglasArquitecturaTest`). Migración Flyway `V1__usuarios_roles_paises.sql` (paso 1.5): `pais`, `rol`, `rol_permiso`, `usuario`, `usuario_rol` (uno o varios roles, decisión del equipo), `usuario_pais` y `token_renovacion`; siembra EC, CO y los 7 roles, sin usuarios. La prueban `MigracionesIT` (Testcontainers). Login y gestión de usuarios y roles: paso 1.7 (ver más abajo).
- `docker-compose.yml` levanta PostgreSQL 17, el backend (`backend/Dockerfile`, JRE 21 sin root) y el frontend (`frontend/Dockerfile`: build de Vite servido por nginx, que envía `/api` al backend con `frontend/nginx.conf`) (paso 1.4). Las variables están en `.env.example`; los valores locales, en `.env` (ignorado por Git).
- Frontend: Vitest + Testing Library + MSW configurados (paso 1.2); TypeScript en modo `strict`; shadcn/ui inicializado con los tokens de `DESIGN.md` en `src/index.css` y componentes en `src/components/ui/`. Todavía no hay Playwright.
- Backend: pruebas JUnit 5 (pasos 1.3 y 1.5) con MockMvc, ArchUnit y Testcontainers (`AplicacionArrancaIT`, `MigracionesIT`); cobertura JaCoCo ≥ 80 % en `./gradlew check`.
- **Paso 1.6 terminado (2026-10-12): pruebas escritas antes de implementar, en rojo a propósito.** Las 82 pruebas anteriores del backend y las 37 del frontend pasan; las 124 nuevas del backend y las 38 del frontend fallan porque la funcionalidad no existe todavía. Las clases de `usuarios` son esqueletos que lanzan `UnsupportedOperationException("Pendiente: paso 1.7")`, y `frontend/src/rutas.tsx` declara rutas sin pantallas. Desde el paso 1.7 el backend está en verde; `npm run test` sigue en rojo hasta el paso 1.8.
- `Origin` ausente o no permitido en `/auth/renovar` y `/auth/cerrar-sesion` → 403 (aprobado el 2026-10-13, ADR-0002).
- **Paso 1.7 terminado (2026-10-13): backend de usuarios implementado.** `./gradlew check` en verde con 298 pruebas (226 iniciales, 17 de la corrección A1, 2 de la A2, 4 de la A4, 5 de la revalidación tras releer, 9 de los duplicados de M2, 32 de la versión D10 y 3 de N1; última ejecución: 2026-10-13) y 95 % de cobertura de líneas. Incluye:
  - dominio y casos de uso de `usuarios`, con la transacción fuera del núcleo (`usuarios/config/Transacciones`, ADR-0012);
  - JPA sobre V1;
  - seguridad: BCrypt (máximo 72 bytes), JWT HS256 de 15 minutos con `roles`, cookie de renovación D3b con rotación y límite de 8 horas, bloqueo de 15 minutos tras 5 fallos, `Origin` y CORS con 403 en formato `Problema`;
  - controladores de `/auth`, `/usuarios` y `/roles`;
  - cargador de usuarios iniciales desde `backend/semilla/*.local.json` (ignorado por Git; ejemplo en `usuarios.ejemplo.json`; ADR-0015).
  
  El backend exige `JWT_SECRETO`, que está en el `.env` local. El frontend (paso 1.8) sigue con sus 38 pruebas nuevas en rojo.
- **Revisión de seguridad del paso 1.7 (`/ecc:springboot-security`, 2026-10-13).** Encontró 1 hallazgo alto, 7 medios, 4 bajos y 2 informativos. El detalle está en el informe del paso 1.7.
  - **Corregido: A1.** La administración de usuarios y roles se decide con el estado actual del solicitante en la base (activo, con roles y con `admin`), no solo con los roles del JWT (`usuarios/application/usecase/Autorizacion`).
    - Un JWT anterior no administra si el solicitante está desactivado o ya no tiene `admin` (403).
    - Si la cuenta sigue activa con otros roles, renueva con esos roles; una cuenta desactivada o sin roles no renueva.
    - Un admin recién nombrado debe renovar para que su JWT incluya `admin`.
    - El cargador de arranque usa `CrearUsuarioCasoDeUso.paraCargaInicial`, que no es un bean ni lo usa ningún controlador.
    - **Aprobado por Richard (2026-10-13):** 403 `SIN_PERMISO`, y no 401, para esa operación administrativa.
  - **Corregido: A2.** La renovación consume el token de forma atómica (`TokenRenovacionRepositorio.consumir`, `UPDATE … WHERE revocado_en IS NULL`). Dos renovaciones simultáneas con el mismo token ya no emiten dos sucesores: una responde 200 y la otra cuenta como reutilización (401 y se revoca la familia).
    - Sin margen de reutilización; el límite de 8 h no cambia.
    - Lo prueba `RenovacionConcurrenteIT` con PostgreSQL e hilos concurrentes.
  - **Corregido: A4.** El login cuenta los fallos sobre la fila del usuario bloqueada y releída (`UsuarioRepositorio.buscarParaActualizar`, `SELECT … FOR UPDATE`). Los intentos simultáneos de una cuenta esperan su turno, ninguno se pierde en un conflicto (antes respondían 500) y, en cuanto uno bloquea la cuenta, los siguientes reciben 423.
    - La política D2 no cambia: 5 fallos, 15 minutos.
    - BCrypt se calcula antes de bloquear la fila.
    - Lo prueba `IntentosConcurrentesIT`: 8 logins fallidos a la vez dan 5 respuestas 401 y 3 respuestas 423, y queda guardado `intentos_fallidos = 5`.
    - D2 sin cambios (confirmado por Richard el 2026-10-13): los 5 fallos responden 401, el 5.º bloquea y durante el bloqueo se responde 423.
    - Tras releer la fila, el login revalida que la cuenta esté activa y tenga roles, y emite el JWT con los roles releídos. Si el hash de la contraseña cambió entre la lectura y la relectura, vuelve a comparar con el vigente. No existe función de cambio de contraseña.
  - **Corregido: M2, solo la parte de duplicados.** Dos altas o ediciones simultáneas con el mismo correo, usuario o teléfono ya no responden 500: la base guarda una y la otra recibe 409 `DUPLICADO` con `errores[0].campo`, sin detalles de SQL.
    - `UsuarioJpaRepositorio.guardar` usa `saveAndFlush`, y `RestriccionesUnicasUsuario` traduce solo `usuario_correo_unico`, `usuario_usuario_unico` y `usuario_telefono_unico` (SQLState 23505). Cualquier otro error de integridad no se convierte en `DUPLICADO`.
    - Lo prueba `AltasConcurrentesIT`.
  - **Corregido: M2, versión de usuarios (D10, aprobado por Richard el 2026-10-13; `openapi.yaml` 0.5.0).**
    - `Usuario.version` va en las respuestas de usuario y de sesión.
    - `PATCH /usuarios/{id}` exige `version`. Si ya no es la vigente: 409 `VERSION_DESACTUALIZADA`, sin cambios ni revocación de sesiones. Si la versión falta o no es válida, o el cuerpo solo trae `version`: 400.
    - La edición bloquea y relee la fila y compara la versión antes de cambiar nada.
    - Los contadores del login (`intentos_fallidos` y `bloqueado_hasta`) están fuera de la versión: `updatable = false` y `UsuarioDatos.guardarIntentos`. El login no cambia la versión y una edición no pisa los contadores.
    - Lo prueban `VersionUsuarioIT`, `EdicionConcurrenteIT` y `UsuarioJpaRepositorioIT`.
  - **Corregido: N1 (revisión final).** El interbloqueo previsto entre desactivar y renovar no ocurre (Hibernate usa `FOR NO KEY UPDATE`). La prueba con orden forzado sí encontró otro fallo: un token sucesor podía quedar vigente en una cuenta desactivada, o la renovación entregar una sesión durante la desactivación.
    - La renovación ahora bloquea al usuario antes de tocar sus tokens; desactivación, login y renovación siguen el mismo orden (primero el usuario).
    - Lo prueba `DesactivarMientrasRenuevaIT`, con orden forzado y estado final sin tokens vigentes.
  - **Pendientes, sin corregir:** A3 (renovación desde varias pestañas, paso 1.8), M1 (límite por IP), M2 (**versionado de roles**, pendiente explícito de D10), M3 (contraseña temporal común), M4 (CSP del frontend, paso 1.8), B1 a B4, I1 e I2.
  - **Frontend (paso 1.8):** sus pruebas del paso 1.6 y los datos ficticios deben agregar `version` (contrato 0.5.0) y recargar el usuario ante un 409 `VERSION_DESACTUALIZADA`.
- El repositorio de GitHub es público (comprobado el 2026-10-12) y ya contiene correos y teléfonos del demo y los logos del cliente (auditoría S5). ⏸ Pasarlo a privado lo decide el cliente. Mientras tanto, no subir secretos ni más datos del cliente.

## Comandos

Frontend (desde `frontend/`):

```bash
npm install
npm run dev       # Vite en http://localhost:3000 (escucha en 0.0.0.0); /api se envía a http://localhost:8080
npm run build     # build de producción en frontend/dist
npm run preview   # sirve el build
npm run lint      # chequeo de tipos: tsc --noEmit en modo strict (no hay ESLint)
npm run test      # Vitest (una pasada); npm run test:watch para modo observación
npm run coverage  # cobertura con @vitest/coverage-v8
npm run clean     # borra dist/ (funciona en PowerShell y en Bash)
```

Componentes de shadcn/ui: `npx shadcn add <componente>` (base `radix`). Revisar cada componente generado contra `DESIGN.md`: foco con `ring-ring` sin transparencia y altura mínima de 40 px (lo comprueban `src/components/ui/*.test.*`).

Backend (desde `backend/`; en PowerShell usar `.\gradlew.bat`):

```bash
./gradlew test       # pruebas; AplicacionArrancaIT usa Testcontainers y necesita Docker encendido
./gradlew check      # pruebas + ArchUnit + cobertura JaCoCo >= 80 % (informe en build/reports/jacoco)
./gradlew bootJar    # jar ejecutable en build/libs
```

La conexión a la base sale de `DB_URL`, `DB_USUARIO` y `DB_CONTRASENA`; no hay secretos en `application.yml`.

Docker (desde la raíz; copiar `.env.example` como `.env` y completar `POSTGRES_PASSWORD`, si falta no arranca):

```bash
docker compose up -d --build --wait   # PostgreSQL :5432 y backend :8080 (solo 127.0.0.1), frontend http://localhost:3000
docker compose up -d postgres         # solo la base, para trabajar con npm run dev y ./gradlew bootRun
docker compose ps                     # los tres deben estar "healthy"
docker compose logs -f backend
docker compose down                   # agrega -v para borrar también los datos de la base
```

El frontend de Docker y `npm run dev` usan el puerto 3000: no correr los dos a la vez.

## Arquitectura del demo (frontend/src)

Toda la lógica vive en el navegador y en memoria; no hay llamadas HTTP, ni `localStorage`, ni uso real de `@google/genai`.

- `App.tsx` — contiene todo el estado (usuario actual, país, pedidos, bodegas, notificaciones) con `useState` y navega por `AppModule` (estado `currentModule`), sin React Router. Los componentes reciben datos y callbacks por props.
- `utils/orderState.ts` — el "motor" del demo: funciones puras que reciben un `Pedido` y devuelven uno nuevo (`advanceOrderStage`, `rejectStageWithObservations`, `returnToCodingWithUrgency`, `reportCustomsDelay`, `uploadStageFile`…). Es la referencia de reglas de negocio para el motor del backend de la fase 2.
- `data/initialData.ts` — definiciones de etapas `ETAPAS_FLUJO_A` / `ETAPAS_FLUJO_B`, usuarios demo (contraseña `DEMO_PASSWORD`), bodegas, pedidos y notificaciones de ejemplo; `buildStagesForOrder` arma las etapas de un pedido.
- `utils/permissions.ts` — permisos por rol y etapa, solo en el cliente. En el sistema real se validan en el servidor.
- `utils/dateUtils.ts` — días hábiles y semáforo de plazos. `SIMULATED_TODAY` fija "hoy" en `2026-10-09`; reemplazarlo por la fecha real al conectar el backend.
- `utils/translations.ts` — textos es/en. `types.ts` — modelo de dominio (`Pedido`, `EtapaInstancia`, `HistorialAuditoria`, `UrgenciaDevolucion`…).
- Alias `@/` apunta a `frontend/src/` (vite.config.ts y tsconfig.json; cambiado en el paso 1.2, antes no lo usaba ningún archivo).

## Reglas del proyecto

- Stack fijo. Frontend: React 19 + Vite + TypeScript + Tailwind 4 + shadcn/ui + TanStack Query + React Router + React Hook Form + Zod. Backend: Spring Boot 4.1 (ADR-0016; la rama 3 quedó sin soporte gratuito el 2026-06-30) + Java 21 + Gradle + Spring Security (JWT + BCrypt) + Spring Data JPA + Flyway + PostgreSQL + Docker. No cambiarlo sin un ADR.
- Arquitectura hexagonal en el backend: el dominio (pedidos, flujos, transiciones, plazos) no depende de Spring, JPA ni Google. Drive y Gmail son adaptadores.
- TDD: escribir las pruebas antes del código.
- Ramas: cada fase en `fase-N-nombre`, creada desde `develop`, con PR hacia `develop`. Nunca hacer commit directo en `main` ni en `develop`; `main` solo recibe `develop` al entregar una versión. Al pasar `develop` a `main`, confirmar con `git diff --stat develop main` que no quedan diferencias.
- Plazos en días hábiles con hora de Ecuador (UTC−5). Una fecha que cae en fin de semana pasa al lunes, y un retraso arrastra todas las etapas siguientes.
- Los permisos se validan en el servidor, nunca solo en la pantalla. El navegador nunca llama a Google: Drive y Gmail solo desde el backend.
- Código, comentarios y documentación en español, salvo los nombres técnicos donde la convención es inglés.
