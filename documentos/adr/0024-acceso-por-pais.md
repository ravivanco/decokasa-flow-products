# ADR-0024: El acceso a pedidos y bodegas se limita a los países asignados hoy a cada persona, también al admin

**Fecha**: 2026-10-22
**Estado**: accepted (equipo, Richard, 2026-10-22) en lo que Richard decidió (Colombia con los mismos flujos, la asignación de países por el admin, Mirian Franco con Ecuador y Colombia, sin Colombia automático para nadie, autorización vigente en el servidor). Los detalles de diseño de esta decisión (404 para otro país, admin sin excepción, compatibilidad con EC) son del equipo y están **pendientes de revisión por Codex** (parte B). Sin verificar.
**Decisores**: Richard (equipo). No es una respuesta del cliente.

## Contexto

Hasta la fase 6 solo existía Ecuador. `usuario_pais` (V1) existía, pero ninguna regla lo usaba: cualquier cuenta activa con roles leía cualquier pedido (P-13). Con Colombia (V19), una persona sin Colombia asignado no debe ver ni tocar pedidos, archivos, cronogramas, historial ni bodegas de Colombia. El cliente respondió que eligen país "admin y usuarios" (requerimientos §5). Richard decidió el 2026-10-22 que el admin asigna los países y que Mirian Franco (admin) tiene Ecuador y Colombia. La lección A1 (ADR-0002) exige decidir con el estado en la base, no con el token.

## Decisión

1. **Fuente.** Los países de una persona son los de `usuario_pais`, leídos de la base en cada operación (como los roles). No van en el JWT. Un cambio de países vale desde la siguiente petición.
2. **Pedidos.** Toda operación sobre un pedido (detalle, búsqueda por número, acciones, observaciones, reasignación, motivo de retraso, archivos, historial y cronograma) comprueba primero la cuenta (P-13) y luego que el país del pedido esté asignado. Si no lo está, responde **404, como si no existiera**, para no revelar pedidos de otro país. Los listados y la bandeja filtran por país **en la consulta**, así que la paginación y el total son los del usuario.
3. **Admin.** **No tiene excepción**: ve y gestiona solo los países que tiene asignados. Mirian Franco tiene Ecuador y Colombia; los demás, solo Ecuador hasta que ella les asigne Colombia.
4. **Creación.** Solo en un país asignado (si no, 403), antes de reservar el número. La numeración ya era por país y año (`secuencia_pedido`, V2).
5. **Bodegas por país.** Editar una bodega exige el rol antes de leerla; una de otro país responde 404. La base rechaza asignar a un pedido una bodega de otro país (trigger `pedido_bodega_mismo_pais`, V19).
   - Cada bodega pertenece a un país y el nombre es único dentro de cada país.
   - `GET /bodegas` muestra solo las de los países asignados; pedir un país no asignado responde 403.
   - Crear o editar exige Logística o admin con el país de la bodega.
   - Al elegir bodega para un pedido, debe ser del país del pedido.
5-bis. **Vistas del admin.** `GET /notificaciones` y `GET /archivos/copias-fallidas` solo devuelven correos y archivos de pedidos de los países asignados hoy al admin. El filtro va en la consulta (`join pedido` por `pais_codigo`), así que la lista, la paginación y el total son los del admin. Las operaciones por identificador (contenido, enlace de Drive, cargas) pasan por la autorización del pedido, que ya comprueba el país: un identificador conocido no da acceso. Los registros no se borran ni se reasignan: los correos y copias de otros países siguen en su estado y los trabajos pendientes siguen su curso. El bean sin filtro `ConsultarBodegas` ya no se publica.
6. **Calendario por país.** Los plazos y el cronograma usan el calendario del país del pedido (`CalendariosPorPais`). Hoy Ecuador y Colombia tienen las mismas reglas provisionales y sus propias zonas (`America/Guayaquil` y `America/Bogota`, ambas UTC−5).
7. **Sin asignaciones en migraciones.** V19 no toca `usuario_pais` ni contiene usuarios. Los países se asignan con `PATCH /usuarios/{id}` (`paises`), siguiendo el procedimiento del plan de la fase 7.
8. **Compatibilidad, no autorización.** "Sin país equivale a Ecuador" tiene un alcance limitado:
   - Las firmas anteriores a la fase 7 (`ActorVigente`, `EstadoVigente`, `Bodega`, `BodegaActiva`, `Vista` y `CrearBodega.Comando` sin país) siguen existiendo **solo** para el código y las pruebas anteriores.
   - **Nunca omiten la autorización vigente.** En producción, el actor y el estado siempre se arman con los países leídos de la base, y un conjunto nulo equivale a vacío (sin acceso).
   - Un `pais` ausente en `POST /pedidos` o `POST /bodegas` se interpreta como EC y después se autoriza igual: sin EC asignado, 403.
   - Los métodos antiguos de los puertos sin filtro de país no los usa ningún caso de uso para responder a una persona.

## Alternativas consideradas

### País en el JWT
- **Pros**: no exige leer la base.
- **Contras**: el token queda desactualizado hasta 15 minutos.
- **Por qué no**: contradice la lección A1.

### 403 para pedidos de otro país
- **Pros**: un error explícito.
- **Contras**: revela que el pedido existe.
- **Por qué no**: el número de un pedido de otro país no debe confirmarse.

### Excepción para el admin (ve todos los países)
- **Pros**: más simple.
- **Contras**: un admin de un solo país vería el otro.
- **Por qué no**: Richard pidió que los países se asignen explícitamente, y Mirian ya tiene los dos.

### Filtrar en memoria después de paginar
- **Pros**: menos cambios en el repositorio.
- **Contras**: páginas incompletas y totales falsos.
- **Por qué no**: el adaptador JPA filtra en la consulta. La versión en memoria queda solo como valor por defecto del puerto, para los dobles de prueba.

## Consecuencias

### Positivas
- El aislamiento entre países se comprueba en el servidor, en un único punto por caso de uso (`ApoyoPedidos.exigirPais`).
- Agregar otro país solo exige definiciones, calendario y asignaciones, sin tocar las reglas.

### Negativas
- Una lectura de la base más por operación (los países van con los roles en la misma consulta del usuario).
- Las pantallas siguen sin selector de país (paso 7.3): quien tenga dos países verá ambos mezclados en los listados.

### Riesgos
- Que un caso de uso nuevo olvide `exigirPais`. Mitigación: los casos de prueba de Codex por endpoint (7.B1).
- Que un adaptador nuevo implemente los puertos sin filtrar por país. Mitigación: los métodos con países son abstractos en `RegistroNotificaciones` y `ConsultaCopias`, y en `ConsultaDePedidos` y `BodegaRepositorio` el valor por defecto en memoria solo existe para dobles de prueba; los adaptadores de la base filtran en la consulta.

## Pendiente

- La ciudad de "Bodega 1" de Colombia.
- Feriados y jornada por país (siguen pendientes del cliente en los dos países).
- La meta de días: una sola para los dos países, como hasta hoy.
- La diferencia con "procesos distintos" del documento del cliente.
- Relacionados: ADR-0002, 0004, 0006, 0009, 0011 y 0019.
