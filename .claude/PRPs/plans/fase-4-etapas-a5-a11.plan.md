# Plan: Fase 4 — Etapas A5 a A11 (parte A)

## Summary
Completar el flujo A de punta a punta sobre el motor de la fase 2 y las integraciones de la fase 3: Fabricación (A5), Embarque (A6), Aduana (A7, con llegada y demora), Autorización de salida (A8), Bodega (A9, con catálogo), Incidencias (A10) y Fin automático (A11). La parte A solo codifica; las pruebas y la verificación son de Codex (parte B). Varias reglas dependen de respuestas del cliente que siguen abiertas: el plan las separa y no las fija como aprobadas.

## User Story
Como responsable de una etapa posterior a Análisis (Compras China o Logística), quiero recibir el pedido en mi bandeja, cumplir los requisitos de la etapa y darle check, para que el pedido llegue a bodega y termine en Fin con su historial completo.

## Problem → Solution
Hoy el motor solo habilita A1–A4 (`EtapasHabilitadas.faseDos()`); A4 → A5 deja el pedido en Fabricación sin acciones. Las transiciones A5–A10 ya están sembradas en V2, pero A6, A9 y A10 tienen documentos `PENDIENTES`, REPORTAR_DEMORA no tiene implementación (`destino == null` → "todavía no está implementada") y no hay bodegas, demora ni cierre automático. → Etapas A5–A10 habilitadas con sus reglas, catálogo de bodegas, demora en aduana registrada por el servidor, Fin automático y bandejas de Compras China y Logística conectadas.

## Metadata
- **Complexity**: XL (migración, módulo nuevo `bodegas`, extensión del motor, contrato y pantallas). Se divide en los pasos 4.1–4.6 del manual.
- **Source PRD**: `.claude/PRPs/prds/decokasa.prd.md` (fila 4 de Implementation Phases)
- **Manual**: `documentos/manual-paso-a-paso.docx`, fase 4, pasos 4.1–4.6 (parte A)
- **Rama**: `fase-4-etapas-a5-a11` (todavía no creada; ver "Rama y dependencia con la fase 3")
- **Estimated Files**: ~45 (backend ~30, frontend ~12, documentación ~4)

---

## Rama y dependencia con la fase 3

Estado comprobado el 2026-10-19 (solo lectura):
- Rama actual `fase-3-drive-gmail`, HEAD `977301a` ("Guardar avance local de fase 3; 3.B3 pospuesto y fase abierta"), igual a `origin/fase-3-drive-gmail`. Está **1 commit por delante de `develop`** (`6caa56c`, merge de la fase 2). La fase 3 **no está integrada en `develop` y no está cerrada** (3.B3 pospuesto).
- Cambios sin confirmar: `frontend/src/components/Header.tsx` (modificado, 2 líneas) y ~374 archivos sin seguimiento (evidencias de fases 2 y 3, `.vscode/`, documentos sueltos). No se tocaron.

**Recomendación (no ejecutada):**
1. Decidir antes qué hacer con `Header.tsx`: confirmarlo en `fase-3-drive-gmail` si es parte de la fase 3, o guardarlo con `git stash push frontend/src/components/Header.tsx`. Así la fase 4 empieza desde un estado conocido. Los archivos sin seguimiento viajan con el árbol de trabajo y no afectan a la rama.
2. Crear la rama **desde `fase-3-drive-gmail`**, no desde `develop`, porque la fase 4 usa sus puertos y migraciones (V4–V8: Drive, notificaciones y outbox, cargas reanudables, cuotas):
   `git switch -c fase-4-etapas-a5-a11 fase-3-drive-gmail`
3. La migración nueva de la fase 4 es **V9** (siguiente disponible tras V8 de la fase 3).
4. Cuando la fase 3 se integre en `develop` (PR), actualizar la fase 4 con `git merge develop` (o `rebase` si la rama no se compartió) y abrir el PR de la fase 4 **después** del de la fase 3; si se abre antes, el PR hacia `develop` incluiría también los commits de la fase 3.
5. Si la fase 3 cambia antes de integrarse (correcciones de 3.B3), traerlos a la fase 4 con `git merge fase-3-drive-gmail`.

---

## Requisitos, propuestas y decisiones pendientes

### Confirmados (requerimientos 4.4, §1.4 y respuestas del cliente de §5)
| Etapa | Regla confirmada | Fuente |
|---|---|---|
| A5 Fabricación | Compras China da check y observaciones al terminar; pasa a A6 | §5 "David lo único que hace es cuando la fabricación termina dar check y observaciones" |
| A6 Embarque | Compras China; 10 días hábiles; "Packing List / BL"; check al enviar | 4.4, §1.4, MoSCoW "Confirmado" |
| A7 Aduana | Compras China da check a la llegada (→ A8); si se demora, sube una observación y se recalcula la llegada; un retraso arrastra las fechas siguientes | 4.4, §1.4, §5 ("si") |
| A8 Autorización de salida | Logística; 24 h hábiles; check → A9 | 4.4, §5 "24h" |
| A9 Bodega | Logística elige la bodega de una lista abierta (Quito, Guayaquil, Ibarra…), da check y sube observaciones y documentos; 24 h hábiles | 4.4, §1.4, 4.2 #14 |
| Catálogo de bodegas | Lo mantiene Anderson (Logística): nombre, ciudad, activa | §5 "Anderson", 4.6 |
| A10 Incidencias | Logística (Anderson) sube todos los documentos y da check | §5 "Aderson sube todos los documentos" |
| A11 Fin | Solo visual; se marca sola cuando A10 recibe el check | §5 contradicciones ("la etapa 11 es solo visual…") |
| Plazos | Días hábiles, hora de Ecuador; fin de semana → lunes; un retraso arrastra las siguientes | ADR-0006, §5 |
| Contenido | Los datos de productos van en los archivos; el sistema no los valida | ADR-0007, §5 |

### Decisiones del equipo ya aprobadas que se reutilizan
P-05/P-17/P-18 (archivos reales, descarga, entrada actual), P-06/P-13/P-19 (permisos, consulta, observaciones), P-07 (reasignación provisional), P-20 (resumen de etapa), F3-D* de la fase 3 (Drive, cola, correo), límite provisional de 100 MiB.

### Pendientes del cliente (⏸) y decisiones necesarias
| Id | Paso | Decisión | Estado / fuente | Propuesta (no aprobada) | Bloquea |
|---|---|---|---|---|---|
| **F4-D1** | 4.1 | ¿Quién mantiene el catálogo de bodegas? | **Aprobada provisionalmente por Richard (2026-10-19) como decisión del equipo, no confirmada por el cliente:** Logística y admin crean, editan y desactivan bodegas, con roles y estado vigentes comprobados en el servidor. Las bodegas usadas por pedidos se conservan; desactivar impide nuevas asignaciones sin alterar el historial | — | — |
| **F4-D2** | 4.4 | ¿La bodega se elige con el APROBAR de A9 (`bodegaId` en el cuerpo, asignación una sola vez al confirmar) o con una acción ELEGIR_BODEGA separada? | Manual 4.4: "registra la asignación una sola vez al confirmar la acción"; contrato tiene `AccionElegirBodega`; V2 no la siembra | **Aprobada provisionalmente por Richard (2026-10-19) como decisión del equipo, pendiente de confirmación del cliente:** la bodega se asigna únicamente con el APROBAR de A9 (`bodegaId`), con P-06, bodega activa y documentos obligatorios; bodega, avance y eventos atómicos; una sola vez, sin cambios posteriores; ELEGIR_BODEGA no se implementa; la bodega inactiva se traduce a un error del contrato sin capturar otros errores SQL | Contrato y regla de A9 |
| **F4-D3** | 4.2 | Plazo de Fabricación | ⏸ "TBD"; la calculadora usa 8 + 31 días; manual: "No fijes como aprobado un plazo… que el cliente no haya definido" | Mantener `PENDIENTE` (sin vencimiento) | Vencimiento de A5 |
| **F4-D4** | 4.2 | Documentos de Embarque: ¿Packing List **y** BL (dos obligatorios) o Packing List **o** BL (uno basta)? | ⏸ S-2; manual: "Packing List o BL según el requisito vigente"; hoy `PENDIENTES` | Ninguna sin respuesta: "y" cabe en el modelo actual; "o" exige una regla nueva de alternativas en el motor | **Avance A6 → A7 (y todo lo posterior)** |
| **F4-D5** | 4.3 | Demora en aduana: ¿la nueva fecha la escribe David o la calcula el sistema? ¿Qué fechas "arrastra" en la fase 4? | ⏸ §5 "si" ambiguo; propuesta documentada "David escribe la fecha" | **Aprobada provisionalmente por Richard (2026-10-19) como decisión del equipo, pendiente de confirmación del cliente:** quien puede actuar en A7 (P-06) escribe `nuevaFechaEstimadaLlegada`; primero pasa al lunes si cae en fin de semana y después la fecha efectiva no puede ser anterior a hoy en Ecuador y debe ser posterior a la estimación vigente; el pedido guarda la demora vigente y el historial cada demora con la estimación anterior y la solicitada si es distinta. **La estimación no es un vencimiento** (A7 sigue `PENDIENTE`). El arrastre de las etapas siguientes lo hará el cronograma de la fase 5 a partir de esa fecha | Regla de REPORTAR_DEMORA |
| **F4-D6** | 4.3 | Plazo de Aduana (tránsito de 45 días de la calculadora): ¿hábiles, corridos o sin plazo? | ⏸ arquitectura 8.1 | Mantener `PENDIENTE` hasta respuesta, también con demora (F4-D5 aprobada: la estimación no se convierte en vencimiento) | Vencimiento de A7 |
| **F4-D7** | 4.4 | Documentos de Bodega ("documentos"): ¿cuáles? | ⏸ hoy `PENDIENTES` | Ninguna sin respuesta | **Avance A9 → A10** |
| **F4-D8** | 4.5 | Incidencias: ¿qué es "todos los documentos", es obligatoria siempre y qué plazo tiene? | ⏸ "TBD"; hoy `PENDIENTES` | Ninguna sin respuesta; plazo `PENDIENTE` | **Avance A10 → Fin** |
| **F4-D9** | 4.3–4.5 | ¿Se avisa al admin por correo la demora en aduana y el Fin? | ADR-0005 dice "cada avance de etapa y cada vencimiento"; la demora no es un avance | Avisar la demora (motivo visible) y el Fin; la bodega va dentro del aviso de A9 → A10 | Encolado de notificaciones |
| **F4-D10** | todos | Jornada hábil y feriados | ⏸ `CalendarioLaboral`: cualquier hora de lunes a viernes, sin feriados | Sin cambios en la fase 4 | 24 h hábiles de A8/A9 (ya calculables) |
| **F4-D11** | 4.6 | Reutilización del demo: `WarehouseCatalogView` y `GmailNotificationsView` | Manual 4.6: "Reutiliza componentes del demo con datos reales" | Reescribir sobre shadcn y la API real, conservando la estructura útil; retirar datos y permisos simulados | Pantallas |

