---
name: agregar-flujo-decokasa
description: "Usar cuando en DECOKASA se pida agregar un flujo de etapas (flujo de Colombia, otro país u otro tipo de pedido), publicar una nueva versión de un flujo existente (cambiar etapas, transiciones, documentos, plazos o permisos) o completar un atributo de una versión ya publicada. Cubre migración Flyway, cambio de vigencia, conservación de pedidos en versiones anteriores, permisos, pantallas y el traspaso a Codex. No escribe ni ejecuta pruebas."
---

# Agregar un flujo a DECOKASA

> **Pendiente de revisión por Codex** (hallazgos F6-B3-H01/H02 de `documentos/pruebas/fase-6-informe.md`, ampliación del 2026-10-21). No cierra ninguna fase.

Un flujo es **datos versionados** (ADR-0004) que interpreta un único motor. Agregar un flujo nunca significa un motor nuevo, casos de uso paralelos ni condiciones del tipo `if (tipo == B)` en el código. Si una regla nueva no cabe en los datos, se amplía el motor de forma genérica, compatible con los flujos existentes, y se registra en un ADR.

**Reparto de fuentes (sin duplicar reglas):**

| Documento | Contiene |
|---|---|
| Esta skill | El procedimiento paso a paso: centraliza los pasos operativos, no las reglas del producto. |
| [`documentos/agregar-flujo.md`](../../../documentos/agregar-flujo.md) | La guía breve: qué es cada tipo de cambio, quién decide y enlaces. No repite pasos. |
| ADR-0004 y [ADR-0019](../../../documentos/adr/0019-habilitacion-por-definicion.md) (proposed) | Por qué el motor funciona así: datos versionados, habilitación por definición, automáticas y creación por rol inicial. |

**Prioridad:** La skill centraliza únicamente los pasos operativos. La guía y la skill están subordinadas a los requisitos y decisiones aprobados (`documentos/requerimientos.md`), al contrato vigente (`documentos/api/openapi.yaml`) y a los ADR aceptados. Ante una discrepancia, se registra y se corrige el procedimiento; no se cambia una regla del producto por seguir la skill o la guía. Un ADR proposed (como ADR-0019) no autoriza cambios por sí mismo.

Referencia completa: el flujo B (fase 6) — `V17__flujo_b_ecuador.sql`, `V18__revision_flujo_b.sql` y `.claude/PRPs/plans/fase-6-flujo-b.plan.md`.

## Qué tipo de cambio es

Decide primero cuál de los tres casos es. Cada uno tiene su procedimiento:

| Caso | Ejemplo | Procedimiento |
|---|---|---|
| **Alta de un flujo nuevo**: país y tipo sin ninguna versión todavía | B/EC v1 (V17); A o B de Colombia | §1 |
| **Nueva versión** de un flujo que ya tiene pedidos o una versión vigente: cambia una regla (etapas, transiciones, destinos, documentos, plazos, roles) | A/EC v2 | §1 y §1-bis |
| **Completar** un atributo nuevo de una versión ya publicada, sin cambiar una regla que se haya aplicado | `revisa_etapa` (V3, V18), `registra_llegada` (V11) | Solo con aprobación expresa de Richard para ese caso; ver §1-bis, último punto |

Un **tipo** de flujo nuevo (C, por ejemplo) no es solo SQL: exige además ampliar el CHECK `definicion_flujo_tipo` (V2), el enum Java `TipoFlujo`, los `z.enum(['A', 'B'])` y las listas `['A', 'B']` del frontend (`api/pedidos.ts`, `formato.ts`, `ListaPedidos`, `BandejaPedidos`, `NuevoPedido`), el enum del contrato y las opciones de pantalla. Eso es una ampliación del motor: pide decisión y ADR.

## 0. Antes de empezar (obligatorio)

