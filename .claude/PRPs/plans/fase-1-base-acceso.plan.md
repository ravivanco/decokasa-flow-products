# Plan: Fase 1 — Base y acceso

## Summary

Convertir el demo de AI Studio en la base real del sistema: backend Spring Boot 4.1 (ADR-0016) + Java 21 con arquitectura hexagonal, PostgreSQL 17 con Flyway, inicio de sesión propio (correo, usuario o teléfono + contraseña, BCrypt y JWT), gestión de usuarios y roles por el admin, y un frontend con React Router y TanStack Query que muestra un dashboard vacío según el rol. Al terminar, ninguna credencial ni permiso vive en el navegador y todo corre con `docker compose up`.

## User Story

Como persona del equipo de Decokasa (Mirian, Marcela, Andrea Q., David, Anderson o Andrea C.), quiero entrar con mi correo, usuario o teléfono y mi contraseña, para ver el dashboard de mi rol sin poder entrar al de otro. Como admin (Mirian), quiero crear usuarios y asignarles un rol, para controlar quién trabaja en el sistema.

## Problem → Solution

Demo sin backend: contraseña única `Demo2026` en el bundle, arranque con sesión de admin, cambio de rol con un clic y permisos solo en el navegador (auditoría S1, S2, S3) → backend que autentica y autoriza en el servidor, frontend que solo muestra lo que la API permite, y entorno reproducible con Docker.

## Metadata

- **Complexity**: XL (backend nuevo, migraciones, seguridad y reestructura del frontend)
- **Source PRD**: `.claude/PRPs/prds/decokasa.prd.md`
- **PRD Phase**: Fase 1 — Base y acceso
- **Historias de Jira**: HU 01, HU 02 y HU 03 (épica E01)
- **Estimated Files**: unos 75 (55 nuevos en `backend/`, 15 nuevos o reescritos en `frontend/`, 5 de configuración y documentación)

## Leyenda

- **Requisito**: lo dice `documentos/requerimientos.md` o un ADR en estado accepted.
- **[P]**: propuesta técnica del equipo; no está aprobada y se puede cambiar.
- **⏸**: depende de una respuesta del cliente.
- **⛔ Dn**: la tarea no empieza hasta que la decisión Dn quede registrada (sección siguiente).

---

## Decisiones antes de implementar

Las decisiones del equipo quedaron aprobadas por Richard el 2026-10-12 y registradas en sus ADR (abajo). Las preguntas del cliente siguen abiertas y **no tienen valor aprobado**; las tareas que dependen de ellas están marcadas con ⛔.

### Preguntas para el cliente ⏸

| # | Tema | Qué dicen las fuentes | Cómo lo trata el plan | Tareas que dependen |
| --- | --- | --- | --- | --- |
| D5b | **¿El repositorio pasa a privado?** | El repositorio es público (comprobado el 2026-10-12, `CLAUDE.md`). El manual (paso 1.5) pide sembrar los 6 usuarios reales con contraseña temporal | Ver D5: no se sube ningún dato real ni contraseña, sea cual sea la respuesta | T10 |
| — | **Alcance del rol `consulta`** (D7) | No aparece en las fuentes; Richard decidió conservarlo | Dashboard vacío de consulta, sin permisos de gestión | T10, T24 |
| — | Qué ve la asistente de compras en China | `requerimientos.md` 4.3: dashboard de consulta, **TBD** qué ve | Dashboard vacío de consulta, sin permisos de gestión | T24 |
| — | Recuperación de contraseña | `requerimientos.md` 4.7: **TBD** | Fuera de la fase 1. El admin cambia la contraseña al editar el usuario [P] (T15) | — |

### Decisiones técnicas del equipo (aprobadas por Richard el 2026-10-12)

| # | Tema | Decisión | Dónde se registra | Tareas |
| --- | --- | --- | --- | --- |
| D1 | **WebSocket** | **No se agrega en la fase 1**; queda para la fase 5. La elección entre WebSocket y consultas periódicas se toma en esa fase | ADR-0014 y manual (paso 1.3) | T5 |
| D2 | **Bloqueo por intentos fallidos** | **5 intentos fallidos de contraseña → 15 minutos de bloqueo** (423 `CUENTA_BLOQUEADA`). Valores configurables. La diferencia de fuentes sobre quién escribió `requerimientos.md` 4.7 queda anotada en ADR-0002, sin afectar a los valores | ADR-0002, `requerimientos.md` 4.7 | T9, T12, T14, T15 |
| D3 | **JWT y renovación** | **Acceso de 15 minutos; renovación de la sesión hasta 8 horas** desde el inicio de sesión (límite absoluto), **con rotación** y detección de reutilización (familias de tokens) | ADR-0002, `requerimientos.md` 4.7, `modelo-datos.md` | T9, T12, T14, T17, T18 |
| D3b | **Almacenamiento del token de renovación** (revisado) | **Propuesta pendiente de aprobación:** cookie `decokasa_renovacion` con `HttpOnly`, `Secure`, `SameSite=Strict` y `Path=/api/v1/auth`; acceso solo en memoria; nada en `localStorage` ni `sessionStorage`; verificación de `Origin`; nuevo `POST /auth/cerrar-sesion`. El contrato ya lo describe como propuesta | ADR-0002, `openapi.yaml` 0.2.0 | T13, T14, T17, T18 |
| D5 | **Datos iniciales** | Las migraciones siembran **países y los 7 roles**; las pruebas usan usuarios ficticios; los usuarios reales se cargan fuera de git; ninguna contraseña real ni clave en el repositorio. [P] La forma de carga: un cargador de arranque con un archivo local ignorado por git | ADR-0015 | T10 |
| D6 | **Pantallas del demo** | Las pantallas de pedidos se conservan **sin ruta** hasta conectarlas en su fase. El router de la fase 1 solo expone login, dashboard vacío por rol, usuarios y roles | PRD (registro de decisiones) | T19, T23 |
| D7 | **Rol `consulta`** | **Se conserva** como 7.º rol, sin permisos de gestión. ⏸ Su alcance queda pendiente del cliente | `requerimientos.md` 5, `openapi.yaml` (`CodigoRol`), ADR-0015 | T10, T24 |
| D4 | **Roles por usuario** (antes pregunta para el cliente) | **Uno o varios roles por usuario, asignados solo por el admin** (tabla `usuario_rol`). Decisión de Richard del 2026-10-12, **no respuesta del cliente**. Un usuario sin roles no puede acceder; una cuenta desactivada no inicia ni renueva sesión y su historial se conserva | `requerimientos.md` 4.7 y 5, `modelo-datos.md` 3.1, `arquitectura.md` 4.1, `openapi.yaml` 0.3.0 (`Usuario.roles`) | T9, T10, T13, T15, T16, T22 |
| D8 | **Backend** | **Gradle, arquitectura hexagonal y transacciones fuera del dominio.** Siguen como propuesta: el nombre del paquete `com.decokasa.seguimiento`, ArchUnit y el outbox (fase 3) | ADR-0010, 0011 y 0012 | T5, T6, T7, T17 |

**Regla que se conserva (manual, paso 1.6):** **un usuario sin roles no puede acceder.** La base permite un usuario sin filas en `usuario_rol` (por ejemplo, uno cargado fuera de Git sin roles); el dominio y el inicio de sesión lo rechazan, y se prueba en T11 y T13. Lo mismo para una cuenta desactivada, al iniciar y al renovar sesión (D4).

---

## UX Design

### Before

```
┌────────────────────────────────────────────────────────────┐
│ Abre la app → ya está dentro como Mirian (admin)           │
│ Login: "Clave demo: Demo2026" + 7 botones de acceso rápido │
│ Encabezado: clic en el usuario → cambia a cualquier rol    │
│ Selector EC / CO en el login                               │
│ Usuarios: tarjetas de solo lectura + "cambiar de sesión"   │
│ Todos los módulos con datos simulados en memoria           │
└────────────────────────────────────────────────────────────┘
```

### After

