# Fase 4: verificación local

**Estado vigente (2026-10-20): F4-H01 cerrado; completados doce casos activos de historial/recuperación, con 66/66 pruebas afectadas y tipos aprobados.** Última suite frontend completa anterior: 312 pruebas, lint y build aprobados. **No se volvió a medir cobertura**: la última global sigue en **65,82 % (1202/1826), objetivo adicional ≥80 % pendiente**; la parcial anterior de ocho módulos fue 97,65 % de líneas, sin sustituir la global. Producción, Header.tsx, prototipos, demo, exclusiones y umbrales intactos. No se repitió backend: su última evidencia sigue siendo 783 casos y 90,99 % global, archivos 86,34 %. **A6/A9/A10 conservan sus bloqueos; 4.B2 real bloqueado; 3.B3 pospuesto; fases 3 y 4 abiertas.** La última sección detalla esta ejecución limitada.

## Histórico: verificación hasta 4.4 y revalidación de F4-P01

**Estado histórico tras revalidación (2026-10-19): F4-P01 revalidado y cerrado.** Check aprobado: 761 casos, cero fallos, errores u omitidos; cobertura global de líneas 90,92 %. F4-H01 sigue pendiente de decisión y no aceptado. Los bloqueos A6/A9 se conservan y la fase permanece abierta. Los resultados históricos siguientes se conservan para trazabilidad.

Fecha: 2026-10-19. Rama: `fase-4-etapas-a5-a11`. Alcance exclusivo: backend y datos de 4.1–4.4. **Fase abierta; no se implementaron ni verificaron 4.5/4.6.** La integración real 3.B3 con Google sigue pospuesta por decisión de Richard.

**Resultado de la verificación anterior: check NO aprobado.** 739 casos, 738 aprobados, un fallo reproducible (F4-P01), cero errores y cero omitidos. Compilación real aprobada en aquella ejecución. Cobertura global de líneas: **90,98 %**; superar cobertura no convierte el check en verde. F4-H01 permanece pendiente y no aceptado.

**Actualización posterior, 2026-10-19:** aplicada exclusivamente la corrección localizada de F4-P01, **pendiente de revalidación**. No se modificaron pruebas ni se ejecutaron pruebas, builds o migraciones durante esta corrección. Los 739 casos, la compilación y los porcentajes anteriores corresponden al código **anterior a la corrección** y no certifican esta versión. F4-H01 sigue separado y pendiente; A6/A9 conservan sus bloqueos documentales. Véase la última sección.

## Fuentes y decisiones respetadas

- [Manual vigente](../manual-paso-a-paso.docx), [plan](../../.claude/PRPs/plans/fase-4-etapas-a5-a11.plan.md), [arquitectura](../arquitectura.md), [contrato](../api/openapi.yaml) y [ADR-0018](../adr/0018-etapa-de-llegada-explicita.md).
- Skills aplicadas: `ecc:springboot-tdd`, `ecc:tdd-workflow`, `ecc:jpa-patterns` y `ecc:java-reviewer`. Spring Boot 4.1.1, Java 21, Gradle 9.7.1; pruebas JUnit/MockMvc, PostgreSQL 17 mediante Testcontainers y JaCoCo. Prevalece la prohibición de modificar producción: no se hizo una implementación para convertir artificialmente un reproductor rojo en verde.
- F4-D1/D2: Logística o admin vigentes mantienen bodegas; asignación única en APROBAR de A9. El JWT deliberadamente obsoleto no concede permisos nuevos.
- F4-D3/D6: Fabricación y Aduana conservan plazo `PENDIENTE`. Embarque: diez días hábiles; Salida y Bodega: 24 horas hábiles. Calendario provisional: lunes a viernes, todas sus horas, Ecuador, sin inventar feriados.
- **F4-D4/D7 siguen pendientes:** A6 y A9 conservan `PENDIENTES` y rechazan avanzar. No se eligió una interpretación de Packing List/BL ni una lista de documentos de Bodega.
- F4-D5 provisional: ajustar primero al lunes, comprobar hoy de Ecuador y una estimación efectiva estrictamente posterior a la vigente. No cambia vencimientos ni fechas planificadas; el arrastre del cronograma corresponde a fase 5 y no se declara implementado.
- ADR-0018: la llegada depende de la marca versionada, no del número de etapa ni de admitir REPORTAR_DEMORA.

## Aislamiento y conservación

Cada suite utiliza su propio contenedor PostgreSQL, sin reutilización de bases. Los casos de migración reutilizan `DatosB1`, con un esquema UUID exclusivo por caso. Los archivos van a directorios temporales exclusivos; se utilizan usuarios, nombres y bytes ficticios. No se usa ninguna cuenta de Google ni se leen sus credenciales.

El [ejecutor](fase-4-evidencias/ejecutar.ps1) fija OAuth, Drive, Gmail, copia automática, correo automático y semilla en `false`; elimina la importación de configuración local y fija una URL de base inválida como barrera si faltara Testcontainers. No se inician workers o schedulers reales. Se conservan los archivos temporales: no se ejecutaron borrados de volúmenes, `clean` de Flyway ni operaciones de limpieza de Docker.

Los [hashes iniciales](fase-4-evidencias/originales-sha256.json) incluyen todo `backend/src/main`, `backend/build.gradle`, el índice Git, `Header.tsx` y `.env.local` cuando existe. La comparación final está en las métricas. No se ejecutó `git add`, commit, push ni merge; tampoco se modificaron el manual, el plan, las decisiones ni las evidencias de otras fases.

**Limitación física que permanece:** la cuota lógica de fase 3 contabiliza archivos/reservas; no constituye una cuota del espacio físico de temporales. Esta verificación no la resuelve ni impone retención automática.

## Pruebas y distinción entre producto y fixtures

Se ampliaron suites existentes y se agregaron únicamente tres suites con alcance distinto:

| Suite | Comprobaciones |
|---|---|
| `MigracionesIT`, `V2RestriccionesIT` | Expectativas explícitas de versión final V12 y conjunto exacto de tablas, sin retirar restricciones previas. |
| `MigracionesArchivosB1IT` | Su caso histórico de fase 3 queda expresamente acotado a V8; conserva el resto de comprobaciones de compatibilidad. |
| `MigracionesFase4IT` | V1–V12 en vacío, validación/reaplicación; V3 y V8 con datos preservados al pasar secuencialmente por V9, V10, V11 y V12; bodegas iniciales, unicidad/normalización, no borrado, CHECK de demora/llegada, marca única/no automática y SQLSTATE UB001/UB002. |
| `Backend24IT` | Conserva el recorrido y rechazos previos; A5 con observación ahora responde 200 y llega a A6. Allí comprueba el bloqueo documental; un fixture SQL explícito de A10 comprueba el bloqueo real de alcance y ausencia de mutaciones. La igualdad estricta de campos de historial incluye los campos nuevos sin quitar los antiguos. |
| `Fase4IT` | Catálogo por HTTP: roles y cuentas vigentes, duplicados, cambios de versión, validaciones, desactivación y carreras. A5→A6; bloqueos reales A6/A9; demora y fechas estrictas; historial/atomicidad; llegada versionada; A8→A9; asignación con documento ficticio, permisos, ID activo, unicidad, concurrencia, UB001 traducido y rollback. |
| `Fase4ReglasTest` | Reloj controlado cerca de medianoche en Ecuador, ajuste antes de comprobar hoy, fecha hábil sin fecha solicitada distinta, validaciones puras de marca de llegada. |
| Arquitectura existente | R1–R4/R9 y fronteras P20. Se extiende la comprobación R4 de pedidos a bodegas/archivos y la de bodegas hacia usuarios, sin dependencia inversa de pedidos. |

Los pedidos de A7/A8/A9 se insertan como **fixtures de estado aislados**. No se los presenta como una navegación real desde A1. Para las reglas posteriores al bloqueo documental de A9 se crea una **versión ficticia no vigente**, con sus propios UUID y un requisito `Documento ficticio F4`; se carga ese archivo por la API y se presenta su identificador real. La versión vigente permanece intacta, con A6/A9 pendientes. Un caso específico demuestra que, incluso en el fixture resuelto, faltar el documento devuelve 422.

Las carreras usan transacciones y bloqueos reales. Un contenedor comprueba mediante `pg_blocking_pids` que la otra transacción está esperando antes de confirmar; no se sustituyen por pausas arbitrarias. Se cubren ambos ganadores: desactivación primero → 400 y rollback completo del pedido; asignación primero → la desactivación espera y la asignación permanece aun después de desactivar. Dos aprobaciones del mismo pedido → una 200 y otra 409, un único evento de asignación.

Los fallos de historial son **inyectados** mediante triggers exclusivos del pedido ficticio. La respuesta esperada es 500 sanitizada; la garantía que se comprueba es que no queden llegada, demora, bodega, avance, entrada, evento ni intención de correo confirmados parcialmente. No son defectos de producción ni se exige que MockMvc propague una excepción que el manejador HTTP transforma en 500.

## Ejecuciones y resultados

1. `& .\documentos\pruebas\fase-4-evidencias\ejecutar.ps1 baseline`, desde la raíz (equivale a `backend\gradlew.bat check --console=plain --no-daemon`). [Log inicial](fase-4-evidencias/20261019-144906-baseline.log), [metadatos](fase-4-evidencias/20261019-144906-baseline.json), [JUnit inicial](fase-4-evidencias/baseline-junit/).
   - **627 casos ejecutados, siete fallos**, incluido el fallo de inicialización de `V2RestriccionesIT` que impide ejecutar sus casos. Fallos por expectativas antiguas V8, ausencia de `bodega`, rechazo de A5 y forma anterior de `DetalleEvento`.
   - `compileJava` y `compileTestJava` se ejecutaron correctamente. Hay dos avisos existentes sobre recursos `bloqueo` no referenciados en `CargasConfig`, y el aviso de la JVM sobre class sharing.
   - Esto es evidencia de compilación **real de esta ejecución**; la revisión estática anterior del plan nunca demostró compilación.
2. `& .\documentos\pruebas\fase-4-evidencias\ejecutar.ps1 fase4`: [log del primer arnés](fase-4-evidencias/20261019-145328-fase4.log), [metadatos](fase-4-evidencias/20261019-145328-fase4.json), [JUnit](fase-4-evidencias/arnes-inicial-junit/).
   - 160 casos, seis fallos: uno de producto (F4-P01) y cinco de preparación/aserción del arnés: el marcador de creación de esquema de Flyway no tiene versión; el manejador devuelve 500 en lugar de propagar tres fallos SQL inyectados; y la API invierte la lista del repositorio. Se corrigieron esas cinco aserciones según el contrato y la interfaz observada, manteniendo las comprobaciones de rollback y el reproductor 400 del defecto real. No se tocó producción.
3. Check completo con las pruebas finales y medición de cobertura: resultados finales y comandos exactos en los archivos de evidencia y en la actualización al final de este informe. **No se declara aprobado el check si subsiste F4-P01.**

## Defectos y reproductores para producción

### F4-P01 — Fecha de demora en arreglo JSON aceptada indebidamente (cerrado tras revalidación; véase la última sección)

Contrato y decisión del paso 4.3: `nuevaFechaEstimadaLlegada` es una **cadena ISO estricta**, y los arreglos deben responder 400 sin modificaciones.

