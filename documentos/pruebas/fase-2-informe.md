# Fase 2 — Informe de pruebas del núcleo

> **Estado vigente, 2026-10-16: FASE 2 CERRADA EN DESARROLLO por validación expresa de Richard, exclusivamente con datos ficticios. PRODUCCIÓN NO AUTORIZADA.** Se conservan P-11, las decisiones provisionales y el backlog documentado. 2.B1/2.B2 respaldados por la última regresión aprobada (backend 484 PASS, 93,00 % de líneas; frontend 219 PASS, 91,99 % instrumentadas). 2.B3 aporta integración real con API, PostgreSQL y nginx aislados, sin cobertura de código nueva. No se ejecutaron suites nuevas para registrar esta validación. Las menciones anteriores a cierre pendiente describen el estado previo a la autorización del último apartado.

Registro histórico inicial, 2026-10-15: **2.B1 y continuación autorizada exclusivamente en 2.B2**. En esa ejecución R4 seguía pendiente, 2.B2 tenía defectos y 2.B3 no estaba iniciado. Los apartados posteriores sustituyen ese estado.

**Resultado histórico de la primera ejecución de 2.B2:** `check` ejecutó **411 pruebas: 397 PASS, 14 FAIL, 0 errores y 0 omitidas**. Tres hallazgos nuevos (lectura de flujos, concurrencia y TRUNCATE del contador), separados de R4 conocido. Cobertura global de líneas **90,79 %** y verificación del umbral aprobada por separado; `check` seguía fallido por las pruebas. Detalle, casos reproducibles y límites en «Paso 2.B2»; los apartados posteriores registran las correcciones y revalidaciones.

Resultado histórico de **2.B1**: **246 pruebas ejecutadas, 245 aprobadas, 1 fallo, 0 errores y 0 omitidas** en la selección de clases `*Test`. Se añadieron **60 casos ejecutables** (incluidas variantes parametrizadas): **59 funcionales aprobados y 1 prueba de arquitectura en rojo**. Los 186 casos anteriores seleccionados pasaron. No se afirma que toda la suite de integración ni `check` estuvieran en verde en ese paso.

## Fuentes, decisiones y conservación del trabajo

- Manual `documentos/manual-paso-a-paso.docx`, apartado «Paso 2.B1 Motor y permisos» y separación de 2.B2. Lectura del OOXML con acceso compartido de solo lectura: el documento estaba abierto por otro proceso. No se modificó el DOCX.
- `CLAUDE.md`; `.claude/PRPs/plans/fase-2-motor-flujo.plan.md` (decisiones P-02 a P-18, S-1 a S-8, resultados de implementación y estrategia B); `documentos/requerimientos.md`, especialmente 4.2, 4.4 y 5.
- ADR-0004 (motor versionado por datos), ADR-0006 (plazos Ecuador), ADR-0007 (documentos sin validación de contenido), ADR-0009 (Ecuador), ADR-0011 (arquitectura), ADR-0012 (transacciones externas; outbox propuesto), ADR-0015 (datos ficticios); índice de ADR.
- `documentos/arquitectura.md` §§2, 4, 7 y 8; secciones relacionadas de `documentos/modelo-datos.md` y `documentos/api/openapi.yaml`. El contrato se consultó como referencia; no se probaron endpoints.
- Skills leídas: `ecc-springboot-tdd`, `ecc-tdd-workflow` y revisión `ecc-java-reviewer` con su referencia original. Gradle/JUnit 5/AssertJ/Mockito/ArchUnit/JaCoCo detectados en `backend/build.gradle`.

Rama comprobada al inicio y al final: `fase-2-motor-flujo`; HEAD `990c35a9cdcfad562e302966d79b7fa35a602ee2`. Había nueve archivos rastreados modificados y numerosos archivos sin seguimiento de la implementación, plan, configuración y documentación. Se conservaron. Este trabajo solo añade pruebas y este informe con sus evidencias. No se alteraron pruebas preexistentes, producción, migraciones ni decisiones.

Se aplica la instrucción expresa de Richard sobre las skills: **sin implementación para conseguir verde, sin refactor de producción, sin checkpoints/commits ni push**. Es verificación de un núcleo ya implementado, no un ciclo TDD previo a esa implementación. Los PASS de caracterización no se presentan como evidencia de un RED anterior. El RED real de R4 se conserva para Claude Code.

## Comportamientos aprobados y provisionales

| Condición | Expectativa usada en las pruebas | Fuente/estado |
|---|---|---|
| Transiciones | A1→A2, A2→A3, A3→A4 o A2, A4→A5 o A2 | Requerimientos 4.4; aprobadas |
| Alcance habilitado | Acciones con origen A1 a A4; A4→A5 permitido; actuar desde A5 bloqueado | P-04, equipo aprobado |
| Una etapa y reentrada | Exactamente una ACTUAL; entrada nueva reinicia el plazo normal | Invariantes 4.2 |
| Rechazo/devolución | Observación obligatoria; producto nuevo también exige urgencia | Requerimientos 4.2 y 4.4 |
| Urgencia | 4/12/24 horas cubren Codificación y Revisión hasta volver a A4 | Aprobado por cliente |
| Asignación | Sin asignación: rol activo o admin; asignada: esa persona activa con rol, o admin; perder rol/desactivarse no habilita al resto | P-06, equipo aprobado |
| Reasignación | Solo admin vigente; candidato activo con el rol responsable | P-07, valor provisional aprobado por equipo; otro rol pendiente del cliente |
| Plazos | 24 h A2/A3; 3 días A4; sin plazo distinto de PENDIENTE, sin inventar vencimiento de Fabricación | ADR-0006, S-2 y decisiones del equipo |
| Calendario | America/Guayaquil; días hábiles y fin de semana al lunes | ADR-0006 aprobado |
| Jornada/feriados | Lunes-viernes completos, 24 h por día, sin descontar feriados | P-09 provisional, no jornada 8–17 inventada |
| Urgencias/rechazo | Horas hábiles; A3→A2 conserva el vencimiento original | P-08/P-10 provisionales, cliente pendiente |
| Asignación al reentrar | Se conserva | Resultado de implementación, provisional; no se declara regla aprobada del cliente |
| Numeración | Mínimo cuatro dígitos, 10000 íntegro; año local Ecuador | P-03 técnico aprobado; formato DK-EC-año y separación anual siguen provisionales |
| Documentos pendientes | Vacío con estado PENDIENTES no equivale a «sin requisitos»; bloquea avance | S-2/S-6 |
| Documentos de entradas previas | No se establece si cumplen en una reentrada | P-18 pendiente; sin requisito nuevo |

Los documentos presentes de los recorridos **puros** son tipos ficticios suministrados a `AccionSolicitada`, para aislar el agregado. No representan archivos reales, metadatos suficientes ni una integración aprobada. Las pruebas del **caso de uso** mantienen A1/A2 bloqueadas con `RequisitoFaltante` porque no existe un módulo de archivos verificable. No se implementó ese módulo.

## Matriz de garantías y pruebas

Rutas relativas a `backend/src/test/java/com/decokasa/seguimiento/`.

| Área | Prueba/archivo | Resultado final y límite |
|---|---|---|
| A1–A4 y A4→A5 | `pedidos/domain/NucleoFaseDosTest`: recorrido; `flujos/application/FlujoFaseDosTest`: siete transiciones parametrizadas | PASS; entrada en A5 con plazo pendiente, sin habilitar A5 |
| Motor por datos/versiones | `FlujoFaseDosTest`: versión histórica, vigente diferente, ausencias, retorno de urgencia deducido de datos en etapa ficticia 8 | PASS; no cambia de versión silenciosamente; no verifica siembra SQL |
| Etapa activa/estado | `NucleoFaseDosTest`: recorrido, reconstrucción con cero/dos ACTUAL | PASS; estados de salida y única ACTUAL |
| Etapa esperada vieja | `NucleoFaseDosTest`; `pedidos/application/AccionNucleoFaseDosTest` | PASS; sin actualización ni segundo evento; modela segunda lectura tras primer avance |
| Observaciones | Tres variantes rechazo/error/producto nuevo; observación en blanco incluso como admin | PASS; `faltantes=[observación]`, sin avance/evento |
| Documentos | Dos variantes Solicitud/Documentos codificados en dominio y caso de uso | PASS; texto de observación no suple documento; admin tampoco lo omite |
| Documentos pendientes | `NucleoFaseDosTest.documentosPendientesNoSignificaSinRequisitos` | PASS; `TransicionNoPermitida` sin cambios/evento |
| Reentradas | Rechazo A3→A2 y error A4→A2 | PASS; contador, entrada nueva, salida nula, vencimiento normal nuevo y etapas DEVUELTAS |
| Plazos | `plazos/domain/CalendarioFaseDosTest` | PASS; definido, SIN_PLAZO/PENDIENTE distintos, tres días, plazo cero inválido |
| Urgencias | Tres niveles en dominio y cálculo; falta de urgencia | PASS; conserva hasta A4, termina con marca en evento; conservación tras rechazo es provisional |
| Fines de semana | Siete variantes de horas; inicio viernes/sábado/domingo, medianoche y noche laboral | PASS para calendario provisional; también días hábiles y corridos con traslado al lunes |
| Permisos/asignación | `NucleoFaseDosTest`: rol correcto/incorrecto, asignado/otro/admin, ejecución denegada sin efectos | PASS; no se amplían permisos si el asignado deja de ser elegible |
| Estado vigente | `AccionNucleoFaseDosTest`: token anterior admin/rol frente a cuenta desactivada o rol actual distinto | PASS; caso de uso consulta el puerto de responsables |
| Reasignación | `NucleoFaseDosTest`; `pedidos/application/CrearReasignarFaseDosTest` | PASS; solo admin, candidato vigente del rol, etapa vieja rechazada, repetición sin evento, candidato desactivado rechazado |
| Creación/número | `CrearReasignarFaseDosTest`; cinco variantes en `NucleoFaseDosTest` | PASS; 0001/0007/9999/10000/10001; a 2027-01-01 02:00Z usa año 2026 de Ecuador; permiso previo a reservar número |
| Eventos/historial | `historial/application/HistorialFaseDosTest`, dominio y casos de uso | PASS; creación, envío, aprobación, rechazo, dos devoluciones y reasignación preservan actor/rol/hora/etapa; registros anteriores conservados, detalle de urgencia y lista de eventos inmutable |
| Arquitectura existente | `arquitectura/ReglasArquitecturaTest`, siete reglas | PASS: R1/R2/R3, R4 entre dominios, R9 y dependencias hacia dentro |
| R4 aplicación entre módulos | `arquitectura/FronterasFaseDosTest` | **FAIL reproducible**, 12 dependencias; D01 abajo |

## Comandos y resultados

PowerShell sin perfil; directorio de Gradle: `backend/`. No se ejecutaron comandos del plan de arranque, Docker, instalaciones, `bootRun` ni pruebas `*IT`. Los tests nuevos usan reloj fijo, UUID ficticios y mocks locales; no usan DB_URL ni credenciales. La regresión incluye el slice MVC anterior de salud, sin base de datos ni servidor real.

| Comando/acción | Resultado real |
|---|---|
| `git status --short`; `git branch --show-current`; `git rev-parse HEAD` | Rama/HEAD anteriores; cambios previos conservados |
| `rg --files` y lecturas `Get-Content` de fuentes y pruebas | No existían pruebas de flujos/pedidos/plazos/historial; suite previa de arquitectura cubría R4 solo entre dominios |
| Lectura DOCX `ZipFile.OpenRead` | Bloqueo por documento abierto; segundo intento requirió cargar `System.IO.Compression`; lectura final correcta con `FileShare.ReadWrite`, sin cerrar Word ni modificar archivo |
| `java -version` | Temurin 21.0.12.1 |
| `.\gradlew.bat --version` | Primer intento: acceso denegado al `.zip.lck` de caché; repetido con aprobación fuera del sandbox: Gradle 9.7.1 correcto |
| `.\gradlew.bat test --tests '*NucleoFaseDosTest' --tests '*ReglasArquitecturaTest' --console=plain` | BUILD SUCCESSFUL, primera selección |
| `.\gradlew.bat test --tests '*FaseDosTest' --tests '*ReglasArquitecturaTest' --console=plain` | Primera ampliación: 48 casos, 1 fallo en expectativa de la prueba de calendario: se había confundido 23:00Z con 23:00 Ecuador. Se corrigió **solo el dato de prueba** a 2026-10-17T04:00Z (viernes 23:00 Ecuador). No es un defecto de producción ni un RED TDD válido de negocio |
| Mismo comando tras añadir flujo/historial/frontera | 59 casos, 1 fallo: R4; el caso de calendario ya pasa |
| `.\gradlew.bat test --tests '*Test' --console=plain` | Regresiones incrementales: 238 y 244 casos, solo R4 falló |
| `.\gradlew.bat test --tests '*Test' --console=plain 2>&1 \| Tee-Object -FilePath ../documentos/pruebas/evidencias-fase-2/regresion-unitaria.log; exit $LASTEXITCODE` | **Final: 246 casos, 1 fallo; exit 1**, BUILD FAILED en 14 s; 0 errores/omitidas en XML |
| `.\gradlew.bat jacocoTestReport -x test --console=plain 2>&1 \| Tee-Object -FilePath ../documentos/pruebas/evidencias-fase-2/cobertura.log; exit $LASTEXITCODE` | BUILD SUCCESSFUL; genera reporte de ejecución anterior, **no** oculta ni vuelve verde la suite fallida |
| Lectura de `TEST-*.xml`, exportación de `resultados.csv`, contadores LINE de JaCoCo y copia XML | Evidencias persistidas en directorio enlazado abajo |
| `git diff --check`; revisión Java y búsqueda de acceso externo en pruebas nuevas | Sin errores de whitespace en diff rastreado; tests nuevos sin arranque completo, acceso JDBC, sleeps ni acceso a secretos |

Los primeros comandos con perfil produjeron un aviso de permisos de `fnm`; se usó PowerShell sin perfil después. Una lectura agrupada usó sintaxis de llaves de Bash no válida en PowerShell y otra tuvo rutas relativas duplicadas desde `backend/`; se repitieron las lecturas con rutas correctas. Estos fallos de herramienta no cuentan como pruebas fallidas de negocio. El aviso de JVM sobre class sharing al instrumentar Mockito aparece como `NativeCommandError` al redirigir stderr en Windows PowerShell; el fallo de Gradle se confirma por el XML y corresponde a R4.

## Cobertura medida

JaCoCo de la **última ejecución `*Test`**, sin integrar los resultados antiguos de las clases `*IT`. XML conservado en [evidencias-fase-2/jacoco.xml](evidencias-fase-2/jacoco.xml); contadores en [cobertura.csv](evidencias-fase-2/cobertura.csv).

| Ámbito de líneas | Cubiertas | Sin cubrir | Porcentaje |
|---|---:|---:|---:|
| pedidos: domain + application | 345 | 32 | 91,51 % |
| flujos: domain + application | 76 | 11 | 87,36 % |
| plazos: domain + application | 46 | 0 | 100 % |
| historial: domain + application | 64 | 5 | 92,75 % |
| Backend completo con esta selección | 1196 | 792 | 60,16 % |

Cada núcleo nuevo supera el 80 % de líneas en esta selección. El 60,16 % global incluye adaptadores que requieren integración; **no satisface la puerta global de `check`**. No se ejecutó `jacocoTestCoverageVerification`, ni se alteró su umbral; tampoco se excluyó código de producción del cálculo. Cobertura de líneas no demuestra concurrencia ni atomicidad de transacciones.

## Hallazgo reproducible para Claude Code

### 2.B1-D01 — R4: aplicación de pedidos depende del dominio interno de flujos

Estado: **abierto, RED confirmado**. Prioridad: media para este paso; impide declarar arquitectura conforme. No se ha observado pérdida de datos por este hallazgo en las pruebas puras.

**Fuente esperada:** `documentos/arquitectura.md` §2 R4 y ADR-0011: un módulo usa a otro solo por puertos de entrada o eventos, nunca por dominio interno. No se encontró una excepción aprobada para estos accesos. El plan los ejemplifica, pero eso no sustituye una decisión de arquitectura.

**Reproducir**, desde backend, sin PostgreSQL:

```powershell
.\gradlew.bat test --tests '*FronterasFaseDosTest' --console=plain
```

Este selector individual sirve para repetir el caso; la prueba se ejecutó realmente dentro de las selecciones ampliada y final. Resultado observado: `Architecture Violation ... was violated (12 times)`.

**Ubicaciones:** `pedidos/application/usecase/CrearPedidoCasoDeUso.java` líneas 58–69 y tipo de retorno del método `definicionVigente`; `ReasignarEtapaCasoDeUso.java:47`. El puerto `flujos/application/port/in/ConsultarDefinicionFlujo.java` devuelve `flujos.domain.DefinicionFlujo`; los consumidores llaman `etapaInicial`, `etapa`, `rolResponsable`, `plazo`, `numero`, etc. El puerto público expone así tipos internos que atraviesan la frontera.

**Esperado:** los consumidores de pedidos obtienen lo necesario mediante contratos permitidos de entrada/eventos y pasan R4, o se documenta y aprueba expresamente un cambio de regla. Devolver a Claude Code para corregir respetando el contrato y las decisiones; **no** quitar, deshabilitar ni relajar la prueba para obtener verde. No se prescribe aquí una nueva arquitectura ni se modifica producción.

**Evidencia:** [XML de la prueba final](evidencias-fase-2/TEST-com.decokasa.seguimiento.arquitectura.FronterasFaseDosTest.xml), [primer RED conservado](evidencias-fase-2/fronteras-red.xml) y [salida de regresión](evidencias-fase-2/regresion-unitaria.log). Las siete reglas anteriores siguen verdes: eso no cubría este hueco de R4.

## Incompatibilidad existente reservada a persistencia

### 2.B1-I01 — MigracionesIT presupone exclusivamente V1

Inspección estática, **no ejecutada en 2.B1**. No se registra como fallo observado de PostgreSQL ni como defecto confirmado de V2.

`backend/src/test/java/com/decokasa/seguimiento/MigracionesIT.java` aplica todas las migraciones de `classpath:db/migration`, pero:

- `flywayAplicaLaVersionUnoSobreUnaBaseVacia` exige `targetSchemaVersion == "1"` (línea 76).
- `reiniciarFlywayValidaLaMigracionSinReaplicarla` exige versión actual `"1"` y una sola migración aplicada (líneas 90–91).
- `creaSoloLasTablasDeUsuariosRolesYPaises` exige exactamente las siete tablas V1 y comenta que no hay pedidos/etapas (líneas 96–104).

Con V2 presente esas expectativas exclusivas dejan de corresponder al esquema completo. **Conservar las comprobaciones de V1**; en 2.B2 decidir cómo separar validación histórica V1 y evolución V2 sin retirar garantías de usuarios/roles/países. No se editó el archivo. SHA256 leído: `A7583E15795F5866025539EAD5651DF5C5570D424048E7A8E8C8B4783CCFDF8B`. Comprobación final: `git hash-object` del archivo y `git rev-parse HEAD:backend/src/test/java/com/decokasa/seguimiento/MigracionesIT.java` coinciden en `3661971e7a97aa92506fcec6244efcc733a60e9b`; `git diff --name-only -- backend/src/test` no muestra cambios a pruebas rastreadas anteriores.

## Bloqueos y límites pendientes

1. **D01** requiere corrección de Claude Code y revalidación de R4. Aquí no se corrige producción.
2. **P-05/P-16/P-17/P-18:** decisiones de archivos y su contrato pendientes. A1/A2 reales siguen bloqueadas por documentos; los PASS de dominio con entradas ficticias no habilitan un recorrido operativo real.
3. **Unicidad y no reutilización de números confirmados:** el formateo y la llamada al generador están comprobados; reservar números únicos entre transacciones, rollback y no reutilización requieren PostgreSQL real en 2.B2. No se simula un generador para «probar» esas garantías. No se exige ausencia de huecos.
4. **Simultaneidad/atomicidad:** se comprobó el rechazo de la segunda acción después de que cambie la etapa y ausencia de actualización/evento en rechazos por requisitos/permisos. El bloqueo real, dos hilos con transacciones, rollback ante fallo al guardar/publicar y acción+historial atómicos se reservan a 2.B2. Un mock no demuestra `SELECT FOR UPDATE`.
5. **P-11/reentrada:** `etapaEsperada` no distingue A3→A2→A3 de la primera entrada en A3. Riesgo ya documentado, sin añadir una versión obligatoria al comando ni declarar como aprobado que un comando de esa primera entrada deba rechazarse.
6. **Historial:** traducción/inserción y conservación en memoria verificadas. Triggers contra UPDATE/DELETE/TRUNCATE, restricciones, persistencia de actor y hora y exposición del número tras commit pendientes de 2.B2. No se afirma que estas pruebas puras los demuestren.
7. **Calendario y políticas provisionales:** P-07/P-08/P-09/P-10 y conservación de asignación al reentrar permanecen identificados; si se decide otro comportamiento, cambiar expectativas con esa decisión. No se fijaron feriados, jornada, documentos de Incidencias ni 31/45 días como requisitos nuevos.

## Evidencias y cierre del trabajo autorizado

- [Resultados por clase](evidencias-fase-2/resultados.csv): 246 ejecutadas, 1 fallo.
- [Estado final de Git](evidencias-fase-2/estado-final.txt): cambios previos y archivos añadidos, sin commit.
- [Salida final Gradle](evidencias-fase-2/regresion-unitaria.log) y [generación de cobertura](evidencias-fase-2/cobertura.log).
- `evidencias-fase-2/TEST-*.xml`: las siete clases nuevas y las siete reglas de arquitectura anteriores; cero omisiones. XML de regresión total resumido en CSV; HTML local disponible en `backend/build/reports/tests/test/index.html` hasta la siguiente ejecución.
- Revisión Java local: Spring detectado en build.gradle; revisión de las clases nuevas, estado mutable aislado por test, datos ficticios, reloj fijo, mocks explícitos, sin excepciones ocultas, sin sleeps ni IO externo. `check` de la skill se acotó al `test --tests '*Test'` autorizado para no adelantar persistencia; no equivale a aprobar el build completo.

**Entrega de 2.B1:** pruebas y evidencia listas para revisión; **resultado no verde por D01**. No se arrancó la aplicación contra ninguna base, no se tocaron datos/volúmenes, no se implementaron endpoints ni archivos, no se hicieron commits ni push y no se avanzó a 2.B2. La fase 2 permanece abierta y requiere corrección/revalidación y los pasos posteriores cuando Richard los autorice.

## Paso 2.B2 — Persistencia y contraste de contrato

Continuación autorizada por Richard el 2026-10-15. Las secciones anteriores conservan el resultado histórico de 2.B1; los pendientes de persistencia se actualizan aquí. **2.B2 no está aprobado: hay defectos reproducibles y endpoints bloqueados.** La fase sigue abierta; no se avanzó a 2.B3.

### Preparación y límites de seguridad

Se releyeron el manual DOCX (apartado 2.B2, por OOXML de solo lectura), el plan de fase 2 —incluidos P-03/P-06/P-07/P-11/P-15, S-1 a S-8 y entrega parcial de 2.4— y este informe. Se aplicaron `ecc-database-reviewer` con su referencia original, `ecc-api-design`, `code-review` con su referencia original en modo local y `ecc-java-reviewer` para las pruebas Java. Las sugerencias genéricas de las skills no sustituyen el contrato ni decisiones del proyecto: no se añadieron RLS, formatos de error, límites o políticas de negocio nuevos.

Rama: `fase-2-motor-flujo`; HEAD sigue en `990c35a9cdcfad562e302966d79b7fa35a602ee2`. Cambios previos conservados. Se modificó **solo la prueba preexistente `MigracionesIT`**, con autorización expresa, y se añadieron `V2RestriccionesIT` y `pedidos/PersistenciaFaseDosIT`, documentación y evidencias. Los 162 archivos de `backend/src/main` (incluidas las migraciones) tienen hashes SHA256 idénticos antes/después. La prueba de R4 también tiene hash idéntico; no se deshabilitó ni debilitó. Véase [integridad](evidencias-fase-2/2.B2/integridad.txt) y sus manifiestos.

Todas las bases provienen de `new PostgreSQLContainer("postgres:17")`, sin reutilización ni montaje de volúmenes existentes. `MigracionesIT` y `V2RestriccionesIT` usan únicamente la URL/usuario/contraseña de sus propios contenedores. La clase JPA importa `TestcontainersConfiguration` con `@ServiceConnection`, deshabilita la importación de `.env.local` y la semilla, exige `ddl-auto=validate` y comprueba que la URL JDBC efectiva coincide con la del contenedor. No se usó `DB_URL` para las nuevas pruebas.