```
┌────────────────────────────────────────────────────────────┐
│ Abre la app → /login (sin sesión no hay nada más)          │
│ Login: identificador + contraseña; errores en español      │
│   401 → "Credenciales incorrectas"                         │
│   423 → "Cuenta bloqueada temporalmente" (D2)            │
│ Entra → /inicio con el dashboard de SU rol (vacío)         │
│ Encabezado: nombre, rol y "Cerrar sesión" (sin cambio)     │
│ Admin: /usuarios (crear, editar, activar, asignar rol)     │
│        /roles (crear y editar roles y permisos)            │
│ Ruta de otro rol → página "Sin permiso"; la API da 403     │
└────────────────────────────────────────────────────────────┘
```

### Interaction Changes

| Touchpoint | Before | After | Notes |
| --- | --- | --- | --- |
| Arranque | Sesión de admin automática (S3) | Redirige a `/login` | |
| Login | Compara `Demo2026` en el navegador (S1) | `POST /api/v1/auth/login` | Mismo mensaje para cuenta inexistente, contraseña mala o usuario desactivado (contrato) |
| Acceso rápido y cambio de rol | Botones en login, encabezado, ventana modal y gestión de usuarios (S2) | Eliminados | |
| País | Selector EC / CO | Sin selector; solo EC | Colombia es la fase 7 |
| Navegación | Estado `currentModule` | React Router con rutas protegidas | |
| Usuarios | Tarjetas de solo lectura | Tabla + formulario (React Hook Form + Zod) | Duplicados → 409 `DUPLICADO` en el campo |
| Sesión vencida | No existía | Renovación transparente (D3); si falla, vuelve a `/login` | |

---

## Mandatory Reading

| Priority | File | Lines | Why |
| --- | --- | --- | --- |
| P0 | `documentos/api/openapi.yaml` | 1-48, 80-358, `components` | Contrato de `auth`, `usuarios` y `roles`; esquemas `Usuario`, `UsuarioActual`, `RespuestaSesion`, `Problema` |
| P0 | `documentos/arquitectura.md` | 17-79, 120-131, 244-295, 331-345 | Reglas R1 a R9, módulo `usuarios`, estructura de paquetes, estrategia de pruebas |
| P0 | `documentos/modelo-datos.md` | 12-23, 56-104, 302-333, 365-385 | Tablas `pais`, `rol`, `rol_permiso`, `usuario`, `usuario_pais`, `token_renovacion`; índices; reglas JPA |
| P0 | `documentos/auditoria-demo.md` | 234-290, 305-335 | Qué se reutiliza, reescribe y descarta; hallazgos S1, S2, S3, S7, S8 |
| P1 | `documentos/adr/0002-autenticacion-propia-jwt.md` | todo | Autenticación propia; propuestas de bloqueo y vida corta |
| P1 | `documentos/adr/0011-monolito-modular-hexagonal.md`, `0012-transacciones-y-outbox.md` | todo | Hexagonal y transacciones fuera del núcleo |
| P1 | `documentos/diseno/DESIGN.md` | 168-218, 326-346 | Tokens de Tailwind y shadcn; accesibilidad Q1–Q13 |
| P1 | `documentos/requerimientos.md` | 12-30, 370-380, 440-460 | Responsables, actores, entidad Usuario, sección 4.7 |
| P2 | `frontend/src/components/LoginScreen.tsx` | 23-41 | Normalización del identificador que pasa al backend |
| P2 | `frontend/src/types.ts` | 1-8 | Códigos de rol |
| P2 | `frontend/src/utils/permissions.ts` | 46-85 | Nombres y colores de rol que se conservan en el cliente |

## External Documentation

No se hizo investigación web al preparar este plan. Las versiones exactas se fijan al crear el proyecto y se anotan en el PR.

| Topic | Source | Key Takeaway |
| --- | --- | --- |
| Spring Boot 4.1.x | api.spring.io, start.spring.io, docs.spring.io/spring-boot/system-requirements.html | La 3.x quedó sin soporte gratuito el 2026-06-30; se usa la 4.1.x (ADR-0016): Java 17+ (se usa 21), Gradle 8.14+ o 9. Usar el wrapper de Gradle porque Gradle no está instalado en el equipo |
| JWT en Spring Security | Spring Security, "OAuth 2.0 Resource Server – JWT" | `spring-boot-starter-oauth2-resource-server` trae `JwtEncoder` y `JwtDecoder` (Nimbus) con HS256; evita sumar `jjwt`. Solo en `adapter.out.security` y `configuracion` (R1 prohíbe JWT en el núcleo) |
| Testcontainers | testcontainers.org y `@ServiceConnection` de Spring Boot | Imagen `postgres:17`, igual que `docker-compose.yml` |
| shadcn/ui con Tailwind 4 | ui.shadcn.com/docs/installation/vite | `npx shadcn@latest init` con alias `@/` hacia `src/` |
| TanStack Query v5, React Router, React Hook Form, Zod | Documentación oficial | v5 usa `useQuery({ queryKey, queryFn })` e `isPending` |

---

## Patterns to Mirror

**El backend no tiene código todavía** (`backend/` solo tiene `.gitkeep`). Sus patrones salen de los documentos de la fase 0, que son la referencia obligatoria. Los del frontend salen del demo.

### NAMING_CONVENTION (backend, de la arquitectura)
// SOURCE: documentos/arquitectura.md:126-129 y 250-283
```text
com.decokasa.seguimiento.usuarios.domain.Usuario
com.decokasa.seguimiento.usuarios.application.port.in.IniciarSesion
com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio
com.decokasa.seguimiento.usuarios.adapter.in.web.AuthController
com.decokasa.seguimiento.usuarios.adapter.out.persistence.UsuarioJpaRepositorio
com.decokasa.seguimiento.usuarios.adapter.out.security.BCryptCodificador / JwtEmisor
com.decokasa.seguimiento.usuarios.config   ← raíz de composición del módulo
```
Módulos y clases en español; capas con los nombres técnicos en inglés (`domain`, `application`, `adapter`).

### NAMING_CONVENTION (frontend, del demo)
// SOURCE: frontend/src/components/Toast.tsx:1-15
```tsx
import React from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info';
  text: string;
}

export const ToastContainer: React.FC<ToastProps> = ({ toasts, onDismiss }) => {
```
Componentes en PascalCase con exportación nombrada; dominio en español (`Usuario`, `Pais`, `rol`).

### ERROR_HANDLING (contrato RFC 9457)
// SOURCE: documentos/api/openapi.yaml, components.schemas.Problema
```yaml
Problema:
  required: [title, status, codigo]
  properties:
    codigo:
      enum: [VALIDACION, NO_AUTENTICADO, SIN_PERMISO, NO_ENCONTRADO, ETAPA_CAMBIO,
             TRANSICION_NO_PERMITIDA, DUPLICADO, REQUISITO_FALTANTE, ARCHIVO_DEMASIADO_GRANDE,
             TIPO_ARCHIVO_NO_ACEPTADO, COPIA_PENDIENTE, CUENTA_BLOQUEADA]
    errores:   # solo con VALIDACION
      items: { required: [campo, mensaje] }
```
El dominio lanza errores propios (`ErrorDeDominio` en `compartido.domain`); un único `@RestControllerAdvice` en `compartido.adapter.in.web` los traduce a `application/problem+json` con `ProblemDetail` de Spring y la propiedad `codigo`.

### LOGIN_IDENTIFIER (lógica del demo que pasa al servidor)
// SOURCE: frontend/src/components/LoginScreen.tsx:27-32
```tsx
const cleanId = identifier.trim().toLowerCase();
const user = USUARIOS_DEMO.find((u) => {
  const matchEmail = u.email.toLowerCase() === cleanId;
  const matchLogin = u.usuarioLogin.toLowerCase() === cleanId;
  const matchTel = u.telefono.replace(/\s+/g, '') === cleanId.replace(/\s+/g, '');
  return matchEmail || matchLogin || matchTel;
});
```
En el backend: correo y usuario sin distinguir mayúsculas; teléfono normalizado a dígitos y `+` [P] (`modelo-datos.md` 3.1).

### ROLE_CODES
// SOURCE: frontend/src/types.ts:1-8 y openapi.yaml `CodigoRol`
```ts
export type UserRole =
  | 'admin' | 'asistente_compras' | 'marketing'
  | 'compras_china' | 'logistica' | 'asistente_china'
  | 'consulta';   // se conserva (D7); alcance ⏸
```

