# 5.B3 — accesibilidad y regresión local

Ejecución 20–21 de octubre de 2026, rama `fase-5-seguimiento`. API real, PostgreSQL 17 Testcontainers con PGDATA tmpfs y almacenamiento propio; frontend compilado. Máximo 20 pedidos ficticios. Sin Google ni procesos automáticos. Los fixtures no sustituyen A1–Fin.

## Comandos y aislamiento

Desde `documentos`, con Testcontainers local y las integraciones automáticas desactivadas:

```powershell
$env:TESTCONTAINERS_RYUK_DISABLED='true'
& ..\backend\gradlew.bat -p ..\backend -I ..\documentos\pruebas\fase-5-evidencias\5.B3\b2.init.gradle f5B3 --offline --console=plain --no-daemon
node pruebas\fase-5-evidencias\5.B3\servidor.mjs
node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test --config pruebas\fase-5-evidencias\5.B3\playwright.config.mjs
node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test --config pruebas\fase-5-evidencias\5.B3\playwright.config.mjs --grep '04 documentos' --reporter=list
node pruebas\fase-5-evidencias\5.B3\inspector-manual.mjs
```

El inspector recibe comandos JSON por stdin: los 32 pasos exactos y sus capturas están en `manual-pasos.json`. Solo usa credenciales ficticias aleatorias del arnés, retiradas al parar. No conservar `.tmp`, storageState, HAR ni trazas privadas. El build real de Vite se ejecuta desde `servidor.mjs` antes de servir, con salida exclusiva `.tmp/dist`; resultado en `frontend-build.log`. Axe-core 4.14.0 instalado en `documentos/.tmp/f5b3-axe`, fuera del proyecto frontend; package.json y lockfiles intactos.

Desde backend, con configuración privada desactivada, OAuth/copia/correo/detección automáticos false, credenciales ficticias y almacenamiento temporal exclusivo:

```powershell
.\gradlew.bat -I ..\documentos\pruebas\fase-5-evidencias\5.B3\b2.init.gradle check --offline --console=plain --no-daemon
```

Desde frontend:

```powershell
npm.cmd run lint
npm.cmd run coverage -- --maxWorkers=1 '--coverage.include=src/**/*.{ts,tsx}' --coverage.reporter=json-summary --coverage.reporter=json --coverage.reporter=text --coverage.reportsDirectory=../documentos/pruebas/fase-5-evidencias/5.B3/cobertura-frontend-confirmada
$codigoCobertura = $LASTEXITCODE
exit $codigoCobertura
```

Se conserva explícitamente el código nativo final 0 en `frontend-exit.json`: Windows PowerShell etiquetó avisos stderr de Node como NativeCommandError en ejecuciones anteriores, pese al resumen aprobado. No se ocultaron fallos de Vitest.

## Resultados

- Backend: **849 pruebas, cero fallos/errores/omitidas**, check PASS, incluidas verificación JaCoCo y fronteras; 5m40s. Líneas globales **5340/5846 = 91,34 %**; archivos **1401/1623 = 86,32 %**; plazos **448/471 = 95,12 %**. `backend-resumen.json`, `backend-check.log`, `jacoco-backend.xml`.
- Frontend: **324 pruebas aprobadas en 25 archivos**, lint/tsc y build PASS. Cobertura global de todo src **1362/2022 líneas = 67,35 %**, ramas 64,22 %, funciones 70,97 %, statements 67,21 %. El **86,80 %** de los archivos cargados por las pruebas es solo diagnóstico, no cobertura global. Objetivo adicional del 80 % pendiente, sin exclusiones nuevas ni cambios de umbral/configuración. `frontend-coverage-confirmada.log` y `cobertura-frontend-confirmada/coverage-summary.json`.
- Se ajustaron exclusivamente dos fixtures en `AccionesPedido.test.tsx`: el POST devolvía A2/urgencia pero el GET posterior del mock conservaba A3/A4. Ahora guardan la misma respuesta en el estado ficticio. Todas las aserciones permanecen, sin cambios de producción. Ejecuciones fallidas y posteriores conservadas; no se atribuyen esos fallos de mock al servidor.
- Build con aviso de chunk >500 kB; aviso de deprecación de configuración Vite. Advertencias MSW de cronograma en ejecuciones anteriores conservadas, no silenciadas como evidencia de calidad.
- Primer barrido Playwright: **2 escenarios aprobados, 4 fallidos de 6**. Tres fallan por axe, sin relajar sus aserciones. El cuarto esperaba incorrectamente 422 para el bloqueo documental: el contrato es **409 TRANSICION_NO_PERMITIDA**. Se corrigió solo esa expectativa nueva, se añadió comprobación del detalle de Problema y se repitió: A6/A9/A10 bloqueadas, historial idéntico. Resultado combinado: **3 aprobados, 3 fallidos por accesibilidad**; no se presenta E2E como verde. `resultados.json`, `bloqueos-revalidacion.log`, `bloqueos.json`.
- Antes de fallar por axe, los escenarios completaron: meta 0 inválida, foco al input, conflicto real 409 y borrador 77 conservado; reentrada, un solo envío al ID original, 409, recuperación sin enviar a la nueva entrada; pestañas con flechas/Home/End, un tabIndex=0, aria-selected y controles/tabpanel; línea con aria-current y selección; Fin automático sin acciones manuales; polling sin robo de foco ni anuncios vacíos repetidos.

