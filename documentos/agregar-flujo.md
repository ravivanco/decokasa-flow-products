# Guía: incorporar un flujo o una versión de flujo

> **Pendiente de revisión por Codex** (hallazgo F6-B3-H02, `documentos/pruebas/fase-6-informe.md`). Creada el 2026-10-21. No cierra ninguna fase.

Esta guía explica **qué** implica cada tipo de cambio en los flujos y **quién** decide. Los pasos operativos se centralizan en la skill [`agregar-flujo-decokasa`](../.claude/skills/agregar-flujo-decokasa/SKILL.md); el **porqué** del motor, en [ADR-0004](adr/0004-flujo-configurable-por-datos.md) y [ADR-0019](adr/0019-habilitacion-por-definicion.md) (proposed).

**Prioridad:** La skill centraliza únicamente los pasos operativos. La guía y la skill están subordinadas a los requisitos y decisiones aprobados ([`requerimientos.md`](requerimientos.md)), al contrato vigente ([`api/openapi.yaml`](api/openapi.yaml)) y a los ADR aceptados. Ante una discrepancia, se registra y se corrige el procedimiento; no se cambia una regla del producto por seguir la skill o la guía. Un ADR proposed (como ADR-0019) no autoriza cambios por sí mismo. Se mantienen separados los requisitos confirmados, las decisiones provisionales y las preguntas pendientes.

## Cómo funciona un flujo

- Un flujo (país + tipo, por ejemplo EC/A) es un conjunto de **datos versionados**: etapas, transiciones, documentos requeridos y plazos (`definicion_flujo`, `definicion_etapa`, `transicion_permitida`, `documento_requerido`). Un único motor los interpreta para todos los flujos.
- Por país y tipo hay **una sola versión vigente**. Los pedidos nuevos nacen en ella.
- **Cada pedido se queda en la versión con la que nació** hasta Fin: sus acciones, permisos, documentos y plazos salen de esa versión, aunque luego se publique otra.
- Qué etapas admiten acciones, quién crea pedidos y qué etapas cierra el sistema sale de los datos (rol de cada etapa, etapa automática); el código no pregunta si el flujo es A o B.

Hoy existen A/EC v1 (11 etapas) y B/EC v1 (12 etapas, V17 y V18).

## Los tres tipos de cambio

| Cambio | Ejemplo | Qué implica | Quién decide |
|---|---|---|---|
| **Flujo nuevo** de un país y tipo ya conocidos | A o B de Colombia | Una migración que inserta la definición completa. Si el país necesita algo que el motor no tiene, es una ampliación del motor con su ADR. | El cliente define etapas, responsables, documentos y plazos; Richard aprueba lo que el cliente no definió. Lo no confirmado queda como pendiente. |
| **Versión nueva** de un flujo existente | Cambiar una etapa, un destino, un documento o un plazo de A | Una migración que retira la vigencia de la versión actual y publica la nueva, en la misma transacción. Los pedidos en curso siguen con su versión. Si cambian etapas, hay que revisar antes las listas y tableros. | Igual que arriba, con el origen de cada cambio. Mover pedidos a la versión nueva no existe y necesitaría su propia decisión y ADR. |
| **Completar** una versión publicada | Marcar la etapa de llegada (V11) o las etapas revisoras (V3, V18) | Solo un atributo que antes no existía o estaba vacío, sin cambiar nada ya ocurrido. | Richard, de forma expresa y para cada caso. No es una autorización general. |

Un **tipo** de flujo nuevo (además de A y B) no es solo datos: también cambia el esquema, el código Java, el contrato y las pantallas. Necesita decisión y ADR.

## Reparto del trabajo

- **Parte A (Claude Code):** migración, ampliaciones genéricas del motor si hacen falta, contrato, documentación y pantallas, siguiendo la skill. No escribe ni ejecuta pruebas.
- **Parte B (Codex):** prueba y verifica con los casos que la skill indica y deja el resultado en `documentos/pruebas/fase-N-informe.md`.
- **Richard** valida y decide el cierre de la fase.

## Reglas que no cambian

- No se extienden por analogía documentos, plazos ni permisos aprobados para otro flujo; lo no confirmado se marca como [P] o ⏸ (`requerimientos.md` §5).
- Una migración aplicada no se edita; cualquier corrección va en una migración nueva con la siguiente versión libre de Flyway.
- Los permisos se deciden en el servidor; las pantallas solo los reflejan.

## Referencias

- Procedimiento: [skill `agregar-flujo-decokasa`](../.claude/skills/agregar-flujo-decokasa/SKILL.md).
- Decisiones: [ADR-0004](adr/0004-flujo-configurable-por-datos.md), [ADR-0018](adr/0018-etapa-de-llegada-explicita.md) y [ADR-0019](adr/0019-habilitacion-por-definicion.md) (proposed).
- Ejemplo completo: el flujo B, en `backend/src/main/resources/db/migration/V17__flujo_b_ecuador.sql`, `V18__revision_flujo_b.sql` y `.claude/PRPs/plans/fase-6-flujo-b.plan.md`.
- Requisitos y respuestas del cliente: [`requerimientos.md`](requerimientos.md) (1.4/1.5, 4.4/4.5 y §5). Tablas: [`modelo-datos.md`](modelo-datos.md).
