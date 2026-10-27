# Plan: Fase 2 — Motor de flujo y etapas A1 a A4

> **Estado vigente — 2026-10-16: complete, únicamente en desarrollo con datos ficticios, validado expresamente por Richard. Producción no autorizada.** Se conservan P-11, las decisiones provisionales y el backlog; no se interpretan como corregidos ni confirmados por el cliente. Las anotaciones históricas de «sin verificar» o «pendiente de parte B» quedan sustituidas en su estado por la validación siguiente, sin borrar su trazabilidad.

## Validación de cierre de desarrollo por Richard — 2026-10-16

Richard autorizó: «Valido el cierre de la fase 2 en desarrollo con datos ficticios, conservando P-11, las decisiones provisionales y el backlog documentado. No autorizo producción».

Base: [informe de fase 2, consolidación y validación](../../../documentos/pruebas/fase-2-informe.md). Parte B completada en el alcance probado: dominio/permisos (2.B1), persistencia/contrato y archivos reales con PostgreSQL Testcontainers aislado (2.B2), y recorrido de navegador con login normal, API real, PostgreSQL y nginx propios (2.B3). Se conservan los resultados previamente medidos: backend `check` 484 PASS, 93,00 % de líneas; frontend lint, 219 PASS, build y 91,99 % de líneas instrumentadas. 2.B3 aporta evidencia nueva de escenarios, no cobertura de código nueva. No se ejecutan suites para registrar el cierre.

P-11 (acciones antiguas tras reentrada), decisiones provisionales del cliente/equipo, P-16 y backlog heredado conservan el estado y destino detallados en el informe. La aceptación es exclusiva para desarrollo con datos ficticios: no autoriza datos reales, producción, integración de ramas ni fases posteriores. Este registro no marca completo el proyecto ni otras fases.

## Summary

Implementar en el servidor los pedidos del flujo A de Ecuador desde su creación hasta el check de Análisis (A1 a A4):

- definiciones de flujo versionadas;
- número único que no se reutiliza;
- una sola etapa activa;
- permisos por rol y etapa;
- urgencias de devolución;
- historial de solo inserción;
- API según el contrato.

Después se conectan las pantallas de creación, Codificación, Revisión y Análisis, y se retira de esos recorridos el motor en memoria del demo.

Este plan cubre solo la **parte A** del manual (Claude Code codifica). Las pruebas, revisiones y mediciones son de la **parte B** (Codex) y no aparecen como tareas.

## Dependencia de la fase 1 (actualizada el 2026-10-15)

- **Fase 1 cerrada en desarrollo** por validación expresa de Richard (2026-10-15; `documentos/pruebas/fase-1-informe.md`, "Validación de Richard"). El PRD la marca `complete (desarrollo validado; producción no autorizada)`.
  - **Parte B:** 1.B1, 1.B2 (con B2-D01 corregido y revalidado) y 1.B3 ejecutadas.
  - **Frontend:** lint, 159 pruebas y build aprobados.
  - **Backend:** `check` con 306 pruebas y 93,52 % de cobertura de líneas.
- **Integración:** la fase 1 llegó a `develop` con el PR #2 (merge `990c35a`). La rama `fase-2-motor-flujo` sale de `develop` en ese commit, como pide el manual. P-01 quedó resuelto por ese hecho.
- **Lo que la validación NO autoriza:** producción, ni usar cuentas reales con la contraseña temporal compartida (M3).
- **Hallazgos que siguen abiertos,** con el destino del informe, y que esta fase no debe empeorar:
  - M1, límite por IP;
  - M2, versionado de roles (pendiente explícito de D10);
  - M3;
  - B1, último administrador;
  - B2, registro de eventos de seguridad;
  - B3, `aud`/`jti` y tolerancia de reloj;
  - B4, HSTS;
  - I1 e I2;
  - el aviso del JS de más de 500 kB.
- **Lo que hereda la fase 2:**
  - `Autorizacion` decide con el estado vigente (A1), un patrón que se repite en `PermisosEtapa`;
  - el orden de bloqueos de N1;
  - las claves de consulta por generación de sesión (corrección de B2-D01: `['sesion', sesionId, …]` y `signal` en `pedir`);
  - el acceso local de desarrollo (`VITE_ACCESO_LOCAL`), que **no** se usa para probar permisos de etapa.

## User Story

Como responsable de una etapa del flujo A (admin, Codificación, Revisión o Compras China), quiero crear y mover un pedido real de A1 a A4 con rechazos, devoluciones y urgencias registrados por el servidor, para no depender de Slack ni de la memoria del navegador y para que cada paso quede en un historial que nadie puede alterar.

## Problem → Solution

Hoy los pedidos solo existen en el demo, en memoria (`frontend/src/utils/orderState.ts`):

- los permisos se deciden en el navegador;
- B3 y B4 mezclan acciones (`auditoria-demo.md` §6, errores 1 a 4);
- la urgencia se borra al primer avance;
- el historial no se guarda.

→ Con la fase 2:

- `flujos` resuelve las transiciones a partir de los datos.
- `pedidos` ejecuta cada acción en una transacción con bloqueo, validando rol, etapa y requisitos (R6).
- `plazos` calcula los vencimientos de etapa y de urgencia con el reloj de Ecuador (R5).
- `historial` guarda cada evento sin permitir `UPDATE` ni `DELETE` (R7).
- El frontend dibuja la línea de tiempo y las acciones que devuelve el servidor.

## Metadata

- **Complexity**: XL. Agrega cuatro módulos de backend nuevos, una migración, la API y las pantallas, así que se divide en los pasos 2.1 a 2.5 del manual.
- **Source PRD**: `.claude/PRPs/prds/decokasa.prd.md`
- **PRD Phase**: 2 — Motor de flujo y A1-A4
- **Manual**: `documentos/manual-paso-a-paso.docx`, fase 2, pasos 2.1 a 2.5 (parte A)
- **Estimated Files**: unos 70 de backend y unos 15 de frontend

---

## Decisiones pendientes (resolver antes del paso indicado)

Leyenda: **⏸** pendiente del cliente; **[P]** propuesta del equipo sin aprobar; **[E]** decisión técnica que debe tomar Richard. El plan lleva un **valor por defecto**; si Richard lo cambia, se ajustan las tareas indicadas. Ninguno de estos valores es una respuesta del cliente.

| ID | Antes de | Decisión | Fuentes | Valor por defecto del plan |
| --- | --- | --- | --- | --- |
| P-01 | — | **Resuelto (2026-10-15):** `fase-2-motor-flujo` se creó desde `develop` después del merge del PR #2 (`990c35a`) | Manual §Forma de trabajo 4 | — |
| P-02 | — | **Aprobado (2026-10-15) como decisión técnica del equipo**, no del cliente: el motor usa definiciones versionadas de flujo, etapas y transiciones almacenadas en datos. ADR-0004 pasa a *accepted (equipo)* y el índice `documentos/adr/README.md` se actualizó. No aprueba contenidos pendientes del cliente (plazos, documentos de Incidencias) ni otras decisiones de la fase 2 | ADR-0004; auditoría C3/D03 | — |
| P-03 | 2.1 | **⏸** Formato del número `DK-EC-AAAA-NNNN` y separación por país y año: siguen provisionales. **Aprobado por el equipo (2026-10-15), no por el cliente:** lectura A (nunca se reutiliza el número de un pedido confirmado); no se exige ausencia de huecos; NNNN de cuatro dígitos como mínimo, sin truncar ni reiniciar; sin límite de 9 999 ni cotas arbitrarias del año | requerimientos 1.3 y 4.2 #5; modelo-datos §5 | `secuencia_pedido` en V2, encapsulada en `NumeroPedido` y `GeneradorNumeroPedido` (paso 2.4) |
| P-04 | — | **Aprobado (2026-10-15) como decisión técnica del equipo**, no del cliente. Se siembran las **once etapas y las transiciones aprobadas** del flujo A de Ecuador, versión 1 (requerimientos 4.4). En la fase 2 **solo se habilitan las acciones de A1 a A4, incluido A4 → A5**; las acciones operativas de A5 en adelante no se implementan hasta su fase. Los plazos, documentos y demás valores pendientes del cliente **conservan su condición** (⏸ o [P]); los datos del esquema que no se pueden sembrar sin una decisión están en "Siembra del flujo A: datos que no se pueden sembrar sin una decisión" | Manual 2.1 ("once etapas"); ADR-0004 | — |
| P-05 | **2.1** | **Aprobada la opción A por Richard (2026-10-15), decisión técnica del equipo:** módulo mínimo `archivos` en la fase 2 con archivos reales, almacenamiento local persistente y metadatos calculados por el servidor (tamaño, tipo y `sha256`). Drive y sus reintentos quedan para la fase 3 sobre el mismo módulo. Incluye la asociación al pedido y a la entrada actual de la etapa, la validación de `adjuntoIds` (un identificador inválido se rechaza, nunca se ignora) y los eventos correspondientes, sin documentos simulados. Una restricción SQL solo acredita la referencia de almacenamiento, no que el archivo exista en disco: se contemplan los fallos de escritura y los archivos ausentes, y una carga incompleta nunca cuenta como válida. **No confirma** formatos ni límites del cliente (⏸); el límite de 100 MB sigue como propuesta [P] hasta justificarlo frente a los requisitos. Migración: la siguiente versión disponible (hoy V3), coordinada con el trabajo existente, sin aprobación aparte por su número. Antes: **[E] Pendiente de aprobación.** Requisitos de documentos en A1 (solicitud) y A2 (documentos codificados) cuando `archivos` es de la fase 3, que va en paralelo. Se decide antes de 2.1 porque cambia el contenido de V3 | arquitectura §3 ("archivos: fase 3; en la fase 2 con adaptador en memoria"); manual 2.5 ("adjuntos") y fase 3 ("acuerda identificadores") | **Recomendada: opción A**, con el diseño completo en "Diseño propuesto para P-05 y P-16". La opción B deja A1 y A2 en 422 hasta integrar la fase 3. **En ningún caso** se aceptan metadatos sin archivo como prueba de un documento obligatorio |
| P-06 | 2.2 | **Aprobado (2026-10-15) como decisión técnica del equipo**, no del cliente: sin persona asignada actúa cualquier usuario activo con el rol responsable, o el admin; con persona asignada, solo ella (activa y con el rol) o el admin; si pierde el rol o se desactiva, solo el admin actúa o reasigna, sin ampliar permisos de forma automática. Antes: **⏸ Auditoría C5/D05:** quién actúa en una etapa. Los requisitos dicen "rol responsable o admin"; la arquitectura, "asignado o admin"; el contrato, "asignado, rol o admin". Sin definir si la asignación excluye al resto del rol | requerimientos 4.2 (invariantes); arquitectura 4.3; openapi `/pedidos/{id}/acciones` | Sin asignación, actúa cualquier usuario activo con el rol responsable, o un admin. Con asignación (reasignada por el admin), solo la persona asignada o un admin. No se asigna a nadie al entrar en una etapa |
| P-07 | 2.3 | **⏸** Reasignar a un usuario de otro rol (sigue pendiente del cliente). **Aprobado (2026-10-15) como valor provisional del equipo**, no del cliente: solo a personas activas con el rol responsable de la etapa | arquitectura 8.1; openapi `x-pendiente` | Solo a usuarios activos con el rol responsable de la etapa (provisional de arquitectura 8.1) |
| P-08 | 2.2 | **⏸** Urgencias en horas hábiles o corridas | requerimientos §5; ADR-0006 | Horas hábiles, valor provisional documentado. El tipo vive en una constante del dominio (`TipoPlazo.HORAS_HABILES`) y no se confunde con las horas corridas |
| P-09 | 2.2 | **⏸** "Horas hábiles": jornada y feriados | requerimientos §5 (feriados); ADR-0006 | Provisional: lunes a viernes completos (24 h por día hábil) en `America/Guayaquil`, sin feriados. Si un vencimiento cae en fin de semana, pasa al lunes. La jornada (por ejemplo, de 8:00 a 17:00) queda ⏸ sin implementar |
| P-10 | 2.2 | **⏸** Un rechazo de Revisión (A3 → A2) durante una urgencia: ¿se mantiene o se reinicia el vencimiento? | arquitectura 8.1 ("No definido en las fuentes") | **[P]** Se mantiene el vencimiento original. Lectura de 4.2 #11: el plazo cubre Codificación y Revisión hasta volver a A4. Va en una sola regla del dominio, fácil de cambiar |
| P-11 | 2.4 | **[E] Auditoría C10:** concurrencia de pedidos. `etapaEsperada` es una propuesta y no distingue reentradas (A3 → A2 → A3). D10 (versión) no se extiende a pedidos por analogía | openapi `AccionBase.etapaEsperada`; modelo-datos §1 (`pedido.version` [P]) | **Sin cambiar el contrato:** bloqueo pesimista de la fila del pedido (`SELECT … FOR UPDATE`, mismo patrón que N1) más comprobación de `etapaEsperada`: el segundo de dos avances simultáneos recibe 409 `ETAPA_CAMBIO`. La versión del pedido en el contrato (`PedidoDetalle.version` y `SolicitudAccion.version` → 409 `VERSION_DESACTUALIZADA`) se propone aparte y no se implementa sin aprobación |
| P-12 | 2.2 | **[E] Auditoría C6/D07:** cómo se representa un pedido finalizado | modelo-datos §3.3 | No aplica en la fase 2 (no se llega a Fin). Se deja el invariante "exactamente una ACTUAL mientras `finalizado = false`" en el dominio; Fin se resuelve en la fase 4 |
| P-13 | 2.4 | **Aprobado provisionalmente por Richard (2026-10-15) como decisión del equipo, solo para desarrollo con datos ficticios; no es un requisito confirmado del cliente (sigue ⏸) y debe confirmarse antes de usar datos reales:** (1) los usuarios activos con al menos un rol vigente listan, buscan y ven el detalle de todos los pedidos; (2) el estado y los roles se comprueban en la base, no en el token; (3) `consulta` y `asistente_china` no conceden acciones, y si la persona tiene otros roles conserva los permisos aprobados de esos roles (P-06); (4) las cuentas desactivadas o sin roles reciben 403; (5) ver un pedido no autoriza por sí solo a descargar sus archivos (P-17 sigue pendiente). Pregunta original: qué ven `asistente_china` y `consulta`, y si los demás roles ven todos los pedidos | arquitectura 8.1; requerimientos 4.3 | Cualquier usuario activo lista, busca y ve el detalle y el historial; solo actúan el responsable o el admin. `asistente_china` y `consulta` no tienen acciones. Es provisional y se marca así en el código |
| P-14 | 2.4 | **⏸** ¿El admin crea pedidos del flujo B? | openapi `x-pendiente` | Fuera de alcance: en la fase 2 solo hay flujo A sembrado. Crear B responde 400 (sin definición vigente) hasta la fase 6 |
| P-15 | 2.3 | **Aprobado (2026-10-15) como decisión técnica del equipo:** los contratos de eventos viven en `compartido.domain.eventos`. Antes: **[E]** Dónde viven los eventos que cruzan módulos (R4, R9) | arquitectura 3.1 y 4.4; ArchUnit R4/R9 | Registros inmutables en `compartido.domain.eventos` (sellados): `historial` y la fase 3 (`notificaciones`) los escuchan sin depender de `pedidos`, que evita ciclos y choques entre las ramas paralelas |
| P-16 | **2.1** | **[E] Pendiente de aprobación.** Propiedad y numeración de las migraciones compartidas con la fase 3 (y 4) | Manual §Forma de trabajo 4 | Tabla de reservas y reglas en "Diseño propuesto para P-05 y P-16" |
| P-17 | 2.1 (contrato) | **Aprobado provisionalmente por Richard (2026-10-15) como decisión del equipo, pendiente de confirmación del cliente:** descarga a través del servidor (`GET /archivos/{adjuntoId}/contenido`) con autorización propia, comprobando estado, roles y asignaciones vigentes en la base. Pueden descargar: (1) el admin; (2) quien esté autorizado a actuar en la etapa de origen del archivo (regla de P-06); (3) quien esté autorizado a actuar en la **etapa revisora** de esa etapa. La relación de revisión no se generaliza a cualquier etapa siguiente: se declara en los datos del flujo y en la fase 2 solo existe A3 (Revisión y aprobación) revisa A2 (Codificación). `consulta` y `asistente_china` no conceden descarga, pero tenerlos no quita permisos que den otros roles. Ver un pedido (P-13) no autoriza por sí solo la descarga. Un archivo ausente, incompleto o con huella distinta nunca se entrega como válido. La accesibilidad real de Drive desde China **queda pendiente de verificar**; no se afirma que sea inaccesible en todos los casos | openapi `/archivos/{id}/enlace`; ADR-0003; requerimientos 4.2 (límites de confianza) y 4.4 (A3) | Ajustes del contrato propuestos antes de implementar |
| P-18 | 2.2 | **Aprobado provisionalmente por Richard (2026-10-15) como decisión del equipo, pendiente de confirmación del cliente:** solo cumplen los requisitos de una acción los archivos vinculados a la **entrada actual** de la etapa mediante un identificador estable de esa entrada (`entradaId`, nuevo en cada entrada o reentrada), no por comparación de fechas. Los archivos de entradas anteriores se conservan para el historial y se consultan o descargan según P-17, pero no satisfacen un envío nuevo | requerimientos 4.2 (invariantes: un archivo queda ligado a la etapa y al evento donde se subió) | Ajustes del contrato propuestos antes de implementar |
| P-19 | 2.4 | **Aprobado provisionalmente por Richard (2026-10-15) como decisión del equipo, pendiente de confirmación del cliente** (opción A): agrega una observación solo quien puede actuar en la etapa actual según P-06, o el admin, comprobando estado, roles y asignación vigentes en la base. Solo sobre la entrada actual de esa etapa (`etapaEsperada` y `entradaEsperadaId`; si cambiaron, 409 ETAPA_CAMBIO). `consulta` y `asistente_china` no conceden escritura ni quitan la de otros roles. Evento OBSERVACION_AGREGADA con actor, rol de intervención y hora del servidor. Con `adjuntoIds`, las validaciones de P-05 y P-18 (pedido, etapa y entrada actual). No avanza la etapa ni reinicia plazos | requerimientos 4.2 #7 (la fuente no dice quién) | Opciones B (cualquiera que vea el pedido) y C (responsables de cualquier etapa) descartadas |
| P-20 | 2.4 | **Aprobado provisionalmente por Richard (2026-10-16) como decisión del equipo, pendiente de confirmación del cliente:** `EtapaEnLinea.resumen` (string o null) resume la **última entrada cerrada** de cada etapa con el evento real del historial que la cerró (ENVIADA, APROBADA, RECHAZADA o DEVUELTO con la misma `etapa`): acción, persona (nombre vigente; si no se resuelve, su identificador), rol histórico de intervención, fecha y hora de Ecuador, destino, observación recortada a 200 caracteres con "…" y número de archivos presentados. Si la etapa está activa otra vez, empieza con "Entrada anterior". Nulo si nunca tuvo una entrada cerrada o no hay evento que la reconstruya. No se atribuye a una etapa intermedia la devolución hecha desde otra ni se inventan eventos | requerimientos 4.2 #17 y §3 ("un resumen de lo ocurrido en cada etapa anterior"); OpenAPI `EtapaEnLinea.resumen`; la fuente no define el contenido | Descartados: resumir la etapa en curso y cambiar `resumen` a objeto (rompe contrato y prueba) |

