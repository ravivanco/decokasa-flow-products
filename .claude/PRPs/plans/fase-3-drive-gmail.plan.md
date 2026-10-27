# Plan de implementación de la fase 3 — Drive y Gmail — Parte A

> **Actualización 2026-10-26 (ADR-0029):** Inicio común con estadísticas y alertas, y documentos vigentes destacados en el detalle del pedido. Parte A compilada, sin pruebas; SQL comprobado en la base restaurada. Detalle y prompt: [entrega ADR-0029](../../../documentos/entrega-inicio-adr29-parte-a.md).
>
> **Actualización 2026-10-26 (ADR-0028):** los documentos se consultan desde DECOKASA; el backend sirve el archivo local o lo transmite desde Drive con la conexión central (verificando tamaño y SHA-256), sin acceso directo de los usuarios a Drive. P-17 ampliada a los documentos presentados. Parte A compilada, sin pruebas ni recorrido real. Detalle y prompt: [entrega ADR-0028](../../../documentos/entrega-documentos-adr28-parte-a.md).
>
> **Actualización 2026-10-26 (ADR-0027), prevalece sobre la organización del aviso siguiente:** las copias nuevas van a `Mi unidad/DECOKASA/{número del pedido}/{usuario-id8}/{AAAA-MM-DD}/` (pedido → autor histórico → día de subida, `adjunto.subido_en` en la zona del país del pedido). V24 `carpeta_pedido_drive` registra la carpeta del pedido al crearlo; las carpetas globales por usuario dejan de crearse. Formatos y seguridad de ADR-0026, vigentes. Parte A compilada, sin pruebas ni recorrido real; parte B y cierre pendientes. Detalle y prompt: [entrega ADR-0027](../../../documentos/entrega-archivos-adr27-parte-a.md).
>
> **Encargo vigente 2026-10-26:** Richard ratifica ADR-0026: formatos PDF/XLSX/XLS/CSV/JPG/JPEG/PNG y `Mi unidad/DECOKASA/Usuarios/{usuario}/{país}/{AAAA}/{MM}/{fecha}_{pedido}`. Sustituye las rutas de ADR-0025 para copias nuevas; conserva las anteriores. Esta ejecución completa la parte A sobre los cambios existentes, sin pruebas, builds, migraciones aplicadas ni activación de Google/Gmail/IA/workers. La validación de parte B y el cierre siguen pendientes. Detalle y prompt: [entrega de parte A](../../../documentos/entrega-archivos-adr26-parte-a.md).

> **Actualización vigente, 2026-10-19 — decisiones provisionales del equipo aprobadas por Richard para desarrollo.** Correo: intento inicial + dos reintentos, bases 1 y 5 minutos, Retry-After mayor, solo rechazo transitorio confirmado sin aceptación; permanentes e inciertos no se reenvían automáticamente. Cuota: 10 GiB globales / 2 GiB por usuario; dos cargas activas personales / diez globales; configurable, reserva atómica y sin doble contabilización reserva/final. Código incorporado, pendiente de revalidación; migraciones V7/V8 sin ejecutar. Google real 3.B3 **pospuesto por decisión del usuario**; OAuth, workers y schedulers desactivados; fase abierta. Este estado prevalece sobre las referencias históricas a política de correo/cuota sin aprobación. Detalle, contabilidad y pendientes: [implementación local](../../../documentos/pruebas/fase-3-pendientes-locales-implementacion.md).

## Resumen y alcance

Ampliar el módulo `archivos` operativo de fase 2 con recepción reanudable y copia a Mi unidad de Drive de la cuenta Gmail personal autorizada (ADR-0017). Incorporar `notificaciones`, previsto por la arquitectura, con cola persistente, envío desde el backend y registro exclusivo para admin. Conservar la descarga por el servidor, la identidad de cada entrada de etapa y el historial de solo inserción.

Como responsable de una etapa quiero subir y reanudar mis documentos sin perderlos; como admin quiero conocer las copias fallidas y el resultado de los correos de cada transición.

Este documento conserva el plan de los pasos 3.1 a 3.5 de la parte A del manual Word. **Actualización 2026-10-17:** Richard autorizó 3.1 y posteriormente solo los puertos/adaptadores/persistencia de 3.2, después la recepción reanudable de 3.3 y copia/recuperación Drive de 3.4 y ahora correo/pantallas de 3.5, con OAuth de usuario/Gmail personal y Mi unidad. Los resultados se registran en este mismo plan; 3.3 queda codificado por encargo posterior, sin comprobaciones de parte B; 3.4 queda codificado por encargo posterior, pendiente de parte B; 3.5 queda codificado por encargo posterior, con destinatarios pendientes y sin validación de parte B. OAuth permanece desactivado. No autoriza operaciones Google reales, pruebas/builds, ejecución de migraciones, commit o push. Los casos pendientes de parte B se documentan en un informe separado y no se ejecutan en la parte A.

## Estado de partida y fuentes

- Fecha de preparación: 2026-10-16. Rama observada: `fase-3-drive-gmail`; HEAD local `6caa56c`. No se cambió de rama ni se consultó el remoto.
- No existía `fase-3-drive-gmail.plan.md` ni otro plan de fase 3 en `.claude/PRPs/plans/` al comenzar; se crea un único plan en esa ruta.
- Fases 1 y 2: completas **solo en desarrollo con datos ficticios**, según las validaciones registradas en el PRD. Producción y datos reales no autorizados. El informe de fase 2 contiene registros históricos anteriores al cierre; prevalece la validación final del 2026-10-16.
- Se conservan el cambio existente de `frontend/src/components/Header.tsx`, `.vscode/`, documentos, archivos sin seguimiento y todas las evidencias pendientes de fase 2. No forman parte del alcance editable de la implementación propuesta.
- Complejidad: grande, transversal a persistencia, archivos, historial, notificaciones y frontend. Dependencias: base/acceso de fase 1 y contrato e implementación real de archivos de fase 2, aunque el PRD permita paralelismo entre fases 2 y 3.
- Se utiliza `ecc-prp-plan` y su referencia original, adaptando su plantilla al límite expreso de parte A. No se cambia el estado del PRD a `in-progress` por haber redactado un plan; no se modifica el PRD en este encargo.

### Lectura obligatoria y descubrimientos

Las rutas de este plan se expresan desde la raíz del repositorio.

| Prioridad | Fuente | Contexto que debe conservar la implementación |
|---|---|---|
| P0 | `documentos/manual-paso-a-paso.docx`, fase 3, parte A | Numeración vigente: 3.1 Google; 3.2 puertos/persistencia; 3.3 carga; 3.4 cola/recuperación; 3.5 correo/pantallas. El `.md` del manual conserva una secuencia anterior y no sustituye al Word solicitado. |
| P0 | `.claude/PRPs/prds/decokasa.prd.md`, Open Questions, Technical Approach, Implementation Phases y validación final de fase 2 | Requisitos, pendientes y alcance de desarrollo. |
| P0 | `documentos/arquitectura.md`, §§2, 3.1, 4.5, 4.6 y 8 | R1–R9, puertos de archivos/correo, límites entre módulos y propuestas. Su referencia antigua a archivos en memoria en fase 2 está superada por P-05 y el código actual. |
| P0 | `documentos/api/openapi.yaml`, introducción y contradicción 7; rutas de archivos y `/notificaciones`; esquemas `Adjunto`, `Notificacion`, estados y `Problema` | Contrato vigente y límite provisional; no inventar rutas paralelas. |
| P0 | `documentos/adr/0003-drive-cuenta-servicio.md`, `0005-alertas-gmail-admin.md`, `0007-documentos-sin-validacion.md`, `0011-monolito-modular-hexagonal.md`, `0012-transacciones-y-outbox.md`, `0013-canal-de-correo.md`, `0016-spring-boot-4.md` y `README.md` | Separar decisiones del cliente, del equipo y propuestas; Outbox aprobado técnicamente por Richard el 2026-10-16; Gmail API con OAuth de usuario y Mi unidad acordados el 2026-10-17 y registrados en ADR-0017. ADR-0012 conserva su texto anterior sobre outbox hasta una actualización documental separada. |
| P0 | `backend/src/main/resources/db/migration/V3__archivos_y_entradas.sql` y `documentos/modelo-datos.md` §§3.4–3.6 | `entrada_etapa`, `adjunto`, `evento_adjunto`, restricciones de copia e historial; `notificacion` está diseñada pero aún no migrada. |
| P0 | `.claude/PRPs/plans/fase-2-motor-flujo.plan.md`, P-05, P-11, P-13, P-16, P-17, P-18 y resultado final; `documentos/pruebas/fase-2-informe.md`, consolidación y cierre final | Conservar decisiones provisionales y backlog sin reabrir ni declarar resueltos los pendientes. |
| P0 | `backend/src/main/java/com/decokasa/seguimiento/archivos/` | `SubirArchivoCasoDeUso`, `ArchivosConfig`, `AlmacenLocal`, `AlmacenLocalEnDisco`, `AdjuntoRepositorio`, `Adjunto`, `AdjuntoEntity`, controladores y descarga reales. |
| P0 | `backend/src/main/java/com/decokasa/seguimiento/compartido/domain/eventos/EventoDePedido.java` y `historial/application/usecase/RegistrarEventoPedidoCasoDeUso.java` | `eventoId()` puede ser nulo; historial genera el UUID. Es necesario enlazar la cola al UUID realmente insertado. |
| P1 | `historial/adapter/in/eventos/OyenteEventosPedido.java`, `compartido/adapter/out/PublicadorEventosSpring.java` y `compartido/application/port/out/{PublicadorEventos,Reloj}.java`, bajo el paquete base anterior | Publicación síncrona y oyente `MANDATORY`, reloj del servidor y transacción atómica. |
| P1 | `frontend/src/api/{cliente,pedidos}.ts`, `frontend/src/components/pedidos/{ArchivosEtapa,DetallePedido}.tsx`, `frontend/src/components/pedidos/formato.ts` | Consultas reales, Zod, errores, listado por entrada y descarga. |
| P1 | `frontend/src/rutas.tsx`, `components/{Sidebar,GmailNotificationsView}.tsx`, `auth/`, `components/ui/` y `documentos/diseno/DESIGN.md` | `SoloAdmin`, navegación, tokens y componentes. La vista Gmail del demo aún usa memoria y no tiene ruta. |
| P1 | `backend/build.gradle`, `backend/src/main/resources/application.yml`, `.env.example`, `docker-compose.yml`, `frontend/limite-archivos.envsh` | Java 21, Spring Boot 4.1.1, dependencias actuales sin clientes Google, volumen `archivos-datos` y límites coordinados. No leer `.env` ni credenciales para redactar el plan. |

## Decisiones aprobadas, propuestas y dependencias externas

### Base aprobada que se reutiliza

| Base | Estado y efecto |
|---|---|
| Drive desde backend | ADR-0003 registra la opción original de cuenta de servicio/unidad compartida. **Richard acordó el 2026-10-17 usar OAuth de usuario y Mi unidad de su Gmail personal (ADR-0017)**. Los tokens y el tráfico de archivos/correo siguen solo en backend; la única visita del navegador a Google es el consentimiento inicial administrativo. |
| Carpetas por país y pedido | Requisito 4.7. Año y etapa como subcarpetas siguen propuestos; el nombre del pedido no se vuelve a generar. |
| Correo al admin por etapas y vencimientos | Aprobado por cliente, ADR-0005. No extender destinatarios al responsable sin respuesta. |
| Sin interpretación comercial de documentos | Aprobado por cliente, ADR-0007. Detección técnica de tipo, tamaño, huella y nombre sí corresponde. |
| Núcleo Java puro y transacciones fuera de casos de uso | Aprobado por equipo, ADR-0011 y parte aceptada de ADR-0012. Persistencia y Google viven en adaptadores. |
| Archivos reales de fase 2, P-05 opción A | Aprobado por equipo; reutilizar módulo, volumen y tablas. No equivale a aprobación de formatos o límites del cliente. |
| Identidad y permisos vigentes | Mantener `pedidoId`, número de etapa y `entradaId`, más `adjuntoId` y `eventoId`. P-13/P-17/P-18 conservan carácter provisional del equipo. |

