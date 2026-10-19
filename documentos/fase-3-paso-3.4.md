# Fase 3, parte A, paso 3.4 — copia y recuperación de Drive

Fecha: 2026-10-17. Rama: `fase-3-drive-gmail`. Entrega de código **sin compilar, ejecutar pruebas/builds, aplicar migraciones ni arrancar servicios**. No hay autorización, conexión, carpeta, subida o envío real a Google. No se cierra parte B ni fase 3.

Se siguieron el manual Word vigente, el plan y los informes 3.2/3.3, con `ecc-prp-implement` y `ecc-error-handling`, y las pautas de Java, migraciones y seguridad. Se omiten las acciones incompatibles de esas skills: pruebas/builds, sincronización remota, archivo del plan y cierre del PRD. El plan sigue activo y es el mismo archivo.

## Decisión de carpetas resuelta durante el encargo

Richard aprobó que **la aplicación cree país/pedido en Mi unidad**, usando el código de país y número real ya asignado: por ejemplo `EC/DK-EC-2026-0001`. No se genera otro número ni se crean niveles adicionales por año/etapa. Es una aprobación técnica del mecanismo, no cierre de las otras preguntas comerciales pendientes.

El worker consulta la raíz real de Mi unidad mediante el identificador API `root`; no inventa un ID, nombre de carpeta raíz o unidad compartida. Las claves estables son `pais:{codigo}` y `pedido:{UUID}`. Antes de crear cada carpeta, reserva un ID remoto y lo confirma en `carpeta_drive`, índice técnico del mismo módulo. Una reserva concurrente perdedora usa el ID ganador; no crea otra carpeta. Ante 409/timeout se consulta el mismo ID y se valida identidad, propiedad, padre y clave técnica.

No se adoptan carpetas humanas solo porque coincida su nombre ni se añaden permisos públicos. Si una carpeta previamente confirmada desaparece, cambia de padre/identidad o no es de la cuenta, se detiene con error: no se reemplaza silenciosamente por otra. Acceso directo de otras personas sigue sin definirse y no se modifica en 3.4.

## Implementación y persistencia

`ReintentarCopiasPendientes` ejecuta una pasada acotada del caso de uso puro `CopiarPendientesCasoDeUso`. Reutiliza **la misma tabla `adjunto`**, almacenamiento local y adaptador `GoogleDriveAlmacen` de 3.2. No crea otro catálogo, cola de archivos o tabla de binarios. Solo selecciona adjuntos ya aceptados; las sesiones RECIBIENDO y sus bytes de 3.3 no cuentan para la copia.

V6 `V6__recuperacion_copias_drive.sql` es la siguiente versión local disponible, creada **sin aplicar**. V1–V5 no cambian. Añade a `adjunto`:

- `copia_en_intento`, para distinguir recuperar un intento interrumpido de comenzar un reintento nuevo;
- `drive_sesion_cifrada`, URI remota cifrada, nunca texto plano;
- `drive_offset`, checkpoint técnico; el estado remoto se vuelve a consultar antes de reanudar;
- `drive_carpeta_id`, destino estable asociado a la sesión.

Conserva `drive_id_reservado`, lease, intentos, próxima fecha y error de V3/V4. `drive_id` definitivo, enlace real, fecha y COPIADO se guardan juntos únicamente al confirmar. V6 crea además `carpeta_drive` con una identidad única por país/pedido. No reescribe historial, entradas, UUID ni relaciones de archivos de fase 2. No hace backfill de envíos, purgas o resets de contadores.

`ProtectorSesionDriveAes` usa AES-256-GCM y la llave externa privada del almacén OAuth 3.1. La AAD separa protocolos y vincula el cifrado al UUID del adjunto. Llave, credenciales y tokens siguen fuera de Git/PostgreSQL; en base solo queda ciphertext. Cambiar o perder esa llave no permite descifrar una sesión: no se sustituye ni inventa otra automáticamente. Las construcciones de beans no leen secretos ni hacen red.

