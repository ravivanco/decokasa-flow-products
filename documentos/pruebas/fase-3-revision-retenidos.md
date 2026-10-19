# Revisión final de retenidos y selección para guardar fase 3

2026-10-19. Sustituye la propuesta conservadora anterior de retener archivos solo por correo/ruta. **Los 12 revisados se recuperan. Ninguno se excluye por ese motivo.** Se revisaron fuentes/texto y contenidos textuales de ZIP/DOCX sin abrir credenciales externas ni mostrar contraseñas/tokens. No se modificaron los 12 originales ni se ejecutó Git de escritura.

**Estado del proyecto:** check local aprobado, 668 casos sin fallos/errores/omisiones; cobertura LINE global **90,64 %** y archivos **86,34 %**. 3.B3 real **pospuesto por decisión de Richard**; fase 3 abierta. OAuth, workers y schedulers permanecen desactivados. La cuota lógica no garantiza espacio físico agregado de temporales; ese límite operativo sigue pendiente.

## Clasificación de seguridad

- **Secretos reales:** no detectados en los 12 archivos. No contienen valores reales de contraseña, access/refresh token, llave AES/privada o JSON OAuth descargado. Las referencias a esos tipos, sus variables y el código que los lee/genera no son sus valores. Se mantienen fuera de Git `.env.local`, `ensayo-privado.properties`, JSON OAuth, llaves y contenedores cifrados de tokens.
- **Datos personales innecesarios:** prefijo resuelto del perfil local en ejemplos/comandos y reportes; se propone normalizarlo, sin aplicar cambios. El DOCX fuente contiene campos de autoría: opcionalmente retirar `dc:creator` y `cp:lastModifiedBy` de `docProps/core.xml` antes de difundir una copia si no se desea atribución personal; no son secretos ni motivo para perder la fuente.
- **Configuración necesaria:** correo corporativo aprobado, callback exacto localhost, scopes, nombres de variables, estructura país/pedido, frases de confirmación y cuenta fija de `GuardasB3`. Mantenerlos. El correo de empresa no acredita acceso a la cuenta y aquí limita el alcance autorizado. No se amplía a destinatario definitivo de producción.
- **Evidencia técnica:** comandos, estados, fechas, UUIDs ficticios y hashes son necesarios para trazabilidad. Ubicaciones de archivos son identificadores, no su contenido. El nombre del responsable de una aprobación se conserva como autoría de decisión; no se convierte una referencia local en una credencial.

La búsqueda estática no sustituye una garantía sobre todos los formatos/historia del repositorio. También se inspeccionaron archivos comprimidos referenciados: no aparecieron patrones de credenciales/tokens reales ni archivos privados empaquetados por nombre. Las trazas Playwright son de las suites con respuestas simuladas, no de consentimiento OAuth real; **no se autoriza adjuntar futuras trazas/HAR de una conexión real**.

## Decisión sobre los doce archivos

