-- V27: avisos de etapa por persona (ADR-0032, decisión del equipo del 2026-10-27; no es una respuesta del cliente).
-- Siguiente versión disponible tras V26; V1–V26 no cambian.
--
-- Una fila por persona y operación, escrita en la misma transacción que la acción y su evento: si la acción se revierte,
-- no queda aviso. Guarda una copia de lo que se muestra (pedido, etapas, actor, motivo y fecha). Solo inserción; la
-- lectura se marca por persona en aviso_lectura (V26). Sin datos iniciales: los avisos anteriores no se reconstruyen.

create table aviso_etapa (
    id uuid primary key,
    evento_id uuid not null references evento_pedido (id) deferrable initially deferred,
    pedido_id uuid not null references pedido (id) deferrable initially deferred,
    usuario_id uuid not null references usuario (id),
    tipo text not null,
    pais_codigo text not null references pais (codigo),
    tipo_flujo text not null,
    numero_pedido text not null,
    etapa_origen smallint,
    nombre_origen text,
    etapa_destino smallint not null,
    nombre_destino text not null,
    actor_id uuid references usuario (id),
    actor_nombre text,
    motivo text,
    como_admin boolean not null,
    como_responsable boolean not null,
    ocurrido_en timestamptz not null,
    constraint aviso_etapa_unico unique (evento_id, usuario_id),
    constraint aviso_etapa_tipo check (tipo in ('CREACION', 'LLEGADA', 'DEVOLUCION', 'REASIGNACION', 'CIERRE')),
    constraint aviso_etapa_motivo check (como_admin or como_responsable),
    constraint aviso_etapa_etapas check (etapa_destino between 1 and 30
        and (etapa_origen is null or etapa_origen between 1 and 30))
);

create index aviso_etapa_usuario on aviso_etapa (usuario_id, ocurrido_en desc);

create function aviso_etapa_solo_insercion() returns trigger
    language plpgsql as $$
begin
    raise exception 'Los avisos de etapa son de solo inserción: no se modifican ni se borran.';
end;
$$;

create trigger aviso_etapa_sin_cambios
    before update or delete on aviso_etapa
    for each row execute function aviso_etapa_solo_insercion();
