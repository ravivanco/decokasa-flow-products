package com.decokasa.seguimiento.bodegas.application.port.in;

import com.decokasa.seguimiento.bodegas.domain.Bodega;
import java.util.List;

/**
 * Catálogo de bodegas (contrato: GET /bodegas). Según el contrato, lo lee cualquier usuario autenticado: no se amplía ni
 * se restringe ese permiso aquí.
 */
public interface ConsultarBodegas {

    /** @param activa nulo para todas; verdadero o falso para filtrar. Ordenadas por nombre. */
    List<Bodega> listar(Boolean activa);
}
