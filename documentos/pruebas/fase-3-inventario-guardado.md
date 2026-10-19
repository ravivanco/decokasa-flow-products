# Inventario para guardar manualmente el avance de fase 3

2026-10-19 · Rama observada: **fase-3-drive-gmail**. **Solo inventario; ningún git add/commit/push/merge ejecutado.** Índice vacío al revisar. Estado inicial: **943 rutas pendientes**, 34 modificadas y 909 sin seguimiento; el CSV enumera ese estado antes de crear este inventario y su CSV. Hay **147 rutas propuestas** contando esos dos archivos nuevos; la selección no guarda todavía toda la fase por los documentos/arnés retenidos.

**Pruebas locales aprobadas:** check oficial, 668 casos, cero fallos/errores/omisiones; cobertura LINE global **90,64 % (4302/4746)** y archivos **86,34 % (1391/1611)**. No se reejecutaron pruebas en este inventario. **Google real 3.B3 pospuesto y fase 3 abierta. OAuth, workers y schedulers permanecen desactivados.** Cuota lógica revalidada; límite físico de temporales, retención/liberación y decisiones productivas pendientes.

## Resultado de revisión de datos y secretos

- Revisión estática de los archivos pendientes de texto: patrones de tokens Google, claves API Google, llaves privadas PEM y JWT compactos; nombres sensibles rastreados y diff del índice. **Sin coincidencias de esos secretos reales detectadas; sin archivos de credenciales/tokens rastreados por nombre ni cambios preparados.** Es una comprobación local del estado actual, no garantía sobre toda la historia remota ni sobre formatos no reconocidos.
- `backend/.env.local`, `backend/datos/` y nombres de credenciales/llaves OAuth quedan ignorados según `git check-ignore`. Los ejemplos `.env.example` mantienen campos de credenciales vacíos y flags false. No se usan `git add -f` o `git add .`.
- Las claves/tokens que aparecen en fuentes de pruebas son valores explícitamente ficticios. Las palabras `client_secret`/`access_token` son campos de protocolo, no credenciales descargadas. No hay documentos comerciales o archivos de datos de producción seleccionados. La inspección no abrió las credenciales externas.
- **Sí hay metadatos identificativos reales** en algunos documentos/arnés: dirección de correo de empresa y rutas del usuario local. No son prueba de una fuga de secreto, pero esos archivos se retienen para cumplir la petición de no incluir datos reales. El inventario no reproduce los valores. No se alteran ni debilitan guardas del arnés.
- No se añaden reportes HTML, ZIP, copias del índice `.bin`, snapshots de pruebas, XML JUnit con salidas completas, duplicados intermedios o logs ignorados. El JaCoCo final y el JSON de métricas se conservan como evidencia generada necesaria, en una sola copia; se han revisado como texto y no contienen los patrones de secretos/metadatos reales anteriores.

## Separación de alcance

| Grupo | Tratamiento |
|---|---|
| Código backend/frontend y V4–V8 | Fase 3: OAuth/Drive/Gmail/outbox, carga reanudable, cuotas, pantallas y APIs |
| Código ya existente con modificaciones de fase 3 | Incluir la versión modificada revisada: archivos, historial, seguridad, errores, cliente API y eliminación de demo. No confundir «archivo existente» con «cambio ajeno» |
| Pruebas heredadas adaptadas | MigracionesIT, V2RestriccionesIT, V3MigracionIT, AlmacenLocalRealTest y Backend24IT: cambios de fase 3, aunque sus nombres incluyan fases anteriores |
| `frontend/src/components/Header.tsx` | **Aparte**: modificación preexistente de marca/texto; no incluida en los comandos |
| `documentos/pruebas/evidencias-fase-2/` | **349 rutas preexistentes**, conservar y guardar por separado, no incluir en fase 3 |
| `.vscode/launch.json`, auditoría anterior y catálogo de skills | Preexistentes/ajenos: aparte, sin borrar |
| DOCX de derivadas y `.tmp-derivadas/` | Trabajo ajeno a fase 3; excluir de esta selección |
| Artefactos ignorados | `.tmp/`, build/dist/node_modules/coverage, `.env.local`, almacenamiento y logs: no forzar su inclusión |

