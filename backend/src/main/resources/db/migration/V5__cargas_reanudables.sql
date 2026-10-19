-- Paso 3.3: solo metadatos. No modifica V1-V4 ni registra parciales como adjuntos.
create table carga_archivo (
    id uuid primary key,
    usuario_id uuid not null references usuario(id),
    clave_idempotencia uuid not null,
    pedido_id uuid not null references pedido(id),
    etapa smallint not null check (etapa between 1 and 30),
    entrada_id uuid not null,
    nombre text not null check (char_length(nombre) between 1 and 255),
    tipo_documento text not null check (char_length(tipo_documento) between 1 and 100),
    tamano_bytes bigint not null check (tamano_bytes between 1 and 104857600),
    offset_confirmado bigint not null default 0,
    estado text not null default 'RECIBIENDO' check (estado in ('RECIBIENDO','COMPLETADA')),
    adjunto_reservado_id uuid not null unique,
    adjunto_id uuid unique references adjunto(id),
    evento_id uuid unique references evento_pedido(id) deferrable initially deferred,
    creada_en timestamptz not null,
    actualizada_en timestamptz not null,
    constraint carga_entrada foreign key (entrada_id,pedido_id,etapa)
        references entrada_etapa(id,pedido_id,etapa),
    constraint carga_inicio_idempotente unique (usuario_id,clave_idempotencia),
    constraint carga_offset_valido check (offset_confirmado between 0 and tamano_bytes
        and (offset_confirmado = tamano_bytes or offset_confirmado % 4194304 = 0)),
    constraint carga_final_completa check (
        (estado = 'COMPLETADA' and offset_confirmado = tamano_bytes
            and adjunto_id is not null and adjunto_id = adjunto_reservado_id and evento_id is not null)
        or (estado = 'RECIBIENDO' and adjunto_id is null and evento_id is null))
);
create index carga_usuario_pedido on carga_archivo(usuario_id,pedido_id,creada_en);
create index carga_entrada_indice on carga_archivo(entrada_id);
-- Una fila por bloque confirmado; máximo 25 con el límite provisional. No contiene bytes.
create table carga_fragmento (
    carga_id uuid not null references carga_archivo(id),
    inicio bigint not null check (inicio >= 0 and inicio % 4194304 = 0),
    tamano integer not null check (tamano between 1 and 4194304),
    sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
    primary key (carga_id,inicio)
);
-- Sin caducidad o borrado automático: la política de retención sigue pendiente.
