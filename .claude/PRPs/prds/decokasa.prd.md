# Decokasa — Sistema de seguimiento de pedidos de importación

> Fuente: `documentos/requerimientos.md` (incluidas las respuestas del cliente del 2026-10-09). Apoyo: `documentos/auditoria-demo.md`. Este PRD no agrega requisitos: lo que el cliente no definió queda como **TBD** o en "Open Questions".

## Problem Statement

Compras, marketing y logística de DECOKASA S.A.S. coordinan cada pedido de importación entre Ecuador y China sin un registro único: no se sabe en qué etapa está un pedido, quién lo tiene ni si venció su plazo. Los errores de codificación detectados tarde obligan a devolver documentos y alargan el ciclo, que con los tiempos estándar planificados ya suma 99 días contra una meta de 90.

## Evidence

- El cliente usa hoy una calculadora de cronograma con recordatorios manuales en Slack, con errores conocidos: canal inexistente, recordatorios rechazados y comandos que se copian juntos (`requerimientos.md`, sección 1.8).
- Los tiempos estándar suman 99 días contra una meta de 90 (`requerimientos.md`, sección 1.8).
- El proceso del cliente define devoluciones frecuentes: Revisión → Codificación y China → Codificación (`requerimientos.md`, secciones 1.4 y 1.5).
- Volumen de pedidos al mes, cantidad de devoluciones y atrasos: **TBD — validar con datos históricos de compras.**

## Proposed Solution

Una aplicación web interna donde cada pedido avanza por etapas. En cada etapa el responsable revisa, sube evidencias, escribe observaciones y aprueba, rechaza o devuelve. El servidor guarda cada transición como historial auditable, copia los archivos a Google Drive, envía alertas por Gmail al admin y mide los plazos en días hábiles con hora de Ecuador. El flujo se define por datos y no fijo en código, porque ya hay dos variantes (A y B) y viene un segundo país. El demo importado de Google AI Studio sirve de base visual: se reutilizan 14 componentes y se reescribe la lógica en el backend (`auditoria-demo.md`, sección 8).

## Key Hypothesis

We believe un flujo de pedidos por etapas con responsables, plazos, checks, evidencias y alertas por Gmail will reducir los atrasos y las devoluciones for el equipo de compras y logística de DECOKASA Ecuador.
We'll know we're right when el tiempo de pedido a llegada baje de 99 días a la meta que fije el cliente (hoy 90; **TBD** la meta ajustada) y el 100 % de los pedidos tenga su historial completo en el sistema.

## What We're NOT Building

- Proceso de Colombia en la primera versión — el cliente lo dejó para más adelante. El modelo de datos lo permite desde el inicio.
- Integración con la API de Claude en la primera versión — se implementará más adelante; el caso de uso está sin definir.
- Recordatorios por Slack — la web reemplaza la calculadora actual y sus recordatorios.
- App móvil — solo web.
- Validación del contenido de los documentos — solo se suben; cada usuario responde por lo que sube.
- Seguimiento producto por producto — el detalle va en los archivos; el sistema sigue el estado del pedido.
- Portal para proveedores, navieras o agentes de aduana — no se menciona en los requerimientos.
- Pagos, facturación o inventario de bodega — fuera del proceso descrito.

## Success Metrics

| Metric | Target | How Measured |
|--------|--------|--------------|
| Días de pedido a llegada | Meta configurable: hoy 90 contra 99 planificados; **TBD** la meta ajustada | Fecha del check de Aduana menos fecha de creación del pedido |
| Codificación y Revisión dentro de plazo | 100 % en ≤ 24 h hábiles cada una; con urgencia, 4 / 12 / 24 h hasta volver a Análisis | Tiempo entre entrada y salida de cada etapa |
| Análisis dentro de plazo | 100 % en ≤ 3 días hábiles | Tiempo entre entrada y check de la etapa |
| Embarque dentro de plazo | 100 % en ≤ 10 días hábiles | Tiempo entre entrada y check de la etapa |
| Autorización de salida y Bodega dentro de plazo | 100 % en ≤ 24 h hábiles cada una | Tiempo entre entrada y check de cada etapa |
| Devoluciones a Codificación por pedido | **TBD** (sin línea base) | Conteo de devoluciones en el historial |
| Pedidos con historial completo | 100 % | Pedidos con check y evidencia en cada etapa |

## Open Questions

