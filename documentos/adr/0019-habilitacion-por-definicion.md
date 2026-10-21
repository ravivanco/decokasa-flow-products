# ADR-0019: Las etapas operables se deciden con la definición del flujo de cada pedido

**Fecha**: 2026-10-21 (registro posterior a la implementación del paso 6.4, del mismo día)
**Estado**: proposed — **pendiente de revisión por Codex y de aceptación por Richard**
**Decisores**: propuesta del equipo (Claude, parte A), a partir de la propuesta P6-B3-ADR de Codex (`documentos/pruebas/fase-6-informe.md`, 6.B3). No es una respuesta del cliente.

## Contexto

Hasta la fase 5, `EtapasHabilitadas` era un conjunto global de números de etapa (`{1..10}`, decisión P-04 del equipo, 2026-10-15) pensado solo para el flujo A. El flujo B (V17) tiene 12 etapas: B11 Incidencias es manual y B12 es Fin. Habilitar el número 11 de forma global habría habilitado también A11, que en A es Fin. Richard pidió en el paso 6.4 (2026-10-21) que la habilitación vaya por flujo y definición, sin habilitar el 11 de forma global, y que las etapas automáticas no admitan acciones manuales. ADR-0004 aprueba el flujo como datos versionados, pero no registra esta ampliación del motor: hoy solo está en el plan de la fase 6 (F6-D3).

## Decisión (propuesta)

1. **Habilitación por definición.** Una etapa admite acciones si, en la definición del flujo **del pedido** (`pedido.flujo_id`), tiene rol responsable y no es automática. `EtapasHabilitadas.segunDefinicion()` es la configuración de la aplicación. Cada caso de uso la concreta con `para(etapas)` o `paraEtapa(n, manual)`; sin concretar, `incluye()` falla en lugar de habilitar algo.
2. **Guard de etapas automáticas.** `DefinicionFlujo` rechaza al cargarse cualquier transición que salga de una etapa automática. Ningún Fin (A11, B12) tiene acciones manuales; lo cierra el sistema.
3. **Creación por el rol de la primera etapa.** Crea un pedido solo quien tiene vigente, en la base, el rol responsable de la primera etapa de la definición vigente (`PermisosEtapa.paraCrear`, instrucción de Richard del paso 6.2, F6-D2). El admin no tiene excepción general.

Las tres reglas se leen de los datos, sin preguntar en el código si el flujo es A o B.

## Alternativas consideradas

### Sumar números al conjunto global (`{1..11}`)
- **Pros**: un cambio mínimo.
- **Contras**: los números son ambiguos entre flujos: 11 es Incidencias en B y Fin en A. Cada flujo nuevo obliga a revisar el conjunto.
- **Por qué no**: Richard pidió no habilitar el 11 de forma global, y contradice ADR-0004.

### Un conjunto fijo por tipo de flujo (`A → {1..10}`, `B → {1..11}`)
- **Pros**: explícito y sin ambigüedad entre A y B.
- **Contras**: duplica en el código lo que ya dicen los datos (rol y `automatica`); una versión nueva o un flujo de Colombia obliga a cambiar el código.
- **Por qué no**: el motor lee las reglas de la definición versionada.

### Un motor o casos de uso paralelos para B
- **Pros**: aislamiento total de A.
- **Contras**: duplica reglas comunes (observación, urgencia, documentos, llegada, bodega, Fin, plazos e historial) y hace que diverjan.
- **Por qué no**: ADR-0004 y ADR-0011 piden un solo motor dirigido por datos.

## Consecuencias

### Positivas
- B11 se habilita en B sin tocar A; A1–A10 siguen igual que con `faseCuatroHastaIncidencias()`.
- Un flujo o una versión nueva define sus etapas operables solo con datos (`rol_codigo`, `automatica`).
- Una definición inconsistente (salida desde Fin) falla al cargar, no durante una acción.

### Negativas
- Cada caso de uso que comprueba la habilitación debe concretarla con la definición del pedido (`ConsultarPedidos`, `EjecutarAccionEtapa`, `AgregarObservacion`, `AutorizarArchivos` y `RegistrarMotivoRetraso`). Olvidarlo produce un error, no un permiso de más.
- Los conjuntos fijos (`faseDos()` … y el constructor con `Set`) se conservan solo por compatibilidad con las pruebas existentes; conviven dos modos en el mismo record.

### Riesgos
- Un caso de uso nuevo que use `incluye()` sin concretar la habilitación falla en ejecución. Mitigación: la excepción explícita y las pruebas de Codex (`HabilitacionFase6Test`, 6.B1).

## Límites

- **Habilitar no es permitir.** La habilitación solo dice qué etapas son operables; quién actúa sigue P-06 (rol de la etapa o persona asignada, admin, estado vigente en la base), y quién descarga sigue P-17 (`revisa_etapa`). Este ADR no cambia esos permisos ni la visibilidad.
- **No decide reglas de negocio.** No aprueba plazos, documentos, P-10, P-14 (que el admin pueda crear B sigue ⏸ pendiente del cliente) ni ninguna otra pregunta abierta.
- **No crea tipos de flujo.** Un tipo nuevo (C) exige además ampliar el CHECK de V2, `TipoFlujo`, el contrato y el frontend: es otra decisión.
- **No migra pedidos de versión.** El procedimiento para publicar versiones está en la skill `agregar-flujo-decokasa` (§1-bis), no aquí.

## Conservación de A y B

- Cada pedido conserva su versión (`pedido.flujo_id`, protegido por trigger desde V2), y la habilitación se calcula con esa versión. Publicar una versión nueva no cambia las etapas operables de los pedidos en curso.
- El flujo A vigente (EC v1) mantiene A1–A10 manuales y A11 automática; el flujo B (EC v1, V17/V18) tiene B1–B11 manuales y B12 automática.
- 6.B1 y 6.B2 lo verificaron localmente (`documentos/pruebas/fase-6-informe.md`); la aceptación de este ADR no se infiere de esas pruebas.

## Relación con otros ADR

- [ADR-0004](0004-flujo-configurable-por-datos.md): este ADR lo complementa, no lo sustituye.
- [ADR-0011](0011-monolito-modular-hexagonal.md): la regla vive en el dominio de `pedidos`, sin Spring; la configuración solo elige `segunDefinicion()`.
- [ADR-0012](0012-transacciones-y-outbox.md): la acción, el Fin automático, el historial y el outbox siguen en una sola transacción.
- [ADR-0018](0018-etapa-de-llegada-explicita.md): mismo criterio de marcar en datos versionados la función de una etapa, en lugar de fijar números en el código.

## Pendiente

- Revisión por Codex y aceptación (o cambios) por Richard. Hasta entonces, el estado es **proposed**.
- Pendientes del cliente que afectan a los flujos, sin resolver aquí: P-14, plazos de B2, B6, B8 y B11, y P-10 (provisional).