Reproductor aislado: `Fase4IT.demoraRechazaFechasNoIsoEstrictas`, parámetro `[2026,10,17]`. El fixture crea un pedido en A7 con la definición real, actor vigente y reloj `2026-10-16T22:00:00Z`.

```http
POST /api/v1/pedidos/{id-del-fixture}/acciones
Content-Type: application/json

{"accion":"REPORTAR_DEMORA","etapaEsperada":7,"observacion":"Demora","nuevaFechaEstimadaLlegada":[2026,10,17]}
```

Esperado: 400 `Problema`, sin actualizar pedido ni historial. Observado: **200**; se interpreta el arreglo como fecha y se registra la demora ajustada al lunes 19. El camino de deserialización de `LocalDate` admite el arreglo pese a `@JsonFormat(shape=STRING, pattern="uuuu-MM-dd", lenient=FALSE)` en `SolicitudAccion`. Es una discrepancia de tipo/contrato; no se ha demostrado omisión de las validaciones de fecha efectiva o permisos.

Comando reducido desde `backend`, usando las mismas variables de aislamiento del ejecutor:

```powershell
.\gradlew.bat test --tests '*Fase4IT.demoraRechazaFechasNoIsoEstrictas' --console=plain --no-daemon
```

En aquella ejecución se conservó la expectativa 400 sin corregir producción, omitir la prueba ni reducir el umbral. La corrección posterior se documenta al final; aún falta revalidar deserialización estricta, rechazo sin persistencia y compatibilidad de fechas ISO válidas.

### F4-H01 — Empate de eventos no conserva secuencia causal (pendiente, no aceptado)

`Pedido.ejecutar` agrega primero `BodegaAsignada` y después `EtapaAprobada` a sus eventos, ambos con una lectura del reloj. `RegistrarEventoPedidoCasoDeUso` asigna UUID aleatorio a los que no traen ID. `HistorialJpaRepositorio` consulta por `ocurrido_en ASC, id ASC`; `ConsultarHistorialPedidoCasoDeUso` invierte la lista para entregar recientes primero. El comentario de ese caso de uso dice «orden de inserción», pero la consulta no garantiza tal orden para horas iguales.

Reproductor determinista, sin depender de que varias ejecuciones aleatorias tengan suerte: `Fase4IT.historialPuedeInvertirElOrdenCausalPorDesempateUuid`. Inserta BODEGA_ASIGNADA con UUID alto y luego ETAPA_APROBADA con UUID bajo, a la misma hora. El repositorio los ordena aprobación/asignación y la API los devuelve asignación/aprobación; al invertir los UUID puede cambiar el orden visible sin cambiar la secuencia de inserción. La prueba demuestra el comportamiento técnico; **no lo aprueba como requisito funcional**.

Impacto: el historial puede sugerir que la asignación ocurrió después de la aprobación, aunque el dominio emitiera los eventos en la secuencia opuesta. La secuencia dentro de la operación no se puede reconstruir a partir de hora+UUID. No se observó pérdida ni duplicación de eventos, avance doble o pérdida de atomicidad. BODEGA_ASIGNADA no encola otro correo: el outbox selecciona eventos de transición. El resumen de cierre filtra ETAPA_APROBADA y no trata BODEGA_ASIGNADA como cierre; por ello no se atribuye un fallo de resumen a este par concreto. Otros empates entre varios cierres necesitarían una comprobación independiente.

La afirmación «aceptable» del plan **no se adopta**. Debe decidirse una garantía de orden (y su representación persistente si se necesita). Un UUID v7 por sí solo no demuestra secuencia para dos eventos en el mismo intervalo. No se modificaron producción, migraciones ni el plan para resolverlo.

## Bloqueos y alcance pendiente

- F4-P01 mantiene rojo el check hasta una corrección posterior de producción.
- F4-H01 sigue pendiente de decisión; no se cierra por tener un reproductor determinista.
- F4-D4: Packing List y BL, o basta uno. F4-D7: documentos obligatorios de Bodega. Ambos continúan bloqueando el producto real. No se declara validado un flujo A1→A10 completo.
- Confirmación del cliente de decisiones provisionales y calendario/feriados; plazos pendientes de Fabricación/Aduana.
- 4.5 (Incidencias/Fin) y 4.6 (pantallas) fuera del encargo. Cronograma/arrastre de fase 5, Google real y 3.B3 no comprobados aquí.
- La fase 4 permanece abierta; esta entrega no es autorización de cierre.

## Resultado final y cobertura medida

Check final: [comando y salida](fase-4-evidencias/20261019-145638-check.log), [metadatos](fase-4-evidencias/20261019-145638-check.json). `check --console=plain --no-daemon` terminó con código **1**, en 3 min 56 s: **739 casos / un fallo / cero errores / cero omitidos**. `Backend24IT`: 48/48; `MigracionesFase4IT`: 7/7; `Fase4ReglasTest`: 4/4; `Fase4IT`: 57/58; arquitectura: 11/11. Se añadieron 71 casos respecto de las 668 pruebas previas, sin duplicar las suites anteriores. Las tres comprobaciones de rollback con fallos SQL inyectados y ambas carreras de bodega pasaron.

Como el fallo de pruebas impidió finalizar normalmente la cadena de `check`, se ejecutó **después**, sin repetir ni omitir un caso dentro de la ejecución completa ya registrada:

```powershell
# Rutas absolutas: funcionan aunque el ejecutor anterior deje la ubicación en backend.
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\ejecutar.ps1' check
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\ejecutar.ps1' cobertura
# El ejecutor deja la ubicación en backend; se usa una ruta absoluta para medir.
& 'C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\medir.py'
```

El modo `cobertura` ejecutó exactamente `gradlew.bat jacocoTestReport jacocoTestCoverageVerification -x test --console=plain --no-daemon`: [log](fase-4-evidencias/20261019-150058-cobertura.log), [metadatos](fase-4-evidencias/20261019-150058-cobertura.json). **Código 0, BUILD SUCCESSFUL en 7 s.** `-x test` evita repetir la ejecución recién terminada: se utilizan sus datos reales completos de JaCoCo, incluido el caso fallido; no se eliminaron clases, suites, contadores ni comprobaciones. Esta ejecución separada valida el umbral, **no aprueba `check`**.

Hubo un error auxiliar al invocar inicialmente el medidor con ruta relativa después del ejecutor: buscó el script dentro de `backend`. Se repitió únicamente el lector de XML con ruta absoluta; no se repitieron pruebas para ocultar el fallo. El lector normaliza separadores de Windows al comparar inventarios, conservando el archivo de hashes original.

| Alcance completo, incluyendo todos sus subpaquetes | Líneas cubiertas/total | Líneas | Ramas |
|---|---:|---:|---:|
| Backend global | 4659/5121 | **90,98 %** | 71,30 % |
| Bodegas | 148/155 | **95,48 %** | 80,65 % |
| Pedidos | 1131/1195 | **94,64 %** | 77,49 % |
| Flujos | 224/240 | **93,33 %** | 82,83 % |
| Historial | 313/322 | **97,20 %** | 80,00 % |
| Archivos (sin sumar adaptadores externos al módulo) | 1391/1611 | **86,34 %** | 63,93 % |

Las filas por módulo incluyen código previo y nuevo; no representan «cobertura exclusiva del diff de fase 4». El umbral de Gradle continúa siendo **80 % LINE global**, sin modificación. Las ramas se informan separadamente; no se las confunde con ese umbral. La lista de fuentes por debajo del 80 % y los denominadores están en [métricas completas](fase-4-evidencias/final-metricas.json). Quedan ramas no ejercitadas; cobertura alta no demuestra un recorrido completo bloqueado ni integración externa.

Evidencia conservada: [JaCoCo XML](fase-4-evidencias/final-jacoco.xml), [JUnit completo final](fase-4-evidencias/final-junit/), [medidor reproducible](fase-4-evidencias/medir.py). Los **340 archivos protegidos conservan su SHA-256**, sin archivos de producción añadidos o eliminados durante esta verificación. `git diff --check -- backend/src/test` devolvió código 0; el escaneo local de evidencias/pruebas nuevas no encontró patrones de claves privadas ni tokens Google. No se imprimieron ni copiaron secretos.

**Entrega:** ocho rutas de pruebas nuevas/adaptadas, este informe y evidencias. F4-P01 se devuelve con reproductor para corrección posterior de producción. F4-H01 queda como hallazgo de orden no aceptado; F4-D4/D7 siguen bloqueando el producto. OAuth, workers y schedulers reales desactivados, sin Google ni bases compartidas. **3.B3 pospuesto; fase 4 abierta.**

## Corrección localizada de F4-P01 — pendiente de revalidación

Encargo posterior: corregir únicamente la aceptación de un arreglo JSON en `nuevaFechaEstimadaLlegada`, sin modificar pruebas ni ejecutar pruebas, builds o migraciones. Skills: `ecc:springboot-patterns` y `ecc:java-reviewer`, aplicadas como revisión estática; la prohibición expresa de ejecución prevalece sobre el comando de build sugerido por la skill.

**Causa:** `@JsonFormat(shape=STRING, pattern="uuuu-MM-dd", lenient=FALSE)` no impidió que el deserializador de `LocalDate` aceptara el token de arreglo. El reproductor anterior respondió 200. Declarar el formato no era una barrera suficiente para el tipo JSON de entrada.

**Cambios exclusivos de producción:**

- [SolicitudAccion.java](../../backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/web/SolicitudAccion.java): sustituida la anotación de formato de ese campo por `@JsonDeserialize(using = FechaEstimadaLlegadaDeserializer.class)`, manteniendo su tipo `LocalDate` y las firmas existentes.
- [FechaEstimadaLlegadaDeserializer.java](../../backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/in/web/FechaEstimadaLlegadaDeserializer.java): deserializador del adaptador web, sin acceso a repositorios, reloj, negocio o estado mutable. Primero exige `VALUE_STRING`; luego exactamente cuatro dígitos ASCII, guion, dos dígitos, guion y dos dígitos; finalmente `LocalDate.parse` con `ISO_LOCAL_DATE`, de resolución estricta. No recorta espacios ni convierte otros tipos. Arreglos, números, objetos, booleanos, texto con hora y fechas imposibles se rechazan mediante `reportInputMismatch`.

El `null` explícito también se rechaza como entrada de tipo incorrecto. Se diferencia del **campo ausente**: `getAbsentValue` conserva su ausencia como `null` Java, para no exigir la fecha en acciones que no la usan ni convertir la falta de fecha de REPORTAR_DEMORA en otra regla (continúa el requisito de dominio 422). `getNullAccessPattern` dinámico evita evaluar ese rechazo al construir el deserializador.

No se cambió el mapper, sus módulos ni ninguna configuración global de Jackson. Se consultaron únicamente las fuentes de las dependencias locales Jackson 3.1.5 para revisar las firmas de API. No se compiló ni ejecutó el código.

**Ruta prevista del error, revisada estáticamente:** el error de mapeo se produce al leer el `@RequestBody`, antes de invocar `AccionEtapaController.ejecutar` y la transacción del caso de uso. Spring MVC lo convierte en error de lectura 400; el `ManejadorErrores` existente conserva `ProblemDetail`, `application/problem+json` y `codigo=VALIDACION`. No se agregó otro manejador ni se expone en el mensaje el contenido recibido. La ausencia de cambios en pedido/historial y el formato efectivo de respuesta **deben comprobarse en revalidación**, no se dan por probados por esta lectura.

