# Fase 6 — informe de pruebas

## Alcance: únicamente 6.B1 — 21 de octubre de 2026

Rama comprobada: `fase-6-flujo-b`. Se conservan todos los cambios anteriores, incluido Header.tsx. No se modifica producción, la configuración de cobertura ni el arnés de Google. Sin escrituras Git, llamadas Google, bases compartidas o borrado de volúmenes. 6.B2 y 6.B3 no se ejecutan; ninguna fase se cierra. 3.B3 continúa pospuesto.

Fuentes leídas: [manual vigente](../manual-paso-a-paso.docx), [extracto de fase 6](fase-6-evidencias/6.B1/manual-fase-6.txt), [plan](../../.claude/PRPs/plans/fase-6-flujo-b.plan.md), [OpenAPI](../api/openapi.yaml), [decisiones](../requerimientos.md). Se aplican `ecc:tdd-workflow`, `ecc:springboot-tdd` y revisión `ecc:java-reviewer` a las pruebas Java. La prohibición expresa de Git y cambios de producción prevalece sobre los checkpoints y la implementación del ciclo TDD: se conserva evidencia de fallos y revalidación, sin introducir un defecto artificial ni corregir producción.

## Decisiones que se comprueban

- F6-D1, Richard: Solicitud en B1, Documentos codificados en B3, Packing List en B7 (BL opcional), bodega activa y algún documento en B10, algún documento en B11. Solo acreditan documentos del pedido, etapa y entrada actual; se permiten adicionales.
- F6-D2: crea quien tiene vigente el rol de la primera etapa. B requiere compras_china; admin sin ese rol recibe 403 antes de consumir numeración. Un admin con compras_china crea por ese rol. A conserva creación por admin. P-14 del cliente sigue pendiente.
- F6-D3: habilitación por definición; A1–A10 y B1–B11 manuales, A11/B12 automáticas. Una definición con transición desde una etapa automática es inválida.
- V18/F6-D4, aprobado por Richard: B2 revisa B1 y B4 revisa B3. Descarga con P-06/P-17: cuenta y roles vigentes, asignación, varios roles y excepción admin. B5 no gana revisión de B3.
- Plazos de B3/B4/B9/B10: 24 horas hábiles; B5: tres días hábiles; B7: diez días hábiles. B1 y B12 sin plazo. **B2/B6/B8/B11 PENDIENTE**, sin convertirlos a cero ni inventar vencimientos.
- Urgencias: 4/12/24 horas y final al regresar a B5; el cómputo hábil y conservación ante rechazo durante urgencia mantienen su carácter provisional documentado. P-10 no queda aprobado por superar un test. La meta de días continúa pendiente; 90 días sigue propuesta configurable.

## Entorno y aislamiento

Java 21 / Gradle del repositorio, Spring Boot 4.1.1, PostgreSQL 17 Testcontainers sin reuse ni conexión a una base compartida. Los tests de migración usan esquemas UUID dentro de su propio contenedor; no ejecutan `clean`. Las pruebas de aplicación importan `TestcontainersConfiguration` y usan Spring/MockMvc, JPA y almacenamiento local reales de prueba, con reloj primario exclusivo fijado en `2026-10-21T15:00:00Z` y avance controlado de un minuto. No hay esperas de minutos ni navegador, ni servidor HTTP externo: esto es integración backend, no el recorrido Playwright de 6.B2.

`Fase6IT` crea todos sus pedidos mediante el controlador de creación y avanza mediante acciones reales; SQL se usa para usuarios ficticios, observación del estado y fallos de escritura delimitados por pedido UUID. No se altera la definición vigente para desbloquear requisitos. La migración usa fixtures SQL explícitos de datos anteriores de A, necesarios para comprobar conservación, sin presentarlos como recorrido funcional. Archivos pequeños ficticios en un directorio temporal exclusivo; no se suben a Google. Los temporales de estas pruebas no son evidencias que deban incorporarse a Git.

Variables de cada ejecución, antes de invocar Gradle desde `backend`:

```powershell
$env:GOOGLE_OAUTH_HABILITADA='false'
$env:GOOGLE_DRIVE_HABILITADA='false'
$env:GOOGLE_GMAIL_HABILITADA='false'
$env:COPIA_DRIVE_AUTOMATICA='false'
$env:CORREO_AUTOMATICO='false'
$env:VENCIMIENTOS_AUTOMATICO='false'
$env:SPRING_CONFIG_IMPORT=''
$env:SEMILLA_HABILITADA='false'
$env:ALMACEN_LOCAL_DIR=Join-Path $env:TEMP ('f6-b1-'+[guid]::NewGuid())
```

Además, el nuevo contexto Spring fija explícitamente todas las integraciones y automatismos desactivados. La propiedad dinámica de archivos apunta al directorio temporal de la suite. Los contenedores y contextos los cierra el ciclo de vida de Testcontainers/Spring; no se detienen recursos ajenos ni se borran volúmenes manualmente.

## Pruebas y matriz de evidencia

| Alcance | Prueba | Comprobaciones |
|---|---|---|
| V17/V18 | `MigracionesFase6IT` (2 casos) | V1–V18 en vacío; V16 con pedido A en A6, etapas, entradas, adjunto, evento y relación conservados al aplicar V17 y V18 por separado; definición A idéntica fila por fila; timestamps/versiones conservados; historial solo inserción; validación Flyway y no reaplicación |
| Definición B | misma suite + `Fase6IT.definicionPublicaDoceEtapasYContratosSinCambiarA` | Doce etapas, responsables, estados/cantidades/tipos de plazo, dieciséis transiciones y sus flags exactos, documentos tipados, algún documento en B10/B11, llegada solo B8 y revisiones B2/B4; contrato público A conserva once etapas |
| Creación y aislamiento | `crearBSinChinaVigenteNoConsumeNumeracion`, `crearBReleeCuentaYRolAunqueJwtConserveChina`, `rolesMultiplesCreanPorResponsableYNumeracionCompartidaSinCruzarPedidos` | Seis roles sin China → 403, contador y pedidos intactos; cuenta desactivada o rol retirado → 403; admin+China → 201 con rol de evento compras_china; números consecutivos A/B; archivo de A no acredita B; avance de B no cambia A |
| Retornos | `devolucionesCreanEntradaCorrectaYNoB2` (3 casos) | B2→B1, B4→B3 y B5→B3, observación obligatoria, entrada nueva, reentrada, rechazo del adjunto de entrada anterior y de falta de documento; B4 DEVUELTA al devolver B5 |
| Urgencia | `urgenciaConIdentidadEstableHastaB5YP10Provisional` (3 casos) | 4/12/24 horas; ID igual a entrada de activación, vencimiento exacto con reloj controlado; rechazo B4 conserva identidad y vencimiento; B5 limpia urgencia; nueva devolución obtiene nueva identidad |
| Descargas | `descargaRevisoraRespetaAsignacionRolesMultiplesYEstadoVigente` (2 casos) | B2/B1 y B4/B3: acceso sin asignación, varios roles, asignada/admin; deniega otros del rol tras asignación; revocación/desactivación deniegan pese al JWT; B5 no descarga B3 |
| B6–Fin | `desdeB6HastaFinConDocumentosDemoraLlegadaYBodega` | Observación B6; B7 vence tras diez días hábiles; sin Packing List/solo BL rechaza; B8 sin vencimiento inventado; demora con fecha estricta y ajuste sábado→lunes; llegada con hora servidor; bodega inactiva/documento ausente rechaza; algún documento en B10/B11; observación y subida B11; Fin automático, evento del sistema único y sin acciones disponibles/manuales/subidas/observaciones/motivo en B12 |
| Concurrencia y rollback | `concurrentesConfirmarB11SoloUnaVezYNoTocarA`, `falloDeHistorialOOutboxRevierteCierreB11Completo` (2 fallos inyectados) | Dos confirmaciones simultáneas → 200/409, un Fin y A intacto; fallo en evento u outbox revierte pedido, etapas, entradas, historial y notificaciones; después se puede confirmar sin perder el trabajo |
| Regresión A | `regresionCompletaDeAYFinA11NoSeHabilitaPorExistirB11` y suites existentes | A1–A11 con requisitos reales de V16 y B sin modificar; seis acciones manuales en A11 rechazadas, sin acciones disponibles, subidas, observaciones ni motivo; conservación de todo el estado ante errores |
| Habilitación | `HabilitacionFase6Test` (2 casos) | Sin concretar falla; A11/B12 excluidas y B11 incluida; compatibilidad con conjuntos fijos; definición rechaza transición desde Fin |

