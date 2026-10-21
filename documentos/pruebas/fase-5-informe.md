# Fase 5: verificaciones 5.B1 y 5.B2

**Estado vigente: correcciones F5-B2-H01 y F5-B2-H02 revalidadas y cerradas en el escenario autorizado de hasta 20 pedidos. Producción y Header.tsx sin modificar. Evidencia de 500 conservada; el objetivo original del manual no se declara cumplido ni retirado. Fases abiertas y 5.B3 no iniciado.**

Fecha: 2026-10-20. Rama comprobada: `fase-5-seguimiento`. **Resultado histórico de 5.B1: calendario y alertas backend. Check APROBADO: 849 casos, 0 fallos, 0 errores y 0 omitidos; cobertura global de líneas 91,36 % · 5326/5830.** El mínimo backend del 80 % se cumple. En esa ejecución de 5.B1 todavía no se había realizado 5.B2. La actualización posterior se detalla al final; ninguna fase se cierra. Producción, cambios preexistentes y Header.tsx conservados; Google y detección automática real desactivados.

## Fuentes, skills y decisiones

Se leyeron el [manual vigente](../manual-paso-a-paso.docx), el [plan de fase 5](../../.claude/PRPs/plans/fase-5-seguimiento.plan.md), sus decisiones F5-D y las clases/contratos implementados. [Extracto de fase 5 del manual](fase-5-evidencias/manual-fase-5-extracto.txt). Skills aplicadas: `ecc:tdd-workflow` y `ecc:springboot-tdd`, cuyos SKILL.md actuales contienen las instrucciones completas; no tienen el antiguo archivo `references/original.md`. Se aplicó además `ecc:java-reviewer` con su referencia original para las pruebas Java. Spring Boot 4.1.1, Java 21, Gradle 9.7.1, JUnit/MockMvc, PostgreSQL 17 mediante Testcontainers y JaCoCo.

La prohibición expresa de cambiar producción y escribir en Git prevaleció sobre los pasos genéricos de implementación/checkpoints de las skills. Se registraron los fallos iniciales, se completaron pruebas y se adaptaron expectativas obsoletas; no se alteró producto para convertir un reproductor rojo en verde. No se ejecutaron comandos incrustados del plan ajenos al alcance (Docker Compose, frontend, instalaciones, merge o publicación).

| Decisión | Interpretación verificada |
|---|---|
| ADR-0006 y F5-D1 aprobada por el equipo | Zona America/Guayaquil; plazo PENDIENTE no es cero ni SIN_PLAZO. Fechas dependientes sin estimar; una llegada válida permite retomar el encadenamiento. Llegada real prevalece sobre estimada y se conserva su procedencia. |
| F5-D3/D4 provisionales del equipo | Motivo por interviniente vigente o admin con vencimiento activo; registro/corrección con historial, sin correo; limpieza del motivo al reentrar. ETAPA y URGENCIA son clases distintas; activación de urgencia por UUID estable de entrada, no solo por hora. |
| F5-D5 / outbox aprobado | Aviso, historial y outbox se confirman o revierten juntos. Destinatario ausente es bloqueo explícito; no se inventa un buzón ni se pierde el vencimiento. |
| F5-D6: **propuesta, no objetivo aprobado** | V14 siembra 90/PROPUESTA. Admin puede cambiar meta y versión, con autor/hora; origen ADMIN no elimina `metaPendienteDelCliente`. Diferencia favorable no acredita objetivo del cliente. |
| F5-D7 y jornada pendientes | Sin feriados ni jornada intradiaria aprobados: no se inventan. Cualquier hora de lunes a viernes es hábil según la regla provisional vigente; PUT con feriados no vacíos se rechaza. |
| F5-D9 técnico pendiente | Lote 100 e intervalo 300000 ms son valores técnicos pendientes de confirmar; no se aprueban por probarlos. Detección automática desactivada. |

El apartado de partida del plan conserva menciones históricas a F4-H01 pendiente; este trabajo no reabre su cierre revalidado en el [informe F4](fase-4-informe.md), ni toma una revisión estática como compilación aprobada.

## Aislamiento y conservación

El [ejecutor](fase-5-evidencias/ejecutar.ps1) fija OAuth/Drive/Gmail, correo/copia automáticos, semilla y `VENCIMIENTOS_AUTOMATICO=false`, elimina la importación local y usa una URL de base inválida como defensa si faltara Testcontainers. No arranca servicios compartidos ni accede a Google. Las pruebas existentes de proveedores usan sus dobles/protocolos locales.

`Fase5IT` importa el PostgreSQL Testcontainers de prueba sin reuse, usa disco UUID temporal propio y un puerto Reloj primario controlado. Migraciones reutiliza `DatosB1`, con esquema UUID exclusivo por caso dentro de SU contenedor. No hay credenciales reales, datos comerciales reales, eliminación de volúmenes, Flyway clean ni borrados de archivos aceptados. Las funciones/triggers de fallo son exclusivos del contenedor y se retiran en finally; no son migraciones de producción.

La concurrencia usa barreras/futuros con límites y bloqueos reales, sin pausas de minutos. El único plazo de espera real intencional es `lock_timeout=100ms` en una conexión PostgreSQL aislada para obtener SQLSTATE 55P03; no avanza el tiempo de negocio. El límite de vencimiento se prueba exactamente y después de él con reloj/Instant controlados.

Comparación de [494 hashes iniciales](fase-5-evidencias/protegidos-iniciales.json): **cero cambios** en `backend/src/main`, todo `frontend/src` existente, Header.tsx, build.gradle, índice, manual y plan. Las eliminaciones preexistentes del demo frontend tampoco se restauraron. No se ejecutaron git add, commit, push, merge ni otros comandos Git de escritura. La limitación del espacio físico temporal de almacenamiento de fase 3 sigue pendiente; esta prueba no la resuelve ni impone retención.

## Pruebas nuevas y garantías

Se añadieron cinco suites con responsabilidades diferentes, **66 casos nuevos**. No se duplicaron suites existentes ni se redujeron comprobaciones.

| Suite | Casos | Garantías |
|---|---:|---|
| `MigracionesFase5IT` | 8 | V1–V15 en vacío con esquema validado y reejecución nula; datos V8/V13/V14 conservados; urgencia vigente con identidad única; urgencia sin coincidencia o ambigua impide V15 atómicamente; tablas, restricciones, unicidad y protecciones de solo inserción. |
| `Fase5ReentradaTest` | 1 | Reentrada limpia motivo vigente, crea identidad distinta, reinicia plazo y conserva asignación. No borra historial. |
| `Fase5IT` | 35 | HTTP y PostgreSQL: cronograma, meta/propuesta, admin vigente, coerciones/feriados y versiones concurrentes; dos clases de vencimiento, frontera exacta, pendientes/Fin ignorados, identidad y reentrada, dos workers; rollback en cada escritura; destinatario nulo y bloqueo en registro; motivo/corrección, roles múltiples, rechazo y rollback; devolución real A4→A2 conserva activación de urgencia; bloqueo SQL real sanitizado; detector integrado con reloj controlado e idempotencia. |
| `Fase5DiagnosticoTest` | 11 | SQLSTATE 55P03/40P01/40001, conflicto optimista, dominio, base de datos, cadena cíclica y fallo de clasificación. Captura de logs sin mensajes, motivos, correos, tokens ni traza; fallo de un pedido no frena al sano, contadores coherentes; candidatos fallidos y tarea comparten pasada; diagnóstico caído y límites de lote. |
| `Fase5CronogramaTest` | 11 | Días hábiles/corridos/horas hábiles, fin de semana y medianoche de Ecuador; indicador de ajuste al lunes; PENDIENTE y propagación sin estimar; última entrada, salida real y descarte de entradas futuras antiguas; llegada estimada y prioridad real; total frente a meta propuesta e inmutabilidad de resultados. |

Los pedidos en A7/A8/A10/Fin son **fixtures de estado aislados**, no un recorrido A1–Fin ni resolución documental de A6/A9/A10. La creación de otra entrada o activación por SQL estudia su identidad en la persistencia; el reinicio/limpieza de entrada se comprueba con el dominio real, y la activación por devolución A4→A2 también atraviesa la API real del entorno de prueba. El detector se invoca manualmente como bean de prueba; se comprueba que no existe `TareaVencimientos` en el contexto desactivado. No se probó ni activó un scheduler de producción.

La atomicidad se fuerza con fallos de inserción específicos del pedido en `evento_pedido`, `notificacion` y `aviso_vencimiento`: quedan cero filas parciales y el pedido vuelve a poder procesarse tras retirar el fallo. El motivo también vuelve a su valor anterior si falla su historial, sin outbox adicional. Dos workers confirman un único aviso/evento/outbox; dos admin con la misma versión producen una 200 y otra 409, con un solo incremento. No se pretende verificar entrega de correo: el destinatario ausente conserva estado PENDIENTE y la API muestra `DESTINATARIO_PENDIENTE`.

## Adaptaciones conservando garantías anteriores

- `MigracionesIT`: V15 y trece migraciones posteriores a V2; secuencia exacta V1–V15, ausencia de pendientes y lista exacta de tablas, añadiendo solo `configuracion_plazos` y `aviso_vencimiento`.
- `V2RestriccionesIT`: expectativa inicial 15/V15; todas sus restricciones previas permanecen.
- `MigracionesFase4IT`: vacío actualizado a V15; los casos históricos que migran explícitamente hasta V13 validan con target V13, conservando filas, tiempos, triggers y restricciones. La validación completa V15 está cubierta además por las suites generales y nueva suite F5.
- `Backend24IT`: contrato de detalle incorpora los dos campos públicos aprobados `claseVencimiento` y `venceEn`. Se contrastó el [OpenAPI](../api/openapi.yaml), esquema DetalleEvento: los UUID de entrada/activación son internos y no se exigen en ese JSON. Mantiene **conjunto exacto de once campos**, los nueve anteriores incluidos, todos nulos para la observación, mismo JSON en historial y ausencia de mutaciones; no se cambió por una comprobación permisiva. Las identidades internas sí se comprueban en persistencia mediante Fase5IT.

