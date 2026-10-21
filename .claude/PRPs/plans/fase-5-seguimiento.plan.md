# Plan: Fase 5 — Seguimiento, plazos y línea de tiempo (parte A)

## Summary

Completa el seguimiento de pedidos sobre el motor de las fases 2 y 4:
- cronograma por pedido en días hábiles con hora de Ecuador, contra una meta configurable (5.1);
- tarea programada que detecta etapas y urgencias vencidas, registra `PLAZO_VENCIDO` y avisa al admin por el outbox sin repetir avisos (5.2);
- dashboards por rol con pendientes, vencidos, responsable, etapa, búsqueda por número y línea horizontal; reemplaza `TimelineCalculatorView` y los `/remind` de Slack (5.3);
- actualización entre pantallas según la decisión que se tome en ADR-0014 (5.4);
- paginación, filtros, índices y accesibilidad de las vistas (5.5).

La parte A no escribe, ejecuta ni modifica pruebas. Tampoco ejecuta builds, migraciones ni operaciones de Google.

## User Story

Como persona de DECOKASA (admin, Compras, Marketing o Logística), quiero ver dónde está cada pedido, quién lo tiene, si va tarde y por qué, y cuándo se planea su llegada frente a la meta, para responder sin perseguir a nadie y actuar antes de que el atraso crezca.

## Problem → Solution

**Hoy:**
- `CalendarioLaboral` calcula los vencimientos de etapa y de urgencia, pero no hay cronograma, meta ni detección de vencimientos.
- `motivoRetraso` siempre es `null`.
- `/pedidos/vencidos`, `/pedidos/{id}/cronograma` y `/configuracion/plazos` están en el contrato pero no tienen controlador.
- El dashboard por rol está vacío: solo enlaza a la bandeja.
- La calculadora del demo suma días corridos y genera comandos de Slack.
- Las pantallas solo se actualizan al recargar, al recuperar el foco o tras `staleTime`.

**Al terminar:**
- `plazos` calcula el cronograma y la diferencia contra la meta, sin inventar duraciones pendientes.
- Una tarea programada registra los vencimientos una sola vez por entrada de etapa o por urgencia y encola el aviso.
- El admin ve el panel de vencidos con el motivo.
- Cada rol ve su trabajo con la línea de tiempo.
- Las pantallas se refrescan con el transporte que apruebe ADR-0014.

## Metadata

- **Complexity**: XL (backend `plazos` y `pedidos`, migraciones nuevas, tarea programada, contrato y frontend). Se divide en los 5 pasos del manual.
- **Source PRD**: `.claude/PRPs/prds/decokasa.prd.md`
- **PRD Phase**: 5 — Seguimiento y plazos
- **Estimated Files**: unos 45 (unos 25 en el backend, 15 en el frontend y 5 de documentación)

---

## Estado de partida y rama (comprobado el 2026-10-20)

**Estado del repositorio:**
- Rama actual: `fase-4-etapas-a5-a11`, sincronizada con `origin/fase-4-etapas-a5-a11` en `d06cc53`.
- Lleva 2 commits sobre `develop` (`977301a` de la fase 3 y `d06cc53` de la fase 4). `develop` termina en el PR #3 (fase 2).
- Las migraciones V1–V13, incluida la corrección F4-H01, están confirmadas en `d06cc53`.
- Único cambio rastreado sin confirmar: `frontend/src/components/Header.tsx`, que es ajeno y se conserva.
- Hay unos 377 archivos sin rastrear: evidencias de `documentos/pruebas/`, `.vscode/`, `documentos/.tmp-derivadas/` y documentos sueltos. No se tocan.

**Estado de las fases:**
- La fase 3 tiene las pruebas locales aprobadas y 3.B3 pospuesto.
- La fase 4 sigue abierta: A6, A9 y A10 están bloqueadas por F4-D4, F4-D7 y F4-D8, y F4-H01 está pendiente de revalidación por Codex.
- Ninguna de las dos está integrada en `develop`.

**Rama:** `fase-5-seguimiento` **creada por Claude el 2026-10-20** desde `fase-4-etapas-a5-a11` (`d06cc53`), por indicación de Richard, sin conflictos; conservó `Header.tsx`, el enlace del PRD y los archivos sin rastrear. Recomendación con la que se creó:

1. Crear la rama de la fase 5 desde la punta de la fase 4, no desde `develop`. La fase 5 depende de la 4 (manual: "las fases 5 y 6 dependen de 4"), y `develop` no tiene ni la 3 ni la 4. Se crea con `git switch -c fase-5-seguimiento fase-4-etapas-a5-a11` desde un árbol con solo `Header.tsx` modificado. `switch -c` mantiene ese cambio sin confirmar en el árbol de trabajo, sin stash ni commit, y conserva los archivos sin rastrear.
2. Mientras la 3 y la 4 sigan abiertas, las correcciones que Codex devuelva para la fase 4 se hacen en `fase-4-etapas-a5-a11`. Después se traen a la 5 con `git merge fase-4-etapas-a5-a11`, sin rebase, para no reescribir commits publicados.
3. Cuando la 3 y la 4 se integren en `develop` (PR), el PR de la fase 5 hacia `develop` solo mostrará lo propio.
4. Si la fase 6 se trabaja en paralelo (manual, regla 4), conviene usar un worktree aparte desde la misma punta y acordar las migraciones al crearlas. **Richard (2026-10-20): no se reservan versiones por adelantado;** cada paso usa la siguiente disponible cuando la necesita (5.1 usó V14).

---

## Decisiones de la fase (F5-D)

Etiquetas: **aprobado** (cliente o ADR aceptado), **provisional** (valor documentado mientras el cliente responde), **⏸ pendiente** (sin respuesta del cliente) y **[P] propuesta** (equipo, sin aprobar).

| ID | Paso | Tema | Fuente y estado | Propuesta | Bloquea |
|---|---|---|---|---|---|
| **F5-D1** | 5.1 | **Duraciones del cronograma para etapas con plazo `PENDIENTE`** (A5, A7, A10) | ⏸ F4-D3, F4-D6 y F4-D8 | **Aprobada por Richard (2026-10-20), opción A, decisión del equipo:** una duración PENDIENTE no equivale a cero ni a SIN_PLAZO; desde el primer plazo desconocido, las fechas dependientes quedan "sin estimar"; el cálculo se retoma desde un dato de llegada válido, con la llegada real por encima de la estimada y su procedencia visible; el total y la diferencia contra la meta no son calculables si faltan duraciones necesarias; se distinguen fechas planificadas, estimadas y reales. | Implementada en 5.1 |
| F5-D2 | 5.4 | Transporte de la actualización entre pantallas | ADR-0014. | **Aprobada provisionalmente por Richard (2026-10-20), decisión técnica del equipo:** consultas periódicas con TanStack Query, 30 s por defecto, configurables entre 10 s y 5 min; sin WebSocket. Un cambio ajeno puede tardar hasta el intervalo más la petición; "al instante" no se declara satisfecho sin aceptación del cliente. | Implementada en 5.4 |
| F5-D3 | 5.2 | Motivo del retraso | Requisito 4.2 #16 (visible en la etapa); las fuentes no dicen quién lo escribe. | **Aprobada provisionalmente por Richard (2026-10-20), decisión del equipo:** lo registra o corrige quien puede actuar en la etapa actual según P-06, o el admin, mientras exista un vencimiento activo (etapa o urgencia). Cada cambio deja historial (`MOTIVO_RETRASO_REGISTRADO`); una reentrada limpia el motivo vigente sin borrar los anteriores; no genera correo. Los usuarios con varios roles conservan sus permisos; `consulta` o `asistente_china` no conceden escritura ni anulan otros roles. | Implementada en 5.2 |
| F5-D4 | 5.2 | ¿La urgencia vencida avisa? | Requisitos 4.2 #11 y #16. | **Aprobada provisionalmente por Richard (2026-10-20):** sí. ETAPA y URGENCIA son avisos diferentes; si ambos vencen, dos avisos identificados por su clase. Unicidad: ETAPA = `entradaId` + vencimiento; URGENCIA = UUID estable de la activación (la `entrada_etapa` creada por la devolución con urgencia) + vencimiento, no solo la hora. Las reglas provisionales de urgencia (horas hábiles; un rechazo de Revisión no la reinicia, P-10 ⏸) no cambian. | Implementada en 5.2 |
| F5-D5 | 5.2 | Destinatarios del aviso | ADR-0005 aceptado: al admin. ⏸ el responsable. | Solo el admin, por `DirectorioDestinatarios` (sin inventar destinatarios). **Sin destinatario configurado, el aviso queda en el outbox con destinatario nulo: bloqueo explícito, el vencimiento no se pierde.** No se envían correos reales en la parte A. | Implementada en 5.2 |
| F5-D6 | 5.1 | Meta de días | Los 90 días son solo propuesta (requerimientos §5 "Propuesta mientras tanto"; PRD "TBD la meta ajustada"; `arquitectura.md` 8.1 "no es una respuesta del cliente"). **No hay aprobación provisional documentada** (comprobado el 2026-10-20). | Por indicación de Richard: **propuesta configurable pendiente**. V14 siembra 90 con `meta_origen = PROPUESTA`; el admin puede cambiarla (`ADMIN`) y la API marca siempre `metaPendienteDelCliente: true`. **Aclaración (Richard, 2026-10-20):** la meta propuesta de 90 días no es un objetivo aprobado: sirve solo como referencia configurable mientras el cliente responde, y un `diferenciaContraMeta` igual o menor que cero no permite declarar que se cumple un objetivo del cliente. | — |
| F5-D7 | 5.1 | Feriados de Ecuador y China; jornada dentro del día | ⏸ F4-D10. `modelo-datos.md`: "la tabla `feriado` solo se crea si se aprueba". | No crear `feriado`. `feriados` sale vacío en `GET` y `PUT` lo rechaza con 400 si trae elementos, con nota en el contrato. | — |
| F5-D8 | 5.1 | Contradicción entre fuentes | PRD fase 5: "el cronograma del caso base de la calculadora da las mismas fechas con la regla de días hábiles". Manual 5.1: "no copies sus fechas". | Prevalece el manual, que es más reciente y coherente con ADR-0006. La señal de éxito del PRD se marca como pendiente de reformular. No se cambia el PRD sin aprobación. | — |
| F5-D9 | 5.2 | Intervalo y lote de la tarea | Técnico. | Tarea **desactivada por defecto** (`VENCIMIENTOS_AUTOMATICO=false`) hasta su verificación. Valores técnicos provisionales configurables: intervalo 300 000 ms y lote 100, **pendientes de confirmar por Richard**. | Antes de activarla |
| F5-D10 | 5.3 | Visibilidad en dashboards y paneles | P-13 provisional: todos ven todos los pedidos. El contrato reserva `/pedidos/vencidos` al admin. Solo Ecuador en la versión 1 (ADR-0009). | Se mantiene. "Conjunto autorizado" del admin = todos los pedidos de los países del admin (hoy EC). | — |

**Se conservan sin cambios:**
- F4-D3, F4-D4, F4-D6, F4-D7, F4-D8 y F4-D9 (pendientes del cliente).
- Que las urgencias se cuenten en horas hábiles (provisional).
- 3.B3 pospuesto.
- Las fases 3 y 4 abiertas.

---

## UX Design

### Before

```
/inicio   [Panel por rol: texto + botón "Ir a la bandeja"]
/bandeja  tabla de pedidos (sin vencidos destacados ni motivo)
/pedidos  lista con filtros (sin vencido en pantalla)
Detalle   línea de tiempo de etapas + historial
(demo sin ruta) TimelineCalculatorView: días corridos + "/remind" Slack
```

### After