| Archivo revisado | Clasificación y decisión |
|---|---|
| `.claude/PRPs/plans/fase-3-drive-gmail.plan.md` | **Incluir.** Callback, scopes, almacenamiento externo y remitente son configuración necesaria. Las dos rutas de ejemplo contienen un perfil personal prescindible; recomendación de normalización, no un secreto |
| `documentos/fase-3-paso-3.5.md` | **Incluir tal cual.** Correo aprobado identifica el remitente, no una contraseña; no autoriza destinatarios productivos |
| `documentos/google-oauth-paso-3.1.md` | **Incluir.** Son instrucciones/ubicaciones, no el JSON ni la llave. Sugerir sustitución de prefijos personales en ejemplos; no copiar credenciales para completar la documentación |
| `documentos/pruebas/arnes-b3/README.md` | **Incluir tal cual.** Cuenta aprobada, confirmaciones y parada son restricciones necesarias. Encabezado vigente mantiene el ensayo pospuesto; indicaciones de ejecución son futuras |
| `documentos/pruebas/arnes-b3/arnes-b3.ps1` | **Incluir tal cual.** Wrapper opt-in, sin secreto literal real detectado; prepararlo en Git no lo ejecuta |
| `documentos/pruebas/arnes-b3/b3.init.gradle` | **Incluir tal cual.** Source set aislado, fuera de test/check normales; no habilita servicios por guardarlo |
| `documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/ArnesGoogleB3.java` | **Incluir tal cual.** Generación/entrada enmascarada y lectura de configuración externa; el código no contiene sus valores reales. No incluir el properties privado que generaría al ejecutarse |
| `documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/GuardasB3.java` | **Incluir tal cual.** `CUENTA` es el destinatario/remitente autorizado del ensayo. **No sustituirlo por comodín, selector o destinatario variable:** debilitaría la guarda |
| `documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/GuardasB3Test.java` | **Incluir tal cual.** Pruebas ficticias de restricciones, sin contraseñas/tokens reales detectados |
| `documentos/pruebas/fase-3-B3-procedimiento.md` | **Incluir tal cual.** Buzón de prueba, callback y operación manual son necesarios. No confundir un snippet que maneja un token en memoria con un token real almacenado en el documento |
| `documentos/pruebas/fase-3-evidencias/revalidacion-B1/resumen-revalidacion.json` | **Incluir.** Resultados, comandos, UUIDs y ubicaciones del disco aislado son evidencia. Solo el prefijo personal del perfil es prescindible; se conserva el sufijo UUID para correlación |
| `documentos/pruebas/fase-3-informe.md` | **Incluir.** Conservar resultados, historia, aprobaciones y cuenta del ensayo. Un prefijo local de la herramienta Playwright puede normalizarse, sin alterar los resultados ni afirmar otra ejecución |


### Saneamiento propuesto, sin aplicarlo

**No hay saneamiento obligatorio por secretos detectados en estos 12 archivos.** Los doce forman parte de la selección final; pueden guardarse localmente tal como están. Antes de difundir el repositorio, recomiendo retirar únicamente los siguientes metadatos personales prescindibles. Son recomendaciones de privacidad/portabilidad, no una razón para excluir el correo o los archivos.

| Ruta y ubicación actual | Retirar/sustituir exactamente | Conservar |
|---|---|---|
| Plan, líneas 181–182 | Sustituir solo el prefijo resuelto del perfil Windows por `%USERPROFILE%` en el texto de ejemplo | `.decokasa`, separación secretos/tokens, nombres de ejemplo, almacenamiento fuera de Git y ACL |
| Guía OAuth, líneas 25 y 43 | Sustituir el prefijo personal por `%USERPROFILE%` en prosa | Función y separación de directorios |
| Guía OAuth, línea 30 | Sustituir la asignación con ruta personal por `$rutaLlaveGoogle = Join-Path $env:USERPROFILE '.decokasa\secretos\google-aes.key'` | Operación de generación local y permisos privados; no mostrar la llave |
| Guía OAuth, líneas 54–56 | Retirar **solo los valores de rutas del equipo** de las tres asignaciones; dejar `GOOGLE_OAUTH_CLIENTE_RUTA=`, `GOOGLE_OAUTH_LLAVE_RUTA=` y `GOOGLE_OAUTH_TOKENS_RUTA=` con instrucción de configurar rutas absolutas externas | Claves de configuración y OAuth false. No introducir `%USERPROFILE%` o `$env:USERPROFILE` como valores literales: el cargador de properties del arnés no los expande |
| Guía OAuth, línea 137 | Cambiar solo el prefijo personal de la carpeta `json` a `%USERPROFILE%` en el registro textual | Hecho histórico del traslado fuera de Git, sin añadir nombre/contenido de la credencial |
| Resumen B1, líneas 10, 641, 648, 655, 662, 669; campos `discoAislado` | Normalizar solo la parte hasta el directorio temporal a `%TEMP%` | `decokasa-revalidacion-b1-` y UUID original; fechas, salida, contadores y hashes |
| Informe, línea 268 | Sustituir solo el prefijo del perfil por una expresión portable: `node (Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js') test --config playwright.config.mjs` | Argumentos originales y resultado; marcar la representación normalizada si se cambia el texto histórico |

