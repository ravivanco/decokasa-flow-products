# Fase 7 — informe de verificación

## 7.B1 Colombia y aislamiento — 2026-10-24

**Resultado parcial y bloqueado. No aprobado como recorrido completo.** Rama comprobada: `fase-7-colombia-ia`. Se conserva producción, Header.tsx, cambios ajenos e índice Git. Se ejecuta únicamente 7.B1; no se avanza a 7.B2 ni se cierra ninguna fase.

Fuentes: manual DOCX vigente ([extracto fase 7](fase-7-evidencias/7.B1/manual-fase-7.txt)), [plan](../../.claude/PRPs/plans/fase-7-colombia-ia.plan.md), [ADR-0024](../adr/0024-acceso-por-pais.md), requisitos y contrato vigente. Skills consultadas: `ecc:springboot-tdd`, `playwright-best-practices` (aislamiento, esperas por condiciones y contextos separados), `agregar-flujo-decokasa` (definiciones y traspaso a Codex). La prohibición de pruebas de esa skill corresponde a la parte A; esta solicitud autoriza expresamente la parte B. También se aplica revisión Java a los cambios de pruebas y se consulta `claude-api` para no inferir firmas del SDK.

[Resultados consolidados y conservación](fase-7-evidencias/7.B1/resultados.json). No son evidencia nueva los porcentajes o recorridos aprobados de fases anteriores.

### Decisiones y aislamiento

- Richard aprobó para esta implementación CO con los mismos flujos A/B y roles de EC, bodega temporal «Bodega 1» con ciudad pendiente y calendarios separados con las mismas reglas provisionales. No se da por resuelta la diferencia con los «procesos distintos» del documento del cliente.
- ADR-0024 exige países asignados **hoy** en la base, sin excepción automática para admin. Pedido de país no asignado: 404; creación/filtro explícito no asignado: 403 antes de numerar. Es la expectativa a revalidar por endpoint, no una autorización ya comprobada en esta ejecución.
- PostgreSQL 17 por Testcontainers, sin reuse ni bases compartidas. Fixtures SQL ficticios por esquema UUID o transacción que se revierte; sin población masiva, sin usuarios reales ni asignaciones de Colombia a cuentas reales. El fixture nuevo de migraciones inserta como máximo un pedido por esquema. Las pruebas SQL históricas mantienen sus datos pequeños y aislados; no se inicia el arnés de 500 pedidos ni el recorrido con una población superior a 20.
- El arnés nuevo es **solo SQL**: no carga Spring ni clases viejas de `main`, no monta una API sustituta y no usa almacenamiento real compartido. Los adjuntos de conservación son metadatos ficticios con clave local ficticia; **no acreditan lectura/integridad de bytes ni aislamiento del almacenamiento de una API**.
- No se inicia una aplicación backend ni se activa IA, OAuth, workers o schedulers. Google y Claude API: cero llamadas; no hay compras. Se descargaron dependencias Maven y un validador OpenAPI en entorno temporal, no datos ni llamadas a proveedores de IA.

### Resumen de resultados

| Comprobación | Resultado | Alcance y límites |
|---|---|---|
| `gradlew check --offline` inicial | Falla resolviendo SDK no cacheado | Bloqueo de caché, no demuestra defecto del SDK ni de código. |
| `gradlew check` con resolución habilitada | **Falla compileJava: 9 errores** | F7-B1-H01. No llega a tests, cobertura ni check completo aprobado. Incluye los módulos nuevos en el intento de compilación. |
| Migraciones/regresión SQL aislada final | **110 casos, 108 aprobados, 2 fallidos, 0 errores, 0 omitidos** | Los dos fallos corresponden a la misma discrepancia F7-B1-H02. No es el check backend. |
| OpenAPI | **Válido**, OpenAPI 3.1.0, contrato 0.19.0-borrador | Validación formal y resolución de referencias locales; no comparación contra un servidor arrancado. |
| Frontend inicial | Lint/build 0; **325/329** tests | Cuatro expectativas antiguas afectadas por el país, adaptadas manteniendo controles. |
| Frontend dirigido final | **31/31** | Incluye seis casos nuevos de país, catálogo y auditoría de persistencia. |
| Frontend final completo | **335/335**, 27 archivos; lint 0, test 0, build 0 | Componentes/rutas reales con MSW; no equivale a API real ni a Playwright en navegador. |
| Recorridos HTTP/E2E CO A/B y EC A/B | **Bloqueados** | No se usa backend viejo ni se sustituye la API real por respuestas simuladas para declararlos aprobados. |