**Conservación:** comparación SHA-256 de todos los archivos existentes de `backend/src/main`, `backend/src/test`, Gradle, índice Git y `Header.tsx`: el único archivo existente cambiado para esta corrección es `SolicitudAccion.java`; se añade únicamente el deserializador indicado. Las pruebas previas y sus evidencias quedan intactas. No se modifica la emisión/orden de eventos, la definición de flujo, los requisitos documentales ni las migraciones. Sin Google, activaciones ni comandos Git de escritura.

**Casos pendientes de revalidar, sin ejecutarlos en esta corrección:**

1. Repetir el reproductor `[2026,10,17]` y exigir 400 en formato `Problema`, `application/problem+json`, `codigo=VALIDACION`, y comparación completa antes/después de pedido e historial (sin demora/evento nuevos).
2. Rechazar asimismo arreglo vacío, arreglo con una cadena ISO, arreglos anidados, número entero/decimal, objeto, booleano y `null` explícito, sin mutaciones.
3. Rechazar `""`, espacios iniciales/finales, anchuras distintas de AAAA-MM-DD, fecha con hora/zona, día/mes fuera de rango, `2026-02-29` y `2026-02-30`, con la misma respuesta y atomicidad.
4. Aceptar una cadena ISO válida y un 29 de febrero de año bisiesto con reloj/fixture adecuados a las reglas de demora; conservar ajuste de fin de semana, hoy de Ecuador y estimación estrictamente posterior.
5. Campo ausente: REPORTAR_DEMORA mantiene 422 por requisito faltante; acciones que no requieren fecha siguen funcionando sin ella. Mantener la validación de fecha enviada con otra acción.
6. Repetir después el arnés de fase 4 y `check`, midiendo nuevamente cobertura y arquitectura. La compilación y cobertura anteriores no se atribuyen a esta corrección.

**Estado:** F4-P01 no cerrado; corrección aplicada pendiente de revalidación. F4-H01 continúa pendiente de decisión, sin aceptación del orden observado. F4-D4/F4-D7 siguen bloqueando A6/A9 en el producto. **3.B3 pospuesto; fase 4 abierta.**


## Revalidación de F4-P01 — 2026-10-19

**Resultado: corrección revalidada; se cierra únicamente F4-P01.** Se amplió la prueba existente de `Fase4IT`, sin crear una suite duplicada ni modificar producción. Las comprobaciones previas siguen presentes. PostgreSQL 17 Testcontainers y disco exclusivos; reloj controlado, usuarios y archivos ficticios. El [ejecutor aislado](fase-4-evidencias/revalidacion-f4p01/ejecutar.ps1) mantiene OAuth, Drive, Gmail, workers automáticos y semilla desactivados, sin importar configuración privada ni usar bases compartidas. No se realizaron operaciones Google ni comandos Git de escritura.

### Casos comprobados

- **23 entradas inválidas:** reproductor `[2026,10,17]`, arreglo vacío, arreglo con cadena ISO, arreglo anidado, números entero/decimal, objetos, booleanos, null explícito, cadena vacía, espacios sobrantes, anchuras incorrectas, fechas imposibles/no bisiestas, día/mes inválidos y cadenas con hora/zona.
- Cada caso exige **400**, `application/problem+json`, `status=400`, `codigo=VALIDACION`, título y detalle no vacíos. Se compara el estado completo antes/después de pedido, etapas, entradas, historial y outbox. Las aserciones se agregan con `assertAll` para comprobar ausencia de mutaciones aunque falle otra comprobación.
- **Cuatro fechas válidas:** día hábil, sábado y domingo ajustados al lunes, y `2028-02-29`. Se comprueban fecha efectiva, actor, único evento de demora, fecha solicitada cuando difiere y conservación de las etapas.
- Campo ausente: REPORTAR_DEMORA conserva 422 y no cambia nada. APROBAR de A5 sin fecha sigue permitido con observación; enviar una fecha con esa otra acción responde 400 sin cambios.
- Se repiten las reglas anteriores de estimación estrictamente posterior, hoy de Ecuador, ajuste previo al lunes, historial y rollback. Los pedidos posteriores a A6 siguen siendo fixtures explícitos, no un recorrido completo que evite los bloqueos documentales.

### Comandos, resultados y evidencia

Se ejecutaron mediante las mismas guardas del ejecutor:

```powershell
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-f4p01\ejecutar.ps1' fechas
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-f4p01\ejecutar.ps1' check
& 'C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-f4p01\medir.py'
```

El modo `fechas` ejecuta `gradlew.bat test --tests '*Fase4IT.demora*' --tests '*Fase4IT.fecha*' --tests '*Fase4ReglasTest' --console=plain --no-daemon`. **41 casos, cero fallos/errores/omitidos**, incluyendo siete reglas ArchUnit descubiertas por su motor. [Log aprobado](fase-4-evidencias/revalidacion-f4p01/20261019-151544-fechas.log), [metadatos](fase-4-evidencias/revalidacion-f4p01/20261019-151544-fechas.json), [JUnit dirigido](fase-4-evidencias/revalidacion-f4p01/fechas-final-junit/).

Se conserva también el [primer intento](fase-4-evidencias/revalidacion-f4p01/20261019-151256-fechas.log) y su [JUnit](fase-4-evidencias/revalidacion-f4p01/fechas-inicial-junit/): 41 casos y 23 fallos debidos a una aserción añadida incorrectamente en el arnés, que exigía `type`. El contrato OpenAPI de `Problema` exige **title/status/codigo**; `type` es opcional y RFC 9457 permite omitirlo. Ya se observaban 400, tipo de contenido y código correctos. Se corrigió únicamente esa exigencia ajena al contrato y se reforzó la comparación independiente de estado; no se debilitó ningún requisito aprobado ni se modificó producción.

Check completo: **761 casos / cero fallos / cero errores / cero omitidos, código 0**. Se ejecutó `.\gradlew.bat check --console=plain --no-daemon`, incluyendo pruebas, verificación del umbral y reporte JaCoCo. [Log](fase-4-evidencias/revalidacion-f4p01/20261019-151645-check.log), [metadatos](fase-4-evidencias/revalidacion-f4p01/20261019-151645-check.json). La compilación real de la corrección se ejecutó en el primer arnés; las posteriores tareas UP-TO-DATE reutilizan exactamente esas fuentes, sin atribuir compilación a una lectura estática.

| Alcance completo | Líneas cubiertas/total | Líneas | Ramas |
|---|---:|---:|---:|
| Backend global | 4668/5134 | **90,92 %** | 71,38 % |
| Bodegas | 148/155 | 95,48 % | 80,65 % |
| Pedidos | 1140/1208 | 94,37 % | 77,84 % |
| Flujos | 224/240 | 93,33 % | 82,83 % |
| Historial | 313/322 | 97,20 % | 80,00 % |
| Archivos | 1391/1611 | 86,34 % | 63,93 % |

La medición procede de la ejecución completa recién aprobada, sin exclusiones ni cambios del umbral **80 % LINE global**. Módulos completos, no exclusivamente el diff de fase 4; las ramas se informan por separado. [Métricas y suites](fase-4-evidencias/revalidacion-f4p01/final-metricas.json), [JaCoCo](fase-4-evidencias/revalidacion-f4p01/final-jacoco.xml), [JUnit completo](fase-4-evidencias/revalidacion-f4p01/final-junit/). Las evidencias anteriores no se sobrescriben.

Los **341 archivos protegidos** conservan sus hashes: producción, migraciones, Gradle, índice Git y `Header.tsx`; ningún archivo de producción se añadió o eliminó. Solo se modificó la prueba existente `Fase4IT` y se añadieron esta documentación y evidencias. `git diff --check` de la prueba devolvió 0.

**Pendientes conservados:** F4-H01 (orden de eventos) no se acepta ni se cierra por pasar el check. F4-D4/F4-D7 mantienen los bloqueos documentales de A6/A9 y se repiten en la regresión. La limitación del espacio físico temporal frente a cuotas lógicas sigue explícita. Sin avances a 4.5/4.6, sin Google; 3.B3 continúa pospuesto. **La fase 4 permanece abierta.**

## Entrega posterior · parte A, paso 4.6 · 2026-10-19

Se conectaron las pantallas de responsables, demora, llegada, catálogo/selección de bodega, Incidencias y Fin a la API existente. Se conservan los bloqueos documentales de A6/A9/A10, los plazos pendientes, roles múltiples y la autorización del servidor. Detalle y casos pendientes para Codex en [entrega de 4.6](../fase-4-paso-4.6-implementacion.md); [evidencia estática](fase-4-evidencias/entrega-4.6-revision-estatica.json).

Sin modificar pruebas ni ejecutar pruebas, lint, builds o migraciones. La comparación de huellas confirma que backend, pruebas, Header.tsx, DESIGN, dependencias e índice Git no cambiaron durante esta entrega. Detector estático de impeccable: `[]`, salida 0. Revisión de whitespace: salida 0, con avisos LF/CRLF documentados. No se declara compilación, validación visual ni cobertura nueva: los resultados de 761 casos y 90,92 % global anteriores no certifican 4.5/4.6.

F4-H01 sigue abierto y el historial conserva el orden recibido. Documentos/plazos y avisos pendientes no se resuelven implícitamente. Sin activaciones OAuth/workers/schedulers ni operaciones Google; 3.B3 permanece pospuesto. Sin comandos Git de escritura. **La fase 4 continúa abierta.**

## Parte B · verificación de 4.5 y pantallas de 4.6 · 2026-10-19

### Alcance, aislamiento y conservación

Rama comprobada: `fase-4-etapas-a5-a11`. Se leyó el manual **DOCX vigente** (4.B1–4.B3), el plan y este informe; la copia Markdown del manual contiene una numeración anterior y no sustituyó al DOCX. Skills: `ecc:springboot-tdd`, `ecc:tdd-workflow`, `ecc:react-test` y `playwright-best-practices`; revisión de las modificaciones Java con `ecc:java-reviewer`.

Solo se modificaron pruebas, fixtures de prueba y documentación. [420 huellas iniciales de producción y configuración](fase-4-evidencias/verificacion-4.5-4.6/produccion-inicial-sha256.json) comparadas al final: **cero cambios**, incluidos Header.tsx, backend, frontend de producción, configuración de pruebas, dependencias e índice Git. Se conservaron los cambios previos de 4.5/4.6 y los ajenos al encargo. `git diff --check` de los seis archivos de prueba modificados/añadidos devolvió 0.

El [ejecutor aislado](fase-4-evidencias/verificacion-4.5-4.6/ejecutar.ps1) desactiva OAuth, Drive, Gmail, copia automática, correo automático y semilla; vacía la importación local y fija una URL de base imposible como barrera. Spring obtiene PostgreSQL 17 de Testcontainers, sin reutilizar bases compartidas. Cada ejecución utiliza un directorio temporal UUID; `Fase4IT` conserva además su disco temporal exclusivo y reloj fijo. No se cargan credenciales Google ni se autorizan cuentas. No se borraron volúmenes ni se aplicaron migraciones a bases compartidas.

Playwright inicia exclusivamente Vite en `127.0.0.1:43184`, sin reutilizar servidores, y detiene su servidor al terminar. Todas las rutas API se interceptan con datos ficticios; peticiones a otros orígenes se bloquean y hacen fallar las comprobaciones. Se comprueban también errores JavaScript. Tras terminar se constató **cero listeners** en ese puerto. Los contextos y pools del backend de pruebas finalizaron. No se detuvieron servicios ajenos ni se activaron workers reales.

