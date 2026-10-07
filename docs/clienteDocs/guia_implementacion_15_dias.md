# Guía de implementación — Decokasa Tracking (15 días hábiles)

**Desarrollador:** 1 persona · **Jornada:** lunes a viernes 8 h + sábados 09:00–12:30
**Inicio:** lunes 12/10/2026 · **Entrega:** viernes 30/10/2026
**Capacidad:** 15 días × 8 h + 2 sábados × 3,5 h = **127 horas**
**Stack:** React + TypeScript + Vite + Tailwind + shadcn/ui · Spring Boot 3 (Java 21) · PostgreSQL 16 · Flyway · Docker

> ⚠️ El alcance completo de la especificación (con backend real) son ~450 h. En 127 h entra la **Fase 1 (núcleo operativo)**. Lo marcado como **Fase 2** se entrega después (semanas 4–6).

---

## 0. Alcance

| Fase 1 — en 15 días (imprescindible) | Fase 2 — después |
|---|---|
| Login con usuario y contraseña, 7 roles y matriz de permisos | Módulo Productos nuevos (6 pasos) |
| Pedidos: crear (código automático), lista, filtros, rastreo | Reportes con gráficos y exportación PDF/Excel |
| Motor de cronograma en días hábiles + semáforo | Configuración editable de plazos |
| Línea de estados horizontal (11 etapas) | Selector ES / EN |
| Panel de etapa: check list, check, devolver, revertir | Copia automática a Google Drive |
| Documentos: subir, validar, versiones (sin borrar) | Alertas por correo |
| Observaciones por etapa | Gestión completa de usuarios (crear/editar en pantalla) |
| Pausar, cancelar, reabrir | Nueva versión del pedido |
| Productos del pedido + verificación en Bodega | |
| Auditoría (registro automático) | |
| Inicio: mis tareas + indicadores + alertas en el sistema | |
| Carga de los 7 pedidos en curso | |

---

## 1. Skills por fase

| Fase | Skills ya instaladas | Recomendadas para instalar |
|---|---|---|
| Requisitos | `ecc:plan-prd`, `ecc:plan`, `ecc:architecture-decision-records` | `mattpocock/skills@to-prd`, `addyosmani/agent-skills@spec-driven-development` |
| Base de datos | `ecc:postgres-patterns`, `ecc:database-migrations`, `ecc:jpa-patterns` | — |
| Backend | `ecc:springboot-patterns`, `ecc:springboot-security`, `ecc:java-coding-standards`, `ecc:api-design` | `github/awesome-copilot@java-springboot`, `addyosmani/agent-skills@security-and-hardening` |
| Frontend | `ecc:react-patterns`, `ecc:frontend-patterns`, `ecc:vite-patterns`, `impeccable:impeccable` | `shadcn-ui/ui@shadcn`, `vercel-labs/agent-skills@vercel-react-best-practices` |
| Pruebas | `ecc:springboot-tdd`, `ecc:springboot-verification`, `ecc:react-testing`, `ecc:e2e-testing`, `ecc:tdd-workflow` | `anthropics/skills@webapp-testing` |
| Revisión | `ecc:security-review`, `code-review`, agentes `ecc:java-reviewer`, `ecc:typescript-reviewer` | — |
| Despliegue | `ecc:docker-patterns`, `ecc:deployment-patterns` | — |

Instalación:
```bash
npx skills add mattpocock/skills@to-prd -g -y
npx skills add addyosmani/agent-skills@spec-driven-development -g -y
npx skills add github/awesome-copilot@java-springboot -g -y
npx skills add addyosmani/agent-skills@security-and-hardening -g -y
npx skills add shadcn-ui/ui@shadcn -g -y
npx skills add vercel-labs/agent-skills@vercel-react-best-practices -g -y
npx skills add anthropics/skills@webapp-testing -g -y
```

---

## 2. Plan día por día

### Semana 1 — Requisitos, base y núcleo