1. **Fuentes.** Lee `documentos/requerimientos.md` (1.4/1.5, 4.4/4.5 y §5), el plan de la fase, [`documentos/agregar-flujo.md`](../../../documentos/agregar-flujo.md) y los ADR 0004, 0006, 0018 y 0019 (proposed).
2. **Requisito, [P] y ⏸.** Para cada etapa y transición, anota de dónde sale: documento del cliente, respuesta del cliente, decisión de Richard (con fecha) o propuesta. **No extiendas por analogía** documentos, plazos ni permisos aprobados para otro flujo: pregunta. Lo no confirmado se siembra como pendiente (`PENDIENTE`, `PENDIENTES`) y se documenta.
3. **Rama y migración.** Comprueba la rama y `git status`; conserva `Header.tsx` y los cambios ajenos. Usa la **siguiente versión libre** de `backend/src/main/resources/db/migration`. No alteres migraciones aplicadas ni reserves versiones.
4. **Sin pruebas ni ejecución.** Claude Code codifica (parte A); Codex prueba y verifica (parte B). Sin commit, push ni merge salvo orden expresa.

## 1. Migración: definición versionada

Una migración que **inserta** la definición completa y nunca cambia las etapas, transiciones ni documentos de otra versión. En el **alta** de un flujo nuevo solo inserta, con `vigente = true` (no hay otra vigente para ese país y tipo). En una **nueva versión** inserta igual, pero además cambia la vigencia de la anterior: sigue §1-bis, porque el índice `definicion_flujo_vigente_unica` (V2) admite **una sola** versión vigente por país y tipo.

| Tabla | Qué sembrar | Columnas clave |
|---|---|---|
| `definicion_flujo` | País, tipo (`A`/`B`; un tipo nuevo exige ampliar los CHECK), versión, `vigente` | Una sola vigente por país y tipo (índice parcial) |
| `definicion_etapa` | Una fila por etapa, numeradas desde 1 | `rol_codigo` (nulo solo en automáticas), `plazo_estado` `DEFINIDO`/`SIN_PLAZO`/`PENDIENTE` con `plazo_cantidad`+`plazo_tipo` solo si `DEFINIDO`, `automatica` (Fin), `revisa_etapa` (P-17), `registra_llegada` (ADR-0018, como máximo una) |
| `transicion_permitida` | Una fila por acción de cada etapa | `accion`, `etapa_destino` (nulo solo en `REPORTAR_DEMORA`/`ELEGIR_BODEGA`), `requiere_observacion`, `requiere_urgencia` (solo `DEVOLVER_POR_PRODUCTO_NUEVO`), `requiere_bodega`, `requisitos_documentales` `DEFINIDOS`/`PENDIENTES`, `exige_algun_documento` (V16; solo con `DEFINIDOS`) |
| `documento_requerido` | Tipos obligatorios exactos por transición | `tipo_documento` (1–100 caracteres; debe coincidir con el tipo del adjunto) |

Reglas:
- **UUIDv7 fijos** y deterministas (marca de tiempo de la fecha de la migración + número de orden: `…0001` flujo, `…01NN` etapas, `…02NN` transiciones), iguales en todos los entornos.
- **Ninguna transición sale de una etapa automática**: `DefinicionFlujo` lo rechaza al cargar.
- **Urgencia**: termina al entrar en la etapa que admite `DEVOLVER_POR_PRODUCTO_NUEVO` (`DefinicionFlujo.esRetornoDeUrgencia`); no la fijes por número.
- Termina con un bloque `DO $$ … $$` que **compruebe** el resultado (número de etapas, Fin automático, llegada, transiciones, destinos de devoluciones, documentos exactos) y que **las definiciones de otros flujos no cambiaron**. Si algo no cuadra, `raise exception`: Flyway revierte entera.
- Comenta en la cabecera las fuentes, qué queda pendiente y por qué no se extendió nada por analogía.
- Si un permiso de lectura o revisión necesita aprobación (por ejemplo `revisa_etapa`), sepáralo en otra migración tras la aprobación (precedente: V18).

## 1-bis. Publicar una nueva versión y conservar los pedidos anteriores

**Qué garantiza el sistema hoy** (comprobado en el código y el esquema, 2026-10-21):
- Cada pedido guarda su versión en `pedido.flujo_id`, y el trigger `pedido_proteger_identidad` (V2) impide cambiarla. Ningún pedido cambia de versión.
- Solo la **creación** resuelve la versión vigente (`CrearPedidoCasoDeUso` → `ConsultarDefinicionFlujo.vigente`). Acciones, permisos, archivos, observaciones, reasignación, cronograma y motivo de retraso cargan la definición por `pedido.flujoId()`. Un pedido en v1 sigue con las reglas de v1 hasta Fin.
- `GET /flujos/{pais}/{tipo}?version=N` sigue sirviendo cualquier versión; el detalle del pedido la usa.