Con respuesta ambigua del cliente (`requerimientos.md`, "Por confirmar con el cliente"). Mientras tanto se usa la propuesta indicada, como valor configurable:

- [ ] Meta de días ("se ajusta"): ¿cuál es la nueva meta o qué etapa se acorta? Propuesta: meta configurable, se mantiene 90.
- [ ] Feriados ("laborables"): ¿se descuentan los feriados de Ecuador y de China (Año Nuevo chino)? Propuesta: calendario de feriados de Ecuador editable por el admin.
- [ ] Recálculo en aduana ("si"): ¿David escribe la nueva fecha o el sistema la calcula? Propuesta: David escribe la fecha y el sistema recalcula las siguientes.
- [ ] Calculadora y Slack ("si"): ¿la web las reemplaza por completo? Propuesta: sí.
- [ ] Alertas ("el admin"): ¿el responsable de la etapa también recibe correo? Propuesta: correo al admin y al responsable; el admin puede apagarlo.
- [ ] Urgencias 4 / 12 / 24 h: ¿son horas hábiles o corridas? Propuesta: hábiles.
- [ ] Archivos ("si esta bien"): ¿qué formatos se aceptan? Propuesta: PDF, Word, Excel, imágenes y ZIP, con 100 MB por archivo.
- [ ] Número de pedido ("uno diferente y más profesional"): ¿aprueban `DK-EC-2026-0001`?
- [ ] Asistente de compras en China ("asistente de compras en China nada más"): ¿qué ve en el sistema? Propuesta: solo consulta, sin acciones.

Sin definir en los requerimientos o detectadas en la auditoría:

- [ ] Plazo de Fabricación (A5 / B6): **TBD**. La calculadora usa 8 + 31 días.
- [ ] Plazo de Incidencias (A10 / B11): **TBD**.
- [ ] ¿Los 45 días de tránsito son hábiles o corridos? La calculadora original usa días corridos (`auditoria-demo.md`, 2.2).
- [ ] ¿Los correos de los usuarios demo son reales, y el repositorio de GitHub es privado? (`auditoria-demo.md`, S5).
- [ ] Recuperación de contraseña por correo: **TBD**.
- [ ] Quién tiene acceso directo a la carpeta de Drive: **TBD**.
- [ ] Logo y colores de la marca (necesarios para el sistema de diseño).
- [ ] Creación de la cuenta de servicio de Google Workspace para Drive y Gmail (el cliente confirmó que tiene Workspace).
- [ ] Proceso de Colombia: etapas, responsables, plazos y bodegas (el cliente lo dejó para más adelante).
- [ ] Caso de uso de la API de Claude (el cliente lo dejó para más adelante).
- [ ] Alojamiento, dominio, respaldos y monitoreo (el cliente lo dejó para el final).
- [ ] ¿Proveedores, navieras o clientes finales usarán el sistema? Hoy no aparecen como usuarios.

---

## Users & Context

**Primary User**
- **Who**: Mirian Franco, admin. Crea los pedidos del flujo A, gestiona usuarios y roles, recibe todas las alertas por Gmail y puede actuar o reasignar en cualquier etapa. Trabaja en Ecuador.
- **Current behavior**: arma el pedido con una plantilla, coordina por mensajes con Ecuador y China, y sigue los plazos con la calculadora y recordatorios de Slack.
- **Trigger**: una necesidad de mercadería, el avance de un pedido o el vencimiento de un plazo.
- **Success state**: sabe en segundos dónde está cada pedido, quién lo tiene, qué pasó antes y si va tarde, sin preguntarle a nadie.

**Otros usuarios con etapas a cargo**

| Persona | Rol | Etapas | Ubicación |
| --- | --- | --- | --- |
| Andrea Quishpe | Marketing / Codificación | A2, B3 | Ecuador |
| Marcela Catucuamba | Asistente de compras | A3, B2, B4 | Ecuador |
| David Túlcan | Compras China | A4–A7, B1, B5–B8 | China |
| Anderson Enriquez | Logística | A8–A10, B9–B11; catálogo de bodegas | Ecuador |
| Andrea Collaguazo | Asistente de compras en China | Ninguna (consulta, **TBD** qué ve) | China |

**Job to Be Done**
When un pedido de importación avanza entre Ecuador y China, I want to ver en qué etapa está, quién lo tiene y si su plazo venció, so I can actuar antes de que el atraso llegue a la fecha de llegada.