```
/inicio
┌──────────────────────────────────────────────────────────────┐
│ [Mis pendientes 7] [Vencidos 2 ⚠] [Con urgencia 1 ⏱]         │  ← número, texto e icono
│ Buscar pedido por número: [DK-EC-2026-____] (Buscar)         │
│ ┌ Pestañas: Pendientes | Vencidos | Todos (admin) ┐          │
│ │ DK-EC-2026-0007  A3 Revisión  Marcela  ⚠ VENCIDA 21/10     │
│ │ ●──●──◉──○──○──○──○──○──○──○──○  (línea horizontal)        │
│ └─────────────────────────────────────────────────┘          │
│ (admin) Panel de vencidos: por etapa ▾  por responsable ▾    │
└──────────────────────────────────────────────────────────────┘
Detalle → pestaña "Cronograma": fechas planificadas / reales, "sin estimar"
          en etapas con plazo pendiente, meta 90 (provisional) y diferencia
          o "no calculable".
/configuracion/plazos (admin): meta de días; feriados "pendiente del cliente".
```

### Interaction Changes

| Touchpoint | Before | After | Notes |
|---|---|---|---|
| Dashboard | Panel estático | Contadores, pestañas, búsqueda y filas con línea horizontal | Estado con texto e icono, no solo color (5.5) |
| Vencidos | No existe | Panel del admin con motivo, etapa y responsable | `GET /pedidos/vencidos` |
| Motivo | No existe | Campo en el panel de etapa vencida (si se aprueba F5-D3) | Solo quien puede actuar o el admin |
| Cronograma | Calculadora del demo, días corridos | Pestaña en el detalle, días hábiles | Sin copiar las fechas de la calculadora |
| Slack | Copia de `/remind` | Retirado | Requisito 4.9 |
| Actualización | Al recargar o recuperar el foco | Según ADR-0014 | 5.4 |

---

## Mandatory Reading

| Priority | File | Lines | Why |
|---|---|---|---|
| P0 | `backend/.../plazos/domain/CalendarioLaboral.java` | all | Cálculo hábil vigente; se extiende y no se reescribe |
| P0 | `backend/.../plazos/application/usecase/CalcularVencimientoCasoDeUso.java` | all | `Pendiente` → vacío; constante provisional de urgencia |
| P0 | `backend/.../pedidos/application/usecase/ConsultarPedidosCasoDeUso.java` | 97-180 | Resumen, `vencida`, carga por lotes de personas |
| P0 | `backend/.../pedidos/adapter/out/persistence/PedidoJpaRepositorio.java` | 160-245 | Página de ids + `fetch join` (evita N+1); filtro `vencido` |
| P0 | `backend/src/main/resources/db/migration/V2__flujos_pedidos_historial.sql` | 40-60, 180-300, 330-360 | `plazo_estado`, `etapa_pedido.vence_en` y `motivo_retraso`, tipo `PLAZO_VENCIDO`, siembra de plazos |
| P0 | `backend/src/main/resources/db/migration/V4__drive_notificaciones_outbox.sql` | 19-45 | `notificacion` única por `(evento_id, destinatario_clave)` |
| P0 | `documentos/api/openapi.yaml` | 635-800, 1586-1650, 2322-2360, 2875-2931 | Contratos de listar, buscar, bandeja, vencidos, cronograma y configuración |
| P0 | `documentos/adr/0005`, `0006`, `0012`, `0014` | all | Alertas, plazos, outbox y tiempo real |
| P1 | `backend/.../notificaciones/adapter/in/eventos/OyenteEventosNotificacion.java` | all | Encolado síncrono `MANDATORY` |
| P1 | `backend/.../notificaciones/application/usecase/EncolarNotificacionCasoDeUso.java` | 20-40 | `switch` sobre los eventos que generan correo |
| P1 | `backend/.../notificaciones/config/CorreoAutomaticoConfig.java` | all | Patrón de tarea programada condicionada por propiedad |
| P1 | `backend/.../compartido/domain/eventos/EventoDePedido.java` | all | `sealed`; agregar `PlazoVencido` |
| P1 | `backend/.../historial/...` (`EventoPedido.desde`, `DetalleEvento`, `HistorialJpaRepositorio`) | — | Conversión y orden F4-H01 (secuencia) |
| P1 | `frontend/src/api/pedidos.ts` | 220-370 | `clavesPedidos` por sesión e invalidaciones |
| P1 | `frontend/src/components/dashboards/DashboardRol.tsx` | all | Paneles por rol; provisionales `consulta` y `asistente_china` |
| P1 | `frontend/src/components/pedidos/LineaDeTiempo.tsx`, `TablaPedidos.tsx`, `BandejaPedidos.tsx` | all | Componentes que se reutilizan |
| P2 | `frontend/src/components/TimelineCalculatorView.tsx`, `TrackingSearchView.tsx`, `OrderScheduleTab.tsx`, `utils/dateUtils.ts` | all | Demo que se sustituye. **No** reutilizar `addCalendarDaysWithWeekendRoll` ni `SIMULATED_TODAY` |
| P2 | `documentos/modelo-datos.md` | 366-400, 443 | `configuracion_plazos`, `feriado` [P] ⏸, consulta de vencidos |
| P2 | `documentos/requerimientos.md` | 99-125, 388-402, 476-494, 530-570 | Calculadora, tabla de etapas, observabilidad y pendientes |

## External Documentation

No hace falta investigación externa para 5.1, 5.2, 5.3 ni 5.5: usan patrones internos (`@Scheduled`, JPA, TanStack Query, shadcn).

Para 5.4 depende de F5-D2:
- con STOMP: Spring WebSocket y Spring Security de mensajería con Spring Boot 4.1, y `@stomp/stompjs`;
- con consultas periódicas: `refetchInterval` de TanStack Query.

Se consulta al implementar 5.4, no antes.

---

## Patterns to Mirror

### CALENDARIO (dominio puro, zona de Ecuador)
```java
// SOURCE: backend/.../plazos/domain/CalendarioLaboral.java (vencimiento)
public Instant vencimiento(Instant inicio, PlazoDeEtapa.Definido plazo) {
    ZonedDateTime desde = inicio.atZone(zona);
    ZonedDateTime vence = switch (plazo.tipo()) {
        case HORAS_HABILES -> sumarHorasHabiles(desde, plazo.cantidad());
        case DIAS_HABILES -> sumarDiasHabiles(desde, plazo.cantidad());
        case DIAS_CORRIDOS -> desde.plusDays(plazo.cantidad());
    };
    return alLunesSiEsFinDeSemana(vence).toInstant();
}
```

### PLAZO PENDIENTE NUNCA ES CERO
```java
// SOURCE: backend/.../plazos/application/usecase/CalcularVencimientoCasoDeUso.java (deEtapa)
case PlazoDeEtapa.Definido definido -> Optional.of(calendario.vencimiento(entrada, definido));
case PlazoDeEtapa.SinPlazo sinPlazo -> Optional.empty();
case PlazoDeEtapa.Pendiente pendiente -> Optional.empty();
```

### COMPOSICIÓN SIN SPRING EN EL CASO DE USO
```java
// SOURCE: backend/.../plazos/config/PlazosConfig.java
@Configuration(proxyBeanMethods = false)
public class PlazosConfig {
    @Bean
    CalcularVencimiento calcularVencimiento() {
        return new CalcularVencimientoCasoDeUso(new CalendarioLaboral(Reloj.ZONA_ECUADOR));
    }
}
```

### TAREA PROGRAMADA CONDICIONADA Y LOG SIN DATOS
```java
// SOURCE: backend/.../notificaciones/config/CorreoAutomaticoConfig.java
@ConditionalOnProperty(name={...}, havingValue="true")
...
@Scheduled(fixedDelayString="${decokasa.google.gmail.worker.intervalo-ms:30000}")
public void ejecutar() {
    try { enviar.procesar(); }
    catch (RuntimeException error) {
        LoggerFactory.getLogger(TareaCorreo.class).warn("correo_pasada_interrumpida; revisar registro persistente");
    }
}
```

### EVENTO → HISTORIAL → OUTBOX EN LA MISMA TRANSACCIÓN
```java
// SOURCE: backend/.../notificaciones/adapter/in/eventos/OyenteEventosNotificacion.java
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void recibir(EventoPedidoRegistrado evento) { encolar.encolar(evento); }
```

### PÁGINA SIN N+1
```java
// SOURCE: backend/.../pedidos/adapter/out/persistence/PedidoJpaRepositorio.java:180-199, 234-238
TypedQuery<UUID> ids = entityManager.createQuery("select p.id from PedidoEntity p" + condiciones ...);
List<UUID> deLaPagina = ids.setFirstResult(pagina * tamano).setMaxResults(tamano).getResultList();
return new Pagina(cargarEnOrden(deLaPagina), total.getSingleResult());
// cargarEnOrden: "select distinct p from PedidoEntity p left join fetch p.etapas where p.id in :ids"
```

### CONTROLADOR CON SOLICITANTE DEL JWT
```java
// SOURCE: backend/.../pedidos/adapter/in/web/PedidoController.java:53-63
@GetMapping
public PedidoRespuestas.Pagina listar(@AuthenticationPrincipal Jwt token, ... @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
    return PedidoRespuestas.de(consultar.listar(SolicitantesWeb.de(token), new ConsultarPedidos.Filtro(...), pagina, tamano));
}
```

### CLAVES DE CONSULTA POR SESIÓN E INVALIDACIÓN
```ts
// SOURCE: frontend/src/api/pedidos.ts:220-225, 359-369
export const clavesPedidos = {
  lista: (sesion: number, filtros: FiltrosPedidos) => ['sesion', sesion, 'pedidos', 'lista', filtros] as const,
  bandeja: (sesion: number, pagina: number) => ['sesion', sesion, 'pedidos', 'bandeja', pagina] as const,
  detalle: (sesion: number, id: string) => ['sesion', sesion, 'pedidos', 'detalle', id] as const,
};
// useRefrescarPedido invalida detalle, historial, archivos, listas, bandejas y notificaciones.
```

### TEST_STRUCTURE
No aplica a la parte A: Codex escribe y ejecuta las pruebas en 5.B1–5.B3. Las suites existentes (`*IT`, `ReglasArquitecturaTest`, `FronterasFaseDosTest`, Vitest) se conservan sin cambios.

---

## Files to Change

