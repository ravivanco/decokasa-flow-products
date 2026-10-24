# Plan: Fase 6 — Flujo B (pedido propuesto por China)

## Summary
Agregar el flujo B de Ecuador (11 etapas + Fin automático) como **datos versionados** sobre el motor existente (ADR-0004), sin duplicar la lógica de A. B1 lo crea Compras China, B2 revisa el producto propuesto, y de B3 en adelante se repiten las reglas de A2–A11. El trabajo de código se limita a quitar los supuestos del flujo A que quedaron fijos (etapas habilitadas, creación por admin y pantallas que asumen A) y a dar a cada rol sus pantallas.

## User Story
Como Compras China (David), quiero crear un pedido con el producto nuevo que propongo y seguirlo hasta Fin, para que Ecuador lo revise, lo codifique y lo importe por el mismo proceso que el flujo A.

## Problem → Solution
Hoy solo existe la definición de A (V2, V3, V11 y V16). Algunas piezas suponen el flujo A: `EtapasHabilitadas` es un conjunto global 1–10; el admin crea pedidos de cualquier flujo; las pantallas piden la definición de A y solo el admin puede crear. → Se añade la migración V17 con la definición de B, y esos supuestos pasan a salir de los datos del flujo del pedido.

## Metadata
- **Complexity**: Large
- **Source PRD**: `.claude/PRPs/prds/decokasa.prd.md`
- **PRD Phase**: 6 — Flujo B
- **Rama**: `fase-6-flujo-b`; no se crea todavía (ver "Estado de partida")
- **Estimated Files**: unos 20 (1 migración, unos 8 de backend, unos 8 de frontend, documentación, guía y skill)

---

## Estado de partida (comprobado el 2026-10-21)

- Rama actual: `fase-5-seguimiento`. El último commit es `512d499` (V16 y recorrido de A), y no hay nada en el índice.
- Cambios sin commit:
  - `frontend/src/components/Header.tsx`, modificado (ajeno; se conserva);
  - 386 archivos sin seguimiento, sobre todo evidencias y temporales de Codex.
  
  Ninguno pertenece a esta fase.
- Las fases 3, 4 y 5 siguen abiertas, y 3.B3 sigue pospuesto. El intento de reanudar 3.B3 quedó sin ejecutar: la consola necesita a Richard.
- **Recomendación de rama:** crear `fase-6-flujo-b` desde `fase-5-seguimiento` (`git switch -c fase-6-flujo-b`, que lleva los cambios sin commit tal cual), para conservar el motor, V16 y la fase 5 sin integrar.
  - Se aparta de la regla de `CLAUDE.md` (las ramas salen de `develop`), porque las fases 3–5 no se integraron. Hay que registrarlo en el PR.
  - Conviene antes que Richard decida qué hacer con los 386 archivos sin seguimiento y con `Header.tsx`.

## Fuentes y su alcance

| Tema | Fuente | Estado |
|---|---|---|
| 12 etapas, responsables y lazos | `requerimientos.md` 1.5, 4.5; PRD línea 142 | Aprobado (documento del cliente) |
| B5 devuelve por error a **B3**, no a B2 | Respuesta del cliente ("la codificación está en la etapa 3") | Aprobado |
| Creación de B solo por David | 4.2 #4 ("Crea solicitudes del flujo B David"); "¿El admin crea pedidos del flujo B? → Solo Compras China" | Compras China aprobado; **admin ⏸ (P-14)** |
| B6–B12 = A5–A11 | Tabla 4.5 (capacidad del producto, redactada por el equipo a partir del documento) | Reglas de motor: sí. **Documentos y plazos: comprobar uno por uno** |
| Documentos de A6/A9/A10 (V16) | Decisión de Richard del 2026-10-21, "para desbloquear el flujo A" | **Alcance: solo flujo A.** No se extiende a B7/B10/B11 sin confirmación |
| Plazo de B2 (24 h en 4.5) | La respuesta "24h" del cliente nombra "Revisión" sin distinguir B2 de B4 | [P]: ⏸ confirmar |
| Diagrama de 4.4 (B2 ✓ → A2) | Contradice la tabla 4.5 (B2 ✓ → B3) | Prevalece la tabla 4.5 |

## Decisiones de la fase (F6-D)

| ID | Decisión | Estado |
|---|---|---|
| **F6-D1** | Documentos obligatorios de B1, B3, B7, B10 y B11 | **Decidida por Richard (2026-10-21), no por el cliente:**<br>• B1: "Solicitud".<br>• B3: "Documentos codificados".<br>• B7: "Packing List"; BL y otros, opcionales.<br>• B10: bodega y al menos un documento de cualquier tipo.<br>• B11: al menos un documento, sin excepción.<br>Se admiten adjuntos adicionales; solo cuentan los del pedido, etapa y entrada actual. Implementada en V17. |
| F6-D2 | Cómo se excluye al admin de crear B mientras P-14 siga pendiente | **Resuelta en 6.2 (instrucción de Richard del 2026-10-21):** crea solo quien tiene, vigente, el rol de la primera etapa de la definición. Se quita la excepción general del admin. A no cambia (A1 es `admin`); B solo `compras_china`. Sin migración ni `TipoFlujo.B` en el código. P-14 sigue ⏸. |
| F6-D3 | Generalizar `EtapasHabilitadas`: un conjunto por flujo, o "todas las etapas manuales de la definición" | Abierta (paso 6.4) |
| F6-D4 | Plazos de B2 (24 h propuesto) y etapas revisoras (B2 revisa B1, B4 revisa B3: [P] por analogía con A3 revisa A2) | Abierta (paso 6.1) |
| F6-D5 | Usuario con roles de A y de B (admin y compras_china): qué flujo crea por defecto en la pantalla | Abierta (paso 6.5) |

---

## Reglas fijadas a A que hay que generalizar (sin duplicar el motor)

| Dónde | Supuesto actual | Generalización propuesta |
|---|---|---|
| `pedidos/domain/EtapasHabilitadas` + `PedidosConfig.etapasHabilitadas()` | Conjunto global `{1..10}` (fases 2–4) | Sin él, B11 (Incidencias) quedaría bloqueada. Que salga de la definición del flujo del pedido, sin números fijos (F6-D3). |
| `PermisosEtapa.paraCrear` | Crean el responsable de la primera etapa **o el admin** | Atributo en los datos de la definición, sin preguntar si el flujo es "A" o "B" en el código (F6-D2). |
| `Pedido.crear` (`incluyeProductosNuevos` solo en A) | Comparación `TipoFlujo.A` | Se mantiene: es una regla del requisito 4.2 #6, con CHECK en V2. No se toca. |
| Frontend `api/pedidos.ts` (`tipoFlujo: 'A'` al crear) | Solo crea A | Admite `'B'` según el rol. |
| `TableroTrabajo` y `BandejaPedidos` (`useDefinicionFlujo('EC','A')`) | La línea compacta usa la definición de A para todos los pedidos | Una consulta por flujo presente (A y B) y la línea elige según `tipoFlujo`. Sin N+1. |
| `DetallePedido` (sección "Llegada y bodega" solo si `tipoFlujo === 'A'`) | Supone A | Mostrarla según los datos (`registraLlegada` o etapa con `requiereBodega` en la definición). |
| `BandejaPedidos` (aviso de documentos pendientes con `codigoEtapa('A', …)`) | Código de A fijo | Usar el `tipoFlujo` de cada pedido. |
| Ruta `/pedidos/nuevo` (`SoloAdmin`) | Solo el admin | Admin para A y Compras China para B; el servidor sigue decidiendo. |
| `DashboardView.tsx` y `OrderProductsTab.tsx` | Demo sin ruta con números de A | Fuera de alcance (no se usan). |

