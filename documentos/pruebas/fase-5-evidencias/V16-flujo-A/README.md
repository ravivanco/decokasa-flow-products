# Revalidación V16 y flujo A — 21 de octubre de 2026

Resultado local aprobado en `fase-5-seguimiento`. No cierra fases. Producción y Header.tsx conservados; no se escribió el índice Git. [Conservación, límites y parada](conservacion-final.json).

## Decisiones y alcance

Se aplican las decisiones confirmadas de Richard recogidas en los planes de fases 3–5: A6 exige Packing List, permite otros adjuntos; A9 exige bodega activa y al menos un documento de cualquier tipo; A10 exige al menos un documento y confirma Fin automáticamente. No se sustituyen esas reglas con fixtures. Los bloqueos documentales anteriores de A6/A9/A10 quedan superados por V16 y esta evidencia local, no por una omisión de requisitos. Los plazos de A5/A7/A10 siguen PENDIENTES y la meta de días sigue propuesta, sin aceptación inferida. El polling comprobado es de 30 segundos, no transporte instantáneo ni aceptación del cliente.

Máximo autorizado: 20 pedidos. La API real creó siete pedidos ficticios, sin insertar pedidos ni redefinir transiciones mediante SQL. Las suites backend usaron bases aisladas Testcontainers y control opt-in de reinicio de contextos: máximo observado 11 pedidos por base ([conteos](conteos-backend.jsonl)). No se repitió la carga histórica de 500; el objetivo original del manual no se declara cumplido ni retirado. PostgreSQL 17 propio, almacenamiento aislado y flags Google/automatismos desactivados. [Entorno inicial](entorno.json), [estado final](estado-final.json), [tmpfs](tmpfs.json). El listener separa bases entre métodos; no borra pedidos ni modifica aserciones.

## Resultados y comandos

Desde `backend`, con las integraciones y automatismos desactivados, semilla desactivada, importación de configuración privada vacía y almacenamiento temporal aislado:

```powershell
.\gradlew.bat -I ..\documentos\pruebas\fase-5-evidencias\V16-flujo-A\v16.init.gradle check --offline --console=plain --no-daemon
```

**851/851**, cero fallos, errores o saltos; check, arquitectura y umbral backend aprobados. [Log final](backend-check-final.log), [salida](backend-check-final-exit.json), [JUnit](final-junit/), [JaCoCo](final-jacoco.xml), [métricas exactas](metricas-finales.json). Cobertura de líneas global 5359/5871 = **91,28 %**; archivos 1391/1611 = **86,34 %**; plazos 448/471 = **95,12 %**. No equivale a 80 % de ramas: global de ramas 72,67 % y archivos 63,84 %.

Desde `frontend`:

```powershell
npm.cmd run lint
npm.cmd run coverage -- --maxWorkers=1 '--coverage.include=src/**/*.{ts,tsx}' --coverage.reporter=json-summary --coverage.reporter=json --coverage.reporter=text --coverage.reportsDirectory=../documentos/pruebas/fase-5-evidencias/V16-flujo-A/cobertura-frontend-final
npm.cmd run build -- --outDir ../documentos/pruebas/fase-5-evidencias/V16-flujo-A/.tmp/build-final
```

Lint y build salida 0; **324/324 pruebas en 25 archivos**. [Lint](frontend-lint.log), [pruebas finales](frontend-test-cobertura-final.log), [build](frontend-build.log), [cobertura](cobertura-frontend-final/coverage-summary.json). Líneas globales de todo `src`: 1372/2038 = **67,32 %**. El objetivo adicional frontend ≥80 % sigue pendiente; no se excluyó código, redujeron umbrales ni añadieron pruebas al demo desconectado. Se conservan avisos de bundle grande, configuración futura de Vite y petición MSW sin manejador en los logs; no se presentan como defectos de producción nuevos.

Desde `documentos`, con la API y servidor estático aislados del arnés opt-in:

```powershell
node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test --config pruebas\fase-5-evidencias\V16-flujo-A\playwright.config.mjs
```

Dos escenarios distintos acreditados: recorrido completo y actualización entre sesiones. La ejecución inicial conserva un error de selector del arnés en A10 (el panel usa Aprobar; Confirmar y finalizar es el diálogo), no un defecto del producto. El escenario de polling pasó allí y no se repitió sin causa. Se corrigió solo el test, conservando las comprobaciones; el recorrido pasó y volvió a pasar al ampliar integridad en A9/A10. [Inicial sin ocultar fallo](flujo-inicial.log), [revalidación](flujo-revalidacion.log), [integridad final](flujo-integridad-final.log), [operaciones HTTP](operaciones.json), [historial y estado final](recorrido-real.json), [polling real](dos-sesiones.json). Los JSON `resultados*.json` conservan el primer intento: no son el resultado final combinado.

## Matriz acreditada

