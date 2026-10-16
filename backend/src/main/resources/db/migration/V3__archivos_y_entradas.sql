-- V3: archivos reales de la fase 2 (P-05 opción A), entradas estables de etapa (P-18) y relación de revisión (P-17).
-- Fuentes: modelo-datos.md §3.5 (adjunto, con los ajustes del plan de la fase 2), ADR-0003 y las decisiones del equipo
-- del 2026-10-15. P-13, P-17 y P-18 son provisionales del equipo, pendientes de confirmación del cliente.
-- Formatos de archivo y límite de tamaño siguen pendientes (⏸): esta migración no los fija.

-- Entradas de etapa (P-18) --------------------------------------------------------------------------------------------
-- Una fila por cada vez que un pedido entra en una etapa (también en cada reentrada). Su id es el identificador estable
-- de esa entrada: un archivo cumple requisitos solo si pertenece a la entrada actual, sin comparar fechas.
create table entrada_etapa (
    id uuid primary key,
    pedido_id uuid not null references pedido (id),
    etapa smallint not null,
    entrada_en timestamptz not null,
    constraint entrada_etapa_numero_rango check (etapa between 1 and 30),
    -- Permite que adjunto apunte a la entrada junto con su pedido y su etapa, y que la base garantice que coinciden.
    constraint entrada_etapa_id_pedido_etapa unique (id, pedido_id, etapa)
);

create index entrada_etapa_pedido on entrada_etapa (pedido_id, etapa);

create function entrada_etapa_solo_insercion() returns trigger
    language plpgsql as $$
begin
    raise exception 'Las entradas de etapa son de solo inserción: no se modifican ni se borran.';
end;
$$;

create trigger entrada_etapa_sin_cambios
    before update or delete on entrada_etapa
    for each row execute function entrada_etapa_solo_insercion();

create trigger entrada_etapa_sin_vaciar
    before truncate on entrada_etapa
    for each statement execute function entrada_etapa_solo_insercion();

-- La entrada actual (o la última) de cada etapa del pedido. La FK es compuesta: la entrada debe ser del mismo pedido
-- y de la misma etapa. Es diferible porque la aplicación escribe la etapa y su entrada en la misma transacción, en
-- cualquier orden (al crear un pedido, la entrada se inserta después que la etapa).
alter table etapa_pedido add column entrada_id uuid;
alter table etapa_pedido add constraint etapa_pedido_entrada foreign key (entrada_id, pedido_id, numero)
    references entrada_etapa (id, pedido_id, etapa) deferrable initially deferred;

-- Las etapas que ya tenían entrada (solo bases de prueba: V2 no se aplicó en ninguna base persistente) reciben la suya.
insert into entrada_etapa (id, pedido_id, etapa, entrada_en)
select gen_random_uuid(), pedido_id, numero, entrada_en from etapa_pedido where entrada_en is not null;
update etapa_pedido e set entrada_id = n.id
from entrada_etapa n where n.pedido_id = e.pedido_id and n.etapa = e.numero and e.entrada_en is not null;

-- Resuelve los eventos pendientes de la FK del backfill antes del siguiente ALTER TABLE (SQLSTATE 55006).
-- Solo cambia el modo en esta transacción; la FK sigue siendo INITIALLY DEFERRED para la aplicación.
set constraints etapa_pedido_entrada immediate;
set constraints etapa_pedido_entrada deferred;

-- Una etapa en la que el pedido entró tiene su entrada, y una etapa sin entrada no la tiene. Se agrega después de
-- completar los datos anteriores a V3. La aplicación escribe entrada_en y entrada_id en la misma sentencia; una
-- escritura directa que no lo haga se rechaza (el dominio solo no basta frente a esas escrituras).
alter table etapa_pedido add constraint etapa_pedido_entrada_con_entrada
    check ((entrada_id is null) = (entrada_en is null));
create unique index etapa_pedido_entrada_unica on etapa_pedido (entrada_id) where entrada_id is not null;

-- Relación de revisión (P-17) -----------------------------------------------------------------------------------------
-- Etapa cuyos archivos revisa esta etapa. No se deduce de la etapa siguiente: se declara en los datos del flujo.
alter table definicion_etapa add column revisa_etapa smallint;
alter table definicion_etapa add constraint definicion_etapa_revisa foreign key (flujo_id, revisa_etapa)
    references definicion_etapa (flujo_id, numero);
alter table definicion_etapa add constraint definicion_etapa_revisa_otra
    check (revisa_etapa is null or revisa_etapa <> numero);