Ya dirigidos por datos, sin cambio: transiciones y requisitos, `revisa_etapa`, `registra_llegada`, Fin automático, urgencia (termina al entrar en la etapa que admite `DEVOLVER_POR_PRODUCTO_NUEVO`, que en B será B5), cronograma, vencimientos, historial, archivos y outbox.

## NOT Building
- Creación de B por el admin (P-14 ⏸).
- Flujo B de Colombia.
- Extender a B, sin confirmación, los documentos de V16 o un plazo que el cliente no definió.
- Un motor o casos de uso paralelos para B.
- Pruebas o verificación en la parte A (las hace Codex en 6.B1–6.B3).
- Cambios en `Header.tsx`, o activar Google, workers o la detección automática.

---

## Step-by-Step Tasks (parte A)

### Paso 6.1 — Definición del flujo (`ecc:database-migrations`)
- **ACTION**: `V17__flujo_b_ecuador.sql` (siguiente versión libre tras V16). Solo inserta filas: no toca las de A ni los pedidos en curso.
- **IMPLEMENT**:
  - `definicion_flujo` EC/B versión 1, vigente;
  - 12 `definicion_etapa`:
    - roles: B1 compras_china, B2 asistente_compras, B3 marketing, B4 asistente_compras, B5–B8 compras_china, B9–B11 logistica, B12 sin rol;
    - plazos: B1 y B12 SIN_PLAZO; B3, B4, B9 y B10 24 h hábiles; B5 3 días; B7 10 días; B6, B8 y B11 PENDIENTE; B2 según F6-D4;
    - `registra_llegada` en B8, `automatica` en B12 y `revisa_etapa` según F6-D4.
  - Transiciones:
    - B1 ENVIAR→B2;
    - B2 APROBAR→B3 y RECHAZAR→B1 (con observación);
    - B3 ENVIAR→B4;
    - B4 APROBAR→B5 y RECHAZAR→B3;
    - B5 APROBAR→B6, DEVOLVER_POR_ERROR→B3 y DEVOLVER_POR_PRODUCTO_NUEVO→B3 (con urgencia);
    - B6 APROBAR→B7 (con observación);
    - B7 APROBAR→B8;
    - B8 APROBAR→B9 y REPORTAR_DEMORA;
    - B9 APROBAR→B10 (con bodega);
    - B10 APROBAR→B11;
    - B11 APROBAR→B12.
  - Documentos según F6-D1. Lo no confirmado queda `PENDIENTES`, como S-2.
  - Un bloque `DO` final comprueba que haya 12 etapas, la forma de las transiciones y que A no cambie.
- **MIRROR**: V2 (siembra de A), V3 (`revisa_etapa`), V11 (`registra_llegada`), V16 (`exige_algun_documento` y bloque de verificación).
- **GOTCHA**: UUID nuevos y deterministas en otro rango. `transicion_destino_segun_accion`: REPORTAR_DEMORA sin destino. `transicion_algun_documento_definido` solo con `DEFINIDOS`. No reservar versiones.
- **VALIDATE** (Codex): flujo B vigente en `GET /flujos/EC/B` y A sin cambios.

### Paso 6.2 — Creación y propuesta (`/ecc:prp-implement`)
- **ACTION**: crear B solo por Compras China; B1 y B2.
- **IMPLEMENT**:
  - exclusión del admin según F6-D2: atributo de la definición leído por `CrearPedidoCasoDeUso`/`PermisosEtapa.paraCrear`, sin `TipoFlujo.B` en el código;
  - mensaje 403 claro;
  - B2 RECHAZAR→B1 con observación obligatoria (dato de la transición y regla existente de RECHAZAR);
  - contrato: documentar la creación de B y el 403 del admin ⏸ (versión menor siguiente).
- **GOTCHA**: un usuario con admin y compras_china crea B por su rol compras_china (P-06: los roles extra no anulan otros).

### Paso 6.3 — Codificación y devoluciones (`/ecc:prp-implement`)
- **ACTION**: B3, B4 y B5 con las reglas del motor.
- **IMPLEMENT**: solo datos y generalización:
  - B4 RECHAZAR→B3;
  - B5 DEVOLVER_POR_ERROR→B3 y DEVOLVER_POR_PRODUCTO_NUEVO→B3 con urgencia de 4/12/24 h, que termina al volver a B5 (`esRetornoDeUrgencia(5)`, por datos);
  - comprobar que ningún texto, aviso o resumen diga "A2"/"A4" fijo: `ResumenDeEtapa` y las plantillas de notificación usan `tipoFlujo`.
- **GOTCHA**: B5 nunca devuelve a B2. Lo garantiza la transición en datos y un CHECK opcional en el bloque `DO`.

### Paso 6.4 — Etapas compartidas (`/ecc:prp-implement`)
- **ACTION**: B6–B12 con las reglas de A5–A11 sin duplicar código.
- **IMPLEMENT**:
  - generalizar `EtapasHabilitadas` (F6-D3), con un constructor de compatibilidad para las pruebas;
  - fabricación con observación, embarque, llegada (`registra_llegada` en B8), demora, bodega en B10, incidencias y Fin automático en B12, por datos;
  - archivos, alertas, historial, cronograma y outbox sin cambios de lógica;
  - registrar en un ADR solo si cambia el motor (por ejemplo, la habilitación por definición).

### Paso 6.5 — Pantallas y guía reutilizable (shadcn, impeccable, `ecc:skill-create`)
- **ACTION**:
  - "Crear pedido propuesto" para Compras China;
  - "Productos propuestos" en la bandeja de Revisión (asistente_compras, B2);
  - 12 etapas en B y 11 en A.
- **IMPLEMENT**:
  - `NuevoPedido` con el tipo según el rol (F6-D5) y la ruta abierta a Compras China;
  - definiciones de A y B cargadas una vez para la línea compacta;
  - sección de llegada y bodega según los datos;
  - etiquetas "B · China";
  - guía `documentos/agregar-flujo.md` (migración, transiciones, permisos, reglas y pantallas) y skill `agregar-flujo-decokasa` (`.claude/skills/`), que recuerda que las pruebas son de Codex;
  - `/impeccable polish` según `DESIGN.md`.
