-- V33: versión 2 del flujo A de Ecuador (skill agregar-flujo-decokasa §1-bis). Fuentes: decisión de Richard del
-- 2026-11-03 y plan a6-gestion-embarques-bl. Solo V32 cambia el esquema; esta migración solo carga datos.
--
-- Cambios sobre la versión 1:
--   * Fase 1 «Pedido y fabricación» (A1–A5) y Fase 2 «Logística» (A6–A12).
--   * A6 pasa a «Gestión de embarques (BL)»: ENVIAR a la etapa nueva, exigiendo BL listos (ya no el tipo Packing List).
--   * A7 nueva «Aprobación de embarque». Aduana y las siguientes etapas suben un número.
--   * Aduana usa un checklist configurable.
--
-- Provisional, pendiente de confirmar por el cliente:
--   * Plazo de A7: 24 horas hábiles (como A3). Plazo de corrección tras un rechazo: 2 días hábiles. Son datos: se cambian
--     en definicion_etapa y en transicion_permitida.plazo_reentrada_*, sin tocar código.
--   * Lista oficial de Aduana: los requisitos quedan PENDIENTES y Aduana no se puede aprobar. No se inventan documentos.
--     La base de prueba carga 16 ficticios con backend/semilla/aduana-ficticia.sql. Antes de producción hace falta la
--     lista oficial y decidir si se exige por pedido o por BL.
--
-- La versión 1 no cambia, salvo perder la vigencia. Ningún pedido cambia de versión (trigger de V2).

do $$
declare
    filas integer;
begin
    update definicion_flujo set vigente = false where pais_codigo = 'EC' and tipo_flujo = 'A' and vigente and version = 1;
    get diagnostics filas = row_count;
    if filas <> 1 then
        raise exception 'V33: debía desactivarse exactamente la versión 1 de A/EC y cambiaron % filas.', filas;
    end if;
end;
$$;

insert into definicion_flujo (id, pais_codigo, tipo_flujo, version, vigente, creada_en) values
    ('01a18f10-0000-7000-8000-000000000001', 'EC', 'A', 2, true, now());

insert into definicion_etapa
    (id, flujo_id, numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo, automatica, revisa_etapa,
     registra_llegada, estimacion_cantidad, estimacion_tipo, estimacion_fuente, fase_numero, fase_nombre,
     gestiona_embarques, aprueba_embarques, checklist_documentos) values
    ('01a18f10-0000-7000-8000-000000000101', '01a18f10-0000-7000-8000-000000000001', 1, 'Crear solicitud de pedido',
     'admin', 'SIN_PLAZO', null, null, false, null, false, null, null, null, 1, 'Pedido y fabricación', false, false, false),
    ('01a18f10-0000-7000-8000-000000000102', '01a18f10-0000-7000-8000-000000000001', 2, 'Codificación',
     'marketing', 'DEFINIDO', 24, 'HORAS_HABILES', false, null, false, null, null, null, 1, 'Pedido y fabricación', false, false, false),
    ('01a18f10-0000-7000-8000-000000000103', '01a18f10-0000-7000-8000-000000000001', 3, 'Revisión y aprobación',
     'asistente_compras', 'DEFINIDO', 24, 'HORAS_HABILES', false, 2, false, null, null, null, 1, 'Pedido y fabricación', false, false, false),
    ('01a18f10-0000-7000-8000-000000000104', '01a18f10-0000-7000-8000-000000000001', 4, 'Análisis y gestión',
     'compras_china', 'DEFINIDO', 3, 'DIAS_HABILES', false, null, false, null, null, null, 1, 'Pedido y fabricación', false, false, false),
    ('01a18f10-0000-7000-8000-000000000105', '01a18f10-0000-7000-8000-000000000001', 5, 'Fabricación',
     'compras_china', 'PENDIENTE', null, null, false, null, false, 31, 'DIAS_CORRIDOS', 'CALCULADORA_ORIGINAL', 1, 'Pedido y fabricación', false, false, false),
    ('01a18f10-0000-7000-8000-000000000106', '01a18f10-0000-7000-8000-000000000001', 6, 'Gestión de embarques (BL)',
     'compras_china', 'DEFINIDO', 10, 'DIAS_HABILES', false, null, false, null, null, null, 2, 'Logística', true, false, false),
    ('01a18f10-0000-7000-8000-000000000107', '01a18f10-0000-7000-8000-000000000001', 7, 'Aprobación de embarque',
     'asistente_compras', 'DEFINIDO', 24, 'HORAS_HABILES', false, 6, false, null, null, null, 2, 'Logística', false, true, false),
    ('01a18f10-0000-7000-8000-000000000108', '01a18f10-0000-7000-8000-000000000001', 8, 'Aduana Ecuador',
     'compras_china', 'PENDIENTE', null, null, false, null, true, 45, 'DIAS_CORRIDOS', 'CALCULADORA_ORIGINAL', 2, 'Logística', false, false, true),
    ('01a18f10-0000-7000-8000-000000000109', '01a18f10-0000-7000-8000-000000000001', 9, 'Autorización de salida',
     'logistica', 'DEFINIDO', 24, 'HORAS_HABILES', false, null, false, null, null, null, 2, 'Logística', false, false, false),
    ('01a18f10-0000-7000-8000-000000000110', '01a18f10-0000-7000-8000-000000000001', 10, 'Bodega',
     'logistica', 'DEFINIDO', 24, 'HORAS_HABILES', false, null, false, null, null, null, 2, 'Logística', false, false, false),
    ('01a18f10-0000-7000-8000-000000000111', '01a18f10-0000-7000-8000-000000000001', 11, 'Incidencias',
     'logistica', 'PENDIENTE', null, null, false, null, false, null, null, null, 2, 'Logística', false, false, false),
    ('01a18f10-0000-7000-8000-000000000112', '01a18f10-0000-7000-8000-000000000001', 12, 'Fin',
     null, 'SIN_PLAZO', null, null, true, null, false, null, null, null, 2, 'Logística', false, false, false);