**Consecuencia directa:** mientras F4-D4, F4-D7 y F4-D8 sigan sin respuesta, el motor seguirá rechazando A6 → A7, A9 → A10 y A10 → Fin con "Los documentos obligatorios de esta etapa todavía no están definidos" (S-2). Es el comportamiento correcto; no se inventan tipos de documento para desbloquearlo.

---

## UX Design

### Before
```
Bandeja de Compras China: solo Análisis (A4). Al aprobar, el pedido queda en A5 sin acciones.
Logística: sin bandeja con trabajo; no hay catálogo de bodegas ni elección.
```

### After
```
Compras China: bandeja con secciones A4 Análisis · A5 Fabricación · A6 Embarque · A7 Aduana
  A5: [Aprobar] con observación obligatoria
  A6: subir Packing List / BL (según F4-D4) → [Aprobar]
  A7: [Aprobar llegada] · [Reportar demora] (observación + nueva fecha estimada)
Logística: bandeja con A8 Salida · A9 Bodega · A10 Incidencias + menú "Bodegas"
  A9: [Aprobar] eligiendo una bodega activa (Select) y documentos (según F4-D7)
  A10: documentos (según F4-D8) → [Aprobar] → el pedido pasa a Fin solo
Detalle: bodega elegida, última demora (motivo, fecha estimada, autor) y Fin como estado final sin acciones.
```

### Interaction Changes
| Touchpoint | Before | After | Notes |
|---|---|---|---|
| Bandeja | A2–A4 | A2–A10 agrupadas por etapa | Mismo `BandejaPedidos`; el servidor decide qué entra (P-06) |
| Diálogo de acción | Observación, urgencia, archivos | + bodega (A9) y nueva fecha (demora A7) | `DialogoAccion` |
| Menú | Inicio, Mi bandeja, Pedidos, (admin) Usuarios/Roles | + "Bodegas" para Logística (y admin según F4-D1) | Lectura del catálogo para todos los que eligen bodega |
| Detalle | Bodega y demora siempre nulas | Se muestran si existen | `PedidoRespuestas` deja de fijar `null` |

---

## Mandatory Reading

| Priority | File | Lines | Why |
|---|---|---|---|
| P0 | `backend/src/main/resources/db/migration/V2__flujos_pedidos_historial.sql` | 335–404 | Siembra de A5–A11, transiciones y documentos `PENDIENTES` |
| P0 | `backend/src/main/java/com/decokasa/seguimiento/pedidos/domain/Pedido.java` | `ejecutar` (≈159–246), `exigirRequisitos` (≈410–440) | Dónde entran REPORTAR_DEMORA, bodega y Fin automático |
| P0 | `backend/.../pedidos/domain/EtapasHabilitadas.java` | todo | Habilitación por fase (`faseDos`) |
| P0 | `backend/.../pedidos/config/PedidosConfig.java` | bean `etapasHabilitadas` y `ejecutarAccionEtapa` | Composición y transacciones (ADR-0012) |
| P0 | `documentos/api/openapi.yaml` (0.7.3-borrador) | `/bodegas`, `AccionReportarDemora`, `AccionElegirBodega`, `AccionAprobar`, `DemoraAduana`, `Bodega`, `PedidoDetalle` | Contrato a ajustar |
| P1 | `backend/.../plazos/domain/CalendarioLaboral.java` | 1–40 | Horas y días hábiles; pendientes de jornada y feriados |
| P1 | `backend/.../compartido/domain/eventos/EventoDePedido.java` | `permits` | Eventos nuevos (demora, bodega, fin) |
| P1 | `backend/.../historial/domain/EventoPedido.java` | `desde` | Conversión de eventos al historial |
| P1 | `backend/.../notificaciones/application/usecase/EncolarNotificacionCasoDeUso.java` | todo | Aviso al admin por transición (fase 3) |
| P1 | `backend/.../pedidos/adapter/in/web/PedidoRespuestas.java` | `Detalle` (`bodega`, `demoraAduana` = null) | Respuesta del detalle |
| P1 | `documentos/modelo-datos.md` | §3.3 (columnas `bodega_id`, `demora_*`, S-3) y §3.7 (`bodega`) | Diseño de V9 |
| P1 | `documentos/arquitectura.md` | 8.1 (pendientes de fase 4) y tabla del motor del demo (`reportCustomsDelay`, `setOrderDestinationWarehouse`) | Puertos previstos (`RecalcularPorDemora`) |
| P2 | `frontend/src/components/pedidos/DialogoAccion.tsx`, `PanelEtapa.tsx`, `BandejaPedidos.tsx` | todo | Pantallas a extender |
| P2 | `frontend/src/components/WarehouseCatalogView.tsx` (demo) | todo | Referencia de la pantalla de bodegas |
| P2 | `.claude/PRPs/plans/fase-3-drive-gmail.plan.md` | decisiones F3-D* | Integraciones que se reutilizan |

## External Documentation
No external research needed — the phase uses established internal patterns (motor por datos, hexagonal, Flyway, TanStack Query, shadcn).

---

## Patterns to Mirror

### NAMING_CONVENTION
Paquetes por módulo con `domain`, `application.port.in|out`, `application.usecase`, `adapter.in.web`, `adapter.out.persistence`, `config`; casos de uso `XxxCasoDeUso implements Xxx` sin Spring (R2). Ejemplo: `pedidos/application/usecase/AgregarObservacionCasoDeUso.java`.

### ERROR_HANDLING
Errores de dominio de `compartido.domain` (`DatoInvalido`, `RequisitoFaltante`, `TransicionNoPermitida`, `EtapaCambio`, `SinPermiso`, `NoEncontrado`) traducidos por `ManejadorErrores` a `Problema` RFC 9457. `RequisitoFaltante` lleva `faltantes` (422).

### TRANSACTIONS
Beans en `config/` envuelven el caso de uso con `TransactionTemplate` (ADR-0012); las consultas usan `setReadOnly(true)`. Ejemplo: `PedidosConfig.agregarObservacion`.

### REPOSITORY_PATTERN
Puerto de salida en `application.port.out`; implementación JPA en `adapter.out.persistence` con entidades solo ahí (R3); bloqueo pesimista del pedido con `buscarParaActualizar`.

### EVENTS
Evento sellado en `compartido.domain.eventos`, publicado por `ApoyoPedidos.publicarEventos`; el historial lo inserta en la misma transacción (`OyenteEventosPedido`, MANDATORY) y notificaciones lo encola (`EncolarNotificacionCasoDeUso`).

### FRONTEND_QUERY
`api/pedidos.ts`: esquemas Zod del contrato, `clavesPedidos` por sesión, mutaciones sin reintento que actualizan detalle, historial, archivos, listas y bandeja (`useRefrescarPedido`).

### TEST_STRUCTURE
Solo referencia para Codex (parte B): `Backend24IT`, `pedidosFicticios.ts`, `AccionesPedido.test.tsx`.

---

## Files to Change

