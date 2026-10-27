# Fase 1.B2 — navegador contra API real

Requiere Docker, las seis cuentas ficticias de `backend/semilla/usuarios.ejemplo.json`, Playwright con su runner y Chromium. No usa MSW. La contraseña predeterminada es la ficticia de las pruebas, `Clave-Prueba-1`; `B2_CLAVE_FICTICIA` permite otra contraseña exclusivamente ficticia.

Desde la raíz, levantar el entorno conservando el volumen existente:

```powershell
docker compose -f docker-compose.yml -f pruebas-navegador/puertos.compose.yaml -f pruebas-navegador/expiracion.compose.yaml up -d --build --wait
```

El origen principal es `http://localhost:3000` (backend publicado en 18080). El entorno auxiliar de expiración es `http://localhost:3001` (backend 18081), con acceso de 3 s y renovación absoluta de 120 s. Comparte la base existente y el mismo código, con configuración de ejecución diferente. No demuestra un recorrido de ocho horas reales ni sustituye la evidencia de 15 min/8 h del backend. No cambiar duraciones del servicio principal para estas pruebas.

Si Playwright está instalado localmente, ejecutar su CLI con `test --config pruebas-navegador/playwright.config.mjs`. En el equipo de esta ejecución se reutilizaron el runner y Chrome ya instalados, sin modificar `package.json` ni instalar paquetes:

```powershell
$env:PLAYWRIGHT_TEST_MODULE='C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs'
$env:B2_CHROME_PATH='C:/Users/Richard/.cache/puppeteer/chrome/win64-155.0.8059.39/chrome-win64/chrome.exe'
& 'C:/Users/Richard/AppData/Roaming/fnm/node-versions/v24.21.0/installation/node.exe' 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/cli.js' test --config pruebas-navegador/playwright.config.mjs
```

Variables opcionales: `B2_BASE_URL`, `B2_REPORT` y `B2_OUTPUT_DIR`. Los últimos dos separan reportes y capturas entre ejecuciones. La prueba de expiración usa intencionalmente el origen auxiliar fijo 3001.

Los contextos se aíslan por prueba; solo los casos de varias pestañas comparten cookies dentro de un contexto. El conflicto y la prueba de caché crean cuentas ficticias con sufijo único y las desactivan al finalizar, conservando sus filas. La escritura tardía crea un rol ficticio sin asignarlo; se conserva porque el servidor ya confirmó la escritura antes de que el cliente descarte la respuesta. No modifica las seis cuentas base ni elimina usuarios, roles, datos o volúmenes.

El caso de identidad conserva la regresión original: Admin no puede permanecer visible mientras la cookie ya representa Marketing y cambiar silenciosamente al recargar. Con los logins ahora serializados, la espera inicial retiene A, envía la intención de login de B y comprueba que B espera el Web Lock. Libera A antes de retener el envío de B; así puede calentar la caché y abrir un formulario de Admin sin esperar circularmente al dueño del bloqueo. Al liberar B exige Marketing y un aviso explícito **antes** de recargar, permisos correctos, formulario y datos anteriores retirados, coherencia con la API y la misma identidad tras recarga. No se limita a aceptar Marketing después de recargar.

La interceptación solo demora envíos o entrega respuestas obtenidas con `route.fetch()` de la API real, sin fabricar cuerpos, estados ni cookies. Las pruebas de lectura y escritura tardías liberan esas respuestas después de cambiar de cuenta y comprueban que no restauran la UI antigua. Otra prueba deja expirar un JWT real del entorno auxiliar, retiene el 401 real de una escritura, cambia la cuenta y verifica un único POST con Admin y ningún reenvío con Marketing. No hay `skip`, `fixme`, reintentos automáticos ni cambios de producción. Consultar `documentos/pruebas/fase-1-informe.md` para resultados y evidencias históricas y actuales.

Para repetir únicamente las carreras, usar `--grep 'dos pestañas|login distinto' --repeat-each=5` y rutas de reporte nuevas.

Para verificar solo el defecto y sus regresiones reales: `--grep 'login distinto|caché caliente|tardía|operación antigua|dos pestañas|acceso vence'`. Las dos pruebas de expiración requieren los servicios auxiliares; las restantes usan el origen principal. Los reportes históricos de 1.B2 se conservan usando `B2_REPORT` y `B2_OUTPUT_DIR` distintos.

Vitest/Testing Library/MSW se ejecutan por separado desde `frontend/`:

```powershell
$env:VITE_ACCESO_LOCAL='false'
& 'C:/Users/Richard/AppData/Roaming/fnm/node-versions/v24.21.0/installation/node.exe' node_modules/vitest/vitest.mjs run --coverage --reporter=json --outputFile=../.tmp/b2-vitest-cobertura.json
```

El ajuste del proceso de prueba conserva `.env.local`. Si se ejecuta sin él y está habilitado el acceso automático de desarrollo, se prueban otros comportamientos y los tests de autenticación fallan.