### Registro de aprobación técnica F3-D1

**Richard aprobó el 2026-10-16** usar `notificacion` como outbox persistente. Es una decisión del equipo, no una nueva respuesta del cliente. Su alcance es:

1. Cada transición confirmada guarda su aviso PENDIENTE en la misma transacción de la acción y del evento de historial. No hay otra tabla outbox para duplicar ese cometido.
2. El worker solo toma filas confirmadas. Si la transacción se revierte, no queda ningún aviso enviable y no se realiza una llamada al proveedor por esa transacción.
3. El envío a Google ocurre después del commit, fuera de la transacción de la acción/historial. Un fallo del proveedor no revierte ni altera el avance confirmado del pedido; el aviso conserva estado y datos duraderos para recuperación. No se pierde ni se marca enviado por capturar una excepción.
4. La clave de idempotencia es estable y se vincula al UUID real del evento y al destinatario normalizado: `UNIQUE(evento_id, destinatario_normalizado)` para los avisos de transición. Cada reintento reutiliza la misma fila y clave. La plantilla y sus datos se conservan como snapshot; cambiar o versionar la plantilla no permite crear otro aviso para el mismo evento/destinatario.
5. La unicidad evita duplicados de encolado y permite controlar repeticiones; no garantiza entrega exactamente una vez en el proveedor. Los resultados inciertos requieren tratamiento explícito sin afirmar una garantía que Google no ofrece.

La aprobación de F3-D1 no resolvía por sí sola F3-D2; Richard la acordó después el 2026-10-17 como Gmail API con OAuth de usuario y Mi unidad (ADR-0017). F3-D3 se concreta en 3.2 con un publicador compartido después del historial. Siguen pendientes F3-D5 (presupuesto/esperas y política de resultado incierto), destinatarios y configuración privada. La aprobación del outbox no autorizó por sí sola implementación: la autorización posterior cubre 3.1 y, por encargo expreso del 2026-10-17, solo la infraestructura de 3.2. Google permanece desactivado.

### Estado de decisiones y propuestas pendientes

| ID | Estado o decisión | Alcance y bloqueo |
|---|---|---|
| F3-D1 | **APROBADA — decisión técnica de Richard, 2026-10-16** | `notificacion` es el outbox persistente y registro: aviso, acción e historial se confirman o revierten juntos. Envío posterior fuera de esa transacción, con recuperación e idempotencia por evento/destinatario. Su implementación quedó autorizada expresamente para 3.2; no activa Google. Detalle en el registro de aprobación. |
| F3-D2 | **ACORDADA por Richard, 2026-10-17; ADR-0017** | Gmail API con OAuth de usuario de la cuenta Gmail personal de empresa, sin cuenta de servicio ni delegación Workspace. Cliente web, callback local fijo, scopes gmail.send/drive.file/openid/userinfo.email, Mi unidad y modo En pruebas. Las rutas privadas y cuenta esperada quedaron configuradas en 3.1; falta consentimiento real y habilitación expresa. `Notificador` se incorpora en 3.2, sin activarlo. |
| F3-D3 | Concreción técnica codificada en 3.2, pendiente de parte B; sin nueva aprobación de negocio | Publicar un aviso interno `EventoPedidoRegistrado` después de insertar historial, dentro de la misma transacción, con su UUID real y el evento original. Notificaciones escucha ese aviso; no deduce el id por hora/pedido ni genera otro UUID. Detalle en 3.2. |
| F3-D4 | Diseño técnico concretado en 3.3, pendiente de parte B; no nueva aprobación comercial | Contrato de sesiones bajo las rutas existentes de archivos, fragmentos secuenciales de 4 MiB, offset persistente y finalización idempotente. Las cuatro rutas se incorporan a OpenAPI 0.7.2-borrador al implementar el encargo 3.3; sin TTL/purga mientras la retención no esté acordada. |
| F3-D5 | Drive autorizado en 3.4; correo **aprobado provisionalmente para desarrollo por Richard, 2026-10-19**, implementado pendiente de revalidación | Drive conserva inicial + tres reintentos (1/5/15 min). Correo: **inicial + dos reintentos**, bases **1 y 5 min**, Retry-After mayor; solo transitorios con evidencia de rechazo sin aceptación. Permanentes e inciertos se detienen. Misma fila/evento/destinatario/Message-ID; presupuesto persistente y fencing. No exactamente una vez ni reenvío ciego. Destinatarios definitivos/resolución administrativa siguen pendientes. |
| F3-D6 | Coordinación de migraciones P-16 | Se confirmó V4 como siguiente versión local disponible al iniciar 3.2. El encargo autoriza usarla: `V4__drive_notificaciones_outbox.sql` amplía adjunto y crea notificacion juntas, sin V5 duplicada. V1–V3 intactas; V4 no aplicada. Coordinar otra vez con ramas paralelas antes de integrar. Las sesiones de recepción se difieren a 3.3, con la siguiente versión entonces libre. |
| F3-D7 | Creación país/pedido aprobada por Richard durante 3.4; retención no aprobada | La aplicación creará país (código) y pedido (número real) en Mi unidad, con IDs persistentes y sin niveles adicionales. Richard eligió expresamente este mecanismo al preguntarle en 3.4; no se adoptan carpetas existentes por nombre. Año/etapa opcionales solo tras aprobación. Mantener copia local incluso después de Drive: las acciones y la descarga de fase 2 dependen de ella. Propuesta de caducidad de sesiones incompletas: 7 días; limpieza de huérfanos con margen de 24 h y exclusión de sesiones/leases activos. Nunca borrar adjuntos aceptados por esta caducidad. |
| F3-D8 | Destinatarios y activación | Resolver admin receptor vigente y buzón remitente explícitos; propuesta de directorio de admins activos del país del pedido con una fila por destinatario, sujeto a acuerdo. No reutilizar direcciones del demo. Activación de envíos reales separada de desplegar código. |
| F3-D9 | **Provisional del equipo, aprobada por Richard para desarrollo, 2026-10-19**; código pendiente de revalidación | Cuota lógica configurable **10 GiB globales / 2 GiB por propietario / 2 cargas activas por usuario / 10 globales**. Reserva atómica PostgreSQL por UUID de adjunto reservado, recuperación de históricos sin cambiar propietario, conversión reserva→final en la misma transacción y sin doble cobro. Pausadas/fallidas conservan reserva/cupo; sin borrado o liberación automática. No se aprueban por deducción reserva 3× o margen libre; espacio físico de trabajo y liberación administrativa siguen pendientes. V7 estructura/V8 datos sin ejecutar. |

### Límite que se conserva

El manual Word y el PRD dicen **100 MB**; interpretado en decimal serían **100 000 000 bytes**. El contrato y la implementación de fase 2 usan provisionalmente **100 MiB = 104 857 600 bytes por archivo**, diferencia de **4 857 600 bytes**. No se cambia silenciosamente a 100 MB ni se declara confirmado por el cliente. Seguir usando el límite configurable vigente; interfaz y documentación deben mostrar MiB y bytes coherentes. En Spring `100MB` representa aquí la cantidad binaria que ya documenta `application.yml`; nginx recibe `ARCHIVOS_TAMANO_MAXIMO_MIB`, con margen actual de 1 MiB para el cuerpo multipart. Las sesiones tendrán límites por fragmento y por total, sin usar el margen como tamaño extra del archivo.

Los formatos siguen pendientes: PDF/Word/Excel/imágenes/ZIP son propuesta. Conservar detección técnica sobre bytes y fallback `application/octet-stream`; no activar una lista cerrada de extensiones/MIME ni rechazar desconocidos con 415 hasta que se apruebe esa política.

### Requisitos externos pendientes

- Proyecto Cloud y APIs habilitadas; JSON de cliente OAuth web externo; Gmail personal de empresa real como usuario de prueba y cuenta permitida. Mi unidad reemplaza la unidad compartida; carpeta raíz se creará solo en un paso posterior autorizado. La autorización manual no se realizó.
- Ruta del secreto suministrada fuera del repositorio y accesible solo al backend. La ruta privada se aportó y configuró en 3.1, fuera de Git; su contenido no se incorpora al plan. El montaje debe ser de solo lectura y no formar parte de imágenes ni variables `VITE_*`.
- Remitente: cuenta Gmail personal que consiente, no suplantación ni SMTP. Destinatario(s) admin siguen pendientes para 3.5. Configurar rutas externas de JSON, llave y tokens y consentimiento manual; no se solicitaron sus contenidos.
- No se utiliza delegación de dominio Workspace. El backend comprueba la cuenta autorizada mediante ID token firmado y allowlist del correo configurado. El modo En pruebas requiere reconexión dentro del límite de siete días.
- Confirmación de formatos, unidad y alcance del límite, subcarpetas por año/etapa y acceso directo. No convertir esas preguntas en decisiones aceptadas.
- Ubicación del backend accesible desde China, salida del servidor hacia Google, capacidad del volumen duradero y política de conservación. El acceso directo a enlaces Google desde China no está garantizado: conservar descarga autenticada por backend.

## Diseño e invariantes

### Datos existentes y reutilización

1. `pedido.id` y su número vigente identifican el pedido. No recalcular el número a partir de fecha, país o año; su formato sigue provisional.
2. `entrada_etapa.id` distingue reentradas; `etapa_pedido.entrada_id` señala la actual. Toda sesión nueva fija `entradaEsperadaId`, usuario, pedido y etapa. Recibir bytes y finalizar exige volver a autorizar con `PedidosParaArchivos`, siguiendo P-06/P-17/P-18.
3. `adjunto` de V3 es la tabla definitiva. Conservar `clave_local`, tamaño, SHA-256, tipo detectado, `drive_id`, `estado_copia` e `intentos`. `AdjuntoEntity` ya tiene campos de Drive/estado/intentos, pero faltan operaciones de aplicación para procesarlos.
4. `evento_adjunto` conserva relaciones `SUBIDO`/`PRESENTADO`. La copia a Drive no crea otro adjunto, otra entrada ni un segundo evento `ARCHIVO_SUBIDO` y no presenta documentos automáticamente en una transición.
5. Respetar `CHECK ((estado_copia = 'COPIADO') = (drive_id IS NOT NULL))`: un id reservado de Google **no** se escribe en `drive_id` antes de completar. Usar columna técnica separada `drive_id_reservado`.
6. Conservar `archivos-datos` montado en `/aplicacion/archivos`, `ALMACEN_LOCAL_DIR` y clave final `pedidoId/etapa/adjuntoId`. No sustituir `AlmacenLocal` por otro almacén temporal ni guardar binarios grandes en PostgreSQL.
7. Las acciones siguen aceptando un archivo íntegro local con copia `PENDIENTE`; no exigir `COPIADO` ni cambiar los requisitos del flujo. La descarga sigue disponible por backend y con permiso vigente.
8. El riesgo P-11 de acciones antiguas tras un ciclo de reentrada sigue abierto. La protección de sesiones mediante `entradaEsperadaId` no lo resuelve para todas las acciones del pedido.

### Experiencia antes y después

| Punto | Antes | Después propuesto |
|---|---|---|
| Subida | Multipart real, sin reanudación | Selección, porcentaje de bytes confirmados, pausa/reanudación y confirmación local. |
| Estado de archivo | PENDIENTE persistente sin Drive | Recibido localmente / copia pendiente / copiado / fallido; enlace solo cuando Google lo entregue. |
| Reentradas | Separación actual/anteriores | Se conserva; una sesión de una entrada antigua no se finaliza sobre la nueva. |
| Descarga | Por backend, íntegra y autorizada | Se conserva; Drive es una opción adicional, no una dependencia del navegador. |
| Admin | Vista Gmail del demo sin ruta | Registro paginado real de pendientes, enviados y fallidos y lista de copias fallidas. |

## Pasos de implementación propuestos

Cada paso describe trabajo futuro. Los bloqueos de decisiones no impiden terminar este plan; sí condicionan las tareas indicadas.

### 3.1 Configuración de Google

