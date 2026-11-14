-- V35: el checklist de documentos se marca a mano (Richard, 2026-11-14, a pedido del cliente): cada requisito se cumple
-- con una marca ✓ de quien actúa en la etapa o con un archivo de ese tipo en la entrada actual. El archivo es opcional.
-- Las marcas son de solo inserción: la última fila de cada entrada y tipo es el estado vigente, y queda quién marcó o
-- desmarcó y cuándo.

create table checklist_marca (
    id             uuid primary key,
    pedido_id      uuid        not null references pedido (id),
    entrada_id     uuid        not null references entrada_etapa (id),
    tipo_documento text        not null check (length(tipo_documento) between 1 and 100),
    marcado        boolean     not null,
    usuario_id     uuid        not null references usuario (id),
    creado_en      timestamptz not null
);
create index checklist_marca_entrada on checklist_marca (entrada_id, tipo_documento, creado_en desc, id desc);

create function checklist_marca_solo_insercion() returns trigger language plpgsql as $$
begin
    raise exception 'checklist_marca es de solo inserción';
end;
$$;
create trigger checklist_marca_solo_insercion before update or delete on checklist_marca
    for each row execute function checklist_marca_solo_insercion();
