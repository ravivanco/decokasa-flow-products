-- Forward-only. Schema only; existing ownership/data are recovered separately in V8.
-- Logical allocations, not a physical filesystem inventory. No bytes in PostgreSQL.
create table archivo_cuota_cerrojo (
    id smallint primary key check(id=1),
    version bigint not null default 0 check(version>=0)
);
create table archivo_cuota (
    adjunto_id uuid primary key,
    propietario_id uuid not null references usuario(id),
    bytes_comprometidos bigint not null check(bytes_comprometidos>0),
    estado text not null check(estado in ('RESERVADA','CONFIRMADA')),
    creada_en timestamptz not null default current_timestamp
);
-- adjunto_id intentionally has no FK: it is reserved before an adjunto exists.
create index archivo_cuota_propietario on archivo_cuota(propietario_id)
    include(bytes_comprometidos,estado);
create function archivo_cuota_conservar() returns trigger language plpgsql as $$
begin
    if TG_OP <> 'UPDATE' then
        raise exception 'No se eliminan reservas automaticamente';
    end if;
    if new.adjunto_id<>old.adjunto_id or new.propietario_id<>old.propietario_id
        or new.creada_en<>old.creada_en then
        raise exception 'La identidad y propiedad de la cuota son inmutables';
    end if;
    if old.estado='CONFIRMADA' and (new.estado<>old.estado or new.bytes_comprometidos<>old.bytes_comprometidos) then
        raise exception 'La asignacion confirmada es inmutable';
    end if;
    return new;
end;
$$;
create trigger archivo_cuota_sin_cambio_propietario before update or delete on archivo_cuota
    for each row execute function archivo_cuota_conservar();
create trigger archivo_cuota_sin_vaciar before truncate on archivo_cuota
    for each statement execute function archivo_cuota_conservar();
