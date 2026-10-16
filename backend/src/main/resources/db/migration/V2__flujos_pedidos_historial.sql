-- V2: motor de flujos, numeración, pedidos, etapas de pedido e historial (fase 2, paso 2.1).
-- Diseño: documentos/modelo-datos.md §3.2 a §3.4 y §5. Decisiones del equipo (no del cliente), 2026-10-15:
--   P-02 / ADR-0004: definiciones versionadas de flujo, etapas y transiciones guardadas en datos.
--   P-04: se siembran las once etapas y las transiciones aprobadas del flujo A de Ecuador, versión 1. En la fase 2
--         la aplicación solo habilita acciones de A1 a A4 (incluido A4 → A5); los datos de A5 en adelante no
--         habilitan nada por sí solos.
--   S-1: rol responsable nulo solo en etapas automáticas (Fin).
--   S-2: los documentos de A6 quedan pendientes; una transición con requisitos documentales pendientes no se puede
--        ejecutar, y la ausencia de filas en documento_requerido no significa "sin requisitos".
--   S-3: pedido.bodega_id y las columnas de demora en aduana se agregan en la fase 4.
--   Triggers: el historial no admite UPDATE, DELETE ni TRUNCATE; país, flujo y número de un pedido no cambian.
-- Los plazos y documentos que el cliente no confirmó conservan su condición pendiente: no se inventan valores.
-- No crea tablas de archivos (adjunto, evento_adjunto): dependen de P-05 y P-16, sin aprobar.
-- Las FK no usan ON DELETE CASCADE: no hay borrado físico de pedidos ni de su historial.

-- Definiciones de flujo versionadas (ADR-0004) -----------------------------------------------------------------------

create table definicion_flujo (
    id uuid primary key,
    pais_codigo text not null references pais (codigo),
    tipo_flujo text not null,
    version integer not null,
    vigente boolean not null,
    creada_en timestamptz not null,
    constraint definicion_flujo_tipo check (tipo_flujo in ('A', 'B')),
    constraint definicion_flujo_version_positiva check (version >= 1),
    constraint definicion_flujo_version_unica unique (pais_codigo, tipo_flujo, version),
    -- Permite que pedido apunte al flujo junto con su país y su tipo, y que la base garantice que coinciden.
    constraint definicion_flujo_id_pais_tipo unique (id, pais_codigo, tipo_flujo)
);

-- Una sola versión vigente por país y tipo de flujo.
create unique index definicion_flujo_vigente_unica on definicion_flujo (pais_codigo, tipo_flujo) where vigente;

create table definicion_etapa (
    id uuid primary key,
    flujo_id uuid not null references definicion_flujo (id),
    numero smallint not null,
    nombre text not null,
    -- S-1: nulo solo en etapas automáticas (Fin), que no tienen responsable.
    rol_codigo text references rol (codigo),
    -- DEFINIDO: el cliente dio el plazo. SIN_PLAZO: la etapa no tiene plazo (A1 "—"; Fin).
    -- PENDIENTE: el cliente no lo definió todavía; no equivale a "sin plazo" (Fabricación, Aduana, Incidencias).
    plazo_estado text not null,
    plazo_cantidad integer,
    plazo_tipo text,
    automatica boolean not null default false,
    constraint definicion_etapa_numero_rango check (numero between 1 and 30),
    constraint definicion_etapa_numero_unico unique (flujo_id, numero),
    constraint definicion_etapa_nombre_longitud check (char_length(nombre) between 1 and 100),
    constraint definicion_etapa_rol_solo_automaticas check (rol_codigo is not null or automatica),
    constraint definicion_etapa_plazo_estado check (plazo_estado in ('DEFINIDO', 'SIN_PLAZO', 'PENDIENTE')),
    constraint definicion_etapa_plazo_tipo check (plazo_tipo in ('HORAS_HABILES', 'DIAS_HABILES', 'DIAS_CORRIDOS')),
    constraint definicion_etapa_plazo_cantidad check (plazo_cantidad > 0),
    -- Cantidad y tipo existen si y solo si el plazo está definido.
    constraint definicion_etapa_plazo_completo check (
        (plazo_estado = 'DEFINIDO') = (plazo_cantidad is not null and plazo_tipo is not null)
        and (plazo_cantidad is null) = (plazo_tipo is null))
);

create index definicion_etapa_rol on definicion_etapa (rol_codigo);

