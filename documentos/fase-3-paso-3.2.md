# Fase 3, parte A, paso 3.2 — puertos, adaptadores y persistencia

Fecha: 2026-10-17. Rama de trabajo: `fase-3-drive-gmail`. Entrega de código sin compilar, ejecutar pruebas ni aplicar la migración. No certifica el funcionamiento de los adaptadores ni cierra la parte B.

Se siguieron `ecc-hexagonal-architecture` y `ecc-api-connector-builder`; revisión de fuente según `ecc-java-reviewer`, persistencia y seguridad. Fuentes: manual Word vigente (3.2), arquitectura R1–R9, modelo de datos, OpenAPI, ADR-0011/0012/0017 y plan existente de fase 3. Como patrones se reutilizaron el almacenamiento local y repositorio de archivos, el adaptador OAuth y la publicación síncrona del historial. Las instrucciones del encargo excluyen las pruebas/builds de los workflows de skills.

## Implementado

| Área | Resultado |
|---|---|
| Archivos | `AlmacenArchivos` y adaptador pasivo `GoogleDriveAlmacen`: reservar id, iniciar sesión remota, consultar progreso, transmitir un fragmento y leer metadatos/enlace. API Drive v3, OAuth de usuario y Mi unidad, sin parámetros de unidad compartida. |
| Persistencia de copia | `CopiaAdjuntoRepositorio`/`CopiaAdjuntoSqlRepositorio` sobre la misma tabla `adjunto` de fase 2. Reclamación acotada con SKIP LOCKED, id reservado persistente, leases y actualizaciones condicionales. No modifica UUID, clave local, entrada, huella ni relaciones históricas. |
| Correo | `Notificador`/`GmailNotificador`: MIME UTF-8, remitente de la cuenta autorizada, Message-ID estable vinculado a notificación, un POST explícito a Gmail y resultado con id del proveedor. Sin plantillas de negocio, worker o reintentos internos de aplicación. |
| Outbox | `notificacion` es cola y registro, con estado, datos JSONB, próximo intento, error sanitizado, identificador Gmail, Message-ID, lease y resultado incierto. No hay otra tabla outbox. |
| Atomicidad | Historial publica `EventoPedidoRegistrado` después de registrar su UUID real, por un puerto compartido síncrono. El listener de notificaciones es MANDATORY: falla todo si no se puede conservar el aviso. Ninguna llamada a Google ocurre en este camino. |
| Fronteras | Casos de uso/puertos/dominio Java puro. SQL/Spring/Jackson/HTTP solo en adaptadores y configuración. Historial no depende de notificaciones; Gmail no depende de adaptadores de archivos; se comparte únicamente el puerto interno de acceso a la cuenta y eventos. |
| Credencial | Reutiliza el almacén cifrado externo del 3.1, sin un segundo almacén de OAuth. Obtención y renovación offline bajo demanda fuera de transacción; bloqueo de archivo compartido con el consentimiento y reemplazo atómico. Una renovación no prolonga la autorización de En pruebas. |
| Configuración | OAuth permanece false. `GOOGLE_DRIVE_HABILITADA` y `GOOGLE_GMAIL_HABILITADA` tienen valor false por defecto. Cada proveedor también necesita OAuth y credencial vigentes; los constructores no abren conexiones ni leen secretos. |

No se alteran endpoints, respuestas o permisos de archivos de fase 2. El backend sigue conservando y descargando el binario local aunque se confirme una copia Drive. No se crean permisos públicos de Drive.

## Migración V4

`backend/src/main/resources/db/migration/V4__drive_notificaciones_outbox.sql` usa la siguiente versión disponible localmente. V1–V3 se conservan. No se ejecutó Flyway ni SQL.

Amplía `adjunto` con `drive_id_reservado`, `drive_enlace`, `proximo_intento_en` y lease. Mantiene la restricción de V3: COPIADO y drive_id definitivo se guardan juntos. El id reservado no se confunde con una copia completa. Las actualizaciones de resultado exigen estado pendiente, propietario y lease vigente; una respuesta tardía no puede pisar el trabajo de otro propietario. Los índices de pendientes existentes se reutilizan, y la reserva remota tiene unicidad.

Crea `notificacion`, FK al evento y al pedido, unicidad `(evento_id,destinatario_clave)` normalizada y los índices de consulta/reclamación. La FK de evento es diferible al commit: historial usa JPA `persist`, mientras outbox usa JDBC sobre la misma DataSource/transacción. La fila del evento debe existir al confirmar; no se permite un aviso huérfano. No se cambian los triggers de historial ni se generan avisos de hechos históricos.

La migración añade columnas/restricciones e índices y requerirá evaluar bloqueos y coordinación con otras ramas antes de aplicarla. No está aplicada ni comprobada en una base persistente.

## Destinatarios pendientes e idempotencia

La decisión F3-D1 está aprobada: acción, historial y aviso se confirman/revierten juntos. F3-D8 todavía no determina qué admin(s) reciben cada aviso. El directorio queda explícitamente sin receptores; no se sustituye por la cuenta Gmail remitente, usuarios del demo o todos los admins por inferencia.

Para conservar el aviso confirmado, V4 permite una intención con destinatario NULL y clave técnica vacía, única por evento. Es una concreción técnica del esquema propuesto, no una nueva aprobación del cliente. Esas filas **nunca son reclamables ni enviables**. El código encola transiciones EtapaEnviada, EtapaAprobada, EtapaRechazada y PedidoDevuelto; no deduce envíos por archivos/observaciones/reasignaciones. Vencimientos pertenecen al desarrollo posterior de plazos.

