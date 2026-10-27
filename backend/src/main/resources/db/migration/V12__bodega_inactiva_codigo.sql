-- V12: código de error propio para "bodega inexistente o inactiva" (fase 4, paso 4.4; F4-D2, decisión provisional del
-- equipo del 2026-10-19). Siguiente versión disponible tras V11. V9 no se modifica: aquí se reemplaza solo la función
-- de su trigger, con la misma regla.
--
-- La aplicación comprueba antes que la bodega esté activa; este trigger es la última barrera ante una desactivación
-- simultánea (FOR SHARE espera a la desactivación en curso). Con el SQLSTATE propio UB001 el adaptador de pedidos
-- traduce solo este rechazo a un error del contrato (400 en bodegaId), sin interpretar el texto del mensaje ni
-- capturar otros errores SQL. El trigger pedido_bodega_activa de V9 sigue llamando a esta función.
-- Los códigos usan la clase propia UB (las clases 0-4 y A-H las reserva el estándar SQL).
-- Además, una bodega ya asignada no se cambia ni se quita (SQLSTATE UB002): F4-D2 no admite cambios posteriores.

create or replace function pedido_bodega_activa() returns trigger
    language plpgsql as $$
begin
    -- F4-D2: la bodega se asigna una sola vez (al aprobar Bodega) y no se cambia ni se quita después. La aplicación ya
    -- lo impide; esto es la barrera ante escrituras directas. Cambiarla queda pendiente del cliente.
    if tg_op = 'UPDATE' and old.bodega_id is not null and new.bodega_id is distinct from old.bodega_id then
        raise exception 'La bodega de un pedido se asigna una sola vez y no se cambia.'
            using errcode = 'UB002';
    end if;
    if new.bodega_id is not null
            and (tg_op = 'INSERT' or new.bodega_id is distinct from old.bodega_id) then
        perform 1 from bodega where id = new.bodega_id and activa for share;
        if not found then
            raise exception 'La bodega no existe o está inactiva: no admite asignaciones nuevas.'
                using errcode = 'UB001';
        end if;
    end if;
    return new;
end;
$$;