**Límite físico conservado:** las cuotas lógicas no limitan el espacio físico de temporales; esta ejecución no resuelve esa limitación ni introduce borrado automático. 3.B3 permanece pospuesto y la fase 3 no se cierra.

### Pruebas completadas

| Garantía | Evidencia ejecutable | Resultado |
|---|---|---|
| A10 vigente conserva documentos `PENDIENTES`, plazo `PENDIENTE`, sin vencimiento; admin y Logística no avanzan con/sin archivo | `Fase4IT.a10VigenteMantieneDocumentosYPlazoPendientes` | 2 casos aprobados; sin cambios de pedido ni intención enviable de cierre |
| Fixture no vigente con documento ficticio obligatorio: falta → 422; documento real de la entrada → A10 y Fin completadas, hora del servidor, sin etapa ACTUAL | `Fase4IT.a10ConRequisitosDeFixtureFinalizaYRegistraSistema` | 2 casos aprobados; relectura por API/SQL y segundo envío rechazado |
| Evento `PEDIDO_FINALIZADO` sin usuario/rol, junto con aprobación por persona; entrada y salida de Fin a la hora controlada | `Fase4IT.comprobarFin`, ejercitado desde los casos de cierre | Aprobado; sin exigir orden causal no aprobado |
| Documento de otro pedido y permisos/asignaciones/cuentas/roles vigentes | `a10NoAdmiteDocumentoDeOtroPedido`, `a10ReleePermisosAntesDeCerrar` | 5 casos aprobados; sin mutaciones ante rechazo |
| Fallos al insertar entrada de Fin, aprobación o evento del sistema | `falloDeCierreRevierteAccionFinEHistorial` | 3 casos aprobados: rollback de pedido, etapas, entradas, historial y outbox; nueva confirmación posterior válida |
| Dos confirmaciones concurrentes | `a10DobleEnvioConcurrenteSoloCierraUnaVez` | Una 200 y otra 409, una aprobación, una entrada y un evento de Fin |
| Fin sin acciones, observaciones, subidas o reasignación manual | `finRechazaAccionesObservacionesSubidasYReasignacion` | 2 casos aprobados con admin y Logística; estado e historial intactos |
| Bloqueo de A10 ya habilitada, no rechazo por estar fuera de fase | `Backend24IT.fase4IncidenciasHabilitadaConservaBloqueoDocumental` | Expectativa adaptada; se conservó la comprobación de ausencia de mutación |
| Bandejas China, Logística y roles múltiples; demora, llegada marcada, catálogo/selección real, errores/vacío, conflictos, historial, resumen y Fin | `frontend/src/components/pedidos/Fase4Pantallas.test.tsx` | 19 casos nuevos aprobados con MSW y componentes/rutas/sesión/Query reales |
| Bloqueos A6/A9/A10 visibles; mutaciones impedidas; respuestas API, invalidación y cambios de etapa/versión sin reenvío automático | Misma suite y suites previas de acciones/archivos | Aprobado; se conserva autoridad del servidor |
| Navegador real sobre respuestas HTTP ficticias: bandejas, bloqueos, demora ISO, llegada, bodega en fixture, conflicto de etapa/versión, permisos de menú/pantalla y Fin | [Playwright](fase-4-evidencias/verificacion-4.5-4.6/pantallas.spec.mjs) | 13/13 aprobados, sin peticiones externas ni errores JavaScript inesperados |
| Regresión de fechas estrictas, migraciones, bodegas, recuperación/archivos y arquitectura | Check completo existente | Aprobado; se conservan los 23 casos de fecha no ISO, incluido arreglo JSON, y las comprobaciones de ausencia de cambios |

Se amplió `Fase4IT` con **15 casos** sin crear otra suite backend. `Fase4IT` termina con 95 casos; `Backend24IT`, 48; `Fase4ReglasTest`, 4; `MigracionesFase4IT`, 7. Las migraciones de prueba llegan a V12 y se ejecutan solo en contenedores exclusivos. El backend global pasa de 761 a 776 casos. Los detalles por suite están en [métricas finales](fase-4-evidencias/verificacion-4.5-4.6/final-metricas.json) y [JUnit completo](fase-4-evidencias/verificacion-4.5-4.6/final-junit/).

**Fixtures ≠ producto aprobado:** los pedidos posteriores a A6 son estados de prueba insertados explícitamente. La versión ficticia de A10 no es vigente y exige `Incidencia ficticia F4`; se suben bytes ficticios por el backend aislado. La definición vigente de A6/A9/A10 sigue bloqueada. Las respuestas de MSW/Playwright se fijan por caso y no prueban el motor ni la autorización del servidor: esas garantías se ejercitan por separado mediante MockMvc/PostgreSQL. **No se ejecutó ni se aprobó un recorrido real A1–Fin de 4.B2.**

### Comandos, ejecuciones iniciales y ajustes del arnés

Desde PowerShell se ejecutaron los siguientes comandos completos; el ejecutor conserva el comando interno, código de salida y disco UUID de cada invocación:

```powershell
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\verificacion-4.5-4.6\ejecutar.ps1' lint
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\verificacion-4.5-4.6\ejecutar.ps1' dirigido
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\verificacion-4.5-4.6\ejecutar.ps1' test
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\verificacion-4.5-4.6\ejecutar.ps1' check
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\verificacion-4.5-4.6\frontend-final.ps1'
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\verificacion-4.5-4.6\ejecutar-browser.ps1'
& 'C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\verificacion-4.5-4.6\medir.py'
```

1. Baseline: lint aprobado; suites backend dirigidas **146/146**, [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-160248-dirigido.log). Frontend inicial **198 aprobados/30 fallidos**, [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-160323-test.log). Los fixtures de acciones omitían `requiereBodega` y los de flujo omitían `registraLlegada`; los esquemas de producción rechazaban esas respuestas antiguas. Se completaron los campos obligatorios, nombres/roles y metadatos de prueba conforme al contrato; no se hicieron opcionales para ocultar los fallos.
2. Con fixtures actualizados y casos nuevos: backend dirigido **161/161**, [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-162102-dirigido.log), [JUnit](fase-4-evidencias/verificacion-4.5-4.6/dirigido-junit/). Frontend **244 aprobados/3 fallidos**, [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-162103-test.log). Dos expectativas antiguas se actualizaron para exigir el nuevo aviso documental y la relectura de detalle/historial tras observar, manteniendo ausencia de avance. El tercer fallo fue una espera de arranque de lista; no se reprodujo en las dos ejecuciones completas posteriores con dos workers. Se acotó el paralelismo, sin ampliar timeouts, omitir casos o añadir reintentos. La causa concreta de esa sensibilidad al arranque no se da por demostrada; queda como observación de estabilidad.
3. La primera secuencia final tuvo un error TypeScript en **la prueba nueva**, por una opción `exact` que Testing Library no acepta en `ByRoleOptions`. Se retiró esa opción: el nombre de cadena ya conserva coincidencia exacta. Lint se repitió y aprobó: [resultado](fase-4-evidencias/verificacion-4.5-4.6/20261019-163408-lint.log). Se conserva el resultado fallido anterior y no se presenta la secuencia agregada como exitosa: `frontend-final.ps1` devolvió 1 por aquel lint inicial, aunque test, cobertura y build terminaron con 0.
4. Playwright inicial **12/13**: [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-163239-playwright.log), [reporte](fase-4-evidencias/verificacion-4.5-4.6/playwright.json), [captura/traza](fase-4-evidencias/verificacion-4.5-4.6/browser-artifacts/). El selector «Cambio ajeno» coincidía tanto con su celda como con la celda del botón «Editar bodega Cambio ajeno». Se hizo exacto el selector del arnés; la validación del borrador, versión, número de envíos y recarga no se retiró. Repetición completa **13/13**, [log final](fase-4-evidencias/verificacion-4.5-4.6/20261019-163412-playwright.log) y [reporte final](fase-4-evidencias/verificacion-4.5-4.6/20261019-163412-playwright-resultados.json). Las evidencias iniciales no se sobrescribieron.
5. Tras el último ajuste de tipos del selector, se repitió específicamente `npm.cmd run test -- src/components/pedidos/Fase4Pantallas.test.tsx --maxWorkers=2`: **19/19, salida 0**, [log posterior a lint](fase-4-evidencias/verificacion-4.5-4.6/pantallas-post-lint.log). No se repitió todo el backend ni se alteraron los contadores de la ejecución completa de cobertura.

Esta es una verificación de código ya implementado. Los fallos de fixtures/arnés no se presentan como un RED de defecto de producción, ni se inventa un ciclo RED/GREEN. No se alteró producción para conseguir verde. La prohibición del usuario prevaleció sobre los checkpoints Git de la skill: no se crearon commits.

### Resultados finales y cobertura real

| Comando interno | Resultado final | Evidencia |
|---|---|---|
| `gradlew.bat check --console=plain --no-daemon` | **0, BUILD SUCCESSFUL, 5 min 9 s; 776/776**, cero errores/omitidos; JaCoCo y umbral global 80 % aprobados | [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-162934-check.log), [metadatos](fase-4-evidencias/verificacion-4.5-4.6/20261019-162934-check.json) |
| `npm.cmd run lint` | **0**, comprobación TypeScript real posterior al ajuste de prueba | [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-163408-lint.log) |
| `npm.cmd run test -- --maxWorkers=2` | **0, 247/247**, 23 suites | [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-163049-test.log) |
| `npm.cmd run coverage -- --maxWorkers=2 --coverage.include=src/**/*.{ts,tsx} --coverage.reporter=text --coverage.reporter=json --coverage.reporter=json-summary --coverage.reporter=html` | **0, 247/247**; cobertura global inferior al 80 %, sin imponer un éxito de umbral inexistente | [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-163138-coverage.log), [resumen JSON](fase-4-evidencias/verificacion-4.5-4.6/frontend-coverage-summary.json), [contadores por línea](fase-4-evidencias/verificacion-4.5-4.6/frontend-coverage-final.json) |
| `npm.cmd run build` | **0**, build Vite real | [log](fase-4-evidencias/verificacion-4.5-4.6/20261019-163241-build.log) |
| Playwright con configuración aislada | **0, 13/13**, API simulada | [configuración](fase-4-evidencias/verificacion-4.5-4.6/playwright.config.mjs), [reporte](fase-4-evidencias/verificacion-4.5-4.6/20261019-163412-playwright-resultados.json) |

El check utilizó clases de producción existentes válidas en caché (`compileJava UP-TO-DATE`); `compileTestJava` ejecutó realmente al ampliar las pruebas. El check y las pruebas ejercitaron esas clases; el resultado no se atribuye a una revisión estática. El build frontend sí se ejecutó y generó el bundle. Los artefactos de `dist` y el HTML masivo de cobertura no se copian a evidencias; se conservan logs, JSON, XML y resultados reproducibles.

| Alcance completo | Líneas cubiertas/total | Líneas | Ramas |
|---|---:|---:|---:|
| Backend global | 4704/5170 | **90,99 %** | 72,01 % |
| Backend archivos | 1391/1611 | **86,34 %** | véase métricas JSON |
| Backend pedidos | 1162/1227 | 94,70 % | véase métricas JSON |
| Backend bodegas | 148/155 | 95,48 % | véase métricas JSON |
| Backend historial | 319/327 | 97,55 % | véase métricas JSON |
| Backend flujos | 227/243 | 93,42 % | véase métricas JSON |
| Frontend global, fuente `src` con exclusiones estándar de pruebas/tipos | 1176/1826 | **64,40 %** | **59,68 %** |