## Intento inicial y exactamente tres reintentos disponibles

El presupuesto es **cuatro intentos de trabajo como máximo** ante fallos reintentables, no cuatro peticiones HTTP. Las consultas, bloques y recuperación del mismo intento no incrementan el contador.

| Ejecución | `intentos` | Si falla de forma reintentable |
|---|---:|---|
| Intento inicial | 1 | PENDIENTE; reintento 1 después de 1 minuto |
| Reintento 1 | 2 | PENDIENTE; reintento 2 después de 5 minutos |
| Reintento 2 | 3 | PENDIENTE; reintento 3 después de 15 minutos |
| Reintento 3 | 4 | Tras conciliación final, FALLIDO; no existe un quinto intento automático |

Las tres esperas son configurables, deben ser positivas y crecientes; los defaults son PT1M/PT5M/PT15M. Se respeta un Retry-After de Drive mayor que la espera base, incluso el recibido durante conciliación. No se hace sleep dentro del worker: la fecha se persiste y una pasada posterior toma la fila cuando corresponde. No se implementa jitter en este encargo.

Cuota temporal, timeout/resultado incierto, 5xx/408, 409 pendiente de conciliación y respuesta incompleta usan ese presupuesto. Revocación/autorización requerida, configuración inválida, identidad o checksum divergentes y archivo local ausente/corrupto son fallos permanentes y pueden terminar antes: no se hacen tres peticiones inútiles con credenciales inválidas ni se sobrescribe otro objeto. Los errores transitorios de renovación offline se conservan como transitorios; no se cambia el consentimiento, state/PKCE/nonce, callback fijo o caducidad de siete días de En pruebas.

Una caída sin registro de fallo deja `copia_en_intento=true`. Después del lease, se recupera **ese mismo intento**, incluso si era el cuarto. No se reinicia el presupuesto al arrancar. El conteo se reserva al comenzar el trabajo, por lo que una caída antes de la primera petición puede dejar un intento iniciado; retomarlo no crea otro. Contención del cerrojo difiere 30 segundos sin consumir reintentos adicionales. FALLIDO no se rearma automáticamente; una acción administrativa futura deberá ser explícita.

## Recuperación, concurrencia y transacciones

La reclamación usa `FOR UPDATE SKIP LOCKED`, lease y token UUID nuevos. Cada operación de repositorio abre/cierra una transacción corta en la composición, antes de la siguiente llamada a Google. El puerto rechaza invocaciones dentro de una transacción activa; el adaptador HTTP conserva también su rechazo. No hay transacciones abiertas durante HTTP, renovación de tokens, lectura completa/hash o transferencia de bloques.

El lease por defecto es 3 minutos, configurable entre 1 y 30 minutos. Se renueva antes de operaciones remotas y mediante latidos durante el cálculo local. Las respuestas se aplican solo si el token y lease siguen vigentes. Un worker antiguo no puede confirmar o marcar fallo sobre el trabajo de otro.

Un segundo cerrojo de filesystem, `.copias-locks/{adjuntoUUID}.lock` en el volumen existente, impide que otro worker empiece red para el mismo archivo mientras un proceso pausado aún conserva el cerrojo tras vencer el lease. Si no puede adquirirlo, cede su reclamación sin hacer red. El proceso antiguo se detiene al fallar su siguiente renovación. Requiere el mismo volumen y bloqueo efectivo entre procesos, igual que 3.3; no se declara compatible un despliegue con discos independientes o locks de red no comprobados.

Tras reiniciar, la pasada vuelve a leer PENDIENTE y fechas desde PostgreSQL. No necesita la memoria de la ejecución anterior. Primero comprueba tamaño y SHA-256 local; no exige que la etapa original siga siendo la actual, porque el archivo ya fue aceptado. Las claves/rutas locales y todos los bytes aceptados y parciales permanecen conservados. No hay limpieza, eliminación por edad ni liberación de disco después de COPIADO.

