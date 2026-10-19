-- Fase 3, parte A, paso 3.2. Aditiva sobre V3; no contiene binarios, credenciales ni tokens OAuth.
-- No aplicada durante la codificación. En producción la reversión es otra migración hacia delante.
-- No recrear adjunto/entrada_etapa/evento_adjunto ni cambiar sus UUID o los triggers de historial.
alter table adjunto add column drive_id_reservado text;
alter table adjunto add column drive_enlace text;
alter table adjunto add column proximo_intento_en timestamptz;
alter table adjunto add column reclamado_por uuid;
alter table adjunto add column reclamado_hasta timestamptz;
alter table adjunto add constraint adjunto_lease_completo
    check ((reclamado_por is null) = (reclamado_hasta is null));
alter table adjunto add constraint adjunto_enlace_copiado
    check (drive_enlace is null or (estado_copia = 'COPIADO' and drive_enlace like 'https://%'));
alter table adjunto add constraint adjunto_copia_sin_lease
    check (estado_copia <> 'COPIADO' or reclamado_por is null);
-- drive_id sigue siendo exclusivamente definitivo: el CHECK de V3 se conserva.
create unique index adjunto_drive_reservado_unico on adjunto (drive_id_reservado)
    where drive_id_reservado is not null;

create table notificacion (
    id uuid primary key,
    pedido_id uuid not null references pedido (id),
    -- Historial usa persist de JPA; su flush puede ocurrir después de la inserción JDBC del outbox.
    -- La FK se comprueba al commit de LA MISMA transacción, nunca contra un evento de otra transacción.
    evento_id uuid not null references evento_pedido (id) deferrable initially deferred,
    -- Sin política aprobada de destinatarios, null significa pendiente de resolución, nunca enviable.
    -- No se inventa un correo ni se pierde el aviso confirmado por no tener todavía un destinatario.
    destinatario text,
    destinatario_clave text generated always as (coalesce(lower(btrim(destinatario)), '')) stored,
    plantilla text not null,
    asunto text not null,
    datos jsonb not null,
    estado text not null default 'PENDIENTE',
    intentos integer not null default 0,
    proximo_intento_en timestamptz,
    ultimo_error text,
    creada_en timestamptz not null,
    enviada_en timestamptz,
    proveedor_id text,
    message_id text not null,
    resultado_incierto boolean not null default false,
    reclamado_por uuid,
    reclamado_hasta timestamptz,
    constraint notificacion_evento_destinatario unique (evento_id, destinatario_clave),
    constraint notificacion_destinatario_valido check (destinatario is null or
        (char_length(destinatario) between 3 and 254 and destinatario = btrim(destinatario)
         and position(chr(10) in destinatario) = 0 and position(chr(13) in destinatario) = 0)),
    constraint notificacion_plantilla_longitud check (char_length(plantilla) between 1 and 100),
    constraint notificacion_asunto_longitud check (char_length(asunto) between 1 and 500),
    constraint notificacion_datos_objeto check (jsonb_typeof(datos) = 'object'),
    constraint notificacion_estado check (estado in ('PENDIENTE','ENVIADA','FALLIDA')),
    constraint notificacion_intentos check (intentos >= 0),
    constraint notificacion_message_id_estable check
        (message_id = '<notificacion-' || id::text || '@decokasa.invalid>'),
    constraint notificacion_lease_completo check ((reclamado_por is null) = (reclamado_hasta is null)),
    constraint notificacion_enviada_completa check ((estado = 'ENVIADA') = (enviada_en is not null)
        and (estado <> 'ENVIADA' or (destinatario is not null and proveedor_id is not null
             and not resultado_incierto and reclamado_por is null))),
    constraint notificacion_incierta_no_enviada check (not resultado_incierto or estado = 'FALLIDA')
);

create index notificacion_pedido on notificacion (pedido_id, creada_en);
create index notificacion_pendiente on notificacion (proximo_intento_en, creada_en)
    where estado = 'PENDIENTE' and destinatario is not null and not resultado_incierto;
create index notificacion_destinatario_pendiente on notificacion (creada_en)
    where estado = 'PENDIENTE' and destinatario is null;
-- Sin backfill de avisos históricos: no debe provocar correos retroactivos al activar una integración.