[JaCoCo completo](fase-4-evidencias/verificacion-4.5-4.6/final-jacoco.xml) y [métricas](fase-4-evidencias/verificacion-4.5-4.6/final-metricas.json). No se excluyó código de producto ni se bajó el umbral del backend. En frontend no se añadió un umbral ficticio: el 0 del comando de cobertura **no demuestra alcanzar 80 %**. La medición incluye, a cero, código demo no conectado como `orderState.ts`, `permissions.ts`, `WarehouseCatalogView.tsx` y otras vistas; no se retiró para elevar el porcentaje ni se amplió artificialmente este encargo para probar pantallas fuera de alcance.

Cobertura de líneas de piezas conectadas, **sin confundirlas con el global**: API bodegas 100 %, API pedidos 89,18 %, catálogo 88,37 %, detalle 94,11 %, diálogo de acción 85,71 %, panel 92 %, historial 80 %, bandeja 78,94 %. Son datos del ejecutor Vitest, no cobertura del navegador sumada a Vitest. La mejora de cobertura global frontend y huecos de ramas permanece pendiente.

Avisos conservados: JVM class sharing; cast redundante en la nueva aserción de prueba Java; Vite sobre futuro `configLoader: native`/`__dirname`; bundle de aproximadamente 790 kB (>500 kB); Playwright sobre `NO_COLOR`/`FORCE_COLOR`. No se modificó producción/configuración para silenciarlos. El extractor inicial del manual necesitó fijar UTF-8 de salida; la lectura posterior del DOCX se completó. Un inventario auxiliar de Docker dentro del sandbox no tuvo permiso sobre el pipe; las pruebas Testcontainers autorizadas se ejecutaron fuera del sandbox y aprobaron. Ninguno de esos avisos se presenta como defecto de producto confirmado.

### Orden de eventos: F4-H01 sigue abierto

Se repitió el reproductor determinista `Fase4IT.historialPuedeInvertirElOrdenCausalPorDesempateUuid`: inserta asignación y aprobación a la misma hora con UUID elegidos para invertir su orden causal; la consulta sigue ordenando por fecha/UUID y la API invierte la lista para reciente→antiguo. El caso aprobado **demuestra la limitación, no acepta ese comportamiento como requisito**.

El cierre nuevo registra aprobación de A10 y `PEDIDO_FINALIZADO` con la misma hora del servidor; por lectura del código y las aserciones de hora, el mismo criterio de desempate tampoco garantiza su relación causal. No se afirma haber resuelto ese segundo orden ni se fuerza desde el navegador. Impacto: el historial puede colocar esos eventos en posiciones que no representan el orden en que ocurrieron dentro de la transacción; no debe usarse la posición de la lista para inferir el estado o la secuencia de negocio. Las pruebas comprueban contenido, atomicidad y estado final por API/SQL, independientemente de esa posición. La decisión de orden estable sigue pendiente y requiere tratamiento separado; producción no se modificó.

## Corrección de F4-H01 — parte A, 2026-10-20 — pendiente de revalidación

Aplicada la corrección técnica aprobada por Richard: secuencia de inserción generada por la base (`V13__historial_secuencia.sql`) y lectura por hora y, en empate, secuencia; la API sigue de más reciente a más antiguo y, con la misma hora, muestra primero el último evento emitido. Los eventos anteriores a V13 conservan secuencia nula y su orden dentro de una misma hora no se reconstruye. No se alteraron horas ni los triggers de solo inserción. **No se modificaron pruebas ni se ejecutaron pruebas, builds o migraciones; F4-H01 sigue abierto hasta la revalidación.** El detalle y los casos están en el plan ("Corrección de F4-H01").

- **Casos para Codex (parte B), pendientes de revalidación:**
  - V13 sobre una base con V1–V12 **y eventos existentes**: la migración no actualiza ni reescribe filas (los triggers de solo inserción siguen intactos y no se disparan), las filas anteriores quedan con `secuencia` nula, sus `ocurrido_en` no cambian y siguen ordenándose como antes (hora e id) dentro de la misma hora;
  - una inserción nueva sin `secuencia` recibe número; un INSERT con `secuencia` nula explícita se rechaza (CHECK `evento_pedido_secuencia_nueva`, NOT VALID); números únicos; UPDATE, DELETE y TRUNCATE siguen rechazados;
  - **par 1**, aprobar Bodega: `BODEGA_ASIGNADA` y `ETAPA_APROBADA` con la misma hora → en la API, `ETAPA_APROBADA` antes que `BODEGA_ASIGNADA` (primero el último emitido), sea cual sea el UUID de cada uno;
  - **par 2**, aprobar Incidencias: `ETAPA_APROBADA` y `PEDIDO_FINALIZADO` con la misma hora → en la API, `PEDIDO_FINALIZADO` antes que `ETAPA_APROBADA`;
  - el resumen de etapas (P-20) usa el mismo orden y sigue tomando el último cierre de cada etapa;
  - los eventos de una acción se insertan de forma síncrona en la transacción de la acción (`@EventListener` con `Propagation.MANDATORY`), en el orden de `retirarEventos()`; si la acción falla, no queda ninguno;
  - **el reproductor `Fase4IT.historialPuedeInvertirElOrdenCausalPorDesempateUuid` documenta el comportamiento anterior** (desempate por UUID): con la corrección, el orden sigue la inserción y no el UUID, así que ese caso debe actualizarse en la parte B para comprobar la garantía nueva. No se modificó.

- **Revisión `ecc:java-reviewer` (lectura estática, 2026-10-20):** sin hallazgos críticos ni altos en producción. V13 es solo de catálogo (sin reescritura ni UPDATE), el DEFAULT y el CHECK NOT VALID solo afectan a filas nuevas, `ddl-auto: validate` acepta la columna, Hibernate la omite en el INSERT, el orden de `persist` se conserva aun con adjuntos intercalados y flushes intermedios, no hay otros escritores ni lectores de `evento_pedido` que dependan del orden, y ambos lectores (historial y resumen) quedan coherentes. No ejecutar `VALIDATE CONSTRAINT evento_pedido_secuencia_nueva`: fallaría por las filas anteriores, nulas a propósito.
- **Pruebas existentes que fallarán y debe adaptar Codex (no se modificaron):**
  - `Fase4IT.historialPuedeInvertirElOrdenCausalPorDesempateUuid` (líneas ~374-385): esperaba el orden por UUID (`BODEGA_ASIGNADA`, `ETAPA_APROBADA`); con V13 la API devuelve `ETAPA_APROBADA`, `BODEGA_ASIGNADA` (último emitido primero). Reescribirla para exigir la garantía nueva;
  - `MigracionesIT` (líneas ~106, 121, 122), `V2RestriccionesIT` (línea ~29) y `MigracionesFase4IT` (línea ~22 y nombre V1AV12): fijan la versión final en 12; ahora es 13;
  - `MigracionesFase4IT` (líneas ~33-38): si se agrega V13 al recorrido, `select * from evento_pedido` gana la columna `secuencia` (nula en filas anteriores): comparar sin ella;
  - pruebas nuevas sugeridas: orden causal con eventos del mismo instante (ambos pares) y secuencia nula en eventos anteriores a V13, no nula en los nuevos.

### Bloqueos y continuidad

- **4.B1 de lo nuevo de 4.5 verificado localmente**, con los fixtures y límites descritos. No aparecieron nuevos defectos de producción confirmados en estos casos. F4-P01 conserva su cierre y su regresión aprobada.
- **4.B2 real bloqueado** por F4-D4/D7/D8 (documentos de A6/A9/A10). No se desactivaron esos requisitos; plazos pendientes se mantienen. Los casos ficticios posteriores a los bloqueos no levantan esta puerta.
- F4-H01 no se acepta ni se cierra. Avisos específicos, destinatarios, feriados y cualquier respuesta documental pendiente no se resuelven implícitamente.
- Cobertura global frontend 64,40 % y sensibilidad de arranque observada siguen explícitas. La cobertura del módulo backend de archivos es 86,34 %, una medición distinta; la limitación física de temporales permanece.
- OAuth, workers y schedulers reales permanecen desactivados. Sin Google real, bases compartidas, borrado de volúmenes o comandos Git de escritura. **3.B3 pospuesto; fases 3 y 4 abiertas.**

## Revalidación F4-H01 · 2026-10-20 · CERRADO

**Resultado:** la corrección recibida con V13 cumple las garantías aprobadas para eventos nuevos; se conservan los límites de los anteriores. F4-H01 se cierra exclusivamente por las pruebas ejecutadas y el check completo descritos aquí. No se modifica producción ni se cierran las fases 3 o 4.

### Alcance y conservación

Rama comprobada: `fase-4-etapas-a5-a11`. Fuentes: sección de corrección del plan, este informe, V13 y sus lectores JPA, incluido el contrato de historial. Skills aplicadas: `ecc:springboot-tdd`, `ecc:database-migrations` y `ecc:jpa-patterns`; revisión Java de las pruebas conforme a `ecc:java-reviewer`.

Se modificaron exclusivamente seis fuentes de prueba y documentación: `MigracionesIT`, `V2RestriccionesIT`, `MigracionesFase4IT`, `MigracionesArchivosB1IT`, `Fase4IT` y la nueva `HistorialSecuenciaH01IT`. Esta última necesita un contexto propio para crear datos bajo V12 y arrancar JPA **después** de V13; no duplica los casos de acciones de `Fase4IT`. Se añadieron siete casos: dos de migración, dos de orden/resumen/concurrencia en la suite existente y tres de legado/API.

**451 archivos protegidos** conservan sus huellas iniciales: producción backend, frontend completo, Header.tsx, Gradle, dependencias, contrato, plan e índice Git. [Huellas iniciales](fase-4-evidencias/revalidacion-h01/produccion-inicial-sha256.json), [comparación y métricas finales](fase-4-evidencias/revalidacion-h01/final-metricas.json). V1–V13 se conservaron exactamente como se recibieron. La revisión `git diff --check` de las seis fuentes de pruebas terminó con 0, sin retirar aserciones para hacer pasar la ejecución.

### Garantías comprobadas con PostgreSQL aislado