### REPOSITORY_PATTERN (de la arquitectura y el modelo)
// SOURCE: documentos/modelo-datos.md:365-385
- Entidades JPA solo en `adapter/out/persistence/` (`UsuarioEntity`, `RolEntity`), con un mapeador hacia el dominio.
- `@Id UUID id` asignado por la aplicación (sin `@GeneratedValue`); `@Version Long version` en `UsuarioEntity`.
- `spring.jpa.hibernate.ddl-auto=validate` y `spring.jpa.open-in-view=false`; Flyway es dueño del esquema.

### SERVICE_PATTERN (casos de uso sin Spring)
// SOURCE: documentos/arquitectura.md:58-79 (R1, R2) y ADR-0012
- Los casos de uso son clases Java puras que implementan un puerto de entrada; se registran como `@Bean` en `usuarios/config/UsuariosConfig.java`.
- La transacción la abre un decorador registrado en `config/` con `TransactionTemplate` (ADR-0012, aprobado), no `@Transactional` en el caso de uso.
- La fecha sale del puerto `Reloj` en `America/Guayaquil` (R5).

### LOGGING_PATTERN
No hay registros en el demo ni en el backend. [P] SLF4J solo en adaptadores y `configuracion`; nunca la contraseña, el token ni el hash; los intentos fallidos se registran con el identificador enmascarado.

### TEST_STRUCTURE (de la arquitectura)
// SOURCE: documentos/arquitectura.md:286-292 y 331-345
```text
backend/src/test/java/com/decokasa/seguimiento/
├── arquitectura/      ← ArchUnit: R1–R4 y R9
└── usuarios/
    ├── domain/        ← JUnit 5 puro
    ├── application/   ← casos de uso con adaptadores falsos (RelojFijo, repositorios en memoria)
    └── adapter/       ← @DataJpaTest / @SpringBootTest + Testcontainers (postgres:17)
```
Frontend: no hay pruebas todavía. Se agregan Vitest + Testing Library + MSW con archivos `*.test.tsx` junto al componente.

### DOCKER_PATTERN
// SOURCE: docker-compose.yml:6-22
```yaml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Define POSTGRES_PASSWORD en .env}
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-decokasa} -d ${POSTGRES_DB:-decokasa}"]
```
Variables obligatorias con `:?mensaje en español`; `healthcheck` en cada servicio.

---

## Files to Change

| File | Action | Justification |
| --- | --- | --- |
| `frontend/package.json` | UPDATE | Quitar dependencias sin uso (S7); agregar router, query, formularios y pruebas |
| `frontend/vite.config.ts` | UPDATE | Proxy `/api` → `http://localhost:8080`; alias `@` → `src`; quitar comentarios de AI Studio; Vitest |
| `frontend/tsconfig.json` | UPDATE | `strict: true` (hoy compila sin errores con `--strict`); alias `@/*` → `./src/*` |
| `frontend/.env.example` | UPDATE | Solo `VITE_API_URL` (S8) |
| `frontend/README.md` | UPDATE | Quitar las instrucciones de la clave de Gemini (S8) |
| `frontend/metadata.json` | DELETE | Configuración de AI Studio (S8, auditoría 8.3) |
| `frontend/index.html` | UPDATE | Título y descripción sin "tiempo real" ni "rastreo marítimo", que todavía no existen |
| `frontend/components.json`, `src/lib/utils.ts`, `src/components/ui/*` | CREATE | shadcn/ui |
| `frontend/src/index.css` | UPDATE | Tokens de `DESIGN.md` §6 |
| `frontend/src/main.tsx` | UPDATE | `QueryClientProvider`, `RouterProvider`, `SesionProvider` |
| `frontend/src/App.tsx` | UPDATE (reescritura) | Pasa a ser el layout autenticado (encabezado, menú, `<Outlet/>`) |
| `frontend/src/rutas.tsx` | CREATE | Rutas y protección por permiso |
| `frontend/src/api/cliente.ts`, `api/esquemas.ts`, `api/auth.ts`, `api/usuarios.ts`, `api/roles.ts` | CREATE | Llamadas HTTP, esquemas Zod del contrato y consultas de TanStack Query |
| `frontend/src/auth/SesionProvider.tsx`, `auth/RutaProtegida.tsx` | CREATE | Sesión en memoria y guardas de ruta |
| `frontend/src/components/LoginScreen.tsx` | UPDATE (reescritura) | Autenticación real (S1, S2) |
| `frontend/src/components/UsersManagementView.tsx` | UPDATE (reescritura) | Crear, editar, activar y asignar rol |
| `frontend/src/components/RolesManagementView.tsx` | CREATE | Crear y editar roles |
| `frontend/src/components/dashboards/DashboardRol.tsx` | CREATE | Dashboard vacío por rol |
| `frontend/src/components/SinPermiso.tsx` | CREATE | Página 403 |
| `frontend/src/components/Header.tsx`, `Sidebar.tsx` | UPDATE | Quitar el cambio de rol (S2); enlaces con React Router |
| `frontend/src/types.ts`, `utils/permissions.ts` | UPDATE | Permisos solo de presentación; `consulta` se conserva (D7) |
| `frontend/src/data/initialData.ts` | UPDATE | Quitar `DEMO_PASSWORD`, `USUARIOS_DEMO`, pedidos, bodegas y notificaciones de ejemplo; quedan `ETAPAS_FLUJO_A/B` y `buildStagesForOrder` (los usan `orderState.ts` y `OrdersListView.tsx`) |
| `frontend/src/components/HelpView.tsx`, `utils/translations.ts` | DELETE | Auditoría 8.3; sin otros importadores |
| `frontend/src/test/setup.ts`, `src/test/servidor.ts` | CREATE | Configuración de Vitest y MSW |
| `backend/settings.gradle`, `build.gradle`, `gradlew*`, `gradle/wrapper/*` | CREATE | Proyecto Gradle |
| `backend/src/main/java/com/decokasa/seguimiento/**` | CREATE | `compartido`, `usuarios`, `configuracion` |
| `backend/src/main/resources/application.yml` | CREATE | Configuración |
| `backend/src/main/resources/db/migration/V1__usuarios_roles_paises.sql`, `V2__semilla_paises_roles.sql` | CREATE | Esquema y datos no personales |
| `backend/semilla/usuarios.ejemplo.json` | CREATE | Ejemplo ficticio para el cargador (D5) |
| `backend/src/test/java/com/decokasa/seguimiento/**` | CREATE | Pruebas |
| `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf` | CREATE | Contenedores de desarrollo |
| `docker-compose.yml` | UPDATE | Servicios `backend` y `frontend` |
| `.env.example` (raíz) | CREATE | Todas las variables, sin valores reales |
| `.gitignore` | UPDATE | `backend/semilla/*.local.json`, `backend/build/`, `.gradle/` |
| `CLAUDE.md` | UPDATE | Comandos de backend y pruebas; alias `@/` → `src/`; estado actual |
| `documentos/adr/0015-datos-iniciales.md` | CREATE | D5 |
| `documentos/api/openapi.yaml` | UPDATE | Descripción de `CodigoRol`: 7 roles iniciales, incluido `consulta` (D7) |
| `.claude/PRPs/prds/decokasa.prd.md` | UPDATE | Fase 1 en `in-progress` con enlace a este plan (se hace al crear el plan) |

## NOT Building

- WebSocket y tiempo real (fase 5; D1).
- Selección de país al iniciar sesión y todo lo de Colombia (fase 7). Las tablas `pais` y `usuario_pais` se crean con CO inactivo.
- Recuperación de contraseña por correo (⏸ TBD).
- Cambio obligatorio de contraseña en el primer ingreso (no está en las fuentes).
- Conexión a la API de las pantallas de pedidos, bodegas, auditoría, correos y cronograma (fases 2 a 5).
- Permisos de pedidos, bodegas, auditoría y plazos: se agregan en la fase que los usa.
- Correos, Drive y cualquier llamada a Google (fase 3).
- Playwright (empieza en la fase 2).
- Borrar `OrderProductsTab.tsx`: lo importa `OrderDetail.tsx`, que se reescribe en la fase 2.
- Imágenes de producción, HTTPS y CI (fase 8).

---

## Step-by-Step Tasks

El orden sigue el manual (pasos 1.2 a 1.11). Dentro de cada paso, cuando hay código con comportamiento, la prueba se escribe primero y se ve fallar. El paso 1.6 del manual concentra las pruebas de autenticación y usuarios, que se escriben antes de implementar los pasos 1.7 y 1.8.