La regresión completa también usa los Testcontainers ya definidos por las pruebas anteriores; algunas pruebas de acceso levantan su servidor de prueba en puerto aleatorio **contra su contenedor**, sin usar bases compartidas. No se ejecutaron `docker compose`, `bootRun`, limpiezas de Docker ni borrados de datos/volúmenes existentes. Los DELETE/TRUNCATE de prueba se dirigen exclusivamente a las tablas del contenedor y se revierten con savepoints/rollback. Las dos restricciones temporales `prueba_b2_*` inyectan fallos únicamente en ese contenedor y se retiran en `finally`; no son migraciones ni cambios a producción.

### Pruebas añadidas y resultados finales

| Clase | Casos | PASS | FAIL | Alcance |
|---|---:|---:|---:|---|
| `MigracionesIT` | 40 | 40 | 0 | V1, evolución V2, validación/reinicio y garantías anteriores de acceso |
| `V2RestriccionesIT` | 23 | 22 | 1 | Siembra, claves, restricciones, índices y triggers SQL |
| `pedidos/PersistenciaFaseDosIT` | 21 | 9 | 12 | Cableado integrado y pruebas de componentes JPA/transacciones |
| Resto de la suite | 327 | 326 | 1 | Regresión, incluido R4 conocido |
| **Total `check`** | **411** | **397** | **14** | **0 errores, 0 omitidas** |

Se incorporaron **45 casos adicionales** respecto a 2.B1: uno adicional en `MigracionesIT`, 23 SQL y 21 JPA. [Resultados por clase](evidencias-fase-2/2.B2/resultados-finales.csv), [casos JPA](evidencias-fase-2/2.B2/casos-jpa.csv) y [salida completa](evidencias-fase-2/2.B2/check-final.log).

**Dos niveles de pruebas JPA, sin confundir sus garantías:**

- Los casos **integrados** usan los beans de producción de `CrearPedido`, `EjecutarAccionEtapa`, `ReasignarEtapa` y `ConsultarDefinicionFlujo`. Once fallan por **2.B2-D01** antes de alcanzar su comportamiento objetivo. Se conservan en rojo. La prueba directa del generador sí pasa.
- Los casos cuyo nombre empieza por **`componente`** aíslan la frontera de `flujos`: suministran una definición/resolución ficticia como entrada al caso de uso. Usan **los adaptadores reales** de pedidos, secuencia, usuarios e historial, el publicador/oyente reales y transacciones PostgreSQL reales. No reemplazan beans ni arreglan el adaptador de flujos. Ocho pasan; la prueba de concurrencia falla por **2.B2-D02**, independiente del bloqueo anterior.

Preparar A3/A4 requiere tipos documentales ficticios en el agregado: el caso de uso real no puede completar A1/A2 mientras no exista archivos. Esa preparación solo sirve para aislar persistencia; no constituye un recorrido operativo ni acredita documentos reales. La conservación de asignación al reentrar, las horas hábiles de urgencia y el calendario siguen con el carácter provisional documentado en 2.B1.

| Comprobación solicitada | Evidencia ejecutable | Resultado y límite |
|---|---|---|
| V1 y V2 en base vacía | `MigracionesIT`: primero target 1, captura tablas V1, luego V2; `V2RestriccionesIT.@BeforeAll`: ambas de una vez en otro contenedor vacío | **PASS**. V1 aplica una migración con sus siete tablas; V2 conserva esas tablas y añade ocho; historial Flyway exactamente 1/2; validate y segundo migrate sin reaplicar |
| Comprobaciones V1 válidas | 40 casos de `MigracionesIT` | **PASS**. Se mantienen roles/países, ausencia de usuarios reales sembrados, unicidad, formatos, longitudes, relaciones y renovación. Solo se actualizan las expectativas que exigían exclusivamente V1. **2.B1-I01 queda resuelto como incompatibilidad de prueba** |
| Siembra | `siembraExactaDeOnceEtapasSinInventarPlazos`, `catorceTransicionesConRequisitosAprobadosYPendientes` | **PASS** SQL: un flujo EC/A/v1 vigente; 11 etapas, roles y Fin automático sin rol; 14 transiciones con destino/observación/urgencia/bodega exactos |
| Documentos/plazos | Misma siembra SQL | **PASS**. Solo Solicitud y Documentos codificados; A6/A9/A10 con documentos PENDIENTES; A5/A7/A10 con plazo PENDIENTE, A1/Fin SIN_PLAZO; 24 h/3 días/10 días según lo aprobado. No se inventan 31/45 días, Packing List+BL separados ni ELEGIR_BODEGA |
| Lectura de esa siembra por JPA | `adaptadorDeFlujosConservaDocumentosYPlazosPendientes` | **FAIL, D01**. El contenido SQL correcto no implica que su adaptador pueda leerlo |
| Restricciones y FK | `numeroUnicoYReferenciasDePedido`, cuatro variantes de etapa, `clavesForaneasDeTransicionesDocumentosYEventos`, `checksDePlazoResponsableYUrgencia` | **PASS**. Unicidad de número; usuario/país/tipo/flujo coherentes; destino/documento/pedido/usuario existentes; estado, número, reentradas, responsable/plazo y urgencia completa/vencimiento válidos |
| Una sola ACTUAL | `indiceUnicoRechazaDosActualesYPermiteCambioA4A2EnOrden` | **PASS**. Violación 23505 con dos ACTUAL, índice único parcial real; cerrar A4 antes de abrir A2 funciona. Exactamente una sigue siendo invariante de dominio, no garantía general del índice |
| Historial solo inserción | Tres variantes SQL UPDATE/DELETE/TRUNCATE | **PASS**. Las tres operaciones son rechazadas con P0001 por los triggers, también con el dueño del esquema del contenedor |
| Identidad y borrado | Cuatro variantes SQL de número/país/tipo/flujo, `pedidoNoSeBorra` | **PASS**. Triggers rechazan cambios y DELETE |
| Contador | Cuatro variantes de retroceso/DELETE/cambio de país/año | **PASS**; `TRUNCATE` adicional **FAIL, D03** |
| Reconstrucción JPA y validate | `componenteValidaJPAContraV2YConexionExclusivaDeTestcontainers` | **PASS**. Hibernate inicia con validate; 11 etapas, número/creador/fecha y evento reconstruidos; URL efectiva de Testcontainers |
| A4→A2, asignación/reentrada/urgencia | Tres variantes `componenteA4A2PersisteUrgenciaAsignacionReentradaYUnaActual` | **PASS por componentes**. Sin colisión del índice; A2 única ACTUAL, A3/A4 DEVUELTAS, asignación conservada, reentrada 1, entrada/salida y plazo normal reiniciados, urgencia 4/12/24 persistida y reconstruida |
| Actor/rol/hora y JSON de historial | Casos de componentes de creación y devolución | **PASS** persistido/releído: UUID del actor, rol admin, instante del reloj fijo, destino y nivel. No prueba serialización HTTP con offset -05:00 |
| Dos avances simultáneos | `componenteDosAvancesSimultaneosConfirmanUnoYElOtroEtapaCambio` | **FAIL, D02**. Bloqueo real observado; solo un avance/evento/versionado confirmado y una ACTUAL; la segunda operación devuelve excepción incorrecta. Caso integrado queda bloqueado por D01 |
| Creaciones simultáneas | `componenteCreacionesSimultaneasUsanContadorRealYNumerosDistintos` | **PASS por componentes**. Dos hilos, transacciones reales, dos números diferentes, dos pedidos y un evento por pedido confirmados. No se exige que sean consecutivos. Integración completa bloqueada por D01 |
| 10000 sin truncar | `generadorNoTruncaDiezMilEnPostgres`, `componenteNumero10000SeConfirmaEnPedidoSinTruncar` | **PASS**. Generador entrega 10000 y 10001; pedido 10000 se inserta, confirma y reconstruye íntegro |
| Rollback de creación | `componenteRollbackCreacionRevierteContadorYPermiteReusarSoloNumeroNoConfirmado` | **PASS por componentes**. Fallo real al insertar evento: no quedan pedido/etapas/evento; valor y número de filas del contador vuelven al estado anterior. La creación siguiente usa el número que nunca se confirmó: lectura A aprobada; no convierte ausencia de huecos en requisito |
| Fallo al guardar historial | `componenteFalloHistorialRevierteAccionYVersion` | **PASS por componentes**. El UPDATE de acción se había vaciado; un CHECK temporal falla al INSERT de evento; tras rollback las filas de pedido/etapas/versiones y número de eventos coinciden con la instantánea anterior |
| Reasignación/autorización con cableado completo | `reasignacionPersisteYRolAjenoNoActuaAunqueSuTokenDigaAdmin` | **Bloqueado por D01**, no acreditado por ese caso. La asignación como dato persistido sí está probada por componentes; reglas puras siguen cubiertas por 2.B1 |

### Comandos, incidencias de preparación y cobertura

Todos los comandos Gradle se ejecutaron desde `backend/`, PowerShell sin perfil. Se guardó la salida con `2>&1 | Tee-Object -FilePath ../documentos/pruebas/evidencias-fase-2/2.B2/<archivo>; exit $LASTEXITCODE` para conservar el código real de salida.

| Comando | Resultado real |
|---|---|
| `git status --short`; `git branch --show-current`; lecturas de manual/plan/informe/skills | Rama correcta, cambios previos preservados; autorización limitada a 2.B2 |
| `docker version --format '{{.Server.Version}}'` | Sandbox: acceso denegado al named pipe. Repetido con aprobación: Docker 29.8.2 accesible. No se inspeccionaron ni manipularon volúmenes existentes |
| `.\gradlew.bat test --tests '*MigracionesIT' --tests '*V2RestriccionesIT' --console=plain` | 70 pruebas, 1 fallo (TRUNCATE del contador); incluye 7 reglas ArchUnit previas descubiertas también por el runner. [sql.log](evidencias-fase-2/2.B2/sql.log) |
| `.\gradlew.bat test --tests '*PersistenciaFaseDosIT' --console=plain` | Primera preparación: 19 pruebas, 12 fallos por pasar Instant sin conversión a JDBC. Se corrigió solo el fixture con `Timestamp.from`. No cuenta como defecto de producción |
| Mismo comando tras corregir fixture | 19 pruebas, 11 fallos por D01; evidencia [jpa-integrado-red.xml](evidencias-fase-2/2.B2/jpa-integrado-red.xml). Generador y reglas previas pasaron |
| Mismo comando al añadir pruebas de componentes | 27 pruebas, 12 fallos: D01 y concurrencia D02. [jpa.log](evidencias-fase-2/2.B2/jpa.log), [XML](evidencias-fase-2/2.B2/jpa-componentes-red.xml) |
| `.\gradlew.bat test --tests '*PersistenciaFaseDosIT.componenteDosAvances*' --console=plain` | 8 pruebas, 1 fallo: caso forzado D02 más siete reglas previas. [Log](evidencias-fase-2/2.B2/concurrencia-forzada.log) y [XML](evidencias-fase-2/2.B2/concurrencia-forzada.xml) |
| `.\gradlew.bat check --console=plain` | **411 pruebas, 14 fallos; exit 1; BUILD FAILED en 59 s**. No se alteraron pruebas para ocultarlos. [check-final.log](evidencias-fase-2/2.B2/check-final.log) |
| `.\gradlew.bat jacocoTestReport jacocoTestCoverageVerification -x test --console=plain` | **BUILD SUCCESSFUL**, sin repetir pruebas. JaCoCo: 1805 líneas cubiertas, 183 sin cubrir, **90,79 %**. Umbral global ≥80 % aprobado sobre ejecución completa; no cambia el fallo de `check`. [Log](evidencias-fase-2/2.B2/cobertura-final.log), [XML](evidencias-fase-2/2.B2/jacoco-final.xml) |
| `git diff --check`; comparación de manifiestos con `Compare-Object` | Sin errores de whitespace; cero diferencias en producción/migraciones y en R4. No se creó commit |

El aviso de class sharing de JVM/Mockito redirigido por PowerShell se conserva como `NativeCommandError`; no es la causa de los fallos. JUnit XML registra 14 failures y cero errors. No se usaron exclusiones ni `@Disabled`. El `-x test` de JaCoCo sirve exclusivamente para leer la ejecución completa ya terminada; no convierte en exitosas las pruebas que fallaron.

### Hallazgos nuevos para Claude Code

Los siguientes selectores permiten repetir cada defecto. Se ejecutaron dentro de las selecciones indicadas arriba; D02 también con su selector individual. **No se modificó producción ni migraciones para corregirlos.**

#### 2.B2-D01 — La lectura de transiciones aplica el grafo al tipo incorrecto

**Prioridad alta. Abierto; bloquea la lectura de flujo y crear pedidos con el cableado real.**

Ubicación: `flujos/adapter/out/persistence/DefinicionFlujoDatos.java:28–30`, `@EntityGraph(attributePaths = "documentos")` en un repositorio cuyo tipo raíz es `DefinicionFlujoEntity`, para una consulta que devuelve `TransicionPermitidaEntity`. El atributo sí existe en la entidad de transición, no en la de flujo.

```powershell
.\gradlew.bat test --tests '*PersistenciaFaseDosIT.adaptadorDeFlujosConservaDocumentosYPlazosPendientes' --console=plain
```

**Preparación:** contenedor PostgreSQL 17 vacío; Flyway V1/V2; contexto JPA validate. La prueba llama al bean real `ConsultarDefinicionFlujo.vigente(EC,A)`.

**Observado:** `InvalidDataAccessApiUsageException`, causa `IllegalArgumentException: Unable to locate Attribute with the given name [documentos] on this ManagedType [...DefinicionFlujoEntity]`. Stack: `DefinicionFlujoDatos.transicionesDe` → `DefinicionFlujoJpaRepositorio.aDominio:51` → caso de uso. El mismo error bloquea once casos integrados; son efectos de este defecto, no once defectos independientes. En los casos de rollback la excepción ocurre antes del fallo de historial objetivo, por eso esos casos no acreditan rollback integrado.

**Esperado:** leer y reconstruir las 11 etapas y 14 transiciones con sus documentos/plazos y permitir que los casos de uso continúen hasta sus validaciones propias. Claude Code debe corregir el acceso a transiciones respetando R4 y revalidar los casos integrados completos; no quitar pruebas ni convertir el error en datos vacíos. No se afirma un estado HTTP: no hay controlador.

Evidencia final: [PersistenciaFaseDosIT XML](evidencias-fase-2/2.B2/TEST-com.decokasa.seguimiento.pedidos.PersistenciaFaseDosIT.xml).

#### 2.B2-D02 — El segundo avance concurrente no produce ETAPA_CAMBIO

**Prioridad alta. Abierto; respuesta de concurrencia incorrecta en el adaptador de pedidos, independiente de D01.**

Ubicación observada: `pedidos/adapter/out/persistence/PedidoJpaRepositorio.java:51–57` y reconstrucción en `aDominio:138–148`; termina en `Pedido.verificarUnaSolaActual` al construir el agregado. La causa interna precisa de la mezcla entre estado del pedido y etapas debe investigarla Claude Code; el stack y las lecturas verifican el fallo, sin asumir una corrección concreta.

```powershell
.\gradlew.bat test --tests '*PersistenciaFaseDosIT.componenteDosAvances*' --console=plain
```

**Preparación reproducible:** pedido ficticio en A3, reglas de transición suministradas como entrada controlada, resto de adaptadores reales. Primera transacción bloquea la fila mediante `PedidoRepositorio.buscarParaActualizar`. Otro hilo pide aprobar A3. La primera espera hasta observar en PostgreSQL que hay un proceso bloqueado por su PID (`pg_blocking_pids`), aplica el avance y confirma. Entonces la segunda prosigue. No depende solo de arrancar dos hilos a la vez ni usa sleeps.

**Observado:** primera confirma; segunda lanza `IllegalStateException: Un pedido en curso tiene exactamente una etapa actual.` antes de llegar al rechazo por etapa esperada. La prueba **sí alcanza y aprueba**, antes de la aserción final fallida: A4 actual, versión +1, solo un evento nuevo, A3 COMPLETADA y sin reentradas extra. Se conserva una ACTUAL en la base; no se observó una segunda acción confirmada ni efectos parciales en este caso.

**Esperado:** primera confirmada, segunda `EtapaCambio` con código `ETAPA_CAMBIO`, mismo estado final sin efectos extra (P-11). Esta prueba de componentes evita el bloqueo D01 exclusivamente mediante entradas controladas del puerto de flujos; **no** demuestra que el recorrido integrado ya funcione. La aserción espera `EtapaCambio`; no acepta `IllegalStateException` como resultado válido. No se atribuye 409 HTTP a esta ejecución.

Evidencias: [caso forzado](evidencias-fase-2/2.B2/concurrencia-forzada.xml) y XML final JPA; el mensaje `B2: bloqueo PostgreSQL observado...` conserva el estado y la segunda excepción.

#### 2.B2-D03 — El contador se puede vaciar con TRUNCATE

**Prioridad media. Abierto; falta una protección de borrado del contador.**

Ubicación: `V2__flujos_pedidos_historial.sql:125–153`: `secuencia_pedido_proteger` cubre UPDATE/DELETE, no TRUNCATE. Se documenta sin modificar la migración.

```powershell
.\gradlew.bat test --tests '*V2RestriccionesIT.contadorTampocoSePuedeVaciarPorTruncate' --console=plain
```

**Preparación:** solo en el contenedor, transacción con fila EC/2026/7. Se intenta `TRUNCATE TABLE secuencia_pedido` dentro de un savepoint.

**Observado:** PostgreSQL permite vaciar la tabla; la prueba falla con «Expecting code to raise a throwable». El `finally` revierte el savepoint y el caso completo se revierte; no se vació ningún contador existente fuera de la prueba.

**Esperado:** rechazar el vaciado, coherente con «contador no retrocede ni se borra». El índice UNIQUE de `pedido.numero` aún impediría confirmar un duplicado, pero no conserva el contador: un reinicio podría provocar colisiones o problemas operativos al reservar. No se afirma que se haya confirmado un número repetido ni pérdida de datos real.

Evidencia: [V2RestriccionesIT XML](evidencias-fase-2/2.B2/TEST-com.decokasa.seguimiento.V2RestriccionesIT.xml).

### Contraste estático con OpenAPI 0.6.0-borrador

Se contrastó `documentos/api/openapi.yaml` vigente con rutas, puertos, entidades y entrega parcial de 2.4. No se editó el contrato ni se implementaron controladores. Las rutas efectivas encontradas pertenecen a salud, auth, usuarios y roles. [Inventario de rutas](evidencias-fase-2/2.B2/rutas-implementadas.txt), [índice del contrato](evidencias-fase-2/2.B2/contrato-indice.txt).

| Contrato o representación | Estado comprobado |
|---|---|
| `GET /flujos/{pais}/{tipoFlujo}` | **No implementado como endpoint.** Puerto/caso de uso y adaptador presentes; lectura falla por D01 |
| `POST /pedidos` | **No implementado como endpoint.** Caso de uso transaccional presente, bloqueado por D01 en cableado real. Números/rollback probados por componentes |
| `GET /pedidos`, `/pedidos/buscar`, `/pedidos/bandeja`, `/pedidos/{pedidoId}` | **No implementados**: tampoco están los casos completos de consulta/listado/proyecciones/accionesDisponibles de la entrega prevista |
| `POST /pedidos/{pedidoId}/acciones` | **No implementado como endpoint.** Caso de uso presente; bloqueos D01/D02. No se han verificado estados HTTP, discriminador JSON, Bean Validation ni RFC 9457 de esa ruta |
| `PUT /pedidos/{pedidoId}/etapas/{numeroEtapa}/asignacion` | **No implementado como endpoint.** Caso de uso presente; integración bloqueada por D01. P-07 continúa provisional respecto a otros roles |
| `POST /pedidos/{pedidoId}/observaciones` | **No implementado.** Existe contrato de evento `ObservacionAgregada`; no su caso de uso/controlador |
| `GET /pedidos/{pedidoId}/historial` | **No implementado como endpoint.** Inserción/lectura interna por repositorio sí probadas; consulta pública/autorización/paginación no acreditadas |
| Archivos, enlaces/descarga y `adjuntoIds` | **No implementados o bloqueados por P-05/P-16/P-17/P-18.** No hay V3 ni módulo de archivos. El comando interno de acción no acepta adjuntoIds y cuenta cero documentos verificables; no simula la ruta del contrato |
| Vencidos/cronograma, etapas posteriores, bodegas y aduana | Fuera del alcance habilitado de fase 2; sus rutas en el contrato no significan implementación |
| `DefinicionEtapa.plazo` (líneas 1656–1660) | Solo `Plazo` o null; no distingue explícitamente **SIN_PLAZO** y **PENDIENTE** como el dominio y V2. Actualización de contrato pendiente de 2.4, sin decidir aquí su forma |
| `DefinicionAccion.documentosObligatorios` (1666–1685) | Lista de documentos, sin estado **PENDIENTES/DEFINIDOS**. No puede deducirse «sin requisitos» de una lista vacía de A6/A9/A10. Propuesta de contrato pendiente |
| Rol nulo de Fin en definición/línea de tiempo | Ya representado por S-1 aprobado; no exige inventar responsable |
| `EventoHistorial` (2038–2074) | Expone usuario con roles actuales de resumen, fecha y urgencia, pero no el **rol de intervención histórico** ni un campo estructurado equivalente a `DetalleEvento` (destino, motivo, fin de urgencia, asignaciones). La persistencia sí guarda esos datos. Propuesta de representación pendiente; no se inventa DTO |
| `PedidoDetalle` y `etapaEsperada` | El contrato no pide versión de pedido; P-11 no se amplía por analogía con usuarios. `etapaEsperada` no distingue reentradas; riesgo anterior continúa |
| Fechas y errores | Se verifican instantes de servidor y excepciones/códigos de dominio en componentes. **No** se declara conformidad HTTP de 201/Location, 400/403/409/422, `faltantes`, offset -05:00 o respuestas paginadas sin endpoints |

Los bloqueos P-13 (visibilidad), P-05 (archivos), P-17 (descarga), P-18 (documentos en reentrada), P-16 (reservas/esquema compartido) y las representaciones pendientes del contrato siguen abiertos. Los valores provisionales de negocio no se convierten en requisitos nuevos por estas pruebas.

### Revisión y entrega de 2.B2

La revisión local `code-review` del alcance de persistencia/contrato concluye **REQUEST CHANGES**: D01 y D02 de prioridad alta, D03 media y R4 conocido de 2.B1 todavía abierto. No es una aprobación de PR ni una revisión integral del frontend o de fases posteriores. No se publicaron revisiones ni mensajes en GitHub.

La compatibilidad de tipos/columnas JPA con V2 pasa `ddl-auto=validate`, pero **validate no detecta** el EntityGraph erróneo ni la carrera de reconstrucción. La cobertura supera el umbral, pero no reemplaza las aserciones fallidas. No se cambiaron configuración de cobertura, dependencias, producción, migraciones, decisiones ni la prueba R4.

**Para devolver a Claude Code:** corregir D01, D02 y D03 dentro del proceso de implementación autorizado y repetir los selectores y `check`; R4 conserva su seguimiento separado. Mantener todas las aserciones y resolver los bloqueos de contrato antes de atribuir conformidad a endpoints. **2.B3 no iniciado, sin commit ni push, fase 2 abierta.**

## Correcciones de Claude Code (2026-10-15) — implementadas, pendientes de revalidación

Correcciones de la parte A sobre los defectos 2.B1-D01, 2.B2-D01, 2.B2-D02 y 2.B2-D03. **No se ejecutaron pruebas, builds ni verificaciones**: la revalidación corresponde a Codex. Ningún defecto se declara cerrado. No se modificaron ni eliminaron pruebas de Codex, ni la regla R4 ni su prueba. No se implementaron endpoints, archivos ni decisiones pendientes del contrato, y no se avanzó al paso 2.5. Sin commit ni push.

### 2.B1-D01 — R4: `pedidos` dependía del dominio de `flujos`

