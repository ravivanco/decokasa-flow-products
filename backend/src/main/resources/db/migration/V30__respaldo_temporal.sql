-- ADR-0034: control del almacenamiento temporal de adjuntos (bucket privado de Supabase, Render Free).
-- Cada adjunto respaldado ocupa una fila: sus bytes reservados (archivo + manifiesto) y el número de fragmentos. La suma
-- de bytes nunca supera la cuota configurada (SUPABASE_CUOTA): la reserva se hace antes de subir, bajo un bloqueo
-- consultivo. La fila se borra cuando sus objetos ya no están en el bucket.
--   SUBIENDO: reservada, fragmentos o manifiesto en camino (o una subida interrumpida).
--   COMPLETO: manifiesto guardado; el adjunto puede recuperarse del bucket.
--   LIBERANDO: la copia de Drive quedó COPIADO o la subida nunca se registró; falta borrar los objetos.
create table respaldo_temporal (
    clave text primary key,
    bytes bigint not null,
    partes integer not null,
    estado varchar(10) not null,
    creado_en timestamptz not null default now(),
    actualizado_en timestamptz not null default now(),
    constraint respaldo_temporal_clave check (char_length(clave) between 1 and 300),
    constraint respaldo_temporal_bytes check (bytes > 0),
    constraint respaldo_temporal_partes check (partes between 1 and 10000),
    constraint respaldo_temporal_estado check (estado in ('SUBIENDO', 'COMPLETO', 'LIBERANDO'))
);

-- Igual que credencial_google: fuera del alcance de la Data API de Supabase.
alter table respaldo_temporal enable row level security;