| File | Action | Paso | Justification |
|---|---|---|---|
| `backend/.../plazos/domain/CalendarioLaboral.java` | UPDATE | 5.1 | Suma por fecha (sin hora) para el cronograma; la regla vigente no cambia |
| `backend/.../plazos/domain/Cronograma.java` (+ `EtapaPlanificada`) | CREATE | 5.1 | Fechas planificadas y reales, arrastre, `movidaAlLunes`, total y diferencia o "no calculable" |
| `backend/.../plazos/application/port/in/CalcularCronograma.java`, `ConsultarConfiguracionPlazos.java`, `ConfigurarMeta.java` | CREATE | 5.1 | Puertos de `arquitectura.md` 4.7 |
| `backend/.../plazos/application/port/out/ConfiguracionPlazosRepositorio.java`, `PedidosConEtapaActiva.java` | CREATE | 5.1/5.2 | Meta; lectura de pedidos implementada por `pedidos` (R9, sin ciclo) |
| `backend/.../plazos/application/usecase/CalcularCronogramaCasoDeUso.java`, `ConfiguracionPlazosCasoDeUso.java` | CREATE | 5.1 | Sin Spring (R2); meta solo para el admin con estado vigente |
| `backend/.../plazos/adapter/in/web/CronogramaController.java`, `ConfiguracionPlazosController.java` | CREATE | 5.1 | `GET /pedidos/{id}/cronograma`, `GET`/`PUT /configuracion/plazos` |
| `backend/.../plazos/adapter/out/persistence/ConfiguracionPlazosJpaRepositorio.java` (+ entidad) | CREATE | 5.1 | Fila única con versión |
| `backend/.../pedidos/adapter/out/persistence/PedidosConEtapaActivaJpa.java` | CREATE | 5.1/5.2 | Adaptador de `pedidos` para el puerto de `plazos` |
| `backend/src/main/resources/db/migration/V14__configuracion_plazos.sql` | CREATE | 5.1 | `configuracion_plazos` (`id=1`, `meta_dias` 90 provisional, `version`) |
| `backend/.../plazos/application/port/in/DetectarVencimientos.java`, `ListarPedidosVencidos.java`, `RegistrarMotivoRetraso.java`* | CREATE | 5.2 | *El último solo si se aprueba F5-D3 |
| `backend/.../plazos/application/usecase/DetectarVencimientosCasoDeUso.java` | CREATE | 5.2 | Lote con bloqueo por pedido, evento `PlazoVencido` y marca de avisado |
| `backend/.../plazos/adapter/in/tareas/TareaVencimientos.java` + `plazos/config/VencimientosAutomaticosConfig.java` | CREATE | 5.2 | `@Scheduled` condicionado por propiedad (F5-D9) |
| `backend/.../compartido/domain/eventos/PlazoVencido.java`, `EventoDePedido.java` | CREATE/UPDATE | 5.2 | Evento del sistema (sin usuario); `permits` |
| `backend/.../historial/...` (`EventoPedido.desde`, `DetalleEvento`, JSON) | UPDATE | 5.2 | `PLAZO_VENCIDO` con clase (`ETAPA`/`URGENCIA`) y `venceEn` |
| `backend/.../notificaciones/application/usecase/EncolarNotificacionCasoDeUso.java` | UPDATE | 5.2 | Caso `PlazoVencido` → aviso al admin (F5-D5) |
| `backend/src/main/resources/db/migration/V<siguiente disponible>__avisos_vencimiento.sql` | CREATE | 5.2 | `aviso_vencimiento` con claves únicas por entrada de etapa y por urgencia |
| `backend/.../pedidos/...` (`PedidoController` `/vencidos`, `ConsultarPedidosCasoDeUso`, `PedidoJpaRepositorio`) | UPDATE | 5.2/5.5 | Panel de vencidos, `motivoRetraso` real, `vencida` que considera la urgencia, filtro `responsableId` |
| `documentos/api/openapi.yaml` | UPDATE | 5.1–5.5 | 0.11.0-borrador: `Cronograma` con `fechaPlanificada` anulable y `estado`, totales anulables, `faltantes`; configuración con `version`; `PLAZO_VENCIDO`; `/vencidos` paginado; tiempo real según F5-D2 |
| `documentos/modelo-datos.md`, `arquitectura.md` (4.7, 4.9), `adr/0014` y `adr/README.md` | UPDATE | 5.1–5.4 | V14, las migraciones siguientes y la decisión de ADR-0014 |
| `frontend/src/api/plazos.ts` (+ esquemas Zod) | CREATE | 5.1/5.3 | `useCronograma`, `useConfiguracionPlazos`, `useVencidos` |
| `frontend/src/components/dashboards/DashboardRol.tsx` + `ResumenRol.tsx`, `PanelVencidos.tsx` | UPDATE/CREATE | 5.3 | Contadores, pestañas, búsqueda y línea horizontal compacta |
| `frontend/src/components/pedidos/CronogramaPedido.tsx` | CREATE | 5.3 | Pestaña en `DetallePedido` |
| `frontend/src/components/pedidos/LineaDeTiempo.tsx` | UPDATE | 5.3/5.5 | Variante compacta por fila (sin consulta por fila) |
| `frontend/src/components/ConfiguracionPlazos.tsx`, `rutas.tsx`, `Sidebar.tsx` | CREATE/UPDATE | 5.3 | Ruta admin `/configuracion/plazos`. **`Header.tsx` no se toca** |
| `frontend/src/components/TimelineCalculatorView.tsx`, `TrackingSearchView.tsx`, `OrderScheduleTab.tsx` | DELETE | 5.3 | Sustituidos; se retiran los comandos `/remind` (requisito 4.9) |
| `frontend/src/api/pedidos.ts`, `main.tsx`, `api/tiempoReal.ts`* | UPDATE/CREATE | 5.4 | Según F5-D2 |
| `.claude/PRPs/prds/decokasa.prd.md` | UPDATE | — | Enlace a este plan (estado sin cambiar) |

## NOT Building

- Feriados de Ecuador o China y jornada laboral: ⏸ F5-D7 y F4-D10. No se crea `feriado`.
- Plazos de A5, A7 o A10 ni duraciones de cronograma para ellas sin decisión (F5-D1). No se usan los 8, 31, 12 ni 45 días de la calculadora.
- Correo al responsable de la etapa (⏸ F5-D5) y avisos de Fin o demora (⏸ F4-D9).
- Desbloqueo de A6, A9 o A10: F4-D4, F4-D7 y F4-D8 siguen `PENDIENTES`.
- Flujo B (fase 6), Colombia y selección de país (fase 7).
- Pruebas, mediciones de rendimiento y accesibilidad: son de Codex (5.B1–5.B3).
- Recordatorios por Slack de cualquier tipo.
- Cambios en V1–V13 o en `Header.tsx`.

---

## Step-by-Step Tasks

### Paso 5.1 — Calendario y cronograma (`/ecc:prp-implement`, `ecc:springboot-patterns`)

**Requiere F5-D1.**

#### Task 1: Calendario por fecha
- **ACTION**: extender `CalendarioLaboral` sin cambiar `vencimiento`.
- **IMPLEMENT**:
  - `LocalDate alLunes(LocalDate)`;
  - `LocalDate sumar(LocalDate, PlazoDeEtapa.Definido)`: las `HORAS_HABILES` en múltiplos de 24 cuentan como días hábiles; si no, se toma el día del instante calculado con `vencimiento`. `DIAS_CORRIDOS` suma y luego pasa al lunes;
  - `int diasCorridosEntre(LocalDate, LocalDate)` para el total contra la meta, en el mismo sentido que la métrica del PRD ("fecha de check de Aduana menos fecha de creación").
- **MIRROR**: CALENDARIO.
- **IMPORTS**: `java.time.LocalDate`, `java.time.temporal.ChronoUnit`.
- **GOTCHA**:
  - No copiar `dateUtils.ts` (`addCalendarDaysWithWeekendRoll`).
  - Feriados: ninguno (F5-D7).
  - Documentar en el Javadoc que la meta se mide en días corridos de pedido a llegada (métrica del PRD) mientras que los plazos son hábiles. Si se prefiere otra lectura, entra en F5-D1.
- **VALIDATE**: Codex en 5.B1 (zona, fines de semana, límites).

#### Task 2: Dominio `Cronograma`
- **ACTION**: crear un record inmutable con `etapas` (`numero`, `nombre`, `fechaPlanificada?`, `fechaReal?`, `movidaAlLunes`, `estado` ∈ {`REAL`, `PLANIFICADA`, `ESTIMADA_ADUANA`, `SIN_ESTIMAR`}), `metaDias`, `totalDiasPlanificados?`, `diferenciaContraMeta?` y `faltantes` (etapas con plazo pendiente).
- **IMPLEMENT**:
  - **Arrastre:** cada etapa empieza en la fecha real de salida de la anterior si existe; si no, en su fecha planificada.
  - **Etapas `SIN_ESTIMAR`:** las que tienen plazo `PENDIENTE` y las siguientes, hasta un ancla.
  - **Ancla en A7:** `demora_nueva_fecha_llegada` (ya movida al lunes por F4-D5) o `llegada_aduana_en`.
  - **Reentradas:** se usa la última entrada (el plazo se reinicia; requisito 4.2).
  - El tratamiento exacto de las etapas pendientes depende de **F5-D1**.
- **MIRROR**: CALENDARIO y PLAZO PENDIENTE NUNCA ES CERO.
- **GOTCHA**:
  - Una etapa `SIN_PLAZO` (A1, Fin) suma cero.
  - Una etapa automática no planifica.
  - La estimación de A7 **no** es un vencimiento (F4-D5).
- **VALIDATE**: Codex en 5.B1.

#### Task 3: V14 y configuración de la meta
- **ACTION**: `V14__configuracion_plazos.sql` según `modelo-datos.md` 3.7, más `version`.
- **IMPLEMENT**:
  - Una sola fila `id = 1` con `meta_dias = 90` y `meta_origen = 'PROPUESTA'` (propuesta configurable pendiente, sin aprobación provisional documentada) y `actualizado_por` nulo en la siembra.
  - `PUT` solo para el admin con estado vigente (patrón `Autorizacion`, A1 de la fase 1).
  - `version` obligatoria; si no coincide, 409 `VERSION_DESACTUALIZADA` (D10).
  - Si viene `feriados` con elementos: 400.
- **MIRROR**: migraciones V10–V13 (comentario de cabecera con decisión y estado).
- **GOTCHA**: no crear `feriado`. V14 es la siguiente libre tras V13; no alterar V1–V13.
- **VALIDATE**: Codex (migración sobre una base con datos).

#### Task 4: Casos de uso, puertos y controladores
- **ACTION**: crear `CalcularCronograma`, `ConsultarConfiguracionPlazos`, `ConfigurarMeta`, el puerto de salida `PedidosConEtapaActiva` (que también da las fechas de entrada y salida de cada etapa y las fechas de A7) y su adaptador en `pedidos`.
- **IMPLEMENT**:
  - El cronograma lo puede ver cualquier usuario con rol vigente (P-13).
  - Un pedido inexistente da 404.
  - Composición en `PlazosConfig` con el decorador transaccional `readOnly` (ADR-0012).
- **MIRROR**: COMPOSICIÓN y CONTROLADOR.
- **GOTCHA**: R9, sin ciclo: `plazos` no importa `pedidos`; el adaptador vive en `pedidos` e implementa el puerto de `plazos` (`arquitectura.md` 3.1).
- **VALIDATE**: Codex (ArchUnit y `FronterasFaseDosTest`).

#### Task 5: Contrato y documentación de 5.1
- **ACTION**: subir `openapi.yaml` a 0.11.0-borrador.
- **IMPLEMENT**:
  - `Cronograma`: `fechaPlanificada` anulable, `estado`, `totalDiasPlanificados` y `diferenciaContraMeta` anulables, `faltantes`, nota F5-D8;
  - `ConfiguracionPlazos`: `version`, `feriados` de solo lectura y vacío (⏸).
  - En `modelo-datos.md`: V14.
- **VALIDATE**: revisión de coherencia.

### Paso 5.2 — Detección de vencimientos (`/ecc:prp-implement`, `ecc:springboot-patterns`)

#### Task 6: Evento `PlazoVencido`
- **ACTION**: agregar `PlazoVencido(pedidoId, etapa, clase, venceEn, ocurridoEn)` a `compartido.domain.eventos` como evento del sistema, sin `Interviniente`.
- **IMPLEMENT**:
  - En el historial, `PLAZO_VENCIDO` ya está permitido sin usuario (V2).
  - `DetalleEvento` lleva `clase` y `venceEn`.
  - El orden es el de F4-H01 (secuencia).
- **GOTCHA**: mantener los constructores secundarios existentes para que compilen las pruebas actuales.