Logs: [check inicial](fase-7-evidencias/7.B1/backend-baseline.log), [check con dependencias](fase-7-evidencias/7.B1/backend-online.log), [salida backend](fase-7-evidencias/7.B1/backend-online-exit.json), [regresión SQL final](fase-7-evidencias/7.B1/migraciones-regresion-02.log), [JUnit SQL final](fase-7-evidencias/7.B1/migraciones-junit), [OpenAPI](fase-7-evidencias/7.B1/openapi-validacion.json), [lint frontend](fase-7-evidencias/7.B1/frontend-lint-final.log), [tests frontend](fase-7-evidencias/7.B1/frontend-test-final.log), [build frontend](fase-7-evidencias/7.B1/frontend-build-final.log), [salidas frontend](fase-7-evidencias/7.B1/frontend-final-exit.json).

### F7-B1-H01 — Alta, abierto: colisión de Resultado bloquea la compilación de producción

**Reproductor**, desde `backend`:

```powershell
.\gradlew.bat check --console=plain --no-daemon
```

Tras resolver `com.anthropic:anthropic-java:3.1.0`, `compileJava` falla en `asistente/application/usecase/PreguntarCasoDeUso.java`. La clase implementa `Preguntar`, que declara un record miembro `Preguntar.Resultado`; también importa el enum `RegistroConsultasIA.Resultado`. Los usos simples de `Resultado` dentro de esa clase resuelven al miembro heredado de `Preguntar` y no al enum esperado.

Evidencia del compilador: no encuentra `ERROR_INCIERTO`, `ERROR_SIN_COBRO`, `LIMITE`, `RECHAZADA`, `INCOMPLETA`, `RESPONDIDA` ni `SIN_EVIDENCIA` (líneas 153, 162, 226, 412–414); en la línea 406 informa que `Preguntar.Resultado` no se puede convertir a `RegistroConsultasIA.Resultado`. Son **nueve diagnósticos de la misma causa**, no nueve defectos independientes.

Propuesta para Claude: desambiguar los tipos del resultado de respuesta y del registro, por ejemplo cualificando el enum en los puntos de registro y clasificación, conservando la firma del puerto de entrada. No cambiar el contrato ni alterar pruebas para ocultarlo. **No se modifica producción aquí.**

No es correcto seguir describiéndolo como bloqueo por «SDK no disponible»: esa condición inicial se superó descargando dependencias. No hay diagnóstico de firma SDK en el pase con dependencias resueltas, pero la compilación global tampoco aprobó. No se certifica el adaptador IA ni se prueba su comportamiento: pertenece a 7.B2, con IA desactivada en este paso.

Impacto: impide arrancar la API actual, compilar sus tests normales, medir JaCoCo actual y ejecutar recorridos reales de Colombia o regresión completa de Ecuador. No se excluye el módulo nuevo, no se introduce un stub en producción ni se usa un build viejo para sortearlo.

### F7-B1-H02 — Media, abierto: Colombia conserva el indicador de país inactivo

Con las migraciones V1–V21 sobre una base vacía, SQL real devuelve:

```sql
select codigo, activo from pais order by codigo;
-- CO | false
-- EC | true
select pais_codigo, tipo_flujo, version, vigente
from definicion_flujo where pais_codigo = 'CO' order by tipo_flujo;
-- CO | A | 1 | true
-- CO | B | 1 | true
```

