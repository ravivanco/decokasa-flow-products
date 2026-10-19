# Fase 3, parte A, paso 3.5 — correo y pantallas

Fecha: 2026-10-17. Rama `fase-3-drive-gmail`. Entrega de código preparada sin pruebas, builds, ejecución de migraciones, arranque, autorización, subida ni envío real. La fase sigue abierta y requiere parte B y validación de Richard.

Se aplicaron shadcn, tanstack-query-best-practices, ecc-springboot-patterns y ecc-java-reviewer. Se leyó el manual Word vigente, el plan único y los informes 3.2–3.4. La prohibición expresa de pruebas/builds prevalece sobre el flujo de esas skills. Se consultaron las composiciones oficiales de shadcn (table/card/alert/button/select) y se reutilizan componentes instalados; no se añaden dependencias del proyecto ni se cambia Header.tsx.

## Outbox y procesamiento

El oyente síncrono MANDATORY y EncolarNotificacionCasoDeUso de 3.2 permanecen intactos: una transición, su historial y su aviso se confirman/revierten juntos. Se conservan UUID reales, datos snapshot y unicidad evento/destinatario normalizado. No se duplica el oyente, encolado, catálogo de archivos ni outbox. Transiciones ya cubiertas: enviar, aprobar, rechazar y devolver. PedidoCreado, vencimientos y otros eventos no se agregan por deducción.

EnviarNotificacionesPendientes ejecuta una pasada limitada de hasta 5 filas (configurable 1–25), una por vez. CorreoConfig abre una transacción corta por reclamación, consulta de lease y confirmación/fallo. El puerto externo rechaza ejecución dentro de una transacción activa; GmailNotificador conserva su protección. El POST y la renovación OAuth ocurren fuera de la transacción de negocio y de las transacciones de persistencia.

La reclamación existente usa SKIP LOCKED, propietario y lease (PT3M por defecto, admitido 1–30 minutos), incrementa intentos y deja FALLIDA/resultado_incierto antes de salir a red. Otro worker nunca reclama esa fila al caducar el lease. Antes del POST se comprueban permiso del destinatario mediante el directorio y vigencia del lease. La confirmación exige propietario/lease vigentes y guarda ENVIADA, fecha e ID real de Gmail. ENVIADA significa aceptación de Gmail, no recepción/lectura del buzón final.

Timeout, 5xx/408 del POST, respuesta incompleta o caída entre aceptación y commit dejan incertidumbre durable. Si vence el lease, una respuesta tardía no confirma ni habilita otro envío. Un fallo de DB posterior al POST interrumpe la pasada conservando la incertidumbre; nunca se convierte en rechazo definitivo ni provoca otro POST. La clave de evento/destinatario evita duplicar el encolado y el Message-ID estable identifica el mismo mensaje, pero **Gmail no garantiza exactamente una vez**. No se amplían scopes para buscar mensajes.

Los errores conocidos se guardan como causas cerradas, nunca mensajes/cuerpos HTTP, tokens, credenciales, rutas o URLs de sesión. Errores inesperados interrumpen la pasada con un log genérico, sin excepción ni destinatarios. La única tabla notificacion conserva destinatario snapshot, contador de intentos, último resultado/error y fecha; no se crea una segunda bitácora o cola. El registro no pretende guardar detalle de cada petición HTTP.

**Política conservadora de correo:** no hay reintentos automáticos tras un fallo en este paso. Se conservan los contadores/persistencia existentes, pero no se hereda el presupuesto inicial+3 de Drive ni se inventa otro. Aprobar presupuesto/esperas y procedimiento administrativo de resolución/reenvío es un pendiente externo antes de activación. Los inciertos nunca se reenvían ciegamente. No hay endpoint de reenvío o resolución administrativa sin política aprobada.

## Destinatarios y activación bloqueada

