-- V16: documentos obligatorios de A6, A9 y A10 del flujo A de Ecuador (decisiones confirmadas por Richard el
-- 2026-10-21; requerimientos 4.4 y 5, F4-D4, F4-D7 y F4-D8). Siguiente versión disponible tras V15.
--
--   A6 Embarque:    al menos un documento de tipo "Packing List". El BL y otros adjuntos son opcionales.
--   A9 Bodega:      al menos un documento, de cualquier tipo (además de la bodega elegida).
--   A10 Incidencias: al menos un documento, de cualquier tipo. Sin excepción "sin incidencias".
--
-- Para A9 y A10 no se inventa un tipo comercial: se agrega a la transición el atributo exige_algun_documento ("al
-- menos un documento adjunto, sea del tipo que sea"). Los documentos se siguen validando por pedido, etapa y entrada
-- actual (V3, P-05): el atributo solo cambia cuántos hacen falta, no qué archivos cuentan.
--
-- Se completa la versión 1 del flujo A en lugar de publicar una versión nueva, como V3 (revisa_etapa) y V11
-- (registra_llegada). Estas tres transiciones estaban en PENDIENTES (S-2), así que nunca se pudieron ejecutar: ningún
-- pedido pasó A6, A9 ni A10 con otra regla, y pasar a DEFINIDOS no cambia el resultado de nada ya ocurrido. Los pedidos
-- que esperan en esas etapas quedan desbloqueados con la regla nueva.

-- Default false: el resto de transiciones mantiene su regla (solo los tipos de documento_requerido).
alter table transicion_permitida add column exige_algun_documento boolean not null default false;

-- Con documentos PENDIENTES la transición no se ejecuta: exigir "algún documento" solo tiene sentido si están definidos.
alter table transicion_permitida add constraint transicion_algun_documento_definido
    check (not exige_algun_documento or requisitos_documentales = 'DEFINIDOS');

-- A6 Embarque → A7: Packing List obligatorio.
update transicion_permitida set requisitos_documentales = 'DEFINIDOS'
    where id = '01a13cdb-cc00-7000-8000-000000000209'
      and flujo_id = '01a13cdb-cc00-7000-8000-000000000001'
      and etapa_origen = 6 and accion = 'APROBAR' and requisitos_documentales = 'PENDIENTES';

insert into documento_requerido (transicion_id, tipo_documento) values
    ('01a13cdb-cc00-7000-8000-000000000209', 'Packing List');

-- A9 Bodega → A10 y A10 Incidencias → A11: al menos un documento de cualquier tipo.
update transicion_permitida set requisitos_documentales = 'DEFINIDOS', exige_algun_documento = true
    where flujo_id = '01a13cdb-cc00-7000-8000-000000000001'
      and accion = 'APROBAR' and requisitos_documentales = 'PENDIENTES'
      and ((id = '01a13cdb-cc00-7000-8000-000000000213' and etapa_origen = 9)
        or (id = '01a13cdb-cc00-7000-8000-000000000214' and etapa_origen = 10));

-- Si la siembra no era la esperada (otra fila, ya modificada), la migración falla en lugar de dejar un estado a medias.
do $$
begin
    if (select count(*) from transicion_permitida
            where flujo_id = '01a13cdb-cc00-7000-8000-000000000001'
              and requisitos_documentales = 'PENDIENTES') <> 0 then
        raise exception 'V16: el flujo A conserva transiciones con documentos PENDIENTES.';
    end if;
    if (select count(*) from transicion_permitida
            where id in ('01a13cdb-cc00-7000-8000-000000000213', '01a13cdb-cc00-7000-8000-000000000214')
              and exige_algun_documento) <> 2 then
        raise exception 'V16: A9 y A10 no quedaron con al menos un documento obligatorio.';
    end if;
    if (select count(*) from documento_requerido
            where transicion_id = '01a13cdb-cc00-7000-8000-000000000209') <> 1
       or not exists (select 1 from documento_requerido
            where transicion_id = '01a13cdb-cc00-7000-8000-000000000209' and tipo_documento = 'Packing List') then
        raise exception 'V16: A6 debe exigir exactamente el tipo Packing List.';
    end if;
end;
$$;
