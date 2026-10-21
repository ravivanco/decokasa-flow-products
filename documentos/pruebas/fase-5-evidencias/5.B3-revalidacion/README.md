# Revalidación H01–H05 de 5.B3 — 2026-10-21

Rama `fase-5-seguimiento`. Aplicadas las skills ecc-accessibility y playwright-best-practices. Solo revalidación de las correcciones descritas en el plan; producción, Header.tsx, índice Git y evidencias anteriores conservados por hashes. No se ejecutaron suites backend ni carga de 500.

## Resultado por hallazgo

| Hallazgo | Resultado y decisión | Evidencias |
|---|---|---|
| H01 | **Cerrado en el alcance comprobado.** Axe no detecta `label-content-name-mismatch`; los once botones obtienen nombre del contenido, con código/título y estado una sola vez, sin aria-label sustitutorio ni círculo numérico duplicado. Ejemplo del árbol: `A8 Autorización de salida , etapa actual`. El espacio antes de coma no duplica palabras. Se conservan las comprobaciones de etapas completadas en DetallePedido.test.tsx. | `axe-detalle-A6-1440.json` y equivalentes A8/A10 × 1440/390; `nombres-scroll-menu.json`; 52 pruebas pertinentes aprobadas. No se verificó locución ni control por voz reales. |
| H02 | **Cerrado.** Región del cronograma alcanzable con Tab, nombre accesible y anillo naranja visible. A 390 px tiene ancho cliente 322 y contenido 704; ArrowRight desplaza de 0 a 40 px. A 1440 el contenido cabe y el foco sigue disponible. Sin infracciones de región desplazable en axe. | `nombres-scroll-menu.json`, `cronograma-teclado-390.png`, revisión manual-08 a -17. |
| H03 | **Cerrado para el contraste estable y los estados comprobados.** Ambos botones habilitados del conflicto: texto rgb(30,27,46) sobre rgb(248,249,251), **15,93:1**. Conserva 409, borrador 77 y un único PUT, sin guardado automático. “Reintentar” examinado en meta, cronograma y bandeja, con y sin foco: cero infracciones axe. No se extrapola a todas las alertas de la aplicación. | `contraste-meta.json`, `colores-conflicto.json`, `axe-meta-conflicto.json`, `meta-estable.log`, `axe-reintentar-normal-*.json`, `axe-reintentar-*.json`. |
| H04 | **Cerrado como defecto de emisión DOM.** Tres ciclos de lectura periódica: cero avisos sin cambios, exactamente uno al cambiar A7→A8 desde otra sesión, y ningún aviso adicional tras otro sondeo idéntico. Foco intacto; `role=status` no urgente. | `anuncios-polling.json`, escenario 05 aprobado en `resultados-iniciales.json`. **Locución de lector de pantalla no verificada**; no se afirma conformidad integral ni aceptación del transporte por el cliente. |
| H05 | **Permanece abierto, parcialmente corregido.** Documento de 390 px a viewport 390, encabezado refluye y cerrar sesión es visible, alcanzable y funciona con Enter. Menú todavía recorta controles al recibir foco: Bodegas x=307,30→414; Roles x=361,02→445,66. El scroll interno no los revela completamente al llegar con Tab. | `menu-parcial-390.json`, `nombres-scroll-menu.json`, `manual-25.png`, fallo estricto del escenario 07. Sin cambios de producción. |

H05: gravedad **moderada**, pendiente para Claude. Reproductor: admin ficticio, viewport 390, foco en Inicio del menú; Tab tres veces hasta Bodegas y después hasta Roles. Capturar rectángulo, contenedor visible y anillo recortado. No confundir ausencia de overflow del documento con visibilidad completa del control enfocado. La prueba sigue fallando; no se amplió la tolerancia ni se ocultó el recorte.

## Ejecuciones y ajustes del arnés

- Frontend: **52/52 pruebas** en cuatro archivos: DetallePedido, AccionesPedido, Fase4Pantallas y DashboardRol. Todas sus comprobaciones originales pasan sin editar esas suites. `frontend-tests-inicial.log` y `frontend-tests-inicial-exit.json` (0).
- `npm run lint`: PASS, TypeScript; `frontend-lint.log` y `frontend-lint-exit.json` (0).
- `npm run build -- --outDir ../documentos/pruebas/fase-5-evidencias/5.B3-revalidacion/.tmp/build-final --emptyOutDir false`: PASS; `frontend-build-final.log` y `frontend-build-exit.json` (0). Acceso local desactivado. También se compiló la salida aislada servida por el navegador. Se conservan avisos de Vite y chunk grande.
- Navegador, seis escenarios pertinentes: **5 aprobados y 1 fallido** (07, menú móvil). Diez estados base con axe 4.14.0, WCAG A/AA 2/2.1/2.2 y best-practice sin exclusiones: dashboard, A6/A8/A10 y meta × escritorio 1440/móvil 390. `resultados-iniciales.json`, `browser.log`, `browser-exit.json`.
- Complemento: tres escenarios, uno aprobado (Reintentar), dos fallidos (menú y contraste transitorio); `resultados-complemento.json`, `browser-complemento.log`. En el conflicto se midió **4,13:1 durante la habilitación/transición** de los botones; se conserva `axe-meta-conflicto-transitorio.json`. Al repetir solo 03 esperando botones habilitados y finalización natural de animaciones finitas, sin cambiar CSS ni desactivar axe, pasa: `meta-estable.log`, `meta-estable-exit.json` (0). El cierre de H03 se refiere al estado estable; esta observación transitoria queda visible para revisión, no se atribuye a contraste persistente ni se borra.
- El escenario 07 pasó de aserciones inmediatas a `expect.soft` para recoger también Roles, cierre de sesión y ancho del documento después del primer recorte: conserva las mismas desigualdades y continúa fallando. No se presenta la batería Playwright como verde. El archivo `resultados.json` corresponde al complemento, no al barrido inicial; ambos se conservan con nombres explícitos.
- Se reforzó 05 para exigir cero/un solo/cero avisos y preservar foco; 07 comprueba nombres, scroll y menú; 08 comprueba contraste normal y con foco en errores HTTP sintéticos. Se conservan las comprobaciones de comportamiento de tabs, Fin sin acciones y conflicto/borrador de meta de los escenarios heredados. No se ejecutaron 04/06 porque quedan fuera de esta revalidación.