## Idempotencia y conciliación remota

1. Se reserva y persiste el ID Drive **antes** de crear el archivo. Si se pierde la respuesta de esa reserva, solo se desperdicia un ID no usado; no se ha creado un objeto. Una reserva ya persistida no se cambia para resolver un timeout/409.
2. Se consulta ese ID antes de transferir. Se comprueban adjunto/pedido/entrada, carpeta real, propiedad, ausencia de papelera/unidad compartida y formato binario sin conversión a Google Docs. Si ya está completo, se verifica tamaño y checksum del proveedor y se confirma el mismo objeto.
3. Una sesión guardada se descifra y consulta con PUT de estado. Se continúa desde el offset realmente reconocido, aunque difiera del checkpoint de base. Los bloques son acotados de 4 MiB; los no finales respetan los múltiplos del protocolo. No se confunden con las sesiones de recepción de 3.3.
4. Si caduca la sesión, se concilia primero el ID. Si ya está completo, se confirma; si no existe, se inicia de nuevo con el MISMO ID reservado. Si hay un blob incompleto propio y con identidad válida, se inicia una sesión PATCH sobre ese ID. Esto puede crear una revisión, nunca otro archivo. No se sobrescribe un objeto con tamaño positivo distinto o hash divergente.
5. Un 200/201 de subida no basta. Se hace una consulta de metadatos y se comprueban identificadores, carpeta, tamaño, checksum y `webViewLink` real antes de guardar COPIADO/id definitivo/enlace. Se prefiere SHA-256 remoto; cuando no está disponible se compara MD5 remoto con el MD5 medido sobre el local cuyo SHA-256 ya coincide con el registro. No se interpreta contenido comercial.
6. Ante timeout, 409 o resultado incierto se intenta conciliar también dentro del último intento. Si Google/base siguen inaccesibles, no se inventa un éxito: se conserva la identidad, sesión y archivo para recuperación/diagnóstico, y puede quedar FALLIDO tras agotar el presupuesto. FALLIDO no demuestra que Google nunca recibiera el objeto. No se borra ni se crea otro ID para ocultar esa incertidumbre.

El bucle acota peticiones de fragmentos por intento para no atascarse con respuestas sin progreso. Una respuesta tardía o un estado remoto incompatible no se aplica por suposición. No hay reintentos automáticos de aplicación escondidos en el adaptador HTTP: el presupuesto lo gobierna el caso de uso. Los errores persistidos son clases cerradas de `Causa`, nunca cuerpo HTTP, token, credencial, ruta o URI privada. El log del scheduler no adjunta throwable ni datos del proveedor.

## Desactivación y límites del encargo

OAuth y Drive permanecen false. El nuevo flag **COPIA_DRIVE_AUTOMATICA=false** impide registrar la tarea por defecto. El scheduler requiere conjuntamente OAuth=true, Drive=true y worker.automatico=true, y esos valores no se cambian aquí. El puerto interno de pasada está protegido además por los flags OAuth/Drive; no se añade un endpoint público de ejecución manual ni arranque inmediato.

Los defaults son lote 5 y sondeo 30 segundos; cada pasada procesa secuencialmente trabajos reclamados y otras instancias pueden reclamar otras filas. La configuración base de Docker sigue sin montar secretos Google ni habilitar el worker; el despliegue/volumen/ACL deberá concretarse antes de activar nada. No se dan instrucciones de activación como parte de este encargo.

No se implementan correo, recuperación/envío de notificaciones o pantallas de 3.5, ni se modifican destinatarios/outbox. Los endpoints de enlace/copias fallidas y su UI siguen para 3.5. No se amplían scopes ni permisos de Drive. El límite provisional continúa en 100 MiB, distinto de los 100 MB decimales del manual.

