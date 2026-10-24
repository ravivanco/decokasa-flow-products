-- V19: Colombia (fase 7, paso 7.2). Siguiente versión disponible tras V18; V1–V18 no cambian.
--
-- Fuentes: decisión de Richard del 2026-10-22 (requerimientos §5, "Definiciones de la fase 7"; no consta como respuesta
-- escrita del cliente): Colombia tiene los mismos flujos A y B y los mismos roles que Ecuador. Bodega temporal de
-- Colombia: "Bodega 1", con la ciudad pendiente. Calendario: las mismas reglas provisionales de Ecuador, configuradas
-- por país en el código (plazos), sin tablas nuevas. La diferencia con "procesos distintos" del documento del cliente
-- (1.3) sigue registrada como pendiente; si el cliente define otro proceso, entrará como una versión nueva de CO.
--
-- Qué hace:
--   1. Alta de CO/A v1 y CO/B v1, vigentes: copia fila por fila las definiciones EC/A v1 (V2, V3, V11, V16) y EC/B v1
--      (V17, V18), con las mismas etapas, roles, plazos (también los PENDIENTE), transiciones, documentos, etapas
--      revisoras y etapa de llegada. Solo cambia el nombre "Aduana Ecuador" por "Aduana Colombia" (documento del cliente,
--      1.4 y 1.5: "Aduana Ecuador o Aduana Colombia"). Nada de EC cambia y ningún pedido existente cambia.
--   2. Bodegas por país: bodega.pais_codigo (las tres existentes quedan en EC), nombre único por país y ciudad
--      opcional solo mientras está pendiente. Alta de "Bodega 1" en CO, activa y con la ciudad pendiente (nula).
--
-- Sin usuarios ni asignaciones de países: los asigna el admin desde la aplicación (ADR-0024). La numeración por país ya
-- existe (secuencia_pedido por país y año, V2): DK-CO-AAAA-NNNN, sujeta al mismo formato pendiente que DK-EC.
--
-- Identificadores fijos, iguales en todos los entornos: UUIDv7 con marca 2026-10-22T00:00:00Z (01a160e8-5000),
-- versión 7 y variante RFC 9562. El grupo de variante distingue el flujo (8001 = CO/A, 8002 = CO/B, 8000 = bodegas) y
-- los 12 últimos dígitos son los de la fila de EC que se copia (…0001 flujo, …01NN etapas, …02NN transiciones).

-- 1. Definiciones CO/A v1 y CO/B v1 --------------------------------------------------------------------------------------

create temporary table v19_copia (tipo text primary key, origen uuid not null, prefijo text not null) on commit drop;
insert into v19_copia (tipo, origen, prefijo)
select 'A', id, '01a160e8-5000-7000-8001-' from definicion_flujo
 where pais_codigo = 'EC' and tipo_flujo = 'A' and version = 1
union all
select 'B', id, '01a160e8-5000-7000-8002-' from definicion_flujo
 where pais_codigo = 'EC' and tipo_flujo = 'B' and version = 1;

do $$
begin
    if (select count(*) from v19_copia) <> 2 then
        raise exception 'V19: faltan las definiciones EC/A v1 o EC/B v1 que se copian.';
    end if;
end;
$$;

insert into definicion_flujo (id, pais_codigo, tipo_flujo, version, vigente, creada_en)
select (c.prefijo || right(f.id::text, 12))::uuid, 'CO', f.tipo_flujo, 1, true, now()
  from v19_copia c join definicion_flujo f on f.id = c.origen;

-- Etapas sin revisa_etapa: su FK apunta a etapas del mismo flujo, así que se completa después.
insert into definicion_etapa (id, flujo_id, numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo,
    automatica, revisa_etapa, registra_llegada)
select (c.prefijo || right(e.id::text, 12))::uuid, (c.prefijo || right(c.origen::text, 12))::uuid, e.numero,
       replace(e.nombre, 'Aduana Ecuador', 'Aduana Colombia'), e.rol_codigo, e.plazo_estado, e.plazo_cantidad,
       e.plazo_tipo, e.automatica, null, e.registra_llegada
  from v19_copia c join definicion_etapa e on e.flujo_id = c.origen;

update definicion_etapa d
   set revisa_etapa = e.revisa_etapa
  from v19_copia c
  join definicion_etapa e on e.flujo_id = c.origen
 where d.id = (c.prefijo || right(e.id::text, 12))::uuid
   and e.revisa_etapa is not null;

