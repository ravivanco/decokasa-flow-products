# Informe de implementación: fase 1, paso 1.7 (backend de usuarios y sesión)

**Fecha:** 2026-10-13 · **Rama:** `fase-1-base-acceso` · **Plan:** `.claude/PRPs/plans/fase-1-base-acceso.plan.md` (tareas 15, 16, 17 y el cargador de la tarea 10)

## Resumen

El backend de `usuarios` queda implementado sobre la migración V1 e incluye:

- **Dominio:** usuario con uno o varios roles, contraseña de 8 caracteres a 72 bytes y teléfono normalizado.
- **Casos de uso:** inicio, renovación y cierre de sesión; alta, edición y consulta de usuarios; alta, edición y consulta de roles; y usuario actual.
- **Persistencia JPA** sobre las tablas de V1.
- **Seguridad** (ADR-0002):
  - contraseñas con BCrypt;
  - JWT HS256 de 15 minutos con los `roles`;
  - token de renovación en cookie `HttpOnly`, `Secure` y `SameSite=Strict` en `/api/v1/auth`, con rotación y límite absoluto de 8 horas;
  - bloqueo de 15 minutos tras 5 fallos;
  - `Origin` obligatoria y permitida en `/auth/renovar` y `/auth/cerrar-sesion` (403 si no cumple; aprobado el 2026-10-13).
- **API REST** de `/auth`, `/usuarios` y `/roles`. La implementación inicial seguía `openapi.yaml` 0.4.0; tras D10 el contrato vigente es **0.5.0-borrador** (sección D10).
- **Cargador de usuarios iniciales** fuera de Git (ADR-0015).

El paso 1.8 (frontend) no se tocó.

## Resultados de la validación

Resultados de la implementación inicial, antes de la corrección A1. Los de después están en la sección "Revisión de seguridad y corrección A1".

| Nivel | Resultado |
| --- | --- |
| Compilación con `-Xlint:all` | Sin avisos |
| `./gradlew cleanTest check` | **BUILD SUCCESSFUL** |
| Pruebas | **226 en total, 0 fallos y 0 omitidas**: las 82 anteriores, las 124 del paso 1.6 y 20 nuevas de este paso |
| Cobertura (JaCoCo) | Líneas **95,1 %**, ramas 72,3 % e instrucciones 95,2 %. El mínimo exigido es 80 % de líneas |
| ArchUnit (R1 a R4, R9 y sentido de las dependencias) | En verde |
| Docker (base real, sin tocar volúmenes) | El backend reconstruido queda `healthy`. Respuestas: `/salud` 200; login con usuario inexistente 401 `NO_AUTENTICADO`; `/renovar` sin `Origin` 403; con `Origin` permitido y sin cookie 401; `/usuarios` sin token 401; login a través del proxy de nginx 401 |
| OpenAPI | No se modificó en este paso; sigue válido |

## Pruebas nuevas de este paso (escritas antes del código)

| Archivo | Pruebas | Qué cubre |
| --- | --- | --- |
| `usuarios/application/CerrarSesionTest` | 2 | Revoca la familia; sin token o con uno desconocido no falla |
| `usuarios/application/ConsultasTest` | 6 | Lista y detalle de usuarios y roles solo para el admin; `/auth/yo` con la unión de los permisos; un usuario desactivado o sin roles no tiene sesión |
| `usuarios/application/ActualizarRolTest` | 4 | Reemplazo de nombre y permisos; el código es único; el rol debe existir; solo el admin |
| `usuarios/adapter/in/arranque/CargadorUsuariosInicialesTest` | 4 | Carga solo con la base vacía y con varios roles; sin contraseña temporal o sin archivo no arranca |
| `AuthControllerIT` (2 más) | 2 | `/auth/yo` con todos los roles; un token manipulado da 401 |
| `UsuarioControllerIT` (2 más) | 2 | `PUT /roles/{id}`; `GET /usuarios/{id}` y su 404 |

## Desvíos respecto del plan

