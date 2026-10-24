# Plan: Fase 7 — Colombia y chat de consulta con Claude API (paso 7.1)

> **Estado:** paso 7.1 (definiciones) documentado el 2026-10-22. **Sin código, migraciones, llamadas a Claude o Google, pruebas ni builds.** Las propuestas de contrato, datos y ADR de este plan **no están aplicadas**: esperan la aprobación de Richard antes de extender el código (manual, 7.1). Ninguna fase se cierra.

## Summary

La fase 7 suma Colombia y un chat de consulta con IA. Colombia usa los mismos flujos A/B y roles que Ecuador. Sus bodegas, calendario, responsables y asignaciones siguen por definir. El chat es de solo lectura: responde sobre pedidos, etapas y mercadería descrita en documentos, con referencias verificables y dentro de los permisos vigentes del usuario. El paso 7.1 deja los requisitos, el PRD y este plan al día, y propone el contrato, los datos y los ADR.

## User Story

Como usuario de DECOKASA (de Ecuador o de Colombia), quiero preguntar en lenguaje natural por el estado de mis pedidos y por la mercadería que describen sus documentos, para encontrar la respuesta sin abrir pedido por pedido y archivo por archivo, y poder comprobarla en la fuente citada.

## Problem → Solution

Hoy el sistema solo atiende Ecuador (`CrearPedidoCasoDeUso` rechaza otro país) y no tiene IA. → Colombia opera los flujos A/B con su numeración y acceso por país, y un chat de solo lectura responde con evidencia, sin salir de los permisos del usuario ni modificar nada.

## Metadata

- **Complexity**: XL (dos subsistemas: país e IA). Se divide en los pasos 7.2–7.5 del manual.
- **Source PRD**: `.claude/PRPs/prds/decokasa.prd.md`
- **PRD Phase**: 7 — Colombia y API de Claude
- **Rama**: `fase-7-colombia-ia`, creada el 2026-10-22 desde `fase-6-flujo-b` con todos los cambios pendientes y el índice vacío, con autorización de Richard. No autoriza commit, push ni merge. La fase 6 sigue abierta.
- **Contrato vigente**: `openapi.yaml` 0.15.0-borrador. **Última migración**: V18. Siguiente libre: V19 (no reservada).

---

## Definiciones del paso 7.1 (origen)

Fuente única: `documentos/requerimientos.md` §5, "Definiciones de la fase 7 comunicadas por Richard (2026-10-22)". **No constan como respuesta escrita del cliente.**

| Tema | Definido | Pendiente (⏸ / [P]) |
|---|---|---|
| Proveedor de IA | Claude API (confirmado por Richard) | Modelo y límites concretos; región o retención de datos aceptable |
| Colombia | Mismos flujos A/B y roles que Ecuador | Bodegas, calendario, responsables y asignaciones por país. Si "mismos flujos" incluye documentos, plazos, revisoras y llegada. **Choca con "procesos distintos" del documento del cliente (1.3).** Quién elige país. Formato `DK-CO-AAAA-NNNN` |
| Chat | Consulta sobre pedidos, etapas y mercadería descrita en documentos; solo lectura | Alcance de una pregunta (F7-D1); quién lo usa; si se guarda el texto |
| Permisos | Los vigentes del usuario, incluidos país, pedido y documento | El filtro por país no existe todavía (ver "Hallazgos del código") |
| Evidencia | Referencia al pedido y al documento; página cuando esté disponible; "sin evidencia suficiente" si falta | Cómo se cita un Excel o una imagen (sin páginas) |
| Datos | Solo documentos ficticios al inicio; los reales requieren aprobación expresa | Quién aprueba, cómo se registra y qué cubre |
| Configuración | Modelo configurable, clave solo en el backend, límites de consumo | Valores de los límites y presupuesto |

## Hallazgos del código (2026-10-22, lectura sin ejecutar)

| Hecho | Dónde | Implicación |
|---|---|---|
| País: `pais` (EC, CO), `usuario_pais` y `secuencia_pedido (pais_codigo, anio)` existen desde V1/V2 | `V1__usuarios_roles_paises.sql`, `V2__flujos_pedidos_historial.sql:125` | La numeración por país ya está en el esquema |
| Crear pedido solo admite EC | `CrearPedidoCasoDeUso.java:55-56` | 7.2 quita esa restricción cuando haya definiciones de CO |
| El alta de usuarios asigna EC por defecto | `CrearUsuarioCasoDeUso.java:57`, `CargadorUsuariosIniciales.java:63` | Las asignaciones por país están pendientes del cliente |
| **Leer pedidos no filtra por país**: cualquier cuenta activa con roles lee cualquier pedido (`ApoyoPedidos.lector`) | `VerificarConsultaCasoDeUso.java`, `ConsultarPedidosCasoDeUso` | El chat **no puede apoyarse solo en el lector actual**: primero hace falta el acceso por país de 7.2 |
| La descarga sigue P-17/P-06 (`autorizarDescarga`) | `AutorizarArchivosCasoDeUso` | El chat aplica la misma regla por documento: lo que el usuario no puede descargar no se envía al modelo |
| El contenido de los archivos vive en el almacenamiento local temporal (con cuota) y se copia a Drive; el puerto `AlmacenArchivos` solo sube, no lee | `archivos/application/port/out/AlmacenArchivos.java`, `AlmacenLocal` | Leer un documento ya copiado exige **descargarlo de Drive** (llamada a Google): con documentos reales, es otra aprobación |
| Formatos de archivo admitidos: PDF, Word, Excel, imágenes y ZIP (propuesta vigente) | `requerimientos.md` §5, "Por confirmar" | Claude lee PDF de forma nativa, con página en las citas. Excel y Word necesitan convertirse en el servidor y no tienen páginas |
| Configuración solo por variables de entorno, sin secretos en `application.yml` | `application.yml:1, 45-47` (`decokasa.*`) | `decokasa.ia.*` sigue el mismo patrón; `ANTHROPIC_API_KEY` sin valor por defecto |

## Investigación externa (skill `claude-api`, 2026-10-22; sin llamadas reales)

- **SDK:** SDK oficial de Java `com.anthropic:anthropic-java` (la skill documenta la 2.34.0; se confirma la versión al implementarlo). Nada de HTTP a mano ni de capas compatibles con OpenAI.
- **Modelo:** **no** se fija Opus por defecto. La comparación con la documentación oficial vigente está en la propuesta F7-D2. El modelo es configurable y lo elige Richard.
- **Citas con página:** los bloques `document` con `citations: {enabled: true}` devuelven `page_location` (páginas desde 1) en los PDF y `char_location` en texto plano. **Las citas no se pueden combinar con salida estructurada (`output_config.format`)**: la referencia al pedido se arma en el servidor con los metadatos de cada documento enviado (título = número de pedido + nombre del archivo).
- **Instrucciones dentro de documentos:** se envían como contenido de documentos, nunca como instrucciones del sistema. El prompt de sistema es fijo y se guarda en caché. El chat **no tiene herramientas que escriban**, así que una instrucción inyectada no puede modificar nada; el riesgo que queda es una respuesta engañosa, y lo limitan las citas obligatorias.
- **Rechazos y respaldo:** con Opus 5.5 hay que comprobar `stop_reason` (incluido `refusal`) antes de leer el contenido. La skill recomienda activar `fallbacks: "default"` (beta del servidor), pero **queda descartado**: Richard no permite cambiar automáticamente a modelos más caros (2026-10-22).
- **Consumo:** cada respuesta trae `usage` (entrada, salida y caché). Se registra por consulta, sin el texto ni la clave. `count_tokens` permite rechazar una pregunta antes de enviarla si supera el límite.
- **Errores:** distinguir los reintentables (429, 5xx, red) de los definitivos (400/404), con las clases tipadas del SDK; tiempo de espera configurable.

---

## Propuestas antes de extender el código (sin aplicar; sustituidas por las "Propuestas consolidadas" del 2026-10-22)

### P7-C — Contrato (`openapi.yaml` 0.16.0-borrador, propuesto)

| Cambio | Detalle propuesto |
|---|---|
| `POST /asistente/consultas` | Cuerpo: `pregunta` (1–2000), `pais` (el país activo), `pedidoId` (según F7-D1). Respuesta 200 `RespuestaAsistente`: `texto`, `evidenciaSuficiente` (bool), `referencias[]` {`pedidoId`, `numeroPedido`, `etapa?`, `archivoId?`, `nombreArchivo?`, `pagina?` (desde 1, nula si no aplica), `textoCitado?`}, `consultaId`. Sin `modelo` ni claves |
| Errores | 400 `VALIDACION`; 403 `SIN_PERMISO` (cuenta, país o pedido); 404 pedido; 429 `LIMITE_CONSUMO` (con `Retry-After` cuando aplique); 503 `IA_NO_DISPONIBLE` (proveedor caído, tiempo agotado o IA desactivada). Sin detalles del proveedor |
| `GET /asistente/consumo` (admin) | Totales por día y usuario: consultas y tokens. Sin el texto |
| País activo | Lo decide 7.3 (selector). Propuesta: parámetro `pais` validado contra `usuario_pais` en cada petición, sin estado de país en el JWT |
| Fuera | Streaming al navegador (posible después), historial de conversación persistente, acciones |

### P7-D — Datos (V19 en adelante, propuestos; no se reserva el número)

| Migración | Contenido propuesto | Depende de |
|---|---|---|
| Definiciones de CO | `definicion_flujo` CO/A v1 y CO/B v1 con las mismas etapas, roles y transiciones que EC v1 (UUIDv7 nuevos, bloque `DO` que compara con EC fila por fila). **Solo cuando el cliente confirme el alcance de "mismos flujos"**; mientras tanto, los documentos y plazos no confirmados se siembran como `PENDIENTE`/`PENDIENTES`, sin copiarlos | Skill `agregar-flujo-decokasa` §1 (alta) |
| Bodegas de CO | Ninguna en la migración: el catálogo lo mantiene Logística o el admin (F4-D1). El país de cada bodega: `bodega.pais_codigo` si aún no existe; **comprobar en 7.2** | Definición del cliente |
| Calendario | Feriados por país y jornada: tabla solo si se aprueba (modelo-datos 3.7) | Definición del cliente |
| `consulta_ia` | `id`, `usuario_id`, `pais_codigo`, `pedido_id?`, `modelo`, `tokens_entrada`, `tokens_salida`, `tokens_cache_lectura`, `tokens_cache_escritura`, `resultado` (`RESPONDIDA`, `SIN_EVIDENCIA`, `LIMITE`, `ERROR`, `RECHAZADA`), `documentos_enviados` (ids), `creada_en`. **Sin pregunta ni respuesta** hasta que se decida guardarlas. Solo inserción, como `evento_pedido` | ADR P7-A2 |
| `configuracion_ia` | Fila única con versión (como `configuracion_plazos`): `habilitada` (false), `documentos_reales_autorizados` (false) con `autorizado_por`/`autorizado_en`, límites (`consultas_por_usuario_dia`, `tokens_por_usuario_dia`, `tokens_mes`) y su origen `PROPUESTA`/`ADMIN`. Los valores sembrados son propuestas sin aprobar | Valores pendientes |
| `adjunto.ficticio` | Marca de documento ficticio para desarrollo. **Alternativa:** un entorno separado con solo datos ficticios (preferible: no marca datos de producción) | F7-D3 |

### P7-A — ADR propuestos (estado `proposed`, ninguno creado todavía)