insert into transicion_permitida
    (id, flujo_id, etapa_origen, accion, etapa_destino, requiere_observacion, requiere_urgencia, requiere_bodega,
     requisitos_documentales, exige_algun_documento, exige_embarques_listos, plazo_reentrada_cantidad,
     plazo_reentrada_tipo) values
    ('01a18f10-0000-7000-8000-000000000201', '01a18f10-0000-7000-8000-000000000001', 1, 'ENVIAR', 2, false, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000202', '01a18f10-0000-7000-8000-000000000001', 2, 'ENVIAR', 3, false, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000203', '01a18f10-0000-7000-8000-000000000001', 3, 'APROBAR', 4, false, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000204', '01a18f10-0000-7000-8000-000000000001', 3, 'RECHAZAR', 2, true, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000205', '01a18f10-0000-7000-8000-000000000001', 4, 'APROBAR', 5, false, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000206', '01a18f10-0000-7000-8000-000000000001', 4, 'DEVOLVER_POR_ERROR', 2, true, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000207', '01a18f10-0000-7000-8000-000000000001', 4, 'DEVOLVER_POR_PRODUCTO_NUEVO', 2, true, true, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000208', '01a18f10-0000-7000-8000-000000000001', 5, 'APROBAR', 6, true, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000209', '01a18f10-0000-7000-8000-000000000001', 6, 'ENVIAR', 7, false, false, false, 'DEFINIDOS', false, true, null, null),
    ('01a18f10-0000-7000-8000-000000000210', '01a18f10-0000-7000-8000-000000000001', 7, 'APROBAR', 8, false, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000211', '01a18f10-0000-7000-8000-000000000001', 7, 'RECHAZAR', 6, true, false, false, 'DEFINIDOS', false, false, 2, 'DIAS_HABILES'),
    ('01a18f10-0000-7000-8000-000000000212', '01a18f10-0000-7000-8000-000000000001', 8, 'APROBAR', 9, false, false, false, 'PENDIENTES', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000213', '01a18f10-0000-7000-8000-000000000001', 8, 'REPORTAR_DEMORA', null, true, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000214', '01a18f10-0000-7000-8000-000000000001', 9, 'APROBAR', 10, false, false, false, 'DEFINIDOS', false, false, null, null),
    ('01a18f10-0000-7000-8000-000000000215', '01a18f10-0000-7000-8000-000000000001', 10, 'APROBAR', 11, false, false, true, 'DEFINIDOS', true, false, null, null),
    ('01a18f10-0000-7000-8000-000000000216', '01a18f10-0000-7000-8000-000000000001', 11, 'APROBAR', 12, false, false, false, 'DEFINIDOS', true, false, null, null);

insert into documento_requerido (transicion_id, tipo_documento) values
    ('01a18f10-0000-7000-8000-000000000201', 'Solicitud'),
    ('01a18f10-0000-7000-8000-000000000202', 'Documentos codificados');

do $$
declare
    v1 constant uuid := '01a13cdb-cc00-7000-8000-000000000001';
    v2 constant uuid := '01a18f10-0000-7000-8000-000000000001';
begin
    if (select count(*) from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = 'A' and vigente) <> 1
       or not exists (select 1 from definicion_flujo where id = v2 and vigente and version = 2) then
        raise exception 'V33: la versión 2 debe ser la única vigente de A/EC.';
    end if;
    if (select count(*) from definicion_etapa where flujo_id = v2) <> 12
       or (select count(*) from transicion_permitida where flujo_id = v2) <> 16
       or (select count(*) from definicion_etapa where flujo_id = v2 and automatica) <> 1
       or not exists (select 1 from definicion_etapa where flujo_id = v2 and numero = 8 and registra_llegada) then
        raise exception 'V33: A v2 debe tener 12 etapas, 16 transiciones, Fin automático y la llegada en Aduana.';
    end if;
    if (select count(*) from definicion_etapa where flujo_id = v1) <> 11
       or (select count(*) from transicion_permitida where flujo_id = v1) <> 14
       or (select count(*) from documento_requerido d join transicion_permitida t on t.id = d.transicion_id
           where t.flujo_id = v1) <> 3
       or exists (select 1 from definicion_etapa where flujo_id = v1 and (fase_numero is not null or gestiona_embarques
           or aprueba_embarques or checklist_documentos))
       or exists (select 1 from transicion_permitida where flujo_id = v1 and (exige_embarques_listos
           or plazo_reentrada_cantidad is not null)) then
        raise exception 'V33: la versión 1 de A/EC no debe cambiar.';
    end if;
    if exists (select 1 from pedido where flujo_id = v2) then
        raise exception 'V33: ningún pedido puede estar ya en la versión 2.';
    end if;
    if (select count(*) from definicion_flujo where vigente) <> 4 then
        raise exception 'V33: B/EC y Colombia deben conservar su versión vigente.';
    end if;
end;
$$;