1. **Cinco puertos de entrada más** (`CerrarSesion`, `ConsultarUsuarios`, `ConsultarRoles`, `ObtenerUsuarioActual` y `ActualizarRol`), con sus pruebas, para cubrir todo el contrato: `/auth/cerrar-sesion`, `/auth/yo`, `GET /usuarios`, `GET /usuarios/{id}`, `GET /roles` y `PUT /roles/{id}`.
2. **CORS en un filtro propio**, antes de la seguridad. El filtro de Spring respondía 403 con un texto plano; ahora responde con el formato `Problema` `SIN_PERMISO`, como exige `SesionHttpIT`.
3. **Transacción que confirma ante ciertos errores.** `usuarios/config/Transacciones.ejecutarConfirmandoAnte` guarda el intento fallido aunque la respuesta sea 401 o 423, y guarda la revocación de la familia aunque `/renovar` responda 401. Sin esto, el bloqueo nunca se acumularía.
4. **Cargador con lista de `roles`.** El JSON de usuarios iniciales lleva `roles` en lugar del `rol` único del plan original (D4). La semilla de países y roles ya estaba en V1.
5. **Docker:**
   - `docker-compose.yml` pasa al backend `JWT_SECRETO` (obligatoria), las duraciones aprobadas, `CORS_ORIGENES` y `SEMILLA_*`, y monta `backend/semilla` en solo lectura;
   - `.env.example` documenta esas variables;
   - el `.env` local recibió un `JWT_SECRETO` generado al azar.

## Revisión de seguridad y corrección A1 (2026-10-13)

### Revisión (hecha, solo lectura)

`/ecc:springboot-security` revisó el backend contra el manual, el plan y ADR-0002. Era la revisión de seguridad que el manual exige en el paso 1.7 y que faltaba.

**Lo que cumple:**
- BCrypt con fuerza 12 y límite de 72 bytes;
- JWT HS256 de 15 min;
- rotación con límite absoluto de 8 h y detección de reutilización;
- cookie `HttpOnly`, `Secure`, `SameSite=Strict` con `Path=/api/v1/auth`;
- `Origin` → 403;
- CORS solo para `CORS_ORIGENES`;
- cabeceras del backend;
- Actuator y Swagger cerrados.

### Implementación realizada: A1 (alto)

**Problema.** La gestión de usuarios y roles se autorizaba solo con los roles del JWT, que sigue vigente hasta 15 min. En la fase roja, por HTTP, un admin desactivado se reactivó con su token anterior (200).

**Corrección, con TDD** (`/ecc:springboot-tdd`):
- **16 pruebas nuevas, en rojo antes de implementar:** 14 de aplicación y 2 de integración. Cubren desactivar al solicitante y quitarle el rol admin. Con su token anterior no puede reactivarse, recuperar `admin`, crear otro administrador, convertir a otro en admin, desactivar a otros, ni consultar o cambiar usuarios y roles. Un solicitante que no existe en la base tampoco puede, aunque su token diga admin.
- **Una prueba más** protege la carga inicial.
- **`usuarios/application/usecase/Autorizacion`** lee el estado actual del solicitante con el puerto `UsuarioRepositorio`: activo, con al menos un rol y con `admin`. Si no, `SinPermiso` (403). Es Java puro (R2).
- **La usan los 6 casos de uso de administración:** `CrearUsuario`, `ActualizarUsuario`, `ConsultarUsuarios`, `CrearRol`, `ActualizarRol` y `ConsultarRoles`. Los tres de roles reciben ahora `UsuarioRepositorio`. `SeguridadWebConfig` no cambia: su filtro con `ROL_admin` del token sigue como primera barrera.
- **`CrearUsuarioCasoDeUso.paraCargaInicial`:** el cargador se presentaba como un admin "sistema" con un id que no existe y habría quedado bloqueado. Ahora el permiso lo da el despliegue (ADR-0015).

**Comprobado en el código: `paraCargaInicial` solo está conectado al cargador de arranque.**
- Solo aparece en el bean condicional `cargadorUsuariosIniciales` de `UsuariosConfig` (con `SEMILLA_HABILITADA=true`) y en `CrearUsuarioTest`.
- El `CrearUsuario` que arma no se registra como bean. El único bean `CrearUsuario` es el de la API, con `Autorizacion`, y es el que recibe `UsuarioController`.
- Ningún controlador usa la vía de carga inicial.

**Comportamiento resultante:**

| Situación | Resultado | Respaldo |
| --- | --- | --- |
| JWT anterior de un solicitante desactivado o que ya no tiene `admin` | No administra: 403 `SIN_PERMISO` | 16 pruebas de A1 |
| La cuenta sigue activa y conserva otros roles | Renueva; el JWT nuevo lleva solo sus roles actuales | `RenovarSesionTest` y `SesionHttpIT` (`elAccesoRenovadoLlevaLosRolesActuales`) |
| Cuenta desactivada o sin roles | No renueva: 401 y se revoca la familia | `RenovarSesionTest` y `SesionHttpIT` |
| Admin recién nombrado | La capa web le responde 403 hasta que renueve; después, su JWT incluye `admin` | Código (`SeguridadWebConfig`) y la prueba de roles al renovar; **sin prueba específica** |