Nuevas suites: [Fase6IT](../../backend/src/test/java/com/decokasa/seguimiento/pedidos/Fase6IT.java), [MigracionesFase6IT](../../backend/src/test/java/com/decokasa/seguimiento/archivos/MigracionesFase6IT.java), [HabilitacionFase6Test](../../backend/src/test/java/com/decokasa/seguimiento/pedidos/domain/HabilitacionFase6Test.java). Son **27 casos nuevos** sin duplicar suites históricas. El check final mantiene la regresión existente, incluidas las fronteras de arquitectura.

### Adaptaciones históricas sin retirar garantías

Cinco suites existentes: `MigracionesIT`, `V2RestriccionesIT`, `MigracionesFase4IT`, `MigracionesFase5IT`, `MigracionesV16IT`. Las suites de esquema actual esperan 18 y conservan el listado exacto y comprobaciones de restricciones. Los conteos/filas de A se filtran por flujo; B se comprueba íntegramente en la suite nueva. V16 queda fijada explícitamente a target 16 para conservar su garantía histórica V15→V16 (una sola migración); las suites nuevas acreditan V17/V18. No se alteran pruebas de negocio anteriores para aceptar un comportamiento distinto de A.

## Ejecuciones

1. **Inicial:** `.\gradlew.bat check --offline --console=plain --no-daemon`. [Log](fase-6-evidencias/6.B1/backend-baseline.log), [salida 1](fase-6-evidencias/6.B1/backend-baseline-exit.json), [JUnit](fase-6-evidencias/6.B1/baseline-junit/). 810 casos reportados, siete fallos de expectativas antiguas. `V2RestriccionesIT` falló en inicialización al esperar 16, por lo que ese total no representa la suite completa de negocio. No se presenta este rojo como defecto nuevo de producción.
2. **Dirigida inicial:** `.\gradlew.bat test --tests '*Fase6IT' --tests '*HabilitacionFase6Test' --tests '*MigracionesIT' --tests '*V2RestriccionesIT' --tests '*MigracionesFase4IT' --tests '*MigracionesFase5IT' --tests '*MigracionesV16IT' --offline --console=plain --no-daemon`. [Log](fase-6-evidencias/6.B1/dirigido-01.log), [JUnit](fase-6-evidencias/6.B1/dirigido-01-junit/): 134 casos, seis fallos de montaje de pruebas nuevas: entrada interna de esquema sin versión en Flyway, nombre del campo `nivelUrgencia` y asignar la revisora antes de entrar en ella. Se corrigen únicamente los tests; no se cambia la expectativa de negocio ni se debilitan aserciones.
3. **Dirigida corregida:** `.\gradlew.bat test --tests '*Fase6IT' --tests '*HabilitacionFase6Test' --offline --console=plain --no-daemon`. [Log](fase-6-evidencias/6.B1/dirigido-02.log), [salida 0](fase-6-evidencias/6.B1/dirigido-02-exit.json), [JUnit](fase-6-evidencias/6.B1/dirigido-02-junit/): **34/34**, 27 nuevos más siete comprobaciones de arquitectura. BUILD SUCCESSFUL, 44 segundos.
4. **Check completo final:** `.\gradlew.bat check --offline --console=plain --no-daemon`. **878/878**, cero fallos, errores o saltos; salida **0**, `BUILD SUCCESSFUL in 4m 35s`. Incluye regresión A/B, restricciones PostgreSQL y fronteras de arquitectura. `jacocoTestCoverageVerification`, `check` y `jacocoTestReport` aprobaron. [Log final](fase-6-evidencias/6.B1/backend-check-final.log), [salida](fase-6-evidencias/6.B1/backend-check-final-exit.json), [JUnit final](fase-6-evidencias/6.B1/final-junit/), [JaCoCo XML](fase-6-evidencias/6.B1/final-jacoco.xml), [resultados y conservación](fase-6-evidencias/6.B1/resultados.json).

Se conservan los intentos iniciales y sus logs originales, incluidos los avisos de JVM CDS y sugerencia de caché Gradle. No se excluye código ni se reduce el mínimo backend de cobertura de líneas del 80 %.

### Cobertura real y conservación final

JaCoCo global: **5382/5895 líneas = 91,30 %**, superior al mínimo de líneas del 80 %. Instrucciones: 90,19 %; ramas: 72,84 %. No se afirma haber alcanzado el 80 % de ramas. Los valores por módulo, contadores y suites individuales están en `resultados.json`; no se mezclan con la cobertura frontend histórica ni con una ejecución dirigida.

| Módulo | Líneas cubiertas/total | Cobertura de líneas |
|---|---:|---:|
| Pedidos | 1341/1415 | 94,77 % |
| Flujos | 238/257 | 92,61 % |
| Archivos | 1391/1611 | 86,34 % |
| Plazos | 448/471 | 95,12 % |

Comparación SHA-256 contra el inicio: **backend de producción y todo frontend intactos, Header.tsx intacto, índice Git intacto**. Solo se modificaron las cinco suites históricas descritas y se añadieron las tres suites nuevas, además del informe y evidencias locales. Revisión Java de las pruebas: SQL parametrizado salvo nombres de tablas de una lista fija y triggers UUID de fallo ficticio; reloj y flags exclusivos del test, concurrencia acotada, sin secretos ni doble motor. No se hallaron riesgos críticos o altos en estos cambios de pruebas. La revisión de patrones de credenciales/tokens en las evidencias no detectó secretos. No se iniciaron workers reales, OAuth ni schedulers; los contextos/conexiones de prueba se cerraron al terminar.