-- Una fila por acción permitida en una etapa. Origen y destino se guardan por número de etapa dentro del mismo flujo,
-- y la base comprueba que las dos etapas existen en ese flujo.
create table transicion_permitida (
    id uuid primary key,
    flujo_id uuid not null,
    etapa_origen smallint not null,
    accion text not null,
    -- Nulo si el pedido se queda en la misma etapa (REPORTAR_DEMORA, ELEGIR_BODEGA).
    etapa_destino smallint,
    requiere_observacion boolean not null,
    requiere_urgencia boolean not null,
    requiere_bodega boolean not null,
    -- DEFINIDOS: las filas de documento_requerido son la lista completa (puede estar vacía).
    -- PENDIENTES: el cliente no definió los documentos; la transición no se puede ejecutar (S-2).
    requisitos_documentales text not null,
    constraint transicion_origen foreign key (flujo_id, etapa_origen)
        references definicion_etapa (flujo_id, numero),
    constraint transicion_destino foreign key (flujo_id, etapa_destino)
        references definicion_etapa (flujo_id, numero),
    constraint transicion_accion check (accion in ('ENVIAR', 'APROBAR', 'RECHAZAR', 'DEVOLVER_POR_ERROR',
        'DEVOLVER_POR_PRODUCTO_NUEVO', 'REPORTAR_DEMORA', 'ELEGIR_BODEGA')),
    constraint transicion_accion_unica unique (flujo_id, etapa_origen, accion),
    constraint transicion_destino_segun_accion check (
        (etapa_destino is null) = (accion in ('REPORTAR_DEMORA', 'ELEGIR_BODEGA'))),
    constraint transicion_urgencia_solo_producto_nuevo check (
        not requiere_urgencia or accion = 'DEVOLVER_POR_PRODUCTO_NUEVO'),
    constraint transicion_requisitos_documentales check (requisitos_documentales in ('DEFINIDOS', 'PENDIENTES'))
);

create index transicion_destino_indice on transicion_permitida (flujo_id, etapa_destino);

create table documento_requerido (
    transicion_id uuid not null references transicion_permitida (id),
    tipo_documento text not null,
    primary key (transicion_id, tipo_documento),
    constraint documento_requerido_tipo_longitud check (char_length(tipo_documento) between 1 and 100)
);

