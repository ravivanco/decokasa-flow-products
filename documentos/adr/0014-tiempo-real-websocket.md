# ADR-0014: Actualización entre pantallas con consultas periódicas (WebSocket STOMP descartado por ahora)

**Fecha**: 2026-10-10 (propuesta); 2026-10-20 (decisión)
**Estado**: accepted (equipo, provisional) — consultas periódicas con TanStack Query. **No es una respuesta del cliente.**
**Decisores**: equipo de desarrollo; aprobada provisionalmente por Richard el 2026-10-20 (fase 5, paso 5.4). Richard aceptó el intervalo de 30 s el 2026-10-21.

## Contexto

El documento del cliente incluye en su stack "WebSocket (STOMP) de Spring" para que "los checks se vean al instante" (`requerimientos.md`, 1.6). El PRD lo clasifica como Should. El "stack fijo" de `CLAUDE.md` no lo menciona. El equipo aprobó el 2026-10-12 que no entrara antes de la fase 5 y dejó la tecnología para esa fase.

## Decisión (provisional del equipo)

Las pantallas de seguimiento se actualizan con **consultas periódicas de TanStack Query**, sin WebSocket:

- **Intervalo:** `VITE_ACTUALIZACION_MS`, 30 s por defecto, limitado a 10 s–5 min.
- **Qué se refresca:** solo las consultas montadas de seguimiento: bandeja y sus totales, lista de pedidos (incluido el panel de vencidos), detalle, historial, cronograma y archivos del detalle visible. No se refrescan definiciones de flujo, usuarios, roles ni bodegas.
- **Pestañas ocultas:** el intervalo se pausa. TanStack no duplica una petición en curso de la misma consulta.
- **Foco y conexión:** al recuperarlos se piden datos actuales aunque la caché se considere reciente.
- **Operaciones propias:** invalidan sus consultas enseguida (sin esperar el intervalo).
- **Sesión:** al cerrar o cambiar de sesión se cancelan las consultas y se vacía la caché; las claves llevan el número de sesión.
- **Permisos:** cada refresco vuelve a pasar por el servidor (P-13, P-06). Un 403 sobre un recurso se muestra como permiso denegado y detiene el intervalo de esa consulta; no cierra la sesión. La sesión solo termina con la política existente (renovación rechazada con 401 o 403).
- **Formularios:** mientras hay un formulario con cambios sin enviar o un diálogo de acción abierto, el refresco automático se pausa; al terminar, se pide una lectura actual. Las comprobaciones de versión, etapa y entrada siguen en el servidor al confirmar.

**Demora esperada (expresa):** un cambio hecho por otra persona puede tardar en verse **hasta el intervalo configurado más el tiempo de la petición** (con el valor por defecto, unos 30 s), o menos si la persona vuelve a la pestaña. **No es "al instante"**: el requisito del cliente de ver los checks "al instante" **no se declara satisfecho** sin su aceptación.

## Alternativas consideradas

### WebSocket (STOMP) desde Spring
- **Pros**: un check se ve casi de inmediato en otras pantallas.
- **Contras**: dependencia fuera del stack fijo; autenticar el CONNECT con el JWT de 15 min y renovarlo en el socket; autorizar cada suscripción por país y rol; proxy de nginx y Vite con Upgrade y CSP `connect-src`; reconexión y pruebas de aislamiento entre sesiones.
- **Por qué no ahora**: con unos 6 usuarios, la ganancia (inmediato frente a ~30 s) no compensa el costo y el riesgo de seguridad en esta fase. Se puede retomar si el cliente exige inmediatez; no cambia el contrato REST.

## Consecuencias

### Positivas
- Sin dependencias nuevas ni conexiones persistentes; el aislamiento por permisos es el mismo de cualquier lectura.
- Comportamiento verificable con un reloj y dos sesiones.

### Negativas
- Demora de hasta el intervalo más la petición para los cambios ajenos.
- Carga periódica sobre la API (baja con ~6 usuarios: 3 a 6 consultas por pestaña visible cada 30 s).

### Pendiente
- Aceptación del cliente de que la actualización no es instantánea. (Richard aceptó el intervalo de 30 s el 2026-10-21; eso no se registra como respuesta del cliente.)
- Medición de carga y de la demora real (Codex, 5.B2).
