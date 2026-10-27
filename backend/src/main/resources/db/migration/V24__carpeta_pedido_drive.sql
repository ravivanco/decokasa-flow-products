-- V24 (2026-10-26, ADR-0027): carpetas de Drive por pedido, autor y día de subida
-- (Mi unidad/DECOKASA/{número del pedido}/{usuario-id8}/{AAAA-MM-DD}). El alta del pedido inserta la intención en su
-- misma transacción, sin llamar a Google; la tarea de copias la procesa con lease, reintentos y sin duplicar carpetas.
-- Los IDs de las carpetas siguen en carpeta_drive (V6); aquí se guarda además el ID confirmado de la carpeta del pedido.
--
-- Sin datos: los pedidos existentes no se programan solos (no hay creación masiva); su carpeta se crea con la primera
-- copia o cuando el admin la programa. No modifica adjuntos, copias, carpetas ni migraciones anteriores.
create table carpeta_pedido_drive (
    pedido_id uuid primary key references pedido (id),
    estado text not null default 'PENDIENTE',
    origen text not null,
    intentos integer not null default 0,
    en_intento boolean not null default false,
    proximo_intento_en timestamptz,
    reclamado_por uuid,
    reclamado_hasta timestamptz,
    ultimo_error text,
    drive_id text,
    programada_en timestamptz not null,
    creada_en timestamptz,
    actualizada_en timestamptz not null,
    constraint carpeta_pedido_estado_valido check (estado in ('PENDIENTE', 'CREADA', 'FALLIDA')),
    constraint carpeta_pedido_origen_valido check (origen in ('ALTA', 'ADMIN')),
    constraint carpeta_pedido_intentos_validos check (intentos between 0 and 4),
    constraint carpeta_pedido_intento_pendiente check (not en_intento or (estado = 'PENDIENTE' and intentos >= 1)),
    constraint carpeta_pedido_creada_con_id check ((estado = 'CREADA') = (drive_id is not null)),
    constraint carpeta_pedido_creada_en check ((estado = 'CREADA') = (creada_en is not null)),
    constraint carpeta_pedido_drive_id_valido check (drive_id is null or drive_id ~ '^[A-Za-z0-9_-]{1,200}$'),
    constraint carpeta_pedido_error_valido check (ultimo_error is null or ultimo_error ~ '^[A-Z_]{1,40}$'),
    constraint carpeta_pedido_reclamo_completo check ((reclamado_por is null) = (reclamado_hasta is null))
);

create index carpeta_pedido_pendientes on carpeta_pedido_drive (programada_en, pedido_id) where estado = 'PENDIENTE';

-- ADR-0027: ya no hay carpetas globales por usuario. Las intenciones de V23 que seguían pendientes se cierran como
-- FALLIDA/OBSOLETA para que ningún proceso cree la estructura Usuarios/…; no se borra ninguna fila ni carpeta remota.
update carpeta_usuario_drive
   set estado = 'FALLIDA', ultimo_error = 'OBSOLETA', en_intento = false, proximo_intento_en = null,
       reclamado_por = null, reclamado_hasta = null, actualizada_en = now()
 where estado = 'PENDIENTE';