#### Task 7: Migración de avisos (siguiente disponible) y caso de uso `DetectarVencimientos`
- **ACTION**: crear `aviso_vencimiento(id, pedido_id, clase, etapa_pedido_id?, entrada_en?, urgencia_activada_en?, vence_en, evento_id, registrado_en)`:
  - UNIQUE parcial `(etapa_pedido_id, entrada_en) where clase='ETAPA'`;
  - UNIQUE parcial `(pedido_id, urgencia_activada_en) where clase='URGENCIA'`;
  - CHECK de coherencia de las columnas por clase.
  - Se reutiliza el índice `etapa_pedido_vence` de V2.
- **IMPLEMENT**:
  - **Selección:** cada pasada pide a `PedidosConEtapaActiva` los candidatos vencidos sin aviso, en lotes con límite.
  - **Bloqueo:** cada pedido se procesa en su propia transacción, bloqueando la fila del pedido como en las acciones. Se vuelve a leer que siga vencido y en la misma entrada.
  - **Registro:** inserta `aviso_vencimiento` y publica `PlazoVencido`. El historial y el outbox se escriben en la misma transacción.
  - **Concurrencia:** si dos pasadas compiten, la UNIQUE deja una sola. La otra se descarta traduciendo solo esa restricción, sin capturar otros errores SQL (patrón UB001 de V12).
- **MIRROR**: EVENTO → HISTORIAL → OUTBOX.
- **GOTCHA**:
  - Una reentrada nueva (nueva `entrada_en`) puede volver a vencer y avisar.
  - Un pedido finalizado no se procesa.
  - Las etapas con plazo `PENDIENTE` no tienen `vence_en` y nunca vencen.
  - La hora sale del `Reloj` (R5).

#### Task 8: Tarea programada
- **ACTION**: `TareaVencimientos` y `VencimientosAutomaticosConfig` con `@ConditionalOnProperty("decokasa.plazos.vencimientos.automatico")` y `fixedDelayString`.
- **IMPLEMENT**: log sin datos de pedidos ni personas (patrón de `TareaCorreo`); en `application.yml`, la propiedad apagada por defecto y el intervalo según F5-D9; documentar en `.env.example` sin secretos.
- **MIRROR**: TAREA PROGRAMADA.

#### Task 9: Aviso al admin y motivo visible
- **ACTION**:
  - Agregar el caso `PlazoVencido` en `EncolarNotificacionCasoDeUso`, con destinatarios de `DirectorioDestinatarios` (admin, F5-D5).
  - `ConsultarPedidosCasoDeUso.resumen` llena `motivoRetraso` desde `etapa_pedido.motivo_retraso`, y `vencida` también considera la urgencia vencida (si se aprueba F5-D4).
  - `GET /pedidos/vencidos` (admin) con `etapa` y `responsableId`.
  - Si se aprueba F5-D3: un endpoint para escribir el motivo (P-06 o admin; 1–4000 caracteres), con su evento de historial.
- **GOTCHA**:
  - La unicidad del outbox por `(evento_id, destinatario)` ya evita duplicados de envío.
  - `aviso_vencimiento` evita duplicados de detección.
  - No avisar demoras ni Fin (F4-D9).

### Paso 5.3 — Dashboards y búsqueda (`ecc:dashboard-builder`, shadcn, tanstack-query-best-practices)

#### Task 10: API del frontend
- **ACTION**: `api/plazos.ts` con esquemas Zod y claves bajo `['sesion', sesion, 'plazos', …]`.
- **IMPLEMENT**: `useCronograma(id)`, `useConfiguracionPlazos()`, `useActualizarMeta()` (sin reintento; 409 → "Cargar datos actuales"), `useVencidos(filtros)`; en `pedidos.ts`, filtros `vencido` y `responsableId`.
- **MIRROR**: CLAVES DE CONSULTA.

#### Task 11: Dashboard por rol
- **ACTION**: reemplazar el contenido estático de `DashboardRol` y conservar `panelesDe` y las marcas provisionales de `consulta` y `asistente_china`.
- **IMPLEMENT**:
  - Tarjetas: pendientes (bandeja `total`), vencidos y con urgencia, cada una desde una consulta paginada que solo lee `total`.
  - Pestañas: Pendientes, Vencidos y Todos (admin).
  - Búsqueda por número con `/pedidos/buscar` y navegación al detalle.
  - Cada fila lleva una línea horizontal compacta, que sale de `etapaActual` y del flujo en caché (`useDefinicionFlujo`), **sin pedir el detalle por fila**.
- **GOTCHA**:
  - Estado con texto e icono, además de color.
  - No mostrar acciones a `consulta` ni a `asistente_china`.

#### Task 12: Panel de vencidos (admin) y cronograma
- **ACTION**: crear `PanelVencidos` (agrupado por etapa y por responsable, con motivo o "Sin motivo registrado") y `CronogramaPedido` como pestaña de `DetallePedido`.
- **IMPLEMENT**:
  - Fechas con `formatearFecha` y estados `SIN_ESTIMAR` con texto explicativo.
  - Meta con la etiqueta "provisional (pendiente del cliente)".
  - Diferencia o "no calculable: faltan plazos de …".

#### Task 13: Configuración de plazos y retiro del demo
- **ACTION**: ruta admin `/configuracion/plazos` (meta; feriados "pendiente del cliente" de solo lectura). Borrar `TimelineCalculatorView.tsx`, `TrackingSearchView.tsx` y `OrderScheduleTab.tsx`, y sus importaciones.
- **GOTCHA**:
  - Comprobar con grep que nada más los importa.
  - `dateUtils.ts` solo se toca si queda sin uso.
  - Las pruebas que vigilen el demo son de Codex: no se editan.
  - `Header.tsx` no se toca.

### Paso 5.4 — Actualización de pantallas (`ecc:springboot-patterns`, tanstack-query-best-practices)

**Requiere F5-D2 (ADR-0014).** Las dos ramas se describen para que la decisión sea informada; solo se implementa la aprobada.

#### Task 14: Transporte aprobado
- **Si STOMP**:
  - Dependencia de WebSocket; ADR-0014 pasa a `accepted` y se agrega al stack de `CLAUDE.md`.
  - Puerto `PublicadorTiempoReal` en `compartido` y adaptador que publica **solo** `{pedidoId, tipo}`, sin datos del pedido, después del commit.
  - Autenticación del CONNECT con el JWT de acceso en la cabecera STOMP, no en la URL.
  - Autorización de SUBSCRIBE por país autorizado, con estado vigente.
  - `Origin` comprobado y CSP `connect-src` en `nginx.conf`, más un `location` con `Upgrade`.
  - Proxy WebSocket en `vite.config.ts`.
  - El cliente invalida `clavesPedidos.*` y `plazos` del pedido.
- **Si consultas periódicas**:
  - `refetchInterval` en bandeja, listas, vencidos y detalle visible, pausado sin foco, con el intervalo aprobado.
  - Sin dependencias nuevas.
- **GOTCHA**:
  - El mensaje nunca lleva datos de otro país ni de otro rol.
  - Solo invalida; los datos se vuelven a pedir con los permisos del servidor.
  - El cierre de sesión corta la conexión.

### Paso 5.5 — Consultas e interfaz (`ecc:react-performance`, performance-optimization, shadcn)

#### Task 15: Consultas e índices
- **ACTION**:
  - Revisar el `listar`, la `bandeja` y los `vencidos` con el patrón PÁGINA SIN N+1.
  - Contadores con `count` y sin cargar filas.
  - Una migración nueva (la siguiente disponible) solo si hace falta un índice para los filtros nuevos (por ejemplo, responsable de la etapa actual), verificando antes qué índices existen en V2–V15.
- **GOTCHA**: la meta de 500 pedidos en menos de 2 s es objetivo de Codex (5.B2). Aquí no se mide.

#### Task 16: Interfaz accesible y eficiente
- **ACTION**:
  - Pestañas con `Tabs` de shadcn (`TabsList`/`TabsTrigger`).
  - Etiquetas en la búsqueda y los filtros, y foco visible `ring-ring`.
  - Navegación por teclado.
  - `aria-live="polite"` para los contadores actualizados.
  - Estados con texto, icono y color.
  - `placeholderData: keepPreviousData` al paginar.
  - `select` para derivar datos.
  - Nada de `useEffect` para datos derivados.
- **GOTCHA**: altura mínima de 40 px en los controles (`DESIGN.md`).

---

## Resultado del paso 5.1 (parte A, 2026-10-20) — pendiente de verificación por Codex

**Implementado sin pruebas, builds ni migraciones ejecutadas; sin commit.** F5-D1 opción A aprobada por Richard.

**Archivos:**
- `plazos/domain/CalendarioLaboral.java` (UPDATE): `finDePlazo` (misma regla que `vencimiento`, con `movidaAlLunes`), `fecha` e `inicioDelDia`; `vencimiento` sin cambios de comportamiento (se extrajo `sumar`). Constructores sin cambios.
- `plazos/domain/Cronograma.java`, `ConfiguracionPlazos.java`, `OrigenMeta.java` (CREATE): cálculo puro (R1).
- `plazos/application/port/in/CalcularCronograma.java`, `ConsultarConfiguracionPlazos.java`, `ConfigurarMeta.java`; `port/out/ConfiguracionPlazosRepositorio.java` (CREATE).
- `plazos/application/usecase/CalcularCronogramaCasoDeUso.java`, `ConfiguracionPlazosCasoDeUso.java` (CREATE): sin Spring (R2); la meta solo la cambia el admin con estado vigente (lección A1) y versión (D10).
- `plazos/adapter/out/persistence/ConfiguracionPlazosEntity.java`, `ConfiguracionPlazosJpaRepositorio.java` (CREATE): fila única con `PESSIMISTIC_WRITE`; si falta la fila, error de despliegue (no se inventa meta).
- `plazos/adapter/in/web/ConfiguracionPlazosController.java` (CREATE): `GET`/`PUT /api/v1/configuracion/plazos`; enteros validados como nodos JSON; feriados no vacíos → 400 en `feriados`.
- `plazos/config/PlazosConfig.java` (UPDATE): beans `calcularCronograma` (solo lectura), `consultarConfiguracionPlazos` y `configurarMeta`; `calcularVencimiento()` igual.
- `pedidos/application/port/in/ConsultarCronograma.java`, `pedidos/application/usecase/ConsultarCronogramaCasoDeUso.java`, `pedidos/adapter/in/web/CronogramaController.java` (CREATE); `pedidos/config/PedidosConfig.java` (UPDATE, bean nuevo; los existentes no cambian).
- `db/migration/V14__configuracion_plazos.sql` (CREATE): siguiente versión disponible.
- `documentos/api/openapi.yaml` 0.11.0-borrador; `documentos/modelo-datos.md`; nota en el PRD.

**Desviaciones del plan:**
- El endpoint del cronograma vive en `pedidos` (`CronogramaController`), no en `plazos`: la visibilidad (P-13) y el 404 son del pedido. `plazos` solo calcula (`CalcularCronograma`), y `pedidos` le entrega los datos, como ya hacía con `CalcularVencimiento`. Así se respeta R9 sin el puerto de salida `PedidosConEtapaActiva`, que queda para 5.2 (detección de vencimientos).
- La meta lleva `meta_origen` (`PROPUESTA`/`ADMIN`) porque no tiene aprobación provisional documentada (F5-D6).
- **Meta no aprobada (aclaración de Richard, 2026-10-20):** la meta propuesta de 90 días no es un objetivo aprobado: sirve solo como referencia configurable mientras el cliente responde, y un `diferenciaContraMeta` igual o menor que cero no permite declarar que se cumple un objetivo del cliente. Tampoco lo permite una meta con `origen ADMIN`: el admin la configura, pero el cliente no la fijó. Ninguna pantalla, informe ni correo debe presentar el resultado como "meta cumplida"; a lo sumo, "dentro/fuera de la meta propuesta (pendiente del cliente)".
- `CalendarioLaboral` no ganó una suma "por fecha" aparte: el cronograma usa la misma suma por instantes de `vencimiento`, para no duplicar reglas.

