# Selección posterior al último commit de fase 5

Base: `053f282e93752ca92bbcd6f36d02eabcdceba2dd`. Rama `fase-5-seguimiento`.

[Selección exacta](fase-5-v16-seleccion.txt) y [revisión por rutas](fase-5-v16-revision.json).

| Grupo | Archivos |
|---|---:|
| Evidencias locales y arnes de verificacion | 258 |
| Documentacion y contrato | 14 |
| Arnes 3.B3: preparacion sin ejecucion | 14 |
| Implementacion | 16 |
| Pruebas | 9 |
| Migracion V16 | 1 |

El cambio funcional incorpora V16 y exigeAlgunDocumento; se incluyen pruebas adaptadas, migraciones, contrato, decisiones y evidencias de la revalidación local. El arnés 3.B3 se clasifica aparte: única comparación de rama aprobada, documentación y evidencias de ocho guardas offline, sin activaciones reales. Se excluyen los artefactos anteriores del arnés ajenos a esta entrega.

Las pruebas locales aprobaron: backend 851, frontend 324, lint/build y recorrido API real A1–Fin. Backend líneas 91,28 %, archivos 86,34 %, frontend 67,32 %. No se repiten suites al preparar el índice. 3.B3 permanece pospuesto; meta de días, objetivo adicional frontend ≥80 %, limitación de espacio físico temporal y diferencia con 500 pedidos continúan documentados. Fases abiertas.

No se incluyen Header.tsx, secretos, temporales, archivos reales, trazas binarias, copias de índices, evidencias ajenas de fases anteriores, HTML redundante de guardas ni cobertura detallada redundante; se conserva resumen frontend y JaCoCo/JUnit necesarios. Correos aprobados y rutas de configuración externas son información necesaria, no credenciales. Los datos del recorrido son ficticios.

La revisión de patrones no encontró tokens ni claves privadas; el único candidato de asignación fue revisado: genera una contraseña ficticia aleatoria en ejecución, sin valor secreto literal. No se modifican evidencias originales para eliminar avisos de whitespace. El resultado final de git diff --cached --check y --stat queda en fase-5-v16-indice-comprobacion.json (registro local fuera de esta selección para no producir autorreferencia del índice).
