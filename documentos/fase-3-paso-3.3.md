# Fase 3, parte A, paso 3.3 — carga por partes y reanudación

Fecha: 2026-10-17. Rama: `fase-3-drive-gmail`. Código preparado sin compilar, ejecutar pruebas/builds, aplicar migraciones ni arrancar servicios. No se ha demostrado su funcionamiento en parte B y no se cierra la fase 3.

## Implementado y reutilizado

Se siguieron el manual Word vigente, el plan existente y el informe 3.2, con `ecc-prp-implement` y `ecc-error-handling`. Se aplicaron también las pautas de Java, migraciones, seguridad, React y TanStack Query a sus respectivas capas. La instrucción expresa de parte A prevalece sobre las tareas de pruebas/builds de esas skills. No se sincronizó el remoto, archivó el plan ni marcó la fase completa.

El mismo módulo `archivos` incorpora sesiones técnicas mediante `GestionarCarga`, `CargaRepositorio` y `AlmacenCargas`, con caso de uso Java puro, composición transaccional en `CargasConfig`, adaptadores SQL/disco y `CargasController`. No hay otro módulo de carga, catálogo de archivos o almacenamiento de binarios en PostgreSQL.

Se conserva el límite provisional de **100 MiB = 104857600 bytes por archivo**; se respeta un límite de configuración menor. Los “100 MB” decimales del manual equivaldrían a 100000000 bytes: la diferencia continúa documentada, sin cambiar silenciosamente el valor provisional. No se crea una lista de extensiones/MIME admitidos.

La política de nombre es la existente `Adjunto.limpiarNombre`: basename, sin controles, valor seguro si queda vacío y máximo 255 puntos de código. El nombre no es una ruta. La firma técnica y SHA-256 se calculan con los mismos mecanismos de fase 2; no se leen productos, cantidades, precios, colores ni otra información comercial para aceptarlo/rechazarlo.

## API y cliente

OpenAPI pasa a `0.7.2-borrador` y describe las cuatro operaciones bajo las rutas del módulo existente:

| Operación | Resultado |
|---|---|
| POST `/pedidos/{pedidoId}/etapas/{numeroEtapa}/archivos/cargas` | Crea/recupera sesión con UUID de idempotencia por usuario y comando saneado. Devuelve 201 y Location. La misma clave con otro comando devuelve conflicto. |
| GET `/archivos/cargas/{cargaId}` | Estado, offset confirmado, huellas de bloques, pedido/etapa/entrada y, cuando está completada, adjunto/evento. Sin rutas de disco, UUID reservado interno o datos de otra cuenta. |
| PUT `/archivos/cargas/{cargaId}/fragmentos` | Bytes secuenciales, Content-Range y X-Fragmento-SHA256. Repetición idéntica reconocida; huecos, cambios y tamaño/huella divergentes rechazados. |
| POST `/archivos/cargas/{cargaId}/finalizacion` | Crea el adjunto y evento una sola vez; una repetición confirmada devuelve el mismo Adjunto. |

El multipart de fase 2, listado y descarga se conservan. La pantalla `ArchivosEtapa` usa la nueva carga con progreso de bytes confirmados, pausa y reanudación explícita. El cliente HTTP existente admite un Blob repetible con cabeceras cerradas; sigue usando JWT en memoria y la coordinación de sesión existente, sin permitir reemplazar Authorization mediante cabeceras arbitrarias. CORS añade únicamente las dos cabeceras del protocolo.

En `sessionStorage` quedan la clave idempotente y el UUID de carga, asociados mediante una huella de metadatos a usuario/pedido/entrada/tipo/archivo. Nunca quedan bytes ni tokens. Tras recargar se debe elegir el mismo archivo, con los mismos metadatos; el cliente comprueba todos los bloques del prefijo confirmado contra las huellas del servidor antes de continuar. No se afirma conocer la huella del contenido que nunca se recibió. Cambiar nombre, tamaño o fecha del archivo puede seleccionar una sesión nueva; la anterior conserva sus bytes.

