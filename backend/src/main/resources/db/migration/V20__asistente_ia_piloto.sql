-- V20: asistente de IA del piloto (fase 7, paso 7.4). Siguiente versión disponible tras V19; V1–V19 no cambian.
--
-- Fuentes: F7-D1 (opción C), F7-D4 y F7-D5, y ADR-0020 a ADR-0023, aprobados por Richard el 2026-10-22 **solo para el
-- piloto con datos ficticios**. Modelo de datos: modelo-datos.md, sección 3.8.
--
-- Qué hace:
--   1. conversacion_ia: una conversación de una persona sobre un único pedido, con vencimiento de 30 días como máximo.
--   2. mensaje_ia: preguntas y respuestas (texto, referencias verificadas, documentos enviados y excluidos). Se borran
--      en cascada con su conversación.
--   3. consulta_ia: registro de consumo de cada llamada al proveedor (actor, fecha, pedido, modelo, tokens, costo,
--      resultado y documentos enviados), **sin texto ni citas**. Solo inserción; al borrar la conversación, su
--      conversacion_id pasa a nulo y el registro se conserva.
--
-- No incluye configuracion_ia, precio_modelo_ia, presupuesto_ia ni los contadores de uso: son del paso 7.5 (reserva
-- atómica, ADR-0022). Tampoco hay datos: ni usuarios, ni pedidos, ni documentos.

-- 1. Conversaciones -----------------------------------------------------------------------------------------------------
create table conversacion_ia (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    pedido_id uuid not null references pedido (id),
    -- Copia del número y del país del pedido, que no cambian (pedido_proteger_identidad, V2). Lo comprueba el trigger.
    numero_pedido text not null,
    pais_codigo text not null references pais (codigo),
    creada_en timestamptz not null,
    expira_en timestamptz not null,
    preguntas smallint not null default 0,
    -- 720 horas exactas, como Duration.ofDays(30) en Java, sin depender de la zona horaria de la sesión.
    constraint conversacion_ia_vencimiento check (expira_en > creada_en and expira_en <= creada_en + interval '720 hours'),
    constraint conversacion_ia_preguntas check (preguntas between 0 and 10)
);

create index conversacion_ia_usuario on conversacion_ia (usuario_id, creada_en desc);
create index conversacion_ia_expira on conversacion_ia (expira_en);

create function conversacion_ia_validar() returns trigger language plpgsql as $$
begin
    if tg_op = 'INSERT' then
        if not exists (select 1 from pedido p
                        where p.id = new.pedido_id and p.numero = new.numero_pedido
                          and p.pais_codigo = new.pais_codigo) then
            raise exception 'La conversación no coincide con su pedido' using errcode = '23514';
        end if;
        return new;
    end if;
    -- Solo avanza el contador de preguntas; la identidad, el autor y las fechas no cambian.
    if (new.id, new.usuario_id, new.pedido_id, new.numero_pedido, new.pais_codigo, new.creada_en, new.expira_en)
            is distinct from
       (old.id, old.usuario_id, old.pedido_id, old.numero_pedido, old.pais_codigo, old.creada_en, old.expira_en)
       or new.preguntas < old.preguntas then
        raise exception 'Una conversación solo cambia su número de preguntas' using errcode = '23514';
    end if;
    return new;
end $$;

create trigger conversacion_ia_validar
    before insert or update on conversacion_ia
    for each row execute function conversacion_ia_validar();