Los [hashes originales de las cuatro pruebas adaptadas](fase-5-evidencias/pruebas-adaptadas-originales.json) y el diff permiten distinguir estas adaptaciones de una corrección del producto.

## Comandos y resultados

Desde documentos se ejecutó `.\pruebas\fase-5-evidencias\ejecutar.ps1` con el modo indicado. Equivalentes desde backend:

```powershell
.\gradlew.bat check --console=plain --no-daemon
.\gradlew.bat test --tests '*Fase5*' --tests '*MigracionesIT' --tests '*V2RestriccionesIT' --tests '*MigracionesFase4IT' --console=plain --no-daemon
```

| Ejecución | Resultado | Evidencia |
|---|---|---|
| Baseline, 15:00 | 742 casos / 9 fallos. V2 falla al inicializar, por lo que este total no incluye todos sus casos. Compilación de producción y pruebas ejecutada realmente. | [Log](fase-5-evidencias/20261020-150008-baseline.log), [JUnit](fase-5-evidencias/20261020-150008-baseline-junit/) |
| Dirigido inicial, 15:07 | 159 casos / 2 fallos de fixtures nuevos; no se corrigió producción. | [Log](fase-5-evidencias/20261020-150701-dirigido.log), [JUnit](fase-5-evidencias/20261020-150701-dirigido-junit/) |
| Compilación de pruebas, 15:09:15 | Falló una sobrecarga de AssertJ con SQLException (también Iterable). Cero pruebas nuevas ejecutadas en esta invocación. | [Log](fase-5-evidencias/20261020-150915-dirigido.log) |
| Dirigido corregido, 15:09:53 | 163 casos, cero fallos/errores/omitidos; BUILD SUCCESSFUL. Después se añadió un caso integrado del detector, incluido en el check final. | [Log](fase-5-evidencias/20261020-150953-dirigido.log), [JUnit](fase-5-evidencias/20261020-150953-dirigido-junit/) |
| Primer check completo, 15:11:25 | 849 casos / un fallo en la nueva expectativa de Backend24IT: se habían añadido dos UUID internos que no figuran en el contrato público. Corrección solo en prueba contra OpenAPI, conservando todas las comprobaciones originales. | [Log](fase-5-evidencias/20261020-151125-check.log), [JUnit](fase-5-evidencias/20261020-151125-check-junit/) |
| **Check final, 20261020-151741** | **APROBADO, salida 0; 849 casos / 0 fallos / 0 errores / 0 omitidos.** | [Log](fase-5-evidencias/20261020-151741-check.log), [comando/salida](fase-5-evidencias/20261020-151741-check.json), [JUnit completo](fase-5-evidencias/20261020-151741-check-junit/), [JaCoCo](fase-5-evidencias/20261020-151741-check-jacoco.xml) |

Los nueve fallos del baseline correspondían a expectativas V13, validación sin target de casos detenidos en V13 y conjunto JSON de detalle previo a los dos campos públicos nuevos; quedaron adaptados manteniendo las garantías. Los dos fallos del primer arnés eran de pruebas: se contaba el marcador sin versión que Flyway crea para un esquema nuevo (ahora se exige la lista exacta V1–V15) y el fixture Fin omitía `finalizado_en` (se completó sin retirar el CHECK). La sobrecarga de AssertJ se resolvió tipando la excepción como Throwable; no se eliminó su aserción. La advertencia nueva de cast redundante también se retiró solo de pruebas. El primer check completo detectó el error adicional de adaptación pública/interna descrito en la tabla; no era un defecto de producción. Un intento de aplicar ese ajuste mediante Python inline falló por quoting antes de escribir: se aplicó después mediante archivo de script, sin alterar código de producto.

**Artefactos históricos:** el ejecutor inicial copiaba resultados existentes incluso si no había una ejecución nueva. Por ello el directorio JUnit copiado con la invocación de compilación fallida de las 15:09:15 y cualquier JaCoCo antiguo junto al baseline **no son evidencia de esa invocación y no se usan para métricas**. Los logs originales se conservan. El ejecutor actual solo copia XML recién generados; las métricas siguientes usan exclusivamente el check final y su JaCoCo fresco. No se atribuye cobertura de un arnés filtrado a todo el backend.

Avisos existentes: los dos recursos `bloqueo` no referenciados en `CargasConfig` durante el baseline y el aviso de class sharing de la JVM; no justifican alterar producción. La compilación inicial está acreditada por tareas reales `compileJava`/`compileTestJava`, no por la revisión estática previa del plan.

## Cobertura real

| Ámbito | Líneas | Ramas |
|---|---:|---:|
| **Backend global** | **91,36 % · 5326/5830** | 72,87 % · 2057/2823 |
| Módulo `plazos` completo | 95,12 % · 448/471 | 83,12 % · 192/231 |
| Módulo `pedidos` completo | 94,86 % · 1310/1381 | 78,39 % · 497/634 |
| Módulo `historial` completo | 97,76 % · 349/357 | 88,06 % · 118/134 |
| Módulo `notificaciones` completo | 83,23 % · 268/322 | 71,91 % · 169/235 |
| Módulo `archivos` completo | 86,34 % · 1391/1611 | 63,84 % · 669/1048 |

Medición reproducible con [medir.py](fase-5-evidencias/medir.py) y [métricas completas](fase-5-evidencias/final-metricas.json), incluidas fuentes bajo el 80 %. Las cifras de módulo incluyen código previo y nuevo; no se presentan como cobertura exclusiva del diff. **build.gradle, exclusiones y mínimo global LINE 0.80 intactos**. No se ejecutó frontend ni se actualiza su última cobertura; el objetivo adicional frontend ≥80 % sigue pendiente.

## Defectos, bloqueos y límites

**No se reprodujeron defectos nuevos de producción en este alcance.** Los rojos iniciales quedan clasificados arriba como expectativas obsoletas o errores del arnés, con logs y casos para reproducirlos. No se oculta ningún caso fallido ni se propone una corrección de producción sin reproductor.

- 5.B2 (dos sesiones, actualización, rendimiento de 500 pedidos y N+1) y 5.B3 (accesibilidad/regresión) **no ejecutados**. No se certifican sus objetivos ni sus endpoints pendientes.
- La propuesta de meta de 90 días sigue sin aprobación del cliente; feriados, jornada y plazos pendientes no se resolvieron. Intervalo/lote del detector y su activación real requieren la confirmación prevista por F5-D9.
- La API de vencidos/agrupaciones globales conserva los pendientes de contrato señalados por el plan. No se implementaron para probar 5.B1 ni se sustituyeron por una vista ficticia.
- A6/A9/A10 mantienen sus bloqueos documentales y A1–Fin real sigue bloqueado; no se presentaron fixtures como recorrido aprobado.
- **Fases 3, 4 y 5 abiertas; 3.B3 pospuesto.** Sin Google real, bases compartidas, eliminación de volúmenes ni comandos Git de escritura. Detección automática desactivada fuera de las invocaciones manuales del entorno de prueba; no se autoriza activarla con este informe.


## 5.B2 — API real, polling y rendimiento con 500 pedidos (2026-10-20)

**Resultado: alcance de diez casos acreditado con API real: nueve aprobados en la última pasada y el caso de polling revalidado 5/5 tras corregir una carrera del arnés. Dos hallazgos de producción abiertos para Claude: estabilidad visual y crecimiento de consultas por usuario.** La meta local de carga inferior a dos segundos se alcanza en las condiciones siguientes; esto no acredita rendimiento en la conexión del cliente, Core Web Vitals de campo ni actualización «al instante». No se avanza a 5.B3 ni se cierra ninguna fase.

Se leyeron manual, plan vigente, ADR-0014 y decisiones F5-D2/D10. Se aplicaron `playwright-best-practices` (incluidas referencias de colaboración, esperas, reloj y rendimiento), `ecc:benchmark`, `web-perf`, `performance-optimization`, `ecc:react-performance` y la revisión Java de `ecc:java-reviewer` al arnés opt-in. La instrucción de no cambiar producción prevaleció sobre los pasos genéricos de optimización de las skills. Las métricas se guardan junto al informe, en vez de crear otro directorio `.ecc/benchmarks`.

### Entorno y alcance real

Rama `fase-5-seguimiento`. API Spring Boot real en Tomcat, PostgreSQL **17.11** de Testcontainers exclusivo, Flyway V1–V15 y almacenamiento local exclusivo del arnés. Frontend Vite compilado en modo producción, sin acceso local de demo y con API del mismo origen a través del proxy del arnés. No se modifican producción, índices, contratos ni definiciones del flujo.

Fixtures: exactamente **500 pedidos**, Ecuador/flujo A, 50 por cada estado inicial A1–A10, 11 filas de etapa por pedido; cuatro usuarios ficticios con roles admin, Compras China, Logística y Consulta. Los pedidos proceden de inserciones explícitas de estado: **no constituyen recorrido A1–Fin**. Las mutaciones de las pruebas acumulan historial, reentradas y algunas llegadas A7→A8; el total continúa en 500. A6/A9/A10 conservan documentos PENDIENTES y botones de confirmación bloqueados. La política provisional P-13 permite lectura general a cuentas activas con rol; no se inventa aislamiento por propiedad del pedido. Las acciones y las bandejas sí respetan permisos vigentes. No se habilitaron otros países: no se acredita aislamiento operativo multpaís.

