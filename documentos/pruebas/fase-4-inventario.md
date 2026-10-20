# Inventario manual del avance de fase 4

Fecha: 2026-10-20. Rama comprobada: `fase-4-etapas-a5-a11`. **Índice vacío** al revisar; no se ejecutaron `git add`, commit, push ni merge. No se ejecutaron suites, builds o migraciones para preparar este inventario.

**Estado a guardar:** pruebas locales aprobadas: última verificación backend `check` 783 casos; última suite frontend global 312 casos y ampliación posterior 66/66 afectados, incluidos doce nuevos; tipos aprobados. Son ejecuciones distintas, no una nueva suite global de 324 casos. Última cobertura frontend global **65,82 %**, objetivo adicional ≥80 % **pendiente**. A1–Fin real sigue bloqueado por documentos **A6/A9/A10**. **Fase 4 abierta; fase 3 abierta y 3.B3 pospuesto.** OAuth, workers y schedulers reales desactivados.

## Selección exacta

**636 rutas**, separadas en 597 normales y 39 logs ignorados necesarios. Los tres manifiestos son UTF-8 sin BOM, una ruta relativa a la raíz por línea, sin comodines ni directorios amplios:

- [Selección completa](fase-4-seleccion-final.txt).
- [Rutas normales](fase-4-seleccion-normal.txt).
- [Únicas rutas autorizadas para `git add -f`](fase-4-seleccion-ignorados.txt): 39 logs originales de fase 4 citados por el informe; todos afectados por `*.log`.
- [Clasificación, revisión y comprobaciones](fase-4-revision-inventario.json).

| Grupo | Rutas | Contenido |
|---|---:|---|
| Código | 73 | 58 backend: bodegas, flujo A5–A11, demora/llegada, cierre e historial ordenado; 15 frontend: API, navegación y pantallas reales. |
| Migraciones | 5 | V9–V13, incluidos códigos de bodega y secuencia de historial. |
| Pruebas y fixtures | 15 | Nueve archivos backend; cinco suites frontend y su fixture compartido. Adaptaciones de suites previas exigidas por fase 4 incluidas, sin añadir evidencias antiguas de fase 2. |
| Documentación | 12 | Plan, ADR/índice, OpenAPI, modelo, entrega 4.6, informe y cinco archivos de este inventario. |
| Evidencias | 531 | Logs, JUnit/JaCoCo, contadores V8, metadatos, hashes, ejecutores y pruebas de navegador simuladas necesarios para reproducir y conservar el histórico del informe. |

## Rutas de código, migraciones, pruebas y documentación

### Código