El patrón de prefijo a normalizar es `C:[\/]+Users[\/]+[^\/]+`; el CSV final identifica las líneas adicionales de logs/XML que contienen ese mismo dato. No hace falta retirar nombres de archivo, IDs ficticios de pruebas, SHA-256, horas, estados o el correo corporativo aprobado para este propósito.

Si se preparan copias saneadas de evidencias, conservar primero el original local y registrar qué cambió. Los hashes previos deben seguir identificando sus originales; no presentarlos como hash de una copia modificada. No reescribir evidencias fallidas para convertirlas en aprobadas.


### Metadatos personales en evidencias adicionales

No obligan a excluir estas evidencias necesarias para los enlaces. Para una copia publicable, la misma normalización opcional del prefijo puede aplicarse en las entradas textuales de estos ZIP, sin cambiar datos de resultados ni recursos relativos:

| Archivo | Entradas textuales con prefijo local |
|---|---|
| `documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-admin-ve-registr-ef198-in-envio-ni-exito-inventado/trace.zip` | 3 |
| `documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-enlaces-solo-par-71359-arga-se-conserva-en-backend/trace.zip` | 3 |
| `documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-no-admin-no-cons-00183-ro-ni-ve-el-menu-de-correos/trace.zip` | 3 |
| `documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-progreso-de-bloq-73091-irmado-sin-exito-anticipado/trace.zip` | 3 |
| `documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/browser-artifacts.zip` | 1 |
| `documentos/pruebas/fase-3-evidencias/reportes-b1-final.zip` | 3 |
| `documentos/pruebas/fase-3-evidencias/revalidacion-B1/reportes-revalidacion.zip` | 14 |

No se ha modificado ni reempaquetado ninguno. En los ZIP de trazas, no retirar de forma indiscriminada los campos `path/file`: referencian recursos y eliminar campos rompería el visor. Normalizar solamente el prefijo personal en campos de origen/pila cuando exista y comprobar la integridad de la copia. El resto de ubicaciones por archivo/línea está en el CSV final.

## Selección final y referencias

**362 rutas concretas**, aproximadamente 15.48 MiB antes de añadir el tamaño de esta guía. Lista completa: [TXT](fase-3-seleccion-final.txt) y [CSV con grupo, comando y saneamiento propuesto](fase-3-seleccion-final.csv). Incluye los 12 originales, plan, informe, código, V4–V8, pruebas, arnés opt-in completo y evidencias enlazadas. Los inventarios anteriores se conservan como historia; esta revisión prevalece.

| Grupo | Rutas |
|---|---|
| 01_configuracion | 3 |
| 02_backend | 81 |
| 03_migraciones | 5 |
| 04_frontend | 12 |
| 05_pruebas | 23 |
| 06_arnes_opt_in | 6 |
| 07_documentacion_y_fuentes | 25 |
| 08_evidencias_referenciadas | 207 |

Se resolvieron **129 enlaces Markdown locales** de los documentos seleccionados: cero destinos inexistentes. Cada destino de archivo queda en la selección o ya está rastreado; cada carpeta enlazada conserva contenido en la selección. Se mantienen archivos históricos que parecían redundantes porque quitarlos dejaría referencias sin respaldo. Los ZIP/visores permanecen completos.

La fuente vigente `documentos/manual-paso-a-paso.docx` también se incluye como dependencia documental: no es una implementación nueva de fase 3, pero el plan la enlaza y estaba ignorada. Su contenido fue revisado como fuente documental. No se reescribe el manual ni se alteran sus instrucciones.

**Excepción de volumen, sin enlace roto:** no añadir `documentos/pruebas/fase-3-evidencias/ejecucion-02/TEST-com.decokasa.seguimiento.archivos.CargasB1IT.xml`. Mide aproximadamente 100 MiB y contiene un volcado del archivo ficticio del caso inicial. No tiene enlaces directos; la carpeta `ejecucion-02/` sigue respaldada por los demás XML. El log inicial y el informe conservan el fallo, sus contadores y el reproductor; el volcado original sigue local, sin borrar o modificar. No es evidencia necesaria para guardar el avance.