- **Causa:** el puerto `ConsultarDefinicionFlujo` entrega `flujos.domain.DefinicionFlujo`. `CrearPedidoCasoDeUso` y `ReasignarEtapaCasoDeUso` llamaban a sus métodos (`etapaInicial`, `etapa`, `etapas`, `rolResponsable`, `plazo`, `numero`, `id`, `tipo`) y declaraban un método que devolvía ese tipo. Son las 12 dependencias que señala la prueba.
- **Corrección:**
  - un contrato compartido en `compartido.domain.flujo`: `ContratoDeFlujo` (id, tipo, etapas, etapa, etapaInicial) y `ContratoDeEtapa` (numero, rolResponsable, plazo), en Java puro, con el mismo criterio que los eventos de P-15;
  - `DefinicionFlujo` y `DefinicionEtapa` implementan esos contratos;
  - los dos casos de uso solo usan los contratos.
  - El puerto de entrada no cambia de firma: las pruebas de Codex siguen construyendo los casos de uso con `ConsultarDefinicionFlujo` y devolviendo `DefinicionFlujo`.
  - No se agregaron excepciones a ArchUnit ni se cambió la regla.
- **Punto que debe confirmar Codex:** la llamada al puerto conserva en el bytecode el tipo de retorno `DefinicionFlujo`. Según el conteo de 12 violaciones del informe, en el que las llamadas a `vigente` y `porId` no figuraban, ArchUnit no cuenta el tipo de retorno de un método llamado. Si `FronterasFaseDosTest` lo contara, la prueba lo mostrará; no se debe relajar.
- **Archivos:**
  - nuevos: `compartido/domain/flujo/ContratoDeFlujo.java` y `ContratoDeEtapa.java`;
  - modificados: `flujos/domain/DefinicionFlujo.java`, `flujos/domain/DefinicionEtapa.java`, `pedidos/application/usecase/CrearPedidoCasoDeUso.java` y `ReasignarEtapaCasoDeUso.java`.
- **Repetir:** `FronterasFaseDosTest`, `ReglasArquitecturaTest` (R1 a R4, R9 y dependencias hacia dentro), `CrearReasignarFaseDosTest`, `FlujoFaseDosTest` y `AccionNucleoFaseDosTest`.

### 2.B2-D01 — Grafo de entidades aplicado al tipo equivocado

- **Causa:** `DefinicionFlujoDatos` es un repositorio de `DefinicionFlujoEntity`. `@EntityGraph(attributePaths = "documentos")` se resolvía contra ese tipo raíz, que no tiene `documentos`, aunque la consulta devolviera `TransicionPermitidaEntity`.
- **Corrección:** se quitó el `@EntityGraph`. La consulta carga los documentos con `select distinct t from TransicionPermitidaEntity t left join fetch t.documentos where t.flujoId = :flujoId`, en una sola consulta y sin N+1. Una transición sin documentos se sigue devolviendo y no se convierte ningún error en datos vacíos.
- **Archivo:** `flujos/adapter/out/persistence/DefinicionFlujoDatos.java`.
- **Repetir:**
  - `PersistenciaFaseDosIT.adaptadorDeFlujosConservaDocumentosYPlazosPendientes`: 11 etapas, 14 transiciones, documentos y plazos PENDIENTES;
  - los once casos integrados que bloqueaba este defecto, en especial la reasignación con cableado completo y los rollback integrados.

### 2.B2-D02 — El segundo avance concurrente lanzaba IllegalStateException

- **Causa probable (deducida del código y del comportamiento observado; no se reprodujo de nuevo):** `buscarParaActualizar` usaba `find` y después `refresh(…, PESSIMISTIC_WRITE)`, dejando a Hibernate el orden entre leer y bloquear. La segunda transacción pudo armar el agregado con la fila del pedido leída antes de obtener el bloqueo (`etapa_actual = 3`) y con las etapas cargadas después, ya con el avance confirmado (A4 ACTUAL). Ese agregado mezclado incumple la invariante de una sola etapa ACTUAL y el constructor lanza `IllegalStateException` antes de comparar `etapaEsperada`.
- **Corrección:**
  - el bloqueo se obtiene primero, con una sentencia propia que no carga entidades: `select id from pedido where id = :id for no key update`, el mismo modo que Hibernate usa para PESSIMISTIC_WRITE en PostgreSQL;
  - después, `find` y `refresh` sin bloqueo releen el pedido y sus etapas, en cascada, con lo último confirmado;
  - el caso de uso compara `etapaEsperada` con el estado vigente y responde `EtapaCambio`.
  - No se captura ni se convierte ninguna excepción general: la invariante del agregado sigue lanzando `IllegalStateException` ante un estado realmente inconsistente.
- **Archivo:** `pedidos/adapter/out/persistence/PedidoJpaRepositorio.java` (`buscarParaActualizar` y retiro del import `LockModeType`).
- **Repetir:**
  - `PersistenciaFaseDosIT.componenteDosAvancesSimultaneosConfirmanUnoYElOtroEtapaCambio`;
  - el caso integrado de dos avances cuando D01 deje de bloquearlo;
  - todos los casos de componentes (A4→A2, rollback, fallo del historial), porque cambió la lectura con bloqueo;
  - la reasignación concurrente con un avance, si se agrega.

### 2.B2-D03 — El contador se podía vaciar con TRUNCATE

- **Causa:** `secuencia_pedido_proteger` es un trigger de fila (UPDATE y DELETE), y TRUNCATE no dispara triggers de fila.
- **Antes de editar V2 se comprobó que no estaba aplicada (solo lectura):**
  - V2 nunca se versionó en Git: ninguna referencia, incluidas `refs/agents/*`, contiene V2.
  - La base local de `bootRun` (puerto 5433, credenciales de `.env.local`, no mostradas) tiene en `flyway_schema_history` solo `1 << Flyway Baseline >>` y no tiene tablas de V2.
  - La base de Docker `decokasa-postgres` tiene solo la versión `1 usuarios roles paises` y ninguna tabla de V2. Estaba detenida: se arrancó solo para esa consulta, en una transacción `read only`, y se volvió a detener (estado final `exited`).
  - El contenedor `decokasa-backend` es anterior a V2 (creado el 2026-10-14; último arranque el 2026-10-15 a las 09:21 -05; V2 se creó a las 10:06 -05), así que no pudo aplicarla.
  - V2 solo se aplicó en los contenedores desechables de las pruebas de Codex.
  - Por eso se corrigió dentro de V2 y no se creó una V3, que además cambiaría la versión final que fijan las pruebas de Codex. No se usó `repair` ni se alteró ningún checksum aplicado.
- **Corrección:** función `secuencia_pedido_sin_vaciar()` (propia, porque un trigger de sentencia no tiene OLD ni NEW) y trigger `secuencia_pedido_sin_truncar` (`before truncate … for each statement`), que lanza una excepción (SQLState P0001). Se conservan `secuencia_pedido_solo_crece` y `secuencia_pedido_proteger` contra el retroceso, el borrado y el cambio de país o año.
- **Archivo:** `backend/src/main/resources/db/migration/V2__flujos_pedidos_historial.sql`.
- **Repetir:**
  - `V2RestriccionesIT.contadorTampocoSePuedeVaciarPorTruncate` y las cuatro variantes del contador (retroceso, DELETE, cambio de país y de año);
  - `MigracionesIT` completo (V1 primero y después V2; validate y reinicio sin reaplicar);
  - `generadorNoTruncaDiezMilEnPostgres`.

### Revalidación completa

Después de los selectores anteriores, repetir `.\gradlew.bat check --console=plain`, con la cobertura de al menos 80 %. Mientras Codex no revalide, el estado es: **D01 (R4), 2.B2-D01, 2.B2-D02 y 2.B2-D03 implementados, pendientes de revalidación**. Los bloqueos de contrato (P-13, P-05, P-16, P-17, P-18 y las representaciones pendientes en OpenAPI) siguen abiertos.

## Revalidación de las cuatro correcciones — Codex, 2026-10-15

### Alcance y revisión

Misma rama `fase-2-motor-flujo`, HEAD `990c35a9cdcfad562e302966d79b7fa35a602ee2`. Leído el manual DOCX por OOXML con acceso compartido de solo lectura, apartados 2.B1/2.B2 y 2.4. Aplicadas `ecc-tdd-workflow`, `ecc-springboot-tdd`, `ecc-database-reviewer`, `ecc-api-design`, `code-review` (modo local) y `ecc-java-reviewer`, incluidas sus referencias originales donde corresponde. Spring Boot 4.1.1, Java 21, Gradle, JUnit y JaCoCo confirmados en build.gradle. La instrucción del usuario limita esta sesión a revisión, ejecución y documentación: no se implementan correcciones ni checkpoints TDD.

Revisados los contratos compartidos, DefinicionFlujo/DefinicionEtapa, CrearPedidoCasoDeUso/ReasignarEtapaCasoDeUso, DefinicionFlujoDatos, PedidoJpaRepositorio, V2 y el diff de MigracionesIT. Las fuentes de las cuatro correcciones son archivos todavía sin seguimiento: `git diff` ordinario no muestra sus modificaciones. Se guardó su diff de altas respecto de HEAD en [correcciones-contra-head.diff](evidencias-fase-2/revalidacion/correcciones-contra-head.diff), el diff rastreado de MigracionesIT y el contraste SHA-256 con la evidencia previa de 2.B2 en [cambios-produccion-vs-2B2.csv](evidencias-fase-2/revalidacion/cambios-produccion-vs-2B2.csv). Esta última contiene siete archivos cambiados; se revisaron además las dos interfaces nuevas. No se dispone en Git de una revisión textual anterior de esos archivos sin seguimiento: el contraste de cada corrección usa también los reproductores y causas conservados arriba, sin inventar un diff histórico.

### Cierre individual y reproductor pendiente para Claude Code

| Hallazgo | Estado tras revalidación | Evidencia y alcance |
|---|---|---|
| 2.B1-D01, R4 | **ABIERTO; corrección incompleta**, prioridad media | FronterasFaseDosTest: 1 PASS; ReglasArquitecturaTest: 7 PASS, sin cambiar reglas. La firma del puerto conserva el tipo interno y las llamadas desde pedidos lo incluyen en bytecode. |
| 2.B2-D01, lectura de flujos | **CERRADO en el alcance probado** | PersistenciaFaseDosIT completo: 21 PASS. Carga 11 etapas/14 transiciones, documentos Solicitud y Documentos codificados, plazos PENDIENTES en 5/7/10 y requisitos pendientes en 6/9/10. Los casos integrados antes bloqueados pasan, incluidos reasignación y rollback. |
| 2.B2-D02, concurrencia | **CERRADO en el alcance probado** | Pasan escenario integrado y componente con espera de bloqueo PostgreSQL; ambos repetidos tres veces adicionales con ejecución forzada. Único avance confirmado, segundo EtapaCambio/ETAPA_CAMBIO, A4 única ACTUAL, A3 COMPLETADA, versión +1, historial +1 y sin reentrada extra. También pasa refresh con entidad ya cargada, devoluciones y rollback. |
| 2.B2-D03, contador | **CERRADO en el alcance probado** | V2RestriccionesIT: 23 PASS, incluido TRUNCATE rechazado con P0001, retroceso, DELETE y cambio de país/año. MigracionesIT: 40 PASS. Generador y persistencia conservan 10000 íntegro. |

**R4: reproductor estático verificable.** En `flujos/application/port/in/ConsultarDefinicionFlujo.java:16,18,21`, `vigente`, `version` y `porId` retornan `flujos.domain.DefinicionFlujo`. `CrearPedidoCasoDeUso.java:86` llama a vigente y `ReasignarEtapaCasoDeUso.java:49` a porId. Asignar el resultado a ContratoDeFlujo elimina usos directos posteriores, pero no cambia el contrato del método invocado ni su descriptor. Comando desde la raíz:

```powershell
javap -classpath backend/build/classes/java/main -c -p com.decokasa.seguimiento.pedidos.application.usecase.CrearPedidoCasoDeUso com.decokasa.seguimiento.pedidos.application.usecase.ReasignarEtapaCasoDeUso
```

Resultado: los dos `invokeinterface ConsultarDefinicionFlujo.vigente/porId` terminan su descriptor de retorno en `Lcom/decokasa/seguimiento/flujos/domain/DefinicionFlujo;`. [Bytecode conservado](evidencias-fase-2/revalidacion/puerto-bytecode.txt). **Pedidos sigue acoplado al tipo interno de flujos a través de la firma pública**, aunque la regla ArchUnit actual no contabilice esa dependencia. Para Claude Code: completar el contrato de frontera de modo que los consumidores no requieran ese tipo interno y volver a ejecutar las reglas y regresión. No relajar ArchUnit ni conservar la firma únicamente para evitar actualizar consumidores/pruebas. El verde de esas ocho pruebas no acredita el cierre de R4; no se fabricó un fallo JUnit.

La consulta de flujos usa `left join fetch` y `distinct`: conserva transiciones con cero documentos, incluidos los avances A3/A4 integrados; no se sustituyen excepciones por colecciones vacías. El bloqueo de pedidos usa SQL parametrizado antes de cargar el agregado y refresh en cascada; no captura IllegalStateException para disfrazar estados inválidos. Las comprobaciones ejecutadas respaldan la corrección sin afirmar estabilidad ilimitada ni conformidad HTTP.

### V2 y aislamiento

Se acepta la edición previa de V2 **en el contexto documentado de migración inédita y no distribuida**: el registro de Claude Code indica V1 únicamente en las dos bases conocidas, ausencia de tablas V2, backend anterior al archivo y uso de V2 solo en contenedores desechables. La inspección actual `git log --all -- .../V2__flujos_pedidos_historial.sql` no devuelve commits; V2 sigue sin seguimiento. Esta sesión no repitió consultas en bases compartidas ni arrancó sus servicios. La ausencia de distribución se apoya en la evidencia registrada por Claude Code, no se presenta como una nueva consulta independiente de esos entornos.

La expectativa fija `"2"` de MigracionesIT **no es una justificación para editar V2**; se descarta expresamente ese argumento del resumen de implementación. El diff de MigracionesIT conserva la verificación histórica de V1 (target 1 y siete tablas) y luego comprueba la evolución a V2, validate y reinicio sin reaplicar. Su adaptación al esquema completo no oculta el defecto del contador. La nueva función de sentencia y trigger BEFORE TRUNCATE resuelven ese defecto y conservan las protecciones de fila existentes. No se ejecutó repair ni se cambió ninguna migración durante esta revalidación.

Todas las integraciones usan `postgres:17` de Testcontainers, sin reuse configurado: ServiceConnection para el contexto Spring y contenedores propios para MigracionesIT/V2RestriccionesIT. El caso componente comprueba que la URL JDBC coincide exactamente con la del contenedor. Sin DB compartida, compose, borrado de volúmenes ni cambios de estado de servicios existentes. Los DDL y SQL destructivos de prueba quedan exclusivamente dentro de esos PostgreSQL desechables.

### Comandos ejecutados y evidencias

PowerShell sin perfil, desde `backend/`. Las salidas redirigidas se guardaron con `2>&1 | Tee-Object -FilePath ...; exit $LASTEXITCODE`. El primer intento de Gradle falló por acceso al `.zip.lck` de su caché; se repitió con permiso fuera del sandbox. El aviso de class sharing de Mockito en stderr no es un fallo: se contrastan salida de Gradle y XML JUnit.

| Comando | Resultado |
|---|---|
| `.\gradlew.bat test --tests '*FronterasFaseDosTest' --tests '*ReglasArquitecturaTest' --console=plain` | BUILD SUCCESSFUL; 8 PASS |
| `.\gradlew.bat test --tests '*PersistenciaFaseDosIT' --tests '*V2RestriccionesIT' --tests '*MigracionesIT' --console=plain` | BUILD SUCCESSFUL; 84 PASS (21 + 23 + 40) |
| `.\gradlew.bat test --tests '*PersistenciaFaseDosIT.componenteDosAvancesSimultaneosConfirmanUnoYElOtroEtapaCambio' --tests '*PersistenciaFaseDosIT.dosAvancesSimultaneosSoloConfirmanUnoYElOtroEsEtapaCambio' --rerun-tasks --console=plain` | Tres ejecuciones adicionales: 2/2 PASS en cada una; 6 PASS, cero fallos/errores/omitidas |
| `.\gradlew.bat test --tests '*Test' --tests '*PersistenciaFaseDosIT' --tests '*V2RestriccionesIT' --tests '*MigracionesIT' --console=plain` | Regresión 2.B1 + 2.B2: BUILD SUCCESSFUL; **330 PASS**, cero fallos/errores/omitidas |

Evidencias en [revalidacion](evidencias-fase-2/revalidacion/): XML de arquitectura; [integracion.log](evidencias-fase-2/revalidacion/integracion.log) y XML en integracion/; concurrencia-1/2/3.log y .xml; [regresion.log](evidencias-fase-2/revalidacion/regresion.log) y XML/CSV en regresion/. Los XML residuales de clases fuera del selector no se suman a la ejecución filtrada (por ejemplo ReglasArquitecturaTest en la copia de integración). La regresión suma su selección efectiva completa. No se suman las repeticiones para inflar el total de la suite final.

| Comando final | Resultado |
|---|---|
| `.\gradlew.bat check --rerun-tasks --console=plain` | **BUILD SUCCESSFUL**, exit 0, 1 min 2 s, siete tareas ejecutadas; **411 pruebas, 411 PASS, 0 FAIL, 0 errores, 0 omitidas**. Incluye jacocoTestCoverageVerification aprobada. |

### Cobertura real final y decisión

JaCoCo corresponde exclusivamente a la última ejecución completa de `check`, forzada con `--rerun-tasks`; no se mezcla con selecciones previas. Sin cambios de umbral, exclusiones, reglas ni pruebas para conseguir verde.

| Ámbito de líneas | Cubiertas | Sin cubrir | Cobertura |
|---|---:|---:|---:|
| Backend completo | 1839 | 154 | **92,27 %** |
| pedidos: domain + application | 353 | 25 | 93,39 % |
| flujos: domain + application | 76 | 11 | 87,36 % |
| plazos: domain + application | 46 | 0 | 100 % |
| historial: domain + application | 64 | 5 | 92,75 % |

Se supera el mínimo global del 80 %. [check.log](evidencias-fase-2/revalidacion/check.log), [resultados finales por clase](evidencias-fase-2/revalidacion/final/resultados.csv), [XML JaCoCo final](evidencias-fase-2/revalidacion/final/jacoco.xml) y [contadores de cobertura](evidencias-fase-2/revalidacion/final/cobertura.csv). Los XML JUnit finales están en final/. El verde automatizado no sustituye la inspección de la firma R4 ni acredita endpoints ausentes.

**Decisión de revisión local: corrección de R4 todavía requerida; fase 2 abierta.** Los tres defectos 2.B2-D01/D02/D03 quedan cerrados por esta revalidación; 2.B1-D01 no se cierra. Las menciones anteriores a 14 fallos y cuatro correcciones pendientes describen ejecuciones históricas, sustituidas únicamente en su estado actual por este apartado.

**Pendientes de 2.4, visibles y sin cierre:** siguen sin controladores de flujos, creación/consulta/listado/búsqueda/bandeja/detalle de pedidos, acciones, asignación, observaciones e historial; la existencia de casos de uso transaccionales no completa esa entrega. [Inventario actual de rutas](evidencias-fase-2/revalidacion/rutas.csv). No se han probado para esas rutas autenticación/autorización HTTP, validación JSON/Bean Validation, paginación, RFC 9457, 201/Location ni estados 400/403/409/422. OpenAPI aún no distingue SIN_PLAZO/PENDIENTE en DefinicionEtapa.plazo ni requisitos documentales DEFINIDOS/PENDIENTES en DefinicionAccion; representación del rol histórico/DetalleEvento pendiente. Permanecen P-13 (visibilidad), P-05/P-16/P-17/P-18 (archivos, reservas, descarga y reentrada), P-11 (etapaEsperada ante reentradas) y las políticas provisionales ya identificadas. A1/A2 operativas siguen bloqueadas sin documentos verificables. No se inventan requisitos para resolverlos.

Conservación comprobada por SHA-256 de todos los archivos de `backend/src/main` y `backend/src/test`: [antes](evidencias-fase-2/revalidacion/integridad-antes.csv), [después](evidencias-fase-2/revalidacion/integridad-despues.csv), [comparación sin diferencias](evidencias-fase-2/revalidacion/integridad-diferencias.txt). Producción, migraciones y pruebas quedaron intactas durante esta sesión. `git diff --check` sin errores; rama y HEAD conservados. Solo se actualizan este informe y sus evidencias. Sin commit, push, borrado de volúmenes ni acceso a bases compartidas. **2.B3 y 2.5 no iniciados.**

## Corrección de R4 en la firma del puerto (Claude Code, 2026-10-15) — implementada, pendiente de revalidación

Atiende el hallazgo de la revalidación: `ConsultarDefinicionFlujo.vigente`, `version` y `porId` seguían devolviendo `flujos.domain.DefinicionFlujo`, así que el descriptor de las llamadas desde `pedidos` incluía el tipo interno. Solo se corrige 2.B1-D01. Los defectos 2.B2-D01, D02 y D03, ya cerrados por Codex, no se tocaron. No se ejecutaron pruebas, builds ni verificaciones. No se modificaron pruebas, ni ArchUnit ni sus reglas. Sin endpoints ni pantallas. Sin commit ni push.

### Cambio

- **Puerto `ConsultarDefinicionFlujo`:** `vigente`, `version` y `porId` devuelven `compartido.domain.flujo.ContratoDeFlujo`. El puerto ya no importa `flujos.domain`. Los dos puertos de entrada de `flujos` (`ConsultarDefinicionFlujo` y `ResolverTransicion`) solo exponen tipos de `compartido` y de `java.util`, también en los genéricos: `ContratoDeFlujo.etapas()` es `List<? extends ContratoDeEtapa>`.
- **Implementación:** `ConsultarDefinicionFlujoCasoDeUso` y el adaptador transaccional de `FlujosConfig` declaran `ContratoDeFlujo`. Dentro del propio módulo la definición sigue siendo `DefinicionFlujo`, que implementa el contrato.
- **Contrato:** `ContratoDeFlujo` agrega `pais()` y `version()`, que `DefinicionFlujo` ya tenía y ahora marca como `@Override`.
- **Consumidores:** `CrearPedidoCasoDeUso`, `ReasignarEtapaCasoDeUso` y `PedidosConfig` ya usaban solo el contrato. Ahora la firma del puerto que llaman tampoco lleva el tipo interno. Búsqueda estática: ningún archivo de producción fuera de `flujos` referencia `flujos.domain`.
- **Comportamiento de negocio:** sin cambios. El puerto devuelve la misma instancia, la misma versión, los mismos errores `NoEncontrado` y la misma transacción de solo lectura.
- **Transiciones:** se leen con el puerto `ResolverTransicion`, como ya hacía `EjecutarAccionEtapa`. Qué debe exponer `GET /flujos/{pais}/{tipoFlujo}` (acciones, documentos pendientes y plazo pendiente) sigue bloqueado por las representaciones pendientes del contrato; no se amplió el contrato compartido con transiciones para adelantar ese endpoint.

**Archivos modificados:**
- `flujos/application/port/in/ConsultarDefinicionFlujo.java`;
- `flujos/application/usecase/ConsultarDefinicionFlujoCasoDeUso.java`;
- `flujos/config/FlujosConfig.java`;
- `compartido/domain/flujo/ContratoDeFlujo.java`;
- `flujos/domain/DefinicionFlujo.java`.

### Prueba que Codex debe adaptar (sin quitar comprobaciones)

`pedidos/PersistenciaFaseDosIT.adaptadorDeFlujosConservaDocumentosYPlazosPendientes`, líneas 122 a 131. `flujos.vigente(...)` devuelve ahora `ContratoDeFlujo`, que no tiene `transiciones()` ni `resolver(...)`.

- **Se mantienen igual:** `f.etapas()` (11 etapas), `f.etapa(1).plazo()` (`SinPlazo`) y `f.etapa(n).plazo()` (`Pendiente` en 5, 7 y 10).
- **Cantidad de transiciones (14):** obtenerla del adaptador de salida del propio módulo, por ejemplo con el bean `DefinicionFlujoRepositorio` (`buscarVigente(EC, A).orElseThrow().transiciones()`). La prueba verifica justamente ese adaptador JPA.
- **`f.resolver(n, APROBAR)` en 6, 9 y 10 y `f.resolver(1|2, ENVIAR)`:** usar el bean `ResolverTransicion` ya inyectado en la clase (`transiciones.resolver(f.id(), n, APROBAR)`), cuya `Resolucion` tiene `documentosPendientes()` y `documentosObligatorios()`. También sirve el `DefinicionFlujoRepositorio` del punto anterior. En ambos casos se conservan las aserciones: pendientes verdadero y lista vacía en 6, 9 y 10; "Solicitud" en 1; "Documentos codificados" en 2.

