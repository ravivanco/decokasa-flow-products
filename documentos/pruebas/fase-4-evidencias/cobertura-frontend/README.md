# Evidencias de comportamiento frontend F4 (2026-10-20)

Resultado final: 312 pruebas aprobadas, lint y build aprobados. Cobertura global 65,82 % (1202/1826), objetivo 80 % pendiente. Ocho módulos priorizados F4: 97,65 %; no sustituye la global. No hubo backend ni Google.

El detalle, alcance y los fallos intermedios están en [el informe](../../fase-4-informe.md#ampliación-de-comportamiento-frontend-y-cobertura-2026-10-20). Los logs originales se conservan, incluidos los fallidos; solo `20261020-103233-coverage-*` es la medición final. La ejecución fallida de las 10:30 no produjo contadores válidos.

Desde documentos: `.\pruebas\fase-4-evidencias\cobertura-frontend\ejecutar.ps1 coverage` (o `dirigido`, `lint`, `build`). El ejecutor usa únicamente frontend y fija guardas apagadas. Requiere dependencias locales ya instaladas. No instala, autoriza ni inicia servicios.

Desde la raíz: `python .\documentos\pruebas\fase-4-evidencias\cobertura-frontend\analizar-cobertura.py` analiza los contadores conservados, sin excluir archivos ni alterar configuración. Registra los subconjuntos explícitos, el grafo estático y la conservación por hashes. No imprime secretos. Rutas absolutas de los contadores son metadatos locales del instrumento; no contienen credenciales.

Producción, Header, backend e índice conservados. A6/A9/A10 bloqueadas; 3.B3 pospuesto; fases 3 y 4 abiertas. No se acredita integración real ni recepción de correos.