| File | Action | Justification |
|---|---|---|
| `backend/src/main/resources/db/migration/V9__bodegas_demora_fin.sql` | CREATE | Tabla `bodega` + carga Quito/Guayaquil/Ibarra; `pedido.bodega_id`, columnas `demora_*` con CHECK de demora completa; ajustes de siembra según decisiones aprobadas |
| `backend/.../bodegas/**` (domain, ports, use cases, JPA, web, config) | CREATE | Módulo `bodegas` (modelo-datos §3.7; arquitectura) |
| `backend/.../pedidos/domain/EtapasHabilitadas.java` | UPDATE | `faseCuatro()` con A1–A10 |
| `backend/.../pedidos/domain/Pedido.java` | UPDATE | REPORTAR_DEMORA sin mover etapa; bodega en A9; Fin automático al entrar en una etapa automática |
| `backend/.../pedidos/domain/AccionSolicitada.java`, `ReglaDeTransicion` | UPDATE | `bodegaId`, `nuevaFechaEstimadaLlegada` |
| `backend/.../compartido/domain/eventos/{DemoraReportada,BodegaAsignada,PedidoFinalizado}.java` | CREATE | Eventos nuevos (tipos ya previstos en `evento_pedido.tipo`) |
| `backend/.../historial/domain/EventoPedido.java`, `DetalleEvento` | UPDATE | Convertir los eventos nuevos (fecha de demora, bodega) |
| `backend/.../notificaciones/application/usecase/EncolarNotificacionCasoDeUso.java` | UPDATE | Según F4-D9 |
| `backend/.../pedidos/adapter/out/persistence/*` | UPDATE | Mapear `bodega_id` y `demora_*` |
| `backend/.../pedidos/application/usecase/{EjecutarAccionEtapa,ConsultarPedidos}CasoDeUso.java` | UPDATE | Validar bodega activa por puerto; vistas con bodega y demora |
| `backend/.../pedidos/adapter/in/web/{SolicitudAccion,PedidoRespuestas}.java` | UPDATE | Campos nuevos del contrato |
| `documentos/api/openapi.yaml` | UPDATE | 0.8.0-borrador: F4-D1, F4-D2, F4-D5, F4-D9 aprobadas |
| `documentos/modelo-datos.md`, `documentos/arquitectura.md` | UPDATE | Reflejar V9 y decisiones |
| `frontend/src/api/{pedidos,bodegas}.ts` | UPDATE/CREATE | Esquemas y hooks de bodegas, bodega y demora |
| `frontend/src/components/pedidos/{DialogoAccion,PanelEtapa,DetallePedido}.tsx` | UPDATE | Bodega, demora y Fin |
| `frontend/src/components/bodegas/CatalogoBodegas.tsx` | CREATE | Reemplaza `WarehouseCatalogView` con datos reales |
| `frontend/src/rutas.tsx`, `components/Sidebar.tsx` | UPDATE | Ruta y menú de Bodegas |

## NOT Building

- Cronograma completo contra la meta, panel de vencidos y alertas por vencimiento (fase 5; `/pedidos/{id}/cronograma`, `/pedidos/vencidos`).
- Flujo B y Colombia (fase 6).
- Calendario de feriados y jornada hábil (F4-D10, pendiente del cliente).
- Tipos de documento inventados para A6, A9 o A10 (F4-D4, F4-D7, F4-D8).
- Validación del contenido comercial de los documentos (ADR-0007).
- Pruebas, builds y verificación (parte B, Codex).

---

## Step-by-Step Tasks

### Paso 4.1 — Datos de bodegas (skill: `ecc:database-migrations`)

#### Task 1: Migración V9
- **ACTION**: crear `V9__bodegas_demora_fin.sql` (siguiente versión disponible tras V8).
- **IMPLEMENT**:
  - `bodega` (modelo-datos §3.7): `id uuid PK`, `nombre text NOT NULL` (1–100), `ciudad text NOT NULL` (1–100), `activa boolean NOT NULL DEFAULT true`, `creada_en`, `actualizada_en`, `version bigint`; índice único `lower(nombre)`; sin borrado físico (trigger que impide DELETE/TRUNCATE).
  - Carga inicial con identificadores fijos: Quito, Guayaquil e Ibarra (nombre = ciudad), activas.
  - `pedido`: `bodega_id uuid NULL FK → bodega`, `demora_motivo text`, `demora_nueva_fecha_llegada date`, `demora_reportada_por uuid FK → usuario`, `demora_reportada_en timestamptz`; CHECK de demora completa (las cuatro nulas o las cuatro presentes; modelo-datos §3.3).
  - Solo si F4-D4/F4-D7/F4-D8 se aprueban: `documento_requerido` de las transiciones 209, 213 y 214 y `requisitos_documentales = 'DEFINIDOS'` (UPDATE en V9; no se edita V2, que ya está aplicada).
- **MIRROR**: V3/V4 (comentarios en español, CHECK con nombre, triggers de solo inserción).
- **GOTCHA**: V1–V8 ya están aplicadas: no se modifican. No asignar bodega a pedidos existentes.
- **VALIDATE**: parte B.

#### Task 2: Módulo `bodegas`
- **ACTION**: crear el módulo hexagonal.
- **IMPLEMENT**: dominio `Bodega` (nombre/ciudad recortados y validados, `activa`, `version`); puertos de entrada `ListarBodegas` (cualquier usuario vigente; activas por defecto), `CrearBodega`, `ActualizarBodega` (con `version`, 409 `VERSION_DESACTUALIZADA` como en usuarios) y `ConsultarBodegaActiva` (para pedidos); permisos con el estado vigente en la base (Logística; admin según **F4-D1**); controlador `/api/v1/bodegas` según el contrato; 409 `DUPLICADO` por nombre.
- **MIRROR**: `usuarios` (versión D10, `RestriccionesUnicasUsuario`, `Autorizacion`).
- **GOTCHA**: R9: `bodegas` no depende de `pedidos`; pedidos usa `ConsultarBodegaActiva` (puerto de entrada).
- **VALIDATE**: parte B.

### Paso 4.2 — Fabricación y embarque (`/ecc:prp-implement`)

#### Task 3: Habilitar A5 y A6
- **ACTION**: `EtapasHabilitadas.faseCuatro()` con A1–A10 y uso en `PedidosConfig`.
- **IMPLEMENT**: A5 APROBAR con observación obligatoria (ya sembrada); plazo `PENDIENTE` (**F4-D3**: sin vencimiento). A6 APROBAR con 10 días hábiles y documentos según **F4-D4**; mientras sigan `PENDIENTES`, el motor rechaza el avance (S-2).
- **MIRROR**: reglas de A1–A4 en `Pedido.ejecutar`.
- **GOTCHA**: si F4-D4 resulta "Packing List **o** BL", hace falta una regla de alternativas en `documento_requerido`/`TransicionPermitida` y en `accionesDisponibles.documentosFaltantes`; se registra en un ADR antes de implementarla.
- **VALIDATE**: parte B.

### Paso 4.3 — Aduana y cronograma (`/ecc:prp-implement`)

#### Task 4: Llegada y demora
- **ACTION**: implementar REPORTAR_DEMORA en `Pedido` y el caso de uso.
- **IMPLEMENT**: la acción no mueve la etapa (destino nulo): exige observación y `nuevaFechaEstimadaLlegada`; según **F4-D5** (aprobada): fin de semana → lunes, luego no anterior a hoy en Ecuador y posterior a la estimación vigente; guarda `demora_*` con autor y hora del servidor **sin cambiar el vencimiento de A7**; emite `DemoraReportada` (tipo `DEMORA_REPORTADA` ya previsto). APROBAR en A7 → A8 registra la llegada.
- **MIRROR**: `Pedido.agregarObservacion` (acción que no mueve la etapa, con autor y hora del servidor).
- **GOTCHA**: el "arrastre" de las etapas siguientes necesita fechas planificadas que hoy no existen; queda para el cronograma (fase 5) salvo decisión distinta en F4-D5. Plazo de A7 según **F4-D6**.
- **VALIDATE**: parte B.

### Paso 4.4 — Salida y bodega (`/ecc:prp-implement`)

#### Task 5: A8 y A9 con bodega
- **ACTION**: A8 APROBAR (24 h hábiles, Logística) y A9 con bodega.
- **IMPLEMENT**: según **F4-D2**, APROBAR en A9 exige `bodegaId` de una bodega **activa** (vía `ConsultarBodegaActiva`; si falta, 422 `REQUISITO_FALTANTE` con "bodega"; si no existe o está inactiva, 400 en `bodegaId`); la asignación se guarda una sola vez al confirmar la acción, en la misma transacción; evento `BodegaAsignada` (y `EtapaAprobada`). Documentos de A9 según **F4-D7**.
- **MIRROR**: validación de `adjuntoIds` antes de la acción en `EjecutarAccionEtapaCasoDeUso`.
- **GOTCHA**: reemplaza el `faltantes.add("bodega")` provisional de `exigirRequisitos`.
- **VALIDATE**: parte B.

### Paso 4.5 — Incidencias y Fin (`/ecc:prp-implement`)

#### Task 6: A10 y Fin automático
- **ACTION**: al entrar en una etapa `automatica` (A11), el pedido se finaliza en la misma transacción.
- **IMPLEMENT**: `finalizado = true`, `finalizadoEn = ahora`, A11 COMPLETADA, ninguna etapa ACTUAL (respeta la invariante "una sola ACTUAL mientras no está finalizado"); evento de sistema `PedidoFinalizado` (usuario nulo, tipo `PEDIDO_FINALIZADO`); cualquier acción sobre un pedido finalizado ya responde `TransicionNoPermitida`. Documentos y plazo de A10 según **F4-D8**.
- **MIRROR**: invariantes de `Pedido.verificarUnaSolaActual`.
- **GOTCHA**: no hay acciones manuales sobre Fin; `accionesDisponibles` vacío.
- **VALIDATE**: parte B.

#### Task 7: Eventos, historial, notificaciones y contrato
- **ACTION**: propagar los eventos nuevos y actualizar el contrato.
- **IMPLEMENT**: eventos nuevos en `compartido.domain.eventos` (permits), conversión en `EventoPedido.desde` y `DetalleEvento` (fecha estimada y bodega), encolado según **F4-D9**; `PedidoRespuestas` con `bodega` y `demoraAduana` reales; OpenAPI 0.8.0-borrador con las decisiones aprobadas (cambios no compatibles anotados como tales).
- **MIRROR**: `ArchivoSubido` y `ObservacionAgregada` (fase 2).
- **GOTCHA**: R4/R9: el historial y las notificaciones solo ven tipos de `compartido`.
- **VALIDATE**: parte B.

