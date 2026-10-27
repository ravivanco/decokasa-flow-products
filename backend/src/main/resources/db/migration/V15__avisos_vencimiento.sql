-- V15: avisos de vencimiento y motivo del retraso (fase 5, paso 5.2; reglas aprobadas provisionalmente por Richard el
-- 2026-10-20 como decisión del equipo). Siguiente versión disponible tras V14; V1–V14 no cambian. Irreversible: una
-- corrección sería otra migración hacia adelante.
--
-- Reglas:
--   * ETAPA y URGENCIA son avisos distintos; si los dos vencen, hay dos avisos, uno por cada plazo.
--   * Aviso único de ETAPA: entrada de la etapa (entrada_etapa, P-18) + vencimiento. Una reentrada es otra entrada.
--   * Aviso único de URGENCIA: activación + vencimiento. La activación se identifica por la entrada que creó la
--     devolución con urgencia (UUID estable de entrada_etapa, solo inserción), no solo por la hora.
--   * Un plazo PENDIENTE no tiene vence_en: nunca genera aviso.
--   * El motivo del retraso vigente sigue en etapa_pedido.motivo_retraso (V2); cada registro o corrección es un evento
--     MOTIVO_RETRASO_REGISTRADO en el historial.

-- 1. Identidad estable de la activación de la urgencia vigente.
alter table pedido add column urgencia_activacion_id uuid;

-- Diferible: la aplicación escribe el pedido y la entrada nueva en la misma transacción, en cualquier orden (como V3).
alter table pedido add constraint pedido_urgencia_activacion foreign key (urgencia_activacion_id)
    references entrada_etapa (id) deferrable initially deferred;

-- Urgencias vigentes anteriores a V15 (solo bases de desarrollo con datos ficticios): la devolución que las activó
-- creó, en la misma transacción y con la misma hora del servidor, una sola entrada de ese pedido. Se enlaza esa
-- entrada. Si hubiera más de una, la subconsulta falla; si no hay ninguna, falla el bloque siguiente. En ningún caso
-- se inventa una identidad.
update pedido p
set urgencia_activacion_id = (select n.id from entrada_etapa n
                              where n.pedido_id = p.id and n.entrada_en = p.urgencia_activada_en)
where p.urgencia_nivel is not null;

do $$
begin
    if exists (select 1 from pedido where urgencia_nivel is not null and urgencia_activacion_id is null) then
        raise exception 'V15: hay urgencias vigentes sin la entrada que las activó; revisar los datos antes de migrar.';
    end if;
end
$$;

-- Comprueba ya la FK diferida (como V3): ALTER TABLE no admite eventos de trigger pendientes en la tabla.
set constraints pedido_urgencia_activacion immediate;
set constraints pedido_urgencia_activacion deferred;

alter table pedido add constraint pedido_urgencia_activacion_completa
    check ((urgencia_nivel is null) = (urgencia_activacion_id is null));

-- 2. Nuevo tipo de evento: MOTIVO_RETRASO_REGISTRADO (con actor; PLAZO_VENCIDO ya existía como evento del sistema).
alter table evento_pedido drop constraint evento_pedido_tipo;
alter table evento_pedido add constraint evento_pedido_tipo check (tipo in ('PEDIDO_CREADO', 'ETAPA_ENVIADA',
    'ETAPA_APROBADA', 'ETAPA_RECHAZADA', 'PEDIDO_DEVUELTO', 'DEMORA_REPORTADA', 'BODEGA_ASIGNADA',
    'OBSERVACION_AGREGADA', 'ARCHIVO_SUBIDO', 'ETAPA_REASIGNADA', 'PLAZO_VENCIDO', 'MOTIVO_RETRASO_REGISTRADO',
    'PEDIDO_FINALIZADO'));

-- 3. Avisos de vencimiento: uno por plazo, de solo inserción.
create table aviso_vencimiento (
    id uuid primary key,
    pedido_id uuid not null references pedido (id),
    clase text not null,
    etapa smallint not null,
    entrada_id uuid references entrada_etapa (id),
    urgencia_activacion_id uuid references entrada_etapa (id),
    vence_en timestamptz not null,
    evento_id uuid not null references evento_pedido (id),
    registrado_en timestamptz not null,
    constraint aviso_vencimiento_clase check (clase in ('ETAPA', 'URGENCIA')),
    constraint aviso_vencimiento_etapa_rango check (etapa between 1 and 30),
    constraint aviso_vencimiento_referencia check (
        (clase = 'ETAPA' and entrada_id is not null and urgencia_activacion_id is null)
        or (clase = 'URGENCIA' and urgencia_activacion_id is not null and entrada_id is null)),
    constraint aviso_vencimiento_evento_unico unique (evento_id)
);

create unique index aviso_vencimiento_etapa_unico on aviso_vencimiento (entrada_id, vence_en)
    where clase = 'ETAPA';
create unique index aviso_vencimiento_urgencia_unico on aviso_vencimiento (urgencia_activacion_id, vence_en)
    where clase = 'URGENCIA';
create index aviso_vencimiento_pedido on aviso_vencimiento (pedido_id);

create function aviso_vencimiento_solo_insercion() returns trigger
    language plpgsql as $$
begin
    raise exception 'Los avisos de vencimiento son de solo inserción: no se modifican ni se borran.';
end;
$$;

create trigger aviso_vencimiento_sin_cambios
    before update or delete on aviso_vencimiento
    for each row execute function aviso_vencimiento_solo_insercion();

create trigger aviso_vencimiento_sin_vaciar
    before truncate on aviso_vencimiento
    for each statement execute function aviso_vencimiento_solo_insercion();