## Defectos y pendientes

**No se acreditó un defecto nuevo de producción reproducible en 6.B1.** El check final aprobó la regresión completa. Los seis errores de montaje anteriores se conservaron y corrigieron únicamente en pruebas; no son defectos para corregir en producción. Los siete fallos iniciales de expectativas históricas también permanecen visibles y su adaptación se revalidó en el check completo.

Pendientes que estas pruebas no resuelven: plazo B2 y plazos B6/B8/B11; P-14 del cliente; reglas provisionales P-06/P-10/P-17 y cómputo de urgencia según sus fuentes; meta de días; objetivo adicional de cobertura frontend; diferencia con el objetivo histórico de 500 pedidos; capacidad física temporal frente a cuotas lógicas. La aprobación por Richard de documentos B/revisiones no se convierte en respuesta del cliente por pasar pruebas.

**Sin avance a 6.B2:** las pruebas de backend no acreditan bandejas, navegación, polling entre navegadores, pantallas ni el recorrido crítico Playwright conjunto A/B. 3.B3 real continúa pospuesto. Fases 3–6 abiertas.

## 6.B2 — recorridos críticos A/B y frontend — 21 de octubre de 2026

Esta sección amplía la evidencia de 6.B1; no modifica ni vuelve a ejecutar su check backend. **Resultado: recorridos críticos A y B aprobados juntos**, más control de roles múltiples y doble envío. Frontend: **329/329 pruebas**, lint y build aprobados. No se modifica producción ni se cierra la fase. [Resumen verificable](fase-6-evidencias/6.B2/resumen-final.json).

### Entorno real y límites

API del producto en `127.0.0.1:44285`, frontend compilado en `127.0.0.1:44284` y control de evidencia solo en loopback `44286`. PostgreSQL 17 Testcontainers propio, sin reuse ni base compartida; archivos ficticios en almacenamiento exclusivo del arnés. [Lanzador Gradle opt-in](fase-6-evidencias/6.B2/aislado.init.gradle), [infraestructura de prueba](fase-6-evidencias/6.B2/java/com/decokasa/b2/ArnesF6B2.java), [servidor estático/proxy](fase-6-evidencias/6.B2/servidor.mjs), [entorno inicial](fase-6-evidencias/6.B2/entorno.json).

Se parte de **cero pedidos**, todos se crean mediante la API o las pantallas reales. Una guarda exclusiva del ensayo con cerrojo transaccional impide superar **20 pedidos**. Se conservaron los intentos fallidos del test: el total final fue **12**, no 20 por ejecución ni una carga de 500. No se sembraron pedidos en etapas posteriores ni se modificaron transiciones, requisitos, plazos o permisos del producto. Los únicos fixtures SQL son usuarios ficticios, roles, pertenencia a Ecuador, la guarda del límite y revocaciones/restauraciones controladas de dos cuentas de prueba. Los flujos son V17/V18 reales; [definiciones obtenidas del servidor](fase-6-evidencias/6.B2/definiciones-reales.json).

Google: puertos Drive y correo sustituidos por dobles primarios que registrarían y rechazarían su invocación, con todas las integraciones y automatismos desactivados. **Cero invocaciones de esos dobles y cero operaciones Google reales.** No se emplean credenciales reales; la contraseña ficticia aleatoria se conservó solo temporalmente en `.tmp/acceso.json` ignorado y fue eliminada al parar. No hay OAuth, worker de copia, procesamiento de correo ni detección automática activos.

El navegador usado es Edge/Chromium headless, escritorio 1440×1000, un worker, cero reintentos automáticos y sesiones separadas por usuario. **No hay interceptaciones que sustituyan la API del producto** en los recorridos. Los dobles MSW descritos después pertenecen exclusivamente a las pruebas React, no a esta integración.

### Recorridos y resultados reales

[Suite @critico](fase-6-evidencias/6.B2/criticos.spec.mjs), [configuración](fase-6-evidencias/6.B2/playwright.config.mjs), [log conjunto final](fase-6-evidencias/6.B2/browser-05.log), [salida 0](fase-6-evidencias/6.B2/browser-05-exit.json), [reporte JSON final](fase-6-evidencias/6.B2/browser-05-resultados.json): **3/3 aprobados en 1,2 minutos**, sin saltos ni reintentos automáticos.

| Caso | Evidencia acreditada |
|---|---|
| `@critico A` | Creación por admin, Solicitud, codificación, rechazo A3→A2, nueva entrada y documento, aprobación, fabricación con observación, Packing List, llegada, salida, bodega y documento, incidencias y Fin automático. Once botones de etapa. Acción manual posterior en Fin →409 sin cambios. [Estado e historial](fase-6-evidencias/6.B2/critico-A.json). |
| `@critico B` | Compras China crea propuesta, adjunta Solicitud y producto propuesto mediante bloques reales. Revisión ve B2 en su bandeja, descarga B1 y rechaza a B1; el archivo de la entrada anterior se rechaza. Corrección con nueva Solicitud, aprobación, codificación, descarga B3 desde B4 y rechazo B4→B3. Nueva codificación/aprobación y devolución B5→B3 con urgencia PRIORITARIO de 12 h; otro rechazo durante la urgencia conserva su vencimiento hasta regresar a B5. Sigue B6 con observación, B7 rechaza sin Packing List/solo BL, B8 registra demora y llegada, B9 salida, B10 bodega y documento, B11 documento de incidencias, B12 Fin automático. Doce botones de etapa y evento final único. [Estado, historial, avisos y filtros](fase-6-evidencias/6.B2/critico-B.json), [captura Fin B](fase-6-evidencias/6.B2/B-Fin.png). |
| Descargas, permisos y roles | Descargas reales en navegador, nombre y bytes ficticios comprobados: B2/B1 por Revisión y B4/B3 por usuario con marketing+asistente_compras, y después por revisora asignada. Cuenta asignada con rol retirado o desactivada →403 al descargar, pese a conservar JWT; restauración limitada al fixture. Otro revisor no puede aprobar la etapa asignada. Admin sin China no ve creación B y API devuelve 403; consulta no puede enviar B1. |
| Definiciones, filtros y aislamiento | A3 Revisión y B3 Codificación se muestran como grupos distintos para una cuenta con varios roles. Filtro B3 manda `tipoFlujo=B` y `etapa=3`, muestra B y excluye A. Un pedido A de contraste se crea y avanza por API/pantallas reales hasta A3; su estado, historial y outbox permanecen idénticos durante el recorrido B. |
| Estado obsoleto | Una sesión abre el diálogo en B5; otra confirma B5→B6. El envío del diálogo antiguo devuelve 409 y muestra «Otro usuario actualizó este pedido», sin reenvío automático. El resto del recorrido usa el estado actual obtenido del servidor. |
| `@critico controles` | Admin+China no tiene flujo preseleccionado; elección de B con teclado. Dos POST simultáneos de ENVIAR con la misma etapa y documento producen **200/409**, un evento y un aviso, dejando B2. [Resultado](fase-6-evidencias/6.B2/doble-envio.json). No es solo un doble clic que el navegador podría impedir. |
| Polling | Otra sesión observa B4→B5 por consultas periódicas reales de **30000 ms**, sin recarga ni reloj falso: comprobada a los **30541 ms**. La sesión que actúa recibe el resultado de su mutación. [Lecturas y tiempo](fase-6-evidencias/6.B2/polling.json). No se acredita transporte instantáneo ni aceptación del cliente. |
| Archivos | Carga por bloques desde las pantallas y descarga por backend, sin simular documentos ni enlaces Drive. En la ejecución conjunta final se registraron **20 peticiones de fragmentos confirmadas** para archivos ficticios pequeños; no se presenta esto como ensayo de 100 MiB o recuperación de 3.B3. [Bloques HTTP](fase-6-evidencias/6.B2/cargas-bloques.json). |