Fuera de la selección: Header.tsx, evidencias pendientes de fase 2, `.vscode`, auditoría/catálogo anteriores, documentos de derivadas, índices `.bin`, dumps/temporales/builds y duplicados no enlazados. Ninguno se borra. No incluir el directorio externo del ensayo: contiene o podría contener datos/credenciales privados.

## Comandos completos — solo para preparación manual

**No ejecutados por el agente.** Revisar primero el saneamiento opcional anterior. Para guardar localmente los originales, las rutas están listas; si se decide anonimizar antes de preparar, hacerlo manualmente y conservar originales/evidencia de la transformación. Desde `documentos`, el directorio de esta sesión:

```powershell
Set-Location ..
git branch --show-current
git status --short
git diff --cached --name-only
```

Debe mostrar `fase-3-drive-gmail` y el índice vacío antes de empezar. Las siguientes órdenes están completas, agrupadas y divididas en hasta 20 archivos para evitar límites de línea en Windows. **32 rutas ignoradas** requieren `git add -f --`: solo el manual y logs exactos enlazados y revisados. No se fuerza ningún directorio, secreto o archivo privado; los restantes usan `git add --` normal. No ejecutar ambas versiones de inventario: usar solamente estas órdenes.

### 01_configuracion — 3 archivos

```powershell
git add -- `
  ".env.example" `
  ".gitignore" `
  "backend/.env.example"
```

### 02_backend — 81 archivos

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
  "backend/src/main/java/com/decokasa/seguimiento/archivos/application/port/in/GestionarCarga.java"
```

```powershell
git add -- `
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
  "backend/src/main/java/com/decokasa/seguimiento/archivos/config/ConsultaDriveConfig.java"
```

```powershell
git add -- `
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
  "backend/src/main/java/com/decokasa/seguimiento/notificaciones/adapter/in/eventos/OyenteEventosNotificacion.java"
```

```powershell
git add -- `
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
  "backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/archivos/PedidosParaCopiasAdaptador.java"
```

```powershell
git add -- `
  "backend/src/main/resources/application.yml"
```

### 03_migraciones — 5 archivos

```powershell
git add -- `
  "backend/src/main/resources/db/migration/V4__drive_notificaciones_outbox.sql" `
  "backend/src/main/resources/db/migration/V5__cargas_reanudables.sql" `
  "backend/src/main/resources/db/migration/V6__recuperacion_copias_drive.sql" `
  "backend/src/main/resources/db/migration/V7__cuotas_archivos_estructura.sql" `
  "backend/src/main/resources/db/migration/V8__cuotas_archivos_datos_existentes.sql"
```

### 04_frontend — 12 archivos

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

### 05_pruebas — 23 archivos

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
  "frontend/src/App.test.tsx"
```

```powershell
git add -- `
  "frontend/src/components/GmailNotificationsView.test.tsx" `
  "frontend/src/components/pedidos/ArchivosYPermisos.test.tsx" `
  "frontend/src/test/sin-demo.test.ts"
```

### 06_arnes_opt_in — 6 archivos

```powershell
git add -- `
  "documentos/pruebas/arnes-b3/README.md" `
  "documentos/pruebas/arnes-b3/arnes-b3.ps1" `
  "documentos/pruebas/arnes-b3/b3.init.gradle" `
  "documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/ArnesGoogleB3.java" `
  "documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/GuardasB3.java" `
  "documentos/pruebas/arnes-b3/java/com/decokasa/ensayob3/GuardasB3Test.java"
```

### 07_documentacion_y_fuentes — 25 archivos

```powershell
git add -- `
  ".claude/PRPs/plans/fase-3-drive-gmail.plan.md" `
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
  "documentos/fase-3-paso-3.5.md" `
  "documentos/google-oauth-paso-3.1.md" `
  "documentos/modelo-datos.md" `
  "documentos/pruebas/fase-3-B3-procedimiento.md" `
  "documentos/pruebas/fase-3-decisiones-locales.md" `
  "documentos/pruebas/fase-3-informe.md" `
  "documentos/pruebas/fase-3-inventario-guardado.csv" `
  "documentos/pruebas/fase-3-inventario-guardado.md" `
  "documentos/pruebas/fase-3-pendientes-locales-evidencia.json"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-pendientes-locales-implementacion.md" `
  "documentos/pruebas/fase-3-revision-retenidos.md" `
  "documentos/pruebas/fase-3-seleccion-final.csv" `
  "documentos/pruebas/fase-3-seleccion-final.txt"
