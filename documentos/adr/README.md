# Registro de decisiones de arquitectura (ADR)

Cada ADR registra una decisión, su contexto, las alternativas y sus consecuencias. Formato: Michael Nygard, adaptado; plantilla en `template.md`.

**Estados:**
- **accepted (cliente):** lo escribió o lo respondió el cliente: el documento `Latinoamérica.docx` o las respuestas del 2026-10-09, recogidas en `requerimientos.md`.
- **accepted (equipo):** decisión técnica que aprobó el equipo (Richard), con fecha. No es una respuesta del cliente; la columna "Origen" lo indica.
- **proposed:** decisión o propuesta del equipo. Se puede cambiar hasta que el equipo la apruebe.

El "Registro de decisiones" de `requerimientos.md` está marcado como "BORRADOR — falta validar con el cliente". Por eso, de sus 9 entradas, solo 8 quedan como accepted (cliente). La del flujo configurable (ADR-0004) es una elección técnica del equipo: estuvo como proposed hasta que Richard la aprobó como decisión del equipo el 2026-10-15 (accepted (equipo)).

## Índice

| ADR | Título | Estado | Fecha | Origen |
| --- | --- | --- | --- | --- |
| [0001](0001-stack-tecnologico.md) | Stack React + Spring Boot + PostgreSQL | accepted | 2026-10-10 | Registro de decisiones |
| [0002](0002-autenticacion-propia-jwt.md) | Autenticación propia con JWT y BCrypt | accepted | 2026-10-10; 2026-10-12; 2026-10-13 | Registro de decisiones. Valores de bloqueo, JWT y renovación aprobados por el equipo el 2026-10-12; token de renovación en cookie `HttpOnly` (D3b) aprobado el 2026-10-12; `Origin` → 403 aprobado el 2026-10-13. Correcciones de la revisión de seguridad del paso 1.7 (2026-10-13): A1 (403 aprobado), A2, A4, D10 (versión de usuarios) y N1 (orden de bloqueos). Versionado de roles: pendiente |
| [0003](0003-drive-cuenta-servicio.md) | Archivos en Google Drive mediante cuenta de servicio | accepted | 2026-10-10 | Registro de decisiones |
| [0004](0004-flujo-configurable-por-datos.md) | Flujo de etapas configurable por datos | accepted (equipo) | 2026-10-10; 2026-10-15 | Registro de decisiones (elección del equipo). Aprobado por Richard el 2026-10-15 como decisión técnica del equipo (P-02 del plan de la fase 2): definiciones versionadas de flujo, etapas y transiciones en datos. No es una respuesta del cliente |
| [0005](0005-alertas-gmail-admin.md) | Alertas por correo de Gmail al admin | accepted | 2026-10-10 | Registro de decisiones |
| [0006](0006-plazos-dias-habiles-ecuador.md) | Plazos en días hábiles con hora de Ecuador | accepted | 2026-10-10 | Registro de decisiones |
| [0007](0007-documentos-sin-validacion.md) | Los documentos solo se suben, sin validar su contenido | accepted | 2026-10-10 | Registro de decisiones |
| [0008](0008-solo-web.md) | Solo aplicación web | accepted | 2026-10-10 | Registro de decisiones |
| [0009](0009-ecuador-primero.md) | Ecuador primero; Colombia después | accepted | 2026-10-10 | Registro de decisiones |
| [0010](0010-monorepo-gradle.md) | Monorepo frontend/ y backend/, backend con Gradle | accepted (equipo) | 2026-10-12 | Manual y `CLAUDE.md`. El nombre del paquete base sigue como propuesta |
| [0011](0011-monolito-modular-hexagonal.md) | Monolito modular con arquitectura hexagonal | accepted (equipo) | 2026-10-12 | `arquitectura.md`, 8.2. ArchUnit sigue como propuesta |
| [0012](0012-transacciones-y-outbox.md) | Transacciones fuera del núcleo y cola de salida (outbox) | accepted (equipo) en transacciones; outbox proposed | 2026-10-12 | `arquitectura.md`, 8.2. El outbox se decide en la fase 3 |
| [0013](0013-canal-de-correo.md) | Canal de correo: Gmail API o SMTP de Workspace | proposed | 2026-10-10 | `arquitectura.md`, 8.2 |
| [0014](0014-tiempo-real-websocket.md) | Actualización entre pantallas con consultas periódicas (STOMP descartado por ahora) | accepted (equipo, provisional, 2026-10-20); intervalo de 30 s aceptado por Richard el 2026-10-21; no es respuesta del cliente; la demora de hasta el intervalo no satisface "al instante" sin su aceptación | 2026-10-20 | `arquitectura.md`, 4.9 y 8.2 |
| [0015](0015-datos-iniciales-fuera-de-git.md) | Países y roles en migraciones; usuarios reales y secretos fuera de git | accepted (equipo) | 2026-10-12 | Plan de la fase 1 (D5); repositorio público |
| [0016](0016-spring-boot-4.md) | Spring Boot 4.1 en lugar de Spring Boot 3 | accepted (equipo) | 2026-10-12 | Paso 1.3: la rama 3.x quedó sin soporte gratuito el 2026-06-30 (API oficial de spring.io) |
| [0017](0017-google-oauth-usuario-gmail-personal.md) | Gmail personal y Mi unidad con OAuth de usuario, cliente web local | accepted (equipo, Richard) | 2026-10-17 | Paso 3.1; sustituye la variante Workspace de 0003 y resuelve el canal de 0013 |
| [0018](0018-etapa-de-llegada-explicita.md) | La etapa de llegada a aduana se marca de forma explícita en la definición del flujo | accepted (equipo) | 2026-10-19 | Paso 4.3 (fase 4): pedido por Richard tras la revisión del paso; marca `definicion_etapa.registra_llegada` (V11), solo A7 en el flujo A vigente. No es una respuesta del cliente |
| [0019](0019-habilitacion-por-definicion.md) | Las etapas operables se deciden con la definición del flujo de cada pedido | proposed (pendiente de revisión por Codex y de aceptación por Richard) | 2026-10-21 | Propuesta P6-B3-ADR de Codex (fase 6, 6.B3). Registra F6-D3 (paso 6.4), el guard de etapas automáticas y la creación por el rol de la primera etapa (F6-D2). Complementa 0004. No es una respuesta del cliente; P-14 sigue pendiente |
| [0020](0020-claude-api-puerto-asistente.md) | Claude API detrás del puerto `AsistenteIA`, con credenciales propias y sin escalado automático de modelo | accepted (equipo, Richard) solo para el piloto | 2026-10-22 | Fase 7 (7.1). Credenciales nuevas de Decokasa; modelo inicial `claude-haiku-5-5` sujeto a disponibilidad. No es una respuesta del cliente; sin implementar |
| [0021](0021-asistente-consulta-por-pedido.md) | El asistente responde sobre un solo pedido, con permisos comprobados en el servidor y referencias verificadas | accepted (equipo, Richard) solo para el piloto | 2026-10-22 | F7-D1 opción C y F7-D4. Sin implementar |
| [0022](0022-presupuesto-ia-reserva-atomica.md) | El consumo del asistente se limita con una reserva atómica del peor caso antes de cada llamada | accepted (equipo, Richard) solo para el piloto | 2026-10-22 | F7-D2 (límites del piloto). Sin implementar |
| [0023](0023-piloto-ia-aislado-y-conservacion.md) | El piloto del asistente se aísla con datos ficticios controlados y conserva los textos 30 días | accepted (equipo, Richard) solo para el piloto; producción y documentos reales pendientes (F7-D6) | 2026-10-22 | F7-D3 y F7-D5. La retención del proveedor es independiente y sigue pendiente. Sin implementar |
| [0024](0024-acceso-por-pais.md) | El acceso a pedidos y bodegas se limita a los países asignados hoy a cada persona, también al admin | accepted (equipo, Richard) en lo decidido; detalles de diseño pendientes de revisión por Codex | 2026-10-22 | Fase 7, paso 7.2 (V19). Colombia con los mismos flujos; países asignados por el admin; sin Colombia automático. No es una respuesta del cliente; sin verificar |
| [0025](0025-carpetas-drive-por-rol-y-persona.md) | Los documentos se copian a `Mi unidad/DECOKASA/{país}/{pedido}/{rol}/{persona}` | accepted (equipo, Richard); implementación sin verificar | 2026-10-24 | Rol de intervención de la subida (P-06) y persona por UUID; sin migración; no mueve copias anteriores. No es una respuesta del cliente |

