-- V22 (2026-10-24): Colombia activa, según el alcance aprobado de la fase 7 (Richard, 2026-10-22: Colombia con los
-- mismos flujos A/B que Ecuador; requerimientos.md, "Definiciones de la fase 7"; ADR-0024; manual, paso 7.2).
--
-- V1 sembró Colombia inactiva (ADR-0009: Ecuador primero) y V19 creó sus flujos, su bodega y su numeración sin cambiar
-- pais.activo (hallazgo de 7.B1 en documentos/pruebas/fase-7-informe.md). V19 ya está publicada (origin) y puede estar
-- aplicada: no se edita; se corrige aquí, en la siguiente versión libre.
--
-- Activar el país no da acceso a nadie: esta migración no toca usuario_pais. Los países los asigna el admin y el
-- servidor autoriza con los asignados hoy (ADR-0024). No cambia Ecuador ni ninguna definición, bodega o pedido.

update pais set activo = true where codigo = 'CO' and not activo;

do $$
begin
    if not exists (select 1 from pais where codigo = 'CO' and activo) then
        raise exception 'V22: Colombia debe quedar activa';
    end if;
    if not exists (select 1 from pais where codigo = 'EC' and activo) then
        raise exception 'V22: Ecuador debe seguir activo';
    end if;
end
$$;