**Non-Users**
Proveedores, navieras, agentes de aduana y clientes finales: no aparecen como usuarios en los requerimientos (**TBD** confirmar con el cliente). Los usuarios de Colombia quedan fuera de la primera versión.

---

## Solution Detail

### Core Capabilities (MoSCoW)

| Priority | Capability | Rationale |
|----------|------------|-----------|
| Must | Login con correo, usuario o teléfono + contraseña; dashboard por rol | Pedido explícito |
| Must | Gestión de usuarios y roles por el admin; el admin actúa o reasigna en cualquier etapa | Pedido explícito; confirmado. Falta en el demo |
| Must | Pedido con número secuencial único (formato propuesto `DK-EC-2026-0001`, pendiente de aprobación) | Base del rastreo |
| Must | Flujo A (10 etapas + Fin automático) y flujo B (11 + Fin automático) con check ✓/✗, archivos y observaciones | Núcleo del proceso |
| Must | Devoluciones a Codificación: por error, y por producto nuevo con urgencia 4 / 12 / 24 h hasta volver a Análisis | Pedido explícito; el demo solo tiene la devolución con urgencia |
| Must | Documentos obligatorios exigidos antes del check (por ejemplo Packing List / BL en Embarque y bodega elegida en Bodega) | Requerimientos 4.4; el demo no los exige |
| Must | Plazos en días hábiles con hora de Ecuador: 24 h en Codificación, Revisión, Salida y Bodega; 3 días en Análisis; 10 días en Embarque | Confirmado por el cliente |
| Must | Archivos en Google Drive (100 MB por archivo) mediante cuenta de servicio desde el servidor | Pedido explícito; debe funcionar desde China |
| Must | Alerta por Gmail al admin en cada etapa y en cada vencimiento, con el motivo visible en la etapa | Confirmado por el cliente |
| Must | Búsqueda de pedido con flujo, etapa actual y resumen de las etapas anteriores | Pedido explícito |
| Must | Línea de tiempo horizontal del pedido, tipo rastreo de envío, en cada dashboard | Pedido del cliente |
| Must | Historial de solo inserción: quién, cuándo, acción, observación y archivos | Invariante de los requerimientos |
| Should | Cronograma contra una meta configurable: fin de semana pasa a lunes y un retraso arrastra las etapas siguientes | Reemplaza la calculadora actual y Slack |
| Should | Recálculo de la fecha de llegada por demora en aduana | Pedido explícito |
| Should | Catálogo de bodegas que mantiene Anderson | Confirmado |
| Should | Actualización en tiempo real de los checks (WebSocket) | En el stack propuesto |
| Could | Feriados de Ecuador y China en el cálculo de plazos | Pendiente de confirmar |
| Won't (v1) | Proceso de Colombia | Se verá más adelante |
| Won't (v1) | Integración con la API de Claude | Se implementará más adelante |
| Won't (v1) | Recordatorios por Slack y app móvil | La web reemplaza Slack; solo web |

### MVP Scope

Ecuador con los flujos A y B completos: login real y roles, creación de pedidos, todas las etapas con sus checks, devoluciones y documentos obligatorios, archivos en Drive, alertas por Gmail al admin, plazos en días hábiles, búsqueda, línea de tiempo y dashboards por rol. Corresponde a las fases 1 a 6 y a la salida a producción de la fase 8.

### User Flow

Flujo A: Mirian crea el pedido (A1) → Andrea codifica (A2) → Marcela aprueba o rechaza (A3; ✗ vuelve a A2) → David analiza en 3 días (A4; devuelve a A2 por error o por producto nuevo con urgencia) → Fabricación (A5) → Embarque con Packing List / BL en 10 días (A6) → Aduana, con posible demora y recálculo (A7) → Anderson autoriza la salida (A8) → elige bodega (A9) → sube los documentos en Incidencias (A10) → Fin automático (A11). En cada paso: alerta por Gmail al admin, plazo visible, línea de tiempo actualizada y evento en el historial.

Flujo B: David crea el pedido propuesto (B1) → Marcela revisa el producto propuesto (B2; ✗ vuelve a B1) → desde B3 sigue igual que el flujo A desde A2, con devoluciones de B5 a B3 y Fin automático en B12.

---

## Technical Approach

**Feasibility**: HIGH. Es un flujo de trabajo con estados, sin algoritmos novedosos. Lo difícil son las integraciones con Google y el acceso desde China.

