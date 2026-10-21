# Fase 3 — informe de pruebas

> **Estado vigente — 2026-10-19: revalidación local APROBADA LOCALMENTE.** Check oficial del backend: salida 0; 668 casos, 0 fallos, 0 errores y 0 omitidos. Cobertura de líneas global **90,64 % (4302/4746)** y del módulo archivos **86,34 % (1391/1611)**, sin exclusiones nuevas ni reducción del 80 %. Producción intacta. Cuota lógica validada; espacio físico temporal sigue limitado por capacidad operativa, no por esta cuota. OAuth/workers/schedulers desactivados. Google real 3.B3 pospuesto por decisión de Richard y fase 3 abierta. Véase la revalidación al final; los estados siguientes son históricos.

> **Estado vigente — 2026-10-19, posterior a la cancelación:** Richard aprobó provisionalmente para desarrollo la política de correo inicial + dos reintentos (1/5 minutos, Retry-After mayor, solo rechazo transitorio confirmado) y cuota lógica 10 GiB globales / 2 GiB por usuario / dos cargas activas personales / diez globales. Implementadas en código, **pendientes de revalidación**, sin modificar pruebas ni ejecutar pruebas/builds/migraciones. Detalle y casos en [implementación de pendientes locales](fase-3-pendientes-locales-implementacion.md) y [decisiones](fase-3-decisiones-locales.md). Google real 3.B3 sigue pospuesto por decisión del usuario; OAuth/workers/schedulers desactivados y fase abierta. Cobertura de archivos 64,83 % es histórica, no una medición de estos cambios. Las notas siguientes conservan su historia.

> **Estado vigente — 2026-10-19: integración real Google POSPUESTA por decisión expresa de Richard.** Ensayo detenido, sin recursos activos del arnés, con archivo y evidencias conservados. OAuth/copia/correo/schedulers desactivados. No se cierra 3.B3 ni la fase 3. Reintentos de correo y cuota agregada se presentan juntos en [decisiones locales pendientes](fase-3-decisiones-locales.md); no se implementan por esta propuesta. Cobertura de archivos pendiente: 64,83 %. Las notas siguientes son históricas.

> Ejecución autorizada de 3.B3, 2026-10-19: archivo ficticio preparado (104857600 bytes); **entorno de ensayo detenido antes de crear base o tokens por conflicto con backend ajeno en 8080**. Se solicita autorización específica para detener ese backend; no se cambia el callback ni se usa su base. Sin autorización Google, copia o correo realizados. Véase el registro de ejecución al final. La fase sigue abierta.

> Actualización más reciente, 2026-10-19: **receptor aprobado exclusivamente para 3.B3 y arnés test-only preparado**. Compilación y ocho guardas offline aprobadas; no se arrancaron Spring/Docker ni se autorizaron cuentas, copiaron archivos o enviaron correos. Véase «Arnés aislado de 3.B3» y [su README](arnes-b3/README.md). Reintentos de correo, cuota agregada y cobertura de archivos siguen pendientes. Las notas anteriores conservan su historia; no se cierra la fase.

> Estado más reciente, 2026-10-19: **3.B3 preparado, sin ejecución real**. Revisión local y procedimiento en la sección «Preparación de 3.B3» y en [el procedimiento aislado](fase-3-B3-procedimiento.md). OAuth/workers siguen desactivados; faltan aprobación del receptor y arnés de invocación única. Política de reintentos de correo y cobertura de archivos permanecen pendientes. La fase sigue abierta.

> Estado más reciente, 2026-10-19: **3.B2 automatizado aprobado con dobles**: frontend lint/test/build correctos, 228 pruebas Vitest, 4 Playwright y 17 casos backend específicos más 7 reglas de arquitectura. Véase «3.B2 — Correo y pantallas». Destinatarios y política de reintentos de correo siguen pendientes; archivos conserva 64,83 % de cobertura histórica B1. No se inició 3.B3 ni se cierra la fase. Los registros siguientes de B1 mantienen su historia.

> Revalidación posterior, 2026-10-17: **B1-P01 y B1-P02 cerrados y revalidados**. Véase la sección final con los comandos, resultados y cobertura actuales. Las secciones anteriores conservan la historia de las ejecuciones previas.

> Actualización posterior, 2026-10-17: B1-P01 y B1-P02 tienen una corrección de código aplicada, **pendiente de revalidación**. Los resultados y evidencias de B1 que siguen son históricos anteriores a esa corrección; no se han vuelto a ejecutar. Véase el registro al final del informe.

## 3.B1: archivos y recuperación — 2026-10-17

**Resultado: 39 pruebas aprobadas y 1 fallida en el arnés aislado de archivos; `test` y `check` completos bloqueados por compilación. 3.B1 no queda aprobado y la fase sigue abierta.** No se avanzó a 3.B2 ni se modificó producción para corregir los defectos.

Rama: `fase-3-drive-gmail`. Se leyeron las skills `ecc-tdd-workflow` y `ecc-springboot-tdd`, el manual Word vigente, el plan activo y los informes de 3.1–3.5. Se aplicaron las pautas de revisión Java a las pruebas. La instrucción de Richard prevalece sobre los pasos de implementación y commits de las skills: se conservan reproductores rojos, sin corregir producción, debilitar aserciones, desactivar casos ni crear commits. No se presenta un ciclo RED/GREEN de producción: no se autorizó esa corrección.

Fuentes: [manual](../manual-paso-a-paso.docx), [plan](../../.claude/PRPs/plans/fase-3-drive-gmail.plan.md), [3.1](../google-oauth-paso-3.1.md), [3.2](../fase-3-paso-3.2.md), [3.3](../fase-3-paso-3.3.md), [3.4](../fase-3-paso-3.4.md), [3.5](../fase-3-paso-3.5.md).

### Alcance, aislamiento y conservación

- Se amplió `AlmacenLocalRealTest`, conservando sus tres casos anteriores de 100 MiB exactos, exceso de un byte y rutas fuera del directorio. Se añadieron cuatro suites específicas de B1 y el fixture `DatosB1`; no se copió otra suite de fase 2.
- PostgreSQL **17 mediante Testcontainers**, con contenedores efímeros propios, puertos asignados por Docker y sin reuse. Migraciones y Drive usan un esquema UUID exclusivo por caso dentro del contenedor de su clase. Cargas usa el contenedor propio de su contexto y datos/UUID exclusivos por caso. No se usaron PostgreSQL local de desarrollo, bases de empresa, Docker Compose o volúmenes compartidos.
- Disco aislado con `@TempDir` para almacenamiento, migraciones y Drive; cargas usa un directorio temporal exclusivo creado con `Files.createTempDirectory("decokasa-b1-cargas-")`, con rutas por UUID. Ningún caso accede al volumen real de archivos. Los parciales corruptos y pendientes se comprueban antes de terminar el caso; no se añade retención a producción.
- `DriveFalso` implementa el puerto `AlmacenArchivos`: simula respuestas, aceptación incierta, caída tras ACK y sesión caducada. No hay HTTP a Google. Su comprobación `fueraTx` exige ausencia de transacción de base durante cada operación remota simulada. El cifrado de sesión se sustituye por un doble técnico, sin leer llaves/tokens reales; su seguridad pertenece a 3.B3.
- `ClockB1` controla el tiempo: avanzar 1, 5, 15 o 20 minutos es cambiar un `Instant`. Las barreras/latches tienen límites cortos de seguridad, no esperas de minutos para simular reintentos.
- OAuth, Drive, Gmail, worker Drive y correo automático se fuerzan a `false` en las ejecuciones y en el contexto de cargas. No se autorizó ninguna cuenta ni se hicieron subidas/envíos reales. No se leyó el JSON OAuth ni se cambiaron credenciales, tokens o `.env.local`.
- Se comprobó por huella la conservación de **1291 archivos previos ajenos a las seis rutas de pruebas**, incluidos producción, configuración, plan, `Header.tsx` y evidencias anteriores; cambios ajenos detectados: **0**. Índice Git intacto y sin archivos preparados. No hubo commit, push, borrado de volúmenes ni limpieza de bases ajenas.

Entorno observado: Windows/PowerShell, Java 21, wrapper Gradle 9.7.1, Spring Boot 4.1.1, Docker Server 29.8.2. No se sustituyeron versiones o dependencias del backend.

### Bloqueo del backend completo y arnés suplementario

La primera ejecución oficial de `test` falló en `compileJava`, antes de ejecutar pruebas, por B1-P01. Después se ejecutó también **el `check` completo solicitado**, sin exclusiones: volvió a fallar por el mismo defecto.

Para obtener evidencia de archivos sin modificar producción se guardó [archivos-aislados.init.gradle](fase-3-evidencias/archivos-aislados.init.gradle). Este script añade tareas suplementarias y compila las fuentes originales excluyendo únicamente el módulo `notificaciones`, que contiene el bloqueo. Ejecuta las suites de `archivos` y la configuración Testcontainers existente. No transforma fuentes de producción y no cambia las tareas oficiales `test`, `check` o su umbral JaCoCo.

**Sus resultados no equivalen a una aprobación del backend completo.** No comprueban la convivencia con el módulo de correo, su composición o el outbox; tampoco sustituyen las suites completas de fase 2, arquitectura o 3.B2/3.B3. Tras corregir B1-P01 en un encargo posterior habrá que ejecutar las tareas oficiales de nuevo.

### Comandos y resultados conservados

Directorio de ejecución: `backend/`. Antes de las ejecuciones de pruebas/check se fijaron:

```powershell
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$env:SPRING_CONFIG_IMPORT=''
```

Cada comando se ejecutó con `--console=plain --no-daemon`, guardando stdout/stderr mediante `Tee-Object` y conservando el código de salida de Gradle. Los avisos `NativeCommandError` de PowerShell corresponden al stderr nativo capturado; el resultado se toma del código de salida y de los XML.

| Ejecución | Comando Gradle | Resultado y evidencia |
|---|---|---|
| 01 | `.\gradlew.bat test --console=plain --no-daemon` | Salida 1: `compileJava FAILED`, dos errores de constructor. No ejecutó pruebas. [Log](fase-3-evidencias/01-baseline-test.log). |
| 02 | `.\gradlew.bat -I ..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle archivosB1Aislados --console=plain --no-daemon` | 36 casos, 16 fallos. Hubo errores de fixture y de aserción; no constituyen RED de producción. [Log](fase-3-evidencias/02-archivos-aislados.log), [XML archivados](fase-3-evidencias/ejecucion-02/). |
| 03 | Mismo comando aislado, con fixtures corregidos y tres casos añadidos | 39 casos, 38 aprobados, 1 fallo de producción: B1-P02. [Log](fase-3-evidencias/03-archivos-aislados.log), [XML](fase-3-evidencias/ejecucion-03/). |
| 04 | `.\gradlew.bat check --console=plain --no-daemon` | Salida 1: bloqueo B1-P01. El check oficial no quedó verde. [Log](fase-3-evidencias/04-backend-check.log). |
| 05 | `.\gradlew.bat -I ..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle archivosB1Cobertura -x archivosB1Aislados --console=plain --no-daemon` | Salida 0: genera cobertura de la ejecución 03 sin repetir pruebas. [Log](fase-3-evidencias/05-cobertura-aislada.log). Sustituida como referencia final por 08. |
| 06 | Comando aislado con caso adicional de reinicio y SHA-256 completo | Fallo de compilación **de la prueba**: intentaba llamar directamente a un método de configuración no público. Corregido solo el montaje de prueba usando un contexto Spring hijo. No es RED de producción. [Log](fase-3-evidencias/06-archivos-aislados-final.log). |
| 07 | Comando aislado final | **40 casos ejecutados, 39 aprobados, 1 fallido, 0 omitidos**, salida 1. Conserva B1-P02 en rojo. [Log](fase-3-evidencias/07-archivos-aislados-final.log), [XML finales](fase-3-evidencias/ejecucion-07/). |
| 08 | Comando de cobertura de 05, sobre la ejecución 07 | Salida 0: informe JaCoCo parcial final. No ejecuta de nuevo las pruebas ni convierte su resultado en verde. [Log](fase-3-evidencias/08-cobertura-final.log). |