**Día 1 · lun 12/10 — Requisitos y diseño (8 h)** · skills: `ecc:plan-prd`, `to-prd`, `ecc:architecture-decision-records`
- [ ] Reunión de 1 h con Mirian para cerrar los 9 puntos pendientes (plazos 125 vs 99, pago, rol Junta, BL/contenedores, Commercial Invoice, producto nuevo, destinatarios de alertas, Drive, archivos de ejemplo).
- [ ] Generar el PRD a partir de `prompt_correccion_ai_studio.txt` + respuestas del cliente → `docs/PRD.md`.
- [ ] Historias de usuario por rol con criterios de aceptación → `docs/historias.md`.
- [ ] Modelo de datos (entidades abajo) y ADRs: JWT, almacenamiento de archivos, Drive en segundo plano.
- [ ] Usar la demo de AI Studio como prototipo aprobado (no rediseñar).
- **Entregable:** PRD firmado por Mirian.

**Día 2 · mar 13/10 — Proyecto base (8 h)** · skills: `ecc:springboot-patterns`, `ecc:vite-patterns`, `ecc:docker-patterns`
- [ ] Repositorio Git: `/backend` (Spring Boot 3, Java 21, Maven) y `/frontend` (Vite + React + TS + Tailwind + shadcn).
- [ ] `docker-compose.yml` con PostgreSQL 16.
- [ ] Flyway con la migración `V1__esquema_inicial.sql`.
- [ ] Tokens de diseño: `#FFD100`, `#515151`, `#6C6B6D`, `#FFFFFF`, semáforo.
- [ ] GitHub Actions: compilar y correr pruebas en cada push.
- **Entregable:** `docker compose up` levanta todo; pantalla vacía con encabezado amarillo.

**Día 3 · mié 14/10 — Login y permisos (8 h)** · skills: `ecc:springboot-security`, `security-and-hardening`
- [ ] Tabla `usuario` (BCrypt), enum `Rol`, enum `Permiso` y matriz `Rol → Permisos` en un solo lugar.
- [ ] Spring Security + JWT (expira en 8 h), endpoint `POST /api/auth/login`.
- [ ] Semilla de los 9 usuarios de la especificación.
- [ ] Frontend: pantalla de login amarilla, guardado de sesión, rutas protegidas, menú según rol.
- [ ] Pruebas: login correcto/incorrecto; cada rol solo accede a lo suyo.

**Día 4 · jue 15/10 — Pedidos y motor de cronograma (8 h)** · skills: `ecc:jpa-patterns`, `ecc:springboot-tdd`
- [ ] Entidades `Pedido`, `Etapa` (catálogo de 11), `PedidoEtapa` (estado, fechas estimada/real, responsable).
- [ ] `CronogramaService`: suma de días hábiles, +10 con producto nuevo, ampliación de fabricación ≤ 10, recálculo cuando una etapa termina tarde.
- [ ] `SemaforoService`: verde / naranja (≤ 3 días) / rojo (vencido).
- [ ] **TDD:** pruebas unitarias del motor (fines de semana, viernes + 3 = miércoles, producto nuevo, ampliación > 10 = error).

**Día 5 · vie 16/10 — API de pedidos (8 h)** · skills: `ecc:api-design`
- [ ] `POST /api/pedidos` con código automático `IMP-AAAA-NNN-PRODUCTO`.
- [ ] `GET /api/pedidos` con filtros (estado, semáforo, responsable, fechas) y paginación.
- [ ] `GET /api/pedidos/{codigo}` con etapas y cronograma.
- [ ] Pausar / reanudar / cancelar / reabrir (motivo obligatorio, solo Admin y Junta).
- [ ] Pruebas de integración de los endpoints.

**Sábado 17/10 (3,5 h)**
- [ ] Completar pruebas del motor de cronograma y de permisos.

### Semana 2 — Pantallas y flujo de etapas

