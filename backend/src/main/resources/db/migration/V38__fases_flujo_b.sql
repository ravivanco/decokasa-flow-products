-- V38: el flujo B de Ecuador (pedido propuesto por China) también se muestra en dos fases, como el flujo A v2 (pedido de
-- Richard, 2026-11-15). Solo completa un atributo visual que estaba vacío (fase de cada etapa, V32), igual que V11 y V18:
-- no cambia etapas, transiciones, documentos, plazos ni pedidos. Mismo criterio que A: hasta Fabricación, «Pedido y
-- fabricación»; desde Embarque, «Logística».
do $$
declare
    flujo uuid;
    cambiadas int;
begin
    select id into flujo from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = 'B' and version = 1;
    if flujo is null then
        raise exception 'V38: no existe el flujo B de Ecuador v1.';
    end if;
    if (select count(*) from definicion_etapa where flujo_id = flujo) <> 12
       or (select nombre from definicion_etapa where flujo_id = flujo and numero = 6) <> 'Fabricación'
       or (select nombre from definicion_etapa where flujo_id = flujo and numero = 7) <> 'Embarque' then
        raise exception 'V38: el flujo B de Ecuador v1 no tiene la forma esperada.';
    end if;
    update definicion_etapa set fase_numero = 1, fase_nombre = 'Pedido y fabricación'
    where flujo_id = flujo and numero between 1 and 6 and fase_numero is null;
    update definicion_etapa set fase_numero = 2, fase_nombre = 'Logística'
    where flujo_id = flujo and numero between 7 and 12 and fase_numero is null;
    select count(*) into cambiadas from definicion_etapa where flujo_id = flujo and fase_numero is not null;
    if cambiadas <> 12 then
        raise exception 'V38: quedaron etapas del flujo B sin fase.';
    end if;
end;
$$;
