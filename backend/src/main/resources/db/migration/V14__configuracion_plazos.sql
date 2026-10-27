-- V14: configuración de plazos (fase 5, paso 5.1). Siguiente versión disponible tras V13; V1–V13 no cambian.
--
-- Una sola fila con la meta de días de pedido a llegada (PRD, métrica "días de pedido a llegada"; arquitectura 4.7,
-- ConfigurarMeta). La meta es configurable por el admin, pero su valor sigue pendiente del cliente ("se ajusta",
-- requerimientos §5). Los 90 días de la siembra son solo la propuesta documentada mientras tanto: no tienen aprobación
-- provisional del cliente ni del equipo, por eso la fila nace con meta_origen = 'PROPUESTA'. Cuando el admin la cambia,
-- meta_origen pasa a 'ADMIN'.
--
-- Fuera de esta migración (⏸ pendientes del cliente): la tabla feriado (modelo-datos 3.7: "solo se crea si se
-- aprueba"), la jornada laboral y los plazos de Fabricación, Aduana e Incidencias (siguen PENDIENTE en
-- definicion_etapa).

create table configuracion_plazos (
    id smallint primary key,
    meta_dias integer not null,
    meta_origen text not null,
    actualizado_en timestamptz,
    actualizado_por uuid references usuario (id),
    version bigint not null default 0,
    constraint configuracion_plazos_fila_unica check (id = 1),
    constraint configuracion_plazos_meta_positiva check (meta_dias > 0),
    constraint configuracion_plazos_meta_origen check (meta_origen in ('PROPUESTA', 'ADMIN')),
    -- La propuesta sembrada no tiene autor; una meta del admin siempre lo tiene, con su hora.
    constraint configuracion_plazos_autor check (
        (meta_origen = 'PROPUESTA' and actualizado_por is null and actualizado_en is null)
        or (meta_origen = 'ADMIN' and actualizado_por is not null and actualizado_en is not null))
);

insert into configuracion_plazos (id, meta_dias, meta_origen) values (1, 90, 'PROPUESTA');