Equipo: Intel Core i5-12400F, **12 hilos**, RAM **16.984.231.936 bytes**; Windows 11 Pro `10.0.26100`, Node `v24.21.0`, Edge **155.0.4283.62**, viewport 1440×1000, headless, un worker Playwright y sin reintentos automáticos del runner. Conexión IPv4 loopback, sin limitación artificial de ancho de banda, latencia o CPU. No equivale a WAN Ecuador–China ni a un móvil. JVM, PostgreSQL y sus páginas ya estaban precalentados al medir; no se reinició la base ni se vació su caché para llamar «fría» a una muestra.

Referencias: [README y comandos del arnés](fase-5-evidencias/5.B2/README.md), [entorno](fase-5-evidencias/5.B2/entorno.json), [equipo](fase-5-evidencias/5.B2/equipo.json), [fuente de fixtures/control](fase-5-evidencias/5.B2/java/com/decokasa/b2/ArnesB2.java), [source set opt-in](fase-5-evidencias/5.B2/b2.init.gradle), [servidor compilado](fase-5-evidencias/5.B2/servidor.mjs).

### Comandos y resultados

Desde `backend`: `$env:TESTCONTAINERS_RYUK_DISABLED='true'; .\gradlew.bat -I ..\documentos\pruebas\fase-5-evidencias\5.B2\b2.init.gradle f5B2 --offline --console=plain --no-daemon`. Compila el backend y el arnés para arrancar la API, **no ejecuta el check completo**. [Log de arranque/cierre](fase-5-evidencias/5.B2/backend-ejecucion.log).

Desde `documentos`: `node pruebas\fase-5-evidencias\5.B2\servidor.mjs`. Vite build aprobado; advertencia de chunk mayor de 500 kB, sin cambiar configuración para ocultarla. [Build](fase-5-evidencias/5.B2/frontend-build.log).

Runner: `node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test -c pruebas\fase-5-evidencias\5.B2\playwright.config.mjs`.

| Ejecución | Resultado acreditado | Evidencia |
|---|---|---|
| Última pasada con fixtures corregidos | 9 aprobados / 1 fallo del arnés, 66,26 s. Incluye métricas y diagnóstico CDP; no aceptación de CLS | [JSON](fase-5-evidencias/5.B2/resultados-fixture-corregido.json), [log](fase-5-evidencias/5.B2/browser-final.log) |
| `--grep 'intervalo sin solapamiento' --repeat-each 5` | 5/5, 11,7 s; revalidación del único caso afectado por la carrera foco→offline, conservando todas las aserciones | [JSON](fase-5-evidencias/5.B2/resultados.json), [log](fase-5-evidencias/5.B2/browser-polling-repetido.log) |
| Planes de lectura | EXPLAIN ANALYZE ejecutado en el contenedor exclusivo | [SQL](fase-5-evidencias/5.B2/planes-exactos.sql), [planes](fase-5-evidencias/5.B2/planes-exactos.txt) |
| Experimento N+1 | 500 pedidos, de 4 a 20 usuarios ficticios | [fixture](fase-5-evidencias/5.B2/usuarios-ficticios.sql), [comando SQL aprobado](fase-5-evidencias/5.B2/usuarios-ficticios.log), [medidor](fase-5-evidencias/5.B2/medir-usuarios.mjs), [resultado](fase-5-evidencias/5.B2/escalado-usuarios.json) |

Las iteraciones previas fallaron por defectos del **arnés**: reloj instalado después de crear timers; evento sintético emitido sobre `document` sin propagación al listener de `window`; selector ambiguo de búsqueda (formulario e input comparten nombre); ruta HTTP sin drenar al terminar; texto esperado de bloqueo distinto del real. Se corrigió únicamente infraestructura/aserciones, conservando comprobaciones. El fixture de reentrada se ajustó para no actualizar una columna `version` inexistente en `etapa_pedido`. También se retiró del fixture un vencimiento artificial de A7: A5/A7/A10 deben permanecer sin fecha, con 350 vencidos globales frente a 50 cargados. Las métricas finales corresponden a esa base corregida; las anteriores se conservan en `iteracion-anterior` y no se usan para acreditar esta entrega. La última pasada reprodujo una carrera del propio caso 03: entraba en offline antes de terminar el refetch de foco; esperar a que termine y procesar 50 ms de reloj controlado antes de desconectar mantuvo el requisito de reconexión y pasó 5/5. No atribuir esos fallos a producción. [Preparación](fase-5-evidencias/5.B2/resultados-preparacion.json), [ampliación inicial](fase-5-evidencias/5.B2/resultados-ampliado.json), [ajuste](fase-5-evidencias/5.B2/resultados-ajustado.json), [revalidación inicial](fase-5-evidencias/5.B2/resultados-revalidacion.json).

**No se repitió el backend completo ni la cobertura**: no se cambió producción y no hubo causa nueva de corrección backend. Se conserva la evidencia real de 5.B1: 849 pruebas/check aprobado, cobertura global de líneas 91,36 %, módulo plazos 95,12 %. Estos valores son de 5.B1, no una medición nueva de este paso. Tampoco se repitieron las suites frontend desconectadas ni se alteraron sus umbrales; el objetivo adicional global del 80 % continúa pendiente según fase 4.

### Polling, permisos y borradores

| Comprobación | Evidencia real y límite |
|---|---|
| Dos sesiones aisladas | Admin publica observación (201); Logística ve el mismo texto en el historial sin navegación, por polling de 30 s. Acción 167 ms; desde confirmación HTTP hasta visible **30421 ms**. [Medición](fase-5-evidencias/5.B2/dos-sesiones.json). Este intervalo empieza al recibir 201 en la sesión A, no en el instante interno exacto del commit. |
| Intervalo y solapamiento | Build usa 30.000 ms; reloj instalado antes de montar consultas. Una respuesta HTTP real retenida durante varios intervalos mantiene máximo **1 petición simultánea de la misma clave**. No implica que claves diferentes deban serializarse. [Política](fase-5-evidencias/5.B2/polling.json). |
| Pestaña oculta, foco, conexión | Page Visibility emulada: sin peticiones durante 95 s controlados oculta; refetch al volver visible. Contexto offline real del navegador y evento online al reconectar provocan lectura. No se acredita una pestaña física minimizada ni ahorro de energía del SO; es política del frontend sobre eventos controlados con API real. |
| 403 y permisos vigentes | Logística recibe 403 al modificar meta admin. Su bandeja devuelve solo A8/A9/A10. Retirar sus roles en la base aislada provoca 403 del detalle; conserva sesión y botón Cerrar sesión. Restaurar rol y foco permite recuperar datos. [Permisos](fase-5-evidencias/5.B2/permisos.json). |
| Cambio de cuenta | Admin→Consulta cancela 2 solicitudes API; no conserva caché/formulario/borrador del admin. Consulta mantiene acceso de lectura P-13, sin editor de observación. [Cancelación](fase-5-evidencias/5.B2/cambio-sesion.json). |
| Respuesta tardía y reentrada | Se retiene una respuesta obtenida del backend, se escribe y luego se entrega intacta: el texto permanece. Fixture aislado de reentrada cambia entrada vigente; publicar contra la anterior devuelve 409; solo 1 POST, texto recuperable y editor nuevo vacío. Los IDs acreditan que nunca se trasladó el texto a otra entrada. [Resultado](fase-5-evidencias/5.B2/borrador.json). |
| Cambio real de etapa | APROBAR A7 registra llegada y avanza a A8 (200). Compras China intenta publicar su texto previo: 409, 1 POST contra A7, borrador recuperable y sin editor en A8. [Cambio](fase-5-evidencias/5.B2/cambio-etapa.json). |
| Producto bloqueado | A6/A9/A10 muestran «Documentos pendientes de definición» y confirmación deshabilitada en respuesta/pantalla reales. Las pruebas no redefinen los documentos para superar esos bloqueos. |
| Paginación | 25 páginas × 20, total 500 en cada respuesta y 500 IDs únicos, sin omisiones ni duplicados. Panel diferencia total global de vencidos de grupos que solo representan los resultados cargados (máximo 50). [Recorrido](fase-5-evidencias/5.B2/paginacion.json). |

No hay API comercial simulada: `route.fetch()` conserva las respuestas del servidor y solo introduce retraso. Sí hay eventos/tiempo inducidos y fixtures de estado, señalados separadamente. Las cookies y tokens de sesiones ficticias nunca se incluyen como evidencia compartible.

### Rendimiento de carga: qué mide la meta de dos segundos

Cinco repeticiones por condición. «Dashboard» va desde solicitar navegación a `/inicio` (o click SPA Inicio) hasta renderizar la pestaña con total 500, la fila esperada de la primera página y el panel global de vencidos. Incluye recuperación de sesión en navegación completa, API y render de los datos iniciales, **no** las 25 páginas ni una espera de polling. «Búsqueda» va desde click en Buscar hasta mostrar el editor del detalle consultado; incluye GET exacto y consultas del detalle/render. El tiempo de escribir el número y login inicial quedan fuera. Se espera aparte la carga de fuentes y dos frames para recoger métricas visuales; ese muestreo posterior no cambia el intervalo de carga ya medido.

Fría significa caché HTTP del navegador vaciada mediante CDP y QueryClient nuevo por navegación; caliente HTTP conserva recursos pero vuelve a crear QueryClient. Caliente-query reutiliza caché HTTP y de consultas al volver por enlace SPA. Ninguna representa arranque frío de JVM o disco de PostgreSQL. Se drenan las peticiones del login antes de medir y las repeticiones están en [muestras sin resumir](fase-5-evidencias/5.B2/pantalla-metricas.json).

