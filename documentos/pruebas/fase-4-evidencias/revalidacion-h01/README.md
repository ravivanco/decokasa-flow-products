# Revalidación aislada de F4-H01 · 2026-10-20

Rama: `fase-4-etapas-a5-a11`. Fuente de decisiones: sección «Corrección de F4-H01» del plan y del informe de fase 4. La migración y producción son las recibidas, sin modificaciones durante esta revalidación.

`ejecutar.ps1` crea almacenamiento temporal UUID, impide recurrir a una base compartida mediante una URL inválida, desactiva las integraciones/automatismos Google y la semilla, y ejecuta Gradle. Las bases las suministran exclusivamente PostgreSQL 17 Testcontainers. No usa reutilización de bases, Flyway clean, Docker prune ni borrado de volúmenes.

```powershell
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\ejecutar.ps1' baseline
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\ejecutar.ps1' dirigido
& 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\ejecutar.ps1' check
& 'C:\Users\Richard\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' 'C:\Users\Richard\Desktop\decoWeb\documentos\pruebas\fase-4-evidencias\revalidacion-h01\medir.py'
```

Los metadatos JSON fechados registran comando interno, salida y almacenamiento de cada ejecución; los logs no se sobrescriben. `medir.py` debe ejecutarse después del check que se quiere conservar, pues copia sus JUnit/JaCoCo y compara las huellas iniciales. El estado definitivo y las cifras están en `final-metricas.json` y en `documentos/pruebas/fase-4-informe.md`, no se deducen del nombre de las carpetas.

- `baseline-junit`: expectativas anteriores de V12/UUID, con fallos conservados.
- `dirigido-junit`: suites adaptadas de migración, orden, API y resumen.
- `check-previo-junit`: primer check completo, anterior a adaptar la comparación de fila histórica de `MigracionesArchivosB1IT` a la columna `secuencia` nula. No terminó la tarea JaCoCo; no se le atribuye cobertura.
- `dirigido-jacoco.xml`: reporte de la ejecución dirigida, sesión 09:46:58–09:47:57 de Ecuador. Era el XML disponible al terminar el primer check fallido; se identificó por `sessioninfo` y se conserva como dirigido, sin confundirlo con cobertura global del check.
- `final-junit`, `final-jacoco.xml`, `final-metricas.json`: ejecución completa final, cuando haya terminado.
- `produccion-inicial-sha256.json`: producción backend, frontend completo, configuración, contrato, plan e índice iniciales, sin contenido de credenciales.

La suite `HistorialSecuenciaH01IT` necesita un contexto propio: inserta sus datos ficticios bajo V12 mediante una estrategia Flyway de prueba y después aplica V13 antes de arrancar JPA. No elimina ni desactiva restricciones, no inserta secuencias nulas después de V13 para fabricar el legado y no modifica V1–V13. Sus pedidos son estados de prueba posteriores al bloqueo documental, no un recorrido A1–Fin.

Desempate verificado: hora descendente; con la misma hora, los eventos nuevos se ordenan por secuencia descendente. Los antiguos conservan NULL y UUID descendente. En una mezcla con la misma hora, PostgreSQL coloca NULL al final en ASC y la API invierte: primero el grupo antiguo por UUID descendente y después el nuevo por secuencia descendente. Es una convención estable, no una reconstrucción causal. El resumen usa el mismo lector y coincide con ese orden. Una hora posterior prevalece sobre ambos grupos.

La secuencia no se expone en JSON, no exige números consecutivos y no representa orden global de commit entre pedidos distintos. Los rollbacks pueden consumir valores sin dejar eventos confirmados. La prueba concurrente de pedidos distintos comprueba unicidad, no inventa una garantía de orden de commit.

No se usan Google real o cuentas reales; 3.B3 permanece pospuesto. Los documentos pendientes de A6/A9/A10 y los plazos pendientes no se cambian. Las fases 3 y 4 permanecen abiertas incluso si F4-H01 se cierra por evidencias.