insert into transicion_permitida (id, flujo_id, etapa_origen, accion, etapa_destino, requiere_observacion,
    requiere_urgencia, requiere_bodega, requisitos_documentales, exige_algun_documento)
select (c.prefijo || right(t.id::text, 12))::uuid, (c.prefijo || right(c.origen::text, 12))::uuid, t.etapa_origen,
       t.accion, t.etapa_destino, t.requiere_observacion, t.requiere_urgencia, t.requiere_bodega,
       t.requisitos_documentales, t.exige_algun_documento
  from v19_copia c join transicion_permitida t on t.flujo_id = c.origen;

insert into documento_requerido (transicion_id, tipo_documento)
select (c.prefijo || right(t.id::text, 12))::uuid, d.tipo_documento
  from v19_copia c
  join transicion_permitida t on t.flujo_id = c.origen
  join documento_requerido d on d.transicion_id = t.id;

-- 2. Bodegas por país ----------------------------------------------------------------------------------------------------

alter table bodega add column pais_codigo text references pais (codigo);
update bodega set pais_codigo = 'EC';
alter table bodega alter column pais_codigo set not null;

-- La ciudad puede faltar mientras esté pendiente (Bodega 1 de Colombia). Si está, mantiene el formato de V9.
alter table bodega alter column ciudad drop not null;

-- Nombre único dentro de cada país (antes, en todo el catálogo). Mismo nombre de índice: la aplicación lo traduce a 409.
drop index bodega_nombre_unico;
create unique index bodega_nombre_unico on bodega (pais_codigo, lower(nombre));
create index bodega_pais on bodega (pais_codigo);

insert into bodega (id, nombre, ciudad, activa, creada_en, actualizada_en, version, pais_codigo) values
    ('01a160e8-5000-7000-8000-000000000401', 'Bodega 1', null, true, now(), now(), 0, 'CO');

-- Última barrera (ADR-0024): la bodega asignada a un pedido es de su mismo país. La aplicación lo comprueba antes; esto
-- evita que una escritura directa o un error lo salte. No cambia pedido_bodega_activa (V9, V12).
create function pedido_bodega_mismo_pais() returns trigger
    language plpgsql as $$
begin
    if new.bodega_id is not null and not exists (
        select 1 from bodega b where b.id = new.bodega_id and b.pais_codigo = new.pais_codigo) then
        raise exception 'La bodega del pedido debe ser de su mismo país.';
    end if;
    return new;
end;
$$;

create trigger pedido_bodega_mismo_pais
    before insert or update of bodega_id on pedido
    for each row execute function pedido_bodega_mismo_pais();

-- 3. Comprobación final: si algo no cuadra, Flyway revierte la migración entera ----------------------------------------

do $$
declare
    tipo_flujo_ text;
    ec uuid;
    co uuid;