V1 siembra Colombia inactiva y V19 crea definiciones/bodega, pero no cambia `pais.activo`. El manual 7.2 pide país habilitado. `MigracionesIT.siembraEcuadorYColombiaActivosSegunFase7` y `MigracionesFase7IT.colombiaDebeQuedarHabilitadaSinAsignarlaAutomaticamente` fallan por ese mismo valor; los demás controles de migración pasan.

**Impacto acreditado:** inconsistencia entre catálogo persistente y habilitación documentada. No se acredita aquí que bloquee crear pedidos, ni que permita acceder sin permiso: el código de autorización revisado usa asignaciones `usuario_pais`, y la API no ha arrancado. No se confunde `pais.activo` con la cuenta activa, los roles vigentes o los países asignados.

Propuesta para Claude: reconciliar explícitamente el significado del indicador con la habilitación aprobada y corregir el dato con la estrategia de migración apropiada (si ya se aplicó, corrección hacia delante, sin reescribir historia). **No** asignar CO automáticamente a usuarios ni conceder excepción admin. No rebajar la expectativa a false para obtener verde. Si el indicador se considera deliberadamente histórico, se necesita documentar y decidir esa semántica, no inferirla de que hoy no se consulte.

### Migraciones y adaptación de pruebas históricas

Nuevo `backend/src/test/java/com/decokasa/seguimiento/archivos/MigracionesFase7IT.java` ([copia revisable](fase-7-evidencias/7.B1/MigracionesFase7IT.java)):

- Base vacía con V1–V21 exactas, checksum/validate de Flyway y segunda aplicación sin cambios; IA configurada apagada, sin usuarios/pedidos sembrados ni asignación automática de países.
- Actualización desde V18, V19 y V20 con un pedido EC, entrada, archivo y evento ficticios: conserva filas, timestamps, país/asignaciones previas, definición EC y columnas anteriores de la bodega; historial sigue rechazando modificación.
- Compara **todas las columnas de reglas relevantes** de etapas, transiciones y documentos CO/EC, salvo «Aduana Ecuador»→«Aduana Colombia»; once etapas A y doce B, sin inventar plazos pendientes.
- Rechaza asignación SQL de bodega CO a pedido EC sin cambiar su bodega; admite mismo nombre en distinto país y rechaza duplicado normalizado dentro de uno.
- El contador persistente separa país/año y revierte una reserva transaccional. **Es evidencia del almacenamiento de la numeración, no del permiso ni del formato de una creación HTTP**, pendientes por H01.

Adaptadas `MigracionesIT`, `V2RestriccionesIT`, `MigracionesFase4IT`, `MigracionesFase5IT` y `MigracionesFase6IT`: expectativas V21, nueve tablas IA adicionales explícitas (sin dejar de rechazar tablas inesperadas), conteos/siembras EC acotados a su país y conservación EC durante cada migración nueva. El fixture que intentaba un nombre de bodega duplicado incorpora `pais_codigo='EC'` para seguir comprobando **23505**, en vez de fallar antes con 23502 por país nulo. `MigracionesV16IT` conserva sus targets históricos y comprobaciones.

[Arnés Gradle SQL](fase-7-evidencias/7.B1/migraciones.init.gradle) compila únicamente estas suites y `DatosB1` con bibliotecas de tests; agrega recursos de migración del producto. **No incluye el output de main** ni la aplicación Spring. Es una verificación limitada permitida por la independencia del SQL, no una exclusión para declarar aprobado el check ni para medir una cobertura artificial.