Remitente aprobado: la cuenta OAuth personal `importadora.latinoamericaworld@gmail.com`, comprobada por el mecanismo 3.1 y usada desde el backend; no se añade alias/SMTP. **F3-D8 sigue pendiente:** se necesitan correo(s) admin receptor(es), asociación a usuario(s) admin vigente(s), alcance por país y aprobación de la política de distribución. No se deduce que todos los admins ni el remitente deban recibir.

DirectorioDestinatarios continúa vacío, tal como 3.2; esto bloquea el procesamiento antes de reclamar filas o consumir intentos. Las transiciones siguen guardando una intención con destinatario nulo dentro de su transacción y conservan su avance. Esas filas nunca son enviables. No se hace backfill de eventos de fase 2 ni resolución automática/reparto retroactivo de intenciones antiguas. Adaptar el directorio y decidir el tratamiento de pendientes requiere los datos/aprobación anteriores.

Los valores por defecto continúan GOOGLE_OAUTH_HABILITADA=false, GOOGLE_DRIVE_HABILITADA=false, GOOGLE_GMAIL_HABILITADA=false y COPIA_DRIVE_AUTOMATICA=false. Se añade CORREO_AUTOMATICO=false, CORREO_LOTE=5, CORREO_INTERVALO_MS=30000 y CORREO_LEASE=PT3M. Solo se registra la tarea de correo si OAuth, Gmail y correo automático están explícitamente true; aun así el directorio vacío bloquea entrega. No se cambia .env.local, credenciales, llave ni tokens externos. No se habilita nada con esta entrega.

Google continúa En pruebas: autorización offline con caducidad aplicable de siete días para estos permisos; no se ha realizado consentimiento real. Mantener el callback fijo registrado y completar manualmente 3.1 solo con autorización posterior. Mi unidad usa carpetas aprobadas país/pedido creadas por la aplicación, sin niveles adicionales. No se modifica el worker de Drive ni la retención no aprobada.

## API y pantallas

- GET /api/v1/notificaciones: filtro opcional de estado, página desde 0 y tamaño 1–100 (default 20), total; comprueba admin activo y rol actual en servidor mediante el puerto público de usuarios, aunque el JWT conserve admin antiguo. DTO cerrado, errores sanitizados, destinatario nullable/bloqueo y resultadoIncierto. Cache-Control no-store.
- GET /api/v1/archivos/{id}/enlace: mismo permiso vigente P-17 que descarga, solo URL persistida de una copia confirmada con ID y fecha; host https://drive.google.com validado. Sin resultado confirmado devuelve 409 COPIA_PENDIENTE. No fabrica enlaces ni hace consultas Google en esta lectura. Abrir Drive exige además permisos de la cuenta en Mi unidad: no se crea compartición pública o por enlace.
- GET /api/v1/archivos/copias-fallidas: admin vigente también en servidor, array existente con paginación acotada adicional, metadatos del mismo módulo; sin rutas internas/URIs privadas. Nueva consulta SQL sobre adjunto; no migración nueva.
- /notificaciones bajo SoloAdmin y menú Correos; sustituye GmailNotificationsView de demo por Zod/TanStack Query con claves por sesión/estado/página, carga, vacío, errores, actualización y paginación. Muestra destinatario real o pendiente, intentos, resultado incierto, errores y fechas. Panel acotado de copias fallidas con acceso al pedido para descarga. No botón de enviar, reenviar o falsos mensajes de entrega.
- ArchivosEtapa conserva sesiones/carga de bloques de 3.3, bytes confirmados, pausa/reselección/reanudación, entradas, estado de copia y descarga por backend. Consulta estados cada 30 segundos solo con pantalla visible y archivos pendientes; no repite mutaciones. Muestra acceso a consultar enlace solo para COPIADO y permiso de descarga; el backend vuelve a decidir y el enlace solo aparece tras respuesta autorizada real. Se retiran URLs/IDs Drive fabricados de los auxiliares del demo. El aviso tras carga confirma recepción local, nunca copia o correo ficticios.