begin
    foreach tipo_flujo_ in array array['A', 'B'] loop
        select id into ec from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = tipo_flujo_ and version = 1;
        select id into co from definicion_flujo where pais_codigo = 'CO' and tipo_flujo = tipo_flujo_ and version = 1;
        if co is null or not (select vigente from definicion_flujo where id = co) then
            raise exception 'V19: falta el flujo CO/% v1 vigente.', tipo_flujo_;
        end if;
        if (select count(*) from definicion_flujo where pais_codigo = 'CO' and tipo_flujo = tipo_flujo_ and vigente) <> 1
        then
            raise exception 'V19: debe haber exactamente un flujo CO/% vigente.', tipo_flujo_;
        end if;
        -- Etapas idénticas salvo el nombre de la aduana.
        if exists (
            (select numero, replace(nombre, 'Aduana Ecuador', 'Aduana Colombia'), rol_codigo, plazo_estado,
                    plazo_cantidad, plazo_tipo, automatica, revisa_etapa, registra_llegada
               from definicion_etapa where flujo_id = ec
             except
             select numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo, automatica, revisa_etapa,
                    registra_llegada
               from definicion_etapa where flujo_id = co)
            union all
            (select numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo, automatica, revisa_etapa,
                    registra_llegada
               from definicion_etapa where flujo_id = co
             except
             select numero, replace(nombre, 'Aduana Ecuador', 'Aduana Colombia'), rol_codigo, plazo_estado,
                    plazo_cantidad, plazo_tipo, automatica, revisa_etapa, registra_llegada
               from definicion_etapa where flujo_id = ec)) then
            raise exception 'V19: las etapas de CO/% no coinciden con las de EC/%.', tipo_flujo_, tipo_flujo_;
        end if;
        -- Transiciones idénticas.
        if exists (
            (select etapa_origen, accion, etapa_destino, requiere_observacion, requiere_urgencia, requiere_bodega,
                    requisitos_documentales, exige_algun_documento from transicion_permitida where flujo_id = ec
             except
             select etapa_origen, accion, etapa_destino, requiere_observacion, requiere_urgencia, requiere_bodega,
                    requisitos_documentales, exige_algun_documento from transicion_permitida where flujo_id = co)
            union all
            (select etapa_origen, accion, etapa_destino, requiere_observacion, requiere_urgencia, requiere_bodega,
                    requisitos_documentales, exige_algun_documento from transicion_permitida where flujo_id = co
             except
             select etapa_origen, accion, etapa_destino, requiere_observacion, requiere_urgencia, requiere_bodega,
                    requisitos_documentales, exige_algun_documento from transicion_permitida where flujo_id = ec)) then
            raise exception 'V19: las transiciones de CO/% no coinciden con las de EC/%.', tipo_flujo_, tipo_flujo_;
        end if;
        -- Documentos obligatorios idénticos por transición.
        if exists (
            (select t.etapa_origen, t.accion, d.tipo_documento from documento_requerido d
               join transicion_permitida t on t.id = d.transicion_id where t.flujo_id = ec
             except
             select t.etapa_origen, t.accion, d.tipo_documento from documento_requerido d
               join transicion_permitida t on t.id = d.transicion_id where t.flujo_id = co)
            union all
            (select t.etapa_origen, t.accion, d.tipo_documento from documento_requerido d
               join transicion_permitida t on t.id = d.transicion_id where t.flujo_id = co
             except
             select t.etapa_origen, t.accion, d.tipo_documento from documento_requerido d
               join transicion_permitida t on t.id = d.transicion_id where t.flujo_id = ec)) then
            raise exception 'V19: los documentos de CO/% no coinciden con los de EC/%.', tipo_flujo_, tipo_flujo_;
        end if;
    end loop;

    if (select count(*) from definicion_etapa e join definicion_flujo f on f.id = e.flujo_id
         where f.pais_codigo = 'CO' and f.tipo_flujo = 'A') <> 11
       or (select count(*) from definicion_etapa e join definicion_flujo f on f.id = e.flujo_id
            where f.pais_codigo = 'CO' and f.tipo_flujo = 'B') <> 12 then
        raise exception 'V19: CO/A debe tener 11 etapas y CO/B 12.';
    end if;

    -- Ecuador intacto: sus definiciones siguen vigentes y ningún pedido cambió de país ni de flujo.
    if (select count(*) from definicion_flujo where pais_codigo = 'EC' and vigente) <> 2 then
        raise exception 'V19: las definiciones vigentes de Ecuador cambiaron.';
    end if;
    if exists (select 1 from pedido p join definicion_flujo f on f.id = p.flujo_id where f.pais_codigo = 'CO') then
        raise exception 'V19: ningún pedido existente puede apuntar a un flujo de Colombia.';
    end if;

    -- Bodegas.
    if exists (select 1 from bodega where pais_codigo is null) then
        raise exception 'V19: toda bodega debe tener país.';
    end if;
    if (select count(*) from bodega where pais_codigo = 'EC'
          and id in ('01a13cdb-cc00-7000-8000-000000000401', '01a13cdb-cc00-7000-8000-000000000402',
                     '01a13cdb-cc00-7000-8000-000000000403')) <> 3 then
        raise exception 'V19: Quito, Guayaquil e Ibarra deben quedar en Ecuador.';
    end if;
    if not exists (select 1 from bodega where id = '01a160e8-5000-7000-8000-000000000401' and pais_codigo = 'CO'
                     and nombre = 'Bodega 1' and ciudad is null and activa) then
        raise exception 'V19: falta Bodega 1 de Colombia, activa y con la ciudad pendiente.';
    end if;
    if exists (select 1 from bodega b join pedido p on p.bodega_id = b.id where b.pais_codigo <> p.pais_codigo) then
        raise exception 'V19: un pedido tiene una bodega de otro país.';
    end if;
end;
$$;
