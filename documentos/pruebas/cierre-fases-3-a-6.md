# Preparación del cierre e integración de fases 3 a 6

Fecha de revisión: 2026-10-24. Revisión documental y de referencias Git locales; no se ejecutaron suites ni se consultó el remoto. Este documento no registra una validación de Richard ni autoriza producción o merges.

## Estado comprobado

| Fase | Evidencia disponible | Qué falta para cerrar |
|---|---|---|
| 3: Google Drive y Gmail | Pruebas locales con dobles aprobadas; cuotas lógicas y reintentos verificados. | 3.B3 real: reparación del arranque del arnés, consentimiento, copia de un archivo ficticio de 104857600 bytes y confirmación de recepción del correo. Mantener visible B3-SEG-01, límite físico de temporales, retención y destinatarios definitivos. |
| 4: etapas A5 a A11 | F4-P01 y F4-H01 cerrados; V16 y recorrido real local A1–Fin acreditados en el informe de fase 5. Los documentos de A6/A9/A10 ya están definidos. | Consolidar el cierre con V16 presente y validación expresa de Richard para desarrollo, conservando plazos/calendario provisionales. La punta original de fase 4 no contiene V16. |
| 5: seguimiento | Calendario y alertas con dobles, polling de 30 s y accesibilidad comprobados dentro del alcance documentado; defectos revalidados. | Meta de días pendiente; 90 no aprobado. Resolver expresamente el alcance de aceptación con hasta 20 pedidos frente al objetivo original de 500. Registrar límites de accesibilidad y aceptación provisional del polling. El 80 % frontend es objetivo adicional, no el umbral backend del manual. |
| 6: flujo B | 878 pruebas backend, 91,30 % de cobertura; 329 pruebas frontend y recorridos críticos A/B aprobados en las ejecuciones documentadas. Guía y skill revisadas, H01/H02 documentales cerrados. | Validación expresa de Richard y decisión sobre ADR-0019, todavía proposed. Conservar pendiente el soporte de nuevas versiones con etapas diferentes en listas/tableros, los plazos pendientes y P-14/P-10. |

Las métricas anteriores pertenecen a las ejecuciones y fuentes de sus respectivos informes, no al código de fase 7 sin verificar. No se han repetido pruebas en esta revisión.

## Evidencias de referencia

- [Fase 3](fase-3-informe.md): verificación local, límites de almacenamiento y 3.B3 pospuesto.
- [Fase 4](fase-4-informe.md): pruebas de reglas, F4-H01 y actualización posterior por V16.
- [Fase 5](fase-5-informe.md): revalidaciones, accesibilidad y sección «Revalidación V16 y recorrido real A1–Fin».
- [Fase 6](fase-6-informe.md): 6.B1, 6.B2, 6.B3 y revisión de entregables documentales.
- [ADR-0019](../adr/0019-habilitacion-por-definicion.md): habilitación por definición; propuesta pendiente de aceptación.

## Referencias Git inspeccionadas

| Rama | Punta local | Contenido relevante |
|---|---|---|
| develop | 6caa56c | Fase 2 integrada. |
| fase-3-drive-gmail | 977301a | Avance local de fase 3; 3.B3 pospuesto. |
| fase-4-etapas-a5-a11 | d06cc53 | Hereda fase 3; todavía sin V16. |
| fase-5-seguimiento | 512d499 | Hereda fases 3/4 e incorpora V16 y recorrido A1–Fin. |
| fase-6-flujo-b | 07f2df3 | Hereda las anteriores e incorpora B y verificaciones A/B. |
| fase-7-colombia-ia | 07f2df3 | Rama actual; cambios sin confirmar de Colombia/IA, fuera de este cierre. |

