-- V9: catálogo de bodegas y datos de llegada del pedido (fase 4, paso 4.1). Siguiente versión disponible tras V8.
-- Fuentes: requerimientos 4.2 #14, 4.4 (A7 "✓ Llegada", A9 "Bodega elegida"), 4.6 y §5 ("Anderson" mantiene la lista);
-- modelo-datos §3.3 (pedido.bodega_id, S-3) y §3.7 (bodega).
--
-- F4-D1, provisional del equipo (2026-10-19), no confirmada por el cliente: Logística y admin crean, editan y desactivan
-- bodegas, con roles y estado vigentes comprobados en el servidor. Es un permiso de la aplicación; esta migración no lo
-- fija.
--
-- Fuera de esta migración porque su significado sigue pendiente del cliente (⏸):
--   - plazos de Fabricación (A5), Aduana (A7) e Incidencias (A10): siguen PENDIENTE en definicion_etapa (no se tocan;
--     PENDIENTE no equivale a SIN_PLAZO);
--   - documentos de Embarque, Bodega e Incidencias: siguen PENDIENTES (F4-D4, F4-D7, F4-D8);
--   - demora en aduana: quién escribe la nueva fecha estimada y qué fechas arrastra (F4-D5); sus columnas se agregan
--     cuando se decida;
--   - cómo se elige la bodega: con el APROBAR de A9 o con una acción aparte (F4-D2).
-- El catálogo inicial es un dato de referencia, como los roles de V1, y va en la misma migración que su tabla.

-- Bodegas --------------------------------------------------------------------------------------------------------------
create table bodega (
    id uuid primary key,
    nombre text not null,
    ciudad text not null,
    activa boolean not null default true,
    creada_en timestamptz not null,
    actualizada_en timestamptz not null,
    -- Bloqueo optimista de las ediciones (como D10 en usuarios).
    version bigint not null default 0,
    -- Sin espacios sobrantes al inicio ni al final; la aplicación los recorta antes de guardar.
    constraint bodega_nombre_formato check (nombre = btrim(nombre) and char_length(nombre) between 1 and 100),
    constraint bodega_ciudad_formato check (ciudad = btrim(ciudad) and char_length(ciudad) between 1 and 100),
    constraint bodega_version_no_negativa check (version >= 0)
);

create unique index bodega_nombre_unico on bodega (lower(nombre));

-- Una bodega no se borra: se desactiva (modelo-datos, "Borrados"). Así las bodegas usadas por pedidos y su historial se
-- conservan.
create function bodega_no_se_borra() returns trigger
    language plpgsql as $$
begin
    raise exception 'Una bodega no se borra: se desactiva.';
end;
$$;

create trigger bodega_sin_borrar
    before delete on bodega
    for each row execute function bodega_no_se_borra();

create trigger bodega_sin_vaciar
    before truncate on bodega
    for each statement execute function bodega_no_se_borra();

-- Carga inicial (requerimientos 4.2 #14 y 4.4): Quito, Guayaquil e Ibarra, activas. El nombre es la ciudad: la fuente no
-- da otro nombre. Identificadores fijos para que las referencias no cambien entre bases.
insert into bodega (id, nombre, ciudad, activa, creada_en, actualizada_en, version) values
    ('01a13cdb-cc00-7000-8000-000000000401', 'Quito', 'Quito', true, now(), now(), 0),
    ('01a13cdb-cc00-7000-8000-000000000402', 'Guayaquil', 'Guayaquil', true, now(), now(), 0),
    ('01a13cdb-cc00-7000-8000-000000000403', 'Ibarra', 'Ibarra', true, now(), now(), 0);

-- Pedido: bodega de destino y llegada a aduana --------------------------------------------------------------------------
-- Bodega elegida en A9. La FK no borra en cascada: una bodega usada por un pedido no se puede borrar.
alter table pedido add column bodega_id uuid references bodega (id);

create index pedido_bodega on pedido (bodega_id) where bodega_id is not null;

-- Llegada a aduana: el momento del check de A7 ("David da check cuando llega la mercadería"), con la hora del servidor.
-- La fecha estimada que se informa con una demora no va aquí (F4-D5).
alter table pedido add column llegada_aduana_en timestamptz;

alter table pedido add constraint pedido_llegada_despues_de_creado
    check (llegada_aduana_en >= creado_en);

-- Una bodega inactiva no recibe asignaciones nuevas. Desactivarla no cambia los pedidos que ya la tienen ni su
-- historial. El bloqueo FOR SHARE espera a una desactivación en curso, así que una asignación y una desactivación
-- simultáneas no dejan un pedido asignado a una bodega inactiva (la FK solo toma un bloqueo de clave, que no la
-- detiene).
create function pedido_bodega_activa() returns trigger
    language plpgsql as $$
begin
    if new.bodega_id is not null
            and (tg_op = 'INSERT' or new.bodega_id is distinct from old.bodega_id) then
        perform 1 from bodega where id = new.bodega_id and activa for share;
        if not found then
            raise exception 'La bodega no existe o está inactiva: no admite asignaciones nuevas.';
        end if;
    end if;
    return new;
end;
$$;

create trigger pedido_bodega_activa
    before insert or update of bodega_id on pedido
    for each row execute function pedido_bodega_activa();
