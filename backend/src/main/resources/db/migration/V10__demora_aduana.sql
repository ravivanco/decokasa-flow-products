-- V10: demora en aduana (fase 4, paso 4.3). Siguiente versión disponible tras V9.
-- Fuentes: requerimientos 4.4 (A7: "si hay demora, sube observación y se recalcula la llegada") y §5 ("un retraso
-- arrastra todas las fechas siguientes"; fin de semana → lunes, también para la llegada del barco); modelo-datos §3.3.
--
-- F4-D5, provisional del equipo (2026-10-19), pendiente de confirmación del cliente: la nueva fecha estimada la escribe
-- quien puede actuar en A7 (Compras China o el admin, P-06). Si cae en fin de semana se guarda el lunes siguiente; la
-- fecha guardada no es anterior al día del reporte en Ecuador y es posterior a la estimación vigente.
--
-- El pedido guarda solo la demora vigente (la última). Cada demora queda además en el historial como DEMORA_REPORTADA
-- (evento_pedido, que no se modifica ni se borra), con la estimación anterior y la solicitada en evento_pedido.datos.
-- La fecha estimada no es un vencimiento: el plazo de Aduana sigue PENDIENTE (F4-D6) y su vencimiento, nulo. El
-- cronograma de la fase 5 usará esta fecha como punto de partida para recalcular las etapas siguientes.
--
-- Fuera de esta migración: feriados de Ecuador y jornada laboral (F4-D10), plazo de Aduana (F4-D6).

alter table pedido add column demora_motivo text;

alter table pedido add column demora_nueva_fecha_llegada date;

-- Sin cascada: una persona con demoras registradas no se borra (se desactiva).
alter table pedido add column demora_reportada_por uuid references usuario (id) on delete restrict;

alter table pedido add column demora_reportada_en timestamptz;

-- Las cuatro columnas van juntas: o no hay demora, o la demora está completa.
alter table pedido add constraint pedido_demora_completa check (
    (demora_motivo is null) = (demora_nueva_fecha_llegada is null)
    and (demora_motivo is null) = (demora_reportada_por is null)
    and (demora_motivo is null) = (demora_reportada_en is null));

-- Mismo límite que las observaciones (4000 caracteres) y sin texto vacío.
alter table pedido add constraint pedido_demora_motivo_longitud
    check (char_length(btrim(demora_motivo)) between 1 and 4000);

alter table pedido add constraint pedido_demora_despues_de_creado
    check (demora_reportada_en >= creado_en);

-- La fecha guardada ya pasó al lunes: nunca es sábado ni domingo (ISO 6 y 7).
alter table pedido add constraint pedido_demora_fecha_habil
    check (extract(isodow from demora_nueva_fecha_llegada) < 6);

-- No es anterior al día del reporte en Ecuador (hora del servidor).
alter table pedido add constraint pedido_demora_fecha_no_pasada
    check (demora_nueva_fecha_llegada >= (demora_reportada_en at time zone 'America/Guayaquil')::date);

-- La FK a usuario no tiene índice propio; las demoras son pocas, por eso el índice es parcial.
create index pedido_demora_reportada_por on pedido (demora_reportada_por) where demora_reportada_por is not null;