**Del cliente, sin efecto en la fase 2:**

- plazos de Fabricación e Incidencias y tránsito de 45 días (se siembran con valores nulos o provisionales);
- correo al responsable (fase 3);
- formatos de archivo (fase 3);
- recálculo en aduana (fase 4).

### Decisiones necesarias para iniciar el paso 2.1

Aprobados por el equipo el 2026-10-15: **P-02** (motor por datos, ADR-0004) y **P-04** (siembra de las once etapas; solo se habilitan A1 a A4, incluido A4 → A5).

**V2 (motor): aprobado el 2026-10-15 por Richard, como decisiones técnicas del equipo** (no del cliente):

- **S-1:** el rol responsable es nulo **solo** en etapas automáticas, como Fin. Actualizados el modelo y el contrato (0.6.0-borrador).
- **S-2:** los requisitos documentales de A6 quedan pendientes. La etapa no se habilita, y que no haya documentos configurados **no** se interpreta como "sin requisitos": columna `requisitos_documentales = 'PENDIENTES'`. Se aplica igual a A9 y A10, cuyos documentos tampoco están confirmados (S-6).
- **S-3:** `bodega_id` y las columnas `demora_*` se incorporan en la fase 4.
- **Plazos y documentos no confirmados:** conservan su condición pendiente, sin valores inventados. `plazo_estado = 'PENDIENTE'` es distinto de `SIN_PLAZO`.
- **Triggers:** `evento_pedido` protegido contra `UPDATE`, `DELETE` y `TRUNCATE`; `numero`, país y flujo de `pedido` protegidos contra modificaciones (y el pedido contra borrado).

Con eso se ejecutó el paso 2.1 (ver "Resultado del paso 2.1").

**V3 (archivos)** y las migraciones siguientes necesitan:

- **P-05 (opción A o B):** define si V3 pertenece a la fase 2.
- **P-16 (reservas y reglas):** define la numeración y el PR de esquema.
- **Ajustes de `adjunto`** del diseño propuesto, si P-05 = A.

Del resto, solo P-03 afecta a 2.1, y ya tiene un valor provisional que manda el manual. Las demás (P-06 a P-15, P-17 y P-18) se deciden antes de 2.2, 2.3 o 2.4.

### Siembra del flujo A: datos que no se pueden sembrar sin una decisión

P-04 aprueba sembrar las etapas y las transiciones **aprobadas** (requerimientos 4.4 y respuestas del cliente en §5), sin inventar valores. Estos datos del esquema no se pueden completar sin una decisión.

**Estado al 2026-10-15:**
- **S-1, S-2 y S-3:** aprobados por el equipo (ver arriba).
- **S-4 a S-8:** aplicados como se describen, conservando la condición pendiente.
- **Columnas nuevas en V2:** `definicion_etapa.plazo_estado` y `transicion_permitida.requisitos_documentales`, para distinguir "pendiente" de "no aplica".

| ID | Dato | Por qué falta | Qué hace el plan mientras no se decida |
|---|---|---|---|
| **S-1** | `definicion_etapa.rol_codigo` de **Fin (A11)**. Es `NOT NULL` en el modelo, y `DefinicionEtapa.rolResponsable` es obligatorio en el contrato | Fin es automática ("solo visual", respuesta del cliente) y no tiene responsable. Ponerle un rol (`admin`, por ejemplo) inventaría una responsabilidad | **Bloquea sembrar la etapa 11.** Hay dos opciones [P]: (a) permitir `rol_codigo` nulo solo si `automatica = true`, y `rolResponsable` nulo en el contrato (cambia `modelo-datos.md` y `openapi.yaml`); (b) asignarle un rol de forma explícita. Recomendación: (a) |
| **S-2** | `documento_requerido` de **A6 Embarque**: "Packing List / BL" | El cliente lo escribe así; no está definido si son dos documentos obligatorios o uno solo (cualquiera de los dos) | No se siembra ningún tipo de documento para A6 hasta decidirlo. No afecta a la fase 2 (A6 no se ejecuta); la fase 4 lo agrega |
| **S-3** | `pedido.bodega_id` (FK a `bodega`) y columnas `demora_*` | `bodega` es de la fase 4, y la regla 1 de P-16 (sin FK en V2) no está aprobada | Recomendación [P]: **no crearlas en V2**; la fase 4 las agrega con su migración. Pendiente de tu aprobación |
| S-4 | Plazos de **A5 Fabricación** y **A10 Incidencias** | ⏸ sin valor del cliente; los 31 días de la calculadora son una propuesta, no una respuesta | Plazo nulo (permitido por el esquema). No bloquea |
| S-5 | Plazo de **A7 Aduana** | La calculadora usa 45 días de tránsito, pero ⏸ si son hábiles o corridos; el esquema exige cantidad **y** tipo juntos | Plazo nulo hasta decidir el tipo, con un comentario que cita los 45 días como dato de la calculadora. No bloquea |
| S-6 | Documentos de **A9 Bodega** ("documentos") y **A10 Incidencias** ("todos los documentos") | Tipos sin definir (⏸) | Sin filas en `documento_requerido`. No bloquea en la fase 2 |
| S-7 | Acción **`ELEGIR_BODEGA`** separada de `APROBAR` en A9 | Es diseño del contrato (borrador; auditoría C14), no una transición de 4.4 | No se siembra. En A9 solo `APROBAR` → A10, con `requiere_bodega = true` (4.4: "Bodega elegida"). La fase 4 decide si se agrega |
| S-8 | Valores con interpretación de equipo | Plazos de 24 h "laborables" (respuesta del cliente) como `24 HORAS_HABILES`; "3 días" y "10 días" como `DIAS_HABILES` (regla del cliente: plazos laborables). El cálculo de una hora hábil sigue ⏸ (P-09). Los nombres de los tipos de documento ("Solicitud", "Documentos codificados") se toman del texto de 4.4 | Se siembran con un comentario SQL que cita la fuente. No son valores nuevos del cliente |

**Consecuencia:** como la fase 4 tendrá que agregar documentos o acciones a la versión 1 (S-2, S-6, S-7), la regla "una versión publicada no se modifica" sigue siendo **[P]**. Mientras no haya pedidos reales, completar la versión 1 en la fase 4 no rompe pedidos en curso. Si ya los hubiera, la fase 4 crearía la versión 2.

---

## Diseño propuesto para P-05 y P-16

**Estado (2026-10-15):** P-05 opción A aprobada (ver la fila P-05). El detalle siguiente es la propuesta de diseño; el límite de 100 MB y los formatos siguen sin confirmar, y las reglas que dependen de P-17 y P-18 esperan su decisión.

### P-05 — Diseño mínimo compartido de archivos

**Quién es dueño.** Hay un solo módulo `archivos` y una sola tabla `adjunto`:

- **Fase 2 (opción A):** crea el módulo, la tabla y la subida con almacenamiento local en un disco persistente. La tabla nace completa, con las columnas de Drive vacías.
- **Fase 3:** solo agrega adaptadores y tareas: `GoogleDriveAlmacen`, copia y reintentos, enlace y copias fallidas. No crea otro módulo ni vuelve a crear tablas.

**Tablas (V3, opción A).**

`adjunto`, la de `modelo-datos.md` §3.5, con tres ajustes **[P]**:

- `ruta_temporal` pasa a llamarse **`clave_local`**: clave relativa generada por el servidor (`{pais}/{numero}/{etapa}/{adjuntoId}`). Nunca lleva el nombre original (S9).
- Se agrega **`sha256 text not null`**, calculado por el servidor.
- Se agrega **`CHECK (clave_local IS NOT NULL OR drive_id IS NOT NULL)`**: siempre hay una copia real en algún lado.
- Se conservan `drive_id`, `estado_copia`, `intentos`, `ultimo_error` y `copiado_en`, y `CHECK ((estado_copia = 'COPIADO') = (drive_id IS NOT NULL))`.