Intentos conservados: [SQL inicial offline](fase-7-evidencias/7.B1/migraciones-01.log), faltaban JAR runtime; [SQL dirigido con dependencias](fase-7-evidencias/7.B1/migraciones-02.log), aprobado; [regresión ampliada inicial](fase-7-evidencias/7.B1/migraciones-regresion-01.log), con expectativas/fixture antiguos y el país inactivo; [JUnit de ese intento](fase-7-evidencias/7.B1/migraciones-regresion-01-junit); [regresión final](fase-7-evidencias/7.B1/migraciones-regresion-02.log), 108/110, solo dos aserciones de H02 pendientes. No se borran ni maquillan fallos previos.

### Frontend: comportamiento probado y límites

Nuevo `frontend/src/pais/PaisFase7.test.tsx` ([copia final](fase-7-evidencias/7.B1/PaisFase7.test.tsx)) monta sesión, rutas, QueryClient y proveedor reales con respuestas MSW **ficticias y explícitas**. Seis casos:

1. Un país elegido automáticamente, sin ofrecer otros y sin persistir credenciales. La elección implícita inicial no escribe sessionStorage; una elección/cambio explícito guarda solo código EC/CO.
2. Sin países: aviso y ausencia de formulario/opciones no asignadas.
3. Con EC/CO: exige elegir; creación envía CO; un 403 conserva el borrador y no inventa una creación exitosa.
4. «Seguir en Colombia» conserva cambios; «Descartar y cambiar» remonta EC sin trasladar el borrador y guarda solo el código del país.
5. Lectura EC en curso: cambio a CO aborta la señal del fetch real del cliente, elimina las claves `pais:EC` y una respuesta liberada tarde no reaparece en lista/caché.
6. Tras 403 por revocación: relee `/auth/yo`, retira vista/caché CO, pasa al único país restante EC y conserva la sesión. No se usan minutos de espera real: se provoca el error autorizado por el diseño para la recuperación inmediata.

La prueba observa el AbortSignal pasado a fetch, porque el Request clonado de MSW no propagaba ese evento de forma fiable; conserva la comprobación de ausencia del dato tardío. Textos iniciales del test se corrigieron al texto real «Todavía no hay pedidos». Se corrigió también la expectativa de persistir automáticamente EC, que el diseño no prometía. Fueron errores del montaje nuevo, no defectos de producción ni eliminación de controles de aislamiento. [Dirigido inicial](fase-7-evidencias/7.B1/frontend-dirigido-01.log), [segundo](fase-7-evidencias/7.B1/frontend-dirigido-02.log), [final 31/31](fase-7-evidencias/7.B1/frontend-dirigido-03.log).

Cuatro fallos históricos: tres comparaciones de alta de bodega debían incluir `pais:'EC'`; la auditoría `sin-demo.test.ts` debía reconocer la persistencia de selección de país. Se mantienen las comprobaciones de prohibición de datos de demo/localStorage y las excepciones quedan limitadas a cargas reanudables y proveedor de país, con prueba del valor real persistido. No se relajan permisos ni aserciones de conflictos o invalidación de consultas.

**No se acredita con estas pruebas** autorización real del servidor, polling HTTP de 30 s en dos sesiones, recorrido A/B con API real ni teclado/descargas de un navegador Playwright. No se presenta jsdom/MSW como esa integración. Las listas/tableros continúan con el límite conocido de definición vigente frente a versiones distintas; no se resuelve en 7.B1.

### Cobertura actual

Frontend global, sin nuevas exclusiones ni reducir umbrales: **1582/2340 líneas = 67,60 %**; sentencias 1746/2592 = 67,36 %, ramas 1560/2524 = 61,80 %, funciones 580/832 = 69,71 %. [Resumen JSON](fase-7-evidencias/7.B1/cobertura-final/coverage-summary.json). El objetivo adicional frontend de 80 % sigue pendiente; no se persigue demo desconectado.

**Backend global y módulos: no medibles actualmente** por H01 antes de ejecutar tests. El XML JaCoCo que pudiera quedar en `backend/build` de una ejecución anterior no representa esta rama y no se utiliza como cobertura vigente. Las 110 pruebas SQL no generan cobertura del motor ni sustituyen el mínimo backend 80 %. No atribuir a fase 7 el 91,30 % histórico de fase 6.