| Garantía | Prueba y resultado |
|---|---|
| V13 con eventos existentes conserva filas completas, timestamps y protecciones | `MigracionesFase4IT.v13ConservaFilasHorasYTriggersDeEventosAnteriores`: compara todas las columnas anteriores más `secuencia=null`, además de `ctid`, `xmin` y definiciones de triggers. Sin relleno retroactivo, cambio de hora o reescritura observada. UPDATE/DELETE/TRUNCATE siguen rechazados con P0001. |
| Eventos nuevos reciben secuencia obligatoria y única | `v13ExigeSecuenciaNuevaUnicaSinRellenarLasAnteriores`: DEFAULT positivo, dos antiguos nulos permitidos, NULL explícito nuevo rechazado con 23514 y número duplicado con 23505. Las protecciones de solo inserción también se comprueban en la fila nueva y se conservan las tres filas tras los rechazos. CHECK permanece NOT VALID, deliberadamente; no se ejecuta VALIDATE sobre los antiguos. |
| Reproductor adversarial preservado y adaptado | El antiguo `historialPuedeInvertirElOrdenCausalPorDesempateUuid` pasa a `historialConservaEmisionAunqueUuidInviertaElOrden`. Conserva la misma hora, los UUID opuestos a la emisión y el contraste SQL por hora/UUID. Ahora exige API `ETAPA_APROBADA` antes de `BODEGA_ASIGNADA`, dos lecturas idénticas y secuencia ascendente de emisión, no el resultado antiguo. |
| Orden real del dominio y adaptador JPA, con adjuntos y flushes | La aprobación de Bodega en fixture conserva `BODEGA_ASIGNADA` → `ETAPA_APROBADA` por secuencia y devuelve el inverso por API. Todos los cierres de Incidencias verificados por `comprobarFin` exigen `ETAPA_APROBADA` → `PEDIDO_FINALIZADO` y, por API, `PEDIDO_FINALIZADO` antes que `ETAPA_APROBADA`. Misma hora controlada, números no nulos y únicos; la secuencia no se expone en JSON. |
| Historial antiguo, mezcla y prioridad de la hora | Tres casos de `HistorialSecuenciaH01IT`, con estrategia Flyway de prueba V12 → datos ficticios → V13 real → JPA validate. No se eliminan CHECK ni triggers para crear filas antiguas. Se comprueban horas/nulos, orden repetible por UUID, mezcla con números nuevos y que una hora posterior prevalezca sobre el grupo nulo. |
| Resúmenes coherentes con el mismo lector | `resumenEligeUltimoCierrePorSecuenciaConLaMismaHora` inserta dos cierres con UUID adversariales: el resumen elige la última emisión, igual que el historial descendente. Los tres casos de legado comprueban el resumen correspondiente a su desempate y a la prioridad de la hora. |
| Rollback y concurrencia | Se repiten los fallos inyectados de entrada de Fin/aprobación/evento del sistema, los de llegada/bodega y demora, y las carreras de Bodega/Fin existentes. Pedido, entradas, historial y outbox conservan atomicidad; doble envío confirma una sola acción. El caso nuevo con cuatro pedidos concurrentes y reloj fijo exige cuatro aprobaciones y secuencias distintas, sin imponer orden global de commit. |
| Esquema y compatibilidad | Versión final y lista exacta de migraciones actualizadas a V13, sin relajar el conjunto exacto de tablas, validación Flyway o reaplicación cero. V3/V8 atraviesan V9–V13 conservando UUID, relaciones, eventos y datos; los eventos anteriores se comparan completos con la nueva columna explícitamente nula. La prueba histórica acotada a V8 sigue acotada a V8. |

`Fase4IT` finaliza con 97 casos; `MigracionesFase4IT`, nueve; `HistorialSecuenciaH01IT`, tres. Las reglas documentales reales de A6/A9/A10 se repiten sin desactivarlas. Los fixtures posteriores a esos bloqueos siguen siendo **estados/versiones ficticios aislados, no un recorrido aprobado A1–Fin**.

### Desempate antiguo y caso mezclado: límite explícito

La API sigue orden descendente por hora y, para los nuevos de igual hora, por secuencia. Los anteriores a V13 conservan NULL y el desempate por UUID descendente; no se conoce su causalidad dentro de la misma hora.

En PostgreSQL, la lectura ascendente usada por el repositorio coloca NULL al final; la API invierte esa lista. **Si antiguos y nuevos comparten exactamente la misma hora, primero aparece el grupo antiguo por UUID descendente y después el grupo nuevo por secuencia descendente.** El test mezclado exige la lista completa y dos lecturas iguales, contrastadas con SQL descendente. El resumen usa ese mismo orden: con la misma hora puede escoger el cierre antiguo con UUID mayor. No se presenta este desempate como una reconstrucción causal ni se afirma que el cierre antiguo ocurrió realmente después. Si el nuevo tiene una hora posterior, aparece primero y determina el resumen, como comprueba el tercer caso.

El cierre de H01 acredita el orden aprobado de eventos **nuevos** y este límite documentado del legado; no reinterpreta timestamps. La secuencia puede tener huecos tras rollback y no expresa orden global de commits de pedidos distintos. No se exige consecutividad ni se modifica el producto para inventarla.

### Ejecuciones y comandos reales

El [ejecutor](fase-4-evidencias/revalidacion-h01/ejecutar.ps1) mantiene Google/OAuth/copia/correo/semilla desactivados, vacía la importación de configuración local y fija una URL inválida como barrera contra bases compartidas. PostgreSQL 17 procede de Testcontainers sin reutilización; cada ejecución tiene almacenamiento temporal UUID y las migraciones utilizan esquemas exclusivos. El contexto de legado tiene contenedor propio y disco temporal exclusivo. No se leen credenciales reales ni se activan workers/schedulers reales. No se usaron Flyway clean, Docker prune ni borrado de volúmenes. Los logs registran el cierre de contextos y pools al terminar.

```powershell
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\ejecutar.ps1' baseline
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\ejecutar.ps1' dirigido
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\ejecutar.ps1' check
# El check se repitió después de adaptar la expectativa de fila histórica del módulo de archivos.
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\ejecutar.ps1' check
& 'C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\medir.py'
```

1. Baseline anterior a adaptar pruebas: **56 casos, siete fallos**, [log](fase-4-evidencias/revalidacion-h01/20261020-094338-baseline.log), [JUnit](fase-4-evidencias/revalidacion-h01/baseline-junit/). Expectativas V12, validación con V13 aún pendiente y resultado antiguo del reproductor. Incluye el fallo de inicialización de `V2RestriccionesIT`, cuyas comprobaciones se ejecutaron correctamente después. `compileJava` se ejecutó realmente en esta invocación.
2. Suites dirigidas adaptadas: **198/198**, [log](fase-4-evidencias/revalidacion-h01/20261020-094648-dirigido.log), [JUnit](fase-4-evidencias/revalidacion-h01/dirigido-junit/). Compilación de pruebas real. El cast redundante señalado por el compilador en la prueba adaptada se retiró después; no cambió sus aserciones.
3. Primer check: **783 casos, un fallo**, salida 1 en 4 min 24 s; [log](fase-4-evidencias/revalidacion-h01/20261020-094857-check.log), [JUnit](fase-4-evidencias/revalidacion-h01/check-previo-junit/). `MigracionesArchivosB1IT.v3PobladaConservaUuidRelacionHuellaBytesEHistorialAlActualizar` comparaba la fila anterior sin la nueva columna. Se añadió al mapa esperado `secuencia=null`, conservando **todas** las columnas, UUID, relación, huella y bytes. No se descartó la columna de la comprobación ni se redujo el alcance de la migración.
4. Check completo final: **783/783**, salida **0**, `BUILD SUCCESSFUL` en **4 min 35 s**; [log](fase-4-evidencias/revalidacion-h01/20261020-095818-check.log), [metadatos](fase-4-evidencias/revalidacion-h01/20261020-095818-check.json), [JUnit completo](fase-4-evidencias/revalidacion-h01/final-junit/). Cero fallos, errores u omitidos. Se ejecutaron `compileTestJava`, `test`, `jacocoTestCoverageVerification`, `check` y `jacocoTestReport`; producción estaba compilada desde el baseline y figura UP-TO-DATE. No se atribuye compilación a una revisión estática.

El primer check fallido **no finalizó JaCoCo**. El XML disponible entonces pertenecía a las suites dirigidas, identificado por `sessioninfo` 09:46:58–09:47:57; se conserva como [dirigido-jacoco.xml](fase-4-evidencias/revalidacion-h01/dirigido-jacoco.xml), sin atribuirle cobertura del check. El medidor verifica que la sesión del [XML final](fase-4-evidencias/revalidacion-h01/final-jacoco.xml) comience después del último check aprobado. Las ejecuciones fallidas y sus evidencias se conservaron.

### Cobertura final, sin exclusiones nuevas

| Alcance completo | Líneas cubiertas/total | Líneas |
|---|---:|---:|
| Backend global | 4704/5170 | **90,99 %** |
| Historial | 319/327 | **97,55 %** |
| Pedidos | 1162/1227 | 94,70 % |
| Archivos | 1391/1611 | **86,34 %** |
| Bodegas | 148/155 | 95,48 % |
| Flujos | 227/243 | 93,42 % |

Ramas globales: **72,05 %**. Se aprobó el umbral existente del **80 % LINE global** de Gradle sin modificarlo ni excluir código. Contadores y sesiones en [métricas finales](fase-4-evidencias/revalidacion-h01/final-metricas.json). No es cobertura exclusiva del diff ni porcentaje de cobertura SQL. La cobertura frontend **64,40 % del 19/10** sigue pendiente y no se recalculó en este encargo backend. Se mantiene también la limitación del espacio físico temporal frente a cuotas lógicas.

**Estado final:** F4-H01 cerrado por revalidación de la corrección aprobada; ningún nuevo defecto de producción confirmado en estos casos. F4-P01 mantiene su regresión aprobada. Documentos de A6/A9/A10 y sus plazos pendientes, avisos/destinatarios y demás decisiones externas siguen sin cambios. **4.B2 real continúa bloqueado; 3.B3 pospuesto; fases 3 y 4 abiertas.** Sin Google real, bases compartidas, commit, push o merge.

## Ampliación de comportamiento frontend y cobertura (2026-10-20)

Alcance: exclusivamente pruebas y documentación del frontend en `fase-4-etapas-a5-a11`. Skills `ecc:react-test` y `ecc:test-coverage`, incluidas sus referencias originales. Se leyó este informe y se contrastaron contadores V8 por archivo/rama con los componentes, contratos y fixtures existentes. No se modificó producción, configuración, dependencias ni pruebas anteriores; se añadieron **65 casos** en dos suites nuevas: [Fase4Ramas.test.tsx](../../frontend/src/components/pedidos/Fase4Ramas.test.tsx) (35) y [formato.test.ts](../../frontend/src/components/pedidos/formato.test.ts) (30). La suite previa `Fase4Pantallas.test.tsx` se conservó y volvió a ejecutarse con las demás.

### Comportamientos y aislamiento

- Permisos: consulta sola; roles múltiples que conservan sus atribuciones; asignación propia vigente, vencida y ajena; admin; pérdida de permiso con 403 después de abrir una acción. Se conserva el borrador y se impide reenvío, incluso por submit. Son pistas y respuestas HTTP simuladas: no se atribuye a RTL una nueva comprobación real del servidor.
- Demora: valores ausentes, fecha imposible, fecha con hora y formato local no se envían desde el control nativo; 400 con error de campo conserva observación y fecha. Una respuesta retenida impide doble envío/cierre y no anuncia éxito antes de confirmar. La suite anterior mantiene la cadena exacta enviada y la fecha efectiva ajustada que devuelve la API; no se calcula el calendario en el navegador. Los rechazos de cuerpos JSON directos y la atomicidad de la base siguen acreditados únicamente por las evidencias backend anteriores.
- Bodegas: consulta fallida y recuperación, catálogo vacío, búsqueda por ciudad/nombre, validación previa, 409 duplicado, 400 con y sin campos, 403 y 503 sin éxito ficticio. Desactivar envía la versión leída y presenta la respuesta confirmada; una desactivación concurrente invalida la selección y no escoge ni reenvía otra automáticamente. El guardado pendiente bloquea doble envío, Cancelar y Escape. Se conservan los casos previos de conflicto de versión, recarga y revisión explícita.
- A6/A9/A10: el aviso documental también aparece para consulta sin acciones; tener archivos guardados no resuelve `PENDIENTES`. El fallo de la definición se informa y no elimina el bloqueo que vino en las acciones. Se prueban recuperación de bandeja/detalle/historial sin inventar filas, eventos o avances.
- Historial: conserva literalmente el orden de la API aunque UUID y horas no indiquen causalidad. Presenta Fin del sistema, fechas solicitada/anterior/confirmada, bodega histórica conocida o genérica sin inventar su nombre, y archivos por metadatos de la etapa. No reconstruye el orden antiguo ni reabre F4-H01.
- Fin: el fixture explícito **v901**, ajeno a la definición real, permite estudiar el estado posterior al bloqueo. Solo anuncia el cierre cuando la API devuelve `finalizado`; evita doble envío. En otro fixture v901, desmarcar el documento deshabilita la confirmación; volver a marcarlo envía su ID, y un 422 mantiene A10 sin anunciar Fin. No se presenta esto como recorrido A1–Fin ni como resolución de F4-D8. Fin continúa sin acciones manuales.
- Formato: fecha civil sin desplazamiento de zona, hora en Ecuador cerca de medianoche UTC, ausencia de fechas sin inventar vencimientos y mensajes del contrato sin revelar detalles técnicos.

