# ADR-0002: Autenticación propia con JWT y BCrypt

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09; decisiones del equipo agregadas el 2026-10-12; correcciones A1, A2, A4, D10 y N1 el 2026-10-13)
**Estado**: accepted
**Decisores**: cliente (stack: "Login propio con roles, sin depender de Google") y equipo

## Contexto

Los usuarios entran con correo, nombre de usuario o teléfono, más contraseña (`requerimientos.md`, 4.2 #1). David trabaja desde China, donde los servicios de Google pueden estar bloqueados. El demo compara una contraseña única escrita en el código y todo lo decide en el navegador (`auditoria-demo.md`, S1 a S4).

## Decisión

El backend autentica con Spring Security, guarda las contraseñas con BCrypt y emite un JWT con los roles del usuario. Los permisos por rol y por etapa se validan en el servidor.

## Alternativas consideradas

### Google Sign-In
- **Pros**: sin contraseñas propias y con el Workspace que la empresa ya tiene.
- **Contras**: depende de Google para entrar.
- **Por qué no**: desde China no se puede depender de Google.

### Sesión con cookie en el servidor
- **Pros**: se revoca fácil.
- **Contras**: no es lo que propone el documento del cliente.
- **Por qué no**: el stack del cliente dice JWT. Se puede revisar en otro ADR.

## Consecuencias

### Positivas
- El acceso no depende de Google.
- Corrige los hallazgos críticos S1 a S4 del demo.

### Negativas
- El equipo mantiene la gestión de contraseñas.

### Riesgos
- Robo del token. Mitigación: acceso de vida corta, renovación con rotación y almacenamiento seguro (abajo).

## Decisiones del equipo (2026-10-12, aprobadas por Richard)

Valores concretos de lo que `requerimientos.md` 4.7 pide sin número ("JWT de vida corta y bloqueo tras varios intentos fallidos"). No son respuesta del cliente.

- **Bloqueo:** tras **5 intentos fallidos** de contraseña, la cuenta queda bloqueada **15 minutos**. Un inicio de sesión correcto reinicia el contador. Mientras dura el bloqueo, la API responde 423 `CUENTA_BLOQUEADA`. Los dos valores son configurables.
- **JWT de acceso:** dura **15 minutos**. Lleva el id y los roles del usuario (uno o varios, `requerimientos.md` 4.7); los permisos se validan siempre en el servidor.
- **Cuentas sin roles o desactivadas** (decisión del equipo, 2026-10-12): no inician ni renuevan sesión. Al desactivar una cuenta se revocan sus tokens de renovación; el usuario no se borra y su historial se conserva.
- **Renovación de la sesión:** hasta **8 horas** contadas desde el inicio de sesión (límite absoluto, no se alarga con el uso). Cada renovación entrega un token de renovación nuevo y revoca el anterior (rotación). Pasadas las 8 horas, hay que volver a iniciar sesión.

## Autorización con el estado actual del usuario (corrección A1, 2026-10-13)

La revisión de seguridad del paso 1.7 (`/ecc:springboot-security`, 2026-10-13) encontró que la gestión de usuarios y roles se autorizaba solo con los roles del JWT (hallazgo A1, gravedad alta). Un token de acceso no se puede revocar y sigue vigente hasta 15 minutos, así que un admin desactivado podía reactivarse con su token anterior. La corrección está **implementada y probada** (2026-10-13); la regla "solo el admin" ya estaba aprobada y no cambia.

- **Administración** (`/usuarios` y `/roles`): el caso de uso decide con el estado actual del solicitante en la base: cuenta activa, al menos un rol y el rol `admin`. Los roles del JWT solo son un primer filtro en la capa web.
- **JWT anterior de un admin desactivado o al que se le quitó `admin`:** no permite administrar (403 `SIN_PERMISO`). No puede reactivarse, recuperar el rol `admin`, crear otro administrador, convertir a otro en admin ni consultar o cambiar usuarios y roles.
- **Si la cuenta sigue activa y conserva otros roles:** puede renovar la sesión, y el token nuevo lleva solo sus roles actuales.
- **Cuenta desactivada o sin roles:** no puede renovar (401 y se revoca la familia), como ya decía este ADR.
- **Admin recién nombrado:** su JWT anterior no incluye `admin`, así que la capa web le responde 403 hasta que renueve la sesión (como máximo 15 minutos, o al recargar la página). Comprobado en el código (`SeguridadWebConfig`); todavía sin prueba específica.
- **Usuarios iniciales (ADR-0015):** el cargador de arranque los crea sin un admin que autorice, con `CrearUsuarioCasoDeUso.paraCargaInicial`. Esa vía no es un bean y ningún controlador la usa.
- **Código de respuesta (aprobado por Richard el 2026-10-13):** una operación administrativa con el token anterior de alguien que fue desactivado o perdió el rol `admin` responde **403 `SIN_PERMISO`**, no 401.
- **[P] Fases siguientes:** las acciones sobre pedidos y etapas deberían seguir el mismo criterio; se decide con sus casos de uso.

## Consumo atómico del token de renovación (corrección A2, 2026-10-13)

La misma revisión encontró una carrera en `/auth/renovar` (hallazgo A2, gravedad media). Cada solicitud leía el token, comprobaba que no estuviera revocado y después lo revocaba, sin bloqueo. Dos renovaciones simultáneas con la misma cookie emitían **dos sucesores válidos**, y la reutilización no se detectaba. Una prueba con PostgreSQL y dos solicitudes concurrentes lo reprodujo: las dos respondieron 200. La corrección está **implementada y probada** (2026-10-13).

- **Consumo atómico:** la rotación marca el token como revocado con una sola sentencia condicional (`UPDATE … WHERE id = :id AND revocado_en IS NULL`). PostgreSQL bloquea la fila, y la segunda solicitud, al obtenerla, vuelve a evaluar la condición con el valor ya confirmado. Solo una solicitud consume el token y solo ella emite un sucesor.
- **La que pierde cuenta como reutilización:** responde 401 y revoca toda la familia, incluido el sucesor que acaba de emitir la otra. Es la política de detección de reutilización ya aprobada en este ADR.
- **Sin margen de reutilización:** no hay ningún intervalo en el que un token ya rotado se acepte otra vez.
- **El límite absoluto de 8 horas no cambia:** el sucesor hereda el `expira_en` del inicio de sesión.
- **Consecuencia aceptada:** dos renovaciones legítimas desde dos pestañas a la vez también cierran la sesión. Coordinarlas en el navegador (una sola renovación compartida) es el hallazgo A3 y queda **pendiente para el paso 1.8**.

## Conteo de intentos fallidos seguro ante concurrencia (corrección A4, 2026-10-13)

La revisión también encontró que el bloqueo por intentos fallidos se podía esquivar con solicitudes simultáneas (hallazgo A4, gravedad media). Cada login leía al usuario sin bloqueo, sumaba el fallo y lo guardaba con bloqueo optimista (`@Version`). Con intentos simultáneos solo uno confirmaba; los demás chocaban al confirmar, respondían 500 y su fallo no quedaba guardado. Una prueba con PostgreSQL y 8 logins fallidos a la vez lo reprodujo: 7 respuestas 500 y un solo fallo contado. La corrección está **implementada y probada** (2026-10-13).

- **Contador sobre la fila bloqueada:** después de comparar la contraseña con BCrypt, el login bloquea la fila del usuario y la relee con lo último confirmado (`SELECT … FOR UPDATE`). Luego vuelve a comprobar el bloqueo de la cuenta y guarda el fallo o el acierto sobre ese estado. Los intentos simultáneos de una misma cuenta esperan su turno.
- **Cada intento procesado antes del bloqueo se cuenta**, y los que llegan después respetan el bloqueo (423), incluso con la contraseña correcta. Ningún intento se pierde en un conflicto ni se responde 500, y ningún conflicto se oculta con una respuesta correcta.
- **La política D2 no cambia:** 5 fallos bloquean la cuenta 15 minutos.
- **BCrypt se calcula antes de bloquear la fila**, así el bloqueo dura solo lo que tarda actualizar el contador.
- **Revalidación sobre el estado releído (2026-10-13):** antes de emitir tokens, el login vuelve a comprobar, sobre la fila bloqueada y releída, que la cuenta esté activa y tenga al menos un rol (si no, 401 sin tokens). El JWT lleva los roles releídos.
- **Cambio de hash entre la lectura y la relectura:** si el hash de la contraseña releído es distinto del leído, la contraseña se compara de nuevo con el hash vigente antes de decidir entre fallo y acierto. La contraseña anterior ya no entra y cuenta como fallo; la vigente entra. Si el hash no cambió, BCrypt no se repite. Esta regla no agrega ninguna función para cambiar la contraseña.
- **Lectura de D2 confirmada por Richard (2026-10-13), sin cambiar el contrato:** cada uno de los 5 fallos responde 401 y el 5.º deja la cuenta bloqueada; mientras dura el bloqueo, cualquier intento responde 423.

Los demás hallazgos de esa revisión **siguen pendientes** y no se han corregido; están en el informe del paso 1.7. Los que tocan este ADR son: renovación desde varias pestañas (A3, paso 1.8), falta de límite por IP (M1), contraseña temporal común (M3), falta de CSP (M4), último admin (B1), registro de eventos de seguridad (B2), `aud` y `jti` del JWT (B3), HSTS (B4) y el 423 que revela la cuenta (I1).

## Versión administrativa del usuario y contadores del login (D10, aprobado por Richard el 2026-10-13)

Cierra la parte de M2 que la revisión de seguridad del paso 1.7 dejó abierta: dos ediciones simultáneas de un usuario se sobrescribían o respondían 500, y un login (que cambia el contador de intentos) podía hacer fallar una edición del admin. Decisión del equipo, no respuesta del cliente.

- **`Usuario.version`** (contrato 0.5.0) aparece en todas las respuestas que devuelven un usuario, incluidas `/auth/login`, `/auth/renovar` y `/auth/yo`. Empieza en 0 y sube con cada cambio que guarda el admin.
- **`PATCH /usuarios/{usuarioId}` exige `version`.** Si no es la vigente: **409 `VERSION_DESACTUALIZADA`** (no es `DUPLICADO` ni `ETAPA_CAMBIO`), sin `errores` y **sin aplicar ningún cambio**; al desactivar, tampoco se revocan las sesiones. Si además hubiera un dato repetido, gana `VERSION_DESACTUALIZADA`.
- **Versión inválida:** ausente, negativa, con decimales o que no sea un entero (por ejemplo, el texto `"3"`): 400 `VALIDACION` con el campo `version`. Un cuerpo con solo `version`, también 400.
- **Ediciones simultáneas:** la edición bloquea y relee la fila del usuario (el mismo `SELECT … FOR UPDATE` del login, A4) y compara la versión antes de cambiar nada. De dos ediciones con la misma versión, una se guarda y la otra recibe 409. `@Version` sigue como red de seguridad: un conflicto optimista también se traduce a 409 `VERSION_DESACTUALIZADA`, nunca a 500.
- **Contadores fuera de la versión:** `intentos_fallidos` y `bloqueado_hasta` los escribe solo el login, con una actualización propia que no incrementa `version`. Las ediciones del admin no los escriben, así que no pueden borrar intentos ni quitar un bloqueo con datos viejos. Un login no hace fallar una edición, y una edición no hace perder intentos. La política D2 no cambia.
- **Respuesta del `PATCH`:** lleva la versión que quedó guardada en la base.
- **Pendiente explícito:** los **roles** no tienen versión. `PUT /roles/{rolId}` sigue con "gana el último" y harían falta una migración y un contrato propios. No se da por corregido.

## Orden de bloqueos entre desactivar y renovar (corrección N1, 2026-10-13)

La revisión final del paso 1.7 planteó un posible interbloqueo entre desactivar a un usuario (bloquea primero al usuario y después sus tokens) y renovar su sesión (bloqueaba primero el token). Se investigó con una prueba que fuerza el orden exacto de las dos transacciones sobre PostgreSQL.

- **Lo previsto no ocurre.** Hibernate pide `FOR NO KEY UPDATE` para `PESSIMISTIC_WRITE`, y ese bloqueo no choca con el `FOR KEY SHARE` que toma la clave foránea al insertar el token sucesor. No hubo interbloqueo en ninguno de los dos órdenes.
- **Sí había un fallo real, causado por el mismo orden distinto de los bloqueos:**
  - Si la renovación consumía el token antes, su sucesor se confirmaba después de que empezara la revocación de la desactivación. Esa revocación no lo veía: **quedaba un token de renovación vigente en una cuenta desactivada**. No servía para renovar, porque la renovación siguiente comprueba que la cuenta esté activa, pero rompía la regla de que desactivar revoca todas las sesiones.
  - Si la desactivación bloqueaba antes, la renovación leía la cuenta todavía activa y **entregaba una sesión nueva**.
- **Corrección:** la renovación bloquea y relee al usuario (`buscarParaActualizar`) antes de tocar sus tokens. Desactivación, login y renovación siguen ahora el mismo orden: primero el usuario, después los tokens.
  - Si la desactivación llega primero, la renovación espera y responde 401.
  - Si la renovación llega primero, la desactivación espera y revoca también el sucesor.
  - En los dos casos la cuenta queda desactivada y sin ningún token de renovación vigente.
- Se conservan A1, A2 (dos renovaciones simultáneas ahora se ordenan también por la fila del usuario, y la segunda sigue contando como reutilización), A4 y D10.

## Almacenamiento seguro del token de renovación (D3b, aprobado por el equipo)

Revisado el 2026-10-12 y **aprobado por Richard el 2026-10-12** (decisión del equipo, no respuesta del cliente). Cambia el contrato: `/auth/login`, `/auth/renovar` y el nuevo `/auth/cerrar-sesion` (`openapi.yaml` 0.4.0).

**En el navegador**

| Token | Dónde se guarda | Por qué |
| --- | --- | --- |
| Acceso (15 min) | Solo en memoria (variable de la aplicación) | Nunca en `localStorage` ni `sessionStorage`: un script inyectado (XSS) podría leerlo |
| Renovación (hasta 8 h) | Cookie `decokasa_renovacion` con `HttpOnly`, `Secure`, `SameSite=Strict` y `Path=/api/v1/auth` | JavaScript no puede leerla; el navegador solo la envía a las rutas de sesión y solo desde el mismo sitio |

- Al recargar la página, el frontend llama a `/auth/renovar` para obtener un acceso nuevo; no necesita guardar nada.
- El token de acceso nunca viaja en cookie. Por eso el resto de la API (que usa `Authorization: Bearer`) no queda expuesta a CSRF.

**En el servidor**

- Solo se guarda el hash SHA-256 del token de renovación (`token_renovacion.token_hash`), nunca el valor.
- Cada inicio de sesión abre una **familia** (`familia_id`) con su vencimiento absoluto (`expira_en` = inicio + 8 h). Las rotaciones heredan ese vencimiento.
- **Detección de reutilización:** si llega un token ya revocado, se revoca toda la familia y se responde 401. Indica que alguien copió el token. También cuenta como reutilización perder la carrera contra una renovación simultánea con el mismo token (consumo atómico, corrección A2).
- Cerrar sesión revoca la familia del token; desactivar al usuario revoca todas sus familias. **Cambiar la contraseña todavía no está implementado** (no hay ninguna operación para hacerlo; la recuperación de contraseña sigue TBD): cuando exista, deberá revocar todas sus familias.
- `/auth/renovar` y `/auth/cerrar-sesion` comprueban que la cabecera `Origin` esté en la lista de orígenes permitidos (`CORS_ORIGENES`), como defensa adicional contra CSRF. Si la cabecera falta o no está en la lista, responden 403 `SIN_PERMISO` (**aprobado por Richard el 2026-10-13**): los navegadores siempre envían `Origin` en un `POST` hecho con `fetch`.
- La cookie se envía al iniciar sesión y en cada renovación, con `Max-Age` igual al tiempo que queda hasta el fin de la sesión de 8 horas. Al cerrar sesión se envía vacía con `Max-Age=0`.

**Alternativas descartadas**

- *Renovación en `sessionStorage` o `localStorage`:* un XSS podría robar una sesión de hasta 8 horas.
- *Renovación solo en memoria:* es segura, pero cada recarga de la página obliga a iniciar sesión de nuevo.

**Condiciones y riesgos**

- `SameSite=Strict` exige que el frontend y la API se sirvan desde el **mismo sitio**. En desarrollo lo resuelve el proxy de Vite; en Docker, nginx; en producción (fase 8) hay que mantener la API bajo el mismo dominio. Si algún día fueran dominios distintos, haría falta `SameSite=None` y un token anti-CSRF (otro ADR).
- Un XSS todavía podría actuar mientras la página está abierta, pero no podría llevarse el token de renovación. Mitigación: CSP en la fase 8 y nada de `dangerouslySetInnerHTML`.
- `Secure` funciona en `http://localhost` porque los navegadores lo tratan como contexto seguro.

## Diferencias entre fuentes (sin resolver)

- `requerimientos.md` 4.7 incluye el bloqueo y el JWT de vida corta, pero versiones anteriores de este ADR y `arquitectura.md` los trataban como propuesta que el cliente no pidió. No consta si el texto de 4.7 lo escribió el cliente o el equipo. Los valores de arriba los fijó el equipo.

## Pendiente del cliente

- Recuperación de contraseña por correo: sin definir.