**Revisión estática de las demás pruebas, sin cambios necesarios:**
- `FlujoFaseDosTest`: `isSameAs` compara la misma instancia, y `version()` ahora pertenece al contrato.
- `CrearReasignarFaseDosTest`: `thenReturn(DefinicionFlujo)` acepta el subtipo del contrato.
- La clase anónima de `PersistenciaFaseDosIT` (`crearConDefinicionFicticia`): un método que devuelve `DefinicionFlujo` sobrescribe uno que devuelve `ContratoDeFlujo` (retorno covariante).
- `AccionNucleoFaseDosTest`: no usa este puerto.

Si el compilador mostrara otra incompatibilidad, adaptarla con el mismo criterio: leer las transiciones por `ResolverTransicion` o por el adaptador de `flujos`, nunca devolviendo el tipo interno desde el puerto.

### Revalidación pedida

- Tras adaptar la prueba anterior: `FronterasFaseDosTest`, `ReglasArquitecturaTest`, `FlujoFaseDosTest`, `CrearReasignarFaseDosTest`, `AccionNucleoFaseDosTest` y `PersistenciaFaseDosIT` completo.
- La inspección de bytecode de la revalidación (`invokeinterface ConsultarDefinicionFlujo.vigente/porId`): su descriptor debe terminar en `Lcom/decokasa/seguimiento/compartido/domain/flujo/ContratoDeFlujo;`.
- `check` completo.

**Estado: 2.B1-D01 (R4) implementado, pendiente de revalidación; no se declara cerrado.**

## Revalidación final exclusiva de R4 — Codex, 2026-10-15

Este apartado sustituye el estado pendiente de 2.B1-D01 de las ejecuciones anteriores; conserva su evidencia histórica. Alcance: nueva firma del puerto, adaptación de pruebas y regresión exigida. Leídas y aplicadas las instrucciones de `ecc-hexagonal-architecture`, `ecc-springboot-tdd` y `ecc-java-reviewer` (incluida su referencia original). Spring Boot 4.1.1/Java 21 detectados en build.gradle. No se implementó producción ni se atribuye un ciclo RED/GREEN previo a esta corrección ya entregada por Claude Code.

### Revisión del cambio y conservación de garantías

Revisados completos los cinco archivos cambiados por Claude Code: ConsultarDefinicionFlujo, ConsultarDefinicionFlujoCasoDeUso, FlujosConfig, ContratoDeFlujo y DefinicionFlujo. El contrato de entrada, su implementación y la envoltura transaccional retornan ContratoDeFlujo en vigente/version/porId. El dominio interno implementa el contrato; pais/version agregados al contrato corresponden a propiedades ya existentes. No se agregaron transiciones ni documentos al contrato compartido. Se conserva el repositorio interno y la traducción NoEncontrado; no se observó un nuevo defecto en este alcance.

Los cinco archivos siguen sin seguimiento en Git: el diff ordinario no muestra sus cambios. Se conserva [diff de altas respecto de HEAD](evidencias-fase-2/r4-final/correccion-contra-head.diff) y [comparación de hashes contra la revalidación anterior](evidencias-fase-2/r4-final/correccion-hashes.csv), que identifica precisamente esos cinco archivos. El antes del puerto y su descriptor constan en la evidencia anterior. No se presenta el diff de altas como un diff textual contra una revisión anterior inexistente en Git.

Adaptación autorizada de `PersistenciaFaseDosIT.adaptadorDeFlujosConservaDocumentosYPlazosPendientes`: conserva 11 etapas, SIN_PLAZO en 1, PENDIENTE en 5/7/10, documentos pendientes y vacíos en 6/9/10, Solicitud en 1 y Documentos codificados en 2. Las cinco consultas de documentos se hacen ahora mediante ResolverTransicion. El conteo de 14 transiciones se **traslada**, sin eliminarlo, a `flujos/DefinicionFlujoPersistenciaIT.adaptadorConservaLasCatorceTransiciones`, que consulta DefinicionFlujoRepositorio dentro del propio módulo y una transacción de lectura. Se ejecuta además de los seis selectores solicitados. [Diff de adaptación](evidencias-fase-2/r4-final/adaptacion.diff). No se introduce el puerto de salida de flujos en pedidos.

### Inspección de frontera, firmas genéricas y bytecode

Desde la raíz, se enumeraron recursivamente todos los `.class` de producción de pedidos (incluidas clases anidadas), se convirtieron sus rutas en nombres de clase y se ejecutó:

```powershell
javap -classpath backend/build/classes/java/main -p -c -s -v $classes
```

**31 clases inspeccionadas; cero referencias a `com/decokasa/seguimiento/flujos/domain` o su forma con puntos** en constant pool, descriptores, atributos Signature e instrucciones. La búsqueda equivalente en fuentes de pedidos tampoco encuentra referencias. Los dos invokeinterface desde CrearPedidoCasoDeUso y ReasignarEtapaCasoDeUso retornan `Lcom/decokasa/seguimiento/compartido/domain/flujo/ContratoDeFlujo;`.

Inspección adicional `javap -p -s -v` del puerto ConsultarDefinicionFlujo, ResolverTransicion y su Resolucion, ContratoDeFlujo y ContratoDeEtapa: vigente/version/porId retornan exactamente ContratoDeFlujo; etapas es `List<? extends ContratoDeEtapa>`; los genéricos y componentes del resultado de ResolverTransicion tampoco exponen el dominio interno. Se permite el acoplamiento interno de la implementación de flujos a su propio dominio, no se confunde con el contrato entre módulos.

Evidencias: [pedidos-bytecode.txt](evidencias-fase-2/r4-final/pedidos-bytecode.txt), [contratos-bytecode.txt](evidencias-fase-2/r4-final/contratos-bytecode.txt), [resumen](evidencias-fase-2/r4-final/inspeccion.txt). ArchUnit y sus reglas no se modificaron. La inspección cubre el hueco de la regla observado en la revisión anterior.

### Ejecución

PowerShell sin perfil, desde backend; logs mediante `2>&1 | Tee-Object -FilePath ...; exit $LASTEXITCODE`. Permiso de acceso a caché Gradle y Docker fuera del sandbox. PostgreSQL 17 temporal exclusivamente mediante Testcontainers/ServiceConnection, sin reuse configurado y con `spring.config.import=` y semilla deshabilitada en las pruebas de integración adaptadas. Sin acceso a bases compartidas ni arranque de servicios existentes.

```powershell
.\gradlew.bat test --tests '*FronterasFaseDosTest' --tests '*ReglasArquitecturaTest' --tests '*FlujoFaseDosTest' --tests '*CrearReasignarFaseDosTest' --tests '*AccionNucleoFaseDosTest' --tests '*PersistenciaFaseDosIT' --tests '*DefinicionFlujoPersistenciaIT' --console=plain
```

BUILD SUCCESSFUL, exit 0. Solo se cuentan XML de los selectores efectivos; no los residuales de otros filtros.

| Clase | Ejecutadas/aprobadas | Fallos/errores/omitidas |
|---|---:|---:|
| FronterasFaseDosTest | 1 | 0/0/0 |
| ReglasArquitecturaTest | 7 | 0/0/0 |
| FlujoFaseDosTest | 11 | 0/0/0 |
| CrearReasignarFaseDosTest | 4 | 0/0/0 |
| AccionNucleoFaseDosTest | 6 | 0/0/0 |
| PersistenciaFaseDosIT completo | 21 | 0/0/0 |
| DefinicionFlujoPersistenciaIT | 1 | 0/0/0 |
| **Total de la selección** | **51** | **0/0/0** |

[Log de selección](evidencias-fase-2/r4-final/seleccion.log), [resultados por clase](evidencias-fase-2/r4-final/seleccion/resultados.csv) y XML en seleccion/.

Comando final desde backend:

```powershell
.\gradlew.bat check --console=plain
```

**BUILD SUCCESSFUL**, exit 0, 1 min 1 s. `test`, `jacocoTestCoverageVerification` y `jacocoTestReport` se ejecutaron; no fue una reutilización de resultados de la selección filtrada. **412 pruebas ejecutadas: 412 PASS, 0 FAIL, 0 errores, 0 omitidas.** El incremento de 411 a 412 corresponde al caso trasladado a una nueva prueba del módulo flujos; no se cuentan dos veces los resultados de selección y suite completa. [check.log](evidencias-fase-2/r4-final/check.log), [resultados finales](evidencias-fase-2/r4-final/check/resultados.csv) y XML JUnit en check/.

Cobertura global real de líneas de esta ejecución: **1839 cubiertas, 154 sin cubrir; 92,27 %**, umbral mínimo 80 % aprobado, sin modificar exclusiones ni configuración. [JaCoCo XML](evidencias-fase-2/r4-final/check/jacoco.xml), [contadores](evidencias-fase-2/r4-final/check/cobertura.csv). El aviso JVM de class sharing de Mockito aparece en stderr como NativeCommandError de PowerShell; no representa fallo de prueba y se contrasta con XML y exit 0.

**2.B1-D01 / R4: CERRADO.** La corrección elimina el tipo interno de las firmas públicas y del bytecode consumidor; pasan las reglas intactas, los casos de uso y persistencia completa, manteniendo todas las comprobaciones anteriores. No se detectó otro defecto reproducible en el alcance revisado. Este cierre no extiende la aprobación a endpoints o pantallas ni cierra toda la fase.

**2.4 sigue pendiente:** controladores de flujos/pedidos/acciones/asignación/observaciones/historial, consultas/listados/búsqueda/bandeja/detalle y conformidad HTTP efectiva; representación en OpenAPI de SIN_PLAZO/PENDIENTE, documentos DEFINIDOS/PENDIENTES y rol histórico/DetalleEvento. Continúan P-13, P-05/P-16/P-17/P-18, el riesgo P-11 ante reentradas y las políticas provisionales ya registradas. No se habilitan A1/A2 reales sin documentos verificables. Los cierres previos de los tres hallazgos 2.B2 se conservan; no se reabre su implementación.

Rama `fase-2-motor-flujo` y HEAD `990c35a9cdcfad562e302966d79b7fa35a602ee2` conservados. [Hashes antes](evidencias-fase-2/r4-final/antes.csv), [después](evidencias-fase-2/r4-final/despues.csv) y [diferencias de la sesión](evidencias-fase-2/r4-final/diferencias-sesion.csv): en backend/src solo cambió la prueba autorizada PersistenciaFaseDosIT y se agregó DefinicionFlujoPersistenciaIT. Producción, migraciones y reglas de arquitectura intactas. `git diff --check` sin errores. Sin bases compartidas, borrado de volúmenes, commit ni push. **Sin avance a 2.B3, endpoints, pantallas o 2.5.**

## Verificación del backend nuevo de 2.4 — Codex, 2026-10-16

**Resultado vigente: NO APROBADO.** La ejecución definitiva de `check` termina con **470 pruebas: 465 PASS, 5 FAIL, 0 errores y 0 omitidas**. Se conservan cuatro defectos nuevos reproducibles y un reproductor del pendiente funcional ya declarado de `EtapaEnLinea.resumen`. No se corrigió producción ni se deshabilitaron pruebas. Los resultados anteriores (412 PASS y 92,27 %) pertenecen a R4 antes de estos cambios y no acreditan el backend nuevo.

### Alcance, fuentes y skills

Solo backend de 2.4: V3, endpoints, autorización vigente, consultas, acciones, historial, asignación, observaciones y archivos locales reales. El recorrido mediante peticiones backend de prueba **no es 2.B3**: no se abrió un navegador ni se ejecutó Playwright. La parte A de 2.5 existe; sus pantallas quedan sin verificar en este turno. La fase permanece abierta y Richard conserva la validación del cierre.

Leídos el manual vigente DOCX (OOXML; el Markdown conserva el manual anterior), el plan de fase 2 —incluido el cierre de 2.4, P-05/P-06/P-13/P-17/P-18/P-19—, OpenAPI 0.7.0-borrador, el informe previo, las fuentes correspondientes y los requisitos de historial/resúmenes/archivos. La nulidad de `resumen` ya estaba reconocida en el plan; no se presenta como descubrimiento nuevo. No se han convertido propuestas en respuestas del cliente.

Aplicadas las skills de **2.B1**: `ecc-tdd-workflow`, `ecc-springboot-tdd`; y **2.B2**: `ecc-database-reviewer`, `ecc-api-design`, `code-review`, con sus referencias originales cuando corresponde. Complementan `ecc-java-reviewer`, `ecc-hexagonal-architecture`, `ecc-springboot-verification` y `ecc-test-coverage`. Framework detectado: Spring Boot 4.1.1, Java 21, Gradle/JUnit 5/JaCoCo. Esto es una verificación de implementación recibida: los reproductores RED se dejan para Claude; no se atribuye un ciclo GREEN de corrección ni se crean los commits sugeridos por la skill, por instrucción expresa de Richard.

### Aislamiento y adaptación de pruebas

- Exclusivamente PostgreSQL 17 de **Testcontainers**, sin reuse; `MigracionesIT`, `V2RestriccionesIT` y `V3MigracionIT` tienen contenedores propios. La integración Spring usa `TestcontainersConfiguration`/`ServiceConnection`. La suite nueva deshabilita `spring.config.import` y la semilla y usa disco temporal propio. No se ejecutó Compose ni se conectó a bases compartidas.
- Primera tentativa: Gradle no podía escribir en su caché desde el sandbox; se repitió con permiso. La primera tentativa con Docker devolvió `Could not find a valid Docker environment`: el daemon estaba apagado. Se inició con `docker desktop start`; esto no es un fallo de negocio ni forma parte del conteo definitivo. No hubo borrado manual de volúmenes.
- `MigracionesIT` conserva sus **40 casos**, incluida la migración V1 y sus siete tablas. Ahora aplica V2 y V3 por separado, exige la ejecución única de ambas, el historial exacto `1,2,3`, validación/idempotencia y el conjunto exacto de tablas V1/V2 más las tres de V3, sin bodegas.
- `V2RestriccionesIT` conserva siembra, 11 etapas, 14 transiciones, requisitos, unicidad, FK, CHECK, inmutabilidad de identidad/historial y contador. Sus datos crean una `entrada_etapa` real y escriben `entrada_en`/`entrada_id` juntos. La prueba de dos ACTUAL sigue exigiendo **23505**, no un fallo previo por datos inválidos.
- V3 añade pruebas de CHECK fecha/id; FK compuesta diferida de etapa (se fuerza `SET CONSTRAINTS ALL IMMEDIATE` para observar el rechazo); escritura válida etapa antes de entrada; FK compuesta de adjuntos; tamaño, huella, almacenamiento, estado de copia e intentos; relación de revisión; vínculos de evento e inmutabilidad.
- Para alcanzar el trigger original de TRUNCATE de historial con la nueva FK de `evento_adjunto`, se usa `TRUNCATE evento_pedido CASCADE`, conservando la exigencia **P0001**. Antes de probar el trigger de TRUNCATE de entrada se resuelven las FK diferidas: así la prueba no termina por **55006** antes de llegar al trigger. Todo ocurre en la transacción revertida del contenedor propio, sin datos compartidos.

### Garantías ejercitadas y resultados

| Área / clase | Ejecutadas | PASS / FAIL | Evidencia funcional |
|---|---:|---:|---|
| `MigracionesIT` | 40 | 40 / 0 | V1→V2→V3 vacía, esquema exacto, validación e idempotencia; restricciones originales |
| `V2RestriccionesIT` | 42 | 42 / 0 | Garantías V2 conservadas y restricciones/FK/triggers V3 con entradas válidas |
| `V3MigracionIT` | 1 | 0 / 1 | V2 poblada→V3 falla; se comprueba rollback de tablas/columna nuevas y conservación de las tres etapas |
| `Backend24IT` | 35 | 31 / 4 | Endpoints con Spring/SQL/disco reales; JWT de prueba con rol admin deliberadamente obsoleto, más multipart por HTTP con JWT firmado |
| `AlmacenLocalRealTest` | 3 | 3 / 0 | 100 MiB reales exactos en disco y lectura completa; 100 MiB + 1 byte rechazado sin temporal; clave fuera del directorio rechazada |
| Resto de la suite | 349 | 349 / 0 | Regresión de acceso, dominio, concurrencia, persistencia, plazos y arquitectura |
| **Total definitivo** | **470** | **465 / 5** | **0 errores, 0 omitidas** |

`Backend24IT` comprueba creación 201/Location, detalle, búsqueda, listado filtrado por número, configuración vigente/versionada, historial, listado de adjuntos, errores 400/401/403/404/409/413/422 y parámetros de paginación inválidos. Contrasta estados/campos/errores relevantes con OpenAPI; no es una validación automática exhaustiva de todos los esquemas de la API ni de rutas fuera de 2.4.

Permisos: los seis roles no admin pueden consultar según P-13, pero un JWT que dice admin no les permite crear ni enviar A1. Cuenta desactivada o sin roles pierde lectura y escritura. En A2 la reasignación exige admin y candidato del rol correcto; excluye al resto del rol. Si el asignado pierde su rol, ni él ni otro del mismo rol avanzan; el admin conserva su facultad. La descarga permite admin y revisor declarado A3→A2; niega consulta y Compras China, sin inferir permiso para A4.

Acciones: documentos reales A1/A2, rechazos con observación obligatoria, devolución por error, devolución por producto nuevo con las tres urgencias 4/12/24 h; el vencimiento se mantiene durante un rechazo posterior y se elimina al volver a A4. A4→A5 permitido, operación de A5 fuera de fase rechazada. Dos avances simultáneos dan **200/409**, un solo evento de avance y una sola etapa ACTUAL. Continúa aprobada la regresión `PersistenciaFaseDosIT` completa (**21/21**), incluidos fallo SQL de historial/rollback de acción, rollback de creación/contador y creaciones concurrentes. Arquitectura intacta: `FronterasFaseDosTest` **1/1**, `ReglasArquitecturaTest` **7/7**.

Observaciones: 201 con actor, rol de intervención y fecha del servidor; no avanzan la etapa. Se conserva el rol histórico después de retirarlo al usuario. Consulta no escribe; entrada obsoleta, texto vacío/blanco/mayor de 4000 y adjunto ajeno se rechazan sin eventos ni registros parciales.

Archivos: se escriben bytes reales, se mide el tamaño y se detecta PDF aunque el cliente declare HTML; el nombre con rutas no se usa como clave. La descarga entrega los bytes con `octet-stream` y `nosniff`, y rechaza ausencia, truncado y corrupción de igual longitud con **409 ARCHIVO_NO_DISPONIBLE**. Presentar ids inexistentes, repetidos, nulos, malformados, de otro pedido, otra etapa o entrada anterior no se ignora. Al reentrar se genera otra entrada y se exigen documentos nuevos. `entradaEsperadaId` ausente responde 400 y obsoleto 409. Una reentrada provocada **durante la lectura de una carga** se detecta al registrar: se descarta el temporal, sin nuevo adjunto ni evento de archivo.

Atomicidad: se comparan las filas completas de pedido, etapas, entradas, eventos, adjuntos y relaciones `evento_adjunto` antes/después de los rechazos ejercitados. Un flujo cortado después de recibir bytes no deja temporal, adjunto ni evento. Un trigger de fallo SQL, creado solo en el contenedor de prueba, provoca un error al insertar `ARCHIVO_SUBIDO` después de mover el archivo y registrar el adjunto: se revierten las filas y se borra el definitivo. Se retira el trigger de prueba al terminar. La excepción funcional importante es el archivo corrupto de igual tamaño: producción lo acepta y avanza, como documenta D05.

Límites: disco real con **104 857 600** bytes aceptados y **104 857 601** rechazados. La suite Spring configura **1 KiB** para ejercitar los límites de forma económica, tanto a través del adaptador como mediante HTTP real en puerto aleatorio con JWT firmado: **1024 bytes → 201; 1025 → 413**. No se confunde MockMvc con el parser multipart del servlet. El 413 real no deja filas ni archivos, pero incumple el código de error del contrato (D07). No se probó nginx ni una subida HTTP de 100 MiB en este alcance backend.

### Defectos reproducibles para Claude — sin correcciones de producción

**2.B2-D04 — ALTO: V3 no migra una V2 con etapas visitadas.**

- Reproductor: `V3MigracionIT.backfillConservaFechasYDaEntradasSoloAEtapasVisitadas`.
- Preparación: V2 en un PostgreSQL vacío aislado; un pedido con A1 COMPLETADA, A2 ACTUAL y A3 PENDIENTE; fechas de entrada válidas en A1/A2. Aplicar V3 mediante Flyway.
- Esperado: entradas de A1/A2 con misma fecha/pedido/etapa, A3 sin entrada, migración confirmada e idempotente.
- Actual: **SQLSTATE 55006**, `cannot ALTER TABLE "etapa_pedido" because it has pending trigger events`, en `V3__archivos_y_entradas.sql:52`. El backfill actualiza la FK diferida y luego ejecuta ALTER en la misma transacción. La migración revierte: versión 2, sin las tres tablas V3 ni columna `entrada_id`, y conserva las tres etapas. Las comprobaciones de éxito permanecen después del punto de fallo para la revalidación.
- Orientación: revisar el orden de creación/validación de la FK diferida y el backfill, sin quitar el CHECK ni la FK compuesta. La nota de que V2 no llegó a bases persistentes no elimina este fallo de la actualización explícitamente implementada.

**2.B2-D05 — ALTO: una acción avanza con un adjunto corrupto de igual tamaño.**

- Reproductor: variante `corrupto` de `Backend24IT.accionRechazaArchivoAusenteTruncadoOCorruptoSinAvance`.
- Crear pedido, subir Solicitud de 13 bytes reales; reemplazar en el disco de prueba los 13 bytes por ceros; POST ENVIAR A1 con su `adjuntoIds`.
- Esperado: rechazo y snapshot SQL intacto. Actual: **200**, pedido en A2 y nuevo evento de avance; fallan **ambas** aserciones, estado HTTP y ausencia de cambios. La descarga del mismo archivo sí detecta la corrupción.
- Localización: `archivos/application/usecase/ValidarAdjuntosCasoDeUso.java:56`: solo comprueba existencia/tamaño. Revisar integridad antes de aceptar el archivo para requisitos/observaciones; no implica validar semánticamente el documento, que sigue fuera de alcance según ADR-0007.

**2.B2-D06 — ALTO: bandeja responde 500 al cargar pedidos con sus etapas.**

- Reproductor: `Backend24IT.bandejaRespetaContratoSinErrorInterno`; crea un pedido, lo lleva a A2 con archivo real y consulta GET `/api/v1/pedidos/bandeja` como admin.
- Esperado: 200 PaginaPedidos. Actual: **500**, `IllegalStateException: Duplicate key … (attempted merging values PedidoEntity …)`.
- Localización: `pedidos/adapter/out/persistence/PedidoJpaRepositorio.java:227–235`, `cargarEnOrden`: `left join fetch p.etapas`, `getResultStream()` y `Collectors.toMap` reciben duplicados de la entidad. Lo usa también el listado; esa reutilización indica riesgo, no se afirma que todas sus combinaciones hayan sido reproducidas. Revisar materialización/deduplicación y paginación sin reducir etapas para conseguir verde.

**2.B2-D07 — MEDIO: el 413 del servlet incumple OpenAPI.**

- Reproductor: `Backend24IT.multipartRealAplicaLimiteDelServletConJwtFirmado` (HttpClient → Tomcat real; no MockMvc).
- Con límite configurable de 1024 bytes, enviar multipart válido de 1025. El estado **413** es correcto y los snapshots de DB/disco quedan intactos. Respuesta real: `{"detail":"Maximum upload size exceeded","instance":"…/archivos","status":413,"title":"Content Too Large"}`; falta **`codigo: ARCHIVO_DEMASIADO_GRANDE`**.
- Revisar `ManejadorErrores` y el manejo de `MaxUploadSizeExceededException`: la conversión actual en `handleExceptionInternal` no completa este cuerpo en esta ruta de Spring. Conservar el rechazo real del parser; la prueba MockMvc del tamaño pasa y por sí sola no descubre el defecto.

**Pendiente funcional previo — resumen de etapas, confirmado en rojo.**

- Reproductor: `Backend24IT.resumenDeEtapaIncluyeLoOcurrido`: llegar a A3, rechazar con observación y consultar detalle; `lineaDeTiempo[2].resumen` sigue **null**.
- El plan ya declara este pendiente. Se conserva por requerimientos 4.2 #17 y la descripción de `EtapaEnLinea.resumen` en OpenAPI. `PedidoRespuestas.etapa` lo fija a null. No se elimina la prueba por ser un pendiente conocido ni se atribuye a R4.