| ADR | Decisión propuesta | Alternativas | Enlaces |
|---|---|---|---|
| **P7-A1** Claude API como proveedor, detrás del puerto `AsistenteIA` | Puerto en un módulo nuevo `asistente` (dominio sin Spring ni SDK); adaptador `ClaudeAsistenteIA` con el SDK oficial de Java; adaptador falso para pruebas; modelo, esfuerzo, `max_tokens`, tiempo de espera y respaldo por configuración; clave en `ANTHROPIC_API_KEY`, sin valor por defecto; si falta, la IA queda desactivada (503) y el resto del sistema arranca. Proveedor confirmado por Richard; la arquitectura queda como propuesta | Llamar desde el navegador (descartado: R-seguridad); otro proveedor (descartado por Richard); Managed Agents (descartado: no hace falta una sandbox ni un agente) | 0001, 0011, 0012, 0015 |
| **P7-A2** Recuperación con permisos en el servidor y respuestas citadas | El servidor elige qué datos y documentos envía, ya filtrados por cuenta vigente, país, pedido y P-17 por documento; el modelo solo recibe eso. Documentos como bloques `document` con citas (PDF → página); los datos del pedido, como texto propio del servidor. Sin herramientas de escritura. Si no hay citas, `evidenciaSuficiente=false`. Prompt de sistema fijo; el contenido de los documentos no es una instrucción | Dar al modelo herramientas de búsqueda sobre todos los pedidos (más alcance, más riesgo de fuga); índice vectorial (más infraestructura, sin necesidad demostrada) | 0007, 0019 (proposed) |
| **P7-A3** Acceso por país | El país activo se valida en cada petición contra `usuario_pais` vigente en la base (como A1 con roles); las consultas filtran por país en el repositorio, no en la pantalla | País en el JWT (queda obsoleto al cambiar asignaciones) | 0002, 0009 |

---

## Credenciales de Claude API (Richard, 2026-10-22) — configuración preparada, sin adaptador

- **Decisión de Richard:** Decokasa usará **credenciales nuevas** de Claude API, independientes de sus herramientas de desarrollo. No se reutiliza ninguna clave existente. Richard la aportará después por configuración local segura. Mientras tanto: sin cuentas, compras ni llamadas.
- **Hecho (solo configuración):**
  - `application.yml`: `decokasa.ia.habilitada` (`IA_HABILITADA`, false por defecto), `decokasa.ia.claude-api-key` (`DECOKASA_CLAUDE_API_KEY`, sin valor por defecto) y `decokasa.ia.modelo` (`IA_MODELO`, vacío hasta F7-D2).
  - `.env.example`: las tres variables, vacías o en false.
  - `docker-compose.yml`: las pasa **solo al backend**.
  
  `.env` sigue ignorado por Git y el frontend no recibe ninguna de ellas. Ninguna clase las lee todavía: el adaptador es del paso 7.4.
- **Por qué un nombre propio y no `ANTHROPIC_API_KEY`:** el SDK, con `AnthropicOkHttpClient.fromEnv()`, toma en orden `ANTHROPIC_API_KEY`, `ANTHROPIC_AUTH_TOKEN`, el perfil de `ant auth login` y otras fuentes del equipo (skill `claude-api`). Usarlo podría reutilizar sin querer una clave de desarrollo. **Regla para 7.4:** construir el cliente con `AnthropicOkHttpClient.builder().apiKey(<propiedad>)` y la URL base explícita, nunca con `fromEnv()`. Si la propiedad está vacía, la IA queda desactivada y no se busca otra fuente.
- **Sin mostrar la clave (para 7.4):**
  - la propiedad en un tipo con `toString()` protegido (como `AlmacenArchivos.Sesion`);
  - ningún log con la configuración ni con las cabeceras;
  - errores del proveedor traducidos a `IA_NO_DISPONIBLE`, sin detalles;
  - actuator sigue exponiendo solo `health` (`application.yml`, `management.endpoints`).
- **Límite conocido:** una variable de entorno es visible para quien inspecciona el contenedor (`docker inspect`). Para producción (fase 8) se valora leerla desde un archivo de secreto montado, como hacen las rutas de Google.

## F7-D1: alcance de una pregunta al chat — aprobada (opción C)

Define el contrato, los datos que se envían, el costo y cómo se aplican los permisos. Todo lo demás del chat depende de ella.

| Opción | Qué significa | Ventajas | Contras |
|---|---|---|---|
| **A. Un pedido por conversación** | El usuario abre un pedido y pregunta sobre él: su estado, etapas, historial y documentos que puede descargar | El permiso se comprueba una vez y por documento; el contexto es pequeño y barato; la cita al pedido es trivial; menos riesgo de fuga | No responde "¿qué pedidos tienen sillas?" ni "¿cuáles van tarde?" |
| B. Varios pedidos por pregunta | Preguntas sobre todos los pedidos visibles del país activo: el servidor busca primero los candidatos (por estado, etapa o texto) y envía solo esos | Más útil para el admin y para el seguimiento | Requiere búsqueda previa (texto de los documentos extraído y guardado, o herramientas de solo lectura), más tokens por pregunta y más superficie de permisos |
| C. A primero y B después | Implementar A en 7.4 y evaluar B con uso real | Entrega temprana y medible | Dos etapas de trabajo |

**Decisión (Richard, 2026-10-22): opción C.** Al principio, una conversación pertenece a un único pedido. Las consultas sobre varios pedidos quedan fuera de esta entrega. Antes de **cada** consulta, el servidor vuelve a comprobar los permisos vigentes de país, pedido y documentos; conocer el identificador no basta. En P7-C, `pedidoId` pasa a ser obligatorio; el contrato sigue como propuesta.

## F7-D2 — Modelo y límites del piloto con datos ficticios

> **Límites aprobados por Richard el 2026-10-22** para el piloto, solo con las credenciales nuevas de Decokasa. **No autoriza compras ni llamadas reales**; `IA_HABILITADA=false` se mantiene y no se reutilizan credenciales del entorno de desarrollo. La aprobación nombra "esos límites": El modelo inicial `claude-haiku-5-5` lo confirmó después Richard (2026-10-22), sujeto a verificar su disponibilidad.

### Fuentes oficiales consultadas (2026-10-22)

