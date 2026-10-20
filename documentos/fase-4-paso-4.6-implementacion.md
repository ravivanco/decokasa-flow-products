# Fase 4 · parte A · paso 4.6

Entrega de implementación del 19 de octubre de 2026. **Pendiente de validación por Codex; la fase permanece abierta.** Esta entrega conecta pantallas con la API existente, sin modificar el backend, las migraciones o las pruebas. No acredita compilación ni funcionamiento mediante ejecución.

## Alcance implementado

- **Bandejas:** se reutiliza `BandejaPedidos`, con los pedidos devueltos por `/pedidos/bandeja` y su paginación. Compras China atiende Fabricación, Embarque y Aduana; Logística, Salida, Bodega e Incidencias, además del alcance anterior de sus roles. El navegador agrupa resultados, sin calcular transiciones ni filtrar por un único rol. Los nombres de las secciones proceden de los pedidos. Un aviso identifica los requisitos documentales pendientes de la definición vigente; el detalle utiliza la versión concreta del pedido.
- **Demora:** el diálogo existente solicita observación y fecha civil mediante un campo de fecha. Envía `REPORTAR_DEMORA`, `etapaEsperada`, `nuevaFechaEstimadaLlegada` y los IDs de adjuntos seleccionados de la entrada actual. La comprobación de fecha válida, sus restricciones y el ajuste del fin de semana pertenecen al servidor. No se calcula una nueva etapa ni un vencimiento en el navegador. El detalle presenta la estimación confirmada, motivo, actor y hora; el historial conserva las fechas anterior, solicitada y efectiva cuando existen.
- **Llegada:** la etiqueta «Confirmar llegada» depende de `registraLlegada` de la definición de la etapa y de su acción APROBAR. Se conserva el POST de acciones. La llegada visualizada es `llegadaAduanaEn`, registrada por el servidor; no se permite introducir una hora ficticia.
- **Bodega:** una acción con `requiereBodega` consulta `/bodegas?activa=true` y exige elegir un ID real. Errores de lectura, recarga o una selección que ya no está disponible impiden confirmar. Se envía el ID, nunca un nombre inventado o una asignación local. El servidor comprueba nuevamente permisos, estado activo, asignación única y concurrencia. Se muestra la bodega confirmada, aunque posteriormente quede inactiva.
- **Catálogo real:** nueva pantalla `/bodegas` con búsqueda por nombre/ciudad, tabla, estados de carga/error/vacío, creación, edición y activación/desactivación por API. La navegación y la pantalla permiten el mantenimiento a admin o Logística, incluida la unión de roles; POST/PATCH conservan la autorización vigente del servidor. El PATCH envía la versión leída al abrir el formulario. Ante `VERSION_DESACTUALIZADA`, se conserva el borrador, se recarga el catálogo y se exige cerrar/revisar la versión actual antes de guardar. No se ofrece borrado. El componente anterior de catálogo demo permanece fuera de las rutas activas.
- **Incidencias y Fin:** se presentan A10 y sus acciones devueltas por el backend. El detalle y el historial muestran la finalización real y el actor «Sistema». La definición automática aporta la etiqueta de confirmación del cierre; `finalizado` y las acciones del servidor mandan. Fin no incorpora botones manuales ni una confirmación adicional.
- **Documentos, resúmenes y plazos:** se reutilizan los archivos reales por etapa/entrada, la carga por bloques, los estados de copia, la descarga por backend y el resumen del servidor. No se generan adjuntos ficticios. Los requisitos `PENDIENTES` se presentan incluso en paneles de consulta y bloquean la confirmación; una lista de documentos vacía no resuelve esa decisión. Los plazos pendientes del detalle se distinguen de `SIN_PLAZO`. Las listas cuyo contrato no incluye `estadoPlazo` no infieren ese estado a partir del número de etapa.

## Consultas y conflictos

Las claves de bodegas incluyen sesión y filtro `activa`; las consultas admiten cancelación con `AbortSignal`. No se añaden reintentos automáticos de mutaciones ni cambios optimistas del negocio. Crear o editar una bodega invalida catálogo y consultas de pedidos de esa sesión, también tras errores o conflictos.

Las acciones y reasignaciones conservan la actualización del detalle con la respuesta del servidor y la invalidación de historial, archivos, listas, bandejas y registro de notificaciones. Las observaciones ahora reutilizan esa misma actualización. Ante errores se relee el estado vigente; no se reenvía automáticamente la operación. Un cambio de etapa o pedido finalizado impide reenviar el formulario abierto. Los rechazos 403 bloquean su confirmación e indican revisar la sesión. La selección de bodega se refresca también tras un rechazo.

La visibilidad del menú es una ayuda; no concede permisos. Se mantienen la sesión, la autorización del backend y las reglas de asignación existentes. No se incorporan llamadas a Google desde el navegador.

## Diseño y revisión de parte A

Se leyeron el manual, el plan, OpenAPI y [DESIGN.md](diseno/DESIGN.md). Se aplicaron `shadcn`, `tanstack-query-best-practices` e `impeccable`: componentes Radix existentes, etiquetas asociadas, títulos de diálogos, estados pendientes y errores, conservación del borrador ante conflictos, texto largo adaptable, tabla con desplazamiento horizontal y jerarquía tipográfica y colores semánticos del proyecto. No se añaden dependencias, tokens de diseño o fuentes externas.

La revisión es estática. El detector mecánico de impeccable se registra a continuación; no sustituye validación visual, accesibilidad o pruebas de interacción. `Header.tsx`, DESIGN.md, el backend y las pruebas se conservan mediante comparación de huellas con el estado inicial. Los resultados previos del informe de fase 4 corresponden al código anterior y no certifican 4.5 ni 4.6.