### Paso 1.2 — Frontend (el demo ya está en `frontend/`; no se traslada)

#### Task 1: Limpiar dependencias y restos de AI Studio
- **ACTION**: Quitar de `package.json` `@google/genai`, `express`, `dotenv`, `motion`, `tsx`, `@types/express` y `autoprefixer`. Mover `@vitejs/plugin-react`, `@tailwindcss/vite` y `vite` a `devDependencies`. Borrar `metadata.json`. Reescribir `.env.example` con solo `VITE_API_URL=` (vacío = usa el proxy). Quitar del `README.md` la sección de la clave de Gemini. En `vite.config.ts`, quitar los comentarios y la lógica de `DISABLE_HMR`. Renombrar `"name": "react-example"` a `"decokasa-frontend"`. Cambiar `clean` a `node -e "require('fs').rmSync('dist',{recursive:true,force:true})"` para que funcione en PowerShell.
- **IMPLEMENT**: Solo configuración; sin cambios de comportamiento.
- **MIRROR**: —
- **IMPORTS**: —
- **GOTCHA**: Comprobar con `grep -rn "motion\|@google/genai\|express\|dotenv" src` que nada los importa antes de quitarlos (la auditoría S7 dice que no). Con `"type": "module"`, `require` no existe en `node -e` de un módulo ES; si falla, usar `node --input-type=commonjs -e`.
- **VALIDATE**: `npm install`, `npm run lint` y `npm run build` sin errores; `npm run dev` muestra el demo igual que antes.

#### Task 2: Agregar las librerías del stack y las de pruebas
- **ACTION**: `npm install react-router @tanstack/react-query react-hook-form zod @hookform/resolvers`. `npm install -D vitest @vitest/coverage-v8 jsdom @testing-library/react @testing-library/user-event @testing-library/jest-dom msw`.
- **IMPLEMENT**: En `package.json`: `"test": "vitest run"`, `"test:watch": "vitest"`, `"coverage": "vitest run --coverage"`. En `vite.config.ts`: bloque `test: { environment: 'jsdom', globals: true, setupFiles: './src/test/setup.ts' }`, alias `'@': path.resolve(__dirname, './src')` y `server.proxy: { '/api': 'http://localhost:8080' }`. En `tsconfig.json`: `"strict": true`, `"paths": { "@/*": ["./src/*"] }`, `"types": ["vite/client", "vitest/globals"]`.
- **MIRROR**: —
- **IMPORTS**: `/// <reference types="vitest/config" />` en `vite.config.ts`.
- **GOTCHA**: Hoy ningún archivo usa `@/`, así que mover el alias a `src/` no rompe nada; actualizar la línea correspondiente de `CLAUDE.md`. El proxy evita CORS en desarrollo; `VITE_API_URL` solo se usa si se define.
- **VALIDATE**: `npm run lint` (con `strict`) sin errores; `npm run test` responde "No test files found" sin errores de configuración.

#### Task 3: Inicializar shadcn/ui con los tokens de DESIGN.md
- **ACTION**: `npx shadcn@latest init` (Vite, alias `@/components` y `@/lib/utils`). Agregar los componentes `button`, `input`, `label`, `form`, `table`, `dialog`, `select`, `badge`, `sonner`, `card` y `dropdown-menu`.
- **IMPLEMENT**: Reemplazar `src/index.css` por el bloque de `DESIGN.md` §6 (tokens `@theme` y variables `:root`). Instalar `@fontsource/inter` y `@fontsource/montserrat` [P] (fuentes servidas por el propio servidor; sin Google Fonts, por China) e importarlas en `main.tsx`. Agregar la regla de `prefers-reduced-motion` de `DESIGN.md` §6.
- **MIRROR**: `DESIGN.md` §6.
- **IMPORTS**: `import '@fontsource/inter/400.css'` y los demás pesos usados.
- **GOTCHA**: Las pantallas sin ruta todavía usan HEX escritos a mano y pueden seguir así hasta su fase. Los componentes nuevos usan **solo** tokens.
- **VALIDATE**: `npm run build`; un `Button` de shadcn se ve morado `#31275B` con foco `#B23F00`.

### Paso 1.3 — Esqueleto del backend

#### Task 4: Prueba del arranque y del endpoint de salud (primero falla)
- **ACTION**: Con el proyecto de la Task 5 recién generado (solo compila), escribir `SaludControllerTest` (`@WebMvcTest`) y `AplicacionArrancaIT` (`@SpringBootTest` + Testcontainers).
- **IMPLEMENT**: `GET /api/v1/salud` → 200 `{"estado":"ok"}` sin autenticación; la aplicación arranca contra `postgres:17`.
- **MIRROR**: TEST_STRUCTURE.
- **GOTCHA**: El manual dice `/api/salud`; el contrato usa la base `/api/v1`. **[P]** Usar `/api/v1/salud`, y `/actuator/health` solo para el `healthcheck` de Docker. Anotarlo en el PR.
- **VALIDATE**: `./gradlew test` falla con 404 antes de la Task 6.

#### Task 5: Crear el proyecto Gradle (D1 y D8 aprobadas)
- **ACTION**: Generar en `backend/` con Spring Initializr (Gradle Groovy, Java 21, Spring Boot 4.1.x estable (ADR-0016), grupo `com.decokasa`, artefacto `seguimiento`, paquete `com.decokasa.seguimiento`).
- **IMPLEMENT**: Dependencias: `web`, `security`, `oauth2-resource-server`, `data-jpa`, `validation`, `flyway-core` + `flyway-database-postgresql`, `postgresql`, `actuator`, `springdoc-openapi-starter-webmvc-ui`. Pruebas: `spring-boot-starter-test`, `spring-security-test`, `spring-boot-testcontainers`, `testcontainers:postgresql`, `testcontainers:junit-jupiter`, `archunit-junit5`. Plugin `jacoco` con verificación de líneas ≥ 80 % (puerta de la fase 1). **Sin `websocket`** (D1: queda para la fase 5).
- **MIRROR**: ADR-0010.
- **GOTCHA**: Gradle no está instalado globalmente: usar siempre `./gradlew` (en PowerShell, `.\gradlew.bat`). Java 21 sí está instalado.
- **VALIDATE**: `cd backend && ./gradlew build -x test`.

#### Task 6: Estructura hexagonal, configuración y salud
- **ACTION**: Crear los paquetes de `arquitectura.md` §5 para `compartido`, `usuarios` y `configuracion`, y `SeguimientoApplication.java`.
- **IMPLEMENT**:
  - `compartido.domain`: `ErrorDeDominio` (abstracta, con `codigo`), `CodigoRol` (record con formato `^[a-z_]{3,40}$`), `CodigoPais` (enum `EC`, `CO`).
  - `compartido.application.port.out.Reloj` (`Instant ahora()`; zona `America/Guayaquil`).
  - `compartido.adapter.out.RelojSistema`; `RelojFijo` en las pruebas.
  - `compartido.adapter.in.web.ManejadorErrores` (`@RestControllerAdvice` → `ProblemDetail` con `codigo` y `errores`).
  - `configuracion.SaludController` en `/api/v1/salud`.
  - `application.yml`: datasource por variables (`DB_URL`, `DB_USUARIO`, `DB_CONTRASENA`), `ddl-auto: validate`, `open-in-view: false`, `spring.jackson.time-zone: America/Guayaquil`.
- **MIRROR**: NAMING_CONVENTION (backend), ERROR_HANDLING.
- **GOTCHA**: Nada de `org.springframework` en `domain` ni en `application` (R1, R2).
- **VALIDATE**: Las pruebas de la Task 4 pasan.

#### Task 7: Pruebas de arquitectura con ArchUnit [P] (la hexagonal está aprobada; ArchUnit sigue como propuesta de ADR-0011)
- **ACTION**: `arquitectura/ReglasArquitecturaTest.java`.
- **IMPLEMENT**: R1 (sin Spring, JPA, Hibernate, PostgreSQL, Google ni JWT en `..domain..` y `..application..`), R2 (sin anotaciones de Spring en esas capas), R3 (`@Entity` solo en `..adapter.out.persistence..`), R4 (un `domain` no importa el `domain` de otro módulo salvo `compartido`), R9 (sin ciclos, con `slices().matching("com.decokasa.seguimiento.(*)..")`).
- **MIRROR**: `arquitectura.md` §2.
- **GOTCHA**: Con un solo módulo de negocio, R4 y R9 pasan trivialmente; quedan escritas para las fases siguientes.
- **VALIDATE**: `./gradlew test --tests '*ReglasArquitectura*'` en verde.

