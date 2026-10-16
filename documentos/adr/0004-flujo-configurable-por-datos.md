# ADR-0004: Flujo de etapas configurable por datos

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09); aprobada por el equipo el 2026-10-15
**Estado**: accepted (equipo)
**Decisores**: equipo de desarrollo; aprobación de Richard el 2026-10-15. No viene del cliente ni es una respuesta del cliente

## Contexto

Hay dos flujos: A, pedido de Ecuador, con 11 etapas, y B, pedido propuesto por China, con 12 etapas (`requerimientos.md`, 4.4 y 4.5). Colombia tendrá un proceso distinto que todavía no está definido. El demo fija las etapas en el código con condiciones por número de etapa, y eso causó los errores del flujo B (`auditoria-demo.md`, sección 6).

## Decisión

Las etapas, los roles responsables, los plazos, las acciones permitidas, los requisitos de cada acción y las transiciones se guardan como datos versionados. Un motor de flujos los interpreta. Un pedido sigue siempre la versión con la que se creó (`arquitectura.md`, 4.2).

## Aprobación (2026-10-15)

Richard aprobó como **decisión técnica del equipo** (decisión P-02 del plan de la fase 2) que el motor usa **definiciones versionadas de flujo, etapas y transiciones almacenadas en datos**. Es el mecanismo que pide el manual en el paso 2.1 ("definiciones versionadas de flujo y etapa, transiciones").

Alcance de la aprobación:

- **Aprobado:** el mecanismo. Las definiciones viven en tablas (`modelo-datos.md`, §3.2), se cargan con migraciones y el motor las interpreta. Cada pedido sigue la versión con la que se creó.
- **No aprobado aquí:**
  - el **contenido** de negocio que siga pendiente del cliente: plazos de Fabricación e Incidencias, tránsito, documentos de Incidencias (ver "Pendiente del cliente");
  - las demás decisiones abiertas de la fase 2 (archivos P-05 y migraciones P-16, entre otras);
  - una pantalla para editar flujos: los cambios siguen entrando por migraciones revisadas.
- **Origen:** equipo. El cliente definió las etapas y las transiciones de los flujos A y B (`requerimientos.md`, 4.4 y 4.5), no el mecanismo para guardarlas.

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
