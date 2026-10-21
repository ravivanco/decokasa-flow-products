# Revalidación de CLS y consultas — máximo 20 pedidos

Resultados vigentes en [informe](../../fase-5-informe.md). H01/H02 cerrados para este escenario, objetivo original de 500 no acreditado ni retirado. Producción y Header.tsx sin modificar. Evidencia anterior de 500 intacta.

Arnés opt-in, PostgreSQL Testcontainers tmpfs exclusivo, API y frontend compilados reales. `.tmp` contiene builds y marcadores de parada; no preparar su contenido para Git. Se retiró el acceso temporal. No hay OAuth/Google/workers/schedulers habilitados. Los tres servicios ya están detenidos.

Ocho escenarios de navegador iniciales, uno dirigido de estados y repetición precisa del caso CLS: nueve escenarios únicos. CLS frío/caliente final usa CDP con 300 ms, caché HTTP habilitada; la serie `cls-carga-con-routing.json` es diagnóstico con caché desactivada, no comparación fría/caliente. SQL 11 constante en usuarios 4/20/60 y tamaños 1/5/20; proyección una sentencia, conjunto vacío cero. 24 JUnit y 57 Vitest aprobados, lint y build dirigidos aprobados. Ver logs y JSON para condiciones exactas.

Las fuentes `revalidacion.spec.mjs`, `b2.init.gradle` y `java/` documentan lo ejecutado. No ejecutar automáticamente ni sobrescribir los resultados; para repetir, usar otro directorio de evidencia y los mismos límites. `b2.spec.mjs` es la base adaptada no seleccionada por el runner; solo se ejecutó `revalidacion.spec.mjs`. Sin comandos Git de escritura, sin avance a 5.B3 ni cierre de fases.
