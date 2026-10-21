# Estado actual: escenario principal de hasta 20 pedidos

Decisión expresa de Richard (2026-10-20). El generador está limitado a 20; no se ejecutó de nuevo. CLS y crecimiento SQL corregidos en código, **pendientes de revalidación**. Ver [configuración](escenario-principal.json) e [informe actualizado](../../fase-5-informe.md).

El objetivo de 500 del manual permanece registrado y no se declara cumplido ni retirado por reducir la evaluación. Se conserva la [fuente histórica de 500](historico-500/ArnesB2.java.txt) y todas las evidencias anteriores.

**Los comandos y suites descritos debajo pertenecen a la ejecución histórica. No ejecutarlos como revalidación actual:** las expectativas Playwright de 500 no se modificaron en este encargo sin pruebas. Codex adaptará las comprobaciones a hasta 20 pedidos y guardará evidencias nuevas separadas, sin activar Google ni sobrescribir el histórico.

---

# Verificación aislada 5.B2

Este arnés usa el backend y el frontend reales del repositorio. No usa MSW ni respuestas comerciales inventadas. Todo lo añadido vive en este directorio; el source set de Gradle es opt-in y no modifica `build.gradle`, las suites normales ni producción.

El arranque crea PostgreSQL 17 exclusivo con Testcontainers en tmpfs, aplica V1–V15, inserta cuatro cuentas ficticias y exactamente 500 pedidos EC distribuidos entre A1–A10. A5/A7/A10 tienen plazo PENDIENTE y ninguna fecha de vencimiento; las otras etapas reciben vencimientos ficticios para ejercitar el panel (350 globales, 50 cargados). Son fixtures explícitos de estado, no un recorrido A1–Fin aprobado. Las definiciones documentales reales de A6/A9/A10 no se alteran. Las pruebas realizan mutaciones reales de observación y A7→A8 sobre esos fixtures. El experimento final añade 16 usuarios ficticios inactivos, manteniendo los 500 pedidos.

La API usa `127.0.0.1:43185`, el frontend compilado `43184` y el control de fixtures/SQL `43186`. El control solo escucha en loopback y existe en el proceso de pruebas; no forma parte del producto. No usarlo en un equipo o red compartidos. Las credenciales aleatorias de las cuentas de ensayo viven temporalmente en `.tmp/acceso.json`, ignorado por Git, y el cierre lo retira. Nunca conservar HAR, storageState ni respuestas de login. Traces de Playwright desactivadas: pueden incluir cabeceras. La traza CDP usa categorías de rendimiento sin netlog y se revisa antes de compartir.

OAuth, Drive, Gmail, sus workers y detección automática se fuerzan a false; no hay autorización, subida ni envío real. El arnés 3.B3 permanece ajeno a este trabajo y pospuesto.

## Comandos utilizados

Desde `backend`, PowerShell, en proceso separado:

```powershell
$env:TESTCONTAINERS_RYUK_DISABLED='true'
.\gradlew.bat -I ..\documentos\pruebas\fase-5-evidencias\5.B2\b2.init.gradle f5B2 --offline --console=plain --no-daemon
```

Desde `documentos`, otro proceso:

```powershell
node .\pruebas\fase-5-evidencias\5.B2\servidor.mjs
node C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright\cli.js test -c .\pruebas\fase-5-evidencias\5.B2\playwright.config.mjs
```

La pasada definitiva de diez casos se conserva en `resultados-fixture-corregido.json` y `browser-final.log`: nueve pasaron y uno reprodujo una carrera del arnés al desconectar antes de terminar el refetch de foco. Esperar su finalización mantuvo las aserciones; la repetición dirigida `--grep 'intervalo sin solapamiento' --repeat-each 5` pasó 5/5 (`resultados.json`, `browser-polling-repetido.log`). Las ejecuciones anteriores se conservan y no sustituyen esa revalidación. No confundir el diagnóstico de CLS sin aserción de umbral con una aprobación de estabilidad visual.

El caso de polling entre sesiones espera el intervalo real de 30 s. El caso de política temporal usa reloj Playwright y Page Visibility emulada; todas sus lecturas siguen llegando a la API real. Las respuestas tardías se inyectan reteniendo una respuesta obtenida con `route.fetch()`, conservando cuerpo y código del servidor. No es una API simulada, aunque sí un fallo de transporte inducido.

Para los planes se ejecutó exclusivamente `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)` mediante `docker exec ... psql` contra el ID de `entorno.json`, con el SQL de `planes-exactos.sql`. El plan nativo de bandeja coincide con el SQL capturado, sustituyendo parámetros por fixtures explícitos; las consultas pequeñas de búsqueda son diagnósticos de sus predicados, no el coste completo de montar el DTO.

## Parada y conservación

Se crean `.tmp/detener` y `.tmp/detener-front`. El backend cierra Spring y el servidor de control y pide STOP a su propio contenedor; el frontend cierra su servidor. **Al terminar la JVM, Testcontainers retiró automáticamente los contenedores efímeros**, incluso con Ryuk desactivado. La ausencia posterior está documentada: no afirmar que se conservaron como contenedores detenidos. La pasada definitiva usa tmpfs para PGDATA y no tiene montajes de volumen Docker (`tmpfs.json`, `montajes.json`). No se ejecutó `docker rm`, `prune`, `down -v` ni borrado manual de volúmenes; no se usaron bases o volúmenes compartidos. Los datos de fixture de esas bases eran efímeros; las evidencias y el almacenamiento local del ensayo se conservaron.

Los marcadores de parada siguen presentes. Antes de una futura repetición hay que archivar las evidencias actuales y renombrar esos dos marcadores; no iniciar por accidente ni reutilizar las métricas anteriores. El arnés no es un mecanismo de backup de PostgreSQL.

`conservacion-final.json` acredita los tres puertos cerrados, ausencia del acceso temporal, hashes intactos de 494 archivos protegidos y escaneo sin tokens en los artefactos revisados. El diagnóstico de una iteración del arnés incluyó un JWT ficticio por el mensaje de `route.fetch`; se retiró de los archivos nuevos afectados. No se tocaron evidencias de fases anteriores.

No preparar `.tmp`, clases/builds generados, hashes de contraseñas, credenciales ni dumps de base para Git. No se ejecutó ningún comando Git de escritura. Fases abiertas; 3.B3 pospuesto; 5.B3 no iniciado.
