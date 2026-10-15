# ADR-0011: Monolito modular con arquitectura hexagonal

**Fecha**: 2026-10-10
**Estado**: accepted (aprobado por el equipo el 2026-10-12)
**Decisores**: equipo de desarrollo (propuesta de `arquitectura.md`, sección 8.2; aprobada por Richard)

## Contexto

`CLAUDE.md` pide que el dominio no dependa de Spring, JPA ni Google. El sistema tiene 8 módulos de negocio con integraciones externas (Drive, correo) que deben poder reemplazarse por versiones falsas en las pruebas.

## Decisión

Un solo backend desplegable, dividido en módulos (`usuarios`, `flujos`, `pedidos`, `historial`, `archivos`, `notificaciones`, `plazos`, `bodegas` y `compartido`), con puertos y adaptadores. Se aplican las reglas R1 a R9 de `arquitectura.md`. R5 a R8 se verifican con pruebas de dominio y de integración.

**Propuesta asociada (no aprobada todavía):** verificar R1 a R4 y R9 con pruebas automáticas (ArchUnit). La fase 1 la incluye como propuesta; si el equipo la descarta, esas reglas quedan solo para la revisión de código.

## Alternativas consideradas

### Capas tradicionales de Spring (controller, service, repository)
- **Pros**: más conocida y con menos clases.
- **Contras**: el dominio queda acoplado a Spring y a JPA.
- **Por qué no**: contradice la regla de `CLAUDE.md`.

### Microservicios
- **Pros**: despliegue independiente por módulo.
- **Contras**: complejidad operativa desproporcionada para unos 10 usuarios.
- **Por qué no**: no hay necesidad de escalar por separado.

### Solo revisión de código, sin ArchUnit
- **Pros**: menos configuración.
- **Contras**: las reglas se rompen sin que nadie lo note.
- **Por qué no**: se propone ArchUnit; el equipo puede descartarlo.

## Consecuencias

### Positivas
- El dominio se prueba sin Spring ni base de datos.
- Drive y el correo se cambian sin tocar las reglas de negocio.

### Negativas
- Más clases (puertos, adaptadores, mapeos) que en una arquitectura por capas.
