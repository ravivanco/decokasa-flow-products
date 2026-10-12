# ADR-0002: Autenticación propia con JWT y BCrypt

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09; decisiones del equipo agregadas el 2026-10-12)
**Estado**: accepted
**Decisores**: cliente (stack: "Login propio con roles, sin depender de Google") y equipo

## Contexto

Los usuarios entran con correo, nombre de usuario o teléfono, más contraseña (`requerimientos.md`, 4.2 #1). David trabaja desde China, donde los servicios de Google pueden estar bloqueados. El demo compara una contraseña única escrita en el código y todo lo decide en el navegador (`auditoria-demo.md`, S1 a S4).

## Decisión

El backend autentica con Spring Security, guarda las contraseñas con BCrypt y emite un JWT con el rol del usuario. Los permisos por rol y por etapa se validan en el servidor.

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
- **JWT de acceso:** dura **15 minutos**. Lleva el id y el rol del usuario; los permisos se validan siempre en el servidor.
- **Renovación de la sesión:** hasta **8 horas** contadas desde el inicio de sesión (límite absoluto, no se alarga con el uso). Cada renovación entrega un token de renovación nuevo y revoca el anterior (rotación). Pasadas las 8 horas, hay que volver a iniciar sesión.

## Almacenamiento seguro del token de renovación (propuesta, pendiente de aprobación)

Revisado el 2026-10-12. Cambia el contrato (`/auth/login`, `/auth/renovar` y un nuevo `/auth/cerrar-sesion`), por eso se deja como propuesta hasta que el equipo la apruebe.

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
- **Detección de reutilización:** si llega un token ya revocado, se revoca toda la familia y se responde 401. Indica que alguien copió el token.
- Cerrar sesión, desactivar al usuario o cambiarle la contraseña revoca todas sus familias.
- `/auth/renovar` y `/auth/cerrar-sesion` comprueban que la cabecera `Origin` esté en la lista de orígenes permitidos (defensa adicional contra CSRF).

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
