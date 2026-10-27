-- Paso 3.4: misma cola adjunto. Sin binarios, secretos o URI de sesión en texto plano.
alter table adjunto add column copia_en_intento boolean not null default false;
alter table adjunto add column drive_sesion_cifrada bytea;
alter table adjunto add column drive_offset bigint not null default 0;
alter table adjunto add column drive_carpeta_id text;
alter table adjunto add constraint adjunto_offset_drive_valido check (drive_offset between 0 and tamano_bytes);
alter table adjunto add constraint adjunto_sesion_con_id check
    (drive_sesion_cifrada is null or (drive_id_reservado is not null and drive_carpeta_id is not null));
alter table adjunto add constraint adjunto_intento_pendiente check
    (not copia_en_intento or (estado_copia = 'PENDIENTE' and intentos between 1 and 4));
alter table adjunto add constraint adjunto_copiado_sin_sesion check
    (estado_copia <> 'COPIADO' or drive_sesion_cifrada is null);

-- Índice técnico de identidades de carpetas; no es otro catálogo de archivos ni otra cola.
create table carpeta_drive (
    clave text primary key,
    nombre text not null,
    padre_id text not null,
    drive_id_reservado text not null unique,
    confirmada boolean not null default false,
    creada_en timestamptz not null,
    constraint carpeta_clave_longitud check (char_length(clave) between 1 and 100),
    constraint carpeta_nombre_longitud check (char_length(nombre) between 1 and 255)
);
-- Sin backfill de archivos/copias ni borrado por antigüedad. V1-V5 se conservan.