Evidencia: [revisión estática de entrega](pruebas/fase-4-evidencias/entrega-4.6-revision-estatica.json). Se publicaron 15 archivos frontend y se actualizó el plan en la rama `fase-4-etapas-a5-a11`. La comparación de huellas confirma que Header, backend, pruebas, índice, DESIGN y archivos de dependencias conservaron su contenido inicial; no se encontraron cambios adicionales de fuente fuera de la selección de 4.6.

Comando desde `frontend`:

```powershell
& 'C:\Users\Richard\.codex\skills\impeccable\scripts\impeccable.cmd' detect --json src/components/CatalogoBodegas.tsx src/components/pedidos/DialogoAccion.tsx src/components/pedidos/DetallePedido.tsx src/components/pedidos/PanelEtapa.tsx src/components/pedidos/HistorialPedido.tsx src/components/pedidos/BandejaPedidos.tsx src/components/pedidos/Distintivos.tsx src/components/Sidebar.tsx src/components/dashboards/DashboardRol.tsx
```

Salida `[]`, código 0. La revisión `git diff --check`, limitada a las 15 rutas frontend publicadas y al plan, también terminó con código 0; Git avisó que LF se sustituirá por CRLF cuando toque `api/pedidos.ts`, `Sidebar.tsx` y `rutas.tsx`. No se normalizaron archivos para retirar esos avisos. Una búsqueda acotada de patrones de claves/tokens en los archivos publicados no encontró coincidencias; no representa una certificación de seguridad.

No se ejecutaron pruebas, lint, compilaciones, builds, migraciones ni operaciones Google. No se iniciaron servicios ni se activaron OAuth, workers o schedulers. Sin comandos Git de escritura.

## Bloqueos que permanecen

- F4-D4, F4-D7 y F4-D8: documentos de A6, A9 y A10 todavía pendientes. No hay recorrido real completo hasta Fin con la definición vigente.
- Plazos pendientes, incluido Incidencias, sin inventar vencimientos. No se introducen detalles comerciales de productos fuera de los archivos.
- F4-H01: orden relativo de eventos con la misma hora. El historial respeta el orden recibido, sin corregirlo ni darlo por aceptado.
- Avisos específicos de demora/Fin y destinatarios pendientes no se resuelven desde estas pantallas.
- Integración real 3.B3 pospuesta por decisión del usuario. Se mantiene explícita la limitación del espacio físico temporal de almacenamiento documentada en fase 3.

## Casos para Codex · pendientes, no ejecutados

1. Adaptar los fixtures existentes al contrato vigente: `requiereBodega` en acciones y `registraLlegada` en definiciones, además de metadatos reales de bodega/demora cuando corresponda. No hacer opcionales esos indicadores obligatorios para ocultar fixtures antiguos ni duplicar suites. Ejecutar lint, test y build del frontend en la parte B.
2. Bandejas con roles China, Logística, admin, solo consulta y combinaciones de roles: solo pedidos autorizados por el servidor, agrupación, paginación, estados vacíos/errores y recarga después de cada operación. Comprobar que la definición vigente no sustituya la versión del detalle.
3. Demora: observación y fecha requeridas; envío de cadena AAAA-MM-DD; mostrar la fecha confirmada ajustada por el servidor y su historial. Repetir fechas imposibles/con hora/arreglo en la API y ausencia de mutación ante 400 Problema. No presentar la estimación como vencimiento.
4. Llegada únicamente donde `registraLlegada` lo indique. El navegador no elige la siguiente etapa ni fabrica fecha/hora. Verificar rechazo del servidor al actuar desde otra etapa.
5. Bodegas activas: lectura vacía, error, recuperación, selección, desactivación concurrente, asignación única y errores del servidor sin éxito local. Catálogo: admin/Logística y roles múltiples; rechazo directo del servidor a otros roles, cuenta desactivada o rol revocado. Nombres duplicados y límites de campos, versión antigua y conservación del borrador sin reenvío automático.
6. A6/A9/A10: aviso documental visible con y sin acciones permitidas, sin forma de omitirlo mediante Enter, adjuntos o una lista vacía. Plazo pendiente distinguido de ausencia de plazo. Resúmenes y archivos de entradas anteriores no satisfacen documentos de la entrada actual; parciales no aparecen como adjuntos válidos.
7. A10 → Fin solo con fixtures explícitos y aislados que permitan probar reglas posteriores al bloqueo documental, sin cambiar la definición vigente ni presentar el caso como recorrido completo. Confirmación, cierre, historial de sistema, lectura tras recarga y ausencia de acciones manuales, observaciones/subidas/reasignación sobre el pedido finalizado. No imponer un orden relativo mientras F4-H01 esté abierto.
8. Conflictos durante un diálogo, doble pulsación, 403 y respuestas perdidas: recarga de las consultas afectadas, sin avance optimista ni reenvío automático. Comprobar que un cambio de sesión no reutilice datos de otra persona.
9. Archivos reales: progreso por bloques, pausa/reanudación, pendiente/copiado/fallido, enlace solo confirmado y descarga por backend. Usar dobles de Google; no activar el ensayo 3.B3.
10. Validación visual y de teclado en móvil/escritorio: etiquetas, foco de diálogos, botones durante envío, mensajes de error, fechas, nombres largos y tablas. Conservar Header.tsx y DESIGN.md. Ejecutar las regresiones de 4.5 y medir cobertura actual, sin atribuirles resultados históricos.

La entrega no cierra 4.6 como verificada ni la fase 4.