**Verificación:** `./gradlew cleanTest check` → BUILD SUCCESSFUL.
- 243 pruebas, 0 fallos y 0 omitidas.
- Cobertura: líneas 95,0 %, ramas 72,6 % e instrucciones 94,9 %.
- ArchUnit en verde.
- Ninguna comprobación anterior se debilitó: donde una prueba esperaba "cero usuarios", ahora comprueba que el usuario intentado no existe.

**Aprobado por Richard (2026-10-13):** 403 `SIN_PERMISO`, y no 401, para una operación administrativa con el token anterior de quien fue desactivado o perdió `admin`.

### Implementación realizada: A2 (medio)

**Problema.** `RenovarSesionCasoDeUso` leía el token, comprobaba que no estuviera revocado y después lo revocaba con un guardado normal, sin bloqueo ni condición. Dos renovaciones simultáneas con la misma cookie podían pasar la comprobación a la vez y emitir dos sucesores válidos, y la reutilización no se detectaba.

**Corrección, con TDD** (`/ecc:springboot-tdd` y `/ecc:springboot-security`):
- **Prueba de integración nueva, `RenovacionConcurrenteIT`.**
  - Montaje: servidor real (`RANDOM_PORT`), PostgreSQL 17 con Testcontainers y dos hilos que esperan la misma señal y envían la misma cookie con `java.net.http.HttpClient`, en 25 rondas.
  - Respuestas: una 200 y otra 401.
  - Estado guardado: solo el token original y un sucesor, ninguno vigente porque la reutilización revocó la familia, una sola familia y el mismo `expira_en` del inicio de sesión.
  - **En rojo, la ronda 1 dio `[200, 200]`**: A2 quedó reproducido.
- **Prueba unitaria nueva** en `RenovarSesionTest`: otra renovación consume el token entre la lectura y el consumo, y el resultado es 401, sin sucesor y con la familia revocada. En rojo antes de la corrección. El doble en memoria ahora devuelve copias, como una lectura de la base, e implementa `consumir`.
- **Puerto `TokenRenovacionRepositorio.consumir(id, ahora)`**, implementado en `TokenRenovacionDatos` con `UPDATE … SET revocado_en = :ahora WHERE id = :id AND revocado_en IS NULL`. Es atómico en PostgreSQL: la segunda solicitud espera el bloqueo de la fila y vuelve a evaluar la condición con el valor ya confirmado.
- **`RenovarSesionCasoDeUso`** rota solo si consume el token. Si no, es una reutilización: revoca la familia y responde 401, y `ejecutarConfirmandoAnte` guarda esa revocación.
- **Lo que no cambia:** no hay margen de reutilización, el sucesor hereda el límite absoluto de 8 h y la política de revocación de la familia es la de siempre.

**Verificación:**
- `RenovacionConcurrenteIT` repetida 3 veces más (75 rondas) sin fallos.
- `./gradlew cleanTest check` → BUILD SUCCESSFUL, con 245 pruebas, 0 fallos y 0 omitidas.
- Cobertura: líneas 95,0 %, ramas 73,0 % e instrucciones 94,9 %. ArchUnit en verde.

**Consecuencia aceptada:** dos renovaciones legítimas desde dos pestañas a la vez también cierran la sesión. La coordinación en el navegador es A3 y queda pendiente para el paso 1.8.

### Implementación realizada: A4 (medio)

**Problema.** `IniciarSesionCasoDeUso` leía al usuario sin bloqueo, sumaba el fallo en memoria y lo guardaba. `UsuarioEntity` tiene `@Version`, así que con intentos simultáneos solo confirmaba uno. Los demás lanzaban `ObjectOptimisticLockingFailureException` al confirmar, respondían 500 y su fallo no quedaba guardado. Se podían probar muchas contraseñas por cada fallo contado.

