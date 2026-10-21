# Inventario revisado de fase 5 — 2026-10-21

Rama `fase-5-seguimiento`. Selección explícita de **797 rutas**: 731 normales y **66 logs ignorados revisados**, únicos que admiten `git add -f`. Índice inicialmente vacío; no se descartarán cambios ni se alterarán evidencias originales.

| Grupo | Rutas |
|---|---:|
| codigo_y_configuracion | 94 |
| documentacion | 14 |
| evidencias | 675 |
| migraciones | 2 |
| pruebas | 12 |

[Selección completa](fase-5-seleccion-final.txt), [normales](fase-5-seleccion-normal.txt), [logs revisados](fase-5-seleccion-ignorados.txt), [revisión](fase-5-revision-seleccion.json), [comprobación del índice y avisos](fase-5-indice-comprobacion.json).

Código de cronograma, meta propuesta, vencimientos, motivos, outbox, historial, polling, dashboard/búsqueda y correcciones verificadas; V14 y V15; pruebas nuevas y adaptaciones de suites existentes; plan/PRD, arquitectura, ADR, OpenAPI y modelo; informe, arneses opt-in, métricas/JUnit/JaCoCo, logs, capturas y trazas necesarias para conservar el histórico y los reproductores. Los históricos de 500 se guardan sin ejecutarlos ni equipararlos al escenario aprobado de hasta 20. Las copias de scripts de entrega son evidencias, no una segunda implementación de producción.

Las tres eliminaciones del demo están expresamente previstas en el plan (5.3). Los demás prototipos ajenos se conservan. Se excluyen Header.tsx y .vscode preexistentes, evidencias de fases anteriores no pertenecientes a esta entrega, derivadas, archivos privados, .tmp, dist, credenciales de acceso y doce .bin internos de JUnit. Se conservan sus XML legibles. No se excluye un archivo por contener un correo de ejemplo o una ruta local necesaria para diagnóstico.

Revisión de secretos: sin patrones reconocibles de credenciales en la selección, incluidas trazas gzip. Siete asignaciones señaladas revisadas: generación aleatoria o lectura del acceso temporal ficticio y DB de prueba; no contienen credenciales persistentes. Los correos hallados son ejemplos de prueba. Los PNG corresponden a capturas ficticias documentadas en las entregas; no se afirma un escaneo OCR exhaustivo. El manual y el informe de fase 4, enlazados desde el informe de fase 5, ya están rastreados: no se vuelven a incluir ni modificar.

Las 162 referencias locales iniciales del informe existen. Se comprueba también que los destinos estén en esta selección o ya en Git, para que no dependan de archivos locales omitidos. El informe conserva todos los estados históricos y los cierres posteriores; no se reescriben las evidencias antiguas.

Las pruebas locales aprobaron según sus ejecuciones documentadas; no se repitieron. H01–H05 cerrados en los alcances acreditados, sin afirmar locución real. Permanecen cobertura global frontend adicional del 80 %, aceptación del polling, meta propuesta y diferencia con los 500 del manual. A6/A9/A10 bloqueadas, A1–Fin no acreditado, 3.B3 Google pospuesto y fases 3/4/5 abiertas. OAuth/workers/schedulers/detección automática desactivados.

El JSON de comprobación registra `git diff --cached --check`, `--stat`, coincidencia exacta y avisos de conversión de línea. Los avisos de espacios de logs/XML se informan sin modificar originales. No se ejecutan commit ni push.

Comando reservado a Richard después de revisar:

```powershell
git commit -m "feat(seguimiento): guardar avance y validación local de fase 5"
```
