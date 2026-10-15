# Traspaso: corrección B2-D01, identidad entre pestañas

Fecha: 2026-10-14. Alcance exclusivo: defecto de identidad documentado en `documentos/pruebas/fase-1-informe.md`, con evidencias en `documentos/pruebas/evidencias/1-b2/`. Se conserva el resto del trabajo de `fase-1-base-acceso`.

## Implementación entregada — pendiente de pruebas de Codex

Skills aplicadas: `ecc-react-patterns` y `tanstack-query-best-practices` (cancelación y separación de cachés por sesión). Se reutilizan los componentes existentes de shadcn para el aviso y se aplica el criterio de `security-review` sobre aislamiento de identidad, sin ejecutar una auditoría ni la verificación de 1.B3.

Archivos modificados por esta corrección:

- `frontend/src/auth/coordinadorSesion.ts`: atiende los avisos de login también con sesión abierta; invalida la generación anterior, retira su token y aborta sus peticiones antes de recuperar la cookie vigente. Renovación, login y cierre comparten el bloqueo `decokasa-renovacion`. Las renovaciones pendientes conservan su generación y no pueden restaurar una identidad descartada. Si una renovación descubre una cuenta diferente sin haber recibido el aviso, aplica igualmente el aislamiento y avisa. Los mensajes entre pestañas siguen sin tokens ni datos de usuario.
- `frontend/src/auth/SesionProvider.tsx`: cancela consultas y vacía QueryClient de forma inmediata al invalidar la sesión; rechaza resultados y reintentos de operaciones anteriores. Cambia la clave del árbol de rutas al cambiar la generación, desmontando formularios, diálogos y estado local previo. Muestra un aviso persistente de cambio de sesión, recuperación en curso y cuenta vigente; la persona puede cerrarlo con Entendido cuando termina la recuperación. Login y cierre usan las operaciones coordinadas.
- `frontend/src/api/cliente.ts`: admite AbortSignal y conserva la cancelación como tal, sin convertirla en SIN_CONEXION ni disparar un reintento por red.
- `frontend/src/api/consultas.ts`: pasa la señal de TanStack Query al cliente y separa claves e invalidaciones de usuarios/roles por generación de sesión. Los callbacks de mutaciones antiguas no invalidan la caché de la sesión nueva.
- Este traspaso.

## Comportamiento previsto

Un nuevo login en otra pestaña retira inmediatamente la sesión mostrada hasta ese momento al recibir el aviso, cancela peticiones, descarta datos y formularios previos, y muestra el estado de recuperación. Recupera después la identidad vigente usando la cookie HttpOnly y una renovación coordinada. Muestra el nombre de la cuenta recuperada y un aviso explícito; no conserva los links, permisos o información de la cuenta anterior.

Una respuesta de renovación que terminó después de un cierre o de otro cambio de sesión no reabre la sesión anterior. Una llamada de negocio que empezó con Admin no se repite con Marketing después de un 401. Las respuestas que ya no pertenecen a la generación actual se descartan incluso si el transporte no pudo abortarlas a tiempo.

Las operaciones que cambian cookies se dejan terminar dentro de su bloqueo: abortar una respuesta de login/renovación podría ocultar que el navegador ya recibió una cookie nueva. Se descarta su resultado si quedó obsoleto y se recupera lo vigente. Las peticiones de negocio sí reciben AbortSignal. Cancelar una escritura no revierte una transacción que el servidor ya confirmó; esta corrección evita usar su resultado o repetirla con otra cuenta.

Los tokens permanecen solo en memoria. No se cambian cookie, atributos, duración, contrato de API, backend, almacenamiento persistente ni el acceso local de desarrollo existente. Se conserva el cierre compartido y la renovación agrupada dentro de la pestaña y serializada entre pestañas.

## Validación reservada a Codex, parte B

Por instrucción explícita de la parte A, **no se escribieron, modificaron ni ejecutaron pruebas; no se ejecutaron lint, builds, comprobaciones de tipos ni verificaciones**. No se cambió la suite fallida, los fixtures de pruebas ni las evidencias históricas. No se afirma que la corrección esté validada. No hubo commit ni push ni avance a 1.B3.

Pendientes para Codex:

1. Repetir Vitest/Testing Library/MSW con el acceso local desactivado, como documenta 1.B2. Comprobar recuperación, 401/403, fallos temporales, cierre compartido y que una renovación normal del mismo usuario conserva su agrupación.
2. Repetir el escenario de identidad con API real y observar también el aviso, permisos, datos y formularios antes de la recarga. Las pruebas existentes se conservaron: el reproductor actual retiene el login de B mientras espera que A termine. Ahora ambos usan el mismo bloqueo y ese orden de espera puede producir un timeout del propio reproductor. Codex deberá adaptar en la parte B el punto de espera para ejercitar las solicitudes encoladas y la sincronización, sin convertir la sustitución silenciosa en una expectativa correcta.
3. Demorar lecturas, escrituras y sus respuestas; cambiar la sesión o cerrarla; liberar las respuestas. No deben restaurar datos, permisos, formularios ni sesión anterior, ni reintentar la escritura con otro usuario. Incluir el cambio de identidad detectado por renovación sin aviso de BroadcastChannel.
4. Comprobar cachés y mutaciones pendientes, incluyendo login del mismo usuario y login de usuario distinto, avisos sucesivos y cierre durante renovación. Solo el resultado de la generación vigente puede modificar la UI.
5. Repetir pestañas y renovaciones simultáneas con cookie real: sin reutilización accidental y sin que un login o cierre se solape con la rotación. Confirmar nuevamente cookies protegidas, tokens solo en memoria y CSP del aviso.
6. Recompilar/recrear el frontend solo cuando Codex entre en la parte B: el contenedor servido durante 1.B2 todavía contiene el artefacto anterior. No se reconstruyó durante esta entrega.

El bloqueo entre pestañas sigue dependiendo de Web Locks y los avisos de BroadcastChannel, con los fallbacks existentes. En un navegador sin esas APIs la recuperación puede detectar la identidad distinta y descartar operaciones antiguas, pero no se promete coordinación inmediata entre pestañas sin canal. Incluir esa limitación en la evidencia que corresponda, sin afirmar compatibilidad no comprobada.

El informe de 1.B2 conserva su estado histórico de fallo hasta que Codex registre la revalidación. La fase continúa abierta.
