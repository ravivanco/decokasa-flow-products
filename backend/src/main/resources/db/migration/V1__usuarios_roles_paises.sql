-- V1: usuarios, roles, permisos, países y renovación de sesión (fase 1, paso 1.5).
-- Diseño: documentos/modelo-datos.md §3.1. Decisiones: ADR-0002 (bloqueo y renovación), ADR-0015 (datos fuera de Git)
-- y requerimientos.md 4.7 (uno o varios roles por usuario, decisión del equipo).
-- Solo siembra países y los 7 roles. Ningún usuario real ni contraseña va en una migración.
-- Las FK no usan ON DELETE CASCADE: los usuarios se desactivan, no se borran, y su rastro se conserva.

-- Países -------------------------------------------------------------------------------------------------------------

create table pais (
    codigo text primary key,
    nombre text not null,
    activo boolean not null,
    constraint pais_codigo_formato check (codigo ~ '^[A-Z]{2}$')
);

-- Roles y permisos ---------------------------------------------------------------------------------------------------

create table rol (
    id uuid primary key,
    codigo text not null,
    nombre text not null,
    creado_en timestamptz not null,
    constraint rol_codigo_unico unique (codigo),
    -- Mismo formato que CodigoRol en el dominio.
    constraint rol_codigo_formato check (codigo ~ '^[a-z_]{3,40}$'),
    constraint rol_nombre_longitud check (char_length(nombre) between 1 and 100)
);

-- Pendiente: la lista de permisos se define con los casos de uso de la fase 1; esta migración no siembra ninguno.
create table rol_permiso (
    rol_id uuid not null references rol (id),
    permiso text not null,
    primary key (rol_id, permiso),
    constraint rol_permiso_longitud check (char_length(permiso) between 1 and 100)
);

-- Usuarios -----------------------------------------------------------------------------------------------------------

create table usuario (
    id uuid primary key,
    nombre text not null,
    correo text not null,
    usuario text not null,
    -- Normalizado: solo dígitos y un + inicial, para entrar con o sin espacios.
    telefono text not null,
    -- Hash BCrypt; nunca la contraseña.
    contrasena_hash text not null,
    activo boolean not null default true,
    -- Bloqueo tras 5 intentos fallidos durante 15 minutos (ADR-0002).
    intentos_fallidos integer not null default 0,
    bloqueado_hasta timestamptz,
    creado_en timestamptz not null,
    actualizado_en timestamptz not null,
    version bigint not null,
    constraint usuario_nombre_longitud check (char_length(nombre) between 1 and 200),
    constraint usuario_usuario_longitud check (char_length(usuario) between 3 and 50),
    constraint usuario_correo_formato check (position('@' in correo) > 1 and char_length(correo) <= 254),
    constraint usuario_telefono_normalizado check (telefono ~ '^\+?[0-9]{7,15}$'),
    constraint usuario_intentos_no_negativos check (intentos_fallidos >= 0)
);

-- Los tres identificadores de inicio de sesión son únicos; correo y usuario sin distinguir mayúsculas.
create unique index usuario_correo_unico on usuario (lower(correo));
create unique index usuario_usuario_unico on usuario (lower(usuario));
create unique index usuario_telefono_unico on usuario (telefono);

-- Uno o varios roles por usuario; solo el admin los asigna (requerimientos.md 4.7).
-- Un usuario sin filas aquí no puede acceder: lo impiden el dominio y el inicio de sesión.
create table usuario_rol (
    usuario_id uuid not null references usuario (id),
    rol_id uuid not null references rol (id),
    asignado_en timestamptz not null,
    primary key (usuario_id, rol_id)
);

create index usuario_rol_rol on usuario_rol (rol_id);

create table usuario_pais (
    usuario_id uuid not null references usuario (id),
    pais_codigo text not null references pais (codigo),
    primary key (usuario_id, pais_codigo)
);

create index usuario_pais_pais on usuario_pais (pais_codigo);

-- Renovación de sesión (ADR-0002): hasta 8 horas desde el inicio de sesión, con rotación por familia --------------

create table token_renovacion (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    -- Una familia por inicio de sesión; si se reutiliza un token revocado, se revoca toda la familia.
    familia_id uuid not null,
    -- SHA-256 del token; nunca el token en claro.
    token_hash text not null,
    creado_en timestamptz not null,
    -- Vencimiento absoluto, igual para toda la familia.
    expira_en timestamptz not null,
    revocado_en timestamptz,
    constraint token_renovacion_hash_unico unique (token_hash),
    constraint token_renovacion_vence_despues check (expira_en > creado_en)
);

create index token_renovacion_usuario on token_renovacion (usuario_id);
create index token_renovacion_familia on token_renovacion (familia_id);

-- Datos iniciales (ADR-0015) -----------------------------------------------------------------------------------------

-- Ecuador activo; Colombia inactivo en v1 (ADR-0009).
insert into pais (codigo, nombre, activo) values
    ('EC', 'Ecuador', true),
    ('CO', 'Colombia', false);

-- Los 7 roles acordados, con identificadores fijos para que sean iguales en todos los entornos.
-- Son UUIDv7 válidos: marca de tiempo 2026-10-12T00:00:00Z (01a12d68-b800), versión 7, variante RFC 9562
-- y un número de orden al final.
-- "consulta" se conserva por decisión del equipo; su alcance está pendiente del cliente.
insert into rol (id, codigo, nombre, creado_en) values
    ('01a12d68-b800-7000-8000-000000000001', 'admin', 'Administrador', now()),
    ('01a12d68-b800-7000-8000-000000000002', 'asistente_compras', 'Asistente de compras', now()),
    ('01a12d68-b800-7000-8000-000000000003', 'marketing', 'Marketing y codificación', now()),
    ('01a12d68-b800-7000-8000-000000000004', 'compras_china', 'Compras China', now()),
    ('01a12d68-b800-7000-8000-000000000005', 'logistica', 'Logística', now()),
    ('01a12d68-b800-7000-8000-000000000006', 'asistente_china', 'Asistente de compras en China', now()),
    ('01a12d68-b800-7000-8000-000000000007', 'consulta', 'Consulta', now());