-- 2. Mensajes -----------------------------------------------------------------------------------------------------------
create table mensaje_ia (
    id uuid primary key,
    conversacion_id uuid not null references conversacion_ia (id) on delete cascade,
    orden smallint not null,
    autor text not null,
    texto text not null,
    referencias jsonb not null default '[]',
    documentos_enviados uuid[] not null default '{}',
    documentos_excluidos jsonb not null default '[]',
    evidencia_suficiente boolean,
    creado_en timestamptz not null,
    constraint mensaje_ia_orden_unico unique (conversacion_id, orden),
    constraint mensaje_ia_orden check (orden between 1 and 20),
    constraint mensaje_ia_autor check (autor in ('USUARIO', 'ASISTENTE')),
    -- Pares pregunta (impar) y respuesta (par).
    constraint mensaje_ia_autor_orden check ((autor = 'USUARIO') = (orden % 2 = 1)),
    constraint mensaje_ia_evidencia check ((autor = 'ASISTENTE') = (evidencia_suficiente is not null)),
    constraint mensaje_ia_pregunta check (
        autor = 'ASISTENTE'
        or (char_length(texto) between 1 and 2000 and referencias = '[]'::jsonb
            and documentos_enviados = '{}'::uuid[] and documentos_excluidos = '[]'::jsonb)),
    constraint mensaje_ia_json check (jsonb_typeof(referencias) = 'array' and jsonb_typeof(documentos_excluidos) = 'array')
);

-- Un mensaje no se edita: solo se borra con su conversación.
create function mensaje_ia_inmutable() returns trigger language plpgsql as $$
begin
    raise exception 'Un mensaje del asistente no se modifica' using errcode = '23514';
end $$;

create trigger mensaje_ia_inmutable
    before update on mensaje_ia
    for each row execute function mensaje_ia_inmutable();

-- 3. Registro de consumo ------------------------------------------------------------------------------------------------
create table consulta_ia (
    id uuid primary key,
    conversacion_id uuid references conversacion_ia (id) on delete set null,
    usuario_id uuid not null references usuario (id),
    pedido_id uuid not null references pedido (id),
    pais_codigo text not null references pais (codigo),
    -- Nulo si el proveedor no llegó a responder.
    modelo text,
    tokens_entrada bigint not null default 0,
    tokens_salida bigint not null default 0,
    tokens_cache_escritura bigint not null default 0,
    tokens_cache_lectura bigint not null default 0,
    costo_micro_usd bigint not null default 0,
    resultado text not null,
    documentos_enviados uuid[] not null default '{}',
    creada_en timestamptz not null,
    constraint consulta_ia_tokens check (tokens_entrada >= 0 and tokens_salida >= 0 and tokens_cache_escritura >= 0
                                         and tokens_cache_lectura >= 0 and costo_micro_usd >= 0),
    constraint consulta_ia_resultado check (resultado in ('RESPONDIDA', 'SIN_EVIDENCIA', 'INCOMPLETA', 'RECHAZADA',
                                                          'LIMITE', 'ERROR_SIN_COBRO', 'ERROR_INCIERTO'))
);

create index consulta_ia_creada on consulta_ia (creada_en);
create index consulta_ia_usuario on consulta_ia (usuario_id, creada_en);
create index consulta_ia_conversacion on consulta_ia (conversacion_id);

-- Solo inserción. La única actualización admitida es la de ON DELETE SET NULL al borrar su conversación.
create function consulta_ia_solo_insercion() returns trigger language plpgsql as $$
begin
    if tg_op = 'UPDATE' and old.conversacion_id is not null and new.conversacion_id is null
       and to_jsonb(new) - 'conversacion_id' = to_jsonb(old) - 'conversacion_id' then
        return new;
    end if;
    raise exception 'consulta_ia es un registro de solo inserción' using errcode = '23514';
end $$;

create trigger consulta_ia_solo_insercion
    before update or delete on consulta_ia
    for each row execute function consulta_ia_solo_insercion();

-- 4. Comprobación ---------------------------------------------------------------------------------------------------------
do $$
begin
    if (select count(*) from information_schema.tables
         where table_schema = current_schema()
           and table_name in ('conversacion_ia', 'mensaje_ia', 'consulta_ia')) <> 3 then
        raise exception 'V20: faltan tablas del asistente';
    end if;
    if exists (select 1 from information_schema.columns
                where table_schema = current_schema() and table_name = 'consulta_ia'
                  and column_name in ('texto', 'pregunta', 'respuesta', 'referencias')) then
        raise exception 'V20: consulta_ia no puede guardar texto';
    end if;
end $$;
