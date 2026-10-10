# Diagramas — Decokasa

**Fase 0, paso 0.10.** Diagramas Mermaid del sistema. Cada archivo `.mmd` es la fuente; GitHub y VS Code los muestran directamente.

**Marcas:** **[P]** = propuesta del equipo, no aprobada (ADR en estado proposed). **⏸** = pendiente del cliente. Sin marca = requisito o ADR accepted.

| # | Archivo | Tipo | Muestra | Fuentes |
| --- | --- | --- | --- | --- |
| 1 | `01-c4-contexto.mmd` | C4 de contexto | Usuarios, el sistema, Google Drive, correo y la API de Claude (fase 7) | `requerimientos.md` 4.2 y 4.3; ADR-0002, 0003, 0005, 0013 |
| 2 | `02-c4-contenedores.mmd` | C4 de contenedores | Frontend, backend, tareas programadas, PostgreSQL y almacén temporal | `arquitectura.md` 1, 3 y 4.9; ADR-0001, 0011, 0012, 0014 |
| 3 | `03-estados-flujo-a.mmd` | Estados | A1 a A11 con aprobaciones, rechazos, devoluciones, demora y Fin automático | `requerimientos.md` 4.4; `arquitectura.md` 4.2 |
| 4 | `04-estados-flujo-b.mmd` | Estados | B1 a B12 con las devoluciones B2→B1, B4→B3 y B5→B3 | `requerimientos.md` 4.5 |
| 5 | `05-secuencia-transicion.mmd` | Secuencia | Una acción de etapa (rechazo en A3), con el error 409 y la cola de correos | `arquitectura.md` 6; `openapi.yaml` |
| 6 | `06-entidad-relacion.mmd` | Entidad-relación | Las 20 tablas, sus relaciones y las columnas clave | `modelo-datos.md` 2 y 3 |

**Propuestas que aparecen en los diagramas:** monolito hexagonal (ADR-0011), outbox y transacción fuera del núcleo (ADR-0012), canal de correo (ADR-0013), WebSocket (ADR-0014), tablas `secuencia_pedido`, `feriado` y `token_renovacion`.

**Pendientes que aparecen:** qué ve la asistente de China, destinatarios de las alertas, caso de uso de la API de Claude, plazos de Fabricación e Incidencias, documentos de Incidencias, nueva fecha en aduana, tránsito de 45 días, urgencias hábiles o corridas, rechazo durante una urgencia, si el admin crea pedidos del flujo B y el formato del número de pedido.

**Validación:** todos se renderizaron sin errores con Mermaid CLI (`npx @mermaid-js/mermaid-cli`) el 2026-10-10. Las imágenes de prueba no se guardan en el repositorio. Si GitHub no muestra los C4 (que en Mermaid todavía son experimentales), conviene una versión alternativa en `flowchart`.

Los diagramas de estados resumen las transiciones; la tabla completa con requisitos está en `arquitectura.md` 4.2 y en `openapi.yaml` (`ejecutarAccionEtapa`).