[Operaciones sanitizadas](fase-6-evidencias/6.B2/operaciones.json), [exportación de datos ficticios](fase-6-evidencias/6.B2/base-final.json), [errores JavaScript del navegador: ninguno](fase-6-evidencias/6.B2/errores-navegador.json).

**Outbox, no entrega de correo:** el conjunto de todos los intentos dejó **93 filas**, todas PENDIENTES, vinculadas al evento del mismo pedido, con **cero intentos y cero envíos**. Las 93 incluyen intentos históricos del test; no son solo los tres escenarios finales. Todos los destinatarios siguen nulos: es el bloqueo explícito de destinatarios pendientes, no un fallo que se oculte eligiendo un correo. En el recorrido B final se comprueba unicidad evento/destinatario y la existencia del evento; los adjuntos conservan copia PENDIENTE. El doble envío agrega un único aviso. **REPORTAR_DEMORA no es una transición y no se le inventa un aviso**: se comprueba que conserva el conteo, manteniendo pendiente la política de avisos de demora. No se prueba Gmail aceptado ni recepción.

### Frontend: adaptación y pruebas

Se adaptan únicamente `src/test/servidor.ts` (respuesta explícita de B), una respuesta A al abrir creación y la expectativa del enlace de A en `ListaBandejaCreacion.test.tsx` (`?flujo=A`). Los permisos, validación, no envío de formularios inválidos y manejo de 403 conservan sus aserciones. Las definiciones de prueba provienen del contrato real V18 aislado en `src/test/flujosFase6.ts`; no se agrega un motor simulado a la aplicación.

Se añaden **cinco pruebas de comportamiento** en `Fase6Pantallas.test.tsx`: creación B y cuerpo enviado; elección obligatoria con roles múltiples y conservación del borrador ante 403; admin solo ve A; bandeja A3/B3 separada; filtro B3 en petición y URL. Se utiliza el soporte Radix de jsdom ya existente, sin reemplazar componentes ni silenciar errores. Las respuestas con estado son fixtures explícitos, no evidencia de integración real.

Desde `frontend`:

```powershell
npm.cmd run lint
npm.cmd run coverage -- --maxWorkers=1 '--coverage.include=src/**/*.{ts,tsx}' --coverage.reporter=json-summary --coverage.reporter=text --coverage.reportsDirectory=../documentos/pruebas/fase-6-evidencias/6.B2/cobertura-frontend
npm.cmd run build -- --outDir ../documentos/pruebas/fase-6-evidencias/6.B2/.tmp/build-final
npm.cmd run test -- src/components/pedidos/ListaBandejaCreacion.test.tsx src/components/pedidos/Fase6Pantallas.test.tsx --maxWorkers=1
```

Build con `VITE_ACCESO_LOCAL=false` y `VITE_API_URL=/api/v1`; no se altera el intervalo aprobado. **Lint 0, test 0, build 0**: [lint](fase-6-evidencias/6.B2/frontend-lint-final.log), [suite completa: 329/329 en 26 archivos](fase-6-evidencias/6.B2/frontend-test-final.log), [build](fase-6-evidencias/6.B2/frontend-build-final.log), [19/19 afectadas sobre la versión final](fase-6-evidencias/6.B2/frontend-dirigido-final.log). El último pase dirigido comprueba también la limpieza de tipos de las pruebas nuevas después del pase completo; no se repite sin motivo toda la suite.

Cobertura global actual de líneas: **1444/2113 = 68,33 %**; sentencias 68,17 %, ramas 64,31 %, funciones 71,92 %. [Resumen de cobertura](fase-6-evidencias/6.B2/cobertura-frontend/coverage-summary.json). **El objetivo adicional de 80 % frontend permanece pendiente**: no se añade exclusión ni se baja un umbral. Backend conserva como evidencia histórica de 6.B1 878 pruebas y 91,30 %; no es una medición nueva de 6.B2.

### Teclado y axe previstos en el plan

Dos casos adicionales con la misma API real, sin crear pedidos: elección de flujo con teclado y navegación a un pedido desde Productos propuestos con foco/Enter. axe-core **4.14.0**, dependencia local existente, reglas WCAG 2 A/AA y 2.1 AA: **cero violaciones y cero incompletos** en ambas vistas. [Suite](fase-6-evidencias/6.B2/accesibilidad.spec.mjs), [log](fase-6-evidencias/6.B2/a11y.log), [JSON](fase-6-evidencias/6.B2/a11y-resultados.json), [creación](fase-6-evidencias/6.B2/axe-creacion.json), [Productos propuestos](fase-6-evidencias/6.B2/axe-propuestos.json), [captura creación](fase-6-evidencias/6.B2/axe-creacion.png), [captura bandeja](fase-6-evidencias/6.B2/axe-propuestos.png).

Es teclado automatizado en escritorio, no revisión humana con lector de pantalla ni dispositivo móvil físico. La interacción de flecha mantiene `keydown` hasta comprobar foco/selección y después `keyup`, sin espera fija: evita que la liberación instantánea de la tecla del primer script ocurra antes del cambio de foco asíncrono de Radix. No se afirma comprobada locución real ni una auditoría completa de accesibilidad del producto.

### Comandos del entorno y navegador

Desde `backend`, tarea opt-in que compila solo la infraestructura y utiliza las clases actuales del producto:

```powershell
.\gradlew.bat -I ../documentos/pruebas/fase-6-evidencias/6.B2/aislado.init.gradle fase6B2 --offline --console=plain --no-daemon
```

Desde `documentos`, con Google/automatismos desactivados en el arnés:

```powershell
node pruebas/fase-6-evidencias/6.B2/servidor.mjs
node C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/cli.js test --config pruebas/fase-6-evidencias/6.B2/playwright.config.mjs --grep '@critico'
node C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/cli.js test --config pruebas/fase-6-evidencias/6.B2/a11y.config.mjs
```

Se cita el argumento `@critico` porque PowerShell interpreta sin comillas la arroba como splatting. Los logs detallan las ejecuciones. No se empleó almacenamiento de sesión persistente ni trazas con tokens; las capturas son posteriores al login y contienen solo datos ficticios.

