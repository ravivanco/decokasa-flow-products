-- V32: embarques por BL, aprobación de embarque y checklist configurable (esquema, solo aditivo). Siguiente versión
-- libre tras V31. Plan: .claude/PRPs/plans/a6-gestion-embarques-bl.plan.md. Los datos del flujo van aparte, en V33.
--
-- Atributos genéricos del motor: nulos o en false dejan intactos A v1, B y Colombia. Las tablas nuevas son de BL y de
-- envíos. La foto de cada envío, sus revisiones y sus decisiones son de solo inserción (como evento_pedido): cada envío
-- conserva lo que se presentó aunque después cambie un archivo o una fecha.
--
-- Pendientes del cliente (reglas provisionales para probar en local): una sola aprobación autorizada, avance logístico
-- por pedido, lista oficial del checklist y su alcance por pedido o por BL, y la regla de corrección de un Packing List
-- ya aprobado.

-- 1. Atributos de etapa y de transición ------------------------------------------------------------------------------
alter table definicion_etapa
    add column fase_numero smallint,
    add column fase_nombre text,
    add column gestiona_embarques boolean not null default false,
    add column aprueba_embarques boolean not null default false,
    add column checklist_documentos boolean not null default false;
alter table definicion_etapa add constraint definicion_etapa_fase_completa
    check ((fase_numero is null) = (fase_nombre is null));

alter table transicion_permitida
    add column exige_embarques_listos boolean not null default false,
    -- Plazo de la etapa de destino al entrar por esta transición (corrección tras un rechazo). Nulo: su plazo normal.
    add column plazo_reentrada_cantidad integer,
    add column plazo_reentrada_tipo text;
alter table transicion_permitida add constraint transicion_plazo_reentrada_completo
    check ((plazo_reentrada_cantidad is null) = (plazo_reentrada_tipo is null));
alter table transicion_permitida add constraint transicion_plazo_reentrada_valido
    check (plazo_reentrada_cantidad > 0
        and plazo_reentrada_tipo in ('HORAS_HABILES', 'DIAS_HABILES', 'DIAS_CORRIDOS'));

alter table documento_requerido add column orden smallint;

-- 2. BL, Packing Lists y vínculos con pedidos ------------------------------------------------------------------------
create table embarque_bl (
    id uuid primary key,
    pais_codigo text not null references pais (codigo),
    codigo text not null,
    codigo_normalizado text not null,
    fecha_salida date,
    llegada_estimada date,
    llegada_origen text not null default 'SIN_DATOS',
    transito_dias smallint,
    -- Algún pedido aprobó este BL: el Packing List aprobado ya no se sustituye hasta definir la regla de corrección.
    bloqueado_en timestamptz,
    creado_por uuid not null references usuario (id),
    creado_en timestamptz not null,
    actualizado_en timestamptz not null,
    version bigint not null default 0,
    constraint embarque_bl_codigo_longitud check (char_length(codigo) between 3 and 40),
    constraint embarque_bl_codigo_unico unique (pais_codigo, codigo_normalizado),
    constraint embarque_bl_llegada_origen check (llegada_origen in ('AUTOMATICA', 'MANUAL', 'SIN_DATOS')),
    constraint embarque_bl_llegada_coherente check ((llegada_origen = 'SIN_DATOS') = (llegada_estimada is null)),
    constraint embarque_bl_llegada_despues check (llegada_estimada is null or fecha_salida is null
        or llegada_estimada >= fecha_salida)
);

-- Cada reemplazo crea una versión. El archivo es un adjunto normal y se guarda una sola vez.
create table embarque_bl_packing_list (
    id uuid primary key,
    bl_id uuid not null references embarque_bl (id),
    version integer not null,
    adjunto_id uuid not null references adjunto (id),
    subido_por uuid not null references usuario (id),
    subido_en timestamptz not null,
    constraint embarque_bl_pl_version unique (bl_id, version),
    constraint embarque_bl_pl_adjunto unique (adjunto_id),
    constraint embarque_bl_pl_version_positiva check (version >= 1)
);
alter table embarque_bl add column packing_list_vigente_id uuid references embarque_bl_packing_list (id);

create table embarque_bl_pedido (
    id uuid primary key,
    bl_id uuid not null references embarque_bl (id),
    pedido_id uuid not null references pedido (id),
    orden smallint not null,
    vinculado_por uuid not null references usuario (id),
    vinculado_en timestamptz not null,
    desvinculado_por uuid references usuario (id),
    desvinculado_en timestamptz,
    constraint embarque_bl_pedido_desvinculo check ((desvinculado_en is null) = (desvinculado_por is null))
);
create unique index embarque_bl_pedido_vigente on embarque_bl_pedido (bl_id, pedido_id) where desvinculado_en is null;
create index embarque_bl_pedido_por_pedido on embarque_bl_pedido (pedido_id) where desvinculado_en is null;

