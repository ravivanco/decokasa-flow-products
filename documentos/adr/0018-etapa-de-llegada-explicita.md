# ADR-0018: La etapa de llegada a aduana se marca de forma explícita en la definición del flujo

**Fecha**: 2026-10-19
**Estado**: accepted (equipo)
**Decisores**: equipo (Richard, 2026-10-19). No es una respuesta del cliente.

## Contexto

En el paso 4.3, aprobar A7 ("✓ Llegada", requerimientos 4.4) debe registrar la llegada a aduana (`pedido.llegada_aduana_en`, V9) con la hora del servidor. La primera versión reconocía la etapa de llegada porque admitía `REPORTAR_DEMORA` en los datos del flujo. La revisión del paso señaló dos problemas: una versión futura del flujo que agregue `REPORTAR_DEMORA` a otra etapa registraría la llegada sin quererlo, y la detección usaba una excepción como control de flujo. El flujo es configurable por datos y versionado (ADR-0004), así que la función de una etapa debe estar en esos datos y no fijarse en el código (número 7) ni deducirse de otra regla.

## Decisión

`definicion_etapa` tiene la marca `registra_llegada` (V11), versionada con el flujo. Al aprobar una etapa marcada con una acción que avanza, el pedido guarda `llegada_aduana_en` con la hora del servidor, en la misma transacción que la acción y su evento. Como máximo una etapa por versión de flujo, y nunca una automática. En el flujo A vigente (EC, versión 1) solo A7 está marcada. `REPORTAR_DEMORA` no influye en la marca.

## Alternativas consideradas

### Deducirla de `REPORTAR_DEMORA`
- **Pros**: sin cambios de esquema.
- **Contras**: une dos reglas distintas; cualquier etapa con demora pasaría a registrar la llegada; la detección se hace con una excepción.
- **Por qué no**: lo señaló la revisión y Richard pidió una marca explícita.

### Fijar el número de etapa en el código (A7 / B8)
- **Pros**: simple.
- **Contras**: contradice ADR-0004; cada flujo o versión nueva obliga a cambiar el código.
- **Por qué no**: el motor lee las reglas de los datos.

### Una versión 2 del flujo A con la marca
- **Pros**: no cambia filas de la versión 1.
- **Contras**: los pedidos existentes siguen en la versión 1 y no registrarían la llegada; obliga a migrar pedidos de versión.
- **Por qué no**: la marca es un atributo nuevo que antes no existía, no un cambio de una regla vigente. Sigue el precedente de V3 (`revisa_etapa`, agregado a la versión 1). Ningún pedido llegó todavía a A7 (A6 sigue bloqueada por F4-D4), así que no cambia el resultado de nada ya ocurrido.

## Consecuencias

### Positivas
- La función de la etapa es visible en los datos y en `GET /flujos` (`registraLlegada`).
- Un flujo B o de Colombia marca su propia etapa de llegada sin tocar el código.
- La base impide dos etapas de llegada en un flujo y una etapa automática marcada.

### Negativas
- Una columna más en `definicion_etapa`, y en el contrato.
- Si A7 se aprobara más de una vez en un mismo pedido, `llegada_aduana_en` guarda la última; el historial conserva cada aprobación.

### Riesgos
- Que una versión futura del flujo olvide marcar su etapa de llegada: la llegada no se registraría. Mitigación: pruebas de la definición (parte B) y revisión de cada versión nueva.

## Pendiente del cliente

- El flujo B todavía no está en los datos; su etapa de llegada (B8, según 4.5) se marcará cuando se cargue.
- Plazo de Aduana (F4-D6) y quién escribe la fecha de una demora (F4-D5, provisional) siguen pendientes; no dependen de esta marca.