### Intentos conservados y defectos

- [Frontend inicial](fase-6-evidencias/6.B2/frontend-baseline.log): 319 aprobadas, cinco fallos de fixtures antiguos al no responder definiciones en creación y esperar el enlace anterior. No eran defectos de producción.
- [Dirigida inicial](fase-6-evidencias/6.B2/frontend-dirigido-01.log): montaje de las nuevas pruebas (nombre del botón B y soporte Radix), corregido sin quitar comportamiento. [Dirigida corregida](fase-6-evidencias/6.B2/frontend-dirigido-02.log): 19/19. [Lint inicial](fase-6-evidencias/6.B2/frontend-lint-01.log): opción `exact` de Playwright no admitida por los tipos de Testing Library, retirada; los nombres literales siguen comparándose exactamente por defecto.
- [Navegador 01](fase-6-evidencias/6.B2/browser-01.log): no llegó a ejecutar tests por el argumento sin comillas. [02](fase-6-evidencias/6.B2/browser-02.log): el test esperaba 201 al finalizar bloques, pero el contrato exige 200. [03](fase-6-evidencias/6.B2/browser-03.log): textos de selectores distintos del texto real de Fin/conflicto. [04](fase-6-evidencias/6.B2/browser-04.log): el test inventaba aviso para demora y liberaba la flecha antes del foco de Radix. **Se corrige únicamente el arnés**, conservando sus comprobaciones de Fin, conflicto, teclado y outbox; [05](fase-6-evidencias/6.B2/browser-05-resultados.json) acredita el conjunto final aprobado.

**No se acreditó un defecto nuevo de producción reproducible en este alcance.** No se modifica producción para hacer pasar pruebas. Se mantienen los avisos de Vite sobre configuración futura y bundle mayor de 500 kB, y NO_COLOR/FORCE_COLOR; no se maquillan con cambios de configuración. Observación de infraestructura: al terminar, el lanzador PowerShell del servidor estático devolvió 1, sin excepción JavaScript en su log (solo avisos Vite); build explícito y recorridos dieron 0. Se conserva este resultado en vez de atribuirlo al producto o declararlo una parada con salida 0. [Comprobación de parada](fase-6-evidencias/6.B2/parada-comprobacion.json).

### Conservación, parada y pendientes

Hashes contra el inicio: **producción backend/frontend intacta, Header.tsx intacto e índice Git intacto**. Solo cambian tests/fixtures, el informe y la infraestructura/evidencias de esta verificación. Revisión Java del arnés aplicable, sin cambiar el backend: dobles primarios, propiedades aisladas, SQL de control acotado y parametrizado, usuarios ficticios, sin credenciales reales ni activaciones generales. Revisión de patrones en evidencia pública: sin tokens ni claves privadas detectados. No preparar `.tmp` ni archivos generados de build/descarga en un futuro commit.

API y contenedor propios detenidos; servidor estático terminado, **puertos 44284/44285/44286 sin escucha**, credencial ficticia temporal ausente. [Registro de parada](fase-6-evidencias/6.B2/parada.txt). Datos/evidencias y bytes ficticios aceptados conservados; sin borrado manual de volúmenes ni recursos ajenos. No se mantiene una política de borrado de archivos por deducción.

Quedan intactos los pendientes de 6.B1: meta de días, plazos B2/B6/B8/B11, decisiones provisionales y pendientes del cliente, destinatarios y avisos de demora, objetivo adicional de cobertura frontend, diferencia con 500 y limitación física temporal. Google sigue sin integración real acreditada; **3.B3 pospuesto**. **No se avanza a 6.B3 ni se cierra ninguna fase.**

## 6.B3 — Revisión del motor, persistencia y guía (2026-10-21)

Esta sección continúa el estado histórico anterior: se ejecuta únicamente **6.B3**, sin cerrar la fase. Skills aplicadas: `code-review`, `ecc:database-reviewer`, `ecc:verification-loop` y `ecc:learn-eval`. Leídos manual vigente, plan, decisiones de Richard, OpenAPI, arquitectura, ADR y skill de nuevos flujos. [Revisión detallada y reproductores para Claude](fase-6-evidencias/6.B3/revision.md), [extracto del manual](fase-6-evidencias/6.B3/manual-fase-6.txt).

### Resultado conjunto y vigencia

Se reutilizan las verificaciones aprobadas, sin atribuirles una fecha nueva de ejecución:

| Evidencia | Resultado vigente | Evaluación 6.B3 |
|---|---|---|
| Backend 6.B1 | Check aprobado, **878/878**, líneas **5382/5895 = 91,30 %** | Producción backend sin diferencias respecto al inicio de 6.B1; no se repite el check sin causa. |
| Frontend 6.B2 | **329/329**, lint/build 0, últimas afectadas **19/19** | Producción vigente; no se repiten tests ni se persigue demo desconectado. |
| Críticos con API real | **3/3**, A y B juntos, usuarios ficticios, máximo 20 pedidos | Recorridos hasta Fin y controles de permisos, aislamiento, doble envío, conflictos y polling acreditados localmente. |
| Teclado/axe focalizados | **2/2**, cero violaciones/incompletos | Alcance original de escritorio conservado; no equivale a locución de lector real ni móvil físico. |
| Cobertura frontend | **1444/2113 = 68,33 %** | **Objetivo adicional de 80 % pendiente**, sin exclusiones nuevas ni reducción de umbral. |

[Backend y cobertura](fase-6-evidencias/6.B1/resultados.json), [resultados conjuntos frontend/E2E](fase-6-evidencias/6.B2/resumen-final.json), [comprobación de vigencia y conservación](fase-6-evidencias/6.B3/revision-local.json).

La revisión de 463 archivos de producción backend/frontend contra el snapshot de 6.B2 no encuentra diferencias; tampoco hay diferencias en los archivos backend del snapshot de 6.B1. Header.tsx permanece intacto e índice vacío. El checker registra hashes de migraciones; no aplica migraciones ni conecta una base. Esto justifica reutilizar las suites, no certifica de nuevo su ejecución.

### Configuración versionada, motor y fronteras

V17 agrega B/EC v1 con doce etapas y transiciones/documentos aprobados; V18 completa únicamente las dos marcas de revisión B2/B4 por decisión de Richard. **V18 modifica esos atributos de v1**, no publica v2: excepción explícita y acotada de completado, no autorización general de cambiar reglas de pedidos en curso. A y los pedidos anteriores se conservan según la comparación de filas y validación de Flyway de 6.B1.

Las acciones y archivos resuelven la definición por `pedido.flujoId`, mientras creación resuelve la vigente. Habilitación concretada por definición, creación por rol inicial vigente antes de numerar, permisos de acción/descarga en servidor, documentos de la entrada actual y Fin automático comparten un motor. Las etapas automáticas no admiten transiciones salientes ni acciones manuales. Bloqueo del agregado, reloj por puerto y decoradores transaccionales mantienen acción/Fin/historial/outbox atómicos.