- V16 sobre base vacía y V15 con pedido esperando en A6, documentos e historial anteriores: filas, timestamps, versiones, entradas y append-only conservados; Flyway valida sin reaplicar. Nueva `MigracionesV16IT`; expectativas antiguas actualizadas a 16 conservando validación de esquema, restricciones y datos.
- A6 sin documento y solo BL: 422; Packing List sola y con BL adicional permiten avanzar. El historial conserva ambos adjuntos cuando se presentan.
- A9 sin bodega, sin documento, con bodega inactiva o documento de otra etapa: rechazo. Bodega activa más documento genérico permite A10.
- A10 sin documento o documento ajeno: rechazo; documento válido cierra pedido y A11, registra exactamente un PEDIDO_FINALIZADO del sistema con usuario nulo; acciones manuales posteriores devuelven 409 sin cambios.
- Documentos de otro pedido, etapa y entrada anterior rechazados. Bytes ausentes y bytes corruptos manteniendo el tamaño se comprueban en A6/A9/A10, con restauración de los archivos ficticios; cada rechazo conserva pedido e historial. El control aislado de bytes no existe en producción.
- Recorrido real A1–Fin incluye rechazo, devolución por error, devolución por producto nuevo, nuevas entradas y adjuntos, demora, llegada, bodega e historial. No es una ruta desbloqueada mediante fixture de definición. Se mantienen además las pruebas backend aisladas históricas de atomicidad, concurrencia y orden de eventos.
- Contrato `exigeAlgunDocumento` y pantallas A9/A10 coherentes: requisito visible, sin banner de definición pendiente, confirmación impedida sin documento y adjunto real seleccionado. Capturas [A9 sin](pantalla-A9-sin.png), [A9 con](pantalla-A9-con.png), [A10 sin](pantalla-A10-sin.png), [A10 con](pantalla-A10-con.png), [contrato](contrato-flujo.json).
- Dos sesiones independientes admin/logística actualizan A7→A8 sin recarga mediante polling real de 30000 ms: 30060 ms hasta comprobar ambas actualizadas, sin reloj simulado. No se declara actualización instantánea.

## Adaptaciones y límites

El primer check falló por expectativas anteriores de versión 15 y bloqueos documentales PENDIENTES. Se conservan [fallos iniciales](baseline-fallos.json), [JUnit inicial](baseline-junit/), [fallo intermedio](adaptacion-fallos.json). Se adaptaron exclusivamente siete suites backend existentes y una expectativa de nombre accesible frontend que ahora incluye la fecha; se añadió la suite de migración V16. No se modificó producción ni se retiraron comprobaciones válidas. Los fixtures históricos de reglas ficticias siguen explícitos y separados del recorrido real. No se detectó un defecto de producción reproducible nuevo en este alcance.

Los servicios API/static/control de esta verificación se detuvieron; puertos 43184/43185/43186 cerrados, acceso temporal eliminado, evidencias y bytes ficticios conservados. No se usaron bases compartidas ni se borraron volúmenes. La revisión de patrones de tokens/llaves en evidencias públicas no detectó secretos; no sustituye la guarda de exclusión de `.tmp` para un futuro inventario. No preparar este directorio recursivamente en Git: `.tmp` contiene generados y archivos de prueba innecesarios.

## Continuación 3.B3 preparada, sin ejecución real

Richard aprobó exigir exactamente `fase-5-seguimiento` en el lanzador existente. Se cambió solo esa comparación: rama correcta salida 0, rama ficticia ajena salida 1; [evidencia](b3-rama-resultados.json), [cambio aprobado](b3-ajuste-rama.json). No se alteraron las frases personales ni las guardas del Java.

```powershell
# Desde backend; segundo init redirige evidencias para conservar las anteriores.
.\gradlew.bat -I ..\documentos\pruebas\arnes-b3\b3.init.gradle -I ..\documentos\pruebas\fase-5-evidencias\V16-flujo-A\b3-evidencias.init.gradle b3ComprobarGuardas b3Classpath --offline --console=plain --no-daemon
```

**8/8 guardas aprobadas**, classpath preparado con el código vigente. [Log](b3-preparacion.log), [JUnit](b3-guardas-junit/TEST-com.decokasa.ensayob3.GuardasB3Test.xml). Esto no acredita OAuth ni servicios Google. No se iniciaron los servicios reales del ensayo.

La continuación está limitada a UN archivo ficticio de **104857600 bytes** en EC/pedido de ensayo y UN correo de `importadora.latinoamericaworld@gmail.com` al mismo buzón. No copiar los adjuntos del recorrido ni procesar su outbox. El plan general no amplía esta autorización. Las confirmaciones y contraseña deben introducirse personalmente en consola real, sin compartirlas; el agente no las omite ni las escribe por Richard.

Para abrir esa consola, desde la raíz del repositorio:

```powershell
powershell.exe -NoProfile -File .\documentos\pruebas\arnes-b3\arnes-b3.ps1 -Accion Iniciar -DirectorioEnsayo 'C:\Users\Richard\.decokasa\ensayos\3b3\c465a284-f1aa-4b0f-aacd-d77cc3328147'
```

Al solicitarlo: `INICIAR ENTORNO AISLADO 3.B3` y contraseña privada de b3-admin. Antes de arrancar debe estar libre 8080 y las variables de automatismos deben seguir desactivadas; no detener otros procesos sin autorización. En el menú, `autorizar` requiere `AUTORIZAR GOOGLE SOLO 3.B3`; **entonces** Richard completa el consentimiento Google en el navegador con el buzón aprobado (nunca entrega su contraseña al agente). Callback fijo `http://localhost:8080/api/v1/integraciones/google/oauth2/callback`, cliente web, acceso offline y tokens cifrados externos. Modo En pruebas: tener presente la caducidad de siete días aplicable al refresh token para estos permisos. Si hay error, detener y conservar evidencia, sin ampliar permisos ni reintentar correo ciegamente.

La copia y el correo exigen sus frases personales independientes. El marcador de correo ya invocado impide repetirlo, incluso ante resultado incierto. Gmail aceptado no acredita recepción: Richard debe confirmarla. `salir` detiene el contexto, OAuth y activaciones temporales; conservar directorio, volumen y evidencias sanitizadas. Schedulers y detección automática permanecen apagados; no hay envío general.

3.B3 real sigue sin ejecutar ni aprobar. Fases 3, 4 y 5 abiertas; meta de días y límites de aceptación restantes no se resuelven por esta preparación. Sigue explícita la limitación de espacio físico temporal: las cuotas lógicas no garantizan capacidad libre del disco durante parciales/copias.