`evento_adjunto`, con `evento_id` (FK a `evento_pedido`), `adjunto_id` (FK a `adjunto`) y **`relacion`** **[P]**:

- `SUBIDO`: el evento `ARCHIVO_SUBIDO`. Es único por adjunto y corresponde a `Adjunto.eventoId`.
- `PRESENTADO`: la acción que usó el archivo. Corresponde a `EventoHistorial.adjuntoIds`.

**Dónde está el archivo real.**

- **Fase 2:** volumen persistente de Docker `archivos_datos`, montado en `/aplicacion/archivos` (variable `ALMACEN_LOCAL_DIR`). Nunca en memoria ni en el `/tmp` del contenedor: un reinicio dejaría filas sin archivo.
- **Fase 3:** Drive es la copia definitiva. La copia local se conserva mientras `estado_copia` sea `PENDIENTE` o `FALLIDO`; borrarla después de `COPIADO` es opcional **[P]**, porque conservarla sirve para la descarga desde China (P-17).
- **La base solo guarda metadatos** (ADR-0003).

**Puertos de `archivos`.**

| Puerto | Tipo | Fase | Uso |
|---|---|---|---|
| `SubirArchivo` | entrada | 2 | Recibe el archivo, lo valida, lo guarda y publica `ArchivoSubido` |
| `ListarAdjuntosDeEtapa` | entrada | 2 | `GET /pedidos/{id}/etapas/{n}/archivos` |
| `ConsultarAdjuntosDeEtapa` | entrada | 2 | Lo usa `pedidos` para los requisitos: adjuntos válidos de la entrada actual (P-18) |
| `VincularAdjuntos` | entrada | 2 | Marca como `PRESENTADO` los adjuntos de una acción, en su misma transacción |
| `ObtenerContenidoArchivo` | entrada | 2 **[P]** | Descarga a través del servidor (P-17) |
| `ObtenerEnlaceArchivo` | entrada | 3 | En la fase 2 responde 409 `COPIA_PENDIENTE` (ya previsto en el contrato) |
| `ReintentarCopiasPendientes`, `ConsultarCopiasFallidas` | entrada | 3 | Tarea programada y vista del admin |
| `AlmacenLocal` | salida | 2 | Disco persistente: escritura atómica, lectura, borrado |
| `AlmacenArchivos` | salida | 3 | `GoogleDriveAlmacen` |
| `AdjuntoRepositorio` | salida | 2 | `adjunto` y `evento_adjunto` |
| `EtapasDePedido` | salida | 2 | Etapa actual, momento de entrada, si el usuario puede actuar o ver. **La implementa un adaptador de `pedidos`** (como `PedidosConEtapaActiva` en `plazos`): la dependencia solo va de `pedidos` a `archivos`, sin ciclo (R9) |

**Cómo se asocia a pedido, etapa y evento.**

1. **Subida** (`POST /pedidos/{id}/etapas/{n}/archivos`): solo a la etapa actual (si no, 409 `ETAPA_CAMBIO`) y solo el responsable o el admin (P-06). Se guarda con `pedido_id`, `etapa` y `subido_en`, y en la misma transacción se crea el evento `ARCHIVO_SUBIDO` con relación `SUBIDO`.
2. **Acción** con `adjuntoIds`: `pedidos` pide a `ConsultarAdjuntosDeEtapa` los adjuntos de la entrada actual y comprueba los tipos obligatorios. Si cumplen, la acción registra su evento y vincula esos adjuntos como `PRESENTADO`.

**Sin metadatos ficticios.**

- **Solo se crea un adjunto subiendo el archivo.** No hay ruta para registrar metadatos sin enviar el archivo, y ninguna migración siembra adjuntos.
- **El servidor mide todo él mismo:** tamaño y `sha256` sobre lo recibido, y tipo por la firma de los primeros bytes (los formatos siguen ⏸). Ignora el tamaño y el `Content-Type` que declara el navegador. Rechaza 0 bytes; si pasa de 100 MB deja de leer y responde 413.
- **Orden de escritura:** archivo temporal en el mismo volumen, sincronizado a disco, `move` atómico y después la fila. Si la transacción falla, se borra el archivo. La limpieza de archivos sin fila queda para la fase 3.
- **Para un requisito solo cuentan adjuntos con archivo real:** `clave_local` o `drive_id`, más `sha256` (los garantizan los `CHECK`).
- **Los adaptadores en memoria** solo existen en el código de pruebas; `config/` nunca los conecta en ejecución.

**Infraestructura (fase 2, opción A).**

- **`docker-compose.yml`:** volumen `archivos_datos` con permisos para el usuario sin root del contenedor y variable `ALMACEN_LOCAL_DIR`.
- **`application.yml`:** `spring.servlet.multipart` con `max-file-size: 100MB`, `max-request-size: 101MB`, `file-size-threshold: 0` y `location` en el volumen.
- **`frontend/nginx.conf`:** `client_max_body_size 101m;` y `proxy_request_buffering off;` en `location /api/`. Hoy no hay límite configurado, así que nginx usa su valor por defecto de 1 MB.

**Opción A frente a opción B.**

| | A: `archivos` mínimo en la fase 2 | B: solo el puerto en la fase 2 |
|---|---|---|
| A1 Enviar (requiere la solicitud) | Funciona con un archivo real | Bloqueado (422) hasta integrar la fase 3 |
| A2 → A3 (documentos codificados) | Funciona | Bloqueado |
| A3 y A4, urgencias, reentradas | Funcionan | Solo con pedidos colocados a mano en la base (no es un recorrido real) |
| Revisión abre los documentos | Sí, con P-17; sin P-17, solo los metadatos | No |
| Codex 2.B3 (recorrido completo) | Se puede ejecutar en la fase 2 | Espera a la fase 3 |
| Riesgo de simular el requisito | Ninguno | Alto (desactivarlo o sembrar metadatos, que está prohibido) |
| Drive, enlace, reintentos, copias fallidas | Fase 3 | Fase 3 |
| Trabajo extra de la fase 2 | Módulo pequeño, V3, volumen y límites | Casi ninguno |

### P-16 — Propiedad y numeración de migraciones

| Versión | Dueña | Contenido | Depende de |
|---|---|---|---|
| V1 (existe) | Fase 1 | Usuarios, roles, países, tokens | — |
| **V2__flujos_pedidos_historial** | Fase 2, paso 2.1 | Definiciones de flujo, `secuencia_pedido`, `pedido`, `etapa_pedido` y `evento_pedido`; triggers solo si se aprueban; siembra del flujo A de Ecuador versión 1 (P-04, con S-1 a S-8) | V1 |
| **V3__archivos** | Fase 2, paso 2.1 (opción A) | `adjunto` (completo, con columnas de Drive) y `evento_adjunto` | V2 |
| **V4__notificaciones** | Fase 3 | `notificacion` (FK a `pedido` y `evento_pedido`) | V2 |
| V5 (reservado) | Fase 3 | Solo si Drive necesita columnas extra en `adjunto`; si no se usa, no se reutiliza | V3 |
| V6 y siguientes | Fase 4 | `bodega`, más la FK de `pedido.bodega_id` con `ALTER` | V2 |

Con la opción B, V3 pasa a la fase 3 con el mismo contenido y V4 y V5 conservan su dueño.

**Reglas propuestas.**

1. **V2 crea `pedido.bodega_id` sin FK.** La tabla `bodega` es de la fase 4, que agrega la FK; si no, V2 no se puede aplicar.
2. **Primero se integra el esquema.** La primera entrega de la fase 2 es un PR pequeño a `develop` con V2 y V3, los eventos de `compartido.domain.eventos` y los puertos de `archivos`. La fase 3 crea su rama desde ahí. Si V4 llegara antes que V2, Flyway la aplicaría sobre una base sin `pedido` y fallaría la FK.
3. **Una migración aplicada no se edita ni se renumera.** Un número reservado que no se usa no se reutiliza. Se mantiene `outOfOrder` desactivado.
4. **Las reservas se anotan** en los planes de las fases 2 y 3, y en `modelo-datos.md`, antes de crear la rama de la fase 3.
5. **Alternativa descartada por ahora:** versiones por fecha (`V2026_10_15_1`). Evitan choques, pero rompen el estilo de V1.

---

## UX Design

### Before

```
┌────────────────────────────────────────────────────────────┐
│ /inicio: panel vacío por rol                               │
│ "Aún no hay pedidos; las bandejas llegan en la fase 2"     │
│ Pantallas de pedidos del demo: en disco, SIN ruta,          │
│ con datos y reglas en memoria (orderState.ts)              │
└────────────────────────────────────────────────────────────┘
```

### After

```
┌────────────────────────────────────────────────────────────┐
│ /inicio  → bandeja real de la persona (por roles):          │
│            Codificación · Revisión · Análisis · Admin       │
│ /pedidos → listado y búsqueda por número                   │
│ /pedidos/nuevo (admin) → crea A1, número del servidor      │
│ /pedidos/:id → línea horizontal A1..A11 (etapa actual)      │
│              · acciones que devuelve el servidor            │
│              · rechazar / devolver (observación)            │
│              · devolver por producto nuevo (urgencia)       │
│              · adjuntos de la etapa (según P-05)            │
│              · historial del servidor (autor y hora)        │
└────────────────────────────────────────────────────────────┘
```

### Interaction Changes

| Touchpoint | Before | After | Notes |
|---|---|---|---|
| Panel por rol | Vacío | Bandeja (`GET /pedidos/bandeja`) con etapa, vencimiento y urgencia | Consulta y asistente de China: sin bandeja, solo consulta (P-13) |
| Crear pedido | Sin ruta | Formulario (RHF + Zod) → `POST /pedidos` | Solo admin (flujo A) |
| Acciones de etapa | Botones del demo según `stageNumber` (errores 1 y 2) | Botones según `accionesDisponibles` del servidor | El navegador no decide permisos |
| Rechazar / devolver | Demo | Diálogo con observación obligatoria; urgencia 4/12/24 h en "producto nuevo" | 422 muestra `faltantes`; 409 `ETAPA_CAMBIO` recarga sin reenviar |
| Historial | En memoria | `GET /pedidos/{id}/historial` | Solo lectura |

---

## Mandatory Reading

| Priority | File | Lines | Why |
|---|---|---|---|
| P0 | `documentos/manual-paso-a-paso.docx` | Fase 2 | Alcance y skills de cada paso |
| P0 | `documentos/requerimientos.md` | 322–403 | Restricciones, invariantes y tabla 4.4 (A1 a A4) |
| P0 | `documentos/arquitectura.md` | 64–243, 296–330, 346–398 | R1 a R9, módulos `flujos`, `pedidos`, `historial` y `plazos`, recorrido de una acción, pendientes |
| P0 | `documentos/modelo-datos.md` | 12–23, 148–292, 345–428 | Tablas, restricciones, índices, numeración y JPA |
| P0 | `documentos/api/openapi.yaml` | 425–830, 1259–1350, 1617–2061 | Endpoints de flujos, pedidos e historial, errores y esquemas |
| P0 | `documentos/auditoria-demo.md` | 142–151, 486–505, 547–620 | Errores 1 a 6 del demo, C1 a C14 y D01 a D12 |
| P1 | `backend/src/main/java/com/decokasa/seguimiento/usuarios/**` | todo | Patrón hexagonal completo que se copia |
| P1 | `backend/src/main/resources/db/migration/V1__usuarios_roles_paises.sql` | todo | Estilo de las migraciones y los ids de los roles sembrados |
| P1 | `backend/src/test/java/.../arquitectura/ReglasArquitecturaTest.java` | 26–90 | R1 a R4 y R9: el código nuevo no debe romperlas |
| P1 | `frontend/src/api/consultas.ts`, `frontend/src/auth/SesionProvider.tsx` | todo | Claves con `sesionId`, `pedir` con `signal` y una renovación ante 401 |
| P2 | `frontend/src/utils/orderState.ts`, `frontend/src/data/initialData.ts` | todo | Solo referencia de casos; **no se copia** su lógica de permisos |
| P2 | `frontend/src/components/HorizontalStatusLine.tsx`, `OrderDetail.tsx`, `StagePanel.tsx`, `OrdersListView.tsx` | todo | Vistas que se reutilizan o reescriben (`auditoria-demo.md` §8) |
| P2 | `documentos/diseno/DESIGN.md` | todo | Tokens, semáforo, línea horizontal y accesibilidad |

## External Documentation

No hace falta investigación externa: la fase usa patrones internos ya establecidos (hexagonal de `usuarios`, Flyway, JPA con bloqueo pesimista, TanStack Query y shadcn). Dos notas técnicas:

- **Hibernate 7 con PostgreSQL:** `PESSIMISTIC_WRITE` genera `FOR NO KEY UPDATE`, ya observado en N1. Basta para serializar acciones sobre un mismo pedido y no bloquea las FK que apuntan a él.
- **Inmutabilidad del historial:** la aplicación se conecta a PostgreSQL con el dueño del esquema (`POSTGRES_USER` en Docker). Un `REVOKE UPDATE, DELETE` sobre el dueño no protege de verdad, así que la barrera real es el trigger `BEFORE UPDATE OR DELETE` (modelo-datos §3.4, opción 2). El `REVOKE` solo sirve si hay un usuario de aplicación distinto del dueño (fase 8).

---

## Patterns to Mirror

### NAMING_CONVENTION (backend)
```java
// SOURCE: backend/src/main/java/com/decokasa/seguimiento/usuarios/application/port/in/ActualizarUsuario.java
public interface ActualizarUsuario {
    Usuario actualizar(Solicitante solicitante, Comando comando);
    record Comando(UUID usuarioId, String nombre, ..., long version) { }
}
// Caso de uso: usuarios/application/usecase/ActualizarUsuarioCasoDeUso.java (final, sin anotaciones, implementa el puerto)
// Persistencia: XxxEntity (JPA), XxxDatos (interfaz Spring Data), XxxJpaRepositorio (implementa el puerto de salida)
```