No se identifican imports de framework en el núcleo ni nuevas dependencias circulares: contratos compartidos y puertos de entrada respetan R1–R4/R9. Se reutiliza ArchUnit aprobado de 6.B1; la lectura estática no se presenta como nueva compilación. **No se acredita un defecto nuevo reproducible de ejecución de producción en este alcance.**

### Hallazgos para Claude y propuesta de ADR

| ID | Gravedad / estado | Reproductor e impacto | Propuesta sin implementar |
|---|---|---|---|
| **F6-B3-H01** | Media, abierto; documentación | Skill §1 pide «solo inserta» una nueva versión vigente, pero `definicion_flujo_vigente_unica` de V2 permite solo una. EC/A v1 vigente + v2 vigente ocasionaría 23505; con v2 no vigente la creación sigue en v1. Deducción del esquema, **sin ejecución SQL nueva**. No afecta la inserción del tipo B nuevo en V17. | Separar alta de otro flujo y publicación de versión; explicar el cambio atómico de vigencia sin alterar etapas antiguas ni `pedido.flujo_id`, y traspasar su prueba a Codex. |
| **F6-B3-H02** | Media, abierto; entrega documental | El plan 6.5, línea 147, promete `documentos/agregar-flujo.md` además de la skill; el archivo no existe. La skill sí ofrece guía y separa implementación/Claude de pruebas/Codex. | Entregar guía breve enlazada a la fuente única, o reconciliar expresamente ese entregable del plan con Richard. No inferir su retirada. |
| **P6-B3-ADR** | Propuesta, sin aceptación inferida | F6-D3 amplía el motor de conjunto numérico global a habilitación por definición. ADR-0004 cubre datos versionados, pero el registro específico sigue solo en el plan. | ADR complementario **proposed**, sin reservar número: contexto B11, alternativas y consecuencias, guard de automáticas, creación por rol y compatibilidad; enlazar 0004/0011/0012/0018. Richard valida. |

La skill refleja V17/V18, documentos, plazos pendientes, revisión, llegada, bodega y Fin; **no escribe ni ejecuta pruebas en parte A**, e incluye migración, permisos, documentos, descargas y recorrido conjunto para Codex. Está alineada con A/B actuales salvo el procedimiento incompleto de nueva versión. Un tipo adicional requiere ampliar también los tipos Java/Zod/contrato y opciones frontend, además de CHECK y datos; no se promete que agregar C sea únicamente SQL.

Los estados «sin verificar» y «última migración V16» del final del plan son anteriores a esta parte B: proponer su reconciliación mediante enlaces al informe conservando historia. No convertir pendientes antiguos ya resueltos en bloqueos actuales: V16 resolvió los documentos de A6/A9/A10 por decisión explícita; los requisitos se mantienen y los recorridos actuales no los omiten.

### Aviso del lanzador estático de 6.B2

Se reproduce **solo** la construcción/arranque/parada estática, sin API, base ni llamadas Google. Copia del mismo texto del lanzador en 6.B3, salida/dist aislada y flag de parada precreado. Primera ejecución confinada falla con `spawn EPERM` de Vite: [log conservado](fase-6-evidencias/6.B3/lanzador.log), [código 1](fase-6-evidencias/6.B3/lanzador-resultado.json). Se repite con permiso fuera del sandbox, sin modificar producción.

La repetición registra **Node `$LASTEXITCODE=0`**, pero **PowerShell `$?=false`**, con avisos Vite en stderr, build terminado y servidor cerrado por flag: [log](fase-6-evidencias/6.B3/lanzador-sin-sandbox.log), [resultado explícito](fase-6-evidencias/6.B3/lanzador-sin-sandbox-resultado.json). PowerShell representa esos avisos como `NativeCommandError` al redirigir todos los flujos. El wrapper captura ambos estados y devuelve explícitamente el código nativo; conserva los avisos de bundle y configuración.

Esto reproduce una causa compatible con el 1 observado, **sin recuperar retroactivamente el código nativo no registrado del lanzador antiguo**. No invalida las evidencias independientes aprobadas de 6.B2: tests y build explícito 0, respuestas reales, exportaciones y ausencia de errores JS. Tampoco autoriza a reescribir aquella salida como 0. Propuesta: registrar código nativo y estado PowerShell separadamente en lanzadores futuros.

Comandos focalizados desde `documentos` (no hubo nuevos tests ni migraciones):

```powershell
git branch --show-current
git diff --cached --name-only
node pruebas/fase-6-evidencias/6.B3/servidor-repro.mjs *> pruebas/fase-6-evidencias/6.B3/lanzador-sin-sandbox.log
$psSuccess = $?
$nativeCode = $LASTEXITCODE
@{ nativeExit=$nativeCode; powerShellSuccess=$psSuccess; stopFlag=$true; apiStarted=$false } | ConvertTo-Json | Set-Content pruebas/fase-6-evidencias/6.B3/lanzador-sin-sandbox-resultado.json -Encoding UTF8
# El wrapper ejecutado concluyó con: exit $nativeCode
python pruebas/fase-6-evidencias/6.B3/comprobar.py
```

El lanzador necesita previamente `.tmp/detener-front` en su directorio para detenerse solo; la copia utiliza el puerto loopback 44284 y no debe ejecutarse si está ocupado. [Fuente de reproducción](fase-6-evidencias/6.B3/servidor-repro.mjs). La reproducción realizó un build incidental, **no una medición nueva de cobertura**. [Puertos finales sin escucha](fase-6-evidencias/6.B3/escuchas-finales.json). No quedan servicios nuevos activos; no se borran datos ni evidencias.

### Learn-eval y criterios de salida

Evaluación de solapamiento en skills globales/proyecto y rutas MEMORY.md consultadas: el conocimiento reutilizable ya pertenece a `agregar-flujo-decokasa` y ADR-0004; MEMORY.md no existe en las tres rutas registradas en la revisión. Veredicto **Drop para crear otra skill**: no hay necesidad concreta de duplicarla o instalar aprendizajes. H01 queda como propuesta de corrección de la existente, sin modificarla.

| Criterio manual / pendiente | Evaluación |
|---|---|
| Recorridos críticos A y B verdes juntos | Acreditado localmente en 6.B2. |
| Doce etapas B correctas y A conservado | Acreditado por 6.B1/6.B2 y revisión actual. |
| Guía de nuevos flujos vigente | Parcial: resolver H01/H02 y registrar la ampliación arquitectónica según decisión del equipo. |
| Cobertura backend mínima 80 % | Acreditada en 6.B1: 91,30 %. |
| Objetivo adicional frontend 80 % | Pendiente: 68,33 %. No se confunde con el criterio manual de cierre. |
| Cierre de fase | **No autorizado automáticamente: Richard valida.** |

Persisten meta de días propuesta, plazos B2/B6/B8/B11 y los pendientes de A, P-10/P-14 y demás decisiones provisionales, destinatarios/avisos de demora, aceptación del polling por el cliente, diferencia frente al objetivo original de 500 y límite físico de almacenamiento temporal. La política local provisional de reintentos y cuotas no se convierte por esta revisión en aceptación definitiva de producción. **3.B3 real pospuesto; fases 3–6 abiertas.**