Una caída de red no dispara reintentos ocultos de la mutación. La persona reanuda consultando el servidor; la renovación de JWT conserva el mecanismo existente y repite el mismo bloque idempotente. Pausar/cerrar la pestaña no elimina lo confirmado ni equivale a borrar la sesión. Una finalización cuyo resultado se perdió se recupera por GET/POST, sin crear otro evento.

Los códigos nuevos son `CARGA_CONFLICTO`, `CARGA_INCOMPLETA`, `CARGA_OCUPADA` (409) y `CARGA_ALMACENAMIENTO` (503), con `application/problem+json`. Se conservan ETAPA_CAMBIO, permisos y 413. Errores de disco o cortes de lectura no se convierten en un adjunto aceptado. Mensajes cerrados no exponen rutas, contenido o causas SQL.

## Durabilidad, concurrencia y atomicidad

V5 `V5__cargas_reanudables.sql` es la siguiente versión local disponible, **creada pero no aplicada**. V1–V4 no cambian. Añade `carga_archivo` y `carga_fragmento`, solo metadatos, con propietario, pedido/etapa/entrada, UUID reservado, offset, huellas, estados y enlaces al adjunto/evento final. La FK de entrada es compuesta; la del evento se comprueba al commit compartido con JPA.

Una sesión RECIBIENDO no tiene `adjunto_id` ni `evento_id`. Tampoco los tiene cuando el offset ya alcanzó el total pero falta finalizar. Por eso no participa en las consultas de adjuntos ni satisface requisitos documentales. Al finalizar se reutiliza `SubirArchivoCasoDeUso.registrar`: conserva la relación `ARCHIVO_SUBIDO`/SUBIDO, pedido, etapa y entrada del mismo modelo. Adjunto, historial y sesión COMPLETADA se confirman o revierten en la misma transacción.

Los bloques son de **4 MiB** salvo el último, alineados y secuenciales: como máximo 25 para el límite provisional. Es una elección técnica concretada a partir del plan, no una nueva respuesta comercial del cliente. La longitud real y SHA-256 se comprueban; declarar otro total, enviar huecos o repetir un bloque distinto no adelanta el offset.

El parcial vive en `.cargas/{cargaId}.parte` del MISMO volumen duradero de `AlmacenLocal`. Se serializan escrituras/finalización por sesión con bloqueo de archivo entre procesos. Las transacciones cortas bloquean pedido y luego carga; las actualizaciones usan el offset esperado. Los bytes se escriben y se fuerza su sincronización antes de guardar offset/huella en base. Recepción y cálculo completo de huellas ocurren fuera de la transacción de negocio.

Si falla el commit de un bloque, pueden quedar bytes por encima del offset confirmado; se truncan bajo bloqueo antes de la siguiente escritura. Si faltan bytes por debajo de ese offset, se responde fallo de almacenamiento en lugar de fingir recuperación. La finalización vuelve a comprobar las huellas de todos los bloques, tamaño completo y SHA-256 total.

El adjunto tiene un UUID reservado desde el inicio y la misma clave interna de fase 2 `pedidoId/etapa/adjuntoId`. El destino se prepara fuera de la transacción final. Si el proceso cae entre mover el archivo y confirmar la base, el parcial y UUID reservado permiten reconocer ese destino y comprobar tamaño/huella antes de reutilizarlo. No se sobrescribe otro contenido. El rollback no borra ese destino reservado; una repetición usa la misma sesión. Una sesión ya completada devuelve el mismo adjunto/evento sin otra inserción.

Esta estrategia puede conservar simultáneamente parcial y definitivo, además de un temporal de trabajo durante la finalización. La limpieza automática no está implementada ni autorizada en 3.3. Hay que dimensionar el volumen antes de activarlo; los parciales completos/incompletos se conservan hasta definir retención. La garantía codificada se orienta a reinicio de proceso y volumen persistente; fallos físicos/semántica de sincronización del filesystem quedan para parte B.

## Permisos, carpetas y decisiones pendientes