**Corrección, con TDD** (`/ecc:springboot-tdd` y `/ecc:springboot-security`):
- **Prueba de integración nueva, `IntentosConcurrentesIT`.**
  - Montaje: servidor real (`RANDOM_PORT`), PostgreSQL 17 y 8 logins fallidos que salen a la vez contra la misma cuenta, en 5 rondas.
  - Respuestas: exactamente 5 respuestas 401 (los fallos contados antes del bloqueo) y 3 respuestas 423, ninguna 500.
  - Estado guardado: `intentos_fallidos = 5` y `bloqueado_hasta` a 15 minutos.
  - Después, el login con la contraseña correcta responde 423.
  - **En rojo, la ronda 1 dio `[500, 500, 500, 401, 500, 500, 500, 500]`**: un solo fallo contado, y conflictos de bloqueo optimista en el log.
- **3 pruebas unitarias nuevas** en `IniciarSesionTest`, en rojo antes de la corrección:
  - el fallo se suma a lo que guardó otra solicitud simultánea;
  - si otra solicitud bloqueó la cuenta entretanto, un fallo responde 423 sin contar de más;
  - en ese mismo caso, ni la contraseña correcta entra.
  
  El doble en memoria simula lo que otra solicitud confirma antes de que esta obtenga la fila.
- **Puerto `UsuarioRepositorio.buscarParaActualizar(id)`**, implementado en `UsuarioJpaRepositorio` con `EntityManager.refresh(entidad, LockModeType.PESSIMISTIC_WRITE)`, que es un `SELECT … FOR UPDATE`. Se usa `refresh` porque la entidad ya está cargada por la búsqueda por correo, usuario o teléfono, y una consulta devolvería esa copia anterior.
- **`IniciarSesionCasoDeUso`:**
  1. compara la contraseña con BCrypt sobre la lectura inicial, fuera del bloqueo;
  2. bloquea y relee la fila;
  3. vuelve a comprobar el bloqueo de la cuenta;
  4. guarda el fallo o el acierto sobre ese estado.
- **Lo que no cambia:** la política D2 (5 fallos, 15 minutos) y el mismo 401 para credenciales inválidas. Ningún conflicto se oculta con una respuesta correcta.

**Verificación:**
- `IntentosConcurrentesIT` repetida 3 veces más sin fallos y sin conflictos de bloqueo optimista en el log.
- `./gradlew cleanTest check` → BUILD SUCCESSFUL, con 249 pruebas, 0 fallos y 0 omitidas.
- Cobertura: líneas 94,9 %, ramas 73,2 % e instrucciones 94,9 %. ArchUnit en verde.

**Efecto aceptado y límite:**
- Los logins simultáneos de una misma cuenta se atienden uno detrás de otro durante la actualización del contador, que es breve porque BCrypt se calcula antes.
- Una edición del admin (`ActualizarUsuario`, que no bloquea la fila) que coincida con un login de la misma cuenta todavía puede chocar con `@Version` y responder 500. El intento del login no se pierde: si el admin confirma primero, el login relee esa versión. Ese caso es parte de M2.

### Comprobación de A4 contra D2 y revalidación tras releer (2026-10-13)

**Coherencia con D2 (revisión de solo lectura).**
- ADR-0002 y `openapi.yaml` dicen que la cuenta se bloquea 15 minutos "tras 5 intentos fallidos" y que se responde 423 "mientras dura el bloqueo".
- Las pruebas del paso 1.6, escritas antes de implementar y no modificadas después, fijan el comportamiento: cada uno de los 5 fallos responde 401, el 5.º deja la cuenta bloqueada y el 6.º responde 423 (`AuthControllerIT.trasCincoFallosLaCuentaSeBloqueaQuinceMinutos` y `IniciarSesionTest`).
- A4 conserva ese orden. Por eso, con 8 intentos simultáneos, 5 responden 401 y 3 responden 423.
- Richard lo confirmó como comportamiento vigente. Se aclaró el texto del plan, que abreviaba "5 fallos → 423"; **el contrato no cambió**.

**Revalidación sobre el estado releído.**

Pruebas nuevas en `IniciarSesionTest`, escritas primero. El doble guarda una instancia nueva justo antes de que el login obtenga la fila bloqueada, como haría una relectura de la base.

| Caso | Resultado esperado | Antes de corregir |
| --- | --- | --- |
| Cuenta desactivada entre la lectura y la relectura | 401, sin tokens ni sesión | En verde (el código ya revalidaba sobre el estado releído; faltaba la prueba) |
| Cuenta que pierde todos sus roles en ese intervalo | 401, sin tokens ni sesión | En verde (ídem) |
| Roles del token | Los del estado releído | En verde (ídem) |
| El hash cambió y llega la contraseña anterior | 401, cuenta como fallo y no hay tokens | **En rojo:** entraba con la contraseña anterior |
| El hash cambió y llega la contraseña vigente | Entra y el contador queda en 0 | **En rojo:** se contaba como fallo |

