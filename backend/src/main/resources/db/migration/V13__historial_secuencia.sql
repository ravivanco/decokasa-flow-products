-- V13: secuencia de inserción del historial (fase 4, corrección de F4-H01, aprobada por Richard el 2026-10-20).
-- Siguiente versión disponible tras V12.
--
-- Una acción puede emitir varios eventos con la misma hora del servidor (BODEGA_ASIGNADA y ETAPA_APROBADA al aprobar
-- Bodega; ETAPA_APROBADA y PEDIDO_FINALIZADO al aprobar Incidencias). Hasta ahora el desempate era el UUID aleatorio
-- del evento, que no conserva el orden en que el dominio los emite. Desde V13 cada evento nuevo recibe un número de
-- una secuencia de la base al insertarse; el historial se inserta de forma síncrona, en orden de emisión, dentro de la
-- transacción de la acción y con la fila del pedido bloqueada. El orden de lectura es hora y, en empate, secuencia.
--
-- Eventos anteriores a V13: no se les asigna número. Su orden real dentro de una misma hora no se conoce y no se
-- inventa; conservan secuencia nula y se desempatan como antes (por id). La columna se agrega sin valor por defecto
-- (no reescribe ni actualiza filas, así que los triggers de solo inserción no se tocan ni se debilitan) y el valor por
-- defecto se fija después, para las filas nuevas. Las horas (ocurrido_en) no cambian.

create sequence evento_pedido_secuencia as bigint;

alter table evento_pedido add column secuencia bigint;

alter table evento_pedido alter column secuencia set default nextval('evento_pedido_secuencia');

alter sequence evento_pedido_secuencia owned by evento_pedido.secuencia;

-- Toda fila nueva lleva número. NOT VALID: no se valida contra las filas anteriores (que quedan nulas a propósito),
-- pero sí se exige en cada inserción posterior.
alter table evento_pedido add constraint evento_pedido_secuencia_nueva check (secuencia is not null) not valid;

-- Un número no se repite (las filas anteriores, nulas, no chocan entre sí).
create unique index evento_pedido_secuencia_unica on evento_pedido (secuencia);

-- Orden de lectura del historial de un pedido: hora, secuencia e id.
create index evento_pedido_orden on evento_pedido (pedido_id, ocurrido_en, secuencia, id);
