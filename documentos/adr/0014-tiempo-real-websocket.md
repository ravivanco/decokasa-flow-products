# ADR-0014: Actualización en tiempo real con WebSocket (STOMP)

**Fecha**: 2026-10-10
**Estado**: proposed
**Decisores**: equipo de desarrollo (propuesta de `arquitectura.md`, 4.9 y 8.2)

## Contexto

El documento del cliente incluye en su stack "WebSocket (STOMP) de Spring" para que "los checks se vean al instante" (`requerimientos.md`, 1.6). El PRD lo clasifica como Should. El "stack fijo" de `CLAUDE.md` no lo menciona.

## Decisión (propuesta)

Desde la fase 5, el backend publica los cambios de pedido por WebSocket (STOMP) mediante el puerto `PublicadorTiempoReal`. El frontend invalida las consultas afectadas al recibir un mensaje.

## Alternativas consideradas

### Consultas periódicas del frontend (polling)
- **Pros**: más simple y sin conexiones persistentes.
- **Contras**: retraso de unos segundos y más carga sobre la API.
- **Por qué no se descarta**: con unos 10 usuarios es viable; se decide en la fase 5.

## Consecuencias

### Positivas
- Un check se ve en otras pantallas sin recargar.

### Negativas
- Conexiones persistentes que el servidor y el proxy deben soportar.

## Diferencias entre fuentes (sin resolver)

- Aparece en el stack recomendado del cliente (`requerimientos.md`, 1.6) y en el paso 1.3 del manual (dependencia `websocket`), pero no en el "stack fijo" de `CLAUDE.md`. Si se aprueba, conviene sumarlo al stack de `CLAUDE.md`.