No queda una decisión de carpetas que bloquee el código: la única consulta se resolvió. Siguen pendientes para activación y parte B el consentimiento real, APIs/scopes efectivos (incluido acceso a metadata de la raíz con drive.file), cuota de Mi unidad, montaje privado de llave/tokens y comportamiento real del proveedor. Retención y acceso directo siguen pendientes, pero no bloquean este flujo porque no se elimina ni comparte ningún archivo.

## Casos reservados a parte B — no escritos ni ejecutados

1. Compilación, contexto/beans y fronteras R1–R4/R9; arranque con los tres flags false, sin scheduler, secretos o red; invocación del puerto desactivado sin reclamar ni consumir intentos.
2. V6 sobre V5/datos de fase 2: restricciones, compatibilidad de pendientes anteriores, índices/FK, locks de ALTER TABLE y versiones de ramas paralelas. No asumir que puede aplicarse sin coordinar ni que existan datos de intentos de una implementación previa.
3. Tabla exacta inicial+3: fechas 1/5/15, configuración inválida, Retry-After mayor/pasado/malformado, cuotas/503 y fallos permanentes tempranos. Ningún quinto intento nuevo, tampoco después de reiniciar.
4. Recuperación tras caída antes de Google, durante bloque y entre ACK/checkpoint; lease expirado del cuarto intento, memoria vacía, reloj y latidos, archivos pendientes en volumen; ausencia de borrado de parciales/aceptados/COPIADO.
5. Dos workers, SKIP LOCKED, cerrojo ocupado, proceso pausado con HTTP en vuelo al vencer lease, token renovado y respuestas tardías; locks Windows/Linux/volumen compartido. Persistencia cerrada durante cada llamada HTTP/refresh.
6. Carpetas: dos pedidos del mismo país, reserva simultánea, respuesta de creación perdida/409, raíz real, identidad/padre/nombre/propiedad incompatibles, carpeta confirmada eliminada y ausencia de niveles año/etapa o permisos públicos.
7. Archivo: 404 inicial, creación 409, respuesta perdida, copia ya completa y caída entre éxito remoto y commit local; mismo ID en todos los casos, sin archivo duplicado, reset ciego de identidad ni evento de subida nuevo.
8. Sesión cifrada: reinicio con misma llave, AAD de otro adjunto, corrupción/llave perdida/ACL insegura y URL ajena; no plaintext en PostgreSQL, respuestas, logs ni excepciones visibles. No leer secretos ni hacer red al construir beans.
9. Protocolo: PUT estado 308 con/sin Range, offset no alineado, ACK parcial/atrasado/sin progreso, límite del bucle, caducidad 404/410, inicio PATCH del mismo blob y longitud final exacta. Respuestas incompletas o sin enlace no fuerzan otra subida completa.
10. Fidelidad: SHA-256 local, falta/corrupción/cambio del archivo durante transferencia, SHA-256/MD5 remotos, padre/UUID/size equivocados, papelera, tipo Google Docs, enlace ausente y metadatos eventualmente disponibles. COPIADO solo con confirmación completa y actualización atómica.
11. Interrupciones/DB caída/commit incierto, renovación OAuth temporal vs revocada/expirada y logs sanitizados. Mantener la autorización real y las copias de cuenta empresa fuera de pruebas locales sin encargo explícito.

No hay resultados PASS ni evidencia de integración real. Sin pruebas escritas/modificadas o ejecutadas, builds, migraciones ejecutadas, operaciones Google, commit o push. Header.tsx, los cambios previos y las evidencias de fase 2 se conservan.

Referencias oficiales consultadas (documentación pública, no APIs de la cuenta): [carga, reanudación e IDs pregenerados](https://developers.google.com/workspace/drive/api/guides/manage-uploads), [generación de IDs](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/generateIds) y [metadatos/checksums de archivos](https://developers.google.com/workspace/drive/api/reference/rest/v3/files).