### Paso 4.6 — Pantallas de responsables (skills: `shadcn`, `tanstack-query-best-practices`, `impeccable`)

#### Task 8: Datos y diálogos
- **ACTION**: extender datos y diálogos del frontend.
- **IMPLEMENT**: `api/bodegas.ts` (lista, alta, edición con versión); en `api/pedidos.ts`, esquemas de `bodega` y `demoraAduana` y campos nuevos de la acción; `DialogoAccion` con Select de bodegas activas (A9) y campo de fecha + observación (demora A7), sin avanzar nada antes de la respuesta.
- **MIRROR**: FRONTEND_QUERY.
- **GOTCHA**: la etapa esperada sigue fijándose al abrir el diálogo.
- **VALIDATE**: parte B.

#### Task 9: Bandejas, detalle y catálogo
- **ACTION**: conectar bandejas, detalle y catálogo.
- **IMPLEMENT**: la bandeja ya agrupa por etapa: mostrará A5–A7 de Compras China y A8–A10 de Logística sin cambios de lógica; detalle con bodega, última demora y Fin sin acciones; `CatalogoBodegas` (ruta `/bodegas`, menú para Logística y admin según F4-D1) reutilizando la estructura de `WarehouseCatalogView` con datos reales; `/impeccable polish` con DESIGN.md.
- **MIRROR**: `components/pedidos/*` (shadcn y tokens de DESIGN.md).
- **GOTCHA**: retirar datos y permisos simulados del demo (`canManage` por `rol`).
- **VALIDATE**: parte B.

#### Resultado del paso 4.1 (2026-10-19). Sin pruebas, builds ni migraciones ejecutadas (parte B, Codex). No está verificado

- **Rama:** `fase-4-etapas-a5-a11` creada desde `fase-3-drive-gmail` (`977301a`) sin conflictos. `Header.tsx` (modificado) y los archivos sin seguimiento siguen intactos, sin commit ni stash. Fase 3 y 3.B3 siguen abiertos.
- **`V9__bodegas_llegada.sql`** (V9 comprobada libre en todas las ramas locales y remotas; V1–V8 sin cambios):
  - `bodega`: nombre y ciudad de 1 a 100 caracteres sin espacios sobrantes, `activa`, `creada_en`, `actualizada_en`, `version`; índice único `lower(nombre)`; sin borrado físico (trigger); carga inicial Quito, Guayaquil e Ibarra (activas, identificadores fijos `…0401`–`…0403`).
  - `pedido.bodega_id` (FK sin cascada; una bodega usada no se borra) con índice parcial, y trigger `pedido_bodega_activa`: una asignación nueva exige bodega activa, con `FOR SHARE` para no cruzarse con una desactivación simultánea; los pedidos ya asignados no cambian.
  - `pedido.llegada_aduana_en`: momento del check de A7, hora del servidor, `>= creado_en`.
  - **No incluye**, por seguir pendientes: columnas de demora (F4-D5), plazos de A5, A7 y A10 (siguen `PENDIENTE`), documentos de A6, A9 y A10 (siguen `PENDIENTES`) ni la forma de elegir la bodega (F4-D2).
- **Contrato:** `/bodegas` con `x-roles: [logistica, admin]` y la decisión F4-D1 en lugar del `x-pendiente`; `nombre` y `ciudad` con `maxLength: 100`, igual que la base.
- **Modelo de datos:** §3.3 (`bodega_id`, `llegada_aduana_en`, demora pendiente) y §3.7 (`bodega`).
- **Para Codex (parte B):** `MigracionesIT` y otras pruebas que fijan la versión final o el conjunto de tablas deberán contemplar V9 (sin quitar comprobaciones).

#### Resultado de la tarea 2 (2026-10-19): módulo `bodegas` y API `/api/v1/bodegas`. Sin pruebas, builds ni migraciones ejecutadas (parte B, Codex). No está verificado

- **Módulo hexagonal `bodegas`** (`backend/src/main/java/com/decokasa/seguimiento/bodegas/`, 16 archivos):
  - `domain/Bodega`: recorta espacios y exige 1–100 caracteres en `nombre` y `ciudad` (igual que V9 y el contrato); `nueva` (activa por defecto, versión 0) y `cambiar` (solo los campos enviados). Java puro.
  - Puertos de entrada: `ConsultarBodegas` (lista, filtro opcional `activa`, por nombre), `CrearBodega`, `ActualizarBodega` y `ConsultarBodegaActiva` (para pedidos en A9; **todavía sin uso**). Puerto de salida `BodegaRepositorio`.
  - Casos de uso sin Spring; `MantenimientoAutorizado` aplica **F4-D1** con el estado vigente en la base mediante `usuarios.ConsultarResponsables` (cuenta activa con roles y `logistica` o `admin`); los roles del JWT no deciden (lección A1).
  - Edición con la pauta D10: bloqueo y relectura de la fila (`FOR UPDATE`), comparación de `version` antes de cambiar nada → 409 `VERSION_DESACTUALIZADA`; versión incrementada por `@Version`; red de seguridad ante bloqueo optimista.
  - Nombres repetidos: comprobación previa sin distinguir mayúsculas y traducción de la violación del índice `bodega_nombre_unico` (23505) a 409 `DUPLICADO` con `campo = nombre`; cualquier otro error de integridad sigue su curso.
  - Sin borrado: la API no tiene DELETE; desactivar es `PATCH` con `activa: false`. Los pedidos que ya tienen la bodega y su historial no cambian (trigger de V9).
  - `BodegaController`: `GET /api/v1/bodegas[?activa=]` (cualquier usuario autenticado, como dice el contrato: no se amplió ni se restringió), `POST` (201 con `Location`) y `PATCH /{bodegaId}` (exige `version` entera ≥ 0 y al menos un campo; si no, 400).
  - `BodegasConfig`: transacciones con `TransactionTemplate` (ADR-0012), lectura en solo lectura. Dependencias (R9): bodegas → usuarios (puerto de entrada) y compartido; no depende de pedidos.
- **Contrato 0.8.0-borrador:** `Bodega.version` (obligatoria en la respuesta), `SolicitudActualizarBodega.version` obligatoria (**cambio no compatible**, anotado), 409 en el `PATCH` (versión y duplicado), descripciones de `GET` y `POST`.
- **No incluido (fuera de la tarea 2):** elección de bodega en pedidos (F4-D2), demora, transiciones A5–A11 y pantallas.
- **Para verificar por Codex (parte B):**
  - permisos: Logística y admin vigentes crean y editan (201/200); otro rol, cuenta desactivada o sin roles con un JWT aún válido → 403; sin token → 401; `GET` con cualquier usuario autenticado;
  - duplicados: nombre repetido con otras mayúsculas o espacios → 409 `DUPLICADO` (`nombre`), también con dos altas o ediciones simultáneas (sin 500);
  - versión: `PATCH` con versión vieja → 409 sin cambios; dos ediciones simultáneas con la misma versión → una 200 y otra 409; `version` ausente, nula, texto, decimal o negativa → 400; cuerpo solo con `version` → 400;
  - validación: nombre o ciudad vacíos, solo espacios o de más de 100 caracteres → 400; espacios sobrantes recortados;
  - desactivar una bodega asignada no cambia el pedido; una bodega inactiva no se puede asignar (trigger de V9, por SQL hasta que exista la acción de A9); DELETE/TRUNCATE de `bodega` rechazados;
  - carga inicial de V9: Quito, Guayaquil e Ibarra activas;
  - ArchUnit (R1–R4, R9) con el módulo nuevo.

#### Resultado del paso 4.2 (2026-10-19): Fabricación y Embarque. Sin pruebas, builds ni migraciones ejecutadas (parte B, Codex). No está verificado

- **Cambio en el motor:** `EtapasHabilitadas.faseCuatroHastaEmbarque()` (A1–A6) y `PedidosConfig.etapasHabilitadas` la usa. `faseDos()` se conserva. Aduana (A7) y lo que sigue **no** se habilitan.
- **Se reutiliza sin cambios** lo que ya estaba en datos y en el motor:
  - A5 → A6: transición `…0208` de V2 (`APROBAR`, `requiere_observacion = true`); el permiso es el de P-06 con el rol `compras_china` (`PermisosEtapa`, estado y asignación vigentes); el evento `EtapaAprobada`, el historial, el resumen P-20 y el aviso al admin (fase 3) funcionan igual que en A1–A4.
  - Plazo de A5: `PENDIENTE` en V2 (**F4-D3**): `CalcularVencimiento.deEtapa` no devuelve vencimiento. No se fija ningún valor.
  - Plazo de A6: `DEFINIDO 10 DIAS_HABILES` en V2; `CalendarioLaboral` suma días de lunes a viernes con la hora de Ecuador y pasa al lunes si cae en fin de semana (jornada y feriados siguen pendientes, F4-D10).
  - Subida de archivos y observaciones en A5 y A6: misma habilitación (P-05, P-18, P-19).