### MODULE_COMPOSITION (config y transacción fuera del núcleo)
```java
// SOURCE: usuarios/config/UsuariosConfig.java y usuarios/config/Transacciones.java
@Configuration(proxyBeanMethods = false)
public class UsuariosConfig {
    @Bean Transacciones transaccionesUsuarios(PlatformTransactionManager gestor) { return new Transacciones(gestor); }
    // cada caso de uso se registra como bean envuelto: argumentos -> transacciones.ejecutar(() -> casoDeUso.metodo(...))
}
```

### ERROR_HANDLING
```java
// SOURCE: compartido/domain/ErrorDeDominio.java y compartido/adapter/in/web/ManejadorErrores.java:33-75
public abstract class ErrorDeDominio extends RuntimeException { ... CodigoError codigo() ... }
// ManejadorErrores traduce CodigoError -> HttpStatus (ETAPA_CAMBIO y TRANSICION_NO_PERMITIDA -> 409,
// REQUISITO_FALTANTE -> 422) y arma el Problema RFC 9457 con codigo y errores[].
```

### AUTHORIZATION (estado vigente, no solo el token)
```java
// SOURCE: usuarios/application/usecase/Autorizacion.java
usuarios.buscarPorId(solicitante.usuarioId()).filter(Usuario::puedeIniciarSesion).filter(u -> u.tieneRol(ADMIN))
// Lección A1: los roles del JWT son un primer filtro; la decisión usa el estado actual del usuario en la base.
```

### CONCURRENCY (bloqueo pesimista y relectura)
```java
// SOURCE: usuarios/application/usecase/ActualizarUsuarioCasoDeUso.java (D10) y RenovarSesionCasoDeUso (N1)
Usuario usuario = usuarios.buscarParaActualizar(id).orElseThrow(...);   // SELECT ... FOR UPDATE
if (usuario.version() != comando.version()) throw new VersionDesactualizada(...);
// Orden de bloqueos (N1): primero el agregado principal y después lo que cuelga de él.
```

### WEB_ADAPTER
```java
// SOURCE: usuarios/adapter/in/web/UsuarioController.java y Solicitantes.java
@RestController @RequestMapping("/api/v1/usuarios")
public UsuarioRespuesta actualizar(@AuthenticationPrincipal Jwt token, @PathVariable UUID usuarioId,
        @Valid @RequestBody SolicitudActualizarUsuario solicitud) { ... Solicitantes.de(token) ... }
```

### MIGRATION_STYLE
```sql
-- SOURCE: backend/src/main/resources/db/migration/V1__usuarios_roles_paises.sql
-- encabezado con fuente (modelo-datos), minúsculas, constraint con nombre <tabla>_<regla>,
-- text + CHECK en lugar de ENUM, FK sin ON DELETE CASCADE, ids de semilla fijos (UUIDv7 literal).
```

### FRONTEND_QUERY
```ts
// SOURCE: frontend/src/api/consultas.ts
export const claves = { usuarios: (sesion: number) => ['sesion', sesion, 'usuarios'] as const, ... };
export function useUsuarios() {
  const { pedir, sesionId } = useSesion();
  return useQuery({ queryKey: claves.usuarios(sesionId),
    queryFn: ({ signal }) => pedir('/usuarios', { esquema: z.array(esquemaUsuario), signal }) });
}
// Mutaciones: invalidan solo las claves afectadas; nunca reintentan solas; 409 lo resuelve la persona.
```

### LOGGING_PATTERN
El backend no registra datos personales ni tokens. Se usan `org.slf4j.Logger` solo en adaptadores, con mensajes en español y sin la observación del usuario (puede contener datos del cliente).

### TEST_STRUCTURE
No aplica a la parte A. La estructura de pruebas (dominio, casos de uso con falsos, Testcontainers y ArchUnit) la sigue Codex en 2.B1 y 2.B2 según `arquitectura.md` §7.

---

## Files to Change

### Backend (`backend/src/main/java/com/decokasa/seguimiento/`)

| File | Action | Justification |
|---|---|---|
| `../resources/db/migration/V2__flujos_pedidos_historial.sql` | CREATE | Paso 2.1 |
| `compartido/domain/TipoFlujo.java`, `NivelUrgencia.java`, `AccionEtapa.java`, `TipoPlazo.java` | CREATE | Valores compartidos del contrato (arquitectura §3) |
| `compartido/domain/EtapaCambio.java`, `TransicionNoPermitida.java`, `RequisitoFaltante.java` | CREATE | Errores 409 y 422 del contrato |
| `compartido/domain/eventos/*.java` | CREATE | Eventos que cruzan módulos (P-15) |
| `compartido/application/port/out/PublicadorEventos.java` | CREATE | Puerto de arquitectura §3 |
| `compartido/adapter/out/PublicadorEventosSpring.java` | CREATE | Adaptador sobre `ApplicationEventPublisher` |
| `compartido/adapter/in/web/ManejadorErrores.java` | UPDATE | Agregar `faltantes` al Problema con `REQUISITO_FALTANTE` |
| `compartido/config/CompartidoConfig.java` | UPDATE | Bean del publicador |
| `flujos/**` | CREATE | Dominio, puertos `ConsultarDefinicionFlujo` y `ResolverTransicion`, JPA y `FlujoController` |
| `plazos/**` | CREATE | `CalendarioLaboral`, `CalcularVencimientoEtapa` y `CalcularVencimientoUrgencia` (fase 2) |
| `pedidos/**` | CREATE | Agregado `Pedido`, casos de uso, JPA, generador de número y controladores |
| `historial/**` | CREATE | `EventoPedido`, oyente, repositorio de solo inserción y `HistorialController` |
| `usuarios/application/port/in/ConsultarResponsables.java` (+ caso de uso y bean) | CREATE | Para R4: `pedidos` lee los roles **vigentes** del solicitante y los usuarios asignables sin tocar el dominio de `usuarios` |
| `configuracion/SeguridadWebConfig.java` | UPDATE | `/api/v1/flujos/**`, `/api/v1/pedidos/**` autenticados (el permiso fino lo deciden los casos de uso) |
| `archivos/**` (mínimo) y `../resources/db/migration/V3__archivos.sql` | CREATE | **Solo si P-05 = Opción A** (diseño propuesto, sección P-05) |
| `docker-compose.yml`, `.env.example` | UPDATE | Solo si P-05 = A: volumen `archivos_datos` y `ALMACEN_LOCAL_DIR` |
| `backend/src/main/resources/application.yml` | UPDATE | Solo si P-05 = A: límites de `spring.servlet.multipart` y ruta del almacén |
| `frontend/nginx.conf` | UPDATE | Solo si P-05 = A: `client_max_body_size 101m` y `proxy_request_buffering off` en `/api/` |
| `documentos/api/openapi.yaml` | UPDATE | Solo si se aprueba P-17: `GET /archivos/{adjuntoId}/contenido` |
| `documentos/modelo-datos.md` | UPDATE | Ajustes aprobados de `adjunto` y `evento_adjunto` y tabla de reservas de P-16 |

### Frontend (`frontend/src/`)

| File | Action | Justification |
|---|---|---|
| `api/esquemas.ts` | UPDATE | Zod de `DefinicionFlujo`, `PedidoResumen`, `PedidoDetalle`, `EtapaEnLinea`, `Urgencia`, `EventoHistorial`, `PaginaPedidos` y `Adjunto` |
| `api/pedidos.ts` | CREATE | Claves y hooks de TanStack Query de pedidos, flujos e historial |
| `api/mensajes.ts` | UPDATE | Textos de `ETAPA_CAMBIO`, `TRANSICION_NO_PERMITIDA` y `REQUISITO_FALTANTE` |
| `rutas.tsx`, `components/Sidebar.tsx` | UPDATE | Rutas y enlaces `/pedidos`, `/pedidos/nuevo` y `/pedidos/:id` |
| `components/dashboards/DashboardRol.tsx` | UPDATE | Bandeja real en lugar del estado vacío (conserva "Provisional" de consulta y asistente de China) |
| `components/pedidos/*.tsx` | CREATE | Listado, creación, detalle, línea de tiempo, panel de acciones, diálogos, historial y adjuntos |
| `components/OrdersListView.tsx`, `OrderDetail.tsx`, `StagePanel.tsx`, `HorizontalStatusLine.tsx` | Referencia / reescritura | Lo visual se reutiliza en `components/pedidos/`; los originales del demo quedan sin ruta hasta retirarlos cuando no los use nadie |

## NOT Building

- Etapas A5 a A11, bodegas, demora en aduana, Packing List / BL y Fin automático (fase 4). Sus datos sí se siembran (P-04).
- Flujo B y la creación de pedidos por Compras China (fase 6).
- Copia a Google Drive, cola, reintentos y correos de Gmail (fase 3). La fase 2 solo publica eventos.
- Detección de vencimientos, `/pedidos/vencidos`, cronograma, meta y feriados (fase 5).
- Tiempo real (fase 5, ADR-0014).
- Colombia (CO responde 400) y la API de Claude (fase 7).
- Pausar o cancelar pedidos y checklist por etapa (no aprobados).
- Versión del pedido en el contrato (P-11), salvo aprobación.
- **Ninguna tarea de pruebas, lint, build, cobertura ni verificación:** son de la parte B (Codex).

---

## Step-by-Step Tasks

### Paso 2.1 — Migraciones del motor (skills: `ecc:database-migrations`, `ecc:postgres-patterns`)

#### Resultado del paso 2.1 (2026-10-15) — sin pruebas ni ejecución (las hace Codex en la parte B)

- **Creado:** `backend/src/main/resources/db/migration/V2__flujos_pedidos_historial.sql`. Es la siguiente versión libre; V1 no se modificó. No se creó `archivos` ni V3, y no se implementaron casos de uso.
- **Tablas:**
  - definiciones de flujo: `definicion_flujo`, `definicion_etapa`, `transicion_permitida`, `documento_requerido`;
  - numeración: `secuencia_pedido`;
  - pedidos: `pedido` (sin `bodega_id` ni `demora_*`, por S-3) y `etapa_pedido`;
  - historial: `evento_pedido`.
- **Triggers:** `secuencia_pedido_proteger`, `pedido_proteger`, `evento_pedido_sin_cambios` y `evento_pedido_sin_vaciar`.
- **Siembra del flujo A de Ecuador, versión 1:** 1 flujo, 11 etapas, 14 transiciones y 2 documentos confirmados ("Solicitud" en A1 y "Documentos codificados" en A2).
  - **Documentos `PENDIENTES`:** A6, A9 y A10.
  - **Plazos `PENDIENTE`:** A5, A7 y A10.
  - **Sin responsable:** Fin (S-1).
- **Diferencias con el borrador de las tareas 1 a 4:**
  - `transicion_permitida` usa (`flujo_id`, `etapa_origen`) en lugar de `etapa_id`, con FK compuestas a origen y destino;
  - `pedido` apunta al flujo con FK compuesta (`flujo_id`, `pais_codigo`, `tipo_flujo`);
  - columnas nuevas `plazo_estado` y `requisitos_documentales`;
  - `CHECK` adicionales en `evento_pedido`.

  Todo quedó registrado en `modelo-datos.md`.
- **Numeración (corregida el 2026-10-15, antes de aplicar V2).** Antes de editarla se comprobó en modo lectura `flyway_schema_history` de las bases conocidas:
  - PostgreSQL de Docker (puerto 5432): solo V1;
  - PostgreSQL nativo (puerto 5433, el de `bootRun`): solo la línea base en la versión 1;
  - en ninguna de las dos existen tablas de V2.

  Cambios:
  - Sin límite de 9 999 (`ultimo_numero >= 0`) y sin cotas arbitrarias del año (`anio > 0`).
  - Comentarios alineados con la lectura A.
  - Separación explícita entre lo que garantiza la migración (contador que no retrocede, pedido que no se borra ni cambia su número, `UNIQUE`) y lo que debe cumplir el generador del 2.4, todavía no implementado: bloqueo de la fila en la misma transacción, exposición del número solo después del commit, `%04d` sin truncar.
  - **La ausencia de huecos no es un requisito.**
- **Reglas que quedan para 2.2 a 2.4:**
  - una transición con `requisitos_documentales = 'PENDIENTES'` responde 409 `TRANSICION_NO_PERMITIDA`, nunca como si no tuviera requisitos;
  - una etapa con `plazo_estado = 'PENDIENTE'` deja `vence_en` nulo y se muestra como "plazo pendiente", no "sin plazo";
  - cómo exponer esos dos estados en el contrato se decide en 2.4.

#### Task 1: V2 — definiciones de flujo
- **ACTION**: Crear `V2__flujos_pedidos_historial.sql`, primera parte.
- **IMPLEMENT**: tablas `definicion_flujo`, `definicion_etapa`, `transicion_permitida` y `documento_requerido`, exactamente como en modelo-datos §3.2:
  - UNIQUE (`pais_codigo`, `tipo_flujo`, `version`);
  - índice único parcial `WHERE vigente`;
  - `CHECK` de `plazo_cantidad`/`plazo_tipo` y de los valores de `accion`.
- **MIRROR**: MIGRATION_STYLE.
- **GOTCHA**:
  - `definicion_etapa.rol_codigo` es FK a `rol (codigo)`, que es UNIQUE en V1.
  - El rol de Fin (A11) depende de **S-1**. Si se aprueba la opción (a): `CHECK (automatica OR rol_codigo IS NOT NULL)` con `rol_codigo` nulo. No asignar un rol inventado.

