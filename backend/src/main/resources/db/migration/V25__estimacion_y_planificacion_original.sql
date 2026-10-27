-- V25: duraciones de referencia para estimar la llegada y planificación original de cada pedido (ADR-0030, decisión
-- del equipo del 2026-10-26; no es una respuesta del cliente). Siguiente versión disponible tras V24; V1–V24 no
-- cambian. Irreversible: una corrección sería otra migración hacia adelante.
--
-- Reglas:
--   * La duración de referencia solo sirve para estimar fechas. No es un plazo de responsabilidad: no genera
--     vencimientos ni alertas, y plazo_estado sigue PENDIENTE donde lo estaba.
--   * Solo una etapa sin plazo definido admite referencia: un plazo aprobado siempre manda.
--   * Como V3 y V11, es un atributo nuevo de la versión 1 de los flujos, no un cambio de una regla vigente.
--   * Colombia no recibe referencias: la calculadora original es de "Llegada a Ecuador" y nada de Ecuador se copia a
--     Colombia por analogía (requerimientos §5, fase 7).
--   * La planificación original se guarda al crear cada pedido nuevo y no se modifica. A los pedidos anteriores no se
--     les inserta nada: su referencia se reconstruye al consultar y se identifica como reconstruida.

-- 1. Duración de referencia por etapa.
alter table definicion_etapa
    add column estimacion_cantidad integer,
    add column estimacion_tipo text,
    add column estimacion_fuente text;

alter table definicion_etapa add constraint definicion_etapa_estimacion_cantidad
    check (estimacion_cantidad > 0);
alter table definicion_etapa add constraint definicion_etapa_estimacion_tipo
    check (estimacion_tipo in ('HORAS_HABILES', 'DIAS_HABILES', 'DIAS_CORRIDOS'));
alter table definicion_etapa add constraint definicion_etapa_estimacion_fuente
    check (estimacion_fuente in ('CALCULADORA_ORIGINAL', 'REQUERIMIENTOS_4_5'));
alter table definicion_etapa add constraint definicion_etapa_estimacion_completa
    check ((estimacion_cantidad is null) = (estimacion_tipo is null)
       and (estimacion_cantidad is null) = (estimacion_fuente is null));
alter table definicion_etapa add constraint definicion_etapa_estimacion_sin_plazo_definido
    check (estimacion_cantidad is null or plazo_estado = 'PENDIENTE');

-- Ecuador, versión 1. Se identifica cada etapa por flujo, número, nombre y plazo pendiente; si alguna no coincide, la
-- migración falla en lugar de marcar otra etapa.
--   A5 / B6 Fabricación: 31 días corridos (calculadora, inicio → fin de fabricación).
--   A7 / B8 Aduana Ecuador: 45 días corridos (calculadora, embarque → llegada).
--   B2 Revisión de productos propuestos: 24 h hábiles (requerimientos 4.5; propuesta sin confirmar, F6-D4).
-- Los 3, 8 y 12 días de la calculadora no se cargan: los sustituyen los plazos aprobados de A2–A4 / B3–B5 y A6 / B7.
update definicion_etapa e
set estimacion_cantidad = r.cantidad, estimacion_tipo = r.tipo, estimacion_fuente = r.fuente
from (values
        ('A', 5, 'Fabricación', 31, 'DIAS_CORRIDOS', 'CALCULADORA_ORIGINAL'),
        ('A', 7, 'Aduana Ecuador', 45, 'DIAS_CORRIDOS', 'CALCULADORA_ORIGINAL'),
        ('B', 2, 'Revisión de productos propuestos', 24, 'HORAS_HABILES', 'REQUERIMIENTOS_4_5'),
        ('B', 6, 'Fabricación', 31, 'DIAS_CORRIDOS', 'CALCULADORA_ORIGINAL'),
        ('B', 8, 'Aduana Ecuador', 45, 'DIAS_CORRIDOS', 'CALCULADORA_ORIGINAL'))
    as r (tipo_flujo, numero, nombre, cantidad, tipo, fuente),
    definicion_flujo f
where f.id = e.flujo_id
  and f.pais_codigo = 'EC' and f.version = 1 and f.tipo_flujo = r.tipo_flujo
  and e.numero = r.numero and e.nombre = r.nombre and e.plazo_estado = 'PENDIENTE';

do $$
begin
    if (select count(*) from definicion_etapa where estimacion_cantidad is not null) <> 5 then
        raise exception 'V25: se esperaban 5 etapas de Ecuador con duración de referencia; revisar las definiciones.';
    end if;
end
$$;

-- 2. Planificación original de cada pedido nuevo (solo inserción).
create table planificacion_original (
    pedido_id uuid primary key references pedido (id),
    flujo_id uuid not null references definicion_flujo (id),
    -- Versión de las reglas de estimación con que se calculó (ADR-0030).
    reglas text not null,
    calculada_en timestamptz not null,
    -- Nula si alguna duración necesaria no estaba documentada (por ejemplo, en Colombia).
    llegada_prevista date,
    finalizacion_prevista date,
    constraint planificacion_original_reglas check (reglas in ('ESTIMACION_REFERENCIA_1'))
);

create table planificacion_original_etapa (
    pedido_id uuid not null references planificacion_original (pedido_id),
    numero smallint not null,
    -- Fin previsto de la etapa; nulo si no se podía estimar.
    fin_previsto date,
    primary key (pedido_id, numero),
    constraint planificacion_original_etapa_numero check (numero between 1 and 30)
);

create function planificacion_original_solo_insercion() returns trigger
    language plpgsql as $$
begin
    raise exception 'La planificación original es de solo inserción: no se modifica ni se borra.';
end;
$$;

create trigger planificacion_original_sin_cambios
    before update or delete on planificacion_original
    for each row execute function planificacion_original_solo_insercion();
create trigger planificacion_original_sin_vaciar
    before truncate on planificacion_original
    for each statement execute function planificacion_original_solo_insercion();
create trigger planificacion_original_etapa_sin_cambios
    before update or delete on planificacion_original_etapa
    for each row execute function planificacion_original_solo_insercion();
create trigger planificacion_original_etapa_sin_vaciar
    before truncate on planificacion_original_etapa
    for each statement execute function planificacion_original_solo_insercion();