- **F4-D4 sigue sin respuesta aprobada:** la transición A6 → A7 (`…0209`) mantiene `requisitos_documentales = 'PENDIENTES'`. El motor rechaza el avance con 409 `TRANSICION_NO_PERMITIDA` ("Los documentos obligatorios de esta etapa todavía no están definidos") y `accionesDisponibles` lo muestra con `requisitosDocumentales = PENDIENTES` y `puedeCompletarse = false`. Pregunta abierta para el cliente: **"¿Packing List y BL, o basta uno de los dos?"**. No se eligió ninguna opción.
- **Frontend:** sin cambios; las pantallas ya muestran las acciones que da el servidor, exigen la observación cuando `requiereObservacion` y explican los documentos pendientes.
- **Casos para Codex (parte B):**
  - A4 → A5 → A6 por Compras China con observación; A5 APROBAR sin observación → 422 `REQUISITO_FALTANTE` con `faltantes: ["observación"]` y sin cambios;
  - permisos P-06 en A5 y A6: otro rol, asignación de otra persona, cuenta desactivada o sin roles → 403; admin vigente puede actuar; `consulta` + `compras_china` conserva el permiso;
  - A5 sin `venceEn` (plazo `PENDIENTE`, no `SIN_PLAZO`) y sin vencimiento en la bandeja; A6 con `venceEn` = entrada + 10 días hábiles (entrada en viernes, en sábado y en domingo; hora de Ecuador);
  - A6 APROBAR → 409 `TRANSICION_NO_PERMITIDA` sin avanzar; `accionesDisponibles` con `PENDIENTES` y `puedeCompletarse = false`; subir archivos y observar en A6 sí se permite;
  - A7 sigue sin acciones habilitadas; historial, resumen P-20 y aviso al admin en A5 → A6;
  - `EtapasHabilitadas.faseDos()` sigue disponible para las pruebas existentes.

#### Resultado del paso 4.3 (2026-10-19): demora y llegada en aduana. Sin pruebas, builds ni migraciones ejecutadas (parte B, Codex). No está verificado

- **F4-D5** aplicada como decisión provisional del equipo, pendiente de confirmación del cliente. **F4-D4 sigue pendiente**: A6 → A7 sigue bloqueada por documentos (S-2), así que ningún pedido real llega a A7 todavía; no se omitió el bloqueo.
- **Migración `V10__demora_aduana.sql`** (siguiente versión libre; V1–V9 sin cambios): `pedido.demora_motivo`, `demora_nueva_fecha_llegada` (`date`), `demora_reportada_por` (FK a `usuario`, sin cascada, índice parcial) y `demora_reportada_en`. CHECK: las cuatro juntas (`pedido_demora_completa`), motivo de 1 a 4000 caracteres, reportada después de creado, fecha de lunes a viernes (`pedido_demora_fecha_habil`) y no anterior al día del reporte en Ecuador (`pedido_demora_fecha_no_pasada`). Sin datos.
- **Dominio (`Pedido`)**: REPORTAR_DEMORA (destino nulo en V2) ya no responde "no implementada":
  - permisos P-06 iguales a cualquier acción (Compras China con asignación vigente o el admin; estado y roles vigentes);
  - 422 `REQUISITO_FALTANTE` con `faltantes` `observación` y/o `fecha estimada de llegada`;
  - la fecha solicitada pasa al lunes si cae en sábado o domingo; después, 400 en `nuevaFechaEstimadaLlegada` si la fecha efectiva es anterior a hoy en Ecuador o, con una demora previa, igual o anterior a la estimación vigente;
  - no cambia etapa, entrada, plazo ni `venceEn`; guarda la demora vigente (`DemoraAduana`) y emite `DemoraReportada` (nuevo evento en `compartido.domain.eventos`);
  - `nuevaFechaEstimadaLlegada` con cualquier otra acción → 400;
  - APROBAR en A7 → A8 guarda `llegadaAduanaEn` (columna de V9) con la hora del servidor, en la misma transacción que la acción y su evento. La etapa de llegada es la marcada con `definicion_etapa.registra_llegada` (**ADR-0018**, V11; ver más abajo), no la que admite REPORTAR_DEMORA ni un número fijo en el código.
- **Atomicidad**: igual que las demás acciones: la fila del pedido se bloquea antes de leerla, y pedido y evento del historial se guardan en la misma transacción. El historial sigue siendo de solo inserción.
- **Historial**: `DEMORA_REPORTADA` con `observacion` = motivo y en `datos`: `fechaEstimadaAnterior` (si había), `fechaEstimadaSolicitada` (solo si es distinta de la guardada) y `nuevaFechaEstimadaLlegada`. `DetalleEvento` del contrato y de las respuestas tiene los tres campos. El resumen P-20 no cambia (una demora no cierra la etapa) y no hay aviso por correo (no mueve el pedido).
- **Habilitación**: `EtapasHabilitadas.faseCuatroHastaAduana()` (A1–A7). Las acciones de A8 en adelante siguen sin habilitar.
- **API**: `SolicitudAccion.nuevaFechaEstimadaLlegada` (fecha ISO); `PedidoDetalle.demoraAduana` (antes siempre nula) y `PedidoDetalle.llegadaAduanaEn` (nuevo). En `accionesDisponibles`, REPORTAR_DEMORA ya puede completarse. Contrato **0.9.0-borrador** (cambios compatibles).
- **Para el cronograma de la fase 5 (no implementado):** el punto de partida será `pedido.demora_nueva_fecha_llegada` (estimación vigente) y, cuando exista, `pedido.llegada_aduana_en` (llegada real). En la fase 4 no se guardan fechas planificadas ni se recalculan etapas siguientes.
- **Compatibilidad**: se conservaron los constructores anteriores (`Pedido` de 17 argumentos, `AccionSolicitada`, `ReglaDeTransicion`, `EjecutarAccionEtapa.Comando`, `DetalleEvento`) y el `ejecutar` de 6 argumentos para no tocar las pruebas existentes.
- **Pendiente para el paso 4.6 (frontend):** el diálogo de demora con su campo de fecha (`AAAA-MM-DD`) y la presentación de `demoraAduana` y `llegadaAduanaEn`; hoy la pantalla ofrece "Reportar demora" sin campo de fecha y el servidor respondería 422. Feriados (F4-D10) y plazo de Aduana (F4-D6) siguen pendientes.
- **Casos para Codex (parte B):**
  - V10 sobre una base con V1–V9 y pedidos existentes (todas las columnas nulas); `MigracionesIT` y pruebas que fijan la versión final deberán contemplar V10; cada CHECK rechaza su caso (demora incompleta, sábado/domingo, fecha anterior al día del reporte en Ecuador, motivo vacío);
  - para llegar a A7 sin saltarse F4-D4 en el producto, preparar el pedido en A7 con datos de prueba (SQL o fixture), sin cambiar V2;
  - REPORTAR_DEMORA por Compras China (asignado o sin asignación) y por el admin; 403 para otro rol, asignación de otra persona, cuenta desactivada o sin roles;
  - sin observación, sin fecha o sin ambas → 422 con los `faltantes` correspondientes, sin cambios ni evento;
  - fecha en sábado → lunes (+2) y en domingo → lunes (+1), con `fechaEstimadaSolicitada` en el evento; fecha en día hábil → sin `fechaEstimadaSolicitada`;
  - fecha efectiva anterior a hoy en Ecuador → 400 (probar cerca de la medianoche: 23:30 en Ecuador ya es el día siguiente en UTC); fecha de hoy → aceptada; un sábado pasado que al ajustarse cae en un lunes de hoy o futuro → aceptado;
  - segunda demora igual o anterior a la vigente → 400; posterior → 200, con `fechaEstimadaAnterior` en el evento y la demora del pedido reemplazada;
  - la demora no cambia `etapaActual`, `entradaId`, `venceEn` (nulo, plazo `PENDIENTE`) ni `vencida`;
  - `nuevaFechaEstimadaLlegada` con APROBAR u otra acción → 400; fecha con formato inválido → 400;
  - APROBAR en A7 → A8 guarda `llegadaAduanaEn` (hora del servidor) y la entrega en el detalle; A8 sin acciones habilitadas (`accionesDisponibles` vacío);
  - historial: evento `DEMORA_REPORTADA` con actor, rol, hora del servidor, motivo, adjuntos y `datos`; inmutable;
  - concurrencia: dos demoras simultáneas sobre el mismo pedido se serializan (la segunda ve la estimación de la primera); demora y APROBAR simultáneos → uno gana y el otro recibe 409 `ETAPA_CAMBIO` o se valida sobre el estado nuevo;
  - las pruebas existentes de las fases 2 y 3 siguen pasando (constructores secundarios); ArchUnit R1–R4 y R9.
  - **validación estricta de la fecha** (`SolicitudAccion`, `@JsonFormat(pattern = "uuuu-MM-dd", lenient = FALSE)`; contrato 0.9.1): `"2026-10-20"` → aceptada; `"2026-10-20T10:00:00"`, `"2026-10-20Z"`, `"20/10/2026"`, `"2026-2-3"`, `"2026-02-30"`, `20261020`, `[2026,10,20]`, `true` y texto basura → 400 sin cambios ni evento. Confirmar también que el 400 sale en formato `Problema` y no 500;
  - solo la etapa que admite REPORTAR_DEMORA (A7) registra `llegadaAduanaEn`; APROBAR en otras etapas no la toca.
- **Revisión `ecc:java-reviewer` (lectura estática):** sin hallazgos críticos ni altos. Corregido: REPORTAR_DEMORA por la sobrecarga sin "hoy" lanza un `IllegalStateException` explícito en lugar de un `NullPointerException`. (1) Resuelto con ADR-0018 (abajo). Anotados: (2) si A7 → A8 ocurriera más de una vez, `llegadaAduanaEn` guarda la última (el historial conserva todas); (3) la demora vigente se conserva tras salir de A7.

#### Etapa de llegada explícita (2026-10-19, ADR-0018, decisión del equipo). Sin pruebas, builds ni migraciones ejecutadas. No está verificado

