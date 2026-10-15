# Informe del paso 1.8: frontend de acceso (fase 1)

Fecha: 2026-10-13. Rama: `fase-1-base-acceso` (sobre `0fe8fcc`). Sin commit ni push: los hace Richard.

## Resumen

El frontend deja el demo en memoria y usa la API real:

- **Acceso y sesión:**
  - login con correo, usuario o teléfono;
  - token de acceso solo en memoria;
  - recuperación, renovación y cierre de la sesión con la cookie `HttpOnly` (D3b).
- **Rutas:** rutas protegidas y un dashboard vacío por rol.
- **Administración:** gestión de usuarios y roles para el admin.
- **Seguridad:**
  - coordinación de renovaciones entre pestañas (A3);
  - CSP (M4).
- **Demo retirado:** se quitaron del acceso real la contraseña demo, los ingresos rápidos y el cambio de usuario sin autenticación.

Skills aplicadas:

- `ecc:react-patterns`: Context para la sesión, TanStack Query para los datos del servidor y HTML semántico.
- `shadcn`: Field con `data-invalid` y `aria-invalid`, FieldSet y FieldLegend para los roles, Dialog, Alert, Table, Badge y `gap-*`.
- `tanstack-query-best-practices`: fábrica de claves, invalidación tras las mutaciones, sin reintentos en 4xx ni en mutaciones.
- `ecc:react-test`: primero RED y después GREEN, con consultas por rol, `userEvent` y MSW.

## Resultados

| Verificación | Resultado |
|---|---|
| `npm run test` | 132 pruebas, 15 archivos, en verde (antes: 37 en verde y 38 en rojo del paso 1.6) |
| Fase roja | 76 pruebas fallando antes de implementar (las del paso 1.6 y las nuevas) |
| `npm run lint` (`tsc --noEmit`, strict) | sin errores |
| `npm run build` | correcto; JS de 620 kB (194 kB con gzip) y aviso de tamaño de chunk |
| `grep` de `onSwitchUser`, `USUARIOS_DEMO`, `DEMO_PASSWORD` y `Demo2026` en `src` (sin pruebas) y en `dist` | sin resultados |

### Pruebas simuladas y prueba real

- **Simuladas (Vitest + MSW, jsdom):** todo lo anterior. El servidor ficticio de `coordinadorSesion.test.ts` aplica la regla real de A2: cada renovación rota la cookie y reutilizar una ya rotada revoca la familia.
  - La prueba de control, con un bloqueo distinto por pestaña, demuestra que sin coordinación la sesión se revoca.
- **Integración real en el navegador (Chrome DevTools, 2026-10-13):** contra Docker (nginx con la CSP, backend y PostgreSQL), con usuarios ficticios `@ejemplo.test`.
  1. Sin sesión, `/usuarios` lleva a `/login`. El login con `adminprueba` vuelve a `/usuarios` y muestra la lista y los roles.
  2. No queda nada en `localStorage` ni en `sessionStorage`, y `document.cookie` está vacío (la cookie es `HttpOnly`).
  3. Al recargar, se hace una sola llamada a `/auth/renovar` (200) y la sesión se recupera sin pedir credenciales.
  4. **A3:** se abrieron dos pestañas a la vez (`/inicio` y `/roles`) que renovaron en paralelo con la misma cookie. Las dos respondieron 200 y la primera pestaña siguió renovando con 200 después, así que no hubo revocación.
  5. "Cerrar sesión" en una pestaña llevó las tres al login; luego `/auth/renovar` responde 401.
  6. El login por teléfono (`+593 99 000 0014`, Logística Prueba) funciona. En `/usuarios` aparece "Sin permiso" y el menú solo muestra Inicio.
  7. **CSP:** la consola queda sin violaciones. La primera prueba mostró una: la detección de `eval` de Zod 4. Se corrigió con `z.config({ jitless: true })`.
  - **No se probó en el navegador:** el 409 `VERSION_DESACTUALIZADA` con dos administradores reales. Está cubierto con MSW, y el backend lo cubren `VersionUsuarioIT` y `EdicionConcurrenteIT`.

### Datos ficticios agregados a la base local

- La base local tenía 0 usuarios. Se cargaron los 6 usuarios de `backend/semilla/usuarios.ejemplo.json`, todos `@ejemplo.test`, copiados en `backend/semilla/usuarios.local.json` (ignorado por Git). Se usó el cargador de arranque con la contraseña temporal ficticia de las pruebas.
- Las variables de la semilla solo se pasaron a ese comando; después el backend se recreó sin ellas. `.env` no cambió.
- No se borraron datos ni volúmenes. La última sesión (Logística Prueba) se cerró.

