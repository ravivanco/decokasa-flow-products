-- V21: controles del asistente de IA (fase 7, paso 7.5). Siguiente versión disponible tras V20; V1–V20 no cambian.
--
-- Fuentes:
--   - Límites: F7-D2, aprobados por Richard el 2026-10-22 **para el piloto** (ADR-0022): 60 000 tokens de entrada,
--     max_tokens 4 000, 10 preguntas por conversación, 20 consultas por usuario y día con 1 en curso, 200 consultas
--     diarias y US$10 por mes calendario (hora de Ecuador).
--   - espera_reserva_segundos (300) **no es un límite aprobado**: es el "tiempo de espera + margen" de ADR-0022 para dar
--     por consumida una reserva huérfana (espera del proveedor de 60 s por defecto + margen). Propuesta del equipo.
--   - Tarifa de claude-haiku-5-5: tabla "Model pricing" de la documentación oficial
--     (https://platform.claude.com/docs/en/about-claude/pricing), consultada el 2026-10-22, sin llamar a la API. Se siembra
--     **sin validar**: mientras un admin no la valide contra la fuente, la IA no llama al proveedor (ni a count_tokens).
--
-- La IA sigue apagada en dos sitios: IA_HABILITADA=false en el entorno y configuracion_ia.habilitada=false aquí. El
-- respaldo de US$15 del workspace en la Console no está en esta base ni se da por configurado.

-- 1. Configuración (fila única) ------------------------------------------------------------------------------------------
create table configuracion_ia (
    id smallint primary key default 1,
    habilitada boolean not null,
    motivo_deshabilitada text,
    tokens_entrada_max integer not null,
    tokens_salida_max integer not null,
    preguntas_por_conversacion integer not null,
    consultas_usuario_dia integer not null,
    en_curso_usuario integer not null,
    consultas_dia integer not null,
    presupuesto_mes_micro_usd bigint not null,
    espera_reserva_segundos integer not null,
    origen text not null,
    actualizado_por uuid references usuario (id),
    actualizado_en timestamptz not null,
    version bigint not null default 0,
    constraint configuracion_ia_unica check (id = 1),
    constraint configuracion_ia_motivo check (habilitada or motivo_deshabilitada is not null),
    -- tokens_salida_max <= 4000 (F7-D2). La entrada se compara con el umbral de la tarifa en el servidor.
    constraint configuracion_ia_valores check (tokens_entrada_max > 0 and tokens_salida_max between 1 and 4000
        and preguntas_por_conversacion = 10 and consultas_usuario_dia > 0 and en_curso_usuario > 0
        and consultas_dia > 0 and presupuesto_mes_micro_usd > 0 and espera_reserva_segundos between 60 and 3600)
);

insert into configuracion_ia (id, habilitada, motivo_deshabilitada, tokens_entrada_max, tokens_salida_max,
                              preguntas_por_conversacion, consultas_usuario_dia, en_curso_usuario, consultas_dia,
                              presupuesto_mes_micro_usd, espera_reserva_segundos, origen, actualizado_en)
values (1, false, 'Pendiente de activar por un admin tras validar la tarifa (paso 7.5).', 60000, 4000, 10, 20, 1, 200,
        10000000, 300, 'F7-D2 aprobado por Richard el 2026-10-22 (piloto)', now());

-- 2. Tarifas por modelo y fecha -----------------------------------------------------------------------------------------
-- Precios en micro-USD por millón de tokens (US$0.10/MTok = 100000). Con umbral, una petición cuyo prompt (entrada +
-- escritura y lectura de caché) supera el umbral paga los precios superiores en todas sus categorías.
create table precio_modelo_ia (
    id uuid primary key,
    modelo text not null,
    vigente_desde date not null,
    entrada_micro_usd_mtok bigint not null,
    salida_micro_usd_mtok bigint not null,
    escritura_cache_micro_usd_mtok bigint not null,
    lectura_cache_micro_usd_mtok bigint not null,
    umbral_tokens_entrada integer,
    entrada_superior_micro_usd_mtok bigint,
    salida_superior_micro_usd_mtok bigint,
    escritura_cache_superior_micro_usd_mtok bigint,
    lectura_cache_superior_micro_usd_mtok bigint,
    fuente text not null,
    consultado_en date not null,
    registrado_por uuid references usuario (id),
    registrado_en timestamptz not null,
    validado_por uuid references usuario (id),
    validado_en timestamptz,
    constraint precio_modelo_ia_unico unique (modelo, vigente_desde),
    constraint precio_modelo_ia_modelo check (char_length(modelo) between 1 and 100),
    constraint precio_modelo_ia_fuente check (fuente ~ '^https://' and char_length(fuente) <= 500),
    constraint precio_modelo_ia_positivos check (entrada_micro_usd_mtok > 0 and salida_micro_usd_mtok > 0
        and escritura_cache_micro_usd_mtok > 0 and lectura_cache_micro_usd_mtok > 0),
    -- Tramo superior completo o ausente: num_nonnulls evita que un NULL haga pasar un tramo a medias.
    constraint precio_modelo_ia_tramo check (
        num_nonnulls(umbral_tokens_entrada, entrada_superior_micro_usd_mtok, salida_superior_micro_usd_mtok,
                     escritura_cache_superior_micro_usd_mtok, lectura_cache_superior_micro_usd_mtok) in (0, 5)
        and coalesce(umbral_tokens_entrada, 1) > 0 and coalesce(entrada_superior_micro_usd_mtok, 1) > 0
        and coalesce(salida_superior_micro_usd_mtok, 1) > 0
        and coalesce(escritura_cache_superior_micro_usd_mtok, 1) > 0
        and coalesce(lectura_cache_superior_micro_usd_mtok, 1) > 0),
    constraint precio_modelo_ia_validacion check ((validado_por is null) = (validado_en is null))
);

-- Una tarifa no se corrige: se registra otra con su fecha. Solo se admite marcarla como validada una vez, y una
-- tarifa validada no se borra.
create function precio_modelo_ia_proteger() returns trigger language plpgsql as $$
begin
    if tg_op = 'DELETE' then
        if old.validado_en is not null then
            raise exception 'Una tarifa validada no se borra' using errcode = '23514';
        end if;
        return old;
    end if;
    if old.validado_en is null and new.validado_en is not null
       and to_jsonb(new) - 'validado_por' - 'validado_en' = to_jsonb(old) - 'validado_por' - 'validado_en' then
        return new;
    end if;
    raise exception 'Una tarifa no se modifica; registra otra con su fecha' using errcode = '23514';
end $$;

create trigger precio_modelo_ia_proteger
    before update or delete on precio_modelo_ia
    for each row execute function precio_modelo_ia_proteger();

-- Columnas de precio, en orden: entrada, salida, escritura de caché (5 min), lectura de caché; umbral; y las mismas
-- cuatro del tramo superior.
--   Hasta 100 000 tokens: $0.10, $0.50, $0.125 y $0.01 por MTok.
--   Más de 100 000 tokens: $0.50, $2.50, $0.625 y $0.05 por MTok.
insert into precio_modelo_ia (id, modelo, vigente_desde, entrada_micro_usd_mtok, salida_micro_usd_mtok,
                              escritura_cache_micro_usd_mtok, lectura_cache_micro_usd_mtok, umbral_tokens_entrada,
                              entrada_superior_micro_usd_mtok, salida_superior_micro_usd_mtok,
                              escritura_cache_superior_micro_usd_mtok, lectura_cache_superior_micro_usd_mtok,
                              fuente, consultado_en, registrado_en)
values ('01a160e8-5000-7000-8003-000000000001', 'claude-haiku-5-5', date '2026-10-22',
        100000, 500000, 125000, 10000,
        100000,
        500000, 2500000, 625000, 50000,
        'https://platform.claude.com/docs/en/about-claude/pricing', date '2026-10-22', now());

-- 3. Presupuesto y contadores -------------------------------------------------------------------------------------------
create table presupuesto_ia (
    periodo date primary key,
    limite_micro_usd bigint not null,
    consumido_micro_usd bigint not null default 0,
    reservado_micro_usd bigint not null default 0,
    constraint presupuesto_ia_periodo check (extract(day from periodo) = 1),
    constraint presupuesto_ia_valores check (limite_micro_usd > 0 and consumido_micro_usd >= 0
                                             and reservado_micro_usd >= 0)
);

create table uso_ia_dia (
    dia date primary key,
    consultas integer not null default 0,
    constraint uso_ia_dia_consultas check (consultas >= 0)
);

create table uso_ia_usuario_dia (
    usuario_id uuid not null references usuario (id),
    dia date not null,
    consultas integer not null default 0,
    en_curso integer not null default 0,
    primary key (usuario_id, dia),
    constraint uso_ia_usuario_dia_valores check (consultas >= 0 and en_curso >= 0)
);

create table reserva_ia (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    dia date not null,
    periodo date not null references presupuesto_ia (periodo),
    maximo_micro_usd bigint not null,
    estado text not null,
    costo_micro_usd bigint,
    creada_en timestamptz not null,
    cerrada_en timestamptz,
    constraint reserva_ia_estado check (estado in ('ABIERTA', 'LIQUIDADA', 'LIBERADA', 'CONSUMIDA')),
    constraint reserva_ia_cierre check ((estado = 'ABIERTA') = (cerrada_en is null)
                                        and (estado = 'ABIERTA') = (costo_micro_usd is null)),
    constraint reserva_ia_valores check (maximo_micro_usd > 0 and (costo_micro_usd is null or costo_micro_usd >= 0))
);

create index reserva_ia_abiertas on reserva_ia (creada_en) where estado = 'ABIERTA';

-- 4. Comprobación -------------------------------------------------------------------------------------------------------
do $$
begin
    if (select count(*) from configuracion_ia where id = 1 and not habilitada and tokens_entrada_max = 60000
          and tokens_salida_max = 4000 and consultas_usuario_dia = 20 and en_curso_usuario = 1 and consultas_dia = 200
          and presupuesto_mes_micro_usd = 10000000) <> 1 then
        raise exception 'V21: configuración del asistente distinta de F7-D2';
    end if;
    if exists (select 1 from precio_modelo_ia where validado_en is not null) then
        raise exception 'V21: ninguna tarifa nace validada';
    end if;
end $$;