**Diseño actualizado por instrucción de Richard — 2026-10-17.** Usar Gmail API y Drive API con **OAuth 2.0 de usuario**, autorizado por la cuenta Gmail que la empresa controla. No usar cuenta de servicio ni delegación de dominio Workspace. Drive será **Mi unidad de esa cuenta**, no una unidad compartida organizacional. La cuenta concreta y los secretos todavía no se suministraron.

Esta actualización y su implementación autorizada se limitan a 3.1 y prevalecen, para autenticación y destino de Drive, sobre las referencias anteriores del plan a Workspace/cuenta de servicio y sobre F3-D2 pendiente. ADR-0017 registra la variante ya acordada, conservando el texto histórico de ADR-0003/0013. El PRD no cambia de estado por esta implementación y los pasos 3.2–3.5 siguen sin implementar. La aprobación de F3-D1 y su idempotencia por evento/destinatario se conserva íntegra.

#### Tipo de cliente y campos exactos en Google Cloud

Diseño elegido: **cliente OAuth de tipo Aplicación web**, confidencial, del backend. Aunque el primer consentimiento se realiza localmente, la integración pertenece a una aplicación servidor que guardará tokens para procesar colas; no es un cliente SPA ni un cliente de escritorio. Crear un cliente local separado del futuro cliente de producción.

Para la pantalla **Google Auth Platform → Clientes → Crear cliente** (también puede aparecer como Credenciales → Crear ID de cliente de OAuth):

| Campo | Valor exacto del diseño local |
|---|---|
| Tipo de aplicación | **Aplicación web** |
| Nombre | `DECOKASA backend local` |
| Orígenes autorizados de JavaScript | **Vacío**, sin entradas: no se usa un SDK OAuth en el frontend. |
| URI de redireccionamiento autorizada | `http://localhost:8080/api/v1/integraciones/google/oauth2/callback` |

La URI anterior está implementada en el código del paso 3.1, **sin compilar ni ejecutar y pendiente de parte B**. Registrar exactamente protocolo, host, puerto y ruta, sin barra final, query ni fragmento. No sustituir `localhost` por `127.0.0.1`, usar el puerto 3000 ni añadir `/api/v1` otra vez. El backend actual utiliza rutas explícitas `/api/v1` y puerto local 8080. Registrar el cliente no arranca el backend ni acredita que el callback haya sido comprobado. La implementación quedó autorizada posteriormente por Richard solo para 3.1.

En **Público / Audience**, para una cuenta Gmail personal:

| Campo | Valor |
|---|---|
| Tipo de usuario / Público | **Externo**; no Interno de Workspace. |
| Estado de publicación inicial | **En pruebas**. No publicar automáticamente. |
| Usuarios de prueba | Añadir la **dirección Gmail real de la empresa que otorgará el consentimiento**, todavía pendiente; no la dirección de cada usuario de la aplicación. |

En **Información de la marca / Branding**, si el proyecto lo solicita: nombre de aplicación `DECOKASA`; correo de asistencia y correo de contacto del desarrollador, direcciones reales controladas por la empresa/responsable. No inventar dominio, política de privacidad o correo. Si la pantalla exige esos campos adicionales, quedan pendientes de sus valores reales.

En **Acceso a los datos / Data Access**, los ámbitos previstos son:

| Ámbito | Finalidad |
|---|---|
| `https://www.googleapis.com/auth/gmail.send` | Enviar avisos como la cuenta que autoriza, sin acceso de lectura al buzón. |
| `https://www.googleapis.com/auth/drive.file` | Crear y gestionar los archivos y carpetas de la integración, sin acceso general a todo Mi unidad. |
| `openid` | Vincular y reconocer de forma segura la identidad Google que autoriza. |
| `https://www.googleapis.com/auth/userinfo.email` | Identificar el correo verificado de esa cuenta. En la solicitud OAuth puede representarse como `email`. |

El correo real debe quedar en una allowlist de configuración; `login_hint` por sí solo no obliga a elegir esa cuenta. No solicitar `mail.google.com`, `gmail.readonly`, `gmail.modify` ni `drive` completo para este alcance. Habilitar Gmail API y Google Drive API en el proyecto será una acción manual posterior autorizada, no realizada ahora.

#### Autorización inicial y acceso offline

1. Un admin vigente de DECOKASA inicia expresamente «Conectar cuenta Google de empresa» desde una futura acción protegida del backend. Es una conexión de la empresa para sus workers, no un cambio del login JWT de la aplicación ni OAuth para cada empleado.
2. El backend crea una solicitud de autorización de código con `response_type=code`, el `client_id` web, la URI exacta registrada, los ámbitos anteriores, `access_type=offline` y `prompt=consent select_account` para la conexión inicial o reconexión deliberada. Añadir `state` aleatorio de un solo uso, caducidad corta y vínculo al admin que inició el proceso; `nonce` para OpenID y PKCE S256 con verificador conservado solo en servidor. No regenerar consentimientos automáticamente en cada envío.
3. La persona abre Google en un navegador normal, elige la cuenta Gmail real y acepta los permisos. La autenticación/consentimiento inicial requiere una ubicación con acceso a Google; no se presupone que pueda realizarse desde China. El navegador solo interactúa con Google para este consentimiento administrativo: los empleados siguen enviando archivos y consultando pedidos contra el backend, nunca contra las APIs Google.
4. Google vuelve al callback local con un código de un solo uso. El backend consume el `state`, valida caducidad y vínculo de inicio, y canjea el código por HTTPS usando su secreto, el verificador PKCE y la misma URI. Validar firma, emisor, audiencia, caducidad y nonce del ID token; correo verificado y cuenta permitida; conservar `sub` como identificador estable y los ámbitos realmente concedidos. Una selección diferente o permisos parciales no habilitan silenciosamente ambos adaptadores.
5. El callback no depende de que Google lleve el JWT de la aplicación en una cabecera ni de que una cookie SameSite=Strict viaje desde Google. El inicio exige admin; el retorno solo admite una solicitud pendiente identificada por estado válido y no permite conectar o sustituir cuentas arbitrarias. No devolver tokens al frontend y redirigir después a una página local de resultado sin código/token en la URL ni recursos de terceros.
6. Guardar el refresh token de forma duradera y renovar access tokens en el backend. En una respuesta posterior que omita refresh token, conservar el anterior, no sobrescribirlo por nulo. Usar `expires_in`/caducidad recibida; no asumir tokens perpetuos. Si falta refresh token en la conexión inicial, no declarar preparado el acceso offline: solicitar reconexión manual, nunca simularla.
7. `invalid_grant`, revocación o expiración dejan la integración «requiere reconexión» y detienen intentos inútiles al proveedor. Conservar archivos locales y avisos pendientes para recuperación; la pérdida de autorización no revierte transiciones ya confirmadas ni marca avisos como enviados.

`access_type=offline` y `prompt` son parámetros del futuro flujo de autorización: **no son campos que se completen en Crear cliente**. No usar OAuth Playground, código fuera de banda, contraseñas de Gmail o copia manual de códigos como mecanismo permanente.

#### Secretos y almacenamiento fuera de Git

- Configuración propuesta para el futuro adaptador: `GOOGLE_OAUTH_CLIENTE_RUTA`, `GOOGLE_OAUTH_TOKENS_RUTA`, `GOOGLE_CUENTA_EMAIL`, `GOOGLE_DRIVE_RAIZ_ID` y flags independientes de Drive/correo. Retirar para este modo la obligación de `GOOGLE_DRIVE_UNIDAD_ID` y del JSON de cuenta de servicio. Solo rutas/valores ficticios en ejemplos versionados.
- Cliente local: propuesta de archivo de credenciales en `C:\Users\Richard\.decokasa\secretos\oauth-client-local.json`, fuera del árbol `decoWeb`. Proteger `client_secret` con ACL para el usuario del backend y administración autorizada. No imprimirlo, enviarlo por chat ni guardarlo en `.env` rastreado, imágenes Docker, frontend o logs.
- Tokens: propuesta de contenedor cifrado `C:\Users\Richard\.decokasa\google-tokens\oauth-tokens-local.enc`, con clave protegida por el sistema operativo y separada del archivo. Persistir cuenta/sub, ámbitos, refresh token y caducidad; mantener access token en memoria cuando sea suficiente. ACL restringida, escritura atómica, bloqueo para renovación concurrente y respaldo cifrado con acceso controlado. No basta con añadir un token en texto claro a `.gitignore`.
- El backend debe poder actualizar el almacén de tokens; no montarlo como secreto de solo lectura. Las credenciales de cliente sí admiten montaje de lectura. Para Docker/producción se definirá un almacén/secret manager externo y duradero con cifrado y llave apropiados al servicio; no copiar un contenedor Windows cifrado a Linux suponiendo que podrá descifrarse.
- No usar localStorage/sessionStorage del navegador para tokens Google, ni incluir códigos de autorización, tokens o URL sensibles en telemetría. Restaurar o rotar credenciales requiere intervención autorizada y no cambia la identidad de los adjuntos o eventos.

#### Limitaciones de «En pruebas»

- Solo autorizan las cuentas incluidas en Usuarios de prueba; Google limita el proyecto a 100 usuarios de prueba. En este diseño basta inicialmente la cuenta de empresa, no los empleados que usan el login propio.
- Con los ámbitos de Gmail/Drive solicitados, la autorización de una app externa En pruebas expira a los **7 días** y también su refresh token offline. Renovar el access token no elimina esa caducidad. La excepción para solo nombre/correo/perfil no aplica al solicitar Gmail/Drive.
- Es un modo temporal para preparar la conexión, no una base de funcionamiento desatendido indefinido. Anticipar reconexión manual; si vence, las colas quedan duraderas hasta recuperar la autorización, sin reenvíos ciegos.
- Pasar a «En producción» no equivale a estar verificada ni autoriza producción de DECOKASA. `gmail.send` es sensible; Google puede exigir verificación según audiencia/uso, presentar advertencia de aplicación no verificada o imponer límites. El uso limitado/personal puede tener excepciones: no garantizar su aplicación a la empresa ni afirmar que toda cuenta individual queda exenta. Producción requerirá revisar la situación concreta y un cliente/retorno HTTPS distintos del cliente local. Aun publicada, una autorización puede revocarse o caducar por otros motivos.

#### Drive con una cuenta personal y Mi unidad

- La persona que autoriza es la propietaria de los archivos creados. Una carpeta compartida de Mi unidad no es una unidad compartida Workspace y no ofrece propiedad organizacional equivalente. La continuidad depende de conservar esa cuenta, recuperación y autorización.
- Propuesta compatible con `drive.file`: la aplicación creará una carpeta `DECOKASA` en Mi unidad **después de una futura autorización y activación expresas**; guardará el id real y creará dentro país/pedido. No crearla en el callback. Año/etapa siguen pendientes; no generar carpetas nuevas por reinicio. Mantener el registro duradero de ids y la idempotencia de Drive ya prevista.
- `drive.file` no permite operar cualquier carpeta existente solo porque alguien copie su id. Una carpeta raíz preexistente exige acceso concedido a la aplicación mediante un mecanismo compatible, como Picker, o revisar scopes con aprobación; este diseño evita ampliar permisos creando la raíz con la propia aplicación. No añadir Picker al navegador de los empleados.
- Usar el id de carpeta como `parents`; eliminar la exigencia de `driveId`, corpus de unidad compartida y membresía de cuenta de servicio en este modo. El adaptador se adapta sin otro módulo ni otras tablas de adjuntos; siguen `adjunto`, ids existentes, volumen local, reanudación y límite provisional **100 MiB**, no 100 MB decimales.
- Los archivos consumen la cuota de la cuenta: una cuenta estándar incluye normalmente 15 GB compartidos entre Gmail, Drive y Fotos, salvo ampliación. Disco o cuota Google agotados son errores recuperables/operativos explícitos, nunca éxito simulado. Compartir una carpeta no concede cuota organizacional.
- No conceder acceso público `anyone` ni permisos a todos los empleados automáticamente. Los enlaces reales de Drive requieren el acceso Google correspondiente y pueden no abrir desde China; mantener descarga autorizada por backend y copia local.