El puerto permite resolver el destinatario pendiente; el caso de uso administrativo/política de expansión a varios destinatarios queda fuera de 3.2. Una vez definida esa política, cada receptor real tendrá su fila y clave estable evento/correo. No se alterará un receptor ya enviado. Asunto y datos se conservan como snapshot; no se incorpora la versión de plantilla a la clave idempotente.

Al reclamar correo se marca preventivamente resultado incierto antes de salir de la transacción corta. Si el proceso cae antes/durante/después del POST, no se vuelve a reclamar ciegamente esa fila por caducar el lease. Una respuesta inequívoca puede confirmar entrega o registrar fallo/reprogramación mediante el mismo propietario y lease vigente. Si el lease venció, queda para resolución posterior. El worker y esa resolución pertenecen al 3.4.

El índice único controla duplicados del outbox y el Message-ID ayuda a identificar el mismo mensaje. **Gmail no ofrece una garantía de exactamente una vez en este mecanismo.** ENVIADA significará aceptación del proveedor, no lectura o recepción final del destinatario. Timeout, respuesta incompleta o error ambiguo no se convierte en éxito ni dispara un reenvío automático.

## Límites y bloqueos

- OAuth, Drive y Gmail siguen desactivados. Ninguna autorización real se ha comprobado. El JSON, la llave y los tokens permanecen externos a Git; no se copia ni imprime su contenido. El callback fijo, su validación opcional de redirect_uris y state/PKCE/nonce/admin del 3.1 se conservan.
- Modo En pruebas: con los permisos Gmail/Drive acordados, el grant/refresh token tiene caducidad de siete días. La renovación del access token no elimina ese límite; `invalid_grant`, revocación o vencimiento exigirán intervención/autorización manual posterior.
- Hace falta aprobación de destinatarios F3-D8 y de la política operativa F3-D5. Los adaptadores no resuelven esas decisiones de negocio.
- Drive exige carpeta padre accesible a `drive.file`; conocer un ID no concede acceso. No se crean carpetas ni se usa la raíz como sustitución silenciosa de país/pedido. La organización, propiedad, cuota y permisos reales de Mi unidad están pendientes; no existe una unidad compartida de Workspace.
- La sesión remota se maneja como valor interno protegido, sin logs/toString revelador. Su URI no se almacena en PostgreSQL en texto plano. La persistencia protegida y recuperación de sesiones se concretará en 3.4, antes de activar transferencias. Un 404 no prueba que sea seguro crear otro archivo; conciliación posterior obligatoria.
- Las primitivas de transferencia de un fragmento no implementan una carga de usuario ni la coordinación de reanudación. No hay nuevas rutas, sesiones navegador→backend, workers, scheduler, limpieza, reintentos, envío operativo o pantallas: son 3.3–3.5.
- Se conserva **100 MiB = 104857600 bytes** como límite provisional. El manual dice “100 MB”, que en decimal son 100000000 bytes; no se resuelve ni aprueba silenciosamente la diferencia. Formatos comerciales, año/etapa en carpetas y demás preguntas previas siguen pendientes.

## Casos reservados a la parte B — no ejecutados

1. Compilación/configuración del contexto y reglas R1–R4/R9; resolución de beans con los tres flags false, sin lectura de secretos ni red. Ausencia de acceso remoto dentro de una transacción.
2. V4 sobre V3, sobre datos de fase 2 y con una base persistente: restricciones, índices, leases, metadatos intactos, archivos locales conservados y conflictos con migraciones paralelas.
3. Acción/historial/outbox en una sola transacción JPA/JDBC: commit, rollback de la acción, fallo al insertar aviso, FK diferida y fallo de commit sin ningún aviso enviable. No depender del orden de oyentes del evento original.
4. Repetición del mismo evento/destinatario (incluida normalización); distintas reentradas del pedido; filas sin receptor nunca reclamables; resolución concurrente y expansión futura a múltiples destinatarios.
5. Reclamación concurrente con SKIP LOCKED, exclusión por lease, vencimiento y respuestas tardías; caída antes/después del envío sin repetición ciega; autorización requerida y errores sanitizados. El worker real todavía no existe.
6. Protocolos de adaptadores mediante dobles locales en un encargo de parte B: id reservado, cabecera Location permitida, rechazo de redirecciones/URI ajenas, rangos 308, límite del fragmento, 404/409, contenido incompleto y metadata/UUID de conciliación. MIME, UTF-8, Message-ID, respuesta Gmail válida/ambigua y ausencia de secretos en logs/errores.
7. Almacén OAuth: cifrado/ACL/rutas externas, renovación concurrente y consentimiento, conservación de refresh/grant, alcance/cuenta/cliente incorrectos, caducidad/revocación y fallos de permisos sin filtraciones. La URI de retorno debe estar registrada en Cloud, aunque pueda faltar en el JSON.
8. Cualquier autorización, copia o envío real requerirá un encargo explícito posterior y no forma parte de esta entrega. El funcionamiento externo no se presume a partir de la lectura de código.

Se conservaron los cambios previos, incluido Header.tsx y las evidencias de fase 2. No se escribieron/modificaron pruebas, ejecutaron pruebas/builds, conectaron servicios, enviaron mensajes ni hicieron commit/push.

## Referencias de protocolo

- [Drive: cargas reanudables e identificadores pregenerados](https://developers.google.com/workspace/drive/api/guides/manage-uploads).
- [Gmail: MIME y base64url](https://developers.google.com/workspace/gmail/api/guides/sending).
- [OAuth web: acceso offline](https://developers.google.com/identity/protocols/oauth2/web-server#offline).

Se consultó documentación oficial pública; no las APIs ni la cuenta de la empresa.