**Architecture Notes**
- Stack fijo (ADR pendiente, paso 0.7): React 19 + Vite + TypeScript + Tailwind 4 + shadcn/ui + TanStack Query + React Router + React Hook Form + Zod en el frontend; Spring Boot 3 + Java 21 + Gradle + Spring Security (JWT + BCrypt) + Spring Data JPA + Flyway + PostgreSQL + Docker en el backend.
- Arquitectura hexagonal: el dominio (pedidos, flujos, transiciones, plazos) no depende de Spring, JPA ni Google. Drive y Gmail son adaptadores con versión falsa para las pruebas.
- Motor de flujos definido por datos (país × tipo → etapas, rol responsable, plazo, transiciones permitidas), con definiciones versionadas para no romper pedidos en curso.
- El navegador nunca llama a Google: el servidor sube a Drive y envía correos con su cuenta de servicio, con cola y reintentos.
- Permisos y eventos del historial validados en el servidor; fecha y hora del servidor en hora de Ecuador.
- El demo de AI Studio es solo frontend y en memoria. `utils/orderState.ts` y las definiciones de etapas de `data/initialData.ts` sirven de referencia de reglas para el motor (`auditoria-demo.md`, secciones 7 y 8).

**Technical Risks**

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Acceso a Google (Drive, Gmail) bloqueado desde China | H | Todo pasa por el servidor; David solo usa la web. Probar con David temprano |
| Latencia o bloqueo desde China hacia el servidor | M | Servidor sin dependencias de dominios de Google; probar en la fase 1 |
| Cuotas y permisos de la cuenta de servicio de Drive | M | Unidad compartida de Workspace; probar en la fase 3 |
| Archivos de 100 MB en conexiones lentas | M | Subida por partes con reanudación |
| Reglas de etapas que cambian a menudo | H | Flujo configurable por datos; cambios sin desplegar |
| Plazos sin definir (Fabricación, Incidencias, tránsito) | M | Valores configurables hasta que el cliente los defina |
| Seguridad del demo (contraseña en código, cambio de rol, permisos en el navegador) | H | No reutilizar esa lógica; login y permisos nuevos en el servidor (`auditoria-demo.md`, sección 10) |
| Accesibilidad del demo (modales, teclado, etiquetas, contraste) | H | Migrar cada componente a shadcn/ui corrigiendo lo listado en `auditoria-demo.md`, 7.4 |

---

## Implementation Phases

<!--
  STATUS: pending | in-progress | complete
  PARALLEL: phases that can run concurrently (e.g., "with 3" or "-")
  DEPENDS: phases that must complete first (e.g., "1, 2" or "-")
  PRP: link to generated plan file once created
-->

| # | Phase | Description | Status | Parallel | Depends | PRP Plan |
|---|-------|-------------|--------|----------|---------|----------|
| 1 | Base y acceso | Monorepo con frontend migrado y backend Spring Boot, Docker y PostgreSQL, login con correo, usuario o teléfono, usuarios y roles, dashboards vacíos por rol | pending | - | - | - |
| 2 | Motor de flujo y A1-A4 | Motor de flujos por datos, pedidos con número secuencial, etapas A1 a A4 con aprobar, rechazar y devolver (por error o con urgencia), historial de solo inserción | pending | with 3 | 1 | - |
| 3 | Drive y Gmail | Puertos y adaptadores de archivos (Drive, 100 MB) y notificaciones (Gmail al admin), cola con reintentos, pantalla de archivos y de correos enviados | pending | with 2 | 1 | - |
| 4 | Etapas A5-A11 | Fabricación, embarque con Packing List / BL, aduana con demora y recálculo, salida, bodega obligatoria, incidencias y Fin automático | pending | - | 2, 3 | - |
| 5 | Seguimiento y plazos | Línea de tiempo horizontal en dashboards y búsqueda, plazos en días hábiles con hora de Ecuador, vencimientos con alerta al admin, cronograma contra meta configurable, tiempo real | pending | with 6 | 4 | - |
| 6 | Flujo B | Configuración del flujo B (11 etapas + Fin) sobre el motor, creación de pedidos por Compras China, bandeja de productos propuestos | pending | with 5 | 4 | - |
| 7 | Colombia y API de Claude | Selección de país para admin y usuarios, flujo colombiano, caso de uso de IA. Espera las definiciones del cliente; puede hacerse después de la fase 8 | pending | - | 5, 6 | - |
| 8 | Producción | Alojamiento, CI, imágenes de producción, HTTPS, respaldos, monitoreo, auditoría final, prueba de aceptación con los 6 usuarios y documentación de entrega | pending | - | 5, 6 | - |