-- Numeración del pedido (modelo-datos.md §5) -------------------------------------------------------------------------
--
-- Fuente aprobada (requerimientos 1.3 y 4.2 #5): el número es secuencial, único y no se reutiliza.
-- Decisión técnica del equipo (2026-10-15), no del cliente:
--   * Lectura A: nunca se reutiliza el número de un pedido CONFIRMADO. No se exige que la numeración no tenga
--     huecos: no es un requisito.
--   * NNNN tiene como mínimo cuatro dígitos; los números mayores se escriben completos, sin truncar ni reiniciar.
--     Sin límite de 9 999 y sin cotas arbitrarias del año.
--   * Siguen provisionales (⏸): el formato DK-EC-AAAA-NNNN y la separación del contador por país y año.
--
-- Lo que garantiza esta migración:
--   * Un número confirmado no se reutiliza: el pedido no se borra (trigger de pedido), el contador no retrocede ni
--     se borra (trigger de esta tabla) y pedido.numero es UNIQUE como última barrera.
--
-- Lo que debe cumplir el generador del paso 2.4 (todavía NO implementado):
--   * Dentro de la misma transacción que crea el pedido: INSERT ... ON CONFLICT DO NOTHING para asegurar la fila del
--     país y el año, y UPDATE ... SET ultimo_numero = ultimo_numero + 1 ... RETURNING ultimo_numero. El UPDATE deja
--     la fila bloqueada hasta el final de la transacción: una creación simultánea del mismo país y año espera y lee
--     el valor ya incrementado, así que dos pedidos confirmados nunca comparten número.
--   * Si la transacción se revierte, el incremento se revierte con ella. Ese número no perteneció a ningún pedido
--     confirmado y puede entregarse al siguiente, lo que la lectura A permite. Que con este mecanismo no queden
--     huecos es una consecuencia, no una garantía exigida: un cambio futuro de generador podría dejarlos.
--   * El número solo se expone después del commit: la respuesta de la API, los eventos y los correos se emiten
--     cuando la transacción confirmó, y el número no se escribe en registros técnicos antes de confirmar.
create table secuencia_pedido (
    pais_codigo text not null references pais (codigo),
    anio smallint not null,
    ultimo_numero integer not null,
    version bigint not null,
    primary key (pais_codigo, anio),
    constraint secuencia_pedido_anio_positivo check (anio > 0),
    constraint secuencia_pedido_no_negativo check (ultimo_numero >= 0)
);

create function secuencia_pedido_solo_crece() returns trigger
    language plpgsql as $$
begin
    if tg_op = 'DELETE' then
        raise exception 'La secuencia de números de pedido no se borra.';
    end if;
    if new.pais_codigo <> old.pais_codigo or new.anio <> old.anio then
        raise exception 'El país y el año de una secuencia de pedidos no cambian.';
    end if;
    if new.ultimo_numero < old.ultimo_numero then
        raise exception 'La secuencia de números de pedido no retrocede.';
    end if;
    return new;
end;
$$;

create trigger secuencia_pedido_proteger
    before update or delete on secuencia_pedido
    for each row execute function secuencia_pedido_solo_crece();

-- TRUNCATE no dispara los triggers de fila: sin este, el contador se podía vaciar y volver a entregar números de
-- pedidos confirmados (2.B2-D03). Función propia porque un trigger de sentencia no tiene OLD ni NEW.
create function secuencia_pedido_sin_vaciar() returns trigger
    language plpgsql as $$
begin
    raise exception 'La secuencia de números de pedido no se vacía.';
end;
$$;

create trigger secuencia_pedido_sin_truncar
    before truncate on secuencia_pedido
    for each statement execute function secuencia_pedido_sin_vaciar();

-- Pedidos (modelo-datos.md §3.3; sin bodega ni demora, que llegan en la fase 4: S-3) -------------------------------

create table pedido (
    id uuid primary key,
    -- Único; el de un pedido confirmado nunca se reutiliza (requerimientos 4.2 #5, lectura A). El formato no se fija
    -- con CHECK porque sigue provisional, y NNNN puede tener más de cuatro dígitos.
    numero text not null,
    pais_codigo text not null references pais (codigo),
    tipo_flujo text not null,
    -- Versión del flujo que sigue el pedido; debe ser del mismo país y tipo.
    flujo_id uuid not null,
    nombre text not null,
    descripcion text,
    incluye_productos_nuevos boolean not null default false,
    creado_por uuid not null references usuario (id),
    creado_en timestamptz not null,
    -- Copia del número de la etapa en estado ACTUAL, para listar y filtrar sin unir tablas.
    etapa_actual smallint not null,
    finalizado boolean not null default false,
    finalizado_en timestamptz,
    -- Urgencia activa (requerimientos 4.2 #11): vencimiento propio hasta volver a Análisis.
    urgencia_nivel text,
    urgencia_activada_en timestamptz,
    urgencia_vence_en timestamptz,
    actualizado_en timestamptz not null,
    version bigint not null,
    constraint pedido_numero_unico unique (numero),
    constraint pedido_numero_longitud check (char_length(numero) between 1 and 40),
    constraint pedido_tipo_flujo check (tipo_flujo in ('A', 'B')),
    constraint pedido_flujo foreign key (flujo_id, pais_codigo, tipo_flujo)
        references definicion_flujo (id, pais_codigo, tipo_flujo),
    constraint pedido_nombre_longitud check (char_length(nombre) between 1 and 200),
    constraint pedido_descripcion_longitud check (char_length(descripcion) <= 4000),
    -- Solo el flujo A mezcla productos existentes y nuevos (requerimientos 4.2 #6).
    constraint pedido_productos_nuevos_solo_a check (tipo_flujo = 'A' or not incluye_productos_nuevos),
    constraint pedido_etapa_actual_rango check (etapa_actual between 1 and 30),
    constraint pedido_finalizado_completo check (finalizado = (finalizado_en is not null)),
    constraint pedido_urgencia_nivel check (urgencia_nivel in ('URGENTE', 'PRIORITARIO', 'NORMAL')),
    constraint pedido_urgencia_completa check (
        (urgencia_nivel is null) = (urgencia_activada_en is null)
        and (urgencia_nivel is null) = (urgencia_vence_en is null)),
    constraint pedido_urgencia_vence_despues check (urgencia_vence_en > urgencia_activada_en)
);

create index pedido_flujo on pedido (flujo_id);
create index pedido_creado_por on pedido (creado_por);
-- Listado y bandeja por etapa.
create index pedido_tipo_etapa_abiertos on pedido (tipo_flujo, etapa_actual) where not finalizado;
-- Orden por defecto del listado.
create index pedido_creado_en on pedido (creado_en desc);
-- Urgencias vencidas.
create index pedido_urgencia_vence on pedido (urgencia_vence_en) where urgencia_nivel is not null;

-- País, flujo y número de un pedido no cambian después de crearlo (invariante de requerimientos 4.2), y un pedido no
-- se borra (su número no queda libre para otro).
create function pedido_proteger_identidad() returns trigger
    language plpgsql as $$
begin
    if tg_op = 'DELETE' then
        raise exception 'Un pedido no se borra.';
    end if;
    if new.numero <> old.numero or new.pais_codigo <> old.pais_codigo
            or new.tipo_flujo <> old.tipo_flujo or new.flujo_id <> old.flujo_id then
        raise exception 'El número, el país y el flujo de un pedido no cambian después de crearlo.';
    end if;
    return new;
end;
$$;

create trigger pedido_proteger
    before update or delete on pedido
    for each row execute function pedido_proteger_identidad();

-- Línea de tiempo: una fila por etapa del flujo y por pedido.
create table etapa_pedido (
    id uuid primary key,
    pedido_id uuid not null references pedido (id),
    numero smallint not null,
    estado text not null,
    -- Reasignación por el admin; la regla de quién puede ser asignado vive en el dominio (P-07, pendiente).
    asignado_a uuid references usuario (id),
    -- Última entrada; el plazo se reinicia en cada reentrada.
    entrada_en timestamptz,
    salida_en timestamptz,
    vence_en timestamptz,
    reentradas integer not null default 0,
    motivo_retraso text,
    fecha_planificada date,
    movida_al_lunes boolean not null default false,
    constraint etapa_pedido_numero_unico unique (pedido_id, numero),
    constraint etapa_pedido_numero_rango check (numero between 1 and 30),
    constraint etapa_pedido_estado check (estado in ('PENDIENTE', 'ACTUAL', 'COMPLETADA', 'DEVUELTA')),
    constraint etapa_pedido_actual_con_entrada check (estado <> 'ACTUAL' or entrada_en is not null),
    constraint etapa_pedido_reentradas check (reentradas >= 0),
    constraint etapa_pedido_motivo_longitud check (char_length(motivo_retraso) <= 4000)
);

-- Un pedido tiene a lo sumo una etapa ACTUAL. Que tenga exactamente una mientras no está finalizado lo garantiza el
-- dominio (P-12; Fin se define en la fase 4).
create unique index etapa_pedido_una_actual on etapa_pedido (pedido_id) where estado = 'ACTUAL';
-- Etapas vencidas (tarea de vencimientos, fase 5).
create index etapa_pedido_vence on etapa_pedido (vence_en) where estado = 'ACTUAL';
-- Bandeja del usuario asignado.
create index etapa_pedido_asignado on etapa_pedido (asignado_a) where estado = 'ACTUAL';

-- Historial de solo inserción (modelo-datos.md §3.4; R7) -------------------------------------------------------------

create table evento_pedido (
    id uuid primary key,
    pedido_id uuid not null references pedido (id) on delete restrict,
    etapa smallint not null,
    tipo text not null,
    -- Nulo solo en los eventos del sistema (Fin automático, plazo vencido).
    usuario_id uuid references usuario (id) on delete restrict,
    -- Rol con el que actuó la persona; se conserva aunque después cambien sus roles.
    rol_codigo text,
    -- Hora del servidor (R5).
    ocurrido_en timestamptz not null,
    observacion text,
    nivel_urgencia text,
    -- Detalle según el tipo: etapa de destino, persona asignada, etc.
    datos jsonb,
    constraint evento_pedido_etapa_rango check (etapa between 1 and 30),
    constraint evento_pedido_tipo check (tipo in ('PEDIDO_CREADO', 'ETAPA_ENVIADA', 'ETAPA_APROBADA',
        'ETAPA_RECHAZADA', 'PEDIDO_DEVUELTO', 'DEMORA_REPORTADA', 'BODEGA_ASIGNADA', 'OBSERVACION_AGREGADA',
        'ARCHIVO_SUBIDO', 'ETAPA_REASIGNADA', 'PLAZO_VENCIDO', 'PEDIDO_FINALIZADO')),
    constraint evento_pedido_usuario_o_sistema check (
        usuario_id is not null or tipo in ('PLAZO_VENCIDO', 'PEDIDO_FINALIZADO')),
    constraint evento_pedido_rol_con_usuario check ((usuario_id is null) = (rol_codigo is null)),
    constraint evento_pedido_observacion_longitud check (char_length(observacion) <= 4000),
    constraint evento_pedido_nivel_urgencia check (nivel_urgencia in ('URGENTE', 'PRIORITARIO', 'NORMAL'))
);

create index evento_pedido_historial on evento_pedido (pedido_id, ocurrido_en desc);
create index evento_pedido_fecha on evento_pedido (ocurrido_en);
create index evento_pedido_usuario on evento_pedido (usuario_id, ocurrido_en);

-- La aplicación se conecta como dueña del esquema, así que un REVOKE no basta: el trigger es la barrera real.
-- Una corrección es un evento nuevo, nunca una edición.
create function evento_pedido_solo_insercion() returns trigger
    language plpgsql as $$
begin
    raise exception 'El historial de pedidos es de solo inserción: no se modifica ni se borra.';
end;
$$;

create trigger evento_pedido_sin_cambios
    before update or delete on evento_pedido
    for each row execute function evento_pedido_solo_insercion();

create trigger evento_pedido_sin_vaciar
    before truncate on evento_pedido
    for each statement execute function evento_pedido_solo_insercion();

-- Flujo A de Ecuador, versión 1 (requerimientos.md 1.4, 4.4 y §5; P-04) ---------------------------------------------
--
-- Identificadores fijos para que sean iguales en todos los entornos: UUIDv7 válidos con marca de tiempo
-- 2026-10-15T00:00:00Z (01a13cdb-cc00), versión 7, variante RFC 9562 y un número de orden al final
-- (…0001 flujo, …01NN etapas, …02NN transiciones).

insert into definicion_flujo (id, pais_codigo, tipo_flujo, version, vigente, creada_en) values
    ('01a13cdb-cc00-7000-8000-000000000001', 'EC', 'A', 1, true, now());

-- Plazos: "24h" y "laborables" son respuestas del cliente; se guardan como 24 horas hábiles, y cómo se cuenta una hora
-- hábil (jornada, feriados) sigue pendiente (P-09). "3 días" y "10 días" son del cliente, en días hábiles.
-- Fabricación, Aduana (45 días de tránsito en la calculadora, sin saber si son hábiles o corridos) e Incidencias
-- quedan PENDIENTE: no se inventa un plazo.
insert into definicion_etapa
    (id, flujo_id, numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo, automatica) values
    ('01a13cdb-cc00-7000-8000-000000000101', '01a13cdb-cc00-7000-8000-000000000001', 1,
        'Crear solicitud de pedido', 'admin', 'SIN_PLAZO', null, null, false),
    ('01a13cdb-cc00-7000-8000-000000000102', '01a13cdb-cc00-7000-8000-000000000001', 2,
        'Codificación', 'marketing', 'DEFINIDO', 24, 'HORAS_HABILES', false),
    ('01a13cdb-cc00-7000-8000-000000000103', '01a13cdb-cc00-7000-8000-000000000001', 3,
        'Revisión y aprobación', 'asistente_compras', 'DEFINIDO', 24, 'HORAS_HABILES', false),
    ('01a13cdb-cc00-7000-8000-000000000104', '01a13cdb-cc00-7000-8000-000000000001', 4,
        'Análisis y gestión', 'compras_china', 'DEFINIDO', 3, 'DIAS_HABILES', false),
    ('01a13cdb-cc00-7000-8000-000000000105', '01a13cdb-cc00-7000-8000-000000000001', 5,
        'Fabricación', 'compras_china', 'PENDIENTE', null, null, false),
    ('01a13cdb-cc00-7000-8000-000000000106', '01a13cdb-cc00-7000-8000-000000000001', 6,
        'Embarque', 'compras_china', 'DEFINIDO', 10, 'DIAS_HABILES', false),
    ('01a13cdb-cc00-7000-8000-000000000107', '01a13cdb-cc00-7000-8000-000000000001', 7,
        'Aduana Ecuador', 'compras_china', 'PENDIENTE', null, null, false),
    ('01a13cdb-cc00-7000-8000-000000000108', '01a13cdb-cc00-7000-8000-000000000001', 8,
        'Autorización de salida', 'logistica', 'DEFINIDO', 24, 'HORAS_HABILES', false),
    ('01a13cdb-cc00-7000-8000-000000000109', '01a13cdb-cc00-7000-8000-000000000001', 9,
        'Bodega', 'logistica', 'DEFINIDO', 24, 'HORAS_HABILES', false),
    -- Responsable: respuesta del cliente ("Anderson sube todos los documentos"). Plazo pendiente.
    ('01a13cdb-cc00-7000-8000-000000000110', '01a13cdb-cc00-7000-8000-000000000001', 10,
        'Incidencias', 'logistica', 'PENDIENTE', null, null, false),
    -- Fin: solo visual; se marca sola cuando Incidencias recibe su check (respuesta del cliente). Sin responsable (S-1).
    ('01a13cdb-cc00-7000-8000-000000000111', '01a13cdb-cc00-7000-8000-000000000001', 11,
        'Fin', null, 'SIN_PLAZO', null, null, true);

-- Transiciones aprobadas de la tabla 4.4. ELEGIR_BODEGA como acción separada es diseño del contrato y no se siembra.
-- Documentos PENDIENTES: A6 Embarque ("Packing List / BL", S-2), A9 Bodega ("documentos") y A10 Incidencias
-- ("todos los documentos"): sus tipos no están definidos y esas transiciones no se pueden ejecutar hasta definirlos.
insert into transicion_permitida (id, flujo_id, etapa_origen, accion, etapa_destino, requiere_observacion,
        requiere_urgencia, requiere_bodega, requisitos_documentales) values
    ('01a13cdb-cc00-7000-8000-000000000201', '01a13cdb-cc00-7000-8000-000000000001', 1, 'ENVIAR', 2,
        false, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000202', '01a13cdb-cc00-7000-8000-000000000001', 2, 'ENVIAR', 3,
        false, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000203', '01a13cdb-cc00-7000-8000-000000000001', 3, 'APROBAR', 4,
        false, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000204', '01a13cdb-cc00-7000-8000-000000000001', 3, 'RECHAZAR', 2,
        true, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000205', '01a13cdb-cc00-7000-8000-000000000001', 4, 'APROBAR', 5,
        false, false, false, 'DEFINIDOS'),
    -- Devolver por error: observación obligatoria; archivos extra opcionales (no son requisito).
    ('01a13cdb-cc00-7000-8000-000000000206', '01a13cdb-cc00-7000-8000-000000000001', 4, 'DEVOLVER_POR_ERROR', 2,
        true, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000207', '01a13cdb-cc00-7000-8000-000000000001', 4,
        'DEVOLVER_POR_PRODUCTO_NUEVO', 2, true, true, false, 'DEFINIDOS'),
    -- Fabricación: "dar check y observaciones" (respuesta del cliente).
    ('01a13cdb-cc00-7000-8000-000000000208', '01a13cdb-cc00-7000-8000-000000000001', 5, 'APROBAR', 6,
        true, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000209', '01a13cdb-cc00-7000-8000-000000000001', 6, 'APROBAR', 7,
        false, false, false, 'PENDIENTES'),
    ('01a13cdb-cc00-7000-8000-000000000210', '01a13cdb-cc00-7000-8000-000000000001', 7, 'APROBAR', 8,
        false, false, false, 'DEFINIDOS'),
    -- La nueva fecha de llegada (la escribe David o la calcula el sistema) sigue pendiente; no es parte de este esquema.
    ('01a13cdb-cc00-7000-8000-000000000211', '01a13cdb-cc00-7000-8000-000000000001', 7, 'REPORTAR_DEMORA', null,
        true, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000212', '01a13cdb-cc00-7000-8000-000000000001', 8, 'APROBAR', 9,
        false, false, false, 'DEFINIDOS'),
    ('01a13cdb-cc00-7000-8000-000000000213', '01a13cdb-cc00-7000-8000-000000000001', 9, 'APROBAR', 10,
        false, false, true, 'PENDIENTES'),
    ('01a13cdb-cc00-7000-8000-000000000214', '01a13cdb-cc00-7000-8000-000000000001', 10, 'APROBAR', 11,
        false, false, false, 'PENDIENTES');

-- Documentos obligatorios confirmados en 4.4: la solicitud en A1 y los documentos codificados en A2.
insert into documento_requerido (transicion_id, tipo_documento) values
    ('01a13cdb-cc00-7000-8000-000000000201', 'Solicitud'),
    ('01a13cdb-cc00-7000-8000-000000000202', 'Documentos codificados');