- **GOTCHA**: no mostrar acciones del admin en B1 mientras P-14 siga pendiente. La pantalla solo refleja lo que decide el servidor.

---

## Resultado del paso 6.1 (parte A, 2026-10-21) — pendiente de verificación por Codex

Rama `fase-6-flujo-b`, creada desde `fase-5-seguimiento` (`512d499`) y con los cambios pendientes conservados (`Header.tsx` y los archivos sin seguimiento). No se ejecutaron pruebas, builds ni migraciones.

**`V17__flujo_b_ecuador.sql`** (siguiente versión libre). Solo inserta filas; la definición de A (V2, V3, V11 y V16) no cambia.
- **Flujo:** EC/B versión 1, vigente. UUIDv7 fijos con marca 2026-10-21 (`01a15bc1-f400…`).
- **12 etapas:**
  - roles: B1 y B5–B8 compras_china; B2 y B4 asistente_compras; B3 marketing; B9–B11 logistica; B12 Fin sin rol;
  - plazos: B3, B4, B9 y B10 24 h hábiles (respuesta "24h"); B5 3 días; B7 10 días (documento); B1 y B12 SIN_PLAZO; B2, B6, B8 y B11 PENDIENTE;
  - `registra_llegada` en B8 y `automatica` en B12.
- **16 transiciones:**
  - B1 ENVIAR→B2;
  - B2 APROBAR→B3 y RECHAZAR→B1 (con observación);
  - B3 ENVIAR→B4;
  - B4 APROBAR→B5 y RECHAZAR→B3 (con observación);
  - B5 APROBAR→B6, DEVOLVER_POR_ERROR→B3 (con observación) y DEVOLVER_POR_PRODUCTO_NUEVO→B3 (con observación y urgencia; termina al volver a B5);
  - B6 APROBAR→B7 (con observación);
  - B7 APROBAR→B8;
  - B8 APROBAR→B9 y REPORTAR_DEMORA (con observación);
  - B9 APROBAR→B10;
  - B10 APROBAR→B11 (con bodega y al menos un documento);
  - B11 APROBAR→B12 (al menos un documento).
  
  Todas en `DEFINIDOS`.
- **Documentos con tipo:** "Solicitud" (B1), "Documentos codificados" (B3) y "Packing List" (B7).
- **Bloque `DO` final:** comprueba un solo B vigente, 12 etapas, Fin en B12, llegada en B8, 16 transiciones definidas, devoluciones de B5 solo a B3, documentos exactos y A intacto (11 etapas y 14 transiciones).

**No extendido por analogía (sigue pendiente, F6-D4):**
- plazo de B2 (PENDIENTE);
- `revisa_etapa` de B2 y B4: sin marca, así que mientras tanto solo el admin y quien actúa en la etapa del archivo pueden descargarlo.

**Huecos conocidos hasta los pasos siguientes** (no se corrigen en 6.1; se pide no adelantar pasos):
- **6.2:** con V17, `POST /pedidos` con `tipoFlujo: B` ya encuentra una definición vigente, y `PermisosEtapa.paraCrear` todavía admite al admin. Hasta 6.2, el admin podría crear B por la API. La pantalla solo envía `'A'`. P-14 sigue ⏸.
- **6.4:** `EtapasHabilitadas` (`{1..10}`) deja B11 Incidencias sin acciones. B no llega a Fin hasta F6-D3.
- **6.5:** el tablero y la bandeja dibujan la línea con la definición de A. El detalle sí usa la definición de su propio flujo.

**Pruebas que pueden verse afectadas (no se modificaron):**
- `MigracionesIT`, `MigracionesV16IT`, `MigracionesFase4IT`, `MigracionesFase5IT` y `V2RestriccionesIT`: la última versión pasa a 17.
- `FlujoFaseDosTest:74` espera que no haya B vigente. Si usa la base real, fallará; si usa un repositorio en memoria, no.
- Cualquier prueba que espere 400 "Ese tipo de flujo todavía no está disponible" para B.

**Casos para Codex (6.B1, referencia):**
- V17 sobre una base limpia y sobre V16 con pedidos de A en curso: A sin cambios.
- `GET /flujos/EC/B` devuelve 12 etapas, sus acciones, documentos y plazos (con PENDIENTE donde corresponde).
- El bloque `DO` falla ante datos alterados.

## Resultado del paso 6.2 (parte A, 2026-10-21) — pendiente de verificación por Codex

No se ejecutaron pruebas, builds ni migraciones. No hay pantallas (son del 6.5) ni B3–B5 (6.3).

**Creación de B (F6-D2):**
- **`PermisosEtapa.paraCrear`:** crea solo quien tiene, vigente en la base (`ActorVigente` releído por `ApoyoPedidos.solicitante`, cuenta activa con roles), el rol responsable de la primera etapa de la definición vigente.
  - Se retiró la excepción "o el admin".
  - A queda igual, porque A1 es `admin` y el `Interviniente` sigue siendo `admin`.
  - B: solo `compras_china`. Un admin que también tenga `compras_china` crea B por ese rol; el `Interviniente` queda como `compras_china`.
  - El admin sin `compras_china` recibe 403 `SIN_PERMISO`: "Solo el responsable de la primera etapa del flujo crea pedidos de ese flujo".
- **Javadoc:** actualizado en `Pedido.crear` y `CrearPedidoCasoDeUso.definicionVigente`.
- **Sin cambios:** P-06 (el admin vigente sigue pudiendo actuar en cualquier etapa, incluida B1) y la reasignación.

**B1 y B2 (datos de V17, motor sin cambios):**
- **B1 ENVIAR→B2:** exige "Solicitud" de la entrada actual. Se admiten adjuntos adicionales, como el producto propuesto. Plazo SIN_PLAZO.
- **B2 APROBAR→B3:** sin documentos obligatorios. Plazo PENDIENTE, sin vencimiento (F6-D4).
- **B2 RECHAZAR→B1:** con observación obligatoria (dato de la transición y regla fija de RECHAZAR). Lo que hace el motor genérico:
  - B2 queda DEVUELTA;
  - B1 vuelve a entrar con una entrada nueva y suma una reentrada;
  - los archivos de la entrada anterior ya no acreditan "Solicitud".
- **Reutilizados sin duplicar:** archivos, P-06, entradas, historial (`ETAPA_RECHAZADA` y demás) y outbox. Los textos usan el `tipoFlujo` del pedido ("B1", "B2").

**Contrato 0.15.0-borrador:**
- `POST /pedidos` documenta la regla y el 403 del admin en B.
- La tabla de transiciones B1/B2 queda actualizada: "Solicitud" en B1 y observación obligatoria con entrada nueva en el rechazo de B2.
- Sin cambios de esquema.
- `arquitectura.md` (quién crea), al día.