**Reglas implementadas (para revisar):**
- Inicio de cada etapa: su última entrada real si es la actual o ya se recorrió; si no, el fin efectivo de la anterior. Las etapas por delante de la actual ignoran datos de entradas anteriores (devoluciones).
- Fin efectivo: salida real → llegada real (ADR-0018, prevalece) → llegada estimada (F4-D5; el día se toma al inicio en Ecuador, convención técnica) → planificada (inicio + plazo con `finDePlazo`). PENDIENTE sin dato real o estimado → `SIN_ESTIMAR`, aparece en `faltantes` y deja sin inicio a las siguientes, hasta un ancla.
- SIN_PLAZO (A1, Fin) suma cero. Fin, al finalizar el pedido, toma `finalizadoEn` como fecha real.
- `llegada` sale de la etapa con `registraLlegada`; `totalDias` = días corridos de la fecha del pedido a la llegada (métrica del PRD); `diferenciaContraMeta = totalDias − meta`; los dos nulos y `calculable: false` sin llegada.
- Sin feriados ni jornada (⏸).

**Casos para Codex (5.B1):**
- Zona `America/Guayaquil`: un pedido creado el viernes 23:30 en Ecuador cae ese viernes, no el sábado UTC.
- Plazos hábiles (24 h, 3 días, 10 días) que cruzan un fin de semana; `movidaAlLunes` solo cuando el resultado cayó en sábado o domingo; un plazo corrido que cae en fin de semana pasa al lunes.
- Pedido en A2 recién creado: A1 real, A2 en curso, A3–A4 planificadas, A5 (PENDIENTE) `SIN_ESTIMAR` y en `faltantes`, A6–Fin `SIN_ESTIMAR`, `llegada` nula, `calculable: false`, total y diferencia nulos.
- Reentrada: tras devolver de A4 a A2, A2 se planifica desde la nueva entrada y A3–A4 desde el fin planificado de A2, no desde sus fechas anteriores.
- Demora en A7 (F4-D5): A7 `ESTIMADA` con `fechaEstimada`, `procedencia LLEGADA_ESTIMADA`; A8 y A9 (24 h hábiles) se planifican desde ese día; `calculable: true` con la llegada estimada; A10 (PENDIENTE) y Fin `SIN_ESTIMAR`.
- Llegada real con demora previa: prevalece la real (`LLEGADA_REAL`), sin `fechaEstimada`.
- Pedido finalizado (cuando F4-D4/F4-D7/F4-D8 lo permitan): todas `REAL`; Fin con `finalizadoEn`.
- Meta: `GET` da 90, `PROPUESTA`, `metaPendienteDelCliente: true`, `feriados: []`, `version 0`. `PUT` del admin con versión vigente → `ADMIN` y versión nueva; versión vieja → 409 sin cambios; no admin, admin desactivado o JWT con `admin` retirado en la base → 403; `metaDias` 0, texto, decimal o ausente → 400; `feriados` no vacío → 400; dos PUT simultáneos → uno 200 y otro 409.
- Cronograma: cuenta inactiva o sin roles → 403; pedido inexistente → 404; cualquier rol vigente lo ve (P-13).
- ArchUnit R1–R4, R9, `FronterasFaseDosTest` y `FronterasP20Test` con los paquetes nuevos (plazos → usuarios; pedidos → plazos por puerto de entrada).
- V14 sobre una base con V1–V13 y datos; CHECKs de `configuracion_plazos`; `ddl-auto: validate`.

**Pruebas existentes que Codex deberá adaptar (no se modificaron):** `MigracionesIT`, `V2RestriccionesIT` y `MigracionesFase4IT` fijan la última versión en 13 (ahora 14). Los IT de la fase 4 que compartan base pueden encontrar la tabla nueva.

**Sigue pendiente:** F5-D2 (5.4), F5-D3, F5-D4, F5-D5, F5-D7, F5-D9; F4-D3/D4/D6/D7/D8/D9/D10; 3.B3 pospuesto; fases 3 y 4 abiertas.

## Resultado del paso 5.2 (parte A, 2026-10-20) — pendiente de verificación por Codex

**Implementado sin pruebas, builds ni migraciones ejecutadas; sin Google ni correos reales; sin commit.** Reglas F5-D3, F5-D4 y F5-D5 aprobadas provisionalmente por Richard.

**Archivos:**
- `compartido`: `ClaseVencimiento` (ETAPA, URGENCIA); eventos `PlazoVencido` (sistema, sin actor) y `MotivoRetrasoRegistrado`; `EventoDePedido` los permite.
- `historial`: tipo `MOTIVO_RETRASO_REGISTRADO`; `DetalleEvento` + `claseVencimiento`, `venceEn`, `entradaId`, `urgenciaActivacionId` (constructores anteriores conservados); `EventoPedido.desde`, JSON de `datos`, puerto y respuestas web con `claseVencimiento` y `venceEn`.
- `notificaciones`: `PlazoVencido` → aviso al admin con plantilla `VENCIMIENTO` (asunto distinto para ETAPA y URGENCIA); sin destinatarios, fila con destinatario nulo (bloqueo explícito); el texto del correo agrega clase, etapa y vencimiento. `MOTIVO_RETRASO_REGISTRADO` no avisa.
- `pedidos`: `EtapaDePedido.motivoRetraso` (constructor nuevo; los anteriores siguen); `Pedido.urgenciaActivacionId` (constructor nuevo de 21 argumentos; los de 17 y 20 siguen; `UrgenciaActiva` no cambia), `registrarMotivoRetraso`, `tieneVencimientoActivo`; persistencia de `motivo_retraso` y `urgencia_activacion_id`; `PedidosConEtapaActivaJpa` (implementa el puerto de plazos); `RegistrarMotivoRetraso` + caso de uso + `MotivoRetrasoController` (`PUT /api/v1/pedidos/{id}/etapas/{n}/motivo-retraso`); `PedidoResumen.motivoRetraso` deja de ser siempre nulo.
- `plazos`: `AvisoVencimiento`; puertos `DetectarVencimientos`, `RegistrarVencimientosDePedido`, `PedidosConEtapaActiva`, `AvisosVencimientoRepositorio`; casos de uso `RegistrarVencimientosCasoDeUso` y `DetectarVencimientosCasoDeUso`; `AvisoVencimientoSqlRepositorio`; `TareaVencimientos` + `VencimientosAutomaticosConfig` (solo con `decokasa.plazos.vencimientos.automatico=true`); beans en `PlazosConfig`.
- `V15__avisos_vencimiento.sql` (siguiente disponible): `pedido.urgencia_activacion_id` (FK diferible a `entrada_etapa`, backfill estricto de urgencias vigentes, CHECK de coherencia), tipo `MOTIVO_RETRASO_REGISTRADO` en `evento_pedido_tipo`, tabla `aviso_vencimiento` (solo inserción, únicos parciales por plazo, FK al evento).
- `application.yml` y `.env.example`: tarea desactivada por defecto. Contrato 0.12.0-borrador. Frontend: solo compatibilidad del esquema del historial (tipo nuevo y campos del detalle), sin pantallas.

**Atomicidad:** cada pedido se procesa en su propia transacción (REQUIRES_NEW): bloqueo de la fila (igual que las acciones), relectura, `PLAZO_VENCIDO` (el historial lo inserta y el outbox lo encola de forma síncrona, oyentes `MANDATORY`) y aviso único. Si algo falla, se revierte todo y el pedido vuelve a ser candidato en la pasada siguiente. El índice único de la base es la última barrera ante dos pasadas simultáneas.

**Desviaciones:** ninguna de regla. Técnicas: la identidad de la activación se guarda en `pedido.urgencia_activacion_id` (no en `UrgenciaActiva`, para no cambiar la igualdad de ese record que usan pruebas existentes); los candidatos se leen sin transacción y sin bloqueo (la comprobación vinculante es la de la transacción por pedido).

**Casos para Codex (5.B1):**
- V15 sobre una base con V1–V14 y datos: pedidos sin urgencia quedan con `urgencia_activacion_id` nulo; urgencias vigentes enlazadas a su entrada de activación; si una urgencia vigente no tiene entrada coincidente, la migración falla con mensaje claro (y no inventa identidad); CHECK de coherencia; nuevo tipo aceptado; `aviso_vencimiento` rechaza UPDATE, DELETE y TRUNCATE.
- Devolución por producto nuevo: `urgencia_activacion_id` = `entrada_id` de la entrada A2 creada; al volver a A4 se limpia; una segunda devolución tiene otra activación aunque comparta hora.
- Etapa vencida (reloj controlado): un aviso ETAPA con su `entradaId` y `venceEn`; `PLAZO_VENCIDO` sin usuario ni rol; una fila de `notificacion` por admin (plantilla `VENCIMIENTO`); pasadas repetidas no duplican nada.
- Reentrada (A4 → A2 → A3 → A2): la nueva entrada puede volver a avisar; la anterior nunca repite.
- Urgencia y etapa vencidas a la vez: dos eventos y dos avisos (ETAPA y URGENCIA), en el orden ETAPA, URGENCIA; si una ya estaba avisada, solo la otra.
- Plazo PENDIENTE (A5, A7, A10): nunca candidato ni aviso. Pedido finalizado: nunca.
- Sin destinatarios configurados: el aviso queda con destinatario nulo; el vencimiento y su evento existen igual.
- Fallo en medio (por ejemplo, outbox que lanza): no queda aviso, evento ni notificación; la pasada siguiente lo reintenta.
- Dos pasadas simultáneas sobre el mismo pedido: un solo aviso por plazo (bloqueo + índice único).
- Tarea: sin `VENCIMIENTOS_AUTOMATICO=true` no existe el bean; con intervalo fuera de 1 s–24 h no arranca; los logs solo llevan contadores.
- Motivo: actor P-06 o admin con vencimiento activo → 200 con el evento; corrección → nuevo evento y motivo vigente reemplazado; sin vencimiento activo → 409; `entradaEsperadaId` vieja o etapa cambiada → 409 `ETAPA_CAMBIO`; vacío o > 4000 → 400; `consulta`/`asistente_china` solos → 403; usuario con `consulta` + rol responsable → permitido; inactivo → 403; reentrada limpia `motivoRetraso` y conserva el historial; no se encola correo.
- `PedidoResumen.motivoRetraso` y el historial (`claseVencimiento`, `venceEn`) en la API y en el frontend sin errores de esquema.
- ArchUnit R1–R4, R9, `FronterasFaseDosTest`, `FronterasP20Test`.

**Pruebas existentes que Codex deberá adaptar (no se modificaron):** `MigracionesIT`, `V2RestriccionesIT` y `MigracionesFase4IT` (última versión 15); pruebas que comparen el JSON de `DetalleEvento`/`EventoHistorial` campo a campo (dos campos nuevos); pruebas del frontend que fijen la lista de tipos de evento.

**Revisión Java (ecc:java-reviewer, solo lectura, 2026-10-20):** sin errores de compilación ni de ArchUnit detectados a ojo; atomicidad, unicidad y SQL de V15 conformes. Corregido tras la revisión: orden de candidatos por el vencimiento más antiguo (un pedido que falla siempre no tapa a los demás), cuerpo del correo de VENCIMIENTO sin las líneas vacías de origen y destino, Javadoc de `EtapaDePedido`. Abiertos, para Codex o decisión: (1) `MigracionesIT` también fija la lista exacta de tablas (faltan `configuracion_plazos` y `aviso_vencimiento`); (2) `DetectarVencimientosCasoDeUso` solo cuenta los fallidos, sin la causa (la tarea registra contadores, sin datos); (3) el backfill de V15 falla si una base antigua tiene dos entradas del pedido con la misma hora que la activación: probar V14 → V15 con una urgencia vigente; (4) el motivo exige que la etapa esté habilitada, como toda acción P-06 (con A6, A9 y A10 bloqueadas por documentos, sus etapas siguen habilitadas y no cambia nada); (5) `@Scheduled` lee el intervalo de la misma propiedad que la validación, con el mismo valor por defecto.