**Procedimiento:**
1. **Decisión.** Cada regla que cambia necesita su origen (cliente, Richard con fecha o propuesta marcada). Anota qué pasa con los pedidos en curso: por defecto **siguen en su versión**. Migrar pedidos de versión no existe en el motor y no se hace sin decisión y ADR.
2. **Una sola migración, siguiente versión libre de Flyway**, que en este orden:
   1. desactiva la vigente: `update definicion_flujo set vigente = false where pais_codigo = 'EC' and tipo_flujo = 'A' and vigente;` (comprueba con `GET DIAGNOSTICS`/`FOUND` que cambió exactamente una fila, o `raise exception`);
   2. inserta la nueva versión (`version` = anterior + 1, comprobada en el bloque `DO`) con `vigente = true` y la definición **completa**: todas sus etapas, transiciones y documentos, aunque coincidan con la anterior (no se comparten filas entre versiones);
   3. termina con el bloque `DO` de §1, más: una sola vigente por país y tipo; la anterior sigue existiendo, sin vigencia y con el mismo número de etapas, transiciones y documentos que antes; ningún pedido apunta a la versión nueva; los pedidos de la anterior no cambiaron.
   
   El índice parcial es único y no diferible: si insertas la nueva vigente antes de desactivar la anterior, PostgreSQL responde 23505. Flyway ejecuta cada migración de PostgreSQL en una transacción, así que los dos pasos se ven juntos o no se ven.
3. **Nada más cambia en la versión anterior**: ni etapas, ni transiciones, ni documentos, ni `revisa_etapa`/`registra_llegada`, ni pedidos, entradas o archivos. No hay `delete`.
4. **Coordinación de migraciones.** Comprueba la rama y la última versión aplicada en todas las ramas abiertas (dos ramas no pueden usar el mismo número de Flyway). Una migración aplicada no se edita: si algo sale mal, otra migración nueva lo corrige. Flyway aplica las migraciones al arrancar el backend, antes de atender peticiones, así que la vigencia cambia con el despliegue, no en caliente.
5. **Pantallas antes de publicar.** Las tablas, bandejas, tableros y "Productos propuestos" dibujan la línea compacta y filtran por número de etapa con la definición **vigente** (`useDefinicionesVigentes`). Solo el detalle usa la versión del pedido (`versionFlujo`, que el resumen de lista `PedidoResumen` todavía no trae; `nombreEtapaActual` sí sale del servidor con la versión del pedido). Si la nueva versión cambia el número, el orden o el nombre de etapas, ese límite debe resolverse antes de publicarla, con decisión propia (por ejemplo, añadir la versión al resumen del contrato). Si solo cambia documentos, plazos o permisos, la línea no cambia.
6. **Completar una versión publicada** (sin versión nueva) solo cuando Richard lo aprueba expresamente para ese caso, y solo para un atributo que antes no existía o estaba vacío y no cambia nada ya ocurrido (precedentes: V3, V11, V18). No es una autorización general de cambiar reglas de pedidos en curso.

## 2. Motor: qué ya es genérico

No hace falta código para: transiciones y destinos, observación obligatoria (RECHAZAR/DEVOLVER), urgencia 4/12/24 h, documentos por tipo o "al menos uno", vinculación por pedido/etapa/entrada actual (P-05), llegada y demora, bodega, Fin automático y `PEDIDO_FINALIZADO`, plazos y calendario, vencimientos ETAPA/URGENCIA, historial, resúmenes, outbox.

Ya están dirigidos por la definición (el porqué está en ADR-0019, proposed):
- **Habilitación** (`EtapasHabilitadas.segunDefinicion()`): las etapas con rol y no automáticas del flujo de cada pedido. Los casos de uso la concretan con `para(flujo.etapas())` o `paraEtapa(n, manual)`.
- **Creación** (`PermisosEtapa.paraCrear`): solo quien tiene, vigente, el rol de la primera etapa. El admin crea solo los flujos cuya primera etapa es suya.
- **Actuar** (P-06): rol de la etapa o persona asignada; admin; cuenta y roles vigentes en la base.
- **Descarga** (P-17): admin, quien actúa en la etapa del archivo o en su etapa revisora (`revisa_etapa`), con la asignación de P-06.