Sin producción modificada, Google, bases compartidas, escrituras Git ni alteraciones de Header.tsx. Comprobación de referencias locales del informe y patrones de credenciales en la nueva evidencia: [auditoría local](fase-6-evidencias/6.B3/revision-local.json). Es una comprobación acotada de patrones, no un escaneo integral de secretos del repositorio.

## Revalidación documental F6-B3-H01/H02 (2026-10-21)

Alcance exclusivo: `.claude/skills/agregar-flujo-decokasa/SKILL.md`, `documentos/agregar-flujo.md`, ADR-0019 y su índice. Se contrasta con esquema V2, resolución de definiciones backend, detalle y listas frontend. Revisión estática: **sin suites, builds, migraciones, producción modificada ni operaciones Google**. Rama `fase-6-flujo-b`; índice vacío; Header.tsx conserva el hash anterior. [Hashes de los cuatro documentos, enlaces y regla de exclusión Git](fase-6-evidencias/6.B3/revalidacion-H01-H02.json).

### H01: cerrado en su alcance documental

La skill distingue ahora alta de flujo, nueva versión y completado excepcional aprobado de un atributo. Sus §1/§1-bis corrigen la instrucción anterior de «solo insertar»:

- Retirar la vigencia anterior **antes** de insertar la nueva vigente, en una sola migración transaccional de PostgreSQL/Flyway; comprobar una fila anterior y una única vigente final. Respeta el índice parcial inmediato `definicion_flujo_vigente_unica` de V2, que impediría insertar primero con 23505.
- Conservar las filas de la definición anterior y todos los `pedido.flujo_id`; no borrar ni reasignar pedidos. El trigger `pedido_proteger_identidad` de V2 protege ese vínculo. Creación consulta la vigente; acciones, consultas y permisos resuelven por ID de definición del pedido. El detalle pide su versión explícita.
- Traspasar a Codex las pruebas de v1 en curso, nuevos pedidos v2 y rollback. Son **casos futuros**, no resultados ejecutados en esta revalidación. No se acredita aquí una publicación v2 real ni una prueba nueva de concurrencia de despliegues.

La transacción asegura que no se publique un estado intermedio visible de vigencia; no se promete que una petición ya iniciada bajo v1 cambie de definición. Flyway coordina las migraciones, y el índice conserva su garantía de unicidad. El procedimiento descrito corresponde a migraciones transaccionales, no a un mecanismo implementado de publicación en caliente.

**F6-B3-H01 cerrado** como defecto de instrucciones. No equivale a habilitar una versión futura ni a resolver la presentación multiversión del frontend.

### H02: guía entregada; remanente documental abierto

`documentos/agregar-flujo.md` ya existe, distingue los tres cambios y enlaza al procedimiento sin duplicarlo. La skill separa parte A/Claude (implementación sin pruebas) y parte B/Codex. Los enlaces relativos de los cuatro documentos resuelven **localmente**, incluido guía↔skill y ADR-0019↔otros ADR; el índice apunta al archivo correcto.

Se acredita la corrección de la **ausencia del archivo** original de H02. Se mantiene **H02 parcialmente resuelto, sin cierre integral**, por la distribución de su dependencia y la ambigüedad de prioridad siguientes:

1. **Skill ignorada y no rastreada.** `git check-ignore -v` desde la raíz devuelve `.gitignore:17:.claude/skills/`. `git ls-files` no incluye la skill (tampoco la guía ni ADR-0019 nuevos todavía; sí el índice ADR). La carpeta de esta skill es un directorio normal, no un enlace simbólico. Que el enlace funcione en esta máquina no garantiza que lo haga en un clon: un alta normal omitiría el procedimiento ignorado. Guía y ADR nuevos solo necesitan su preparación manual posterior; la skill requiere resolver expresamente la exclusión.
2. **Prioridad ambigua.** Skill, línea 16: «es la **única fuente** de los pasos». Guía, línea 5: «Si algo de aquí no coincide con la skill, vale la skill y esta guía se corrige». La segunda frase establece prioridad entre esos dos textos; no declara literalmente prioridad sobre requisitos o contrato, pero sin una salvedad puede interpretarse como autoridad general. La primera debe delimitarse como centralización del procedimiento, no de reglas. **Ninguna de ellas prevalece sobre requisitos, contrato ni ADR aprobados.**

Propuesta textual para ambas introducciones, sin aplicarla: «La skill centraliza únicamente los pasos operativos. La guía y la skill están subordinadas a los requisitos y decisiones aprobados, al contrato vigente y a los ADR aceptados. Ante una discrepancia, se registra y se corrige el procedimiento; no se cambia una regla del producto por seguir la skill. Un ADR proposed no autoriza cambios por sí mismo». En la guía, reemplazar «vale la skill» por esa regla de reconciliación. Mantener separados requisitos confirmados, decisiones provisionales y preguntas pendientes.

### Excepción Git limitada propuesta, no aplicada

Recomiendo conservar la ruta de la skill para no romper enlaces y **reemplazar** la línea `.claude/skills/` de `.gitignore` por estas reglas acotadas:

```gitignore
/.claude/skills/*
!/.claude/skills/agregar-flujo-decokasa/
/.claude/skills/agregar-flujo-decokasa/*
!/.claude/skills/agregar-flujo-decokasa/SKILL.md
```

No basta agregar solo una negación de SKILL.md debajo de la exclusión actual del directorio: Git no recorre un padre excluido. La propuesta abre únicamente ese directorio y ese archivo; otras skills locales y futuros archivos auxiliares siguen ignorados. **No se modifica `.gitignore`, ni se ejecuta git add -f o cualquier escritura Git.** Antes de guardar manualmente, comprobar con `git check-ignore -v --no-index` la excepción del archivo y que otra skill local siga excluida; estas comprobaciones de la regla propuesta aún no se han ejecutado porque no está aplicada.

Alternativa si Richard prefiere no tocar reglas: ubicar la fuente canónica en `documentos/skills/agregar-flujo-decokasa/SKILL.md`, actualizar los enlaces y documentar cómo se utiliza desde su ubicación rastreable. No mantener dos copias independientes ni incluir todas las skills locales. Esta alternativa es propuesta, no una reubicación realizada.

### ADR-0019 e índice

El ADR refleja `EtapasHabilitadas.segunDefinicion`, concreción antes de `incluye`, guard contra salidas de automáticas y creación por rol inicial sin excepción general del admin. Es coherente con el código revisado. Sus límites separan habilitación de permisos, no migran pedidos ni aprueban plazos/documentos pendientes. Complementa ADR-0004/0011/0012/0018 y no los sustituye.