**Descarga por Revisión (P-17, sigue pendiente; no se amplió):**
- V17 no marca `revisa_etapa` en B2 ni en B4.
- Con `AutorizarArchivosCasoDeUso`, la asistente de compras en B2 no puede descargar los archivos de B1 (la Solicitud y el producto propuesto) salvo que también sea admin o pueda actuar en B1. En B4 pasará lo mismo con los de B3.
- **Efecto en el recorrido:** no bloquea ninguna transición, porque B2 APROBAR/RECHAZAR no exige documentos. Pero quien revisa decide sin poder descargar la Solicitud ni el producto propuesto de B1 (`AutorizarArchivosCasoDeUso` y `Pedido.puedeDescargar`). Qué metadatos ve en el listado no está comprobado; lo comprueba Codex. Hasta decidir F6-D4, la revisión real necesita al admin o un rol adicional, o queda limitada. Es un pendiente visible, no un permiso inferido.

**Huecos que siguen hasta pasos posteriores:**
- B11 sin acciones hasta 6.4 (F6-D3).
- Las pantallas crean solo A, y los tableros dibujan B con la definición de A, hasta 6.5.

**Pruebas que pueden verse afectadas (no se modificaron):**
- Las que crean pedidos como admin en definiciones ficticias cuya primera etapa no es `admin` (por ejemplo, fixtures por SQL en `Fase4IT` y suites de migración): ahora 403.
- Las que buscaban el mensaje anterior "Solo el responsable de la primera etapa o el admin crean pedidos." (no se encontraron referencias en `src/test`).
- No se encontraron pruebas que esperen 400 al crear B. `FlujoFaseDosTest:74` usa un repositorio simulado y no depende de V17.
- Desde V17 (no por 6.2), `V2RestriccionesIT:65-69` espera solo `EC:A:1:true` en `definicion_flujo` y cuenta etapas sin filtrar por flujo: fallará con B. Hay que filtrar por el flujo A o añadir B a lo esperado, y revisar conteos globales en `MigracionesIT`.

**Revisión Java (`ecc:java-reviewer`, solo lectura):**
- **Resultado:** sin hallazgos críticos ni altos. El actor se relee de la base, el permiso se revisa antes de reservar el número, A no cambia, P-06 y la reasignación siguen igual, y B2→B1 funciona con el motor genérico.
- **Anotado para 6.4:** `EtapasHabilitadas` es global por número, así que habilitar el 11 para B también habilitaría A11 (Fin). El motor rechaza acciones en etapas automáticas, pero F6-D3 debe resolverlo por flujo o por definición.
- **Sin cobertura todavía (Codex):** creación de B por compras_china, por admin solo y por admin con compras_china; B2 RECHAZAR→B1 con la definición real de B.

**Casos para Codex (6.B1):**
- **Crear B:**
  - `compras_china` activo → 201, el pedido en B1 y `PedidoCreado` con rol `compras_china`;
  - admin solo → 403, sin número consumido ni pedido;
  - admin con `compras_china` → 201 con rol `compras_china`;
  - `compras_china` desactivado o con el rol retirado después de emitir el JWT → 403 (estado vigente);
  - `asistente_compras` u otro rol → 403.
- **Crear A:** como antes. El admin → 201; `compras_china` sin admin → 403.
- **B1 ENVIAR:**
  - sin Solicitud → 422 "Solicitud";
  - con una Solicitud de otra entrada → 400;
  - con Solicitud y un adjunto adicional (producto propuesto) → avanza a B2, con los dos en el historial.
- **B2 RECHAZAR:**
  - sin observación → 422;
  - con observación → B1 con entrada nueva y B2 DEVUELTA; hay que volver a subir la Solicitud para el siguiente ENVIAR.
- **B2 APROBAR** → B3, sin documentos. Las acciones de B3 en adelante quedan para 6.3.
- **Permisos de B2:** la asistente de compras actúa en B2; el admin actúa por P-06; un rol ajeno, 403. La descarga de archivos de B1 desde B2: 403 para la asistente sin otro rol (documentado, no ampliado).
- **Regresión de A**, del historial y del outbox (aviso de cambio de etapa con códigos "B1"/"B2").

## Resultado del paso 6.3 (parte A, 2026-10-21) — pendiente de verificación por Codex

No se ejecutaron pruebas, builds ni migraciones. **No hizo falta ampliar el motor:** B3–B5 funcionan con los datos de V17 y el código existente. Solo se actualizaron dos comentarios para citar B: el de P-10 en `Pedido.java` y el de `MotivoDevolucion`.

| Regla | Cómo la cumple el motor (sin números de A) |
|---|---|
| B3 ENVIAR→B4 con "Documentos codificados" | `documento_requerido` de V17 y `Pedido.exigirRequisitos` sobre `documentosPresentes`, ya validados por pedido, etapa y entrada actual. |
| B4 APROBAR→B5 y RECHAZAR→B3 | Destino tomado de la transición. RECHAZAR exige observación por el dato y por la regla fija. B4 queda DEVUELTA y B3 vuelve a entrar con una entrada nueva. |
| B5 DEVOLVER_POR_ERROR→B3, nunca B2 | Destino en los datos. El bloque `DO` de V17 rechaza otro destino. B4, que estaba COMPLETADA, pasa a DEVUELTA (regla numérica genérica de intermedias). |
| B5 DEVOLVER_POR_PRODUCTO_NUEVO→B3 con 4/12/24 h | Una `UrgenciaActiva` nueva reemplaza a la anterior. `urgenciaActivacionId` es la entrada creada en B3. El vencimiento usa horas hábiles (regla provisional). |
| Hasta regresar a B5 | `DefinicionFlujo.esRetornoDeUrgencia`: termina al entrar en la etapa que admite `DEVOLVER_POR_PRODUCTO_NUEVO`, que en B es B5, dato de V17. B4 APROBAR→B5 la cierra. |
| Rechazo de B4 durante la urgencia | La mantiene, con el mismo vencimiento e identificador (P-10, provisional del equipo, igual que A3→A2). No se inventó otra regla. |
| Calendario, avisos e historial | `CalcularVencimiento`, la detección ETAPA/URGENCIA (V15), `PedidoDevuelto` con motivo y nivel, `ResumenDeEtapa` (`tipoFlujo` + número, "vuelve a B3") y el outbox: todo genérico. Plazos de B3/B4/B5 de V17. |

Compatibilidad con A: sin cambios de código, así que A conserva su comportamiento.

