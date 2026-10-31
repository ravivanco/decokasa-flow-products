-- ADR-0034: tokens OAuth de la cuenta Google de la empresa en PostgreSQL, para despliegues sin disco persistente
-- (Render Free). Una sola fila. El contenedor va cifrado con AES-256-GCM por el backend con GOOGLE_OAUTH_LLAVE, que
-- nunca se guarda en la base: quien lea la tabla sin la llave no obtiene tokens. version permite renovar sin bloquear
-- la fila durante la llamada a Google (actualización condicional).
create table credencial_google (
    id smallint primary key,
    contenedor bytea not null,
    version bigint not null,
    actualizado_en timestamptz not null default now(),
    constraint credencial_google_una_fila check (id = 1),
    constraint credencial_google_tamano check (octet_length(contenedor) between 32 and 131072),
    constraint credencial_google_version check (version >= 1)
);

-- En Supabase el esquema public también se publica por la Data API (PostgREST). Con RLS activo y sin políticas, los
-- roles anon y authenticated no leen ni escriben esta tabla; el backend se conecta como dueño de la tabla y no le
-- afecta. En PostgreSQL local no cambia nada.
alter table credencial_google enable row level security;