-- 3. Envíos a aprobación ---------------------------------------------------------------------------------------------
create table envio_embarque (
    id uuid primary key,
    pedido_id uuid not null references pedido (id),
    entrada_id uuid not null references entrada_etapa (id),
    numero integer not null,
    confirma_asignacion boolean not null,
    enviado_por uuid not null references usuario (id),
    enviado_en timestamptz not null,
    estado text not null default 'PENDIENTE',
    cerrado_en timestamptz,
    constraint envio_embarque_numero unique (pedido_id, numero),
    constraint envio_embarque_confirmado check (confirma_asignacion),
    constraint envio_embarque_estado check (estado in ('PENDIENTE', 'RECHAZADO', 'APROBADO')),
    constraint envio_embarque_cierre check ((estado = 'PENDIENTE') = (cerrado_en is null))
);
create unique index envio_embarque_pendiente on envio_embarque (pedido_id) where estado = 'PENDIENTE';

create table envio_embarque_bl (
    envio_id uuid not null references envio_embarque (id),
    bl_id uuid not null references embarque_bl (id),
    codigo text not null,
    fecha_salida date not null,
    llegada_estimada date,
    llegada_origen text not null,
    packing_list_id uuid not null references embarque_bl_packing_list (id),
    primary key (envio_id, bl_id)
);

create table envio_embarque_bl_pedido (
    envio_id uuid not null,
    bl_id uuid not null,
    pedido_id uuid not null references pedido (id),
    vinculado_por uuid not null references usuario (id),
    vinculado_en timestamptz not null,
    primary key (envio_id, bl_id, pedido_id),
    foreign key (envio_id, bl_id) references envio_embarque_bl (envio_id, bl_id)
);

-- Un Packing List cambió durante la revisión: quien aprueba deja constancia de haber revisado la versión nueva.
create table envio_embarque_revision (
    envio_id uuid not null references envio_embarque (id),
    bl_id uuid not null references embarque_bl (id),
    packing_list_id uuid not null references embarque_bl_packing_list (id),
    revisado_por uuid not null references usuario (id),
    revisado_en timestamptz not null,
    primary key (envio_id, packing_list_id)
);

create table envio_embarque_decision (
    id uuid primary key,
    envio_id uuid not null references envio_embarque (id),
    decision text not null,
    usuario_id uuid not null references usuario (id),
    motivo text,
    bl_senalados uuid[] not null default '{}',
    decidido_en timestamptz not null,
    constraint envio_decision_valor check (decision in ('APROBADA', 'RECHAZADA')),
    constraint envio_decision_motivo check (decision <> 'RECHAZADA' or length(trim(motivo)) > 0),
    constraint envio_decision_persona unique (envio_id, usuario_id)
);

create function embarque_solo_insercion() returns trigger
    language plpgsql as $$
begin
    raise exception 'Lo presentado en un embarque es de solo inserción: no se modifica ni se borra.';
end;
$$;
create trigger embarque_bl_packing_list_fija before update or delete on embarque_bl_packing_list
    for each row execute function embarque_solo_insercion();
create trigger envio_embarque_bl_fija before update or delete on envio_embarque_bl
    for each row execute function embarque_solo_insercion();
create trigger envio_embarque_bl_pedido_fija before update or delete on envio_embarque_bl_pedido
    for each row execute function embarque_solo_insercion();
create trigger envio_embarque_revision_fija before update or delete on envio_embarque_revision
    for each row execute function embarque_solo_insercion();
create trigger envio_embarque_decision_fija before update or delete on envio_embarque_decision
    for each row execute function embarque_solo_insercion();

-- 4. Historial: lo que ocurre con BL, envíos y decisiones ------------------------------------------------------------
alter table evento_pedido drop constraint evento_pedido_tipo;
alter table evento_pedido add constraint evento_pedido_tipo check (tipo in ('PEDIDO_CREADO', 'ETAPA_ENVIADA',
    'ETAPA_APROBADA', 'ETAPA_RECHAZADA', 'PEDIDO_DEVUELTO', 'DEMORA_REPORTADA', 'BODEGA_ASIGNADA',
    'OBSERVACION_AGREGADA', 'ARCHIVO_SUBIDO', 'ETAPA_REASIGNADA', 'PLAZO_VENCIDO', 'MOTIVO_RETRASO_REGISTRADO',
    'PEDIDO_FINALIZADO', 'EMBARQUE_REGISTRADO'));

-- 5. Alertas de WhatsApp simuladas: ningún envío real ni teléfono guardado hasta confirmar proveedor y credenciales.
create table notificacion_whatsapp (
    id uuid primary key,
    pedido_id uuid not null references pedido (id),
    evento_id uuid not null,
    usuario_id uuid not null references usuario (id),
    texto text not null,
    estado text not null default 'SIMULADO',
    creado_en timestamptz not null,
    constraint notificacion_whatsapp_estado check (estado in ('SIMULADO')),
    constraint notificacion_whatsapp_unica unique (evento_id, usuario_id)
);
create index notificacion_whatsapp_por_pedido on notificacion_whatsapp (pedido_id, creado_en);