Selección para reproducir estos casos después de que Claude corrija, desde `backend/` (comando recomendado; la evidencia de este turno corresponde a la suite completa indicada a continuación):

```powershell
.\gradlew.bat test --tests '*V3MigracionIT' --tests '*Backend24IT' --console=plain
```

### Comandos definitivos y cobertura real

Ejecutados desde `backend/`, PowerShell sin perfil, guardando stdout/stderr y el código de salida:

```powershell
.\gradlew.bat check --console=plain
# exit 1; BUILD FAILED en 1 min 20 s; 470 pruebas, 5 fallidas.
.\gradlew.bat jacocoTestReport jacocoTestCoverageVerification -x test --console=plain
# exit 0; BUILD SUCCESSFUL; usa jacoco.exec de las 470 pruebas anteriores, sin volver a filtrar la suite.
```

El primer `check` completo intermedio tuvo 465 pruebas y el siguiente 467; la ejecución definitiva añade las tres pruebas de disco de 100 MiB y suma **470**. No se suman ejecuciones entre sí. Las correcciones entre intentos afectaron exclusivamente a fixtures/aserciones de pruebas: CASCADE y comprobación previa de FK en TRUNCATE, sustitución de una suposición incorrecta sobre nombres largos (producción los recorta) por fallo SQL real, separación de la bandeja como reproductor independiente y aserciones agrupadas para verificar rollback aunque el formato del error falle. No se cambió una expectativa funcional para ocultar un defecto.

Cobertura de **esta suite completa**, no de los XML históricos ni de ejecuciones filtradas:

| Ámbito | Líneas cubiertas | Sin cubrir | Cobertura |
|---|---:|---:|---:|
| **Backend global** | **2795** | **219** | **92,73 %** |
| archivos | 326 | 32 | 91,06 % |
| pedidos | 920 | 66 | 93,31 % |
| historial | 222 | 9 | 96,10 % |
| flujos | 215 | 14 | 93,89 % |

Ramas globales: **772 cubiertas / 270 sin cubrir = 74,09 %**. La regla existente exige **80 % de líneas**, que se aprueba; no se afirma 80 % de ramas ni cobertura uniforme por archivo. Se conservan las [19 fuentes por debajo de 80 % y sus líneas pendientes](evidencias-fase-2/backend-2.4-2026-10-16/final/brechas-cobertura.json). No se tocaron exclusiones, umbral ni configuración JaCoCo. El denominador cambió respecto del informe de R4 (1993→3014 líneas), por lo que el porcentaje anterior no se presenta como una medición del mismo código.

No hay tareas SpotBugs/PMD/Checkstyle/OWASP en `build.gradle`; no se instalaron herramientas ni se atribuye a `check` un análisis que no contiene. El aviso de class sharing de la JVM/Mockito se conserva en el log; los cinco fallos se identifican por XML JUnit, no por el formato NativeCommandError de PowerShell.

Evidencias de esta ejecución, separadas de las anteriores:

- [check definitivo](evidencias-fase-2/backend-2.4-2026-10-16/check-definitivo.log), [verificación de cobertura](evidencias-fase-2/backend-2.4-2026-10-16/cobertura.log).
- [Resultados por clase](evidencias-fase-2/backend-2.4-2026-10-16/final/resultados.csv), XML JUnit completos en `final/`, [fallos con mensajes](evidencias-fase-2/backend-2.4-2026-10-16/final/fallos.json).
- [JaCoCo XML](evidencias-fase-2/backend-2.4-2026-10-16/final/jacoco.xml) y [contadores globales/paquetes](evidencias-fase-2/backend-2.4-2026-10-16/final/cobertura.csv).
- [Hashes de producción antes](evidencias-fase-2/backend-2.4-2026-10-16/produccion-antes.csv) y [después](evidencias-fase-2/backend-2.4-2026-10-16/produccion-despues.csv): comparación por ruta/SHA-256 **sin diferencias**, incluidas migraciones y archivos sin seguimiento. `git diff --check` sin errores; los avisos CRLF son informativos.

### Pendientes conservados y siguiente responsabilidad

Los cierres anteriores de R4/2.B1-D01 y 2.B2-D01/D02/D03 se conservan; la regresión de arquitectura/persistencia anterior pasa. Los nuevos defectos D04–D07 se devuelven a Claude y el resumen sigue pendiente. A1/A2 ahora pueden avanzar con archivos reales verificados por estas pruebas, pero **no se declara listo el backend** por el fallo de integridad D05, la bandeja y la migración poblada.

Siguen vigentes las condiciones provisionales del plan: P-03 (formato de número), P-07 (reasignación entre roles pendiente), P-08/P-09/P-10 (tipo de horas, jornada/feriados y conservación del vencimiento), P-11 (acciones con solo `etapaEsperada` no detectan por sí mismas un ciclo de reentrada), P-13 (visibilidad), P-14 (flujo B), P-16 (coordinación de migraciones), P-17 (descargas y documentos A2 para A4), P-18 (entradas), P-19 (observaciones), formatos/límite y contradicción 7. P-05 opción A es decisión técnica del equipo, no aprobación de formatos del cliente. La limpieza de huérfanos ante caída del proceso, Drive y reintentos permanecen en fase 3. También siguen los plazos/documentos pendientes y los hallazgos heredados de fase 1 que no se cerraron explícitamente.

Solo se editaron pruebas y este informe/evidencias. Rama **`fase-2-motor-flujo`**, HEAD **`990c35a9cdcfad562e302966d79b7fa35a602ee2`** conservados. **Sin cambios de producción, bases compartidas, borrado de volúmenes, commit, push, 2.B3 ni cierre de fase.**

## Correcciones de D04–D07 — 2026-10-16, pendientes de revalidación

Este apartado registra una intervención posterior a la verificación anterior, autorizada para corregir producción. **No se ejecutaron pruebas, builds, migraciones, consultas a bases ni comandos de verificación. No se modificaron las pruebas de Codex ni las evidencias anteriores.** Los 470 resultados y la cobertura de 92,73 % son históricos respecto de estos cambios; no acreditan su resultado. Sin commit, push, ampliación a 2.5/2.B3 ni cierre de fase.

Skills utilizadas: `ecc-database-migrations` para D04, `ecc-jpa-patterns` para D06, `ecc-springboot-patterns` y `ecc-api-design` para D05/D07, y criterios de `ecc-java-reviewer` sobre los cambios Java. La prohibición expresa de ejecutar verificaciones prevalece sobre los comandos de build/check sugeridos por las skills. No se presenta una revisión automatizada ni una compilación como realizadas.

### D04 — backfill y eventos diferidos

**Determinación previa de migración:** el plan, en «Cierre del paso 2.4», y la cabecera de V3 documentan que V2/V3 no se aplicaron en bases persistentes. Las evidencias del informe corresponden exclusivamente a Testcontainers desechables: V3 se aplicó allí sobre vacío y falló con rollback al partir de V2 poblada. Se usa ese estado documentado, sin consultar ni modificar bases. Por ello se corrige **V3 aún no desplegada**, conservando V1/V2. No corresponde añadir V4: Flyway no podría alcanzarla desde la V2 poblada porque V3 fallaría antes. No se ejecuta `repair`, no se reemplazan checksums y no se propone reescribir ningún historial persistente. Si existe una aplicación persistente de V3 no recogida en estas fuentes, este archivo modificado no debe desplegarse sobre ella; debe reconciliarse ese historial antes de aplicar cambios.

Archivo: `backend/src/main/resources/db/migration/V3__archivos_y_entradas.sql`.

Causa: el UPDATE del backfill genera eventos de la FK compuesta diferida; PostgreSQL impide el ALTER siguiente mientras están pendientes. Tras el UPDATE se añade `SET CONSTRAINTS etapa_pedido_entrada IMMEDIATE`, que exige y resuelve esa integridad antes del ALTER, y se restaura `DEFERRED`. Se mantiene una única transacción: no se añade COMMIT ni ejecución fuera de transacción. Se conservan datos, fechas, identificadores creados por el backfill, CHECK, FK compuesta y su declaración `INITIALLY DEFERRED` para la aplicación.

**Pendiente:** repetir `V3MigracionIT` completo: V2 poblada→V3, fechas/pedido/etapa conservados, etapas no visitadas sin entrada, idempotencia. Repetir `MigracionesIT` y `V2RestriccionesIT`, incluida la escritura etapa antes de entrada y los rechazos CHECK/FK.

### D05 — integridad del archivo antes de aceptar una acción

Archivo: `backend/src/main/java/com/decokasa/seguimiento/archivos/application/usecase/ValidarAdjuntosCasoDeUso.java`.

Causa: se validaban referencias y tamaño, pero no los bytes frente al SHA-256 registrado. `tiposValidos` ahora exige además una lectura completa por bloques de 64 KiB, el tamaño leído exacto y el SHA-256 coincidente. Una lectura fallida, un archivo truncado, ampliado o corrupto no acredita el documento y produce el mismo **400 VALIDACION en adjuntoIds** que los archivos no disponibles. No se carga el archivo entero en memoria ni se valida su contenido semántico/formato: se conserva ADR-0007.

La validación sigue precediendo a la mutación del agregado, la persistencia del pedido y la publicación del evento dentro del caso de uso transaccional existente. La misma ruta protege los adjuntos de observaciones; no se introducen escrituras ni eventos en el verificador. El rechazo impide el avance y debe conservar íntegros los snapshots SQL. La atomicidad efectiva queda pendiente de la revalidación; no se afirma una ejecución exitosa. Se conservan permisos y comprobaciones de pedido/etapa/entrada antes de leer el archivo.

**Pendiente:** repetir las tres variantes de `accionRechazaArchivoAusenteTruncadoOCorruptoSinAvance`, verificando tanto HTTP como snapshots; descargas equivalentes; ids ajenos/duplicados/nulos, reentradas, observaciones con adjuntos y un fallo de lectura. Repetir regresión de archivos válidos, cargas incompletas, fallo SQL al registrar y límites reales de tamaño.

### D06 — materialización de la colección JPA

Archivo: `backend/src/main/java/com/decokasa/seguimiento/pedidos/adapter/out/persistence/PedidoJpaRepositorio.java`, método `cargarEnOrden`.

Causa: procesar mediante `getResultStream` un fetch join de colección entrega raíces repetidas antes de completar su materialización y falla al construir el mapa por id. Se sustituye por `getResultList().stream()` para materializar el resultado antes del mapa. Se conserva `select distinct`, el fetch completo de etapas, la consulta previa paginada de ids, el total y la reconstrucción en el orden original. No se pagina sobre la colección, no se quitan etapas ni se añade una función de mezcla que silencie duplicados inesperados.

**Pendiente:** repetir `bandejaRespetaContratoSinErrorInterno`; listado y bandeja con varios pedidos, todas sus etapas, asignaciones, diferentes roles, filtros, páginas y orden. El consumidor compartido también necesita regresión de detalle/búsqueda y de persistencia.

### D07 — cuerpo contractual en el 413 multipart

Archivo: `backend/src/main/java/com/decokasa/seguimiento/compartido/adapter/in/web/ManejadorErrores.java`.

Causa: Spring puede delegar el exceso multipart con cuerpo null; el código anterior solo enriquecía cuerpos que ya eran `ProblemDetail`, antes de que la superclase construyera el cuerpo. Se sobrescribe `handleMaxUploadSizeExceededException` para crear explícitamente el problema **413**, con mensaje en español, título común y **`codigo: ARCHIVO_DEMASIADO_GRANDE`**, y delegarlo por la ruta existente conservando cabeceras/contexto. No cambia el límite ni se sustituye el rechazo del parser por una validación posterior del controlador. OpenAPI ya exige ese código y no se modifica.

**Pendiente:** repetir `multipartRealAplicaLimiteDelServletConJwtFirmado` contra el servlet real (1024→201, 1025→413 con código), incluidos snapshots DB/disco. Repetir los errores MVC existentes y la validación de tamaño del adaptador.

### Entrega para revalidación

**D04, D05, D06 y D07: cambios implementados; PENDIENTES DE REVALIDACIÓN, no cerrados.** Deben repetirse los reproductores anteriores y después la suite `check` y la medición JaCoCo por Codex, sin modificar ni debilitar pruebas. Ninguno de esos comandos se ejecutó en este turno.

**`EtapaEnLinea.resumen = null` permanece como pendiente funcional separado**, con su reproductor intacto y sin cambios en `PedidoRespuestas`. Continúan todas las decisiones y pendientes funcionales del apartado anterior.

## Revalidación de D04–D07 — Codex, 2026-10-16

Esta es una ejecución posterior a las correcciones. Conserva íntegros los resultados y evidencias anteriores; los conteos de ejecuciones distintas no se suman. Alcance: los reproductores originales, después `check` completo y cobertura de esa suite. Se aplican `ecc-springboot-verification` y `ecc-test-coverage`. No se modifican producción, migraciones ni pruebas para conseguir verde.

### Primero: repetición de los cinco fallos originales

Ejecutado desde `backend/`, PowerShell sin perfil, con acceso autorizado a caché Gradle y Docker:

```powershell
.\gradlew.bat test --tests '*V3MigracionIT' --tests '*Backend24IT.accionRechazaArchivoAusenteTruncadoOCorruptoSinAvance*' --tests '*Backend24IT.bandejaRespetaContratoSinErrorInterno' --tests '*Backend24IT.multipartRealAplicaLimiteDelServletConJwtFirmado' --tests '*Backend24IT.resumenDeEtapaIncluyeLoOcurrido' --console=plain
```

**Exit 1, BUILD FAILED en 28 s**, exclusivamente por el resumen nulo. Los cinco fallos originales se reproducen como cuatro PASS y un FAIL; además pasan las variantes ausente/truncado de la prueba parametrizada. Los XML registran **7 ejecuciones objetivo: 6 PASS / 1 FAIL**, más **7 PASS de `ReglasArquitecturaTest`** que el runner ejecutó también: **14 ejecuciones reales, 13 PASS / 1 FAIL, 0 errores/omitidas**. No se atribuyen esos siete casos adicionales a los reproductores ni se reutilizan XML históricos para el conteo.

| Reproductor original | Resultado | Garantía observada |
|---|---|---|
| `V3MigracionIT.backfillConservaFechasYDaEntradasSoloAEtapasVisitadas` | **PASS** | V2 poblada→V3 confirmada; conserva pedido, etapa y fecha de cada entrada; etapa no visitada sin entrada; validación e idempotencia de Flyway |
| `Backend24IT.accionRechazaArchivoAusenteTruncadoOCorruptoSinAvance`, variante corrupto | **PASS** | Archivo alterado conservando sus 13 bytes rechazado con 400; snapshot completo de pedido/etapas/entradas/eventos/adjuntos/vínculos intacto |
| `Backend24IT.bandejaRespetaContratoSinErrorInterno` | **PASS** | Bandeja real responde 200 después de llevar el pedido a A2; conserva el snapshot SQL |
| `Backend24IT.multipartRealAplicaLimiteDelServletConJwtFirmado` | **PASS** | HTTP contra Tomcat con JWT firmado: 1024 bytes→201; 1025→413 con `ARCHIVO_DEMASIADO_GRANDE`; sin cambios SQL ni archivos parciales al rechazar |
| `Backend24IT.resumenDeEtapaIncluyeLoOcurrido` | **FAIL — pendiente separado** | `lineaDeTiempo[2].resumen` sigue null después del rechazo con observación; la expectativa no se cambia ni se omite |

Las variantes ausente/truncado de la acción pasan también con 400 y ausencia de cambios parciales. Evidencias nuevas: [log de reproductores](evidencias-fase-2/revalidacion-2.4-2026-10-16/reproductores.log), [siete casos objetivo](evidencias-fase-2/revalidacion-2.4-2026-10-16/reproductores/casos.csv) y XML de las tres clases efectivamente ejecutadas en `reproductores/`.

El aislamiento se conserva sin modificar fixtures: PostgreSQL 17 exclusivamente por Testcontainers (`V3MigracionIT` con contenedor propio y `Backend24IT` con `ServiceConnection`), sin reuse, importación de configuración local ni semilla en el contexto nuevo. Archivos en directorios temporales propios, con el límite de prueba de 1 KiB para HTTP. No se consultó ninguna base compartida, no se ejecutó Compose y no se borraron volúmenes manualmente.

### Después: suite completa y cobertura de esta ejecución

Ejecutado desde `backend/` después de conservar los XML de la selección:

```powershell
.\gradlew.bat check --console=plain
```

**Exit 1, BUILD FAILED en 1 min 20 s. 470 pruebas: 469 PASS / 1 FAIL, 0 errores / 0 omitidas.** La tarea `test` se ejecutó de nuevo, sin filtro de métodos, aunque compilación y recursos estaban UP-TO-DATE. No se suman sus resultados a los 14 de la selección. El único fallo es `Backend24IT.resumenDeEtapaIncluyeLoOcurrido`, con `Expecting not blank but was: null`; no se oculta ni se cambia su expectativa.

| Clase / alcance | PASS | FAIL |
|---|---:|---:|
| `MigracionesIT` | 40 | 0 |
| `V2RestriccionesIT` | 42 | 0 |
| `V3MigracionIT` | 1 | 0 |
| `Backend24IT` | 34 | 1 — resumen nulo |
| `AlmacenLocalRealTest` | 3 | 0 |
| `PersistenciaFaseDosIT` | 21 | 0 |
| `FronterasFaseDosTest` + `ReglasArquitecturaTest` | 8 | 0 |
| Resto de la suite | 320 | 0 |
| **Total** | **469** | **1** |

La regresión conserva CHECK/FK compuesta diferida y datos válidos V3, acciones simultáneas, permisos vigentes, historial, reasignación y observaciones, reentradas, ids inválidos, archivos ausentes/truncados/corruptos, cargas interrumpidas y rollback ante fallo SQL. Pasan también los tres casos de almacenamiento local real: 100 MiB exactos, 100 MiB + 1 byte y rechazo de clave fuera del directorio. El cierre D06 acredita el reproductor de bandeja y la regresión existente; no se presenta como una nueva matriz exhaustiva de todas las combinaciones de filtros/páginas.

Como `check` se detuvo en `test`, se generó y verificó JaCoCo **por separado sobre el `jacoco.exec` de esa suite completa**, sin ejecutar de nuevo ni filtrar las pruebas:

```powershell
.\gradlew.bat jacocoTestReport jacocoTestCoverageVerification -x test --console=plain
```

**Exit 0, BUILD SUCCESSFUL en 1 s.**

| Contador global real | Cubiertas | Sin cubrir | Porcentaje |
|---|---:|---:|---:|
| **Líneas** | **2812** | **220** | **92,74 %** |
| Ramas | 780 | 274 | 74,00 % |

El umbral existente de **80 % de líneas pasa**. No se cambiaron exclusiones, configuración ni umbral. No se afirma 80 % de ramas: se conservan los contadores por paquete y las 19 fuentes por debajo del 80 % con sus líneas pendientes. El 92,73 % anterior correspondía al código previo a las correcciones (3014 líneas); esta ejecución mide 3032 líneas, por lo que no se atribuye la pequeña diferencia porcentual a la misma base de código. La cobertura no convierte en verde el fallo funcional del resumen.

### Cierres comprobados y evidencia

- **2.B2-D04: CERRADO.** V2 poblada→V3, conservación de fechas/referencias, validación e idempotencia aprobadas; migraciones y restricciones completas en verde. Solo se ejercitaron bases aisladas; no se aplica ni se repara ningún historial persistente.
- **2.B2-D05: CERRADO.** El archivo corrupto de igual longitud recibe 400 y los snapshots completos permanecen intactos; ausencia/truncado y regresión de archivos/acciones también aprobados.
- **2.B2-D06: CERRADO.** Reproductor de bandeja 200 sin error de duplicados ni mutaciones; regresión existente aprobada.
- **2.B2-D07: CERRADO.** El parser multipart real devuelve 413 con código contractual y conserva DB/disco; el caso de límite exacto sigue aceptado.
- **Resumen nulo: ABIERTO, pendiente funcional separado.** Su caso sigue ejecutándose y fallando; por eso **`check` continúa ROJO y la fase no se cierra**. No es una regresión nueva de D04–D07 ni una reapertura de R4. Los demás pendientes funcionales y decisiones provisionales del informe permanecen vigentes.

Evidencias nuevas, sin sobrescribir las anteriores:

- [check completo](evidencias-fase-2/revalidacion-2.4-2026-10-16/check.log), [cobertura y umbral](evidencias-fase-2/revalidacion-2.4-2026-10-16/cobertura.log).
- [Resultados finales por clase](evidencias-fase-2/revalidacion-2.4-2026-10-16/final/resultados.csv), XML JUnit completos en `final/`, [único fallo](evidencias-fase-2/revalidacion-2.4-2026-10-16/final/fallos.json).
- [JaCoCo XML completo](evidencias-fase-2/revalidacion-2.4-2026-10-16/final/jacoco.xml), [contadores](evidencias-fase-2/revalidacion-2.4-2026-10-16/final/cobertura.csv), [brechas por fuente](evidencias-fase-2/revalidacion-2.4-2026-10-16/final/brechas-cobertura.json).
- [Hashes de fuentes antes](evidencias-fase-2/revalidacion-2.4-2026-10-16/fuentes-antes.csv), [después](evidencias-fase-2/revalidacion-2.4-2026-10-16/fuentes-despues.csv), [comparación](evidencias-fase-2/revalidacion-2.4-2026-10-16/integridad-fuentes.txt): **cero diferencias en producción y pruebas**, incluidas migraciones y fuentes sin seguimiento.

El aviso JVM/Mockito de class sharing aparece en stderr como NativeCommandError de PowerShell; el resultado se determina por el código de salida y los XML, cuyo único fallo es el resumen. No se atribuyen a `check` análisis SpotBugs/PMD/Checkstyle/OWASP que el proyecto no tiene configurados.

Rama `fase-2-motor-flujo` y HEAD `990c35a9cdcfad562e302966d79b7fa35a602ee2` conservados. **Sin cambios de producción o pruebas, bases compartidas, borrado de volúmenes, commit, push ni avance a 2.B3.**

## Corrección del resumen nulo (Claude Code, 2026-10-16) — implementada, PENDIENTE DE REVALIDACIÓN

No se ejecutaron pruebas ni builds y no se modificó ninguna prueba; el reproductor `Backend24IT.resumenDeEtapaIncluyeLoOcurrido` sigue igual. Este apartado no cierra el hallazgo: lo cierra la revalidación de Codex.

- **Regla aplicada (P-20, provisional del equipo, pendiente del cliente):** `EtapaEnLinea.resumen` resume la última entrada cerrada de cada etapa con el evento real del historial que la cerró (ENVIADA, APROBADA, RECHAZADA o DEVUELTO con esa `etapa`): acción, persona (nombre vigente o su identificador si no se resuelve), rol histórico, fecha y hora de Ecuador (`dd/MM/yyyy HH:mm`), destino, observación recortada a 200 caracteres con "…" y archivos presentados. Si la etapa está activa otra vez, empieza con "Entrada anterior". Nulo sin entrada cerrada o sin evento que la reconstruya. Una etapa intermedia marcada DEVUELTA por una devolución hecha desde otra etapa conserva el evento de su propio cierre; no se le atribuye la devolución.
- **Cambios:**
  - `historial`: puerto de entrada `ResumirEtapasDePedido` (registro `Cierre`, sin tipos del dominio de historial) y `ResumirEtapasDePedidoCasoDeUso`; bean de solo lectura en `HistorialConfig`.
  - `usuarios`: puerto `NombrarRoles` y `NombrarRolesCasoDeUso` (nombres de la tabla `rol`); bean en `UsuariosConfig`.
  - `pedidos`: `VistasDePedido.Etapa.ultimoCierre`; `ConsultarPedidosCasoDeUso` recibe `ResumirEtapasDePedido` (bean en `PedidosConfig`); el texto lo arma `adapter/in/web/ResumenDeEtapa` y `PedidoRespuestas` deja de fijar `null`.
  - Contrato: descripción de `EtapaEnLinea.resumen` con la regla; el tipo sigue siendo `string | null`.
- **Fronteras:** `pedidos.application` usa solo el puerto de entrada de `historial` (permitido por `FronterasFaseDosTest`); `historial` depende de `usuarios` y `compartido`, no de `pedidos` (R9). El dominio no cambia.
- **Revalidar (Codex):** el reproductor; la suite completa y la cobertura; ArchUnit (R1–R4, R9 y fronteras de fase 2); casos de la regla: etapa reentrada ("Entrada anterior"), etapa intermedia tras una devolución desde A4, observación de más de 200 caracteres (incluidos caracteres fuera del plano básico), persona sin nombre resoluble y etapa sin cierre (nulo).