- `backend/src/main/java/com/decokasa/seguimiento/bodegas/adapter/in/web/BodegaController.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/adapter/in/web/BodegaRespuesta.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/adapter/out/persistence/BodegaDatos.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/adapter/out/persistence/BodegaEntity.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/adapter/out/persistence/BodegaJpaRepositorio.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/port/in/ActualizarBodega.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/port/in/ConsultarBodegaActiva.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/port/in/ConsultarBodegas.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/port/in/CrearBodega.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/port/in/DescribirBodega.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/port/out/BodegaRepositorio.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/usecase/ActualizarBodegaCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/usecase/ConsultarBodegasCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/usecase/CrearBodegaCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/application/usecase/MantenimientoAutorizado.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/config/BodegasConfig.java`
- `backend/src/main/java/com/decokasa/seguimiento/bodegas/domain/Bodega.java`
- `backend/src/main/java/com/decokasa/seguimiento/compartido/domain/eventos/BodegaAsignada.java`
- `backend/src/main/java/com/decokasa/seguimiento/compartido/domain/eventos/DemoraReportada.java`
- `backend/src/main/java/com/decokasa/seguimiento/compartido/domain/eventos/EventoDePedido.java`
- `backend/src/main/java/com/decokasa/seguimiento/compartido/domain/eventos/PedidoFinalizado.java`
- `backend/src/main/java/com/decokasa/seguimiento/compartido/domain/flujo/ContratoDeEtapa.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/adapter/in/web/DefinicionFlujoRespuesta.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/adapter/out/persistence/DefinicionEtapaEntity.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/adapter/out/persistence/DefinicionFlujoJpaRepositorio.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/application/port/in/DescribirFlujo.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/application/port/in/ResolverTransicion.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/application/usecase/DescribirFlujoCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/application/usecase/ResolverTransicionCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/domain/DefinicionEtapa.java`
- `backend/src/main/java/com/decokasa/seguimiento/flujos/domain/DefinicionFlujo.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/adapter/in/web/EventoHistorialRespuesta.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/adapter/out/persistence/EventoPedidoDatos.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/adapter/out/persistence/EventoPedidoEntity.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/adapter/out/persistence/HistorialJpaRepositorio.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/application/port/in/ConsultarHistorialPedido.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/application/port/out/HistorialRepositorio.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/application/usecase/ConsultarHistorialPedidoCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/application/usecase/ResumirEtapasDePedidoCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/domain/DetalleEvento.java`
- `backend/src/main/java/com/decokasa/seguimiento/historial/domain/EventoPedido.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/web/AccionEtapaController.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/web/FechaEstimadaLlegadaDeserializer.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/web/ObservacionRespuesta.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/web/PedidoRespuestas.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/web/SolicitudAccion.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/out/persistence/PedidoEntity.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/out/persistence/PedidoJpaRepositorio.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/application/port/in/EjecutarAccionEtapa.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/application/port/in/VistasDePedido.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/application/usecase/ConsultarPedidosCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/application/usecase/EjecutarAccionEtapaCasoDeUso.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/config/PedidosConfig.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/domain/AccionSolicitada.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/domain/DemoraAduana.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/domain/EtapasHabilitadas.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/domain/Pedido.java`
- `backend/src/main/java/com/decokasa/seguimiento/pedidos/domain/ReglaDeTransicion.java`
- `frontend/src/App.tsx`
- `frontend/src/api/bodegas.ts`
- `frontend/src/api/esquemas.ts`
- `frontend/src/api/pedidos.ts`
- `frontend/src/components/CatalogoBodegas.tsx`
- `frontend/src/components/Sidebar.tsx`
- `frontend/src/components/dashboards/DashboardRol.tsx`
- `frontend/src/components/pedidos/BandejaPedidos.tsx`
- `frontend/src/components/pedidos/DetallePedido.tsx`
- `frontend/src/components/pedidos/DialogoAccion.tsx`
- `frontend/src/components/pedidos/Distintivos.tsx`
- `frontend/src/components/pedidos/HistorialPedido.tsx`
- `frontend/src/components/pedidos/PanelEtapa.tsx`
- `frontend/src/components/pedidos/formato.ts`
- `frontend/src/rutas.tsx`

### Migraciones

- `backend/src/main/resources/db/migration/V10__demora_aduana.sql`
- `backend/src/main/resources/db/migration/V11__etapa_de_llegada.sql`
- `backend/src/main/resources/db/migration/V12__bodega_inactiva_codigo.sql`
- `backend/src/main/resources/db/migration/V13__historial_secuencia.sql`
- `backend/src/main/resources/db/migration/V9__bodegas_llegada.sql`

### Pruebas y fixtures