### Paso 1.4 — Docker

#### Task 8: Compose con PostgreSQL, backend y frontend
- **ACTION**: `backend/Dockerfile` multietapa (`eclipse-temurin:21-jdk` compila con `./gradlew bootJar`; `eclipse-temurin:21-jre` ejecuta con un usuario sin privilegios). `frontend/Dockerfile` (Node 24 compila; `nginx:alpine` sirve `dist/` y hace proxy de `/api` al servicio `backend`). `.env.example` en la raíz.
- **IMPLEMENT**: Servicios `postgres` (sin cambios), `backend` (`depends_on: postgres: condition: service_healthy`, `healthcheck` a `/actuator/health`) y `frontend` (`depends_on: backend`, puerto 3000→80). Variables en `.env.example`: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRETO` (mínimo 32 bytes), `JWT_DURACION_ACCESO=15m` y `JWT_DURACION_RENOVACION=8h` (D3, aprobadas), `BLOQUEO_INTENTOS_MAXIMOS=5` y `BLOQUEO_DURACION=15m` (D2), `CORS_ORIGENES=http://localhost:3000`, `SEMILLA_HABILITADA=false`, `SEMILLA_ARCHIVO` y `SEMILLA_CONTRASENA_TEMPORAL`.
- **MIRROR**: DOCKER_PATTERN.
- **GOTCHA**: `JWT_SECRETO` y `POSTGRES_PASSWORD` con `:?` (obligatorias). Nunca valores reales en `.env.example`; el `.gitignore` ya ignora `.env*` salvo `.env.example`.
- **VALIDATE**: `docker compose up -d --build` → tres servicios `healthy`; `curl http://localhost:8080/api/v1/salud` → 200; `http://localhost:3000` muestra el frontend.

### Paso 1.5 — Migración inicial

#### Task 9: Prueba de migración y migración V1 (D2, D3, D4)
- **ACTION**: Primero `MigracionesIT` (Testcontainers con `postgres:17`; Flyway aplica sobre una base vacía y la prueba revisa con JDBC): esquema, semilla y restricciones. Después, `V1__usuarios_roles_paises.sql`. La validación de Hibernate contra el esquema llega con las entidades (T16).
- **IMPLEMENT**: Tablas de `modelo-datos.md` §3.1: `pais`, `rol`, `rol_permiso`, `usuario` (sin `rol_id`; con `intentos_fallidos` y `bloqueado_hasta`, D2), `usuario_rol` (D4: PK `usuario_id` + `rol_id`, `asignado_en`), `usuario_pais` y `token_renovacion` (`familia_id`, `token_hash`, `creado_en`, `expira_en`, `revocado_en`; D3). Índices únicos `lower(correo)`, `lower(usuario)` y `telefono`; índices en cada FK que no cubra una PK y en `familia_id`. `CHECK` de formato en `pais.codigo` y `rol.codigo`, `intentos_fallidos >= 0` y `expira_en > creado_en`. FK sin `ON DELETE CASCADE`. Semilla: EC (activo), CO (inactivo) y los 7 roles; sin usuarios ni permisos.
- **MIRROR**: REPOSITORY_PATTERN, `modelo-datos.md` §1 y §4.
- **GOTCHA**: Ningún usuario real ni contraseña en la migración (D5). La lista de permisos sigue pendiente y se define con los casos de uso (T10); `rol_permiso` queda vacía. Los 7 roles usan UUIDv7 fijos y válidos (marca de tiempo 2026-10-12). Hecho el 2026-10-12: `MigracionesIT` con 38 pruebas.
- **VALIDATE**: `./gradlew test --tests '*MigracionesIT*'`.

#### Task 10: Semilla de países y roles; usuarios fuera de git (D5 confirmada)
- **ACTION**: `V2__semilla_paises_roles.sql`: `EC` activo y `CO` inactivo; los 7 roles (`admin`, `asistente_compras`, `marketing`, `compras_china`, `logistica`, `asistente_china` y `consulta`, D7) con sus nombres; permisos `usuarios.gestionar` y `roles.gestionar` solo para `admin` [P]. Después, la prueba `CargadorUsuariosInicialesTest` y el cargador.
- **IMPLEMENT**: `usuarios.adapter.in.arranque.CargadorUsuariosIniciales` (`ApplicationRunner`, activo solo con `decokasa.semilla.habilitada=true`): si la tabla `usuario` está vacía, lee el JSON de `SEMILLA_ARCHIVO` (lista de `{nombre, correo, usuario, telefono, rol}`), usa `SEMILLA_CONTRASENA_TEMPORAL` para todos y llama al caso de uso `CrearUsuario`, para que se apliquen las mismas validaciones. Si la tabla tiene datos, no hace nada. `backend/semilla/usuarios.ejemplo.json` con 6 usuarios **ficticios** (`admin@ejemplo.test`…). El archivo real, `usuarios.local.json`, lo prepara Richard fuera de git.
- **MIRROR**: SERVICE_PATTERN.
- **GOTCHA**: Ningún dato personal real, contraseña real ni clave en migraciones, código, pruebas, `application.yml` o `.env.example` (el repositorio es público). Las pruebas usan solo usuarios ficticios y contraseñas de prueba generadas en la propia prueba. La contraseña temporal no se escribe en los logs.
- **VALIDATE**: Con el archivo de ejemplo y la semilla habilitada, al arrancar hay 6 usuarios; un segundo arranque no los duplica.

### Paso 1.6 — Pruebas primero (deben fallar)

#### Task 11: Pruebas de dominio y de casos de uso de `usuarios`
- **ACTION**: `usuarios/domain/UsuarioTest`, `usuarios/application/IniciarSesionTest`, `CrearUsuarioTest`, `ActualizarUsuarioTest` y `CrearRolTest`, con repositorios en memoria, `RelojFijo` y un `CodificadorContrasena` falso.
- **IMPLEMENT**: Los casos de la sección "Testing Strategy": los tres identificadores, contraseña incorrecta, usuario desactivado, **usuario sin rol no puede acceder**, duplicados, solo admin y normalización del teléfono. La prueba "sin rol" se escribe contra el dominio (`Usuario` sin rol asignado → `CredencialesInvalidas`), así vale para los dos modelos de D4.
- **MIRROR**: TEST_STRUCTURE.
- **VALIDATE**: Compilan y fallan (`./gradlew test` en rojo por las clases vacías).

#### Task 12: Pruebas de bloqueo y renovación (D2, D3)
- **ACTION**: `IniciarSesionBloqueoTest` y `RenovarSesionTest`.
- **IMPLEMENT**: Bloqueo: 5 fallos → `CuentaBloqueada` durante 15 min; un acierto antes del límite reinicia el contador; con el `RelojFijo` adelantado 15 min, vuelve a entrar. Renovación: un token válido devuelve un acceso nuevo y un token de renovación nuevo, y revoca el anterior; reutilizar uno revocado → 401 y se revoca toda la familia; pasadas 8 h desde el inicio de sesión → 401 aunque se haya rotado; desactivar al usuario o cambiar su contraseña revoca sus familias.
- **VALIDATE**: Fallan.

#### Task 13: Pruebas de la API y de la seguridad
- **ACTION**: `AuthControllerIT` y `UsuarioControllerIT` (`@SpringBootTest` + `MockMvc` + Testcontainers).
- **IMPLEMENT**: Login con correo, usuario y teléfono → 200 con `tokenAcceso` y `expiraEn: 900`, y la cookie `decokasa_renovacion` con `HttpOnly`, `Secure`, `SameSite=Strict` y `Path=/api/v1/auth` (⛔ D3b); el cuerpo no lleva el token de renovación; contraseña incorrecta, usuario inexistente, usuario desactivado y **usuario sin rol** → 401 con el mismo cuerpo (`codigo: NO_AUTENTICADO`; también sin roles o con la cuenta desactivada, D4); 5 fallos → 423; `/auth/renovar` con la cookie → 200 y cookie nueva, con `Origin` ajeno → 403; `/auth/cerrar-sesion` → 204 y la cookie se borra; el JWT lleva `sub` = id y `rol`; `GET /auth/yo` devuelve `permisos`; `GET`/`POST` de `/usuarios` y `/roles` sin token → 401, con token de `marketing` → 403 `SIN_PERMISO`, con token de `admin` → 200/201; duplicado → 409 `DUPLICADO`; datos inválidos → 400 `VALIDACION` con `errores[].campo`; CORS solo para `CORS_ORIGENES`; cabeceras de seguridad presentes.
- **MIRROR**: ERROR_HANDLING (formas exactas del contrato).
- **GOTCHA**: Las respuestas deben coincidir campo por campo con `openapi.yaml`. Si algo no encaja, se cambia el contrato siguiendo su protocolo; no se cambia la implementación en silencio.
- **VALIDATE**: Fallan.

