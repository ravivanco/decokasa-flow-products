# Fase 4: evidencias vigentes del recorrido A1–Fin

Consolidación documental de la parte A (2026-10-24). **Sin pruebas, builds ni servicios nuevos**: solo enlaza evidencias ya registradas y dice a qué código corresponden. No reabre defectos cerrados, no sustituye los informes originales y **no cierra la fase 4**: el cierre lo valida Richard.

## A qué código corresponden las evidencias

El recorrido A1–Fin depende de V16 (documentos de A6, A9 y A10), que llegó con la fase 5. La punta de `fase-4-etapas-a5-a11` (`d06cc53`) no contiene V16, así que **no sirve como referencia del cierre de la fase 4**. Las evidencias vigentes corresponden a:

| Evidencia | Rama y código | Qué acredita |
|---|---|---|
| [Revalidación V16 y recorrido A1–Fin](fase-5-evidencias/V16-flujo-A/README.md), 2026-10-21 | `fase-5-seguimiento` (V1–V16) | Check backend 851/851, frontend 324/324 y recorrido real A1–Fin con API y navegador |
| [6.B2 `@critico A`](fase-6-evidencias/6.B2/critico-A.json) ([resumen](fase-6-evidencias/6.B2/resumen-final.json)), 2026-10-21 | `fase-6-flujo-b` (V1–V18) | El recorrido A sigue verde junto al flujo B, con el motor compartido de ADR-0019 (proposed) |

Ninguna de las dos acredita el código de la fase 7 (V19–V21, sin verificar).

## Puerta de salida de la fase 4 (manual) frente a la evidencia

| Criterio del manual | Evidencia | Estado |
|---|---|---|
| La prueba @critico del flujo A pasa de A1 a Fin | V16: [recorrido real](fase-5-evidencias/V16-flujo-A/recorrido-real.json), [operaciones](fase-5-evidencias/V16-flujo-A/operaciones.json) y [log final](fase-5-evidencias/V16-flujo-A/flujo-integridad-final.log). 6.B2: `@critico A`, 3/3 junto con B ([resultados](fase-6-evidencias/6.B2/browser-05-resultados.json)) | Acreditado localmente en los dos códigos |
| Embarque sin BL y Bodega sin bodega elegida se rechazan | **Bodega:** A9 sin bodega, con bodega inactiva o sin documento → rechazo (matriz de V16). **Embarque:** la regla vigente es la decisión de Richard del 2026-10-21 (A6 exige "Packing List"; BL opcional). V16 acredita A6 sin documento → 422 y A6 solo con BL → 422 | Bodega: acreditado. Embarque: acreditado **con la regla vigente**, que difiere del texto del manual. Pendiente de que Richard confirme la nueva redacción (ver más abajo) |
| Una demora en aduana mueve todas las fechas siguientes | El recorrido real registra la demora (`demoraAduana.nuevaFechaEstimadaLlegada`). El recálculo del cronograma lo cubren `Fase5CronogramaTest` y `Fase5IT` (fase 5, 5.B1: llegada estimada, prioridad de la real y propagación) | Acreditado para las etapas con plazo definido. Los plazos de A5, A7 y A10 siguen PENDIENTES del cliente: el sistema no estima lo que depende de ellos y no inventa fechas |
| Fin se marca solo | V16: A10 con documento válido cierra A10 y A11 y registra un único `PEDIDO_FINALIZADO` del sistema; una acción manual posterior responde 409 sin cambios. 6.B2: lo mismo, junto con B | Acreditado localmente |

## Defectos de la fase 4: cerrados, no se reabren

- **F4-P01** (fecha de demora en arreglo JSON): cerrado el 2026-10-19 ([informe](fase-4-informe.md), "Revalidación de F4-P01").
- **F4-H01** (orden de eventos en empate): cerrado el 2026-10-20 ([informe](fase-4-informe.md), "Revalidación F4-H01"), con el límite explícito del desempate antiguo.
- **Bloqueo documental de A6, A9 y A10:** resuelto por las decisiones de Richard del 2026-10-21 y V16. No era un defecto del producto.

Ninguna evidencia posterior (V16, 6.B1, 6.B2 ni 6.B3) registró un defecto nuevo de producción en el flujo A.

## Pendientes que se conservan

- Plazos de A5, A7 y A10: ⏸ pendientes del cliente. Calendario y plazos siguen siendo provisionales.
- Meta de días: ⏸, ver la decisión de la fase 5.
- Objetivo adicional de cobertura frontend ≥ 80 %: pendiente (67,32 % en V16 y 68,33 % en 6.B2). No es el umbral backend del manual, que sí se cumple (91,28 % y 91,30 %).
- Google real (3.B3): pospuesto. El recorrido no copia a Drive ni envía correos.

## Qué falta para cerrar la fase 4

1. Que Richard confirme la redacción del criterio de Embarque: "Embarque sin **Packing List** se rechaza; el BL es opcional".
2. Que Richard valide el cierre de desarrollo con estos pendientes expresos, sobre una referencia que contenga V16 (`fase-5-seguimiento` o posterior), sin autorización implícita de producción.