### Phase Details

**Phase 1: Base y acceso**
- **Goal**: que cada persona entre al sistema y vea su dashboard.
- **Scope**: mover el demo a la estructura nueva quitando dependencias sin usar; backend Spring Boot con Gradle; Docker Compose con PostgreSQL; migración inicial de usuarios, roles y países; login con JWT y BCrypt; gestión de usuarios y roles por el admin; rutas protegidas y dashboards vacíos por rol. Resuelve S1, S2, S3, S7 y S8 de `auditoria-demo.md`.
- **Success signal**: los 6 usuarios entran con correo, usuario o teléfono; Mirian crea un usuario y le asigna un rol; ningún usuario abre el dashboard de otro rol ni por la pantalla ni por la API.

**Phase 2: Motor de flujo y A1-A4**
- **Goal**: que un pedido real recorra desde su creación hasta el check de Análisis.
- **Scope**: definiciones de flujo y etapa versionadas, pedido con número secuencial, transiciones A1 a A4 (aprobar, rechazar con observación, devolver por error, devolver por producto nuevo con urgencia hasta volver a A4), permisos por rol y etapa en el servidor, historial de solo inserción, API según el contrato OpenAPI, bandejas de Codificación, Revisión y Análisis.
- **Success signal**: un pedido pasa por un rechazo de Revisión y por una devolución urgente de China, llega a Análisis aprobado, y el historial muestra cada acción con su autor y su hora.

**Phase 3: Drive y Gmail**
- **Goal**: que los archivos lleguen a Drive y los correos al admin sin perder nada si Google falla.
- **Scope**: puertos de archivos y notificaciones con adaptadores reales y falsos, cola con reintentos, subida por partes de hasta 100 MB con validación en el servidor, pantalla de archivos por etapa y registro de correos enviados y fallidos. Requiere la cuenta de servicio de Google Workspace.
- **Success signal**: un archivo de 100 MB llega a la carpeta correcta de Drive, cada transición genera su correo al admin, y con el adaptador falso fallando todo se reintenta sin perderse.

**Phase 4: Etapas A5-A11**
- **Goal**: que un pedido del flujo A llegue de punta a punta.
- **Scope**: Fabricación con check al terminar; Embarque con Packing List / BL obligatorios; Aduana con reporte de demora y recálculo de las fechas siguientes; Autorización de salida; Bodega con bodega obligatoria y catálogo que mantiene Anderson; Incidencias con los documentos de Anderson; Fin automático.
- **Success signal**: la prueba de punta a punta del flujo A pasa de A1 a Fin; Embarque sin BL y Bodega sin bodega elegida se rechazan; una demora en aduana mueve todas las fechas siguientes.

**Phase 5: Seguimiento y plazos**
- **Goal**: saber dónde está cada pedido y si va tarde, sin preguntar.
- **Scope**: línea de tiempo horizontal en dashboards y búsqueda, cálculo de plazos en días hábiles con hora de Ecuador, detección de vencimientos con correo al admin y motivo visible, cronograma contra meta configurable que reemplaza la calculadora y Slack, actualización en tiempo real.
- **Success signal**: el cronograma del caso base de la calculadora da las mismas fechas con la regla de días hábiles; un vencimiento llega al admin con su motivo; un check aparece en otra pantalla abierta sin recargar.

**Phase 6: Flujo B**
- **Goal**: que Compras China proponga pedidos y estos recorran su flujo.
- **Scope**: configuración del flujo B sobre el motor (B2 ✗ vuelve a B1; B4 ✗ y B5 devuelven a B3), creación de pedidos por David, bandeja de productos propuestos para Marcela, dashboard propio de David.
- **Success signal**: un pedido creado por David llega a Fin, y las pruebas de punta a punta de los flujos A y B pasan juntas.

**Phase 7: Colombia y API de Claude**
- **Goal**: sumar Colombia y el caso de uso de IA cuando el cliente los defina.
- **Scope**: selección de país al entrar para el admin y los usuarios, flujos de Colombia con numeración propia, integración con la API de Claude desde el servidor. **Bloqueada** hasta que el cliente defina el proceso colombiano y el uso de la IA; puede hacerse después de la fase 8.
- **Success signal**: un pedido de Colombia recorre su flujo sin romper los de Ecuador, y la IA funciona con el adaptador real mientras las pruebas usan el falso.