### Matriz pendiente de integración real por H01

Tras la corrección de producción por Claude, retomar en PostgreSQL/almacenamiento/usuarios ficticios nuevos, máximo 20 pedidos, IA y proveedores desactivados:

| Alcance | Comprobación requerida | Estado actual |
|---|---|---|
| Creación/numeración | CO A y B con roles actuales; admin sin Compras China no crea B; país no asignado 403 sin consumir numeración; EC y CO independientes. | Contador SQL parcial aprobado; API bloqueada. |
| Consulta y modificación | Listas/bandejas filtran antes de paginar; detalle/número/acción/observación/reasignación/motivo de otro país 404, también admin; revocación se aplica a siguiente petición. | Revisión estática solamente; no aprobado HTTP. |
| Archivos e historial | Inicio/finalización de carga, listado, contenido, Drive/link e historial de país ajeno 404, sin leer bytes ni cambiar estado; permisos tras revocación y reentrada. | Bloqueado, sin almacenamiento de API verificado. |
| Cronograma/calendario | País del pedido, pendientes sin estimar, Guayaquil/Bogotá, fechas, demora/llegada y prioridad de llegada real. | Reglas clonadas SQL; cálculo/endpoints pendientes. |
| Bodegas | Catálogo/edición por países actuales, admin sin excepción; bodega del país del pedido, ciudad pendiente y duplicados por país. | Restricciones SQL aprobadas; permisos HTTP pendientes. |
| Correos/copias | Admin autorizado, filas y totales de países actuales; un filtro ajeno responde 403 y no expone registros, contenido ni enlace; Google no se llama. | Código muestra filtros SQL y autorización; integración bloqueada. |
| Selector/caché/borradores | Dos sesiones reales, cancelación y respuestas tardías de API, borrador de pedido/observación/bodega, cambios de etapa y permisos retirados. | Seis casos simulados aprobados; no se generaliza a todos los formularios ni servidor. |
| CO y EC completos | A y B hasta Fin, rechazo/devolución/urgencia, documentos reales ficticios, demora, llegada, bodega, historial/outbox y bloqueo de acciones manuales en Fin. | **No ejecutados** por H01; fixtures no los sustituyen. |

La lectura estática encuentra `ApoyoPedidos.exigirPais` por caso de uso y filtros de países en repositorios; archivos autoriza antes de abrir contenido y normaliza el 404. No equivale a probar ausencia de exposición de todos los endpoints. ADR-0024 mantiene sus detalles de diseño pendientes de esa revisión dinámica; no se marca completamente verificado.

### Comandos y entorno

Desde `backend`:

```powershell
.\gradlew.bat check --offline --console=plain --no-daemon
.\gradlew.bat check --console=plain --no-daemon
.\gradlew.bat -I ../documentos/pruebas/fase-7-evidencias/7.B1/migraciones.init.gradle migracionesF7 --console=plain --no-daemon
.\gradlew.bat -I ../documentos/pruebas/fase-7-evidencias/7.B1/migraciones.init.gradle migracionesF7 --offline --console=plain --no-daemon
```

Desde `frontend`:

```powershell
npm.cmd run lint
npm.cmd run test -- src/pais/PaisFase7.test.tsx src/components/pedidos/Fase4Pantallas.test.tsx src/test/sin-demo.test.ts --maxWorkers=1
npm.cmd run coverage -- --maxWorkers=1 '--coverage.include=src/**/*.{ts,tsx}' --coverage.reporter=json-summary --coverage.reporter=text --coverage.reportsDirectory=../documentos/pruebas/fase-7-evidencias/7.B1/cobertura-final
npm.cmd run build -- --outDir ../documentos/pruebas/fase-7-evidencias/7.B1/.tmp/build-final
```