- **ADR-0018** (`documentos/adr/0018-etapa-de-llegada-explicita.md`, accepted (equipo); índice actualizado): la etapa de llegada se marca en la definición versionada del flujo. No se deduce de REPORTAR_DEMORA ni se fija el número en el código.
- **Migración `V11__etapa_de_llegada.sql`** (siguiente libre; V1–V10 sin cambios): `definicion_etapa.registra_llegada boolean NOT NULL DEFAULT false`, `CHECK` `definicion_etapa_llegada_no_automatica`, índice único parcial `definicion_etapa_llegada_unica (flujo_id) WHERE registra_llegada` y marca solo A7 del flujo A EC v1 (id `…0107`). Como V3 con `revisa_etapa`, es un atributo nuevo de la versión 1; ningún pedido llegó a A7. El flujo B no está en los datos.
- **Código:** `DefinicionEtapa.registraLlegada` (con validación: no automática; como máximo una por flujo en `DefinicionFlujo`), entidad y repositorio de flujos, `ContratoDeEtapa.registraLlegada()` (por defecto falso), `ResolverTransicion.Resolucion.origenRegistraLlegada`, `DescribirFlujo.Etapa` y `GET /flujos` (`registraLlegada`). `EjecutarAccionEtapaCasoDeUso` usa la marca y se quitó `admiteDemora`. Se conservaron los constructores anteriores de `DefinicionEtapa` (5 y 6 argumentos) y de `Resolucion` (11) para las pruebas existentes.
- **Contrato 0.9.1-borrador:** `DefinicionEtapa.registraLlegada` (obligatorio en la respuesta), nota de la acción y formato estricto de `nuevaFechaEstimadaLlegada`.
- **Casos para Codex (parte B):**
  - V11 sobre V1–V10: solo A7 del flujo A v1 con `registra_llegada = true`; el resto, falso; `MigracionesIT` y las pruebas que fijan la versión final deben contemplar V10 y V11;
  - la base rechaza marcar una etapa automática (23514) y una segunda etapa de llegada en el mismo flujo (23505);
  - `DefinicionFlujo` rechaza dos etapas de llegada; `DefinicionEtapa` rechaza una automática marcada;
  - `GET /flujos/EC/A` devuelve `registraLlegada: true` solo en la etapa 7;
  - APROBAR en A7 guarda `llegadaAduanaEn` con la hora del servidor en la misma transacción (si la acción falla, no queda llegada); APROBAR en otras etapas no la toca; REPORTAR_DEMORA no registra llegada;
  - una definición de prueba con REPORTAR_DEMORA en otra etapa sin la marca no registra llegada; una con la marca en otra etapa sí.

#### Resultado del paso 4.4 (2026-10-19): Autorización de salida y Bodega. Sin pruebas, builds ni migraciones ejecutadas (parte B, Codex). No está verificado

- **F4-D2** aplicada como decisión provisional del equipo (pendiente de confirmación del cliente). **F4-D7 sigue pendiente**: A9 → A10 sigue bloqueada por documentos (S-2) y, por eso, ninguna bodega se guarda todavía. **F4-D4** sigue bloqueando A6 → A7.
- **Habilitación**: `EtapasHabilitadas.faseCuatroHastaBodega()` (A1–A9). A10 en adelante sin habilitar.
- **A8 Autorización de salida**: sin cambios de datos; V2 ya tiene 24 horas hábiles y responsable `logistica`; APROBAR → A9 con plazo de 24 horas hábiles. Permisos P-06 como en el resto.
- **A9 Bodega (F4-D2)**:
  - la bodega se elige solo con el APROBAR de A9 (`bodegaId`); `ELEGIR_BODEGA` no se implementa (sigue sin transición en los datos) y el contrato la marca como no usada;
  - comprobaciones: permiso P-06; si los documentos de A9 están definidos, la bodega debe existir y estar activa (`ConsultarBodegaActiva`, 400 en `bodegaId`); documentos pendientes → 409 sin guardar nada; `bodegaId` con otra acción → 400; pedido con bodega ya asignada → 409 (no se cambia); sin `bodegaId` → 422 con `faltantes: [bodega]`;
  - bodega, avance a A10 y eventos `BODEGA_ASIGNADA` y `ETAPA_APROBADA` se guardan en la misma transacción, con la fila del pedido bloqueada; un doble envío recibe 409 `ETAPA_CAMBIO`;
  - desactivación simultánea: el trigger de V9 (`FOR SHARE`) es la última barrera; **V12** le da el SQLSTATE propio `UB001` y `PedidoJpaRepositorio` traduce solo ese código a 400 en `bodegaId` (los demás errores SQL siguen su curso). V12 también impide en la base cambiar o quitar una bodega asignada (`UB002`, sin traducir: la aplicación no lo intenta);
  - permisos: P-06 sin cambios, con los roles adicionales de quien tiene varios; `consulta` y `asistente_china` no conceden acciones por sí mismos.
- **Detalle y respuestas**: `PedidoDetalle.bodega` (vía el nuevo puerto `bodegas.DescribirBodega`, también si la bodega se desactivó después), `AccionDisponible.requiereBodega` y `DetalleEvento.bodegaId`. `puedeCompletarse` ya no excluye las acciones con bodega. Contrato **0.10.0-borrador**.
- **Compatibilidad**: se conservaron los constructores anteriores de `AccionSolicitada`, `EjecutarAccionEtapa.Comando`, `DetalleEvento` y `EjecutarAccionEtapaCasoDeUso` (sin bodegas: cualquier `bodegaId` → 400).
- **Pendiente para el paso 4.6 (frontend):** el selector de bodega activa en el diálogo de aprobación de A9 y mostrar la bodega del pedido.
- **Casos para Codex (parte B):**
  - V12 sobre V1–V11: misma regla que V9 con `UB001`; `UB002` al cambiar o quitar una bodega asignada; asignar por primera vez una activa sigue funcionando;
  - A8 APROBAR por Logística (asignado o sin asignación) y por el admin → A9 con `venceEn` = entrada + 24 horas hábiles; 403 para otro rol, asignación de otra persona, cuenta desactivada o sin roles; `consulta` + `logistica` sí actúa; `consulta` o `asistente_china` solos no;
  - A9 APROBAR con datos actuales (documentos `PENDIENTES`) → 409, sin bodega, sin avance ni eventos, con o sin `bodegaId`;
  - con una definición de prueba donde A9 tenga documentos `DEFINIDOS` (sin cambiar V2): sin `bodegaId` → 422 `faltantes: [bodega]`; bodega inexistente o inactiva → 400 `bodegaId`; bodega activa → 200, pedido en A10, `bodega` en el detalle, eventos `BODEGA_ASIGNADA` (con `bodegaId`) y `ETAPA_APROBADA` en la misma transacción;
  - concurrencia: desactivar la bodega mientras se confirma → si la desactivación gana, 400 `bodegaId` sin cambios; si gana la asignación, la desactivación espera y el pedido conserva la bodega; doble envío → una 200 y otra 409 `ETAPA_CAMBIO`, con una sola asignación y un solo `BODEGA_ASIGNADA`;
  - `bodegaId` con ENVIAR, APROBAR de otra etapa, REPORTAR_DEMORA o cualquier otra acción → 400; `ELEGIR_BODEGA` → 409 `TRANSICION_NO_PERMITIDA`;
  - el detalle muestra la bodega aunque después se desactive; `accionesDisponibles` de A9 con `requiereBodega = true`, `requisitosDocumentales = PENDIENTES` y `puedeCompletarse = false`;
  - orden en el historial: `BODEGA_ASIGNADA` y `ETAPA_APROBADA` tienen la misma hora; el orden entre ambos depende del identificador (aceptable; revisar si la pantalla lo necesita);
  - ArchUnit R1–R4, R9 y `FronterasFaseDosTest` (pedidos usa `bodegas.application.port.in`, permitido); las pruebas existentes siguen pasando.
  - `bodegaId` con UUID mal formado → 400.
- **Revisión `ecc:java-reviewer` (lectura estática):** sin errores de compilación ni de fronteras. Corregidos: SQLSTATE en la clase propia `UB` (`UB001`, `UB002`) en lugar de `DK`, que pertenece a una clase reservada; y la bodega no se consulta si el pedido ya tiene una (responde 409 "una sola vez" y no 400). Anotados: orden no determinista entre `BODEGA_ASIGNADA` y `ETAPA_APROBADA` en el historial (misma hora, desempate por UUID aleatorio; opciones: columna de orden, UUID v7 o aceptarlo y documentarlo); con documentos de A9 definidos, una bodega inactiva (400) se informa antes que documentos faltantes (422).
- **Conflicto con una prueba existente (no se modificó):** `Backend24IT.rechazosDevolucionPorErrorYA4A5MantienenContrato` (línea ~153) espera 409 al aprobar A5 "fuera de fase", pero desde el paso 4.2 A5 está habilitada (decisión aprobada) y responde 200. Codex debe actualizar esa expectativa en la parte B; el código sigue la decisión vigente.

---

## Entrega del paso 4.5 — Incidencias y Fin automático (2026-10-19)

**Implementación de parte A, pendiente de verificación por Codex.** Se leyó el manual vigente y se aplicaron `ecc:prp-implement` y `ecc:java-reviewer` mediante revisión estática. Por instrucción expresa no se modificaron pruebas ni se ejecutaron pruebas, compilación, builds o migraciones. No se hizo sincronización remota ni comandos Git de escritura. Los 761 casos aprobados y la cobertura de la revalidación de F4-P01 corresponden al código anterior a 4.5; no prueban esta entrega. La fase permanece abierta.

### Implementado