Las tres primeras no pasan por casualidad: si el código usara la lectura inicial, que sigue activa y con roles, emitiría tokens y fallarían.

- **Corrección en `IniciarSesionCasoDeUso`:** si el hash releído es distinto del leído, la contraseña se compara de nuevo con el vigente antes de decidir entre fallo y acierto. BCrypt sigue sin repetirse cuando el hash no cambió.
- **Sin cambios de alcance:** no se agregó ninguna función para cambiar la contraseña.
- **Verificación:** `./gradlew cleanTest check` → BUILD SUCCESSFUL, con **254 pruebas**, 0 fallos y 0 omitidas. Cobertura: líneas 94,9 %, ramas 73,4 % e instrucciones 94,9 %. ArchUnit en verde.

### Implementación realizada: M2, solo la parte de duplicados (medio)

**Problema.** El alta (y la edición) comprueba primero que el correo, el usuario y el teléfono no existan. Dos altas simultáneas con el mismo dato pasaban las dos esa comprobación; PostgreSQL guardaba una y rechazaba la otra al confirmar la transacción. Esa excepción terminaba en el 500 genérico, cuando el contrato pide 409 `DUPLICADO` con `errores[0].campo` (D9).

**Corrección, con TDD** (`/ecc:springboot-tdd`, `/ecc:springboot-security` y `/ecc:jpa-patterns`):
- **Prueba de integración nueva, `AltasConcurrentesIT`** (parametrizada por campo).
  - Montaje: servidor real, PostgreSQL 17 y dos altas que salen a la vez y comparten solo el campo probado, con los otros dos distintos; 10 rondas por campo.
  - Respuestas: un 201 y un 409 `application/problem+json`, con `codigo: DUPLICADO` y `errores[0].campo` igual al campo repetido.
  - Sin detalles internos: la respuesta no contiene "duplicate key", el nombre de la restricción, "sql" ni "constraint".
  - Estado guardado: un solo usuario con ese dato.
  - **En rojo, la ronda 1 dio `[201, 500]` en correo, usuario y teléfono.**
- **Prueba unitaria nueva, `RestriccionesUnicasUsuarioTest`.**
  - Las tres restricciones conocidas con SQLState 23505 se traducen a `correo`, `usuario` y `telefono`, con un mensaje sin nombres de la base.
  - **No** se traducen: una restricción única desconocida (`usuario_pkey`), un 23505 sin nombre, un nombre conocido con otro SQLState (23503, clave foránea) ni un `DataIntegrityViolationException` sin detalle de la base.
  - Las traducciones positivas fallaban en rojo.
- **`RestriccionesUnicasUsuario`** (adaptador de persistencia): busca en la cadena de causas la `ConstraintViolationException` de Hibernate y traduce solo esa lista cerrada de tres nombres.
- **`UsuarioJpaRepositorio.guardar`:** `saveAndFlush` en lugar de `save`, para que la violación llegue al adaptador y no al confirmar. Si la traducción aplica, lanza `DatoDuplicado`, la transacción se revierte y el manejador ya existente responde 409 con el campo. Si no aplica, vuelve a lanzar el error original (500 sin detalles, como antes).
- **Alcance:** cubre altas y ediciones, porque las dos guardan por el mismo adaptador. El contrato no cambia.

**Verificación:**
- `AltasConcurrentesIT` repetida 3 veces más, sin ningún 500. En el log del servidor aparecen las violaciones de los índices únicos (30 líneas por ejecución): las carreras llegan de verdad a la base.
- `./gradlew cleanTest check` → BUILD SUCCESSFUL, con **263 pruebas**, 0 fallos y 0 omitidas.
- Cobertura: líneas 94,9 %, ramas 74,0 % e instrucciones 95,0 %. ArchUnit en verde.

**Queda pendiente de M2:** el conflicto de edición por versión. `UsuarioEntity` tiene `@Version`, pero la versión no se compara con la que vio el cliente. Dos ediciones simultáneas sobrescriben o responden 500, y una edición del admin que coincida con un login de la misma cuenta puede responder 500. Antes hay que definir en el contrato cómo se envía la versión (`If-Match` o un campo) y qué 409 se responde.

