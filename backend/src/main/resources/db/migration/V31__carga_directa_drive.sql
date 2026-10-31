-- ADR-0034 (aclaración aprobada el 2026-10-31): los documentos se guardan únicamente en Google Drive. Con
-- ARCHIVOS_DESTINO=drive cada fragmento de una carga reanudable se reenvía a una sesión reanudable de Drive; aquí solo
-- se recuerda esa sesión, nunca el contenido:
--   drive_id_reservado   ID del archivo en Drive, reservado antes de abrir la sesión (un reintento no crea otro).
--   drive_carpeta_id     carpeta DECOKASA/{pedido}/{autor}/{día} donde se crea.
--   drive_sesion_cifrada URI de la sesión reanudable, cifrada con AES-GCM (GOOGLE_OAUTH_LLAVE), ligada al adjunto.
-- La tabla respaldo_temporal (V30) queda sin uso; no se borra.
alter table carga_archivo add column drive_id_reservado text;
alter table carga_archivo add column drive_carpeta_id text;
alter table carga_archivo add column drive_sesion_cifrada bytea;

create unique index carga_drive_reservado_unico on carga_archivo (drive_id_reservado)
    where drive_id_reservado is not null;

alter table carga_archivo add constraint carga_drive_sesion_coherente check
    (drive_sesion_cifrada is null or (drive_id_reservado is not null and drive_carpeta_id is not null));
alter table carga_archivo add constraint carga_drive_ids_validos check
    ((drive_id_reservado is null or drive_id_reservado ~ '^[A-Za-z0-9_-]{1,200}$')
     and (drive_carpeta_id is null or drive_carpeta_id ~ '^[A-Za-z0-9_-]{1,200}$'));