## Revalidación P-20 — Codex, 2026-10-16

**Resumen nulo del backend: CERRADO en el alcance de la regla P-20.** El reproductor original pasa y `check` completo termina **BUILD SUCCESSFUL: 482 PASS, 0 fallos, 0 errores y 0 omitidas**. P-20 sigue siendo una decisión provisional del equipo, pendiente del cliente. Este resultado no acredita la presentación del campo en las pantallas ni cierra la fase.

Se leyeron el informe y su traspaso, P-20 en el plan/OpenAPI, los puertos/implementaciones nuevos, sus consumidores y las reglas R4/R9. Skills aplicadas: `ecc-springboot-tdd`, `ecc-java-reviewer` (con referencia original), `ecc-hexagonal-architecture`, y el flujo de verificación/cobertura usado en los apartados anteriores. No se atribuye un ciclo RED de producción a código recibido ya implementado. Solo se ampliaron pruebas y se añadió este registro/evidencia: **producción y frontend intactos**.

### Reproductor original y pruebas añadidas

Primero, desde `backend/`:

```powershell
.\gradlew.bat test --tests '*Backend24IT.resumenDeEtapaIncluyeLoOcurrido' --console=plain
```

**Exit 0, BUILD SUCCESSFUL en 24 s.** El método original permanece sin cambios y pasa. Los XML de esta ejecución contienen su caso más las siete reglas de `ReglasArquitecturaTest` que el runner ejecuta también: **8 PASS**, sin omisiones. [Log](evidencias-fase-2/p20-2026-10-16/reproductor.log), XML en `p20-2026-10-16/reproductor/`.

Ampliación de `Backend24IT`, conservando todos sus casos anteriores y usando peticiones contra el backend, PostgreSQL Testcontainers y archivos reales en el disco temporal de la clase:

| Caso nuevo | Garantía comprobada | Resultado |
|---|---|---|
| `resumenP20ReentradaConservaUltimoCierreYLoReemplazaAlCerrarDeNuevo` | A2 reentrada muestra `Entrada anterior: Enviada`, destino A3 y un archivo presentado; al cerrar otra vez usa el segundo envío, sin prefijo; una tercera entrada conserva precisamente ese cierre más reciente | PASS |
| `resumenP20EtapaIntermediaConservaSuCierreTrasDevolucionDesdeA4` | A3 queda DEVUELTA por la devolución desde A4, pero su resumen sigue siendo su aprobación propia hacia A4; solo A4 describe la devolución y su observación hacia A2 | PASS |
| `resumenP20RecortaUnicodeSinCambiarObservacionCompletaDelHistorial` | Una observación con 200 emojis más texto adicional se presenta con los 200 puntos de código y `…`, sin cortar un par UTF-16; PostgreSQL y GET historial conservan el texto completo | PASS |
| `resumenP20SinCierreEsNuloAunqueHayaObservacionYArchivo` | Crear, subir y observar no cierran A1: su resumen es null; A2 nunca visitada también es null; búsqueda y detalle coinciden | PASS |
| `resumenP20UsaNombreVigentePeroConservaRolHistoricoYFechaEcuador` | Tras cambiar el nombre de la persona, el resumen usa el nombre vigente, el rol del evento, el destino y la fecha del cierre convertida a `America/Guayaquil` | PASS |

Los casos de integración comparan snapshots SQL antes/después de consultar: el resumen no crea ni modifica pedidos, etapas, entradas, eventos, adjuntos o vínculos.

Nueva `pedidos/adapter/in/web/ResumenDeEtapaP20Test`: **6 PASS**. Comprueba nombre no disponible → UUID del actor y código del rol sin inventar una persona; conversión de fecha que cruza al día anterior en Ecuador; ausencia de actor; conteo plural de archivos; límites de 199/200/201 puntos de código con emojis; resumen null sin cierre para todos los estados. El nombre no disponible se prueba mediante el contrato `Cierre` en una prueba pura, sin borrar usuarios ni fabricar filas que violen las FK.

Nueva `arquitectura/FronterasP20Test`: **1 PASS**. Prohíbe que el módulo `com.decokasa.seguimiento.historial..` dependa del módulo `com.decokasa.seguimiento.pedidos..`. No se modificaron `FronterasFaseDosTest` ni `ReglasArquitecturaTest`.

Selección ampliada final:

```powershell
.\gradlew.bat test --tests '*Backend24IT.resumen*' --tests '*ResumenDeEtapaP20Test' --tests '*FronterasP20Test' --tests '*FronterasFaseDosTest' --tests '*ReglasArquitecturaTest' --console=plain
```

**Exit 0, BUILD SUCCESSFUL en 24 s: 21 PASS** (6 integración, 6 presentación, 1 frontera nueva, 1 frontera de fase 2 y 7 reglas de arquitectura), sin fallos/errores/omitidas. [Log final](evidencias-fase-2/p20-2026-10-16/seleccion-final.log), XML en `seleccion-final/`.

Se conserva un intento inicial fallido de esa selección: la regla nueva usaba el patrón demasiado amplio `..historial..`, que también seleccionaba `pedidos.adapter.in.historial` y confundía una dependencia interna de pedidos con una dependencia del módulo historial. Se corrigió **el selector de la prueba nueva** a los paquetes completos de los dos módulos, manteniendo la prohibición y las reglas existentes. No fue un defecto de producción ni se cambió una expectativa funcional. En ese intento ya pasaban los 12 casos de resumen y las ocho comprobaciones anteriores de arquitectura. [Log inicial](evidencias-fase-2/p20-2026-10-16/seleccion.log), XML conservados en `seleccion-inicial/`.

### Fronteras R4/R9: resultado y límite de las reglas existentes

**Camino nuevo de P-20 conforme:** `ConsultarPedidosCasoDeUso` consume `historial.application.port.in.ResumirEtapasDePedido` y su registro público `Cierre`; `ResumenDeEtapa` también recibe ese contrato. No se filtra `historial.domain`, su repositorio, implementación o configuración al núcleo de pedidos. La nueva lectura de historial consume los puertos de entrada de usuarios y tipos compartidos, sin volver a pedidos.

Además de ArchUnit, se inspeccionaron descriptores, firmas genéricas y constant pool mediante `javap -classpath backend/build/classes/java/main -p -s -v`:

- **35 clases de `pedidos.application`**: cero referencias a tipos internos de historial (`domain`, `application.port.out`, `application.usecase`, `adapter`, `config`).
- **27 clases de historial**: cero referencias a pedidos.
- Se conserva también la inspección de **74 clases de pedidos**, incluidos adaptadores; no se presenta todo ese conjunto como libre de referencias a adaptadores de historial.

Evidencia: [resumen](evidencias-fase-2/p20-2026-10-16/fronteras-bytecode.txt), [aplicación de pedidos](evidencias-fase-2/p20-2026-10-16/pedidos-application-bytecode.txt), [historial](evidencias-fase-2/p20-2026-10-16/historial-bytecode.txt), [pedidos completo](evidencias-fase-2/p20-2026-10-16/pedidos-bytecode.txt).

**P20-R4-01 — MEDIO, ABIERTO, acoplamiento previo fuera del cambio P-20.** `pedidos/adapter/in/web/ObservacionController.java:5`, firma de `agregar` (línea 37) y conversión de respuesta (línea 42) usan `historial.adapter.in.web.EventoHistorialRespuesta`. La regla R4 escrita en `arquitectura.md` prohíbe consumir adaptadores de otro módulo. El bytecode confirma la dependencia entre adaptadores. El archivo tiene exactamente el mismo SHA-256 que en la revalidación previa a P-20: [comparación con el snapshot anterior](evidencias-fase-2/p20-2026-10-16/r4-acoplamiento-previo.txt). **No lo introduce el resumen**, no reabre 2.B1-D01 (la firma interna de flujos ya corregida) y no se corrige en este turno.

Las reglas automáticas actuales no cubren esa dependencia HTTP: `FronterasFaseDosTest` se limita a `pedidos.application`, y R4 general limita su selección a dominios. Por eso las pruebas R4/R9 y `check` pasan, pero **no se afirma conformidad R4 absoluta de todos los adaptadores**. El adaptador previo `pedidos.adapter.in.historial` que implementa el SPI `PedidosParaHistorial` se distingue de este consumo de un DTO web externo; la dirección de dependencias de módulos sigue sin ciclos. Una corrección posterior debe separar el mapeo HTTP sin cambiar el contrato ni debilitar esas reglas.

### `check` y cobertura real

Después de la selección y de conservar sus XML, se ejecutó desde `backend/`:

```powershell
.\gradlew.bat check --console=plain
```

**Exit 0, BUILD SUCCESSFUL en 1 min 25 s.** Se ejecutaron `test`, `jacocoTestCoverageVerification`, `check` y `jacocoTestReport`; no se reutilizó la selección filtrada. **482 pruebas ejecutadas: 482 PASS, 0 FAIL, 0 errores y 0 omitidas.** El incremento de 470→482 corresponde a cinco pruebas de integración, seis variantes de presentación y una frontera nueva; el reproductor original no se cuenta como alta.

| Clase / conjunto final | PASS | FAIL / errores / omitidas |
|---|---:|---:|
| `Backend24IT` completo | 40 | 0 / 0 / 0 |
| `ResumenDeEtapaP20Test` | 6 | 0 / 0 / 0 |
| `FronterasP20Test` | 1 | 0 / 0 / 0 |
| `FronterasFaseDosTest` + `ReglasArquitecturaTest` | 8 | 0 / 0 / 0 |
| Resto de la regresión | 427 | 0 / 0 / 0 |
| **Total** | **482** | **0 / 0 / 0** |

Cobertura JaCoCo de esa **suite completa**, sin sumar ni reutilizar cobertura de ejecuciones filtradas:

| Contador global | Cubiertas | Sin cubrir | Porcentaje |
|---|---:|---:|---:|
| **Líneas** | **2914** | **221** | **92,95 %** |
| Ramas | 834 | 279 | 74,93 % |

**Umbral existente del 80 % de líneas: PASS.** No se cambian configuración, exclusiones ni umbral. El código medido tiene 3135 líneas frente a las 3032 de la ejecución anterior: se conservan ambos resultados como mediciones distintas. No se afirma 80 % de ramas ni cobertura uniforme por archivo. En los archivos nuevos de P-20: `ResumirEtapasDePedidoCasoDeUso` 38/38 líneas, `ResumenDeEtapa` 37/38, `NombrarRolesCasoDeUso` 11/12. Se conserva el detalle de las 19 fuentes bajo el 80 % en la evidencia.

Fuentes y migraciones del backend y todo `frontend/src` se compararon por SHA-256 antes/después: **cero diferencias de producción**. Se añadieron pruebas sin alterar el método reproductor original; el control de espacios finales sobre los tres archivos de pruebas editados no detectó incidencias. El aviso JVM/Mockito de class sharing en stderr no es un fallo; se contrasta con exit 0 y XML JUnit. No se atribuyen a `check` análisis estáticos o de vulnerabilidades que el proyecto no configura.

### Aclaración del traspaso: dónde se presenta el resumen en el frontend

**Conclusión de la inspección de consumidores: el campo P-20 no se presenta en ninguna pantalla activa.** La afirmación «el frontend muestra resumen» no describe el código vigente. Lo que sí existe es el esquema que acepta el campo y un bloque de resumen del demo antiguo, sin consumir la respuesta P-20.

| Archivo / consumidor | Qué hace realmente |
|---|---|
| `frontend/src/api/pedidos.ts:64` | Acepta `resumen: z.string().nullish()` en `EtapaEnLinea`; valida y conserva el dato, sin dibujarlo |
| `frontend/src/api/pedidos.ts:250` | `usePedido` consulta el detalle real con `esquemaPedidoDetalle`; creación/acciones/asignación también aceptan ese esquema. Son consumidores de datos, no de presentación |
| `frontend/src/rutas.tsx:32` → `DetallePedido.tsx:25` | La ruta activa `/pedidos/:pedidoId` lee el detalle mediante `usePedido` |
| `DetallePedido.tsx:144` y `:154` → `LineaDeTiempo` / `PanelEtapa` | Pasa las etapas a los componentes, pero ninguno lee ni renderiza `etapa.resumen`. La línea presenta nombre/estado/fecha; el panel, asignación, fechas, reentradas, acciones, archivos y observaciones |
| `DetallePedido.tsx:166` → `HistorialPedido` | Presenta el historial de eventos en otro panel; no es el campo resumen P-20 ni prueba que se presente el resumen por etapa |
| `TrackingSearchView.tsx:210–236` | Tiene el bloque «Resumen de lo ocurrido en las etapas anteriores», pero lo construye con `Pedido` del demo, `completada`, `completadoPor` y `fechaRealFin`; no usa `EtapaEnLinea.resumen` ni la API nueva |
| `main.tsx`, `rutas.tsx` y `App.tsx` | El router se crea desde `rutas`; `App` renderiza un `Outlet`. `TrackingSearchView` no está importado ni montado en las rutas activas; la búsqueda vigente está en `ListaPedidos` |

La búsqueda recursiva en `frontend/src` no encuentra lecturas de `.resumen`: [inventario de coincidencias](evidencias-fase-2/p20-2026-10-16/consumidores-frontend.txt). **Presentar P-20 en la interfaz queda pendiente**, separado del cierre del backend y de la validación del cliente. No se modifica el frontend ni se ejecuta un recorrido de navegador para aparentar que ya está conectado.

### Evidencias y estado final de este turno

- [check](evidencias-fase-2/p20-2026-10-16/check.log), [resultados finales](evidencias-fase-2/p20-2026-10-16/final/resultados.csv), XML JUnit en `final/` y [lista de fallos vacía](evidencias-fase-2/p20-2026-10-16/final/fallos.json).
- [JaCoCo XML](evidencias-fase-2/p20-2026-10-16/final/jacoco.xml), [contadores](evidencias-fase-2/p20-2026-10-16/final/cobertura.csv), [brechas de cobertura](evidencias-fase-2/p20-2026-10-16/final/brechas-cobertura.json).
- [Producción antes](evidencias-fase-2/p20-2026-10-16/produccion-antes.csv), [después](evidencias-fase-2/p20-2026-10-16/produccion-despues.csv), [hashes finales de pruebas](evidencias-fase-2/p20-2026-10-16/pruebas-finales.csv).

**Se cierra el defecto del resumen siempre nulo del backend bajo P-20**, con los cinco bordes solicitados ejercitados. Se conservan D04–D07 y R4/2.B1-D01 cerrados en sus alcances específicos. Quedan abiertos la presentación en frontend, P20-R4-01 y los demás pendientes/decisiones provisionales del informe. El verde de `check` no aprueba por sí solo toda la fase ni una conformidad arquitectónica fuera del alcance cubierto.

Rama `fase-2-motor-flujo`, HEAD `990c35a9cdcfad562e302966d79b7fa35a602ee2` conservados. PostgreSQL Testcontainers y archivos ficticios aislados, sin bases compartidas, borrado de volúmenes, cambios de producción, commit, push ni recorrido 2.B3.

## Verificación del frontend del paso 2.5 (2026-10-16) — PARCIAL, con hallazgos

Leídos el manual vigente (fase 2, pasos 2.5 y 2.B), el plan (tareas 16 a 19 y "Resultado del paso 2.5") y este informe. Skills de referencia: `ecc:react-test`, `shadcn`, `tanstack-query-best-practices` y `playwright-best-practices` (Playwright no está instalado: el recorrido con navegador real y API real queda para 2.B3). **No se modificó código de producción.** Solo pruebas y utilidades de prueba, con datos ficticios.

### Pruebas escritas o adaptadas

| Archivo | Casos | Cubre |
|---|---:|---|
| `src/components/pedidos/DetallePedido.test.tsx` (nuevo) | 7 | encabezado; etapa actual con `aria-current="step"`; **resumen visible como texto** (conserva "Entrada anterior", no interpreta HTML); resumen de la etapa consultada en la línea de tiempo; **ausente cuando es null**; 404 y 403 |
| `src/components/pedidos/AccionesPedido.test.tsx` (nuevo) | 12 | ENVIAR en A1 con `adjuntoIds` solo de la entrada actual; APROBAR en A3; RECHAZAR en A3 con observación obligatoria; DEVOLVER_POR_ERROR y DEVOLVER_POR_PRODUCTO_NUEVO en A4 con urgencia obligatoria; 400 en `adjuntoIds`, 403, 409 `ETAPA_CAMBIO` (recarga el detalle, "Ver estado actual"), 422 con `faltantes`, 500; documentos faltantes y PENDIENTES; **sin avance ficticio** (la etapa solo cambia con la respuesta) y **sin reintentos** (una sola petición) |
| `src/components/pedidos/ArchivosYPermisos.test.tsx` (nuevo) | 16 | subida multipart con `tipoDocumento` y `entradaEsperadaId`; rechazo local de más de 100 MiB; 413/409/403 en la subida, sin reintento; descarga por el servidor con el nombre original; descarga con 403 y 409 `ARCHIVO_NO_DISPONIBLE`; observación (P-19) que solo relee el historial, observación vacía y 409; reasignación del admin solo con personas activas del rol (P-07); permisos con varios roles: consulta + marketing actúa; solo consulta no sube, no observa ni descarga; asistente_compras descarga A2 desde A3 (P-17) |
| `src/components/pedidos/ListaBandejaCreacion.test.tsx` (nuevo) | 14 | lista con enlace, etapa y vencimiento; búsqueda en el servidor con filtro en la URL y "Limpiar filtros"; solo vencidos; paginación; error con reintento; "Nuevo pedido" solo para el admin; bandeja agrupada A2/A3/A4 y bandeja vacía; creación (cuerpo enviado, navega al detalle), nombre vacío, 400 con campo, 403 y "Sin permiso" para quien no es admin |
| `src/App.test.tsx` (adaptado) | +4 | el menú sin admin ofrece Inicio, **Mi bandeja y Pedidos** (antes "solo Inicio") y sigue sin Usuarios ni Roles; con solo roles de consulta no aparece Mi bandeja; un rol de consulta no quita la bandeja que da otro rol |
| `src/components/dashboards/DashboardRol.test.tsx` (adaptado) | = | el texto "Aún no hay pedidos; las bandejas llegan en la fase 2." se sustituye por el enlace a la bandeja (o a Pedidos en los paneles provisionales) y se comprueba que el texto anterior ya no aparece; la ruta inexistente pasa de `/pedidos` (que ahora existe) a `/ruta-que-no-existe`. Se conservan las demás comprobaciones |
| `src/test/pedidosFicticios.ts` y `src/test/radix.ts` (nuevos) | — | datos y API simulada con estado y registro de llamadas; utilidades de jsdom para el `Select` de Radix |

### Comandos y resultados (desde `frontend/`)

| Comando | Resultado | Evidencia |
|---|---|---|
| `npm run lint` | **exit 0**, sin errores de tipos | [lint.log](evidencias-fase-2/frontend-2.5-2026-10-16/lint.log) |
| `npm run test` (con el `.env.local` del equipo) | **exit 1: 21 archivos (7 con fallos), 218 pruebas: 173 PASS / 45 FAIL.** Fallan también suites anteriores al 2.5 (`LoginScreen`, `SesionProvider`, `RutaProtegida`); ver F-2.5-01 | [test-con-env-local.log](evidencias-fase-2/frontend-2.5-2026-10-16/test-con-env-local.log) |
| `VITE_ACCESO_LOCAL=false npm run test` | **exit 0: 21 archivos, 218 pruebas PASS**, 0 fallos | [test.log](evidencias-fase-2/frontend-2.5-2026-10-16/test.log) |
| `npm run build` | **exit 0**; aviso informativo de un fragmento JS de 760,86 kB (más de 500 kB) | [build.log](evidencias-fase-2/frontend-2.5-2026-10-16/build.log) |

### Navegador: presentación del resumen

Chrome DevTools sobre `npm run dev` (puerto 3000), en un contexto aislado. Las respuestas de `/api/v1/pedidos/{id}`, `/historial`, `/archivos` y `/flujos` se sustituyeron por datos ficticios con un script inicial: sin backend y sin bases de datos. La sesión es el acceso local de desarrollo del `.env.local`. El servidor de desarrollo se detuvo al terminar.

- **Escritorio 1440×1000:** el resumen de A3 aparece bajo los datos de la etapa, como texto, con "Entrada anterior" y la observación `<b>…</b>` literal (sin elemento `b`). [Captura](evidencias-fase-2/frontend-2.5-2026-10-16/resumen-escritorio.png). En el mismo recorrido aparece el **defecto F-2.5-02**.
- **Móvil 390×844 (táctil):** el resumen se ajusta al ancho (326 px dentro de 390) y el documento no se desborda (`scrollWidth` 390). [Captura](evidencias-fase-2/frontend-2.5-2026-10-16/resumen-movil.png).

### Hallazgos (reproductores para Claude; no se corrigió producción)

**F-2.5-01 — MEDIO, entorno de pruebas, ABIERTO.** Si `frontend/.env.local` (ignorado por Git, solo de desarrollo) define `VITE_ACCESO_LOCAL=true`, Vitest carga ese valor (`import.meta.env.DEV` es verdadero en las pruebas) y `SesionProvider` abre la sesión local de admin en todas las pruebas. Reproductor: con ese `.env.local`, `npm run test` da 45 fallos, incluidas suites de la fase 1; con `VITE_ACCESO_LOCAL=false` en el entorno del proceso, 218 PASS. El build de producción no se ve afectado (`DEV` es falso). Corrección sugerida: fijar `VITE_ACCESO_LOCAL` en `'false'` en `test.env` de la configuración de Vitest, o excluir el modo `test` en `accesoLocalHabilitado`.

**F-2.5-02 — MEDIO, producción (maquetación), ABIERTO.** En escritorio, el detalle del pedido se desborda en horizontal: con una ventana de 1440 px, `document.documentElement.scrollWidth` es 1522 (82 px de desplazamiento) y el encabezado morado no llega al borde. Medición: `<main>` 1298 px más el menú de 224 px; la línea de tiempo de 11 etapas tiene 1232 px de contenido (`min-w-max`). Causa probable: `main` es `flex-1` sin `min-w-0`, así que crece hasta el ancho de la línea de tiempo en vez de dejar que su contenedor `overflow-x-auto` la desplace. Reproductor: abrir `/pedidos/{id}` de un flujo de 11 etapas a 1440×1000 y comparar `scrollWidth` con `innerWidth`. Contradice DESIGN.md 7.2 (el desplazamiento horizontal es de la línea de tiempo, no de la página).

**F-2.5-03 — BAJO, accesibilidad, ABIERTO.** El resumen usa el `Alert` de shadcn, que lleva `role="alert"`: un lector de pantalla lo anunciaría como una alerta urgente cada vez que aparece, aunque es contenido informativo. Reproductor: `document.querySelector('[data-slot="alert"]').getAttribute('role')` en el panel de la etapa devuelve `alert`. Sugerencia: un contenedor sin rol de alerta, o `role="note"`.

**Limitación del entorno (no es un defecto):** en jsdom, el `fetch` de Node no puede enviar el `FormData` ni el `File` de jsdom ("Premature close"), y con los nativos MSW no puede leer el cuerpo multipart (longitud distinta en undici). La prueba de subida usa `FormData` y `File` nativos, registra los campos que arma la aplicación y comprueba que la petición llega como `multipart/form-data`. El envío multipart real queda para 2.B3.

### Pendiente y fuera de esta verificación

- Recorrido integral A1–A4 con API y navegador reales (Playwright, 2.B3), incluidas la descarga `blob:` bajo la CSP de `nginx.conf` y el 413 de nginx, que no se probaron (no había nginx).
- Escáner de impeccable y revisión visual completa de todas las pantallas: solo se revisaron el detalle y el resumen.
- **P20-R4-01 sigue ABIERTO** (acoplamiento entre adaptadores: `ObservacionController` usa `historial.adapter.in.web.EventoHistorialRespuesta`); esta verificación no lo toca.
- Pendientes funcionales y decisiones provisionales del cliente: sin cambios.

Sin bases compartidas, borrado de volúmenes, commit, push ni cierre de fase. **La fase 2 sigue abierta.**

## Correcciones de F-2.5-02, F-2.5-03 y P20-R4-01 (Claude Code, 2026-10-16) — implementadas, PENDIENTES DE REVALIDACIÓN