#### Task 2: V2 — siembra del flujo A de Ecuador, versión 1 (P-04, aprobado)
- **ACTION**: Insertar el flujo `EC`/`A`/versión 1, vigente, con ids UUIDv7 literales fijos.
- **IMPLEMENT**: once etapas. Responsables y nombres según requerimientos 1.4, 4.4 y §5; ningún valor pendiente del cliente se convierte en aprobado.

  | # | Nombre | Rol | Plazo | Fuente / condición |
  | --- | --- | --- | --- | --- |
  | 1 | Crear solicitud de pedido | `admin` | sin plazo | 4.4 ("—") |
  | 2 | Codificación | `marketing` | 24 `HORAS_HABILES` | Cliente: "24h", plazos "laborables" (S-8); hora hábil ⏸ P-09 |
  | 3 | Revisión y aprobación | `asistente_compras` | 24 `HORAS_HABILES` | Igual que 2 |
  | 4 | Análisis y gestión | `compras_china` | 3 `DIAS_HABILES` | Cliente: 3 días; plazos laborables |
  | 5 | Fabricación | `compras_china` | nulo | ⏸ sin plazo del cliente (S-4); 31 días es propuesta |
  | 6 | Embarque | `compras_china` | 10 `DIAS_HABILES` | Cliente: 10 días; plazos laborables |
  | 7 | Aduana Ecuador | `compras_china` | nulo | ⏸ 45 días de tránsito (calculadora), hábiles o corridos sin definir (S-5) |
  | 8 | Autorización de salida | `logistica` | 24 `HORAS_HABILES` | Igual que 2 |
  | 9 | Bodega | `logistica` | 24 `HORAS_HABILES` | Igual que 2 |
  | 10 | Incidencias | `logistica` | nulo | Responsable: cliente ("Anderson sube todos los documentos"); plazo ⏸ (S-4) |
  | 11 | Fin | según **S-1** | sin plazo | `automatica = true`; **no se siembra hasta resolver S-1** |

  **Transiciones aprobadas (4.4):**
  - 1 `ENVIAR` → 2, con documento "Solicitud". Es requisito del cliente y se siembra con cualquier opción de P-05; con la B no se puede cumplir hasta la fase 3.
  - 2 `ENVIAR` → 3, con documento "Documentos codificados".
  - 3 `APROBAR` → 4, y `RECHAZAR` → 2 con observación.
  - 4 `APROBAR` → 5; `DEVOLVER_POR_ERROR` → 2 con observación (archivos opcionales); `DEVOLVER_POR_PRODUCTO_NUEVO` → 2 con observación y urgencia.
  - 5 `APROBAR` → 6 con observación (cliente: "check y observaciones").
  - 6 `APROBAR` → 7. Documentos según **S-2**: sin filas hasta decidir.
  - 7 `APROBAR` → 8, y `REPORTAR_DEMORA` → se queda, con observación. La fecha nueva es ⏸ y no está en el esquema de la transición.
  - 8 `APROBAR` → 9.
  - 9 `APROBAR` → 10 con `requiere_bodega = true`. Documentos sin tipo (S-6); `ELEGIR_BODEGA` no se siembra (S-7).
  - 10 `APROBAR` → 11. Documentos sin tipo (S-6).
- **Habilitación (P-04):** en la fase 2, `EjecutarAccionEtapa` solo permite acciones cuya etapa de origen esté en `ETAPAS_IMPLEMENTADAS = {1, 2, 3, 4}`, lo que incluye A4 `APROBAR` → A5. Desde A5, cualquier acción responde 409 `TRANSICION_NO_PERMITIDA` hasta la fase 4, aunque la transición exista en los datos. `accionesDisponibles` aplica el mismo filtro.
- **GOTCHA**:
  - Los nombres de los tipos de documento son texto libre y deben coincidir con `tipoDocumento` de la subida.
  - Cada valor sembrado lleva un comentario SQL con su fuente (requerimientos, respuesta del cliente o interpretación S-8) y su condición (⏸ o [P]).
  - No sembrar el flujo B (fase 6).

#### Task 3: V2 — pedidos y línea de tiempo
- **ACTION**: Crear `secuencia_pedido`, `pedido` y `etapa_pedido` (modelo-datos §3.3).
- **IMPLEMENT**:
  - `CHECK` de urgencia completa, de demora completa y de `finalizado`;
  - índice único parcial `etapa_pedido (pedido_id) WHERE estado = 'ACTUAL'`;
  - trigger `BEFORE UPDATE` en `pedido` que rechaza cambios en `pais_codigo`, `tipo_flujo`, `flujo_id` y `numero`, y también el borrado del pedido: **aprobado el 2026-10-15** y creado en V2;
  - los índices de modelo-datos §4 para `pedido` y `etapa_pedido`.
- **GOTCHA**:
  - `secuencia_pedido` existe por P-03 (provisional).
  - `pedido.version bigint not null` se crea igual: lo usa JPA aunque el contrato no lo exponga (P-11).
  - `pedido.bodega_id` y las columnas `demora_*` dependen de **S-3**. La recomendación es no crearlas en V2; la fase 4 las agrega con `bodega` y su FK.
  - El `CHECK` de "demora completa" solo existe si existen esas columnas.

#### Task 4: V2 — historial de solo inserción
- **ACTION**: Crear `evento_pedido` (modelo-datos §3.4) con FK `ON DELETE RESTRICT` y los índices de §4.
- **IMPLEMENT**:
  - función y trigger `BEFORE UPDATE OR DELETE` que lanzan una excepción "El historial es de solo inserción", más `BEFORE TRUNCATE`: **aprobado el 2026-10-15** y creado en V2;
  - `evento_adjunto` va en la migración de `adjunto` (V3), porque necesita esa tabla.
- **GOTCHA**: el `REVOKE` no protege al dueño del esquema (ver External Documentation). Se agrega como comentario para la fase 8.

#### Task 5 (solo si P-05 = A, con los ajustes aprobados): V3 — archivos
- **ACTION**: `V3__archivos.sql` con `adjunto` y `evento_adjunto`, según "Diseño propuesto para P-05 y P-16": `clave_local`, `sha256`, el `CHECK` de copia real y la columna `relacion`, si se aprueban.
- **GOTCHA**:
  - No siembra adjuntos.
  - Acordar con la fase 3 que esta tabla es la definitiva: la fase 3 solo agrega en V5 si hace falta (P-16).
  - La infraestructura (volumen, `multipart`, nginx) se cambia junto con el módulo en 2.2 y 2.4, no en la migración.

### Paso 2.2 — Reglas y transiciones (skills: `/ecc:prp-implement`, `ecc:hexagonal-architecture`)

#### Resultado del paso 2.2 (2026-10-15) — sin pruebas, builds ni verificaciones (parte B, Codex). No está verificado

- **`compartido.domain`:**
  - enums `TipoFlujo`, `AccionEtapa`, `NivelUrgencia` (4, 12 y 24 h) y `TipoPlazo`;
  - `PlazoDeEtapa`, sellado: `Definido`, `SinPlazo` o `Pendiente`;
  - errores `TransicionNoPermitida`, `RequisitoFaltante` (con `faltantes`) y `EtapaCambio`.
- **`flujos`:**
  - dominio: `DefinicionFlujo` (resolver, etapa inicial, retorno de urgencia), `DefinicionEtapa` (rol nulo solo si es automática, S-1), `TransicionPermitida` y `RequisitosDocumentales`;
  - puertos: `ResolverTransicion` y `ConsultarDefinicionFlujo` (entrada), `DefinicionFlujoRepositorio` (salida);
  - casos de uso puros de esos dos puertos.
- **`plazos`:**
  - `CalendarioLaboral` (ADR-0006: lunes a viernes, `America/Guayaquil`, el fin de semana pasa al lunes);
  - puerto `CalcularVencimiento` y su caso de uso;
  - urgencias en `HORAS_HABILES`, el valor provisional documentado (requerimientos §5 y arquitectura 8.1).
  - Una hora hábil es cualquier hora de lunes a viernes: la jornada dentro del día y los feriados no están documentados (⏸) y no se aplican.
- **`pedidos.domain`:**
  - agregado `Pedido` (`crear` y `ejecutar`) y `EtapaDePedido`;
  - `EstadoEtapa`, `UrgenciaActiva`, `NumeroPedido` (`%04d` sin truncar), `ReglaDeTransicion` y `AccionSolicitada` (`ResultadoAccion` se retiró en el 2.3: la acción devuelve su evento);
  - `EtapasHabilitadas.faseDos()` = {1, 2, 3, 4} y la interfaz `Vencimientos`.
- **Reglas aplicadas en `Pedido.ejecutar`:**
  - exactamente una etapa `ACTUAL`;
  - `etapaEsperada` distinta de la actual: 409 `ETAPA_CAMBIO`;
  - solo acciones con origen en A1 a A4;
  - documentos `PENDIENTES`: 409 `TRANSICION_NO_PERMITIDA`, sin avanzar;
  - requisitos (observación, urgencia, bodega, documentos presentes): 422 con `faltantes`;
  - avance: la etapa queda `COMPLETADA`; retroceso: queda `DEVUELTA`, igual que las intermedias que estaban completadas;
  - reentrada con el plazo reiniciado; un plazo pendiente o sin plazo deja `venceEn` nulo;
  - devolución por producto nuevo: activa la urgencia con su propio vencimiento; termina al volver a la etapa de retorno (A4), que se deduce de los datos.
- **Transiciones:** el motor es por datos, así que A2 Enviar → A3 también funciona aunque no se nombre aparte. A1 y A2 piden documentos confirmados ("Solicitud" y "Documentos codificados"): sin el módulo de archivos (P-05) nadie puede aportarlos, por eso **A1 y A2 quedan bloqueadas con 422** hasta que exista ese módulo. No se simulan.
- **Desvíos del plan:**
  - `MotorFlujos` no es una clase aparte: la resolución vive en `DefinicionFlujo.resolver`.
  - `EtapasHabilitadas` está en el dominio de `pedidos` (el plan la ponía en el caso de uso).
  - `pedidos` recibe la regla y los vencimientos con tipos propios (`ReglaDeTransicion` y `Vencimientos`), y `PlazoDeEtapa` está en `compartido`, para cumplir R4.
  - Pasan al 2.4: `faltantes` en `ManejadorErrores`, los casos de uso de `pedidos` y la configuración de beans.
- **P-10 (⏸):** un rechazo de A3 durante una urgencia no modifica la urgencia; no se agrega ninguna regla de reinicio.

#### Task 6: Tipos compartidos y errores
- **ACTION**: En `compartido/domain`:
  - enums `TipoFlujo`, `AccionEtapa`, `NivelUrgencia` (con horas 4, 12 y 24) y `TipoPlazo`;
  - errores `EtapaCambio` (409), `TransicionNoPermitida` (409) y `RequisitoFaltante` (422, con `List<String> faltantes`).
- **MIRROR**: ERROR_HANDLING; `DatoInvalido` como modelo de error con datos.
- **GOTCHA**:
  - `NivelUrgencia` lleva sus horas en el dominio. Su tipo de plazo es `HORAS_HABILES` por P-08, con un comentario ⏸ que remite a requerimientos §5.
  - `ManejadorErrores` debe incluir `faltantes` solo cuando el código es `REQUISITO_FALTANTE`.

#### Task 7: Módulo `flujos` (dominio y puertos)
- **ACTION**:
  - **Dominio:** `DefinicionFlujo` (país, tipo, versión, etapas), `DefinicionEtapa`, `TransicionPermitida` (acción, destino opcional, requisitos) y `MotorFlujos` (interno).
  - **Puertos de entrada:**
    - `ResolverTransicion.resolver(flujoId, etapaOrigen, accion)` → `Resolucion(destino, requiereObservacion, requiereUrgencia, requiereBodega, documentos)`, o `TransicionNoPermitida`;
    - `ConsultarDefinicionFlujo.vigente(pais, tipo)` y `.version(...)`.
  - **Puerto de salida:** `DefinicionFlujoRepositorio`.
- **IMPLEMENT**: el destino y los requisitos salen de los datos, no de `if (etapa == 3)` (corrige los errores 1 y 2 del demo).
- **MIRROR**: NAMING_CONVENTION; R1, R2 (sin Spring en el núcleo).

#### Task 8: Módulo `plazos` (fase 2)
- **ACTION**:
  - **Dominio:** `CalendarioLaboral` (lunes a viernes, `America/Guayaquil`, sin feriados por P-09) y `Plazo` (cantidad y `TipoPlazo`).
  - **Puertos de entrada:** `CalcularVencimientoEtapa(entrada, plazo)` y `CalcularVencimientoUrgencia(activada, nivel)`.
- **IMPLEMENT**:
  - `HORAS_HABILES`: suma horas solo en días hábiles.
  - `DIAS_HABILES`: suma días saltando sábado y domingo.
  - `DIAS_CORRIDOS`: suma días naturales.
  - Si el resultado cae en sábado o domingo, pasa al lunes a la misma hora.
- **MIRROR**: `Reloj` (R5); nunca `Instant.now()`.
- **GOTCHA**: P-08 y P-09 son provisionales. Documentar en el Javadoc que la jornada y los feriados están ⏸ y que el cálculo vive solo aquí, para cambiarlo en un punto. El "arrastre" de fechas y el recálculo por demora son de las fases 4 y 5.