### Implementación realizada: M2, parte de versión en usuarios (D10, aprobado por Richard el 2026-10-13)

**Contrato primero** (`/ecc:contract-first` y `/ecc:api-design`), en `openapi.yaml` 0.5.0-borrador, validado con Redocly sin avisos nuevos:
- `Usuario.version` (`int64`, ≥ 0, `readOnly`) en todas las respuestas con un usuario, también las de sesión.
- `SolicitudActualizarUsuario.version` es obligatoria, con `minProperties: 2`.
- Código nuevo `VERSION_DESACTUALIZADA` (409), descrito en `Conflicto` y en el `PATCH`.

También quedó registrado en `requerimientos.md` 4.7, ADR-0002, `modelo-datos.md` (columnas, concurrencia y reglas JPA) y en la fila D10 del plan.

**Pruebas primero** (`/ecc:springboot-tdd`): 32 nuevas. En rojo fallaron 30; las otras dos ya se cumplían (el cuerpo con solo `version` responde 400 por la regla existente, y la cobertura de los demás códigos).

| Prueba | Qué comprueba | En rojo |
| --- | --- | --- |
| `VersionUsuarioIT` (20) | Versión en `GET`, listado, `POST`, login, `/auth/yo` y `/auth/renovar`; la respuesta del `PATCH` coincide con la base (1 y luego 2); versión vieja → 409 `VERSION_DESACTUALIZADA` sin `errores` ni cambios; desactivar con versión vieja → no desactiva ni revoca sesiones; versión vieja con dato repetido → gana `VERSION_DESACTUALIZADA`; versión ausente, `null`, `-1`, `1.5`, `0.0`, `"0"`, `true`, `{}`, `[0]` o fuera de rango → 400 en `version` sin cambios; solo `version` → 400; logins y edición en los dos órdenes y edición con un bloqueo activo, sin perder intentos ni quitar el bloqueo | 19 en rojo |
| `EdicionConcurrenteIT` (2, 10 rondas cada una) | Dos ediciones simultáneas con la misma versión: 200 y 409, y solo el cambio ganador. Edición a la vez que 3 logins fallidos: la edición da 200, se cuentan los intentos (3, o 5 con el bloqueo formado) y la versión sube solo 1 | Edición + logins: **500**. Dos ediciones: **`[200, 500]`** |
| `UsuarioJpaRepositorioIT` (3) | Un conflicto de `@Version` se traduce a `VersionDesactualizada`; `guardar` no pisa los contadores; `guardarIntentos` no sube la versión | `ObjectOptimisticLockingFailureException` sin traducir; **`guardar` sobrescribía 4 intentos con 0**; `guardarIntentos` subía la versión |
| `ActualizarUsuarioTest` (5), `IniciarSesionTest` (1), `ManejadorErroresTest` (1) | La comparación ocurre sobre la fila releída y antes de los duplicados; ninguna revocación con versión vieja; el login no cambia la versión; 409 para el código nuevo | En rojo |

**Implementación** (`/ecc:jpa-patterns` y `/ecc:springboot-security`):
- **`VersionDesactualizada`** (error de dominio) y **`CodigoError.VERSION_DESACTUALIZADA`** → 409 "Los datos cambiaron" en `ManejadorErrores`.
- **`ActualizarUsuarioCasoDeUso`:** `buscarParaActualizar` (`SELECT … FOR UPDATE`), compara la versión antes de cualquier cambio y devuelve el usuario releído, con la versión que quedó en la base.
- **`UsuarioController`:** `version` llega como `JsonNode` y se valida de forma estricta (un entero JSON desde 0 y que quepa en un `long`). Así Jackson no puede convertir `"3"`, `1.5` ni `true`.
- **`UsuarioRespuesta`** y **`/auth/yo`** incluyen `version`.
- **`UsuarioEntity`:** `intentos_fallidos` y `bloqueado_hasta` con `updatable = false`, y `actualizar(...)` ya no los recibe.
- **`UsuarioDatos.guardarIntentos`:** JPQL sin `versioned`. `IniciarSesionCasoDeUso` lo usa para fallos y aciertos, sobre la fila bloqueada (A4).
- **`UsuarioJpaRepositorio.guardar`:** traduce `ObjectOptimisticLockingFailureException` a `VersionDesactualizada`, como red de seguridad.

**Se conservan** A1 (las pruebas de A1 envían ahora la versión vigente y siguen dando 403), A2, A4, la revalidación tras releer y la traducción de duplicados.