La cobertura no se volvió a medir: la última medición global documentada es **67,35 %**, histórica y objetivo adicional del 80 % pendiente. Los 849 tests/check backend anteriores siguen siendo evidencia histórica, no una ejecución de este turno. Arrancar la API compiló el arnés y cargó el backend; no ejecutó sus suites.

## Automatizado frente a manual

Automatizado: navegador Edge headless y API real aislada; sin mocks de negocio en los reproductores de meta/polling. Solo el escenario Reintentar induce respuestas HTTP 500 con Playwright; esas son evidencias **simuladas de error**, no fallos reales del servidor. Conserva `incomplete` de axe (algunos textos recortados de línea/panel), que no corresponde a los botones Reintentar; cero infracciones no certifica toda la página.

Revisión manual **asistida por agente**: 37 comandos individuales de teclado y juicio visual sobre capturas. Revisadas manual-04 (cierre móvil visible), -12 (scroll/foco móvil), -17 (foco escritorio), -25 (menú recortado), además del conflicto y Reintentar. `manual-pasos.json` registra la secuencia; Enter sobre Meta navega y Enter sobre Cerrar sesión vuelve a `/login` (manual-36/-37). Es móvil emulado, no hardware físico. El campo `foco.nombre` del inspector usa textContent, por lo que incluye aria-hidden; **no es el nombre accesible**. Para éste se utiliza el árbol ARIA de `nombres-scroll-menu.json` y consultas por rol.

No hay lector de pantalla disponible en esta ejecución; no se acredita locución NVDA/VoiceOver ni control por voz. Se comprueba emisión DOM y teclado, no una sesión humana con tecnologías de asistencia. Tampoco se certifican zoom físico de SO, alto contraste o todas las combinaciones de alertas.

## Reproducción y parada

Desde documentos, Docker local disponible, Testcontainers exclusivo y Google/tareas desactivados por el arnés:

```powershell
$env:TESTCONTAINERS_RYUK_DISABLED='true'
& ..\backend\gradlew.bat -p ..\backend -I ..\documentos\pruebas\fase-5-evidencias\5.B3-revalidacion\b2.init.gradle f5B3Revalidacion --offline --console=plain --no-daemon
node pruebas\fase-5-evidencias\5.B3-revalidacion\servidor.mjs
node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test --config pruebas\fase-5-evidencias\5.B3-revalidacion\playwright.config.mjs --grep '01 axe|02 teclado|03 formulario|05 polling|07 nombres|08 contraste'
node pruebas\fase-5-evidencias\5.B3-revalidacion\inspector-manual.mjs
```

Desde frontend:

```powershell
npm.cmd test -- --run --maxWorkers=1 src/components/pedidos/DetallePedido.test.tsx src/components/pedidos/AccionesPedido.test.tsx src/components/pedidos/Fase4Pantallas.test.tsx src/components/dashboards/DashboardRol.test.tsx
npm.cmd run lint
npm.cmd run build -- --outDir ../documentos/pruebas/fase-5-evidencias/5.B3-revalidacion/.tmp/build-final --emptyOutDir false
```

Para detener se crearon únicamente los archivos de control `.tmp/detener` y `.tmp/detener-front`; los servicios cerraron y el inspector se cerró. PostgreSQL tmpfs exclusivo, 20 pedidos, sin operaciones Google ni borrado manual de volúmenes. Acceso temporal retirado, puertos 43184/43185/43186 cerrados. `estado-final.json`, `tmpfs.json`, `parada.txt`, `resumen-final.json`. `.tmp` no es evidencia para guardar. Escaneo por patrones sin JWT, claves privadas ni secretos OAuth en artefactos públicos nuevos; no certificación absoluta.

No se modificó producción ni Header, no hubo escrituras Git. Persisten A6/A9/A10 bloqueadas, A1–Fin no aprobado, aceptación del polling, meta propuesta, diferencia con 500 del manual, cobertura y demás pendientes anteriores. Fases abiertas, 3.B3 Google pospuesto y detección/OAuth/workers/schedulers desactivados.