Busca supuestos fijos antes de terminar: `grep` de `TipoFlujo.A`, `'A'`, números de etapa (`== 7`, `Set.of(1, …)`) y textos "A2/A4" en backend y frontend. Si amplías el motor, conserva constructores y firmas anteriores para las pruebas existentes, y regístralo en un ADR y en el plan.

## 3. Contrato y documentación

- `documentos/api/openapi.yaml`: sube la versión menor, añade un párrafo de cambios, actualiza la tabla de transiciones y los textos de permisos. Solo campos nuevos; nada se retira sin decisión.
- `documentos/requerimientos.md` (§5: decisiones con su origen), `documentos/modelo-datos.md` (siembra) y `documentos/arquitectura.md` (quién crea, pendientes).

## 4. Pantallas (frontend)

Reutiliza componentes y consultas; **no implementes un motor en el navegador** ni inventes plazos:
- Definiciones: `useDefinicionesVigentes()` (una consulta por flujo) para tablas y tableros, y `useDefinicionFlujo(pais, tipo, version)` en el detalle (versión del pedido). Pasa `recorrido={{ A: …, B: … }}` a `TablaPedidos`.
- Códigos con `codigoEtapa(pedido.tipoFlujo, n)`; nombres de flujo con `nombreFlujo`. Nunca `codigoEtapa('A', …)` ni nombres de etapa fijos: salen de la definición.
- Agrupar por flujo **y** etapa (A3 ≠ B3). Las secciones condicionales (llegada, bodega) dependen de la definición, no del tipo.
- Crear: `flujosQuePuedeCrear(definiciones, roles)` decide qué botones mostrar; la ruta `/pedidos/nuevo?flujo=X` no está bajo `SoloAdmin`. El servidor decide.
- Bandejas especiales (como "Productos propuestos" de B2): `usePedidos({ tipoFlujo, etapa })` con la etapa sacada de la definición (por ejemplo, la que `revisa_etapa` la primera).
- shadcn y `documentos/diseno/DESIGN.md` (tokens, foco `ring-ring`, altura mínima 40 px, estados con texto e ícono). Accesibilidad: nombres que contengan el texto visible.

## 5. Traspaso a Codex (parte B)

Documenta en el plan de la fase, por paso: qué se entregó, archivos, decisiones con su origen, pendientes, huecos conocidos, revisión Java (`ecc:java-reviewer`) y **pruebas existentes que probablemente cambien** (versión de Flyway, conteos globales de definiciones/etapas/transiciones, `revisa_etapa`). Casos mínimos para Codex:
1. Migración sobre base limpia y sobre la anterior con pedidos en curso; otros flujos intactos; el bloque `DO` falla ante datos alterados.
   - **Nueva versión** (§1-bis): con pedidos en v1 en varias etapas, tras migrar hay una sola vigente; los pedidos de v1 conservan `flujo_id` y avanzan con las reglas de v1 hasta Fin; un pedido nuevo nace en v2; `GET /flujos/{pais}/{tipo}?version=1` sigue respondiendo; insertar v2 vigente sin desactivar v1 falla con 23505 y Flyway revierte la migración entera.
2. `GET /flujos/{pais}/{tipo}`: etapas, acciones, documentos y plazos (con pendientes visibles).
3. Creación por el rol de la primera etapa; admin sin ese rol → 403; varios roles conservados.
4. Cada transición con y sin sus requisitos (observación, urgencia, bodega, documentos de la entrada actual vs. de otra entrada → 400).
5. Devoluciones y urgencia hasta la etapa de retorno; Fin automático sin acciones manuales.
6. Descarga por etapa revisora con y sin asignación.
7. Pantallas: número correcto de etapas por flujo, códigos sin heredar de otro flujo, bandejas sin mezclar flujos; recorrido crítico E2E junto con los flujos existentes.

Codex guarda resultados en `documentos/pruebas/fase-N-informe.md`; Richard valida el cierre. **No cierres fases** desde la parte A.