**Phase 8: Producción**
- **Goal**: poner el sistema en uso real con los 6 usuarios.
- **Scope**: elección de alojamiento con el cliente; integración continua; imágenes de producción; HTTPS, cabeceras de seguridad y CSP; respaldos con prueba de restauración; monitoreo; auditoría final de seguridad, rendimiento y accesibilidad; prueba de aceptación por rol (David desde China); manuales de uso y de operación. Resuelve S10 y S11 de `auditoria-demo.md`.
- **Success signal**: producción con HTTPS, respaldos probados y monitoreo; auditoría final sin hallazgos críticos ni altos; los 6 usuarios aprueban la prueba de aceptación.

### Parallelism Notes

- **Fases 2 y 3 en paralelo:** solo comparten la base de la fase 1. El motor trabaja contra los puertos de archivos y notificaciones con adaptadores falsos, mientras la fase 3 construye los reales. Conviene trabajarlas en ramas o worktrees separados.
- **Fases 5 y 6 en paralelo:** las dos dependen del flujo A completo (fase 4). El seguimiento lee el historial y los plazos; el flujo B agrega configuración sobre el mismo motor.
- **Fase 7 después de la 8 si hace falta:** depende de definiciones del cliente que no existen todavía. El sistema puede salir a producción solo con Ecuador (fase 8) y sumar Colombia y la IA después.

---

## Decisions Log

| Decision | Choice | Alternatives | Rationale |
|----------|--------|--------------|-----------|
| Stack | React + Spring Boot + PostgreSQL | — | Lo eligió el cliente |
| Arquitectura del backend | Hexagonal (puertos y adaptadores) | Capas acopladas a Spring y Google | Dominio aislado y probable sin Google ni base de datos |
| Autenticación | Propia (JWT + BCrypt) | Google Sign-In | China no puede depender de Google |
| Archivos | Drive mediante cuenta de servicio desde el servidor | Subida directa desde el navegador | El navegador nunca llama a Google |
| Flujo | Configurable por datos, con versiones | Etapas fijas en código | Dos flujos y dos países |
| Alertas | Gmail al admin | Slack | Pedido del cliente; la web reemplaza la calculadora y Slack |
| Plazos | Días hábiles con hora de Ecuador; fin de semana pasa al lunes; los retrasos arrastran las fechas | Días calendario | Confirmado por el cliente |
| Documentos | Solo se suben, sin validar el contenido | Formulario estructurado | Confirmado: cada usuario responde por lo que sube |
| Plataforma | Solo web | App móvil | Confirmado |
| País inicial | Ecuador | — | Pedido del cliente |
| Repositorio | Monorepo `frontend/` + `backend/`; ramas de fase desde `develop`, PR hacia `develop`, `main` solo para versiones | Repositorios separados | Decisión del equipo (`documentos/manual-paso-a-paso.md`) |
| Demo de AI Studio | Reutilizar 14 componentes, reescribir 11, descartar 7 | Empezar el frontend de cero | Resultado de `auditoria-demo.md`, sección 8 |

---

## Research Summary

**Market Context**
No se hizo investigación de mercado: es una herramienta interna con un proceso definido por el cliente. **TBD** si se quiere comparar con sistemas de seguimiento de importaciones existentes.

**Technical Context**
- El demo de AI Studio cubre la forma del sistema: de 64 requisitos revisados, 31 cumplen, 28 parcialmente y 4 faltan. Todo vive en memoria, sin backend (`auditoria-demo.md`, secciones 1 a 6).
- Errores del demo que no deben copiarse: los botones del flujo B mal asignados en B3 y B4, los documentos obligatorios que no se exigen y la urgencia que se borra al primer avance (`auditoria-demo.md`, sección 6).
- Calidad técnica: 9/20 en diseño y accesibilidad; 0 críticos en la revisión React; 0 vulnerabilidades en dependencias (`auditoria-demo.md`, sección 7).
- Seguridad: 4 hallazgos críticos, todos propios de un demo sin backend; se resuelven en las fases 1, 2, 3 y 8 (`auditoria-demo.md`, sección 10).

---

*Generated: 2026-10-10*
*Status: DRAFT - needs validation*
