-- V39: versión 2 del flujo B de Ecuador (Richard, 2026-11-15: «el flujo B es lo mismo que el A, solo que en B aumentan
-- etapas»). B v2 = B v1 hasta Fabricación (con su etapa propia «Revisión de productos propuestos») + la logística de
-- A v2 (Gestión de embarques por BL, Aprobación de embarque, Aduana con checklist confidencial, Autorización, Bodega,
-- Incidencias y Fin), una etapa más adelante. Todo se copia de las definiciones publicadas: mismas reglas, documentos,
-- plazos, fases y estimaciones. Los pedidos B existentes siguen en v1 hasta Fin; los nuevos nacen en v2 (ADR-0004).
do $$
declare
    b1 uuid;
    a2 uuid;
    nuevo constant uuid := '01a18f20-0000-7000-8000-000000000001';
    desactivadas int;
begin
    select id into b1 from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = 'B' and version = 1;
    select id into a2 from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = 'A' and version = 2;
    if b1 is null or a2 is null then
        raise exception 'V39: faltan las definiciones de origen (B/EC v1 o A/EC v2).';
    end if;
    if (select count(*) from definicion_etapa where flujo_id = b1) <> 12
       or (select nombre from definicion_etapa where flujo_id = b1 and numero = 6) <> 'Fabricación'
       or (select count(*) from definicion_etapa where flujo_id = a2) <> 12
       or not (select gestiona_embarques from definicion_etapa where flujo_id = a2 and numero = 6) then
        raise exception 'V39: las definiciones de origen no tienen la forma esperada.';
    end if;

    -- 1. Una sola vigente por país y tipo: primero se retira la v1.
    update definicion_flujo set vigente = false where id = b1 and vigente;
    get diagnostics desactivadas = row_count;
    if desactivadas <> 1 then
        raise exception 'V39: B/EC v1 no era la versión vigente.';
    end if;
    insert into definicion_flujo (id, pais_codigo, tipo_flujo, version, vigente, creada_en)
    values (nuevo, 'EC', 'B', 2, true, now());

    -- 2. Etapas: B1–B6 de B v1; B7–B13 de A6–A12 de A v2. revisa_etapa de A se traslada con el mismo corrimiento.
    insert into definicion_etapa (id, flujo_id, numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo,
        automatica, revisa_etapa, registra_llegada, estimacion_cantidad, estimacion_tipo, estimacion_fuente,
        fase_numero, fase_nombre, gestiona_embarques, aprueba_embarques, checklist_documentos)
    select ('01a18f20-0000-7000-8000-0000000001' || lpad(numero::text, 2, '0'))::uuid, nuevo, numero, nombre,
        rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo, automatica, revisa_etapa, registra_llegada,
        estimacion_cantidad, estimacion_tipo, estimacion_fuente, 1, 'Pedido y fabricación', gestiona_embarques,
        aprueba_embarques, checklist_documentos
    from definicion_etapa where flujo_id = b1 and numero between 1 and 6;
    insert into definicion_etapa (id, flujo_id, numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo,
        automatica, revisa_etapa, registra_llegada, estimacion_cantidad, estimacion_tipo, estimacion_fuente,
        fase_numero, fase_nombre, gestiona_embarques, aprueba_embarques, checklist_documentos)
    select ('01a18f20-0000-7000-8000-0000000001' || lpad((numero + 1)::text, 2, '0'))::uuid, nuevo, numero + 1, nombre,
        rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo, automatica,
        case when revisa_etapa is null then null when revisa_etapa >= 6 then revisa_etapa + 1 end,
        registra_llegada, estimacion_cantidad, estimacion_tipo, estimacion_fuente, 2, 'Logística',
        gestiona_embarques, aprueba_embarques, checklist_documentos
    from definicion_etapa where flujo_id = a2 and numero between 6 and 12;

    -- 3. Transiciones: las de B1–B6 tal cual (B6 → B7 ahora llega a Gestión de embarques); las de A6–A11 corridas una.
    create temporary table v39_transiciones on commit drop as
    select t.id as origen_id, row_number() over (order by fuente, t.etapa_origen, t.accion) as n, fuente,
        t.etapa_origen + desplazamiento as etapa_origen, t.accion,
        case when t.etapa_destino is null then null else t.etapa_destino + desplazamiento end as etapa_destino,
        t.requiere_observacion, t.requiere_urgencia, t.requiere_bodega, t.requisitos_documentales,
        t.exige_algun_documento, t.exige_embarques_listos, t.plazo_reentrada_cantidad, t.plazo_reentrada_tipo
    from (
        select t.*, 1 as fuente, 0 as desplazamiento from transicion_permitida t where t.flujo_id = b1 and t.etapa_origen <= 6
        union all
        select t.*, 2, 1 from transicion_permitida t where t.flujo_id = a2 and t.etapa_origen >= 6
    ) t;
    insert into transicion_permitida (id, flujo_id, etapa_origen, accion, etapa_destino, requiere_observacion,
        requiere_urgencia, requiere_bodega, requisitos_documentales, exige_algun_documento, exige_embarques_listos,
        plazo_reentrada_cantidad, plazo_reentrada_tipo)
    select ('01a18f20-0000-7000-8000-0000000002' || lpad(n::text, 2, '0'))::uuid, nuevo, etapa_origen, accion,
        etapa_destino, requiere_observacion, requiere_urgencia, requiere_bodega, requisitos_documentales,
        exige_algun_documento, exige_embarques_listos, plazo_reentrada_cantidad, plazo_reentrada_tipo
    from v39_transiciones;
    insert into documento_requerido (transicion_id, tipo_documento, orden)
    select ('01a18f20-0000-7000-8000-0000000002' || lpad(v.n::text, 2, '0'))::uuid, d.tipo_documento, d.orden
    from v39_transiciones v join documento_requerido d on d.transicion_id = v.origen_id;

    -- 4. Comprobaciones: forma de la v2 y origen intacto.
    if (select count(*) from definicion_etapa where flujo_id = nuevo) <> 13
       or (select nombre from definicion_etapa where flujo_id = nuevo and numero = 2) <> 'Revisión de productos propuestos'
       or not (select gestiona_embarques from definicion_etapa where flujo_id = nuevo and numero = 7)
       or not (select aprueba_embarques from definicion_etapa where flujo_id = nuevo and numero = 8)
       or not (select checklist_documentos from definicion_etapa where flujo_id = nuevo and numero = 9)
       or not (select automatica from definicion_etapa where flujo_id = nuevo and numero = 13)
       or (select rol_codigo from definicion_etapa where flujo_id = nuevo and numero = 1) <> 'compras_china' then
        raise exception 'V39: la versión 2 del flujo B no quedó como se esperaba.';
    end if;
    if (select count(*) from documento_requerido d join transicion_permitida t on t.id = d.transicion_id
        where t.flujo_id = nuevo and t.etapa_origen = 9 and t.accion = 'APROBAR') <> 16 then
        raise exception 'V39: el checklist de Aduana de B v2 debe tener 16 documentos.';
    end if;
    if (select etapa_destino from transicion_permitida where flujo_id = nuevo and etapa_origen = 6 and accion = 'APROBAR') <> 7
       or (select etapa_destino from transicion_permitida where flujo_id = nuevo and etapa_origen = 8 and accion = 'RECHAZAR') <> 7
       or (select count(*) from transicion_permitida where flujo_id = nuevo and etapa_origen > 13) > 0 then
        raise exception 'V39: las transiciones de B v2 no enlazan como se esperaba.';
    end if;
    if exists (select 1 from pedido where flujo_id = nuevo)
       or (select count(*) from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = 'B' and vigente) <> 1
       or (select count(*) from definicion_etapa where flujo_id = b1) <> 12 then
        raise exception 'V39: la versión anterior o los pedidos existentes cambiaron.';
    end if;
end;
$$;