#### Task 14: Pruebas del frontend con Vitest, Testing Library y MSW
- **ACTION**: `LoginScreen.test.tsx`, `RutaProtegida.test.tsx`, `UsersManagementView.test.tsx` y `api/cliente.test.ts`.
- **IMPLEMENT**: Un login correcto navega a `/inicio`; un 401 muestra "Credenciales incorrectas" en una región `role="alert"`; un 423 muestra el aviso de bloqueo (D2); sin sesión, `/usuarios` redirige a `/login`; con rol `marketing`, `/usuarios` muestra "Sin permiso"; el formulario de usuario valida con Zod (correo; usuario ≥ 3; teléfono ≥ 7; contraseña ≥ 8) y muestra el 409 en el campo duplicado; ante un 401, el cliente HTTP llama a `/auth/renovar` (con `credentials: 'include'`, sin leer la cookie) y repite la llamada una vez (D3); al recargar la página, la sesión se recupera llamando a `/auth/renovar`; ningún token aparece en `localStorage` ni en `sessionStorage` (⛔ D3b). Cada campo tiene su `<label>` (auditoría, sección 7).
- **VALIDATE**: `npm run test` en rojo.

### Paso 1.7 — Implementar el backend

#### Task 15: Dominio y casos de uso de `usuarios`
- **ACTION**: Implementar hasta que pasen las Tasks 11 y 12.
- **IMPLEMENT**:
  - Dominio: `Usuario` (id, nombre, correo, usuario, teléfono normalizado, uno o varios roles (D4), países, activo, intentos, bloqueadoHasta; métodos `registrarFallo(ahora, politica)`, `registrarAcierto()`, `estaBloqueado(ahora)` y `puedeIniciarSesion()`, que exige activo y al menos un rol), `Rol` (código, nombre, permisos), `PoliticaBloqueo` (record: 5 intentos, 15 min; D2), `SesionRenovable` (familia, hash, vencimiento absoluto de 8 h; D3), `Credencial` y `ContrasenaNueva` (record que valida al menos 8 caracteres y **como máximo 72 bytes en UTF-8**, `valor.getBytes(StandardCharsets.UTF_8).length <= 72`, y lanza `DatoInvalido` en el campo; se usa al crear el usuario y al cambiar la contraseña). Errores: `CredencialesInvalidas`, `CuentaBloqueada`, `DatoDuplicado(campo)`, `NoEncontrado` y `SinPermiso`.
  - Puertos de entrada (`arquitectura.md` 4.1): `IniciarSesion`, `RenovarSesion` (D3), `CerrarSesion`, `ObtenerUsuarioActual`, `ConsultarUsuarios`, `CrearUsuario`, `ActualizarUsuario` (países, activo, roles y, [P], una contraseña nueva; desactivar revoca los tokens de renovación), `CrearRol`, `ActualizarRol` y `ConsultarRoles`.
  - `Usuario` expone `tieneRol(CodigoRol)` y `roles()` (D4). `puedeIniciarSesion()` y la renovación exigen activo y al menos un rol.
  - Puertos de salida: `UsuarioRepositorio`, `RolRepositorio`, `CodificadorContrasena`, `EmisorTokens`, `TokenRenovacionRepositorio` (D3) y `Reloj`.
  - La verificación "solo admin" va **dentro del caso de uso**, con el permiso del usuario que llama (R6), además de la regla de la capa web.
- **MIRROR**: SERVICE_PATTERN.
- **GOTCHA**: Para no revelar si la cuenta existe, con un identificador inexistente se ejecuta igual un `matches` de BCrypt contra un hash fijo, para que el tiempo de respuesta sea similar.
- **GOTCHA (contraseña de más de 72 bytes)**: BCrypt solo procesa 72 bytes; una contraseña más larga nunca se recorta en silencio. Al crear o cambiarla, `ContrasenaNueva` la rechaza (400 `VALIDACION`). Al iniciar sesión no puede coincidir con ninguna guardada: `IniciarSesion` responde `CredencialesInvalidas` y cuenta el intento fallido (D2) sin pasarla a BCrypt. El límite se mide en bytes UTF-8, no en caracteres (`String.length()` no sirve).
- **VALIDATE**: Pruebas de dominio y de aplicación en verde.

#### Task 16: Persistencia y seguridad (adaptadores de salida)
- **ACTION**: `UsuarioEntity`, `RolEntity` y `TokenRenovacionEntity` (D3); la relación usuario–rol como tabla de unión `usuario_rol` (D4); repositorios Spring Data y mapeadores; `BCryptCodificador` (fuerza 12 [P]; segunda barrera: si recibe más de 72 bytes en UTF-8, `codificar` lanza un error y `coincide` devuelve falso, sin depender de cómo lo trate la versión de Spring Security); `JwtEmisor` con `NimbusJwtEncoder` HS256 (claims `sub`, `roles`, `iat`, `exp`; emisor `decokasa`).
- **MIRROR**: REPOSITORY_PATTERN.
- **GOTCHA**: La búsqueda por identificador es una sola consulta: `where lower(correo)=:id or lower(usuario)=:id or telefono=:telefonoNormalizado`. `@Version` en `UsuarioEntity`; la excepción de bloqueo optimista se traduce a 409.
- **VALIDATE**: `UsuarioJpaRepositorioIT` (Testcontainers) en verde.

#### Task 17: API REST, Spring Security y composición (D3, D8; ⛔ D3b para la cookie)
- **ACTION**: `AuthController` (`/api/v1/auth/login`, `/renovar` (D3), `/cerrar-sesion` y `/yo`), `UsuarioController` y `RolController`; DTO como `record` con Bean Validation igual al contrato. Cookie de renovación con `ResponseCookie` (`httpOnly(true)`, `secure(true)`, `sameSite("Strict")`, `path("/api/v1/auth")`, `maxAge` = tiempo restante de la sesión de 8 h; ⛔ D3b); `/renovar` y `/cerrar-sesion` rechazan con 403 un `Origin` que no esté en `CORS_ORIGENES`. `configuracion.SeguridadWebConfig`: sin sesión (`STATELESS`); CSRF de Spring desactivado (el resto de la API usa Bearer; las dos rutas con cookie se protegen con `SameSite=Strict` y la verificación de `Origin`); `permitAll` también en `/api/v1/auth/cerrar-sesion`; `oauth2ResourceServer().jwt()` con un conversor que lee `rol` y carga los permisos; `permitAll` solo en `/api/v1/auth/login`, `/api/v1/auth/renovar`, `/api/v1/salud` y `/actuator/health`; CORS con `CORS_ORIGENES`; cabeceras `X-Content-Type-Options`, `Referrer-Policy` y `frame-options DENY`. `usuarios.config.UsuariosConfig` registra los casos de uso, el decorador transaccional (ADR-0012) y la `PoliticaBloqueo` leída de las propiedades. Los 401 y 403 de Spring Security también salen con formato `Problema`.
- **MIRROR**: ERROR_HANDLING, NAMING_CONVENTION.
- **GOTCHA**: `springdoc` sirve `/swagger-ui` solo con el perfil `dev` [P]. Revisar con `/ecc:springboot-security` (manual, paso 1.7).
- **VALIDATE**: `./gradlew test` completo en verde, incluido ArchUnit; `./gradlew jacocoTestCoverageVerification` ≥ 80 %.

### Paso 1.8 — Implementar el frontend

