# Registro de decisiones de arquitectura (ADR)

Cada ADR registra una decisión, su contexto, las alternativas y sus consecuencias. Formato: Michael Nygard, adaptado; plantilla en `template.md`.

**Estados:**
- **accepted:** lo escribió o lo respondió el cliente: el documento `Latinoamérica.docx` o las respuestas del 2026-10-09, recogidas en `requerimientos.md`.
- **proposed:** decisión o propuesta del equipo. Se puede cambiar hasta que el equipo la apruebe.

El "Registro de decisiones" de `requerimientos.md` está marcado como "BORRADOR — falta validar con el cliente". Por eso, de sus 9 entradas, solo 8 quedan como accepted. La del flujo configurable (ADR-0004) es una elección técnica del equipo y queda como proposed.

## Índice

| ADR | Título | Estado | Fecha | Origen |
| --- | --- | --- | --- | --- |
| [0001](0001-stack-tecnologico.md) | Stack React + Spring Boot + PostgreSQL | accepted | 2026-10-10 | Registro de decisiones |
| [0002](0002-autenticacion-propia-jwt.md) | Autenticación propia con JWT y BCrypt | accepted | 2026-10-10 | Registro de decisiones |
| [0003](0003-drive-cuenta-servicio.md) | Archivos en Google Drive mediante cuenta de servicio | accepted | 2026-10-10 | Registro de decisiones |
| [0004](0004-flujo-configurable-por-datos.md) | Flujo de etapas configurable por datos | proposed | 2026-10-10 | Registro de decisiones (elección del equipo) |
| [0005](0005-alertas-gmail-admin.md) | Alertas por correo de Gmail al admin | accepted | 2026-10-10 | Registro de decisiones |
| [0006](0006-plazos-dias-habiles-ecuador.md) | Plazos en días hábiles con hora de Ecuador | accepted | 2026-10-10 | Registro de decisiones |
| [0007](0007-documentos-sin-validacion.md) | Los documentos solo se suben, sin validar su contenido | accepted | 2026-10-10 | Registro de decisiones |
| [0008](0008-solo-web.md) | Solo aplicación web | accepted | 2026-10-10 | Registro de decisiones |
| [0009](0009-ecuador-primero.md) | Ecuador primero; Colombia después | accepted | 2026-10-10 | Registro de decisiones |
| [0010](0010-monorepo-gradle.md) | Monorepo frontend/ y backend/, backend con Gradle | proposed | 2026-10-10 | Manual y `CLAUDE.md` |
| [0011](0011-monolito-modular-hexagonal.md) | Monolito modular con arquitectura hexagonal | proposed | 2026-10-10 | `arquitectura.md`, 8.2 |
| [0012](0012-transacciones-y-outbox.md) | Transacciones fuera del núcleo y cola de salida (outbox) | proposed | 2026-10-10 | `arquitectura.md`, 8.2 |
| [0013](0013-canal-de-correo.md) | Canal de correo: Gmail API o SMTP de Workspace | proposed | 2026-10-10 | `arquitectura.md`, 8.2 |
| [0014](0014-tiempo-real-websocket.md) | Tiempo real con WebSocket (STOMP) | proposed | 2026-10-10 | `arquitectura.md`, 8.2 |

## Propuestas de `arquitectura.md` (8.2) incluidas dentro de otro ADR

| Propuesta | Dónde quedó |
| --- | --- |
| Bloqueo tras N intentos fallidos y JWT de vida corta | ADR-0002, "Propuestas asociadas" |
| Subcarpetas de Drive por año y etapa | ADR-0003, "Propuestas asociadas" |
| ArchUnit para R1 a R4 y R9 | ADR-0011 |
| Paquete base `com.decokasa.seguimiento` | ADR-0010 |

**No se registra como ADR:** los 31 días propuestos para Fabricación. No es una decisión de arquitectura sino un valor de negocio pendiente del cliente; está en `arquitectura.md`, 8.1, y en el ADR-0006 como pendiente.

## Diferencias entre fuentes (sin resolver)

Cada una está explicada en el ADR correspondiente.

| Tema | Qué dice cada fuente | ADR |
| --- | --- | --- |
| Estado del stack | `requerimientos.md` 1.6 lo titula "propuesto"; el registro de decisiones y `CLAUDE.md` lo tratan como elegido y fijo | 0001 |
| Versión de PostgreSQL | `requerimientos.md` dice "16 o 17"; `docker-compose.yml` usa la 17 | 0001 |
| Monorepo y Gradle | No aparecen en `requerimientos.md`; el manual y `CLAUDE.md` los presentan como stack fijo | 0010 |
| WebSocket | Está en el stack recomendado del cliente y en el manual (paso 1.3), pero no en el stack fijo de `CLAUDE.md` | 0014 |
| Destinatarios de las alertas | El registro dice "Gmail al admin"; el valor provisional de `requerimientos.md` y `arquitectura.md` agrega al responsable | 0005 |
| Tránsito de 45 días | La calculadora usa días corridos; `arquitectura.md` usa días hábiles como provisional | 0006 |

## Preguntas pendientes del cliente

Siguen abiertas y no se resuelven en estos ADR. Lista completa en `requerimientos.md` ("Por confirmar con el cliente"), `arquitectura.md` (8.1) y el PRD ("Open Questions").