Correcciones exclusivamente del montaje de pruebas: parámetro `currentSchema` añadido respetando los parámetros que ya traía la URL JDBC de Testcontainers; comparación de etapa como `Integer` devuelto por PostgreSQL; 404 contractual para una carga ajena que oculta su existencia; reconstrucción de configuración mediante Spring. Se mantuvo la exigencia de denegar acceso ajeno y la misma asociación de etapa, sin aceptar respuestas alternativas.

### Matriz de garantías y resultado final

| Requisito B1 | Suite/caso representativo | Resultado aislado |
|---|---|---|
| Migraciones V1–V6 desde base vacía, validación y reaplicación idempotente | `MigracionesArchivosB1IT.baseVaciaAplicaHastaV6SinBinariosYReaplicarNoCambiaNada` | PASS |
| V4–V6 sobre fase 2/V3 poblada: UUID, campos anteriores, evento, relación y bytes conservados | `v3PobladaConservaUuidRelacionHuellaBytesEHistorialAlActualizar` | PASS |
| Restricciones de estado/ID y offset remoto | `restriccionesRechazanCopiaSinIdYOffsetsMayoresAlArchivo` | PASS |
| Menor de 100 MiB, exactamente 100 MiB y un byte por encima en almacenamiento real | `AlmacenLocalRealTest` ampliado | 5/5 PASS |
| 100 MiB recibidos en 25 bloques, SHA-256 completo y archivo duradero, finalización repetida sin otro adjunto | `CargasB1IT.exactamente100MiBEn25BloquesConBytesRealesYAsociacionCompleta` | PASS |
| Exceso rechazado antes de crear sesión; bloque mayor de 4 MiB sin avanzar offset | `mayorA100MiBSeRechazaAntesDeCrearSesion`, `fragmentoMayorA4MiBNoAdelantaOffset` | PASS |
| Rangos malformados/invertidos, hueco, total distinto, hash incorrecto y replay alterado | Casos de rangos y `rechazaHashIncorrectoHuecoTotalDistintoYReplayAlterado` | PASS |
| Pausa/reanudación, repetición idempotente de inicio/bloque, parcial no adjunto | `pausaReanudaConIdempotenciaYParcialNoCuentaComoAdjunto` | PASS |
| Reconstrucción del coordinador y adaptadores con PostgreSQL/disco conservados; truncar únicamente tail no confirmado | `nuevoCoordinadorRecuperaOffsetSqlYTruncaTailNoConfirmadoSinPerderPrefijo` | PASS |
| Corrupción impide finalizar y conserva el parcial | `corrupcionDelParcialImpideFinalizarYConservaBytes`, `ParcialesB1Test` | PASS |
| Pedido/etapa/entrada/evento reales; cambio de entrada impide asociación a otra; permisos al iniciar/finalizar y carga ajena | Casos de asociación, entrada obsoleta y permiso vigente de `CargasB1IT` | PASS |
| Estado PENDIENTE inicial, COPIADO con ID/enlace confirmado, FALLIDO por agotamiento/permanente/corrupción | `DriveRecuperacionB1IT` | PASS en esos escenarios |
| Intento inicial + tres reintentos: contadores 1/2/3/4, esperas 1/5/15, no quinto tras reinicio/tiempo adicional | `inicialMasTresReintentosPersistidosSinDormirNiQuintoIntento` | PASS |
| Retry-After mayor y detención por autorización requerida | `retryAfterMayorYErrorPermanenteNoConsumenTresIntentosInutiles` | PASS |
| Caída después del ACK remoto, antes del checkpoint: nueva instancia recupera offset y mismo intento | `reinicioRecuperaMismoIntentoDesdeOffsetRemotoSinPerderBytes` | PASS |
| Cuarto intento interrumpido recuperable sin comenzar un quinto | `cuartoIntentoInterrumpidoTambienSeRecuperaSinQuinto` | PASS |
| Sesión Drive caducada y blob incompleto: PATCH/reinicio del mismo ID, sin otra reserva | `sesionCaducadaReiniciaContenidoEnElMismoId` | PASS |
| Respuesta incierta tras aceptación: reconciliar metadatos y confirmar sin segundo archivo/envío | `respuestaInciertaDespuesDeAceptarSeReconciliaSinSegundoArchivo` | PASS |
| Dos reclamaciones/workers, exclusión de fila, fencing de propietario antiguo y transferencia única | Casos concurrentes de `DriveRecuperacionB1IT` | PASS |
| Locks reales de filesystem entre instancias; prefijos faltantes no rellenados con ceros | `ParcialesB1Test` | 5/5 PASS |
| Cerrojo de copia ocupado: cesión durante 30 segundos sin red ni intento extra | `cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional` | **FAIL: B1-P02** |
| Archivos pendientes conservados tras reintentos, fallo permanente, checksum divergente y reinicio | Aserciones de bytes/ruta en Drive y parciales | PASS en los escenarios ejecutados |

Las reconstrucciones vacían el estado del coordinador/worker y crean instancias nuevas sobre el almacenamiento persistido; no se mató ni reinició una JVM del servidor real. Los casos de concurrencia usan hilos y locks del sistema de archivos dentro del entorno de pruebas. No acreditan comportamiento de un volumen de red o despliegue distribuido real.

Se conserva el límite provisional aprobado **100 MiB = 104857600 bytes**. El “100 MB” decimal del manual sería 100000000 bytes: no se cambió el límite ni se declaró aprobada otra unidad. No existe una lista comercial de formatos aprobada; las pruebas exigen aceptar contenido desconocido y no introducen reglas por extensión/MIME ni interpretación comercial. Los nombres siguen basename, eliminación de controles, fallback `archivo` y máximo 255 puntos Unicode.

### Defectos de producción reproducibles — sin corregir

#### B1-P01 — Alta: el backend completo no compila