Se usa React Testing Library, user-event, rutas/sesión/TanStack Query y componentes reales con MSW. No se sustituyen componentes hijos ni React por mocks. Las respuestas retenidas mediante promesas permiten comprobar operaciones en curso sin esperas de minutos. Nombres, usuarios, documentos, eventos y destinos son ficticios. **No se inició backend, PostgreSQL ni almacenamiento compartido**, ni se llamó a Google; no se leyeron credenciales. OAuth, workers y schedulers reales permanecieron desactivados. Esta ejecución no aporta nuevos casos Playwright ni integración real.

### Comandos y resultados reales

Desde `C:\Users\Richard\Desktop\decoWeb\documentos`, el [ejecutor](fase-4-evidencias/cobertura-frontend/ejecutar.ps1) fija las guardas locales. Sus logs/metadatos conservan cada salida. Comandos equivalentes desde `frontend`:

```powershell
npm.cmd run test -- src/components/pedidos/Fase4Pantallas.test.tsx src/components/pedidos/Fase4Ramas.test.tsx src/components/pedidos/formato.test.ts --maxWorkers=2
npm.cmd run coverage -- --maxWorkers=2 '--coverage.include=src/**/*.{ts,tsx}' --coverage.reporter=text --coverage.reporter=json --coverage.reporter=json-summary
npm.cmd run lint
npm.cmd run build
```

| Ejecución | Resultado | Evidencia |
|---|---|---|
| Baseline global, 10:14 | 247/247, 23 suites, salida 0; líneas 1176/1826 | [Log](fase-4-evidencias/cobertura-frontend/20261020-101442-baseline.log), [resumen](fase-4-evidencias/cobertura-frontend/20261020-101442-baseline-summary.json) |
| Dirigida inicial, 10:22 | Tres fallos de fixture MSW; corregidos solo en las pruebas | [Log conservado](fase-4-evidencias/cobertura-frontend/20261020-102251-dirigido.log) |
| Coverage intermedia, 10:23 | 300/300, salida 0 | [Log](fase-4-evidencias/cobertura-frontend/20261020-102349-coverage.log) |
| Lint inicial, 10:24 | Falló un fixture sin `roles`; completado según contrato | [Log conservado](fase-4-evidencias/cobertura-frontend/20261020-102403-lint.log) |
| Dirigida ampliada, 10:26 | Cuatro fallos de consultas de pruebas; corregidos sin relajar las comprobaciones | [Log conservado](fase-4-evidencias/cobertura-frontend/20261020-102641-dirigido.log) |
| Coverage intermedia, 10:27 | 309/309, salida 0 | [Log](fase-4-evidencias/cobertura-frontend/20261020-102722-coverage.log) |
| Coverage ampliada, 10:30 | Un fixture incompleto de bandeja; corregido solo en prueba; no produjo contadores válidos | [Log conservado](fase-4-evidencias/cobertura-frontend/20261020-103009-coverage.log) |
| **Coverage final, 10:32** | **312/312, 25 suites, cero fallos/omitidos, salida 0; 56,33 s** | [Log final](fase-4-evidencias/cobertura-frontend/20261020-103233-coverage.log), [comando/salida](fase-4-evidencias/cobertura-frontend/20261020-103233-coverage.json), [resumen V8](fase-4-evidencias/cobertura-frontend/20261020-103233-coverage-summary.json), [contadores](fase-4-evidencias/cobertura-frontend/20261020-103233-coverage-contadores.json) |
| **Lint final** | **`tsc --noEmit`, salida 0** | [Log](fase-4-evidencias/cobertura-frontend/20261020-103258-lint.log) |
| **Build real** | **Vite 8.3.4, 2049 módulos, salida 0** | [Log](fase-4-evidencias/cobertura-frontend/20261020-102704-build.log) |

Los fallos intermedios no son defectos de producción: el handler genérico de `apiPedido` tapaba la definición específica cuando se registraba primero; un archivo aparecía tanto en la lista como en la selección y requería una consulta inequívoca; los encabezados del fondo no son accesibles mientras el modal está abierto; faltaba el handler de flujo y el nombre exacto de A8 en un caso nuevo de bandeja. Se corrigieron fixtures y consultas, conservando todos los bloqueos y aserciones de negocio. La ejecución final incluye las tres suites dirigidas completas.

Avisos existentes: Vite anticipa incompatibilidad de `__dirname` con su futuro cargador nativo; el bundle JS sigue en **790,28 kB** (243,83 kB gzip), por encima del aviso de 500 kB. No se ocultaron avisos ni se alteró producción para corregirlos. El build acredita compilación real del frontend; **no se atribuye al backend**.

### Cobertura: global y subconjuntos, sin exclusiones nuevas

Mismo comando e inclusión explícita `src/**/*.{ts,tsx}` antes y después; se conservan únicamente los filtros predeterminados de Vitest. `vite.config.ts`, `package.json`, lockfile y umbrales permanecen intactos. La base de comparación sigue incluyendo auxiliares de pruebas instrumentados por Vitest; no se los retiró para cambiar el denominador. El objetivo global se evalúa como **NO alcanzado**, aunque las pruebas terminen en verde.

| Medición | Antes | Después |
|---|---:|---:|
| **Líneas globales** | 64,40 % · 1176/1826 | **65,82 % · 1202/1826** |
| Ramas globales | 59,74 % · 1150/1925 | 63,16 % · 1216/1925 |
| Funciones globales | 68,45 % · 447/653 | 71,51 % · 467/653 |
| Sentencias globales | 64,00 % · 1289/2014 | 65,88 % · 1327/2014 |
| Líneas de ocho módulos priorizados F4 | 88,24 % · 225/255 | **97,65 % · 249/255** |
| Ramas de esos ocho módulos | 81,47 % · 431/529 | **93,57 % · 495/529** |

Los ocho archivos son `api/bodegas.ts`, `CatalogoBodegas.tsx`, `BandejaPedidos.tsx`, `DetallePedido.tsx`, `DialogoAccion.tsx`, `HistorialPedido.tsx`, `PanelEtapa.tsx` y `formato.ts`. El subconjunto alcanzable por imports estáticos desde `main.tsx` mide **91,68 % · 1124/1226 líneas**; incluye imports de tipos y no equivale a una comprobación del bundle. **Ningún porcentaje parcial se presenta como cobertura global ni como cumplimiento del 80 %.** Los contadores por archivo y las ramas todavía no recorridas están en el [análisis reproducible](fase-4-evidencias/cobertura-frontend/analisis-cobertura.json), generado por [este script](fase-4-evidencias/cobertura-frontend/analizar-cobertura.py).

### Bloqueo de cobertura global y entrega para Claude

**F4-COV01 — objetivo ≥80 % pendiente, no un defecto funcional acreditado.** Faltan **259 líneas** para llegar al mínimo de 1461/1826. El grafo estático conserva **520 líneas a cero en 13 módulos no alcanzables desde la aplicación**: `AuditTrailView` (25), `DashboardView` (19), `OrderProductsTab` (26), `OrderScheduleTab` (4), `TimelineCalculatorView` (37), `Toast` (5), `TrackingSearchView` (26), `WarehouseCatalogView` (27), `ui/dropdown-menu` (15), `data/initialData` (31), `utils/dateUtils` (72), `utils/orderState` (203), `utils/permissions` (30). No se afirma que todos deban borrarse: hay componentes/utilidades reutilizables junto al demo. El listado exacto y la traza de imports se pueden reproducir con el analizador; no hay `import.meta.glob` ni imports dinámicos de producción encontrados en `src`.

Incluso cubrir todas las demás líneas dejaría **1306/1826 = 71,52 %** si esos módulos permanecen a cero. Por tanto, ampliar únicamente el comportamiento conectado de fase 4 no basta para el objetivo global. Parte del demo contiene reglas que no deben convertir pruebas en requisitos: `utils/orderState.ts` inventa un día cuando el plazo no está definido y construye mensajes como “Alerta enviada por Gmail” sin integración. No se escribieron expectativas positivas para aprobar esas simulaciones ni se conectó el demo a las rutas.

**Procedimiento para Claude:** ejecutar el comando global anterior y después `python .\documentos\pruebas\fase-4-evidencias\cobertura-frontend\analizar-cobertura.py` desde la raíz; contrastar `main.tsx`/`rutas.tsx` y los 13 módulos listados. Decidir qué legado se conserva con un contrato de comportamiento aprobado y qué se retira en un encargo que permita modificar producción. Si se conserva, añadir pruebas útiles contra ese contrato; si se retira, hacerlo sin exclusiones de cobertura y volver a medir. En esta entrega no se elimina ni excluye nada. Las ramas activas restantes incluyen fallbacks defensivos y variantes de historial ajenas al nuevo comportamiento A5–A11; permanecen enumeradas, sin declarar cobertura total.

**Defectos nuevos reproducibles de producción:** ninguno acreditado en estos casos. Los problemas de fixtures se distinguen arriba de F4-COV01 y de los avisos de build. No se inventa un defecto del servidor a partir de HTTP simulado y no se modificó producción para hacer pasar un reproductor.

### Conservación y pendientes

La comparación de [491 hashes iniciales](fase-4-evidencias/cobertura-frontend/protegidos-iniciales.json) arroja **cero alteraciones** en producción frontend, `Header.tsx`, todo `backend/src` (incluidas sus pruebas), configuración/dependencias e índice Git. Los hashes de las dos suites nuevas están en [pruebas finales](fase-4-evidencias/cobertura-frontend/pruebas-finales-sha256.json). No se ejecutaron comandos Git de escritura; no se inició ni repitió backend, ni se ejecutaron migraciones. Sus resultados de 783 casos y coberturas anteriores siguen siendo evidencia histórica, no una ejecución de hoy atribuible a este trabajo.

**A6/A9/A10 siguen documentalmente bloqueadas**, A10 conserva su plazo pendiente y los fixtures v901 no son una aprobación de requisitos. El recorrido real 4.B2 continúa bloqueado; **3.B3 con Google permanece pospuesto**. OAuth, copia, correo y schedulers reales continúan desactivados; **fases 3 y 4 abiertas**. La limitación del espacio físico temporal de almacenamiento sigue explícitamente sin resolver. Permanece pendiente el objetivo global frontend ≥80 %.

## Diagnóstico breve de las 520 líneas desconectadas (2026-10-20)