No se ejecutaron pruebas ni builds y no se modificó ninguna prueba. Ninguna de las tres correcciones existía antes; se comprobó en `App.tsx`, `PanelEtapa.tsx`, `ui/alert.tsx` y `ObservacionController.java`. El resumen de `PanelEtapa.tsx` se conserva igual: el mismo texto del servidor, como texto, y nada si es null. **F-2.5-01 (entorno de pruebas) no estaba en el alcance y sigue ABIERTO.**

- **F-2.5-02, desbordamiento horizontal:** `frontend/src/App.tsx`, `<main>` pasa a `min-w-0 flex-1`. Sin `min-w-0`, el elemento flexible no se encogía por debajo del ancho de su contenido (la línea de tiempo con `min-w-max`) y ensanchaba la página; ahora el contenido ancho se desplaza dentro de su propio contenedor `overflow-x-auto` (DESIGN.md 7.1 y 7.2). La corrección está en el diseño común, así que también cubre las tablas de pedidos.
  - **Revalidar:** a 1440×1000, en `/pedidos/{id}` con 11 etapas, `document.documentElement.scrollWidth` debe ser igual a `innerWidth` y la línea de tiempo debe desplazarse dentro de su tarjeta; repetir en móvil (390) y con `/pedidos`.
- **F-2.5-03, semántica del resumen:** `frontend/src/components/pedidos/PanelEtapa.tsx`, el `Alert` del resumen recibe `role="note"`, que reemplaza el `role="alert"` del componente base porque las props se aplican después, y `aria-labelledby` apunta a su título "Lo ocurrido en la etapa". Se mantiene `data-slot="alert"` y el estilo de shadcn. El componente base `ui/alert.tsx` no cambia: los avisos de error siguen siendo alertas.
  - **Revalidar:** el contenedor del resumen tiene `role="note"` y nombre accesible "Lo ocurrido en la etapa"; un lector de pantalla no lo anuncia como alerta. Fuera del alcance: el `Alert` de "acciones incompletas" del mismo panel conserva `role="alert"`.
- **P20-R4-01, dependencia entre adaptadores:**
  - `pedidos/adapter/in/web/ObservacionController` ya no importa `historial.adapter.in.web.EventoHistorialRespuesta`: responde con su propio `ObservacionRespuesta` (mismo esquema `EventoHistorial` del contrato, sin cambios en el JSON) construido desde el puerto de entrada `ConsultarHistorialPedido`.
  - Además se cerró una fuga del puerto: `ConsultarHistorialPedido.Entrada` exponía `historial.domain.EventoPedido`; ahora expone la vista `ConsultarHistorialPedido.Evento` (tipos de `compartido` y del JDK). `ConsultarHistorialPedidoCasoDeUso` hace la conversión dentro del módulo.
  - `EventoHistorialRespuesta.de` vuelve a ser de paquete y solo lo usa `HistorialController`.
  - **Revalidar:** ArchUnit (R1–R4, R9, `FronterasFaseDosTest`, `FronterasP20Test`) e inspección del bytecode de las clases de `pedidos` (incluidos adaptadores) sin referencias a `historial.adapter` ni a `historial.domain`; contrato de `POST /pedidos/{id}/observaciones` (201, esquema `EventoHistorial`) y de `GET /pedidos/{id}/historial` sin cambios; suite completa y cobertura.

## Revalidación F-2.5-01/02/03 y P20-R4-01 — Codex, 2026-10-16

**Los cuatro hallazgos se cierran en sus alcances comprobados.** Se conservan todos los resultados anteriores como históricos. No se modifica producción ni `.env.local`: el único cambio de configuración autorizado es `test.env.VITE_ACCESO_LOCAL = 'false'` en `frontend/vite.config.ts`. Se añaden aserciones, sin eliminar ni debilitar pruebas existentes. Skills aplicadas: `ecc-react-test` (y referencia original), `ecc-vite-patterns`, `playwright-best-practices` (guías de viewport y aserciones), `ecc-accessibility`; se mantienen los criterios de verificación Spring Boot/JaCoCo y fronteras R4/R9 de las revalidaciones previas. Es verificación de correcciones recibidas, no un recorrido 2.B3.

**Frase de traspaso reparada y completa:** «El frontend presenta el resumen que entrega el servidor en el panel de la etapa seleccionada, bajo el título “Lo ocurrido en la etapa”, como nota informativa con nombre accesible y sin `role=alert`; si el resumen es null, no presenta ese bloque». Esta es la descripción vigente, sin la contradicción anterior con «no lo usa en ninguna pantalla». Las conclusiones de inspecciones previas describen versiones anteriores y se conservan como evidencia histórica.

### F-2.5-01 — corrección limitada al entorno de Vitest

`frontend/vite.config.ts`, dentro de `test`:

```typescript
env: { VITE_ACCESO_LOCAL: 'false' },
```

No se cambia `accesoLocalHabilitado`, ninguna pantalla, configuración de build o archivo `.env.local`. El comando **`npm run test` sin sobrescribir variables en el shell** pasa ahora **219/219**, incluidas las suites de acceso que antes fallaban por la sesión local de admin. La nueva prueba de nota accesible explica el incremento de 218→219. La repetición instrumentada para cobertura también pasa las mismas 219; no se suman ambas ejecuciones. Los hashes de `.env.local` antes/después coinciden.

### F-2.5-02 y F-2.5-03 — navegador aislado y accesibilidad específica

Se ejecuta [revalidacion-2.5.mjs](../../pruebas-navegador/revalidacion-2.5.mjs) con Playwright y Chrome headless del equipo. Playwright se instaló **solo en `.tmp/verificacion-ui`**, sin modificar dependencias o lockfile del frontend. Vite temporal en `127.0.0.1:3017`, con acceso local deshabilitado en el entorno de ese proceso, sesión ficticia y todas las peticiones `/api/v1/` interceptadas; **sin backend ni base compartida**. Contextos nuevos por ancho, escritorio 1440×1000 y móvil táctil 390×844. Vite y Chrome se detuvieron al terminar.

La prueba espera componentes por rol/nombre, comprueba medidas DOM, mueve la línea de tiempo y verifica su semántica accesible. Resultados:

| Ancho | `documentElement.scrollWidth` detalle | `scrollWidth` listado | Ancho visible / contenido de línea | `scrollLeft` inicial→final |
|---:|---:|---:|---:|---:|
| **1440** | **1440** | **1440** | 1166 / 1248 | 0→82 |
| **390** | **390** | **390** | 356 / 1248 | 0→500 |

**Sin desbordamiento horizontal de página** en detalle ni listado; `body.scrollWidth` también coincide con el viewport. La línea mantiene `overflow-x: auto` y contenido más ancho que su tarjeta; se desplaza dentro de ella. No se oculta el exceso de contenido para aparentar que cabe. Las cuatro capturas se conservan y se inspeccionaron visualmente las del detalle. [Mediciones y aserciones](evidencias-fase-2/revalidacion-2.5-2026-10-16/navegador.json), [log final](evidencias-fase-2/revalidacion-2.5-2026-10-16/navegador-final.log), [detalle escritorio](evidencias-fase-2/revalidacion-2.5-2026-10-16/detalle-1440.png), [detalle móvil](evidencias-fase-2/revalidacion-2.5-2026-10-16/detalle-390.png), [listado escritorio](evidencias-fase-2/revalidacion-2.5-2026-10-16/lista-1440.png), [listado móvil](evidencias-fase-2/revalidacion-2.5-2026-10-16/lista-390.png).

El resumen se encuentra por **`getByRole('note', { name: 'Lo ocurrido en la etapa' })`** en ambos tamaños, y el árbol accesible confirma ese nombre. El contenedor tiene `role=note`, no `alert`, y no contiene alertas descendientes. Se conserva el texto exacto con «Entrada anterior» y `<b>Texto literal</b>` sin crear elementos HTML. Cero errores JavaScript de página y cero rutas API sin simular en la ejecución final. No se afirma una auditoría WCAG completa ni una prueba con lector de pantalla real; la comprobación acredita el rol, nombre y ausencia de alerta urgente del resumen. Los avisos de error de la aplicación conservan sus roles.

Nueva prueba RTL en `DetallePedido.test.tsx`: **«presenta el resumen como nota informativa con nombre accesible y sin alerta urgente»**, aprobada junto con los siete casos previos de detalle (texto literal, null, selección de etapa, errores y encabezado).

Hubo dos intentos de navegador anteriores no válidos: el interceptor inicial `**/api/**` capturaba también el módulo de Vite `/src/api/cliente.ts`, impidiendo cargar la aplicación. El diagnóstico mostró cuerpo vacío y esa ruta en `sinMock`. Se corrigió exclusivamente el script de prueba para interceptar `/api/v1/`; no se cambió producción ni se relajó ninguna aserción. Se conservan [log inicial](evidencias-fase-2/revalidacion-2.5-2026-10-16/navegador.log) y [diagnóstico](evidencias-fase-2/revalidacion-2.5-2026-10-16/navegador-diagnostico.log). No se cuentan esos fallos de fixture como defectos de la interfaz.

### P20-R4-01 — arquitectura, firmas y JSON público

Nueva prueba **`FronterasP20Test.r4PedidosIncluidosAdaptadoresNoUsaDominioNiAdaptadoresDeHistorial`**: aplica a todo `com.decokasa.seguimiento.pedidos..`, incluidos sus adaptadores, y prohíbe depender de `historial.domain..` y `historial.adapter..`. Pasa; se conserva la prueba R9 inversa de historial y las reglas anteriores sin cambios. En la suite completa: `FronterasP20Test` **2/2**, `FronterasFaseDosTest` **1/1** y `ReglasArquitecturaTest` **7/7**.

Inspección adicional de todas las clases compiladas de pedidos, incluidas anidadas:

```powershell
javap -classpath backend/build/classes/java/main -p -s -c -v $clases
```

**76 clases inspeccionadas; cero referencias a `historial.domain` o `historial.adapter`**, tanto con puntos como con barras, en constant pool, descriptores, firmas genéricas e instrucciones. `ObservacionController.agregar` retorna su propio `ObservacionRespuesta`. Se inspeccionaron además `ConsultarHistorialPedido` y sus registros `Entrada`, `Evento` y `Persona` con `javap -p -s -v`: **cero tipos internos de dominio/adaptadores de historial**. `Entrada.evento` expone la vista del puerto, no el antiguo `EventoPedido`; los componentes de `Evento` usan JDK y tipos compartidos. [Resumen](evidencias-fase-2/revalidacion-2.5-2026-10-16/fronteras.txt), [pedidos-bytecode](evidencias-fase-2/revalidacion-2.5-2026-10-16/pedidos-bytecode.txt), [contratos-bytecode](evidencias-fase-2/revalidacion-2.5-2026-10-16/contratos-bytecode.txt).

Nueva integración **`Backend24IT.observacionEHistorialConservanElMismoJsonPublicoTrasSepararAdaptadores`**: PostgreSQL Testcontainers y archivo ficticio real, sin reutilización de base. Crea una observación con adjunto válido mediante **POST `/pedidos/{id}/observaciones` → 201**. Exige exactamente los 12 campos públicos de `EventoHistorial`: `id`, `pedidoId`, `numeroPedido`, `etapa`, `tipo`, `usuario`, `rolIntervencion`, `detalle`, `fecha`, `observacion`, `nivelUrgencia`, `adjuntoIds`. Comprueba valores, UUID, fecha ISO con offset Ecuador, usuario con `id/nombre/roles`, los cinco campos nulos de detalle cuando no aplican y la lista del adjunto presentado. Después exige que el evento de **GET `/pedidos/{id}/historial` → 200** sea **idéntico como JSON completo** al POST, incluidos nulos, anidados y tipos; consultar no modifica el snapshot SQL. **PASS.** Es una comprobación de compatibilidad del esquema y comportamiento vigentes, no una afirmación basada solo en que dos DTO compilan.

### Comandos, resultados y cobertura reales

Desde `frontend/`, sin modificar `.env.local` ni anteponer `VITE_ACCESO_LOCAL=false` a los comandos de pruebas:

| Comando | Resultado final |
|---|---|
| `npm run lint` | **exit 0** (`tsc --noEmit`) |
| `npm run test` | **exit 0: 21 archivos, 219 PASS, 0 fallos/omitidas**, 22,25 s |
| `npm run build` | **exit 0**; JS principal 760,94 kB, aviso de más de 500 kB conservado |
| `npm run coverage -- --coverage.reporter=json-summary --coverage.reporter=text --reporter=json --outputFile=…/frontend-vitest.json` | **exit 0: 219 PASS**, informe V8 conservado |

Las primeras tentativas de test/build dentro del sandbox fallaron al cargar dependencias nativas Tailwind/Vite (`UNLOADABLE_DEPENDENCY`); se repitieron fuera con permiso. No fueron fallos de aserciones y no se instalaron ni repararon dependencias de producción. Los avisos de Vite sobre `__dirname` y el fragmento JS grande permanecen informativos; no se ocultaron.

Desde `backend/`:

```powershell
.\gradlew.bat check --console=plain
```

**Exit 0, BUILD SUCCESSFUL en 1 min 47 s; 484 pruebas PASS, 0 fallos/errores/omitidas.** Se ejecutaron `test`, `jacocoTestCoverageVerification`, `check` y `jacocoTestReport`. El incremento de 482→484 son la prueba de contrato y la frontera nueva; no se cuentan dos veces ejecuciones previas. `Backend24IT` completo: **41/41**. La regresión de migraciones, archivos reales, concurrencia, permisos y P-20 sigue aprobada.

| Cobertura medida | Líneas cubiertas / total | Líneas % | Ramas cubiertas / total | Ramas % |
|---|---:|---:|---:|---:|
| Frontend, informe V8 de archivos instrumentados | 1011 / 1099 | **91,99 %** | 847 / 1030 | 82,23 % |
| Backend, JaCoCo de toda la suite | 2934 / 3155 | **93,00 %** | 840 / 1123 | 74,80 % |

JaCoCo exige el **80 % de líneas** y pasa sin cambiar exclusiones ni umbral. Vitest no tiene un umbral de cobertura configurado: se informa la medición, no una puerta inexistente. Su total V8 incluye utilidades de `src/test` y los archivos ejecutados, no todos los archivos antiguos del demo sin importar. Como dato adicional, excluyendo `src/test` del JSON ya generado, los **54 archivos de producción instrumentados** suman **933/1019 líneas = 91,56 %**; no se cambió configuración para obtener esa cifra. No se afirma cobertura completa de toda la UI ni 80 % de ramas del backend.

Evidencias: [lint](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-lint.log), [test sin variables externas](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-test.log), [build](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-build.log), [cobertura frontend](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-cobertura.log), [JSON Vitest](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-vitest.json), [resumen V8](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-coverage-summary.json), [check backend](evidencias-fase-2/revalidacion-2.5-2026-10-16/backend-check.log), [resultados backend por clase](evidencias-fase-2/revalidacion-2.5-2026-10-16/backend-final/resultados.csv), [JaCoCo](evidencias-fase-2/revalidacion-2.5-2026-10-16/backend-final/jacoco.xml) y [contadores](evidencias-fase-2/revalidacion-2.5-2026-10-16/backend-final/cobertura.csv). XML JUnit y brechas de cobertura en `backend-final/`.

### Cierres y conservación del alcance

- **F-2.5-01: CERRADO.** Vitest fija acceso local false, comando ordinario aprobado y `.env.local` intacto.
- **F-2.5-02: CERRADO.** Detalle y lista sin desbordamiento de página a 1440/390; línea de tiempo con desplazamiento interno real.
- **F-2.5-03: CERRADO.** Nota informativa con nombre accesible y sin alerta urgente, tanto en RTL como en Chrome.
- **P20-R4-01: CERRADO.** Regla ampliada y bytecode sin referencias prohibidas, puerto sin fuga de dominio y compatibilidad JSON POST/GET comprobada.

El backend P-20 y D04–D07 conservan sus cierres. Continúan las decisiones provisionales del cliente, el aviso de tamaño JS y los demás pendientes no cerrados expresamente. Estos casos visuales con API simulada **no son 2.B3**: no se hizo el recorrido integral navegador+API A1–A4, ni se verificaron nginx, CSP de descarga o Drive. **La fase 2 sigue abierta.**

Cambios de este turno: `vite.config.ts` solo en `test.env`; una prueba nueva de nota en `DetallePedido.test.tsx`; una prueba de contrato en `Backend24IT`; una regla nueva en `FronterasP20Test`; script visual aislado e informe/evidencias. Los hashes del backend de producción y del frontend sin sus tests coinciden con el inicio; el único cambio bajo `frontend/src` es la prueba de detalle. Los hashes de `.env.local` también coinciden. [Antes](evidencias-fase-2/revalidacion-2.5-2026-10-16/produccion-antes.csv), [después](evidencias-fase-2/revalidacion-2.5-2026-10-16/produccion-despues.csv), [diferencias de fuentes](evidencias-fase-2/revalidacion-2.5-2026-10-16/diferencias-fuentes.csv), [env antes](evidencias-fase-2/revalidacion-2.5-2026-10-16/env-local-antes.csv), [env después](evidencias-fase-2/revalidacion-2.5-2026-10-16/env-local-despues.csv).

Rama `fase-2-motor-flujo`, HEAD `990c35a9cdcfad562e302966d79b7fa35a602ee2` conservados. Sin bases compartidas, borrado de volúmenes, commit, push ni cierre de fase.

## Paso 2.B3 — integración real y revisión de interfaz, Codex, 2026-10-16

**Resultado: recorrido solicitado aprobado en el entorno aislado descrito abajo. No se cierra la fase.** Se leyeron el manual vigente DOCX (OOXML, «Paso 2.B3 Recorrido A1 a A4»), el plan de fase 2, este informe, el contrato OpenAPI y `documentos/diseno/DESIGN.md`. El plan conserva frases anteriores a las revalidaciones; los resultados cronológicos de este informe son la evidencia de sus correcciones. Skills aplicadas: `ecc-e2e-testing`, `playwright-best-practices` (localizadores, espera por respuestas, archivos y contextos separados) e `impeccable` (`context`, playbook `audit` y detector). La revisión es de solo lectura; no se modificó producción ni se debilitaron pruebas.

### Entorno, comandos y trazabilidad

Evidencias de esta ejecución: [carpeta 2.B3](evidencias-fase-2/2.B3-2026-10-16/). Proyecto Docker **`decokasa-f2b3-20261016`**, red propia y volúmenes nuevos **`decokasa-f2b3-20261016_pg-propio`** y **`decokasa-f2b3-20261016_archivos-propios`**. No se utilizó el compose principal ni las bases/volúmenes del recorrido de fase 1. PostgreSQL 17.11, Flyway V1/V2/V3 aprobado en esta base vacía ([migraciones](evidencias-fase-2/2.B3-2026-10-16/migraciones.txt)). Esta ejecución no sustituye la prueba anterior de migración V2 poblada.

API Spring real, JAR del código vigente sobre `eclipse-temurin:21-jre`; frontend compilado de la ejecución anterior montado en nginx, con **la plantilla y el script de límites vigentes**. Origen `http://localhost:13023`, API directa de diagnóstico en `127.0.0.1:18093`; PostgreSQL sin puerto publicado. El arnés usa el JRE como usuario predeterminado de su imagen: no acredita el endurecimiento del usuario del Dockerfile productivo. [Compose completo](evidencias-fase-2/2.B3-2026-10-16/compose-aislado.yaml), [nginx efectivo](evidencias-fase-2/2.B3-2026-10-16/nginx-efectivo.txt) y [registros de servicios](evidencias-fase-2/2.B3-2026-10-16/servicios.log).

Siete cuentas ficticias: admin, marketing, revisión, China, logística, consulta/asistente China y una persona marketing/asistente de compras. [Semilla exclusiva](evidencias-fase-2/2.B3-2026-10-16/usuarios-ficticios.json). Todas las sesiones de navegador entraron por `/login`, escribiendo identificador y contraseña ficticia y esperando **POST `/auth/login` 200**. No se inyectaron tokens ni se usó autoingreso de desarrollo. Los probes HTTP adicionales utilizan el token emitido por ese login real; se distinguen del gesto de interfaz. **Ningún script usa `route.fulfill` ni respuestas simuladas.**

Comandos ejecutados desde la raíz salvo indicación:

```powershell
# backend/: empaquetado necesario; sin volver a ejecutar suites
.\gradlew.bat bootJar -x test --console=plain
# raíz: solo este compose, nunca el principal
docker compose -f .tmp/f2b3-20261016/compose.yaml up -d
node pruebas-navegador/fase-2-b3.mjs
node pruebas-navegador/fase-2-b3-sesiones.mjs
node pruebas-navegador/fase-2-b3-auditoria.mjs
docker compose -f .tmp/f2b3-20261016/compose.yaml restart backend
node pruebas-navegador/fase-2-b3-persistencia.mjs
docker compose -f .tmp/f2b3-20261016/compose.yaml logs --no-color
docker compose -f .tmp/f2b3-20261016/compose.yaml exec -T frontend nginx -T
docker compose -f .tmp/f2b3-20261016/compose.yaml exec -T postgres psql -U f2b3 -d f2b3 -c 'select version, description, success from flyway_schema_history order by installed_rank;'
& C:\Users\Richard\.codex\skills\impeccable\scripts\impeccable.cmd context --target frontend/src/components/pedidos/DetallePedido.tsx
& C:\Users\Richard\.codex\skills\impeccable\scripts\impeccable.cmd detect --json frontend/src/components/pedidos
```

Chrome instalado, Playwright aislado en `.tmp/verificacion-ui/node_modules`; scripts reproducibles en `pruebas-navegador/fase-2-b3*.mjs`. El empaquetado dio **BUILD SUCCESSFUL**, sin ejecutar tests. Docker/Chrome/Gradle necesitaron ejecución autorizada fuera del sandbox (acceso al pipe, creación del proceso y caché Gradle); sus primeros errores de permisos no son defectos del producto.

### Resultados comprobados

El recorrido principal final tiene **16 comprobaciones PASS** ([log](evidencias-fase-2/2.B3-2026-10-16/recorrido.log), [resultados y tráfico](evidencias-fase-2/2.B3-2026-10-16/resultados.json)). Pedido principal **DK-EC-2026-0002**; el UUID completo y autoritativo se conserva en [pedido.json](evidencias-fase-2/2.B3-2026-10-16/pedido.json).

| Caso | Evidencia y resultado real |
|---|---|
| Crear y avanzar A1 → A2 → A3 → A4 → A5 | Creación, subida y acciones desde formularios; 201/200 reales, etapa de la respuesta comprobada en cada paso. Se detuvo en A5, sin operaciones de fases posteriores. |
| Archivos obligatorios | Solicitud y cuatro documentos codificados de entradas diferentes, con bytes ficticios reales. Subidas 201; selección de adjuntos de la entrada actual en la confirmación. |
| Rechazar A3 → A2 | Observación obligatoria enviada; cambia `entradaId`. Presentar el id de la entrada anterior da **400** según OpenAPI; enviar sin documentos da **422**. Un documento nuevo permite reenviar. |
| Devolver A4 por error | China devuelve con observación a A2; nuevo archivo, envío y aprobación permiten volver a A4. |
| Devolver A4 por producto nuevo | Se selecciona **Urgente · 4 h** en la interfaz; respuesta con `urgencia.nivel=URGENTE`, se conserva en A3 y desaparece al regresar a A4. El historial muestra que terminó. No se esperaron cuatro horas reales ni se revalidó el calendario ya probado en dominio. |
| Observaciones y resumen | Texto con espacios se persiste recortado; POST 201. Nota real «Lo ocurrido en la etapa», sin alertas descendientes, con texto del servidor. Capturas muestran datos de reentradas, documentos antiguos separados y resumen de cierre. |
| Descargas y permisos P-17 | Descarga real desde el botón para revisión (A2), admin y Marketing; comparación de bytes y SHA-256. Consulta y China reciben **403** para los documentos de A2; China no obtiene permiso por inferencia desde A4. Sin violaciones CSP en las descargas comprobadas. |
| Cuenta sin permiso | Consulta no ve Aprobar; intento HTTP real de actuar devuelve **403**. |
| Reasignación y roles múltiples | Admin reasigna mediante diálogo a Multi Prueba; Marketing deja de ver Enviar, recibe 403 y el historial permanece idéntico. Multi sube, envía como Marketing y aprueba como asistente de compras. [Suplemento](evidencias-fase-2/2.B3-2026-10-16/suplemento.json). |
| Conflicto entre sesiones | Admin y China abren sus confirmaciones antes del envío simultáneo. Resultados **200 y 409 ETAPA_CAMBIO**; el diálogo perdedor ofrece «Ver estado actual». Solo **un evento adicional**, etapa final A5. [Captura](evidencias-fase-2/2.B3-2026-10-16/conflicto-sesiones.png). |
| Acceso y bandeja | Login normal de los siete perfiles, bandeja multirrol real 200, logout y recarga mantienen el formulario de login. No es una repetición de toda la suite de seguridad de fase 1. |
| Persistencia | Recarga del navegador conserva pedido y resumen. Tras reiniciar **solo la API aislada**, nuevo login recupera A5, cuatro adjuntos de A2 y sus bytes; los eventos anteriores coinciden como JSON completo con el snapshot previo. |