**Revisión Java (`ecc:java-reviewer`, solo lectura):**
- Sin hallazgos críticos ni altos. Confirma, con referencias a archivo y línea, que no hay nada fijado al flujo A que impida B3–B5.
- Medio: la descarga de los documentos codificados de B3 está bloqueada para B4 (y para B5) mientras `revisa_etapa` siga nulo. Es lo mismo que A4 respecto a A2; se documenta y no se amplía.
- Medio: pruebas afectadas desde V17 (no por 6.3), listadas abajo.
- Bajo: el Javadoc de `EtapasHabilitadas` está redactado solo para A. En B la etapa 10 es Bodega y la 11 Incidencias; se revisa en 6.4.

**Pruebas que pueden verse afectadas (desde V17; no se modificaron):** hacen consultas globales sin filtrar por flujo.
- `V2RestriccionesIT:65-88`: flujos, 11 etapas, 14 transiciones y 3 documentos.
- `MigracionesFase4IT:26`: `registra_llegada` pasa de 1 a 2.
- `MigracionesV16IT:18`: `exige_algun_documento` pasa de [9,10] a [9,10,10,11].
- Las suites de versión de Flyway esperan la versión 16.

**Casos para Codex (6.B1/6.B2):**
- **B3:** sin "Documentos codificados" → 422; con documentos de otra entrada → 400; con los válidos → B4.
- **B4:** sin observación al rechazar → 422; rechazo → B3 con entrada nueva; aprobación → B5.
- **B5 por error:** sin observación → 422; con ella → B3, con B4 DEVUELTA y sin urgencia.
- **B5 por producto nuevo:** sin nivel → 422. Con 12 h: urgencia con vencimiento en horas hábiles, `urgenciaActivacionId` igual a la entrada nueva de B3, y un aviso URGENCIA al vencer (solo detección manual).
  - Un rechazo de B4 la mantiene.
  - B4→B5 la termina (`urgenciaTerminada`).
  - Una segunda devolución la reemplaza.
- **Historial y resúmenes:** dicen "B3"/"B5", nunca "A2"/"A4".
- **Regresión de A2–A4.**

### Bloqueo funcional de revisión de documentos (B2 y B4) — propuesta para decidir (F6-D4)

**Situación:** el permiso de descarga (P-17) admite al admin, a quien actúa en la etapa del archivo y a quien actúa en la etapa que la definición marca como revisora (`revisa_etapa`). En A solo está marcada A3 revisa A2 (V3). V17 no marcó ninguna en B, así que:
- en B2, la asistente de compras no puede abrir la Solicitud ni el producto propuesto de B1;
- en B4, no puede abrir los "Documentos codificados" de B3.

Puede aprobar o rechazar sin verlos. No bloquea transiciones, pero vacía de contenido la revisión.

**Propuesta concreta (solo datos; sin código; compatible con A):**
- Una migración posterior (V18) que marque, en la versión 1 del flujo B:
  - B2 `revisa_etapa = 1`, porque B2 es "Revisión de productos propuestos" (1.5: "da check o devuelve a la etapa 1");
  - B4 `revisa_etapa = 3`, porque B4 es "Revisión y aprobación" de la Codificación, igual que A3 revisa A2 (requerimientos 1.5: "Si desaprueba, vuelve a la etapa 3").
- Con un bloque `DO` que compruebe solo esas dos marcas y A3 revisa A2 sin cambios.
- B5 (Análisis) no queda como revisora de B3, igual que A4 respecto a A2, que sigue pendiente. Si se quisiera, sería otra decisión.
- Requiere aprobación de Richard, porque amplía quién descarga. Hasta entonces el bloqueo sigue visible.

## Revisión en B2/B4 (F6-D4, parte) y resultado del paso 6.4 (parte A, 2026-10-21) — pendiente de verificación por Codex

No se ejecutaron pruebas, builds ni migraciones. No hay pantallas (son del 6.5).

**Etapas revisoras (aprobado por Richard el 2026-10-21, no por el cliente): `V18__revision_flujo_b.sql`.**
- Solo datos de B: B2 `revisa_etapa = 1` y B4 `revisa_etapa = 3`.
- Un bloque `DO` verifica que en B solo existen esas dos marcas (B5 no revisa B3) y que A conserva solo A3 revisa A2.
- El permiso de descarga no cambia de código: `AutorizarArchivosCasoDeUso.autorizarDescarga` → `Pedido.puedeDescargar` → `PermisosEtapa.puedeActuar`. Pueden descargar:
  - el admin vigente;
  - quien puede actuar en la etapa del archivo;
  - quien puede actuar en la etapa revisora, con la regla P-06: cuenta activa y roles vigentes, y si la revisora tiene persona asignada, solo ella (no todo el rol) o el admin.
  
  Se conservan los permisos por varios roles.
- **Pendiente:** el plazo de B2 sigue PENDIENTE (la otra parte de F6-D4).

**Habilitación por flujo y definición (F6-D3; ampliación del motor, compatible con A):**
- **`EtapasHabilitadas`** pasa a ser un record `(numeros, porDefinicion)`, con la fábrica estática `segunDefinicion()`:
  - `segunDefinicion()` es el bean de `PedidosConfig`. Cada caso de uso la concreta con la definición del flujo del pedido:
    - `para(etapas)` habilita las etapas con rol y no automáticas;
    - `paraEtapa(n, manual)` sirve cuando la acción ya se resolvió.
  - Sin concretar, `incluye()` lanza `IllegalStateException`: nunca habilita por defecto.
  - Los conjuntos fijos (`faseDos()` … `faseCuatroHastaIncidencias()` y el constructor con `Set`) se conservan para las pruebas y devuelven el mismo conjunto.
- **Casos de uso que concretan:** `ConsultarPedidos` (acciones disponibles), `EjecutarAccionEtapa` (avance y bodega), `AgregarObservacion`, `AutorizarArchivos` (subida) y `RegistrarMotivoRetraso`.
- **`ContratoDeEtapa.automatica()`:** método por defecto que el record `DefinicionEtapa` sobrescribe.
- **`DefinicionFlujo`:** rechaza al construirse cualquier transición que salga de una etapa automática. Ningún Fin (A11, B12) admite acciones manuales, en ningún flujo.
- **Resultado:**
  - A: A1–A10 habilitadas, igual que con `faseCuatroHastaIncidencias`, y A11 automática;
  - B: B1–B11 habilitadas y B12 automática.
  
  El número 11 no se habilita de forma global: en A sigue siendo Fin.

**B6–B12 (reglas de A5–A11 por datos de V17, sin duplicar el motor):**
- B6 Fabricación con observación.
- B7 Embarque con "Packing List" (BL opcional).
- B8 Aduana: `registra_llegada` para el "✓ Llegada" (`llegadaAduanaEn`) y `REPORTAR_DEMORA`.
- B9 Autorización de salida.
- B10 Bodega: bodega activa y al menos un documento.
- B11 Incidencias: al menos un documento.
- B12 Fin automático, en la misma transacción, con `PEDIDO_FINALIZADO` del sistema.