## Archivos

| Archivo | Acción |
|---|---|
| `frontend/src/api/cliente.ts`, `esquemas.ts`, `consultas.ts` | nuevos: cliente HTTP (`ErrorApi`, `VITE_API_URL`), esquemas Zod del contrato 0.5.0 y consultas y mutaciones |
| `frontend/src/auth/coordinadorSesion.ts` | nuevo: A3 |
| `frontend/src/auth/SesionProvider.tsx` | reescrito: sesión en memoria, recuperación, `pedir` con una renovación ante 401 |
| `frontend/src/auth/RutaProtegida.tsx`, `components/PaginasSimples.tsx` | nuevos: protección por sesión y por admin, "Sin permiso" y 404 |
| `frontend/src/rutas.tsx`, `main.tsx`, `App.tsx` | reescritos: router, proveedores y layout |
| `frontend/src/components/LoginScreen.tsx`, `Header.tsx`, `Sidebar.tsx`, `UsersManagementView.tsx` | reescritos sin datos del demo |
| `frontend/src/components/usuarios/FormularioUsuario.tsx`, `RolesManagementView.tsx`, `dashboards/DashboardRol.tsx` | nuevos |
| `frontend/src/components/ui/checkbox.tsx`, `alert.tsx` | agregados con shadcn; el foco del checkbox se ajustó a `ring-ring` |
| `frontend/src/lib/validacion.ts`, `lib/configurarZod.ts`, `src/vite-env.d.ts` | nuevos |
| `frontend/src/data/initialData.ts` | se quitaron los usuarios, la contraseña demo, las bodegas, los pedidos y los avisos; quedan las etapas y `buildStagesForOrder` |
| `frontend/src/components/HelpView.tsx`, `utils/translations.ts` | borrados (no los usaba nada más) |
| `frontend/nginx.conf` | CSP y `Permissions-Policy` (M4) |
| `frontend/.env.example` | uso de `VITE_API_URL` |
| Pruebas: `apiFicticia.ts` (`version`), `setup.ts` (`ResizeObserver`), `UsersManagementView.test.tsx`, `SesionProvider.test.tsx`, `LoginScreen.test.tsx` (orden de manejadores), `App.test.tsx`; nuevas: `api/cliente.test.ts`, `auth/coordinadorSesion.test.ts`, `RolesManagementView.test.tsx`, `DashboardRol.test.tsx`, `test/csp.test.ts`, `test/sin-demo.test.ts` | actualizadas o nuevas |
| `.claude/PRPs/plans/fase-1-base-acceso.plan.md`, `CLAUDE.md` | registro del paso |

Las pantallas de fases posteriores (pedidos, bodegas, avisos, auditoría y cronograma) siguen en `src/components` y compilan, pero no tienen ruta.

## Desvíos del plan

Están en el plan, en "Resultado del paso 1.8":

- no hay consulta `['yo']`;
- la protección usa el rol `admin`, no los permisos;
- el código de un rol es de solo lectura al editar;
- dos pruebas del paso 1.6 tenían el orden de manejadores de MSW invertido;
- hay un `ResizeObserver` vacío en las pruebas.

## Pendientes

- **Versionado de roles (D10):** sigue pendiente. Dos administradores que editan el mismo rol se pisan.
- **M1, M3, B1 a B4, I1 e I2** (revisión del paso 1.7): sin cambios.
- **`rol_permiso`:** los permisos por rol no están definidos; las rutas usan el rol `admin`.
- **⏸ del cliente:** el alcance del rol `consulta` y el paso del repositorio a privado.
- **[P] Tamaño del JS (620 kB):** propuesta de dividirlo cuando entren las pantallas de la fase 2.
- **Sin Web Locks** (navegadores antiguos): la coordinación entre pestañas no aplica. Los navegadores actuales la soportan.
- **Errores de red:** al recuperar la sesión, un error que no sea 401 también lleva al login. No se reintenta.
- **Desactivar sin confirmación:** las pruebas del paso 1.6 esperan el `PATCH` inmediato. [P] Agregar un diálogo de confirmación si el cliente lo pide.
- **Recuperación de contraseña:** por definir.
- **Archivos de `documentos/` que no son de este paso:** hay cambios en `auditoria-demo.md` y dos archivos sin seguimiento (`auditoria-implementacion-2026-10-13.md` y `skills-manual-instaladas.md`). No los modifiqué; revisa si van en tu commit.
- **Siguiente:** el paso 1.9 (revisión), que no se empezó.