El generador tiene límite duro 20. El listener opt-in de las suites renueva contextos Spring cuando acumulan ocho pedidos, sin borrar filas durante pruebas ni cambiar aserciones: 286 mediciones al final de método, máximo observado 11. No es vigilancia continua de cada inserción ni instrumenta bases de migración sin bean Spring; esas suites usan fixtures pequeños, no la carga histórica de 500.

## Reproductores abiertos para revisión y corrección

| ID | Gravedad y reproducción | Evidencia / alcance |
|---|---|---|
| F5-B3-H01 | **Seria según axe, clasificación normativa pendiente de revisión.** Abrir detalle y ejecutar axe: `label-content-name-mismatch` en botones de línea. Escritorio 11 nodos por detalle; móvil A6/A8/A10: 7/5/3 visibles. Comparar texto visible con aria-label en `LineaDeTiempo.tsx`, incluidos círculo numérico, código, título y fecha/estado. | `axe-detalle-*.json`, manual-25. Los números redundantes y concatenación de spans pueden influir: no declarar todos los nodos incumplimientos confirmados por una persona ni silenciar la regla. Falta contraste con control por voz y revisión normativa de fechas omitidas. |
| F5-B3-H02 | **Seria.** Detalle a 390 CSS px, navegar solo con Tab: contenedor `div[data-slot=table-container]` de cronograma con overflow horizontal sin foco ni descendientes enfocables. No permite llegar al desplazamiento por teclado. | `scrollable-region-focusable` en tres detalles móviles. Repetir con tabla ancha y teclado tras corregir. |
| F5-B3-H03 | **Seria.** A conserva meta 77; B cambia servidor a 81; guardar A produce 409. Los dos botones outline del conflicto: #d16a6a sobre #fafbfc, 14 px normal, **3,38:1**, esperado 4,5:1. | `axe-meta-conflicto.json`, `meta-formulario.json`. Formulario inicial/error no presenta esta infracción. Sin cambios CSS. |
| F5-B3-H04 | **Moderada, brecha de anuncio a validar con lector real.** Dashboard enfocado; otra sesión aprueba A7→A8; polling cambia fila sin cambiar foco. Observadores de live/status/alert: cero anuncios, igual que sin cambios. | `anuncios-polling.json`. No demuestra por sí solo incumplimiento 4.1.3: decidir mensaje de estado apropiado y validar con NVDA/VoiceOver, evitando anuncios de cada sondeo vacío. |
| F5-B3-H05 | **Moderada, shell preexistente.** A 390 px documento de 456 px: encabezado/navegación recortados; comprobar acceso visible a cerrar sesión y navegación. | manual-05 y manual-pasos. `Header.tsx` conservado; no atribuirlo a cambios de seguimiento. |

## Automatización frente a revisión manual

Automatizado: Edge/Chromium headless, 1440 y 390 CSS px; axe-core 4.14.0, etiquetas WCAG 2/2.1/2.2 A/AA y best-practice, sin excluir reglas. Diez estados base: dashboard, A6/A8/A10 y meta por dos anchos. Dashboard/meta inicial sin infracciones detectadas; barridos adicionales error, conflicto de meta y borrador. Se conservan `incomplete`: cero violaciones no acredita accesibilidad completa.

Manual **asistido por agente**, separado de axe: comandos individuales de teclado y juicio visual sobre capturas; no una persona con lector de pantalla. Revisadas manual-05, -12, -19, -21, -25 y -32: foco naranja distinguible, navegación→contenido, pestañas, selección de etapa, error visible/foco al input, textarea y botón; nombres, estados y pendientes legibles en texto, además de colores/iconos. Los 32 pasos no son 32 flujos independientes. Capturas inmediatas tras navegar pueden mostrar carga; las aserciones automatizadas esperan el contenido.

Límites: móvil emulado, sin dispositivo físico, lector de pantalla, control por voz, zoom real de SO ni alto contraste. No se certifica auditoría exhaustiva de todos los colores/estados. Anuncios se inspeccionaron en DOM, no salida hablada. No se presenta axe como sustituto de estas comprobaciones pendientes.

## Parada y conservación

Puertos 43184/43185/43186 cerrados y acceso ficticio temporal retirado. Estado previo, tmpfs y parada en `estado-final.json`, `tmpfs.json`, `parada.txt`, `parada-docker.json`; Testcontainers retiró su contenedor al salir. Sin borrado manual de volúmenes ni recursos compartidos. `resumen-final.json`: solo cambió el test mencionado respecto a los hashes iniciales; producción, Header, índice Git e históricos B2 intactos. Escaneo por patrones sin JWT/claves privadas/secretos OAuth en los artefactos nuevos públicos; no certificación absoluta. `.tmp` no es evidencia para guardar.

**5.B3 ejecutado con hallazgos abiertos; ninguna fase cerrada.** A6/A9/A10 bloqueadas; A1–Fin no aprobado; fixtures no lo sustituyen. Siguen pendientes aceptación del polling periódico, meta propuesta y diferencia con el objetivo original de 500 del manual: no cumplido ni retirado por inferencia, históricos intactos y no repetidos. Fase 3 abierta, su 3.B3 Google real pospuesto; fases 4 y 5 abiertas. OAuth/workers/schedulers/detección automática desactivados. Se conservan limitación de espacio físico temporal y pendientes anteriores.