Archivos, entradas, calendario, vencimientos, historial, resúmenes y outbox, sin cambios de lógica. En las migraciones de bodega, demora y llegada no hay restricciones atadas a números de A; solo la siembra de A7 en V11. Plazos PENDIENTES de B6, B8 y B11, sin inventar valores.

**Pruebas que pueden verse afectadas (no se modificaron):**
- Las que cargan el contexto de Spring usan ahora el bean según la definición. Para A el resultado es el mismo (A1–A10).
- Las que construyen `EtapasHabilitadas` fijas o llaman a `numeros()` siguen compilando.
- Una prueba que construya `DefinicionFlujo` con transiciones desde una etapa automática fallará al construirla (no se conoce ninguna).
- Las suites de migración esperan la versión 16 o 17 y la versión pasa a 18.
- **`V2RestriccionesIT:186`** (alto, por V18): la consulta de `revisa_etapa` no filtra por flujo y espera exactamente `"3:2"`; ahora también devolverá `"2:1"` y `"4:3"`. Hay que filtrar por el flujo A y añadir una aserción para B. Las líneas 187 y 188 (`where numero=3`) siguen fallando con 23514 y 23503 como esperan, pero conviene filtrarlas por flujo.

**Revisión Java (`ecc:java-reviewer`, solo lectura):**
- **Crítico, corregido:** el componente del record se llamaba `segunDefinicion`, igual que la fábrica estática, y no compilaba. Se renombró el componente a `porDefinicion`.
- **Verificado sin hallazgos:**
  - todas las llamadas a `incluye()` reciben una instancia ya concretada, y ningún otro módulo usa el bean;
  - las pruebas con conjuntos fijos siguen compatibles;
  - el guard de `DefinicionFlujo` no rompe V2, V17 ni los fixtures conocidos;
  - `automatica()` queda sobrescrito por el record `DefinicionEtapa`;
  - ArchUnit, correcto;
  - V18 es idempotente y su bloque `DO` es correcto;
  - la descarga respeta P-06 (cuenta vigente, asignación, admin y varios roles); B5 no revisa B3 y A no cambia;
  - sin literales de A en B6–B12 en pedidos, flujos ni plazos.
- **Bajo, para Codex:**
  - una prueba unitaria del contrato de `EtapasHabilitadas`: sin concretar lanza la excepción; `para` excluye A11 y B12; `paraEtapa(n, false)` queda vacía;
  - una prueba de `DefinicionFlujo` que rechace una transición desde una etapa automática.

**Casos para Codex (6.B1/6.B2):**
- **V18:**
  - B2 revisa B1 y B4 revisa B3; B5 sin acceso; A3 revisa A2 sin cambios.
  - Descarga de B1 desde B2:
    - asistente_compras sin asignación → 200;
    - B2 asignada a otra persona → 403 para el resto del rol y 200 para la asignada;
    - cuenta desactivada o rol retirado → 403;
    - admin → 200;
    - compras_china en B5 descargando B3 → 403.
- **Habilitación:**
  - A1–A10 como antes; A11 sin acciones.
  - B11 con acciones; B12 sin acciones, ni por API ni en `accionesDisponibles`.
  - Observaciones, subidas y motivo de retraso en B11 permitidos; en A11 y B12 rechazados.
- **Recorrido B6→B12:**
  - B6 sin observación → 422;
  - B7 sin Packing List → 422, solo con BL → 422, con Packing List → B8;
  - B8: demora y llegada;
  - B9 → B10;
  - B10 sin bodega o sin documento → 422; bodega inactiva → 400;
  - B11 sin documento → 422; con documento → B11 y B12 completadas, pedido finalizado y un solo `PEDIDO_FINALIZADO`;
  - luego, acciones manuales → 409.
- **Recorrido completo B1→B12** con rechazo de B2, rechazo de B4 y devolución con urgencia de 12 h, junto con la regresión de A (6.B2).

## Resultado del paso 6.5 (parte A, 2026-10-21) — pendiente de verificación por Codex

No se ejecutaron pruebas, builds ni lint. Sin cambios en el backend ni en `Header.tsx`. Ningún motor en el navegador: las pantallas solo leen las definiciones y el servidor decide.

**Definiciones por flujo:**
- `api/pedidos.ts → useDefinicionesVigentes()`: una consulta por flujo (A y B de Ecuador), con la misma clave de caché que `useDefinicionFlujo`.
- `TableroTrabajo`, `BandejaPedidos`, `ListaPedidos` y "Productos propuestos" pasan `recorrido={{ A, B }}`: la línea compacta dibuja 11 etapas en A y 12 en B. El detalle sigue usando la definición de la versión de su pedido.

