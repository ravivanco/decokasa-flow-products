# Fase 3 — decisiones provisionales locales: implementación y revalidación pendiente

2026-10-19 · Rama `fase-3-drive-gmail` · **Pendiente de revalidación; fase abierta.** Fuente: aprobación expresa de Richard, provisional del equipo para desarrollo. Alcance: solo reintentos de correo y cuotas/reservas; no destinatarios productivos ni activación Google.

## Implementación de correo

- `FalloIntegracionGoogle` incorpora evidencia explícita de rechazo transitorio confirmado; los constructores anteriores conservan ese valor en false. Una excepción genérica CUOTA/TRANSITORIA no autoriza reenvío.
- `GmailNotificador` reconoce rechazo completo HTTP 403/429: código coincidente, todos los motivos `usageLimits/rateLimitExceeded` o `usageLimits/userRateLimitExceeded`, sin ID de mensaje. Motivo desconocido o respuesta incompleta no acredita rechazo seguro. También puede acreditar que no se inició `messages.send` si falla transitoriamente la obtención de credencial.
- Intento inicial + **dos reintentos**, sobre la misma fila/evento/destinatario/Message-ID. Primer rechazo confirmado: próxima fecha = fin + max(1 minuto, Retry-After); segundo: fin + max(5 minutos, Retry-After); tercero: FALLIDA terminal. Drive conserva inicial + tres reintentos.
- Retry-After admite segundos y fecha HTTP; fecha pasada se trata como cero antes de aplicar la base. Cabecera inválida/desbordada o vencimiento no representable detienen, no acortan la espera. Fecha persistida, sin sleep ni retry HTTP oculto.
- Timeout, 408/5xx, corte de transporte, 429 ambiguo, 2xx sin ID válido o caída tras POST no se reenvían automáticamente. Se conserva incertidumbre donde corresponde. Permanentes/autorización detienen. No hay promesa de exactamente una entrega ni recepción humana deducida de la aceptación.
- `TrabajoCorreo` añade operación de reprogramación con default conservador para adaptadores antiguos. `CorreoConfig` la persiste en transacción corta separada de negocio/HTTP. El SQL mantiene fencing y lease, limita selección a `intentos < 3` y fuerza terminal al agotar presupuesto. Parámetro de fecha nula tipado explícitamente como timestamptz.
- La reclamación existente consume un intento y deja marca incierta **antes** del POST. Es un conteo conservador: una caída entre reclamar y enviar también consume y detiene. Consultas, ticks y espera no añaden intentos. No se modifica el encolado transaccional ni se multiplican listeners.
- Documentación pública consultada, sin API autenticada: [errores Gmail](https://developers.google.com/workspace/gmail/api/guides/handle-errors). No se registran cuerpos, cabeceras sensibles o tokens. Directorio productivo vacío; el destinatario aprobado exclusivamente para 3.B3 no se añade al envío general.

## Implementación y contabilidad de almacenamiento

`CuotaAlmacenamiento` es un puerto del módulo archivos existente; `CuotaSqlRepositorio` mantiene **una asignación por UUID de adjunto reservado**. Un cerrojo PostgreSQL compartido por instancias serializa admisión y confirmación en transacciones cortas. La fila de cerrojo se actualiza: con READ COMMITTED las sumas ven lo confirmado; con REPEATABLE READ una carrera debe abortar de forma segura en lugar de admitir sobre una instantánea antigua.

| Situación | Bytes comprometidos | Cupo activo |
|---|---|---|
| Adjunto histórico/confirmado, incluso COPIADO o FALLIDO en Drive | Tamaño real, una vez | No |
| Nueva carga por bloques | Tamaño completo declarado, antes de recibir bloques | Sí |
| Repetición idempotente, pausa o reinicio | Misma reserva, sin otro cobro | Conserva el cupo |
| Bloque inválido, fallo de disco, permisos revocados o finalización revertida | Conserva reserva total; no presume liberación de bytes | Conserva el cupo |
| Finalización por bloques confirmada | Misma fila RESERVADA → CONFIRMADA, dentro del commit de adjunto/entrada/evento; bytes sin aumento | Libera un cupo al confirmar |
| Multipart heredado, sin tamaño declarado fiable | Reserva el máximo configurable por archivo antes de la copia de aplicación; ajusta al tamaño medido al confirmar | Sí hasta confirmar |
| Multipart fallido o caída antes de confirmar | Reserva conservadora completa y bytes retenidos, sin liberación ni borrado automático | Conserva cupo para revisión |

**Global = suma de `archivo_cuota.bytes_comprometidos`; por usuario = esa suma filtrada por propietario.** No se suman de nuevo las tablas `adjunto` o `carga_archivo`. Cargas activas = filas RESERVADA, incluidas pausadas/fallidas no resueltas. Los bytes recibidos por bloque están incluidos en la reserva, no se añaden otra vez. Un parcial sigue sin ser adjunto válido ni requisito documental cumplido.

Inicio por bloques y reserva se confirman/revierten juntos; finalizar adjunto/historial/sesión/cuota también. Los bytes duraderos sobreviven al rollback de finalización; la reserva sigue cubriendo el archivo lógico. Multipart reserva antes de la copia de aplicación, registra y ajusta en el mismo commit; se retira el borrado del preparado/final ante fallo de registro. No se introduce limpieza de aceptados o parciales ni TTL.

Configuración provisional:

| Variable | Default | Significado |
|---|---|---|
| `ARCHIVOS_CUOTA_GLOBAL` | `10GB` | 10737418240 bytes, 10 GiB; GB binario de Spring DataSize |
| `ARCHIVOS_CUOTA_USUARIO` | `2GB` | 2147483648 bytes, 2 GiB |
| `ARCHIVOS_CARGAS_USUARIO` | `2` | Asignaciones reservadas del propietario |
| `ARCHIVOS_CARGAS_GLOBALES` | `10` | Asignaciones reservadas globales |

Valores positivos e individuales no superiores a globales. Todas las instancias deben compartir configuración. Reducir límites no borra lo existente ni impide finalizar/reanudar una reserva previa: bloquea nuevas admisiones que no quepan. Propietario original inmutable; cambiar responsable del pedido no cambia a quién se cobra.

V7 añade estructura y V8 recupera datos anteriores, separadas y **no ejecutadas**. V1–V6 intactas. Los adjuntos se incorporan con `subido_por`; incompletos con `usuario_id` y `adjunto_reservado_id`; completas ya tienen la misma fila final, sin doble alta. Exceso histórico se conserva, no se fuerza su borrado para ajustarlo al nuevo límite. Aplicar posteriormente con escritores detenidos. Error público `CUOTA_ALMACENAMIENTO` HTTP 409 en OpenAPI, sin filtrar rutas o detalles SQL.

### Límites de esta contabilidad

La cuota es de **asignaciones lógicas**, no un inventario/límite físico del volumen. No cobra dos veces el mismo documento por sus representaciones internas parcial/final. Estas representaciones, temporales de preparación y spooling del servlet/proxy sí requieren espacio físico adicional; no se afirma que un volumen de 10 GiB sea suficiente. Multipart puede crear temporales antes de la admisión por caso de uso: el cupo no es un límite de conexiones HTTP o de esos temporales.

No se aplica por deducción la propuesta anterior de reserva **3×** o margen libre **max(1 GiB, 10 %)**. Dimensionamiento/observación del volumen, huérfanos físicos anteriores sin fila y liberación administrativa de abandonadas siguen pendientes. Un multipart fallido conserva reserva aunque el temporal ya no exista: es conservador y puede agotar cupos; requiere revisión explícita, no un TTL inventado. No se incorpora un endpoint de borrado/liberación ni se reasignan propietarios.

Se conserva el máximo provisional **100 MiB = 104857600 bytes**, distinto de “100 MB” decimales del manual. No se inventan formatos permitidos, reglas comerciales o carpetas.

## Casos para revalidación — correo

Extender suites existentes con dobles de Google y reloj controlado, sin esperas reales:

1. Confirmación de transición deja un aviso por evento/destinatario; rollback no deja aviso enviable. Repetir la evidencia de B2 sin duplicar suites.
2. Rechazo confirmado del intento 1 → +60 s; no reclamar antes; segundo rechazo → +300 s; tercero terminal sin cuarto POST. Reiniciar entre intentos sin resetear presupuesto.
3. Retry-After menor/mayor, cero, segundos, fecha futura/pasada, inválida, desbordamiento y fecha fuera de rango. No ejecutar antes del máximo ni acortar por error de parseo.
4. 403/429 estructurado válido; motivos mezclados/desconocidos, cuerpo vacío/truncado/inválido, código discordante, ID de mensaje presente: no atribuir rechazo confirmado. 429 ambiguo conserva incertidumbre.
5. Fallo transitorio al obtener credencial antes del POST, frente a CUOTA/TRANSITORIA genérica de un doble sin evidencia. Solo el primero habilita reintento acotado.
6. 401/permanentes, 408/5xx, timeout/corte, 2xx sin ID y fallo de commit posterior: sin reenvío ciego, con errores sanitizados.
7. Workers concurrentes, lease vencido, persistencia tardía del worker antiguo y reinicio: conservar fencing, una reclamación válida y la incertidumbre, sin alterar trabajo ajeno.
8. Flags false/directorio vacío: sin entregas ni consumo por tick. El receptor exclusivo 3.B3 no habilita correos generales. Clave evento/destinatario y Message-ID estables sin prometer exactamente una vez.

## Casos para revalidación — cuotas y archivos

PostgreSQL Testcontainers y almacenamiento aislados por suite; sin bases compartidas o Google:

1. V1–V8 desde vacío; V7/V8 sobre fase 2 con adjuntos/datos y fase 3 con cargas parciales/completadas. Propietarios/tamaños/UUIDs intactos, sin cobro doble de completas; exceso histórico conservado y nuevas reservas bloqueadas.
2. Defaults exactos y configuración alternativa; cero/negativos, usuario > global, reducción con reservas existentes. Reanudar/finalizar sigue posible, sin borrar datos.
3. Cuota exacta y un byte por encima, global/usuario; usuario agotado no bloquea otro con capacidad global. Históricos y estados COPIADO/FALLIDO siguen contando. Cambio de responsable no cambia propietario.
4. Dos cargas personales admitidas y tercera rechazada; diez globales admitidas y undécima rechazada. Finalizar libera exactamente un cupo. Consultar, fragmentar y repetir inicio/final no crean asignaciones nuevas.
5. Conexiones/JVMs simultáneas: último byte/cupo, distintas claves y misma idempotencia, confirmaciones paralelas, rollback. No sobreasignación o doble cobro. Cubrir READ COMMITTED y aborto seguro de REPEATABLE READ, orden de bloqueos pedido/carga/cuota y ausencia de red dentro de transacción.
6. Idempotencia devuelve la misma sesión/reserva; datos o propietario incompatibles rechazan sin modificar otra carga. Registro de reserva y sesión revierte junto si falla admisión.
7. Parcial 0/N bytes, pausa, caída tras escribir antes del offset, permisos revocados y rollback final: reserva/cupo duraderos; incompleto no satisface requisito. Finalización cambia la clasificación, sin sumar reserva y final.
8. Multipart menor/exacto 100 MiB/mayor: reserva previa máxima y ajuste al tamaño medido al confirmar. Fallo antes/después del movimiento conserva reserva/bytes para revisión; no limpieza automática. Separar spooling del servlet de la copia admitida.
9. Propietario inmutable en SQL, confirmación con otro usuario, reserva ausente o final mayor a lo reservado: conflicto y rollback sin historial parcial. Confirmación repetida correcta no altera bytes/cupos.
10. Fallos de disco/base y reinicio con reservas pausadas/fallidas: sin liberación o TTL automático. La evidencia no se elimina para hacer pasar el caso. Revisar el dimensionamiento físico y la atención de multipart huérfano de forma separada.

## Completar cobertura de archivos después

Ampliar las suites B1 existentes sin duplicar comportamientos. Además de cuota, cubrir ramas pendientes de rangos, integridad, pausa/reanudación, permisos inicial/final, asociación pedido/etapa/entrada/evento, descarga por backend y parciales no válidos. Repetir estados pendiente/copiado/fallido, inicial + tres reintentos de Drive, concurrencia/lease, reinicio, reanudación y reconciliación/idempotencia con dobles y tiempo controlado.

Después ejecutar el arnés de archivos y el check completo del backend con JaCoCo; publicar numerador/denominador global y del módulo por separado y alcanzar **80 % sin reducir umbrales**. Última medición histórica: archivos **1012/1561 = 64,83 %**; global **3727/4639 = 80,34 %**. Los cambios añaden código: esos porcentajes no miden esta entrega.

## Estado y restricciones

Implementación revisada estáticamente con criterios Spring Boot/JPA, migraciones, Java y PostgreSQL; no sustituye compilación o ejecución. **No se modifican pruebas ni se ejecutan pruebas, builds o migraciones.** No se cierran hallazgos por escribir código: B3-SEG-01 sigue pendiente de revalidación y dimensionamiento físico; B1-P01/P02 conservan su cierre histórico, sin revalidación nueva inventada.

Google real 3.B3 sigue **pospuesto por decisión de Richard**. OAuth, Drive, Gmail, workers y schedulers false por defecto; no se arrancan servicios ni se usan credenciales/tokens. Sin operaciones Google o comandos Git de escritura. Destinatarios definitivos, resolución administrativa de incertidumbre, retención y dimensionamiento físico pendientes; fase abierta.
