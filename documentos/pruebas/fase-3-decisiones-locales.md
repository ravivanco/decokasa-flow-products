# Fase 3 — decisiones provisionales del equipo

**Actualización vigente, 2026-10-19:** Richard aprobó provisionalmente para desarrollo e implementación inicial + **dos reintentos de correo**, esperas base **1 y 5 minutos**, respetando Retry-After, únicamente ante rechazo transitorio confirmado. Permanentes e inciertos no se reenvían automáticamente.

Aprobó también **10 GiB globales, 2 GiB por usuario, dos cargas activas por usuario y diez globales**, configurables, con reserva atómica y sin doble cobro reserva/final. Sin borrar archivos automáticamente ni cambiar su propietario original.

Ambas decisiones quedan codificadas **pendientes de revalidación**. No son requisitos definitivos del cliente ni habilitan producción. La propuesta anterior de reserva **3×** y margen **max(1 GiB, 10 %)** no se aprueba por deducción ni se implementa. La cuota mide asignaciones lógicas de archivos/cargas; el espacio físico de representaciones internas y temporales requiere dimensionamiento aparte. No se afirma un límite físico de volumen de 10 GiB.

Contabilidad, límites de alcance y casos pendientes: [implementación de pendientes locales](fase-3-pendientes-locales-implementacion.md). Integración real 3.B3 pospuesta; fase abierta. OAuth, workers y schedulers desactivados. Cobertura histórica de archivos 64,83 %, sin nueva medición.

## Registro histórico de la propuesta anterior, sustituida por la aprobación precedente

2026-10-19. **Propuestas para aprobación; no implementadas ni activadas.** La integración real Google queda pospuesta por decisión de Richard. No se cierra 3.B3 ni la fase.

| Decisión | Recomendación inicial | Lo que conserva |
|---|---|---|
| Reintentos de correo | **Intento inicial + 2 reintentos**, esperas base de **1 y 5 minutos**, solo ante rechazo inequívoco sin aceptación (por ejemplo CUOTA/429 confirmado) o fallo demostrado antes de iniciar el POST. Respetar Retry-After mayor; no reenviar por deducción a partir de una excepción genérica | Misma fila outbox, evento/destinatario y Message-ID; transacciones cortas, fencing y persistencia antes de la llamada; sin entrega exactamente una vez |
| Cuota agregada de almacenamiento | Como punto de partida sujeto a capacidad real: **10 GiB globales, 2 GiB por usuario, 2 cargas activas por usuario y 10 globales**. Mantener margen libre de **max(1 GiB, 10 % del volumen)**. No aplicar estos números sin aprobar capacidad/usuarios del despliegue | Límite por archivo de 100 MiB, metadatos PostgreSQL y bytes duraderos fuera de base; sin retención o borrado automático |

## Correo: clasificación y límites de la propuesta

- El presupuesto propuesto es de **tres intentos totales**, distinto de los cuatro de Drive (inicial + tres reintentos ya aprobados). No se hereda automáticamente la política Drive.
- Un timeout, 5xx/408, desconexión después de iniciar POST, respuesta 2xx sin ID válido o fallo de persistencia posterior al envío conserva **resultado incierto y se detiene sin reenvío automático**. No usar el Message-ID como garantía de deduplicación del proveedor. El adaptador actual clasifica varios de esos casos como RESULTADO_INCIERTO; no reclasificarlos como seguros para conseguir reintentos.
- Error permanente, credencial/autorización requerida, permiso o destinatario inválido: detener y registrar causa sanitizada. Si no se puede demostrar que un fallo de red ocurrió antes del POST, tratarlo como incierto.
- Consumo de intentos por ejecución real de entrega, no por tick, consulta o bloqueo; esperar no consume intento. Los reintentos no pueden multiplicarse con retries ocultos de SDK/HTTP. Tras agotar el presupuesto, FALLIDA terminal, conservando destinatario/snapshot/resultado.
- Resolver resultados inciertos requiere revisión administrativa y una decisión explícita; no hay reenvío ciego, búsqueda automática Gmail ni ampliación a scopes de lectura. El flujo exacto de resolución y cualquier acción manual de reenvío siguen por aprobar.
- **Estado actual sin cambios:** correo se detiene ante fallos, directorio productivo vacío y procesamiento automático false. La recomendación no aprueba destinatarios definitivos ni habilita envíos. El receptor empresa sigue aprobado exclusivamente para el ensayo 3.B3 pospuesto.

## Cuota: contabilidad y concurrencia

- Contar **todos los bytes locales conservados**: parciales, finales aceptados, preparados tras rollback y temporales. Una copia COPIADO en Drive no elimina automáticamente sus bytes de la cuota, porque no existe política de borrado aprobada.
- Propuesta conservadora de reserva al iniciar: **3 × tamaño declarado** para cubrir parcial, preparación temporal y final. Es una reserva de admisión, no un aumento del límite de 100 MiB ni una afirmación de que cada flujo duplique siempre tres veces el archivo. Al terminar o fallar, reconciliar reserva con bytes reales conservados antes de liberar capacidad.
- Validar/reservar de forma atómica los presupuestos y límites de sesiones entre procesos; un simple chequeo de espacio libre antes de escribir permite sobreasignación concurrente. Reutilizar el módulo archivos y sus identificadores/persistencia; no crear otro módulo o almacenar bytes grandes en PostgreSQL.
- Reanudar la misma sesión idempotente no cobra otra reserva completa. Reiniciar debe recuperar/reconciliar las reservas; una sesión pausada sigue ocupando almacenamiento y no se elimina por TTL deducido. Falta concretar por aprobación cómo se liberan sesiones abandonadas sin borrar evidencia o archivos pendientes.
- Si falta cuota o margen libre, rechazar una **nueva** carga antes de aceptar bytes. Si el disco se agota durante otra operación, devolver error controlado, conservar los bytes confirmados y no marcar completado. Aviso operativo y revisión de capacidad, sin limpieza automática ni modificación del avance del pedido.
- Los valores de 10/2 GiB y 2/10 sesiones son un punto de partida a confirmar, no requisitos del manual o cliente. El dimensionamiento definitivo depende del volumen dedicado, usuarios y crecimiento de archivos retenidos. Una cuota limita admisión, pero no sustituye monitoreo ni resuelve retención.

## Siguiente trabajo local

Primero aprobar o ajustar ambas decisiones, sin ejecutar Google. Después autorizar su implementación/pruebas locales con dobles y almacenamiento exclusivo, sin relajar aserciones. La cobertura de archivos puede completarse en un encargo separado: último dato completo **1012/1561 líneas = 64,83 %**; backend global histórico **3727/4639 = 80,34 %**, denominador distinto. No se declara resuelta ni se reduce el umbral por estas propuestas.