**Crear pedidos:**
- **Quién ve el botón:** `formato.ts → flujosQuePuedeCrear(definiciones, roles)` devuelve los flujos cuya primera etapa tiene un rol de la persona (la regla del servidor del 6.2). El admin ve A; Compras China ve B; quien tiene los dos roles ve ambos. El admin sin Compras China no ve B (P-14).
- **Ruta:** `/pedidos/nuevo?flujo=A|B` ya no está bajo `SoloAdmin`. `NuevoPedido` muestra "Sin permiso" si la persona no puede crear ningún flujo.
- **`NuevoPedido`:**
  - título "Nuevo pedido" (A) o "Crear pedido propuesto" (B);
  - si la persona puede crear los dos, elige con un `RadioGroup`, sin valor por defecto (F6-D5);
  - textos de la primera etapa, su destino y sus documentos, sacados de la definición (B1 → B2, "Solicitud", con el producto propuesto como adjunto adicional);
  - "Incluye productos nuevos" solo en A (requisito 4.2 #6).
- **Accesos:** botones de crear en Pedidos y en el dashboard.

**Productos propuestos (`dashboards/ProductosPropuestos.tsx`):**
- Para quien tiene el rol de la etapa que revisa la primera de B (B2, por `revisa_etapa`), o para el admin.
- Lista los pedidos B de esa etapa (`GET /pedidos?tipoFlujo=B&etapa=2`, P-13), paginados, con su línea de 12 etapas y un enlace a Pedidos filtrado.
- **Diferencia con la bandeja:** esta lista no filtra por asignación; la bandeja sí. Las acciones y la descarga las decide el servidor.

**Coherencia por flujo:**
- **Bandeja:** grupos por flujo y etapa (A3 ≠ B3), cada uno con su definición; aviso de documentos pendientes con el código del flujo correcto.
- **Pedidos:** filtro "Flujo y etapa actual" agrupado por flujo (`flujo` y `etapa` en la dirección); una etapa solo filtra junto con su flujo. Se envía `tipoFlujo` al servidor.
- **Detalle:**
  - nombre del flujo con `nombreFlujo`;
  - la sección "Llegada y bodega" aparece si la definición tiene etapa de llegada o elige bodega, ya no si el tipo es "A";
  - línea de tiempo, cronograma, historial y resúmenes ya usaban `tipoFlujo`.
- **`DialogoAccion`:** la urgencia dice "hasta que el pedido vuelva a {código} · {nombre}" de la etapa que devuelve (A4 o B5), sin "Análisis" fijo. Los destinos salen de la definición: B5→B3 y B4→B3.
- **Descarga desde B2 (de B1) y desde B4 (de B3):** `PanelEtapa` ya calculaba la pista con `revisaEtapa`. Con V18 funciona sin código nuevo, y el servidor la vuelve a validar (P-06 y P-17).

**Skill `agregar-flujo-decokasa`** (`.claude/skills/agregar-flujo-decokasa/SKILL.md`, nueva; se comprobó que no existía). Cubre:
- fuentes y origen de cada decisión, sin extender nada por analogía;
- migración versionada solo de inserción, con bloque `DO`;
- tablas y columnas;
- qué parte del motor ya es genérica (habilitación, creación, P-06, P-17, urgencia, Fin);
- búsqueda de supuestos fijos y ADR si se amplía el motor;
- contrato y documentación;
- pantallas (definiciones, códigos, agrupación, creación, bandejas especiales, DESIGN.md);
- traspaso a Codex con los casos mínimos.

**impeccable polish:**
- se reutilizaron los componentes shadcn y los tokens de `DESIGN.md`, sin colores nuevos;
- estados con texto e ícono, `min-h-10` en las opciones y títulos jerárquicos;
- la elección de flujo es un grupo de radio con leyenda, y los errores se muestran con `FieldError`.

No hubo revisión visual en el navegador: queda para Codex.

**Pruebas del frontend que pueden verse afectadas (no se modificaron):**
- `NuevoPedido`, rutas y `ListaPedidos`: necesitan respuestas MSW de `GET /flujos/EC/A` y `/flujos/EC/B`. El botón y la pantalla de crear dependen de las definiciones, el texto del filtro pasó a "Flujo y etapa actual" y `/pedidos/nuevo` ya no muestra "Sin permiso" para Compras China.
- `DashboardRol`: botones de crear según las definiciones y el panel "Productos propuestos".
- `BandejaPedidos`: los ids de las secciones pasan de `bandeja-etapa-3` a `bandeja-etapa-A3`.
- `DialogoAccion`: el texto de la urgencia (antes "vuelva a Análisis").

### Casos para Codex (6.B1–6.B3)

**6.B1 — Reglas del flujo B** (`ecc:tdd-workflow`, `ecc:springboot-tdd`):
1. **V17 y V18** sobre una base limpia y sobre V16 con pedidos de A en curso: A intacto y bloques `DO` correctos. Adaptar las pruebas globales: `V2RestriccionesIT` (`definicion_flujo`, etapas, transiciones, documentos y `revisa_etapa`), `MigracionesFase4IT:26`, `MigracionesV16IT:18` y la versión de Flyway (18).
2. **Creación:** compras_china → 201 en B1; admin solo → 403 sin consumir número; admin con compras_china → 201 con rol compras_china; cuenta desactivada o rol retirado → 403; A sin cambios.
3. **B1 y B2:** sin Solicitud → 422; Solicitud de otra entrada → 400; B2 RECHAZAR sin observación → 422; con ella → B1 con entrada nueva.
4. **B3–B5:**
   - B3 sin "Documentos codificados" → 422;
   - B4 RECHAZAR → B3;
   - B5 DEVOLVER_POR_ERROR → B3, con B4 DEVUELTA;
   - DEVOLVER_POR_PRODUCTO_NUEVO de 12 h → urgencia en horas hábiles, que se mantiene si B4 rechaza (P-10, provisional), termina en B5 y se reemplaza con una devolución nueva.
5. **B6–B12:** las reglas de A5–A11 (observación, Packing List, llegada y demora en B8, bodega y documento en B10, documento en B11), Fin automático y `PEDIDO_FINALIZADO` único; acciones posteriores → 409.
6. **Habilitación:** A1–A10 y B1–B11 operables; A11 y B12 sin acciones (API y `accionesDisponibles`), y tampoco observaciones, subidas ni motivo de retraso en ellas. Pruebas unitarias de `EtapasHabilitadas` (sin concretar lanza; `para` excluye las automáticas) y de `DefinicionFlujo` (rechaza transiciones desde automáticas).
7. **Descarga P-17:**
   - B2 descarga B1 y B4 descarga B3, con y sin asignación (con asignación: solo la asignada o el admin);
   - B5 no descarga B3;
   - cuenta o rol no vigente → 403.
8. **Regresión** del motor y del flujo A completo (V16).

**6.B2 — Recorrido crítico A y B** (`playwright-best-practices`, `ecc:e2e-testing`):
- **Escenario @critico de B:** Compras China crea un pedido propuesto, adjunta la Solicitud y el producto propuesto, y envía. Luego:
  - Revisión lo ve en "Productos propuestos", descarga y rechaza a B1;
  - Compras China corrige y reenvía; Revisión aprueba;
  - Marketing codifica; Revisión de B4 descarga y aprueba;
  - B5 devuelve por producto nuevo con 12 h; ciclo de vuelta hasta B5;
  - B6→B12 con Packing List, llegada, bodega y documento de B11, hasta Fin.
- **Junto con A:** ejecutar a la vez el recorrido de A, con usuarios ficticios (máximo 20 pedidos).
- **Comprobar:**
  - 12 y 11 etapas en las líneas;
  - códigos B en historial, bandeja, avisos y urgencia ("vuelva a B5");
  - bandejas sin mezclar A3 y B3;
  - adjuntos por entrada, avisos del outbox y aislamiento entre pedidos;
  - el admin sin Compras China no ve "Crear pedido propuesto" y la API le responde 403.
- **Accesibilidad:** axe y teclado en `NuevoPedido` (grupo de radio) y en "Productos propuestos".

**6.B3 — Revisión y guía de flujos** (`/code-review`, `ecc:database-reviewer`, `/ecc:verification-loop`, `/ecc:learn-eval`):
- Revisar V17, V18 y la ampliación del motor (`EtapasHabilitadas.segunDefinicion`, `ContratoDeEtapa.automatica`, el guard de `DefinicionFlujo`, `PermisosEtapa.paraCrear`). Proponer un ADR si el equipo considera que la habilitación por definición es una decisión de arquitectura; hoy está registrada solo en este plan.
- Comprobar que `agregar-flujo-decokasa` refleja el código entregado y el traspaso a Codex.
- Registrar el resultado conjunto de A y B y los pendientes en `documentos/pruebas/fase-6-informe.md`.

## Estado de la parte A de la fase 6

Pasos 6.1–6.5 implementados y **sin verificar**: falta la parte B de Codex (6.B1–6.B3) y la validación de Richard. **La fase 6 no está cerrada.**

Siguen pendientes:
- plazos de B2, B6, B8 y B11;
- P-10 (provisional);
- P-14 (el admin crea B);
- la meta de días;
- 3.B3 (Google real, pospuesto).

Las fases 3, 4 y 5 siguen abiertas.

## Bloqueos
1. ~~F6-D1 (documentos de B)~~: decidida por Richard el 2026-10-21 e implementada en V17.
2. P-14 (admin crea B) sigue ⏸: se implementa la exclusión, no la permisión.
3. Plazos PENDIENTES de B6/B8/B11 (como A5/A7/A10): no bloquean el recorrido, pero el cronograma muestra "sin estimar".
4. 3.B3 sigue pospuesto (necesita a Richard en la consola). Las fases 3–5 siguen abiertas.

## Testing Strategy (parte B, Codex; referencia)
6.B1–6.B3 del manual:
- creación solo por Compras China y admin rechazado;
- B2→B1, B4→B3, B5→B3 por error, y producto nuevo con urgencia hasta B5;
- 12 etapas y B6–B12 compartidas;
- regresión de A;
- recorrido @critico de B con urgencia de 12 h, junto con A;
- revisión de la configuración versionada y de la guía.

## Risks
| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Extender por inercia los documentos de A a B | Media | Alta | F6-D1 explícita; `PENDIENTES` mientras tanto |
| Que la habilitación global siga bloqueando B11 | Alta | Alta | F6-D3 en 6.4, con constructor de compatibilidad |
| Tableros que dibujan B con la definición de A | Alta | Media | Definición por `tipoFlujo` (6.5) |
| Rama desde una fase sin integrar | Media | Media | Registrar el desvío; integrar en orden 3→4→5→6 |

## Notes
- Contrato vigente: 0.14.0-borrador. Última migración: V16. Siguiente libre: V17 (no reservada).
- ⏸ y [P] se mantienen. Ninguna decisión de esta tabla se presenta como respuesta del cliente.

## Hallazgos documentales de 6.B3 (parte A, 2026-10-21) — pendiente de revisión por Codex

Solo documentación, según `documentos/pruebas/fase-6-informe.md` (6.B3). No se tocaron producción ni pruebas, no se ejecutó nada y no se cierra ninguna fase. Las notas anteriores de este plan («sin verificar», «última migración V16») se conservan como historia de la parte A; la verificación vigente está en el informe de Codex.

- **F6-B3-H01:** la skill `agregar-flujo-decokasa` distingue alta de flujo, versión nueva y completar una versión publicada. Su §1-bis describe la publicación de versiones: en una sola migración, desactivar la vigente y luego insertar la nueva completa (índice `definicion_flujo_vigente_unica`, 23505 si se invierte el orden); conservar `pedido.flujo_id` y todas las filas de la versión anterior; coordinar números de Flyway entre ramas; y revisar antes las listas, que dibujan con la definición vigente (`PedidoResumen` no trae `versionFlujo`). Añade el caso de Codex «nueva versión con pedidos en v1».
- **F6-B3-H02:** se creó `documentos/agregar-flujo.md`, una guía breve (qué es cada cambio, quién decide y reparto A/B) enlazada a la skill, que centraliza los pasos operativos. Ninguna de las dos prevalece sobre requisitos, contrato ni ADR aceptados (corregido tras la revisión de Codex).
- **P6-B3-ADR:** se registró [ADR-0019](../../../documentos/adr/0019-habilitacion-por-definicion.md) en estado **proposed**: habilitación por definición (F6-D3), guard de automáticas, creación por el rol de la primera etapa (F6-D2), límites y conservación de A/B. Se le asignó el siguiente número libre al registrarlo; su aceptación la decide Richard. P-14 sigue ⏸.
- **Límite conocido, sin implementar:** una versión nueva que cambie el número, el orden o el nombre de las etapas requiere antes resolver la línea compacta de las listas por versión del pedido (decisión propia).

## Delimitación del límite de versiones en listas y tableros (parte A, 2026-10-24) — no corregido

Solo análisis del código de `fase-6-flujo-b`, sin cambios de código ni ejecución. **El límite sigue abierto**: documentarlo no lo corrige.

**Cuándo aparece.** Solo si se publica una versión nueva de un flujo que cambia el número, el orden o el nombre de sus etapas mientras hay pedidos en curso en la anterior. Hoy cada país y tipo tiene una sola versión (EC/A v1 y EC/B v1), así que no hay un fallo visible. Una versión que solo cambie documentos, plazos o permisos tampoco lo provoca.

**Qué falla entonces:**

| Dónde | Qué hace hoy | Efecto con v1 y v2 a la vez |
|---|---|---|
| `TablaPedidos` → `LineaCompacta` en `ListaPedidos`, `BandejaPedidos`, `TableroTrabajo` y `ProductosPropuestos` | `recorrido[tipoFlujo]` sale de `useDefinicionesVigentes()`, la versión **vigente** del tipo | Un pedido de v1 se dibuja con las etapas de v2: número de pasos, nombres y posición de la etapa actual incorrectos |
| Filtro por etapa (`?tipoFlujo=B&etapa=N`) y grupos (flujo, etapa) del tablero | Filtran y agrupan por **número** de etapa, sin versión | Mezcla en un mismo grupo o filtro etapas distintas de v1 y v2 que comparten número |
| "Productos propuestos" | Toma la etapa revisora de la definición vigente de B | En pedidos de v1 puede filtrar una etapa que no es la revisora de su versión |
| Contrato `PedidoResumen` (0.15.0-borrador) | No trae `versionFlujo` | La pantalla no puede saber con qué versión dibujar cada fila |

**Lo que no está afectado:** las reglas del backend (acciones, permisos, archivos y plazos se resuelven por `pedido.flujo_id`); `nombreEtapaActual`, que el servidor calcula con la versión del pedido; el detalle, que pide `versionFlujo`; y la creación y el botón "Crear", que usan la vigente, como corresponde.

**Garantía actual:** solo de procedimiento (skill `agregar-flujo-decokasa` §1-bis.5: resolverlo antes de publicar). No hay una guarda técnica que impida publicar esa versión.

**Corrección propuesta (pendiente de decisión de Richard; no implementada):**
1. Contrato: añadir `versionFlujo` a `PedidoResumen` (campo nuevo, versión menor del contrato). El backend ya lo conoce por `pedido.flujo_id`.
2. Frontend: cargar con `useQueries` la definición de cada (país, tipo, versión) presente en la página, con caché larga porque una versión publicada no cambia, y dibujar cada fila con la suya.
3. Filtros por etapa: decidir si se aplican a la versión vigente o llevan versión. Es una regla de pantalla que hoy no está definida.
4. Casos para Codex: pedidos de v1 en varias etapas junto a pedidos de v2 con otra numeración, en las cuatro pantallas; detalle sin cambios; ninguna petición N+1 (una consulta por versión, no por fila).

Conviene hacerlo **después de integrar la fase 7**: `api/pedidos.ts` y las listas cambiaron allí (claves por país).