- `backend/src/test/java/com/decokasa/seguimiento/MigracionesIT.java`
- `backend/src/test/java/com/decokasa/seguimiento/V2RestriccionesIT.java`
- `backend/src/test/java/com/decokasa/seguimiento/archivos/HistorialSecuenciaH01IT.java`
- `backend/src/test/java/com/decokasa/seguimiento/archivos/MigracionesArchivosB1IT.java`
- `backend/src/test/java/com/decokasa/seguimiento/archivos/MigracionesFase4IT.java`
- `backend/src/test/java/com/decokasa/seguimiento/arquitectura/FronterasFaseDosTest.java`
- `backend/src/test/java/com/decokasa/seguimiento/pedidos/Backend24IT.java`
- `backend/src/test/java/com/decokasa/seguimiento/pedidos/Fase4IT.java`
- `backend/src/test/java/com/decokasa/seguimiento/pedidos/domain/Fase4ReglasTest.java`
- `frontend/src/components/pedidos/AccionesPedido.test.tsx`
- `frontend/src/components/pedidos/ArchivosYPermisos.test.tsx`
- `frontend/src/components/pedidos/Fase4Pantallas.test.tsx`
- `frontend/src/components/pedidos/Fase4Ramas.test.tsx`
- `frontend/src/components/pedidos/formato.test.ts`
- `frontend/src/test/pedidosFicticios.ts`

### Documentación

- `.claude/PRPs/plans/fase-4-etapas-a5-a11.plan.md`
- `documentos/adr/0018-etapa-de-llegada-explicita.md`
- `documentos/adr/README.md`
- `documentos/api/openapi.yaml`
- `documentos/fase-4-paso-4.6-implementacion.md`
- `documentos/modelo-datos.md`
- `documentos/pruebas/fase-4-informe.md`
- `documentos/pruebas/fase-4-inventario.md`
- `documentos/pruebas/fase-4-revision-inventario.json`
- `documentos/pruebas/fase-4-seleccion-final.txt`
- `documentos/pruebas/fase-4-seleccion-ignorados.txt`
- `documentos/pruebas/fase-4-seleccion-normal.txt`

## Evidencias necesarias

Todas las rutas se enumeran literalmente en la selección completa y en el JSON por grupo. Se preservan los fallos iniciales, reproductores y revalidaciones; no se sustituyen los originales por evidencia posterior. Los JUnit de otras suites dentro del check completo de fase 4 acreditan su regresión y cobertura global, no son las evidencias pendientes de fase 2.

| Carpeta bajo `documentos/pruebas/fase-4-evidencias` | Archivos |
|---|---:|
| `(raíz)` | 14 |
| `arnes-inicial-junit` | 8 |
| `baseline-junit` | 58 |
| `cobertura-frontend` | 39 |
| `final-junit` | 61 |
| `historial-consultas` | 7 |
| `revalidacion-f4p01` | 79 |
| `revalidacion-h01` | 150 |
| `verificacion-4.5-4.6` | 115 |

Se excluyen expresamente `verificacion-4.5-4.6/browser-artifacts/.last-run.json`, `verificacion-4.5-4.6/browser-artifacts-20261019-163412/.last-run.json` y `verificacion-4.5-4.6/browser-artifacts/pantallas-catalogo-guarda--faa5d-serva-borrador-sin-reenviar/trace.zip`. No son enlaces directos del informe. La carpeta enlazada conserva contexto del fallo y captura revisada con datos ficticios. No se incluyen HTML/CSS de reportes, cachés, `build`, `dist`, `coverage` ni binarios del arnés. Los XML/JSON copiados y logs seleccionados sí son evidencia histórica necesaria.

## Seguridad y referencias

Se aplicó `security-review`: escaneo de las 631 rutas de contenido previo a este inventario, revisión de las nueve coincidencias, XML JUnit sin propiedades de entorno y revisión visual de la captura. **Sin secretos reales detectados**: tres coincidencias son nombres de campos/esquemas, cuatro son marcadores ficticios de la conexión inválida de las guardas y dos son tokens simulados de Playwright. No se muestran sus valores aquí. Las direcciones de correo encontradas pertenecen a `ejemplo.test`; las rutas locales y hashes de conservación son metadatos necesarios, no credenciales. Los JSON de hashes que mencionan `.env.local`, Header o el índice contienen huellas, no esos archivos. No se han abierto ni copiado credenciales externas.