## Propuestas de `arquitectura.md` (8.2) incluidas dentro de otro ADR

**Actualización 2026-10-17:** [ADR-0017](0017-google-oauth-usuario-gmail-personal.md), accepted (equipo, configuración acordada expresamente por Richard), establece OAuth de usuario con Gmail personal y Mi unidad para 3.1. Sustituye la variante Workspace de ADR-0003 y resuelve el canal Gmail API de ADR-0013 en este alcance. Los registros anteriores conservan su texto histórico; no se acreditan comprobaciones ni se autorizan operaciones reales.

| Propuesta | Dónde quedó |
| --- | --- |
| Bloqueo tras N intentos fallidos y JWT de vida corta | ADR-0002, "Decisiones del equipo" (aprobado el 2026-10-12: 5 intentos, 15 min; acceso de 15 min; renovación hasta 8 h con rotación) |
| Almacenamiento del token de renovación (cookie `HttpOnly`) | ADR-0002 (aprobado por el equipo el 2026-10-12, D3b) |
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
| WebSocket | Está en el stack recomendado del cliente, pero no en el stack fijo de `CLAUDE.md`. El equipo decidió que no entra antes de la fase 5; el manual (paso 1.3) ya no lo incluye en la fase 1 | 0014 |
| Bloqueo y JWT de vida corta | `requerimientos.md` 4.7 los incluye; versiones anteriores de ADR-0002 y `arquitectura.md` decían que el cliente no los pidió. No consta quién escribió 4.7; los valores los fijó el equipo | 0002 |
| Uno o varios roles por usuario | `requerimientos.md` 4.6 dice "rol(es)"; el contrato y el modelo de datos describían un rol. **Resuelto por el equipo (Richard, 2026-10-12), no por el cliente:** uno o varios roles con la tabla `usuario_rol` (`requerimientos.md` 4.7) | — |
| Destinatarios de las alertas | El registro dice "Gmail al admin"; el valor provisional de `requerimientos.md` y `arquitectura.md` agrega al responsable | 0005 |
| Tránsito de 45 días | La calculadora usa días corridos; `arquitectura.md` usa días hábiles como provisional | 0006 |

## Preguntas pendientes del cliente

Siguen abiertas y no se resuelven en estos ADR. Lista completa en `requerimientos.md` ("Por confirmar con el cliente"), `arquitectura.md` (8.1) y el PRD ("Open Questions").
