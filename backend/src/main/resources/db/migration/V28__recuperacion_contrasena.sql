-- ADR-0033: recuperación y cambio de contraseña.
--
-- 1) Sello de credenciales: momento del último cambio o restablecimiento de contraseña. Los JWT de acceso emitidos
--    antes dejan de valer (validador del recurso). Nula: nunca se cambió desde esta versión.
alter table usuario add column credenciales_cambiadas_en timestamptz;

-- 2) Solicitudes de recuperación. El token en claro nunca se guarda: se crea al enviar el correo y aquí solo queda su
--    SHA-256 (hexadecimal). Un solo intento de envío por solicitud; una solicitud que no salió antes de expirar no se
--    envía nunca (corte temporal propio, distinto de NOTIFICACIONES_ENVIAR_DESDE).
create table recuperacion_contrasena (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    creada_en timestamptz not null,
    expira_en timestamptz not null,
    token_hash char(64) unique,
    estado varchar(10) not null default 'PENDIENTE',
    enviada_en timestamptz,
    usada_en timestamptz,
    anulada_en timestamptz,
    ultimo_error varchar(40),
    constraint recuperacion_estado_valido
        check (estado in ('PENDIENTE', 'ENVIANDO', 'ENVIADA', 'FALLIDA', 'ANULADA', 'USADA')),
    constraint recuperacion_expira_despues check (expira_en > creada_en),
    -- Sin token no se puede haber enviado ni usado.
    constraint recuperacion_token_al_enviar
        check (estado in ('PENDIENTE', 'ANULADA') or token_hash is not null),
    constraint recuperacion_hash_hex check (token_hash is null or token_hash ~ '^[0-9a-f]{64}$')
);

-- Límite por cuenta y hora.
create index recuperacion_por_usuario on recuperacion_contrasena (usuario_id, creada_en);
-- Trabajador de envío.
create index recuperacion_pendientes on recuperacion_contrasena (creada_en) where estado = 'PENDIENTE';