Se comprobaron inicialmente **107 referencias del informe** (incluidas repeticiones) y **132 referencias Markdown** de la selección, todas existentes y cubiertas por rutas seleccionadas o archivos ya rastreados; la comprobación final abarca **141 enlaces locales**, sin referencias rotas ni dependencias sin seleccionar/rastrear. El manual ya está rastreado y no tiene cambios: **no se vuelve a agregar ni se fuerza**. Arquitectura y DESIGN.md también se conservan como dependencias ya rastreadas.

## Exclusiones y separación de cambios preexistentes

- `frontend/src/components/Header.tsx`: cambio preexistente conservado, fuera de los tres manifiestos; tampoco se descarta del árbol de trabajo.
- `.vscode/launch.json`, `documentos/auditoria-implementacion-2026-10-13.md`, `documentos/skills-manual-instaladas.md`, documentos/imágenes de derivadas y sus temporales: ajenos a fase 4.
- `documentos/pruebas/evidencias-fase-2/`, `documentos/pruebas/arnes-b3/`, evidencias y documentos de fase 3: fuera de la selección.
- `.tmp/`, `documentos/.tmp*`, `.env*`, usuarios reales/semillas locales, almacenamiento real, JSON OAuth, tokens, claves, volúmenes, volcados y archivo de 100 MiB: excluidos; no se inspeccionaron credenciales externas ni se propone agregarlos.

Los cambios pequeños de `App.tsx` y `DashboardRol.tsx` sí corresponden a 4.6 (comentarios sobre rutas activas y descripciones de roles). Los ajustes en `AccionesPedido`, `ArchivosYPermisos` y las pruebas de migración antiguas responden al bloqueo documental, invalidación de consultas y V13 de fase 4; se conservaron sus comprobaciones válidas. No se selecciona un directorio entero para arrastrar cambios ajenos.

## Comandos para ejecutar manualmente

**No ejecutados por el agente.** Revisar los manifiestos antes de utilizarlos. Las guardas detienen el procedimiento ante rama inesperada, índice con rutas ajenas o discrepancia; no descartan nada. `-f` se aplica únicamente a la lista explícita de los 39 logs revisados.

```powershell
Set-Location 'C:\Users\Richard\Desktop\decoWeb'
if ((git branch --show-current).Trim() -ne 'fase-4-etapas-a5-a11') { throw 'Rama inesperada' }
$seleccionF4 = @(Get-Content -LiteralPath 'documentos/pruebas/fase-4-seleccion-final.txt' -Encoding UTF8)
$indiceF4 = @(git -c core.quotepath=false diff --cached --name-only)
$ajenosF4 = @($indiceF4 | Where-Object { $_ -cnotin $seleccionF4 })
if ($ajenosF4.Count) { throw 'Hay rutas ajenas en el indice; revisar sin descartar cambios' }

git add --pathspec-from-file=documentos/pruebas/fase-4-seleccion-normal.txt
if ($LASTEXITCODE -ne 0) { throw 'Fallo al preparar rutas normales; conservar lo ya preparado y revisar' }
git add -f --pathspec-from-file=documentos/pruebas/fase-4-seleccion-ignorados.txt
if ($LASTEXITCODE -ne 0) { throw 'Fallo al preparar los logs revisados; conservar lo ya preparado y revisar' }

$indiceF4 = @(git -c core.quotepath=false diff --cached --name-only)
$diferenciasF4 = @(Compare-Object -CaseSensitive -ReferenceObject @($seleccionF4 | Sort-Object -Unique) -DifferenceObject @($indiceF4 | Sort-Object -Unique))
if ($diferenciasF4.Count) { $diferenciasF4; throw 'La seleccion y el indice no coinciden; no continuar' }
git diff --cached --check
# Revisar los avisos sin reformatear los logs ni otras evidencias originales.
git diff --cached --stat
```

Después de revisar el índice y resolver cualquier discrepancia o aviso, el comando de commit propuesto es:

```powershell
git commit -m "Guardar avance de fase 4: A5-A11 y pruebas locales; fase abierta"
```

No se propone push ni merge. El commit no implica cerrar las fases ni aceptar el recorrido bloqueado, cobertura pendiente o integración Google pospuesta.