- **A10 habilitada, A11 manual excluida.** `EtapasHabilitadas.faseCuatroHastaIncidencias()` incluye A1–A10 y se utiliza desde `PedidosConfig`. Se conservan los métodos de fases anteriores. Los permisos de A10 siguen P-06: Logística o admin vigente, respetando asignación y estado actual de la cuenta.
- **Bloqueos sin cambios.** No se alteran V1–V12 ni la definición vigente. `APROBAR` de A10 conserva documentos `PENDIENTES` (F4-D8) y responde 409 antes de mutar el agregado; no equivale a una lista vacía. Su plazo sigue `PENDIENTE`, sin vencimiento inventado. A6 y A9 conservan los bloqueos F4-D4/F4-D7. Por tanto, hoy no se puede cerrar un pedido real pasando por esos requisitos sin resolver.
- **Destino automático leído de datos.** `ResolverTransicion.Resolucion.destinoEsAutomatico` se obtiene de la etapa destino de la versión del flujo y se lleva a `ReglaDeTransicion`; no se fija el número 11 en la regla de cierre. Se preservan los constructores anteriores de ambos registros, con el nuevo indicador en `false` para los llamadores previos. Una regla de cierre automático exige destino que avance.
- **Confirmación y cierre.** Cuando la definición tenga documentos aprobados y el caso de uso valide los adjuntos reales de la entrada actual, A10 se completa y la entrada a la etapa automática completa también Fin en la misma acción. El agregado guarda `finalizado=true`, `finalizadoEn=ahora`, `etapaActual=Fin`, Fin `COMPLETADA`, entrada y salida en ese mismo instante del servidor, `venceEn=null` y ninguna etapa `ACTUAL`. No se deja Fin esperando un segundo check. Se reutilizan las columnas y los adaptadores de persistencia existentes.
- **Evento del sistema.** Se añade `compartido.domain.eventos.PedidoFinalizado`, con pedido, etapa automática y hora del servidor, sin actor ni rol. Se incluye en el contrato sellado de eventos. `historial.EventoPedido.desde` lo transforma explícitamente en `PEDIDO_FINALIZADO` con `usuario_id`/`rol_codigo` nulos; el caso separado evita desreferenciar su actor nulo y no relaja los eventos de personas. El tipo y las restricciones de sistema ya están previstos en V2. La aprobación de A10 conserva su actor y sus adjuntos; los archivos no se duplican en el evento del sistema.
- **Atomicidad y doble envío.** Se conserva `buscarParaActualizar` con bloqueo de fila y relectura dentro de `TransactionTemplate`. Guardado del agregado, entrada de Fin, aprobación e historial del sistema se ejecutan en esa misma transacción. La publicación de eventos al historial es síncrona. Si falla el historial no debe confirmarse el cierre; una segunda confirmación que espere el bloqueo relee `finalizado=true` y recibe el 409 existente «El pedido ya terminó», sin generar otro cierre. Esta garantía de diseño queda pendiente de ejecución en la parte B.
- **Consultas existentes.** `accionesDisponibles` ya devuelve lista vacía para pedidos finalizados; A11 no forma parte de las etapas habilitadas y el flujo no tiene acciones manuales de Fin. Se conservan el detalle, la línea de tiempo y la lectura de historial con usuarios de sistema nulos.
- **Correo y orden pendientes.** No se cambian `EncolarNotificacionCasoDeUso`, destinatarios ni política de avisos. La aprobación conserva el outbox de avances ya existente, en la transacción; no se agrega un correo específico por `PedidoFinalizado` (queda fuera de los casos que encolan, F4-D9 pendiente). No se garantiza orden causal de lectura: la aprobación y el cierre tienen la misma hora y el historial desempata por UUID. F4-H01 permanece abierto, también para este par; no se implementó una solución de ordenación.
- **Contenido y contrato.** Los datos comerciales permanecen en los archivos; no se agregan campos de productos ni interpretación de su contenido. Se actualiza la descripción de acciones en OpenAPI para distinguir A10 habilitada pero bloqueada por documentos de Fin automático, sin cambiar los esquemas JSON. Sin pantallas de 4.6, Google, OAuth o workers activados.

### Revisión estática y límites

Se recorrieron los cambios Java siguiendo `ecc:java-reviewer`: flujo por puertos R4, núcleo sin Spring/JPA, origen de la marca automática, lectura única del reloj existente, rama explícita del evento de sistema, consumidores de `actor()`, constructoras anteriores, persistencia de la nueva entrada y cierre dentro de la transacción existente. No se añadieron transacciones ni llamadas externas en el dominio. El switch exhaustivo del historial contempla el nuevo evento; el switch de notificaciones mantiene su `default` sin encolarlo. **No se afirma que compile ni que los checks pasen**: no se ejecutaron. La revisión no resuelve los bloqueos del cliente ni F4-H01.

### Casos para Codex — parte B, pendientes; no ejecutados en esta entrega

1. Definición vigente: A10 con Logística/admin vigentes muestra documentos `PENDIENTES`, plazo `PENDIENTE`, `venceEn=null` y `puedeCompletarse=false`; APROBAR con/sin adjuntos devuelve 409 sin modificar pedido, etapas, entradas, historial u outbox. Repetir A6/A9 y preservar sus bloqueos.
2. Preparar fixtures **explícitos y aislados** posteriores a los bloqueos, con otra versión no vigente que defina documentos ficticios de A10; no cambiar V2 ni la definición real, ni presentar esos casos como recorrido completo. Sin documentos reales de la entrada actual → 422. Adjuntos ajenos, inválidos o de otra entrada → rechazo sin cambios. Repetir permisos actuales, asignación ajena, cuenta desactivada y roles revocados.
3. Requisitos cumplidos en el fixture: una aprobación de A10 deja A10 y Fin completadas, `finalizado=true`, `finalizado_en` real del reloj controlado, última etapa Fin, cero etapas ACTUAL; entrada/salida de Fin iguales a ese instante, sin vencimiento. Reconstruir desde PostgreSQL y comprobar detalle, lista y línea de tiempo.
4. Historial: una aprobación por la persona con sus archivos y un `PEDIDO_FINALIZADO` del sistema sin usuario/rol, misma hora del servidor; sin duplicar adjuntos. Comprobar serialización y consulta de autor nulo. No exigir un orden relativo no aprobado.
5. Inyectar fallos independientes al guardar la nueva entrada, la aprobación y el evento de sistema: rollback de cierre, etapas, eventos e intención del outbox. Concurrencia de dos confirmaciones → un cierre, un evento del sistema y una aprobación; la segunda recibe 409 sin nueva mutación. Reintentar después del commit y después de un rollback.
6. A11 nunca admite acciones manuales, reasignación, observaciones o subidas posteriores al cierre; acciones disponibles vacías. Verificar con admin y responsable. Validar el rechazo existente de pedidos ya finalizados.
7. Definición aislada con etapa automática de otro número: el cierre depende de los datos, no de A11 hardcodeada; comprobar que una etapa destino normal no finaliza. Mantener compatibilidad de constructores anteriores y fronteras R1–R4/R9.
8. Outbox: conservar únicamente el aviso de transición existente cuando sus destinatarios estén configurados en el entorno ficticio; no generar un aviso adicional por Fin ni inventar receptores. Registrar F4-D9 como pendiente.
9. Adaptar la prueba existente `Backend24IT.fase4NoHabilitaAccionesDeIncidenciasFueraDelAlcance`: A10 ahora está habilitada y su 409 corresponde a documentos pendientes, no a exclusión de fase. Separar esa garantía de la ausencia de acciones manuales de Fin. Las pruebas no se modificaron en parte A.
10. Después de las correcciones que procedan, ejecutar compilación, check, regresión de F4-P01, arquitectura y cobertura real. Mantener el historial de resultados anterior separado y no cerrar F4-H01 o la fase por esos checks.

**Decisiones que siguen pendientes:** F4-D8 (documentos/obligatoriedad/plazo de Incidencias), F4-D4/F4-D7 (Embarque/Bodega), F4-D9 (aviso específico de Fin/demora y receptores no aprobados), F4-H01 (orden). El cierre está implementado detrás de los requisitos, no desbloqueado con valores inventados. 4.6 sigue fuera de esta entrega; 3.B3 continúa pospuesto.

## Entrega del paso 4.6 — Pantallas conectadas (2026-10-19)

**Parte A implementada; pendiente de validación.** Detalle y casos para Codex en `documentos/fase-4-paso-4.6-implementacion.md`. Se leyeron manual, OpenAPI y DESIGN.md y se aplicaron shadcn, TanStack Query e impeccable polish sobre la interfaz existente. No se modificaron pruebas ni se ejecutaron pruebas, lint, builds o migraciones. Los resultados anteriores no certifican estos cambios.

- Tasks 8 y 9: ampliación de esquemas del contrato con bodega, demora, llegada, `requiereBodega` y `registraLlegada`; consulta de bodegas por sesión/filtro y mantenimiento con versión. Diálogo de demora con observación/fecha y selector real de bodega activa, sin calcular transiciones o fechas de negocio en el navegador.
- Bandejas reutilizadas: China A5–A7 y Logística A8–A10 mediante datos y permisos del servidor, conservando roles múltiples, paginación y las etapas anteriores. Detalle con llegada, bodega, estimación confirmada y Fin; historial y resúmenes del backend. Fin no añade acciones manuales.
- Catálogo `/bodegas` conectado a GET/POST/PATCH para admin o Logística. Sin borrado, IDs ficticios o permisos simulados. Conflictos de versión conservan el borrador y exigen revisar la versión vigente; cambios de etapa, 403 y errores recargan el estado sin reenvío automático. Se invalidan consultas afectadas de la sesión después de operaciones; observaciones reutilizan la actualización de pedido existente.
- Documentos de A6/A9/A10 continúan PENDIENTES: aviso y confirmación bloqueada, incluso mediante Enter. Se conservan carga real, estados de copia y descarga por backend. Plazos PENDIENTES se distinguen de SIN_PLAZO, sin vencimientos inventados. No se alteran decisiones de destinatarios, productos, feriados o avisos específicos.
- Polish con componentes existentes, etiquetas, estados de carga/error/vacío, texto adaptable y conservación de DESIGN.md. Header.tsx y backend intactos. La revisión estática no acredita compilación o verificación visual.

