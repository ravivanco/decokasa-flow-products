# ADR-0012: Transacciones fuera del núcleo y cola de salida (outbox)

**Fecha**: 2026-10-10
**Estado**: accepted en la parte de transacciones (aprobado por el equipo el 2026-10-12); el outbox sigue como propuesta
**Decisores**: equipo de desarrollo (propuesta de `arquitectura.md`, 3.1 y 8.2; aprobada en parte por Richard)

## Contexto

Ninguna acción puede quedar sin su evento de historial (`requerimientos.md`, invariantes), y el correo debe reintentarse sin perderse (PRD, fase 3: "cola y reintentos"). Los casos de uso no deben conocer Spring (`arquitectura.md`, R2).

## Decisión

- **Aprobado (2026-10-12):** la transacción la abre el adaptador de entrada o un decorador transaccional registrado en `config/`; el caso de uso no lleva anotaciones. Se aplica desde la fase 1.

**Propuesta asociada (no aprobada todavía; se decide en la fase 3):**
- Dentro de esa misma transacción se guardan la acción, el evento del historial y la fila de la cola de correos (outbox).
- Una tarea programada envía los correos pendientes y reintenta los que fallan.

## Alternativas consideradas

### Enviar el correo después de confirmar la transacción, sin cola
- **Pros**: más simple.
- **Contras**: si el envío o el proceso fallan, el correo se pierde.
- **Por qué no**: el PRD pide que no se pierda nada.

### Anotaciones transaccionales en los casos de uso
- **Pros**: es lo habitual en Spring.
- **Contras**: mete Spring en la capa de aplicación.
- **Por qué no**: rompe R2.

## Consecuencias

### Positivas
- Historial y correos consistentes con cada acción.

### Negativas
- Los correos salen con unos segundos de demora.
- Hay una tabla de cola más que mantener y limpiar.