#### Task 18: Cliente HTTP, esquemas y sesión (D3; ⛔ D3b)
- **ACTION**: `api/esquemas.ts` (Zod de `Usuario`, `UsuarioActual`, `RespuestaSesion`, `Rol` y `Problema`, igual que el contrato). `api/cliente.ts` (`fetch` con base `import.meta.env.VITE_API_URL || '/api/v1'`; agrega `Authorization`; convierte `application/problem+json` en un `ErrorApi` con `codigo` y `errores`; ante un 401, renueva y repite una vez). `auth/SesionProvider.tsx` (token de acceso en memoria, usuario actual con `useQuery({ queryKey: ['yo'] })`, `iniciarSesion` y `cerrarSesion`).
- **MIRROR**: NAMING_CONVENTION (frontend).
- **GOTCHA**: El token de acceso vive solo en memoria; el de renovación, solo en la cookie `HttpOnly`, que JavaScript no puede leer. Nada en `localStorage` ni en `sessionStorage`. Al recargar la página, `SesionProvider` llama a `/auth/renovar` antes de decidir si muestra el login. `cerrarSesion` llama a `/auth/cerrar-sesion` y borra el acceso de la memoria. Las llamadas a `/auth/*` usan `credentials: 'include'`; el frontend y la API se sirven desde el mismo sitio (proxy de Vite y nginx), condición de `SameSite=Strict` (ADR-0002).
- **VALIDATE**: `api/cliente.test.ts` en verde.

#### Task 19: Router, rutas protegidas y layout
- **ACTION**: `rutas.tsx` con `createBrowserRouter`: `/login` (pública); bajo `RutaProtegida`, `/` → `/inicio`, `/inicio`, `/usuarios` y `/roles` (permisos `usuarios.gestionar` y `roles.gestionar`), y `*` → 404. `App.tsx` pasa a ser el layout (Header + Sidebar + `<Outlet/>`). `main.tsx` monta `QueryClientProvider`, `SesionProvider` y `RouterProvider`.
- **IMPLEMENT**: `RutaProtegida({ permiso? })`: sin sesión → `<Navigate to="/login" replace state={{ desde }} />`; sin permiso → `<SinPermiso />`. Las rutas usan los permisos que devuelve `/auth/yo`, pero la protección real es la del servidor.
- **GOTCHA**: Las pantallas de pedidos, bodegas, auditoría, correos y cronograma **no tienen ruta** en la fase 1 (D6). Sus archivos siguen en el repositorio y deben seguir compilando con `strict`.
- **VALIDATE**: `RutaProtegida.test.tsx` en verde.

#### Task 20: Login real
- **ACTION**: Reescribir `LoginScreen.tsx` con React Hook Form, Zod y shadcn (`Form`, `Input`, `Button`).
- **IMPLEMENT**: Campos "Correo, usuario o teléfono" y "Contraseña" con `<label>`; error en `role="alert"`; botón deshabilitado mientras se envía; después del login navega a `state.desde` o a `/inicio`. Se eliminan los botones de acceso rápido, la clave demo y el selector de país (S1, S2).
- **MIRROR**: `DESIGN.md` §4 (tamaño mínimo de texto) y §8 (foco, etiquetas).
- **GOTCHA**: Mostrar "Credenciales incorrectas" para cualquier 401; nunca indicar si el usuario existe.
- **VALIDATE**: `LoginScreen.test.tsx` en verde.

#### Task 21: Encabezado y menú sin cambio de rol
- **ACTION**: `Header.tsx`: quitar `onSwitchUserPrompt`, el chip clicable, el cambio de país y la fecha simulada; mostrar nombre, rol (`getRoleNameDisplay`) y "Cerrar sesión". `Sidebar.tsx`: `NavLink` a `/inicio`, `/usuarios` y `/roles` según los permisos. Borrar la ventana "Simulador de roles" de `App.tsx` (S2).
- **VALIDATE**: `grep -rn "onSwitchUser\|USUARIOS_DEMO\|DEMO_PASSWORD\|Demo2026" src` sin resultados.

#### Task 22: Gestión de usuarios y roles (admin) (D4)
- **ACTION**: Reescribir `UsersManagementView.tsx`: tabla shadcn con nombre, correo, usuario, teléfono, roles y estado; diálogo de alta y edición con React Hook Form + Zod; activar o desactivar. Crear `RolesManagementView.tsx`: lista, alta y edición de código, nombre y permisos.
- **IMPLEMENT**: `useMutation` + `invalidateQueries({ queryKey: ['usuarios'] })`; un 409 `DUPLICADO` se asigna al campo con `setError`. Los roles se eligen con una selección múltiple que exige al menos uno (D4; `minItems: 1` del contrato). El esquema Zod usa los mismos límites que la base y la API (`modelo-datos.md` 3.1, "Límites de campos"); la contraseña se valida en bytes con `.refine((v) => new TextEncoder().encode(v).length <= 72, 'Máximo 72 bytes: las letras con tilde y los emoji ocupan más de uno')`; para quitar el acceso, el formulario ofrece desactivar la cuenta, no dejarla sin roles.
- **GOTCHA**: El diálogo de shadcn ya resuelve el foco, la tecla Escape y `aria-modal` (auditoría Q1).
- **VALIDATE**: `UsersManagementView.test.tsx` en verde.

#### Task 23: Limpiar los datos del demo (D6)
- **ACTION**: En `initialData.ts`, quitar `DEMO_PASSWORD`, `USUARIOS_DEMO`, `PEDIDOS_INICIALES`, `BODEGAS_CATALOGO_INICIAL` y `NOTIFICACIONES_GMAIL_INICIALES`. Borrar `HelpView.tsx` y `utils/translations.ts`.
- **GOTCHA**: `ETAPAS_FLUJO_A/B` y `buildStagesForOrder` se quedan (los importan `orderState.ts` y `OrdersListView.tsx`). Los datos ya publicados siguen en el historial de git; esto no los borra de GitHub (S5).
- **VALIDATE**: `npm run lint` y `npm run build` sin errores.

#### Task 24: Dashboard vacío por rol (D7: `consulta` se conserva)
- **ACTION**: `components/dashboards/DashboardRol.tsx`.
- **IMPLEMENT**: Según `usuario.rol`: admin, asistente_compras ("Revisión"), marketing ("Codificación"), compras_china, logistica, asistente_china y consulta (los dos con el dashboard "Consulta"; alcance ⏸). Cualquier otro rol creado por el admin usa "Consulta". Cada uno muestra el título, el nombre del usuario y el estado vacío "Aún no hay pedidos; las bandejas llegan en la fase 2". Sin datos simulados (HU 03).
- **VALIDATE**: Una prueba comprueba que cada rol ve su título.

### Paso 1.9 — Revisión
- **Task 25**: `/code-review`; agentes `ecc:java-reviewer` sobre `backend/` y `ecc:typescript-reviewer` sobre `frontend/src/`; `/security-review`, **obligatorio** según el manual. Sin hallazgos críticos ni altos.

### Paso 1.10 — Verificación
- **Task 26**: `/ecc:springboot-verification`, `/ecc:verification-loop`, `/ecc:test-coverage` (backend ≥ 80 %) y `/run` con los 6 usuarios del archivo de semilla local.

### Paso 1.11 — Cierre
- **Task 27**: Actualizar `CLAUDE.md` (comandos del backend, `npm run test`, alias `@/` → `src/`, estado); registrar D1 a D8 en sus ADR; marcar la fase 1 `complete` en el PRD; pasos G y H. Richard hace el commit, el push y el PR hacia `develop`.

---

## Testing Strategy

### Unit Tests (backend, dominio y aplicación)