El historial principal final contiene **19 eventos**, derivados de las operaciones efectivamente ejecutadas: 1 creación, 5 archivos, 5 envíos, 1 rechazo, 2 devoluciones, 4 aprobaciones y 1 observación. No se impuso un total inventado del contrato: los contadores corresponden a este guion concreto. Todos tienen actor, rol y fecha parseable. [Historial antes de la aprobación final](evidencias-fase-2/2.B3-2026-10-16/historial.json), [contadores y descargas](evidencias-fase-2/2.B3-2026-10-16/auditoria.json), [reinicio y efectos parciales](evidencias-fase-2/2.B3-2026-10-16/persistencia.json).

Los rechazos por permiso, documentos anteriores o faltantes y el conflicto no añaden eventos de acción ni permiten avances. Tras ambos 413, el pedido de límites sigue en A1, sin adjuntos y únicamente con PEDIDO_CREADO. El historial anterior a la denegación por reasignación es idéntico al posterior. No se modificó la base directamente para fabricar estados del flujo.

Inspección de rutas activas, componentes de pedidos y cliente API: sin referencias a `orderState`, `utils/permissions` ni `initialData`, y sin persistencia de pedidos en local/sessionStorage. El estado observado tras recargar y reiniciar la API proviene de PostgreSQL y del almacén propio. Los módulos de demo siguen fuera de las rutas activas; no se afirma que hayan sido eliminados del repositorio.

### nginx, CSP y límites

La plantilla efectiva mantiene la CSP productiva, incluido `default-src 'self'`; la descarga mediante blob funciona en Chrome y conserva contenido/nombre. Se comprobó la prohibición de descarga por rol con la API real, además de la visibilidad del botón.

**Configuración exclusiva de prueba: 1 MiB por archivo, 2 MiB de cuerpo nginx**, usando las variables existentes. Un archivo real de **1 048 577 bytes** pasa nginx y Spring devuelve 413 problem+json con `codigo=ARCHIVO_DEMASIADO_GRANDE` e `instance`. Uno de **3 145 728 bytes** es rechazado por nginx con el mismo código y formato contractual. El límite productivo provisional de 100 MiB y el texto fijo de la interfaz no se cambiaron; estos tamaños acreditan las dos rutas configurables, no una medición nueva de 100 MiB. Respuestas completas en `resultados.json`; `nginx-efectivo.txt` confirma el límite de cuerpo aplicado.

### Revisión impeccable y presentación

`impeccable context` funcionó: no encontró PRODUCT.md ni DESIGN.md en la raíz; para esta auditoría acotada se utilizó la autoridad existente `documentos/diseno/DESIGN.md`, sin crear contexto ni rediseñar. El detector produjo **[]**, cero hallazgos mecánicos ([salida](evidencias-fase-2/2.B3-2026-10-16/impeccable-detector.json)). Se inspeccionaron las capturas reales en una pasada de escritorio/móvil y una confirmación táctil; no hubo cambios visuales.

| Dimensión técnica | Puntuación orientativa /4 | Evidencia y límite |
|---|---:|---|
| Accesibilidad | 3 | Formularios localizados por sus etiquetas, botones y diálogos semánticos; resumen como nota nombrada. Sin auditoría WCAG completa, lector real o barrido de contraste de todos los estados. |
| Rendimiento | 2 | Respuestas y navegación operativas, sin errores JS observados; se conserva el aviso previo de fragmento JS de 760,94 kB. Sin medición de Core Web Vitals o carga concurrente. |
| Tema | 3 | Tokens de identidad y estados, tipografía y foco coherentes con DESIGN.md. Solo tema claro aprobado, sin atribuir una verificación de modo oscuro. |
| Adaptación | 3 | Página sin desbordamiento a 1440/390; botones medidos del detalle no menores de 40 px, conforme al diseño vigente. No se certifica objetivo reforzado de 44 px para todos los enlaces. |
| Integridad | 4 | Detector vacío; etapas/acciones/documentos/historial expresan el estado del servidor; sin contenido decorativo que simule el flujo. |
| **Total** | **15/20** | Buena base; puntuación acotada a las superficies y estados observados, no certificación global. |

[Escritorio 1440](evidencias-fase-2/2.B3-2026-10-16/detalle-1440.png), [móvil 390](evidencias-fase-2/2.B3-2026-10-16/detalle-390.png), [táctil](evidencias-fase-2/2.B3-2026-10-16/movil-tactil.png), [bandeja multirrol](evidencias-fase-2/2.B3-2026-10-16/bandeja-multirrol.png). En móvil Chrome emulado con touch, un gesto sintetizado CDP desplaza la línea internamente de **0 a 260 px** (máximo 892), con ancho de página **390**. No equivale a una prueba en teléfono físico. Se mantienen lectura del historial, estado con texto además del color y separación de archivos de entradas previas. El menú móvil tiene su propio desplazamiento interno según su diseño.

**Sin hallazgos funcionales nuevos reproducibles para devolver a Claude.** Se conserva como pendiente técnico menor el tamaño del bundle ya conocido; una futura revisión autorizada puede usar `$impeccable optimize` y terminar con `$impeccable polish`, sin hacer esas correcciones en este turno. No se reabren F-2.5-02/03 por límites no probados ni se declara conformidad WCAG completa.

### Incidencias del arnés, cobertura y pendientes

Se conservaron diagnósticos que **no son defectos de producción**: el primer script esperaba 422 para id antiguo (OpenAPI establece 400) y buscaba `nivelUrgencia` en el pedido en vez de `urgencia.nivel`. Se corrigieron las aserciones conforme al contrato y se añadió explícitamente el 422 por lista vacía, sin eliminar el control de entrada. El suplemento sufrió conversión de acentos en selectores al escribirse por una tubería PowerShell; el conflicto pasó tras corregir el selector y el logout se comprobó independientemente en `auditoria.json`. Finalmente, el probe de 413 buscaba respuestas de APIRequestContext en eventos de `page`; se corrigió para usar `instance` de Spring y repetir las lecturas. [Diagnóstico inicial](evidencias-fase-2/2.B3-2026-10-16/diagnostico-inicial.json), [suplemento diagnóstico](evidencias-fase-2/2.B3-2026-10-16/suplemento-diagnostico.json), [persistencia diagnóstico](evidencias-fase-2/2.B3-2026-10-16/persistencia-diagnostico.json). Las salidas fallidas de fixtures no se cuentan como integración aprobada; los resultados posteriores identifican qué se repitió.

No se ejecutaron `check`, lint, Vitest, coverage ni builds completos de frontend: ya estaban aprobados y no hubo cambios de producción que justificaran repetirlos. **No hay cobertura de código nueva medida en 2.B3.** Se conservan exclusivamente como medición de la ejecución anterior: JaCoCo **2934/3155 líneas = 93,00 %**, V8 **1011/1099 = 91,99 %** instrumentadas (producción instrumentada sin helpers: 91,56 %). Los scripts E2E aportan cobertura de escenarios, no un porcentaje JaCoCo/V8 adicional.

Cambios de este turno: scripts de navegador, compose/semilla de pruebas aislados y este informe/evidencias. Los hashes de fuentes comparados con el cierre de la revalidación anterior coinciden ([fuentes](evidencias-fase-2/2.B3-2026-10-16/fuentes-actuales.csv), [diferencias vacías](evidencias-fase-2/2.B3-2026-10-16/diferencias-fuentes.csv)). Sin editar `.env.local`, fuentes productivas, migraciones ni pruebas aprobadas. Se mantienen decisiones provisionales, límites/formatos pendientes del cliente y demás pendientes funcionales del informe. Drive/Gmail y fases posteriores quedan fuera; «Copia a Drive pendiente» es el estado local previsto en fase 2.

**Sin bloqueo actual para los escenarios solicitados en este entorno. Sin commit, push, uso/borrado de bases o volúmenes compartidos ni cierre de fase. Richard valida el cierre.**

Entorno de prueba detenido al terminar mediante `docker compose -f .tmp/f2b3-20261016/compose.yaml stop`; se conservan los dos volúmenes propios y el compose para reproducción. [Estado de contenedores](evidencias-fase-2/2.B3-2026-10-16/estado-contenedores.json) y [registros finales](evidencias-fase-2/2.B3-2026-10-16/servicios-finales.log). PowerShell registró como NativeCommandError el progreso que Docker escribió en stderr; el log muestra Stopped para los tres servicios y la consulta posterior verifica su estado. No se ejecutó down ni eliminación de volúmenes.

## Consolidación para validar el cierre de desarrollo — 2026-10-16

**Dictamen: sí, la fase 2 está lista para que Richard valide su cierre en desarrollo, exclusivamente con datos ficticios y conservando las limitaciones siguientes. No se marca cerrada.** La evidencia satisface el criterio técnico del manual dentro del alcance A1–A4, incluido su avance a A5; no acredita operación de A5 en adelante, producción ni uso de datos reales. La aceptación de decisiones provisionales no se deduce del verde de las pruebas.

Esta sección consolida el estado vigente y prevalece sobre frases históricas como «no iniciado», «pendiente de revalidación» o «NO APROBADO» de ejecuciones anteriores. Esas frases y sus fallos se conservan para trazabilidad; no representan por sí solos hallazgos todavía abiertos.

### Vigencia y procedencia de los resultados

Revisión nueva de esta consolidación: lectura del criterio de salida del DOCX vigente, plan e informe; contraste de fuentes con el snapshot de 2.B3 y revisión de fechas de pruebas/configuración. **334 archivos con hashes coincidentes, cero cambiados y cero adicionales** bajo backend/src/main y frontend/src. En **396 archivos** revisados (fuentes, pruebas y configuraciones seleccionadas) no aparecen fechas posteriores al último `backend-check.log`. La comparación por fecha complementa los hashes: no se presenta como prueba criptográfica de configuración. [Registro de vigencia](evidencias-fase-2/consolidacion/vigencia.json). Rama y HEAD conservados: `fase-2-motor-flujo`, `990c35a9cdcfad562e302966d79b7fa35a602ee2`.

**No se detectaron cambios posteriores sin verificar que justificaran repetir suites. En este turno no se ejecutaron pruebas, builds ni servicios.** Los archivos ya modificados/sin seguimiento son el trabajo previo, no se interpretan como cambios nuevos solo por aparecer en `git status`.

| Paso | Evidencia consolidada | Procedencia y alcance |
|---|---|---|
| **2.B1 — motor y permisos** | Numeración única/no reutilizable, transiciones, rechazo y devoluciones, urgencias 4/12/24, responsable/admin, reasignación, plazos, reentradas, historial e invariantes. El primer resultado fue 245/246 con R4 en rojo; R4 se corrigió y revalidó, y la regresión final backend incluye los casos de dominio. | Se reutiliza la última suite aprobada, no se suma el número de pruebas de ejecuciones históricas. [Cierre R4 y regresión de entonces](evidencias-fase-2/r4-final/check.log), [última regresión completa](evidencias-fase-2/revalidacion-2.5-2026-10-16/backend-check.log). |
| **2.B2 — persistencia y contrato** | PostgreSQL Testcontainers aislado; V2 poblada→V3 conservando datos, CHECK/FK compuesta, contador protegido, concurrencia y rollback; endpoints, autorización vigente, archivos reales/corruptos/ausentes y cargas incompletas. JSON POST observaciones/GET historial idéntico; R4/R9 y bytecode verificados. | Último `check`: **484 PASS, 0 fallos/errores/omitidas**, incluido Backend24IT 41/41. [JUnit por clase](evidencias-fase-2/revalidacion-2.5-2026-10-16/backend-final/resultados.csv), [revalidación de D04–D07](evidencias-fase-2/revalidacion-2.4-2026-10-16/), [firmas y bytecode](evidencias-fase-2/revalidacion-2.5-2026-10-16/fronteras.txt). |
| **Frontend de 2.5** | lint, **219 PASS** y build aprobados; Vitest sin autoingreso; desbordamiento corregido y resumen como nota accesible. | Resultados reutilizados de la última revalidación. Sus primeros casos visuales tenían API simulada y **no** se cuentan como integración 2.B3. [Vitest](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-vitest.json), [build](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-build.log). |
| **2.B3 — integración real** | Login normal por siete perfiles, A1→A5, rechazo/reentrada, ambas devoluciones, urgencia, observaciones, reasignación y roles múltiples; 200/409 entre sesiones con un evento; descarga bajo CSP, 413 Spring/nginx, persistencia tras reinicio y escritorio/móvil táctil. | Evidencia **nueva del paso 2.B3 anterior a esta consolidación**, sin mocks: 16 comprobaciones principales PASS, dos casos de reasignación/conflicto PASS en suplemento, cuatro comprobaciones de auditoría PASS y dos de persistencia PASS. Hay solapamientos: no constituyen un total de casos únicos ni se suman a las 484 pruebas. [Principal](evidencias-fase-2/2.B3-2026-10-16/resultados.json), [sesiones](evidencias-fase-2/2.B3-2026-10-16/suplemento.json), [auditoría](evidencias-fase-2/2.B3-2026-10-16/auditoria.json), [persistencia](evidencias-fase-2/2.B3-2026-10-16/persistencia.json). |

El suplemento conserva un FAIL de selector de logout por codificación del arnés: **no se presenta todo ese archivo como verde**. El mismo comportamiento, bandeja/logout/recarga, se comprobó posteriormente en `auditoria.json` y pasó. Los diagnósticos iniciales del arnés no se convierten en defectos del producto ni se ocultan.

**Cobertura reutilizada, sin medición nueva en esta consolidación ni en 2.B3:**

| Medición de la última suite aprobada | Líneas | Ramas | Interpretación |
|---|---:|---:|---|
| Backend JaCoCo | **2934/3155 = 93,00 %** | 840/1123 = 74,80 % | Supera el umbral vigente de **80 % de líneas**. No afirma 80 % de ramas ni cobertura uniforme por clase. |
| Frontend V8 instrumentado | **1011/1099 = 91,99 %** | 847/1030 = 82,23 % | Incluye helpers; no es cobertura de todos los archivos del antiguo demo. Sin helpers: 933/1019 = 91,56 % de líneas de producción instrumentadas. Vitest no tiene puerta de cobertura configurada. |

[JaCoCo conservado](evidencias-fase-2/revalidacion-2.5-2026-10-16/backend-final/jacoco.xml), [V8 conservado](evidencias-fase-2/revalidacion-2.5-2026-10-16/frontend-coverage-summary.json). El recorrido de navegador acredita escenarios reales, no un incremento calculado de esos porcentajes.

### Criterios de salida del manual frente a la evidencia

Texto vigente: «Dominio, contrato, persistencia y recorrido A1 a A4 en verde; permisos e historial verificados y sin hallazgos altos de datos». Richard valida el cierre.

| Criterio | Resultado para desarrollo | Base verificable |
|---|---|---|
| Dominio en verde | **Cumplido** | Regresión final backend; invariantes, urgencias y reentradas, sin debilitar pruebas. |
| Contrato en verde | **Cumplido en los endpoints/escenarios ejercitados** | Backend24IT, JSON público POST/GET, errores contractuales, 413 de servlet y nginx. No equivale a una certificación exhaustiva de cada combinación OpenAPI. |
| Persistencia en verde | **Cumplido** | Migraciones y restricciones Testcontainers, pruebas de rollback y archivo corrupto; recuperación de datos y bytes tras reinicio en 2.B3. |
| Recorrido A1–A4 en verde | **Cumplido** | Navegador con API/DB/archivos reales, corrección tras rechazo y devolución urgente hasta volver a Análisis; también A4→A5. |
| Permisos verificados | **Cumplido con las reglas provisionales vigentes** | Estado/roles/asignación actuales en servidor, cuentas sin permiso, multirrol y descarga independiente de lectura. |
| Historial verificado | **Cumplido** | Inmutabilidad SQL, actor/rol/fecha, atomicidad y conteos derivados del guion; resúmenes reales P-20. |
| Sin hallazgos altos de datos | **Cumplido en el alcance revisado** | Defectos de corrupción, migración, concurrencia y contador cerrados por revalidación. No se conoce un alto abierto en ese alcance; no es una auditoría global de producción. |
| Validación de Richard | **PENDIENTE** | Este informe prepara la decisión; no la sustituye. |

El plan añade resolver las decisiones o aceptar sus valores de desarrollo. Varias siguen provisionales; por tanto, **«lista para validar» no significa que todas estén confirmadas por el cliente**. P-13 autoriza expresamente solo desarrollo con datos ficticios. La validación debe conservar ese límite y los riesgos enumerados a continuación.

### Hallazgos y riesgos abiertos; cierres que se conservan

**Cerrados por evidencia posterior:** 2.B1-D01/R4; 2.B2-D01/D02/D03; D04 migración poblada, D05 archivo corrupto, D06 bandeja 500, D07 413 sin código; defecto de resumen siempre nulo bajo P-20; F-2.5-01/02/03 y P20-R4-01. No se reabren por las frases históricas del informe. El resumen puede seguir siendo null cuando no hay cierre reconstruible: es el comportamiento aprobado provisionalmente, no el defecto antiguo.

| Pendiente abierto | Estado e impacto | Condición siguiente |
|---|---|---|
| **P-11, acción antigua tras ciclo de reentrada** | Riesgo contractual conocido: `etapaEsperada` distingue una etapa diferente, pero no A3→A2→A3. El bloqueo pesimista evita el doble avance concurrente probado; **no demuestra protección contra ese ciclo completo**. Archivos y observaciones sí llevan identidad de entrada, lo que no resuelve por sí solo todas las acciones antiguas. | Decidir contrato de versión/entrada para acciones y su prueba específica, o aceptar expresamente esta limitación en desarrollo. No está cerrado ni se extrapola el 200/409 a ese escenario. |
| Bundle JS grande | Aviso previo, pendiente técnico menor; fragmento de 760,94 kB en el último build. No impidió los recorridos. | Optimización futura autorizada y medición; no se realizó aquí. |
| Migraciones compartidas P-16 | Coordinación de equipo pendiente en el plan. No invalida la prueba aislada ni demuestra que una base persistente esté migrada. | Confirmar propiedad/reservas antes de integrar cambios de esquema de otras ramas. No editar migraciones ya aplicadas. |
| Huérfanos tras caída del proceso, copia Drive y reintentos | Trabajo de fase 3; persistencia local ya probada no sustituye reconciliación ante caída abrupta. | Implementar/verificar en su fase; no se amplía el cierre de fase 2. |
| Pendientes heredados de fase 1 | **Medios:** M1 límite por IP, M2 versionado de roles, M3 contraseña temporal compartida. **Bajos:** B1 último admin, B2 eventos de seguridad, B3 aud/jti y tolerancia temporal, B4 HSTS. No se corrigieron ni se aceptaron para producción en fase 2. | Mantener [cierre de desarrollo y backlog de fase 1](fase-1-informe.md). M3 debe resolverse antes de usar cuentas reales; HSTS requiere HTTPS remoto. |

**Bloqueos:** ninguno técnico activo detectado para repetir el recorrido de desarrollo con datos ficticios. El cierre administrativo sigue esperando a Richard. El uso de datos reales/producción sigue sin autorización y requiere confirmar P-13, resolver provisión de cuentas reales y verificar el entorno remoto. Acceso desde China, Drive desde esa red, HTTPS/HSTS, dispositivos físicos y auditoría WCAG completa **no están acreditados por localhost**; son límites/verificaciones futuras, no éxitos implícitos.

### Decisiones provisionales o de cliente aún pendientes

1. **P-03:** formato de numeración y separación país/año; regla técnica vigente de no reutilizar confirmados, permitir huecos y no truncar al superar 9999.
2. **P-07:** reasignación entre roles; vigente solo persona activa con el rol responsable. **P-06** está aprobado por el equipo y probado, sin convertirlo en confirmación del cliente.
3. **P-08/P-09/P-10:** urgencias hábiles o corridas, jornada/feriados y mantenimiento del vencimiento tras rechazo. Valores usados: horas hábiles, lunes–viernes completos en Ecuador sin feriados y conservación del vencimiento original.
4. **P-13:** visibilidad global para usuarios activos con rol; aprobación provisional solo para desarrollo ficticio. Debe confirmarse antes de datos reales.
5. **P-17:** descargas por admin, actor de etapa y revisor explícito; A3 revisa A2. Sigue pendiente si A4 debe descargar documentos de A2; actualmente no hereda ese permiso. Acceso real a Drive desde China no verificado.
6. **P-18/P-19:** documentos válidos solo de la entrada actual; observaciones solo por quien puede actuar o admin, sin avance ni reinicio de plazos. Reglas provisionales comprobadas, pendientes del cliente.
7. **P-20:** contenido del resumen de la última entrada cerrada, nombre vigente o identificador, rol histórico, fecha Ecuador, recorte y «Entrada anterior». Implementación y presentación verificadas; decisión de contenido todavía provisional.
8. **Archivos:** formatos/plantillas aceptados y precisión del límite. Valor técnico vigente **100 MiB por archivo**, no confirmación definitiva de la ambigüedad «100 MB» del cliente (contradicción 7). 2.B2 probó límite real de disco; 2.B3 probó las dos capas 413 con configuración exclusiva de 1 MiB/2 MiB, sin volver a medir 100 MiB a través de nginx. **P-05 opción A** sí es decisión técnica aprobada del equipo; no reabre la elección de almacenamiento local de fase 2.
9. **Fuera de la operativa de fase 2:** P-14 (quién crea B, fase 6), P-12 (Fin, fase 4), plazos de Fabricación/Incidencias, tipo del tránsito de 45 días, documentos de Embarque/Bodega/Incidencias y recálculo aduanero. Conservan PENDIENTE y no se interpretan como ausencia de requisitos. No bloquean A1–A4, pero sí condicionan sus futuras fases.

P-16 y P-11 son decisiones de coordinación/contrato del equipo, separadas de preguntas al cliente. P-01/P-02/P-04/P-05/P-06/P-15 ya tienen resolución o aprobación técnica documentada; esta consolidación no las devuelve a pendiente ni convierte propuestas de las demás filas en respuestas confirmadas.

**Entrega para la decisión de Richard:** evidencia suficiente para validar el cierre **de desarrollo** en el alcance probado, conservando explícitamente P-11, los pendientes anteriores y la restricción a datos ficticios. No hay hallazgos altos abiertos identificados dentro de ese alcance. **Fase 2 permanece ABIERTA y PENDIENTE DE VALIDACIÓN DE CIERRE POR RICHARD.** En este turno solo se actualizan el informe y el registro de vigencia; sin cambios de producción, commit, push, merge ni operaciones sobre bases/volúmenes.

## Validación de cierre de desarrollo por Richard — 2026-10-16

Richard validó expresamente: «Valido el cierre de la fase 2 en desarrollo con datos ficticios, conservando P-11, las decisiones provisionales y el backlog documentado. No autorizo producción».

**Fase 2 CERRADA EN DESARROLLO, exclusivamente con datos ficticios. PRODUCCIÓN NO AUTORIZADA.** Esta validación sustituye el estado de cierre pendiente de la consolidación anterior; no modifica sus evidencias ni convierte los pendientes en corregidos o confirmados por el cliente.

Se conserva la base de 2.B1/2.B2/2.B3 resumida en la consolidación: última regresión backend de 484 PASS y 93,00 % de líneas; frontend lint, 219 PASS, build y 91,99 % de líneas instrumentadas; recorrido real de navegador con API, PostgreSQL, archivos y nginx aislados. Son resultados ya registrados, sin nuevas suites ni nueva medición de cobertura para este acto.

**Siguen abiertos y con su destino documentado:** P-11, P-16, decisiones provisionales, bundle grande, backlog heredado de fase 1 y verificaciones de fases posteriores. La validación no autoriza producción, datos reales, commit, push, merge ni el inicio de otra fase. Se actualizan únicamente informe, plan y PRD; Richard preparará el commit manual.