ADR e índice coinciden en **proposed**, pendiente de aceptación por Richard, sin atribuir la decisión al cliente. La revisión documental de Codex queda acreditada aquí; la frase «pendiente de revisión por Codex» en sus cabeceras es anterior a esta sección y puede actualizarse posteriormente, **conservando proposed y la aceptación pendiente**. P6-B3-ADR pasa de «registro ausente» a «registro propuesto entregado y revisado»; no se declara accepted.

### Límites y estado final

**No se resuelve el límite de listas/tableros:** `useDefinicionesVigentes` sigue suministrando la definición vigente para las líneas compactas y opciones de filtro; el resumen no trae `versionFlujo`. Solo el detalle utiliza la versión del pedido. La skill §1-bis.5 lo reconoce y exige resolverlo antes de publicar versiones que cambien número, orden o nombres de etapas. Reconocerlo no es corregirlo ni probarlo. La conservación de reglas backend de v1 no acredita presentación correcta de v1/v2 simultáneas en esas vistas.

Comandos de lectura ejecutados desde la raíz: `git branch --show-current`, `git check-ignore -v -- .claude/skills/agregar-flujo-decokasa/SKILL.md`, `git ls-files -- .claude/skills/agregar-flujo-decokasa/SKILL.md documentos/agregar-flujo.md documentos/adr/0019-habilitacion-por-definicion.md documentos/adr/README.md`, `git diff --cached --name-only`; lectura y comparación estática de código/esquema; comprobación local de destinos Markdown y hashes SHA-256. Sin comandos Git de escritura.

Balance: **H01 cerrado; H02 parcial y abierto**, registro ADR revisado pero proposed; reproducibilidad en un clon y prioridad documental requieren los ajustes indicados. Sin hallazgo nuevo de producción en este alcance. Se conservan objetivo adicional frontend 80 %, presentación multiversión, demás pendientes reales, **3.B3 pospuesto y todas las fases abiertas**. Richard valida cualquier cierre de fase.

## Última comprobación H02 y preparación del avance local (2026-10-21)

**H02 cerrado en su alcance documental y de distribución.** Se conserva el historial de revalidaciones anteriores; este resultado lo actualiza, sin cambiar sus evidencias originales.

- `.gitignore` reemplaza la exclusión del padre por reglas que solo permiten `.claude/skills/agregar-flujo-decokasa/SKILL.md`. `git check-ignore -v --no-index` identifica su negación de inclusión en línea 22, mientras `shadcn/SKILL.md` sigue excluida por línea 19 y `agregar-flujo-decokasa/otro.txt` por línea 21. La skill se puede preparar normalmente, sin `-f`; no se abren todas las skills locales.
- Skill y guía dicen ahora expresamente que centralizan **solo pasos operativos**, subordinados a requisitos/decisiones aprobados, contrato vigente y ADR aceptados. No contienen ya «vale la skill» ni «única fuente» con alcance general. ADR proposed no autoriza por sí mismo cambios de reglas. Enlaces locales revisados sin destinos ausentes.
- ADR-0019 y su índice conservan **proposed**. La revisión de Codex está acreditada en este informe, sin inferir aceptación de Richard. Sus rótulos de «pendiente de revisión» son históricos y no convierten la propuesta en accepted.
- El contador de `CLAUDE.md` suma correctamente **19**: ocho cliente, ocho equipo, uno mixto y dos proposed, y corresponde a la clasificación que hoy escribe el índice. **Esta coherencia aritmética no certifica que todos los estados históricos estén actualizados**: la nota del outbox es la discrepancia siguiente.

### Discrepancia histórica del outbox: no reabre una aprobación

`CLAUDE.md:14` todavía cuenta ADR-0012 como mixto con outbox proposed; el índice ADR y ADR-0012 conservan la misma formulación antigua. La decisión vigente ya fue aprobada técnicamente por Richard en fase 3 (2026-10-16), como registra el plan de fase 3, §Registro de aprobación técnica F3-D1 y su tabla de fuentes: aviso pendiente en la misma transacción que acción/historial, entrega posterior fuera de ella, rollback sin aviso enviable e idempotencia sin prometer exactamente una entrega.

Es **deuda de sincronización documental**, no una pregunta de arquitectura reabierta. Propuesta posterior para Claude: anotar la aprobación técnica con fecha en ADR-0012 e índice y recalcular el resumen de CLAUDE.md, preservando las notas históricas. ADR-0013 también conserva proposed histórico aunque ADR-0017 resuelve Gmail API en el alcance acordado; su resolución ya está anotada en el índice. No se cambian aquí esos estados por inferencia. Outbox aprobado no implica destinatarios definitivos, integración Google real ni aceptación de ADR-0019.

### Selección para guardar manualmente

Se revisan únicamente cambios de fase 6: motor compartido A/B, V17/V18, ajustes y pruebas nuevas, frontend, contrato, plan/PRD, skill/guía, ADR-0019/índice y cambio del contador en CLAUDE.md. Las evidencias de intentos iniciales y finales se conservan para no perder reproductores. Se excluyen `.tmp`, binarios de resultados, builds, tokens/cuentas temporales, almacenamientos y archivos ficticios descargados; Header.tsx y todos los archivos ajenos de fases anteriores quedan fuera.

Selección exacta: [normal](fase-6-seleccion-normal.txt) y [22 logs ignorados revisados](fase-6-seleccion-ignorados.txt). [Inventario y guardas](fase-6-inventario.md), [auditoría de selección](fase-6-indice-auditoria.json). Son **300 rutas**: 278 normales y 22 logs que requieren `git add -f` exclusivamente por `*.log`. Las referencias del informe deben existir en la selección o estar ya rastreadas, y el índice debe coincidir exactamente con ambas listas. La preparación no hace commit, push ni merge.

Comprobaciones de secretos acotadas a la selección: sin claves privadas, tokens Google, secretos OAuth ni JWT Bearer literales; sin archivos mayores de 10 MiB. Los correos encontrados son ficticios `.test`/`.invalid`, usados en pruebas y Message-ID del outbox. Las rutas locales identifican procedencia de ejecución y no son credenciales. No se excluye una evidencia solo por contener correo o ruta. No se incluyen credenciales temporales del arnés.

No se repiten suites. Pruebas locales previamente aprobadas permanecen como evidencia, con frontend adicional 80 % pendiente. **Límite de listas/tableros por definición vigente pendiente; ADR-0019 proposed; 3.B3 real pospuesto; ninguna fase se cierra.**

**Preparación realizada:** 300 rutas, coincidencia exacta con las dos listas, sin archivos ajenos ni Header.tsx en el índice. Skill preparada sin forzar; `-f` solo para los 22 logs revisados. `git diff --cached --stat` termina en 0. `git diff --cached --check` devuelve **2 con 28 avisos**: 27 espacios al final de línea (plan, skill, requerimientos y XML históricos de pruebas) y una línea vacía final en el reproductor del lanzador. También se advierte conversión futura LF→CRLF por la configuración de Git. **No es un check limpio**; se informan los avisos y se conservan las evidencias originales. Salidas de la primera captura del índice en la auditoría; las comprobaciones se repiten al incorporar esta anotación final. No hubo commit, push ni merge.