-- Flujo A de Ecuador, versión 1: A3 "Revisión y aprobación" revisa A2 "Codificación" (requerimientos 4.4). Si A4
-- necesita los documentos de A2 sigue pendiente: no se concede por inferencia.
update definicion_etapa set revisa_etapa = 2 where id = '01a13cdb-cc00-7000-8000-000000000103';

-- Adjuntos (P-05 opción A) ---------------------------------------------------------------------------------------------
-- Solo metadatos; el archivo vive en el almacenamiento local (fase 2) y en Drive (fase 3). Una fila no prueba que el
-- archivo exista en disco: la aplicación lo comprueba al usar el adjunto (requisitos y descarga).
create table adjunto (
    id uuid primary key,
    pedido_id uuid not null references pedido (id),
    etapa smallint not null,
    entrada_id uuid not null,
    tipo_documento text not null,
    -- Nombre original, solo para mostrar. Nunca se usa para guardar ni para decidir el tipo.
    nombre text not null,
    -- Medido y calculado por el servidor sobre lo recibido.
    tamano_bytes bigint not null,
    sha256 text not null,
    -- Tipo detectado por el contenido (firma de los primeros bytes); null si no se reconoce. No es el que declara el
    -- cliente y no decide la aceptación mientras los formatos sigan pendientes.
    tipo_detectado text,
    -- Clave relativa generada por el servidor en el almacenamiento local; no lleva el nombre original.
    clave_local text,
    drive_id text,
    estado_copia text not null,
    intentos integer not null default 0,
    ultimo_error text,
    copiado_en timestamptz,
    subido_por uuid not null references usuario (id),
    subido_en timestamptz not null,
    constraint adjunto_entrada foreign key (entrada_id, pedido_id, etapa)
        references entrada_etapa (id, pedido_id, etapa),
    constraint adjunto_etapa_rango check (etapa between 1 and 30),
    constraint adjunto_tipo_documento_longitud check (char_length(tipo_documento) between 1 and 100),
    constraint adjunto_nombre_longitud check (char_length(nombre) between 1 and 255),
    constraint adjunto_tamano_positivo check (tamano_bytes > 0),
    constraint adjunto_sha256_formato check (sha256 ~ '^[0-9a-f]{64}$'),
    constraint adjunto_clave_local_longitud check (char_length(clave_local) between 1 and 300),
    -- Siempre hay una referencia de almacenamiento (local o Drive). Solo acredita la referencia, no el archivo.
    constraint adjunto_con_almacenamiento check (clave_local is not null or drive_id is not null),
    constraint adjunto_estado_copia check (estado_copia in ('PENDIENTE', 'COPIADO', 'FALLIDO')),
    constraint adjunto_copiado_con_drive check ((estado_copia = 'COPIADO') = (drive_id is not null)),
    constraint adjunto_intentos check (intentos >= 0)
);

create index adjunto_pedido_etapa on adjunto (pedido_id, etapa, subido_en);
create index adjunto_entrada_indice on adjunto (entrada_id);
create index adjunto_copia_pendiente on adjunto (estado_copia) where estado_copia <> 'COPIADO';

create function adjunto_no_se_borra() returns trigger
    language plpgsql as $$
begin
    raise exception 'Un adjunto no se borra: queda para el historial.';
end;
$$;

create trigger adjunto_sin_borrar
    before delete on adjunto
    for each row execute function adjunto_no_se_borra();

create trigger adjunto_sin_vaciar
    before truncate on adjunto
    for each statement execute function adjunto_no_se_borra();

-- Archivos de cada evento del historial: SUBIDO (evento ARCHIVO_SUBIDO) o PRESENTADO (la acción que los usó).
create table evento_adjunto (
    evento_id uuid not null references evento_pedido (id),
    adjunto_id uuid not null references adjunto (id),
    relacion text not null,
    primary key (evento_id, adjunto_id),
    constraint evento_adjunto_relacion check (relacion in ('SUBIDO', 'PRESENTADO'))
);

create unique index evento_adjunto_un_subido on evento_adjunto (adjunto_id) where relacion = 'SUBIDO';
create index evento_adjunto_adjunto on evento_adjunto (adjunto_id);

create trigger evento_adjunto_sin_cambios
    before update or delete on evento_adjunto
    for each row execute function evento_pedido_solo_insercion();

create trigger evento_adjunto_sin_vaciar
    before truncate on evento_adjunto
    for each statement execute function evento_pedido_solo_insercion();