OpenAPI: `openapi-spec-validator` instalado en `documentos/.tmp/f7-openapi`, entorno virtual aislado sin cambios de dependencias de producto; `read_from_filename` y `validate(..., base_uri=...)` sobre `documentos/api/openapi.yaml`. [Resultado](fase-7-evidencias/7.B1/openapi-validacion.json), [instalación](fase-7-evidencias/7.B1/openapi-instalacion.log). Descarga de dependencias no equivale a activar la IA.

Avisos conservados: Vite `__dirname`/carga futura de configuración, bundle mayor de 500 kB, anotaciones API Guardian en classpath independiente de pruebas SQL. Se capturan códigos nativos `$LASTEXITCODE` en JSON para no confundir avisos stderr de PowerShell con el resultado. No se corrigen configuraciones de producción para silenciarlos.

### Conservación y salida

[Snapshot inicial](fase-7-evidencias/7.B1/conservacion-inicial.json) y [comparación final](fase-7-evidencias/7.B1/resultados.json): **533 archivos de producción comparados sin cambios**, Header.tsx intacto, índice Git intacto. Se modifican exclusivamente tests, infraestructura de verificación y este informe/evidencias. Los scripts transitorios de ajuste y `.tmp` no forman parte de una futura selección automática; no se preparó ningún índice.

Testcontainers termina sus contenedores propios al acabar las tareas, sin borrado manual de volúmenes ni manipulación de recursos ajenos. No se inició servicio HTTP. Se conservan las evidencias; no se establece política de retención de archivos. Comprobación acotada de patrones de secretos en evidencias públicas: sin coincidencias; no es un pentest ni un escaneo integral de credenciales.

**Bloqueos abiertos:** H01 de compilación, H02 de catálogo CO, integración real de permisos/recorridos/almacenamiento aún no ejecutada, cobertura backend actual, objetivo adicional frontend 80 %, límite de listas/tableros por versión, meta de días y demás pendientes genuinos. **IA desactivada; sin 7.B2, Google/Claude API real ni compras; 3.B3 pospuesto; todas las fases abiertas. Richard decide cualquier cierre.**

## Checkpoint para continuar en otro PC (2026-10-24)

Por solicitud de Richard se prepara únicamente el índice del avance completo de fase 7. **No se corrige código ni se repiten pruebas.** [Contenido, exclusiones y procedimiento de continuación](fase-7-checkpoint.md), [selección normal](fase-7-seleccion-normal.txt), [logs revisados ignorados](fase-7-seleccion-ignorados.txt) y [auditoría](fase-7-checkpoint-auditoria.json).

**El checkpoint no está aprobado:** se guardan tanto implementación como pruebas y evidencias fallidas. 7.B1 continúa bloqueado por los tipos Resultado y Colombia inactiva; API real y cobertura backend pendientes. Se conservan los resultados parciales 335/335 frontend, 67,60 % de líneas y 108/110 SQL, sin atribuirles una ejecución nueva.

**Header.tsx se conserva aparte y no viajará con este commit.** No se descarta ni se prepara. Las pruebas frontend existentes usaron el Header local; el otro PC recibirá el Header ya confirmado, no esta modificación excluida. No se transfieren credenciales, entornos privados, builds ni bytes de almacenamiento. El índice se compara con la selección explícita; los avisos de whitespace se informan sin alterar evidencias originales. No se realiza commit, push ni merge ni se cierra fase alguna.

Preparación del checkpoint: **220 rutas** (204 normales y 16 logs revisados preparados con `-f`), igualdad exacta entre selección e índice. Sin coincidencias de patrones de secretos en archivos seleccionados ni en su contenido preparado. `git diff --cached --stat`: salida 0. `git diff --cached --check`: salida **2, doce avisos de espacios finales** en plan/XML, conservados. También hay avisos LF→CRLF. Los detalles y la primera captura quedan en la auditoría; no se modifica producción ni evidencia original para limpiarlos.