**Día 6 · lun 19/10 — Estructura de pantallas (8 h)** · skills: `shadcn`, `ecc:frontend-patterns`
- [ ] Layout: encabezado amarillo, menú lateral por rol (hamburguesa en celular), migas de pan.
- [ ] Pantalla **Pedidos**: tabla, filtros, buscador, botón "Nuevo pedido" (Admin y Marketing).
- [ ] Pantalla **Rastrear pedido**: buscador grande + resultado.

**Día 7 · mar 20/10 — Línea horizontal y detalle (8 h)** · skills: `impeccable:impeccable`
- [ ] Componente `LineaEstados` (11 etapas, agrupadores, relleno amarillo, porcentaje, desplazamiento en celular centrado en la etapa actual).
- [ ] Detalle del pedido: encabezado con estado, semáforo, llegada estimada y pestañas.
- [ ] Pestaña Cronograma (tabla de las 11 etapas).

**Día 8 · mié 21/10 — Flujo de checks (backend) (8 h)**
- [ ] Check list por etapa (`ItemChecklist`, `ItemChecklistMarcado`).
- [ ] `POST .../etapas/{n}/check`: valida permiso por etapa, check list completo y documentos obligatorios; avanza el pedido.
- [ ] Devolver y revertir con motivo obligatorio.
- [ ] Subestados de aduana; aprobación/rechazo de la Junta; validación final del cierre.
- [ ] Pruebas: no se puede dar check si falta un documento; editor no puede actuar en etapa ajena.

**Día 9 · jue 22/10 — Panel de etapa (frontend) (8 h)**
- [ ] Bloques: ¿Qué se hace?, Check list, Documentos, Observaciones, Acciones, Historial.
- [ ] Botones según permiso; si está deshabilitado, mostrar qué falta.
- [ ] Modales de confirmación y avisos de éxito.
- [ ] Mensaje "Esta etapa la gestiona…" para quien no tiene permiso.

**Día 10 · vie 23/10 — Documentos (8 h)**
- [ ] `POST` multipart (máx. 100 MB; PDF, Excel, Word, JPG/PNG, Zip), guardado en disco o MinIO con nombre único.
- [ ] Versiones: reemplazar crea v2, v3; sin borrar.
- [ ] Descarga con control de permisos (editores solo su etapa; Consulta nada).
- [ ] Frontend: zona arrastrar y soltar, miniaturas de imágenes, lista de versiones, enlace de Drive pegado.

**Sábado 24/10 (3,5 h)**
- [ ] Observaciones: backend (`Observacion` con adjuntos) + UI tipo conversación.

### Semana 3 — Completar, probar y entregar

**Día 11 · lun 26/10 — Auditoría (8 h)**
- [ ] Servicio de auditoría llamado en cada acción (quién, qué, cuándo, pedido, etapa, motivo).
- [ ] Pestaña Historial del pedido y de la etapa; pantalla Auditoría (solo Admin) con filtros.

**Día 12 · mar 27/10 — Productos del pedido y Bodega (8 h)**
- [ ] Entidad `LineaPedido` (código Decokasa, código chino, descripción, medida, color, pedida, recibida, estado).
- [ ] En Bodega: cantidad recibida editable, diferencia automática, incidencias con foto.
- [ ] Botón "Generar informe de incidencias" (queda como documento de Bodega).
- [ ] Cargar el pedido real WPC (5.310 + 1.593 = 6.903).

**Día 13 · mié 28/10 — Inicio y alertas (8 h)**
- [ ] Inicio: "Mis tareas pendientes" por usuario, indicadores, próximas llegadas, retrasados.
- [ ] Tarea programada diaria (`@Scheduled`): crea alertas 3 días antes y al vencer.
- [ ] Campana con contador y pantalla Alertas.

**Día 14 · jue 29/10 — Usuarios, datos reales y servidor de pruebas (8 h)** · skills: `ecc:deployment-patterns`
- [ ] Pantalla Usuarios (lista y activar/desactivar) + matriz de permisos visible.
- [ ] Cargar los 7 pedidos en curso con su etapa real.
- [ ] Desplegar en el servidor de pruebas (VPS con Docker Compose + HTTPS) y probar acceso desde China.