```

```powershell
git add -f -- `
  "documentos/manual-paso-a-paso.docx"
```

### 08_evidencias_referenciadas — 207 archivos

```powershell
git add -- `
  "documentos/pruebas/arnes-b3/evidencias/cancelacion-ensayo-real.json" `
  "documentos/pruebas/arnes-b3/evidencias/conservacion-cancelacion.json" `
  "documentos/pruebas/arnes-b3/evidencias/conservacion-final.json" `
  "documentos/pruebas/arnes-b3/evidencias/ejecucion-real-estado.json" `
  "documentos/pruebas/arnes-b3/evidencias/html/index.html" `
  "documentos/pruebas/arnes-b3/evidencias/junit/TEST-com.decokasa.ensayob3.GuardasB3Test.xml" `
  "documentos/pruebas/arnes-b3/evidencias/reinicio-tras-liberar-puerto.json" `
  "documentos/pruebas/arnes-b3/evidencias/sha256.json" `
  "documentos/pruebas/fase-3-evidencias/3.B2/backend.json" `
  "documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/.last-run.json" `
  "documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-admin-ve-registr-ef198-in-envio-ni-exito-inventado/trace.zip" `
  "documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-enlaces-solo-par-71359-arga-se-conserva-en-backend/trace.zip" `
  "documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-no-admin-no-cons-00183-ro-ni-ve-el-menu-de-correos/trace.zip" `
  "documentos/pruebas/fase-3-evidencias/3.B2/browser-artifacts/pantallas-progreso-de-bloq-73091-irmado-sin-exito-anticipado/trace.zip" `
  "documentos/pruebas/fase-3-evidencias/3.B2/conservacion.json" `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/TEST-com.decokasa.seguimiento.archivos.CorreoB2IT.xml" `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/TEST-com.decokasa.seguimiento.arquitectura.ReglasArquitecturaTest.xml" `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/TEST-com.decokasa.seguimiento.pedidos.Backend24IT.xml" `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/browser-artifacts.zip" `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/playwright.json"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecutar-b2.ps1" `
  "documentos/pruebas/fase-3-evidencias/3.B2/jacoco-backend-b2-filtrado.xml" `
  "documentos/pruebas/fase-3-evidencias/3.B2/playwright.json" `
  "documentos/pruebas/fase-3-evidencias/3.B2/resultados-finales.json" `
  "documentos/pruebas/fase-3-evidencias/archivos-aislados.init.gradle" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-02/TEST-com.decokasa.seguimiento.archivos.AlmacenLocalRealTest.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-02/TEST-com.decokasa.seguimiento.archivos.DriveRecuperacionB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-02/TEST-com.decokasa.seguimiento.archivos.MigracionesArchivosB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-02/TEST-com.decokasa.seguimiento.archivos.ParcialesB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-03/TEST-com.decokasa.seguimiento.archivos.AlmacenLocalRealTest.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-03/TEST-com.decokasa.seguimiento.archivos.CargasB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-03/TEST-com.decokasa.seguimiento.archivos.DriveRecuperacionB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-03/TEST-com.decokasa.seguimiento.archivos.MigracionesArchivosB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-03/TEST-com.decokasa.seguimiento.archivos.ParcialesB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-07/TEST-com.decokasa.seguimiento.archivos.AlmacenLocalRealTest.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-07/TEST-com.decokasa.seguimiento.archivos.CargasB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-07/TEST-com.decokasa.seguimiento.archivos.DriveRecuperacionB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-07/TEST-com.decokasa.seguimiento.archivos.MigracionesArchivosB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/ejecucion-07/TEST-com.decokasa.seguimiento.archivos.ParcialesB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/jacoco-archivos-b1-final.xml"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/preparacion-B3/acl-local.json" `
  "documentos/pruebas/fase-3-evidencias/preparacion-B3/auditar-local.py" `
  "documentos/pruebas/fase-3-evidencias/preparacion-B3/auditoria-local.json" `
  "documentos/pruebas/fase-3-evidencias/preparacion-B3/conservacion-final.json" `
  "documentos/pruebas/fase-3-evidencias/preparacion-B3/sha256.json" `
  "documentos/pruebas/fase-3-evidencias/preparacion-B3/sintaxis-procedimiento.json" `
  "documentos/pruebas/fase-3-evidencias/reportes-b1-final.zip" `
  "documentos/pruebas/fase-3-evidencias/resumen-final.json" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/arnes-final/TEST-com.decokasa.seguimiento.archivos.AlmacenLocalRealTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/arnes-final/TEST-com.decokasa.seguimiento.archivos.CargasB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/arnes-final/TEST-com.decokasa.seguimiento.archivos.DriveRecuperacionB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/arnes-final/TEST-com.decokasa.seguimiento.archivos.GoogleDriveProtocoloB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/arnes-final/TEST-com.decokasa.seguimiento.archivos.MigracionesArchivosB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/arnes-final/TEST-com.decokasa.seguimiento.archivos.ParcialesB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/arnes-final/TEST-com.decokasa.seguimiento.archivos.TareaCopiaB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.AplicacionArrancaIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.MigracionesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.V2RestriccionesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.V3MigracionIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.archivos.AlmacenLocalRealTest.xml"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.archivos.CargasB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.archivos.DriveRecuperacionB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.archivos.GoogleDriveProtocoloB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.archivos.MigracionesArchivosB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.archivos.ParcialesB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.archivos.TareaCopiaB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.arquitectura.FronterasFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.arquitectura.FronterasP20Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.arquitectura.ReglasArquitecturaTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.compartido.adapter.in.web.ManejadorErroresTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.compartido.adapter.out.RelojSistemaTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.compartido.domain.CodigoRolTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.configuracion.SaludControllerTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.flujos.DefinicionFlujoPersistenciaIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.flujos.application.FlujoFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.historial.application.HistorialFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.pedidos.Backend24IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.pedidos.PersistenciaFaseDosIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.pedidos.adapter.in.web.ResumenDeEtapaP20Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.pedidos.application.AccionNucleoFaseDosTest.xml"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.pedidos.application.CrearReasignarFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.pedidos.domain.NucleoFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.plazos.domain.CalendarioFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.arranque.CargadorUsuariosInicialesTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.AltasConcurrentesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.AuthControllerIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.EdicionConcurrenteIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.IntentosConcurrentesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.RenovacionConcurrenteIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.SesionHttpIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.UsuarioControllerIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.VersionUsuarioIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.out.persistence.RestriccionesUnicasUsuarioTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.adapter.out.persistence.UsuarioJpaRepositorioIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.ActualizarRolTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.ActualizarUsuarioTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.CerrarSesionTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.ConsultasTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.CrearRolTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.CrearUsuarioTest.xml"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.IniciarSesionTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.application.RenovarSesionTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.config.DesactivarMientrasRenuevaIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.domain.ContrasenaNuevaTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.domain.SesionRenovableTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.domain.TelefonoTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/check-final/TEST-com.decokasa.seguimiento.usuarios.domain.UsuarioTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/jacoco-arnes-final.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/jacoco-global-final.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/reportes-revalidacion.zip" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/resumen-revalidacion.json" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/sha256-revalidacion.json" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/ejecutar.ps1" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-jacoco.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.AplicacionArrancaIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.MigracionesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.V2RestriccionesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.V3MigracionIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.AlmacenLocalRealTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.CargasB1IT.xml"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.CorreoB2IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.CorreoReintentosLocalesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.CuotasLocalesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.DriveRecuperacionB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.GmailProtocoloLocalTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.GoogleDriveProtocoloB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.MigracionesArchivosB1IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.ParcialesB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.TareaCopiaB1Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.adapter.out.google.AlmacenGoogleLocalTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.archivos.adapter.out.google.OAuthFlujoLocalTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.arquitectura.FronterasFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.arquitectura.FronterasP20Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.arquitectura.ReglasArquitecturaTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.compartido.adapter.in.web.ManejadorErroresTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.compartido.adapter.out.RelojSistemaTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.compartido.domain.CodigoRolTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.configuracion.SaludControllerTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.flujos.DefinicionFlujoPersistenciaIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.flujos.application.FlujoFaseDosTest.xml"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.historial.application.HistorialFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.pedidos.Backend24IT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.pedidos.PersistenciaFaseDosIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.pedidos.adapter.in.web.ResumenDeEtapaP20Test.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.pedidos.application.AccionNucleoFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.pedidos.application.CrearReasignarFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.pedidos.domain.NucleoFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.plazos.domain.CalendarioFaseDosTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.arranque.CargadorUsuariosInicialesTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.AltasConcurrentesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.AuthControllerIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.EdicionConcurrenteIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.IntentosConcurrentesIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.RenovacionConcurrenteIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.SesionHttpIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.UsuarioControllerIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.VersionUsuarioIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.out.persistence.RestriccionesUnicasUsuarioTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.adapter.out.persistence.UsuarioJpaRepositorioIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.ActualizarRolTest.xml"
```