| Caché cliente | Dashboard mediana / máximo (ms) | Búsqueda mediana / máximo (ms) | HTTP API hasta dashboard listo, por repetición |
|---|---:|---:|---|
| Fría HTTP + QueryClient nuevo | 191,47 / 244,32 | 131,41 / 142,41 | [7, 7, 7, 7, 7] |
| Caliente HTTP + QueryClient nuevo | 175,39 / 188,09 | 114,21 / 118,15 | [7, 7, 7, 7, 7] |
| Caliente HTTP + QueryClient conservado (SPA) | 111,16 / 138,65 | 109,01 / 124,98 | [0, 0, 0, 0, 0] |

Todas las muestras locales de esos intervalos son menores de 2.000 ms. No se declara cumplido el rendimiento del cliente en otra red/equipo ni «al instante»; **ADR-0014/F5-D2 sigue provisional y pendiente de aceptación del cliente**. La observación entre sesiones tarda unos 30 s, coherente con polling + petición.

API: 100 peticiones secuenciales por endpoint, sin reloj simulado, más un lote de 10 concurrentes por endpoint. Tiempo wall-clock del cliente HTTP local, incluye transferencia/lectura del cuerpo; JVM/PG calientes. Percentiles por rango más próximo, no estimación poblacional con intervalos de confianza. Todo 200; sin Google.

| Endpoint | p50 ms | p95 ms | p99 ms | Cuerpo bytes | Lote de 10 concurrentes ms |
|---|---:|---:|---:|---:|---:|
| Bandeja dashboard, 20 pedidos | 23,84 | 33,12 | 36,83 | 6487 | 61,24 |
| Búsqueda exacta por número | 21,41 | 31,76 | 35,43 | 3712 | 45,09 |

[Muestras API](fase-5-evidencias/5.B2/api-metricas.json), [resumen reproducible](fase-5-evidencias/5.B2/metricas-resumen.json), [script](fase-5-evidencias/5.B2/resumir.py). La API de bandeja de 20 no equivale por sí sola al dashboard completo: este lanza **7 HTTP** en navegación completa (renovar, tres conteos, página de bandeja, definición de flujo y página global vencida), o **0** lecturas al volver con caché de consultas fresca. La búsqueda exacta se solicita incluso con caché del detalle caliente.

### SQL, planes y hallazgo F5-B2-H02 abierto

`StatementInspector` cuenta SQL ejecutado por Hibernate, incluyendo lectura de permisos y montaje de vistas; no captura valores enlazados, credenciales ni SQL de preparación JDBC. Para `/pedidos/bandeja` se miden **21 SQL** con tamaños 1, 20 y 100. El fetch join evita un SELECT por cada pedido/etapa de la página. No se midió como un solo número SQL agregado de las siete peticiones del dashboard: se informan por separado endpoint y fan-out HTTP. [SQL capturado](fase-5-evidencias/5.B2/consultas.json).

**F5-B2-H02 — N+1 confirmado respecto del total de usuarios.** Conservar 500 pedidos y página de 20, añadir 16 cuentas inactivas ficticias con rol/país, y repetir el mismo GET eleva el total de **21 a 69 SQL** (+48, tres por usuario añadido). Lectura final 51,91 ms, muestra diagnóstica única, no benchmark comparativo de latencia. `ResumirUsuariosCasoDeUso.resumenes` llama a `usuarios.listar()`; su mapeo JPA materializa roles/países de usuarios que ni siquiera aparecen como responsables en la página. El listado general ya se realiza incluso cuando la colección buscada queda limitada. Impacto: consultas y datos personales cargados internamente crecen con usuarios, aunque la página siga siendo de 20. No se observaron esos datos ajenos en la respuesta autorizada.

Reproductor: arrancar fixtures de cuatro usuarios; reset inspector; GET bandeja tamaño20; conservar 21. Ejecutar `usuarios-ficticios.sql` solo sobre el contenedor de `entorno.json`; `node pruebas\fase-5-evidencias\5.B2\medir-usuarios.mjs`; conservar 69. **Propuesta para Claude, no implementada ni aprobada:** consultar resúmenes por IDs requeridos y cargar roles por lote/proyección, manteniendo permisos vigentes en cada acción. No añadir un índice para ocultar un problema de fan-out. Clasificación: hallazgo de eficiencia, no fallo de la meta de dos segundos en esta muestra.

EXPLAIN ANALYZE/BUFFERS, mismo contenedor y datos ficticios: bandeja admin **0,188 ms**, índices `pedido_creado_en`, `etapa_pedido_una_actual`, `definicion_etapa_numero_unico`; bandeja Logística **0,757 ms**, combinación de índice/joins; predicado exacto por número **0,022 ms**, `pedido_numero_unico`; predicado textual `%ficticio%` **0,470 ms**, Seq Scan + Sort. Los planes de bandeja usan el SQL nativo observado con parámetros ficticios; los de búsqueda aíslan sus predicados y **no** representan todo el mapeo DTO. La muestra no justifica añadir índices; un Seq Scan con 500 filas no es por sí solo un defecto. [Planes](fase-5-evidencias/5.B2/planes-exactos.txt), [diagnósticos complementarios](fase-5-evidencias/5.B2/explain.json).

### Core Web Vitals y hallazgo F5-B2-H01 abierto

Métricas **de laboratorio** vía PerformanceObserver, más una traza CDP de `devtools.timeline,loading,blink.user_timing`. LCP observado: frío **124–160 ms**, HTTP caliente **96–108 ms**. Los candidatos de interacción observados mediante Event Timing tienen máximos de **32–48 ms** según recorrido; son un **candidato INP de esta interacción de laboratorio**, no INP de campo/p75 poblacional. La navegación SPA caliente-query conserva la métrica del documento anterior: no se presenta como un LCP nuevo de la ruta.

