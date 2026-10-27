-- V18: etapas revisoras del flujo B de Ecuador, versión 1 (fase 6; F6-D4, aprobado por Richard el 2026-10-21, no por
-- el cliente). Siguiente versión disponible tras V17.
--
--   B2 "Revisión de productos propuestos" revisa B1 (la Solicitud y el producto propuesto).
--   B4 "Revisión y aprobación" revisa B3 (los Documentos codificados), como A3 revisa A2 (V3).
--
-- Solo cambia datos de B y solo esas dos marcas (P-17). El permiso de descarga no cambia de código: además del admin
-- y de quien puede actuar en la etapa del archivo, descarga quien puede actuar en la etapa revisora según P-06 (cuenta
-- y roles vigentes; si la revisora tiene persona asignada, solo ella o el admin, no todo el rol).
--
-- No se tocan A (A3 revisa A2 se conserva) ni B5: Análisis no gana acceso a B3, igual que A4 respecto a A2.
-- Como V3 y V11, completa la versión 1 del flujo B con un atributo que no tenía marcado. Solo amplía la lectura de
-- archivos a las revisoras aprobadas; no cambia transiciones, documentos ni plazos.

update definicion_etapa set revisa_etapa = 1
    where id = '01a15bc1-f400-7000-8000-000000000102'
      and flujo_id = '01a15bc1-f400-7000-8000-000000000001'
      and numero = 2 and revisa_etapa is null;

update definicion_etapa set revisa_etapa = 3
    where id = '01a15bc1-f400-7000-8000-000000000104'
      and flujo_id = '01a15bc1-f400-7000-8000-000000000001'
      and numero = 4 and revisa_etapa is null;

do $$
declare
    flujo_b constant uuid := '01a15bc1-f400-7000-8000-000000000001';
    flujo_a constant uuid := '01a13cdb-cc00-7000-8000-000000000001';
begin
    -- En B solo revisan B2 (a B1) y B4 (a B3); B5 y las demás, nada.
    if (select count(*) from definicion_etapa where flujo_id = flujo_b and revisa_etapa is not null) <> 2
       or not exists (select 1 from definicion_etapa where flujo_id = flujo_b and numero = 2 and revisa_etapa = 1)
       or not exists (select 1 from definicion_etapa where flujo_id = flujo_b and numero = 4 and revisa_etapa = 3) then
        raise exception 'V18: el flujo B debe tener solo B2 revisa B1 y B4 revisa B3.';
    end if;
    -- A sin cambios: solo A3 revisa A2.
    if (select count(*) from definicion_etapa where flujo_id = flujo_a and revisa_etapa is not null) <> 1
       or not exists (select 1 from definicion_etapa where flujo_id = flujo_a and numero = 3 and revisa_etapa = 2) then
        raise exception 'V18: la relación de revisión del flujo A no debe cambiar.';
    end if;
end;
$$;
