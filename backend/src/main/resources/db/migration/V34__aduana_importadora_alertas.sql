-- V34: pedido del cliente comunicado por Richard el 2026-11-14.
--   1. Importadora del BL: DECOKASA o Latinoamérica, elegida en Gestión de embarques.
--   2. Checklist oficial de Aduana (A8 de A/EC v2): 16 documentos, obligatorios y por pedido (provisional: falta
--      confirmar si alguno es opcional y si se exige por BL). Completa una transición que estaba PENDIENTES y nunca se
--      pudo ejecutar, como V16: no cambia nada ya ocurrido. Sustituye los requisitos ficticios de una base de prueba.
--   3. Alerta cuando la etapa activa supera la fecha planificada al crear el pedido (PLAN_VENCIDO, evento del sistema).
--   4. Cola de WhatsApp: los avisos se simulan o, con proveedor y credenciales, se envían.

alter table embarque_bl add column importadora text;
alter table embarque_bl add constraint embarque_bl_importadora check (importadora in ('DECOKASA', 'LATINOAMERICA'));
alter table envio_embarque_bl add column importadora text;

do $$
declare
    transicion constant uuid := '01a18f10-0000-7000-8000-000000000212';
begin
    if not exists (select 1 from transicion_permitida where id = transicion and etapa_origen = 8 and accion = 'APROBAR') then
        raise exception 'V34: no existe el APROBAR de Aduana de A/EC v2.';
    end if;
    delete from documento_requerido where transicion_id = transicion;
    insert into documento_requerido (transicion_id, tipo_documento, orden) values
        (transicion, 'Documento a declarar', 1),
        (transicion, 'Invoice', 2),
        (transicion, 'Certificado de origen', 3),
        (transicion, 'Seguro', 4),
        (transicion, 'Pago destino', 5),
        (transicion, 'Liberación', 6),
        (transicion, 'Pago THC', 7),
        (transicion, 'Gastos locales', 8),
        (transicion, 'Liquidaciones', 9),
        (transicion, 'Garantías', 10),
        (transicion, 'Bodega', 11),
        (transicion, 'Salida autorizada', 12),
        (transicion, 'Devolución', 13),
        (transicion, 'Otro', 14),
        (transicion, 'FOB real', 15),
        (transicion, 'FOB declarado', 16);
    update transicion_permitida set requisitos_documentales = 'DEFINIDOS' where id = transicion;
    if (select count(*) from documento_requerido where transicion_id = transicion) <> 16 then
        raise exception 'V34: el checklist de Aduana debe tener 16 documentos.';
    end if;
end;
$$;

alter table evento_pedido drop constraint evento_pedido_tipo;
alter table evento_pedido add constraint evento_pedido_tipo check (tipo in ('PEDIDO_CREADO', 'ETAPA_ENVIADA',
    'ETAPA_APROBADA', 'ETAPA_RECHAZADA', 'PEDIDO_DEVUELTO', 'DEMORA_REPORTADA', 'BODEGA_ASIGNADA',
    'OBSERVACION_AGREGADA', 'ARCHIVO_SUBIDO', 'ETAPA_REASIGNADA', 'PLAZO_VENCIDO', 'MOTIVO_RETRASO_REGISTRADO',
    'PEDIDO_FINALIZADO', 'EMBARQUE_REGISTRADO', 'PLAN_VENCIDO'));
alter table evento_pedido drop constraint evento_pedido_usuario_o_sistema;
alter table evento_pedido add constraint evento_pedido_usuario_o_sistema check (
    usuario_id is not null or tipo in ('PLAZO_VENCIDO', 'PEDIDO_FINALIZADO', 'PLAN_VENCIDO'));

alter table aviso_etapa drop constraint aviso_etapa_tipo;
alter table aviso_etapa add constraint aviso_etapa_tipo
    check (tipo in ('CREACION', 'LLEGADA', 'DEVOLUCION', 'REASIGNACION', 'CIERRE', 'VENCIMIENTO'));

alter table notificacion_whatsapp drop constraint notificacion_whatsapp_estado;
alter table notificacion_whatsapp add column enviado_en timestamptz,
    add column proveedor_id text,
    add column error text;
-- SIMULADO: sin proveedor. PENDIENTE → ENVIANDO → ENVIADA o FALLIDA con proveedor. ENVIANDO no se reintenta solo,
-- para no duplicar un mensaje si el proceso se corta tras el envío.
alter table notificacion_whatsapp add constraint notificacion_whatsapp_estado
    check (estado in ('SIMULADO', 'PENDIENTE', 'ENVIANDO', 'ENVIADA', 'FALLIDA'));
create index notificacion_whatsapp_pendientes on notificacion_whatsapp (creado_en) where estado = 'PENDIENTE';