**Sigue pendiente:** F5-D2 (5.4), confirmar F5-D9 antes de activar la tarea, F5-D7; F4-D3/D4/D6/D7/D8/D9/D10; P-10 (urgencia ante rechazo); 3.B3 pospuesto; fases 3 y 4 abiertas.

## Resultado del paso 5.3 (parte A, 2026-10-20) — pendiente de verificación por Codex

**Implementado sin pruebas, builds ni migraciones ejecutadas; sin Google ni commit. La detección automática de vencimientos sigue desactivada. No se implementó el transporte de actualización de 5.4.**

**Backend (mínimo, contrato 0.13.0-borrador):**
- `GET /pedidos/bandeja` acepta `vencido` y `conUrgencia` (solo `true` filtra; fragmentos SQL fijos, sin concatenar datos). `ConsultarPedidos.bandeja` gana `FiltroBandeja` y conserva la firma anterior como método por defecto; `ConsultaDePedidos.bandeja` recibe los filtros y la hora del servidor.
- `Urgencia.vencida` (respuesta de pedidos): la urgencia vigente ya venció, decidido por el servidor; el navegador no calcula plazos.

**Frontend:**
- Dashboard (`DashboardRol`): búsqueda por número (`BuscarPorNumero`, GET /pedidos/buscar → detalle), `TableroTrabajo` con pestañas Pendientes / Plazo vencido / Con urgencia (filtros y totales del servidor; pestañas ARIA con flechas, Inicio y Fin; estado con texto e ícono) y, para el admin, `PanelVencidos` (GET /pedidos?vencido=true agrupado por etapa y por responsable, con motivo o "Sin motivo registrado"; avisa si hay más de 100). Quien solo tiene paneles provisionales (`consulta`, `asistente_china`) ve la búsqueda y sus paneles, sin bandeja. Se conservan `panelesDe` y las marcas "Provisional".
- `TablaPedidos`: columna opcional "Recorrido" con `LineaCompacta` (etapa actual y etapas de la definición vigente, cargada una vez por tabla; sin pedir el detalle por fila), motivo del retraso bajo el vencimiento y urgencia vencida. Se usa en el dashboard y en la bandeja.
- `CronogramaPedido` en el detalle: meta como "Propuesta · pendiente del cliente" (o configurada por el admin, igual pendiente), aviso de que no es objetivo aprobado ni indica cumplimiento; fechas planificada, estimada y real por separado; estado con texto e ícono; "Plazo pendiente del cliente" y "Sin estimar" en lugar de cero; total y diferencia solo si es calculable; si no, las etapas que faltan.
- `MotivoRetraso` en el panel de la etapa actual: muestra el motivo vigente y, con vencimiento activo (etapa o urgencia, según el servidor), lo deja registrar o corregir a quien tiene acciones del servidor en la etapa o al admin (pista; el servidor decide). Montado con `key` por entrada.
- API: `useBandeja(pagina, filtro, tamano)` (compatible con la llamada anterior), `useConteosBandeja`, `useCronograma`, `useBuscarPedido`, `useRegistrarMotivo`; `useRefrescarPedido` invalida también el cronograma. Esquemas Zod tolerantes (`vencida` opcional).
- Retirados: `TimelineCalculatorView.tsx` (cálculo en días corridos y comandos `/remind` de Slack), `TrackingSearchView.tsx` y `OrderScheduleTab.tsx`. Eran demo sin ruta y nadie los importaba. `utils/dateUtils.ts` y el resto del demo de otras fases se conservan.

**Desviaciones del plan:** no se creó la pantalla de configuración de la meta (`/configuracion/plazos`) ni se implementó `GET /pedidos/vencidos` (admin) del contrato: el panel usa `GET /pedidos?vencido=true`, que ya existía con la visibilidad de P-13. Las pestañas se hicieron a mano porque el componente `tabs` de shadcn no está instalado y agregarlo requería ejecutar la CLI.

**Casos para Codex (5.B2/5.B3 y regresión):**
- Bandeja con `vencido=true` y `conUrgencia=true`: solo los pedidos que corresponden, totales correctos; sin parámetros, igual que antes; consulta/asistente_china sin rol de trabajo reciben bandeja vacía.
- `Urgencia.vencida`: falso antes del vencimiento, verdadero después, falso si el pedido terminó.
- Dashboard por rol: totales de las tres pestañas; cambio de pestaña con teclado; paginación; línea compacta "Etapa N de 11" sin peticiones por fila (inspeccionar la red); admin ve el panel de vencidos agrupado y el aviso de más de 100.
- Búsqueda por número: exacta → abre el detalle; inexistente → "No hay un pedido con ese número."; vacía → aviso.
- Cronograma: pedido en A2 (no calculable, faltan A5, A7, A10), con demora en A7 (estimada), con llegada real; meta siempre como propuesta pendiente; nunca "cumple".
- Motivo: visible con vencimiento activo; registrar y corregir (historial con `MOTIVO_RETRASO_REGISTRADO`); sin permiso → mensaje del servidor; reentrada lo limpia; `consulta` solo no ve el formulario y el servidor responde 403.
- Accesibilidad (5.B3): pestañas ARIA, foco visible, etiquetas, estado con texto e ícono, contraste de `bg-estado-vencido-fondo`.
- Regresión: `BandejaPedidos`, `ListaPedidos`, `DetallePedido` y sus pruebas (las de fixtures MSW necesitan handlers para `/cronograma` y los filtros de bandeja; no se modificaron). Pruebas existentes que pueden verse afectadas: `DashboardRol.test.tsx`, `DetallePedido.test.tsx`, `ListaBandejaCreacion.test.tsx` y las de fase 4 que rendericen el detalle.

**Hallazgo abierto de 5.2 (se mantiene visible):** `DetectarVencimientosCasoDeUso` solo cuenta los pedidos fallidos y no registra la causa; un error repetido no deja rastro diagnóstico (la tarea solo registra contadores). Pendiente de decisión antes de activar la detección automática, que sigue desactivada.

**Sigue pendiente:** 5.4 (F5-D2, ADR-0014), 5.5, configuración de la meta en pantalla, F5-D7 y F5-D9; pendientes del cliente de la fase 4; fases 3 y 4 abiertas; 3.B3 pospuesto.

## Resultado del paso 5.4 (parte A, 2026-10-20) — pendiente de verificación por Codex

**Implementado sin pruebas, builds ni migraciones ejecutadas; sin Google, WebSocket ni commit. Sin cambios de backend ni de contrato REST** (solo la nota de tiempo real del contrato). La detección automática de vencimientos sigue desactivada.

**Archivos:**
- `frontend/src/api/actualizacion.ts` (nuevo): intervalo `VITE_ACTUALIZACION_MS` (30 s por defecto, 10 s–5 min), `opcionesSeguimiento` (intervalo solo con la pestaña visible, recarga `'always'` al recuperar foco o conexión, `meta.seguimiento`), parada del intervalo tras un 4xx de esa consulta y `usePausarActualizacion` (pausa con edición en curso y lectura actual al terminar).
- `frontend/src/api/pedidos.ts`: `opcionesSeguimiento` en lista de pedidos (y panel de vencidos), bandeja y totales, detalle, historial, cronograma y archivos del detalle (los archivos ya no solo refrescan con copias pendientes).
- `NuevaObservacion`, `MotivoRetraso` y `DetallePedido` (diálogos de acción y reasignación): pausan el refresco mientras hay cambios sin enviar o un diálogo abierto.
- `TableroTrabajo`: texto con el intervalo y la demora posible.
- `vite-env.d.ts`, `frontend/.env.example`; ADR-0014 reescrito (accepted equipo, provisional) y su índice; `arquitectura.md` 4.9 y 8.2; nota 5 del contrato.

**Se conserva:** invalidación inmediata tras operaciones propias (`useRefrescarPedido`, `useCrearPedido`); cancelación y vaciado de la caché al cambiar de sesión; claves con el número de sesión; política de sesión (401 → una renovación; la sesión solo se cierra si la renovación responde 401 o 403). Un 403 de un recurso se muestra como permiso denegado (`MENSAJE_SIN_PERMISO`) y no cierra la sesión.

**Límites conocidos:** al empezar una edición, una lectura del intervalo ya programada puede ejecutarse una vez más antes de detenerse; si en ella cambió la entrada de la etapa, el panel se vuelve a montar (lo escrito pertenecía a una entrada ya cerrada y el servidor lo habría rechazado). Las definiciones de flujo, usuarios, roles y bodegas no se refrescan solas.

**Casos para Codex (5.B2):**
- Dos sesiones (roles distintos): un check en una aparece en la otra en ≤ intervalo + petición, sin recargar; en la propia, enseguida.
- Pestaña oculta: no hay peticiones periódicas; al volver, una lectura inmediata aunque la caché sea reciente. Sin red y al recuperarla: lectura inmediata.
- Sin solicitudes solapadas: una respuesta lenta no genera una segunda petición de la misma consulta.
- `VITE_ACTUALIZACION_MS`: vacío o inválido → 30 s; 5000 → 10 s; 600000 → 5 min.
- 403 en una consulta (por ejemplo, cuenta sin roles): se muestra permiso denegado, el intervalo de esa consulta se detiene y la sesión no se cierra; con la renovación rechazada (401/403 en `/auth/renovar`), la sesión sí se cierra como antes.
- Cambio de sesión: no se ven datos de la cuenta anterior ni quedan intervalos activos de ella.
- Formularios: con texto en una observación o un motivo, o con un diálogo de acción abierto, no se refresca solo; al enviar o cerrar, se pide una lectura actual; un 409 por etapa, entrada o versión sigue mostrándose.
- Aislamiento: ningún refresco trae pedidos de otro país o rol que el servidor no autorice.
- Carga: número de peticiones por minuto con 6 usuarios y pestañas visibles.

**Pruebas que pueden verse afectadas (no se modificaron):** las del frontend que controlan tiempos o cuentan peticiones de detalle, bandeja o archivos (ahora tienen intervalo y recarga al recuperar foco), y las que esperaban el intervalo de archivos solo con copias pendientes.

**Sigue pendiente:** aceptación del cliente de que la actualización no es instantánea; 5.5; hallazgo de 5.2 (causas de fallos no registradas) y activación de la detección automática (F5-D9); configuración de la meta en pantalla; pendientes del cliente de la fase 4; fases 3 y 4 abiertas; 3.B3 pospuesto.

## Resultado del paso 5.5 (parte A, 2026-10-20) — pendiente de verificación por Codex

**Implementado sin pruebas, builds, migraciones, mediciones ni Google; sin commit.** La meta de rendimiento (dashboard y búsqueda con 500 pedidos en menos de 2 s) queda como objetivo para la medición de Codex (5.B2); no se declara cumplida.