#### Lectura externa y límites de este encargo

Fuentes oficiales consultadas el 2026-10-17: [cliente web, código y retorno](https://developers.google.com/identity/protocols/oauth2/web-server), [tokens y estado seguros](https://developers.google.com/identity/protocols/oauth2/resources/best-practices), [OpenID y vinculación de cuenta](https://developers.google.com/identity/openid-connect/openid-connect), [Público y En pruebas](https://support.google.com/cloud/answer/15549945), [ámbitos Gmail](https://developers.google.com/workspace/gmail/api/auth/scopes), [ámbitos Drive](https://developers.google.com/workspace/drive/api/guides/api-specific-auth), [Mi unidad y unidades compartidas](https://developers.google.com/workspace/drive/api/guides/about-sdk), [cuota personal](https://support.google.com/googleone/answer/9004014).

La autorización posterior de Richard permite implementar exclusivamente configuración y consentimiento inicial de 3.1. No se crearon clientes, carpetas, secretos o tokens ni se abrió un consentimiento; tampoco se ejecutaron pruebas, builds, subidas, envíos, commit o push. Pendientes externos: ruta del JSON (solicitada sin pedir contenido), dirección Gmail exacta, configuración privada y arranque/consentimiento manual posteriores. El callback existe en código, pero no se compiló ni se arrancó.



#### Resultado de implementación de 3.1 — parte A — 2026-10-17

- Añadidos `AutorizarCuentaGoogle` y `ConexionOAuthGoogle` con caso de uso puro en el módulo `archivos`; reutilización del puerto `ConsultarResponsables` para admin vigente. Sin módulo paralelo ni migraciones.
- Adaptador `GoogleOAuthUsuario`: inicio administrativo, estado de cinco minutos de un solo uso, cookie de vínculo, PKCE/nonce, canje exclusivamente backend y validación criptográfica del ID token/cuenta/scopes. La llamada externa solo ocurre al completar un consentimiento manual, nunca al arrancar o preparar inicio.
- Almacén externo AES-256-GCM con llave separada, permisos owner-only, rechazo de rutas internas/enlaces, bloqueo entre procesos y reemplazo atómico. Errores cerrados; ningún token o client_secret aparece en respuestas ni logs propios.
- Configuración deshabilitada por defecto en `application.yml` y `.env.example`. Callback fijo; se mantiene el compose base sin montajes OAuth. La guía documenta arranque nativo posterior y las condiciones de un montaje futuro.
- Añadidos POST `/api/v1/integraciones/google/oauth2/inicio`, GET del callback acordado y GET informativo `/resultado`. Solo callback/resultado son públicos a nivel JWT; inicio revalida admin en base. El callback redirige 303 a una URL sin código/state. La página de resultado no sirve para consultar el estado real de una cuenta.
- No se añadió SDK ni se cambió `build.gradle`: transporte JDK y validación JWT con Nimbus/Spring Security ya usados por el proyecto; beans y controladores siguen las capas existentes. Sin renovación operativa por workers en este paso.
- Guía manual y pendientes de parte B: `documentos/google-oauth-paso-3.1.md`. Se conservan F3-D1, 100 MiB, Header.tsx y las evidencias de fase 2. **Codificado, sin comprobar: no se marca fase 3 completa.**


### 3.2 Puertos y persistencia

**Acción:** extender el mismo módulo de archivos y añadir el módulo de notificaciones ya previsto. Skills aplicadas en la entrega 3.5: `ecc-hexagonal-architecture`, `ecc-api-connector-builder`.

- Mantener `AlmacenLocal` y `AdjuntoRepositorio`; mantener el contrato actual del segundo y añadir `CopiaAdjuntoRepositorio` para selección/reclamación y actualizaciones condicionales sobre la misma tabla `adjunto`, sin otro catálogo ni nuevas identidades. Añadir `AlmacenArchivos` con operaciones para iniciar/continuar/consultar copia y obtener metadatos/enlace. Implementación: `archivos/adapter/out/drive/GoogleDriveAlmacen`; las clases Google no cruzan el puerto.
- Las entradas `ObtenerEnlaceArchivo`, `ReintentarCopiasPendientes` y `ConsultarCopiasFallidas` corresponden a 3.4/3.5; no se implementan en 3.2. Reutilizarán los controladores existentes. Las sesiones de recepción añaden puertos propios dentro de `archivos`, sin otro módulo de cargas.
- V4 amplía `adjunto` con `proximo_intento_en`, `drive_id_reservado`, `drive_enlace` y lease de trabajo, y reutiliza `ultimo_error` de V3. No sobrescribe metadata inmutable ni filas de fase 2. La URI protegida y offset de sesión Drive se difieren a 3.4 junto con su recuperación; no reescribir V3.
- **Diferido a 3.3, no creado en V4:** crear `carga_archivo` como metadata técnica de sesiones: UUID, usuario, pedido, etapa, entrada esperada, nombre saneado, tipo documental, tamaño total declarado, offset confirmado, clave parcial local, estado técnico, fechas de creación/actividad/caducidad, versión/lease y `adjunto_id` nullable único al completar. No usar el `adjunto` como archivo aceptado mientras falten bytes. Los bytes siguen en el mismo volumen.
- Crear `notificacion` según modelo-datos §3.6 **conforme a F3-D1, ya aprobada técnicamente**: UUID, pedido/evento, destinatario, plantilla/datos, estado, intentos, próximo intento, error, creación/envío. Añadir asunto o derivación determinista de plantilla/datos para cumplir OpenAPI; identificador del proveedor, Message-ID, lease y marca de resultado incierto. Una fila es cola y registro; no crear `correo` y `outbox` duplicados.
- Índices por próximo intento y estado, FK al evento real e idempotencia `UNIQUE(evento_id, destinatario_normalizado)` para avisos de transición, conforme a F3-D1; la versión de plantilla no es parte de la clave. No deduplicar por pedido/etapa: el mismo pedido puede entrar varias veces en la misma etapa. Conservar snapshots de destinatario/asunto para no alterar la historia de un envío.
- **F3-D3, diseño concretado en 3.2:** conservar la firma `void` de `RegistrarEventoPedido`. `RegistrarEventoPedidoCasoDeUso` conserva/genera el UUID como antes y publica `EventoPedidoRegistrado(eventoId, eventoOriginal)` mediante un puerto compartido después de `historial.insertar`, dentro de la transacción `MANDATORY` ya abierta. La composición de producción siempre inyecta el publicador síncrono. El constructor anterior se mantiene para compatibilidad de consumidores aislados y no es el que configura Spring. El aviso es un contrato compartido separado, no implementa el sealed `EventoDePedido` y no vuelve a disparar el oyente de historial. Notificaciones escucha este aviso e inserta su fila en la misma transacción; si falla, acción e historial retroceden. Evita depender del orden entre dos oyentes del evento original, de ids nulos o de consultas heurísticas. No modificar eventos históricos ni constructores de cada transición para resolverlo.
- `EncolarNotificacion`, `Notificador`, `NotificacionRepositorio` y `DirectorioDestinatarios` se implementan en 3.2; `EnviarNotificacionesPendientes` y `ConsultarNotificaciones` quedan para 3.4/3.5. Se ubican en las capas de `notificaciones` descritas por arquitectura §4.6. Cuando necesite usuarios o datos de pedidos, consumir sus puertos de entrada o un puerto propio implementado por el proveedor, nunca repositorios de otro módulo.

**Patrones:** `TransactionTemplate` de `ArchivosConfig`; `@EventListener` y `MANDATORY` solo en adaptadores; JPA solo en `adapter/out/persistence`; registros Java en dominio/aplicación; UUID de aplicación y tiempos de `Reloj`. **Alcance concretado:** V4 contiene copia/outbox; F3-D3 está cableado sin cambiar la firma pública del historial. D4 y la persistencia de sesiones quedan para 3.3/3.4; la política de destinatarios D8 bloquea el envío, no la conservación transaccional del aviso. F3-D1 ya está aprobada; no se solicita nuevamente.

### 3.3 Carga de archivos reanudable

**Acción:** añadir reanudación de recepción sin romper multipart ni las relaciones actuales. Skills aplicadas en la entrega 3.5: `ecc-prp-implement`, `ecc-error-handling`.

**Contrato incorporado a OpenAPI 0.7.2-borrador por el encargo 3.3 (sin ejecución):**

| Operación propuesta bajo `/api/v1` | Datos y respuesta |
|---|---|
| `POST /pedidos/{pedidoId}/etapas/{numeroEtapa}/archivos/cargas` | `entradaEsperadaId`, nombre, tipoDocumento, tamanoBytes y clave idempotente de inicio. Devuelve 201, `cargaId`, offset 0, tamaño de fragmento y Location; caducidad diferida mientras siga pendiente su política. Un inicio repetido con la misma clave devuelve la misma sesión si el comando coincide. |
| `GET /archivos/cargas/{cargaId}` | Estado de sesión propia, offset durable, total y `adjuntoId` si terminó. |
| `PUT /archivos/cargas/{cargaId}/fragmentos` | Bytes, `Content-Range` y huella del fragmento; respuesta con offset confirmado. Reenvío idéntico no concatena dos veces; rango divergente devuelve conflicto. |
| `POST /archivos/cargas/{cargaId}/finalizacion` | Finalización idempotente: devuelve el mismo `Adjunto` y evento de subida cuando ya terminó; estado incompleto no produce un adjunto. |

- Preservar `POST /pedidos/{pedidoId}/etapas/{numeroEtapa}/archivos` multipart, listado, descarga y sus respuestas. Ambos caminos comparten saneamiento, límite y registro final; no duplicar lógica de negocio.
- Autorizar creación, fragmentos, consulta y finalización con usuario vigente, propietario de sesión y alcance de pedido/etapa/entrada; un UUID conocido no concede acceso. La clave del archivo la genera el servidor. Un admin no reanuda una sesión ajena sin una regla expresa nueva.
- Tamaño total: 1 a 104 857 600 bytes configurable. Contar bytes recibidos realmente, rechazar exceso y EOF prematuro. Fragmentos secuenciales propuestos de 4 MiB, último menor. Serializar escritura por sesión; sincronizar a disco antes de adelantar el offset de base. Tras caída, reconciliar/truncar bytes por encima del offset confirmado antes de admitir más datos.
- Completar usando SHA-256 de todo el archivo, detección técnica real y `Adjunto.limpiarNombre`; el `Content-Type` del navegador no es autoridad. El nombre jamás es ruta. No inspeccionar productos, cantidades, colores o contenido comercial.
- Reutilizar la secuencia `prevalidar → recibir fuera de transacción → registrar con pedido bloqueado → confirmar` de fase 2, adaptando recepción para usar el parcial íntegro ya guardado. La finalización reautoriza la entrada actual. Guardar `carga.adjunto_id` y marcar completada junto con `adjunto`, `ARCHIVO_SUBIDO` y su relación SUBIDO en la transacción final. La respuesta se expone después de commit.
- El objetivo final del archivo se fija a partir del `adjuntoId` reservado en la sesión. Si el proceso cae tras mover y antes de commit, el recuperador reconoce el archivo por la sesión y sus bytes, sin generar un segundo adjunto ni descartarlo como huérfano activo.
- Progreso de recepción y progreso de copia Drive son distintos. La UI no muestra “copiado” al recibir 201 local. Al recargar, recuperar sesión y offset; por las restricciones del navegador la persona puede necesitar volver a seleccionar el archivo, comprobando coincidencia de tamaño y huellas de fragmentos, sin guardar todo el archivo en localStorage.
- Errores con RFC 9457 y `codigo` mediante `ManejadorErrores`: conservar 401/403/404, 409 `ETAPA_CAMBIO` para entrada inválida y 413 `ARCHIVO_DEMASIADO_GRANDE`. Proponer en OpenAPI códigos específicos de sesión caducada/rango incompatible; no reutilizar un código con significado distinto. La finalización incompleta devuelve error explícito sin metadatos aceptados.

**Archivos previstos:** puertos/casos de uso y adaptador de sesiones en `archivos`; extensión de `AlmacenLocalEnDisco`; `SubirArchivoCasoDeUso` y `ArchivosConfig`; controladores actuales/nuevo controlador de sesiones; `CodigoError`/manejador solo si el contrato incorpora códigos nuevos; OpenAPI y cliente frontend. **Concreción 3.3:** V5 añade sesiones y bloques de metadatos sin modificar V4; 4 MiB por bloque, bloqueo de archivo compartido y offset condicional. La retención propuesta se difiere; las carpetas reales de Mi unidad bloquean la copia posterior, no esta recepción local.

### 3.4 Cola y recuperación

**Acción:** incorporar procesamiento duradero e idempotencia, sin red dentro de transacciones de negocio. Skills aplicadas en la entrega 3.5: `ecc-prp-implement`, `ecc-error-handling`.

- Copias: usar `adjunto.estado_copia` e `intentos` como cola existente; no crear otra colección de archivos pendientes. Reclamar lotes por `FOR UPDATE SKIP LOCKED`, fijar lease/token y próximo intento en transacción corta, salir de ella para Google y terminar con actualización condicional por token. Estado público sigue `PENDIENTE/COPIADO/FALLIDO`; “en proceso” se expresa con lease técnico.
- El worker de correo se difiere a 3.5 por el alcance expreso de este encargo; cuando se implemente usará la estrategia de `notificacion`; los leases caducados se recuperan tras reinicio y una respuesta tardía no pisa el resultado de otro worker. Límite de concurrencia, timeouts, intervalo y lease configurables; duración/renovación coherente con llamadas y transferencias.
- Aplicar F3-D5: los fallos reintentables quedan pendientes con siguiente fecha hasta agotar tres reintentos; entonces FALLIDO/FALLIDA terminal. Incrementar intentos por ejecución de entrega real, no por cada fragmento, sondeo o tick del programador; URI y offset de Drive permiten continuar el mismo intento interrumpido. Cuotas/429/5xx usan espera creciente y Retry-After; credencial o permiso inválido es fallo de configuración/permanente, sin ciclo de reenvío ciego. SDK y worker comparten un presupuesto para no multiplicar reintentos invisibles.
- Drive: reservar y persistir un id remoto antes de crear, asociar metadata de `adjuntoId`/pedido/entrada y resolver la carpeta sin duplicaciones mediante clave país/pedido y reclamación persistente. Si hace falta, añadir índice técnico `carpeta_drive`, sin convertirlo en un segundo módulo ni renumerar pedidos.
- Usar subida reanudable también **backend→Drive**, independiente de la sesión navegador→backend: URI protegida y offset remotos persistidos; consultar estado antes de reenviar; seguir Range de 308; fragmentos múltiplos de 256 KiB salvo el último. Si caduca la sesión, reconciliar el id reservado y reanudar/recrear la sesión sin crear un archivo distinto.
- Un timeout o 409 de creación no demuestra éxito ni fracaso: consultar el id reservado, tamaño, ubicación y metadata antes de confirmar. Solo guardar `drive_id` definitivo y estado COPIADO juntos al tener el archivo completo; obtener `webViewLink` real. Guardar binarios sin convertirlos a formatos Google para conservar fidelidad e idempotencia.
- `GET /archivos/{adjuntoId}/enlace` reutiliza el permiso de descarga y devuelve enlace real; pendiente/fallido no devuelve URL inventada. `GET /archivos/copias-fallidas` mantiene exclusividad admin. No añadir reintento manual HTTP sin un contrato explícito aprobado.
- Arranque: incorporar adjuntos PENDIENTE existentes de fase 2 sin cambiar ids ni generar eventos de subida. Copiar archivos locales que siguen íntegros aunque su etapa ya no sea actual. La reautorización de una subida nueva no debe cancelar la copia de un archivo ya aceptado.
- Retención: conservar parciales activos y todo archivo confirmado, pendiente o fallido. Mantener también la copia local de COPIADO para acciones/descargas actuales. No aplicar caducidad ni barrido mientras retención no esté aprobada; la limpieza de sesiones incompletas sigue propuesta y no se implementa en 3.4; el barrido compara disco, filas, sesiones y leases, con margen de antigüedad, sin borrar a partir de timestamps aislados. El disco lleno produce fallo explícito sin un adjunto ficticio.
- Registrar errores sanitizados por ids y clase, con `ultimo_error` útil y fechas de servidor. No registrar claves, tokens, URI de sesiones de Google, contenido del archivo o cuerpos completos de correo.

**Archivos previstos:** `TareaCopiaArchivos`, casos de uso de copia/recuperación y adaptador Drive en `archivos`; repositorios/mapeos existentes; tareas y repositorios de `notificaciones`; configuración de ambos módulos. **Concreción 3.4:** Drive tiene inicial+3 reintentos y creación país/pedido autorizados en el encargo; no hay decisión pendiente que bloquee el código de copia. Capacidades reales externas y parte B pendientes, sin activar servicios. Correo, pantallas y retención no aprobada quedan fuera.

### 3.5 Correo y pantallas

**Acción:** conectar avisos a transiciones y sustituir simulaciones por datos persistidos. Skills aplicadas en la entrega 3.5: `shadcn`, `tanstack-query-best-practices`; para backend, patrones de 3.2 y 3.4.

- Oyente síncrono de `EventoPedidoRegistrado` → `EncolarNotificacion` → fila por destinatario en la misma transacción. Eventos de transición: `EtapaEnviada`, `EtapaAprobada`, `EtapaRechazada` y `PedidoDevuelto`; propuesta incluir `PedidoCreado` como entrada inicial, a acordar con política de avisos. No enviar un correo por `ArchivoSubido`, observación o reasignación por deducción; requieren regla expresa. Describir en la plantilla origen/destino y motivo reales, sin textos fijos exclusivos del flujo A.
- Soporte de vencimiento: reservar plantilla y puerto/evento compartido futuro de `PlazoVencido` para fase 5. Fase 3 no implementa el detector ni finge eventos; al incorporarlo, la clave deberá distinguir entrada y vencimiento para evitar aviso en cada sondeo.
- F3-D8 sigue pendiente. Se conserva el directorio vacío y la intención transaccional con destinatario nulo de 3.2: nunca entregable, sin revertir el avance confirmado ni inventar receptores. El procesamiento se bloquea antes de reclamar/consumir intentos. La propuesta anterior de impedir transiciones por falta de destinatario no se aplica. Falta aprobar correos receptores, asociación a admin vigente, alcance por país y tratamiento explícito de intenciones históricas; no hay backfill automático.
- Elegido Gmail API: `GmailNotificador` construye MIME y envía con `users.messages.send`; un Message-ID estable deriva de la notificación y se guarda junto con id real de proveedor, fecha y resultado. No adjuntar archivos grandes al correo: usar enlaces autenticados de la web. Credencial y remitente siempre del servidor. Si se elige SMTP, sustituir solo adaptador/configuración y conservar registros/semántica.
- La unicidad de la fila evita duplicados de encolado; el lease evita ejecución paralela normal. **Message-ID no garantiza deduplicación de Gmail.** Ante timeout después de aceptación posible o caída entre aceptación y persistencia, marcar resultado incierto (FALLIDA más marca técnica y explicación) y detener el reenvío automático. La política administrativa de resolución se acuerda antes de activación; no ampliar scopes de lectura para reconciliar sin aprobación.
- No enviar automáticamente correos de eventos históricos de fase 2 al habilitar la integración. Una puesta al día sería un encargo explícito; esto es distinto de recuperar copias pendientes ya existentes.
- Implementar `GET /notificaciones` según OpenAPI: estado, paginación, destinatario, asunto, creación/envío, intentos y error; autorización admin vigente en servidor y `SoloAdmin` en UI. Completar rutas `/archivos/{adjuntoId}/enlace` y `/archivos/copias-fallidas` sin alterar sus esquemas salvo extensión acordada.
- Extender `frontend/src/api/pedidos.ts` y `components/pedidos/ArchivosEtapa.tsx`: sesiones/progreso real, reanudar, errores, invalidación de consultas por pedido/etapa y consulta acotada de estado mientras haya pendientes. Conservar separación de entradas y descarga. No reintentar mutaciones a ciegas al recuperar red; consultar sesión/resultado idempotente.
- Reemplazar datos en memoria de `GmailNotificationsView.tsx` por pantalla conectada, o sustituirla por un componente nuevo retirando su uso anterior. Crear `api/notificaciones.ts` con Zod y claves de TanStack Query; nueva ruta propuesta `/notificaciones` bajo `SoloAdmin`, menú admin en `Sidebar.tsx`; registro de copias fallidas admin en ruta o panel propio con contrato existente.
- UI con componentes shadcn/tokens de DESIGN.md, foco y teclado, progreso etiquetado, estado no dependiente solo de color, errores junto a la acción y enlace Drive solo tras resultado real. Mensajes distinguen “recibido localmente”, “pendiente de copia” y “enviado al proveedor”; no confundir aceptación de Gmail con recepción en la bandeja final.
- `Header.tsx` no necesita cambios para estas rutas; conservar la modificación previa del usuario. No reactivar pantallas del demo ni enlaces ficticios.

**Dependencias:** 3.2–3.4, F3-D1 y F3-D2 acordadas, decisiones pendientes F3-D5/D8 y remitente/destinatarios externos. No se habilitan envíos reales por el mero hecho de implementar este paso.

## Patrones concretos y mapa de cambios futuros

Paquete base backend: `backend/src/main/java/com/decokasa/seguimiento/`.

| Archivo o ubicación | Acción futura y patrón existente |
|---|---|
| `archivos/application/port/{in,out}/`, `application/usecase/`, `domain/` | Ampliar puertos/casos de uso actuales; comandos y resultados Java puros. Mantener `SubirArchivo`, `ListarAdjuntosDeEtapa` y `DescargarArchivo`. |
| `archivos/adapter/out/almacen/AlmacenLocalEnDisco.java` | Ampliar recepción parcial sobre mismo volumen. Mantener escritura por streaming, `force(true)`, saneamiento y confirmación atómica; una carga incompleta no es un adjunto. |
| `archivos/adapter/out/persistence/{AdjuntoEntity,AdjuntoDatos,AdjuntoJpaRepositorio}.java` | Ampliar cola en la misma tabla con actualizaciones condicionales y entidades JPA fuera del núcleo. |
| `archivos/config/ArchivosConfig.java` | Extender decoradores/beans con transacciones cortas; patrón actual `tx.executeWithoutResult(estado -> caso.prevalidar(...))`, bytes fuera y registro dentro. |
| `archivos/adapter/in/web/{ArchivoController,SubidaArchivoController}.java` | Mantener rutas actuales y añadir control de sesiones mediante puertos; autorización en aplicación. |
| `archivos/adapter/out/drive/`, `archivos/adapter/in/tareas/` | Crear adaptador Google y tareas de copia/barrido, sujetos a decisiones. |
| `historial/application/port/in/RegistrarEventoPedido.java`, `application/usecase/RegistrarEventoPedidoCasoDeUso.java`, `adapter/in/eventos/OyenteEventosPedido.java` | Puente propuesto del UUID persistido, sin tocar eventos históricos. Patrón real actual: `UUID id = evento.eventoId() == null ? UUID.randomUUID() : evento.eventoId()`. |
| `compartido/domain/eventos/EventoPedidoRegistrado.java` | Nuevo aviso público propuesto; no añadir Google, Spring ni repositorios de otro módulo. |
| `notificaciones/{domain,application,adapter,config}/` | Crear el módulo previsto, sus puertos y adaptador de canal elegido; ninguna dependencia a dominios internos de pedidos/historial. |
| `compartido/adapter/in/web/ManejadorErrores.java` | Mantener RFC 9457 `application/problem+json`, `codigo` estable y log SLF4J sanitizado; nuevos errores solo tras extender contrato. |
| `backend/src/main/resources/db/migration/` | Solo migraciones nuevas, después de coordinación P-16. Sin recrear `adjunto`, `entrada_etapa` o `evento_adjunto`. |
| `documentos/api/openapi.yaml`, `modelo-datos.md`, `arquitectura.md`, ADR afectados | Reflejar extensiones y decisiones cuando se aprueben. No marcar propuestas aceptadas por estar descritas en este plan. |
| `frontend/src/api/{pedidos,notificaciones}.ts`, `components/pedidos/ArchivosEtapa.tsx` | Esquemas Zod y hooks reales del cliente existente; recepción idempotente con progreso observable. |
| `frontend/src/components/GmailNotificationsView.tsx`, pantalla de copias fallidas, `rutas.tsx`, `Sidebar.tsx` | Sustituir demo y añadir rutas admin; reutilizar `SoloAdmin` y componentes UI disponibles. |
| `backend/build.gradle`, `application.yml`, `.env.example`, `docker-compose.yml`, `frontend/limite-archivos.envsh` | Dependencias/configuración coordinadas, sin claves ni reducción accidental del límite vigente. |

## Riesgos, bloqueos y orden recomendado

| Riesgo o bloqueo | Tratamiento propuesto |
|---|---|
| Configuración privada/consentimiento pendientes | F3-D1 aprobó el outbox y F3-D2 quedó acordada como OAuth de usuario/Gmail API el 2026-10-17. Las rutas/cuenta se configuraron en 3.1; falta autorizar manualmente; los adaptadores pasivos están codificados en 3.2, sin envío/subida operativa ni autorización real. |
| Identidad de evento nula y orden de oyentes | F3-D3 enlaza el UUID ya insertado y mantiene la transacción; no otra tabla histórica. |
| Cuenta, unidad, permisos y buzón no suministrados | Implementación local/configurable puede separarse; adaptadores reales permanecen sin activación externa. |
| Doble entrega de correo ante resultado incierto | Registro durable, lease, Message-ID estable y resolución explícita; no garantía de exactamente una vez. |
| Caída con bytes en disco antes del commit | Sesiones, offsets y objetivos estables; recuperación antes del barrido y sin borrar adjuntos aceptados. |
| Drive bloqueado para navegador en China | Backend realiza todo tráfico Google; descarga autenticada local se conserva. Enlace directo es opcional y no garantiza accesibilidad. |
| P-16 y migraciones de ramas paralelas | Números candidatos, sin apropiarse de reservas ni editar migraciones existentes. |
| P-11 y preguntas de cliente conservadas | No resolverlas implícitamente ni condicionar su cierre a este plan. |

Orden: **F3-D1 y F3-D2 acordadas; entregar 3.1 y reservar su comprobación a parte B** → acordar remitente y F3-D3/D4/D5/D6/D8 → implementar configuración y persistencia → recepción reanudable → copia y recuperación → correo/pantallas. La decisión sobre año/etapa no bloquea la estructura base país/pedido; la falta de permisos o buzón sí bloquea su activación real. La implementación y el cierre de fase requieren encargos posteriores; este plan no los ejecuta ni los declara terminados.

## Documentación externa consultada

Fuentes oficiales consultadas el 2026-10-16 para fundamentar propuestas, sin configurar servicios. Los protocolos descritos no equivalen a disponer de permisos o credenciales del cliente.

| Fuente | Aplicación y límite |
|---|---|
| [Drive — carga de archivos](https://developers.google.com/workspace/drive/api/guides/manage-uploads) | Reanudación, offsets y sesión persistida; caducidad de sesión. |
| [Drive — soporte de unidades compartidas](https://developers.google.com/workspace/drive/api/guides/enable-shareddrives) | Adaptar operaciones y búsquedas a la unidad con `supportsAllDrives`, `driveId`/corpus cuando corresponda. |
| [Drive — ids pregenerados](https://developers.google.com/workspace/drive/api/guides/manage-uploads#use_a_pre-generated_id_to_upload_files) | Un id reservado permite repetir una creación sin otro archivo; reconciliar respuesta incierta y 409. No convertir el binario a documento Google. |
| [Gmail — users.messages.send](https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/send) | Canal propuesto de envío y scope `gmail.send`; no documenta una clave de idempotencia de aplicación ni recepción final garantizada. |
| [Gmail — creación y envío de mensajes](https://developers.google.com/workspace/gmail/api/guides/sending) | Construcción MIME y representación del mensaje en el adaptador. |
| [Google — cuenta de servicio y delegación](https://developers.google.com/identity/protocols/oauth2/service-account#delegatingauthority) | Requiere autorización administrativa, scopes y usuario delegado; no confundir fijar el remitente en código con restricción administrativa al buzón. |

Las dependencias Java concretas y sus versiones se fijarán al ejecutar el paso autorizado de integración; este plan no instala SDKs ni cambia el stack.

## Resultado de implementación — paso 3.2, parte A (2026-10-17)

Alcance autorizado: puertos/adaptadores y persistencia. Código entregado sin compilar, ejecutar pruebas, aplicar migraciones o realizar llamadas Google. **No equivale al cierre de la parte B ni de la fase.** Detalle, límites y casos reservados: [informe del 3.2](../../../documentos/fase-3-paso-3.2.md).

- `archivos` conserva `AlmacenLocal`, `AdjuntoRepositorio`, ids y contratos HTTP de fase 2. `CopiaAdjuntoRepositorio` usa la MISMA tabla `adjunto`; V4 añade id Drive reservado, enlace definitivo, fecha de próximo intento y lease. La confirmación condicional guarda COPIADO/id definitivo/enlace juntos y no borra el archivo local.
- `AlmacenArchivos` y `GoogleDriveAlmacen`: reserva de id, inicio de sesión remota, consulta de offset, transferencia de un fragmento y consulta de metadatos. Son primitivas pasivas; no hay reanudación de navegador, worker, búsqueda/creación de carpetas ni persistencia de URI secreta en texto plano. El llamador futuro debe comprobar UUID/tamaño/ubicación antes de confirmar una copia.
- `Notificador` y `GmailNotificador`: MIME UTF-8, Message-ID estable de la fila, `users/me/messages/send`, identificador del proveedor y errores sanitizados. No hay plantilla operativa, tarea de envío, reintento automático ni pantalla de 3.5.
- `notificacion` es cola y registro, sin segunda outbox: FK diferible al evento persistido por JPA, clave única evento/destinatario normalizado, datos JSONB, estados, intentos, leases y resultado incierto. La FK se comprueba al commit compartido aunque JDBC inserte antes del flush JPA.
- El aviso compartido se publica síncronamente después del registro del historial; el listener MANDATORY solo inserta. Fallo o rollback elimina también el aviso. Ningún proveedor participa en esta transacción; ambos adaptadores rechazan una transacción activa.
- **Diseño técnico concretado, no aprobación nueva del cliente:** mientras F3-D8 siga pendiente, se conserva una intención con destinatario NULL (clave técnica vacía), excluida de reclamación. El directorio no inventa receptores. La resolución futura debe crear/conservar una fila por receptor real y la misma clave evento/correo; no ofrece endpoint en este paso.
- La reclamación de correo marca incertidumbre antes de salir a red: si el proceso cae, esa fila no vuelve a reclamarse automáticamente. Solo una respuesta explícita permite liberar/reprogramar por el mismo token vigente. La política de recuperación administrativa sigue para 3.4; Message-ID no garantiza exactamente una vez.
- La credencial backend reutiliza el almacén cifrado 3.1 y renueva access tokens bajo bloqueo de archivo solo por demanda, fuera de transacción. No extiende el grant de siete días del modo En pruebas. Credenciales/tokens/llave siguen fuera de Git.
- OAuth, Drive y Gmail permanecen desactivados; los nuevos flags tienen valor false. No se cambió la configuración privada ni se abrió una autorización. El callback fijo y su validación opcional de redirect_uris, PKCE/state/nonce y admin vigente se conservan.

Bloqueos externos: autorización real pendiente y modo En pruebas; habilitación expresa pendiente; permisos de carpeta `drive.file`/cuota de Mi unidad; destinatarios D8 sin acordar. Pendientes técnicos 3.3–3.5: sesiones locales y remotas durables, organización por país/pedido, workers, conciliación, política de reintentos, resolución de resultado incierto, plantillas y pantallas. Continúa el límite provisional 100 MiB (104857600 bytes), distinto de los 100 MB decimales del manual.

## Resultado de implementación — paso 3.3, parte A (2026-10-17)

Encargo posterior de Richard: implementar solo carga por partes/reanudación, sin pruebas/builds/migraciones/Google ni 3.4–3.5. Se conserva este único plan activo, sin archivarlo ni declarar completa la fase o el PRD.

- Puertos `GestionarCarga`, `CargaRepositorio` y `AlmacenCargas` dentro del MISMO módulo archivos; caso de uso puro, composición de transacciones en `CargasConfig`, adaptadores de disco/SQL y cuatro rutas de `CargasController`.
- V5 es la siguiente versión local disponible: `V5__cargas_reanudables.sql`, creada y no aplicada. `carga_archivo` y `carga_fragmento` solo guardan metadatos. V1–V4 intactas; `adjunto`, entrada e historial de fase 2 se reutilizan.
- Inicio idempotente por usuario/comando, 4 MiB por bloque salvo el último, huellas/rangos/longitud real, serialización por sesión y force antes del offset confirmado. Bytes en `.cargas` del mismo volumen; recuperación a demanda de tail no confirmado tras reinicio, sin worker.
- RECIBIENDO no crea adjunto ni cuenta para requisitos, incluso con todos los bytes. Finalización reutiliza el registro de fase 2; permisos/entrada vigentes, metadatos del archivo, evento ARCHIVO_SUBIDO/SUBIDO y sesión COMPLETADA comparten transacción. UUID reservado y destino estable permiten repetir tras rollback sin otro adjunto/evento ni perder el parcial.
- Interfaz existente con progreso confirmado, pausa, reselección y comprobación del prefijo al recargar; sessionStorage guarda referencias UUID, sin bytes/tokens. Multipart/listado/descarga anteriores permanecen disponibles. Nuevos errores cerrados de carga y cabeceras CORS específicas; no una lista de formatos.
- **Concreciones técnicas y desviaciones:** sin caducidad, purga o `caducaEn` mientras no se acuerde retención; se conserva lo recibido. Bloqueo de archivo entre procesos sobre volumen común, más bloqueo de pedido/carga y offset esperado, en lugar de inventar un lease de recepción. Una referencia local identifica el archivo por sus metadatos y comprueba todos los bloques ya confirmados; no se promete conocer el contenido aún no recibido.
- Límite provisional de 100 MiB conservado y configuración menor respetada; sin interpretar contenido comercial. No se crean carpetas Drive: país/pedido sigue siendo la única estructura aprobada; año/etapa continúan pendientes. La clave local de fase 2 no se convierte en jerarquía Drive.
- OAuth/Drive/Gmail siguen desactivados. Sin worker de copia, reintentos de copia, correo o pantallas operativas de 3.4–3.5. No se modifica la configuración privada ni se realizan operaciones reales.

Retención/capacidad, despliegue con volumen común y semántica de locks/atomicidad física pendientes; carpeta real/permisos Mi unidad siguen siendo dependencia de la copia posterior. Código sin compilar ni probar y V5 sin aplicar. Detalle, decisiones y casos reservados de parte B: [informe 3.3](../../../documentos/fase-3-paso-3.3.md). Registro del workflow: [reporte PRP](../reports/fase-3-paso-3.3-report.md).

## Resultado de implementación — paso 3.4, parte A (2026-10-17)

Encargo de Richard limitado a copia/recuperación Drive. **Código sin compilar/probar ni migraciones aplicadas**, OAuth/Drive desactivados y ejecución automática false. No hay operaciones Google, correo, pantallas, commit o push; no se archiva este plan ni se cierra el PRD/fase.

- Decisión resuelta durante el encargo: Richard aprobó que la aplicación cree `código de país/número real de pedido` en Mi unidad (ejemplo EC/DK-EC-2026-0001), con reservas estables; sin año/etapa adicionales ni números nuevos. No queda otra decisión de carpetas que bloquee este código.
- `ReintentarCopiasPendientes` y caso de uso puro sobre el mismo `adjunto`. V6 `V6__recuperacion_copias_drive.sql` es la siguiente versión local disponible, sin aplicar: añade intento activo, sesión cifrada, offset/carpeta e índice técnico `carpeta_drive`. V1–V5 intactas; no duplica catálogo/cola ni guarda binarios.
- Presupuesto Drive: inicial (intentos=1), reintento 1 (2), reintento 2 (3), reintento 3 (4). Tras fallos reintentables, esperas base 1/5/15 min y Retry-After mayor; sin sleep/jitter ni quinto intento nuevo. Fallos permanentes pueden terminar antes. Reinicio/lease expirado retoman el intento activo, también el cuarto, sin incrementar de nuevo o resetear el presupuesto.
- SKIP LOCKED, tokens de lease, renovación y actualizaciones condicionales. Cerrojo del volumen compartido evita solapar red del mismo adjunto mientras un proceso anterior aún esté pausado. Cada llamada a repositorio cierra su transacción antes de Google, refresh o lectura/hash. El puerto y adaptador HTTP rechazan transacciones activas.
- Mismo ID reservado persistido antes de crear. Conciliación antes de reanudar y ante 409/timeout, incluido último intento. Sesión persistida AES-GCM con llave externa/AAD del adjunto y offset remoto autoritativo; reinicio de sesión caducada sobre el mismo ID (PATCH si hay blob incompleto propio). Nunca resolver incertidumbre con otro archivo/ID.
- Confirmación solo tras consultar identidad/propiedad/carpeta/UUID/tamaño/checksum/enlace real. Estado COPIADO, drive_id y enlace se guardan juntos; un error incierto no se declara éxito. Archivo local SHA-256 comprobado, sin interpretación comercial ni conversión Google Docs. Se conservan aceptados y parciales sin ninguna limpieza.
- La creación de carpetas tiene reservas únicas por país/pedido; peticiones concurrentes usan el mismo ID y concilian. Una carpeta confirmada eliminada/movida o un objeto con contenido divergente no se reemplaza silenciosamente. No se concede acceso público/directo.
- `COPIA_DRIVE_AUTOMATICA=false` es el nuevo opt-in. La tarea solo se registra si además OAuth y Drive son true; no hay ejecución al construir beans ni endpoint manual público. Defaults técnicos: lote 5, sondeo 30 s y lease 3 min. Contención de cerrojo se difiere 30 s sin otro intento.
- Errores persistidos cerrados, sin cuerpos, secretos o URI privadas; sesiones cifradas en base. La renovación offline conserva clasificación transitoria/cuota en vez de convertir esos fallos en autorización permanente; callback, state/PKCE/nonce/scopes y límite En pruebas siguen iguales.

Parte B y activación siguen pendientes: permisos efectivos de raíz/carpeta con drive.file, cuota, OAuth real, llave/volumen privados y semántica de locks/red/proveedor. Retención, acceso directo, rearme administrativo de FALLIDO, correo y pantallas no se inventan ni ejecutan. Informe y casos detallados: [paso 3.4](../../../documentos/fase-3-paso-3.4.md); [registro PRP](../reports/fase-3-paso-3.4-report.md).


## Entrega autorizada del paso 3.5 — 2026-10-17

Solo parte A: se implementan EnviarNotificacionesPendientes, composición de transacciones cortas y tarea opt-in, reutilizando GmailNotificador/notificacion y el encolado único de 3.2. Sin HTTP dentro de transacciones; reclamación con SKIP LOCKED/lease y marca incierta durable previa al POST. ENVIADA exige ID real y confirmación con fencing. Inciertos no se reclaman por vencer lease ni se reenvían; Message-ID no garantiza exactamente una vez. Los fallos se registran mediante causas cerradas.

No se adopta el presupuesto de Drive para correo: sin política aprobada, los fallos se detienen; reintentos/resolución administrativa quedan como decisión pendiente antes de activar. F3-D8 bloquea envíos incluso con flags activados mientras el directorio siga vacío. Remitente ya aprobado: importadora.latinoamericaworld@gmail.com mediante OAuth personal; no es destinatario por deducción. Sin reparto retroactivo de avisos pendientes.

GET /notificaciones y copias-fallidas comprueban admin activo/rol vigente en servidor. Enlace de archivo reutiliza P-17 y entrega únicamente URL persistida tras COPIADO/ID/fecha reales. UI /notificaciones SoloAdmin, registro real paginado y panel de copias fallidas; conserva carga por bloques, estado por etapa/entrada y descarga backend. Se retiran enlaces/IDs falsos del demo. OpenAPI 0.7.3-borrador. Sin segunda cola/catálogo ni nueva migración.

Todos los defaults de OAuth/Drive/Gmail/worker Drive permanecen false; se añade CORREO_AUTOMATICO=false. Sin pruebas escritas/modificadas/ejecutadas, builds, migraciones, autorizaciones ni operaciones reales, commit o push. No se cambia Header.tsx ni evidencias anteriores. No se archiva este plan ni se cierra el PRD/fase.

Entrega detallada y casos pendientes **3.B1–3.B3**: [documentos/fase-3-paso-3.5.md](../../../documentos/fase-3-paso-3.5.md). La parte A no acredita validación funcional: el cierre exige parte B y aprobación de Richard.

## Arranque del arnés 3.B3 al reutilizar PostgreSQL (parte A, 2026-10-24) — pendiente de Codex

Corregido solo en el arnés opt-in (`documentos/pruebas/arnes-b3`), sin cambios en producción ni en las pruebas existentes:
- **Espera del contenedor:** la de Testcontainers 2.0.5 esperaba dos mensajes "ready", y un volumen ya inicializado emite uno solo. Ahora usa `pg_isready` por TCP, con un máximo de 120 s.
- **Durabilidad:** se quita `fsync=off`.
- **Guarda nueva:** no se generan credenciales para un volumen existente ni se crea una base vacía si ya hubo una.

Detalle y siete casos para Codex en el [README del arnés](../../../documentos/pruebas/arnes-b3/README.md), sección del 2026-10-24.

**Lo que sigue sin hacer:**
- La prueba real de Drive y Gmail (consentimiento OAuth, copia de 104857600 bytes y un correo) sigue **pospuesta**. Requiere el consentimiento personal de Richard y la ejecuta Codex.
- Siguen abiertos B3-SEG-01, el límite físico de los temporales, la retención, los destinatarios definitivos y la política definitiva de reintentos.
- La fase 3 sigue abierta.

## Carpetas de Drive por país, pedido, rol y persona (parte A, 2026-10-24) — sin compilar ni verificar

**Decisión:** organización aprobada por Richard, registrada en el [ADR-0025](../../../documentos/adr/0025-carpetas-drive-por-rol-y-persona.md) y en `requerimientos.md` 4.7: `Mi unidad/DECOKASA/{EC|CO}/{númeroPedido}/{rol}/{persona}/`. Se hizo en la rama `fase-7-colombia-ia` porque necesita CO.

**Implementación (reutiliza la copia del paso 3.4; sin migración):**

| Archivo | Cambio |
|---|---|
| `archivos/application/port/out/AutoresDeCopias.java` (nuevo) | Puerto: autor (UUID) y rol de intervención de un adjunto |
| `archivos/adapter/out/persistence/AutoresDeCopiasSql.java` (nuevo) | Lee el evento ARCHIVO_SUBIDO del adjunto (`evento_adjunto` SUBIDO → `evento_pedido`), exigiendo el mismo pedido y el mismo autor (`subido_por`) |
| `archivos/application/usecase/CopiarPendientesCasoDeUso.java` | `resolverCarpetas` crea y comprueba los cinco niveles con `asegurarCarpeta`, que es el mecanismo existente de reserva, creación con ID reservado y consulta ante 409 o incertidumbre. El nombre de la persona lo fija la primera reserva. Hay un constructor nuevo con `AutoresDeCopias` y `ResumirUsuarios`; el de 11 argumentos se conserva, con la organización anterior, solo para las pruebas existentes |
| `archivos/config/CopiaDriveConfig.java` | Usa el constructor nuevo; `AutoresDeCopias` va en su propia transacción corta, como los demás repositorios |

**Reglas:**
- **Rol de intervención con varios roles:** se reutiliza P-06 (`PermisosEtapa.paraActuar`), que ya decidió el rol al subir y lo guardó en el evento. Es el rol responsable de la etapa si la persona lo tiene (y la etapa está libre o asignada a ella); si no, `admin`. Un cambio posterior de roles no mueve el destino.
- **Claves internas:** `d`, `d/pais/{país}`, `d/p/{pedido}`, `d/p/{pedido}/{rol}` y `d/p/{pedido}/{rol}/{usuario}`, con UUID en base64url y un máximo de 90 caracteres.
- **Nombre de la carpeta de persona:** "Nombre · últimos 8 de su UUID". Un cambio de nombre no crea otra carpeta.
- **Permisos y enlaces:** no se comparten carpetas ni se crean enlaces públicos. El archivo conserva `adjuntoId`, `pedidoId` y `entradaId` en `appProperties`; la etapa y la entrada siguen en `adjunto`.
- **Copias anteriores:** lo ya copiado no se mueve. Una copia ya empezada en la carpeta `pedido:{UUID}` confirmada termina allí.
- **Integraciones:** OAuth, Drive, Gmail, workers y schedulers siguen en `false`.

**Casos para Codex** (dobles de Drive y PostgreSQL de Testcontainers, salvo el último):
1. **Compilación y ArchUnit.** El caso de uso importa `usuarios.application.port.in.ResumirUsuarios`, igual que `SubirArchivoCasoDeUso`.
2. **Creación automática.** La primera copia de un adjunto crea en orden DECOKASA → EC → número → rol → persona. Se comprueban el padre de cada nivel, el nombre, `appProperties.carpetaClave` igual a la clave de `carpeta_drive` y `confirmada = true`. El archivo queda con `parents = [persona]`.
3. **Reutilización:**
   - otra copia de la misma persona, rol y pedido no reserva ni crea carpetas;
   - otra persona con el mismo rol crea solo su carpeta de persona;
   - la misma persona con otro rol (por ejemplo, `admin` en otra etapa) crea rol y persona.
4. **Nombres:**
   - dos usuarios con el mismo nombre tienen carpetas distintas, con sufijos distintos;
   - si se renombra al usuario entre dos copias, se usa la misma carpeta, con el nombre original.
5. **Concurrencia** (dos hilos o dos workers, adjuntos distintos del mismo pedido y la misma persona, PostgreSQL real):
   - una sola fila por clave y una sola creación remota por clave;
   - los dos archivos quedan en la misma carpeta;
   - el perdedor de la reserva usa el ID ganador.
6. **Reintentos e incertidumbre:**
   - si `crearCarpeta` hace timeout después de crearla o devuelve 409, se consulta el mismo ID y no se duplica;
   - si una carpeta confirmada desaparece, se renombra o se mueve en Drive, sale `METADATOS_DIVERGENTES`, sin crear otra.
7. **Aislamiento:**
   - un pedido EC y uno CO van a `DECOKASA/EC` y `DECOKASA/CO`;
   - dos pedidos del mismo país tienen carpetas de pedido distintas;
   - la misma persona en dos pedidos tiene dos carpetas de persona.
8. **Datos del servidor:**
   - el rol sale del evento, no de la petición: un usuario con `marketing` y `asistente_compras` que sube en una etapa de `asistente_compras` queda en esa carpeta; un admin sin el rol de la etapa queda en `admin`;
   - si el evento SUBIDO falta, es de otro usuario o es de otro pedido, la copia termina FALLIDO por `CONFIGURACION` sin crear carpetas;
   - quitarle roles a la persona después de subir no cambia el destino.
9. **Compatibilidad:**
   - una copia ya empezada con `drive_carpeta_id` igual a la carpeta `pedido:{UUID}` confirmada termina allí, sin crear la jerarquía nueva;
   - las filas COPIADO anteriores y sus enlaces no cambian;
   - `DriveRecuperacionB1IT` (constructor anterior) sigue igual;
   - `AplicacionArrancaIT` arranca con el bean nuevo.
10. **Seguridad:**
    - ninguna llamada a `permissions` ni enlaces públicos (revisión de `GoogleDriveAlmacen` y del doble);
    - los logs no contienen nombres de personas ni claves.
11. **Prueba real, posterior y solo con el consentimiento de Richard** (desde la aplicación normal, sin arnés):
    - activar OAuth y Drive solo para la prueba, autorizar la cuenta y subir un archivo ficticio desde la pantalla del pedido;
    - comprobar a la vista en Drive `DECOKASA/EC/{número}/{rol}/{persona}/archivo`, con su contenido;
    - volver a dejar las variables en `false`.

**Pruebas que pueden cambiar (no modificadas):** las que comprueben las claves `pais:`/`pedido:` en `carpeta_drive` con la configuración de la aplicación, o que el primer nivel cuelgue de `root`.

### Corrección de los bloqueos de la verificación ADR-0025 (parte A, 2026-10-24) — sin compilar ni revalidar

Bloqueos reproducidos por Codex en `documentos/pruebas/fase-3-informe.md`, sección "Verificación de carpetas Drive — ADR-0025". Solo se corrigen esos dos. No cambian las reglas de negocio, la organización `DECOKASA/país/pedido/rol/persona`, la conciliación por ID ante 409 ni las copias anteriores. No se tocaron pruebas, `Header.tsx` ni el índice Git. OAuth, Drive, Gmail, workers, schedulers e IA siguen desactivados.

**F3-ADR25-01: el tipo del evento no se comprobaba.**
- **Causa:** `AutoresDeCopiasSql` aceptaba cualquier evento enlazado como `SUBIDO` al adjunto. La base permite esa relación con otro tipo de evento, y Codex la reprodujo con un `ETAPA_ENVIADA`: la copia terminaba en COPIADO.
- **Además:** el autor se resolvía dentro de `resolverCarpetas`, **después** de `drive.reservarId()` (el ID del archivo) y de leer la raíz de Drive. Un evento incoherente ya iniciaba operaciones en Drive antes de fallar.
- **Corrección en el SQL:** se añadió `e.tipo = 'ARCHIVO_SUBIDO'` a las condiciones existentes (relación `SUBIDO`, mismo adjunto, mismo pedido, mismo autor `subido_por` y rol registrado). El rol de intervención sigue saliendo del evento, sin cambios.
- **Corrección en el caso de uso:** `CopiarPendientesCasoDeUso.Ejecucion.copiar()` resuelve el autor **al empezar**, antes de cualquier operación Drive. Si falta, sale `CONFIGURACION`, que no es reintentable: la copia queda FALLIDO sin reservar ID, sin leer la raíz, sin crear carpetas y sin conciliar (`md5` sigue nulo).
- **Constructor de compatibilidad:** no cambia (no usa autor).

**F7-COMP-01: nueve errores de compilación en `PreguntarCasoDeUso` (fase 7, previos a Drive).**
- **Causa:** la clase implementa `Preguntar`, que declara el record anidado `Preguntar.Resultado`. En Java, un tipo miembro heredado oculta el import de un solo tipo `RegistroConsultasIA.Resultado` (enum), así que `Resultado.ERROR_INCIERTO` y las demás constantes se buscaban en el record.
- **Corrección:** se quitó ese import y se califican los dos tipos explícitamente:
  - `Preguntar.Resultado` en el valor que devuelve `preguntar`;
  - `RegistroConsultasIA.Resultado` en el registro de consumo, en `resultado(Fin, boolean)` y en las constantes `ERROR_INCIERTO`, `ERROR_SIN_COBRO`, `LIMITE`, `RECHAZADA`, `INCOMPLETA`, `RESPONDIDA` y `SIN_EVIDENCIA`.
- **Qué no cambia:** la lógica. Tampoco se excluye `asistente` de la compilación.
- **Búsqueda del mismo patrón:** un script recorrió todo `src/main/java` (import de tipo anidado que coincide con un tipo anidado de la interfaz implementada) y no encontró otras colisiones.

**Casos para que Codex revalide** (sin `-PdiagnosticoSinAsistente`):
1. **Compilación completa:** `compileJava` sin errores, con `asistente` incluido. Si aparecen errores nuevos de fase 7 que la atribución anterior ocultaba, registrarlos aparte.
2. **Arquitectura y arranque:** `ReglasArquitecturaTest`, `FronterasFaseDosTest` y `AplicacionArrancaIT` con todos los módulos.
3. **Variante `otro-tipo` de `eventoIncoherenteFallaSinCrearCarpetas`:** FALLIDO por `CONFIGURACION`, ninguna fila nueva en `carpeta_drive` y **cero llamadas al doble de Drive**, incluidas `reservarId` y `metadatos("root")`. Las variantes ausente, otro autor y otro pedido siguen bloqueadas igual.
4. **Copia con `drive_id_reservado` ya puesto y evento incoherente:** sin conciliación ni consultas a Drive.
5. **Regresión:**
   - la matriz completa de `CarpetasDriveAdr25IT` y `CarpetasDriveProtocoloAdr25Test` (cinco niveles, reutilización, homónimos, renombre, P-06 con varios roles, EC/CO, 409 y timeout, divergencias, compatibilidad y concurrencia);
   - `DriveRecuperacionB1IT` y `GoogleDriveProtocoloB1Test`.
6. **Asistente, compilado de nuevo con la IA desactivada:** pruebas de `asistente` que existan. El registro de consumo guarda las mismas constantes que antes (LIMITE, ERROR_SIN_COBRO, ERROR_INCIERTO, RESPONDIDA, SIN_EVIDENCIA, INCOMPLETA, RECHAZADA). `IA_HABILITADA=false` responde 503 sin llamar al proveedor.
7. **`check` global y cobertura**, si la compilación ya lo permite.

**Revisión `ecc:java-reviewer` (solo lectura, 2026-10-24):** sin hallazgos críticos, altos ni medios en los dos archivos. Confirmó que:
- no queda ningún `Resultado` sin calificar, todas las constantes existen y el `switch` sobre `Fin` es exhaustivo;
- con el constructor nuevo no hay ninguna operación Drive antes de validar el autor;
- el constructor de compatibilidad, la jerarquía nueva y la conciliación ante 409 no cambian.

Dejó un hallazgo **bajo**, aceptado: el autor también se exige a una copia que ya había empezado en la carpeta anterior. Si alguna careciera de un evento `ARCHIVO_SUBIDO` coherente, quedaría FALLIDO en vez de terminar. Es el efecto buscado; no consta que existan copias anteriores, porque la copia real nunca se activó. Codex puede confirmarlo con `select count(*) from adjunto where drive_carpeta_id is not null and estado_copia = 'PENDIENTE'`.

**Precisión sobre la concurrencia:** el código garantiza una sola carpeta por clave, no un solo POST. Dos workers pueden enviar la misma creación con el mismo ID reservado y el segundo se concilia por 409, tal como describe el ADR-0025. No se añadió exclusión mutua entre peticiones. Si se quisiera "un solo POST", sería un requisito nuevo.

**La prueba real** desde la aplicación normal **no se inicia**: espera a que Codex apruebe la revalidación y al consentimiento personal de Richard.

## Entorno local exclusivo para 3.B en el PC nuevo (parte A, 2026-10-24) — preparación, no validación

Por encargo de `documentos/preparar-entorno-3b-claude.md`. Detalle, comandos y ubicaciones privadas en [documentos/entorno-b3/README.md](../../../documentos/entorno-b3/README.md); script revisable en `documentos/entorno-b3/entorno-b3.ps1`, sin secretos.

- **Hechos comprobados:** PostgreSQL 17.11 en un contenedor y un volumen nuevos (`decokasa-b3-20261024-371bdc`), publicado solo en `127.0.0.1`. Flyway del backend normal aplicó V1–V22 sobre la base vacía. Hay 2 usuarios ficticios (`admin` y `asistente_compras`) con EC explícito, cargados con la semilla normal y después desactivada. Sin pedidos, adjuntos, notificaciones ni cargas. Almacenamiento propio vacío. Backend en `127.0.0.1:8080` y Vite en `127.0.0.1:3000`, con salud y login correctos. Las rutas de OAuth responden 404 porque el controlador no se registra. Ciclo detener/arrancar probado dos veces, con los datos conservados.
- **Configuración:** reutiliza las rutas del cliente OAuth, la llave y los tokens de `backend/.env.local`, sin regenerarlas. El backend no importa `.env.local`. OAuth, Drive, Gmail, copia y correo automáticos, vencimientos automáticos, IA, documentos reales de IA y borrado automático de IA quedan en `false` en cada arranque. Callback sin cambios. No hay tokens.
- **Scripts de `documentos/.tmp`:** revisados y sin cambios; `ensayo-google-local.ps1` queda reemplazado por el nuevo script, que corrige el puntero dentro del repositorio, la mezcla con `.env.local`, el puerto que cambia en cada arranque, la carrera de la semilla y el exceso de roles.
- **No hecho:** pruebas de la parte B, modos OAuth/Drive/Correo, archivo de 104857600 bytes, consentimiento y ensayo real. B3-SEG-01 sigue abierto. La fase 3 sigue abierta.

## Formatos cerrados y una carpeta de Drive por usuario (parte A, 2026-10-24) — ADR-0026, sin compilar ni verificar

Requisitos aprobados por Richard. La decisión se registró antes del código ([ADR-0026](../../../documentos/adr/0026-carpetas-drive-por-usuario-y-formatos.md); ADR-0025 queda superseded para las copias nuevas; `requerimientos.md` §4.7 y §5). Sin pruebas, builds ni llamadas reales; Google, Gmail, IA y procesamientos siguen apagados.

- **Formatos:** `FormatoArchivo` (PDF, XLSX, XLS, CSV, JPEG y PNG) comprueba la extensión y el `Content-Type` declarado antes de reservar cuota o recibir bytes (subida simple e inicio de carga). `VerificadorFormatoEnDisco` comprueba el contenido sobre el archivo completo: firma; OLE para XLS; ZIP con `[Content_Types].xml` de libro para XLSX (u OLE si está cifrado); texto sin NUL o UTF-16 con BOM para CSV. El rechazo es 415 `TIPO_ARCHIVO_NO_ACEPTADO`. `tipo_detectado` guarda el MIME canónico. El límite sigue en 100 MiB.
- **Organización:** `CopiarPendientesCasoDeUso` (constructor de 14 argumentos, el de la aplicación) copia a `DECOKASA/Usuarios/{usuario · id8}/{EC|CO}/{AAAA}/{MM}/{AAAA-MM-DD}_{número}/`, con el autor del evento `ARCHIVO_SUBIDO` y la fecha `pedido.creado_en` en la zona del país (`CodigoPais.zona()`). La lógica de carpetas pasa a `CarpetasDrive`, sin cambios en reserva, creación ni verificación. Los constructores de 11 y 13 argumentos conservan las organizaciones del paso 3.4 y de ADR-0025 para sus pruebas, y una copia ya empezada en esas carpetas termina allí.
- **Aprovisionamiento:** V23 `carpeta_usuario_drive`. `CrearUsuarioCasoDeUso` avisa del alta (`AvisoAltaUsuario` → `UsuarioCreado`), y `OyenteAltaUsuario` registra la intención en la misma transacción. `AprovisionarCarpetasUsuariosCasoDeUso` la procesa desde `TareaCopiaArchivos` (mismos flags, lote, lease y reintentos que la copia). La semilla no avisa del alta. Rutas del admin: `GET /usuarios/carpetas-drive`, y `GET` y `POST /usuarios/{id}/carpeta-drive`. En el frontend, una columna "Carpeta de Drive" en Usuarios, con Programar o Reintentar.
- **Pendiente:** la parte B de Codex. Una reserva de cuota de una subida simple rechazada por contenido queda retenida, como las demás recepciones fallidas (no existe política aprobada de liberación). Los fixtures y las pruebas de ADR-0025 y de formatos antiguos (TXT, ZIP, Word, GIF) deben adaptarse. `MigracionesIT` debe incluir V23.