#### Task 9: Agregado `Pedido` (dominio)
- **ACTION**: `Pedido` (número, país, tipo, `flujoId`, versión del flujo, creador, `etapas: List<EtapaDePedido>`, urgencia activa con vencimiento propio, `finalizado`), `NumeroPedido` y `EtapaDePedido` (número, estado `PENDIENTE`/`ACTUAL`/`COMPLETADA`/`DEVUELTA`, asignado, entrada, salida, `venceEn`, reentradas).
- **IMPLEMENT** (reglas del manual 2.2):
  - **Una sola etapa activa:** `ejecutar(accion, resolucion, actor, ahora, vencimientos)` cierra la etapa actual y abre la de destino.
  - **A1 enviar → A2.**
  - **A3 rechazar:** exige observación y vuelve a A2. Las etapas intermedias que se dejan pasan a `DEVUELTA`.
  - **A4 devolver por error:** vuelve a A2.
  - **A4 devolver por producto nuevo:** exige urgencia, fija `urgencia.venceEn` con `CalcularVencimientoUrgencia` y vuelve a A2.
  - **Urgencia:** se limpia **solo al entrar de nuevo en A4**. Un rechazo en A3 durante la urgencia la mantiene (P-10, una sola regla `urgenciaAlRechazar()`).
  - **Reentrada:** incrementa `reentradas` y reinicia `entradaEn` y `venceEn` con el plazo normal de la etapa (manual 2.3).
  - **País y flujo** no cambian después de crear el pedido.
- **GOTCHA**:
  - Un pedido devuelto a A2 con urgencia muestra **dos** vencimientos: el de la etapa (24 h) y el de la urgencia. No confundirlos.
  - `ETAPAS_IMPLEMENTADAS = 1..4` vive en el caso de uso, no en el dominio (P-04).

### Paso 2.3 — Responsables e historial (skills: `ecc:hexagonal-architecture`, `ecc:jpa-patterns`)

#### Resultado del paso 2.3 (2026-10-15) — sin pruebas, builds ni verificaciones (parte B, Codex). No está verificado

- **Decisiones aplicadas (del equipo, aprobadas por Richard el 2026-10-15; no del cliente):** P-06, P-07 (valor provisional; reasignar a otro rol sigue ⏸) y P-15.
- **Eventos (`compartido.domain.eventos`):**
  - `EventoDePedido`, sellada, con `PedidoCreado`, `EtapaEnviada`, `EtapaAprobada`, `EtapaRechazada`, `PedidoDevuelto` (`MotivoDevolucion` ERROR o PRODUCTO_NUEVO), `EtapaReasignada` y `ObservacionAgregada` (solo el contrato; su caso de uso es del 2.4);
  - cada evento lleva `DatosDelPedido` (id, número, país y tipo de flujo, que no cambian), la etapa, el `Interviniente` (usuario y rol de intervención) y la hora del servidor;
  - sin `adjuntoIds`: los archivos dependen de P-05.
- **Publicación:** puerto `PublicadorEventos` en `compartido.application.port.out`, adaptador `PublicadorEventosSpring` (síncrono) y bean en `CompartidoConfig`.
- **Autorización (`pedidos.domain`):**
  - `ActorVigente`: estado actual en la base (activo y roles), no los roles del token;
  - `PermisosEtapa`: `paraActuar`, `puedeActuar`, `paraCrear`, `paraReasignar` y `exigirAsignable`;
  - el rol de intervención es el responsable de la etapa, o `admin` si la persona actúa como admin.
- **Agregado `Pedido`:**
  - `crear` recibe `FlujoDelPedido` y el creador vigente;
  - `ejecutar` recibe el actor vigente y comprueba el permiso antes de cambiar nada; el admin pasa por las mismas validaciones;
  - `reasignar`: solo el admin vigente, solo la etapa actual (409 `ETAPA_CAMBIO`), destino activo con el rol responsable (400), motivo opcional de hasta 1000 caracteres; si ya es de esa persona, no hay cambio ni evento;
  - los eventos quedan pendientes y se entregan con `retirarEventos()`.
- **`flujos`:** `Resolucion` y `ReglaDeTransicion` llevan `rolResponsableOrigen`.
- **`usuarios`:** puerto `ConsultarResponsables` (estado vigente; se une a la transacción que lo llama) con su caso de uso y su bean.
- **`historial`:**
  - dominio `EventoPedido`, `TipoEventoPedido` y `DetalleEvento` (jsonb `datos`);
  - `RegistrarEventoPedido` y su caso de uso; puerto `HistorialRepositorio` (`insertar` y `listarPorPedido`, sin modificar ni borrar);
  - JPA: `EventoPedidoEntity` (`@Immutable`, `updatable = false`, jsonb con `@ColumnTransformer`) y `EventoPedidoDatos`, que extiende `Repository` y no `JpaRepository`, así que no ofrece `save` ni `delete`;
  - `HistorialJpaRepositorio` inserta con `persist`;
  - `OyenteEventosPedido`: `@EventListener` síncrono con `@Transactional(MANDATORY)`; acción e historial se confirman o se revierten juntos.
- **Refuerzo explícito, sin cambios con los datos sembrados:**
  - el rechazo y las dos devoluciones siempre exigen observación;
  - la devolución por producto nuevo siempre exige urgencia;
  - una urgencia en otra acción responde 400.
- **Reentradas:** reinician el plazo normal de la etapa (sin cambios desde el 2.2). La asignación de una etapa se conserva al volver a ella: borrarla ampliaría permisos sin que nadie lo decida. Si una reentrada debe limpiarla no está definido.
- **P-10 (⏸):** mantener la urgencia durante un rechazo de A3 sigue siendo **provisional** y está marcado así en el código.
- **No incluido:**
  - outbox (ADR-0012 propuesto);
  - casos de uso, JPA de `pedidos` y beans de `pedidos` (2.4);
  - consulta y endpoint del historial, que dependen de P-13 (2.4);
  - lista de personas asignables para la pantalla;
  - `AgregarObservacion` (2.4).
- **Riesgo conocido:** el estado del actor se lee sin bloquear su fila. Una desactivación que se confirme durante la acción puede no verse en esa acción (ventana de milisegundos). Bloquear la fila del usuario se evalúa en el 2.4 junto con el orden de bloqueos (pedido y luego usuario).

#### Task 10: Quién actúa y reasignación (P-06, P-07)
- **ACTION**:
  - En `usuarios`, el puerto de entrada `ConsultarResponsables`:
    - `rolesVigentes(usuarioId)`: roles del usuario si está activo; vacío si no puede iniciar sesión.
    - `puedeAsignarse(usuarioId, rol)`.
  - En `pedidos`, `PermisosEtapa`, que decide con esos datos vigentes y no con los roles del token (lección A1).
- **IMPLEMENT**:
  - Actúan el admin vigente, o:
    - si hay asignado, solo esa persona;
    - si no lo hay, quien tenga el rol responsable.
  - `ReasignarEtapa`:
    - solo el admin;
    - solo sobre la etapa actual (si no, 409 `ETAPA_CAMBIO`);
    - el usuario destino debe estar activo y tener el rol responsable (P-07).
- **GOTCHA**: R4: `pedidos` no importa `usuarios.domain`. El puerto devuelve `Set<CodigoRol>` de `compartido`.

#### Task 11: Eventos y módulo `historial`
- **ACTION**:
  - **Eventos (P-15):** registros en `compartido/domain/eventos/` (`PedidoCreado`, `EtapaEnviada`, `EtapaAprobada`, `EtapaRechazada`, `PedidoDevuelto`, `ObservacionAgregada`, `EtapaReasignada`), con `pedidoId`, `numero`, `etapa`, `etapaDestino`, `usuarioId`, `rolCodigo`, `ocurridoEn` (del `Reloj`), `observacion`, `nivelUrgencia` y `adjuntoIds`.
  - **Publicación:** puerto `PublicadorEventos` en `compartido` y adaptador sobre `ApplicationEventPublisher`.
  - **Historial:** `EventoPedido` (inmutable), `HistorialRepositorio` (solo `insertar` y consultas), `OyenteEventosPedido` (adaptador de entrada con `@EventListener`, **síncrono**, en la misma transacción de la acción), `ConsultarHistorialPedido` y `HistorialController`.
- **IMPLEMENT**:
  - `rol_codigo` guarda el rol con el que actuó la persona, que es el rol responsable de la etapa, o `admin` si actuó como admin.
  - `datos` (jsonb) guarda `etapaDestino` (y `asignadoA` en la reasignación).
- **MIRROR**: JPA de modelo-datos §7: `@Immutable`, `updatable = false`, sin setters.
- **GOTCHA**: la fase 3 escuchará los mismos eventos para `notificaciones`. **No** usar `@TransactionalEventListener(AFTER_COMMIT)` para el historial: debe fallar junto con la acción (R7). El outbox sigue *proposed* (ADR-0012) y no se adelanta.

### Paso 2.4 — API y transacciones (skills: `ecc:api-design`, `ecc:springboot-patterns`)

#### Resultado del paso 2.4 (2026-10-15) — parcial: sin endpoints. Sin pruebas, builds ni verificaciones (parte B, Codex). No está verificado

- **Persistencia:**
  - `flujos`: entidades de solo lectura y `DefinicionFlujoJpaRepositorio`. No completa valores: PENDIENTE sigue PENDIENTE (S-2).
  - `pedidos`:
    - `PedidoEntity`, con `@Version` e identidad `updatable = false`, y `EtapaPedidoEntity`;
    - `PedidoJpaRepositorio`: bloqueo con `refresh` y `PESSIMISTIC_WRITE`; guarda primero las etapas que dejan de ser ACTUAL, por el índice `etapa_pedido_una_actual`; los conflictos se traducen a 409 `ETAPA_CAMBIO`;
    - `SecuenciaPostgresGenerador`: `INSERT … ON CONFLICT DO NOTHING` y `UPDATE … RETURNING` en la conexión de la transacción, con `%04d` sin truncar.
- **Casos de uso:**
  - `CrearPedido`:
    - comprueba el permiso con el estado vigente antes de reservar el número;
    - solo EC (ADR-0009);
    - un flujo sin definición vigente (B) responde 400 (P-14 ⏸).
  - `EjecutarAccionEtapa`, en este orden:
    1. bloquea el pedido;
    2. comprueba `etapaEsperada` (409);
    3. resuelve la acción;
    4. aplica el dominio;
    5. guarda;
    6. publica los eventos.
  - `ReasignarEtapa`: bloquea el pedido y aplica P-07 provisional.
  - Apoyo común: el estado vigente viene de `ConsultarResponsables` y los vencimientos de `CalcularVencimiento`.
- **Transacciones (ADR-0012):** `PedidosConfig`, `FlujosConfig` (solo lectura, se une a la transacción de la acción) y `PlazosConfig`. Acción, eventos e historial van en la misma transacción.
- **Errores:** `ManejadorErrores` agrega `faltantes` con `REQUISITO_FALTANTE`.
- **Documentos:** no se simulan. Sin módulo de archivos, A1 y A2 responden 422 con los documentos que faltan.
- **Asignación al reentrar:** se conserva (provisional).
- **Endpoints: no implementados.** Quedan bloqueados por decisiones pendientes (P-13, P-05, P-17 y P-18) y por estados del dominio que OpenAPI 0.6.0 no representa. La lista está en la entrega del paso; nada se implementa sin aprobación.
- **Desvíos:**
  - `Transacciones` de `usuarios` no se reutiliza (evita que `pedidos`, `flujos` e `historial` dependan de `usuarios.config`); cada configuración usa `TransactionTemplate`.
  - `PermisosEtapa.paraCrear` pasa a ser público.
  - `ConsultarDefinicionFlujo.porId` es nuevo.

#### Resultado del paso 2.4, continuación (2026-10-15): API, archivos y consultas. Sin pruebas, builds ni verificaciones (parte B, Codex). No está verificado ni completo

- **Decisiones aplicadas:**
  - del equipo, provisionales y pendientes del cliente: P-13 (consulta), P-17 (descarga) y P-18 (entrada actual);
  - del equipo: P-05 opción A.
  - No se convirtieron en requisitos.
- **Contrato:** `openapi.yaml` 0.7.0-borrador con los ajustes aprobados y sus condiciones:
  - `entradaEsperadaId` en la subida;
  - `asignacionVigente` booleano nulable;
  - `accionesDisponibles` estructurado; es un cambio de tipo, no compatible;
  - `rolIntervencion` nulable en los eventos del sistema;
  - bandeja según P-06;
  - A4 sin descarga por inferencia.
  - Agrega la contradicción 7 (100 MB).
- **V3 (`V3__archivos_y_entradas.sql`):**
  - tablas `entrada_etapa`, `adjunto` y `evento_adjunto`;
  - columnas `etapa_pedido.entrada_id` (FK diferible e índice único) y `definicion_etapa.revisa_etapa`, con A3 revisa A2;
  - triggers de solo inserción y de no borrado.
  - No hay CHECK de `entrada_id` con `entrada_en`, para no invalidar la siembra SQL de las pruebas de V2; lo garantiza el dominio.
- **`archivos`, módulo nuevo:**
  - dominio `Adjunto`, `TipoDetectado` (firma de bytes), `LimiteDeTamano` y `ArchivoDemasiadoGrande`;
  - puertos `SubirArchivo`, `ListarAdjuntosDeEtapa`, `DescargarArchivo`, `ValidarAdjuntosParaAccion`, `ConsultarAdjuntosDeEntrada`, `AlmacenLocal`, `AdjuntoRepositorio` y `PedidosParaArchivos`;
  - almacenamiento en disco: temporal, `fsync` y movimiento atómico;
  - JPA y controladores.
  - **Subida en dos transacciones:**
    1. prevalida;
    2. recibe fuera de la transacción;
    3. registra con el pedido bloqueado;
    4. si algo falla, descarta el temporal y el definitivo.
  - **Descarga:** verifica tamaño y SHA-256 antes de entregar; responde `octet-stream`, `attachment` y `nosniff`.