| Test | Input | Expected Output | Edge Case? |
| --- | --- | --- | --- |
| Login por correo | `Admin@Ejemplo.test` + contraseña correcta | Sesión con rol `admin` | Mayúsculas |
| Login por usuario | `admin1` | Sesión | |
| Login por teléfono | `+593 99 000 0001`, guardado como `+593990000001` | Sesión | Espacios |
| Contraseña incorrecta | Contraseña mala | `CredencialesInvalidas`; `intentos_fallidos` + 1 (D2) | |
| Contraseña nueva de 72 bytes | 36 veces "ñ" (36 caracteres, 72 bytes) | Se acepta y luego inicia sesión con ella | Límite exacto en bytes |
| Contraseña nueva de 73 bytes | 36 veces "ñ" + "a" (37 caracteres, 73 bytes) | 400 `VALIDACION` en `contrasenaInicial`; no se guarda nada | Pasa `maxLength: 72` en caracteres, pero no el límite en bytes |
| Contraseña nueva con emoji | 18 emoji de 4 bytes (72 bytes) y 19 (76 bytes) | Se acepta / 400 | Caracteres de 4 bytes |
| Sin recorte silencioso | Usuario con una contraseña de 72 bytes; login con esa contraseña + "x" (73 bytes) | `CredencialesInvalidas`; `intentos_fallidos` + 1 | Si se recortara a 72 bytes, entraría |
| Usuario inexistente | Identificador desconocido | `CredencialesInvalidas` (mismo error) | Enumeración de cuentas |
| Usuario desactivado | `activo = false` | `CredencialesInvalidas` | |
| Usuario sin rol | Usuario sin filas en `usuario_rol` | `CredencialesInvalidas` (no puede acceder) | Regla del manual (paso 1.6) y D4 |
| Varios roles (D4) | Usuario con `compras_china` y `logistica` | Sesión; el token lleva los dos roles y los permisos son la unión | |
| Renovar con cuenta desactivada (D4) | Token válido, `activo = false` | 401 y la familia queda revocada | |
| Renovar sin roles (D4) | Token válido, usuario sin roles | 401 y la familia queda revocada | |
| Bloqueo (D2) | 5 fallos seguidos | `CuentaBloqueada` hasta `ahora + 15m` | Un acierto en el 4.º intento reinicia |
| Desbloqueo (D2) | Reloj adelantado 15 min | Puede entrar | |
| Renovación (D3) | Token válido / revocado / vencido | Par nuevo / 401 / 401 | Reutilización |
| Crear usuario | Admin, datos válidos | Usuario con hash BCrypt, nunca la contraseña | |
| Crear usuario sin permiso | Marketing | `SinPermiso` | R6 |
| Duplicado | Correo, usuario o teléfono existente | `DatoDuplicado(campo)` | Mayúsculas en el correo |
| Asignar rol | Admin cambia el rol | Cambian los permisos efectivos | |
| Crear rol | Admin, código nuevo | Rol con permisos | Código con formato inválido |

### Edge Cases Checklist
- [ ] Identificador vacío o solo con espacios → 400 `VALIDACION`
- [ ] Contraseña de 1 MB → 400 (límite de tamaño del cuerpo)
- [ ] Tipos inválidos en el JSON → 400
- [ ] Dos ediciones simultáneas del mismo usuario → 409 (bloqueo optimista)
- [ ] Base de datos caída → 503 con `Problema`, sin traza interna
- [ ] Token manipulado, sin firma o con `alg: none` → 401
- [ ] El admin intenta desactivarse o quitarse el rol admin [P]: rechazar si es el último admin activo

---

## Validation Commands

### Static Analysis
```bash
cd frontend && npm run lint          # tsc --noEmit con strict
cd backend && ./gradlew compileJava compileTestJava
```
EXPECT: Cero errores

### Unit Tests
```bash
cd backend && ./gradlew test --tests '*usuarios.domain*' --tests '*usuarios.application*'
cd frontend && npm run test
```
EXPECT: Todo en verde

### Full Test Suite
```bash
cd backend && ./gradlew check        # pruebas, ArchUnit, Testcontainers (requiere Docker) y JaCoCo ≥ 80 %
cd frontend && npm run test && npm run build
```
EXPECT: Sin regresiones

### Database Validation
```bash
docker compose up -d postgres
cd backend && ./gradlew test --tests '*MigracionesIT*'
docker compose exec postgres psql -U decokasa -d decokasa -c "select codigo, activo from pais; select codigo from rol;"
```
EXPECT: EC activo y CO inactivo; 7 roles

### Browser Validation
```bash
docker compose up -d --build
# Abrir http://localhost:3000
```
EXPECT: Login, dashboard por rol y gestión de usuarios funcionan

### Manual Validation (puerta de salida de la fase 1)
- [ ] `docker compose up` levanta PostgreSQL, el backend y el frontend
- [ ] Los 6 usuarios entran con correo, con usuario y con teléfono
- [ ] Mirian crea un usuario y le asigna un rol; el usuario nuevo entra y ve su dashboard
- [ ] Con la sesión de Andrea Q., `/usuarios` muestra "Sin permiso", y `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/usuarios` responde 403
- [ ] Después de recargar la página, el token no aparece en `localStorage`
- [ ] Cobertura del backend ≥ 80 %
- [ ] `/security-review` sin hallazgos críticos ni altos
- [ ] David prueba el acceso desde China (si todavía no hay un servidor accesible, queda para la fase 8, como dice el manual)

---

## Acceptance Criteria
- [ ] D1 a D8 registradas antes de las tareas que dependen de ellas
- [ ] Todas las tareas completas y los comandos de validación en verde
- [ ] Pruebas escritas antes del código y en verde
- [ ] Sin errores de tipos ni de compilación
- [ ] Respuestas de la API iguales al contrato `openapi.yaml`
- [ ] Hallazgos S1, S2, S3, S7 y S8 de la auditoría resueltos
- [ ] Ningún dato personal real ni secreto agregado al repositorio

## Completion Checklist
- [ ] Reglas R1 a R9 respetadas (ArchUnit en verde)
- [ ] Errores con formato `Problema` y `codigo` estable
- [ ] Sin contraseñas, tokens ni hashes en los logs
- [ ] Pruebas con la estructura `domain` / `application` / `adapter`
- [ ] Sin valores escritos en el código: duraciones, intentos, orígenes CORS y secretos van por configuración
- [ ] `CLAUDE.md` y ADR actualizados
- [ ] Nada fuera del alcance (ver NOT Building)

## Risks

| Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- |
| El cliente pide después un solo rol por usuario (D4 la decidió el equipo, no el cliente) | Baja | Medio | Una migración nueva lo restringe (por ejemplo, índice único en `usuario_rol.usuario_id`); el dominio ya trabaja con `roles()` |
| Los datos personales del demo siguen en el historial público | Alta (ya ocurrió) | Alto | Pasar el repositorio a privado (⏸ D5b); no agregar datos nuevos |
| No consta quién escribió `requerimientos.md` 4.7 (bloqueo y JWT) | Baja | Bajo | Valores decididos por el equipo y configurables (ADR-0002) |
| Docker o Testcontainers fallan en Windows | Media | Medio | Docker 29 y Compose v5 están instalados; si fallan, ejecutar las pruebas de integración en WSL o en la CI (fase 8) |
| Las pantallas sin ruta dejan de compilar al cambiar tipos | Media | Bajo | No modificar el `Usuario` del demo; los tipos de la API viven aparte, en `api/esquemas.ts` |
| El soporte gratuito de Spring Boot 4.1 termina el 2027-07-31 | Alta (fecha conocida) | Medio | Actualizar a la versión menor vigente antes de producción (fase 8); ADR-0016 |
| XSS mientras la página está abierta (D3b) | Baja | Alto | El token de renovación queda fuera de JavaScript (cookie `HttpOnly`); acceso de 15 min solo en memoria; CSP en la fase 8; sin `dangerouslySetInnerHTML` |
| Frontend y API en dominios distintos en producción rompen `SameSite=Strict` | Baja | Medio | Servir la API bajo el mismo dominio (fase 8); si no se puede, nuevo ADR con `SameSite=None` y token anti-CSRF |

## Notes

- El demo ya está en `frontend/`: la parte del paso 1.2 del manual que dice "mueve el demo a frontend/ con `git mv`" no aplica; solo se limpian dependencias y se agregan las librerías.
- `npx tsc --noEmit --strict` sobre el demo actual da **0 errores** (comprobado el 2026-10-12), así que activar `strict` no requiere correcciones previas.
- El manual (paso 1.3) dice `/api/salud`; el contrato usa la base `/api/v1`. El plan usa `/api/v1/salud` [P].
- El contrato no define los permisos concretos ("la lista exacta se define en la fase 1"). Este plan propone `usuarios.gestionar` y `roles.gestionar` [P]; las fases siguientes agregan los suyos.
- Herramientas comprobadas en el equipo: OpenJDK 21.0.12, Docker 29.8.2, Compose v5.5.1, Node 24.21 y npm 11.19. Gradle no está instalado (se usa el wrapper).
