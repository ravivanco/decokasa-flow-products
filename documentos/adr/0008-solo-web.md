# ADR-0008: Solo aplicación web

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: accepted
**Decisores**: cliente (respuesta del 2026-10-09: "Solo web")

## Contexto

Se preguntó si hacía falta una versión móvil.

## Decisión

El sistema es una aplicación web, con un diseño que se adapta a pantallas pequeñas. No hay app móvil nativa.

## Alternativas consideradas

### App móvil nativa
- **Pros**: notificaciones push y acceso rápido.
- **Contras**: un segundo producto que construir y mantener.
- **Por qué no**: el cliente pidió solo web.

## Consecuencias

### Positivas
- Un solo frontend.

### Negativas
- Sin notificaciones push; las alertas llegan por correo (ADR-0005).

### Riesgos
- Ninguno identificado.