- **`pedidos`:**
  - entrada estable por cada entrada a una etapa, registrada en `entrada_etapa`;
  - `autorizarSubida` y `puedeDescargar` en el agregado;
  - `ConsultarPedidos`: detalle, búsqueda, listado y bandeja, con `accionesDisponibles` calculado con la misma regla P-06;
  - `AutorizarArchivosDePedido` y su adaptador hacia archivos;
  - validación de `adjuntoIds` en las acciones (400 si es inválido, nunca se ignora; el permiso se revisa antes);
  - controladores `PedidoController` y `AccionEtapaController`.
- **Otros módulos:**
  - `flujos`: `ConsultarAccionesDeEtapa`; `revisaEtapa` en `ContratoDeEtapa` y `DefinicionEtapa`.
  - `usuarios`: `ResumirUsuarios`.
  - `historial`: `evento_adjunto` (SUBIDO o PRESENTADO), `ArchivoSubido` y `adjuntoIds` en `EventoPedido`.
  - `compartido`: `ARCHIVO_NO_DISPONIBLE` (409), `SolicitantesWeb`, `FechasWeb` y `UsuarioResumenRespuesta`.
- **Configuración:**
  - `decokasa.archivos.directorio` (`ALMACEN_LOCAL_DIR`);
  - volumen `archivos-datos` y carpeta con dueño `decokasa` en el Dockerfile;
  - `backend/datos/` en `.gitignore`.
  - ~~El límite de tamaño no tiene valor~~: resuelto como valor provisional del equipo en el cierre (ver abajo).
- **Bloqueado o pendiente:**
  - (1) y (2) **contradicción 7 y nginx:** resueltos en el cierre con un valor provisional del equipo (ver abajo);
  - (3) ~~fuera del alcance pedido~~ (corregido: las tareas 13 y 14 de este plan los incluyen): `PUT …/asignacion`, `GET …/historial` y `GET /flujos` se implementan en el cierre; observaciones sigue bloqueado; `/enlace` es de la fase 3;
  - (4) P-14, formatos de archivo, A4 y los documentos de A2, la limpieza de archivos huérfanos (fase 3). `EtapaEnLinea.resumen`: implementado el 2026-10-16 con P-20, pendiente de revalidación.
- **Pruebas de Codex que deberán adaptarse, sin quitar comprobaciones:**
  - `MigracionesIT`: versión final y lista de migraciones ("2" → "3"; líneas 99, 114 y 115) y el conjunto exacto de tablas tras V2 (línea 128).
  - `V2RestriccionesIT`: versión final "2" (línea 29).
  - Las demás deberían compilar gracias a los constructores compatibles: `EtapaDePedido`, `AccionSolicitada`, `DefinicionEtapa`, `EjecutarAccionEtapa.Comando`, `EjecutarAccionEtapaCasoDeUso`, los eventos y `EventoPedido`.

#### Cierre del paso 2.4 (2026-10-15), aprobado por Richard. Sin pruebas, builds ni verificaciones (parte B, Codex). No está verificado ni completo

- **V3 (no aplicada en ninguna base persistente; se corrige en el mismo archivo):**
  - se restaura `etapa_pedido_entrada_con_entrada CHECK ((entrada_id IS NULL) = (entrada_en IS NULL))`, agregado después de completar los datos anteriores a V3. Quitarlo para conservar los datos de `V2RestriccionesIT` fue un error;
  - `etapa_pedido_entrada` pasa a FK compuesta `(entrada_id, pedido_id, numero)` → `entrada_etapa (id, pedido_id, etapa)`, diferible (al crear un pedido la entrada se inserta después que la etapa). El CHECK no es diferible: la aplicación escribe las dos columnas en la misma sentencia (`EtapaPedidoEntity.cambiar`).
  - **Codex adapta `V2RestriccionesIT`** sin quitar comprobaciones: las líneas 37, 100 y 102 escriben `entrada_en` sin `entrada_id`; deben insertar la fila de `entrada_etapa` y escribir las dos columnas juntas.
- **Endpoints (tareas 13 y 14):**
  - `GET /pedidos/{id}/historial`: `HistorialController`, `ConsultarHistorialPedido` (más reciente primero, actor y rol del momento). Permiso P-13 a través de `PedidosParaHistorial`, que implementa `pedidos` (`adapter/in/historial`, puerto `VerificarConsultaDePedido`): la dependencia va de pedidos a historial (R9). 403 agregado al contrato.
  - `PUT /pedidos/{id}/etapas/{n}/asignacion`: en `AccionEtapaController`, con el `ReasignarEtapaCasoDeUso` existente (admin vigente, etapa actual, P-07 provisional); responde el detalle leído después del commit.
  - `GET /flujos/{pais}/{tipoFlujo}?version=`: `FlujoController` y `DescribirFlujo` (vista propia del puerto, con `estadoPlazo`, `revisaEtapa` y `requisitosDocumentales`); cualquier usuario autenticado, según el contrato.
  - `POST /pedidos/{id}/observaciones` (P-19, provisional del equipo): `ObservacionController`, `AgregarObservacionCasoDeUso` y `Pedido.agregarObservacion`. Bloquea el pedido, comprueba la entrada actual y el permiso antes de mirar los archivos, valida `adjuntoIds` con `ValidarAdjuntosParaAccion` y publica `ObservacionAgregada`, que el historial inserta en la misma transacción (relación PRESENTADO para los archivos). No guarda el pedido: no cambia versión, etapa ni plazos. El evento trae su id (`EventoDePedido.eventoId`) para responder 201 con ese `EventoHistorial`. Contrato: `etapaEsperada` y `entradaEsperadaId` obligatorios (cambio no compatible) y 409.
- **Límite provisional del equipo (no confirmado por el cliente):** 100 MiB por archivo (104 857 600 bytes), configurable.
  - Una sola variable, `ARCHIVOS_TAMANO_MAXIMO_MIB` (`.env`, valor por defecto 100): `docker-compose.yml` la pasa al backend como `DECOKASA_ARCHIVOS_TAMANO_MAXIMO=<n>MB` y al frontend; fuera de Docker, `application.yml` usa `100MB`.
  - Spring: `max-file-size` = límite y `max-request-size` = límite + 1 MiB (resto del formulario multipart). La subida y su multipart ya no dependen de `@ConditionalOnProperty`.
  - nginx: `nginx.conf` es una plantilla (`/etc/nginx/templates`); `frontend/limite-archivos.envsh` calcula `ARCHIVOS_CUERPO_MAXIMO_MIB` = límite + 1 y la imagen oficial lo sustituye con envsubst. `client_max_body_size` usa ese valor, `proxy_request_buffering off` evita copiar el archivo al disco de nginx y el 413 de nginx responde en formato `Problema` (`ARCHIVO_DEMASIADO_GRANDE`). Los tiempos de espera no cambian.
  - `.gitattributes` fija LF para `frontend/*.envsh` (lo ejecuta `sh` en Linux).

#### Task 12: Persistencia de `pedidos` y número
- **ACTION**: Crear:
  - `PedidoEntity` con `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)` de `EtapaPedidoEntity` y `@Version`;
  - `PedidoDatos`;
  - `PedidoJpaRepositorio`, con `buscarParaActualizar(id)` (`PESSIMISTIC_WRITE` y `refresh`, mismo patrón que `UsuarioJpaRepositorio`), `guardar`, `buscarPorNumero`, el listado paginado con filtros del contrato y la bandeja;
  - `SecuenciaPostgresGenerador`: asegura la fila (`pais`, `año`) con `INSERT … ON CONFLICT DO NOTHING`, la incrementa con `UPDATE … SET ultimo_numero = ultimo_numero + 1 … RETURNING` (que la bloquea hasta el final de la transacción) y arma `DK-{pais}-{anio}-{%04d}` con el año del `Reloj` en Ecuador.
- **GOTCHA**:
  - El generador corre en la **misma** transacción que crea el pedido. Un número revertido no perteneció a ningún pedido confirmado y puede volver a entregarse (lectura A); **no se garantiza ni se exige ausencia de huecos**.
  - NNNN tiene como mínimo cuatro dígitos y nunca se trunca: 10 000 se escribe `DK-EC-2026-10000`.
  - El número solo se expone después del commit: respuesta, eventos y correos se emiten al confirmar, y no se registra antes.
  - Los listados no ordenan por el texto del número (`…-10000` < `…-9999` en orden de texto).
  - `NumeroPedido` encapsula el formato, que sigue provisional (P-03).
  - Listados y bandeja usan proyecciones, no entidades completas (N+1).

#### Task 13: Casos de uso de `pedidos`
- **ACTION**: `CrearPedido`, `EjecutarAccionEtapa`, `AgregarObservacion`, `ReasignarEtapa`, `ConsultarPedido` (con `accionesDisponibles` para el solicitante), `BuscarPedido`, `ListarPedidos` y `ListarBandeja`.
- **IMPLEMENT** — `EjecutarAccionEtapa`, en este orden:
  1. Bloquea el pedido.
  2. Comprueba `etapaEsperada` (409 `ETAPA_CAMBIO`).
  3. Verifica que la etapa esté en `ETAPAS_IMPLEMENTADAS` (si no, 409 `TRANSICION_NO_PERMITIDA`).
  4. Resuelve la transición con `flujos`.
  5. Revisa permisos (403).
  6. Revisa requisitos (422 con `faltantes`: observación, urgencia y documentos según P-05).
  7. Aplica el agregado, calcula vencimientos con `plazos`, guarda y publica eventos.
- **IMPLEMENT** — `CrearPedido`:
  - solo el admin vigente, flujo A;
  - si `pais != EC`, responde 400 (ADR-0009);
  - si el flujo es B, responde 400 (P-14);
  - crea las etapas desde la definición vigente con A1 `ACTUAL` y publica `PedidoCreado`.
- **MIRROR**: CONCURRENCY y AUTHORIZATION.
- **GOTCHA**:
  - **Orden de bloqueos:** primero el pedido y después la secuencia o las demás filas, como en N1.
  - **Dos avances simultáneos:** el segundo espera el bloqueo, ve otra etapa y recibe 409, sin cambios parciales (manual 2.4).
  - **`accionesDisponibles`** se calcula con la misma `PermisosEtapa` que se usa al ejecutar.

#### Task 14: Adaptadores web y seguridad
- **ACTION**: `FlujoController` (`GET /api/v1/flujos/{pais}/{tipoFlujo}`), `PedidoController` (`GET`/`POST /pedidos`, `/buscar`, `/bandeja`, `GET /{id}`), `AccionEtapaController` (`POST /{id}/acciones`, `PUT /{id}/etapas/{n}/asignacion` y `POST /{id}/observaciones`) y `HistorialController` (`GET /{id}/historial`).
- **IMPLEMENT**:
  - DTO con los nombres del contrato y fechas `FechaHora` con offset `-05:00`.
  - `SolicitudAccion` con discriminador `accion`, validado con Bean Validation por variante.
  - Respuesta 201 con `Location` al crear.
  - `SeguridadWebConfig` solo exige autenticación en esas rutas; el permiso fino es del caso de uso (R6).
- **MIRROR**: WEB_ADAPTER. `Solicitantes` es *package-private* en `usuarios`: crear su equivalente en `pedidos.adapter.in.web` (o moverlo a `compartido.adapter.in.web` si Richard lo prefiere) sin importar adaptadores de otro módulo (R4).
- **GOTCHA**:
  - `/pedidos/vencidos` y `/pedidos/{id}/cronograma` no entran: son de la fase 5.
  - Ningún endpoint devuelve 500 por una carrera: las violaciones de los índices únicos (`numero` y etapa `ACTUAL`) se traducen a 409, igual que `RestriccionesUnicasUsuario`.

#### Task 15: Composición
- **ACTION**: `FlujosConfig`, `PlazosConfig`, `PedidosConfig` e `HistorialConfig`, con `Transacciones` por módulo (ADR-0012), igual que `UsuariosConfig`.
- **GOTCHA**: ArchUnit R9. La dirección es `pedidos` → `flujos`, `plazos` y `usuarios`; `historial` escucha eventos de `compartido`. Ningún módulo nuevo puede depender de `pedidos`.

### Paso 2.5 — Pantallas conectadas (skills: `shadcn`, `tanstack-query-best-practices`, `impeccable`)

#### Task 16: Esquemas y consultas
- **ACTION**:
  - **Esquemas:** Zod en `api/esquemas.ts` para los esquemas del contrato de la fase 2.
  - **Consultas:** `api/pedidos.ts` con la fábrica `clavesPedidos` (`['sesion', sesionId, 'pedidos', …]`) y los hooks `useFlujo`, `usePedidos(filtros)`, `useBandeja`, `usePedido(id)`, `useHistorial(id)`, `useCrearPedido`, `useEjecutarAccion`, `useAgregarObservacion` y `useReasignar`.
- **IMPLEMENT**: después de una acción, invalidar el detalle, el historial, la bandeja y el listado. Usar el `signal` de la consulta en `pedir`.
- **MIRROR**: FRONTEND_QUERY.
- **GOTCHA**:
  - 409 `ETAPA_CAMBIO`: recargar el detalle y avisar; nunca reenviar la acción sola.
  - 422: mostrar `faltantes`.

#### Task 17: Rutas, menú y bandejas
- **ACTION**:
  - **Rutas:** `/pedidos`, `/pedidos/nuevo` (solo admin, protegida además por el servidor) y `/pedidos/:id`.
  - **Menú:** el enlace "Pedidos" aparece para todos los roles con sesión.
  - **Dashboard:** `DashboardRol` muestra la bandeja real.
- **GOTCHA**: consulta y asistente de China conservan la etiqueta "Provisional" y no tienen bandeja (P-13).