Revisión estática, **sin ejecutar suites**: manual DOCX vigente, plan F4, auditoría del demo, imports de `frontend/src`, `main.tsx`/`rutas.tsx` y contadores V8 ya conservados. No se modificaron código, pruebas, exclusiones ni umbrales. Las cifras son **líneas instrumentadas**, no líneas físicas del archivo. Los 13 módulos siguen sin camino de importación desde la aplicación; algunos sí se consumen entre sí dentro del demo.

| Grupo | Archivos y líneas | Aporte al denominador global de 1826 |
|---|---|---:|
| Capacidad prevista para fases posteriores, conservada como **prototipo**, no implementación aprobada | `TimelineCalculatorView.tsx` 37; `OrderScheduleTab.tsx` 4; `utils/dateUtils.ts` 72 | **113 · 6,19 %** |
| Demo sustituido y sin consumidores activos, más infraestructura actualmente ociosa | `AuditTrailView.tsx` 25; `DashboardView.tsx` 19; `OrderProductsTab.tsx` 26; `Toast.tsx` 5; `TrackingSearchView.tsx` 26; `WarehouseCatalogView.tsx` 27; `data/initialData.ts` 31; `utils/orderState.ts` 203; `utils/permissions.ts` 30; `ui/dropdown-menu.tsx` 15 | **407 · 22,29 %** |
| Funcionalidad activa pendiente de pruebas **dentro de estas 520 líneas** | Ninguna: no hay consumidor activo encontrado | **0 · 0 %** |
| Total | 13 módulos, todos actualmente a cero | **520 · 28,48 %** |

La primera categoría describe el **destino funcional**, no una reserva aprobada de esos archivos. El manual, **5.1**, exige calendario/cronograma con reglas de negocio vigentes y advierte que la calculadora antigua usa días calendario; **5.3** ordena sustituir `TimelineCalculatorView` y retirar `/remind`. `OrderScheduleTab` usa fechas del modelo demo y `dateUtils` contiene también reloj simulado y semáforo provisional. No se deben dar por válidas sus duraciones ni copiar su cálculo al futuro producto. La fase 6 debe reutilizar el motor del servidor: que `orderState` tenga ramas B no lo convierte en implementación futura aprobada.

En la segunda categoría, catálogo, búsqueda, historial, permisos, motor y notificaciones ya tienen alternativas activas: `CatalogoBodegas`, `ListaPedidos`/`DetallePedido`/`HistorialPedido`, sesión y autorización del servidor, y Sonner. `DashboardRol` es el punto de entrada actual; sus futuras métricas de fase 5 no justifican certificar el viejo dashboard. `OrderProductsTab` conserva detalle comercial por producto que debe permanecer en archivos. **Matiz:** las 15 líneas de `dropdown-menu` son infraestructura shadcn sin consumidor, no lógica demo ni un componente cuyo reemplazo esté probado; se agrupan aquí como código ocioso, sin recomendar su borrado automático. `initialData`, `orderState`, `permissions` y `dateUtils` tienen consumidores internos del conjunto desconectado, por lo que “sin consumidores” significa **sin consumidores activos**, no cero referencias textuales.

**Pruebas todavía útiles:** en el producto actual, variantes del historial de reasignación/devolución/urgencia y metadatos ausentes; recuperación de la definición del flujo en bandejas y conservación del estado al releer pedidos. Las ramas concretas están en el análisis V8 previo. Para fase 5, una vez conectado el contrato: zona Ecuador y medianoche, fines de semana, arrastre de demora, plazos pendientes sin inventar fechas, meta configurable y coherencia entre vistas; teclado/foco del dropdown solo si llega a tener un uso real. No aporta valor escribir pruebas de éxitos ficticios de Gmail, permisos locales obsoletos ni calendarios arbitrarios para subir el porcentaje.

**Origen del 80 %:** el manual, **1.B3**, dice “Mide cobertura backend de al menos 80 %”; su criterio de salida repite “cobertura backend ≥ 80 %”. Para frontend pide lint, test y build; **4.B1–4.B3 no añaden un mínimo global frontend del 80 %**. Ese mínimo es el **objetivo adicional explícito del último encargo**, reforzado por las guías de pruebas, y sigue pendiente: este diagnóstico no lo revoca ni cambia umbrales. La medición vigente continúa en **65,82 % global**; el **97,65 % parcial** no lo sustituye.

**Recomendación:** priorizar las pruebas útiles del producto conectado; en un encargo posterior que permita tocar producción, resolver por separado qué prototipos reescribir en fase 5 y qué demo sustituido retirar, comprobando consumidores y conservando los componentes genéricos que se decida usar. Después volver a medir todo el código conservado, sin exclusiones nuevas. No presentar el 80 % frontend como obligación del manual ni darlo por cumplido. **A6/A9/A10 bloqueadas, 3.B3 pospuesto y fases 3 y 4 abiertas.**

## Cierre de casos pendientes de historial y recuperación activos (2026-10-20)

Alcance exclusivo: ampliación de `Fase4Ramas.test.tsx`, conservando sus 35 casos anteriores y agregando **12 pruebas de comportamiento**, sin nuevas suites ni pruebas del código desconectado. Se mantuvo el enfoque `ecc:react-test`: RTL/user-event, componentes y consultas reales, HTTP simulado con MSW y datos ficticios. No se modificaron producción, prototipos, demo, configuración, exclusiones ni umbrales.

| Casos nuevos | Comprobaciones |
|---|---|
| Reasignaciones: 3 | Personas conocidas, primera asignación y personas desconocidas; actor y rol históricos sin reinterpretarlos según la sesión actual; no exponer UUID como nombre ni conceder acciones. |
| Devolución y urgencia: 2 | Error frente a producto nuevo, destino del evento y observación; urgencia solo cuando viene informada; “La urgencia terminó” únicamente en el evento que lo confirma; orden de la API conservado. |
| Metadatos opcionales: 2 | `null` y ausencia de detalle, rol y adjuntos admitidos por el contrato; actor conservado, fecha visible, sin inventar archivos; texto parecido a HTML se presenta como texto. |
| Metadatos de archivo recuperados: 2 | Listado vacío o 503 permite mantener el evento con etiqueta genérica; actualización HTTP recupera el nombre sin duplicar el evento, revelar detalle técnico ni fabricar enlaces. |
| Definición de bandeja recuperada: 1 | Una definición inicialmente fallida se recupera sin inventar pedidos; aparece el aviso recibido de pendientes A9/A10 y no se ofrecen aprobaciones. |
| Definición del detalle recuperada: 1 | Conserva la etapa histórica seleccionada y su resumen cuando la etapa actual no cambia; desaparece el aviso de error y se muestra el bloqueo documental recuperado. |
| Cambio de estado al releer detalle: 1 | Si la API devuelve otra etapa actual, la selección vuelve a ella y se actualizan historial/resumen. El estado A10 es un fixture explícito de lectura, no un recorrido que omita A9; mantiene el bloqueo documental y no calcula transiciones en el navegador. |

Las actualizaciones externas se estimulan mediante la API pública `QueryClient.refetchQueries({ type: 'active' })`; las respuestas atraviesan HTTP/MSW y la validación del contrato. No se escribe directamente en la caché ni se simulan mutaciones de negocio. No se acreditan permisos reales del servidor, recorrido A1–Fin ni operaciones Google mediante estas pruebas.

**Resultados ejecutados:** dos suites afectadas, **66/66 casos aprobados** (`Fase4Ramas`: 47; `Fase4Pantallas`: 19), cero fallos/omitidos, salida 0, **15,85 s**. Comprobación adicional `npm.cmd run lint` (`tsc --noEmit`): salida 0. [Log de pruebas](fase-4-evidencias/historial-consultas/20261020-104504-afectadas.log), [comando y salida exactos](fase-4-evidencias/historial-consultas/20261020-104504-afectadas.json), [log de tipos](fase-4-evidencias/historial-consultas/20261020-104527-tipos.log), [revisión de conservación](fase-4-evidencias/historial-consultas/revision-final.json).

Comandos equivalentes desde `frontend`:

```powershell
npm.cmd run test -- src/components/pedidos/Fase4Ramas.test.tsx src/components/pedidos/Fase4Pantallas.test.tsx --maxWorkers=2
npm.cmd run lint
```

El comando literal conservado en los metadatos incluía además un filtro `Pedidos.test.tsx` sin coincidencias; Vitest ejecutó únicamente las dos suites contabilizadas arriba. Se retiró ese filtro obsoleto del [ejecutor](fase-4-evidencias/historial-consultas/ejecutar.ps1) para futuras invocaciones, sin repetir suites ni atribuirle una tercera suite ejecutada. Persiste el aviso conocido de Vite sobre `__dirname` y el futuro cargador nativo; no es un fallo de las pruebas.

**Defectos de producción:** ninguno reproducido por estos doce casos; no se aplicaron correcciones de producción ni se debilitaron comprobaciones. El hash del prefijo anterior de la suite coincide con el [original](fase-4-evidencias/historial-consultas/original-test-sha256.json): todos los casos previos se conservaron. La comparación de los 491 archivos protegidos confirma cero alteraciones en producción, Header.tsx, backend, configuración/dependencias e índice Git; prototipos y demo incluidos.

**Cobertura y alcance:** no se ejecutó cobertura, suite global, build ni backend en esta ampliación limitada. El **65,82 % global y el 97,65 % parcial son mediciones anteriores**, no nuevas; el objetivo adicional frontend ≥80 % permanece pendiente. Los casos locales de historial y recuperación descritos quedan verificados, sin declarar completas todas las ramas posibles. **A6/A9/A10 siguen bloqueadas, 3.B3 pospuesto y fases 3 y 4 abiertas.** Sin Google, servicios reales ni comandos Git de escritura.

## Inventario para guardar manualmente el avance (2026-10-20)

Se preparó un [inventario clasificado](fase-4-inventario.md) y una [selección exacta de 636 rutas](fase-4-seleccion-final.txt): código 73, migraciones V9–V13 cinco, pruebas/fixtures 15, documentación 12 y evidencias necesarias 531. Hay [597 rutas normales](fase-4-seleccion-normal.txt) y [39 logs ignorados revisados](fase-4-seleccion-ignorados.txt); el inventario contiene los comandos manuales y limita `git add -f` a esos logs. Rama comprobada `fase-4-etapas-a5-a11`; índice vacío durante la revisión. **No se preparó el índice ni se ejecutaron comandos Git de escritura.**

Quedan fuera Header.tsx, archivos privados/temporales, cambios ajenos, evidencias pendientes de fase 2 y arnés 3.B3; los archivos originales se conservan. El escaneo y la revisión de coincidencias no detectaron secretos reales en la selección. Se comprobaron las referencias locales del informe y de los Markdown seleccionados, conservando sus evidencias y dependencias ya rastreadas. Detalles sin valores sensibles en la [revisión del inventario](fase-4-revision-inventario.json). No se repitieron pruebas, builds ni backend para elaborar esta selección.

El estado a guardar sigue siendo **pruebas locales aprobadas en las ejecuciones documentadas**, **objetivo adicional de cobertura frontend ≥80 % pendiente**, y **recorrido A1–Fin bloqueado por A6/A9/A10**. **Fase 4 abierta; fase 3 abierta con 3.B3 pospuesto.** Preparar o realizar posteriormente un commit no equivale a cerrar esas fases ni a aprobar la integración real.

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