CLS usa ventanas de sesión (pausas >1 s o duración >5 s) y excluye desplazamientos con entrada reciente. Frío **0,187–0,289**, caliente **0,286797** en las cinco repeticiones. **No cumple estabilidad visual de referencia**. Las referencias oficiales de [Web Vitals](https://web.dev/articles/vitals) distinguen medición de laboratorio y campo; sus referencias buenas son LCP≤2,5 s, INP≤200 ms y CLS≤0,1 al p75. No se toma una muestra local rápida como aprobación global de los tres indicadores.

**F5-B2-H01 — desplazamiento visual reproducible del dashboard.** Reproductor: fixtures 500, Edge 1440×1000, login, vaciar caché HTTP, navegar `/inicio`, observar llegada de bandejas/fuentes. La traza diagnóstica recoge dos desplazamientos: 0,002450 y **0,286797**, suma de la ventana ≈**0,289246**. Fuentes: sección de vencidos, tarjetas inferiores y celdas/cabeceras de tabla; cambios en la geometría cuando se sustituye carga por contenido. `PanelVencidos` usa skeleton h-24 y la tabla inicial reserva menos geometría que los datos reales. Es una hipótesis causal apoyada en rectángulos/traza; el aporte exacto de fuentes frente a contenido requiere una comparación controlada, sin modificar producto en esta revisión. El pequeño desplazamiento del encabezado no autoriza tocar Header.tsx.

**Propuestas para Claude, no implementadas:** reservar dimensiones de carga/paginación consistentes con el contenido visible y estabilizar columnas; separar y medir la contribución de fuentes. Repetir CLS con las mismas cinco condiciones/repeticiones antes de aceptar una corrección. [Fuentes y rectángulos](fase-5-evidencias/5.B2/estabilidad-visual.json), [traza comprimida](fase-5-evidencias/5.B2/dashboard.trace.json.gz), [imagen ficticia](fase-5-evidencias/5.B2/dashboard-ficticio.png). Bundle: JS **823,54 kB / 252,22 kB gzip**; CSS **84,70 / 14,88 kB gzip**. La página fría registra unos **1,05 MB** transferidos/15 recursos en la muestra inicial. El servidor de medición sirve sin compresión HTTP; gzip del build es tamaño estimado, no bytes comprimidos realmente entregados. Code splitting es propuesta medible, no cambio autorizado.

### Cierre del entorno y limitaciones pendientes

Se cerraron únicamente frontend, API y control propios: **43184/43185/43186 sin listener**, archivo temporal de acceso retirado. El código pidió STOP al contenedor; al salir la JVM **Testcontainers lo retiró automáticamente**, igual que los contenedores de preparación. La primera inspección devolvió «no such object»; la comprobación final por ID confirma ausencia, no «contenedor conservado detenido». La pasada definitiva usó **tmpfs para PGDATA**, sin montajes de volúmenes Docker: [tmpfs](fase-5-evidencias/5.B2/tmpfs.json), [montajes](fase-5-evidencias/5.B2/montajes.json). No se ejecutó un comando de borrado manual de volúmenes, `docker rm`, `prune` ni `down -v`; no hubo bases compartidas ni volúmenes productivos. Los datos de fixture fueron efímeros; evidencias y almacenamiento local se conservan. [Parada del proceso](fase-5-evidencias/5.B2/parada.txt), [estado previo](fase-5-evidencias/5.B2/estado-final.json), [comprobación Docker](fase-5-evidencias/5.B2/parada-docker.json), [conservación comprobada](fase-5-evidencias/5.B2/conservacion-final.json).

Se contrastaron **494 hashes** de producción/frontend/Header.tsx, build, manual, plan e índice con el inventario protegido de 5.B1: sin diferencias. No se ejecutó Git de escritura. Una iteración fallida de `route.fetch` volcó una cabecera JWT ficticia en el diagnóstico automático: se retiró de los artefactos nuevos afectados, se drenaron rutas antes de terminar y el escaneo final de texto/traza no detecta tokens/llaves reales. No se modificaron evidencias anteriores. `.tmp`, dist, credenciales transitorias y clases generadas no forman parte de la selección de evidencia compartible.

Pendientes: F5-B2-H01 y H02 para Claude; aceptación del cliente del polling periódico; medición representativa de red/equipo y Core Web Vitals de campo; visibilidad física/minimización si se requiere evidencia de SO; aislamiento multipaís fuera de EC v1. Se conservan los pendientes del cliente sobre plazos, meta provisional, destinatarios y políticas no aprobadas. La limitación del espacio físico temporal de archivos y los pendientes anteriores no se resuelven por medir este dashboard. A6/A9/A10 bloqueadas, recorrido A1–Fin sin aprobar; objetivo adicional frontend 80 % pendiente; **3.B3 real pospuesto, detección automática/OAuth/workers desactivados y fases 3/4/5 abiertas. 5.B3 no iniciado.**


## Ajuste expreso a 20 pedidos y correcciones de 5.B2 — pendientes de revalidación

Decisión de Richard (2026-10-20): **máximo de 20 pedidos ficticios en el escenario principal**. Esto configura la evaluación, no limita pedidos productivos ni cambia la paginación del servidor. El objetivo de 500 pedidos del manual sigue documentado: **no se declara cumplido ni retirado por inferencia**. Las cifras anteriores son evidencia histórica, anterior a estas correcciones. No se repitió esa carga ni se sustituyeron sus resultados por cifras de 20.

[Configuración principal](fase-5-evidencias/5.B2/escenario-principal.json). El generador opt-in tiene `MAXIMO_PEDIDOS = 20`; conserva A5/A7/A10 sin vencimiento inventado. Su [fuente histórica de 500](fase-5-evidencias/5.B2/historico-500/ArnesB2.java.txt) se guardó fuera del source set antes de modificarlo. No se alteraron las suites Playwright ni resultados, trazas, logs o capturas anteriores. **El runner Playwright existente sigue siendo histórico de 500 y no debe ejecutarse sin que Codex lo adapte al escenario de 20**, conservando sus comprobaciones útiles y escribiendo evidencia nueva en otro destino. Los scripts que resumen las mediciones anteriores siguen siendo históricos; no constituyen una nueva evaluación de las correcciones.

### F5-B2-H01: corrección implementada; no cerrada

La lectura del código confirma el mecanismo independiente del volumen: `TablaPedidos` sustituía cuatro filas de skeleton por todas las filas reales y dejaba ancho de columnas al contenido; la sección de vencidos sustituía skeleton h-24 por agrupaciones y tabla completas. La traza histórica había identificado los elementos que desplazaban las tarjetas inferiores. Reducir a 20 pedidos no elimina estas diferencias de geometría. El aporte residual de las fuentes no queda cuantificado de nuevo.

Cambios:

- `TablaPedidos` ofrece `alturaEstable` opt-in para el dashboard: viewport desplazable de 28 rem para carga/datos/vacío/error; columnas de ancho definido y `table-fixed`. Otros consumidores conservan su presentación normal. Región desplazable enfocada por teclado y con nombre accesible.
- `TableroTrabajo` activa esa opción, reserva el hueco de paginación y un ancho mínimo para los contadores, evitando insertar esos espacios cuando llegan los datos.
- `PanelVencidos` reserva un viewport desplazable de 32 rem para el cuerpo, con región y foco de teclado; las tarjetas que siguen no cambian de posición porque crezca su contenido.
- No se toca Header.tsx, DESIGN.md, reglas de transición, datos de documentos ni configuración de fuentes. No se simulan filas comerciales ni enlaces; los skeleton siguen siendo decorativos.

Pendiente para Codex, con hasta 20 pedidos: carga fría/caliente, pocos resultados, cero, error y recuperación, pestañas y cambios de página; conservar acceso a todas las filas mediante scroll, teclado y pantallas pequeñas, sin recortes de controles/texto importante. Volver a recoger CLS y sus fuentes, incluidas cargas tardías de definición y fuentes tipográficas; comparar con el histórico sin atribuir la diferencia únicamente a la corrección, pues también cambió el volumen. No se promete CLS cero ni se acredita el umbral sin medir.

### F5-B2-H02: corrección implementada; no cerrada

La causa se confirma estáticamente: `ResumirUsuariosCasoDeUso` invocaba `usuarios.listar()`, y el adaptador convertía todos los usuarios a dominio, materializando roles, países y campos que no se necesitaban para el resumen. Eso explica el crecimiento histórico 21→69 al añadir 16 usuarios ajenos; puede ocurrir con un solo pedido.

Cambios:

- El caso de uso solicita `resumirPorIds` únicamente para el conjunto deduplicado de IDs necesarios; para cero IDs retorna vacío.
- El puerto del repositorio expone una proyección mínima de id, nombre, roles y estado, sin Spring/JPA. Conserva una implementación básica por IDs para repositorios en memoria; no recorre el listado completo. El adaptador persistente sobreescribe esa ruta.
- El adaptador JPA ejecuta **una sentencia nativa con parámetros enlazados** que une usuario/roles y filtra por esos IDs. Agrupa roles sin materializar entidades ni relaciones lazy; no selecciona correo, teléfono, hash ni países. Usuarios inexistentes se omiten, sin roles permanecen con conjunto vacío; el estado sigue siendo `activo && tiene algún rol`, igual que antes.
- No se añaden índices, caché ni cambios en comprobaciones de autorización. Las lecturas de roles/estado para decidir permisos permanecen vigentes y separadas del resumen visual.

Pendiente para Codex: compilar y revalidar con PostgreSQL aislado; cero IDs, IDs repetidos/ausentes, usuario sin roles, inactivo y múltiples roles; mismo contrato de nombre/roles/activo. Mantener hasta 20 pedidos y variar usuarios ajenos (activos e inactivos): el SQL del resumen persistente debe permanecer por lote y no crecer por cada usuario ajeno. Contar SQL sin confundirlo con consultas legítimas de permisos. Revisar dobles existentes que solo preparen `listar()` para adaptarlos al puerto de lectura por IDs, conservando sus aserciones: **no se modificaron pruebas en esta implementación**. La implementación básica del puerto no acredita por sí sola la eficiencia del adaptador JPA.

### Estado de la entrega

Solo implementación, revisión estática y documentación. **No se ejecutaron pruebas, builds, lint, migraciones, servicios ni mediciones.** No hay nuevos resultados de rendimiento, compilación o cobertura; la cobertura y aprobaciones previas pertenecen a la revisión anterior a los cambios. La revisión Java fue estática según `ecc:java-reviewer`; no se ejecutó su sugerencia genérica de check por la prohibición expresa del encargo. Se aplicó `ecc:react-performance` a estabilidad geométrica y se conservaron las fronteras del repositorio.

Ambos hallazgos quedan **implementados, pendientes de revalidación**, no descartados por reducir el número de pedidos. La futura evaluación de 20 es distinta del objetivo de 500 del manual y debe registrarse como tal. OAuth, workers, schedulers y detección automática continúan desactivados; no hay Google real ni comandos Git de escritura. A6/A9/A10 permanecen bloqueadas, 3.B3 pospuesto y fases abiertas. No se avanza a 5.B3.


## Revalidación de H01/H02 con hasta 20 pedidos — 2026-10-20

**H01 y H02 cerrados por evidencia del escenario actual de 20 pedidos.** No se modificó producción, no se redujeron umbrales ni se ocultaron fallos. Las secciones anteriores describen estados históricos sucesivos: sus pendientes de corrección quedan sustituidos únicamente por este cierre acotado. El objetivo original de 500 pedidos del manual permanece registrado; **esta revalidación no lo cumple ni lo retira por inferencia**. No se repitió la carga de 500 ni se sobrescribió su evidencia.

### Entorno y conservación

Rama `fase-5-seguimiento`, API real Spring Boot/Tomcat, PostgreSQL 17 exclusivo Testcontainers con PGDATA tmpfs y almacenamiento local aislado. **20 pedidos**, dos por A1–A10; A5/A7/A10 sin vencimiento inventado y documentos A6/A9/A10 sin redefinir. Fixtures de estado, no recorrido A1–Fin. Los usuarios varían 4→20→60, los pedidos permanecen siempre en 20. No son datos reales.

Frontend compilado real, no servidor de desarrollo; `/api/v1` llega al backend real por el proxy del arnés. Equipo Intel i5-12400F/12 hilos, RAM 16.984.231.936 bytes, Windows 11 Pro, Edge 155, un worker sin retries. Navegador headless, viewports 1440×1000 y 390×844. Base ya inicializada; caché fría significa HTTP del navegador vaciada por CDP, no JVM/PostgreSQL fríos. Esta revisión mide CLS y consultas, no repite un benchmark completo de latencias ni Core Web Vitals de campo.

Se creó [un directorio separado](fase-5-evidencias/5.B2-revalidacion-20/README.md). [604 hashes de partida](fase-5-evidencias/5.B2-revalidacion-20/protegidos-iniciales.json) cubren producción/frontend, Header.tsx, índice, manual, plan y todo el histórico 5.B2 fuera de `.tmp`. El contraste final solo cambia **dos archivos de pruebas autorizados**, sin otras diferencias: [resumen final](fase-5-evidencias/5.B2-revalidacion-20/resumen-final.json). No se ejecutó Git de escritura. Las imágenes muestran exclusivamente cuentas y pedidos ficticios.

### Comandos y verificaciones afectadas

Desde backend se ejecutó el arranque opt-in: `.\gradlew.bat -I ..\documentos\pruebas\fase-5-evidencias\5.B2-revalidacion-20\b2.init.gradle f5B2 --offline --console=plain --no-daemon`. [Backend compilado y arrancado](fase-5-evidencias/5.B2-revalidacion-20/backend.log), [fuente del arnés](fase-5-evidencias/5.B2-revalidacion-20/java/com/decokasa/b2/ArnesB2.java), [entorno](fase-5-evidencias/5.B2-revalidacion-20/entorno.json). Todos los flags OAuth/Drive/Gmail/workers/detección se fuerzan a false; importación de configuración privada desactivada.

Frontend: `node pruebas\fase-5-evidencias\5.B2-revalidacion-20\servidor.mjs` realizó build real en `.tmp/dist` y sirvió loopback. [Build aprobado](fase-5-evidencias/5.B2-revalidacion-20/frontend-build.log); persiste el aviso de chunk >500 kB, sin cambiar su umbral ni producción para ocultarlo.

Backend dirigido: `.\gradlew.bat -I ..\documentos\pruebas\fase-5-evidencias\5.B2-revalidacion-20\b2.init.gradle revalidarResumenesB2 test --tests '*ConsultasTest' --tests '*UsuarioJpaRepositorioIT' --tests '*ReglasArquitecturaTest' --tests '*FronterasFaseDosTest' --offline --console=plain --no-daemon`. Configuración externa desactivada, DB general apuntando a puerto inválido; la integración obtiene su PostgreSQL por Testcontainers y el almacenamiento usa un directorio temporal exclusivo. No hubo base compartida.

| Comprobación | Resultado real | Evidencia |
|---|---|---|
| Backend afectado | **24/24**, 0 fallos/errores/omitidos; build dirigido aprobado en 41 s | [Log](fase-5-evidencias/5.B2-revalidacion-20/backend-pruebas.log), [JUnit nuevo de resúmenes](fase-5-evidencias/5.B2-revalidacion-20/junit-resumenes/TEST-com.decokasa.b2.ResumirUsuariosB2Test.xml), [persistencia](fase-5-evidencias/5.B2-revalidacion-20/junit-backend/TEST-com.decokasa.seguimiento.usuarios.adapter.out.persistence.UsuarioJpaRepositorioIT.xml), [permisos](fase-5-evidencias/5.B2-revalidacion-20/junit-backend/TEST-com.decokasa.seguimiento.usuarios.application.ConsultasTest.xml), [fronteras](fase-5-evidencias/5.B2-revalidacion-20/junit-backend/TEST-com.decokasa.seguimiento.arquitectura.FronterasFaseDosTest.xml), [arquitectura](fase-5-evidencias/5.B2-revalidacion-20/junit-backend/TEST-com.decokasa.seguimiento.arquitectura.ReglasArquitecturaTest.xml) |
| `npm.cmd run lint` | Aprobado, TypeScript sin errores | [Log final](fase-5-evidencias/5.B2-revalidacion-20/frontend-lint-final.log) |
| `npm.cmd run test -- src/components/dashboards/DashboardRol.test.tsx src/components/pedidos/ListaBandejaCreacion.test.tsx src/components/pedidos/Fase4Pantallas.test.tsx src/auth/SesionProvider.test.tsx` | **57/57**, 4 suites aprobadas, 10,23 s | [Log final](fase-5-evidencias/5.B2-revalidacion-20/frontend-tests-final.log) |
| Playwright inicial con API real | **8/8**, incluye carga, polling, consultas, permisos, sesiones y paginación | [JSON](fase-5-evidencias/5.B2-revalidacion-20/resultados-ocho-casos.json), [log](fase-5-evidencias/5.B2-revalidacion-20/browser.log) |
| Playwright dirigido, vacío/error/recuperación | **1/1**, 2,0 s | [JSON](fase-5-evidencias/5.B2-revalidacion-20/resultados-estados.json), [log](fase-5-evidencias/5.B2-revalidacion-20/browser-estados.log) |
| Playwright dirigido, CLS sin routing y caché válida | **1/1**, 34,7 s; 20 muestras | [JSON](fase-5-evidencias/5.B2-revalidacion-20/resultados.json), [log](fase-5-evidencias/5.B2-revalidacion-20/browser-cls-cache.log) |

Runner: `node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test -c pruebas\fase-5-evidencias\5.B2-revalidacion-20\playwright.config.mjs`, y dirigidos con `--grep 'área de resultados estable'` / `--grep 'CLS de carga fría'`. Son **nueve escenarios distintos**, con repetición adicional del caso de carga para precisar las condiciones de caché, no diez escenarios distintos. [Suite ejecutada](fase-5-evidencias/5.B2-revalidacion-20/revalidacion.spec.mjs).

La primera ejecución Vitest tuvo 55 aprobados y 2 fallos: la expectativa de encabezados del dashboard omitía «Mi trabajo», ya añadido en fase 5; el test de llegada usaba un selector síncrono antes de recibir la definición de flujo. Se adaptaron exclusivamente `DashboardRol.test.tsx` y `Fase4Pantallas.test.tsx`: se conserva la igualdad exacta de roles sin duplicados incluyendo Mi trabajo; se incorporan handlers de lectura para ese fixture de composición, y llegada espera el mismo botón que verificaba antes, conservando sus comprobaciones de hora/etapa. No se suprimieron aserciones ni errores. [Primer log conservado](fase-5-evidencias/5.B2-revalidacion-20/frontend-tests.log). Los tests con MSW se identifican como simulados y no sustituyen la integración real Playwright.

No se repitió el check completo backend: las verificaciones se limitaron al código y fronteras afectados. El `jacocoTestReport` que finaliza el test dirigido **no acredita cobertura global nueva ni cumplimiento del 80 %**. Las cifras globales de 5.B1 siguen históricas, anteriores a estas correcciones; el objetivo adicional frontend del 80 % sigue pendiente. No se declara compilación aprobada basándose en la revisión estática anterior: aquí sí se compilaron backend y frontend.

### H01 — CLS: cerrado en el escenario autorizado

La versión corregida mantiene la geometría de bandeja/vencidos durante carga y datos. Se midieron cinco repeticiones por combinación de viewport y caché, veinte muestras, con **300 ms de latencia CDP**, sin limitar caudal ni interceptar peticiones. Así se reproduce una llegada tardía de datos/fuentes sin sustituir respuestas del servidor. Se esperan datos, fuentes y dos frames; se usan ventanas CLS (separación >1 s / duración >5 s) y se excluyen entradas recientes según PerformanceObserver. Cada muestra debe tener CLS≤0,1; se conserva el mismo umbral, no solo una media favorable.

| Viewport | Caché HTTP | n | p75 CLS | Máximo CLS |
|---|---|---:|---:|---:|
| 1440 px | fria | 5 | 0.00099910 | 0.00099910 |
| 1440 px | caliente | 5 | 0.00005589 | 0.00717740 |
| 390 px | fria | 5 | 0.00319902 | 0.00319902 |
| 390 px | caliente | 5 | 0.00008072 | 0.00008072 |

[Todas las muestras y elementos desplazados](fase-5-evidencias/5.B2-revalidacion-20/cls-carga.json). Caché comprobada: los recursos estáticos transfieren **1.021.231 bytes** en cada carga fría y **0** en cada caliente. Las peticiones API siguen siendo reales. La serie inicial retenía respuestas reales con `route.fetch`, pero Playwright desactiva caché HTTP cuando hay routing: sus etiquetas fría/caliente no acreditaban esa diferencia. Se conserva únicamente como [diagnóstico con caché desactivada](fase-5-evidencias/5.B2-revalidacion-20/cls-carga-con-routing.json); la serie de la tabla es la repetición válida sin routing. No se descartó ninguna muestra desfavorable de la serie final.

Durante una actualización real desde otra sesión, el motivo del retraso de un pedido A8 visible apareció en el dashboard por polling de 30 s: CLS **0.00222257**, propagación **29900 ms** desde iniciar el PUT hasta observarlo. Sin entrada del lector que excluyera ese cambio; payload, persistencia y lectura del servidor reales. [Métrica y fuentes](fase-5-evidencias/5.B2-revalidacion-20/cls-polling.json).

Vacío por pestaña «Con urgencia», error 403 por retirar roles vigentes y recuperación tras restaurarlos conservan la altura del panel dentro de 1 px, y mantienen abierta la sesión: [rectángulos y resultado](fase-5-evidencias/5.B2-revalidacion-20/estados-resultados.json). [Captura escritorio](fase-5-evidencias/5.B2-revalidacion-20/dashboard-1440.png), [captura móvil](fase-5-evidencias/5.B2-revalidacion-20/dashboard-390.png). No se acredita aquí una auditoría AA/responsive completa: la captura móvil conserva el desbordamiento preexistente del encabezado, que no se modificó por la instrucción sobre Header.tsx.

Las mediciones históricas de 500 (CLS≈0,289) se mantienen como contexto. **No se atribuye toda la diferencia a la corrección**, pues el volumen también cambió y no se midió un baseline previo sin corregir a 20. El cierre acredita el umbral y los escenarios actuales de carga/polling; no acredita Core Web Vitals de campo ni el objetivo original de 500.

### H02 — consultas por usuarios: cerrado

Se mantuvieron los mismos 20 pedidos y se añadieron usuarios ficticios ajenos al resultado, incluso inactivos, reproduciendo la causa del crecimiento histórico. `StatementInspector` registró **11 SQL** por GET de bandeja en las **nueve combinaciones**: usuarios 4/20/60 × página de 1/5/20. No creció el número de consultas por usuario ajeno ni por pedido de la página. Se cuenta el endpoint completo, incluidos permisos; no se confunde con una sola consulta de resumen.

La lectura directa del caso de uso persistente con IDs duplicados, ausentes, persona sin roles, inactiva y con varios roles ejecutó **1 sentencia** y devolvió cuatro usuarios existentes con los estados y conjuntos de roles correctos. Cero IDs produjo cero sentencias. La proyección no selecciona correo, teléfono, hash ni países y no materializa relaciones lazy; las comprobaciones de permisos conservan su ruta vigente. El caso de uso también tiene tres pruebas unitarias que impiden volver al listado general y conservan el contrato. [SQL y aserciones](fase-5-evidencias/5.B2-revalidacion-20/sql-revalidacion.json), [pruebas nuevas](fase-5-evidencias/5.B2-revalidacion-20/java/com/decokasa/b2/ResumirUsuariosB2Test.java).

Esto confirma el cambio del mecanismo que originaba el N+1, independientemente de tener pocos pedidos. El 21→69 histórico con 500 no se ha vuelto a medir y no se presenta como comparación controlada de latencias. No se añadieron índices ni cachés para lograr el resultado.

### Permisos, paginación y sincronización conservados

Cuatro páginas reales de cinco devuelven total 20 en cada respuesta y **20 IDs únicos**. Para vencidos se confirma total global 14 y una página API de cinco; la pantalla carga los 14 y no anuncia grupos parciales ni paginación inexistente. [Paginación](fase-5-evidencias/5.B2-revalidacion-20/paginacion.json).

Logística solo ve su trabajo A8/A9/A10 (6 pedidos); modificar la meta admin devuelve 403. Retirar roles provoca 403 sin cierre de sesión; restaurarlos y recuperar foco permite recargar. Cambiar admin→Consulta cancela lecturas pendientes y descarta caché/borrador previo, sin conceder edición. Se conserva la lectura general P-13 y EC v1, no se inventa aislamiento por propiedad ni países nuevos. [Permisos](fase-5-evidencias/5.B2-revalidacion-20/permisos.json), [sesión](fase-5-evidencias/5.B2-revalidacion-20/cambio-sesion.json).

Dos contextos de navegador confirman que una observación 201 aparece en la otra sesión sin navegar: [tiempos reales](fase-5-evidencias/5.B2-revalidacion-20/dos-sesiones.json). La política de pestaña oculta/foco/reconexión y no solapamiento también pasó con reloj/eventos controlados y HTTP real: [política](fase-5-evidencias/5.B2-revalidacion-20/polling.json). Esta parte no acredita minimización física del SO. El transporte es periódico, **no «al instante»**, y su aceptación por el cliente sigue pendiente.

### Parada y pendientes preservados

API, frontend y control detenidos; **43184/43185/43186 cerrados**, acceso temporal retirado. PostgreSQL era tmpfs exclusivo; Testcontainers retiró automáticamente su contenedor al salir. [Estado previo](fase-5-evidencias/5.B2-revalidacion-20/estado-final.json), [tmpfs](fase-5-evidencias/5.B2-revalidacion-20/tmpfs.json), [retirada comprobada](fase-5-evidencias/5.B2-revalidacion-20/parada-docker.json), [conservación/escaneo](fase-5-evidencias/5.B2-revalidacion-20/resumen-final.json). Sin borrado manual de volúmenes ni recursos compartidos. Escaneo de los artefactos nuevos sin tokens o claves privadas; no se conservaron storageState/HAR/credenciales. El histórico de 500 queda intacto por hashes.

**Solo se cierran F5-B2-H01 y F5-B2-H02 en el escenario aprobado de hasta 20 pedidos.** A6/A9/A10 bloqueadas, recorrido A1–Fin no aprobado, 3.B3 pospuesto, detección automática/OAuth/workers/schedulers desactivados. Se conservan pendientes de cobertura, espacio físico temporal y aceptación del cliente. Fases abiertas; **5.B3 no iniciado en aquel corte**; ejecución posterior documentada a continuación.

## 5.B3 — accesibilidad y regresión local (20–21 de octubre de 2026)

**Ejecutado con hallazgos abiertos; ninguna fase cerrada.** [Comandos, resultados, reproductores, gravedad y limitaciones](fase-5-evidencias/5.B3/README.md).

Backend: **849 pruebas aprobadas, check PASS**, cobertura de líneas global **91,34 %**, archivos **86,32 %**, plazos **95,12 %**. [Contadores reales](fase-5-evidencias/5.B3/backend-resumen.json), [check](fase-5-evidencias/5.B3/backend-check.log), [JaCoCo](fase-5-evidencias/5.B3/jacoco-backend.xml).

Frontend: **324 pruebas aprobadas en 25 archivos**, lint/tsc y build PASS; cobertura global de todo src **67,35 % (1362/2022)**. El objetivo adicional del 80 % continúa pendiente; el 86,80 % de archivos cargados no es cobertura global. Sin exclusiones ni cambios de umbral. [Ejecución](fase-5-evidencias/5.B3/frontend-coverage-confirmada.log), [código nativo 0](fase-5-evidencias/5.B3/frontend-exit.json), [cobertura global](fase-5-evidencias/5.B3/cobertura-frontend-confirmada/coverage-summary.json), [lint](fase-5-evidencias/5.B3/frontend-lint-final.log), [build](fase-5-evidencias/5.B3/frontend-build.log). Solo se ajustaron dos fixtures de `AccionesPedido.test.tsx` que devolvían un estado POST distinto del GET posterior; se conservan las aserciones y ejecuciones fallidas iniciales. Producción intacta.

Axe/Playwright con API real: barrido inicial 2/6 aprobado. Se corrigió la expectativa nueva de bloqueo documental de 422 al contrato 409 y pasó al repetir A6/A9/A10 con historial intacto. Tres escenarios siguen fallidos por accesibilidad, sin debilitar axe: resultado combinado 3 aprobados y 3 fallidos. [Barrido](fase-5-evidencias/5.B3/resultados.json), [repetición documental](fase-5-evidencias/5.B3/bloqueos-revalidacion.log), [respuestas e historial](fase-5-evidencias/5.B3/bloqueos.json).

Hallazgos abiertos: **F5-B3-H01**, nombres visibles/accesibles de línea de tiempo, seria según axe y pendiente de contraste normativo manual; **H02**, cronograma móvil sin acceso por teclado al desplazamiento horizontal, seria; **H03**, contraste 3,38:1 en los dos botones del conflicto de meta, seria; **H04**, cambio por polling sin anuncio DOM observado, moderada y pendiente de lector real; **H05**, overflow preexistente del shell móvil, moderada, con Header intacto. Reproductores y límites detallados en el README. [Axe base](fase-5-evidencias/5.B3/axe-resumen.json), [contraste del conflicto](fase-5-evidencias/5.B3/axe-meta-conflicto.json), [polling](fase-5-evidencias/5.B3/anuncios-polling.json).

**Correcciones de la parte A (2026-10-21), pendientes de revalidación por Codex:** H01 a H05 tienen corrección en código, sin ejecutar pruebas, builds ni mediciones. Los hallazgos siguen **abiertos** hasta repetir sus reproductores. El detalle por hallazgo está en el plan de la fase 5, sección "Correcciones de accesibilidad de 5.B3". `Header.tsx` no se modificó.

Teclado, pestañas y foco comprobados; error de meta y conflictos conservan borradores; reentrada no envía a otro ID; Fin no tiene acciones manuales. Revisión visual y de teclado asistida por agente en escritorio y móvil emulado, separada de axe. No acredita lector de pantalla, control por voz ni móvil físico. [32 pasos manuales](fase-5-evidencias/5.B3/manual-pasos.json), [teclado automatizado](fase-5-evidencias/5.B3/teclado.json), [borrador/reentrada](fase-5-evidencias/5.B3/borrador-conflicto.json), [formulario](fase-5-evidencias/5.B3/meta-formulario.json). Cero infracciones detectadas en dashboard/meta inicial no equivale a conformidad completa.

Entorno exclusivo con máximo 20 pedidos y PostgreSQL tmpfs. Servicios detenidos y credencial ficticia temporal retirada; puertos cerrados. [Conservación y escaneo](fase-5-evidencias/5.B3/resumen-final.json), [estado previo a parar](fase-5-evidencias/5.B3/estado-final.json), [aislamiento](fase-5-evidencias/5.B3/tmpfs.json), [parada](fase-5-evidencias/5.B3/parada.txt). Sin Google, bases compartidas, borrado manual de volúmenes ni escrituras Git; producción, Header e históricos de 500 conservados.

Permanecen pendientes la aceptación del polling periódico, meta propuesta, objetivo adicional del 80 % frontend y diferencia con los 500 originales del manual (no cumplido ni retirado). A6/A9/A10 siguen bloqueadas y A1–Fin no aprobado; los fixtures no lo sustituyen. 3.B3 real Google pospuesto, detección/OAuth/workers/schedulers desactivados. Fases 3, 4 y 5 abiertas.

## Revalidación de H01–H05 de 5.B3 — 2026-10-21

[Procedimiento, comandos, resultados y límites completos](fase-5-evidencias/5.B3-revalidacion/README.md). Se utilizó API real con PostgreSQL Testcontainers exclusivo, almacenamiento aislado y **20 pedidos ficticios**, sin repetir suites backend ni carga de 500. Producción, Header.tsx, índice Git y evidencias anteriores intactos respecto al inicio.

| Hallazgo | Estado acreditado |
|---|---|
| **H01** | **Cerrado** en el alcance de axe/árbol accesible y teclado. Once nombres derivados del contenido, estado una vez, sin círculo numérico duplicado. Cero `label-content-name-mismatch` en A6/A8/A10 a 1440/390. No acredita locución ni control por voz reales. |
| **H02** | **Cerrado**. Tab llega a la región nombrada, foco visible; ArrowRight mueve el scroll móvil de 0 a 40 px (contenido 704, cliente 322). Axe sin región desplazable inaccesible. |
| **H03** | **Cerrado para contraste estable y estados revisados**. Botones habilitados de conflicto: **15,93:1**, conservando 409 y borrador. Reintentar pasó axe con/sin foco en meta, cronograma y bandeja con errores HTTP ficticios. Señal transitoria 4,13:1 durante habilitación conservada y distinguida del estado estable; no se afirma revisar todas las alertas. |
| **H04** | **Cerrado como defecto de emisión DOM**. Un anuncio tras A7→A8, ninguno en sondeos idénticos anterior/posterior, foco intacto. Sin lector de pantalla: locución pendiente, no comprobada. |
| **H05** | **Abierto, parcialmente resuelto**. Documento 390/390 y cerrar sesión funciona con teclado. Bodegas y Roles permanecen recortados al recibir Tab en el menú horizontal. Gravedad moderada; reproductor para Claude en README. |

[Nombres, scroll y menú](fase-5-evidencias/5.B3-revalidacion/nombres-scroll-menu.json), [recortes exactos](fase-5-evidencias/5.B3-revalidacion/menu-parcial-390.json), [contraste](fase-5-evidencias/5.B3-revalidacion/contraste-meta.json), [axe conflicto estable](fase-5-evidencias/5.B3-revalidacion/axe-meta-conflicto.json), [señal transitoria conservada](fase-5-evidencias/5.B3-revalidacion/axe-meta-conflicto-transitorio.json), [Reintentar](fase-5-evidencias/5.B3-revalidacion/reintentar.json), [polling](fase-5-evidencias/5.B3-revalidacion/anuncios-polling.json).

**Frontend pertinente: 52/52 pruebas aprobadas**, cuatro archivos, sin retirar ni modificar sus comprobaciones originales. [Log](fase-5-evidencias/5.B3-revalidacion/frontend-tests-inicial.log), [salida 0](fase-5-evidencias/5.B3-revalidacion/frontend-tests-inicial-exit.json). Lint/TypeScript y build aprobados: [lint](fase-5-evidencias/5.B3-revalidacion/frontend-lint.log), [build npm](fase-5-evidencias/5.B3-revalidacion/frontend-build-final.log). Cobertura no repetida; **67,35 % global es la última medición histórica**, objetivo adicional del 80 % pendiente, sin atribuir un porcentaje nuevo a esta ejecución.

Navegador: barrido pertinente **5 aprobados y 1 fallido de 6**, por H05. Complemento conservado: Reintentar pasa; menú falla; conflicto registró contraste transitorio y pasa al repetir solo ese escenario esperando habilitación y finalización natural de la transición. Sin cambiar CSS, desactivar axe ni ampliar tolerancias. La aserción de visibilidad del menú conserva el fallo, aunque se recoge con `expect.soft` para registrar también Roles y cierre de sesión. **No se declara Playwright completamente aprobado.** [Barrido](fase-5-evidencias/5.B3-revalidacion/resultados-iniciales.json), [complemento](fase-5-evidencias/5.B3-revalidacion/resultados-complemento.json), [meta estable aprobada](fase-5-evidencias/5.B3-revalidacion/meta-estable.log).

Revisión visual y de teclado asistida por agente, separada de axe, en escritorio y móvil emulado; [37 pasos](fase-5-evidencias/5.B3-revalidacion/manual-pasos.json). No hay lector de pantalla en esta ejecución ni verificación de su locución real, control por voz o hardware móvil. Las capturas y árbol ARIA acreditan únicamente las comprobaciones descritas.

Entorno detenido, puertos cerrados, acceso temporal retirado y sin secretos detectados por patrones en artefactos públicos nuevos. [Conservación y parada](fase-5-evidencias/5.B3-revalidacion/resumen-final.json), [estado final previo a parar](fase-5-evidencias/5.B3-revalidacion/estado-final.json). Sin Google, bases compartidas, borrado de volúmenes ni escrituras Git. H05, aceptación del polling, meta propuesta y diferencia con los 500 originales pendientes. A6/A9/A10 bloqueadas, A1–Fin no acreditado, 3.B3 Google pospuesto y fases abiertas; detección automática desactivada.

**Corrección del remanente de H05 (parte A, 2026-10-21), pendiente de revalidación por Codex:** en móvil, el menú pasa de una tira con desplazamiento horizontal a varias líneas (`Sidebar.tsx`). "Bodegas" y "Roles" ya no deberían quedar recortados al recibir foco. No se ejecutaron pruebas ni builds, y H05 sigue **abierto** hasta repetir el reproductor. El detalle está en el plan de la fase 5, "Remanente de H05". `Header.tsx` no se modificó.

## Cierre del remanente H05 — revalidación 2026-10-21

**H05 cerrado en el alcance solicitado.** [Comandos, comprobaciones, evidencias y límites](fase-5-evidencias/5.B3-H05-menu/README.md). Esta entrada sustituye el estado pendiente anterior; conserva el reproductor original y las evidencias de su fallo.

A **390 y 1440 px**, los ocho enlaces se recorren con **Tab y Shift+Tab**, con texto y anillo de foco completos: 16 posiciones por anchura. En móvil, Bodegas ocupa x=8…114,70 y Roles x=8…92,64, en líneas nuevas; a 1440 se conserva la columna lateral. Se comprobaron margen de 3 px del anillo y ausencia de recorte por antecesores; documento de 390/1440 px sin overflow horizontal. Los ocho enlaces se activan con Enter y dejan la URL/aria-current correctas. Cerrar sesión es alcanzable, con anillo completo, y Enter lleva a `/login` en ambas anchuras. [390 px](fase-5-evidencias/5.B3-H05-menu/resultado-390.json), [1440 px](fase-5-evidencias/5.B3-H05-menu/resultado-1440.json), [Bodegas](fase-5-evidencias/5.B3-H05-menu/390-Tab-Bodegas.png), [Roles en recorrido inverso](fase-5-evidencias/5.B3-H05-menu/390-ShiftTab-Roles.png), [cierre de sesión](fase-5-evidencias/5.B3-H05-menu/cerrar-390.png).

**2/2 recorridos Playwright aprobados**, [log final](fase-5-evidencias/5.B3-H05-menu/browser-foco-final.log), [salida 0](fase-5-evidencias/5.B3-H05-menu/browser-foco-final-exit.json). **32/32 pruebas frontend afectadas aprobadas** en App, SesionProvider y DashboardRol; sin editar sus comprobaciones originales. [Log](fase-5-evidencias/5.B3-H05-menu/frontend-tests.log), [salida 0](fase-5-evidencias/5.B3-H05-menu/frontend-tests-exit.json). Se conserva la ejecución inicial con una aserción prematura del cierre HTTP ficticio; se adaptó únicamente la espera asíncrona, sin cambiar la condición. La revisión visual asistida por agente confirma el foco en las capturas; no es una prueba de lector de pantalla o dispositivo físico.

**Sin backend ni base de datos**: servidor estático frontend y dobles HTTP explícitos para sesión/consultas. La activación acredita navegación del frontend, no integración real ni permisos del servidor. Ningún pedido creado. Se compiló el frontend para servir los assets aislados; no se repitieron suites backend, H01–H04, cobertura ni carga de 500. Producción, Header.tsx, índice Git, reproductor original y comprobaciones de H01–H04 conservados por hashes. [Conservación y parada](fase-5-evidencias/5.B3-H05-menu/resumen-final.json). Servidor detenido y puertos cerrados; sin Google ni escrituras Git.

Se mantienen los límites y estados de H01–H04, la aceptación pendiente del polling, meta propuesta, objetivo adicional del 80 % frontend y diferencia con los 500 del manual. La última cobertura global 67,35 % sigue siendo histórica. A6/A9/A10 bloqueadas, A1–Fin no acreditado, 3.B3 Google pospuesto, detección automática desactivada y fases abiertas. Cerrar H05 no cierra ninguna fase.

## Selección e índice para guardar el avance — 2026-10-21

Por autorización expresa de Richard, se prepara el índice únicamente con las **797 rutas revisadas** de fase 5, excluyendo Header.tsx, secretos, temporales y cambios ajenos. [Inventario por grupos y límites](fase-5-inventario.md), [selección exacta](fase-5-seleccion-final.txt), [revisión de secretos y exclusiones](fase-5-revision-seleccion.json), [resultado del índice, check y stat](fase-5-indice-comprobacion.json). Solo se fuerza la inclusión de los 66 logs ignorados revisados; las evidencias originales permanecen intactas. La correspondencia exacta y las referencias se registran en el resultado de comprobación; un estado pendiente no equivale a preparación terminada.

No se repiten pruebas: se conservan sus resultados locales aprobados y sus límites. No se ejecutan commit ni push. A6/A9/A10 bloqueadas, A1–Fin no acreditado, aceptación del polling y meta pendientes, objetivo adicional de cobertura frontend y diferencia con 500 intactos. 3.B3 Google pospuesto; fases 3, 4 y 5 abiertas.

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

### Preparación del índice posterior a V16 — 2026-10-21

Selección revisada en fase-5-v16-inventario.md y fase-5-v16-seleccion.txt: implementación, V16, pruebas, documentación y evidencias relacionadas. Preparación del arnés clasificada aparte; 3.B3 permanece pospuesto, sin operaciones Google. Se mantienen las fases abiertas, meta de días pendiente, objetivo adicional frontend ≥80 % pendiente, límite físico temporal y diferencia frente a 500. No se repiten pruebas ni se modifica producción. No se realiza commit, push ni merge.