- Origen: [NotificacionSqlRepositorio.java:96](../../backend/src/main/java/com/decokasa/seguimiento/notificaciones/adapter/out/persistence/NotificacionSqlRepositorio.java#L96) y línea 101, incorporadas en el paso 3.5.
- Reproductor: desde `backend`, ejecutar `.\gradlew.bat test --console=plain --no-daemon` o `.\gradlew.bat check --console=plain --no-daemon`, sin arnés ni exclusiones.
- Esperado: compilación de producción y ejecución de las pruebas/gates correspondientes.
- Observado: `no suitable constructor found for MapSqlParameterSource(String,...,int)`, dos errores; `compileJava FAILED`.
- Causa localizada: se invoca un constructor de tres argumentos que esa clase de Spring no ofrece. El tipo SQL se registra mediante la API de parámetros, no ese constructor. Se documenta para corrección posterior, sin cambiar la fuente ahora.
- Impacto: bloquea todas las suites oficiales y la cobertura global actual del backend. Logs 01 y 04. No se usa este fallo como evidencia de un caso de negocio RED.

#### B1-P02 — Media: la cesión por cerrojo ocupado falla en PostgreSQL

- Origen: [CopiaAdjuntoSqlRepositorio.java:108](../../backend/src/main/java/com/decokasa/seguimiento/archivos/adapter/out/persistence/CopiaAdjuntoSqlRepositorio.java#L108), método `ceder`.
- Reproductor persistente: `DriveRecuperacionB1IT.cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional`. Fixture PostgreSQL V6 y archivo local; mantener ocupado el lock de ese adjunto y ejecutar otro worker.
- Comando acotado tras disponer de compilación oficial: `.\gradlew.bat test --tests '*DriveRecuperacionB1IT.cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional' --console=plain --no-daemon`.
- Reproductor ejecutable con el bloqueo actual: añadir `-I ..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle` y usar la tarea `archivosB1Aislados` con el mismo filtro `--tests`.
- Esperado: ceder el lease, persistir próxima fecha +30 segundos, no llamar a Google y recuperar el mismo intento al pasar ese plazo.
- Observado: `BadSqlGrammarException`; PostgreSQL informa `column "proximo_intento_en" is of type timestamp with time zone but expression is of type interval`.
- Expresión localizada: `proximo_intento_en=:ahora + interval '30 seconds'`. La inferencia de tipo del parámetro en esa expresión no produce el timestamp requerido. Requiere revisar el enlace/cast o calcular la fecha antes de persistir en un encargo de corrección; no se altera ahora.
- Impacto: la pasada se interrumpe en vez de ceder. La reclamación anterior queda para recuperación por vencimiento del lease; no se demuestra la demora de 30 segundos. La prueba mantiene sus aserciones y permanece roja. Evidencia en ejecuciones 03 y 07, con stack y mensaje PostgreSQL en XML.

### Cobertura final — parcial, no aprobación del gate

JaCoCo de **la ejecución aislada 07**, generado en 08. Denominador: todas las clases del módulo `archivos` compiladas por el arnés, incluidos adaptadores Google/OAuth no ejercitados en B1. No es cobertura global del backend; no se reutilizó como actual el XML anterior de la fase 2.

| Alcance | Líneas cubiertas/total | Porcentaje |
|---|---:|---:|
| Módulo `archivos` completo del arnés | 752/1560 | **48,21 %** |
| Ramas del mismo módulo | 308/1006 | **30,62 %** |
| `GestionarCargaCasoDeUso` | 69/72 | 95,83 % |
| `CopiarPendientesCasoDeUso`, incluida `Ejecucion` | 145/167 | 86,83 % |
| `CopiaAdjuntoSqlRepositorio` | 35/37 | 94,59 % |
| `AlmacenCargasEnDisco` | 49/57 | 85,96 % |

**No se alcanza el objetivo de cobertura del 80 % en la medición completa de archivos.** El gate global del backend no pudo evaluarse porque `check` falla al compilar. No se rebajó el umbral ni se amplió este encargo a las pruebas OAuth, correo o pantallas para cerrar artificialmente ese pendiente.

Evidencia: [XML JaCoCo final](fase-3-evidencias/jacoco-archivos-b1-final.xml), [resumen legible por máquina](fase-3-evidencias/resumen-final.json), [reportes HTML y datos JaCoCo](fase-3-evidencias/reportes-b1-final.zip), [huellas de evidencias](fase-3-evidencias/sha256-final.json). Los HTML originales también quedan en `backend/build/reports/tests/archivosB1Aislados` y `backend/build/reports/jacoco/archivosB1Cobertura`.

### Pendientes y estado

1. Corregir B1-P01 y B1-P02 en un encargo posterior de producción; repetir sus reproductores conservando las pruebas.
2. Ejecutar `test` y `check` oficiales con todos los módulos y suites previas; obtener cobertura global nueva y atender los fallos que aparezcan. Los resultados del arnés no resuelven este bloqueo.
3. Completar la cobertura pendiente dentro de los encargos autorizados. Los adaptadores remotos/OAuth, cuentas reales y seguridad externa no se comprobaron aquí.
4. 3.B2 y 3.B3 quedan sin iniciar por este encargo. No se recibió autorización real de Google ni se resolvieron destinatarios de correo, formatos comerciales o retención.

**No se cierra la fase 3.** Este informe conserva comandos, resultados positivos, reproductores rojos, limitaciones y bloqueos para la siguiente corrección y reejecución.


## Corrección acotada de B1-P01 y B1-P02 — pendiente de revalidación

Fecha: 2026-10-17. Rama `fase-3-drive-gmail`. Encargo posterior de Richard limitado a estos dos defectos. Estado: **código corregido, sin pruebas/builds/migraciones ejecutados; no aprobado ni cerrado**. Las ejecuciones 01–08, XML, cobertura, resumen y huellas anteriores se conservan como evidencia previa; sus resultados no describen el código corregido.

Se aplicaron `ecc-springboot-patterns`, `ecc-jpa-patterns` y `ecc-java-reviewer`, con revisión por lectura de fuentes y del diff preparado. Se conserva el patrón JDBC dentro de los adaptadores existentes y la transacción `MANDATORY` que comparte el DataSource de la aplicación; no se añaden entidades, tablas, dependencias, transacciones o migraciones. La prohibición expresa de ejecutar pruebas/builds prevalece sobre los comandos de verificación de las skills.

### B1-P01: registro de parámetros SQL

Causa: `listar` y `contar` invocaban un constructor inexistente de tres argumentos de `MapSqlParameterSource`. Se sustituye en ambos sitios por `new MapSqlParameterSource().addValue("estado", valor, java.sql.Types.VARCHAR)`, conservando el tipo SQL explícito cuando el filtro es nulo. No cambia el SQL, la paginación, el filtro, el DTO ni los permisos. La corrección se limita al enlace del parámetro; no se declara confirmada la compilación sin ejecutarla.

### B1-P02: cesión sin ambigüedad de tipos temporales

Causa: PostgreSQL infiere como `interval` la expresión parametrizada `:ahora + interval '30 seconds'` del reproductor, incompatible con la columna timestamptz. `ceder` ahora calcula `ahora.plusSeconds(30)` a partir del Instant recibido del reloj controlado y enlaza `Timestamp.from(...)` como `:siguiente` directamente a `proximo_intento_en`. `:ahora` conserva su uso independiente en la condición de vigencia. No se usa el reloj del sistema ni se duerme.

La misma sentencia UPDATE sigue comprobando **id + reclamado_por + reclamado_hasta > :ahora + estado PENDIENTE**. Un propietario anterior, un lease caducado o una copia ya terminada no cumplen ese filtro y no deben modificar la fila. Solo se limpian reclamado_por/reclamado_hasta y se pospone la próxima fecha. Se conservan `copia_en_intento`, `intentos`, ID reservado, sesión, carpeta, offset, referencia local y metadatos: la siguiente reclamación recupera el mismo intento. No se inserta otra fila, crea otro archivo ni borra bytes. La exclusión de la reclamación con SKIP LOCKED y el cerrojo local no cambia. Estas garantías requieren repetición de los casos concurrentes; aquí solo se revisó el cambio de código.

### Revalidación pendiente — no ejecutada en este encargo

1. Repetir `test` y `check` oficiales completos para B1-P01 y obtener cobertura global nueva; no asumir verde por retirar el constructor incorrecto. Usar PostgreSQL Testcontainers y disco aislados, con todos los flags de Google/workers false como en B1. Atender cualquier fallo adicional sin confundirlo con la evidencia anterior.
2. Repetir sin cambios `DriveRecuperacionB1IT.cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional`: cesión sin error SQL, ausencia de red, próxima fecha de 30 segundos y recuperación del mismo intento tras avanzar el reloj 31 segundos.
3. Repetir las reclamaciones y workers concurrentes, fencing del propietario anterior, recuperación tras reinicio incluido cuarto intento y conservación de pendientes. Comprobar la carrera de cesión frente a renovación/reclamación por otro worker, lease caducado y estado COPIADO/FALLIDO: una cesión tardía debe afectar cero filas y no cambiar el trabajo ajeno. Registrar cobertura de esos escenarios y cualquier hueco antes de aprobarlos; no se añadieron ni alteraron pruebas ahora.
4. En un encargo de validación del registro, comprobar `listar`/`contar` con filtro nulo y con cada estado, incluida base sin filas, para conservar el tipado SQL y la paginación. No se avanzó aquí a 3.B2 ni se habilitó correo.

No se tocaron pruebas, arnés, evidencias históricas, configuración OAuth/workers, credenciales o tokens. Los cambios previos, Header.tsx y el índice Git se conservan. Sin llamadas a Google, commit o push. La fase y 3.B1 permanecen pendientes de revalidación; no se actualizan los contadores históricos ni se marca complete.


## Revalidación de B1-P01 y B1-P02 — 2026-10-17

**B1-P01 y B1-P02: CERRADOS, REVALIDADOS.** Check completo: **APROBADO**, salida 0. Resultado de la tarea oficial test: **565 casos, 0 fallos, 0 errores, 0 omitidos**. Arnés final: **84 casos, 0 fallos, 0 errores, 0 omitidos**. Esta sección es posterior a la corrección y a las evidencias históricas 01–08; no modifica sus resultados.

### Reproductores y cierre acotado

- **B1-P01:** se repitió mediante la tarea oficial `test` filtrada al reproductor de Drive, sin exclusiones de producción. `compileJava` compiló todos los módulos, incluido notificaciones, y la ejecución terminó correctamente. Los checks completos posteriores también superan compilación. Se cierra el defecto del constructor; no se declara validado el procesamiento de correo ni se inicia 3.B2.
- **B1-P02:** el reproductor original `cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional` pasa sin cambiar sus aserciones. También pasan las cesiones de propietario ajeno, lease caducado, respuesta tardía tras nueva reclamación, estados COPIADO/FALLIDO, renovación concurrente y conservación de ID reservado, sesión, offset, referencia local, próxima fecha de 30 segundos y mismo intento. Se cierra el error SQL de cesión, sin cerrar otros pendientes de fase 3.
- La prueba de transferencia en vuelo deja vencer el lease con reloj controlado, permite que otro worker reclame y ceda al encontrar el lock, libera la respuesta antigua y recupera mediante reconciliación: **un solo envío, un solo inicio remoto y el mismo intento**. Ningún propietario antiguo confirma el trabajo del nuevo.

### Casos B1 completados, sin duplicar suites

Se extendieron las suites existentes de carga, almacenamiento y recuperación. Se añadieron suites específicas de protocolo Drive y del adaptador de tarea, donde no existían. Son **44 casos adicionales respecto a los 40 del informe inicial**, sin omitir los anteriores:

- Cesión y fencing concurrentes; expiración durante transferencia; respuesta incierta al crear carpeta; rechazo de metadatos de otro pedido, entrada, padre o propietario.
- Misma clave idempotente con comando distinto; permiso revocado antes del bloque; fallo de persistencia al finalizar: rollback de adjunto/evento/sesión, conservación del final preparado y recuperación con el mismo UUID reservado.
- Protocolo reanudable con cliente HTTP completamente simulado: rangos 308, progreso confirmado, sesión 404/410, POST y PATCH sobre el mismo ID, error/cuota/Retry-After con tiempo controlado, metadatos incompletos, fallo de transporte, suscriptor real de respuesta, cancelación de respuesta excesiva y clasificación de errores sin causa privada.
- Consultas backend de archivos: ID reservado no produce enlace; ID/enlace/fecha confirmados permiten consultarlo; descarga local conservada; permisos actuales también con JWT antiguo; lista de copias fallidas sin rutas/claves privadas y con paginación acotada. No se ejecutaron pantallas ni pruebas de correo de 3.B2.
- Firmas técnicas existentes de PDF/ZIP/OLE/PNG/JPEG/GIF se registran como metadatos, sin una lista nueva de formatos permitidos ni interpretación comercial. Se conserva el caso de contenido desconocido aceptado. Límite provisional: **100 MiB = 104857600 bytes**, distinto de los 100 MB decimales del manual.
- El adaptador de tarea se invoca manualmente con un puerto falso: una excepción no impide la pasada siguiente y no se registra el throwable ni la sesión ficticia. No se registra scheduling ni se habilita el worker real.

| Suite del arnés final | Casos | Fallos | Errores | Omitidos |
|---|---:|---:|---:|---:|
| `AlmacenLocalRealTest` | 11 | 0 | 0 | 0 |
| `CargasB1IT` | 22 | 0 | 0 | 0 |
| `DriveRecuperacionB1IT` | 23 | 0 | 0 | 0 |
| `GoogleDriveProtocoloB1Test` | 19 | 0 | 0 | 0 |
| `MigracionesArchivosB1IT` | 3 | 0 | 0 | 0 |
| `ParcialesB1Test` | 5 | 0 | 0 | 0 |
| `TareaCopiaB1Test` | 1 | 0 | 0 | 0 |

### Mantenimiento de fixtures de migraciones

El primer check completo detectó cinco fallos de expectativas heredadas de V3, no nuevos defectos de producción. Se conserva su evidencia en `check-inicial`. `MigracionesIT` ahora exige exactamente V1–V6, las cuatro migraciones posteriores a V2 y la lista completa de tablas, incluidas las de cargas, carpetas y notificación; conserva las comprobaciones de V1/V2, restricciones y validación/reaplicación. `V2RestriccionesIT` actualiza solo el total/versión del esquema y vuelve a ejecutar todos sus casos de restricciones contra V6. El reproductor del backfill/rollback `V3MigracionIT` fija target 3 para esa migración concreta; `MigracionesArchivosB1IT` sigue exigiendo el upgrade poblado V3→V6 y la base vacía V6. No se eliminan casos ni se aceptan versiones/tablas inesperadas.

El siguiente check superó las pruebas, pero falló el umbral global con ratio 0,78. Esa evidencia queda en `check-fixtures` y `jacoco-check-fixtures.xml`. Se completaron los comportamientos B1 anteriores y se repitió el check; no se redujo el umbral.

### Comandos efectivamente ejecutados

Se usa [revalidar-b1.ps1](fase-3-evidencias/revalidar-b1.ps1), desde backend, con stdout/stderr y código de salida conservados. Todos incluyen `--console=plain --no-daemon`.

| Inicio | Comando | Salida | Log |
|---|---|---:|---|
| 20261017-123328 | `.\gradlew.bat test --tests *DriveRecuperacionB1IT.cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional --console=plain --no-daemon` | 0 | [reproductores](fase-3-evidencias/revalidacion-B1/20261017-123328-reproductores.log) |
| 20261017-123451 | `.\gradlew.bat -I ..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle archivosB1Aislados --console=plain --no-daemon` | 0 | [arnes](fase-3-evidencias/revalidacion-B1/20261017-123451-arnes.log) |
| 20261017-123759 | `.\gradlew.bat -I ..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle archivosB1Aislados --console=plain --no-daemon` | 0 | [arnes](fase-3-evidencias/revalidacion-B1/20261017-123759-arnes.log) |
| 20261017-123944 | `.\gradlew.bat check --console=plain --no-daemon` | 1 | [check](fase-3-evidencias/revalidacion-B1/20261017-123944-check.log) |
| 20261017-124215 | `.\gradlew.bat check --console=plain --no-daemon` | 1 | [check](fase-3-evidencias/revalidacion-B1/20261017-124215-check.log) |
| 20261017-124706 | `.\gradlew.bat -I ..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle archivosB1Aislados --console=plain --no-daemon` | 0 | [arnes](fase-3-evidencias/revalidacion-B1/20261017-124706-arnes.log) |
| 20261017-124836 | `.\gradlew.bat -I ..\documentos\pruebas\fase-3-evidencias\archivos-aislados.init.gradle archivosB1Aislados --console=plain --no-daemon` | 0 | [arnes](fase-3-evidencias/revalidacion-B1/20261017-124836-arnes.log) |
| 20261017-124937 | `.\gradlew.bat check --console=plain --no-daemon` | 0 | [check](fase-3-evidencias/revalidacion-B1/20261017-124937-check.log) |

El runner fuerza OAuth/Drive/Gmail y ambos procesamientos automáticos a false, semilla false y configuración importada vacía. Para el check completo crea un directorio temporal UUID mediante ALMACEN_LOCAL_DIR; los fixtures con @TempDir/DynamicPropertySource conservan sus discos exclusivos. DB_URL apunta a un puerto local cerrado como protección si falta un Testcontainer: las conexiones válidas proceden exclusivamente de ServiceConnection o las URL del contenedor de la clase. No usa bases de desarrollo/empresa, reuse, Docker Compose, limpieza de volúmenes ni cuentas reales. Los backoffs y el vencimiento de leases usan ClockB1; las barreras solo tienen timeouts cortos de seguridad.

### Cobertura real final, sin mezclar denominadores

El XML final proviene de **la tarea oficial test dentro del check completo**, con todos los módulos de producción. La cobertura del módulo se obtiene sumando los paquetes `com/decokasa/seguimiento/archivos/` de ese mismo XML. El arnés conservado sigue siendo suplementario y no sustituye esta ejecución.

| Medición | Cubierto/total y porcentaje |
|---|---:|
| Backend global, líneas | **3727/4639 (80,34 %)** |
| Backend global, ramas | 1240/2186 (56,72 %) |
| Módulo archivos, líneas, desde el XML global | **1012/1561 (64,83 %)** |
| Módulo archivos, ramas, desde el XML global | 463/1006 (46,02 %) |

El umbral existente **0.80 de líneas globales** permanece intacto en build.gradle. No se añadió ninguna exclusión al check o a JaCoCo. La cifra del módulo es independiente y sigue por debajo del 80 %: no se declara alcanzado ese porcentaje para archivos. Incluye OAuth/almacén de tokens y ramas restantes del módulo; las comprobaciones de cuentas, correo y seguridad externa no se incorporaron por deducción a 3.B1. Mantener esa deuda de cobertura visible y completarla en el alcance que se autorice.

### Evidencia y conservación

[Resumen y comandos](fase-3-evidencias/revalidacion-B1/resumen-revalidacion.json), [JUnit oficial final](fase-3-evidencias/revalidacion-B1/check-final/), [JUnit del arnés](fase-3-evidencias/revalidacion-B1/arnes-final/), [XML global JaCoCo](fase-3-evidencias/revalidacion-B1/jacoco-global-final.xml), [XML del arnés](fase-3-evidencias/revalidacion-B1/jacoco-arnes-final.xml), [HTML y execution data](fase-3-evidencias/revalidacion-B1/reportes-revalidacion.zip), [huellas](fase-3-evidencias/revalidacion-B1/sha256-revalidacion.json). Se conservan también el reproductor inicial, arnés inicial y las ejecuciones intermedias; no se sobrescriben evidencias 01–08.

Producción y build.gradle conservados por SHA-256; cambios ajenos al alcance detectados: **0**, índice Git intacto. Se conservan Header.tsx, plan, evidencias de fase 2, pruebas previas y arnés original. No se modificaron credenciales/tokens ni se llamó a Google. Sin commit/push. Solo se cierran B1-P01 y B1-P02; la fase permanece abierta y no se avanza a 3.B2.

## 3.B2 — Correo y pantallas, 2026-10-19

**Validación automatizada con dobles: APROBADA. No acredita integración real con Google ni cierra la fase.** Rama comprobada: `fase-3-drive-gmail`. Se aplicaron `ecc-react-test`, `playwright-best-practices` y revisión de las pruebas Java con `ecc-java-reviewer`. Se leyeron el manual Word vigente, el plan activo y los antecedentes de este informe. No se modificó producción.

### Resultados y comandos

| Ejecución | Comando efectivo | Resultado final |
|---|---|---|
| Frontend, lint | `npm.cmd run lint` | Salida 0; TypeScript sin errores |
| Frontend, suite completa | `npm.cmd test` | Salida 0; **228 casos, 22 archivos; cero fallos** |
| Frontend, build | `npm.cmd run build` | Salida 0 |
| Backend, casos B2 y reglas incluidas por Gradle | `gradlew.bat test --tests '*CorreoB2IT' --tests '*Backend24IT.b2*' --console=plain --no-daemon` | Salida 0; **13 + 4 casos B2 y 7 reglas de arquitectura**, cero fallos/errores/omitidos |
| Navegador | `node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test --config playwright.config.mjs` | Salida 0; **4 casos aprobados**, cero reintentos automáticos |

Los comandos frontend/backend se ejecutaron mediante [ejecutar-b2.ps1](fase-3-evidencias/3.B2/ejecutar-b2.ps1), con parámetro `frontend` o `backend`. El comando Playwright se ejecutó desde `documentos/pruebas/fase-3-evidencias/3.B2`. Usa Playwright 1.62.1 del runtime existente y Edge headless; no se añadieron dependencias ni se modificaron package.json, lockfile o configuración productiva. Vite usa exclusivamente `127.0.0.1:43182`, sin reutilizar servidores existentes, con acceso local de demo desactivado y API local simulada. Toda petición del navegador fuera de ese origen se aborta antes de salir; los casos de archivos y registro comprueban además ausencia de peticiones inesperadas.

Evidencias finales: [lint](fase-3-evidencias/3.B2/lint-final.log), [Vitest](fase-3-evidencias/3.B2/test-final.log), [build](fase-3-evidencias/3.B2/build-final.log), [códigos frontend](fase-3-evidencias/3.B2/resultados-finales.json), [backend](fase-3-evidencias/3.B2/backend.log), [comando backend](fase-3-evidencias/3.B2/backend.json), [Playwright](fase-3-evidencias/3.B2/playwright.log), [resultado estructurado](fase-3-evidencias/3.B2/playwright.json), [trazas de navegador](fase-3-evidencias/3.B2/browser-artifacts/), [conservación y contadores](fase-3-evidencias/3.B2/conservacion.json). Los JUnit XML finales se guardan en esa misma carpeta.

### Comportamientos comprobados

- **Transiciones y atomicidad reales del backend:** se amplió `Backend24IT`, reutilizando sus fixtures y su contexto Testcontainers. ENVIAR, RECHAZAR, APROBAR y DEVOLVER_POR_ERROR atraviesan los controladores y oyentes existentes: seis transiciones producen seis intenciones con seis UUID de evento distintos, sin destinatario inventado y con cero intentos. Otra prueba ejecuta una transición válida dentro de una transacción exterior, observa su aviso antes del rollback y comprueba después que pedido, historial, adjuntos y relaciones coinciden con el estado anterior y no queda ninguna notificación. No se añadió un segundo encolador.
- **Persistencia del aviso con receptores ficticios:** `CorreoB2IT` reutiliza `DatosB1`, V1–V6 y almacenamiento de fixtures existente. Cada caso usa su propio esquema dentro del PostgreSQL 17 Testcontainer de la clase, sin reuse ni conexión a bases compartidas. Se comprueban los cuatro tipos de evento, sus destinos y snapshots, normalización del correo y clave evento/destinatario; repetir el evento no inserta otra fila. La intención con destinatario nulo nunca es reclamable ni consume intentos.
- **Procesamiento y recuperación de correo:** un doble de `Notificador` comprueba que la llamada ocurre sin transacción activa; se persisten destinatario, ID ficticio del proveedor, resultado e intento y se conserva Message-ID. Dos reclamaciones concurrentes solo obtienen una fila. Un propietario ajeno o un lease caducado no confirma la entrega. El resultado incierto queda durable antes del POST: vencimiento, nuevo repositorio, excepción posterior al POST o respuesta incierta no producen otro envío ciego. Un destinatario retirado del directorio falla con CONFIGURACION sin invocar el proveedor. La cuota termina en FALLIDA con CUOTA y un solo intento conforme a la parada conservadora actual; **no se inventó un presupuesto de reintentos de correo**.
- **Registro exclusivo del admin:** comprobación de la ruta y menú reales en RTL y Playwright; el no admin no consulta el registro. El endpoint real permite admin vigente, rechaza otros roles y vuelve a rechazar al revocar el rol aunque exista un token anterior. El caso de uso también rechaza al usuario desactivado. Se comprueban `Cache-Control: no-store`, límites de paginación y sanitización: un error SQL ficticio no clasificable sale como ERROR_NO_CLASIFICADO, sin publicar datos privados, Message-ID o ID del proveedor. El frontend respeta un 403 del servidor aun con sesión que dice admin, sin reintento automático.
- **Pantallas sin entrega inventada:** se comprueban intención bloqueada, destinatario ficticio, intento, fallo incierto, aceptación simulada y lista vacía. La pantalla distingue aceptación por Gmail de recepción/lectura y no ofrece envío, reenvío o reintento. Las respuestas son exclusivamente de MSW o rutas Playwright; no prueban recepción de correos.
- **Carga real en navegador frente a backend simulado:** archivo ficticio de 4 MiB + 1 byte, dos bloques con rangos y SHA-256 comprobados. Un acuse retenido mantiene progreso en cero y no muestra éxito; tras confirmar el primer bloque muestra 4194304 bytes. Se pausa durante el segundo, se consulta la sesión al reanudar, se valida el prefijo confirmado y se termina sin repetir el primer bloque. Solo después de finalizar aparece el aviso de recepción por el servidor, con copia pendiente. El test RTL existente se adaptó del multipart antiguo al protocolo por bloques y comprueba invalidación de archivos/detalle y que sessionStorage solo contiene UUID de sesión/idempotencia, sin bytes ni tokens.
- **Archivos y enlaces:** PENDIENTE y FALLIDO no ofrecen enlace Drive; COPIADO permite consultarlo, pero no publica URL antes de responder el backend. Se prueban 403, 409 y URL ajena a Drive sin generar enlace, conservando la descarga por backend. Playwright comprueba estados, enlace ficticio confirmado y descarga local con nombre original; no abre el enlace externo. Las pruebas de descarga previas siguen presentes. La búsqueda de URLs Drive en producción solo encuentra la validación del hostname en `EnlaceDrive.tsx`, sin IDs/URLs fabricados del demo.

### Mantenimiento de pruebas y ejecuciones iniciales

La ejecución inicial tuvo lint/build correctos y **211/219 pruebas frontend aprobadas**. Ocho expectativas correspondían al multipart anterior, al texto anterior del botón, a la prohibición absoluta de sessionStorage y a la marca anterior a la edición existente de Header.tsx. Se actualizaron las expectativas al protocolo aprobado y a la marca que ya muestra el Header, sin modificarlo. La excepción de sessionStorage queda limitada a `api/cargas.ts`; se mantiene la prohibición de localStorage y se comprueba el contenido efectivo de la referencia durante una carga. Se conservaron todos los casos de permisos, rechazos y descarga. La ampliación deja 228 casos: seis nuevos de registro y tres de enlaces; los diez casos de la nueva suite backend se ampliaron a trece y cuatro se añadieron a la suite HTTP existente. No se duplicaron suites heredadas.

Durante la preparación se corrigieron exclusivamente fixtures/tests: UTF-8, inferencia genérica de AssertJ, inserción del evento de rollback respetando el historial de solo inserción y selectores de mensajes. Una ejecución intermedia de la suite completa mostró un timeout del test heredado de listado bajo carga; las ejecuciones completas finales pasan sin alterar ese test ni ampliar sus tiempos. La primera prueba Playwright de progreso seleccionaba también un texto explicativo; se acotó al aviso completo del archivo, manteniendo la aserción de ausencia antes de finalizar. [Ejecución inicial frontend](fase-3-evidencias/3.B2/test-inicial.log) y [evidencia intermedia conservada](fase-3-evidencias/3.B2/ejecucion-inicial/). No se identificó un defecto productivo reproducible en los escenarios finales ejecutados; no se aplicaron correcciones productivas.

El build conserva avisos de tamaño de chunk y Vite avisa del uso existente de `__dirname`; no impiden lint/test/build. No se modificó configuración para ocultarlos.

### Decisiones pendientes y límites de la evidencia

1. **F3-D8, destinatarios:** siguen sin aprobarse correos receptores, asociación al admin vigente, alcance por país y tratamiento de intenciones históricas. La cuenta remitente aprobada no se adopta como destinatario. El directorio productivo continúa vacío y las intenciones nulas permanecen no enviables. `admin@ejemplo.test` y los demás correos de fixtures solo sirven para pruebas; no configuran producción ni autorizan backfill.
2. **F3-D5, correo:** falta aprobar presupuesto, esperas, errores reintentables y resolución administrativa de resultados inciertos. Los tres reintentos aprobados para Drive no se trasladan a Gmail. Se valida la parada conservadora existente, sin reenvío automático ni promesa de entrega exactamente una vez. No se habilita un reintento manual inexistente.
3. **Cobertura de archivos:** permanece la deuda registrada en 3.B1: **1012/1561 líneas = 64,83 %**, frente al último global completo **3727/4639 = 80,34 %**. Ambos son datos de la ejecución completa B1 conservada; no son una medición global nueva de B2. El [XML B2 filtrado](fase-3-evidencias/3.B2/jacoco-backend-b2-filtrado.xml) procede solo de estos casos seleccionados y no sustituye ese denominador ni acredita 80 % para archivos. No se redujo el umbral, no se cambió JaCoCo y no se ejecutó un nuevo check completo del backend en este paso.
4. **3.B3 no iniciado:** quedan reservadas la autorización OAuth expresamente aprobada, capacidades y carpeta reales de Mi unidad, enlace accesible confirmado en Google, entrega real/recepción del correo y revisión de seguridad externa. Los enlaces e IDs de estas pruebas son datos ficticios confirmados por dobles. No se hicieron autorizaciones, subidas, envíos o llamadas a Google; no se acredita integración externa por inferencia.

### Conservación

OAuth, Drive/Gmail y ambos procesamientos automáticos se forzaron a false en el runner backend. Los casos invocan manualmente servicios con puertos falsos; no registran ni activan workers reales. Se usó disco temporal exclusivo y URL fail-closed si faltara un Testcontainer. No hubo esperas reales de minutos, bases compartidas, borrado de volúmenes, commit ni push. Huellas de todos los archivos productivos iniciales sin cambios e índice Git intacto; Header.tsx, plan, evidencias de fase 2 y cierre previo de B1-P01/B1-P02 conservados. **La fase sigue abierta y no se avanza a 3.B3.**

## Preparación de 3.B3 — revisión local de seguridad, 2026-10-19

**Resultado: preparación documentada; ensayo real NO EJECUTADO. No habilita integraciones ni acredita seguridad/integración externa completa.** Se comprobaron manual Word, antecedentes B1/B2 y fuentes/configuración locales en `fase-3-drive-gmail`. Se aplicaron `security-review`, `ecc-springboot-security`, las partes de inspección de `ecc-springboot-verification`/`ecc-verification-loop` y la revisión local de configuración indicada por `ecc-security-scan` en el manual. No se ejecutaron builds, suites, migraciones, autorización, workers o llamadas a Google en este encargo. Los resultados B1/B2 son históricos y se conservan, sin presentarlos como nuevas ejecuciones.

El [procedimiento concreto del ensayo](fase-3-B3-procedimiento.md) define cuenta, propuesta de receptor pendiente, carpeta compatible con el código, archivo de exactamente 100 MiB, aislamiento, matriz de flags, autorización en el mismo navegador, operaciones, recuperación, parada y evidencia sanitizada. **No es una orden de ejecutarlo ahora.**

### Comprobaciones locales y evidencia

Se ejecutó [auditar-local.py](fase-3-evidencias/preparacion-B3/auditar-local.py) con el Python del runtime local, sin red y sin imprimir credenciales. Usa `git branch`, `git ls-files`, `git diff --cached`, `git rev-list --objects --all` y `git cat-file --batch`; revisa metadatos y permisos sin escribir secretos. [Resultado estructurado](fase-3-evidencias/preparacion-B3/auditoria-local.json), [ACL](fase-3-evidencias/preparacion-B3/acl-local.json), [conservación final](fase-3-evidencias/preparacion-B3/conservacion-final.json) y [huellas de evidencia](fase-3-evidencias/preparacion-B3/sha256.json).

- Cuenta local configurada coincidente con **importadora.latinoamericaworld@gmail.com**. OAuth false explícito; Drive, Gmail y ambos workers ausentes en el archivo privado y false por defecto en `application.yml`. No hay overrides de esos cinco flags en el proceso de revisión. Esto describe la configuración leída, no una inspección del entorno de posibles procesos ajenos ya arrancados.
- JSON configurado de tipo **web**, fuera del repositorio; `redirect_uris` ausente, permitido por la decisión aprobada. El callback del backend permanece fijo. No se confirmó su registro real en Cloud.
- JSON, llave y sus directorios: ACL con herencia deshabilitada, cero reglas ALLOW para identidades distintas del propietario, sin reparse point en los objetos comprobados. Llave de 32 bytes. Directorio de tokens privado y externo; **no existe el contenedor en la ruta configurada**. No se crearon tokens, archivos privados o llaves, ni se cambió una ACL.
- Escaneo de **1550 entradas actuales inventariadas y 2035 blobs históricos**: cero coincidencias de los patrones revisados de secreto Google, access token Google o llave privada, y cero coincidencias del **valor exacto del client_secret configurado**. El valor se comparó solo en memoria; no figura en script, resultado, consola o informe. Cero nombres de credenciales/tokens sensibles rastreados o preparados en el índice. No equivale a demostrar ausencia universal de todo secreto o vulnerabilidad.
- `rg` por hosts Google, logging, rutas, comprobaciones de permiso, límites y configuración; lectura de adaptadores y casos de uso. No se enviaron peticiones a endpoints OAuth/Drive/Gmail ni se consultó Cloud. La revisión de configuración de `.claude` encontró PRPs y enlaces de skills, sin settings/hooks/MCP locales de proyecto que ejecutar o modificar.
- Las instrucciones futuras se comprobaron solo por sintaxis: `node --check` del snippet de operador, salida 0, y parser PowerShell del ejemplo de archivo, cero errores. [Resultado](fase-3-evidencias/preparacion-B3/sintaxis-procedimiento.json). No se ejecutó ninguno de esos scripts ni se creó el archivo de 100 MiB. `git diff --check` del informe sin errores; comparación de huellas sin cambios ajenos al informe y nuevo procedimiento/evidencias, índice intacto.

### Matriz de revisión de seguridad por lectura

| Área | Observación local y límite de la conclusión |
|---|---|
| Permisos | `AutorizarCuentaGoogleCasoDeUso` exige admin activo al inicio y al consumir state; el adaptador revalida antes de guardar. Cargas comprueban propietario y permiso vigente al iniciar/bloquear/completar. Descarga/enlace requieren autorización de pedido/etapa; notificaciones y copias fallidas requieren admin vigente en servidor. B1/B2 aportan pruebas anteriores, pero OAuth y sus variantes no se ejecutaron de nuevo aquí |
| Rutas y nombres | UUID técnicos para almacenamiento; `Adjunto.limpiarNombre` elimina rutas/controles y limita nombre; claves locales normalizadas bajo base. Parciales comprueban symlinks y usan NOFOLLOW_LINKS; secretos también resuelven junctions y excluyen árboles Git. La contención de rutas no sustituye ACL privadas del disco ni constituye prueba universal contra carreras locales |
| Límites e integridad | Total 104857600 bytes; bloques 4194304; rangos y SHA-256 verificados; máximo técnico en backend y adaptador Drive. Finalizar exige integridad y bytes completos. La descarga comprueba tamaño/hash y conserva backend. Límite agregado de disco/cargas: hallazgo B3-SEG-01, abajo. No hay lista de tipos aprobada; firmas son metadatos, no validación comercial |
| Tokens y secretos | AES-256-GCM, llave externa separada, versión autenticada, nonce aleatorio, bloqueo y reemplazo atómico. Tokens Google solo servidor; JWT de la aplicación en memoria del frontend, no en almacenamiento persistente. sessionStorage conserva referencias UUID de carga. No confundir JWT de negocio con tokens Google |
| OAuth | Cliente web fijo; callback exacto, sin entrada de retorno del navegador; state aleatorio de un uso y cinco minutos, vínculo cookie, PKCE S256, nonce, RS256, emisor/audiencia/azp/caducidad/email verificado y cuenta/ámbitos. Offline solicitado. La página `/resultado?codigo=200` es informativa y puede abrirse directamente: no sirve sola como prueba de consentimiento |
| Cookie y exposición local | Cookie OAuth HttpOnly, host-only, Lax, cinco minutos y Secure=false por callback HTTP local. El ensayo exige bind 127.0.0.1, uso del host localhost, sin túnel/LAN. No se aprueba reutilizar este callback/cookie como despliegue público HTTPS. El login mantiene su protección Origin/SameSite y Bearer |
| Egreso Google | Transporte de archivo/correo en backend: endpoints HTTPS fijos, sin redirects en clientes Drive/Gmail/token, timeouts y cuerpos acotados. Sesión Drive valida esquema/host/puerto/path y se cifra en persistencia. Frontend usa `/api/v1`; CSP nginx connect-src self. Única visita administrativa prevista del navegador: consentimiento, más verificación manual del resultado en Drive/Gmail; no APIs Google desde el frontend |
| Errores y registros | Causas cerradas; no se adjunta throwable privado en tareas; DTO de notificaciones sanitiza errores. Callback evita propagar query, devuelve 303 a página limpia, no-store y no-referrer; Tomcat accesslog false por defecto. No habilitar debug/wire logs ni exportar HAR OAuth. No se certifican logs de proxies o servicios ajenos no inspeccionados |
| Concurrencia y envío | Outbox único transaccional; envío externo después; reclamación con fencing y marca incierta durable, sin otro POST ciego. No hay promesa de entrega exactamente una vez. Esta lectura y los dobles B2 no prueban entrega/recepción Google real |

### Hallazgo local y bloqueos

**B3-SEG-01 — severidad media, disponibilidad / cuota agregada pendiente.** `GestionarCargaCasoDeUso.iniciar`, `CargasController` y V5 limitan cada archivo y cada bloque, pero no establecen presupuesto total de disco, máximo agregado por usuario ni número de cargas activas. La unicidad `(usuario_id, clave_idempotencia)` permite sesiones distintas con claves distintas. Un usuario con permiso de carga puede acumular parciales válidos y agotar disco aun respetando los 100 MiB. Es una observación de fuentes; no se agotó disco ni se enviaron cargas para reproducirla.

Reproductor pendiente, solo en un entorno local autorizado y acotado: disco de prueba con cuota pequeña conocida, mismo usuario/pedido/entrada vigentes, varias claves idempotentes distintas y varios bloques válidos; observar suma de bytes y rechazo por falta de espacio sin superar el límite de cada archivo. Mantener Google apagado y conservar parciales. La recomendación es acordar presupuesto/reserva y concurrencia de cargas, junto con monitorización; **no implementar ni inferir retención o borrado no aprobados**. Para el ensayo propuesto: un solo archivo, capacidad suficiente y vigilancia manual; esto contiene el ensayo, no resuelve el hallazgo para producción.

No se identificó un hallazgo crítico/alto confirmado en las fuentes y configuración inspeccionadas. **No es una certificación de ausencia**: OAuth real, verificaciones dinámicas específicas pendientes y análisis de dependencias actualizado no se han realizado. La fase no satisface por esta lectura el criterio de cierre del manual.

- **B3-BLOQ-01, destinatario:** pendiente aprobación explícita como receptor y asociación a admin vigente/país. El remitente no se convierte en receptor por estar configurado. El procedimiento propone, sin aprobar, un autoenvío al buzón de empresa; si se exige entrega entre buzones, falta otro correo controlado.
- **B3-BLOQ-02, mecanismo de ensayo:** `NotificacionesConfig` devuelve directorio vacío sin opción externa; habilitar Gmail/worker no lo soluciona. Falta arnés test-only con directorio aprobado y una invocación del puerto existente, sin modificar producción ni hacer backfill manual. No hay endpoint de ejecución manual. El procedimiento no inventa variables, endpoints o comandos de arnés inexistentes. No se implementó ese arnés ahora.
- **B3-BLOQ-03, carpeta:** la estructura implementada es `root → país → número real`, sin raíz de ensayo configurable. Se propone carpeta nueva EC/pedido con IDs exclusivos del ensayo en Mi unidad; requiere autorización de creación. Exigir una raíz alternativa bloquearía la ejecución hasta acordar un cambio. `drive.file`, acceso a root, cuota y capacidades reales siguen sin probarse; no se amplían permisos a `drive` general para sortear un error.
- **Herramientas:** AgentShield, Gitleaks y TruffleHog no se encontraron como comandos; AgentShield tampoco en dependencias locales comprobadas. Se hizo inspección manual/local con herramientas existentes; no se descargó/instaló nada ni se ejecutó AgentShield o se atribuyó una calificación a su nombre. No hay tareas SpotBugs/PMD/Checkstyle/OWASP configuradas en el build revisado; no se afirma que hayan pasado. Un análisis CVE actualizado y el bucle completo quedan fuera de esta preparación, sin modificar build.gradle ni repetir por rutina las suites B2.

### Pendientes que siguen abiertos

**F3-D5 correo:** presupuesto, esperas, causas reintentables y resolución administrativa siguen pendientes; Drive conserva inicial + tres reintentos, no trasladables a Gmail. El eventual ensayo autorizaría un único intento inicial y parada ante fallo/incertidumbre, sin establecer política productiva.

**Cobertura archivos:** permanece **1012/1561 líneas = 64,83 %** de B1. Global completo histórico **3727/4639 = 80,34 %**; no confundir denominadores ni afirmar medición nueva. B2 aprobado con dobles no elimina esta deuda. No se alteraron umbrales ni exclusiones.

La prueba real futura requerirá aprobación expresa de Richard, datos anteriores, aislamiento y mecanismo revisado. Detener solo procesos del ensayo, devolver flags false para el próximo arranque y conservar DB/volúmenes/parciales/tokens cifrados bajo ACL privada; no borrar ni revocar automáticamente. Evidencias públicas sin secretos y anexo privado de correlación fuera de Git. No hubo modificaciones de producción, activaciones, conexiones Google, commit, push o cierre de fase.

## Arnés aislado de 3.B3 — destinatario aprobado solo para ensayo, 2026-10-19

**Richard aprobó importadora.latinoamericaworld@gmail.com como destinatario exclusivamente de 3.B3. No es aprobación como destinatario definitivo de producción ni autorización de OAuth, copia o envío.** Se preparó el [arnés opt-in](arnes-b3/README.md) y se actualizó el [procedimiento](fase-3-B3-procedimiento.md). Se aplicaron `ecc-springboot-tdd` y `ecc-java-reviewer` a las fuentes de ensayo, sin cambios productivos.

### Entrega y aislamiento

- Todo el código nuevo está en `documentos/pruebas/arnes-b3`: init script Gradle, source set **ensayoB3**, `ArnesGoogleB3`, `GuardasB3`, sus pruebas y runner PowerShell. No se modificó build.gradle ni se añadió código a src/main o src/test. El JAR de producción y los `test/check` normales no incorporan este arnés; solo se compila al pasar su init script explícitamente. El paquete del arnés está fuera del component scan productivo.
- La acción predeterminada del runner es **Estado**, sin JVM/Docker/red. El arranque exige consola interactiva, directorio externo con nombre UUID, ACL privada, fuera de Git y frase explícita. Se rechaza un puerto 8080 ocupado, sin detener procesos ajenos. A continuación arrancaría PostgreSQL 17 Testcontainers sin reuse, con DB/usuario/credenciales propios, puerto aleatorio publicado solo en loopback y volumen nombrado etiquetado por ensayo y raíz. No acepta una URL de base compartida. **Ese arranque no se ejecutó.**
- El volumen y las propiedades privadas externas permiten recuperar el mismo ensayo. El arnés rechaza volumen ajeno, raíz distinta o contenedor activo con el mismo ID; mantiene el cerrojo de raíz hasta detener su contexto/contenedor. El cierre hace STOP a su contenedor, no removeVolume/prune/down -v. La preservación efectiva del montaje y recuperación quedan pendientes de validar cuando se autorice el arranque real.
- Se reutilizan las rutas externas del JSON/llave y la cuenta configurada. No se copian credenciales ni se usan DB, JWT o tokens habituales. Los tokens de ensayo serían `tokens-3b3.enc` bajo ACL privada externa, cifrados por el almacén productivo existente. No se creó ese archivo ni se preparó la carpeta privada del ensayo hoy.
- Directorio **test-only @Primary**, cargado únicamente por el main del arnés: receptor fijo aprobado, EC y admin exclusivo activo con rol admin y asociación EC vigente. No hay parámetro de destinatario arbitrario ni cambio del directorio productivo, que sigue vacío. El admin futuro se crea únicamente en la base nueva con contraseña enmascarada y hash BCrypt, sin copiar datos empresariales.
- Los cinco flags se fijan por contexto de ensayo, por encima de configuraciones externas: todos false al iniciar/volver. OAuth, Drive y Gmail tienen modos separados; ambos schedulers permanecen false en **todos** los modos. No hay endpoint HTTP de ejecución general. El código llama a los puertos existentes únicamente tras confirmación de consola; no registra otro encolador, worker o política de reintentos.
- Antes de copiar/enviar se exige un pedido EC, un archivo de nombre fijo y exactamente 104857600 bytes y ninguna carga incompleta. Para correo: archivo COPIADO y exactamente un aviso PENDIENTE de ENVIAR del admin al único receptor aprobado, cero intentos y sin incertidumbre. Una marca externa se crea de forma exclusiva **antes** de invocar correo; un segundo intento desde este arnés queda bloqueado incluso tras reiniciar. No borrar esa marca para forzar envío. Outbox, fencing y resolución de respuestas siguen siendo los existentes; no se promete entrega exactamente una vez.

### Comprobaciones ejecutadas sin operaciones reales

Comando desde documentos: `powershell.exe -NoProfile -File pruebas/arnes-b3/arnes-b3.ps1 -Accion Comprobar`.

Comando Gradle efectivo desde backend: `gradlew.bat -I ..\documentos\pruebas\arnes-b3\b3.init.gradle b3ComprobarGuardas b3Classpath --offline --console=plain --no-daemon`.

**Salida 0: compilación del arnés y ocho pruebas aprobadas, cero fallos/errores/omitidos.** No se iniciaron Spring, Testcontainers o Google. Las pruebas cubren receptor exacto y rechazo de nulo/otro; confirmaciones que no se deducen de un «sí»; un archivo/un pedido/100 MiB frente a 100 MB; incertidumbre/fallo/segundo intento; ruta relativa o bajo repo; imposibilidad de sobrescribir marca; todos los modos con schedulers false y rechazo de dos adaptadores activos a la vez; y main sin argumentos/consola que no inicia entorno. Los temporales de JUnit son exclusivos y no contienen credenciales ni bytes comerciales.

La primera compilación mostró advertencias por anotaciones API ausentes en el classpath del source set nuevo. Se corrigió **solo el init script del arnés**, usando también testCompileClasspath existente; no se añadió dependencia o exclusión. Las comprobaciones posteriores pasan sin esas advertencias. Se revisaron por lectura scopes, propiedades, cerrojo y exclusividad de volumen antes de la ejecución offline final. No se atribuye a estas guardas validación de OAuth real, montaje Docker, arranque completo o entrega.

Evidencias: [log final](arnes-b3/evidencias/comprobar-final.log), [JUnit](arnes-b3/evidencias/junit/TEST-com.decokasa.ensayob3.GuardasB3Test.xml), [HTML](arnes-b3/evidencias/html/index.html), [conservación](arnes-b3/evidencias/conservacion-final.json), [huellas](arnes-b3/evidencias/sha256.json). El runner sin acción y el parser PowerShell también se comprobaron: acción Estado sin iniciar servicios, cero errores de sintaxis.

### Autorización manual y alcance que queda por ejecutar

Los comandos exactos, creación de raíz UUID con ACL, usuario **b3-admin**, entrada enmascarada y pasos de navegador están en el [README del arnés](arnes-b3/README.md). Tras decidir iniciar el entorno aislado:

1. `-Accion Iniciar -DirectorioEnsayo <raíz privada UUID>` y frase `INICIAR ENTORNO AISLADO 3.B3`; prepara únicamente base/backend aislados con adaptadores apagados.
2. Menú `autorizar`, frase `AUTORIZAR GOOGLE SOLO 3.B3`; abrir `http://localhost:8080/api/v1/salud` en el mismo navegador, usar el snippet enmascarado del procedimiento con b3-admin e iniciar consentimiento de la cuenta empresa. Callback **http://localhost:8080/api/v1/integraciones/google/oauth2/callback**, cliente web, scopes aprobados, modo En pruebas. No iniciar desde curl y trasladar URL a otro navegador. No compartir code/state, headers o tokens. Enter en la consola vuelve a apagar OAuth y no ejecuta copia/correo.
3. Ejecución posterior propuesta, aún no autorizada por este encargo: un archivo ficticio exacto de 100 MiB, un pedido EC con número real, carpeta nueva país/pedido, una pasada manual de copia, una transición válida y un aviso inicial al propio buzón empresa. `copia` y `correo` requieren cada uno su frase explícita. El correo requiere COPIADO confirmado y nunca reintenta automáticamente. No verifica entrega entre buzones ni resuelve reintentos de producción.
4. `salir` o Ctrl+C cierra solo este proceso/contexto/contenedor, manteniendo volumen, archivos, propiedades privadas y tokens cifrados. No compartir toda la raíz: contiene secretos de infraestructura. Exportar solo estados, contadores, horas/hashes y comprobaciones manuales sanitizadas; anexo privado separado para IDs/enlaces reales. No borrar datos ni revocar permisos automáticamente.

**B3-BLOQ-01 queda atendido solo para el receptor del ensayo. B3-BLOQ-02 tiene arnés implementado y guardas comprobadas; el arranque/montaje y la integración real siguen pendientes.** F3-D8 de producción, política F3-D5 de reintentos de correo y hallazgo B3-SEG-01 de cuota agregada continúan abiertos. La limitación de un archivo del arnés es contención del ensayo, no una solución de cuota de producción. Cobertura de archivos histórica **64,83 %** y global completo histórico **80,34 %**, sin medición nueva o reducción de umbral.

No se habilitaron flags de producción ni workers, no se autorizaron cuentas ni hubo subidas/envíos Google, commit, push o cierre de fase. Se conservan las fuentes/configuración/pruebas existentes, Header.tsx, plan, evidencias anteriores e índice Git, con comparación SHA-256.

## Ejecución real autorizada 3.B3 — inicio y bloqueo de puerto, 2026-10-19

Richard autorizó explícitamente iniciar/detener servicios del arnés en la raíz privada `c465a284-f1aa-4b0f-aacd-d77cc3328147`, preparar datos ficticios, habilitar OAuth solo para su consentimiento personal, copiar un archivo de 104857600 bytes y enviar un único aviso del buzón empresa al mismo buzón. Exige conservar guardas y confirmaciones personales, schedulers apagados y detener ante fallo. Esta autorización no incluye detener servicios ajenos ni usar sus bases.

- Docker local 29.8.2 y Java 21 disponibles. La consulta Docker desde sandbox dio acceso denegado; su repetición escalada confirmó el motor, sin iniciar contenedores.
- Se creó exclusivamente `ficticio-3b3-100MiB.bin` en la raíz privada aprobada, con CreateNew y sin sobrescribir. **104857600 bytes; SHA-256 `20492A4D0D84F8BEB1767F6616229F85D44C2827B64BDBFB260EE12FA1109E0E`.** Archivo conservado; no cargado al backend ni a Google.
- Se abrió consola visible del arnés (PowerShell PID 20540, Java hijo 28232), sin redirigir stdin, contraseña, captura o transcripción. Se pidió al usuario introducir personalmente la frase de inicio y elegir la contraseña enmascarada; el agente no introdujo ni recibió contraseña.
- El usuario comunicó que aparecía el menú. La verificación independiente escalada no corroboró un entorno arrancado: **no existe ensayo-privado.properties, tokens-3b3.enc ni marca de correo, y no hay contenedor del ensayo**. El proceso que responde en 8080 es **Java PID 1808, backend Decokasa ajeno al arnés**, escuchando en `::`, no el bind loopback del arnés. La salud pública devuelve ok, pero no se usa como evidencia de este ensayo. La consulta inicial de listeners dentro del sandbox no lo había mostrado: para continuar se requiere verificar puerto/proceso fuera del sandbox.
- Se detuvo exclusivamente el Java hijo y la consola abiertos por el agente, tras comprobar PID/parentesco/comando del arnés y ausencia de configuración privada del ensayo. No se había creado su entorno ni contenedor. El backend ajeno 1808 permanece intacto; no se accedió a datos de negocio o base compartida.
- **Pendiente permiso específico para detener el backend ajeno y liberar el callback fijo.** No se cambia a otro puerto, no se salta la guarda y no se autoriza Google contra un servidor no verificado. Se conservarán las confirmaciones personales al reanudar.

Evidencia inicial: [estado de ejecución](arnes-b3/evidencias/ejecucion-real-estado.json). OAuth, copia, correo y ambos schedulers del ensayo no se activaron. No se obtuvo ni mostró ningún token; no hubo operación Google, git add/commit/push/merge o cierre. Reintentos de correo, cuota agregada y cobertura de archivos siguen pendientes.

### Continuación: liberación de puerto aprobada y confirmación personal pendiente

Richard autorizó expresamente detener el backend Decokasa ajeno PID 1808. Se revalidaron nombre y comando antes de detener exclusivamente ese PID y se comprobó que 8080 quedaba libre; no se alteraron su configuración o datos. No se reinicia ese backend mientras se prepara el ensayo.

Se abrió otra consola visible, titulada «DECOKASA 3.B3 - CONSOLA PRIVADA» (PowerShell 22720, Java hijo 25976), para mantener las guardas personales y la contraseña fuera del control del agente. Tras la respuesta del usuario indicando menú, la verificación escalada seguía mostrando **cero listeners 8080, ningún contenedor del ensayo y ausencia de ensayo-privado.properties y tokens-3b3.enc**. Por ello no se da el arranque por aprobado ni se continúa a OAuth. Se pidió únicamente el texto no sensible de las últimas líneas de esa consola para distinguir la confirmación pendiente de un error. No se introdujo por el usuario ninguna frase mediante automatización, ni se pidió su contraseña.

[Registro de relanzamiento](arnes-b3/evidencias/reinicio-tras-liberar-puerto.json). El archivo ficticio exacto permanece conservado. No hay activaciones o operaciones Google realizadas; se requiere completar la interacción personal verificable antes de proseguir. Las comprobaciones de recepción y las pendientes de cuota, reintentos y cobertura siguen abiertas.

## Cancelación del ensayo real y continuación local — 2026-10-19

Richard canceló el ensayo real **por ahora** y decidió posponer la integración Google. Se detuvieron exclusivamente los recursos propios identificados, sin borrar datos o evidencias. La autorización anterior no se usa para nuevas operaciones remotas; cualquier reanudación requiere una instrucción posterior. El destinatario aprobado solo para 3.B3 no se convierte en destinatario definitivo.

- La consola PowerShell 22720 todavía existía, sin Java hijo: se comprobó comando propio y se cerró únicamente esa consola. No se detuvieron servicios ajenos en esta cancelación. El backend ajeno 1808 se había detenido antes por autorización específica; no se reinició ni se modificó ahora.
- Docker consultado por etiqueta exacta `decokasa.b3=c465a284-f1aa-4b0f-aacd-d77cc3328147`: **cero contenedores activos y cero volúmenes del ensayo inventariados**. No se ejecutaron remove, prune, down -v o borrados. No se había llegado a crear la base del ensayo.
- **No existen ensayo-privado.properties ni tokens-3b3.enc** en la raíz aprobada. No se realizó autorización, carga backend, copia Drive o envío Gmail; tampoco recepción verificada. El archivo ficticio conserva **104857600 bytes y SHA-256 20492A4D0D84F8BEB1767F6616229F85D44C2827B64BDBFB260EE12FA1109E0E**. No se borró ni alteró ninguna evidencia.
- Configuración local conservada: GOOGLE_OAUTH_HABILITADA=false; Drive, Gmail y ambos flags automáticos ausentes y false por defecto. No hay contexto del arnés activo que conserve activaciones temporales. No se cambia producción para cancelar.
- Evidencia sanitizada de parada y conservación: [cancelacion-ensayo-real.json](arnes-b3/evidencias/cancelacion-ensayo-real.json).

**3.B3 y fase 3 permanecen abiertos; integración real POSPUESTA POR DECISIÓN DEL USUARIO, no aprobada ni cerrada.** No se convierte la cancelación en un resultado positivo o negativo de Google.

### Propuestas locales presentadas conjuntamente

Detalle y límites: [fase-3-decisiones-locales.md](fase-3-decisiones-locales.md). No se implementaron código, migraciones, reservas, cuotas o reintentos.

| Pendiente | Recomendación para aprobación |
|---|---|
| Correo, F3-D5 | Inicial + **2 reintentos**, esperas **1 y 5 min** y Retry-After mayor, únicamente para rechazo inequívoco/fallo demostrado previo al POST. Incierto, 5xx/408 o timeout tras iniciar POST: detener sin reenvío automático. Permanentes/autorización: revisión. Conservar misma fila/evento/destinatario/Message-ID, sin promesa de exactamente una vez |
| Cuota, B3-SEG-01 | Punto de partida condicionado a capacidad: **10 GiB globales, 2 GiB por usuario; 2 sesiones activas por usuario y 10 globales**, margen libre **max(1 GiB, 10 % del volumen)**. Reservas atómicas conservadoras de **3 × tamaño declarado**, contando después bytes reales retenidos. Sin borrar parciales/aceptados ni inventar TTL |

Son propuestas, no decisiones aprobadas o requisitos nuevos del manual. Los límites de un único archivo del arnés no resuelven la cuota productiva. La política de retención y la resolución administrativa de resultados inciertos tampoco se deducen de estos números.

Cobertura de archivos continúa en **64,83 % (1012/1561)**, frente al global histórico **80,34 % (3727/4639)** con denominador distinto. No se ejecutaron nuevas suites o mediciones ni se redujo el umbral. Completar cobertura será el siguiente trabajo local que se autorice, después de revisar estas decisiones. Sin git add, commit, push o merge; fase no cerrada.

## Implementación de decisiones provisionales — 2026-10-19

Encargo posterior de Richard: implementar solo reintentos de correo y cuota agregada. Decisiones **provisionales del equipo para desarrollo**, no requisitos definitivos ni aprobación productiva. Se actualiza el plan existente, F3-D5 y F3-D9; no se duplica el plan.

- Correo inicial + **dos reintentos**, bases **1/5 minutos** y Retry-After mayor, solo rechazo transitorio confirmado. Reprogramación persistente con presupuesto máximo en SQL, fencing y transacciones cortas; inciertos y permanentes sin reenvío automático. Se reutiliza el outbox, sin duplicar encolado.
- Cuota lógica configurable **10 GiB globales / 2 GiB por propietario / dos cargas activas personales / diez globales**, registro por UUID de adjunto reservado y cerrojo PostgreSQL. Reserva y sesión atómicas; conversión a final en el commit de adjunto/historial, sin sumar reserva y final. También cubre la copia de aplicación multipart heredada. Pausadas/fallidas conservan reserva y cupo; no se borran bytes por una retención no aprobada ni se cambian propietarios.
- V7 estructura y V8 recuperación histórica, siguientes versiones libres; **escritas, no ejecutadas**. V1–V6 intactas. Nuevos límites no borran datos anteriores por encima de cuota. Error `CUOTA_ALMACENAMIENTO` HTTP 409 incorporado a OpenAPI.
- La cuota es de asignaciones de archivos, **no un límite físico de todos los temporales del volumen**. No se implementan por deducción la antigua reserva 3× o margen de espacio libre. Dimensionamiento físico, temporales del servlet y liberación administrativa de abandonadas pendientes; B3-SEG-01 no se cierra por este código.

Contabilidad y matriz de casos para revalidación/cobertura: [informe de implementación](fase-3-pendientes-locales-implementacion.md). **No se modifican pruebas ni se ejecutan pruebas, builds o migraciones.** No hay comandos Gradle/Maven/Java de compilación/ejecución, acceso a base o API Google en este encargo.

### Evidencia local sin ejecución

1. `git -C .. branch --show-current`: `fase-3-drive-gmail`. No checkout ni otro comando Git de escritura.
2. Lectura del manual DOCX vigente mediante python-docx, plan, arquitectura, persistencia y casos de uso existentes. Preparación y copia restringida a **17 archivos** de backend/plan; comparación de hashes antes y después: **94 archivos protegidos sin cambios**, incluidas pruebas, arnés Java, Header.tsx, índice Git, configuración privada y V1–V6. Archivos aplicados coinciden con los preparados.
3. Lectura limitada de las cinco claves de activación, sin imprimir credenciales: OAuth/Drive/Gmail/automatismos false o ausentes con default false; variables del proceso también false o ausentes. No se inician servicios.
4. `git -C .. diff --check -- backend/src/main/java/com/decokasa/seguimiento/archivos backend/src/main/java/com/decokasa/seguimiento/notificaciones backend/src/main/java/com/decokasa/seguimiento/compartido backend/src/main/resources/application.yml documentos/api/openapi.yaml .claude/PRPs/plans/fase-3-drive-gmail.plan.md documentos/pruebas/fase-3-informe.md documentos/pruebas/fase-3-decisiones-locales.md documentos/pruebas/fase-3-pendientes-locales-implementacion.md`: salida **1**, espacio final preexistente en `documentos/api/openapi.yaml:1425` (`$ref` de Tamano). No se corrige un cambio ajeno para simular salida limpia. Este comando es revisión de texto, no test ni build; no incluye nuevos archivos sin seguimiento.

Evidencia sanitizada: [fase-3-pendientes-locales-evidencia.json](fase-3-pendientes-locales-evidencia.json). La inspección estática no demuestra compilación, comportamiento concurrente o migración correcta: todos los casos nuevos quedan pendientes de ejecución con dobles y almacenamiento exclusivo.

**Cobertura sin medir de nuevo**: 64,83 % de archivos y 80,34 % global son únicamente datos históricos con denominadores distintos; el código nuevo cambia el alcance. Completar B1 con cuotas y revalidar B2 correo en suites existentes, después medir módulo y global sin reducir 80 %. No se cierran defectos nuevos. B1-P01/P02 conservan su cierre histórico. **3.B3 real pospuesto por decisión de Richard; OAuth/workers/schedulers desactivados; fase 3 abierta.**

## Revalidación de pendientes locales — 2026-10-19

**Resultado: APROBADA LOCALMENTE.** Encargo exclusivo de pruebas/cobertura, usando `ecc:springboot-tdd`, `ecc:tdd-workflow` y `ecc:test-coverage`, con revisión Java de los cambios de pruebas. No se modificó producción ni se hicieron commits de checkpoints: prevalece la prohibición expresa de Git de escritura.

### Entorno, aislamiento y ciclo de evidencia

- Rama observada `fase-3-drive-gmail`, sin checkout. Java 21, Gradle wrapper 9.7.1, Spring Boot 4.1.1, PostgreSQL 17 Testcontainers. No instalaciones o cambios de dependencias.
- Cada fixture SQL ligero usa esquema UUID dentro de un contenedor de su clase, sin reuse. Los contextos Spring de cargas se recrean por caso con contenedor propio y nuevo directorio: las reservas retenidas de un caso no contaminan el siguiente. No se elevan cuotas para facilitar tests ni se desactivan triggers para limpiar reservas.
- Runner `ejecutar.ps1` fija OAuth/Drive/Gmail/automatismos/semilla en false, importación privada vacía y DB_URL en 127.0.0.1:1: una prueba sin Testcontainer falla cerrada. Almacenamiento exclusivo temporal por ejecución; ningún dato de desarrollo o del ensayo pospuesto.
- HTTP Google simulado antes de construir los adaptadores. Las pruebas OAuth usan JSON/llave/tokens ficticios propios, HTTP simulado y verificador JWT sustituido: prueban lógica de claims, state y cifrado, **no firma JWK o consentimiento reales**. No se leen credenciales privadas reales. Relojes controlados para esperas/caducidades; concurrencia sincronizada con barreras, sin sleeps de minutos.
- RED inicial: check con 541 casos y 14 fallos, sin defecto productivo nuevo confirmado. Había constantes V6/tablas antiguas, contaminación del cupo global entre fixtures y expectativas antiguas de borrado. Se corrigieron solo fixtures/aserciones: V8/lista exacta, aislamiento de casos y conservación exacta de preparado/reserva según la decisión aprobada. La prueba de vacío/exacto/exceso es ahora parametrizada con propietario independiente por caso: no libera reservas fallidas ni aumenta cupos para permitir el siguiente archivo.
- GREEN focalizado: 60 casos de cuotas/correo/protocolo Gmail/migraciones, sin fallos. GREEN de archivos antes de las últimas seis aserciones: 175 casos y 82,37 % de líneas del módulo. Check intermedio: 662 casos, sin fallos/omisiones, global 90,64 % y archivos 86,34 %. Después se añadieron el caso HTTP de cupo/rollback de inicio y cinco variantes del rechazo Gmail, y se repitió el check oficial completo.

### Comandos y resultados

Desde `documentos`, `& .\pruebas\fase-3-evidencias\revalidacion-local\ejecutar.ps1 <etapa>` prepara el aislamiento y ejecuta desde `backend` el comando literal de cada fila. Los scripts de copia solo admiten fuentes `src/test/java`, comprueban hashes y no son un arnés alternativo de compilación.

| Marca/etapa | Comando Gradle desde backend | Salida | Evidencia |
|---|---|---|---|
| `20261019-102140` · baseline | `.\gradlew.bat check --console=plain --no-daemon` | 1 | [log](fase-3-evidencias/revalidacion-local/20261019-102140-baseline.log) |
| `20261019-102511` · cobertura | `.\gradlew.bat jacocoTestReport -x test --console=plain --no-daemon` | 0 | [log](fase-3-evidencias/revalidacion-local/20261019-102511-cobertura.log) |
| `20261019-103000` · nuevas | `.\gradlew.bat test --tests *CuotasLocalesIT --tests *CorreoReintentosLocalesIT --tests *GmailProtocoloLocalTest --tests *MigracionesArchivosB1IT --console=plain --no-daemon` | 0 | [log](fase-3-evidencias/revalidacion-local/20261019-103000-nuevas.log) |
| `20261019-103212` · archivos | `.\gradlew.bat test --tests com.decokasa.seguimiento.archivos.* --console=plain --no-daemon` | 0 | [log](fase-3-evidencias/revalidacion-local/20261019-103212-archivos.log) |
| `20261019-103920` · check | `.\gradlew.bat check --console=plain --no-daemon` | 0 | [log](fase-3-evidencias/revalidacion-local/20261019-103920-check.log) |
| `20261019-105712` · check | `.\gradlew.bat check --console=plain --no-daemon` | 0 | [log](fase-3-evidencias/revalidacion-local/20261019-105712-check.log) |

Se usó siempre el source set/test/check oficial, **sin el antiguo arnés que excluía notificaciones**. La ejecución de cobertura `jacocoTestReport -x test` solo generó XML a partir del `.exec` recién producido por el baseline; no omitió pruebas de la validación final. La copia automática `*-baseline-jacoco.xml` anterior a esa generación era un XML histórico: no se usa como medición del baseline ni del resultado final.

### Casos cubiertos y evidencia de comportamiento

| Alcance | Suite/casos finales | Garantías comprobadas |
|---|---|---|
| Correo persistente | CorreoReintentosLocalesIT: 16 | Inicial + dos; fechas 1/5 min; Retry-After mayor/menor; reinicio sin reset; terminal tras tres; sin reenvío de permanente/incierto/transitorio genérico; éxito conserva identidad; lease perdido y rechazo tardío no rearman |
| Protocolo Gmail | GmailProtocoloLocalTest: 25 | Rechazos estructurados 403/429 reconocidos, motivos mixtos/desconocidos y códigos discordantes; Retry-After segundos/fecha/pasada/inválida/desbordada; transporte/408/5xx/2xx sin ID inciertos; credencial fallida antes del POST; cero red real |
| Cuotas SQL | CuotasLocalesIT: 19 | Defaults binarios y configuraciones pequeñas; inválidas; límite exacto/byte extra global y personal; dos/diez cupos; pausa/fallo/reinicio; reserva/confirmación idempotente; ajuste multipart; rollback; propiedad inmutable; carreras de bytes/cupos/idempotencia y aborto seguro REPEATABLE READ |
| Carga real aislada | CargasB1IT: 23 | Archivo de 104857600 bytes en 25 bloques; hash/asociación pedido/etapa/entrada/evento; permisos vigentes; reanudación y tail sin confirmar; incompletos no válidos; rollback conserva reserva y bytes; HTTP 409 del tercer inicio no deja sesión; finalizar libera un cupo sin duplicar bytes |
| Migraciones | MigracionesArchivosB1IT: 5; MigracionesIT: 40; V2RestriccionesIT: 42 | V1–V8 desde vacío y reaplicación sin cambios; fase 2 poblada conserva archivo/huella/propietario/relaciones/historial; V6 con completa y parcial recupera exactamente dos asignaciones; exceso histórico no se borra y bloquea nueva admisión |
| Retención multipart | Backend24IT: 47 | Regresión del backend, límites y permisos, archivo preparado/final retenido al fallar registro y reserva pendiente; rollback de negocio/outbox sin avisos enviables |
| Almacén/OAuth local | AlmacenGoogleLocalTest: 7; OAuthFlujoLocalTest: 14 | Cifrado persistente/ACL, corrupción, identidad/propiedad y vencimiento de grant; sesiones Drive ligadas a UUID; callback fijo, metadatos opcionales, state de un uso/nonce/PKCE, cuenta/claims/scopes y renovación simulada sin extender siete días |

También se repitieron las suites existentes de disco/parciales/Drive: recuperación, estados, inicial + tres reintentos de Drive, concurrencia, lease e idempotencia/reconciliación con proveedor simulado. No se añade whitelist de formatos ni interpretación comercial del contenido. Las pruebas de cuota con tamaños agregados grandes son de metadatos; el caso de archivo exactamente 100 MiB sí escribe/verifica bytes reales en disco exclusivo.

### Cobertura final medida — criterio LINE del check vigente

| Alcance | Cubiertas/total | Porcentaje | Objetivo |
|---|---|---|---|
| Backend global completo | 4302/4746 | **90,64 %** | ≥80 % |
| Módulo archivos, paquete raíz y todos sus subpaquetes | 1391/1611 | **86,34 %** | ≥80 % |
| Compatibilidad ampliada: archivos + adaptador de entrada de pedidos para archivos | 1401/1623 | 86,32 % | Sin excluir ese adaptador del global |

Global y módulo son denominadores diferentes; no se promedian ni se suman. Para comparar el mismo código previo a estas pruebas: baseline fresco **3859/4746 = 81,31 % global** y **1039/1611 = 64,49 % archivos**. La lectura inicial por coincidencia de texto incluía además 12 líneas de `pedidos/adapter/in/archivos` (1049/1623 = 64,63 %); el cálculo final delimita el módulo por prefijo y publica también el agregado ampliado. Ningún archivo se retira del reporte global. La antigua medición 64,83 % (1012/1561) corresponde a otro código/denominador y no es la medición actual.

Ramas, informadas sin confundirlas con el umbral LINE: global 69,54 %, archivos 63,93 %. Hay ramas/clases aún por mejorar; superar 80 % de líneas no prueba todas las rutas o la integración externa. El detalle de fuentes bajo 80 % está en el JSON, sin exclusiones o modificaciones de JaCoCo/Gradle.

Evidencia final preservada: [métricas y fuentes con huecos](fase-3-evidencias/revalidacion-local/final-metricas.json), [JaCoCo completo](fase-3-evidencias/revalidacion-local/final-jacoco.xml), [XML JUnit por suite](fase-3-evidencias/revalidacion-local/final-junit/), [medidor reproducible](fase-3-evidencias/revalidacion-local/medir.py). Total final **668 casos / 0 fallos / 0 errores / 0 omitidos**.

### Defectos, conservación y pendientes

No se confirmó un defecto productivo nuevo en los casos ejecutados. Los fallos iniciales documentados eran expectativas/aislamiento de fixtures adaptados al contrato provisional, no correcciones productivas. B1-P01/P02 conservan su cierre histórico y fueron ejercitados por la suite completa, sin cambiar su implementación.

Comparación SHA-256: **317 archivos protegidos sin cambios**, incluyendo todo `backend/src/main`, `build.gradle`, Header.tsx, índice Git y configuración privada. Solo cambian pruebas y documentación/evidencias. No se modifica el umbral ni se crean exclusiones de código. Contextos/testcontainers propios terminan con las suites; no se detienen servicios ajenos ni se hace limpieza de volúmenes compartidos.

**La cuota sigue siendo lógica.** Representaciones parcial/final, temporales de preparación, spool servlet/proxy y huérfanos físicos no tienen un límite físico agregado garantizado por esta implementación. Un volumen de 10 GiB no queda validado como suficiente. B3-SEG-01 permanece abierto para dimensionamiento/espacio físico y revisión operativa; no se aprueban por deducción reserva 3×, margen libre, borrado o TTL. Las cargas fallidas conservan reserva/cupo; la recuperación/liberación administrativa de abandonadas continúa pendiente.

Las decisiones provisionales de reintentos/cuota quedan **revalidadas localmente para los casos ejecutados**, sin convertirlas en política definitiva de producción. Destinatarios definitivos, resolución administrativa de incertidumbre, retención y validación externa siguen pendientes. **OAuth, workers y schedulers desactivados; sin Google real, bases compartidas o Git de escritura. 3.B3 real sigue pospuesto por decisión de Richard y la fase 3 abierta.**

## Revalidación V16 y recorrido real A1–Fin — 2026-10-21

Esta entrada actualiza los estados históricos anteriores sin borrar sus evidencias. Richard confirmó A6 con Packing List, A9 con bodega activa y algún documento, y A10 con documento y Fin automático. V16 y la verificación local acreditan esas reglas: A6/A9/A10 ya no quedan bloqueadas por una definición documental pendiente. Los plazos pendientes y la meta de días propuesta siguen sin resolverse; esto no cierra fases.

[Informe detallado, comandos y matriz](fase-5-evidencias/V16-flujo-A/README.md), [métricas reales](fase-5-evidencias/V16-flujo-A/metricas-finales.json), [conservación y parada](fase-5-evidencias/V16-flujo-A/conservacion-final.json).

- Backend: check aprobado, **851/851**, PostgreSQL Testcontainers aislado; cobertura global de líneas **91,28 %**, archivos **86,34 %**, plazos **95,12 %**. Migración V16 sobre base vacía y V15 con pedido esperando en A6, documentos e historial conservados. Se adaptaron expectativas antiguas sin retirar comprobaciones de esquema ni comportamiento.
- Frontend: lint, **324/324** y build aprobados; cobertura global de líneas **67,32 %**. El objetivo adicional ≥80 % sigue pendiente, sin exclusiones nuevas ni reducción de umbrales.
- API real y navegador: recorrido A1–Fin con rechazo, ambas devoluciones, reentradas, demora, llegada, bodega, documentos reales en almacenamiento aislado e historial. Packing List sola y con adjuntos adicionales; rechazos de documentos ajenos, ausentes y corruptos; algún documento obligatorio en A9/A10; Fin automático del sistema sin acciones manuales. Contrato y pantallas coherentes con exigeAlgunDocumento.
- Dos sesiones reales actualizadas por polling de 30 segundos: ambas comprobadas a los **30060 ms**, sin reloj simulado. No equivale a “al instante” ni aceptación del cliente. Siete pedidos en esta API; máximo observado 11 por base de suites, límite autorizado 20. No se repitió la carga de 500 ni se retiró por inferencia el objetivo original del manual.

Los fallos iniciales de expectativas obsoletas y selector del arnés permanecen registrados; las revalidaciones posteriores aprobaron. No se identificó un defecto de producción nuevo en este alcance. Producción, Header.tsx e índice Git conservados por hashes. API, frontend y control locales detenidos, sin bases compartidas ni borrado de volúmenes. No hubo Google real.

Continuación del arnés 3.B3 existente preparada **sin ejecutarla**: comparación de rama cambiada únicamente a fase-5-seguimiento por aprobación expresa de Richard; rechaza una rama ajena. Ocho guardas aprobadas y classpath actualizado, con evidencias anteriores conservadas. Solo se autoriza UN archivo ficticio de **104857600 bytes** y UN correo del buzón aprobado a sí mismo; no el outbox ni todos los adjuntos del recorrido. El inicio, autorización, copia y correo conservan sus confirmaciones personales. El consentimiento solo corresponde después de elegir autorizar y confirmar su guarda en consola. Recepción requiere confirmación de Richard, no basta aceptación de Gmail.

3.B3 real continúa sin ejecutar ni aprobar; fases 3, 4 y 5 abiertas. OAuth, workers, schedulers y detección automática permanecen desactivados fuera de la activación aislada que todavía no se ha iniciado. Sigue explícita la limitación de espacio físico temporal: las cuotas lógicas no garantizan capacidad de disco. Se mantiene la distinción entre recomendaciones, decisiones provisionales del equipo y requisitos aceptados por el cliente.