## Archivos retenidos antes de preparar documentación completa

**No se proporcionan comandos para añadirlos todavía.** Revisar/anonimizar copias publicables de la documentación y decidir el tratamiento del destinatario fijo del arnés antes de incluir ese conjunto. Este inventario no modifica originales. En particular, el plan y el informe completos siguen disponibles localmente pero no forman parte de la selección sin metadatos reales.

| Ruta | Motivo |
|---|---|
| `.claude/PRPs/plans/fase-3-drive-gmail.plan.md` | Revisar identificadores/rutas reales: correo_empresa_real, ruta_usuario |
| `documentos/fase-3-paso-3.5.md` | Revisar identificadores/rutas reales: correo_empresa_real |
| `documentos/google-oauth-paso-3.1.md` | Revisar identificadores/rutas reales: ruta_usuario |
| `documentos/pruebas/arnes-b3/README.md` | Arnes debe revisarse como conjunto: contiene destinatario real aprobado solo para 3.B3; no modificar guardas |
| `documentos/pruebas/arnes-b3/arnes-b3.ps1` | Arnes debe revisarse como conjunto: contiene destinatario real aprobado solo para 3.B3; no modificar guardas |
| `documentos/pruebas/arnes-b3/b3.init.gradle` | Arnes debe revisarse como conjunto: contiene destinatario real aprobado solo para 3.B3; no modificar guardas |
| `documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/ArnesGoogleB3.java` | Arnes debe revisarse como conjunto: contiene destinatario real aprobado solo para 3.B3; no modificar guardas |
| `documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/GuardasB3.java` | Arnes debe revisarse como conjunto: contiene destinatario real aprobado solo para 3.B3; no modificar guardas |
| `documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/GuardasB3Test.java` | Arnes debe revisarse como conjunto: contiene destinatario real aprobado solo para 3.B3; no modificar guardas |
| `documentos/pruebas/fase-3-B3-procedimiento.md` | Revisar identificadores/rutas reales: correo_empresa_real |
| `documentos/pruebas/fase-3-evidencias/revalidacion-B1/resumen-revalidacion.json` | Revisar identificadores/rutas reales: ruta_usuario |
| `documentos/pruebas/fase-3-informe.md` | Revisar identificadores/rutas reales: correo_empresa_real, ruta_usuario |

El CSV distingue también generados redundantes e históricos; no se borra ninguno. Los enlaces de informes hacia evidencia no seleccionada solo funcionarán en el entorno local hasta decidir qué versión publicable conservar. La selección propuesta es un guardado **parcial**, no un commit final listo para publicar toda la fase.

## Comandos preparados, sin ejecutar

Ejecutarlos manualmente desde la raíz del repositorio, solo después de revisar el inventario. Son rutas de archivos concretas, sin glob, sin directorios amplios, sin Header.tsx ni datos privados retenidos. Las continuaciones de línea son PowerShell.

```powershell
# Abrir PowerShell en la raiz local del repositorio decoWeb antes de ejecutar estos comandos.
git status --short
git diff --cached --name-only
```

### Configuración segura (3 archivos)

```powershell
git add -- `
  ".env.example" `
  ".gitignore" `
  "backend/.env.example"