OpenAPI 0.7.3-borrador conserva rutas y amplía el esquema de notificación para representar intenciones pendientes/resultado incierto. No hay binarios en PostgreSQL ni migración V7: V3–V6 ya contienen los campos utilizados. No se modifica ninguna migración ni se ejecuta SQL. Dominio/aplicación siguen Java puro, sin Spring/JPA; usuarios solo por puertos públicos, sin ciclo R4/R9.

## Entrega de parte A y pendientes de 3.B1–3.B3

3.1–3.5 cuentan con entrega de código documentada, con envío bloqueado por destinatarios/política y validación funcional pendiente. Esto **no cierra la fase** ni acredita funcionamiento. Se conservan cambios previos, Header.tsx y evidencias de fase 2; no se escribe/modifica ninguna prueba, ni se hace commit/push.

### 3.B1 Archivos y recuperación — casos pendientes, no ejecutados

Probar límites menores/exactos/mayores con el valor provisional **100 MiB = 104857600 bytes**, que difiere de los “100 MB” decimales del manual (100000000); resolver la diferencia antes del caso real. Nombres y formatos pendientes no admiten una lista inventada. Asociación pedido/etapa/entrada/evento, sesiones incompletas no válidas, permisos al inicio/final, almacenamiento duradero y reinicios. Copias pendiente/copiado/fallido; intento inicial y tres reintentos Drive, fechas/Retry-After, reanudación, concurrencia y reconciliación sin duplicados, conservación de parciales/aceptados. Aplicabilidad de migraciones V4–V6 y fronteras de arquitectura/contexto quedan por comprobar en B.

### 3.B2 Correo y pantalla — casos pendientes, no ejecutados

Atomicidad acción/historial/único aviso, rollback sin fila enviable; cuatro transiciones y reentradas; normalización/unicidad evento/receptor; directorio vacío sin reclamar, receptores aprobados vigentes y sin reparto retroactivo automático. Dos workers, SKIP LOCKED, caídas antes/durante/después del POST, vencimiento de lease, commit fallido y respuesta tardía: ninguna repetición ciega. Error definitivo, autorización revocada, cuota, resultado incierto y datos sanitizados. Admin desactivado o rol retirado con JWT antiguo recibe 403 en ambos registros; paginación y separación de caché al cambiar sesión. Progreso basado en ACK, reanudar, error/pausa, enlaces solo confirmados/P-17, descarga backend; vacío/error/loading en registro. Usar dobles, sin correo real desde suites. Presupuesto de correo y resolución administrativa requieren acuerdo antes de ampliarlos.

### 3.B3 Integración Google y seguridad — casos pendientes, no ejecutados

Revisión de secretos, rutas, límites, callbacks/state/PKCE/nonce y fronteras de red; arranque con flags false sin tareas/llamadas; configuración externa privada y caducidad/revocación del modo En pruebas. Confirmar alcance efectivo drive.file sobre raíz/carpetas, cuenta, cuota y acceso privado a enlaces de Mi unidad. Solo con entorno/operación expresamente autorizados por Richard: consentimiento manual, archivo ficticio del tamaño acordado, carpeta/enlace real y correo al destinatario de prueba aprobado. Separar evidencias reales de suites con dobles; no prometer entrega final por un HTTP 2xx.

Evidencias futuras: documentos/pruebas/fase-3-informe.md, comandos/resultados/hallazgos y bloqueos cuando se autorice B. Richard valida el cierre; este documento no marca complete.

La entrega se aplicó a 32 archivos del alcance. La copia protegida conservó por huella 1259 archivos ajenos al cambio y el contenido del índice Git, incluidos Header.tsx y las evidencias previas. No se preparó ningún commit. La revisión de código Java se limitó a lectura de fuentes: fronteras de módulos, autorización vigente, SQL parametrizado, límites de transacción, fencing y errores sanitizados. No sustituye compilación, contexto Spring ni los casos pendientes de parte B.