**Verificación:**
- `RenovacionConcurrenteIT`, `IntentosConcurrentesIT`, `AltasConcurrentesIT` y `EdicionConcurrenteIT` se repitieron juntas 3 veces, sin ningún 500 ni conflicto optimista en el log.
- `./gradlew cleanTest check` → BUILD SUCCESSFUL, con **295 pruebas**, 0 fallos y 0 omitidas.
- Cobertura: líneas 95,3 %, ramas 76,2 % e instrucciones 95,3 %. ArchUnit en verde.

**Pendientes de D10:**
- **Versionado de roles:** `PUT /roles/{rolId}` sigue con "gana el último" y la tabla `rol` no tiene `version`. Pendiente explícito; **no se da por corregido**.
- **Frontend (paso 1.8):** `apiFicticia.ts` y los `PATCH` de `UsersManagementView.test.tsx` (pruebas del paso 1.6, todavía en rojo) deben adaptarse al contrato 0.5.0. Además, la pantalla debe recargar el usuario ante un 409 `VERSION_DESACTUALIZADA`.

### Revisión final y corrección N1 (2026-10-13)

**Revisión de solo lectura** (`/ecc:springboot-security`) sobre el código final, después de A1, A2, A4 y D10. No encontró regresiones ni formas de saltarse la seguridad. Planteó un posible interbloqueo (N1) y varias frases desactualizadas en los documentos.

**Investigación de N1, con TDD** (`/ecc:springboot-tdd`, `/ecc:jpa-patterns` y `/ecc:springboot-security`):
- **Prueba nueva, `DesactivarMientrasRenuevaIT`.**
  - Llama a los casos de uso reales, con sus transacciones, contra PostgreSQL 17.
  - **No depende de repetir solicitudes:** un `BeanPostProcessor` que solo existe en la prueba envuelve `UsuarioRepositorio` y `TokenRenovacionRepositorio` con puntos de espera por hilo (`RENOVAR` y `EDITAR`), y así fuerza el orden exacto.
  - Escenario (A): la renovación consume el token antes de que la desactivación bloquee al usuario. Escenario (B): al revés.
  - Comprueba el resultado de cada operación y el **estado final**: la cuenta desactivada y **cero tokens de renovación vigentes**.
- **Prueba unitaria nueva en `RenovarSesionTest`:** si la cuenta se desactiva antes de que la renovación obtenga la fila, se responde 401, sin sucesor y con la familia revocada.
- **Resultado en rojo:**
  - **El interbloqueo previsto no se reprodujo.** No apareció ningún `deadlock detected` ni `40P01`. Hibernate 7.4.5 usa `FOR NO KEY UPDATE` para `PESSIMISTIC_WRITE` (comprobado en `PostgreSQLDialect`), y ese bloqueo no choca con el `FOR KEY SHARE` que toma la clave foránea al insertar el sucesor.
  - **Sí se reprodujo un fallo real** por el mismo orden distinto de los bloqueos:
    - (A) las dos operaciones terminaban bien, pero **quedaba 1 token de renovación vigente en la cuenta desactivada**. La revocación no veía el sucesor que se confirmó después de empezar.
    - (B) la renovación leía la cuenta como activa y **entregaba una sesión** a un usuario que se estaba desactivando.
    - El token que sobraba no servía para renovar, porque la renovación siguiente comprueba la cuenta, pero rompía la regla de que desactivar revoca todas las sesiones.
- **Corrección:** `RenovarSesionCasoDeUso` usa `buscarParaActualizar` antes de tocar los tokens. Desactivación, login y renovación siguen el mismo orden: primero el usuario, después los tokens.
- **Ajuste de la prueba (B):** con la corrección, la renovación toca al usuario antes de consumir el token. Por eso el punto de espera del escenario se movió a su primer acceso al usuario (`buscarPorId` o `buscarParaActualizar`), para mantener el orden "la desactivación bloquea primero". **Con la corrección revertida de forma temporal, los dos escenarios siguieron fallando**; después se restauró.
- **Se conservan** A1, A2, A4 y D10.

**Documentos corregidos en esta pasada:**
- índice `adr/README.md`, fila 0002;
- ADR-0002: el cambio de contraseña consta como **no implementado**, y hay una sección nueva para N1;
- plan: la siembra está en V1 (no hay V2), el claim es `roles` (dos lugares), la búsqueda usa hasta tres consultas, el 409 es `VERSION_DESACTUALIZADA`, y hay un bloque nuevo para N1;
- `CLAUDE.md`: el login ya existe;
- este informe: OpenAPI 0.5.0 en el resumen.