```

### Código backend (81 archivos)

```powershell
git add -- `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/in/tareas/TareaCopiaArchivos.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/in/web/CargasController.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/in/web/ConsultaDriveController.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/in/web/GoogleOAuthController.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/almacen/AlmacenCargasEnDisco.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/almacen/AlmacenLocalEnDisco.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/almacen/BloqueoCopiasEnDisco.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/drive/GoogleDriveAlmacen.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/google/AlmacenTokensGoogle.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/google/ArchivosPrivadosGoogle.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/google/GoogleOAuthUsuario.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/google/ProtectorSesionDriveAes.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/persistence/CargaSqlRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/persistence/CarpetaDriveSqlRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/persistence/ConsultaCopiasSql.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/persistence/CopiaAdjuntoSqlRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/persistence/CuotaSqlRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/in/AutorizarCuentaGoogle.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/in/ConsultarArchivosDrive.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/in/GestionarCarga.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/in/ReintentarCopiasPendientes.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/AlmacenArchivos.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/AlmacenCargas.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/BloqueoCopiaLocal.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/CargaRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/CarpetaDriveRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/ConexionOAuthGoogle.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/ConsultaCopias.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/CopiaAdjuntoRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/CuotaAlmacenamiento.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/PedidosParaCopias.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/out/ProtectorSesionDrive.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/usecase/AutorizarCuentaGoogleCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/usecase/ConsultarArchivosDriveCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/usecase/CopiarPendientesCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/usecase/GestionarCargaCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/usecase/SubirArchivoCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/ArchivosConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/CargasConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/ConsultaDriveConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/CopiaDriveAutomaticaConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/CopiaDriveConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/DriveConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/GoogleOAuthConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/domain/CargaArchivo.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/domain/CuotaAlmacenamientoExcedida.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/domain/GoogleNoDisponible.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/domain/PoliticaCopia.java" `
  "backend/src/main/java/com/decokasa/seguimiento/archivos/domain/ProblemaCarga.java" `
  "backend/src/main/java/com/decokasa/seguimiento/compartido/adapter/in/web/ManejadorErrores.java" `
  "backend/src/main/java/com/decokasa/seguimiento/compartido/application/port/out/AccesoCuentaGoogle.java" `
  "backend/src/main/java/com/decokasa/seguimiento/compartido/application/port/out/PublicadorEventosRegistrados.java" `
  "backend/src/main/java/com/decokasa/seguimiento/compartido/config/CompartidoConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/compartido/domain/CodigoError.java" `
  "backend/src/main/java/com/decokasa/seguimiento/compartido/domain/FalloIntegracionGoogle.java" `
  "backend/src/main/java/com/decokasa/seguimiento/compartido/domain/eventos/EventoPedidoRegistrado.java" `
  "backend/src/main/java/com/decokasa/seguimiento/configuracion/SeguridadWebConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/historial/application/usecase/RegistrarEventoPedidoCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/historial/config/HistorialConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/adapter/in/eventos/OyenteEventosNotificacion.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/adapter/in/web/NotificacionesController.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/adapter/out/gmail/GmailNotificador.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/adapter/out/persistence/NotificacionSqlRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/in/ConsultarNotificaciones.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/in/EncolarNotificacion.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/in/EnviarNotificacionesPendientes.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/out/DirectorioDestinatarios.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/out/NotificacionRepositorio.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/out/Notificador.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/out/RegistroNotificaciones.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/port/out/TrabajoCorreo.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/usecase/ConsultarNotificacionesCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/usecase/EncolarNotificacionCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/application/usecase/EnviarNotificacionesCasoDeUso.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/config/CorreoAutomaticoConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/config/CorreoConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/config/GmailConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/config/NotificacionesConfig.java" `
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/domain/Notificacion.java" `
  "backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/archivos/PedidosParaCopiasAdaptador.java" `
  "backend/src/main/resources/application.yml"
```

### Migraciones V4–V8 (5 archivos)

```powershell
git add -- `
  "backend/src/main/resources/db/migration/V4__drive_notificaciones_outbox.sql" `
  "backend/src/main/resources/db/migration/V5__cargas_reanudables.sql" `
  "backend/src/main/resources/db/migration/V6__recuperacion_copias_drive.sql" `
  "backend/src/main/resources/db/migration/V7__cuotas_archivos_estructura.sql" `
  "backend/src/main/resources/db/migration/V8__cuotas_archivos_datos_existentes.sql"