- Inicio, fragmentos y finalización comprueban propietario y permisos vigentes mediante `PedidosParaArchivos`; no se confía solo en roles del JWT. La finalización comprueba otra vez la entrada actual dentro de la transacción del registro, incluso si cambió durante recepción/cálculo. Una cuenta desactivada, reasignación o cambio de entrada impide un adjunto nuevo. Un admin no reanuda una sesión ajena.
- Una repetición ya completada exige que su propietario mantenga acceso de consulta al pedido; no intenta autorizar una nueva subida a una etapa que ya avanzó. El resultado no contiene bytes.
- La estructura aprobada de Drive sigue siendo país/pedido. No se añaden año/etapa ni se utiliza una carpeta raíz inventada. `.cargas` y la clave interna anterior son almacenamiento técnico local de fase 2, no una decisión nueva de carpetas Drive.
- El ID/acceso de la carpeta real de Mi unidad sigue pendiente para la copia posterior. No bloquea la recepción local de 3.3; sí impide activar una copia sin resolverlo. OAuth, Drive y Gmail permanecen desactivados y no se accede a sus credenciales/tokens en este paso.
- Caducidad y limpieza siguen propuestas, sin duración aprobada. Se difiere el `caducaEn` propuesto en el plan: no hay TTL, purga, scheduler ni valores de retención inventados. Falta acordar conservación, cuotas/capacidad y recuperación administrativa de sesiones que ya no puedan completarse.
- La coordinación de disco requiere que los procesos vean el mismo volumen y que sus bloqueos de archivo sean efectivos. No se anuncia soporte multiinstancia con discos independientes; hace falta resolver ese despliegue antes de usarlo así.
- Formatos admitidos, subcarpetas opcionales, destinatarios y políticas de correo/copia siguen pendientes. No se implementan workers, reintentos de copia Drive, conciliación remota, limpieza de 3.4, plantillas, envío o pantallas de 3.5.

## Casos pendientes de parte B — no ejecutados ni escritos como pruebas

1. Compilación/configuración y fronteras R1–R4/R9; coexistencia con multipart, listado, descarga y el outbox 3.2; ausencia de conexiones Google con flags desactivados.
2. Aplicación de V5 sobre V4 y datos de fase 2, restricciones, claves/FK diferidas, rollback e índices; compatibilidad JDBC/JPA y triggers de historial. Coordinar versión con ramas paralelas antes de aplicar; reversión hacia delante sin borrar bytes/metadatos activos.
3. Archivo vacío; 1 byte; límites 4 MiB/100 MiB; exceso de total real/declarado; configuración menor; EOF y cuerpo interrumpido; nombres con rutas, controles, Unicode y longitud; formatos desconocidos sin rechazo comercial.
4. Inicio simultáneo/repetido con misma clave; clave con otro comando; carga ajena incluso por admin; usuario desactivado, rol quitado, reasignación y nueva entrada entre inicio/bloques/finalización.
5. Rangos adelantados/desalineados, duplicado idéntico o divergente, SHA incorrecto, último bloque, dos peticiones concurrentes, bloqueo entre procesos y ausencia de avance cuando falla la transacción posterior al force.
6. Reinicio con tail sin confirmar, archivo más corto que offset, disco lleno, archivo ausente/corrupto y volumen no durable; comprobar que ningún parcial satisface requisitos. Semántica Windows/Linux de locks, ATOMIC_MOVE y sincronización.
7. Finalización incompleta; repetición concurrente y tras pérdida de respuesta; caída antes/después del movimiento y antes/después del commit; fallo de historial/FK/commit sin otro evento o adjunto ni borrado del parcial recuperable.
8. Cliente: pausa, recarga, reselección/prefijo diferente, almacenamiento bloqueado/corrupto, JWT renovado o sesión cambiada, desconexión, respuesta perdida, CORS y límites nginx; progreso recibido distinto de copia Drive; accesibilidad del formulario/progreso.

Ninguno de estos casos tiene resultado acreditado. No se escribieron/modificaron pruebas ni se ejecutaron pruebas/builds/migraciones, autorizaciones, subidas o envíos reales, commit o push. Los cambios previos, Header.tsx y evidencias de fase 2 se conservan.