**Consultas, paginación e índices:**
- Sin consultas por fila: listas y bandeja cargan los ids de la página y luego los pedidos con sus etapas en una consulta (`left join fetch`); personas en lote (`ResumirUsuarios.resumenes`); definición de flujo una vez por flujo; la línea compacta usa la definición vigente cargada una vez por tabla; los totales del dashboard son tres lecturas de una fila.
- Paginación del servidor en lista, bandeja, pestañas del dashboard y, desde 5.5, en el panel de vencidos (50 por página).
- Índices: **no se agregó migración.** Los caminos de acceso ya tienen índice: `etapa_pedido_una_actual` (pedido, ACTUAL), `etapa_pedido_vence` y `etapa_pedido_asignado` (parciales ACTUAL), `pedido_creado_en`, `pedido_tipo_etapa_abiertos`, `pedido_urgencia_vence` (V2), `aviso_vencimiento` (V15). La búsqueda por texto (`lower(...) like '%x%'`) no usa índice B-tree; con ~500 pedidos se espera un recorrido corto. Si la medición de Codex lo exige, la opción es `pg_trgm` (extensión y permisos a decidir) o un índice por número exacto; queda registrado como decisión pendiente, no se implementa sin medición.
- React: `LineaCompacta` memorizada; agrupaciones del panel de vencidos con `useMemo`; `keepPreviousData` al paginar (ya existente).

**Panel de vencidos (no se declara completo con una sola página):** muestra el **total global** del servidor y el **rango de resultados cargados**; los grupos por etapa y la tabla por responsable dicen que cuentan solo los resultados cargados cuando hay más de una página; paginación para recorrer el resto. Los totales globales por etapa y por responsable requerirían una agregación en el servidor (no implementada; ver pendientes).

**Corrección del pendiente de 5.4 (borradores):** el texto sin enviar de una observación o de un motivo vive en `ProveedorBorradores` (por pedido, en memoria; no en el navegador), ligado a la etapa y a la entrada donde se escribió. Si una respuesta tardía o un cambio remoto cambia la etapa o la entrada (o la persona deja de tener acciones), el panel se vuelve a montar sin perder el texto y `BorradoresEnConflicto` lo muestra con aviso, texto de solo lectura, "Copiar texto" y "Descartar borrador". **Nunca se envía solo ni se pasa a la entrada nueva.** Un envío correcto descarta el borrador; un 409 lo conserva. Se mantiene la pausa del refresco mientras hay texto sin enviar.

**Accesibilidad (revisión de la parte A, sin auditoría automática):** pestañas del dashboard con `tablist`/`tab`/`tabpanel`, `aria-selected`, `aria-controls`, foco itinerante (flechas, Inicio, Fin) y `focus-visible:ring-3`; búsquedas con `role="search"` y etiqueta; formularios con `FieldLabel`, contador y error asociado; estados con texto e ícono además de color (vencimiento, urgencia vencida, estados del cronograma, conflicto de borrador); avisos con `aria-live="polite"`; línea compacta con puntos ocultos a lectores y texto "Etapa N de M". La evaluación WCAG 2.2 AA (axe y revisión manual) es de Codex (5.B3).

**Casos para Codex:**
- 5.B2: dashboard, bandeja, lista y búsqueda con 500 pedidos ficticios (caché fría y caliente) por debajo de 2 s; `EXPLAIN ANALYZE` de lista (con y sin texto, etapa y vencido), bandeja (admin y rol, con filtros), conteos y vencidos; N+1 (número de sentencias por página); peticiones del navegador por pantalla.
- Panel de vencidos con más de 50: total global correcto, rango cargado, avisos de "resultados cargados", paginación.
- Borradores: escribir una observación o un motivo, cambiar la etapa desde otra sesión y esperar el refresco → el texto aparece como conflicto, no se envía, se puede copiar y descartar; con 409 al enviar, el texto se conserva; al publicar bien, desaparece; al cambiar de pedido o de sesión, no persiste.
- 5.B3: WCAG 2.2 AA en dashboard, línea de tiempo, panel de etapa, cronograma, motivo y conflicto de borrador (teclado, etiquetas, foco, contraste de `bg-estado-aviso-fondo` y `bg-estado-vencido-fondo`, estado por texto e ícono, avisos).

**Pruebas que pueden verse afectadas (no se modificaron):** las de `DetallePedido`, `NuevaObservacion` y el panel de etapa que dependan del estado local del texto o del montaje; las del dashboard que esperaban el panel de vencidos de 100.

## Cierre de la parte A de la fase 5 (2026-10-20) — pendiente de verificación por Codex

**Implementado sin pruebas, builds, migraciones, mediciones ni Google; sin commit.** La detección automática de vencimientos sigue desactivada.

**1. Configuración de la meta en pantalla** (`frontend/src/api/plazos.ts`, `components/ConfiguracionPlazos.tsx`, ruta `/configuracion/plazos` dentro de `SoloAdmin`, enlace "Meta de plazos" solo para el admin):
- Validación según el contrato: entero desde 1 (hasta el máximo de un entero de 32 bits), con mensaje en el campo; el servidor vuelve a validar (400) y exige admin con estado vigente (403, mostrado como permiso denegado; no cierra la sesión).
- Control de versión: el `PUT` envía la versión leída. Ante 409 `VERSION_DESACTUALIZADA` el valor escrito se conserva, se relee la configuración y se informa el valor vigente; la persona elige "Usar el valor vigente" o "Mantener mi valor" (sobre la versión nueva, y vuelve a guardar). Nada se reenvía solo.
- La meta se identifica siempre como "Propuesta · pendiente del cliente" (o "Configurada por el admin · pendiente del cliente") con el aviso de que no es un objetivo aprobado ni permite declarar cumplimiento. Los feriados se muestran como pendientes del cliente, sin edición.
- Al guardar, los cronogramas se vuelven a pedir.

**2. Diagnóstico del detector de vencimientos:**
- Puerto `DiagnosticoVencimientos` (aplicación, sin Spring) y adaptador `DiagnosticoVencimientosLog` (`plazos/adapter/out/diagnostico`): una línea WARN clave=valor por fallo, `vencimientos_fallo_pedido pasada=… pedido=… categoria=… codigo=… excepcion=… hora=…` o `vencimientos_fallo_pasada …` si falla la lectura de candidatos.
- Identificadores técnicos: la pasada (UUID por ejecución, también en `Resultado` y en la línea resumen de la tarea) y el pedido (UUID). Categoría y código estables: `DOMINIO` (código del contrato), `BLOQUEO`, `BASE_DE_DATOS` (SQLSTATE y nombre de la restricción si Hibernate lo informa) o `INESPERADO` (clase de la causa raíz). Cadena de clases de la excepción. Hora del reloj del servidor.
- No se registra el mensaje ni la traza de la excepción (pueden contener parámetros SQL, motivos escritos por usuarios o correos), ni tokens ni credenciales.
- Un pedido fallido no impide procesar los demás; cada fallo queda registrado (no solo contado). Un fallo del propio registro no detiene la pasada.

**Casos para Codex:**
- Meta: `/configuracion/plazos` solo visible y accesible para el admin (otros roles: "Sin permiso" en la ruta; API 403); `0`, `-1`, `1.5`, `abc`, vacío → error en el campo sin petición; valor válido → 200, versión nueva, cronogramas actualizados; dos sesiones de admin: la segunda recibe 409, conserva su valor, ve el vigente y puede usarlo o mantener el suyo y volver a guardar; admin desactivado en la base → mensaje de permiso, sesión abierta; la pantalla nunca dice "aprobada" ni "cumplida".
- Diagnóstico: con un pedido que falla (por ejemplo, forzando una violación de restricción o un error de dominio en `registrar`) y otros sanos, la pasada procesa los sanos, registra una línea `vencimientos_fallo_pedido` con pasada, pedido, categoría y código correctos, y `fallidos` coincide; un fallo en `candidatos` deja `vencimientos_fallo_pasada`; ninguna línea contiene mensajes de excepción, texto de motivos, correos, tokens ni valores de parámetros SQL (capturar el log con un appender de prueba); ArchUnit R1/R2 (el núcleo no importa SLF4J/Spring/JDBC: la clasificación vive en el adaptador).

**Revisión Java (ecc:java-reviewer, solo lectura):** sin hallazgos críticos ni altos, sin errores de compilación ni de ArchUnit vistos a ojo y sin fugas de datos. Corregido tras la revisión: la pasada interrumpida se propaga como `DetectarVencimientos.PasadaInterrumpida` con su `pasadaId` (mensaje constante, sin copiar la causa) y la tarea lo registra; el adaptador escribe `vencimientos_fallo_sin_clasificar` si su propia clasificación falla (el fallo no queda solo como contador); `BLOQUEO` reconoce además los SQLSTATE 55P03, 40P01 y 40001 y hay una categoría `CONCURRENCIA` para conflictos de versión optimista; nombres de clase anónimos usan el nombre completo; el Javadoc aclara que la hora va en UTC. Casos adicionales para Codex: interbloqueo (40P01) → `BLOQUEO`; versión optimista → `CONCURRENCIA`; fallo en `candidatos` → línea con la misma pasada en `vencimientos_fallo_pasada` y `vencimientos_pasada_interrumpida`.

**Pruebas que pueden verse afectadas (no se modificaron):** las de `DetectarVencimientosCasoDeUso` o `DetectarVencimientos.Resultado` si se escribieron en la parte B (constructor y record con `pasadaId`); las del `Sidebar` que fijan la lista de enlaces del admin.

## Estado real de la parte A de la fase 5

**Pasos 5.1–5.5 implementados y los dos pendientes de alcance (meta en pantalla y diagnóstico del detector) completados.** Nada está compilado, probado ni medido: todo queda **pendiente de la parte B de Codex** (5.B1–5.B3) y de la validación de Richard. La fase 5 **no está cerrada**.

| Pendiente | Tipo | Estado |
|---|---|---|
| Totales globales del panel de vencidos por etapa y por responsable | Mejora opcional (requiere agregación en el servidor) | Sin implementar; sin decisión |
| `GET /pedidos/vencidos` (admin) del contrato | Decisión de contrato | Sin implementar y sin retirar; sin decisión expresa |
| Índice de texto (`pg_trgm`) | Solo si la medición lo exige | Pendiente de medición (Codex) |
| Activar la detección automática | F5-D9 (intervalo y lote provisionales) | Desactivada hasta la verificación y la confirmación de Richard |

**Siguen pendientes fuera de la parte A:** aceptación del cliente sobre la actualización no instantánea (ADR-0014) y sobre la meta; F5-D7 (feriados); pendientes del cliente de la fase 4; fases 3 y 4 abiertas; 3.B3 pospuesto.

## Testing Strategy (parte B, Codex; referencia)

La parte A no escribe ni ejecuta pruebas. Casos que se entregan a Codex:

**5.B1 — Calendario y vencimientos:**
- zona `America/Guayaquil`;
- fines de semana;
- arrastre desde la salida real;
- reentradas;
- ancla de A7 con demora y con llegada;
- etapas `SIN_ESTIMAR`;
- meta 90 editable con versión;
- `feriados` rechazado;
- vencimiento de etapa y de urgencia avisado una vez por entrada;
- reentrada que vuelve a avisar;
- dos pasadas concurrentes;
- `PLAZO_VENCIDO` sin usuario en el historial y el outbox;
- reloj controlado.

**5.B2 — Tiempo real y rendimiento:**
- dos sesiones: un check actualiza la otra sin filtrar datos de otro país o rol;
- 500 pedidos ficticios con caché fría y caliente, en menos de 2 s;
- N+1.

**5.B3:** WCAG 2.2 AA en el dashboard, la línea de tiempo y el panel de etapa; regresión del flujo A hasta donde lo permitan F4-D4, F4-D7 y F4-D8.

**Suites que afectarán V14 y las migraciones siguientes, y que Codex deberá adaptar** (no se tocan en la parte A): `MigracionesIT`, `V2RestriccionesIT` y `MigracionesFase4IT` (última versión), y cualquier prueba que vigile los archivos del demo retirados.

## Validation Commands (parte B, Codex)

