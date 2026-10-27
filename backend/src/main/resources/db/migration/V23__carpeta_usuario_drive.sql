-- V23 (2026-10-24, ADR-0026): intención duradera de crear la carpeta de Drive de cada usuario de DECOKASA
-- (Mi unidad/DECOKASA/Usuarios/{usuario}). El alta del usuario inserta la fila en su misma transacción, sin llamar a
-- Google; la tarea de copias la procesa con lease, reintentos y sin duplicar carpetas. Los IDs de las carpetas siguen en
-- carpeta_drive (V6); aquí se guarda además el ID confirmado de la carpeta del usuario.
--
-- Sin datos: los usuarios existentes no se programan solos (no hay creación masiva); el admin los programa uno a uno.
-- No modifica adjuntos, copias, carpetas ni migraciones anteriores.
create table carpeta_usuario_drive (
    -- La aplicación no borra usuarios; si se borrara uno (limpieza de pruebas), su intención no tiene sentido.
    usuario_id uuid primary key references usuario (id) on delete cascade,
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
    constraint carpeta_usuario_estado_valido check (estado in ('PENDIENTE', 'CREADA', 'FALLIDA')),
    constraint carpeta_usuario_origen_valido check (origen in ('ALTA', 'ADMIN')),
    constraint carpeta_usuario_intentos_validos check (intentos between 0 and 4),
    constraint carpeta_usuario_intento_pendiente check (not en_intento or (estado = 'PENDIENTE' and intentos >= 1)),
    constraint carpeta_usuario_creada_con_id check ((estado = 'CREADA') = (drive_id is not null)),
    constraint carpeta_usuario_creada_en check ((estado = 'CREADA') = (creada_en is not null)),
    constraint carpeta_usuario_drive_id_valido check (drive_id is null or drive_id ~ '^[A-Za-z0-9_-]{1,200}$'),
    constraint carpeta_usuario_error_valido check (ultimo_error is null or ultimo_error ~ '^[A-Z_]{1,40}$'),
    constraint carpeta_usuario_reclamo_completo check ((reclamado_por is null) = (reclamado_hasta is null))
);

create index carpeta_usuario_pendientes on carpeta_usuario_drive (programada_en, usuario_id) where estado = 'PENDIENTE';
