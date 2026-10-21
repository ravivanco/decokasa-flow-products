# H05: revalidación final del menú — 2026-10-21

**H05 cerrado en el alcance solicitado.** Rama `fase-5-seguimiento`. Corrección de Sidebar ya existente, sin modificar producción ni Header.tsx.

## Comprobaciones

Dos recorridos Playwright aprobados, a **390 y 1440 CSS px**, Edge headless:

- Los ocho enlaces del admin se recorren con Tab y Shift+Tab: **16 posiciones por anchura**, orden conservado.
- Texto y anillo de 3 px completos dentro del viewport y de los antecesores que puedan recortar; foco visible y capturas por cada posición.
- A 390, Bodegas está en x=8…114,70, Roles en x=8…92,64; ambos en nuevas líneas, sin recorte, en ambos sentidos. A 1440 se mantiene la columna lateral, x=8…215.
- Los ocho enlaces se activan con Enter en ambas anchuras: URL correcta y `aria-current=page`. Se prueba navegación del frontend, no datos/autorización del servidor.
- Documento con `scrollWidth` 390/1440 respectivamente: sin overflow horizontal durante el recorrido.
- Cerrar sesión alcanzable por teclado, anillo completo de 5 px, Enter activa el cierre ficticio y vuelve a `/login` en ambas anchuras.

`resultado-390.json`, `resultado-1440.json`, `resultados.json` y `browser-foco-final.log` contienen comprobaciones y resultados. Capturas de cada enlace en cada dirección; revisión visual asistida por agente de `390-Tab-Bodegas.png`, `390-ShiftTab-Roles.png`, `1440-Tab-Roles.png` y `cerrar-390.png`. No se acredita lector de pantalla, hardware móvil ni control por voz; no se comprueba locución real.

**32/32 pruebas frontend aprobadas**: App.test.tsx, SesionProvider.test.tsx y DashboardRol.test.tsx. Sin editar esas suites ni sus aserciones. `frontend-tests.log`, código 0 en `frontend-tests-exit.json`. No se repitieron suites backend, cobertura, carga de 500 ni H01–H04.

## Aislamiento y límites

Solo servidor estático frontend local 43184. **Backend no iniciado, ninguna base ni pedido creado.** Las respuestas de sesión y consultas son dobles HTTP explícitos en el navegador. El servidor estático rechaza `/api/` con 503 y no tiene proxy backend. No es evidencia de integración real ni de autorización del servidor. El build Vite genera únicamente `.tmp/dist`; `frontend-build.log`, aviso de chunk grande conservado. `.tmp` no es evidencia para guardar.

El reproductor original y todas las comprobaciones/evidencias de H01–H04 permanecen intactos en `5.B3` y `5.B3-revalidacion`, comprobados por hashes. No se usó el nuevo entorno con dobles para volver a acreditar esos hallazgos.

Primera ejecución: 390 aprobó; 1440 comprobó los enlaces y la llegada a login, pero la aserción inmediata del cierre HTTP ficticio se adelantó a su resolución. Se conserva `browser.log` y `resultados-iniciales.json`. Se cambió solo a `expect.poll` con la misma condición; luego se añadió espera de la transición CSS natural y aserción explícita del foco en el botón. Sin ocultar errores ni desactivar estilos. Ejecución final: **2/2 aprobadas**, código 0 en `browser-foco-final-exit.json`.

## Comandos

Desde documentos, en una consola para servir frontend:

```powershell
node pruebas\fase-5-evidencias\5.B3-H05-menu\servidor.mjs
```

En otra consola:

```powershell
node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test --config pruebas\fase-5-evidencias\5.B3-H05-menu\playwright.config.mjs
```

Desde frontend:

```powershell
npm.cmd test -- --run --maxWorkers=1 src/App.test.tsx src/auth/SesionProvider.test.tsx src/components/dashboards/DashboardRol.test.tsx
```

Parada mediante `.tmp/detener-front`; servidor detenido, puertos 43184/43185/43186 cerrados. `resumen-final.json`: producción, Header.tsx, índice Git y evidencia anterior sin cambios respecto al inicio. Sin Google, escrituras Git ni recursos compartidos.

H01–H04 conservan el estado y límites de la revalidación anterior. A6/A9/A10 bloqueadas, A1–Fin no acreditado, polling pendiente de aceptación y meta propuesta; objetivo original de 500 y objetivo adicional de cobertura frontend pendientes. La última cobertura global 67,35 % es histórica, no medida aquí. 3.B3 Google pospuesto y fases abiertas; detección automática desactivada.