#### Task 18: Detalle del pedido y acciones
- **ACTION**: `components/pedidos/`:
  - `LineaDeTiempo`: reescribe `HorizontalStatusLine` con `EtapaEnLinea`, la etapa actual marcada, `aria-current` y el semáforo de `DESIGN.md`.
  - `PanelAcciones`: botones **solo** de `accionesDisponibles`.
  - `DialogoAccion`: RHF + Zod; observación obligatoria en rechazar y devolver; urgencia 4/12/24 h en "producto nuevo".
  - `HistorialPedido`.
  - `AdjuntosEtapa`: según P-05.
  - `FormularioNuevoPedido`.
  - `ListadoPedidos`: búsqueda por número y filtros del contrato.
- **IMPLEMENT**: enviar `etapaEsperada` = la etapa que muestra la pantalla.
- **GOTCHA**: no importar `utils/orderState.ts` ni `utils/permissions.ts` en estos recorridos (manual 2.5: "Retira el motor de negocio en memoria"). Las pantallas del demo quedan sin ruta.

#### Task 19: Pulido
- **ACTION**: `/impeccable polish` sobre los componentes de 2.5, conservando `DESIGN.md`:
  - tokens;
  - foco `ring-ring`;
  - 40 px de altura mínima;
  - texto de 12 px como mínimo;
  - `Empty` o `Card` para los estados vacíos.

#### Resultado del paso 2.5 (2026-10-15): pantallas conectadas. Sin pruebas, builds ni verificaciones (parte B, Codex). No está verificado ni completo

- **Rutas:** `/bandeja`, `/pedidos`, `/pedidos/nuevo` (bajo `SoloAdmin`; el servidor lo vuelve a comprobar) y `/pedidos/:pedidoId`. Menú: "Mi bandeja" (oculta si la persona solo tiene `consulta` o `asistente_china`) y "Pedidos" para todos. `DashboardRol` enlaza a la bandeja, o a Pedidos en los paneles provisionales.
- **Datos (`api/pedidos.ts`):** esquemas Zod del contrato 0.7.0, fábrica `clavesPedidos` por sesión y hooks `usePedidos`, `useBandeja`, `usePedido`, `useHistorial`, `useAdjuntosDeEtapa(s)`, `useDefinicionFlujo`, `useCrearPedido`, `useEjecutarAccion`, `useReasignarEtapa`, `useAgregarObservacion`, `useSubirArchivo` y `useDescargarArchivo`. Tras cada operación se actualizan el detalle (con la respuesta del servidor o releyéndolo), el historial, los archivos, el listado y la bandeja. Ninguna mutación se reintenta sola ni avanza la etapa en el navegador.
- **Cliente (`api/cliente.ts`):** cuerpo `FormData` sin `Content-Type` propio (multipart), `formato: 'blob'` para la descarga y `ErrorApi.faltantes` (422).
- **Pantallas (`components/pedidos/`):** `ListaPedidos` (filtros en la URL, paginación del servidor), `BandejaPedidos` (P-06, agrupada por etapa: Codificación A2, Revisión A3, Análisis A4), `NuevoPedido` (flujo A), `DetallePedido`, `LineaDeTiempo`, `PanelEtapa`, `DialogoAccion`, `DialogoReasignar`, `NuevaObservacion`, `ArchivosEtapa`, `HistorialPedido`, `TablaPedidos`, `Distintivos` y `formato.ts`.
- **Reglas aplicadas:**
  - acciones solo de `accionesDisponibles`; `etapaEsperada` fija al abrir el diálogo (si otra persona mueve el pedido, el servidor responde 409 y el diálogo lo muestra con "Ver estado actual");
  - observación obligatoria en rechazar y devolver; urgencia 4/12/24 h en "producto nuevo"; documentos obligatorios con su estado y `requisitosDocumentales` PENDIENTES como no completable;
  - `adjuntoIds` = archivos de la **entrada actual** que la persona marca; la subida envía `entradaEsperadaId`; los archivos de entradas anteriores se muestran aparte y no cuentan (P-18);
  - subir y observar solo si el servidor dio acciones (misma regla P-06 / P-19); descarga con la pista P-17 (admin, etapa del archivo o etapa revisora según `revisaEtapa`) y 403 explicado si el servidor la niega;
  - reasignación solo admin, con personas activas del rol responsable (P-07).
- **Demo retirado de estos recorridos:** se borraron `OrdersListView`, `OrderDetail`, `StagePanel`, `HorizontalStatusLine`, `OrderHistoryTab` y `OrderDocumentsTab`. `utils/orderState.ts`, `permissions.ts`, `dateUtils.ts` e `initialData.ts` siguen solo porque los importan pantallas sin ruta de fases posteriores (`DashboardView`, `TrackingSearchView`, `TimelineCalculatorView`, `WarehouseCatalogView`); ninguna ruta los usa.
- **Sistema de diseño:** shadcn `textarea`, `radio-group` y `skeleton` agregados (foco `ring-ring` sin transparencia). `Input` y `SelectTrigger` pasan a 40 px de alto (DESIGN.md 5). Tokens nuevos de fondo de estado en `index.css` (`--color-estado-*-fondo`, valores de DESIGN.md 3.4). `alert-dialog` no se agregó porque la CLI sobrescribía `button.tsx`; las confirmaciones usan `Dialog`.
- **Desviaciones del plan:** los esquemas Zod de pedidos están en `api/pedidos.ts` y no en `api/esquemas.ts`; `DialogoAccion` valida con estado propio, no con React Hook Form (solo dos campos); los nombres de componentes difieren (`PanelEtapa` en vez de `PanelAcciones`, `ArchivosEtapa`, `ListaPedidos`, `NuevoPedido`).
- **Huecos del contrato (documentados, sin inventar respuesta):**
  - "Por vencer" necesita días hábiles y el contrato solo da `vencida` y `venceEn`: se muestran "A tiempo" o "Vencida";
  - `EventoHistorial.detalle` de una reasignación solo trae ids: el nombre se muestra si la persona aparece en el pedido; si no, "otra persona";
  - `EventoHistorial` no trae el vencimiento de la urgencia ni la entrada de la observación;
  - `EtapaEnLinea.resumen` llegaba nulo; desde el 2026-10-16 lo calcula el servidor (P-20, revalidado por Codex). `PanelEtapa` lo muestra tal cual como texto (sin HTML), incluido "Entrada anterior", y no muestra nada si es null; el navegador no lo recalcula. Pendiente de verificar en la parte B (prueba y navegador);
  - **P20-R4-01 sigue ABIERTO** (informe de pruebas): `pedidos/adapter/in/web/ObservacionController` usa `historial.adapter.in.web.EventoHistorialRespuesta`, un acoplamiento entre adaptadores que R4 prohíbe y que las reglas automáticas no detectan. No se resolvió en este cambio de pantalla;
  - la subida usa `fetch`, que no informa progreso: se muestra "Subiendo…" en lugar de una barra (DESIGN.md 7.5);
  - el flujo B no se ofrece al crear (P-14 y sin definición sembrada).
- **Pendientes para la parte B (Codex), además de los del 2.4:**
  - compilar (`npm run lint`, `npm run build`) y ejecutar `npm run test`; nada de esto se ejecutó;
  - adaptar sin quitar comprobaciones `App.test.tsx` ("el menú de un usuario sin admin solo ofrece Inicio": ahora también Mi bandeja y Pedidos) y `DashboardRol.test.tsx` (texto "Aún no hay pedidos; las bandejas llegan en la fase 2.", reemplazado por el enlace a la bandeja);
  - comprobar en el navegador la descarga con `blob:` bajo la CSP de `nginx.conf` y el 413 de nginx;
  - escáner de impeccable (`impeccable detect`) e inspección visual en escritorio y móvil, que no se hicieron;
  - recorrido Playwright 2.B3 sobre estas pantallas.
- **Pendientes del 2.4 que siguen abiertos:** nada compilado ni probado; adaptar `MigracionesIT` (versión "3" y tablas) y `V2RestriccionesIT` (versión "3" y `entrada_id` junto con `entrada_en` en las líneas 37, 100 y 102); el historial no guarda la entrada de una observación; decisiones provisionales sin confirmar del cliente (P-07, P-13, P-17, P-18, P-19 y el límite de 100 MiB), formatos de archivo y si A4 descarga los documentos de A2; contrato en `0.7.0-borrador`.

---

## Testing Strategy (parte B — Codex; no son tareas de este plan)

Se registra solo para que el resumen de implementación apunte a lo que Codex debe cubrir según el manual:

- **2.B1 — Dominio puro:**
  - número único que no se reutiliza;
  - A1 → A2;
  - A3 rechazo con observación → A2;
  - A4 devolución por error → A2;
  - A4 devolución por producto nuevo con urgencia de 4, 12 o 24 h hasta volver a A4;
  - responsable o admin, y reasignación (P-06, P-07);
  - historial inmutable, plazos y reentrada;
  - una sola etapa activa;
  - acciones simultáneas sin cambios parciales.
- **2.B2 — Persistencia y contrato:** migraciones V2/V3, restricciones y triggers, concurrencia con PostgreSQL real, endpoints contra OpenAPI, autorización por etapa, y actor y fecha en cada evento.
- **2.B3 — Recorrido con Playwright y usuarios ficticios por rol:** crear, codificar, rechazar, corregir, aprobar, devolver con urgencia de 4 h y volver a Análisis.

### Edge Cases Checklist (para Codex)
- [ ] Acción con `etapaEsperada` vieja → 409 sin cambios
- [ ] Rechazo sin observación → 422 (`faltantes: ["observación"]`)
- [ ] Devolución por producto nuevo sin urgencia → 422
- [ ] Usuario con el rol pero sin asignación cuando la etapa está asignada a otro → 403 (P-06)
- [ ] Usuario desactivado con JWT vigente → 403 (lección A1)
- [ ] Dos creaciones simultáneas → números distintos (que sean consecutivos no es un requisito)
- [ ] Creación revertida → ningún pedido confirmado comparte número; el número no aparece en la respuesta, los eventos ni los registros
- [ ] Número 10 000 del año → `DK-EC-AAAA-10000`, sin truncar ni fallar
- [ ] `UPDATE` o `DELETE` sobre `evento_pedido` → error de la base
- [ ] Acción en A5 o posterior → 409 `TRANSICION_NO_PERMITIDA` (P-04)
- [ ] Crear con `pais = CO` o `tipoFlujo = B` → 400

---

## Validation Commands

No se ejecutan en la parte A. Codex los ejecuta en la parte B: `.\gradlew.bat check` (ArchUnit, pruebas y cobertura ≥ 80 %) desde `backend/`, y `npm run lint`, `npm run test` y `npm run build` desde `frontend/`. Usa `docker compose up -d --build --wait` para el recorrido real.

## Acceptance Criteria (de la fase 2, según el manual)
- [ ] Parte A: motor, API y pantallas A1–A4 implementados, con persistencia e historial del servidor
- [ ] Decisiones P-03 a P-18 resueltas o con el valor por defecto aceptado por Richard (P-01 resuelto; P-02 aprobado el 2026-10-15). Las previas al paso 2.1 se listan en "Decisiones necesarias para iniciar el paso 2.1"
- [ ] Parte B (Codex): dominio, contrato, persistencia y recorrido A1–A4 en verde; permisos e historial verificados, sin hallazgos altos de datos
- [ ] Richard valida el cierre (no se marca `complete` solo por terminar la parte A)

## Completion Checklist (parte A)
- [ ] El código sigue los patrones de `usuarios` (puertos, casos de uso sin Spring, `config/`, transacción externa)
- [ ] Los errores usan `ErrorDeDominio` y `CodigoError`, y el Problema incluye `faltantes`
- [ ] No hay valores provisionales sin su marca ⏸ o [P] en el código y los comentarios SQL
- [ ] No se agregó alcance de las fases 3 a 8
- [ ] Resumen de implementación para Codex: archivos, funciones, configuración y pendientes

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| P-05 sin decidir bloquea A1 y A2 (documentos) | Alta | Alto | Decidir antes del paso 2.2; la opción A permite el recorrido sin esperar a Drive |
| Choque de migraciones o de módulo `archivos` con la fase 3 en paralelo | Media | Alto | P-16: V2 y V3 para la fase 2, desde V4 para la fase 3; contrato de eventos en `compartido` (P-15) |
| Comando viejo aplicado tras una reentrada (C10) | Media | Medio | Bloqueo y `etapaEsperada` ahora; proponer la versión del pedido en el contrato (P-11) |
| Cálculo de horas hábiles distinto del esperado por el cliente | Media | Medio | P-08 y P-09 en un solo componente (`CalendarioLaboral`); marcados ⏸ |
| Ampliar permisos por interpretación (C5) | Media | Alto | P-06 explícito; el navegador solo muestra `accionesDisponibles` |
| Hallazgos abiertos de la fase 1 (M1–M3, B1–B4) se agravan con datos de pedidos | Media | Medio | No usar cuentas reales (M3); no ampliar permisos; los eventos de pedido no reemplazan el registro de seguridad B2 |
| El PR de esquema (V2 y V3) no llega antes de que la fase 3 cree su rama | Media | Alto | Regla 2 de P-16: integrar primero el esquema y los contratos |

## Notes

- **Fuentes:** el plan sigue el nuevo manual (`documentos/manual-paso-a-paso.docx`, 2026-10-13). `CLAUDE.md` todavía cita `manual-paso-a-paso.md` como fuente; conviene actualizarlo.
- **`application.yml`:** el valor por defecto de `DB_URL` es ahora `localhost:5433` y hay un `import` opcional de `./.env.local`, cambios hechos fuera de este plan. Se conservan.
- **Valores provisionales:** ninguno es una respuesta del cliente. El formato del número, las horas hábiles, los 45 días de tránsito y las facultades de consulta y asistente de China siguen ⏸.
