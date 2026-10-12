# ADR-0004: Flujo de etapas configurable por datos

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: proposed
**Decisores**: equipo de desarrollo (no viene del cliente)

## Contexto

Hay dos flujos: A, pedido de Ecuador, con 11 etapas, y B, pedido propuesto por China, con 12 etapas (`requerimientos.md`, 4.4 y 4.5). Colombia tendrá un proceso distinto que todavía no está definido. El demo fija las etapas en el código con condiciones por número de etapa, y eso causó los errores del flujo B (`auditoria-demo.md`, sección 6).

## Decisión

Las etapas, los roles responsables, los plazos, las acciones permitidas, los requisitos de cada acción y las transiciones se guardan como datos versionados. Un motor de flujos los interpreta. Un pedido sigue siempre la versión con la que se creó (`arquitectura.md`, 4.2).

## Alternativas consideradas

### Etapas fijas en el código
- **Pros**: más simple al principio.
- **Contras**: cada cambio de etapa o país obliga a programar y desplegar.
- **Por qué no**: ya hay dos flujos y viene un segundo país, y el demo muestra que así se cuelan errores.

## Consecuencias

### Positivas
- El flujo B y Colombia se agregan como configuración.
- Los plazos pendientes del cliente se cambian sin tocar el código.

### Negativas
- El motor es más abstracto y necesita buenas pruebas.

### Riesgos
- Un error en la configuración. Mitigación: migraciones revisadas y pruebas de punta a punta por flujo.

## Pendiente del cliente

- Plazos de Fabricación e Incidencias, y si los 45 días de tránsito son hábiles o corridos.
- Qué documentos son "todos los documentos" de Incidencias.
- Si se conservan pausar o cancelar un pedido y el checklist por etapa, que el demo tenía y los requerimientos no (`arquitectura.md`, 8.1).