**Resultados históricos y actuales:**
- **Históricos** (ejecuciones anteriores, ya superadas): 226 pruebas al terminar la implementación inicial, 243 tras A1, 245 tras A2, 249 tras A4, 254 tras la revalidación, 263 tras los duplicados y 295 tras D10.
- **Actual (esta pasada, 2026-10-13):**
  - `./gradlew cleanTest check` → BUILD SUCCESSFUL, con **298 pruebas**, 0 fallos y 0 omitidas;
  - cobertura: líneas 95,3 %, ramas 76,2 % e instrucciones 95,3 %; ArchUnit en verde;
  - `DesactivarMientrasRenuevaIT` y las 4 pruebas concurrentes, repetidas 3 veces juntas, sin interbloqueos, 500 ni conflictos optimistas en el log.

### Hallazgos de la revisión todavía pendientes (sin corregir)

| # | Gravedad | Hallazgo | Destino propuesto |
| --- | --- | --- | --- |
| A3 | Media | Una renovación legítima desde dos pestañas revoca toda la sesión (más probable ahora que A2 se corrigió sin margen) | Paso 1.8: una sola renovación compartida entre pestañas |
| M1 | Media | Sin límite por IP; cualquiera puede bloquear la cuenta del admin | Decisión [P] |
| M2 (roles) | Media | Corregido para usuarios (duplicados y versión, D10). Queda **el versionado de roles**: `PUT /roles/{rolId}` sigue con "gana el último" y `rol` no tiene `version` | Pendiente explícito de D10; requiere migración y contrato propios |
| M3 | Media | Contraseña temporal común a los usuarios iniciales, sin cambio obligatorio | Decisión [P], antes del paso 1.10 |
| M4 | Media | Sin `Content-Security-Policy` en el nginx del frontend | Paso 1.8 |
| B1 | Baja | Se puede desactivar o quitar el rol al último admin | Decisión [P] |
| B2 | Baja | No se registran eventos de seguridad | Pendiente [P] del plan |
| B3 | Baja | El JWT no lleva `aud` ni `jti`; tolerancia de reloj de 60 s | Paso 1.9 |
| B4 | Baja | Sin HSTS | Fase 8 |
| I1 | Informativa | El 423 revela que la cuenta existe (aprobado en D2) | Aceptar o cambiar con un ADR |
| I2 | Informativa | Al cerrar sesión, el token de acceso vive hasta 15 min (por diseño) | — |

## Pendientes y avisos

- **Hallazgos de seguridad:** A1, A2, A4, M2 para usuarios (duplicados y versión, D10) y N1 (orden de bloqueos entre desactivar y renovar) están corregidos. Siguen abiertos A3, M1, M2 (versionado de roles), M3, M4, B1 a B4, I1 e I2 (tabla anterior).
- **Bloqueo optimista (M2, versión):** corregido para usuarios el 2026-10-13 (D10; sección anterior). Los roles siguen sin versión (pendiente explícito).
- **Carreras de unicidad (M2, duplicados):** corregido el 2026-10-13; responden 409 `DUPLICADO` con el campo.
- **Frontend y contrato 0.5.0:** las pruebas del paso 1.6 (`apiFicticia.ts`, `UsersManagementView.test.tsx`) deben agregar `version` en el paso 1.8.
- **Registro de intentos fallidos (B2):** no se escribe en el log; el plan lo dejó como propuesta [P].
- **Permisos de `rol_permiso`:** siguen pendientes de definir. `/auth/yo` devuelve la unión de los permisos, hoy vacía.
- **Alcance del rol `consulta`:** sigue pendiente del cliente ⏸.
- **Frontend:** sus 38 pruebas nuevas siguen en rojo hasta el paso 1.8.
- **Commit:** no hay ninguno de los pasos 1.6 y 1.7; Richard los hace a mano.

## Siguiente paso

Antes del paso 1.8, Richard decide qué hallazgos pendientes se corrigen ya y cuáles pasan al 1.9 (el 403 de A1 ya está aprobado). Después, el paso 1.8 (frontend, que incluye M4 y la renovación desde una sola pestaña de A3) y el paso 1.9 (revisión): `/code-review`, `ecc:java-reviewer` y `/security-review`.
