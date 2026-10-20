-- V11: marca explícita de la etapa de llegada a aduana (fase 4, paso 4.3; ADR-0018, decisión del equipo del
-- 2026-10-19). Siguiente versión disponible tras V10.
--
-- Al aprobar una etapa marcada con una acción que avanza, el pedido guarda pedido.llegada_aduana_en (V9) con la hora
-- del servidor. La marca es parte de la definición versionada del flujo (ADR-0004): no se deduce de REPORTAR_DEMORA
-- ni se fija el número de etapa en el código.
--
-- Como V3 con revisa_etapa, es un atributo nuevo de la versión 1 del flujo A, no un cambio de una regla vigente.
-- Ningún pedido llegó todavía a A7 (A6 sigue bloqueada por documentos pendientes, F4-D4).
-- El flujo B todavía no está en los datos; su etapa de llegada se marcará cuando se cargue.

-- Default false: todas las etapas existentes quedan sin marca (cambio solo de catálogo; la tabla tiene pocas filas).
alter table definicion_etapa add column registra_llegada boolean not null default false;

-- Una etapa automática (Fin) no tiene responsable que dé el check de llegada.
alter table definicion_etapa add constraint definicion_etapa_llegada_no_automatica
    check (not (registra_llegada and automatica));

-- Como máximo una etapa de llegada por versión de flujo.
create unique index definicion_etapa_llegada_unica on definicion_etapa (flujo_id) where registra_llegada;

-- Flujo A de Ecuador, versión 1: solo A7 "Aduana Ecuador" (requerimientos 4.4: "✓ Llegada").
update definicion_etapa set registra_llegada = true
    where id = '01a13cdb-cc00-7000-8000-000000000107'
      and flujo_id = '01a13cdb-cc00-7000-8000-000000000001'
      and numero = 7;