```bash
cd backend && ./gradlew check        # pruebas, ArchUnit R1–R4/R9, JaCoCo ≥ 80 %
cd frontend && npm run lint && npm run test && npm run build
docker compose up -d --build --wait  # navegador: dos sesiones, dashboard, cronograma
```

## Acceptance Criteria (cierre de la fase; lo valida Richard tras la parte B)

- [ ] Cronograma aprobado: días hábiles, arrastre y meta configurable; no reproduce las fechas corridas de la calculadora.
- [ ] Vencimientos detectados, con motivo visible y aviso al admin sin duplicados.
- [ ] Actualización entre vistas según ADR-0014, sin exponer datos de otro rol o país.
- [ ] Métricas de 500 pedidos y accesibilidad AA documentadas en `documentos/pruebas/fase-5-informe.md`.

## Completion Checklist (parte A)

- [ ] Patrones de `plazos`, `pedidos`, `notificaciones` y `api/pedidos.ts` respetados.
- [ ] Ningún plazo, feriado, meta ni transporte inventado: lo pendiente queda marcado como ⏸ o [P].
- [ ] V1–V13 y `Header.tsx` sin cambios; cada migración nueva usa la siguiente versión disponible.
- [ ] Contrato, modelo de datos, arquitectura y ADR-0014 actualizados.
- [ ] Al terminar cada paso, el resumen para Codex queda en este plan.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| F5-D1 sin respuesta del cliente: cronograma casi vacío después de A4 | Alta | Media | Estado `SIN_ESTIMAR` explícito y anclas de A7; pedir F4-D3 y F4-D6 al cliente |
| Contradicción entre el PRD y el manual sobre las fechas de la calculadora | Media | Media | F5-D8: prevalece el manual; reformular la señal del PRD con aprobación |
| Avisos duplicados por pasadas concurrentes o reinicios | Media | Media | UNIQUE en `aviso_vencimiento` + bloqueo del pedido + outbox único por evento |
| Fuga de datos por tiempo real | Baja | Alta | Mensajes sin datos, suscripción por país autorizado y relectura con permisos |
| Conflicto de migraciones con la fase 6 en paralelo | Media | Media | Sin reservas (Richard, 2026-10-20): comprobar la siguiente versión disponible al crear cada migración y coordinarla si la fase 6 avanza en paralelo |
| Fase 4 aún abierta: correcciones de Codex | Alta | Media | Corregir en la rama de la fase 4 y fusionar a la 5 sin rebase |

## Notes

- `etapa_pedido.motivo_retraso` y el tipo `PLAZO_VENCIDO` existen desde V2: no hace falta migrarlos.
- `vencida` hoy solo mira `etapa.venceEn`; la urgencia tiene su propio `urgencia_vence_en` (V2), que no se refleja (F5-D4).
- El listado ya pagina ids y carga por lotes; los contadores deben seguir ese patrón.
- La calculadora del demo (`TimelineCalculatorView`) usa `addCalendarDaysWithWeekendRoll` y `SIMULATED_TODAY`: es referencia de la pantalla, no del cálculo.
- El outbox (`notificacion`) es decisión técnica aprobada (F3-D1, 2026-10-16); ADR-0012 sigue diciendo "outbox proposed" en su índice. Se puede corregir el índice al documentar 5.2.


## Decisión expresa de Richard y correcciones 5.B2 — 2026-10-20

El escenario principal de evaluación/revalidación pasa a **un máximo de 20 pedidos ficticios**, por instrucción expresa de Richard. No es un límite del producto ni de la API. Los resultados de 500 pedidos permanecen históricos y no se repetirá esa carga. El objetivo de 500 pedidos del manual permanece registrado: **no se declara cumplido ni retirado por inferencia**. La diferencia de alcance queda explícita; la revalidación siguiente de las correcciones será únicamente con hasta 20.

Implementadas, sin pruebas, builds ni mediciones: F5-B2-H01 reserva viewports desplazables en bandeja y vencidos, ancho de columnas estable y hueco de paginación; F5-B2-H02 sustituye listado completo y materialización de relaciones por proyección mínima por IDs en una sentencia del adaptador JPA. Ambos hallazgos siguen **pendientes de revalidación**, no cerrados por disminuir pedidos. Header.tsx conservado; no cambia permisos, documentos ni configuración de Google/tareas.

El generador opt-in queda limitado a 20; la fuente de 500 se conserva fuera del source set como histórico. Las suites/evidencias antiguas no se modifican ni se ejecutan: Codex debe adaptar el arnés de revalidación a 20, sin escribir resultados sobre el histórico. Casos pendientes: CLS frío/caliente con pocos datos, vacío/error/refresco y teclado; roles múltiples, usuarios sin roles/inactivos/ausentes, cero IDs; variación de usuarios ajenos manteniendo hasta 20 pedidos y número de consultas constante; permisos vigentes, concurrencia y aislamiento entre sesiones. No se acredita compilación o rendimiento nuevos mediante lectura estática. Véase documentos/pruebas/fase-5-informe.md.

OAuth/workers/schedulers y detección automática permanecen desactivados; A6/A9/A10 bloqueadas; 3.B3 pospuesto. Fases abiertas, sin avance a 5.B3.

## Correcciones de accesibilidad de 5.B3 (parte A, 2026-10-21) — pendientes de revalidación por Codex

Solo los hallazgos F5-B3-H01 a H05 de `documentos/pruebas/fase-5-informe.md` (reproductores en `fase-5-evidencias/5.B3/README.md`). No se ocultó contenido ni se retiró ningún control, y no se silenció ninguna regla de axe. Sin pruebas, builds ni mediciones; ninguna corrección está verificada. Cada una queda **pendiente de revalidación por Codex**.

| Hallazgo | Corrección | Archivo | Cómo revalidar |
|---|---|---|---|
| F5-B3-H01, nombres de la línea de tiempo (`label-content-name-mismatch`) | Se retira el `aria-label` que sustituía al texto visible. El nombre sale del contenido en su orden: código, nombre, estado y fecha. El estado se lee una vez, en texto `sr-only`; los rótulos visibles "Etapa actual" y "Devuelta" que lo repiten quedan con `aria-hidden` para no leerse dos veces. La fecha lleva el calificativo "salió el" o "vence el". Ejemplo de nombre: "A2 Codificación, completada, salió el 14 oct". | `frontend/src/components/pedidos/LineaDeTiempo.tsx` | Axe en detalle A6/A8/A10 a 1440 y 390 px. Comprobar que el nombre empieza con el texto visible (control por voz) y que la lectura no duplica el estado. La revisión normativa de las fechas sigue pendiente, como indica el README. |
| F5-B3-H02, desplazamiento del cronograma en móvil (`scrollable-region-focusable`) | El marco de la tabla del cronograma es una región enfocable (`role="region"`, `tabIndex=0`, nombre "Fechas de cada etapa (desplazable)", foco visible). El contenedor interno de shadcn deja de desplazar, para que no quede un segundo desplazamiento sin foco. `ui/table.tsx` no cambia. | `frontend/src/components/pedidos/CronogramaPedido.tsx` | A 390 px, llegar con Tab a la región y desplazar con las flechas; axe sin `scrollable-region-focusable`. |
| F5-B3-H03, contraste de los botones del conflicto de meta (3,38:1) | Los dos botones outline del conflicto 409 usan `text-foreground` en lugar de heredar el rojo atenuado de la alerta. No cambian ni el texto ni el comportamiento. | `frontend/src/components/ConfiguracionPlazos.tsx` | Repetir el conflicto A=77 / B=81 y pasar axe `color-contrast` (≥ 4,5:1). Codex puede comprobar si otros botones outline dentro de `Alert variant="destructive"` (los "Reintentar") tienen el mismo patrón: no aparecieron en el barrido y no se cambiaron. |
| F5-B3-H04, cambios por polling sin anuncio | El tablero tiene una región `role="status"` (discreta, no urgente), siempre presente y `sr-only`. Anuncia "Lista actualizada a las HH:MM: N pendientes, N con plazo vencido y N con urgencia" solo cuando la lista visible o los totales ya conocidos cambian dentro de la misma pestaña y página. No anuncia la carga inicial, el cambio de pestaña o página, la llegada de los totales ni un sondeo sin cambios (los datos no cambian de referencia, por el structural sharing). No mueve el foco. | `frontend/src/components/dashboards/TableroTrabajo.tsx` | Repetir el reproductor A7→A8 desde otra sesión: un solo anuncio y el foco intacto; varios sondeos sin cambios: cero anuncios. Validar con NVDA/VoiceOver: la salida hablada sigue sin acreditarse. |
| F5-B3-H05, desbordamiento del shell a 390 px (documento de 456 px) | Sin tocar `Header.tsx`: el layout permite que el encabezado pase a dos líneas (`flex-wrap`) y que el bloque de usuario encoja (`min-w-0`, `max-w-full`), para que el nombre use el truncado que ya tenía. "Cerrar sesión" y el menú siguen visibles. | `frontend/src/App.tsx` | A 390 px, `scrollWidth` igual al viewport y "Cerrar sesión" visible y alcanzable con Tab. El menú horizontal sigue desplazándose dentro de su lista, con enlaces enfocables. |

**Pruebas que pueden verse afectadas (no se modificaron):**
- `DetallePedido.test.tsx` busca los botones por nombre: `/A3 Revisión y aprobación, etapa actual/`, `/A2 Codificación, completada/`, `/A1 Crear solicitud de pedido, completada/`. El nuevo nombre conserva ese prefijo, pero depende de cómo el entorno de pruebas concatene los `span`; hay que confirmarlo al ejecutar.
- Cualquier prueba del tablero que cuente las regiones `status`: ahora hay una más, siempre presente.

Se conservan:
- `Header.tsx` sin cambios;
- A6/A9/A10 bloqueadas;
- los pendientes de aceptación del polling, la meta propuesta y el rendimiento (objetivo de 500 sin cumplir ni retirar; 80 % de cobertura del frontend).

La detección automática, OAuth y Google siguen desactivados. Sin commit, push ni merge; ninguna fase cerrada.

### Remanente de H05 (parte A, 2026-10-21) — pendiente de revalidación por Codex

En la revalidación de Codex, H01–H04 quedaron cerrados y H05 solo en parte: el documento ya mide 390 px y "Cerrar sesión" funciona. Lo que sigue abierto: a 390 px, "Bodegas" (x 307→414) y "Roles" (x 361→446) quedan recortados al recibir Tab en el menú (`5.B3-revalidacion/menu-parcial-390.json`).

**Causa:** en móvil, el menú era una tira de una línea con `overflow-x-auto`. Al llegar con Tab a un enlace que ya se ve en parte, el navegador no desplaza la tira, así que el enlace queda cortado contra el borde.

**Corrección (`frontend/src/components/Sidebar.tsx`):**
- En móvil, la lista pasa a `flex-wrap` y deja de desplazarse en horizontal: los enlaces que no caben pasan a la línea siguiente.
- Desde `md` sigue la columna lateral (`md:flex-col md:flex-nowrap`), sin cambios.
- Ningún enlace se oculta, se acorta ni pierde el acceso por teclado.
- El orden de Tab no cambia.
- El menú ocupa más alto en móvil (unas tres líneas con el admin).
- `Header.tsx`, el cierre de sesión (arreglo de `App.tsx`) y H01–H04 se conservan.

**Cómo revalidar:** admin ficticio a 390 px; Tab desde "Inicio" por todo el menú. Para cada enlace, el rectángulo y el anillo de foco quedan dentro del viewport sin recorte; `scrollWidth` igual a 390; cada enlace se activa con Enter. Repetir a 1440 px para confirmar la columna lateral. Pruebas que pueden verse afectadas (no se modificaron): ninguna conocida comprueba las clases del menú.