**Día 15 · vie 30/10 — Pruebas finales y entrega (8 h)** · skills: `ecc:e2e-testing`, `webapp-testing`, `ecc:security-review`
- [ ] E2E con Playwright de los flujos críticos (abajo).
- [ ] Revisión de seguridad.
- [ ] Prueba con usuarios (UAT) guiada con Mirian, correcciones bloqueantes.
- [ ] Puesta en producción, respaldo de base de datos y acta de entrega.

---

## 3. Modelo de datos (Fase 1)

```
usuario(id, nombre, correo, clave_hash, rol, area, pais, activo)
usuario_etapa(usuario_id, etapa_id)
etapa(id, orden, nombre, plazo_dias_habiles, rol_responsable)
pedido(id, codigo, nombre, fecha_pedido, incluye_producto_nuevo, bodega, estado, version, creado_por)
pedido_etapa(id, pedido_id, etapa_id, estado, subestado, fecha_inicio_est, fecha_fin_est, fecha_fin_real, ampliacion_dias, responsable_id)
item_checklist(id, etapa_id, texto, rol)
item_checklist_marcado(pedido_etapa_id, item_id, usuario_id, fecha)
documento(id, pedido_etapa_id, tipo, nombre, ruta, tamano, version, obligatorio, subido_por, fecha, enlace_drive)
observacion(id, pedido_etapa_id, usuario_id, texto, fecha)
linea_pedido(id, pedido_id, codigo, codigo_chino, descripcion, medida, color, cantidad_pedida, cantidad_recibida, estado, incidencia)
alerta(id, pedido_id, etapa_id, tipo, mensaje, fecha, leida)
auditoria(id, fecha_hora, usuario_id, rol, pedido_id, etapa_id, accion, motivo)
```

---

## 4. Pruebas

| Nivel | Herramienta | Qué cubre | Cuándo |
|---|---|---|---|
| Unitarias | JUnit 5 | Motor de cronograma, semáforo, matriz de permisos | Días 3–5 (TDD) |
| Integración | Spring Boot Test + Testcontainers (PostgreSQL) | Endpoints, reglas de check, documentos, auditoría | Días 5, 8, 10 |
| Componentes | Vitest + Testing Library | Línea de estados, panel de etapa, formularios | Días 7, 9 |
| Punta a punta | Playwright | Flujos críticos | Día 15 |
| Usuarios (UAT) | Guía por rol | Uso real con Mirian, David, Anderson | Días 14–15 |
| Piloto | 7 pedidos reales en paralelo con el Excel | Que el sistema refleje la realidad | 2 semanas tras la entrega |

**Flujos E2E críticos (Playwright):**
1. Login correcto e incorrecto.
2. Mirian crea un pedido → el cronograma se calcula en días hábiles.
3. David intenta dar check en Embarque sin Packing List → bloqueado con mensaje → sube el archivo → check correcto.
4. Anderson pasa Aduana a "Retenido" → aparece alerta.
5. Invitado no ve botones ni archivos.
6. Junta pausa y reanuda un pedido con motivo.
7. Reemplazar un documento crea v2 y no borra v1.
8. Bodega: cantidad recibida menor → estado "Faltante" e informe de incidencias.

**Criterio de entrega:** todas las pruebas en verde, cero errores bloqueantes en UAT y acta firmada por Mirian.

---

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| 127 h sin margen | Si un día se atrasa, pasar a Fase 2 en este orden: usuarios en pantalla → alertas programadas → informe de incidencias |
| El cliente tarda en cerrar los pendientes | Tomar la decisión por defecto del PRD y anotarla en un ADR |
| Acceso desde China | Probarlo el día 14 con David; sin recursos de Google en la interfaz |
| Archivos de 100 MB | Límite en Spring (`spring.servlet.multipart.max-file-size=100MB`) y en el proxy (Nginx `client_max_body_size 100m`) |