**Pendientes para Codex:** adaptar fixtures al contrato obligatorio sin debilitar esquemas; probar roles múltiples y permisos vigentes, demora/fechas, llegada marcada, bodega activa y concurrencia, catálogo/duplicados/versiones, invalidaciones y errores inciertos, documentos/entradas reales, plazos pendientes, Fin sin acciones, accesibilidad/responsive. Usar fixtures aislados explícitos para reglas posteriores a bloqueos; no presentar esos casos como recorrido completo. Ejecutar lint/test/build y las regresiones de 4.5 en la parte B. Lista detallada en el documento de entrega.

F4-D4/D7/D8 y sus bloqueos permanecen abiertos; F4-H01 conserva su estado separado y el historial respeta el orden del servidor. Integración real 3.B3 pospuesta; OAuth, workers y schedulers no activados. **La fase 4 permanece abierta.**

## Corrección de F4-H01 (2026-10-20): orden de eventos con la misma hora

**Parte A implementada; sin pruebas, builds ni migraciones ejecutadas. Pendiente de revalidación por Codex; F4-H01 sigue abierto hasta entonces.**

- **Aprobada por Richard (2026-10-20), corrección técnica:** el orden de inserción de los eventos nuevos se conserva con una secuencia única que genera la base. La API ordena de forma descendente por hora y, en empate, por secuencia: muestra primero el último evento emitido. No se inventa el orden de los eventos antiguos que comparten hora, no se alteran horas y no se debilitan las protecciones de solo inserción.
- **Migración `V13__historial_secuencia.sql`** (siguiente libre; V1–V12 sin cambios): secuencia `evento_pedido_secuencia`, columna `evento_pedido.secuencia bigint` agregada sin valor por defecto (las filas existentes quedan nulas, sin reescritura ni UPDATE) y después con `DEFAULT nextval(...)` para las nuevas; CHECK `evento_pedido_secuencia_nueva` NOT VALID (toda fila nueva lleva número), índice único `evento_pedido_secuencia_unica` e índice de lectura `evento_pedido_orden (pedido_id, ocurrido_en, secuencia, id)`.
- **Código:** `EventoPedidoEntity.secuencia` (solo lectura: `insertable = false, updatable = false`; la escribe la base); `EventoPedidoDatos.findByPedidoIdOrderByOcurridoEnAscSecuenciaAscIdAsc` (el id solo desempata eventos anteriores a V13). Todos los lectores pasan por `HistorialRepositorio.listarPorPedido`: la consulta del historial (que invierte la lista) y el resumen de etapas; se actualizaron sus comentarios. El frontend no reordena el historial.
- **Emisión:** sin cambios y síncrona: `ApoyoPedidos.publicarEventos` publica en el orden de `retirarEventos()`, `OyenteEventosPedido` (`@EventListener`, `Propagation.MANDATORY`) inserta con `persist` en la misma transacción, y Hibernate respeta el orden de `persist` al insertar (no hay `order_inserts`).
- **Contrato 0.10.1-borrador:** nota de orden en `GET /pedidos/{id}/historial`; la secuencia no se expone.
- **Casos para Codex (parte B), pendientes de revalidación:**
  - V13 sobre una base con V1–V12 **y eventos existentes**: la migración no actualiza ni reescribe filas (los triggers de solo inserción siguen intactos y no se disparan), las filas anteriores quedan con `secuencia` nula, sus `ocurrido_en` no cambian y siguen ordenándose como antes (hora e id) dentro de la misma hora;
  - una inserción nueva sin `secuencia` recibe número; un INSERT con `secuencia` nula explícita se rechaza (CHECK `evento_pedido_secuencia_nueva`, NOT VALID); números únicos; UPDATE, DELETE y TRUNCATE siguen rechazados;
  - **par 1**, aprobar Bodega: `BODEGA_ASIGNADA` y `ETAPA_APROBADA` con la misma hora → en la API, `ETAPA_APROBADA` antes que `BODEGA_ASIGNADA` (primero el último emitido), sea cual sea el UUID de cada uno;
  - **par 2**, aprobar Incidencias: `ETAPA_APROBADA` y `PEDIDO_FINALIZADO` con la misma hora → en la API, `PEDIDO_FINALIZADO` antes que `ETAPA_APROBADA`;
  - el resumen de etapas (P-20) usa el mismo orden y sigue tomando el último cierre de cada etapa;
  - los eventos de una acción se insertan de forma síncrona en la transacción de la acción (`@EventListener` con `Propagation.MANDATORY`), en el orden de `retirarEventos()`; si la acción falla, no queda ninguno;
  - **el reproductor `Fase4IT.historialPuedeInvertirElOrdenCausalPorDesempateUuid` documenta el comportamiento anterior** (desempate por UUID): con la corrección, el orden sigue la inserción y no el UUID, así que ese caso debe actualizarse en la parte B para comprobar la garantía nueva. No se modificó.

- **Revisión `ecc:java-reviewer` (lectura estática, 2026-10-20):** sin hallazgos críticos ni altos en producción. V13 es solo de catálogo (sin reescritura ni UPDATE), el DEFAULT y el CHECK NOT VALID solo afectan a filas nuevas, `ddl-auto: validate` acepta la columna, Hibernate la omite en el INSERT, el orden de `persist` se conserva aun con adjuntos intercalados y flushes intermedios, no hay otros escritores ni lectores de `evento_pedido` que dependan del orden, y ambos lectores (historial y resumen) quedan coherentes. No ejecutar `VALIDATE CONSTRAINT evento_pedido_secuencia_nueva`: fallaría por las filas anteriores, nulas a propósito.
- **Pruebas existentes que fallarán y debe adaptar Codex (no se modificaron):**
  - `Fase4IT.historialPuedeInvertirElOrdenCausalPorDesempateUuid` (líneas ~374-385): esperaba el orden por UUID (`BODEGA_ASIGNADA`, `ETAPA_APROBADA`); con V13 la API devuelve `ETAPA_APROBADA`, `BODEGA_ASIGNADA` (último emitido primero). Reescribirla para exigir la garantía nueva;
  - `MigracionesIT` (líneas ~106, 121, 122), `V2RestriccionesIT` (línea ~29) y `MigracionesFase4IT` (línea ~22 y nombre V1AV12): fijan la versión final en 12; ahora es 13;
  - `MigracionesFase4IT` (líneas ~33-38): si se agrega V13 al recorrido, `select * from evento_pedido` gana la columna `secuencia` (nula en filas anteriores): comparar sin ella;
  - pruebas nuevas sugeridas: orden causal con eventos del mismo instante (ambos pares) y secuencia nula en eventos anteriores a V13, no nula en los nuevos.

## Testing Strategy (parte B — Codex; no son tareas de este plan)
Referencia del manual: 4.B1 reglas A5–A11 (adjunto requerido, 10 días hábiles, demora con observación y fecha, 24 h hábiles, bodega obligatoria, permisos del catálogo, Fin automático y rechazo de acciones sobre Fin); 4.B2 Playwright A1 → Fin con BL, demora, bodega Guayaquil e incidencias (@critico); 4.B3 revisión y regresión.

## Validation Commands
Solo para la parte B (Codex): `./gradlew check` y `npm run lint && npm run test && npm run build`, más el recorrido Playwright. La parte A no ejecuta comandos.

## Acceptance Criteria (de la entrega de implementación)
- [ ] Flujo A implementado de A1 a Fin con adjuntos, bodega, demoras e historial, con las reglas pendientes del cliente marcadas y sin valores inventados.

## Risks
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| F4-D4/D7/D8 sin respuesta: el flujo no pasa de A6 | Alta | Alta | Pedir la respuesta antes del paso 4.2; mientras tanto, el motor rechaza con un mensaje claro |
| Fase 3 cambia antes de integrarse (3.B3) | Media | Media | Rama desde `fase-3-drive-gmail`; traer cambios con merge |
| "Packing List o BL" exige extender el motor | Media | Media | ADR antes de implementar |
| Arrastre de fechas mal entendido sin cronograma | Media | Media | F4-D5 explícita; cronograma en la fase 5 |

## Documentos de A6, A9 y A10 resueltos (2026-10-21) — pendiente de verificación por Codex

Richard confirmó F4-D4, F4-D7 y F4-D8 el 2026-10-21; las implementa V16 (detalle en el plan de la fase 5, "Documentos de A6, A9 y A10 y actualización de 30 s"):
- **A6:** al menos un "Packing List"; BL y otros, opcionales.
- **A9:** bodega y al menos un documento de cualquier tipo.
- **A10:** al menos un documento de cualquier tipo, sin excepción "sin incidencias".

El riesgo "el flujo no pasa de A6" deja de depender del cliente. El recorrido A1–Fin sigue sin verificar y la fase 4 sigue abierta.

## Notes
- Contrato vigente: 0.7.3-borrador; última migración: V8 (fase 3).
- Decisiones nuevas con prefijo F4-D para no chocar con P-01…P-20 y F3-D1…F3-D9.