```

### Código frontend, sin Header.tsx (12 archivos)

```powershell
git add -- `
  "frontend/src/api/cargas.ts" `
  "frontend/src/api/cliente.ts" `
  "frontend/src/api/notificaciones.ts" `
  "frontend/src/api/pedidos.ts" `
  "frontend/src/components/GmailNotificationsView.tsx" `
  "frontend/src/components/Sidebar.tsx" `
  "frontend/src/components/pedidos/ArchivosEtapa.tsx" `
  "frontend/src/components/pedidos/CopiasFallidas.tsx" `
  "frontend/src/components/pedidos/EnlaceDrive.tsx" `
  "frontend/src/data/initialData.ts" `
  "frontend/src/rutas.tsx" `
  "frontend/src/utils/orderState.ts"
```

### Pruebas backend/frontend (23 archivos)

```powershell
git add -- `
  "backend/src/test/java/com/decokasa/seguimiento/MigracionesIT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/V2RestriccionesIT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/V3MigracionIT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/AlmacenLocalRealTest.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/CargasB1IT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/CorreoB2IT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/CorreoReintentosLocalesIT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/CuotasLocalesIT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/DatosB1.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/DriveRecuperacionB1IT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/GmailProtocoloLocalTest.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/GoogleDriveProtocoloB1Test.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/MigracionesArchivosB1IT.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/ParcialesB1Test.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/TareaCopiaB1Test.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/adapter/out/google/AlmacenGoogleLocalTest.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/adapter/out/google/FixtureGoogle.java" `
  "backend/src/test/java/com/decokasa/seguimiento/archivos/adapter/out/google/OAuthFlujoLocalTest.java" `
  "backend/src/test/java/com/decokasa/seguimiento/pedidos/Backend24IT.java" `
  "frontend/src/App.test.tsx" `
  "frontend/src/components/GmailNotificationsView.test.tsx" `
  "frontend/src/components/pedidos/ArchivosYPermisos.test.tsx" `
  "frontend/src/test/sin-demo.test.ts"
```

### Documentación revisada e inventario (16 archivos)

```powershell
git add -- `
  ".claude/PRPs/reports/fase-3-paso-3.3-report.md" `
  ".claude/PRPs/reports/fase-3-paso-3.4-report.md" `
  ".claude/PRPs/reports/fase-3-paso-3.5-report.md" `
  "documentos/adr/0017-google-oauth-usuario-gmail-personal.md" `
  "documentos/adr/README.md" `
  "documentos/api/openapi.yaml" `
  "documentos/arquitectura.md" `
  "documentos/fase-3-paso-3.2.md" `
  "documentos/fase-3-paso-3.3.md" `
  "documentos/fase-3-paso-3.4.md" `
  "documentos/modelo-datos.md" `
  "documentos/pruebas/fase-3-decisiones-locales.md" `
  "documentos/pruebas/fase-3-inventario-guardado.csv" `
  "documentos/pruebas/fase-3-inventario-guardado.md" `
  "documentos/pruebas/fase-3-pendientes-locales-evidencia.json" `
  "documentos/pruebas/fase-3-pendientes-locales-implementacion.md"
```

### Evidencias mínimas y herramientas reproducibles (7 archivos)

```powershell
git add -- `
  "documentos/pruebas/arnes-b3/evidencias/cancelacion-ensayo-real.json" `
  "documentos/pruebas/arnes-b3/evidencias/conservacion-cancelacion.json" `
  "documentos/pruebas/fase-3-evidencias/3.B2/resultados-finales.json" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/ejecutar.ps1" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-jacoco.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-metricas.json" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/medir.py"
```

## Revisión manual posterior, sin commit automático

```powershell
git diff --cached --stat
git diff --cached --name-only
git diff --cached --check
```

Revisar el contenido preparado localmente, especialmente cualquier secreto que se hubiera agregado después de este inventario. No publicar salidas con valores sensibles. Un estado de tests local aprobado no constituye aprobación de Google real, del espacio físico temporal o del cierre de fase. **3.B3 permanece pospuesto; fase abierta.**