```powershell
git add -- `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.ActualizarUsuarioTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.CerrarSesionTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.ConsultasTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.CrearRolTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.CrearUsuarioTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.IniciarSesionTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.application.RenovarSesionTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.config.DesactivarMientrasRenuevaIT.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.domain.ContrasenaNuevaTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.domain.SesionRenovableTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.domain.TelefonoTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-junit/TEST-com.decokasa.seguimiento.usuarios.domain.UsuarioTest.xml" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/final-metricas.json" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/medir.py" `
  "documentos/pruebas/fase-3-evidencias/revalidar-b1.ps1" `
  "documentos/pruebas/fase-3-evidencias/sha256-final.json"
```

```powershell
git add -f -- `
  "documentos/pruebas/arnes-b3/evidencias/comprobar-final.log" `
  "documentos/pruebas/fase-3-evidencias/01-baseline-test.log" `
  "documentos/pruebas/fase-3-evidencias/02-archivos-aislados.log" `
  "documentos/pruebas/fase-3-evidencias/03-archivos-aislados.log" `
  "documentos/pruebas/fase-3-evidencias/04-backend-check.log" `
  "documentos/pruebas/fase-3-evidencias/05-cobertura-aislada.log" `
  "documentos/pruebas/fase-3-evidencias/06-archivos-aislados-final.log" `
  "documentos/pruebas/fase-3-evidencias/07-archivos-aislados-final.log" `
  "documentos/pruebas/fase-3-evidencias/08-cobertura-final.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/backend.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/build-final.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/backend.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/ejecucion-inicial/playwright.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/lint-final.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/playwright.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/test-final.log" `
  "documentos/pruebas/fase-3-evidencias/3.B2/test-inicial.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-123328-reproductores.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-123451-arnes.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-123759-arnes.log"
```

```powershell
git add -f -- `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-123944-check.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-124215-check.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-124706-arnes.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-124836-arnes.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-B1/20261017-124937-check.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/20261019-102140-baseline.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/20261019-102511-cobertura.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/20261019-103000-nuevas.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/20261019-103212-archivos.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/20261019-103920-check.log" `
  "documentos/pruebas/fase-3-evidencias/revalidacion-local/20261019-105712-check.log"
```

## Revisión del resultado preparado, sin commit

```powershell
git diff --cached --stat
git diff --cached --name-only
git diff --cached --check
```

No incluir Header.tsx ni archivos privados por un `git add .` posterior. Comparar la lista preparada con el TXT final y revisar localmente el diff; no pegar valores sensibles en el chat. Este inventario no autoriza ejecución del arnés ni reanudación de Google. **3.B3 pospuesto, fase abierta; sin commit/push/merge.**