Las referencias origin/* locales coincidían con las puntas de las ramas 3 a 6 al inspeccionarlas; no equivale a una actualización mediante fetch.

## Integración propuesta, pendiente de aprobación

1. Preservar primero los cambios sin confirmar de fase 7 y el cambio ajeno de Header.tsx. No cambiar de checkout ni hacer stash/reset/descarte de forma implícita. Para reparar o revisar las fases anteriores se puede trabajar en un checkout aislado desde la referencia apropiada.
2. Resolver 3.B3 o aprobar expresamente una integración parcial con Google desactivado. Integrar código parcial y cerrar una fase son decisiones diferentes: una excepción de integración no acredita la prueba real.
3. Integrar en orden de dependencia: fase 3, fase 4, fase 5 y fase 6. Después de cada merge, actualizar la comparación del PR siguiente contra develop. No incorporar por accidente los cambios sin verificar de fase 7.
4. No registrar la fase 4 como acreditada en la punta d06cc53: su criterio de recorrido depende de V16, que llega con fase 5. Consolidar ese cierre después de integrar el conjunto dependiente, o coordinar previamente el traslado del cambio y su evidencia sin duplicar migraciones.
5. Las correcciones nuevas deben verificarse en su código concreto. Reutilizar los informes históricos solo cuando corresponden a las mismas entradas; no repetir indiscriminadamente todas las suites ni atribuirles verificación de cambios posteriores.
6. Richard realiza manualmente commits, push y merges. Antes de ello se presentan los archivos concretos, evidencias y pendientes. Mantener main sin cambios hasta una entrega autorizada.

## Decisiones aún necesarias

- Google: completar 3.B3 o mantener el bloqueo y autorizar por separado una integración parcial de desarrollo. No dar por cerrado Drive/Gmail sin evidencia externa.
- Meta: proporcionar una cifra y su interpretación aprobada, o dejar el cronograma como entrega parcial con meta propuesta. No asumir que «mismo calendario» aprueba 90 días.
- Alcance de rendimiento: documentar la aceptación del escenario de hasta 20 pedidos si reemplaza el requisito original; no convertir la evidencia histórica de 500 en una aprobación nueva.
- ADR-0019: aceptar o solicitar cambios; no convertir proposed a accepted por terminar pruebas.
- Validación de cierre de desarrollo de las fases elegibles, con pendientes expresos y sin autorización implícita de producción.

No se modificaron código, informes originales, estados de ADR, índice Git, ramas, credenciales ni configuraciones. Las pruebas de fase 7 quedan en pausa por la petición de resolver primero las fases anteriores.

## Trabajo de la parte A sobre los pendientes — 2026-10-24, sin pruebas ni ejecución

Rama de trabajo comprobada: `fase-7-colombia-ia`, con la fase 7 ya guardada en `0fb061d` e índice vacío. Ningún archivo tocado tiene cambios de la fase 7: los seis archivos rastreados son idénticos en `fase-6-flujo-b`, `fase-5-seguimiento` (los del arnés) y `HEAD`, así que el parche se puede llevar a otra rama sin arrastrar la fase 7. `Header.tsx`, informes originales, estados de ADR, producción, migraciones y pruebas sin cambios.

| Fase | Cambio | Archivos |
|---|---|---|
| 3 | **Corregido** el arranque del arnés al reutilizar PostgreSQL: espera `pg_isready` en lugar de dos mensajes "ready", sin `fsync=off`, y guarda de credenciales y datos. Sin compilar; lo verifica Codex | `arnes-b3/java/.../ArnesGoogleB3.java`, `GuardasB3.java`, `arnes-b3/README.md`, plan de la fase 3 |
| 4 | Consolidación de las evidencias vigentes de A1–Fin frente a la puerta del manual; F4-P01 y F4-H01 siguen cerrados | [fase-4-consolidacion-A1-Fin.md](fase-4-consolidacion-A1-Fin.md) (nuevo) |
| 5 | Decisiones de meta de días y alcance de 20 pedidos presentadas con opciones; sin fijar cifra ni retirar los 500 | plan de la fase 5, "Decisiones para el cierre de la fase 5" |
| 6 | Límite de versiones en listas y tableros delimitado, **no corregido** | plan de la fase 6, "Delimitación del límite de versiones" |

**No hechos, a propósito:**
- Estado de ADR-0019, índice de ADR y `CLAUDE.md`: esperan la decisión de Richard. El índice y `CLAUDE.md` también cambiaron en la fase 7, y editarlos ahora complicaría el commit separado.
- Sincronización documental del outbox de ADR-0012 (aprobado técnicamente el 2026-10-16, según 6.B3): por el mismo motivo, se hará junto con ADR-0019.
- Cambio de la guarda de rama del lanzador del arnés (sigue exigiendo `fase-5-seguimiento`).