`platform.claude.com/docs`: [precios](https://platform.claude.com/docs/en/about-claude/pricing), [modelos](https://platform.claude.com/docs/en/about-claude/models/overview), [Haiku 5.5](https://platform.claude.com/docs/en/models/haiku-5-5/overview), [elegir modelo](https://platform.claude.com/docs/en/about-claude/models/choosing-a-model), [PDF](https://platform.claude.com/docs/en/build-with-claude/pdf-support), [visión](https://platform.claude.com/docs/en/build-with-claude/vision), [citas](https://platform.claude.com/docs/en/build-with-claude/citations) y [límites](https://platform.claude.com/docs/en/api/rate-limits). Solo lectura; ni llamadas a la API ni compras. Los precios se vuelven a comprobar antes de implementar.

### Comparación de modelos

| Modelo (ID) | Entrada / salida por millón de tokens | Lectura de caché | Notas oficiales | Costo de una consulta en el peor caso del piloto (60 000 de entrada + 4 000 de salida, sin caché) |
|---|---|---|---|---|
| **Claude Haiku 5.5** (`claude-haiku-5-5`) | $0.10 / $0.50 con prompts de hasta 100 000 tokens; **$0.50 / $2.50 por encima** | $0.01 | Publicado el 2026-10-07; el más rápido; "cost-sensitive deployments needing strong reasoning"; esfuerzo por defecto `medium`; contexto de 1M; tokenizador nuevo (~30 % más tokens que Haiku 4.5); retiro no antes del 2027-10-07 | **≈ $0.008** |
| Claude Sonnet 5.5 (`claude-sonnet-5-5`) | $2 / $10 | $0.10 | "best combination of speed and intelligence"; esfuerzo por defecto `high` | ≈ $0.16 |
| Claude Opus 5.5 (`claude-opus-5-5`) | $4 / $20 | $0.20 | Pensado para trabajo agéntico largo; el pensamiento no se puede apagar | ≈ $0.32 |
| Claude Haiku 4.5 (`claude-haiku-4-5`) | $1 / $5 | $0.10 | Generación anterior; más caro que Haiku 5.5 | ≈ $0.08 |

Todas las citas funcionan en todos los modelos activos (documentación de citas), y `cited_text` no se cobra como salida. La guía oficial "Choosing a model" recomienda empezar por el modelo eficiente (Haiku 5.5) en prototipos, despliegues sensibles al costo y tareas directas, y subir de modelo solo si una evaluación muestra carencias.

**Recomendación:** `claude-haiku-5-5` como modelo inicial del piloto, con esfuerzo `medium` (el valor por defecto; se mide también `low`). En paralelo, una evaluación con 30–50 preguntas ficticias, sus respuestas esperadas y sus citas, comparada contra `claude-sonnet-5-5`. Se cambia a Sonnet solo si Haiku falla en precisión de citas o en decir "sin evidencia". Opus no se justifica para preguntas sobre un solo pedido. El modelo queda en la configuración del servidor (`decokasa.ia.modelo`), junto con su tabla de precios.

### Límites iniciales propuestos

| Límite | Valor propuesto | Por qué |
|---|---|---|
| Contexto por consulta | **60 000 tokens de entrada como máximo**, medidos con `count_tokens` antes de enviar (prompt de sistema, datos del pedido, documentos e historial) | Margen claro bajo el umbral de 100 000 tokens de Haiku 5.5, que quintuplica el precio. Un PDF cuesta 1 500–3 000 tokens de texto por página más la imagen de cada página (hasta 4 784 tokens en modelos 4.7 o posteriores): unas 8–10 páginas por consulta. Si no cabe, 400 `CONTEXTO_EXCEDIDO` y el usuario elige qué documentos incluir |
| Respuesta | `max_tokens` = **4 000** (incluye el pensamiento adaptativo) | Respuestas breves con citas; acota el costo de salida |
| Turnos por conversación | **10 preguntas** | Cada turno reenvía el historial; la caché de 5 minutos abarata los documentos repetidos (lectura a 0.1× en Haiku 5.5) |
| Por usuario | **20 consultas por día** y **1 en curso** | Evita los dobles clics y los bucles |
| Global | **200 consultas por día** | Techo diario del piloto |
| Presupuesto del servidor | **US$10 por mes calendario** | Peor caso con Haiku: ≈ 1 250 consultas; también cubre una evaluación de 50 preguntas en Sonnet (≈ US$8) si se hace aparte |
| Respaldo en la Console | Límite de gasto del **workspace** dedicado al piloto: **US$15 por mes**, con su propia clave | Lo configura Richard en la Console (el workspace por defecto no admite límites de gasto). No es una compra; los créditos y la cuenta los decide Richard |

Todos los valores se guardan en `configuracion_ia` con origen `PROPUESTA` hasta que Richard los apruebe. El admin los cambia con versión.

### Cómo se bloquea el consumo al llegar al presupuesto (incluidas las consultas simultáneas)

1. **Reserva antes de llamar.** El servidor calcula el costo máximo de la consulta: tokens de entrada exactos (`count_tokens`) × precio de entrada + `max_tokens` × precio de salida, en **micro-USD enteros** y con la tabla de precios del modelo configurado. Si el modelo no tiene precio configurado, la IA no responde (falla cerrada).
2. **Reserva atómica en PostgreSQL**, en una transacción corta y antes de llamar al proveedor:
   - presupuesto del mes: `UPDATE presupuesto_ia SET reservado = reservado + :max WHERE periodo = :mes AND consumido + reservado + :max <= limite RETURNING …`;
   - lo mismo para el usuario y el día (consultas, y `en_curso < 1`) y para el total diario.

   Si alguna actualización afecta 0 filas, responde **429 `LIMITE_CONSUMO`** sin llamar al proveedor. La condición y la suma se evalúan en una sola sentencia, sobre una fila bloqueada: dos consultas simultáneas se serializan, y la segunda ve la reserva de la primera. **No hay carrera que permita pasar el límite**, porque se reserva el peor caso.
3. **Liquidación después de responder.** Con el `usage` real (entrada, escritura y lectura de caché, salida): `consumido += costo real`, `reservado -= reserva` y `en_curso -= 1`, y se registra en `consulta_ia`.
4. **Fallos.** Un error que con seguridad no se cobró (validación del servidor antes de enviar, o 400 del proveedor) libera la reserva. Si el resultado es dudoso (tiempo agotado o conexión cortada), la reserva **se da por consumida** (criterio conservador). Los reintentos automáticos del SDK se desactivan (`maxRetries = 0`) o cada intento reserva por separado. Una reserva sin liquidar más allá del tiempo de espera + margen se da por consumida en la siguiente comprobación (cubre la caída del servidor).
5. **Respaldo externo.** Si el proveedor responde que se alcanzó el límite del workspace (400 `invalid_request_error`, "You have reached your specified workspace API usage limits") o el tope de la organización (429 con `enforced_spend_limit_reached`, sin `retry-after`), el servidor **no reintenta**, desactiva la IA (`configuracion_ia.habilitada = false`, con motivo) y responde 503 `IA_NO_DISPONIBLE`. La reactiva el admin.
6. **Interruptor general.** `habilitada = false` por defecto; sin `DECOKASA_CLAUDE_API_KEY` la IA no arranca, pero el resto del sistema sí.

Todo esto es propuesta. Las tablas `presupuesto_ia` (o columnas en `configuracion_ia`) y `consulta_ia` se suman a P7-D. Nada está implementado.

## F7-D3, F7-D4 y F7-D5 — propuesta conjunta (2026-10-22; aprobada con los ajustes de la sección siguiente; la conservación en producción no se aprobó)

### F7-D3 — Aislamiento de documentos ficticios y formatos iniciales

- **Entorno separado para el piloto:** otro proyecto de Docker Compose, con su propia base, su propio volumen de archivos y su propio `.env`. Es el único entorno con `IA_HABILITADA=true` y con la clave nueva. En el entorno habitual la IA sigue desactivada y sin clave.
- **Sin Google en el piloto:** `GOOGLE_*` en false. Los documentos se leen solo del almacenamiento local del piloto; ninguna lectura de Drive.
- **Contenido 100 % sintético:** usuarios, pedidos y PDF inventados, sin datos, logos, proveedores ni precios del cliente (el repositorio es público). Se generan con un script o se guardan bajo una ruta de semilla del piloto; nunca se copian de la base real.
- **Barrera en el código (7.4):** la IA envía documentos solo si `documentos_reales_autorizados=false` va acompañado de la marca `decokasa.ia.entorno=PILOTO_FICTICIO`, que solo existe en el `.env` del piloto. Con cualquier otro valor, el envío de documentos se bloquea hasta F7-D6.
- **Formatos iniciales: solo PDF.** Son nativos de Claude y permiten citar por página. Las imágenes (JPEG/PNG) se pueden sumar después, sin página. Excel y Word requieren una conversión en el servidor (otra dependencia y otra decisión). Un documento con otro formato no se envía, y la respuesta indica qué documentos quedaron fuera.

### F7-D4 — Roles autorizados para el chat

- **Piloto:** `admin` y los cuatro roles operativos (`asistente_compras`, `marketing`, `compras_china`, `logistica`), con usuarios ficticios.
- **Fuera por ahora:** `consulta` y `asistente_china`. Su alcance sigue provisional (⏸) y no se amplía por analogía.
- **El rol habilita el chat, pero no los datos:** cada consulta aplica además país, pedido y P-17 por documento (F7-D1). La lista de roles va en la configuración del servidor, no en la pantalla.

### F7-D5 — Almacenamiento y conservación de preguntas y respuestas

- **El historial lo guarda el servidor**, no el navegador. Así el cliente no puede inventar turnos o citas anteriores. Solo lo lee su autor; el admin ve el consumo y los metadatos, no el texto.
- **Piloto (datos ficticios):** preguntas, respuestas y referencias se conservan **30 días** para evaluar la calidad (precisión de citas y "sin evidencia") y armar el conjunto de evaluación. Luego un proceso las borra.
- **Producción (datos reales):** por defecto se conserva el texto solo mientras la conversación está activa, con un máximo de **24 horas**. Los metadatos (actor, fecha, pedido, documentos usados, modelo, tokens, costo y resultado) se conservan como registro de consumo. El plazo definitivo se decide con F7-D6, junto con la retención del lado del proveedor según su documentación vigente.
- **Nunca** en los logs: preguntas, respuestas, documentos ni la clave.
- **Datos (amplía P7-D, propuesto):** `conversacion_ia` (pedido, autor, apertura, cierre, `expira_en`) y `mensaje_ia` (rol, texto, referencias, `expira_en`), con borrado por vencimiento. `consulta_ia` sigue sin texto.

## Decisiones del 2026-10-22 (Richard) — modelo, F7-D3, F7-D4 y F7-D5

- **Modelo:** `claude-haiku-5-5`, sujeto a verificar su disponibilidad para la cuenta nueva. Se comprobará con `GET /v1/models/claude-haiku-5-5` en la primera llamada autorizada, que todavía no existe. **Sin cambio automático a modelos más caros:** sin el parámetro `fallbacks` del servidor, sin herramienta advisor y sin escalado por código. Un `refusal` o un error se responden como tales.
- **F7-D3 aprobada:** entorno aislado con base, archivos y credenciales propios; solo datos ficticios y PDF locales, sin Google; la respuesta informa de los documentos excluidos por formato. La variable de entorno no demuestra que un documento sea ficticio, así que se aplica el procedimiento de abajo.
- **F7-D4 aprobada:** el chat lo usan `admin`, `asistente_compras`, `marketing`, `compras_china` y `logistica`. `consulta` y `asistente_china` no dan acceso por sí solos, pero no quitan el que dé otro rol. Antes de enviar datos al modelo y antes de mostrar una conversación guardada, se comprueban los permisos vigentes de país, pedido y descarga.
- **F7-D5 aprobada solo para el piloto:** el servidor guarda las conversaciones y solo las ve su autor; el admin ve el consumo, no textos ajenos; 30 días y borrado; nada de textos, documentos ni secretos en los logs.
- **No aprobado:** la conservación en producción (24 h) y el envío de documentos reales (F7-D6).

### Procedimiento de control de archivos ficticios (F7-D3)

1. **Origen:** cada PDF del piloto se crea desde cero, con un script generador o una plantilla, con texto inventado. Nunca se exporta, copia ni edita un documento del cliente.
2. **Revisión humana:** antes de incluirlo, Richard revisa cada PDF con una lista de comprobación: sin nombres de personas o empresas reales, RUC/NIT, direcciones, correos, teléfonos, precios reales, logos ni marcas del cliente o sus proveedores.
3. **Manifiesto:** `piloto-ia/manifiesto.json`, local y fuera de Git igual que los PDF, con una entrada por archivo: `nombre`, `sha256`, `paginas`, `origen` (`GENERADO` o `PLANTILLA`), `revisado_por` y `revisado_en`. Un archivo sin entrada no existe para el piloto.
4. **Carga controlada:** en el piloto, los documentos entran solo por el cargador de semilla del piloto. La subida desde la pantalla, con `decokasa.ia.entorno=PILOTO_FICTICIO`, solo acepta archivos cuyo `sha256` esté en el manifiesto. Se compara con `adjunto.sha256`, que ya calcula el servidor (V3).
5. **Envío al modelo:** antes de cada consulta, el servidor vuelve a comprobar que cada documento elegido esté en el manifiesto y que `adjunto.tipo_detectado` sea PDF (firma del contenido, no el nombre). Si no lo es, queda fuera y se informa.
6. **Auditoría:** `consulta_ia.documentos_enviados` permite comprobar después que todo lo enviado estaba en el manifiesto. Codex lo verifica en la parte B.
7. **Fuera del piloto:** con cualquier otro valor de `decokasa.ia.entorno`, no se envía ningún documento hasta F7-D6.

### Eliminación de copias y referencias a los 30 días (F7-D5, piloto)

| Dónde | Qué hay | Cómo se elimina |
|---|---|---|
| `mensaje_ia` | Pregunta, respuesta, referencias con `textoCitado` y página, documentos excluidos | Borrado físico al vencer `expira_en` (creación + 30 días), por un proceso diario por lotes; en cascada al borrar su conversación |
| `conversacion_ia` | Autor, pedido, país y fechas | Se borra con sus mensajes al vencer |
| `consulta_ia` | Metadatos sin texto: actor, pedido, ids de los documentos enviados, modelo, tokens, costo y resultado | Se conserva como registro de consumo; su `conversacion_id` pasa a nulo al borrar la conversación. No guarda texto ni citas |
| Navegador | Caché de TanStack Query en memoria | Nunca en `localStorage` ni en `sessionStorage`; se limpia al cerrar sesión o al recargar |
| Logs | Nada (regla) | No aplica |
| Copias de seguridad del piloto | Ninguna por defecto | Si se hacen, rotan en 30 días o menos |
| Proveedor | Lo enviado en cada petición (PDF en base64, sin Files API) | **Independiente de este sistema.** No subir archivos con la Files API **no implica retención cero**. La retención del proveedor sigue pendiente (F7-D6) |

Los PDF ficticios siguen siendo adjuntos de su pedido: no forman parte del borrado del chat. Si el autor pierde el permiso, no puede ver la conversación (403) y esta vence igual. Una cuenta desactivada no lee nada.

---

## Propuestas consolidadas de contrato, datos y ADR (2026-10-22) — aprobadas para el piloto con precisiones y aplicadas en la documentación

> Sustituyen a P7-C, P7-D y P7-A de arriba, que se conservan como historia. **Aprobadas por Richard el 2026-10-22 para el piloto** y aplicadas solo en la documentación: `openapi.yaml` 0.16.0-borrador, `modelo-datos.md` 3.8, `arquitectura.md` 4.10 y ADR-0020 a ADR-0023, con su fila en el índice y el contador de `CLAUDE.md`. **Sin código ni migraciones.** El ADR de acceso por país (E) **no se creó**: sigue pendiente hasta las definiciones de Colombia.
>
> **Precisiones de Richard incorporadas:**
> 1. No se declara retención cero del proveedor por enviar los PDF dentro de la petición: su retención es independiente y sigue pendiente (F7-D6).
> 2. Cada referencia debe corresponder a un documento autorizado enviado en esa consulta y a una página válida; si no, se descarta y no cuenta como evidencia.
> 3. Si cambian los permisos, los mensajes con fuentes que el usuario ya no puede consultar se devuelven retenidos (sin texto) y no se reenvían al modelo; sin acceso al pedido o al país, 404.
> 4. La eliminación a 30 días distingue datos activos, copias de seguridad y retención del proveedor, sin prometer el borrado inmediato de todas las copias.

### Contrato — `openapi.yaml` 0.16.0-borrador (propuesto)

| Operación | Detalle |
|---|---|
| `POST /pedidos/{pedidoId}/asistente/conversaciones` | Abre una conversación sobre ese pedido. 201 `Conversacion` {`id`, `pedidoId`, `numeroPedido`, `creadaEn`, `expiraEn`, `preguntasRestantes`} |
| `POST /asistente/conversaciones/{conversacionId}/mensajes` | Cuerpo {`pregunta`: 1–2000}. 200 `RespuestaAsistente` {`texto`, `evidenciaSuficiente`, `referencias[]` {`numeroPedido`, `etapa?`, `archivoId?`, `nombreArchivo?`, `pagina?`, `textoCitado?`}, `documentosExcluidos[]` {`archivoId`, `nombreArchivo`, `motivo`: `FORMATO_NO_ADMITIDO` \| `CONTEXTO_EXCEDIDO` \| `NO_AUTORIZADO_PILOTO`}, `preguntasRestantes`} |
| `GET /asistente/conversaciones?pedidoId=` | Conversaciones propias y vigentes (paginado) |
| `GET /asistente/conversaciones/{conversacionId}` | Una conversación propia con sus mensajes, si los permisos se mantienen |
| `GET /asistente/consumo?desde&hasta` (admin) | Consultas, tokens y costo por día y usuario; sin texto |
| Errores | 400 `VALIDACION`, `CONTEXTO_EXCEDIDO`, `LIMITE_PREGUNTAS`; 403 `SIN_PERMISO` (rol, país, pedido); **404 para una conversación ajena o inexistente** (no revela que existe); 429 `LIMITE_CONSUMO`; 503 `IA_NO_DISPONIBLE` |
| Reglas | `documentosExcluidos` solo lista documentos que el usuario **puede** descargar, para no revelar los demás. Sin `modelo`, claves ni detalles del proveedor en las respuestas. Sin streaming en esta entrega |

### Datos — V19 en adelante (propuesto; sin reservar números)

| Tabla | Columnas principales | Reglas |
|---|---|---|
| `configuracion_ia` | Fila única: `habilitada`, `modelo`, límites aprobados de F7-D2 (`tokens_entrada_max` 60000, `tokens_salida_max` 4000, `preguntas_por_conversacion` 10, `consultas_usuario_dia` 20, `en_curso_usuario` 1, `consultas_dia` 200, `presupuesto_mes_micro_usd` 10000000), `retencion_dias` 30, `origen`, `version` | Como `configuracion_plazos`; el admin la cambia con versión. La clave **no** va aquí: solo en el entorno |
| `precio_modelo_ia` | `modelo`, precios en micro-USD por millón de tokens (entrada, salida, escritura y lectura de caché, más el tramo superior de Haiku 5.5), `fuente`, `vigente_desde` | Sin precio, la IA no responde |
| `presupuesto_ia` | `periodo` (mes), `limite`, `consumido`, `reservado` | Reserva atómica (sección F7-D2) |
| `uso_ia_usuario_dia`, `uso_ia_dia` | Contadores de consultas y `en_curso` | Mismo `UPDATE … WHERE` condicional |
| `conversacion_ia` | `id`, `usuario_id`, `pedido_id`, `pais_codigo`, `creada_en`, `expira_en`, `preguntas` | Índice por `expira_en` para el borrado |
| `mensaje_ia` | `id`, `conversacion_id` (cascada), `orden`, `rol`, `texto`, `referencias` (jsonb), `documentos_excluidos` (jsonb), `creado_en` | Se borra con la conversación |
| `consulta_ia` | `id`, `conversacion_id` (`ON DELETE SET NULL`), `usuario_id`, `pais_codigo`, `pedido_id`, `modelo`, los cuatro contadores de tokens, `costo_micro_usd`, `resultado`, `documentos_enviados` (uuid[]), `creada_en` | Solo inserción; sin texto |
| Colombia | Definiciones CO/A y CO/B, bodegas y calendario | **Siguen bloqueadas**: falta la definición del cliente y la contradicción con "procesos distintos" sigue abierta |

El manifiesto del piloto **no** va en la base: es un archivo local revisado por Richard, y el servidor solo lee sus `sha256`.

### ADR propuestos (estado `proposed`; el número se asigna al crearlos, desde 0020)

| Propuesta | Decide | Enlaces |
|---|---|---|
| **A. Claude API detrás de `AsistenteIA`** | Módulo `asistente`; adaptador con el SDK oficial de Java; credenciales propias (`DECOKASA_CLAUDE_API_KEY`, cliente con `.apiKey()`, nunca `fromEnv()`); modelo configurable, inicial `claude-haiku-5-5`; **sin cambio automático a modelos más caros**; IA desactivada por defecto | 0001, 0011, 0015 |
| **B. Consulta por pedido con permisos en el servidor y respuestas citadas** | Conversación de un solo pedido (F7-D1); roles de F7-D4; país, pedido y P-17 comprobados antes de enviar y antes de mostrar; citas por página; sin herramientas de escritura; documentos como contenido, no como instrucciones | 0004, 0007, 0019 |
| **C. Presupuesto y límites con reserva atómica** | Reserva del peor caso antes de llamar, liquidación con `usage`, criterio conservador ante fallos, respaldo del workspace y apagado automático | 0012 |
| **D. Piloto aislado y conservación de 30 días** | Entorno `PILOTO_FICTICIO`, manifiesto con `sha256`, solo PDF, borrado de copias y referencias; producción y documentos reales quedan para F7-D6 | 0015 |
| E. Acceso por país (Colombia) | Ya propuesto (P7-A3). Se mantiene **pendiente** hasta las definiciones de Colombia | 0002, 0009 |

## Pendientes (en orden de bloqueo)

1. ~~F7-D1~~ — aprobada el 2026-10-22 (opción C).
2. ~~F7-D2~~ — límites del piloto aprobados y modelo `claude-haiku-5-5` confirmado (2026-10-22), pendiente de verificar su disponibilidad.
3. ~~F7-D3, F7-D4 y F7-D5~~ — aprobadas el 2026-10-22 (F7-D5 solo para el piloto).
4. ~~Revisión conjunta~~ — propuestas consolidadas aprobadas para el piloto y aplicadas en la documentación (2026-10-22).
5. **F7-D6** — aprobación para documentos reales: quién la da (el manual y 4.7 dicen "el cliente"), cómo se registra, qué cubre, y la región o retención aceptable. Incluye leerlos desde Drive.
6. **Colombia (cliente):** alcance de "mismos flujos" frente a "procesos distintos"; bodegas; calendario; responsables y asignaciones por país; quién elige país; formato `DK-CO-AAAA-NNNN`.

---

## Pasos de la fase (parte A; sin tareas de pruebas ni verificación)

### Paso 7.1 — Definiciones (este documento)
- **Hecho (2026-10-22):** `requerimientos.md` §5 y 4.7; PRD (fila y detalle de la fase 7, preguntas abiertas y registro de decisiones); este plan con P7-C/P7-D/P7-A y F7-D1.
- **No hecho a propósito:** `openapi.yaml`, migraciones y ADR. Se aplican cuando Richard apruebe las propuestas.

### Paso 7.2 — Datos y permisos de Colombia (implementado el 2026-10-22, sin verificar; ver "Resultado del paso 7.2")
- **ACTION:** sembrar CO/A y CO/B con la skill `agregar-flujo-decokasa` (§1, alta) solo con lo confirmado; aplicar el acceso por país (P7-A3) en `ApoyoPedidos.lector` y en las consultas; permitir CO en `CrearPedidoCasoDeUso` cuando haya definición vigente.
- **MIRROR:** V17 (alta con bloque `DO`); `Autorizacion` de usuarios (estado vigente en la base, lección A1).
- **GOTCHA:** no copiar documentos ni plazos de EC por analogía; no tocar las definiciones EC.

#### Datos mínimos que faltan para 7.2 (2026-10-22)

Se da por confirmado (Richard): mismos flujos A/B y roles que Ecuador. La diferencia con "procesos distintos" del documento del cliente sigue registrada como pendiente.

**Se reutiliza sin preguntar:** las definiciones EC/A v1 y EC/B v1 (etapas, roles, transiciones, revisoras, llegada y documentos decididos por Richard) se copian como CO/A v1 y CO/B v1, conservando como `PENDIENTE` los plazos que siguen pendientes en Ecuador; el motor, la habilitación y los permisos; la numeración por país (`secuencia_pedido` por país y año: `DK-CO-AAAA-NNNN`, sujeta al mismo formato pendiente que `DK-EC`); la carpeta de Drive por país. La hora: los plazos usan `America/Guayaquil` (UTC−5), el mismo desfase que Colombia (UTC−5, sin horario de verano), así que el cálculo horario no cambia.

**Requiere valores propios de Colombia:**
1. **Bodegas:** nombre y ciudad de cada bodega de Colombia. `bodega` no tiene país (V9): 7.2 añade `pais_codigo` y asigna EC a Quito, Guayaquil e Ibarra.
2. **Calendario:** feriados de Colombia, o confirmar que por ahora no se descuentan (como en Ecuador, donde siguen pendientes), y jornada laboral si difiere.
3. **Asignaciones:** qué usuarios tienen Colombia (solo CO o EC y CO) y con qué roles. Por ejemplo, si Compras China y Logística son las mismas personas en ambos países. Los datos reales van en la semilla local, fuera de Git (ADR-0015).

### Paso 7.3 — Pantallas por país (implementado el 2026-10-22, sin verificar; ver "Resultado del paso 7.3")
- Selector de país para usuarios con más de un país; bandejas, pedidos, rastreo y cronograma por país activo; `useDefinicionesVigentes` por país. shadcn y `DESIGN.md`.

### Paso 7.4 — Integración de IA (implementado el 2026-10-22, sin verificar; ver "Resultado del paso 7.4")
- **ACTION:** módulo `asistente`: puerto de entrada `ConsultarAsistente`, puerto de salida `AsistenteIA`, adaptador `ClaudeAsistenteIA` (SDK de Java), adaptador falso, `consulta_ia`, configuración `decokasa.ia.*`.
- **MIRROR:** módulo `archivos` (puertos, adaptador Google aislado, protección de secretos en `toString`); `configuracion_plazos` (fila única con versión).
- **GOTCHA:** citas incompatibles con `output_config.format`; en Opus 5.5 no hay `thinking: disabled` ni prefill; comprobar `stop_reason` (incluido `refusal`) antes de leer; nunca registrar la clave, el documento ni el texto.

#### Resultado del paso 7.4 (parte A, 2026-10-22) — pendiente de verificación por Codex

Skills: `claude-api` (SDK de Java, consultado en el repositorio oficial `anthropics/anthropic-sdk-java`, versión 3.1.0) y `ecc:hexagonal-architecture`; revisión `ecc:java-reviewer`. **Sin pruebas, builds, migraciones ni llamadas a Claude API**; sin modificar pruebas; sin commit. `Header.tsx` y los cambios ajenos se conservan. **La integración no está verificada**: el código no se ha compilado.

**Qué existe:**
- **Módulo `asistente`** (hexagonal; R1, R2, R4 y R9):
  - `domain`: `ConversacionIA` (10 preguntas, vencimiento), `MensajeIA` (con `retenidoPara`), `Referencia`, `DocumentoExcluido`, `RolesAsistente` (F7-D4), `VerificadorReferencias`, `FuenteEnviada`, `CitaPropuesta`, `FalloProveedorIA` y los errores `IaNoDisponible` (503) y `LimitePreguntas` (400), añadidos a `CodigoError` y `ManejadorErrores`.
  - Puertos de salida: `AsistenteIA` (responder y contar tokens), `ConversacionesIA`, `RegistroConsultasIA`, `ManifiestoPiloto`, `PresupuestoIA`, y `CuentasParaAsistente`, `PedidosParaAsistente` y `DocumentosParaAsistente`, que usan los puertos de entrada de usuarios, pedidos y archivos.
  - Casos de uso: `AbrirConversacion`, `ConsultarConversaciones`, `BorrarConversacionesVencidas` y `Preguntar`.
- **Adaptador `ClaudeAsistenteIA`:**
  - Solo `AnthropicOkHttpClient.builder().apiKey(DECOKASA_CLAUDE_API_KEY).baseUrl(...)`, nunca `fromEnv()`.
  - `maxRetries(0)`, modelo configurable, sin `fallbacks`, advisor ni herramientas, y sin `thinking` explícito.
  - Documentos como bloques `document` con citas (PDF en base64 y los datos del pedido como texto), y la pregunta al final.
  - Comprueba `stop_reason` (incluido `refusal`) y lee `usage`.
  - Clasifica las excepciones tipadas sin guardar mensajes del proveedor. Se cierra al apagar.
  - Con `IA_HABILITADA=false`, sin clave o sin modelo, el bean es `AsistenteIADesactivado`: no se construye ningún cliente.
- **Rutas publicadas (no llaman al modelo):**
  - `POST /pedidos/{id}/asistente/conversaciones`: 503 con la IA desactivada.
  - `GET /asistente/conversaciones?pais&pedidoId`.
  - `GET /asistente/conversaciones/{id}`.
- **Sin ruta capaz de gastar:**
  - `PreguntarCasoDeUso` no se registra como bean y no hay `POST …/mensajes`.
  - `PresupuestoIA` solo tiene `PresupuestoPendiente`, que falla cerrado antes de cualquier llamada.
  - `GET /asistente/consumo` no existe.
- **V20** (`V20__asistente_ia_piloto.sql`, sin aplicar):
  - `conversacion_ia`, con número y país del pedido comprobados por un trigger y vencimiento de 720 horas como máximo.
  - `mensaje_ia`, inmutable y borrado en cascada.
  - `consulta_ia`, de solo inserción, sin texto, que conserva el registro con `conversacion_id` nulo.
- **Permisos (estado vigente en la base, en cada operación):**
  - Una conversación ajena, vencida o inexistente responde 404, y esto se comprueba primero.
  - Después se exigen cuenta activa y un rol de F7-D4 (403), y país asignado (404).
  - El pedido se revalida con `ConsultarPedidos.detalle` (P-13 y país).
  - Al leer una conversación, un mensaje se retiene (sin texto, evidencia, referencias ni excluidos) si alguna de sus fuentes ya no se puede descargar (P-17 con `DescargarArchivo.autorizar`).
  - El historial que se reenviaría solo lleva pares pregunta-respuesta sin fuentes retenidas.
- **`Preguntar` (escrito para 7.5):**
  - Cada consulta envía primero los datos del pedido, como documento de texto citable con un tramo por etapa, sin personas ni observaciones.
  - Después envía los PDF de las etapas que la persona puede descargar. Los demás ni se nombran.
  - Fuera de `IA_ENTORNO=PILOTO_FICTICIO`, o con `IA_DOCUMENTOS_REALES_AUTORIZADOS=true`, no se envía ningún documento (`NO_AUTORIZADO_PILOTO`).
  - Requisitos de cada PDF: tipo detectado `application/pdf` (si no, `FORMATO_NO_ADMITIDO`); entrada en el manifiesto con `sha256` y páginas; **SHA-256 recalculado sobre el contenido enviado** igual al del manifiesto; tope por documento y total (si no, `CONTEXTO_EXCEDIDO`).
- **Referencias verificadas:**
  - Una cita de PDF vale si apunta a un documento enviado en esa consulta y a una página inicial entre 1 y las páginas del manifiesto.
  - Una cita de los datos del pedido vale si su texto coincide con el tramo enviado.
  - Las demás se descartan. Sin ninguna válida, `evidenciaSuficiente=false` y la respuesta empieza diciendo que no hay evidencia verificable.
  - Un `refusal` responde "No puedo responder esa pregunta."; un `max_tokens` marca la respuesta como incompleta.
- **Instrucciones fijas** (`InstruccionesAsistente`): los documentos son datos y no órdenes; solo consulta; citar o reconocer la falta de evidencia.
- **Registro sin texto** (`consulta_ia`): actor, fecha, pedido, país, modelo, tokens, costo, resultado y documentos enviados, también tras un fallo.
  - Un fallo dudoso se da por consumido.
  - Si algo falla después de la respuesta, la reserva igualmente se cierra y el consumo se registra.
- **Sin textos en los logs:**
  - `toString` redactado en los records con contenido.
  - Los errores SQL o JSON de los mensajes se reemplazan por uno sin causa.
  - Solo se registra la causa clasificada del proveedor.
- **Conservación:** tarea diaria (`BorradoAsistenteConfig`, activa por defecto incluso con la IA desactivada) que borra las conversaciones vencidas por lotes.
- **Frontend:** `api/asistente.ts` con esquemas Zod y claves `['sesion', s, 'asistente', 'pais:XX', …]`.
  - Una respuesta tardía de otra cuenta o de otro país no se guarda: la mutación fija sesión y país al enviar.
  - Sin pantalla todavía.
- **Configuración:**
  - `build.gradle` incluye `com.anthropic:anthropic-java:3.1.0`.
  - `application.yml`, `.env.example` y `docker-compose.yml` añaden `IA_ENTORNO` y `IA_DOCUMENTOS_REALES_AUTORIZADOS=false`; `IA_HABILITADA` sigue en `false`.
  - `.gitignore` incluye `/piloto-ia/`.
- **Contrato:** 0.18.0-borrador, con `pais` en la lista y la indicación de qué rutas siguen sin publicar.

**Revisión `ecc:java-reviewer` (2026-10-22):** sin críticos, sin caminos que gasten y sin fugas entre usuarios o países.
- **ALTO 1, no aplicado.** El revisor no tenía el jar y supuso que el método se llama `addUserMessageOfBlockParams`. El código fuente oficial (3.1.0) tiene `addUserMessageOfContentBlockParams` en `MessageCreateParams` y en `MessageCountTokensParams`, y no `…OfBlockParams`. **La compilación de Codex lo decide.**
- **Corregidos:**
  - M2: errores SQL o JSON sin texto en los logs.
  - M3: `toString` redactado.
  - M4: registro y cierre de la reserva tras la respuesta.
  - M5: documento ilegible omitido y tope total de bytes.
  - M6: SHA-256 del contenido contra el manifiesto.
  - B9: el asistente desactivado libera la reserva.
  - B10: el cliente se cierra al apagar.
  - B11: 720 horas en V20.
  - B14: título acotado.
- **Aceptados o para 7.5:**
  - M7: tope de conversaciones por usuario (7.5, junto con los límites).
  - M8: la lista no revalida P-13 por cada conversación. Hoy P-13 equivale a cuenta activa con roles y país, y la lista ya filtra por esas dos condiciones; el detalle sí revalida.
  - B12: el arranque falla con valores de IA fuera de rango, a propósito.
  - B13: índices UTF-16 en las citas de texto; falla cerrado.

**Pendiente del paso 7.5 (no hecho a propósito):**
1. Tablas `configuracion_ia`, `precio_modelo_ia`, `presupuesto_ia`, `uso_ia_usuario_dia` y `uso_ia_dia`, y la implementación real de `PresupuestoIA`:
   - `count_tokens` con 60 000 de entrada;
   - reserva atómica del peor caso en micro-USD;
   - 20 consultas por usuario y día, con 1 en curso;
   - 200 al día;
   - US$10 al mes;
   - liquidación con `usage`;
   - reservas abandonadas dadas por consumidas.
2. Detectar el límite del workspace u organización, apagar la IA y devolver 503. Hoy un 429 se clasifica como `LIMITE_PROVEEDOR` sin cobro y no apaga nada.
3. Registrar `Preguntar` como bean y publicar `POST /asistente/conversaciones/{id}/mensajes` (`CONTEXTO_EXCEDIDO`, `LIMITE_CONSUMO`) y `GET /asistente/consumo` (admin), con los códigos `CONTEXTO_EXCEDIDO` y `LIMITE_CONSUMO`.
4. Tope de conversaciones abiertas por usuario (M7) y bloqueo de "1 en curso" antes de reservar.
5. Pantalla del chat, que reutiliza `usePaisActivo` y `api/asistente.ts`, con un hook de preguntar con la misma guarda de sesión y país, y avisos de retenido, sin evidencia y documentos excluidos.
6. Precios de `claude-haiku-5-5` desde la documentación oficial vigente, y comprobar su disponibilidad con `GET /v1/models/claude-haiku-5-5` en la primera llamada **autorizada por Richard**.
7. Sigue pendiente de F7-D6: documentos reales, conservación en producción y retención del proveedor.

**Para Codex (parte B, 7.B1–7.B3):**
- **Compilación:** compilar contra `anthropic-java` 3.1.0 confirmado en Maven Central. Revisar los nombres del SDK que señaló el revisor:
  - `addUserMessageOfContentBlockParams`;
  - `DocumentBlockParam.Builder.textSource`, `citations` y `title`;
  - `CitationCharLocation.startCharIndex` y `endCharIndex`;
  - `AnthropicServiceException.statusCode()`;
  - `StopReason.REFUSAL`;
  - `MessageTokensCount.inputTokens()`.
- **Arquitectura:** ArchUnit R1–R9 con el módulo nuevo, y una regla propuesta que prohíba `fromEnv` en `src/main`.
- **Migración:** V20 sobre una base limpia y sobre V19 con datos. Probar:
  - el trigger de número y país;
  - que una conversación no se pueda editar salvo `preguntas`;
  - que un mensaje no se pueda editar;
  - que `consulta_ia` admita solo inserción y conserve sus filas al borrar la conversación;
  - los checks de orden y evidencia.
- **Rutas y permisos:**
  - abrir con un rol de F7-D4 → 503 con la IA desactivada;
  - abrir con `consulta` o `asistente_china` solos → 403;
  - abrir un pedido de otro país → 404;
  - leer una conversación ajena, vencida o de un país revocado → 404;
  - leer tras perder P-17 de un documento citado → mensaje retenido sin texto;
  - listar con un `pais` no asignado → 403.
- **Rutas ausentes:** `POST …/mensajes` y `GET /asistente/consumo` → 404, y ningún bean `Preguntar` en el contexto.
- **Adaptador falso de `AsistenteIA`** para probar `PreguntarCasoDeUso` sin red:
  - referencias válidas, inválidas (índice o página fuera de rango) y de texto no coincidente;
  - `refusal`, `max_tokens` y los fallos de cada causa;
  - historial sin pares retenidos;
  - exclusiones por formato, manifiesto, huella distinta, entorno no piloto y tamaño.
- **Logs:** no deben aparecer la clave, la pregunta, la respuesta ni los documentos en ningún escenario de error.
- **Borrado:** borrado por lotes y tarea diaria.
- **Frontend:** `api/asistente.ts`. Una mutación que termina tras cambiar de país o de cuenta no escribe en la caché, y las claves llevan `pais:XX`.
- **Ninguna llamada real a Claude API** sin la autorización expresa de Richard.

**Pruebas que probablemente cambien (no modificadas):** las que fijen la última versión de Flyway (V19) o enumeren `CodigoError` o el mapa de estados de `ManejadorErrores`.

### Paso 7.5 — Controles y entrega (implementado el 2026-10-22, sin verificar; ver "Resultado del paso 7.5")
- Límites (`count_tokens` antes de enviar y contabilidad con `usage`), errores tipados, protección frente a instrucciones dentro de documentos u observaciones, y documentación de los datos enviados. Los flujos de Ecuador no cambian.

#### Resultado del paso 7.5 (parte A, 2026-10-22) — pendiente de verificación por Codex

Skills: `claude-api`, `ecc:hexagonal-architecture`, `shadcn` y `tanstack-query-best-practices`; revisión `ecc:java-reviewer` (ver más abajo). **Sin pruebas, builds, migraciones, compras ni llamadas a Claude API**; sin modificar pruebas; sin commit. `Header.tsx` y los cambios ajenos se conservan. `IA_HABILITADA` sigue en `false` y F7-D6 sigue pendiente. **Nada está verificado**: el código no se ha compilado ni la migración aplicado.

**Fuente de los precios.** Se leyó la página oficial https://platform.claude.com/docs/en/about-claude/pricing el 2026-10-22, sin llamar a la API. Precios de `claude-haiku-5-5` por MTok:

| Prompt | Entrada | Salida | Escritura de caché (5 min) | Lectura de caché |
|---|---|---|---|---|
| Hasta 100 000 tokens | $0.10 | $0.50 | $0.125 | $0.01 |
| Más de 100 000 tokens | $0.50 | $2.50 | $0.625 | $0.05 |

La misma documentación dice:
- count_tokens es gratuito, pero su resultado es una **estimación** que puede diferir ligeramente.
- `inference_geo` con solo EE. UU. multiplica los precios por 1.1; no se usa.

**Qué existe:**
- **V21** (`V21__controles_asistente_ia.sql`):
  - `configuracion_ia` con los límites de F7-D2 y `habilitada=false`.
  - `precio_modelo_ia`, con la tarifa oficial sembrada **sin validar** e inmutable salvo su validación.
  - `presupuesto_ia`, `uso_ia_dia`, `uso_ia_usuario_dia` y `reserva_ia`.
- **Controles antes de cualquier llamada** (`ControlesAsistente`, también antes de count_tokens):
  - `IA_HABILITADA=true` con clave y modelo;
  - interruptor de la base activado;
  - tarifa **vigente** del modelo **validada**, sin usar una anterior si la última no lo está;
  - límite de entrada (60 000) bajo el umbral del tramo caro (100 000);
  - espera de reserva mayor que la del proveedor.

  Si falta uno: 503 `IA_NO_DISPONIBLE`.
- **Conteo del contexto** (count_tokens) de instrucciones, historial autorizado, datos del pedido, PDF y pregunta:
  1. Si la base sin PDF supera 60 000 tokens: 400 `CONTEXTO_EXCEDIDO`.
  2. Cada PDF se mide aparte (en el orden de las etapas) y entra si cabe; si no, se excluye con `CONTEXTO_EXCEDIDO`.
  3. Se cuenta la consulta final exacta y, si se pasa, se quitan PDF desde el último.

  `max_tokens` = el menor entre la configuración del entorno y 4 000.
- **Reserva atómica** (`PresupuestoIASql`), en una sola transacción con `FOR UPDATE` y en orden fijo (presupuesto del mes → día → usuario):
  - comprueba 1 en curso, 20 por usuario y día, 200 al día y US$10 al mes con el **peor caso**: tokens contados × precio (el mayor entre entrada y escritura de caché) + 4 000 × precio de salida;
  - si alguno no cabe, 429 `LIMITE_CONSUMO` con su motivo, sin llamar y con el intento registrado como `LIMITE`.
- **Cierre de la reserva:**
  - **Liquidación:** con el costo real del `usage` (tramo según el prompt real).
  - **Liberación:** ante un rechazo confirmado o la IA desactivada; devuelve también la consulta contada.
  - **Dada por consumida:** ante un resultado dudoso o un error inesperado.
  - **Una sola vez:** `UPDATE … WHERE estado='ABIERTA'`.
  - **Caídas:** antes de cada consulta, las reservas abiertas más allá de la espera (300 s, propuesta del equipo) se dan por consumidas (`FOR UPDATE SKIP LOCKED`). Una liquidación tardía ya no cambia nada.
- **Límite de gasto del proveedor:**
  - Lo detecta `ClaudeAsistenteIA.esLimiteDeGasto` (400 "usage limit" o 429 de límite de gasto; el texto no se registra).
  - Libera la reserva, apaga el interruptor de la base con su motivo y responde 503.
  - Lo reactiva un admin (`PATCH /asistente/configuracion`, con versión y tarifa validada).
  - **El límite de US$15 del workspace no se da por configurado**: lo configura Richard en la Console y no consta en el sistema.
- **Rutas** (contrato 0.19.0-borrador):
  - `POST /asistente/conversaciones/{id}/mensajes`;
  - `GET /asistente/consumo` (admin; por día de Ecuador y usuario, solo de sus países, sin textos; el presupuesto del mes es global);
  - `GET`/`POST /asistente/tarifas` y `POST /asistente/tarifas/{id}/validacion` (admin; la fuente debe ser una página https de los dominios oficiales de Anthropic, una lista que es propuesta del equipo);
  - `GET`/`PATCH /asistente/configuracion` (admin).
- **Documentos como contenido:**
  - Cada bloque `document` lleva un `context` que dice que el documento es dato, no instrucción, además del prompt de sistema fijo.
  - Sin herramientas, así que no hay acciones de escritura.
  - Las referencias se verifican en el servidor (7.4).
- **Chat** (`components/asistente/AsistentePedido.tsx`, ruta `/pedidos/:pedidoId/asistente`; acceso desde el detalle para los roles de F7-D4):
  - Conversaciones del pedido y botón "Nueva conversación".
  - Mensajes con distintivo "Con referencias verificadas" o "Sin evidencia suficiente", referencias (archivo, etapa, página y texto citado) y documentos no usados con su motivo.
  - Mensajes retenidos con aviso y sin texto.
  - Errores claros por código (503, 429, 400, 404).
  - Aviso de piloto ficticio y contador de 2 000 caracteres.
  - `usePreguntarAsistente` sin reintentos y con la guarda de sesión y país fijada al enviar. Se compone con Card, Alert y Badge porque los componentes de chat de shadcn no están instalados.

**Lo que no se hizo a propósito:**
- **Ningún límite inventado.** El tope de conversaciones por usuario (M7 de la revisión de 7.4) queda **solo como propuesta**, sin implementar ni documentar como regla.
- **Sin pantalla de consumo, tarifas ni configuración para el admin:** solo existen sus rutas.
- **Sin `Retry-After` en 429.**
- **Sin caché de prompts** (`cache_control`): se valoraría con uso real.

**Hallazgos pendientes de 7.4 que siguen abiertos:**
- **ALTO 1:** la compilación decide los nombres del SDK. La documentación oficial de count_tokens (Java) confirma `addUserMessageOfContentBlockParams` y `MessageTokensCount`.
- **M8:** la lista de conversaciones no revalida P-13 por cada una. Está aceptado: hoy P-13 equivale a cuenta activa con roles y país.
- **B12 y B13:** aceptados.

**Para Codex (parte B):**
1. **Compilación del SDK** `com.anthropic:anthropic-java:3.1.0`, confirmando la versión en Maven Central. Nombres que hay que comprobar:
   - `addUserMessageOfContentBlockParams` en `MessageCreateParams` y en `MessageCountTokensParams`;
   - `addUserMessage` y `addAssistantMessage` en `MessageCountTokensParams`;
   - `DocumentBlockParam.Builder.textSource`, `title`, `citations` y `context`;
   - `CitationsConfigParam.builder().enabled`;
   - `CitationCharLocation.startCharIndex` y `endCharIndex`;
   - `AnthropicServiceException.statusCode()`;
   - `StopReason.REFUSAL`;
   - `MessageTokensCount.inputTokens()`;
   - `AnthropicOkHttpClient.Builder.maxRetries` y `timeout(Duration)`;
   - `AnthropicClient.close()`.
2. **ArchUnit** R1–R9 con el módulo completo, y la regla propuesta que prohíba `fromEnv` en `src/main`.
3. **Contrato:** validar que `openapi.yaml` 0.19.0 sigue siendo YAML/OpenAPI válido (no había PyYAML para comprobarlo).
4. **V20 y V21** sobre una base limpia y sobre V19 con datos. Probar:
   - el bloque `DO`;
   - que una tarifa no se pueda editar ni borrar una vez validada;
   - `reserva_ia` (checks de estado y cierre);
   - `ON DELETE SET NULL` de `consulta_ia`.
5. **Concurrencia** (Testcontainers, hilos):
   - 2 consultas simultáneas del mismo usuario → una pasa y otra 429 `EN_CURSO`;
   - 201 consultas de usuarios distintos el mismo día → la 201 da 429 `DIA`;
   - un presupuesto casi agotado con reservas simultáneas → nunca `consumido + reservado > limite`;
   - una liquidación y una recuperación simultáneas → un solo cierre;
   - sin interbloqueos.
6. **Caídas:** con una reserva `ABIERTA` vieja, la siguiente consulta la da por consumida, devuelve `en_curso` y suma el máximo, y una liquidación posterior no cambia nada.
7. **Adaptador falso de `AsistenteIA`:**
   - sin tarifa validada, con la base apagada o con el entorno desactivado → 503 y **cero** llamadas, ni siquiera count_tokens;
   - base > 60 000 → 400;
   - PDF que no caben → excluidos;
   - rechazo confirmado → liberada;
   - incierto → consumida;
   - límite de gasto → liberada y desactivada;
   - `refusal` y `max_tokens`;
   - liquidación con el tramo correcto;
   - fallo de liquidación → consumida y registrada.
8. **Dominio `Tarifa`:** redondeo hacia arriba en micro-USD, tramo superior (por encima de 100 000), desbordamientos y `admiteLimiteDeEntrada`.
9. **Rutas de admin:** 403 para quien no es admin. Además:
   - tarifa con fuente no oficial o fecha de consulta futura → 400;
   - tarifa duplicada → 409;
   - validar dos veces → 400;
   - activar sin tarifa validada → 400;
   - versión vieja → 409;
   - consumo filtrado por los países del admin y sin textos.
10. **Frontend** (`AsistentePedido`, `api/asistente.ts`):
    - las respuestas tardías tras cambiar de país o de cuenta no se muestran;
    - mensajes retenidos sin texto;
    - distintivos de evidencia, referencias y excluidos;
    - errores por código;
    - accesibilidad (etiquetas, `aria-pressed`, foco, 40 px).
11. **Logs:** no deben aparecer la clave, la pregunta, la respuesta, los documentos ni el texto del error del proveedor.
12. **Ninguna llamada real** a Claude API ni la validación de la tarifa en un entorno real sin la autorización expresa de Richard. Si se valida, hay que comparar antes con la página oficial vigente.

**Revisión `ecc:java-reviewer` del paso 7.5 (2026-10-22).** Sin críticos.
- **Verificado por el revisor:**
  - orden de bloqueo sin ciclos;
  - una reserva no se cierra dos veces;
  - controles antes de count_tokens y de la llamada;
  - todas las salidas cierran la reserva;
  - ArchUnit;
  - permisos de admin y filtro por países;
  - triggers de V21.
- **A1, no aplicado:** el revisor recordaba `addUserMessageOfBlockParams`; la documentación oficial de count_tokens para Java usa `addUserMessageOfContentBlockParams`. Lo decide la compilación de Codex.
- **Corregidos:**
  - **M1:** `precio_modelo_ia_tramo` con `num_nonnulls`, para que no pase un tramo a medias.
  - **M2:** si la verificación de citas falla tras pagar, la respuesta sale como "sin evidencia" y no como 500, y el consumo queda registrado.
  - **M4:** `comprobarDisponibilidad` antes de count_tokens, sin reservar y con los **mismos** límites aprobados; no es un límite nuevo.
  - **M5:** el apagado por límite de gasto va primero, también desde count_tokens.
  - **M6:** lo dado por consumido se registra con el costo reservado.
  - **B1:** `tokens_salida_max` entre 1 y 4 000 en la base.
  - **B4:** un solo día de Ecuador por consulta.
  - **B5:** un fallo al registrar no convierte el 429 en un 500.
- **Aceptados o pendientes de decisión:**
  - **M3:** count_tokens es una estimación (documentación oficial), así que el presupuesto de US$10 puede superarse por la diferencia entre lo estimado y el `usage` real. **Propuesta para Richard, sin aplicar:** reservar con un margen sobre la entrada contada.
  - **B2:** el límite del mes se fija al crear su fila; no hay ruta para cambiarlo.
  - **B3:** la detección del límite de gasto lee el mensaje del error (texto del proveedor). Codex debe confirmar si el SDK expone un código estructurado para sustituirlo.
  - **B6:** el mismo admin puede registrar y validar una tarifa. **Decisión pendiente de Richard:** exigir otra persona o no.
  - **B7:** las reservas huérfanas se recuperan al preguntar, no con una tarea periódica.
  - **Consumo por día:** las reservas recuperadas tras una caída no tienen fila en `consulta_ia`. El consumo por día es informativo; la fuente de verdad del gasto es `presupuesto_ia`.

**Pruebas que probablemente cambien (no modificadas):** las que fijen la última versión de Flyway, enumeren `CodigoError` o el mapa de `ManejadorErrores`, o rendericen `DetallePedido` (nuevo botón) o las rutas.

## NOT Building (fase 7)

- Ninguna acción desde el chat (avanzar, rechazar, subir, borrar o enviar correos).
- Enviar documentos reales sin la aprobación de F7-D6.
- Procesos de Colombia distintos de los de Ecuador sin la definición del cliente.
- Índice vectorial, agentes o Managed Agents.
- Streaming al navegador e historial de conversación persistente (posibles más adelante).

## Risks

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Fuga entre países o pedidos por el chat | Media | Alta | El filtro se aplica en el servidor antes de enviar (P7-A2/A3); 7.2 antes que 7.4 |
| Instrucciones inyectadas en documentos | Media | Media | Sin herramientas de escritura; prompt fijo; citas obligatorias |
| Costos fuera de control | Media | Media | Límites (F7-D2), `count_tokens` previo, IA desactivada por defecto |
| "Mismos flujos" frente a "procesos distintos" | Media | Alta | Confirmar con el cliente antes de 7.2; no copiar por analogía |
| Documentos reales enviados por error | Baja | Alta | `documentos_reales_autorizados=false`; entorno ficticio separado (F7-D3) |
| Excel o imágenes sin página | Alta | Baja | Referencia al documento sin página, como piden los requisitos |

## Notes

- La parte B (7.B1–7.B3) la hace Codex con el adaptador falso; solo hay llamadas reales con datos que autorice Richard.
- Se mantienen los ⏸ y [P]. Ninguna decisión de este plan se presenta como respuesta del cliente.

## Resultado del paso 7.2 (parte A, 2026-10-22) — pendiente de verificación por Codex

Rama `fase-7-colombia-ia`. Hecho con la skill `agregar-flujo-decokasa` (§1, alta de un flujo nuevo). **No se ejecutaron pruebas, builds, migraciones ni llamadas externas**, y no se modificaron pruebas. No hay commit. `Header.tsx` y los cambios ajenos se conservan. No se implementaron 7.3 ni la IA.

**Decisiones de Richard aplicadas (2026-10-22):**
- Colombia con los mismos flujos A/B y roles que Ecuador.
- "Bodega 1" temporal, con la ciudad pendiente.
- Calendario con las mismas reglas provisionales y configuración por país.
- Mirian Franco con Ecuador y Colombia; ella asigna los países a los demás, y nadie recibe Colombia automáticamente.
- Autorización vigente en el servidor.

Lo que es diseño del equipo está en ADR-0024 y queda pendiente de la revisión de Codex.

**Archivos:**
- **Migración** `V19__colombia_flujos_y_bodegas_por_pais.sql`:
  - CO/A v1 y CO/B v1 copiadas fila por fila de Ecuador, con "Aduana Colombia".
  - `bodega.pais_codigo`; ciudad nula solo si está pendiente; nombre único por país.
  - "Bodega 1" en Colombia.
  - Bloque `DO` que compara CO con EC (etapas, transiciones y documentos), comprueba que Ecuador no cambió, que ningún pedido apunta a CO y que ningún pedido tiene bodega de otro país.
  - Sin usuarios.
- **usuarios:** `ConsultarResponsables.EstadoVigente` gana `paises` (constructor anterior = EC); `ConsultarResponsablesCasoDeUso` los lee de `usuario`.
- **pedidos:**
  - `ActorVigente` gana `paises` y `veePais` (constructor anterior = EC).
  - `ApoyoPedidos`: `exigirPais` (404) y `vencimientos(pais)`.
  - Casos de uso con el país comprobado: `ConsultarPedidos` (detalle, número, listar y bandeja), `VerificarConsulta` (historial), `AutorizarArchivos` (subida, listado y descarga), `ConsultarCronograma` (más el calendario del país), `EjecutarAccionEtapa` (más la bodega del país del pedido), `AgregarObservacion`, `ReasignarEtapa` y `RegistrarMotivoRetraso`.
  - `CrearPedido`: CO permitido si el país está asignado (403 si no), antes de reservar el número.
  - `ConsultaDePedidos`: `listar` y `bandeja` con países (por defecto, filtro en memoria para dobles de prueba). `PedidoJpaRepositorio` filtra en la consulta (JPQL `p.paisCodigo in :paises`; SQL nativo `p.pais_codigo in (:paises)`).
- **plazos:** `CalendariosPorPais` (EC `America/Guayaquil`, CO `America/Bogota`); `CalcularVencimiento.paraPais` y `CalcularCronograma.paraPais` (por defecto, el mismo); casos de uso con su constructor nuevo y el anterior; `PlazosConfig`.
- **bodegas:**
  - `Bodega` gana `pais`, la ciudad puede ser nula y `nueva(pais, …)` sigue exigiendo ciudad.
  - Nuevo puerto `ConsultarBodegasPorPais` (`GET /bodegas`).
  - `MantenimientoAutorizado` exige el país de la bodega.
  - Crear y actualizar trabajan por país (nombre único por país).
  - `BodegaActiva` y `Vista` ganan `pais`.
  - Entidad, Spring Data, repositorio, controlador (`?pais`, `pais` en el alta), respuesta y configuración.
- **Contrato:** `openapi.yaml` 0.17.0-borrador.
- **Documentación:** ADR-0024 e índice, `CLAUDE.md` (24 ADR), `requerimientos.md` §5, `modelo-datos.md`, `arquitectura.md` 4.9-bis.
- **Frontend (solo compatibilidad, no es 7.3):** `esquemaBodega` admite `ciudad` nula y `pais`, y `ciudadDeBodega` muestra "Ciudad pendiente" en el catálogo, el detalle, el diálogo de acción y el historial. Sin selector de país.

**Límites conocidos (sin cambiar aquí):**
- `GET /bodegas` ahora exige una cuenta activa con roles, antes "cualquier autenticado". Es un cambio de contrato documentado.
- El "hoy" de `REPORTAR_DEMORA` y el año de la numeración siguen con la hora de Ecuador: es el mismo desfase que Colombia, así que el resultado no cambia.
- La meta de días sigue siendo una sola.
- ~~Las copias fallidas de Drive y los correos del admin no filtran por país.~~ Resuelto en "Cierre de la autorización por país" (más abajo).
- Sin selector de país, quien tenga dos países ve los dos mezclados en las pantallas (7.3).

### Procedimiento seguro para asignar países (no ejecutado)

Las asignaciones **no** van en migraciones ni en Git (ADR-0015, ADR-0024), y nadie recibe Colombia por defecto.

1. **Antes:** confirmar que la migración V19 está aplicada en esa base (la aplica Flyway al arrancar el backend; Richard decide cuándo) y que la cuenta que asigna es admin y está activa.
2. **Mirian Franco (Ecuador y Colombia):** con su sesión de admin en la aplicación:
   1. `GET /api/v1/usuarios/{suId}` para leer su estado y su `version`.
   2. `PATCH /api/v1/usuarios/{suId}` con `{"paises": ["EC", "CO"], "version": <leída>}`.
   3. Si responde 409 `VERSION_DESACTUALIZADA`, **no se repite la edición automáticamente**:
      - se recarga el usuario y se revisa su estado actual (países, roles, si está activo) y qué cambió;
      - la persona decide si el cambio sigue siendo correcto;
      - solo entonces confirma de nuevo, con la versión recién leída.

      Nunca se fuerza ni se reintenta en bucle (el mismo criterio que D10 en la pantalla de usuarios: "Cargar datos actuales").
   
   La base local de desarrollo usa usuarios ficticios (semilla fuera de Git): ahí se prueba con "Admin Prueba", nunca con la cuenta real.
3. **Otros usuarios:** solo Mirian (u otro admin con el país) asigna Colombia, con el mismo `PATCH` y la misma regla ante un 409, uno por uno y por decisión expresa. Para quitarlo, `paises: ["EC"]`.
4. **Efecto:** el servidor lee los países en cada petición, así que no hace falta volver a iniciar sesión. Quien pierde Colombia deja de ver sus pedidos al instante (404).
5. **Comprobación:**
   - `GET /api/v1/usuarios/{id}` muestra `paises`;
   - con esa cuenta, `GET /api/v1/bodegas?pais=CO` devuelve "Bodega 1";
   - una cuenta sin CO recibe 403 en esa misma llamada.
6. **Nunca:** `UPDATE usuario_pais` a mano, datos reales en semillas versionadas ni asignaciones en Flyway.

### Pruebas existentes que probablemente cambien (no modificadas)

- **Versión de Flyway y conteos globales** (ahora V19, 4 definiciones vigentes y una bodega más): `MigracionesIT`, `V2RestriccionesIT`, `MigracionesFase4IT`, `MigracionesFase5IT` y `MigracionesFase6IT`.
- **Usos de `CO` o de la regla "solo Ecuador"**: `MigracionesIT`, `V2RestriccionesIT` y `NucleoFaseDosTest`. Crear en CO ya no responde 400: con CO asignado se crea y, sin él, 403.
- **Bodegas** (nombre único por país, mensaje "en ese país", `GET /bodegas` con cuenta activa y país, respuesta con `pais`): `Fase4IT`, `Fase6IT`, `PersistenciaFaseDosIT`.
- **Calendario**: `CalendarioFaseDosTest`, si construye los casos de uso; los constructores anteriores siguen existiendo.
- **Frontend** (bodega con `ciudad` nula o `pais`): `Fase4Pantallas.test.tsx` y `Fase4Ramas.test.tsx`, si comparan la forma exacta.

### Casos para Codex (7.B1, parte del manual)

1. **V19** sobre base limpia y sobre la base con pedidos de EC:
   - CO/A tiene 11 etapas y CO/B 12, iguales a EC salvo "Aduana Colombia";
   - EC intacto; "Bodega 1" en CO con la ciudad nula;
   - nombre de bodega repetido en otro país, permitido; en el mismo país, 409;
   - el bloque `DO` falla ante datos alterados.
2. **`GET /flujos/CO/A` y `GET /flujos/CO/B`.**
3. **Acceso por país** con cuentas ficticias (solo EC, solo CO, EC y CO, y admin solo EC):
   - detalle, búsqueda por número, acciones, observaciones, reasignación, motivo de retraso, archivos (subida, listado y descarga), historial y cronograma de un pedido de otro país responden 404;
   - listados y bandeja solo con sus países, con paginación y total correctos.
4. **Creación:** CO con CO asignado da `DK-CO-AAAA-0001` con su propia secuencia; sin CO, 403 sin consumir número; EC sigue igual.
5. **Bodegas:**
   - `GET /bodegas` con y sin `pais`, y con un país no asignado (403);
   - alta y edición por Logística o admin con y sin el país;
   - elegir una bodega de otro país da 400 en `bodegaId`.
6. **Plazos** de un pedido CO: los mismos vencimientos que EC; cronograma calculado.
7. **Cambio de países en caliente** (quitar CO): el siguiente acceso da 404.
8. **Regresión completa de Ecuador** (A y B), sin cambios de comportamiento.

## Cierre de la autorización por país (paso 7.2, ajuste del 2026-10-22) — pendiente de verificación por Codex

Pedido por Richard antes de avanzar a 7.3. Skills: `ecc:springboot-security` y `ecc:java-reviewer`. Sin pruebas, builds, migraciones ni llamadas externas; sin modificar pruebas; sin commit.

**Hecho:**
- **Correos del admin** (`GET /notificaciones`): `ConsultarNotificacionesCasoDeUso` pasa los países del admin vigente; `RegistroNotificaciones.listar` y `contar` tienen una versión con países (abstracta), que `NotificacionSqlRepositorio` implementa con `join pedido` por `pais_codigo`, así que la lista y el total salen del mismo filtro.
- **Copias de Drive del admin** (`GET /archivos/copias-fallidas`): `ConsultaCopias.fallidos` exige los países (se quitó la firma sin países); `ConsultaCopiasSql` filtra con `join pedido`, y la página se arma después del filtro.
- **Operaciones por identificador:**
  - `GET /archivos/{id}/contenido` y `GET /archivos/{id}/enlace` pasan por `autorizarDescarga`;
  - el listado de archivos pasa por `autorizarConsulta`;
  - las cargas pasan por `autorizarConsulta` y `autorizarSubida`, y además exigen que la carga sea del solicitante.

  Todas ellas comprueban el país desde 7.2 (404). Un identificador conocido no da acceso.
- **Sin registros tocados:** no se borra ni se reasigna ningún correo, copia o trabajo pendiente; el envío y la copia siguen su curso.
- **Ruta sin filtro retirada:** el bean `ConsultarBodegas`, que listaba sin país, ya no se publica.
- **"Sin país equivale a EC":** aclarado en el código (Javadoc de los constructores de compatibilidad), en ADR-0024 (punto 8) y en el contrato. Es solo compatibilidad documentada y nunca reemplaza la autorización vigente.
- **Procedimiento de asignación:** ante un 409 se recarga, se revisa el estado y se confirma de nuevo de forma expresa; nunca se repite la edición automáticamente.
- **Contrato** 0.17.0-borrador (textos de `/notificaciones` y `/archivos/copias-fallidas`, resumen de permisos y párrafo de cambios); ADR-0024 (punto 5-bis, punto 8 y riesgos); `arquitectura.md` 4.9-bis.

**Pruebas que probablemente cambien (no modificadas):** `CorreoB2IT` (mock de `RegistroNotificaciones`: ahora se llama `listar` y `contar` con países), y las que lean copias fallidas o notificaciones de pedidos de un país no asignado al admin.

### Casos adicionales para Codex (7.B1)

9. **`GET /notificaciones`:**
   - admin solo EC con correos de pedidos EC y CO: ve solo EC, y el `total` coincide con EC;
   - admin con EC y CO: ve los dos;
   - el filtro `estado` se combina con el país;
   - la paginación no deja huecos;
   - los correos CO siguen en la base sin cambios.
10. **`GET /archivos/copias-fallidas`:** lo mismo con adjuntos FALLIDO de EC y CO; ninguna copia cambia de estado.
11. **Identificador conocido de otro país:**
    - `GET /archivos/{id}/contenido` y `GET /archivos/{id}/enlace` de un archivo CO con admin solo EC dan 404;
    - las cargas de otro país dan 404 o 403 según el caso actual, sin revelar datos.
12. **Cambio de países en caliente:** al quitar CO al admin, la siguiente consulta ya no muestra correos ni copias CO.
13. **Compatibilidad sin autorización:** un usuario sin EC asignado que hace `POST /pedidos` o `POST /bodegas` sin `pais` recibe 403.
14. **Procedimiento ante un 409:** con dos ediciones concurrentes de los países de un usuario, la segunda recibe 409 y no se aplica nada hasta una nueva confirmación con la versión recargada.

### Revisión con `ecc:java-reviewer` (2026-10-22, solo lectura; sin compilar) y correcciones

El revisor no encontró rutas de lectura sin comprobación de país ni errores de compilación.

**Corregido:**
- **ALTO:** `EjecutarAccionEtapaCasoDeUso` comprobaba `finalizado` y `etapaEsperada` antes del país, así que un pedido de otro país respondía 409 y revelaba su estado. Ahora el país (404) va primero.
- **MEDIO:**
  - Reasignar a una persona sin el país del pedido responde 400 en `usuarioId`.
  - Editar una bodega autoriza el rol antes de bloquear la fila, y una bodega de otro país responde 404 (antes, 403).
  - `buscarPorNombre` sin país busca en Ecuador (compatibilidad) y ya no puede fallar con dos bodegas del mismo nombre en países distintos. Se retiró `findByNombreIgnoreCase`.
  - V19, todavía sin aplicar, gana el trigger `pedido_bodega_mismo_pais` como última barrera en la base.
- **BAJO:** un archivo de otro país responde igual que uno inexistente ("El archivo no existe.") en la descarga y en el enlace de Drive.

**Pendiente, con motivo:**
- Los constructores de compatibilidad siguen siendo públicos. Moverlos a `src/test` o crear una regla ArchUnit exige cambiar pruebas, que no se tocan en la parte A. Propuesta para Codex: una regla ArchUnit que prohíba usarlos desde `src/main`.
- `GET /archivos/copias-fallidas` no devuelve total: el contrato responde un arreglo. Añadirlo es un cambio de contrato y queda a decisión de Richard.
- El "hoy" de la demora usa la zona de Ecuador, que tiene el mismo desfase que Colombia; sacarlo de `CalendariosPorPais` queda para cuando las zonas difieran.

**Casos para Codex:**
15. Acción sobre un pedido de otro país, finalizado o con otra etapa esperada: 404, nunca 409.
16. Reasignar a una persona sin el país del pedido: 400.
17. Editar una bodega de otro país: 404 sin bloquear la fila; sin rol: 403.
18. El trigger rechaza `bodega_id` de otro país con una escritura directa.

## Resultado del paso 7.3 (parte A, 2026-10-22) — pendiente de verificación por Codex

Pantallas por país. Skills: `agregar-flujo-decokasa`, `shadcn` y `tanstack-query-best-practices`. **Sin pruebas, builds, migraciones ni llamadas externas**; sin modificar pruebas; sin commit. `Header.tsx` y los cambios ajenos se conservan. Sin el chat de IA.

**Frontend:**
- **`pais/PaisProvider.tsx` (nuevo):**
  - **Países vigentes:** solo se ofrecen los asignados hoy. Se releen con `GET /auth/yo` cada 30 s y al volver a la pestaña, y en el acto ante cualquier 403 o 404 del servidor. Mientras no hay respuesta, se usan los de la sesión.
  - **Selección:** con un solo país queda elegido sin ofrecer otros. Sin ninguno, no se muestra nada. Con varios, hay que elegir (`PuertaPais`). La elección se recuerda por usuario en `sessionStorage` (con `try/catch`); es solo una comodidad y siempre se valida contra los países vigentes.
  - **Cambio de país:** se cancelan y se borran de la caché todas las consultas del país anterior (predicado sobre el segmento `pais:XX`) y el contenido se vuelve a montar (`key` por país), así que ninguna respuesta tardía llega a la pantalla.
  - **Revocación:** si el país elegido deja de estar asignado, sus consultas se cancelan y se borran, aparece un aviso y se exige otra selección autorizada.
  - **Cambios sin guardar** (`useCambiosSinGuardar`): borradores del detalle, pedido nuevo y bodega en edición. Antes de cambiar de país aparece un diálogo de confirmación ("Seguir en …" / "Descartar y cambiar"); nunca se trasladan a otro país.
- **Selector** (`SelectorPais`, shadcn `Select`) arriba del menú (`Sidebar.tsx`), y `App.tsx` envuelve menú y contenido con el proveedor y la puerta.
- **Claves de caché con país:** pedidos (lista, bandeja, conteos, detalle, historial, archivos y cronograma), bodegas, correos y copias fallidas, siempre con el segmento `pais:XX`. Las definiciones vigentes son las del país activo (`/flujos/{pais}/{tipo}`). La meta invalida los cronogramas de cualquier país.
- **Peticiones con el país:** listado, bandeja, búsqueda por número, bodegas (lista y alta), correos y copias envían `pais`. La creación de pedidos es siempre en el país activo.
- **Detalle de un pedido de otro país:** no muestra ningún dato; dice que el pedido no es del país activo y ofrece cambiar de país solo si está asignado, con la misma confirmación.
- **Bodega con la ciudad pendiente:** se puede editar (activar, renombrar) sin inventar la ciudad.

**Backend:**
- `GET /pedidos`, `/pedidos/bandeja`, `/pedidos/buscar`, `/notificaciones` y `/archivos/copias-fallidas` aceptan `pais`. Si no está asignado hoy, 403; sin él, todos los asignados (firmas anteriores conservadas).
- En la búsqueda por número, un pedido de otro país responde 404.
- `GET /bodegas?pais` ya existía (7.2).

**Contrato:** 0.17.0-borrador, con el parámetro común `PaisSeleccionado` en esos cinco endpoints y su párrafo de cambios.

**Pendiente, sin cambiar aquí:**
- **Total de copias fallidas:** el contrato responde un arreglo; añadirlo es un cambio de contrato a decisión de Richard.
- **Constructores de compatibilidad** (`ActorVigente`, `EstadoVigente` y `Bodega` sin país, más los records y comandos de bodegas). Siguen públicos y solo los usan las pruebas y el código anterior. **Para Codex:** proponer una regla ArchUnit que prohíba usarlos desde `src/main`, o moverlos a utilidades de prueba; no se cambian pruebas en la parte A.
- **`usePaisActivo` fuera del proveedor** responde EC solo para pruebas aisladas de componentes. El servidor sigue autorizando.
- **Ventana de revocación:** una vista ya cargada puede seguir visible hasta la siguiente relectura de `/auth/yo`. Son 30 s como máximo, o el instante del primer 403/404; el servidor ya rechaza cualquier petición nueva.

**Pruebas que probablemente cambien (no modificadas):**
- **Frontend:**
  - las que usan `clavesPedidos.*` o `clavesBodegas.lista`, que ahora reciben el país;
  - las que esperan la URL exacta de `/pedidos`, `/pedidos/bandeja`, `/pedidos/buscar`, `/bodegas`, `/notificaciones`, `/archivos/copias-fallidas` o `/flujos/EC/*` (ahora con `pais`);
  - las que renderizan `App` o `Sidebar` (selector y puerta);
  - `CopiasFallidas`, y `NuevoPedido` (texto con el país).
- **Backend:** las que construyen `ConsultarPedidos.Filtro` o `FiltroBandeja` (los constructores anteriores siguen), y las que usan la lambda de `ConsultarNotificaciones` o los métodos de `ConsultarArchivosDrive` (los métodos anteriores quedan como valor por defecto).

### Casos de aislamiento entre países para Codex (7.B1)

19. **Selector:**
    - con solo EC, no hay opciones y se ve "País: Ecuador";
    - con EC y CO, se exige elegir y solo aparecen esos dos;
    - sin países, aviso y ningún dato.
20. **Cambio de país:**
    - con respuestas lentas de EC en curso, cambiar a CO las cancela, y ninguna lista, bandeja, conteo, detalle, bodega, correo o copia de EC aparece después en CO;
    - la caché ya no tiene claves `pais:EC`.
21. **Claves:** todas las claves de datos por país llevan `pais:XX`; las peticiones llevan `pais` y el servidor filtra con él.
22. **Creación:** en CO, el pedido nace `DK-CO-…`; con un `pais` alterado a uno no asignado, el servidor responde 403.
23. **Búsqueda por número** de un pedido CO estando en EC (con los dos asignados): 404, "No hay un pedido con ese número".
24. **Detalle por URL** de un pedido de otro país asignado: aviso sin datos y botón para cambiar. De un país no asignado: 404.
25. **Cambios sin guardar:**
    - con un borrador de observación, un pedido nuevo o una bodega a medio editar, cambiar de país pide confirmación;
    - "Seguir" conserva todo;
    - "Descartar y cambiar" lo descarta y no aparece en el otro país.
26. **Revocación:**
    - al quitar CO al usuario con CO elegido, antes de 30 s (o al primer 403/404) se ve el aviso, desaparecen los datos de CO y se exige elegir EC (o queda EC si es el único);
    - el servidor ya responde 403/404 a cualquier petición de CO.
27. **Bodegas:** el catálogo y el selector de bodega de una acción solo muestran las del país activo. "Bodega 1" aparece con "Ciudad pendiente" y se puede desactivar sin escribir una ciudad.
28. **Correos y copias del admin:** solo del país activo. Con un `pais` no asignado en la URL, 403.
29. **Accesibilidad y diseño:** el selector es alcanzable con teclado, tiene etiqueta visible y anillo de foco, y mide al menos 40 px. El diálogo de confirmación es accesible.
30. **Regresión de Ecuador** con un usuario de un solo país: mismas pantallas que antes, salvo el texto "País: Ecuador".
